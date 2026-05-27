# Performance and Scalability Guide

## Current Performance Baseline

### System Capacity (As-Built)

**Backend**:
- Spring Boot 3.3 with embedded Tomcat
- Default thread pool: 200 max threads
- Database connection pool: HikariCP default (10 connections)
- No caching layer
- No rate limiting
- Synchronous request processing

**Frontend**:
- Vue 3 SPA with code splitting
- Vite build with tree-shaking
- No CDN, assets served from origin
- Client-side routing (no SSR)

**Database**:
- MySQL 8+ single instance
- Indexes on primary keys and foreign keys (defined in `schema.sql`)
- No query optimization or slow query monitoring
- No read replicas

**Estimated Capacity** (single server, no optimization):
- Concurrent users: 50-100
- Requests per second: 20-50 RPS
- Database connections: 10 (HikariCP default)
- Response time: 200-500ms (uncached queries)

### Known Bottlenecks

1. **Recommendation Engine** (`/api/recommend/home`):
   - Calculates popularity scores on every request
   - Queries multiple tables (products, orders, reviews)
   - No result caching
   - **Impact**: 500-1000ms response time under load

2. **Product Media Storage** (`product_media` table):
   - Images stored as BLOB in MySQL
   - Fetched with every product query
   - Increases database load and network transfer
   - **Impact**: Slow product list rendering, high memory usage

3. **Search Logging** (`SearchController.search()`):
   - Synchronous INSERT into `search_logs` table
   - Blocks request until write completes
   - **Impact**: 50-100ms added latency per search

4. **No Connection Pooling Configuration**:
   - Default 10 connections insufficient for >50 concurrent users
   - Connection exhaustion causes request queuing
   - **Impact**: Timeout errors under moderate load

5. **No HTTP Caching Headers**:
   - Static assets (JS, CSS, images) not cached by browser
   - API responses not cacheable (no `Cache-Control` headers)
   - **Impact**: Unnecessary bandwidth, slower page loads

## Performance Optimization Roadmap

### Phase 1: Pre-Launch Essentials (P0)

**Goal**: Support 100-200 concurrent users with <500ms p95 latency

#### 1.1 Database Connection Pool Configuration

**File**: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50        # Up from default 10
      minimum-idle: 10             # Keep 10 connections warm
      connection-timeout: 20000    # 20 seconds
      idle-timeout: 300000         # 5 minutes
      max-lifetime: 1200000        # 20 minutes
      leak-detection-threshold: 60000  # Warn if connection held >60s
```

**Rationale**: 50 connections supports ~200 concurrent users (assuming 4:1 user-to-connection ratio).

#### 1.2 Redis Caching Layer

**Add Dependency** (`backend/pom.xml`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**Configuration** (`application.yml`):

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

**Cache Strategy**:

| Data Type | TTL | Invalidation Trigger |
|-----------|-----|---------------------|
| Hot search keywords | 1 hour | Admin updates `search_governance_rules` |
| Recommendation results | 15 minutes | New order/review created |
| Product details | 5 minutes | Product updated/deleted |
| Shop profiles | 10 minutes | Shop updated |
| Category list | 1 day | Category added/removed |

**Implementation Example** (`RecommendServiceImpl.java`):

```java
@Service
public class RecommendServiceImpl implements RecommendService {

    @Cacheable(value = "recommend:home", key = "#userId", unless = "#result == null")
    @Override
    public List<Map<String, Object>> getHomeRecommendations(Long userId) {
        // Existing logic unchanged
        // Result automatically cached for 15 minutes
    }

    @CacheEvict(value = "recommend:home", allEntries = true)
    public void invalidateRecommendCache() {
        // Called when new order/review created
    }
}
```

**Expected Impact**:
- Recommendation endpoint: 500ms → 50ms (10x improvement)
- Hot search keywords: 100ms → 10ms
- Product detail page: 300ms → 100ms

#### 1.3 Async Search Logging

**Current** (synchronous):

```java
// SearchController.java
searchLogMapper.insert(searchLog);  // Blocks request
return ApiResponse.success(results);
```

**Optimized** (async with `@Async`):

```java
@Service
public class SearchLogService {

    @Async("searchLogExecutor")
    public void logSearchAsync(SearchLog log) {
        searchLogMapper.insert(log);
    }
}

// SearchController.java
searchLogService.logSearchAsync(searchLog);  // Non-blocking
return ApiResponse.success(results);
```

**Thread Pool Configuration** (`AsyncConfig.java`):

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "searchLogExecutor")
    public Executor searchLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("search-log-");
        executor.initialize();
        return executor;
    }
}
```

**Expected Impact**: Search latency reduced by 50-100ms.

#### 1.4 HTTP Caching Headers

**Static Assets** (Vite build, `frontend/vite.config.js`):

```javascript
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        assetFileNames: 'assets/[name].[hash].[ext]',  // Content-hash filenames
        chunkFileNames: 'assets/[name].[hash].js',
        entryFileNames: 'assets/[name].[hash].js'
      }
    }
  }
})
```

**Backend Response Headers** (`WebMvcConfig.java`):

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CacheControlInterceptor());
    }
}

// Add Cache-Control headers to API responses
public class CacheControlInterceptor implements HandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                          Object handler, ModelAndView modelAndView) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/categories") || path.startsWith("/api/search/hot")) {
            response.setHeader("Cache-Control", "public, max-age=3600");  // 1 hour
        }
    }
}
```

**Expected Impact**: 30-50% reduction in bandwidth, faster repeat page loads.

#### 1.5 Rate Limiting

**Add Dependency** (`pom.xml`):

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

**Implementation** (`RateLimitFilter.java`):

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String key = getClientKey(request);  // IP address or user ID
        Bucket bucket = cache.computeIfAbsent(key, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);  // Too Many Requests
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

**Rate Limits**:
- Anonymous users: 100 requests/minute per IP
- Authenticated users: 300 requests/minute per user ID
- Admin endpoints: 1000 requests/minute

**Expected Impact**: Prevent abuse, protect against DDoS, ensure fair resource allocation.

### Phase 2: Post-Launch Optimization (P1, within 1 month)

**Goal**: Support 500-1000 concurrent users, <300ms p95 latency

#### 2.1 Object Storage Migration (OSS)

**Problem**: `product_media` BLOB storage increases database size and slows queries.

**Solution**: Migrate to Aliyun OSS or Tencent COS.

**Implementation**:

1. Add OSS SDK dependency
2. Create `MediaStorageService` interface with `uploadImage()`, `getImageUrl()` methods
3. Update `ProductController.createProduct()` to upload images to OSS
4. Store OSS URL in `product_media.media_url` (change column type to VARCHAR(512))
5. Migrate existing BLOB data to OSS (one-time script)

**Schema Change**:

```sql
ALTER TABLE product_media
  ADD COLUMN media_url VARCHAR(512) COMMENT 'OSS object URL',
  MODIFY COLUMN media_data LONGBLOB NULL COMMENT 'Deprecated, use media_url';
```

**Expected Impact**:
- Database size reduced by 60-80%
- Product query latency: 300ms → 100ms
- Image load time: 500ms → 200ms (OSS CDN acceleration)

#### 2.2 Database Query Optimization

**Add Indexes** (based on slow query log analysis):

```sql
-- Recommendation queries
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

-- Product search
CREATE INDEX idx_products_category_status ON products(category_id, status);
CREATE INDEX idx_products_seller_status ON products(seller_id, status);

-- Order queries
CREATE INDEX idx_orders_buyer_status ON orders(buyer_id, status);
CREATE INDEX idx_orders_seller_status ON orders(seller_id, status);
```

**Enable Slow Query Log** (`my.cnf`):

```ini
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 1  # Log queries >1 second
```

**Expected Impact**: 20-30% reduction in query latency for complex joins.

#### 2.3 Frontend Performance

**Code Splitting** (already implemented via Vite, verify):

```javascript
// router/index.js
const routes = [
  {
    path: '/app/product/:id',
    component: () => import('@/views/app/ProductDetailView.vue')  // Lazy load
  }
]
```

**Image Lazy Loading** (add to product cards):

```vue
<template>
  <img :src="product.imageUrl" loading="lazy" alt="product image" />
</template>
```

**Bundle Size Analysis**:

```bash
npm run build -- --mode analyze
```

**Target**: Main bundle <500KB gzipped, route chunks <200KB each.

#### 2.4 Monitoring and Alerting

**Add Spring Boot Actuator** (`pom.xml`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Configuration** (`application.yml`):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Metrics to Monitor**:
- Request rate (RPS)
- Response time (p50, p95, p99)
- Error rate (5xx responses)
- Database connection pool usage
- Cache hit rate
- JVM heap usage

**Alerting Rules** (Prometheus):

```yaml
groups:
  - name: youyu_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        annotations:
          summary: "Error rate >5% for 5 minutes"

      - alert: HighLatency
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1
        annotations:
          summary: "p95 latency >1 second"
```

**Visualization**: Grafana dashboard with panels for RPS, latency, error rate, database metrics.

### Phase 3: Scale-Out Architecture (P2, 3-6 months)

**Goal**: Support 5000+ concurrent users, multi-region deployment

#### 3.1 Database Read-Write Separation

**Setup**:
- 1 primary (write) + 2 replicas (read)
- Spring Boot `@Transactional(readOnly = true)` routes to replicas
- Replication lag monitoring (<1 second acceptable)

**Configuration** (`application.yml`):

```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:mysql://primary.db.internal:3306/youyu
    replica:
      jdbc-url: jdbc:mysql://replica.db.internal:3306/youyu
```

**Expected Impact**: 3x read capacity, reduced primary database load.

#### 3.2 Distributed Session Management

**Problem**: JWT tokens stored in localStorage, no server-side session invalidation.

**Solution**: Redis-backed session store with token blacklist.

**Implementation**:

```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: youyu:session
```

**Benefits**:
- Instant logout (add token to blacklist)
- Cross-device session management
- Horizontal scaling (stateless app servers)

#### 3.3 Message Queue for Async Processing

**Use Cases**:
- Search log writes
- Email notifications (order confirmation, review reminders)
- Analytics event processing

**Technology**: RabbitMQ or Kafka

**Example** (order confirmation email):

```java
// OrderServiceImpl.java
@Transactional
public void submitOrder(OrderSubmitRequest request) {
    // Create order (synchronous)
    Order order = createOrder(request);

    // Send email (asynchronous via message queue)
    messagingService.sendOrderConfirmation(order.getId());

    return order;
}
```

**Expected Impact**: Reduced request latency, improved reliability (retry failed jobs).

#### 3.4 CDN for Static Assets

**Setup**:
- Aliyun CDN or Cloudflare
- Origin: OSS bucket (images) + app server (JS/CSS)
- Cache TTL: 1 year for versioned assets, 1 hour for HTML

**Expected Impact**:
- 50-80% reduction in origin bandwidth
- 200-500ms faster asset load time (edge caching)
- Global availability (multi-region CDN nodes)

## Performance Testing Strategy

### Load Testing Tools

**Recommended**: Apache JMeter or Gatling

**Test Scenarios**:

1. **Baseline Load** (100 concurrent users, 10 minutes):
   - 40% browse products
   - 30% search
   - 20% view product details
   - 10% add to cart

2. **Peak Load** (500 concurrent users, 5 minutes):
   - Simulate flash sale scenario
   - Measure response time degradation

3. **Stress Test** (ramp up to failure):
   - Identify breaking point (max RPS before errors)
   - Measure recovery time after load drops

**Success Criteria**:
- p95 latency <500ms under baseline load
- Error rate <1% under peak load
- No memory leaks (heap usage stable over 1 hour)
- Database connection pool <80% utilization

### Continuous Performance Monitoring

**Pre-Deployment Checks**:
- Run load test on staging environment
- Compare metrics to baseline (fail if p95 latency increases >20%)
- Check bundle size (fail if main bundle increases >10%)

**Production Monitoring**:
- Real User Monitoring (RUM) via frontend instrumentation
- Synthetic monitoring (health check every 5 minutes)
- Slow query log analysis (weekly review)

## Scalability Checklist

**Before Launch**:
- [ ] Database connection pool configured (50 connections)
- [ ] Redis caching enabled for hot data
- [ ] Async search logging implemented
- [ ] HTTP caching headers configured
- [ ] Rate limiting enabled
- [ ] Load test passed (100 concurrent users, <500ms p95)

**Within 1 Month**:
- [ ] Object storage migration completed
- [ ] Database indexes optimized
- [ ] Monitoring and alerting operational
- [ ] Frontend bundle size optimized (<500KB)

**Within 3-6 Months** (if scaling to multi-campus):
- [ ] Read-write separation implemented
- [ ] Distributed session management (Redis)
- [ ] Message queue for async processing
- [ ] CDN for static assets

## Cost Estimation

**Phase 1** (100-200 users):
- Cloud server (2 core, 4GB): ¥200/month
- Cloud database (MySQL basic): ¥150/month
- Redis (1GB): ¥50/month
- **Total**: ¥400/month

**Phase 2** (500-1000 users):
- Cloud server (4 core, 8GB): ¥400/month
- Cloud database (MySQL standard): ¥300/month
- Redis (4GB): ¥150/month
- Object storage (100GB): ¥50/month
- CDN (1TB traffic): ¥100/month
- **Total**: ¥1000/month

**Phase 3** (5000+ users):
- Load balancer: ¥200/month
- App servers (2x 4 core, 8GB): ¥800/month
- Database (primary + 2 replicas): ¥1200/month
- Redis cluster (16GB): ¥600/month
- Object storage (500GB): ¥200/month
- CDN (5TB traffic): ¥500/month
- Message queue: ¥300/month
- **Total**: ¥3800/month

## References

- [Spring Boot Performance Tuning](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Redis Caching Best Practices](https://redis.io/docs/manual/patterns/caching/)
- [Vite Build Optimization](https://vitejs.dev/guide/build.html)
- [Web Performance Working Group](https://www.w3.org/webperf/)
