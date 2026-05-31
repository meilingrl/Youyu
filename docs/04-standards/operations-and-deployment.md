# Operations and Deployment Standards

## Deployment Architecture

### Current State (Development)

**Environment**: Local development only
- Backend: Spring Boot embedded Tomcat on `localhost:8080`
- Frontend: Vite dev server on `localhost:5173`
- Database: MySQL 8+ on `localhost:3306`
- No containerization
- No CI/CD pipeline for deployment (CI for testing only)

### Target State (Production-Ready)

**Environment**: Cloud-hosted with containerization
- Application: Docker containers orchestrated by Docker Compose or Kubernetes
- Database: Managed cloud database (Aliyun RDS, Tencent Cloud MySQL)
- Static assets: Object storage (OSS) + CDN
- HTTPS: Mandatory with automatic certificate renewal
- Monitoring: Prometheus + Grafana
- Logging: Centralized log aggregation (ELK stack or Loki)

## Pre-Launch Deployment Checklist

### Security Hardening (P0 - Must Complete Before Launch)

#### 1. HTTPS Configuration

**Requirement**: All production traffic must use HTTPS.

**Implementation Options**:

**Option A: Cloud Provider Certificate** (Recommended for simplicity)
- Aliyun/Tencent Cloud load balancer with managed SSL certificate
- Automatic renewal
- Zero configuration in application code

**Option B: Let's Encrypt with Certbot** (For self-hosted)
```bash
# Install Certbot
apt-get install certbot python3-certbot-nginx

# Obtain certificate
certbot --nginx -d youyu.example.com

# Auto-renewal (cron job)
0 0 * * * certbot renew --quiet
```

**Application Configuration** (`application-prod.yml`):
```yaml
server:
  port: 8080
  ssl:
    enabled: false  # SSL termination at load balancer/reverse proxy

# Force HTTPS redirect in reverse proxy (Nginx example)
# server {
#   listen 80;
#   return 301 https://$host$request_uri;
# }
```

**Verification**:
- [ ] All HTTP requests redirect to HTTPS
- [ ] SSL Labs test score A or higher
- [ ] Certificate auto-renewal configured and tested

#### 2. JWT Secret Management

**Current Issue**: Development JWT secret hardcoded in `application.yml`.

**Production Requirement**: Secret must be:
- At least 32 characters (current dev secret meets this)
- Randomly generated (not the dev default)
- Stored as environment variable, never committed to git
- Rotated periodically (every 90 days recommended)

**Implementation**:

```yaml
# application-prod.yml
app:
  jwt:
    secret: ${APP_JWT_SECRET}  # Must be set via environment variable
    expiration-hours: 72
```

**Deployment Script**:
```bash
# Generate secure random secret (32+ bytes, base64 encoded)
export APP_JWT_SECRET=$(openssl rand -base64 48)

# Store in cloud provider secret manager (Aliyun KMS, Tencent Cloud SSM)
# Or pass as environment variable in Docker/K8s deployment
```

**Verification**:
- [ ] `JwtSecretGuard` passes in production profile
- [ ] Secret not visible in logs or error messages
- [ ] Secret rotation procedure documented

#### 3. Database Credentials

**Current**: Credentials in `application.yml` or `MYSQL_PASSWORD` env var.

**Production**: Use cloud provider secret manager or encrypted configuration.

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:youyu}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**Best Practice**: Managed database with IAM authentication (no password).

#### 4. SMTP Email Delivery

Registration verification and forgotten-password recovery send real email
through a configured SMTP provider. Provider credentials and recipient
addresses are deployment inputs, never repository source.

**Required Runtime Variables**:

| Name | Purpose |
|------|---------|
| `APP_MAIL_HOST` | SMTP server host |
| `APP_MAIL_PORT` | SMTP server port |
| `APP_MAIL_USERNAME` | SMTP login username |
| `APP_MAIL_PASSWORD` | SMTP login password or provider authorization code |
| `APP_MAIL_FROM` | Verified sender address |
| `APP_MAIL_SSL_ENABLED` | Whether SMTP SSL is enabled |

**Operational Rules**:
- Verify the provider-specific sender address or sender domain before enabling
  public email-code endpoints.
- Store credentials in the deployment secret manager and inject them as
  environment variables.
- Do not log SMTP passwords, verification codes, recipient addresses, or
  password-reset payloads.
- The `test` profile uses a deterministic fake sender and must not access the
  network.
- Treat missing configuration, authentication failure, or provider rejection
  as a failed delivery. Do not report a successful send to the caller.

**Acceptance Check**:
1. Send a registration code to an approved manual-test recipient and complete
   registration without receiving a JWT.
2. Trigger password recovery for that account, consume the received code once,
   set a new password, and log in with the new password.
3. Confirm that no secret or verification code appears in application logs.

**Troubleshooting**:
- Check that all six `APP_MAIL_*` variables are present in the runtime process.
- Confirm the sender is verified and the provider accepts the configured port
  and SSL mode.
- Use a provider authorization code where the provider does not accept the
  account password.
- Re-run delivery only after fixing the operational error. Do not add a
  development log-only sender.

#### 5. CORS Configuration

**Current**: Likely permissive or not configured.

**Production** (`WebMvcConfig.java`):

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)  // Specific domains only
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

```yaml
# application-prod.yml
app:
  cors:
    allowed-origins: https://youyu.example.com,https://www.youyu.example.com
```

**Verification**:
- [ ] Only production domains allowed
- [ ] Wildcard origins (`*`) not used in production
- [ ] Credentials (cookies, Authorization header) work correctly

#### 6. SQL Injection Prevention

**Current Status**: ✅ Already implemented via JDBC parameterized queries.

**Verification Checklist**:
- [ ] All `JdbcTemplate` queries use `?` placeholders or named parameters
- [ ] No string concatenation in SQL queries
- [ ] User input never directly interpolated into SQL

**Example of Safe Pattern** (already used in codebase):
```java
// SAFE: Parameterized query
String sql = "SELECT * FROM users WHERE username = ?";
jdbcTemplate.queryForMap(sql, username);

// UNSAFE: String concatenation (DO NOT USE)
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

#### 7. Input Validation

**Add JSR-303 Bean Validation** (`pom.xml`):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Controller Example**:
```java
@PostMapping("/api/auth/register")
public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // @Valid triggers validation, throws MethodArgumentNotValidException if invalid
}

// RegisterRequest.java
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Email(message = "Invalid email format")
    private String email;
}
```

**Global Exception Handler** (add to `GlobalExceptionHandler.java`):
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest()
            .body(ApiResponse.error(ResultCode.INVALID_PARAM, message));
}
```

### Infrastructure Setup (P0)

#### 1. Database Backup Strategy

**Automated Backup Configuration** (Cloud RDS):
- **Frequency**: Daily full backup + continuous binlog backup
- **Retention**: 7 days (minimum), 30 days (recommended)
- **Storage**: Separate region for disaster recovery
- **Testing**: Monthly restore drill to verify backup integrity

**Manual Backup Script** (for self-hosted):
```bash
#!/bin/bash
# backup-mysql.sh

BACKUP_DIR="/var/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="youyu"

# Create backup directory
mkdir -p $BACKUP_DIR

# Dump database
mysqldump -u root -p$MYSQL_ROOT_PASSWORD \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  $DB_NAME | gzip > $BACKUP_DIR/youyu_$DATE.sql.gz

# Delete backups older than 30 days
find $BACKUP_DIR -name "youyu_*.sql.gz" -mtime +30 -delete

echo "Backup completed: youyu_$DATE.sql.gz"
```

**Cron Job**:
```cron
0 2 * * * /usr/local/bin/backup-mysql.sh >> /var/log/mysql-backup.log 2>&1
```

**Verification**:
- [ ] Backup script runs successfully
- [ ] Restore procedure documented and tested
- [ ] Backup monitoring alerts configured

#### 2. Logging Configuration

**Production Logging** (`application-prod.yml`):

```yaml
logging:
  level:
    root: INFO
    com.youyu.backend: INFO
    org.springframework.web: WARN
    org.springframework.security: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/youyu/application.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 10GB
```

**Structured Logging** (add Logback JSON encoder):

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON" />
    </root>
</configuration>
```

**Log Aggregation** (optional but recommended):
- **ELK Stack**: Elasticsearch + Logstash + Kibana
- **Loki + Grafana**: Lightweight alternative
- **Cloud Provider**: Aliyun SLS, Tencent Cloud CLS

**Sensitive Data Masking**:
```java
// Mask passwords in logs
log.info("User login attempt: username={}, password=***", username);

// Never log full JWT tokens
log.debug("Token validation: userId={}", userId);  // Not the full token
```

#### 3. Monitoring and Alerting

**Health Check Endpoint** (already provided by Spring Boot Actuator):

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
```

**Prometheus Metrics** (see Performance guide for full setup):
- Request rate and latency
- Error rate (5xx responses)
- Database connection pool usage
- JVM heap and GC metrics

**Alert Rules** (critical alerts only):
```yaml
groups:
  - name: youyu_critical
    rules:
      - alert: ServiceDown
        expr: up{job="youyu-backend"} == 0
        for: 1m
        annotations:
          summary: "Youyu backend is down"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        annotations:
          summary: "Error rate >10% for 5 minutes"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        annotations:
          summary: "Database connection pool >90% utilized"
```

**Notification Channels**:
- Email (for non-urgent alerts)
- SMS (for critical alerts: service down, database failure)
- DingTalk/WeChat Work webhook (for team notifications)

### Deployment Process (P1)

#### 1. Containerization

**Backend Dockerfile**:

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR file
COPY target/youyu-backend-*.jar app.jar

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Frontend Dockerfile**:

```dockerfile
# frontend/Dockerfile
FROM node:22-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production image
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

**Nginx Configuration** (`frontend/nginx.conf`):

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy (if backend on same host)
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**Docker Compose** (`docker-compose.yml`):

```yaml
version: '3.8'

services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=db
      - DB_PORT=3306
      - DB_NAME=youyu
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - APP_JWT_SECRET=${APP_JWT_SECRET}
    depends_on:
      - db
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: unless-stopped

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=youyu
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql-data:
```

**Build and Deploy**:

```bash
# Build images
docker-compose build

# Start services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

#### 2. CI/CD Pipeline Enhancement

**Current**: GitHub Actions runs tests only (`.github/workflows/ci.yml`).

**Enhancement**: Add deployment stage for staging/production.

**Deployment Workflow** (`.github/workflows/deploy.yml`):

```yaml
name: Deploy to Production

on:
  push:
    branches: [master]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build backend
        run: |
          cd backend
          ./mvnw clean package -DskipTests

      - name: Build frontend
        run: |
          cd frontend
          npm ci
          npm run build

      - name: Build Docker images
        run: |
          docker build -t youyu-backend:${{ github.sha }} ./backend
          docker build -t youyu-frontend:${{ github.sha }} ./frontend

      - name: Push to registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push youyu-backend:${{ github.sha }}
          docker push youyu-frontend:${{ github.sha }}

      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/youyu
            docker-compose pull
            docker-compose up -d
            docker-compose logs -f --tail=100
```

**GitHub Secrets to Configure**:
- `DOCKER_USERNAME`, `DOCKER_PASSWORD` (Docker Hub or private registry)
- `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` (production server)
- `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` (passed to containers)

#### 3. Zero-Downtime Deployment

**Strategy**: Rolling update with health checks.

**Docker Compose** (with multiple replicas):

```yaml
services:
  backend:
    deploy:
      replicas: 2
      update_config:
        parallelism: 1
        delay: 10s
        order: start-first
      restart_policy:
        condition: on-failure
```

**Kubernetes** (for larger scale):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: youyu-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: backend
        image: youyu-backend:latest
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
```

### Operational Procedures

#### 1. Deployment Runbook

**Pre-Deployment**:
1. Run full test suite locally: `mvnw test && npm test`
2. Review changes: `git log master..HEAD --oneline`
3. Check for breaking changes in API contracts
4. Notify team in group chat: "Deploying to production in 10 minutes"

**Deployment**:
1. Trigger deployment workflow (manual or automatic on merge to master)
2. Monitor deployment logs: `docker-compose logs -f`
3. Verify health check: `curl https://youyu.example.com/actuator/health`
4. Smoke test critical paths: login, product list, order submission

**Post-Deployment**:
1. Monitor error rate for 15 minutes (Grafana dashboard)
2. Check application logs for exceptions
3. Verify database migrations applied (if any)
4. Update deployment log: date, version, deployer, issues

**Rollback Procedure** (if deployment fails):
```bash
# Revert to previous Docker image
docker-compose down
docker-compose up -d --force-recreate

# Or rollback git commit and redeploy
git revert HEAD
git push origin master
```

#### 2. Incident Response

**Severity Levels**:
- **P0 (Critical)**: Service completely down, data loss, security breach
- **P1 (High)**: Major feature broken, significant user impact
- **P2 (Medium)**: Minor feature broken, workaround available
- **P3 (Low)**: Cosmetic issue, no user impact

**Response Timeline**:
- P0: Acknowledge within 15 minutes, resolve within 2 hours
- P1: Acknowledge within 1 hour, resolve within 8 hours
- P2: Acknowledge within 4 hours, resolve within 2 days
- P3: Acknowledge within 1 day, resolve within 1 week

**Incident Checklist**:
1. **Detect**: Alert fires or user report
2. **Acknowledge**: Assign incident owner, notify team
3. **Investigate**: Check logs, metrics, recent deployments
4. **Mitigate**: Rollback, hotfix, or disable feature
5. **Resolve**: Deploy fix, verify resolution
6. **Document**: Post-mortem (what happened, why, how to prevent)

**Post-Mortem Template** (`docs/incidents/YYYY-MM-DD-incident-name.md`):
```markdown
# Incident: [Brief Description]

**Date**: YYYY-MM-DD
**Severity**: P0/P1/P2/P3
**Duration**: X hours
**Impact**: X users affected, Y transactions failed

## Timeline
- HH:MM - Alert fired
- HH:MM - Investigation started
- HH:MM - Root cause identified
- HH:MM - Mitigation deployed
- HH:MM - Incident resolved

## Root Cause
[Technical explanation]

## Resolution
[What was done to fix it]

## Action Items
- [ ] Prevent recurrence: [specific task]
- [ ] Improve detection: [monitoring/alerting improvement]
- [ ] Update runbook: [documentation update]
```

#### 3. Database Migration

**Schema Changes**:
- Always use `ALTER TABLE` with `IF NOT EXISTS` / `IF EXISTS` for idempotency
- Test migrations on staging database first
- Backup database before applying migrations
- Avoid destructive operations (DROP COLUMN, DROP TABLE) without data export

**Migration Script Template** (`backend/src/main/resources/db/migration/V2__add_user_consent_logs.sql`):

```sql
-- Migration: Add user consent logs table
-- Date: 2026-05-25
-- Author: [Your Name]

CREATE TABLE IF NOT EXISTS user_consent_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  consent_type VARCHAR(50) NOT NULL,
  consented BOOLEAN NOT NULL,
  ip_address VARCHAR(45),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_consent (user_id, consent_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rollback script (keep in comments for reference)
-- DROP TABLE IF EXISTS user_consent_logs;
```

**Applying Migrations**:
```bash
# Connect to production database
mysql -h prod-db.internal -u admin -p youyu

# Apply migration
source db/migration/V2__add_user_consent_logs.sql

# Verify
SHOW TABLES LIKE 'user_consent_logs';
DESCRIBE user_consent_logs;
```

**Flyway Integration** (optional, for automated migrations):

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Compliance and Regulatory

**ICP Filing** (ICP备案, required for China-hosted servers):
- Register with MIIT (工信部) via cloud provider portal
- Provide: business license, domain ownership, server details
- Timeline: 20-30 business days
- Display ICP number in website footer

**Public Security Bureau Filing** (公安备案, required in some provinces):
- Register within 30 days of ICP approval
- Provide: same documents as ICP + legal representative ID
- Display filing number in website footer

**Data Residency**:
- User data must remain in China (if serving Chinese users)
- Use China region for cloud services (Aliyun China, Tencent Cloud China)
- Cross-border data transfer requires approval (if applicable)

### Cost Optimization

**Resource Right-Sizing**:
- Start small (2 core, 4GB server)
- Monitor CPU/memory usage for 1 week
- Scale up only when utilization >70% sustained

**Reserved Instances**:
- Commit to 1-year reserved instances for 30-40% discount
- Only after traffic patterns stabilize (3-6 months)

**Auto-Scaling** (for variable load):
- Scale backend replicas based on CPU usage
- Scale database read replicas based on connection count
- Set max limits to prevent runaway costs

**Cost Monitoring**:
- Set billing alerts (e.g., alert if monthly cost >¥1000)
- Review cost breakdown monthly
- Identify and eliminate unused resources (old snapshots, orphaned volumes)

## Document Maintenance

| Section | Update Trigger |
|---------|----------------|
| Security checklist | New vulnerability discovered, compliance requirement added |
| Deployment process | CI/CD pipeline changes, new environment added |
| Monitoring alerts | New critical metric identified, false positive rate too high |
| Incident runbook | Post-mortem action item, new failure mode discovered |
| Cost estimation | Pricing changes, architecture changes |

**Review Cadence**: Quarterly review of all operational procedures.

## References

- [Spring Boot Production-Ready Features](https://docs.spring.io/spring-boot/reference/actuator/index.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Twelve-Factor App](https://12factor.net/)
- [Site Reliability Engineering (SRE) Book](https://sre.google/sre-book/table-of-contents/)
- [China ICP Filing Guide](https://www.alibabacloud.com/help/en/icp-filing/)
