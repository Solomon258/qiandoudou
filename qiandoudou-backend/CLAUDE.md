# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Qiandoudou (钱兜兜)** is a Spring Boot backend service for a gamified personal finance application that combines wallet management, AI-driven savings challenges, companion interactions, and social networking features.

- **Language:** Java 8+
- **Framework:** Spring Boot 2.7.18
- **Build Tool:** Maven
- **Database:** MySQL 8.0+ (underscore naming, utf8mb4, timezone Shanghai UTC+8)
- **ORM:** MyBatis Plus 3.5.3.1 with soft deletion pattern
- **Architecture:** Service-oriented with event-driven async processing
- **Entry Point:** `QiandoudouApplication.java` with `@MapperScan`, `@EnableAsync`, `@EnableScheduling`

## Quick Start

### Build
```bash
mvn clean package -DskipTests
mvn clean compile
```

### Run
```bash
# Using Maven
mvn spring-boot:run

# Using packaged JAR
java -jar target/qiandoudou-backend-1.0.0.jar
```

### Server
- **Port:** 8080
- **API Context Path:** `/api`
- **Configuration:** `src/main/resources/application.yml`

### Test Single Class/Method
Tests are integration-based. To test a specific feature:
1. Use `TestController` (src/main/java/com/qiandoudou/controller/TestController.java) for quick API verification
2. Make HTTP calls to test endpoints directly
3. Check logs at `debug` level for com.qiandoudou package

### Key Files
- **Main Class:** `QiandoudouApplication.java`
- **Config:** `src/main/resources/application.yml`
- **Database Migrations:** `src/main/resources/sql/`
- **MyBatis Mappers:** `src/main/resources/mapper/`

## Project Structure

### Core Domains
The application is organized into 5 main business domains:

1. **User Management & Authentication** (User, UserLoginLog)
   - JWT-based token auth with 24-hour expiration
   - WeChat mini-program OAuth integration
   - Login history tracking

2. **Wallet & Transaction Management** (Wallet, Transaction, WalletView)
   - 5 wallet types: Personal (1), Couple (2), Buddy (3), Dream Shopping (4), Dream Travel (5)
   - Transaction history with AI partner metadata
   - Public/private wallet visibility
   - Background customization

3. **Script-Based Savings Challenges** (Script, ScriptChapter, UserScriptProgress)
   - Story-driven savings goals
   - Categories: Recommended, Travel, Shopping, Learning, Fitness
   - Progress tracking with chapters and completion rewards

4. **AI Companion System** (AiPartner, BuddyGroup, BuddyCharacter, AiLoverInteraction)
   - Multiple companion types with personality/voice settings
   - Text-to-speech via ByteDance Volcano Engine
   - Auto-comments on transactions
   - Image-to-text OCR recognition
   - Couple wallet AI support

5. **Social & Dream Features** (UserFollow, PostLike, PostComment, DreamWallet, Notification)
   - Follow/like/comment system
   - Dream savings (Shopping/Travel) with milestones
   - Notifications with interaction messages
   - Share functionality

### Layer Structure
- **Controller** (12 files) - REST API endpoints with unified Result<T> wrapper
- **Service** - Business logic (24 interfaces + 25 implementations)
- **Mapper** (25 files) - MyBatis data access with XML mappings in `src/main/resources/mapper/`
- **Entity** (26 files) - JPA/MyBatis entities with Lombok annotations and soft deletion

### Key Architectural Patterns

#### Service-Oriented Architecture

- Clear separation: Controllers → Services → Mappers
- Interface-based design for all services
- All services extend `IService<T>` from MyBatis Plus

#### Event-Driven Processing

- `TransactionCreatedEvent` triggers after transaction commits
- Async listeners process AI interactions without blocking
- Uses `ThreadPoolTaskExecutor` (2 core, 5 max threads)

#### Soft Deletion Pattern

- All entities use `@TableLogic(value = "0", delValue = "1")` annotation
- Queries automatically exclude deleted records
- `deleted=0` for active, `deleted=1` for soft-deleted records

#### Data Access

- MyBatis Plus with custom XML mappings for complex queries
- Pagination built-in via interceptor
- ID generation: `@TableId(type = IdType.ASSIGN_ID)`

## Key External Integrations

### APIs & Cloud Services
- **Aliyun OSS** - Image/audio/video storage (bucket: qiandoudou)
- **ByteDance APIs** - TTS (Volcano Engine) and image-to-text recognition
- **OpenAI API** - Text generation (optional, currently commented out)
- **WeChat Mini Program** - OAuth login integration

### Configuration
All external API keys, credentials, and endpoints are in `application.yml`. Key properties:
- `aliyun.oss.*` - Aliyun configuration
- `byte-dance.*` - ByteDance API keys
- `spring.datasource.*` - Database connection
- `jwt.secret` and `jwt.expiration` - Token settings

## Database

### Key Tables
- **users** - User accounts with login history
- **wallets** - Multiple wallet types per user with balances
- **transactions** - Financial transactions with optional AI partner metadata
- **scripts** - Savings challenge definitions
- **ai_partners** - AI companion definitions with personality/voice
- **buddy_groups** - Buddy circle social groups
- **user_follows** - Follow relationships
- **post_likes, post_comments** - Social interactions
- **dream_wallets** - Dream savings data with progress
- **notifications** - User notifications with interaction messages

### Design Notes
- All soft-deleted (deleted column)
- Underscore naming convention (auto-mapped to camelCase by MyBatis Plus)
- Auto-filled: `create_time`, `update_time`
- Timezone: Shanghai
- Character set: utf8mb4

## Important Development Notes

### Adding New Features
1. Create entity in `entity/`
2. Create mapper in `mapper/` with XML in `src/main/resources/mapper/`
3. Create service interface in `service/` and implementation in `service/impl/`
4. Create controller in `controller/` with `@RestController` and `/api` base path
5. Add database migrations in `src/main/resources/sql/`

### API Response Format
All endpoints return a unified wrapper:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { /* actual response */ },
  "timestamp": 1234567890
}
```

### Async Processing
- Audio duration calculation happens async
- AI text generation can trigger async
- Monitor thread pool: `AsyncConfig` class (2 core, 5 max threads)

### Transaction & AI Integration
When a transaction is created:
1. `TransactionCreatedEvent` published
2. `AiLoverInteractionEventListener` processes asynchronously
3. AI comment generated and stored
4. Transaction enriched with AI metadata

### Querying with Soft Deletion

MyBatis Plus automatically filters `deleted=0`. For complex queries in mappers, ensure your WHERE clause includes `AND deleted = 0` if not using the ORM directly.

## Codebase Structure

### Package Organization

```text
com.qiandoudou/
├── QiandoudouApplication.java        # Main entry point
├── common/                           # Shared utilities
│   └── Result.java                   # API response wrapper
├── config/                           # Spring configurations
│   ├── AsyncConfig.java              # Thread pool for async tasks
│   ├── MybatisPlusConfig.java        # ORM configuration
│   ├── JacksonConfig.java            # JSON serialization
│   └── WeChatConfig.java             # WeChat integration
├── controller/                       # REST endpoints (12 classes)
│   ├── WalletController.java
│   ├── UserController.java
│   ├── TransactionController.java    # Note: may be merged with WalletController
│   ├── ScriptController.java
│   ├── SocialController.java
│   ├── BuddyController.java
│   ├── DreamWalletController.java
│   ├── AiController.java
│   └── ...others
├── entity/                           # Data models (26 classes)
│   ├── Core: User, Wallet, Transaction, Script, ScriptChapter
│   ├── AI: AiPartner, BuddyCharacter, BuddyGroup, AiLoverInteraction
│   ├── Social: UserFollow, PostLike, PostComment, Notification
│   ├── Dream: DreamItem, DreamProgressHistory, DreamReward, DreamWallet
│   └── ...others
├── event/                            # Event classes
│   └── TransactionCreatedEvent.java  # Published after transaction creation
├── listener/                         # Event listeners
│   └── AiLoverInteractionEventListener.java  # Async AI processing
├── mapper/                           # Data access interfaces (25 classes)
│   └── Custom XML mappings in src/main/resources/mapper/
├── service/                          # Service interfaces (24 classes)
├── service/impl/                     # Service implementations (25 classes)
│   ├── Core services: WalletServiceImpl, UserServiceImpl, etc.
│   ├── TTS services: VolcengineTtsService, ByteDanceImageToTextService
│   ├── Cloud services: OssService, AliyunOssService
│   └── ...others
└── util/                             # Utilities
    ├── JwtUtil.java                  # Token generation/validation
    └── HttpUtil.java                 # HTTP client helpers
```

### Key File Locations

- **Main application class:** [QiandoudouApplication.java](src/main/java/com/qiandoudou/QiandoudouApplication.java)
- **API response wrapper:** [common/Result.java](src/main/java/com/qiandoudou/common/Result.java)
- **Configuration:** [application.yml](src/main/resources/application.yml)
- **Database migrations:** [src/main/resources/sql/](src/main/resources/sql/)
- **MyBatis mappers:** [src/main/resources/mapper/](src/main/resources/mapper/)
- **Async configuration:** [config/AsyncConfig.java](src/main/java/com/qiandoudou/config/AsyncConfig.java)
- **Event listener:** [listener/AiLoverInteractionEventListener.java](src/main/java/com/qiandoudou/listener/AiLoverInteractionEventListener.java)

### Controller Entry Points

All controllers are under the `/api` context path:

- `/api/user` - User information and settings
- `/api/wallet` - Wallet and transaction management
- `/api/transaction` - Detailed transaction operations
- `/api/script` - Savings scripts/goals and chapters
- `/api/buddy` - AI buddy/companion interactions
- `/api/dream` - Dream wallet and savings features
- `/api/social` - Social features (follow, like, comment)
- `/api/ai` - AI-specific operations and text generation
- `/api/auth` - Authentication and token management
- `/api/test` - Test endpoints for development

### Service Layer Organization

Services follow the interface-based pattern and extend `IService<T>`:

**Core Domain Services:**

- `UserService/UserServiceImpl` - User management
- `WalletService/WalletServiceImpl` - Wallet operations
- `TransactionService/TransactionServiceImpl` - Transaction handling
- `ScriptService/ScriptServiceImpl` - Savings script management

**AI & Companion Services:**

- `AiPartnerService` - AI companion configuration
- `BuddyService/BuddyServiceImpl` - Buddy circle operations
- `VolcengineTtsService` - ByteDance TTS integration
- `ByteDanceImageToTextService` - Image recognition
- `AiLoverInteractionService` - Couple wallet AI interactions
- `AiAutoCommentService` - Auto-comment on transactions

**Social Services:**

- `SocialService/SocialServiceImpl` - Follow, like, comment operations
- `NotificationService` - User notifications

**Dream Services:**

- `DreamWalletService` - Dream savings management
- `DreamImageConfigService` - Dream image configuration

**Cloud Services:**

- `OssService/AliyunOssService` - Aliyun OSS storage
- `StaticResourceService` - Static resource handling
- `ShareImageService` - Share image generation

## Common Development Patterns

### Adding a New API Endpoint

1. **Define Entity** - Create the data model in `entity/`
2. **Create Mapper** - Add interface in `mapper/` with optional XML in `src/main/resources/mapper/`
3. **Create Service** - Add interface in `service/` and implementation in `service/impl/`
4. **Create Controller** - Add REST endpoint in `controller/` with `@RestController` and `@RequestMapping("/api/path")`
5. **Add Database Schema** - Create migration SQL in `src/main/resources/sql/`
6. **Return Result Wrapper** - All responses should use `Result<T>` from `common/Result.java`

Example structure:
```java
// Controller
@RestController
@RequestMapping("/api/myfeature")
public class MyFeatureController {
    @GetMapping("/{id}")
    public Result<MyFeatureDTO> getById(@PathVariable String id) {
        return Result.success(myFeatureService.getById(id));
    }
}

// Service
public interface MyFeatureService extends IService<MyFeature> {
    MyFeatureDTO getById(String id);
}
```

### Publishing Async Events

For operations that need async processing (like AI interactions):

1. **Create Event** - Define event class extending `ApplicationEvent`
2. **Publish in Service** - Use `applicationContext.publishEvent()` after operation completes
3. **Create Listener** - Implement event listener with `@TransactionalEventListener`
4. **Monitor Execution** - Check `AsyncConfig` thread pool settings

Example:
```java
// In TransactionServiceImpl after saving
applicationContext.publishEvent(new TransactionCreatedEvent(transaction));

// Listener handles async processing
@TransactionalEventListener
public void onTransactionCreated(TransactionCreatedEvent event) {
    // AI processing happens here asynchronously
}
```

### Working with Soft Deletion

All entities automatically filter `deleted=0` via MyBatis Plus `@TableLogic` annotation. However, when writing custom XML mappers:

```xml
<select id="selectByCondition">
  SELECT * FROM my_table
  WHERE deleted = 0
  AND some_field = #{value}
</select>
```

For queries using ORM methods (`selectById`, `selectList`), soft deletion is handled automatically.

### Integrating External APIs

Services like `VolcengineTtsService` and `ByteDanceImageToTextService` handle API integrations:

1. **Configuration** - API keys and endpoints in `application.yml`
2. **HTTP Client** - Use `RestTemplate` or OkHttp for API calls
3. **Error Handling** - Gracefully handle API failures and timeouts
4. **Async Execution** - For long operations, use `@Async` or thread pool

### Transaction with AI Interactions

When creating a transaction:

1. Transaction is saved to database
2. `TransactionCreatedEvent` is published
3. `AiLoverInteractionEventListener` processes async:
   - Generates AI comment via `AiAutoCommentService`
   - Stores comment in `ai_lover_interaction` table
   - Updates transaction with AI partner metadata
4. Frontend queries transaction API and receives enriched data with AI fields

## Testing & Debugging

### Quick Testing
- Use `TestController` endpoints for API validation
- Run `mvn test` for unit tests (if available)
- Check logs: Set `logging.level.com.qiandoudou: debug` in application.yml

### Common Debug Points
1. **Transaction API** - Check if transaction has `ai_partner_id`, `ai_partner_name`, `ai_partner_avatar`
2. **Notification API** - Verify AI fields are returned from JOIN with transactions table
3. **OAuth Flow** - Check WeChat response and token generation in `AuthController`
4. **TTS Generation** - Monitor ByteDance API response in `VolcengineTtsService`

### Logs
- Application logs go to console (Spring Boot default)
- Enable file logging via `logging.file.name` in application.yml
- Debug mode for `com.qiandoudou.*` recommended during development

## Deployment

### Prerequisites
- Java 1.8+
- MySQL 8.0+
- 1.8GB minimum memory (2GB+ recommended)
- 500MB+ free disk space

### Build for Production
```bash
mvn clean package -DskipTests
```

### Deploy
1. Update `application.yml` with production database/API credentials
2. Run migrations: Execute SQL files in `src/main/resources/sql/`
3. Start service: `java -jar qiandoudou-backend-1.0.0.jar`
4. Use provided shell scripts: `start.sh`, `stop.sh`, `status.sh`

### Database Migration
All SQL migration files are in `src/main/resources/sql/`. Execute them in order before deployment.

## Configuration Checklist

Before running in any environment:
- [ ] Database host, port, username, password set correctly
- [ ] JWT secret and expiration configured
- [ ] External API keys (Aliyun, ByteDance, WeChat) provided
- [ ] File upload directory permissions verified
- [ ] Server port not conflicting with other services
- [ ] Logging configuration appropriate for environment

## Common Commands

```bash
# Clean build
mvn clean install -DskipTests

# Run with Spring Boot
mvn spring-boot:run

# Package JAR
mvn clean package -DskipTests

# Run packaged JAR
java -jar target/qiandoudou-backend-1.0.0.jar

# View dependency tree
mvn dependency:tree

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Key Entity Relationships

Understanding how entities relate to each other is crucial for working with this codebase:

### User & Authentication

- `User` (1) → (N) `UserLoginLog` - User login history
- `User` (1) → (N) `Wallet` - User can have multiple wallets
- `User` (1) → (N) `UserFollow` - User follow relationships

### Wallet & Transactions

- `Wallet` (1) → (N) `Transaction` - Wallet contains transactions
- `Transaction` (N) → (1) `AiPartner` (optional) - Transaction optionally linked to AI partner
- `Wallet` (1) → (1) `WalletView` - View metadata for wallet (optional)
- `Wallet` (1) → (N) `WalletBuddy` - Buddy circle membership

### AI Companions

- `AiPartner` (1) → (N) `AiLoverInteraction` - AI interactions on couple wallet
- `BuddyGroup` (1) → (N) `BuddyCharacter` - Group has multiple buddy characters
- `BuddyCharacter` (1) → (1) `CharacterVoiceMapping` - Voice configuration

### Savings Goals (Scripts)

- `Script` (1) → (N) `ScriptChapter` - Script contains chapters
- `User` (1) → (N) `UserScriptProgress` - Track user progress in scripts
- `ScriptChapter` (1) → (N) `UserScriptProgress` - Progress mapped to chapters

### Dreams & Goals

- `Wallet` (1) → (N) `DreamItem` - Dream items in wallet (Shopping/Travel)
- `DreamItem` (1) → (N) `DreamProgressHistory` - Progress tracking
- `DreamItem` (1) → (N) `DreamReward` - Rewards for milestones
- `DreamItem` (1) → (1) `DreamImageConfig` - Image configuration

### Social Interactions

- `User` (1) → (N) `UserFollow` - Who they follow
- `User` (1) → (N) `PostLike` - Posts they like
- `User` (1) → (N) `PostComment` - Comments they make
- `User` (1) → (N) `Notification` - User notifications

### Query Patterns

When querying related data:

1. **Simple relationships** - Use `@Resource` to inject related service and query separately
2. **Complex relationships** - Use custom mapper with XML to JOIN multiple tables
3. **Always account for soft deletion** - Include `AND deleted = 0` in custom queries
4. **Pagination** - Built-in via MyBatis Plus interceptor, use `Page<T>` and `IPage<T>`

## Important Notes

- **No Redis:** Redis dependency is commented out in pom.xml. Enable if caching needed.
- **Event System:** Understand `TransactionCreatedEvent` flow for AI interactions.
- **Async Tasks:** Audio processing runs in thread pool. Monitor thread usage.
- **Soft Deletion:** All queries automatically filter deleted=0 via MyBatis Plus.
- **Chinese Localization:** Most comments and some field values are in Chinese (target market).
- **WeChat Integration:** Mini-program appid: `wx8372421078267cd9`

## Related Documentation

Additional documentation files in parent directory:
- `00_READ_ME_FIRST.md` - Project completion summary
- `QUICK_START.md` - 3-minute deployment guide
- `DEPLOYMENT_AND_TESTING_GUIDE.md` - Detailed testing and troubleshooting
- `FINAL_IMPLEMENTATION_REPORT.md` - Complete technical report
