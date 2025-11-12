# 前端优化：关注按钮防抖处理

## 问题描述

原来的关注按钮实现使用了"乐观更新"模式：
1. **立即更新 UI**（按钮从"关注"切换到"已关注"）
2. 然后发送 API 请求到后端
3. 如果失败再回滚

这导致用户快速点击关注/已关注按钮时：
- 按钮会立即切换状态，但后端请求还未完成
- 多个请求会发送到后端，导致并发冲突
- 后端数据库违反唯一约束，报错

## 解决方案

改为"保守更新"模式：
1. **禁用按钮**，防止重复点击（设置 `followLoading` 状态）
2. **发送 API 请求**到后端
3. **后端确认成功后才更新 UI**
4. **显示加载动画**，提供视觉反馈

## 修改内容

### 1. JavaScript 逻辑修改（wallet-detail.js）

#### 添加加载状态
```javascript
data: {
  // ... 其他data
  followLoading: false // 关注按钮加载状态，防止快速点击
}
```

#### 改进 toggleFollow() 函数

**核心变化：**
- 检查 `followLoading` 状态，防止重复点击
- **设置加载状态** → **发送请求** → **成功后才更新 UI**
- 请求完成后清除加载状态

```javascript
toggleFollow() {
  // 1. 防止重复点击
  if (this.data.followLoading) {
    return
  }

  // 2. 设置加载状态（禁用按钮）
  this.setData({
    followLoading: true
  })

  // 3. 发送请求
  const apiCall = isFollowing ?
    walletAPI.unfollowWallet(...) :
    walletAPI.followWallet(...)

  apiCall
    .then(result => {
      // 4. 后端成功后才更新 UI
      this.setData({
        isFollowing: !isFollowing,
        socialStats,
        followLoading: false // 清除加载状态
      })
    })
    .catch(error => {
      // 失败时只清除加载状态，UI 保持不变
      this.setData({
        followLoading: false
      })
    })
}
```

### 2. WXML 修改（wallet-detail.wxml）

添加加载状态检查和禁用事件绑定：

```wxml
<image class="follow-btn-image {{isFollowing ? 'following' : ''}} {{followLoading ? 'loading' : ''}}"
       src="{{...}}"
       bindtap="{{followLoading ? '' : 'toggleFollow'}}"
       style="{{followLoading ? 'opacity: 0.6; cursor: not-allowed;' : ''}}"
       mode="aspectFit" />
```

**关键点：**
- 动态添加 `loading` class
- 绑定事件为空字符串 `''` 时，点击事件不会触发
- 通过 `opacity` 和样式提供视觉反馈

### 3. CSS 样式修改（wallet-detail.wxss）

添加加载状态动画：

```css
/* 加载状态样式 */
.follow-btn-image.loading {
  opacity: 0.6;
  pointer-events: none;
  animation: followLoading 1s infinite;
}

@keyframes followLoading {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 0.4;
  }
}
```

**效果：**
- 按钮闪烁，告诉用户"正在加载"
- `pointer-events: none` 禁用指针事件，双保险防止点击

## 修改文件列表

| 文件 | 修改内容 |
|------|--------|
| `wallet-detail.js` | 添加 `followLoading` 状态，改进 `toggleFollow()` 函数逻辑 |
| `wallet-detail.wxml` | 添加加载状态 class 和事件禁用 |
| `wallet-detail.wxss` | 添加加载动画样式 |

## 用户体验对比

### 修改前（乐观更新）
```
用户点击"关注"
  ↓
立即显示"已关注"（UI 更新）
  ↓
后台发送请求（可能失败）
  ↓
如果失败：恢复为"关注"
```
**问题**：快速点击时，多个请求并发，后端报错

### 修改后（保守更新）
```
用户点击"关注"
  ↓
按钮变灰，不可点击（显示加载中...）
  ↓
后台发送请求
  ↓
请求成功：更新 UI 为"已关注"，恢复按钮可点击
  ↓
请求失败：UI 保持不变，恢复按钮可点击，显示错误信息
```
**优点**：
- ✅ 防止快速重复点击
- ✅ UI 始终与后端状态一致
- ✅ 提供清晰的加载反馈
- ✅ 错误处理更可靠

## 前端部署步骤

1. **代码已修改**，无需手动操作
2. **在微信开发者工具中清空缓存**（项目 → 工具 → 清空所有缓存）
3. **重新编译前端**（如需要）
4. **测试关注功能**

## 测试场景

### 场景 1：正常关注
```
1. 打开社交圈中的某个钱包
2. 点击关注按钮
3. 按钮变灰闪烁，提示"加载中"
4. 2-3 秒后，按钮恢复为"已关注"
5. 显示"关注成功"提示
```

### 场景 2：快速点击（防抖测试）
```
1. 打开钱包详情
2. 快速连续点击关注按钮 10 次
3. 只有第一次点击生效
4. 其他点击被忽略（因为按钮被禁用）
5. 后端只接收 1 个请求（不会报并发错误）
```

### 场景 3：网络延迟
```
1. 打开钱包详情
2. 点击关注
3. 模拟网络延迟（设置→关闭网络 2 秒后恢复）
4. 按钮保持灰色，持续闪烁
5. 网络恢复后，请求完成，按钮更新
```

### 场景 4：取消关注
```
1. 点击"已关注"按钮
2. 按钮变灰闪烁
3. 2-3 秒后，变回"关注"
4. 显示"取消关注成功"提示
```

## 与后端修复的协调

这次前端优化与之前的后端修复（INSERT IGNORE + UPDATE）形成**双重防护**：

| 防护层 | 实现方式 | 作用 |
|------|--------|------|
| **前端** | 防抖 + 禁用按钮 | 阻止快速重复点击 |
| **后端** | INSERT IGNORE + UPDATE | 处理并发冲突 |

即使用户设法绕过前端限制，后端也能正确处理并发请求。

## 性能提升

- **减少无效请求**：快速点击时，只发送 1 个请求而不是 N 个
- **降低服务器压力**：并发请求数减少
- **改善用户体验**：清晰的加载反馈，不会出现闪烁的状态切换

---

**修改日期**：2025-11-05
**修改类型**：前端 UX 优化 + 防抖处理
