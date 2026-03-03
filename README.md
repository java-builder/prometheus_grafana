# Spring Boot Monitoring với Prometheus + Grafana + Alertmanager

## Monitoring là gì?

**Monitoring** (giám sát hệ thống) là quá trình liên tục thu thập, lưu trữ, phân tích và trực quan hóa các chỉ số (metrics) của ứng dụng và hạ tầng để đảm bảo hệ thống hoạt động ổn định, phát hiện sớm các vấn đề và đưa ra quyết định tối ưu hóa.

**Các thành phần chính của Monitoring:**

1. **Metrics Collection** - Thu thập dữ liệu
   - CPU usage, Memory usage, Disk I/O
   - Request count, Response time, Throughput
   - Error rate (HTTP 4xx, 5xx)
   - Database connection pool, Query performance
   - JVM metrics (Heap, GC, Thread count)

2. **Data Storage** - Lưu trữ time-series data
   - Lưu trữ metrics theo thời gian
   - Cho phép query và phân tích lịch sử

3. **Visualization** - Trực quan hóa
   - Dashboard hiển thị metrics real-time
   - Graphs, Charts để theo dõi xu hướng

4. **Alerting** - Cảnh báo tự động
   - Phát hiện anomaly (bất thường)
   - Gửi thông báo qua Email, Slack, SMS
   - Escalation khi vấn đề nghiêm trọng

## Tại sao cần Monitoring?

### 1. Phát hiện sự cố sớm (Proactive vs Reactive)

**Không có Monitoring (Reactive):**
```
09:00 - Hệ thống bắt đầu chậm (không ai biết)
09:30 - Memory leak nghiêm trọng (không ai biết)
10:00 - Application crash
10:05 - User bắt đầu phàn nàn
10:10 - Team nhận được ticket từ user
10:15 - Bắt đầu điều tra (không có data)
10:45 - Tìm ra nguyên nhân
11:00 - Fix và deploy
→ Downtime: 1 giờ, User experience: Rất tệ
```

**Có Monitoring (Proactive):**
```
09:00 - Hệ thống bắt đầu chậm
09:05 - Prometheus phát hiện response time tăng
09:06 - Alert gửi đến team qua email
09:10 - Team check dashboard, thấy memory leak
09:20 - Restart service hoặc scale up
09:25 - Hệ thống trở lại bình thường
→ Downtime: 0, User không hề biết có vấn đề
```

### 2. Tối ưu hóa Performance

**Ví dụ thực tế:**
- Phát hiện API `/api/users` có response time 3s (chậm)
- Xem metrics: Database query mất 2.8s
- Phân tích: Query không có index
- Fix: Thêm index → Response time giảm xuống 200ms
- Kết quả: User experience tốt hơn, giảm server load

### 3. Capacity Planning (Lập kế hoạch mở rộng)

**Phân tích xu hướng:**
```
Tháng 1: CPU usage trung bình 30%
Tháng 2: CPU usage trung bình 45%
Tháng 3: CPU usage trung bình 60%
→ Dự đoán: Tháng 5 sẽ đạt 90% (nguy hiểm)
→ Hành động: Scale up trước khi quá tải
```

### 4. Debugging nhanh hơn

**Khi có lỗi production:**
- Xem metrics: Thời điểm nào lỗi bắt đầu?
- Xem logs: Error message là gì?
- Xem traces: Request đi qua service nào?
- So sánh: Có thay đổi gì trước khi lỗi?
→ Tìm root cause nhanh hơn 10 lần

### 5. SLA/SLO Compliance

**Service Level Agreement:**
- Uptime: 99.9% (downtime tối đa 43 phút/tháng)
- Response time: 95% requests < 500ms
- Error rate: < 0.1%

Monitoring giúp track và đảm bảo đạt được các chỉ số này.

### So sánh: Không có vs Có Monitoring

| Tiêu chí | Không có Monitoring | Có Monitoring |
|----------|---------------------|---------------|
| Phát hiện lỗi | User phàn nàn mới biết | Tự động phát hiện trước |
| Thời gian xử lý | Lâu (không có data) | Nhanh (có đầy đủ data) |
| Downtime | Dài (phát hiện muộn) | Ngắn (phát hiện sớm) |
| User experience | Tệ (bị ảnh hưởng) | Tốt (ít bị ảnh hưởng) |
| Chi phí | Cao (mất khách hàng) | Thấp (ngăn chặn sớm) |
| Quyết định | Dựa trên cảm tính | Dựa trên data |

## Monitoring trong Java Spring Boot

### Stack phổ biến

1. **Spring Boot Actuator** - Expose metrics từ ứng dụng
2. **Micrometer** - Abstraction layer cho metrics (như SLF4J cho logging)
3. **Prometheus** - Thu thập và lưu trữ metrics
4. **Grafana** - Visualize metrics qua dashboard
5. **Alertmanager** - Quản lý và gửi alerts

### Kiến trúc

![Monitoring Architecture](https://res.cloudinary.com/drdskl2up/image/upload/v1772531329/1_OMHxzd0ToQPTEcx117lMfg_oqfrhi.png)

---

## Phần 1: Setup Spring Boot Application

### 1.1. Dependencies (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Giải thích:**


- **spring-boot-starter-actuator**: Cung cấp các endpoints để expose metrics, health check, info
  - `/actuator/health` - Trạng thái ứng dụng
  - `/actuator/metrics` - Metrics dạng JSON
  - `/actuator/prometheus` - Metrics format cho Prometheus

- **micrometer-registry-prometheus**: Chuyển đổi metrics sang format Prometheus hiểu được
  - Micrometer là abstraction layer (giống SLF4J)
  - Có thể đổi sang Datadog, New Relic mà không cần sửa code

### 1.2. Configuration (application.yaml)

```yaml
spring:
  application:
    name: monitoring

management:
  endpoints:
    web:
      exposure:
        include: "*"
  
  metrics:
    tags:
      application: ${spring.application.name}
```

**Giải thích:**

- `spring.application.name`: Tên ứng dụng, sẽ được gắn vào mọi metrics
- `management.endpoints.web.exposure.include: "*"`: Expose tất cả actuator endpoints
  - Production nên chỉ expose cần thiết: `prometheus,health,info`
- `management.metrics.tags.application`: Thêm tag `application=monitoring` vào mọi metrics
  - Giúp filter metrics khi có nhiều services

### 1.3. Test Actuator

Sau khi cấu hình, start app và test:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Xem tất cả metrics
curl http://localhost:8080/actuator/metrics

# Xem metrics format Prometheus
curl http://localhost:8080/actuator/prometheus
```

### 1.4. MonitoringController - API để test alerts

```java
package com.javabuilder.alerttelegram.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
@Slf4j(topic = "MONITORING-TEST-CONTROLLER")
public class MonitoringController {

    @GetMapping("/slow")
    ResponseEntity<String> slowResponse(@RequestParam(defaultValue = "2000") int delayMs) 
            throws InterruptedException {
        log.info("Simulating slow response with delay: {}ms", delayMs);
        Thread.sleep(delayMs);
        return ResponseEntity.ok("Response completed after " + delayMs + "ms delay");
    }

    @GetMapping("/error")
    ResponseEntity<String> triggerError() {
        log.error("Triggering 500 error for demo");
        throw new RuntimeException("Simulated 500 error for Prometheus alert testing");
    }

    @GetMapping("/health")
    ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is running");
    }
}
```

**Giải thích:**

- **`/slow`**: Mô phỏng API chậm
  - Nhận parameter `delayMs` (mặc định 2000ms = 2s)
  - Dùng `Thread.sleep()` để delay
  - Dùng để test alert "SlowResponseTime"

- **`/error`**: Mô phỏng lỗi 500
  - Throw `RuntimeException` → Spring trả về HTTP 500
  - Dùng để test alert "HighServerErrorRate"

- **`/health`**: API đơn giản để test
  - Trả về 200 OK
  - Dùng để kiểm tra app còn sống

**Test:**

```bash
# Test slow API (2 giây)
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=2000"

# Test slow API (6 giây - trigger alert)
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000"

# Test error
curl http://localhost:8080/api/v1/monitoring/error
```

---

## Phần 2: Prometheus + Grafana + Alertmanager

### 2.1. Prometheus là gì?

**Prometheus** là hệ thống monitoring và alerting mã nguồn mở:
- Thu thập metrics từ các targets (pull model)
- Lưu trữ dạng time-series database
- Cung cấp query language (PromQL)
- Đánh giá alert rules

**Đặc điểm:**
- Pull-based: Prometheus chủ động lấy metrics (không phải app push)
- Time-series: Lưu data theo thời gian
- Multi-dimensional: Metrics có labels (tags)

### 2.2. Grafana là gì?

**Grafana** là công cụ visualize metrics:
- Tạo dashboard đẹp mắt
- Hỗ trợ nhiều data sources (Prometheus, MySQL, etc)
- Alerting (nhưng thường dùng Alertmanager)

### 2.3. Alertmanager là gì?

**Alertmanager** quản lý alerts từ Prometheus:
- Grouping: Gom nhiều alerts thành 1 notification
- Inhibition: Tắt alerts trùng lặp
- Silencing: Tạm tắt alerts
- Routing: Gửi đến nhiều channels (Email, Slack, PagerDuty)

---

## Phần 3: Docker Compose Setup

### 3.1. docker-compose.yaml

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alert-rules.yml:/etc/prometheus/alert-rules.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:

networks:
  monitoring:
```

**Giải thích:**

**Prometheus service:**
- `image: prom/prometheus:latest` - Docker image của Prometheus
- `ports: "9090:9090"` - Expose port 9090 ra ngoài để truy cập UI
- `volumes` - Mount config files từ máy host vào container
- `command` - Chỉ định config file và storage path
- `networks: monitoring` - Kết nối vào network chung

**Alertmanager service:**
- `ports: "9093:9093"` - Expose port 9093
- Mount config `alertmanager.yml`

**Grafana service:**
- `ports: "3000:3000"` - Expose port 3000
- `depends_on: prometheus` - Start sau Prometheus

**Volumes & Network:**
- Persistent storage cho data
- Network `monitoring` để containers giao tiếp với nhau

### 3.2. Khởi động Docker services

```bash
# Start tất cả services
docker-compose up -d
```

---

## Phần 4: Cấu hình Prometheus

### 4.1. prometheus.yml

```yaml
global:
  # Khoảng thời gian scrape metrics từ targets
  scrape_interval: 5s
  
  # Khoảng thời gian đánh giá alert rules
  evaluation_interval: 10s

# Load alert rules từ file
rule_files:
  - '/etc/prometheus/alert-rules.yml'

# Cấu hình Alertmanager - nơi nhận alerts từ Prometheus
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

# Cấu hình scrape targets - nơi lấy metrics
scrape_configs:
  # Scrape metrics từ chính Prometheus
  - job_name: 'prometheus'
    static_configs:
      - targets: ['prometheus:9090']
  
  # Scrape metrics từ Spring Boot app
  - job_name: 'backend-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ["host.docker.internal:8080"]
```

**Giải thích chi tiết:**



**1. Global section:**

```yaml
scrape_interval: 5s
```
- Prometheus sẽ lấy metrics từ targets mỗi 5 giây
- Giá trị nhỏ = real-time hơn nhưng tốn tài nguyên
- Production thường dùng 15s-60s

```yaml
evaluation_interval: 10s
```
- Prometheus đánh giá alert rules mỗi 10 giây
- Check xem có alert nào firing không

**2. rule_files:**

```yaml
rule_files:
  - '/etc/prometheus/alert-rules.yml'
```
- Đường dẫn đến file chứa alert rules
- Prometheus load rules từ file này
- Có thể có nhiều files: `- '/etc/prometheus/rules/*.yml'`

**3. alerting:**

```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```
- Khi có alert firing, Prometheus gửi đến Alertmanager
- `alertmanager:9093`: Hostname trong Docker network
- Có thể có nhiều Alertmanagers (high availability)

**4. scrape_configs:**

```yaml
- job_name: 'backend-service'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ["host.docker.internal:8080"]
```

- `job_name`: Tên job, sẽ thành label `job="backend-service"`
- `metrics_path`: Endpoint để lấy metrics (mặc định `/metrics`)
- `targets`: Danh sách targets để scrape

**Về `host.docker.internal:8080`:**

- **Trong bài này**: Spring Boot chạy ngoài Docker (localhost:8080)
- Docker container cần gọi về máy host → Dùng `host.docker.internal`
- `host.docker.internal` = hostname đặc biệt trỏ về máy host

**Khi deploy production (cùng Docker network):**

```yaml
# Nếu Spring Boot cũng chạy trong Docker
- job_name: 'backend-service'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ["backend-service:8080"]
```

- `backend-service`: Tên container/service của Spring Boot
- Các containers trong cùng network gọi nhau qua tên

**Ví dụ docker-compose.yaml khi deploy cùng nhau:**

```yaml
services:
  backend:
    image: my-spring-boot-app
    container_name: backend-service
    ports:
      - "8080:8080"
    networks:
      - monitoring
  
  prometheus:
    # ... config như trên
    # Trong prometheus.yml dùng: backend-service:8080
```

### 4.2. Kiểm tra Prometheus

```bash
# Truy cập Prometheus UI
http://localhost:9090

# Check targets
http://localhost:9090/targets
# Phải thấy "backend-service" status UP

# Check rules
http://localhost:9090/rules

# Check alerts
http://localhost:9090/alerts
```

![Prometheus Alerts](https://res.cloudinary.com/drdskl2up/image/upload/v1772531440/Screenshot_2026-03-03_165020_bvz5l0.png)

**Trạng thái alerts:**
- **INACTIVE** (màu xanh): Alert chưa trigger
- **PENDING** (màu vàng): Condition đúng nhưng chưa đủ thời gian `for`
- **FIRING** (màu đỏ): Alert đang firing, đã gửi đến Alertmanager

---

## Phần 5: Cấu hình Alert Rules

### 5.1. alert-rules.yml

```yaml
groups:
  - name: application_alerts
    interval: 5s
    rules:
      # Alert khi có HTTP 500 errors
      - alert: HighServerErrorRate
        expr: rate(http_server_requests_seconds_count{status="500"}[1m]) > 0
        for: 5s
        labels:
          severity: critical
          service: backend
        annotations:
          summary: "High 500 error rate detected"
          description: "Application {{ $labels.application }} has {{ $value }} errors per second"

      # Alert khi response time quá chậm (> 5s)
      - alert: SlowResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m])) > 5
        for: 10s
        labels:
          severity: warning
          service: backend
        annotations:
          summary: "Slow API response time"
          description: "95th percentile response time is {{ $value }}s (threshold: 5s)"

      # Alert khi application down
      - alert: ApplicationDown
        expr: up{job="backend-service"} == 0
        for: 5s
        labels:
          severity: critical
          service: backend
        annotations:
          summary: "Application is down"
          description: "Backend service {{ $labels.instance }} is not responding"
```

**Giải thích chi tiết:**

**1. Group structure:**

```yaml
groups:
  - name: application_alerts
    interval: 5s
```
- `name`: Tên group (để tổ chức rules)
- `interval`: Đánh giá rules trong group này mỗi 5s

**2. Alert: HighServerErrorRate**

```yaml
expr: rate(http_server_requests_seconds_count{status="500"}[1m]) > 0
```
- `http_server_requests_seconds_count`: Metric đếm số requests
- `{status="500"}`: Filter chỉ lấy requests có status 500
- `[1m]`: Trong 1 phút gần nhất
- `rate()`: Tính tốc độ tăng per second
- `> 0`: Nếu có bất kỳ lỗi 500 nào

**Ý nghĩa**: Nếu có lỗi 500 trong 1 phút gần nhất → Alert

```yaml
for: 5s
```
- Alert chỉ firing sau khi condition đúng liên tục trong 5s
- Tránh false positive (lỗi thoáng qua)

```yaml
labels:
  severity: critical
  service: backend
```
- Labels để phân loại alert
- Dùng cho routing và inhibition

```yaml
annotations:
  summary: "High 500 error rate detected"
  description: "Application {{ $labels.application }} has {{ $value }} errors per second"
```
- `summary`: Tiêu đề ngắn gọn
- `description`: Mô tả chi tiết
- `{{ $labels.application }}`: Lấy label từ metric
- `{{ $value }}`: Giá trị của expression

**3. Alert: SlowResponseTime**

```yaml
expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m])) > 5
```
- `http_server_requests_seconds_bucket`: Histogram metric
- `histogram_quantile(0.95, ...)`: Tính percentile 95
- Nghĩa: 95% requests phải nhanh hơn giá trị này
- `> 5`: Nếu p95 > 5 giây → Alert

**Ví dụ**: 
- 100 requests trong 1 phút
- 95 requests nhanh nhất < 5s → OK
- 5 requests chậm nhất > 5s → ALERT

**4. Alert: ApplicationDown**

```yaml
expr: up{job="backend-service"} == 0
```
- `up`: Metric tự động của Prometheus
- `up=1`: Target đang UP
- `up=0`: Target DOWN (không scrape được)
- `{job="backend-service"}`: Filter theo job name

**Ý nghĩa**: Nếu Prometheus không scrape được metrics từ app → Alert

---

## Phần 6: Cấu hình Alertmanager

### 6.1. Tạo Gmail App Password

1. Truy cập: https://myaccount.google.com/apppasswords
2. Đăng nhập Gmail
3. Chọn "App passwords"
4. Tạo password mới:
   - App: Mail
   - Device: Other (nhập "Alertmanager")
5. Copy password 16 ký tự (ví dụ: `abcd efgh ijkl mnop`)

**Lưu ý:**
- Phải bật 2FA trước khi tạo App Password
- Không dùng password thường của Gmail
- Gmail đã tắt "Less secure app access"

### 6.2. alertmanager.yml

```yaml
global:
  # Sau 5 phút không nhận alert nữa thì coi như đã resolved
  resolve_timeout: 5m
  
  # SMTP server của Gmail, port 587 (STARTTLS)
  smtp_smarthost: 'smtp.gmail.com:587'
  
  # Email người gửi (FROM field)
  smtp_from: 'your-email@gmail.com'
  
  # Username để login vào Gmail SMTP
  smtp_auth_username: 'your-email@gmail.com'
  
  # Password để login (phải dùng App Password)
  smtp_auth_password: 'your-app-password'
  
  # Bắt buộc dùng TLS encryption (bảo mật)
  smtp_require_tls: true

route:
  # Gom các alerts có cùng alertname và service thành 1 email
  group_by: ['alertname', 'service']
  
  # Đợi 10 giây sau khi nhận alert đầu tiên
  group_wait: 10s
  
  # Nếu có thêm alerts mới trong cùng group, đợi 10s rồi gửi batch tiếp
  group_interval: 10s
  
  # Nếu alert vẫn đang firing, gửi lại email sau mỗi 1 giờ (tránh spam)
  repeat_interval: 1h
  
  # Gửi đến receiver tên 'email-alerts'
  receiver: 'email-alerts'

receivers:
  - name: 'email-alerts'
    email_configs:
      - send_resolved: true
        to: 'recipient@gmail.com'
        headers:
          Subject: '🚨 [{{ .Status | toUpper }}] {{ .GroupLabels.alertname }} - {{ .GroupLabels.service }}'
        html: |
          <h2>🚨 Alert: {{ .GroupLabels.alertname }}</h2>
          <p><strong>Status:</strong> {{ .Status | toUpper }}</p>
          <p><strong>Severity:</strong> {{ .GroupLabels.severity }}</p>
          <p><strong>Service:</strong> {{ .GroupLabels.service }}</p>
          <hr>
          {{ range .Alerts }}
          <h3>{{ .Annotations.summary }}</h3>
          <p>{{ .Annotations.description }}</p>
          <p><strong>Started at:</strong> {{ .StartsAt }}</p>
          {{ if .EndsAt }}<p><strong>Ended at:</strong> {{ .EndsAt }}</p>{{ end }}
          <hr>
          {{ end }}
```

**Giải thích chi tiết:**

**1. Global section - SMTP config:**

```yaml
smtp_smarthost: 'smtp.gmail.com:587'
```
- SMTP server của Gmail
- Port 587: STARTTLS (TLS encryption)
- Port 465: SSL/TLS (implicit)

```yaml
smtp_from: 'your-email@gmail.com'
smtp_auth_username: 'your-email@gmail.com'
```
- Email gửi đi (FROM field)
- Username để authenticate vào SMTP server
- Với Gmail: username = email address

```yaml
smtp_auth_password: 'your-app-password'
```
- App Password (16 ký tự)
- KHÔNG phải password thường của Gmail

```yaml
smtp_require_tls: true
```
- Bắt buộc dùng TLS encryption
- Bảo mật khi gửi email

**2. Route section - Routing rules:**

```yaml
group_by: ['alertname', 'service']
```
- Gom alerts có cùng `alertname` và `service` thành 1 notification
- Ví dụ: 10 alerts "HighServerErrorRate" từ "backend" → 1 email

```yaml
group_wait: 10s
```
- Sau khi nhận alert đầu tiên, đợi 10s
- Xem có thêm alerts tương tự không
- Rồi gửi chung 1 lần (tránh spam)

```yaml
group_interval: 10s
```
- Nếu có thêm alerts mới trong cùng group
- Đợi 10s rồi gửi batch tiếp

```yaml
repeat_interval: 1h
```
- Nếu alert vẫn đang firing
- Gửi lại email sau mỗi 1 giờ
- Nhắc nhở team xử lý

**3. Receivers section:**

```yaml
send_resolved: true
```
- Gửi email khi alert resolved (hết lỗi)
- Ví dụ: "✅ [RESOLVED] ApplicationDown"

```yaml
to: 'recipient@gmail.com'
```
- Email nhận alert
- Có thể nhiều emails: `to: 'team@company.com,oncall@company.com'`

```yaml
headers:
  Subject: '🚨 [{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
```
- Subject của email
- `{{ .Status }}`: firing hoặc resolved
- `{{ .GroupLabels.alertname }}`: Tên alert

**Template variables:**
- `{{ .Status }}`: firing / resolved
- `{{ .GroupLabels.alertname }}`: Tên alert
- `{{ .GroupLabels.severity }}`: critical / warning
- `{{ .Annotations.summary }}`: Summary text
- `{{ .StartsAt }}`: Thời gian bắt đầu
- `{{ .EndsAt }}`: Thời gian kết thúc (nếu resolved)

### 6.3. Cập nhật config

Sửa file `alertmanager.yml`:

```yaml
smtp_from: 'your-email@gmail.com'
smtp_auth_username: 'your-email@gmail.com'
smtp_auth_password: 'abcd efgh ijkl mnop'  # App Password

receivers:
  - name: 'email-alerts'
    email_configs:
      - to: 'recipient@gmail.com'  # Email nhận alert
```

Restart Alertmanager:

```bash
docker-compose restart alertmanager
```

---

## Phần 7: Testing Alerts

Sau khi setup xong, đã đến lúc test hệ thống monitoring! Chúng ta sẽ mô phỏng các tình huống lỗi thực tế để kiểm tra xem alerts có hoạt động không.

### 7.1. Test Alert: HighServerErrorRate (Lỗi 500)

**Mục đích:** Kiểm tra alert khi API trả về lỗi 500 (Internal Server Error)

**Cách test:**

**Sử dụng Postman:**

1. Mở Postman
2. Tạo request mới:
   - Method: `GET`
   - URL: `http://localhost:8080/api/v1/monitoring/error`
3. Click **Send** nhiều lần (5-10 lần)

**Hoặc sử dụng cURL:**

```bash
# Gọi API error 5 lần
curl http://localhost:8080/api/v1/monitoring/error
curl http://localhost:8080/api/v1/monitoring/error
curl http://localhost:8080/api/v1/monitoring/error
curl http://localhost:8080/api/v1/monitoring/error
curl http://localhost:8080/api/v1/monitoring/error
```

**Kết quả mong đợi:**

1. **Sau 10-15 giây:**
   - Vào Prometheus: http://localhost:9090/alerts
   - Alert **HighServerErrorRate** chuyển sang màu đỏ (FIRING)

2. **Sau 20-30 giây:**
   - Check email: Nhận email với subject:
     ```
     🚨 [FIRING] HighServerErrorRate - backend-service
     ```
   - Nội dung email:
     ```
     Alert: HighServerErrorRate
     Status: FIRING
     Severity: critical
     Service: backend-service
     Summary: High 500 error rate detected
     Description: Application monitoring has X errors per second
     ```

3. **Trong Grafana:**
   - Vào dashboard
   - Phần **Total Error** sẽ tăng lên
   - Graph **ERROR logs** sẽ có spike

**Giải thích:**
- API `/monitoring/error` throw `RuntimeException` → Spring Boot trả về HTTP 500
- Prometheus scrape metrics và phát hiện `http_server_requests_seconds_count{status="500"}` > 0
- Alert rule trigger sau 5 giây
- Alertmanager nhận alert và gửi email

---

### 7.2. Test Alert: SlowResponseTime (API chậm)

**Mục đích:** Kiểm tra alert khi API response time > 5 giây

**Cách test:**

**Sử dụng Postman:**

1. Mở Postman
2. Tạo request mới:
   - Method: `GET`
   - URL: `http://localhost:8080/api/v1/monitoring/slow?delayMs=6000`
3. Click **Send** nhiều lần (10-15 lần)
4. Chờ mỗi request hoàn thành (6 giây/request)

**Hoặc sử dụng cURL (chạy song song):**

```bash
# Gọi 10 requests song song
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
curl "http://localhost:8080/api/v1/monitoring/slow?delayMs=6000" &
```

**Kết quả mong đợi:**

1. **Sau 20-30 giây:**
   - Vào Prometheus: http://localhost:9090/alerts
   - Alert **SlowResponseTime** chuyển sang màu đỏ (FIRING)

2. **Sau 40-50 giây:**
   - Check email: Nhận email với subject:
     ```
     🚨 [FIRING] SlowResponseTime - backend-service
     ```
   - Nội dung email:
     ```
     Alert: SlowResponseTime
     Status: FIRING
     Severity: warning
     Service: backend-service
     Summary: Slow API response time
     Description: 95th percentile response time is 6.2s (threshold: 5s)
     ```

3. **Trong Grafana:**
   - Response time graph sẽ tăng lên
   - Có thể thấy spike trong CPU Usage

**Giải thích:**
- API `/monitoring/slow` sleep 6 giây trước khi trả về
- Prometheus tính toán percentile 95 của response time
- Khi p95 > 5 giây trong 1 phút → Alert trigger
- Cần nhiều requests để có đủ data cho histogram

---

### 7.3. Test Alert: ApplicationDown (Ứng dụng down)

**Mục đích:** Kiểm tra alert khi Spring Boot app không phản hồi

**Cách test:**

1. **Stop Spring Boot application:**

   Vào terminal đang chạy Spring Boot, nhấn `Ctrl + C`

   Hoặc tìm và kill process:
   ```bash
   # Windows
   jps
   # Tìm process ID của Spring Boot
   taskkill /F /PID <process-id>

   # Linux/Mac
   ps aux | grep java
   kill <process-id>
   ```

2. **Đợi 10-15 giây**

3. **Check Prometheus:**
   - Vào http://localhost:9090/alerts
   - Alert **ApplicationDown** chuyển sang màu đỏ (FIRING)

4. **Check email:**
   - Nhận email với subject:
     ```
     🚨 [FIRING] ApplicationDown - backend-service
     ```
   - Nội dung email:
     ```
     Alert: ApplicationDown
     Status: FIRING
     Severity: critical
     Service: backend-service
     Summary: Application is down
     Description: Backend service host.docker.internal:8080 is not responding
     ```

5. **Start lại application:**
   ```bash
   ./mvnw spring-boot:run
   ```

6. **Đợi 10-15 giây**

7. **Check email lần nữa:**
   - Nhận email với subject:
     ```
     ✅ [RESOLVED] ApplicationDown - backend-service
     ```
   - Nội dung email:
     ```
     Alert: ApplicationDown
     Status: RESOLVED
     Severity: critical
     Service: backend-service
     Summary: Application is down
     Description: Backend service host.docker.internal:8080 is not responding
     Ended at: 2026-03-03 17:30:00
     ```

**Giải thích:**
- Khi app down, Prometheus không scrape được metrics từ `/actuator/prometheus`
- Metric `up{job="backend-service"}` chuyển từ 1 → 0
- Alert trigger sau 5 giây
- Khi app start lại, `up` chuyển về 1 → Alert resolved
- Alertmanager gửi email "RESOLVED" (vì `send_resolved: true`)

---

### 7.4. Kiểm tra trong Grafana

Sau khi test các alerts, vào Grafana dashboard để xem metrics:

1. **Vào dashboard:** http://localhost:3000/dashboards
2. **Chọn dashboard:** backend-service
3. **Quan sát các metrics:**
   - **Total Error**: Số lượng lỗi tăng lên sau test 7.1
   - **CPU Usage**: Tăng lên khi test 7.2 (API chậm)
   - **Uptime**: Reset về 0 sau khi restart app (test 7.3)
   - **ERROR logs**: Spike khi có lỗi 500

---

## Kết luận

Bạn đã setup thành công hệ thống monitoring với:

[✓] Spring Boot expose metrics qua Actuator
[✓] Prometheus scrape và lưu trữ metrics
[✓] Alert rules tự động phát hiện lỗi
[✓] Alertmanager gửi email cảnh báo
[✓] Grafana visualize metrics

**Khi triển khai production, hệ thống này sẽ giúp bạn:**

- **Proactive Monitoring**: Phát hiện lỗi trước khi user phàn nàn
- **Real-time Alerting**: Nhận cảnh báo ngay lập tức qua email
- **Performance Tracking**: Theo dõi performance qua Grafana dashboard
- **Faster Debugging**: Debug nhanh hơn với metrics chi tiết
- **Capacity Planning**: Phân tích xu hướng để scale hệ thống

---

**Source code:** https://github.com/java-builder/prometheus_grafana

---

## Phần 8: Grafana Dashboard

### 8.1. Login Grafana

```
URL: http://localhost:3000
Username: admin
Password: admin
```

![Grafana Login](https://res.cloudinary.com/drdskl2up/image/upload/v1772531528/Screenshot_2026-03-03_165111_lloxdq.png)

**Lưu ý:**
- Tài khoản mặc định: `admin/admin`
- Lần đầu login, Grafana sẽ yêu cầu đổi password
- Nên đổi password mạnh cho production

### 8.2. Add Prometheus Data Source

Để Grafana có thể visualize metrics từ Prometheus, cần thiết lập kết nối:

**Bước 1:** Vào **Connections** → **Add new connection**

Hoặc truy cập trực tiếp: http://localhost:3000/connections/add-new-connection?search=Prometheus

![Add Prometheus Connection](https://res.cloudinary.com/drdskl2up/image/upload/v1772531734/Screenshot_2026-03-03_165305_vcahrn.png)

**Bước 2:** Tìm kiếm và chọn **Prometheus**

![Prometheus Data Source](https://res.cloudinary.com/drdskl2up/image/upload/v1772531813/Screenshot_2026-03-03_165618_axdjig.png)

**Bước 3:** Click **Add new data source** (góc trên bên phải)

**Bước 4:** Cấu hình connection:

- **Name**: `prometheus`
- **Prometheus server URL**: `http://prometheus:9090`
  - Dùng `prometheus:9090` vì Grafana và Prometheus cùng Docker network
  - Hostname `prometheus` là tên container trong docker-compose.yaml
  - Nếu Grafana chạy ngoài Docker: dùng `http://localhost:9090`

Các field khác để mặc định.

**Bước 5:** Scroll xuống dưới cùng, click **Save & Test**

Nếu thành công, sẽ thấy message màu xanh: "Successfully queried the Prometheus API."

### 8.3. Import Dashboard

Sau khi add Prometheus data source thành công, import dashboard để visualize metrics:

**Bước 1:** Download file dashboard JSON

- Truy cập: https://res.cloudinary.com/drdskl2up/raw/upload/v1772534038/grafana-dashboard_epbxr5.json
- Copy toàn bộ nội dung JSON
- Tạo file mới tên `grafana-dashboard.json` ở thư mục root của project
- Paste nội dung vào file và save

**Bước 2:** Import dashboard vào Grafana

1. Quay lại mục **Dashboards**, click **Create Dashboard**

![Create Dashboard](https://res.cloudinary.com/drdskl2up/image/upload/v1772531948/Screenshot_2026-03-03_165841_oj4nau.png)

2. Click **Import dashboard**

![Import Dashboard](https://res.cloudinary.com/drdskl2up/image/upload/v1772532014/Screenshot_2026-03-03_165938_zr9nwi.png)

3. Click **Upload dashboard JSON file**
4. Chọn file `grafana-dashboard.json` vừa tạo
5. Grafana sẽ tự động load nội dung file

![Select Prometheus Data Source](https://res.cloudinary.com/drdskl2up/image/upload/v1772532671/Screenshot_2026-03-03_171024_budqve.png)

6. Ở phần **DS_PROMETHEUS**, chọn data source **prometheus** (data source bạn đã tạo ở bước 8.2)
7. Click **Import**

Dashboard sẽ hiển thị các metrics:

![Grafana Dashboard - Basic Statistics](https://res.cloudinary.com/drdskl2up/image/upload/v1772533194/Screenshot_2026-03-03_171905_mf4qu7.png)

![Grafana Dashboard - Logs](https://res.cloudinary.com/drdskl2up/image/upload/v1772533222/Screenshot_2026-03-03_171931_l2oqgj.png)

**Metrics hiển thị:**
- **Basic Statistics**: Uptime, Start time, Heap Used, Non-Heap Used, Process Open Files
- **CPU Usage**: System CPU Usage, Process CPU Usage
- **Load Average**: System load average 1m, CPU Core Size
- **JVM Statistics - Memory**: G1 Eden Space, G1 Old Gen, G1 Survivor Space, Direct Buffers, Mapped Buffers, Threads
- **JVM Statistics - GC**: GC Count, GC Stop the World Duration
- **Logs**: Total Error, Warning Logs, INFO logs, ERROR logs, WARN logs, DEBUG logs, TRACE logs

**Lưu ý:** Nếu muốn dùng dashboard có sẵn từ cộng đồng, có thể:
- Vào https://grafana.com/grafana/dashboards/
- Tìm "Spring Boot" hoặc "JVM"
- Copy Dashboard ID (ví dụ: 4701)
- Import bằng cách nhập ID vào ô "Import via grafana.com"
