# 关注功能修复 - 执行步骤

## 快速开始

### 步骤1：更新代码（已完成）
以下文件已修改，无需手动操作：
- ✅ `UserFollowMapper.java` - 添加了 `upsertFollow()` 和 `reactivateFollow()` 方法
- ✅ `SocialServiceImpl.java` - 改用 INSERT IGNORE + UPDATE 的两步法
- ✅ `fix_user_follows_duplicate_constraint.sql` - 数据库迁移脚本

### 步骤2：执行数据库迁移（必须由你执行）

在 MySQL 数据库中执行以下命令：

```bash
# 进入 MySQL 客户端
mysql -u root -p 你的密码

# 选择数据库
USE qiandoudou;

# 执行迁移脚本
source D:\4_code\qiandoudou\qiandoudou-backend\src\main\resources\sql\fix_user_follows_duplicate_constraint.sql;
```

或者在 MySQL Workbench/Navicat 中直接打开脚本文件或逐条执行以下 SQL：

**脚本内容（简化版）：**
```sql
-- 清理所有软删除的记录（新方案会激活重复记录，不需要保留已删除的）
DELETE FROM user_follows WHERE deleted = 1;

-- 验证表结构
DESC user_follows;

-- 验证约束是否存在
SHOW KEYS FROM user_follows WHERE Key_name = 'uk_follower_following';
```

**注意**：表结构中的 `uk_follower_following` 约束已经正确存在，所以脚本只做数据清理。

### 步骤3：重新编译后端（在 IntelliJ IDEA 中）

1. 打开 `qiandoudou-backend` 项目
2. 执行 **Build → Rebuild Project** 或按 `Ctrl+Shift+F9`
3. 确保编译成功，没有错误

### 步骤4：重启后端服务

1. 停止正在运行的 Spring Boot 应用
2. 重新启动 Spring Boot 应用
3. 等待应用完全启动

### 步骤5：测试修复

在小程序中测试以下场景：

#### 场景A：快速关注/取消关注
```
1. 打开社交圈，找到一个钱包
2. 快速点击 "关注" → "已关注" → "关注"
3. 应该不报错，界面正常切换
```

#### 场景B：并发关注
```
1. 打开多个浏览器标签页，同时访问同一个钱包
2. 在各个标签页快速点击关注
3. 应该能正确处理并发，不报错
```

#### 场景C：关注/取消/再关注
```
1. 关注一个钱包 → 钱包详情显示 "已关注"
2. 点击 "取消关注" → 变回 "关注"
3. 再点 "关注" → 应该能重新关注，显示 "已关注"
4. 整个流程不报错
```

---

## 如果出现问题

### 问题1：编译失败
**解决**：
- 确保有 `@Update` import 语句
- 清理 Maven 缓存：`mvn clean`
- 重新生成：`mvn install`

### 问题2：数据库报错
**常见错误及解决**：

| 错误 | 原因 | 解决 |
|-----|------|------|
| `Duplicate entry...` | 约束未正确更新 | 重新执行迁移脚本，确保约束删除后再添加 |
| `Unknown column 'deleted'` | 表结构不一致 | 执行 `DESC user_follows;` 检查结构 |
| `Can't DROP INDEX...` | 索引名称不对 | 在 MySQL 中执行 `SHOW KEYS FROM user_follows;` 查看实际索引名 |

### 问题3：关注仍然报错
**排查步骤**：
1. 检查数据库迁移是否成功（查看索引是否存在）
2. 检查后端代码是否编译成功
3. 查看后端日志，找到具体错误信息
4. 重启后端服务

---

## 验证迁移成功

执行以下 SQL 检查迁移是否正确：

```sql
-- 检查约束是否存在
SHOW KEYS FROM user_follows WHERE Key_name = 'uk_follower_following';

-- 应该返回一行数据，显示约束信息

-- 检查是否还有软删除的记录
SELECT COUNT(*) FROM user_follows WHERE deleted = 1;

-- 应该返回 0
```

---

## 修复内容总结

### 问题
- 频繁点击关注/已关注导致 `Duplicate entry` 错误
- 原因：并发竞态条件 + 软删除 + 唯一约束冲突

### 解决方案
- 使用 `INSERT IGNORE` 替代普通 `INSERT`，避免异常
- 用 `UPDATE` 激活已删除的记录，而不是重新插入
- 不在 SQL 中生成 UUID，避免 ID 类型不匹配

### 修改文件
1. `UserFollowMapper.java` - 新增 2 个方法
2. `SocialServiceImpl.java` - 改用两步法
3. `fix_user_follows_duplicate_constraint.sql` - 数据库迁移

---

预计修复时间：**5-10 分钟**
