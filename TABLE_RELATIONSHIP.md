# 📊 dream_image_config 表关联关系详解

## 一、表间关联概览

```
dream_items (配置表)
    ↓ (通过 name 和 category)

dream_image_config (新增：图片配置表) ⭐
    ↓ (通过 dream_type 和 item_name)

wallets (用户数据表)
```

---

## 二、详细关联关系

### 关键关联字段

| 源表 | 源字段 | 目标表 | 目标字段 | 关联说明 |
|-----|--------|-------|---------|---------|
| **wallets** | `dream_type` + `dream_item_name` | **dream_image_config** | `dream_type` + `item_name` | 🔗 **主要关联** |
| **wallets** | `dream_destination` | **dream_image_config** | `item_name` | 🔗 旅行梦想关联 |
| **dream_items** | `name` | **dream_image_config** | `item_name` | 🔗 配置关联 |
| **dream_items** | `category` | **dream_image_config** | `dream_type` | 🔗 类型对应 |

---

## 三、表结构对比

### dream_items 表 (商品配置表)
```sql
CREATE TABLE dream_items (
  id BIGINT PRIMARY KEY,
  category TINYINT,          -- 1=购物, 2=旅行
  name VARCHAR(100),         -- car, beijing 等 ⭐ 关键字段
  display_name VARCHAR(100), -- 轿车, 北京 等
  image_url VARCHAR(500),    -- 商品列表图片
  bubble_image_url VARCHAR(500), -- 气泡图片
  ...
);
```

**用途**: 存储梦想商品/目的地的基本配置信息

---

### dream_image_config 表 (图片配置表) ✨ NEW
```sql
CREATE TABLE dream_image_config (
  id BIGINT PRIMARY KEY,
  dream_type TINYINT,              -- 1=购物, 2=旅行
  item_name VARCHAR(100),          -- car, beijing 等 ⭐ 关键字段
  item_display_name VARCHAR(100),  -- 轿车, 北京 等
  progress_image_base_url VARCHAR(500),  -- 进度图前缀 ✨ 新增
  share_image_url VARCHAR(500),          -- 分享图URL ✨ 新增
  ...
);
```

**用途**: 存储梦想商品/目的地的图片配置（进度图+分享图）

**与 dream_items 的关系**:
- `item_name` 对应 `dream_items.name`
- `dream_type` 对应 `dream_items.category`
- `item_display_name` 对应 `dream_items.display_name` (可选)

---

### wallets 表 (用户钱包表)
```sql
CREATE TABLE wallets (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  ...基础字段...

  -- 梦想攒相关字段
  dream_type TINYINT,              -- 1=购物, 2=旅行 ⭐ 关键字段
  dream_item_name VARCHAR(100),    -- car, beijing 等 ⭐ 关键字段
  dream_item_image VARCHAR(500),   -- 当前显示的图片
  dream_destination VARCHAR(100),  -- 旅行目的地
  dream_progress INT,              -- 进度百分比 (0-100)
  dream_target_amount DECIMAL,     -- 目标金额
  ...
);
```

**用途**: 存储用户创建的梦想钱包数据

**与 dream_image_config 的关系**:
- `dream_type` 对应 `dream_image_config.dream_type`
- `dream_item_name` 对应 `dream_image_config.item_name` (购物梦想)
- `dream_destination` 对应 `dream_image_config.item_name` (旅行梦想)

---

## 四、关联流程图

### 购物梦想示例

```
┌─────────────────────────────────────────────────────────────┐
│ 1️⃣ 用户在app中选择"轿车"创建梦想钱包                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2️⃣ 查询 dream_items 表                                  │
│    WHERE category = 1 AND name = 'car'                      │
│    获得: id=1, name='car', display_name='轿车' 等        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3️⃣ 创建 wallets 记录                                    │
│    dream_type = 1                                           │
│    dream_item_name = 'car'  ⭐ 复制 dream_items.name  │
│    dream_target_amount = 100000.00                          │
│    dream_progress = 0                                       │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4️⃣ 显示梦想详情页时，查询进度图片                     │
│    SELECT * FROM dream_image_config                         │
│    WHERE dream_type = 1                                     │
│    AND item_name = 'car'                                    │
│    AND is_active = 1                                        │
│                                                             │
│    获得:                                                    │
│    progress_image_base_url = 'https://.../car/'            │
│    share_image_url = 'https://.../car/share.png'          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 5️⃣ 后端根据进度自动拼接完整的进度图片URL              │
│    dream_progress = 50% → 返回 progress_image_base_url + 'part3.png' │
│    (40%-60% 对应第3张图片)                                │
└─────────────────────────────────────────────────────────────┘
```

### 旅行梦想示例

```
┌─────────────────────────────────────────────────────────────┐
│ 1️⃣ 用户选择"北京"创建旅行梦想钱包                   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2️⃣ 查询 dream_items 表                                  │
│    WHERE category = 2 AND name = 'beijing'                  │
│    获得: id=11, name='beijing', display_name='北京'     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3️⃣ 创建 wallets 记录                                    │
│    dream_type = 2                                           │
│    dream_destination = 'beijing'  ⭐ 关键字段        │
│    dream_item_name = 'beijing'    (也存这个)             │
│    dream_days = 7                                           │
│    dream_target_amount = 5000.00                            │
│    dream_progress = 0                                       │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4️⃣ 显示梦想详情页时，查询进度图片                     │
│    SELECT * FROM dream_image_config                         │
│    WHERE dream_type = 2                                     │
│    AND item_name = 'beijing'                                │
│    AND is_active = 1                                        │
│                                                             │
│    获得:                                                    │
│    progress_image_base_url = 'https://.../travel/beijing/'  │
│    share_image_url = 'https://.../travel/beijing/share.png' │
└─────────────────────────────────────────────────────────────┘
```

---

## 五、实际数据示例

### dream_items 表数据

```
id | category | name     | display_name | image_url
---|----------|----------|--------------|------------------------------------------
1  | 1        | car      | 轿车        | /static/icon/goods/轿车@3x.png
2  | 1        | bag      | 包包        | /static/icon/goods/包包@3x.png
...
11 | 2        | beijing  | 北京        | /static/icon/destinations/beijing.jpg
12 | 2        | xian     | 西安        | /static/icon/destinations/xian.jpg
```

### dream_image_config 表数据

```
id | dream_type | item_name | item_display_name | progress_image_base_url              | share_image_url
---|------------|-----------|-------------------|--------------------------------------|------------------------------------
1  | 1          | car       | 轿车             | https://.../dream/goods/car/        | https://.../dream/goods/car/share.png
2  | 1          | bag       | 包包             | https://.../dream/goods/bag/        | https://.../dream/goods/bag/share.png
...
11 | 2          | beijing   | 北京             | https://.../dream/travel/beijing/   | https://.../dream/travel/beijing/share.png
12 | 2          | xian      | 西安             | https://.../dream/travel/xian/      | https://.../dream/travel/xian/share.png
```

### wallets 表数据（用户数据）

```
id   | user_id | dream_type | dream_item_name | dream_destination | dream_progress | dream_target_amount
-----|---------|-----------|-----------------|-------------------|----------------|-------------------
1001 | 100     | 1         | car             | NULL              | 50             | 100000.00
1002 | 101     | 2         | beijing         | beijing           | 75             | 5000.00
1003 | 102     | 1         | bag             | NULL              | 25             | 8000.00
```

---

## 六、关联查询示例

### 查询示例 1️⃣ : 获取用户的梦想详情 (包含图片配置)

```sql
-- 查询用户的梦想钱包及对应的图片配置
SELECT
    w.id as wallet_id,
    w.dream_type,
    w.dream_item_name,
    w.dream_progress,
    w.dream_target_amount,
    dic.progress_image_base_url,
    dic.share_image_url
FROM wallets w
LEFT JOIN dream_image_config dic
    ON w.dream_type = dic.dream_type
    AND w.dream_item_name = dic.item_name
WHERE w.id = 1001 AND dic.is_active = 1;

结果:
wallet_id | dream_type | dream_item_name | dream_progress | dream_target_amount | progress_image_base_url        | share_image_url
----------|------------|-----------------|----------------|---------------------|--------------------------------|---------------------------
1001      | 1          | car             | 50             | 100000.00           | https://.../dream/goods/car/  | https://.../car/share.png
```

### 查询示例 2️⃣ : 根据商品名称查询配置

```sql
-- 查询汽车商品的所有配置信息
SELECT
    di.name,
    di.display_name,
    di.image_url,
    dic.progress_image_base_url,
    dic.share_image_url
FROM dream_items di
LEFT JOIN dream_image_config dic
    ON di.category = dic.dream_type
    AND di.name = dic.item_name
WHERE di.name = 'car' AND di.is_active = 1 AND dic.is_active = 1;

结果:
name | display_name | image_url                      | progress_image_base_url         | share_image_url
-----|--------------|--------------------------------|--------------------------------|--------------------------
car  | 轿车        | /static/icon/goods/轿车@3x.png | https://.../dream/goods/car/   | https://.../car/share.png
```

### 查询示例 3️⃣ : 自动拼接进度图片URL

```java
// Java后端代码
List<String> getProgressImages(Integer dreamType, String itemName) {
    DreamImageConfig config = dreamImageConfigMapper.selectByTypeAndName(dreamType, itemName);
    List<String> progressImages = new ArrayList<>();

    String baseUrl = config.getProgressImageBaseUrl(); // "https://.../dream/goods/car/"

    for (int i = 1; i <= 5; i++) {
        String imageUrl = baseUrl + "part" + i + ".png";
        progressImages.add(imageUrl);
    }

    return progressImages;
    // 返回: [
    //   "https://.../dream/goods/car/part1.png",
    //   "https://.../dream/goods/car/part2.png",
    //   "https://.../dream/goods/car/part3.png",
    //   "https://.../dream/goods/car/part4.png",
    //   "https://.../dream/goods/car/part5.png"
    // ]
}
```

---

## 七、为什么没有外键约束

### 设计原因

| 方面 | 无外键设计 | 有外键设计 | 选择 |
|-----|---------|---------|------|
| **灵活性** | 高（可独立管理） | 低（受约束） | ✅ 无外键 |
| **数据历史** | 用户数据不受配置删除影响 | 删除配置可能级联删除用户数据 | ✅ 无外键 |
| **扩展性** | 容易添加新商品 | 需要维护外键约束 | ✅ 无外键 |
| **性能** | 无约束检查，查询快 | 需要约束检查，略慢 | ✅ 无外键 |

### 实现方式

**逻辑关联** 而不是 **物理外键**:
```
wallets.dream_type + wallets.dream_item_name
    ↓ (通过应用层代码)
dream_image_config.dream_type + dream_image_config.item_name
```

这种方式的好处:
- ✅ 同一个 `dream_item_name` 可以有多个图片配置（购物+旅行）
- ✅ 删除旧商品配置不会影响用户已有的梦想钱包
- ✅ 修改配置不会影响用户数据
- ✅ 可以灵活启用/禁用配置

---

## 八、关键字段映射表

### 购物梦想映射

```
dream_items          dream_image_config       wallets
┌──────────┐         ┌──────────────┐        ┌─────────────┐
│ id: 1    │         │ id: 1        │        │ id: 1001    │
│ cat: 1   ├────────▶│ type: 1      │◀───────┤ type: 1     │
│ name: car├────────▶│ item_name:car│◀───────┤ item_name:car│
│ disp:轿车│         │ disp: 轿车   │        │ disp: 轿车  │
└──────────┘         └──────────────┘        └─────────────┘
                            ▲
                            │
                     ┌──────┴────────┐
                     │ progress_url  │
                     │ share_url ✨  │
                     └───────────────┘
```

### 旅行梦想映射

```
dream_items          dream_image_config       wallets
┌──────────┐         ┌──────────────┐        ┌──────────────┐
│ id: 11   │         │ id: 11       │        │ id: 1002     │
│ cat: 2   ├────────▶│ type: 2      │◀───────┤ type: 2      │
│ name: beijing      │ item_name:bj │◀───────┤ destination:bj│
│ disp:北京│         │ disp: 北京   │        │ item_name:bj │
└──────────┘         └──────────────┘        └──────────────┘
                            ▲
                            │
                     ┌──────┴────────┐
                     │ progress_url  │
                     │ share_url ✨  │
                     └───────────────┘
```

---

## 九、增删改查操作

### 添加新商品的图片配置

```sql
-- 1. 首先确保 dream_items 中有这个商品
INSERT INTO dream_items (category, name, display_name, ...)
VALUES (1, 'yacht', '游艇', ...);

-- 2. 然后在 dream_image_config 中添加图片配置
INSERT INTO dream_image_config (dream_type, item_name, item_display_name, progress_image_base_url, share_image_url)
VALUES (1, 'yacht', '游艇', 'https://.../dream/goods/yacht/', 'https://.../dream/goods/yacht/share.png');
```

### 查询某个梦想的所有配置

```sql
SELECT * FROM dream_image_config
WHERE dream_type = 1 AND is_active = 1
ORDER BY item_name;
```

### 更新图片配置

```sql
UPDATE dream_image_config
SET progress_image_base_url = 'https://new-url/',
    share_image_url = 'https://new-url/share.png'
WHERE item_name = 'car' AND dream_type = 1;
```

### 禁用某个商品的配置

```sql
UPDATE dream_image_config
SET is_active = 0
WHERE item_name = 'old_product' AND dream_type = 1;
```

---

## 十、总结

### 核心关联关系

```
dream_items (商品基本配置)
    ↓ (name, category 字段)
dream_image_config (图片配置) ✨ NEW
    ↓ (dream_type, item_name 字段)
wallets (用户数据)
```

### 关键关联字段

| 关系 | 字段 | 说明 |
|-----|------|------|
| **wallets ↔ dream_image_config** | `dream_type` + `dream_item_name/dream_destination` | 主要查询关联 |
| **dream_items ↔ dream_image_config** | `category ↔ dream_type`, `name ↔ item_name` | 配置对应关系 |

### 为什么采用逻辑关联而非物理外键

✅ 保护用户数据不受配置删除影响
✅ 提高系统灵活性和可维护性
✅ 便于独立管理配置和用户数据
✅ 支持一对多的灵活映射关系

