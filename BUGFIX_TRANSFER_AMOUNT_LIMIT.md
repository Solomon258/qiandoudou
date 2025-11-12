# Bug 修复：转入/转出金额限制

## 问题描述
用户转入金额为 123,456,789 时，后端报错：
```
Data truncation: Out of range value for column 'balance' at row 1
```

## 根本原因分析

### 数据库列类型限制
钱包表 `wallets` 的 `balance` 字段原来定义为：
```sql
DECIMAL(10,2)  -- 最大值：99,999,999.99（8位整数 + 2位小数）
```

当用户输入 123,456,789 时，超过了 DECIMAL(10,2) 的最大范围，导致数据库报错。

### 数据类型对比
| 类型 | 最大值 | 最小值 | 问题 |
|------|-------|-------|------|
| DECIMAL(10,2) | 99,999,999.99 | -99,999,999.99 | 不足，会报错 ❌ |
| DECIMAL(15,2) | 999,999,999,999.99 | -999,999,999,999.99 | 足够（999万亿） ✅ |

## 修复方案

### 方案1：数据库层面修复 ✅

**文件**: `qiandoudou-backend/src/main/resources/sql/fix_balance_column_size.sql`

修改所有涉及金额的列为 `DECIMAL(15,2)` 类型：

```sql
-- 1. 修改 wallets 表的 balance 列
ALTER TABLE `wallets`
MODIFY COLUMN `balance` DECIMAL(15,2) DEFAULT 0.00 COMMENT '余额';

-- 2. 修改钱包目标金额
ALTER TABLE `wallets`
MODIFY COLUMN `dream_target_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '梦想目标金额';

-- 3. 修改交易金额
ALTER TABLE `transactions`
MODIFY COLUMN `amount` DECIMAL(15,2) NOT NULL COMMENT '交易金额';

-- 4. 修改梦想进度历史中的金额
ALTER TABLE `dream_progress_history`
MODIFY COLUMN `trigger_amount` DECIMAL(15,2) NOT NULL COMMENT '触发进度变化的金额';

-- 5. 修改梦想商品建议金额
ALTER TABLE `dream_items`
MODIFY COLUMN `suggested_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '建议金额';
```

### 方案2：前端应用层面校验 ✅

**限制单次转入/转出金额不超过 ¥10,000,000**

#### 转入页面 (transfer-in.js)
```javascript
// 校验2：金额上限检查（不能超过 10,000,000）
const maxAmount = 10000000
const amount = parseFloat(transferAmount)
if (amount > maxAmount) {
  wx.showModal({
    title: '金额超过限制',
    content: `单次转入金额不能超过 ¥${maxAmount.toLocaleString()}，请重新输入。`,
    showCancel: false,
    confirmText: '确定'
  })
  return
}
```

#### 转出页面 (transfer-out.js)
```javascript
// 相同的校验逻辑
const maxAmount = 10000000
const amount = parseFloat(transferAmount)
if (amount > maxAmount) {
  wx.showModal({
    title: '金额超过限制',
    content: `单次转出金额不能超过 ¥${maxAmount.toLocaleString()}，请重新输入。`,
    showCancel: false,
    confirmText: '确定'
  })
  return
}
```

## 修改文件清单

### 后端
- ✅ `qiandoudou-backend/src/main/resources/sql/fix_balance_column_size.sql` (新建)

### 前端
- ✅ `qiandoudou-frontend/pages/transfer-in/transfer-in.js` (第440-454行)
- ✅ `qiandoudou-frontend/pages/transfer-out/transfer-out.js` (第406-417行)

## 部署步骤

### 第1步：执行数据库迁移脚本
```sql
-- 在 MySQL 数据库中执行以下脚本
-- 路径: qiandoudou-backend/src/main/resources/sql/fix_balance_column_size.sql
```

**具体操作：**
1. 打开 MySQL 管理工具（Navicat、DataGrip 等）
2. 连接到 qiandoudou 数据库
3. 打开 `fix_balance_column_size.sql` 文件
4. 执行脚本

**验证结果：**
```sql
-- 执行以下查询确认修改成功
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'qiandoudou'
  AND TABLE_NAME IN ('wallets', 'transactions', 'dream_progress_history', 'dream_items')
  AND (COLUMN_NAME LIKE '%amount%' OR COLUMN_NAME = 'balance')
ORDER BY TABLE_NAME, COLUMN_NAME;

-- 应该看到 DECIMAL(15,2) 的列定义
```

### 第2步：清除小程序缓存并重新编译
```
微信开发者工具 → 清空缓存 → 清空所有缓存
Ctrl+Shift+P 重新编译小程序
```

### 第3步：重启后端服务
```
在 IntelliJ IDEA 中重新启动后端服务
```

## 测试场景

### 测试1：正常金额转入
```
输入金额：5,000,000 元
预期：正常转入，显示成功弹框 ✅
```

### 测试2：金额超过限制
```
输入金额：15,000,000 元
预期：弹框提示"单次转入金额不能超过 ¥10,000,000，请重新输入" ❌
```

### 测试3：大额金额在限制内
```
输入金额：9,999,999.99 元
预期：正常转入 ✅
```

### 测试4：测试边界值
```
输入金额：10,000,000 元
预期：正常转入 ✅

输入金额：10,000,000.01 元
预期：被拦截 ❌
```

## 安全性考虑

### 双重保护机制
1. **前端校验**：提供良好的用户体验，提前拦截不合法输入
2. **后端校验**：应该在后端也添加相同的校验（可选增强）

### 建议的后端校验
可以在后端 `WalletServiceImpl.transferIn()` 方法中添加：
```java
final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("10000000");
if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
    throw new RuntimeException("单次转入金额不能超过 ¥10,000,000");
}
```

## 金额显示格式

使用 `toLocaleString()` 方法可以自动格式化大数字：
```javascript
const amount = 10000000
amount.toLocaleString()  // 显示为 "10,000,000"
```

## 数据库修改影响分析

| 字段 | 原类型 | 新类型 | 影响 |
|------|--------|--------|------|
| balance | DECIMAL(10,2) | DECIMAL(15,2) | ✅ 无影响，只是扩大范围 |
| amount | DECIMAL(10,2) | DECIMAL(15,2) | ✅ 无影响，只是扩大范围 |
| dream_target_amount | DECIMAL(10,2) | DECIMAL(15,2) | ✅ 无影响，只是扩大范围 |

**兼容性：** 完全向后兼容，现有数据无需迁移。

## 相关知识点

### MySQL DECIMAL 类型
```
DECIMAL(P, S)
- P: 总位数（precision），范围1-65
- S: 小数位数（scale），范围0-30

DECIMAL(15,2) 示例：
最大值：999,999,999,999.99（13位整数 + 2位小数）
最小值：-999,999,999,999.99
```

### 微信小程序数值处理
```javascript
// 字符串转数字
parseFloat("10000000")  // 10000000

// 数字格式化
(10000000).toLocaleString()  // "10,000,000"

// BigDecimal 在前端使用
// 前端原生不支持 BigDecimal，需要注意精度问题
```

## 日期
修复时间: 2025-11-11

## 相关文档
- [MySQL DECIMAL 文档](https://dev.mysql.com/doc/refman/8.0/en/fixed-point-types.html)
- [微信小程序 showModal](https://developers.weixin.qq.com/miniprogram/dev/api/ui/interaction/wx.showModal.html)
