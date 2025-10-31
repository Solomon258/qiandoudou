# 🔗 dream_image_config 关联关系 - 快速查看

## 三表关联简明图

```
┌─────────────────────────┐
│   dream_items           │  配置表：商品基本信息
│   (商品/目的地配置)      │
├─────────────────────────┤
│ id          BIGINT      │
│ category    TINYINT ◄───┐
│ name        VARCHAR ◄────┐  ⭐ 关键关联字段
│ display_name VARCHAR     │
│ image_url   VARCHAR      │
│ ...                      │
└─────────────────────────┘
         ▲
         │ (category→dream_type, name→item_name)
         │
┌─────────────────────────────────────────────┐
│   dream_image_config                        │  🆕 新增表：图片配置
│   (图片配置)                                │
├─────────────────────────────────────────────┤
│ id                 BIGINT                   │
│ dream_type         TINYINT ◄────────────┐   │
│ item_name          VARCHAR ◄────────────┤   │  ⭐ 关键关联字段
│ progress_image_base_url  VARCHAR   ✨  │   │
│ share_image_url    VARCHAR         ✨  │   │
│ is_active          TINYINT              │   │
│ ...                                     │   │
└─────────────────────────────────────────────┘
         ▲
         │ (dream_type + dream_item_name/dream_destination)
         │
┌─────────────────────────┐
│   wallets               │  用户数据表：梦想钱包
│   (梦想钱包)            │
├─────────────────────────┤
│ id                      │
│ user_id                 │
│ dream_type         ────▶│ 1 or 2
│ dream_item_name    ────▶│ car, beijing
│ dream_destination  ────▶│ beijing (旅行时)
│ dream_progress    (0-100)
│ dream_target_amount     │
│ ...                     │
└─────────────────────────┘
```

---

## 关键问题解答

### ❓ Q: dream_image_config 与 dream_items 如何关联？

**A:** 通过两个字段关联（无物理外键）

| dream_items | dream_image_config | 说明 |
|-----------|------------------|------|
| category | dream_type | 商品分类（1=购物, 2=旅行） |
| name | item_name | 商品/目的地名称（如：car, beijing） |

**查询示例**：
```sql
SELECT di.*, dic.*
FROM dream_items di
JOIN dream_image_config dic
    ON di.category = dic.dream_type
    AND di.name = dic.item_name
WHERE di.name = 'car';
```

---

### ❓ Q: dream_image_config 与 wallets 如何关联？

**A:** 通过梦想类型和商品名称关联（无物理外键）

| wallets | dream_image_config | 说明 |
|---------|------------------|------|
| dream_type | dream_type | 梦想类型（1=购物, 2=旅行） |
| dream_item_name | item_name | **购物梦想**时关联 |
| dream_destination | item_name | **旅行梦想**时关联 |

**购物梦想查询示例**：
```sql
SELECT w.*, dic.*
FROM wallets w
JOIN dream_image_config dic
    ON w.dream_type = dic.dream_type
    AND w.dream_item_name = dic.item_name
WHERE w.id = 1001 AND dic.is_active = 1;
```

**旅行梦想查询示例**：
```sql
SELECT w.*, dic.*
FROM wallets w
JOIN dream_image_config dic
    ON w.dream_type = dic.dream_type
    AND w.dream_destination = dic.item_name
WHERE w.id = 1002 AND dic.is_active = 1;
```

---

### ❓ Q: 为什么不用外键约束？

**A:** 设计权衡表

| 维度 | 外键约束 | 无外键约束 | 选择 |
|-----|--------|---------|------|
| 数据完整性 | ✅ 强制约束 | ⚠️ 应用层保证 | ❌ 外键 |
| 用户数据安全 | ❌ 删除配置影响用户数据 | ✅ 用户数据独立 | ✅ 无外键 |
| 系统灵活性 | ❌ 严格约束限制 | ✅ 灵活管理 | ✅ 无外键 |
| 添加新商品 | ⚠️ 需要同时插入两张表 | ✅ 只需两个记录 | ✅ 无外键 |

**结论**: 采用 **逻辑关联** 而非 **物理外键**

---

## 数据流向实例

### 📌 购物梦想：用户创建"轿车"梦想钱包

```
步骤 1️⃣ : 用户选择商品
  ↓
  app → backend API: /dream-wallet/create
         { dreamType: 1, itemId: 1 (car的ID) }

步骤 2️⃣ : 后端查询 dream_items
  ↓
  SELECT * FROM dream_items WHERE id = 1
  结果: { id: 1, category: 1, name: 'car', display_name: '轿车', ... }

步骤 3️⃣ : 后端创建 wallets 记录
  ↓
  INSERT INTO wallets (
    dream_type = 1,
    dream_item_name = 'car',  ⭐ 复制自 dream_items.name
    dream_target_amount = 100000,
    dream_progress = 0,
    ...
  )

步骤 4️⃣ : 前端展示梦想详情页
  ↓
  从钱包数据获取: dream_type=1, dream_item_name='car'

步骤 5️⃣ : 后端查询图片配置
  ↓
  SELECT * FROM dream_image_config
  WHERE dream_type = 1 AND item_name = 'car' AND is_active = 1

  结果: {
    id: 1,
    dream_type: 1,
    item_name: 'car',
    progress_image_base_url: 'https://.../dream/goods/car/',
    share_image_url: 'https://.../dream/goods/car/share.png',
    ...
  }

步骤 6️⃣ : 后端返回图片URL给前端
  ↓
  progressImages: [
    'https://.../dream/goods/car/part1.png',
    'https://.../dream/goods/car/part2.png',
    'https://.../dream/goods/car/part3.png',
    'https://.../dream/goods/car/part4.png',
    'https://.../dream/goods/car/part5.png'
  ]
  shareImage: 'https://.../dream/goods/car/share.png'

步骤 7️⃣ : 前端根据进度显示图片
  ↓
  if (dream_progress >= 80) display progressImages[4]
  else if (dream_progress >= 60) display progressImages[3]
  ...
```

### 📌 旅行梦想：用户创建"北京"梦想钱包

```
步骤 1️⃣ : 用户选择目的地
  ↓
  app → backend API: /dream-wallet/create
         { dreamType: 2, itemId: 11 (beijing的ID) }

步骤 2️⃣ : 后端查询 dream_items
  ↓
  SELECT * FROM dream_items WHERE id = 11
  结果: { id: 11, category: 2, name: 'beijing', display_name: '北京', ... }

步骤 3️⃣ : 后端创建 wallets 记录
  ↓
  INSERT INTO wallets (
    dream_type = 2,
    dream_item_name = 'beijing',    ⭐ 复制自 dream_items.name
    dream_destination = 'beijing',  ⭐ 也存这个
    dream_days = 7,
    dream_target_amount = 5000,
    dream_progress = 0,
    ...
  )

步骤 4️⃣ : 前端展示梦想详情页
  ↓
  从钱包数据获取: dream_type=2, dream_destination='beijing'

步骤 5️⃣ : 后端查询图片配置
  ↓
  SELECT * FROM dream_image_config
  WHERE dream_type = 2 AND item_name = 'beijing' AND is_active = 1

  结果: {
    progress_image_base_url: 'https://.../dream/travel/beijing/',
    share_image_url: 'https://.../dream/travel/beijing/share.png',
    ...
  }

步骤 6️⃣-7️⃣ : 同购物梦想...
```

---

## 后端代码中的关联

### 查询进度图片（DreamImageConfigService.java）

```java
/**
 * 获取进度图片列表
 * 通过 dream_type + item_name 查询 dream_image_config
 */
public List<String> getProgressImages(Integer dreamType, String itemName) {
    // 关联查询点：dream_type 和 item_name
    DreamImageConfig config = this.getByTypeAndName(dreamType, itemName);

    if (config == null) {
        return null;
    }

    String baseUrl = config.getProgressImageBaseUrl();
    List<String> progressImages = new ArrayList<>();

    // 自动拼接 part1.png ~ part5.png
    for (int i = 1; i <= 5; i++) {
        progressImages.add(baseUrl + "part" + i + ".png");
    }

    return progressImages;
}
```

### 在钱包服务中调用（DreamWalletServiceImpl.java）

```java
/**
 * 获取当前阶段的图片
 * 关联 wallets 和 dream_image_config
 */
private String getCurrentStageImage(Wallet wallet, Integer progress) {
    if (wallet.getDreamType() == 1) {
        // 购物梦想：使用 wallet.dreamItemName
        String itemName = wallet.getDreamItemName();

        // 通过 (dream_type=1, item_name) 查询 dream_image_config
        List<String> progressImages = dreamImageConfigService
            .getProgressImages(1, itemName);

        if (progressImages != null && !progressImages.isEmpty()) {
            if (progress >= 80) return progressImages.get(4);
            if (progress >= 60) return progressImages.get(3);
            if (progress >= 40) return progressImages.get(2);
            if (progress >= 20) return progressImages.get(1);
            return progressImages.get(0);
        }
    } else {
        // 旅行梦想：使用 wallet.dreamDestination
        String destination = wallet.getDreamDestination();

        // 通过 (dream_type=2, item_name) 查询 dream_image_config
        List<String> progressImages = dreamImageConfigService
            .getProgressImages(2, destination);

        // ... 同上
    }
}
```

---

## 前端代码中的关联

### dream-detail.js 中的调用

```javascript
async getCarImageByProgress(progress) {
    const dreamWallet = this.data.dreamWallet;
    const dreamType = dreamWallet.dreamType || 1;

    // 确定关联的字段
    let itemName = null;
    if (dreamType === 1) {
        // 购物梦想：使用 dreamItemName
        itemName = dreamWallet.dreamItemName;
    } else {
        // 旅行梦想：使用 dreamDestination
        itemName = dreamWallet.dreamDestination;
    }

    // 调用 API: GET /dream-image-config/progress-images?dreamType=1&itemName=car
    const result = await dreamImageAPI.getProgressImages(dreamType, itemName);

    // 根据进度选择图片
    const progressImages = result.data;
    if (progress >= 80) return progressImages[4];
    if (progress >= 60) return progressImages[3];
    if (progress >= 40) return progressImages[2];
    if (progress >= 20) return progressImages[1];
    return progressImages[0];
}
```

---

## 总结表

### 关联字段对应表

| 场景 | dream_items | dream_image_config | wallets | 查询条件 |
|-----|----------|----------|--------|---------|
| **购物梦想** | name='car' category=1 | item_name='car' dream_type=1 | dream_item_name='car' dream_type=1 | dream_type=1 AND (dream_item_name='car') |
| **旅行梦想** | name='beijing' category=2 | item_name='beijing' dream_type=2 | dream_destination='beijing' dream_type=2 | dream_type=2 AND (dream_destination='beijing') |

### 数据一致性检查

```sql
-- 验证关联的一致性
-- 1. 检查 wallets 中的每个 dream_item_name 是否都有对应的 dream_image_config
SELECT w.dream_item_name, COUNT(dic.id) as config_count
FROM wallets w
LEFT JOIN dream_image_config dic
    ON w.dream_type = dic.dream_type
    AND w.dream_item_name = dic.item_name
WHERE w.dream_type = 1
GROUP BY w.dream_item_name
HAVING config_count = 0;  -- 如果有结果说明配置缺失

-- 2. 检查 dream_image_config 中是否有 wallets 中不存在的配置
SELECT DISTINCT dic.item_name
FROM dream_image_config dic
WHERE NOT EXISTS (
    SELECT 1 FROM wallets w
    WHERE w.dream_type = dic.dream_type
    AND w.dream_item_name = dic.item_name
);
```

---

## 核心要点

✅ **没有物理外键** - 采用逻辑关联
✅ **主关联字段** - dream_type + item_name
✅ **购物用 dream_item_name** - 旅行用 dream_destination
✅ **查询时必须指定 dream_type** - 确保关联正确
✅ **无需JOIN** - 应用层在代码中关联

