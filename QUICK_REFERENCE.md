# 🚀 梦想攒图片配置 - 快速参考

## 📌 核心概念

| 概念 | 说明 | 字段 | 示例 |
|-----|------|------|------|
| **梦想类型** | 1=购物, 2=旅游 | `dream_type` | `1` |
| **商品名称** | 商品/目的地ID | `item_name` | `car`, `beijing` |
| **进度图片** | 5张，分别对应20%-100% | `progress_image_base_url` | 前缀地址 |
| **分享图片** | 1张，分享时显示 | `share_image_url` | 完整地址 |

---

## 🗂️ 表结构

```sql
dream_image_config
├── id (BIGINT) - 主键
├── dream_type (TINYINT) - 梦想类型：1-购物，2-旅游
├── item_name (VARCHAR) - 商品/目的地名称
├── item_display_name (VARCHAR) - 显示名称
├── progress_image_base_url (VARCHAR) - 进度图前缀 ⭐
├── share_image_url (VARCHAR) - 分享图URL ⭐
├── is_active (TINYINT) - 是否启用
├── create_time (DATETIME)
├── update_time (DATETIME)
└── deleted (TINYINT)
```

---

## 💾 SQL快速执行

### 1️⃣ 建表
```sql
-- 见 dream_image_config.sql 文件
```

### 2️⃣ 插入汽车配置（核心示例）
```sql
INSERT INTO `dream_image_config` (
  `dream_type`, `item_name`, `item_display_name`,
  `progress_image_base_url`,
  `share_image_url`, `is_active`
) VALUES (
  1, 'car', '轿车',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png',
  1
);
```

### 3️⃣ 查询示例
```sql
-- 查询汽车的进度图片前缀
SELECT progress_image_base_url FROM dream_image_config
WHERE dream_type = 1 AND item_name = 'car' AND is_active = 1;

-- 查询汽车的分享图片
SELECT share_image_url FROM dream_image_config
WHERE dream_type = 1 AND item_name = 'car' AND is_active = 1;
```

---

## 🔌 后端 API

### 获取进度图片列表（重要⭐）
```
GET /dream-image-config/progress-images?dreamType=1&itemName=car

返回 5 个 URL:
[
  ".../car/part1.png",
  ".../car/part2.png",
  ".../car/part3.png",
  ".../car/part4.png",
  ".../car/part5.png"
]
```

### 获取分享图片（重要⭐）
```
GET /dream-image-config/share-image?dreamType=1&itemName=car

返回 1 个 URL:
".../car/share.png"
```

### 获取完整配置
```
GET /dream-image-config/complete-info?dreamType=1&itemName=car

返回: {
  progressImages: [...],
  shareImage: "...",
  config: {...}
}
```

---

## 📱 前端关键方法

### dreamImageAPI（新增）
```javascript
// 获取进度图片列表
dreamImageAPI.getProgressImages(dreamType, itemName)

// 获取分享图片
dreamImageAPI.getShareImage(dreamType, itemName)

// 获取完整配置
dreamImageAPI.getCompleteImageInfo(dreamType, itemName)
```

### car-share.js（修改）
```javascript
// 页面加载时调用
onLoad() {
  this.loadShareImage() // 从 API 获取分享图片
}
```

### dream-detail.js（修改）
```javascript
// 获取进度图片
await getCarImageByProgress(progress) // 现在是异步的
```

---

## 🔄 工作流程

### 梦想详情页
```
加载页面 → 获取钱包信息 → 调用API获取进度图片列表
→ 根据进度百分比选择对应图片 → 显示
```

### 分享页面
```
打开car-share → 获取钱包信息 → 获取dreamItemName
→ 调用API获取分享图片 → 显示
```

---

## 📊 汽车商品详细信息

| 字段 | 值 |
|-----|-----|
| dream_type | 1 |
| item_name | `car` |
| item_display_name | 轿车 |
| progress_image_base_url | `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/` |
| share_image_url | `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png` |

### 进度图片自动生成规则
基础URL + `part{1-5}.png`

| 阶段 | 进度范围 | 文件名 | 完整URL |
|-----|---------|--------|--------|
| 1 | 0-20% | part1.png | `.../car/part1.png` |
| 2 | 20-40% | part2.png | `.../car/part2.png` |
| 3 | 40-60% | part3.png | `.../car/part3.png` |
| 4 | 60-80% | part4.png | `.../car/part4.png` |
| 5 | 80-100% | part5.png | `.../car/part5.png` |

---

## 🎯 添加新商品（3步）

### 第一步：准备图片
```
上传到OSS:
- .../dream/goods/myitem/part1.png
- .../dream/goods/myitem/part2.png
- .../dream/goods/myitem/part3.png
- .../dream/goods/myitem/part4.png
- .../dream/goods/myitem/part5.png
- .../dream/goods/myitem/share.png
```

### 第二步：插入数据库
```sql
INSERT INTO dream_image_config VALUES (
  NULL,
  1,
  'myitem',
  '我的新商品',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/myitem/',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/myitem/share.png',
  1,
  NOW(),
  NOW(),
  0
);
```

### 第三步：测试
```javascript
// 前端测试
const images = await dreamImageAPI.getProgressImages(1, 'myitem')
// 应该返回5张图片URL

const shareImage = await dreamImageAPI.getShareImage(1, 'myitem')
// 应该返回1张分享图片URL
```

---

## ⚠️ 常见问题

### Q: 图片显示不出来怎么办？
A: 检查：
1. OSS图片链接是否正确
2. `progress_image_base_url` 末尾是否有斜杠 `/`
3. 数据库中 `is_active = 1`
4. 前端是否正确调用API

### Q: 进度图片顺序错了怎么办？
A: 检查：
1. `getProgressImages()` 返回的顺序（应为1-5）
2. `getCarImageByProgress()` 中的逻辑
3. 进度百分比计算是否正确

### Q: 如何替换现有商品的图片？
A: 两种方案：
```sql
-- 方案1：更新URL
UPDATE dream_image_config
SET progress_image_base_url = 'new_url/',
    share_image_url = 'new_url/share.png'
WHERE item_name = 'car' AND dream_type = 1;

-- 方案2：禁用旧的，添加新的
UPDATE dream_image_config SET is_active = 0 WHERE item_name = 'car';
INSERT INTO dream_image_config VALUES (...);
```

---

## 📝 文件清单

| 文件 | 类型 | 操作 |
|-----|------|------|
| dream_image_config.sql | SQL | ✨ 新增 |
| DreamImageConfig.java | Entity | ✨ 新增 |
| DreamImageConfigMapper.java | Mapper | ✨ 新增 |
| DreamImageConfigService.java | Service接口 | ✨ 新增 |
| DreamImageConfigServiceImpl.java | Service实现 | ✨ 新增 |
| DreamImageConfigController.java | Controller | ✨ 新增 |
| DreamWalletServiceImpl.java | 修改 | 📝 修改2个方法 |
| utils/api.js | 修改 | 📝 新增dreamImageAPI |
| pages/car-share/car-share.js | 修改 | 📝 新增loadShareImage方法 |
| pages/dream-detail/dream-detail.js | 修改 | 📝 修改getCarImageByProgress方法 |

---

## ✅ 验证步骤

```bash
# 1. 检查表是否创建成功
SELECT * FROM dream_image_config LIMIT 1;

# 2. 检查汽车配置
SELECT * FROM dream_image_config WHERE item_name = 'car';

# 3. 启动后端，调用API
curl 'http://localhost:8080/dream-image-config/progress-images?dreamType=1&itemName=car'

# 4. 前端测试
console.log(await dreamImageAPI.getProgressImages(1, 'car'))

# 5. 打开app测试分享功能
# 在car-share页面查看分享图片是否正确
```

---

## 🎓 最佳实践

✅ **DO:**
- 使用数据库管理所有图片URL
- 为每个商品创建5张进度图片 + 1张分享图片
- 使用统一的命名规范（part1.png...part5.png, share.png）
- 在Controller返回API时做错误处理和日志记录

❌ **DON'T:**
- 直接在代码中硬编码图片URL
- 混淆progress_image_base_url和share_image_url
- 忘记在URL末尾添加斜杠
- 跳过降级方案的实现

---

**如有问题，查看完整文档: IMPLEMENTATION_SUMMARY.md**

