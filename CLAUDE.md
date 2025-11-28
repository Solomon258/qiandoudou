# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Qiandoudou (钱兜兜)** is a full-stack gamified personal finance application combining wallet management, AI-driven savings challenges, companion interactions, and social networking. The project uses a WeChat mini-program frontend and Spring Boot backend with event-driven architecture.

- **Frontend:** WeChat Mini Program (JavaScript, WXML, WXSS)
- **Backend:** Spring Boot 2.7.18, Java 8+, MyBatis Plus
- **Database:** MySQL 8.0+
- **Architecture:** Microservice-oriented with event-driven async processing

## Key Development Rules

**WeChat Mini Program Frontend:**
1. Code must follow the official WeChat mini-program specification (https://developers.weixin.qq.com/miniprogram/dev/framework/)
2. Use official WeChat APIs (`wx.*`) only; do not use browser APIs
3. All API calls must follow WeChat security standards
4. Use WeChat Developer Tools for development and testing

**Backend Development:**
1. Code is developed locally and compiled/executed in IntelliJ IDEA
2. Maven is the build tool; compile and run locally
3. SQL scripts must be provided separately for manual execution

## Quick Start

### Backend

#### Build
```bash
mvn clean package -DskipTests      # Full production build
mvn clean compile                   # Quick compile without packaging
```

#### Run
```bash
# Option 1: Maven Spring Boot plugin
mvn spring-boot:run

# Option 2: Execute packaged JAR
java -jar target/qiandoudou-backend-1.0.0.jar
```

**Server Details:**
- Port: 8080
- API Context Path: `/api`
- Configuration: `qiandoudou-backend/src/main/resources/application.yml`

#### Quick Testing
- Use TestController endpoints for API validation
- Check logs with `logging.level.com.qiandoudou: debug` in application.yml
- Frontend will connect to `http://localhost:8080/api` during local development

### Frontend

**Development Environment:**
- Use WeChat Developer Tools (official IDE)
- Run `npm install` to install dependencies (axios, js-cookie, eslint)
- Development URL is configured in `app.js` as `http://localhost:8080/api`

**Linting:**
```bash
npm run lint    # Run ESLint validation
```

## Architecture

### Monolithic Codebase
The repository contains two independent subprojects:
- `qiandoudou-backend/` - Spring Boot REST API service
- `qiandoudou-frontend/` - WeChat mini-program frontend

Both should be developed, compiled, and deployed independently.

### Backend Architecture

**Domain Layers:**

1. **User Management & Authentication**
   - JWT-based tokens (24-hour expiration)
   - WeChat mini-program OAuth integration
   - Login history via `UserLoginLog`

2. **Wallet & Transactions**
   - 5 wallet types: Personal (1), Couple (2), Buddy (3), Dream Shopping (4), Dream Travel (5)
   - Transaction history with optional AI partner metadata
   - Public/private visibility, customizable backgrounds

3. **Savings Scripts (Stories)**
   - Story-driven savings goals with chapters
   - Categories: Recommended, Travel, Shopping, Learning, Fitness
   - Progress tracking with completion rewards

4. **AI Companion System**
   - Multiple companion types with personality/voice settings
   - Text-to-speech integration (ByteDance Volcano Engine)
   - Auto-comments on transactions via async events
   - Image-to-text OCR capability
   - Special support for couple wallet AI interactions

5. **Social & Dreams**
   - Follow/like/comment system
   - Dream savings (Shopping/Travel) with milestone tracking
   - Notification system with AI interaction messages
   - Share functionality for social discovery

**Code Organization:**
- **Controllers** (12 files) - REST endpoints, all return `Result<T>` wrapper
- **Services** (24 interfaces + 25 implementations) - Business logic
- **Mappers** (25 files) - MyBatis data access, XML in `src/main/resources/mapper/`
- **Entities** (26 files) - JPA/MyBatis entities with Lombok, soft deletion via `@TableLogic`
- **Config** - Spring configuration, async executors, event listeners
- **Event/Listener** - Event-driven async processing

**Key Patterns:**

| Pattern | Usage |
|---------|-------|
| Service-Oriented | Controllers → Services → Mappers, all services extend `IService<T>` |
| Event-Driven | `TransactionCreatedEvent` triggers async listeners for AI interactions |
| Soft Deletion | All entities use `deleted=0/1` (active/soft-deleted) |
| Pagination | Built-in via MyBatis Plus interceptor |
| ID Generation | `@TableId(type = IdType.ASSIGN_ID)` for all entities |

### Frontend Architecture

**Page Structure:**
Pages follow the feature-based organization:
- `pages/wallet-*` - Wallet management and transactions
- `pages/buddy-*` - AI buddy/companion features
- `pages/dream-*` - Savings dream features
- `pages/social/*` - Social features (profiles, follows, etc.)

**Global Setup:**
- `app.js` - Global app lifecycle, login status checking, token expiry checks
- `app.json` - Page registration and WeChat configuration
- `utils/api.js` - Centralized HTTP client with token/auth handling
- Global data includes event bus for component communication

**API Integration:**
- All requests go through `utils/api.js` which automatically adds JWT token via Authorization header
- Base URL: `http://localhost:8080/api` (local dev) or `https://heartllo.cn/api` (production)
- Response format: `{ code: 200, message: "...", data: {...}, timestamp: ... }`
- 401 responses trigger auto-logout and redirect to login page

**Key Features:**
- Local storage for token, user info via `wx.getStorageSync`/`wx.setStorageSync`
- Event bus for parent-child component communication
- Automatic token refresh and expiry handling

## External Integrations

**Cloud Services:**
- **Aliyun OSS** - Image/audio/video storage (bucket: qiandoudou)
- **ByteDance Volcano Engine** - TTS and image-to-text OCR
- **OpenAI API** - Text generation (currently commented out)
- **WeChat Mini Program** - OAuth login (appid: `wx8372421078267cd9`)

**Configuration:**
All credentials and endpoints are in `qiandoudou-backend/src/main/resources/application.yml`:
```yaml
aliyun.oss.*              # Aliyun credentials
byte-dance.*              # ByteDance API keys
spring.datasource.*       # Database connection
jwt.secret/expiration     # Token settings
```

## Database

**Key Tables:**
- `users` - User accounts with profiles
- `wallets` - Multiple wallet types per user with balances
- `transactions` - Financial transactions, optionally linked to AI partners
- `scripts` - Savings challenge definitions
- `ai_partners` - AI companion definitions
- `buddy_groups` - Buddy circle social groups
- `user_follows` - Follow relationships
- `post_likes`, `post_comments` - Social interactions
- `dream_wallets` - Dream savings with milestones
- `notifications` - User notifications

**Design Notes:**
- All tables use soft deletion (`deleted=0/1`)
- Underscore naming convention (auto-mapped to camelCase by MyBatis Plus)
- Auto-filled: `create_time`, `update_time`
- Timezone: Shanghai (UTC+8)
- Character set: utf8mb4
- Default database name: qiandoudou

## Common Workflows

### Adding a New API Feature

1. **Create entity** in `qiandoudou-backend/src/main/java/com/qiandoudou/entity/`
2. **Create mapper** in `qiandoudou-backend/src/main/java/com/qiandoudou/mapper/` and XML in `src/main/resources/mapper/`
3. **Create service interface** in `service/` and implementation in `service/impl/`
4. **Create controller** with `@RestController` and `/api` base path in `controller/`
5. **Add database migration** in `src/main/resources/sql/` (user will execute)
6. **Frontend integration** - Add API call in `utils/api.js` and use in page components

### Adding Frontend Pages

1. Create page directory in `qiandoudou-frontend/pages/page-name/`
2. Create four files: `page-name.js`, `page-name.wxml`, `page-name.wxss`, `page-name.json`
3. Register in `app.json` under `"pages"` array
4. Use `utils/api.js` for backend communication
5. Access app global data via `getApp().globalData`

### Async Processing (AI Interactions)

When a transaction is created:
1. `TransactionCreatedEvent` is published
2. `AiLoverInteractionEventListener` processes asynchronously
3. AI comment is generated and stored
4. Transaction enriched with AI metadata
5. Frontend receives AI data in transaction/notification APIs

Monitor in `AsyncConfig` (2 core, 5 max thread pool).

### Querying with Soft Deletion

MyBatis Plus automatically filters `deleted=0`. For complex custom queries in mappers:
- Ensure WHERE clause includes `AND deleted = 0`
- Or use `@TableLogic` annotation on entity fields

## API Response Format

All endpoints return this unified wrapper:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { /* actual response data */ },
  "timestamp": 1234567890
}
```

Errors use non-200 codes with descriptive messages.

## Development & Debugging

**Backend Testing:**
- Use TestController endpoints for quick validation
- Set `logging.level.com.qiandoudou: debug` for detailed logs
- Monitor async thread pool in logs for AI interactions

**Frontend Testing:**
- Use WeChat Developer Tools console for JavaScript logs
- Check network tab to inspect API requests/responses
- Local storage can be inspected in developer tools

**Common Debug Points:**
1. **Transactions with AI** - Verify `ai_partner_id`, `ai_partner_name`, `ai_partner_avatar` fields
2. **Notifications** - Check JOIN with transactions table includes AI fields
3. **OAuth** - Inspect WeChat response and token generation in `AuthController`
4. **TTS** - Monitor ByteDance API responses in logs

## Deployment

**Prerequisites:**
- Java 1.8+ (backend)
- MySQL 8.0+
- Node.js 12+ (frontend build tools, optional)
- 1.8GB minimum memory
- 500MB+ free disk space

**Backend Deployment:**
1. Update `application.yml` with production database and API credentials
2. Run database migrations: Execute SQL files in `src/main/resources/sql/`
3. Build: `mvn clean package -DskipTests`
4. Start: `java -jar qiandoudou-backend-1.0.0.jar`
5. Use provided shell scripts: `start.sh`, `stop.sh`, `status.sh` (if available)

**Frontend Deployment:**
1. Update `app.js` baseUrl to production domain: `https://heartllo.cn/api`
2. Update `utils/api.js` BASE_URL to production domain
3. Build with WeChat Developer Tools and upload to WeChat platform

**Database Initialization:**
```bash
# Execute all migrations in order
cd qiandoudou-backend/src/main/resources/sql/
mysql -u root -p qiandoudou < migration_1.sql
mysql -u root -p qiandoudou < migration_2.sql
# ... continue for all files
```

## Important Implementation Notes

**Soft Deletion Queries:**
When adding new mapper queries, always include soft deletion checks:
```xml
<select id="selectByCondition">
  SELECT * FROM my_table WHERE deleted = 0 AND ...
</select>
```

**Event System:**
Understand the `TransactionCreatedEvent` flow for correct AI interaction behavior:
1. Service publishes event after transaction saved
2. Listener processes async (does not block API response)
3. AI comment stored separately
4. Transaction API may or may not include AI fields depending on query

**Token Management:**
- JWT expiration: 24 hours
- Frontend auto-handles 401 responses (logout + redirect)
- Token stored in `wx.getStorageSync('token')`

**Thread Pool for Async:**
- Core threads: 2
- Max threads: 5
- Used for audio duration calculation, AI text generation
- Monitor via logs if experiencing performance issues

**No Redis:**
Redis dependency is commented out in pom.xml. Enable if caching becomes necessary.

**Chinese Localization:**
Most business logic comments and some field values are in Chinese (target market is Chinese users).

## Configuration Checklist

Before deployment in any environment:

- [ ] Database host, port, username, password configured correctly
- [ ] JWT secret and expiration set appropriately
- [ ] External API keys provided (Aliyun, ByteDance, WeChat)
- [ ] File upload directory exists and has write permissions
- [ ] Server port (8080) not conflicting with other services
- [ ] Logging level configured for the environment
- [ ] Frontend baseUrl points to correct backend domain
- [ ] WeChat mini-program credentials updated

## Common Commands

```bash
# Backend
mvn clean install -DskipTests
mvn spring-boot:run
mvn clean package -DskipTests
java -jar target/qiandoudou-backend-1.0.0.jar
mvn dependency:tree
mvn versions:display-dependency-updates

# Frontend
npm install
npm run lint

# Database
mysql -u root -p qiandoudou < script.sql
```

## Documentation Map

Key documents at repository root (most recent):
- **00_READ_ME_FIRST.md** - Project completion status and current state
- **QUICK_START.md** - 3-minute deployment guide
- **FINAL_IMPLEMENTATION_REPORT.md** - Complete technical report
- **DEPLOYMENT_AND_TESTING_GUIDE.md** - Detailed testing and troubleshooting
- **IMPLEMENTATION_COMPLETE_SUMMARY.md** - Technical implementation details
- **AI_AVATAR_DEBUG_GUIDE.md** - AI avatar feature debugging

Backend has additional CLAUDE.md in `qiandoudou-backend/` with Java-specific guidance.

## Notes for Claude Code

- **Project Structure:** Read frontend and backend files in their respective directories
- **API Testing:** Frontend automatically connects to backend at `http://localhost:8080/api` during local dev
- **Soft Deletion:** Always account for `deleted=0` filtering in queries
- **Async Processing:** Event listeners run asynchronously; understand the flow before modifying
- **WeChat APIs:** Only use `wx.*` APIs, no browser APIs in frontend code
- **Chinese Context:** Project targets Chinese market; preserve Chinese localization
