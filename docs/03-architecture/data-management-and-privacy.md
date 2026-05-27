# Data Management and Privacy Compliance

## Data Storage Architecture

### Current Implementation

**Primary Storage**: MySQL 8+ database (`localhost:3306/youyu`)

- All user data (accounts, orders, reviews, addresses, preferences) stored in MySQL
- Password hashing: bcrypt via `spring-security-crypto` (`PasswordConfig.java`)
- Media storage: BLOB fields in `product_media` table (interim solution)
- Session management: JWT tokens (jjwt 0.12.6)

**Client-Side Storage** (localStorage):

- JWT authentication token (session management)
- Search history (max 8 entries, non-sensitive)
- UI state (sidebar collapsed, theme preferences)

### Browser Storage Limitations

Web applications cannot rely on client-side storage for business-critical data:

- **Capacity**: localStorage/IndexedDB limited to 5-10MB per origin
- **Persistence**: User can clear browser data at any time
- **Synchronization**: No cross-device sync without server backend
- **Security**: Accessible to JavaScript, vulnerable to XSS attacks

**Guideline**: Only store ephemeral UI state and non-sensitive preferences client-side. All business data must persist server-side.

### Storage Cost Analysis

**Current Phase** (Campus MVP, <1000 DAU):
- Single MySQL instance sufficient
- Estimated storage: <10GB (1 year operation)
- Cost: Low (self-hosted or basic cloud RDS)

**Growth Phase** (Multi-campus, 1000-5000 DAU):
- Database backup strategy required
- Media migration to object storage (OSS) recommended
- Estimated cost: ¥500-800/month (cloud services)

**Scale Phase** (Regional expansion, >5000 DAU):
- Read-write separation
- Distributed session management (Redis)
- CDN for static assets
- Estimated cost: ¥2000-5000/month

### Cost Reduction Strategies

1. **Managed Database Services**: Use cloud provider RDS (阿里云 RDS, 腾讯云 MySQL) to reduce operational overhead
2. **Automated Backup**: Daily incremental + weekly full backup (retention: 30 days)
3. **Object Storage Migration**: Move `product_media` BLOB data to OSS (Aliyun OSS, Tencent COS)
4. **Data Lifecycle Policies**:
   - Archive orders older than 2 years
   - Purge search logs after 90 days
   - Soft-delete user accounts (retain 30 days for recovery)

## Privacy and Compliance

### Legal Framework

**Applicable Laws**:
- 《中华人民共和国个人信息保护法》(Personal Information Protection Law, PIPL)
- 《中华人民共和国网络安全法》(Cybersecurity Law)
- 《中华人民共和国数据安全法》(Data Security Law)

**Core Principles**:
1. **Informed Consent**: Users must explicitly agree before data collection
2. **Purpose Limitation**: Collect only data necessary for stated purposes
3. **Security Safeguards**: Implement encryption, access control, audit logging
4. **User Rights**: Provide mechanisms for data access, correction, deletion

### Required Legal Documents

#### 1. Privacy Policy (隐私政策)

**Must Include**:
- **Data Collection Scope**: Name, student ID, phone, address, order history, browsing behavior
- **Usage Purposes**: Transaction fulfillment, recommendation algorithms, credit scoring, fraud prevention
- **Third-Party Sharing**: Payment processors (mock in MVP, real in production), logistics partners
- **Data Retention**: Active accounts (indefinite), deleted accounts (30 days), order records (3 years per tax law)
- **User Rights**: Access, correction, deletion, complaint procedures
- **Contact Information**: Data protection officer email, customer service hotline

**Template Resources**:
- [iubenda Privacy Policy Generator](https://www.iubenda.com/en/privacy-policy-generator)
- [Termly Privacy Policy Generator](https://termly.io/products/privacy-policy-generator/)
- Aliyun/Tencent Cloud compliance document templates

#### 2. User Agreement (用户协议)

**Must Include**:
- **Service Scope**: Campus marketplace platform (intermediary role)
- **User Obligations**: Provide truthful information, no prohibited items (weapons, drugs, counterfeit goods)
- **Platform Responsibilities**: Facilitate transactions, not liable for product quality (seller responsibility)
- **Dispute Resolution**: Mediation process, arbitration clause, jurisdiction
- **Intellectual Property**: User-generated content licensing, trademark usage
- **Termination Conditions**: Account suspension/deletion criteria

**Reference**: Taobao, JD.com user agreements (adapt for campus context)

#### 3. Cookie Policy (Cookie 政策)

**Cookie Classification**:
- **Strictly Necessary**: JWT token (no consent required, essential for service)
- **Functional**: Search history, UI preferences (requires consent)
- **Analytics**: If integrating Baidu Analytics or similar (requires consent)
- **Advertising**: Not applicable in current scope

**Implementation**: See Technical Implementation section below

### User Rights Implementation

**Required Backend Endpoints**:

```java
// UserController.java additions
@PostMapping("/api/user/data-export")
@LoginRequired
public ApiResponse<DataExportResponse> exportPersonalData() {
    // Generate JSON export of all user data
    // Include: profile, addresses, orders, reviews, preferences
    // Exclude: other users' data, system internals
}

@DeleteMapping("/api/user/account")
@LoginRequired
public ApiResponse<Void> deleteAccount(@RequestBody AccountDeletionRequest request) {
    // Soft delete: set users.deleted_at = NOW()
    // Anonymize: clear PII fields (name, phone, email)
    // Retain: order records (legal requirement), anonymized
    // Schedule: hard delete after 30 days (cron job)
}

@GetMapping("/api/user/consent-log")
@LoginRequired
public ApiResponse<List<ConsentRecord>> getConsentHistory() {
    // Return user's consent history (privacy policy, cookie preferences)
}
```

**Database Schema Addition**:

```sql
CREATE TABLE user_consent_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  consent_type VARCHAR(50) NOT NULL COMMENT 'privacy_policy, cookie_functional, cookie_analytics',
  consented BOOLEAN NOT NULL,
  ip_address VARCHAR(45),
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_consent (user_id, consent_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User consent audit log';

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL COMMENT 'Soft delete timestamp';
ALTER TABLE users ADD INDEX idx_deleted_at (deleted_at);
```

### Technical Implementation

#### Frontend Changes

**New Views** (`frontend/src/views/legal/`):
- `PrivacyPolicy.vue` — Full privacy policy text
- `UserAgreement.vue` — Full user agreement text
- `CookiePolicy.vue` — Cookie usage explanation

**New Components** (`frontend/src/components/common/`):
- `CookieConsent.vue` — Cookie consent banner (first visit)
- `LegalFooter.vue` — Footer links to legal documents

**Registration Flow Update** (`frontend/src/views/auth/RegisterView.vue`):

```vue
<el-checkbox v-model="agreedToTerms" required>
  我已阅读并同意
  <router-link to="/legal/user-agreement" target="_blank">《用户协议》</router-link>
  和
  <router-link to="/legal/privacy-policy" target="_blank">《隐私政策》</router-link>
</el-checkbox>
```

**Cookie Consent Implementation**:

```javascript
// stores/consent.js
export const useConsentStore = defineStore('consent', () => {
  const cookieConsent = ref(localStorage.getItem('cookie_consent'))

  function acceptCookies(categories) {
    const consent = {
      necessary: true, // Always true
      functional: categories.includes('functional'),
      analytics: categories.includes('analytics'),
      timestamp: new Date().toISOString()
    }
    localStorage.setItem('cookie_consent', JSON.stringify(consent))
    cookieConsent.value = consent

    // Send to backend for audit log
    api.user.logConsent({ type: 'cookie_functional', consented: consent.functional })
  }

  function clearNonEssentialData() {
    if (!cookieConsent.value?.functional) {
      localStorage.removeItem('search_history')
      localStorage.removeItem('ui_preferences')
    }
  }

  return { cookieConsent, acceptCookies, clearNonEssentialData }
})
```

**Recommended Library**: [cookie-consent-banner](https://github.com/porscheofficial/cookie-consent-banner) (open-source, GDPR-compliant)

#### Backend Changes

**New API Module** (`controller/user/ConsentController.java`):

```java
@RestController
@RequestMapping("/api/user/consent")
public class ConsentController {

    @PostMapping("/log")
    @LoginRequired
    public ApiResponse<Void> logConsent(@RequestBody ConsentLogRequest request) {
        // Insert into user_consent_logs
        // Fields: user_id (from AuthContext), consent_type, consented, ip_address, user_agent
    }

    @GetMapping("/history")
    @LoginRequired
    public ApiResponse<List<ConsentRecord>> getConsentHistory() {
        // Query user_consent_logs for current user
    }
}
```

**Registration Flow Update** (`controller/auth/AuthController.java`):

```java
@PostMapping("/register")
public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
    // Validate agreedToTerms = true (reject if false)
    // Create user account
    // Log initial consent: privacy_policy = true, timestamp = NOW()
    // Return JWT token
}
```

### Compliance Checklist

**Pre-Launch (P0)**:
- [ ] Privacy Policy page created and linked in footer
- [ ] User Agreement page created and linked in footer
- [ ] Registration requires explicit consent checkbox
- [ ] Cookie consent banner on first visit
- [ ] HTTPS enabled (Let's Encrypt or cloud provider certificate)
- [ ] Password hashing verified (bcrypt, already implemented ✅)
- [ ] SQL injection prevention verified (parameterized queries, already implemented ✅)

**Post-Launch (P1, within 1 month)**:
- [ ] Data export endpoint implemented (`/api/user/data-export`)
- [ ] Account deletion endpoint implemented (`/api/user/account`)
- [ ] Consent audit log table created and populated
- [ ] Backup and recovery procedures documented and tested

**Regulatory (P2, if operating in mainland China)**:
- [ ] ICP filing (ICP备案) with MIIT (工信部) — required for China-hosted servers
- [ ] Public Security Bureau filing (公安备案) — required in some provinces
- [ ] Cybersecurity Level Protection (等保) assessment — required if handling sensitive data at scale

### Security Best Practices

**Already Implemented** ✅:
- Password hashing (bcrypt)
- JWT authentication with configurable expiration
- SQL injection prevention (JDBC parameterized queries)
- XSS prevention (Vue.js auto-escaping, Element Plus sanitization)

**To Implement** (see Performance and Scalability guide):
- HTTPS enforcement (redirect HTTP → HTTPS)
- Rate limiting (prevent brute-force attacks)
- CSRF protection (Spring Security CSRF tokens)
- Input validation (JSR-303 Bean Validation)
- Audit logging (track sensitive operations: login, data export, account deletion)

### Data Breach Response Plan

**Preparation**:
1. Designate Data Protection Officer (DPO) or responsible person
2. Document incident response procedures
3. Maintain contact list (legal counsel, cloud provider support, regulatory authority)

**Response Steps** (if breach occurs):
1. **Contain** (0-24h): Isolate affected systems, revoke compromised credentials
2. **Assess** (24-72h): Determine scope (how many users, what data types)
3. **Notify** (72h): Report to regulatory authority (Cyberspace Administration of China) if >1000 users affected
4. **Remediate**: Patch vulnerability, force password resets, enhance monitoring
5. **Document**: Post-mortem report, update security policies

**Legal Obligation**: PIPL Article 57 requires notification within 72 hours if breach affects user rights.

## Document Maintenance

| Document | Owner | Update Trigger |
|----------|-------|----------------|
| Privacy Policy | Legal/Product | Data collection scope changes, new third-party integrations |
| User Agreement | Legal/Product | Service terms change, new features with legal implications |
| Cookie Policy | Engineering | New analytics tools, tracking mechanisms added |
| Consent logs schema | Engineering | New consent types required (e.g., marketing emails) |
| Data export format | Engineering | New data tables added to user profile |

**Version Control**: Legal documents should include "Last Updated" date and version number. Archive previous versions in `docs/legal-archive/`.

## References

- [Personal Information Protection Law (PIPL) Full Text](http://www.npc.gov.cn/npc/c30834/202108/a8c4e3672c74491a80b53a172bb753fe.shtml)
- [GDPR Compliance Checklist](https://gdpr.eu/checklist/) (reference for best practices, not directly applicable)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/) (security vulnerabilities to avoid)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
