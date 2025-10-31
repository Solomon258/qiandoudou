# 梦想攒图片配置实现 - 文件清单

## 📋 总览

本次实现涉及1个新表、3个后端文件、1个后端控制器、2个前端API文件、2个前端页面文件

---

## 🗄️ 数据库文件

### 1. dream_image_config.sql
**位置**: `D:\4_code\qiandoudou\dream_image_config.sql`

**内容**:
- 创建表: `dream_image_config`
- 插入汽车商品配置
- 插入其他购物商品（SUV、超跑、包包、劳力士）
- 插入旅游目的地（北京、西安、拉萨、悉尼、新加坡、阿格拉市、重庆）

**执行方式**:
```bash
mysql -u your_user -p your_database < dream_image_config.sql
```

---

## 🔙 后端文件

### 后端新增文件（3个）

#### 1. DreamImageConfig.java
**路径**: `qiandoudou-backend/src/main/java/com/qiandoudou/entity/DreamImageConfig.java`

**功能**: 梦想攒图片配置实体类

**字段**:
- `id`: 主键
- `dreamType`: 梦想类型（1-购物，2-旅游）
- `itemName`: 商品/目的地名称
- `itemDisplayName`: 显示名称
- `progressImageBaseUrl`: 进度图片URL前缀
- `shareImageUrl`: 分享图片完整URL
- `isActive`: 是否启用
- `createTime`, `updateTime`, `deleted`: 审计字段

---

#### 2. DreamImageConfigMapper.java
**路径**: `qiandoudou-backend/src/main/java/com/qiandoudou/mapper/DreamImageConfigMapper.java`

**功能**: Mapper接口，继承BaseMapper

**方法**:
- `selectByTypeAndName(@Param("dreamType") Integer dreamType, @Param("itemName") String itemName)`: 根据梦想类型和商品名称查询

---

#### 3. DreamImageConfigService.java & DreamImageConfigServiceImpl.java
**路径**:
- 接口: `qiandoudou-backend/src/main/java/com/qiandoudou/service/DreamImageConfigService.java`
- 实现: `qiandoudou-backend/src/main/java/com/qiandoudou/service/impl/DreamImageConfigServiceImpl.java`

**主要方法**:
- `getByTypeAndName()`: 查询配置
- `getProgressImages()`: 返回5张进度图片URL列表
  - 自动拼接part1.png ~ part5.png
- `getShareImage()`: 返回分享图片URL
- `getActiveByType()`: 获取所有启用的配置

---

### 后端新增控制器（1个）

#### 4. DreamImageConfigController.java
**路径**: `qiandoudou-backend/src/main/java/com/qiandoudou/controller/DreamImageConfigController.java`

**路由前缀**: `/dream-image-config`

**端点**:
- `GET /get?dreamType=1&itemName=car` → 获取配置
- `GET /progress-images?dreamType=1&itemName=car` → 获取进度图片列表（返回5张）
- `GET /share-image?dreamType=1&itemName=car` → 获取分享图片
- `GET /list/{dreamType}` → 获取所有启用的配置
- `GET /complete-info?dreamType=1&itemName=car` → 获取完整信息（包含进度图片和分享图片）

---

### 后端修改文件（1个）

#### 5. DreamWalletServiceImpl.java
**路径**: `qiandoudou-backend/src/main/java/com/qiandoudou/service/impl/DreamWalletServiceImpl.java`

**修改内容**:
- 添加 `@Autowired private DreamImageConfigService dreamImageConfigService;`
- 修改 `getCurrentStageImage()` 方法：
  - 优先从数据库查询进度图片
  - 支持购物梦想（使用 `dreamItemName`）和旅游梦想（使用 `dreamDestination`）
  - 降级方案：返回商品图片

- 修改 `generateShareImage()` 方法：
  - 优先从数据库查询分享图片
  - 降级方案：返回当前阶段图片

---

## 🎨 前端文件

### 前端新增文件（0个）
所有前端修改都是对现有文件的改动

### 前端修改文件（3个）

#### 1. utils/api.js
**路径**: `qiandoudou-frontend/utils/api.js`

**修改内容**:
- 新增 `dreamImageAPI` 对象，包含5个方法：
  ```javascript
  dreamImageAPI = {
    getImageConfig(dreamType, itemName),      // 获取配置
    getProgressImages(dreamType, itemName),   // 获取进度图片列表
    getShareImage(dreamType, itemName),       // 获取分享图片
    getActiveByType(dreamType),               // 获取所有配置
    getCompleteImageInfo(dreamType, itemName) // 获取完整信息
  }
  ```
- 添加 `dreamImageAPI` 到 `module.exports`

---

#### 2. pages/car-share/car-share.js
**路径**: `qiandoudou-frontend/pages/car-share/car-share.js`

**修改内容**:
- 导入 `dreamImageAPI`
- 在 `onLoad()` 中调用 `loadShareImage()`
- 实现 `loadShareImage()` 异步方法：
  1. 获取钱包详情，提取 `dreamItemName`
  2. 调用 `dreamImageAPI.getShareImage(1, dreamItemName)`
  3. 设置 `carShareImage` 为返回的URL
  4. 降级方案：使用默认图片

---

#### 3. pages/dream-detail/dream-detail.js
**路径**: `qiandoudou-frontend/pages/dream-detail/dream-detail.js`

**修改内容**:
- 导入 `dreamImageAPI`
- 修改 `getCarImageByProgress()` 为异步方法：
  1. 根据 `dreamType` 确定使用 `itemName` (购物) 或 `destination` (旅游)
  2. 调用 `dreamImageAPI.getProgressImages(dreamType, itemName)`
  3. 根据进度百分比从返回的5张图片中选择
  4. 降级方案：使用本地硬编码的汽车图片

---

## 📊 API 调用示例

### 获取汽车的进度图片列表

```bash
GET http://localhost:8080/dream-image-config/progress-images?dreamType=1&itemName=car
```

**返回**:
```json
{
  "code": 0,
  "message": "获取成功",
  "data": [
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part1.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part2.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part3.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part4.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part5.png"
  ]
}
```

### 获取汽车的分享图片

```bash
GET http://localhost:8080/dream-image-config/share-image?dreamType=1&itemName=car
```

**返回**:
```json
{
  "code": 0,
  "message": "获取成功",
  "data": "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png"
}
```

---

## ✅ 修改检查清单

### 后端
- [x] 创建 DreamImageConfig.java 实体类
- [x] 创建 DreamImageConfigMapper.java 接口
- [x] 创建 DreamImageConfigService.java 接口和实现
- [x] 创建 DreamImageConfigController.java 控制器
- [x] 修改 DreamWalletServiceImpl.java
  - [x] 添加 dreamImageConfigService 依赖注入
  - [x] 修改 getCurrentStageImage() 方法
  - [x] 修改 generateShareImage() 方法

### 前端
- [x] 修改 utils/api.js
  - [x] 新增 dreamImageAPI 对象
  - [x] 添加到 module.exports
- [x] 修改 pages/car-share/car-share.js
  - [x] 导入 dreamImageAPI
  - [x] 实现 loadShareImage() 方法
- [x] 修改 pages/dream-detail/dream-detail.js
  - [x] 导入 dreamImageAPI
  - [x] 修改 getCarImageByProgress() 为异步

### 数据库
- [x] 创建 dream_image_config.sql 脚本
- [x] 包含汽车商品配置
- [x] 包含其他商品配置（可选）
- [x] 包含旅游目的地配置（可选）

---

## 📈 版本信息

- **创建日期**: 2025-10-30
- **状态**: 完成✅
- **后端**: Java Spring Boot + MyBatis Plus
- **前端**: WeChat Mini Program (小程序)
- **数据库**: MySQL 5.7+

---

## 🔧 后续维护

### 添加新商品
在数据库中执行:
```sql
INSERT INTO dream_image_config
(dream_type, item_name, item_display_name, progress_image_base_url, share_image_url, is_active)
VALUES
(1, 'new_item', '新商品', 'https://.../', 'https://.../share.png', 1);
```

### 添加新目的地
```sql
INSERT INTO dream_image_config
(dream_type, item_name, item_display_name, progress_image_base_url, share_image_url, is_active)
VALUES
(2, 'new_city', '新城市', 'https://.../', 'https://.../share.png', 1);
```

### 禁用配置
```sql
UPDATE dream_image_config SET is_active = 0 WHERE item_name = 'old_item';
```

---

