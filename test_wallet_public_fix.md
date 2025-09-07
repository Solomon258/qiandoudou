# 钱包公开状态修复 - 测试验证指南

## 修改内容总结

### 1. 后端代码修改
- **文件**: `qiandoudou-backend/src/main/java/com/qiandoudou/service/impl/WalletServiceImpl.java`
- **修改**: 将第48行的 `wallet.setIsPublic(0)` 改为 `wallet.setIsPublic(1)`
- **效果**: 所有新建钱包默认公开到社交圈

### 2. 数据库修复脚本
- **文件**: `update_wallets_public.sql`
- **作用**: 将现有的私密钱包设置为公开状态

## 测试验证步骤

### 步骤1: 执行SQL脚本
1. 在数据库管理工具中打开 `update_wallets_public.sql`
2. 按顺序执行脚本中的SQL语句
3. 确认"test2-情侣1"钱包的 `is_public` 字段变为 1

### 步骤2: 重启后端服务
1. 在IDEA中停止后端服务
2. 重新启动后端服务，确保代码修改生效

### 步骤3: 验证现有钱包在社交圈显示
1. 打开微信开发者工具
2. 进入社交圈页面
3. 确认能看到"test2-情侣1"钱包
4. 检查其他之前看不到的钱包是否也显示了

### 步骤4: 验证新建钱包默认公开
1. 创建一个新的钱包（个人或情侣钱包）
2. 立即检查社交圈页面
3. 确认新建的钱包直接出现在社交圈中

### 步骤5: 后端API测试
可以使用以下API端点验证：

```bash
# 获取公开钱包列表
GET http://localhost:8080/wallet/public

# 获取钱包统计信息
GET http://localhost:8080/wallet/test/stats
```

## 预期结果

### 修改前
- 新建钱包默认 `is_public = 0`（私密）
- "test2-情侣1"等钱包在社交圈不可见
- 只有手动设置为公开的钱包才在社交圈显示

### 修改后
- 新建钱包默认 `is_public = 1`（公开）
- "test2-情侣1"等钱包在社交圈可见
- 所有钱包都在社交圈显示（除非用户主动设置为私密）

## 回滚方案
如果需要回滚修改：

1. **代码回滚**:
   ```java
   wallet.setIsPublic(0); // 改回默认不公开
   ```

2. **数据库回滚**:
   ```sql
   -- 将所有钱包设回私密（谨慎操作）
   UPDATE wallets SET is_public = 0 WHERE deleted = 0;
   ```

## 注意事项
- 执行SQL脚本前建议备份数据库
- 确保后端服务重启后再进行测试
- 如果前端缓存了数据，可能需要清除小程序缓存或重新编译
