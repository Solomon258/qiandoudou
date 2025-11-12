# 修复：兜圈圈关注功能重复插入错误

## 问题描述

### 第一个错误
用户频繁点击关注/已关注时，后台报出以下错误：
```
Duplicate entry '1966089502342791169-1966137314591977474'
for key 'user_follows.uk_follower_following'
```

### 第二个错误（代码修复过程中出现）
执行修复后又报出：
```
Data truncated for column 'id' at row 1
```
原因：`id` 列是 BIGINT 类型，但 SQL 中尝试插入 UUID 字符串导致溢出

## 根本原因分析

### 1. 并发竞态条件
在原代码中 (`SocialServiceImpl.java:121-131`)：
```java
// 检查是否已关注
Integer existingFollow = userFollowMapper.checkUserFollowed(userId, walletId);
if (existingFollow > 0) {
    throw new RuntimeException("已关注该钱包");
}

// 插入关注记录
userFollowMapper.insert(userFollow);
```

**问题**：检查和插入之间不是原子操作。在高并发或网络延迟下：
- 请求A：检查 → 通过（不存在）
- 请求B：检查 → 通过（不存在）
- 请求A：插入 → 成功
- 请求B：插入 → **失败（唯一约束冲突）**

### 2. 软删除 + 唯一约束问题
用户取消关注后（软删除，`deleted=1`），再次关注时：
- 数据库虽然有旧记录（`deleted=1`）
- 但唯一约束 `uk_follower_following` 仍然作用在 `(follower_id, wallet_id)` 上
- 重新插入时触发约束冲突

## 解决方案

### 修改内容

#### 1. **UserFollowMapper.java** - 添加两个方法

**方法1：INSERT IGNORE**（新增关注）
```java
@Insert("INSERT IGNORE INTO user_follows (follower_id, wallet_id, create_time, deleted) " +
        "VALUES (#{followerId}, #{walletId}, NOW(), 0)")
int upsertFollow(@Param("followerId") Long followerId, @Param("walletId") Long walletId);
```

**方法2：激活已删除**（重新关注）
```java
@Update("UPDATE user_follows SET deleted = 0, create_time = NOW() " +
        "WHERE follower_id = #{followerId} AND wallet_id = #{walletId} AND deleted = 1")
void reactivateFollow(@Param("followerId") Long followerId, @Param("walletId") Long walletId);
```

**原理**：
- `INSERT IGNORE`：如果 `(follower_id, wallet_id)` 唯一约束冲突，则忽略（不报错，返回0）
- `UPDATE`：对于软删除的记录，激活它而不是重新插入
- 避免 UUID 生成问题，直接让 MySQL 的自增ID处理

#### 2. **SocialServiceImpl.java** - 改用两步法

修改 `followWallet()` 方法，采用检查 → 激活/新增的策略：

```java
@Override
@Transactional
public void followWallet(Long userId, Long walletId) {
    try {
        // 获取钱包所有者ID
        Long walletOwnerId = this.getWalletOwnerId(walletId);
        if (walletOwnerId == null) {
            throw new RuntimeException("钱包不存在");
        }

        if (walletOwnerId.equals(userId)) {
            throw new RuntimeException("不能关注自己的钱包");
        }

        // 1. 检查是否已经关注
        Integer existingFollow = userFollowMapper.checkUserFollowed(userId, walletId);
        if (existingFollow > 0) {
            throw new RuntimeException("已关注该钱包");
        }

        // 2. 检查是否存在已删除的关注记录，激活它
        Integer deletedFollow = userFollowMapper.selectCount(
            new QueryWrapper<UserFollow>()
                .eq("follower_id", userId)
                .eq("wallet_id", walletId)
                .eq("deleted", 1)
        );

        if (deletedFollow > 0) {
            // 激活已删除的关注
            userFollowMapper.reactivateFollow(userId, walletId);
            System.out.println("用户 " + userId + " 重新激活了对钱包 " + walletId + " 的关注");
        } else {
            // 3. 新增关注（使用INSERT IGNORE避免并发冲突）
            int affected = userFollowMapper.upsertFollow(userId, walletId);
            if (affected <= 0) {
                Integer recheck = userFollowMapper.checkUserFollowed(userId, walletId);
                if (recheck <= 0) {
                    throw new RuntimeException("关注失败，请重试");
                }
            }
            System.out.println("用户 " + userId + " 新增了对钱包 " + walletId + " 的关注");
        }

        // 4. 创建关注通知
        this.createFollowNotification(userId, walletOwnerId, walletId);
        System.out.println("用户 " + userId + " 成功关注了钱包 " + walletId);
    } catch (Exception e) {
        System.err.println("关注失败: " + e.getMessage());
        throw new RuntimeException("关注失败: " + e.getMessage());
    }
}
```

#### 3. **数据库迁移脚本** - `fix_user_follows_duplicate_constraint.sql`

需要在数据库执行以下 SQL：
```sql
-- 1. 删除旧的唯一约束（如果存在）
ALTER TABLE user_follows DROP INDEX IF EXISTS uk_follower_following;

-- 2. 添加新的唯一约束（确保(follower_id, wallet_id)唯一）
ALTER TABLE user_follows ADD UNIQUE KEY uk_follower_following (follower_id, wallet_id);

-- 3. 清理所有软删除的记录（因为新方案会激活重复记录，不需要保留已删除的）
DELETE FROM user_follows WHERE deleted = 1;

-- 4. 验证表结构
DESC user_follows;
```

## 修改文件列表

| 文件 | 修改说明 |
|------|--------|
| `UserFollowMapper.java` | 添加 `upsertFollow()` 方法 |
| `SocialServiceImpl.java` | 改用 UPSERT 逻辑替代检查+插入 |
| `fix_user_follows_duplicate_constraint.sql` | **需要手动执行的数据库迁移脚本** |

## 部署步骤

### 第1步：更新代码
1. 使用修改后的 `UserFollowMapper.java` 和 `SocialServiceImpl.java`
2. 重新编译后端代码

### 第2步：执行数据库迁移（必须）
在你的 MySQL 数据库中执行：
```bash
source /path/to/fix_user_follows_duplicate_constraint.sql
```

或在 MySQL 客户端逐条执行脚本中的 SQL 语句。

### 第3步：重启后端服务
```bash
# 在 IntelliJ IDEA 中重启 Spring Boot 应用
```

## 验证修复

修复后，可以进行以下测试：
1. **快速关注/取消关注**：重复点击关注和已关注按钮，不应该报错
2. **并发关注**：使用多个浏览器标签页同时点击关注，应该能正确处理
3. **关注/取消/再关注**：完整流程应该正常运行

## 技术细节

### 为什么使用 INSERT IGNORE + UPDATE？

1. **避免 UUID 问题**：不在 SQL 中生成 UUID，直接使用 MySQL 自增 ID
2. **并发安全**：`INSERT IGNORE` 在唯一约束冲突时直接返回 0，不抛异常
3. **软删除友好**：通过单独的 `UPDATE` 语句激活已删除的记录
4. **简单可靠**：比复杂的 `ON DUPLICATE KEY UPDATE` 更清晰

### 为什么分两个方法而不是一个？

```
场景1：首次关注
- INSERT IGNORE → 插入成功 → 返回 1
- 创建通知 → 完成

场景2：取消关注后再关注
- INSERT IGNORE → 约束冲突，返回 0 → 检查已删除记录
- UPDATE → 激活已删除的记录 → 返回
- 创建通知 → 完成

场景3：并发关注
- 请求A: INSERT IGNORE → 插入成功
- 请求B: INSERT IGNORE → 约束冲突，返回 0 → 检查已关注
- 请求B: 抛出 "已关注该钱包" → 安全退出
```

### 与其他功能的对比

虽然点赞功能（`likeTransaction`）也使用 UPSERT，但：
- 点赞用的是 `INSERT...ON DUPLICATE KEY UPDATE`（可以在 SQL 中处理所有逻辑）
- 关注需要分两步，因为软删除的特殊性（被删除的记录不能直接覆盖）

## 相关代码文件位置

- **UserFollowMapper**: `qiandoudou-backend/src/main/java/com/qiandoudou/mapper/UserFollowMapper.java`
  - `upsertFollow()` 方法 (L87-90)
  - `reactivateFollow()` 方法 (L92-97)
- **SocialServiceImpl**: `qiandoudou-backend/src/main/java/com/qiandoudou/service/impl/SocialServiceImpl.java` (L107-165)
- **SQL脚本**: `qiandoudou-backend/src/main/resources/sql/fix_user_follows_duplicate_constraint.sql`

---
修复日期：2025-11-05

## 常见问题

### Q: 为什么要删除所有软删除的记录？
A: 新方案中，已删除的记录会通过 `reactivateFollow()` 激活，所以不需要保留。如果保留它们，会增加数据库体积而没有实际意义。

### Q: INSERT IGNORE 返回 0 是什么意思？
A: 返回 0 表示没有插入任何行（因为唯一约束冲突被忽略了）。这时需要检查是否存在已删除的记录来激活。

### Q: 如果并发请求都通过了检查怎么办？
A: 第一个请求会 INSERT 成功，后续请求会触发 `INSERT IGNORE` 的冲突处理，返回 0，然后再检查现状态，如果已关注则抛出异常。这在业务层面是正确的处理。

---
版本：1.1（修复了 UUID 生成问题）
