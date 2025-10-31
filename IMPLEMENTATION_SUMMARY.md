# 梦想攒图片配置表实现总结

## 一、SQL表结构和数据

### 1. 创建表 (dream_image_config.sql)

```sql
-- 梦想攒图片配置表
-- 用于存放梦想攒（购物和旅游）所有需要的图片地址
CREATE TABLE `dream_image_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dream_type` TINYINT NOT NULL COMMENT '梦想类型：1-购物，2-旅游',
  `item_name` VARCHAR(100) NOT NULL COMMENT '商品或目的地名称（如：car、suv、bag、beijing等）',
  `item_display_name` VARCHAR(100) NOT NULL COMMENT '商品或目的地显示名称（如：轿车、北京）',
  `progress_image_base_url` VARCHAR(500) NOT NULL COMMENT '进度图片URL前缀（如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/）',
  `share_image_url` VARCHAR(500) NOT NULL COMMENT '分享图片完整URL（如：https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png）',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_name` (`dream_type`, `item_name`, `deleted`),
  KEY `idx_dream_type` (`dream_type`),
  KEY `idx_item_name` (`item_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='梦想攒图片配置表';
```

### 2. 汽车商品的INSERT语句

```sql
-- 汽车商品
INSERT INTO `dream_image_config` (
  `dream_type`,
  `item_name`,
  `item_display_name`,
  `progress_image_base_url`,
  `share_image_url`,
  `is_active`
) VALUES (
  1,
  'car',
  '轿车',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/',
  'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png',
  1
);
```

#### 图片文件说明：

- **进度图片**（5张）：
  - `part1.png` - 0-20% 阶段
  - `part2.png` - 20-40% 阶段
  - `part3.png` - 40-60% 阶段
  - `part4.png` - 60-80% 阶段
  - `part5.png` - 80-100% 阶段

- **分享图片**（1张）：
  - `share.png` - 用户点击分享时显示的背景图片

---

## 二、后端代码修改

### 1. 新增Entity类

**文件**: `DreamImageConfig.java`
- 位置: `qiandoudou-backend/src/main/java/com/qiandoudou/entity/`
- 用途: 梦想攒图片配置实体类

### 2. 新增Mapper接口

**文件**: `DreamImageConfigMapper.java`
- 位置: `qiandoudou-backend/src/main/java/com/qiandoudou/mapper/`
- 主要方法: `selectByTypeAndName(Integer dreamType, String itemName)`

### 3. 新增Service接口和实现

**接口文件**: `DreamImageConfigService.java`
- 位置: `qiandoudou-backend/src/main/java/com/qiandoudou/service/`
- 主要方法:
  - `getByTypeAndName(Integer dreamType, String itemName)` - 根据梦想类型和商品名称查询配置
  - `getProgressImages(Integer dreamType, String itemName)` - 获取5张进度图片URL列表
  - `getShareImage(Integer dreamType, String itemName)` - 获取分享图片URL
  - `getActiveByType(Integer dreamType)` - 获取该梦想类型的所有启用配置

**实现文件**: `DreamImageConfigServiceImpl.java`
- 位置: `qiandoudou-backend/src/main/java/com/qiandoudou/service/impl/`
- 功能:
  - 自动拼接进度图片URL（part1.png、part2.png等）
  - 返回分享图片URL
  - 缓存支持（可选）

### 4. 新增Controller

**文件**: `DreamImageConfigController.java`
- 位置: `qiandoudou-backend/src/main/java/com/qiandoudou/controller/`
- 路由前缀: `/dream-image-config`
- 主要端点:
  - `GET /dream-image-config/get?dreamType=1&itemName=car` - 获取图片配置
  - `GET /dream-image-config/progress-images?dreamType=1&itemName=car` - 获取进度图片列表
  - `GET /dream-image-config/share-image?dreamType=1&itemName=car` - 获取分享图片
  - `GET /dream-image-config/list/{dreamType}` - 获取梦想类型的所有配置
  - `GET /dream-image-config/complete-info?dreamType=1&itemName=car` - 获取完整信息

### 5. 修改DreamWalletServiceImpl

**修改内容**:
- 添加 `DreamImageConfigService` 依赖注入
- 修改 `getCurrentStageImage()` 方法：
  - 优先从数据库配置获取进度图片
  - 根据 `dreamItemName` (购物) 或 `dreamDestination` (旅游) 查询图片
  - 降级方案：返回商品图片或空值

- 修改 `generateShareImage()` 方法：
  - 优先从数据库配置获取分享图片
  - 降级方案：返回当前阶段图片

---

## 三、前端代码修改

### 1. 修改API层 (utils/api.js)

**新增对象**: `dreamImageAPI`

```javascript
const dreamImageAPI = {
  getImageConfig(dreamType, itemName) { },
  getProgressImages(dreamType, itemName) { },
  getShareImage(dreamType, itemName) { },
  getActiveByType(dreamType) { },
  getCompleteImageInfo(dreamType, itemName) { }
}
```

**导出**: 添加到 `module.exports` 中

### 2. 修改car-share.js

**变更**:
- 导入 `dreamImageAPI`
- 在 `loadShareImage()` 中调用后端API获取分享图片
- 根据 `dreamItemName` 查询对应的分享图片
- 添加降级方案处理

### 3. 修改dream-detail.js

**变更**:
- 导入 `dreamImageAPI`
- 修改 `getCarImageByProgress()` 方法为异步：
  - 优先从后端API加载进度图片列表
  - 根据进度百分比选择对应的图片
  - 降级到本地硬编码图片
  - 支持购物梦想 (itemName) 和旅游梦想 (destination)

---

## 四、工作流程

### 梦想详情页加载图片流程：

```
1. 加载梦想钱包详情
   ↓
2. 获取 dreamType 和 itemName/destination
   ↓
3. 调用 getCarImageByProgress(progress) 异步方法
   ↓
4. 优先调用后端 API: GET /dream-image-config/progress-images
   ↓
5. 根据进度百分比从返回的图片列表中选择对应图片
   ↓
6. 如果API失败，降级到本地硬编码图片
```

### 分享页面加载图片流程：

```
1. car-share 页面加载
   ↓
2. 获取钱包详情，提取 dreamItemName
   ↓
3. 调用后端 API: GET /dream-image-config/share-image?dreamType=1&itemName=car
   ↓
4. 显示返回的分享图片 URL
   ↓
5. 如果API失败，使用默认图片
```

---

## 五、数据库查询示例

### 查询汽车的进度图片URL前缀：

```sql
SELECT progress_image_base_url FROM dream_image_config
WHERE dream_type = 1 AND item_name = 'car' AND is_active = 1;
```

结果: `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/`

后端自动补全为:
- `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part1.png`
- `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part2.png`
- `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part3.png`
- `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part4.png`
- `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/part5.png`

### 查询汽车的分享图片URL：

```sql
SELECT share_image_url FROM dream_image_config
WHERE dream_type = 1 AND item_name = 'car' AND is_active = 1;
```

结果: `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/goods/car/share.png`

---

## 六、新增功能特性

✅ **图片配置集中管理**: 所有梦想攒图片地址在数据库中集中管理，无需修改代码即可更新

✅ **支持多商品/目的地**: 购物梦想支持车、包、手表等多种商品；旅游梦想支持多个目的地

✅ **灵活的图片规则**:
- 进度图片: 5张，自动根据进度选择
- 分享图片: 1张，用户分享时显示

✅ **降级保障**: 前后端都有降级方案，API失败时使用本地配置

✅ **易于扩展**: 添加新商品只需在表中插入新行，无需修改代码

---

## 七、后续扩展建议

1. **添加其他购物商品**:
   ```sql
   INSERT INTO dream_image_config VALUES (...) for 'suv', 'bag', 'rolex' etc.
   ```

2. **添加其他旅游目的地**:
   ```sql
   INSERT INTO dream_image_config VALUES (...) for 'beijing', 'xian', 'lasa' etc.
   ```

3. **缓存优化**: 在 `DreamImageConfigServiceImpl` 中添加缓存，避免频繁数据库查询

4. **CDN优化**: 所有图片统一使用OSS/CDN服务

5. **管理后台**: 开发管理页面，支持配置的增删改查

---

## 八、测试检查清单

- [ ] 表创建成功，能正常执行插入语句
- [ ] 后端项目编译通过
- [ ] 前端项目编译通过
- [ ] 梦想详情页正确显示进度图片
- [ ] 分享页面正确显示分享图片
- [ ] API降级方案正常工作
- [ ] 新增商品配置后，前端能正确加载图片

