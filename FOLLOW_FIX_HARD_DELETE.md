# 修复：关注功能软删除导致的唯一约束问题

## 问题分析

用户反馈：多次点击关注/已关注后仍然出现唯一约束冲突错误，且数据库中 `deleted` 字段一直为 1。

### 根本原因

原来的实现使用了**软删除**来处理"取消关注"操作：

```java
// 取消关注时执行
UPDATE user_follows SET deleted = 1 WHERE follower_id = ? AND wallet_id = ?
```

**问题**：
1. 虽然记录被标记为 `deleted = 1`，但它仍然占用唯一约束 `uk_follower_following(follower_id, wallet_id)`
2. 当用户重新关注时，尝试插入新记录，但唯一约束发现同一对 `(follower_id, wallet_id)` 已存在
3. 即使是软删除的记录也会触发唯一约束冲突

### 为什么原方案失效

原来的 `followWallet` 方法试图激活已删除的记录：

```java
// 检查是否有软删除的记录
if (deletedFollow > 0) {
    // 激活记录
    UPDATE user_follows SET deleted = 0, create_time = NOW()
    WHERE deleted = 1 AND follower_id = ? AND wallet_id = ?
}
```

但这个方案在高并发或重复操作时容易失败，因为：
- 激活失败后没有适当的后备方案
- 多次点击可能导致激活失败，然后再尝试插入，触发唯一约束冲突

## 解决方案

改为**真删除**而非软删除：

```java
// 取消关注时执行
DELETE FROM user_follows WHERE follower_id = ? AND wallet_id = ?
```

**优点**：
- ✅ 完全删除记录，不占用唯一约束
- ✅ 重新关注时可以直接插入新记录，无约束冲突
- ✅ 逻辑简单，无需激活已删除的记录
- ✅ 并发安全，避免激活失败的复杂场景

## 修改内容

### 1. UserFollowMapper.java

#### 修改 removeFollow 方法（真删除）
```java
@Delete("DELETE FROM user_follows " +
        "WHERE follower_id = #{followerId} AND wallet_id = #{walletId}")
int removeFollow(@Param("followerId") Long followerId, @Param("walletId") Long walletId);
```

#### 移除不再需要的 reactivateFollow 方法
```java
// 已删除，不再需要激活已删除的记录
```

#### 简化 upsertFollow 说明
```java
@Insert("INSERT IGNORE INTO user_follows (follower_id, wallet_id, create_time, deleted) " +
        "VALUES (#{followerId}, #{walletId}, NOW(), 0)")
int upsertFollow(...);
```

### 2. SocialServiceImpl.java - followWallet 方法

大幅简化逻辑：

```java
public void followWallet(Long userId, Long walletId) {
    // 1. 验证钱包存在和不能自关注

    // 2. 检查是否已关注
    if (checkUserFollowed(userId, walletId) > 0) {
        throw new RuntimeException("已关注该钱包");
    }

    // 3. 直接插入关注记录（不需要处理软删除激活）
    int affected = userFollowMapper.upsertFollow(userId, walletId);
    if (affected > 0) {
        System.out.println("新增关注记录");
    } else {
        // INSERT IGNORE 返回0表示记录已存在（并发）
        Integer recheck = userFollowMapper.checkUserFollowed(userId, walletId);
        if (recheck <= 0) {
            throw new RuntimeException("关注失败");
        }
    }

    // 4. 创建通知
    createFollowNotification(...);
}
```

### 3. unfollowWallet 方法

无需修改，直接调用真删除的 `removeFollow`：

```java
public void unfollowWallet(Long userId, Long walletId) {
    // ... 验证逻辑

    // 直接删除（真删除）
    userFollowMapper.removeFollow(userId, walletId);
}
```

## 数据库迁移

### 执行 SQL

在数据库中执行：

```sql
-- 删除所有软删除的记录
DELETE FROM user_follows WHERE deleted = 1;

-- 验证数据
SELECT COUNT(*) as total_records FROM user_follows;
SELECT COUNT(*) as deleted_records FROM user_follows WHERE deleted = 1;

-- 验证约束
SHOW KEYS FROM user_follows WHERE Key_name = 'uk_follower_following';
```

脚本文件：`fix_user_follows_hard_delete.sql`

## 测试场景

### 场景 1：正常关注-取消-再关注
```
1. 打开钱包，点击"关注"
   → 数据库插入记录（deleted=0）

2. 点击"已关注"取消关注
   → 数据库删除记录（真删除）

3. 再点击"关注"
   → 数据库插入新记录（deleted=0）
   → ✅ 成功，无约束冲突
```

### 场景 2：快速多次点击
```
1. 快速点击关注 10 次
   → 前端防抖：只发 1 个请求
   → 后端 INSERT IGNORE：第一个成功，后续返回 0
   → ✅ 成功，无约束冲突
```

### 场景 3：并发操作
```
1. 用户A和B同时点击关注同一个钱包
   → 用户A: INSERT 成功
   → 用户B: INSERT IGNORE 返回 0（约束冲突）
           但 checkUserFollowed 检查已关注
   → ✅ 都成功，数据库只有一条记录
```

## 关键改进点

| 方面 | 修改前 | 修改后 |
|-----|------|------|
| **删除方式** | 软删除（deleted=1） | 真删除 |
| **占用约束** | ✗ 仍占用 | ✓ 完全释放 |
| **激活逻辑** | 复杂 | 无需激活 |
| **并发安全** | ✗ 易失败 | ✓ 简单可靠 |
| **代码行数** | ~70行 | ~20行 |

## 部署步骤

### 第1步：执行数据库清理（必须）
```bash
# 在数据库中执行
DELETE FROM user_follows WHERE deleted = 1;
```

### 第2步：重新编译后端
```bash
# 在 IntelliJ IDEA 中
Build → Rebuild Project
```

### 第3步：重启后端服务

### 第4步：清空前端缓存（可选）
在微信开发者工具中清空缓存

### 第5步：测试
测试关注、取消关注、再关注的完整流程

## 为什么这个方案更好

1. **符合实际需求**
   - 用户取消关注就是不再关注，完全删除是合理的
   - 关注历史可以在审计日志中记录（如需要）

2. **数据库层面的优化**
   - 真删除避免了表膨胀（软删除的记录永不清理）
   - 约束管理更简单

3. **代码逻辑的简化**
   - 不需要复杂的激活逻辑
   - 不需要处理软删除和未删除的两种状态

4. **并发性能**
   - INSERT IGNORE 比 UPDATE + INSERT 的组合更高效
   - 减少数据库锁争夺

## 后续优化建议

1. **审计日志**（如有需要）
   - 可在关注表中添加操作日志表，记录关注/取消关注的历史

2. **deleted 列**
   - 该列现在已不使用，未来可考虑删除（保持向后兼容，暂时保留）

3. **性能监控**
   - 监控用户关注操作的成功率
   - 记录并发关注的情况

---

**修改日期**：2025-11-05
**修改类型**：后端重要修复（真删除替代软删除）
**风险等级**：低（逻辑简化，并发更安全）
