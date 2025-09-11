<template>
  <div class="mobile-container">
    <!-- 头部 -->
    <div class="header">
      <div class="header-title">钱兜兜</div>
      <div class="header-actions">
        <el-icon class="notification-icon" @click="showNotifications">
          <Bell />
          <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount }}</span>
        </el-icon>
        <el-dropdown @command="handleUserAction">
          <el-avatar :src="user?.avatar" :size="32">
            {{ user?.nickname?.charAt(0) }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content" :class="{ 'social-mode': currentTab === 'social' }">
      <!-- 钱包页面 -->
      <div v-if="currentTab === 'wallet'" class="wallet-page">
        <!-- 新建钱包按钮 -->
        <div class="create-wallet-section-mobile">
          <button 
            class="create-wallet-btn-mobile"
            @click="$router.push('/create-wallet')"
          >
            <span class="plus-icon">+</span>
            新建钱包
          </button>
        </div>

        <!-- 钱包列表 -->
        <div class="wallet-list" v-loading="loading">
          <div v-if="wallets.length === 0" class="empty-state">
            <el-icon class="icon"><Wallet /></el-icon>
            <div class="text">还没有钱包，快去创建一个吧！</div>
          </div>
          
          <div 
            v-for="wallet in wallets" 
            :key="wallet.id" 
            class="wallet-card"
            :class="{ 'couple': wallet.type === 2 }"
            :style="{ background: getWalletBackground(wallet) }"
            @click="goToWalletDetail(wallet.id)"
          >
            
            <!-- 钱包信息 -->
            <div class="wallet-info">
              <div class="wallet-header">
                <h3 class="wallet-name">{{ wallet.name }}</h3>
                <div class="wallet-type">
                  {{ wallet.type === 1 ? '个人钱包' : '情侣钱包' }}
                </div>
              </div>
              
              <div class="wallet-balance">
                <span class="balance-label">余额</span>
                <span class="balance-amount">¥{{ formatAmount(wallet.balance) }}</span>
              </div>
              
              <!-- AI伴侣信息（情侣钱包） -->
              <div v-if="wallet.type === 2 && wallet.ai_partner_name" class="ai-partner">
                <el-avatar :src="wallet.ai_partner_avatar" :size="24">
                  {{ wallet.ai_partner_name?.charAt(0) }}
                </el-avatar>
                <span class="partner-name">{{ wallet.ai_partner_name }}</span>
              </div>
              

            </div>
          </div>
        </div>
      </div>

      <!-- 社交圈页面 -->
      <div v-if="currentTab === 'social'" class="social-page">
        <Social />
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div 
        class="nav-item" 
        :class="{ active: currentTab === 'wallet' }"
        @click="handleTabClick('wallet')"
      >
        <el-icon class="icon"><Wallet /></el-icon>
        <span class="text">钱兜兜</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: currentTab === 'social' }"
        @click="currentTab = 'social'"
      >
        <el-icon class="icon"><ChatDotSquare /></el-icon>
        <span class="text">兜圈圈</span>
      </div>
    </div>

    <!-- 通知抽屉 -->
    <el-drawer
      v-model="showNotificationDrawer"
      title="消息通知"
      direction="rtl"
      size="80%"
    >
      <div class="notification-list">
        <div v-if="notifications.length === 0" class="empty-state">
          <el-icon class="icon"><Bell /></el-icon>
          <div class="text">暂无新消息</div>
        </div>
        
        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          class="notification-item"
          :class="{ 'unread': !notification.is_read }"
        >
          <div class="notification-content">
            <h4>{{ notification.title }}</h4>
            <p>{{ notification.content }}</p>
            <span class="notification-time">{{ formatTime(notification.create_time) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Social from './Social.vue'

export default {
  name: 'Home',
  components: {
    Social
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    
    const currentTab = ref('wallet')
    const showNotificationDrawer = ref(false)
    const notifications = ref([])
    const unreadCount = ref(0)
    
    // 计算属性
    const user = computed(() => store.state.user)
    const wallets = computed(() => store.state.wallets)
    const loading = computed(() => store.state.loading)
    
    // 背景选项（与钱包详情页保持一致）
    const backgroundOptions = [
      { value: 'gradient1', name: '蓝紫渐变', gradient: 'linear-gradient(135deg, #fa6402 0%, #764ba2 100%)' },
      { value: 'gradient2', name: '粉红渐变', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
      { value: 'gradient3', name: '绿色渐变', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
      { value: 'gradient4', name: '橙色渐变', gradient: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)' },
      { value: 'gradient5', name: '紫色渐变', gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' },
      { value: 'gradient6', name: '金色渐变', gradient: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)' }
    ]
    
    // 获取钱包背景样式
    const getWalletBackground = (wallet) => {
      if (!wallet.background_image) {
        // 默认背景
        return wallet.type === 2 ? 
          'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' : 
          'linear-gradient(135deg, #fa6402 0%, #764ba2 100%)'
      }
      
      // 检查是否是预设渐变
      const bg = backgroundOptions.find(bg => bg.value === wallet.background_image)
      if (bg) {
        return bg.gradient
      }
      
      // 如果是图片URL或base64数据，使用背景图片
      if (wallet.background_image.startsWith('data:') || 
          wallet.background_image.startsWith('http') || 
          wallet.background_image.startsWith('/uploads/')) {
        return `url(${wallet.background_image}) center/cover`
      }
      
      // 默认背景
      return wallet.type === 2 ? 
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' : 
        'linear-gradient(135deg, #fa6402 0%, #764ba2 100%)'
    }
    
    // 格式化金额
    const formatAmount = (amount) => {
      return parseFloat(amount).toFixed(2)
    }
    
    // 格式化时间
    const formatTime = (time) => {
      return new Date(time).toLocaleString()
    }
    
    // 跳转到钱包详情
    const goToWalletDetail = (walletId) => {
      console.log('点击的钱包ID:', walletId, '类型:', typeof walletId)
      console.log('当前钱包列表:', wallets.value)
      const clickedWallet = wallets.value.find(w => w.id == walletId) // 使用 == 而不是 === 避免类型问题
      console.log('点击的钱包信息:', clickedWallet)
      
      // 确保ID作为字符串传递，避免精度丢失
      const idStr = String(walletId)
      console.log('传递给路由的ID字符串:', idStr)
      router.push(`/wallet/${idStr}`)
    }
    
    // 分享钱包
    const shareWallet = (wallet) => {
      ElMessage.success(`分享了钱包：${wallet.name}`)
      // TODO: 实现真实的分享功能
    }
    
    // 显示通知
    const showNotifications = () => {
      showNotificationDrawer.value = true
      // TODO: 加载通知列表
    }
    
    // 处理用户操作
    const handleUserAction = (command) => {
      switch (command) {
        case 'profile':
          ElMessage.info('个人信息功能开发中...')
          break
        case 'logout':
          ElMessageBox.confirm('确定要退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            store.dispatch('logout')
            router.push('/login')
            ElMessage.success('已退出登录')
          })
          break
      }
    }
    
    // 刷新钱包列表
    const refreshWallets = () => {
      console.log('刷新钱包列表')
      store.dispatch('fetchWallets')
    }
    
    // 处理标签页点击
    const handleTabClick = (tab) => {
      currentTab.value = tab
      if (tab === 'wallet') {
        console.log('切换到钱包页面，刷新数据')
        refreshWallets()
      }
    }
    
    // 页面初始化
    onMounted(() => {
      refreshWallets()
      
      // 监听页面可见性变化，当页面重新可见时刷新数据
      const handleVisibilityChange = () => {
        if (!document.hidden) {
          console.log('页面重新可见，刷新钱包数据')
          refreshWallets()
        }
      }
      
      document.addEventListener('visibilitychange', handleVisibilityChange)
      
      // 组件卸载时清理事件监听
      const cleanup = () => {
        document.removeEventListener('visibilitychange', handleVisibilityChange)
      }
      
      return cleanup
    })
    
    return {
      currentTab,
      showNotificationDrawer,
      notifications,
      unreadCount,
      user,
      wallets,
      loading,
      getWalletBackground,
      formatAmount,
      formatTime,
      goToWalletDetail,
      shareWallet,
      showNotifications,
      handleUserAction,
      handleTabClick
    }
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.notification-icon {
  font-size: 20px;
  cursor: pointer;
  position: relative;
  color: #606266;
  transition: color 0.3s;
}

.notification-icon:hover {
  color: #409eff;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #f56c6c;
  color: white;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 80px;
}

.content.social-mode {
  padding-bottom: 0;
}

/* 微信小程序风格的钱包页面 */
.wallet-page {
  padding: 12px;
  background: #f7f7f7;
}

.create-wallet-section-mobile {
  margin-bottom: 16px;
  padding: 0 4px;
}

.create-wallet-btn-mobile {
  width: 100%;
  height: 44px;
  background: #07c160;
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 16px;
  font-weight: 400;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.create-wallet-btn-mobile:active {
  background: #06ad56;
  transform: scale(0.98);
}

.plus-icon {
  font-size: 18px;
  font-weight: 300;
}

/* 微信小程序风格的钱包列表 */
.wallet-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 4px;
}

.wallet-card {
  position: relative;
  border-radius: 12px;
  padding: 16px;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  min-height: 120px;
  background-size: cover;
  background-position: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.wallet-card:active {
  transform: scale(0.98);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

/* 钱包背景现在通过 :style 动态设置 */

.wallet-info {
  position: relative;
  z-index: 1;
}

/* 微信小程序风格的钱包信息 */
.wallet-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.wallet-name {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.wallet-type {
  font-size: 10px;
  background: rgba(255, 255, 255, 0.25);
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 400;
}

/* 微信小程序风格的余额显示 */
.wallet-balance {
  margin-bottom: 12px;
}

.balance-label {
  display: block;
  font-size: 11px;
  opacity: 0.9;
  margin-bottom: 2px;
  font-weight: 400;
}

.balance-amount {
  font-size: 22px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.ai-partner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  font-size: 14px;
}

.partner-name {
  opacity: 0.9;
}

.wallet-actions {
  display: flex;
  justify-content: flex-end;
}

.share-btn {
  color: white !important;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.share-btn:hover {
  opacity: 1;
}

.social-page {
  height: 100%;
}

.notification-list {
  padding: 20px;
}

.notification-item {
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.3s;
}

.notification-item.unread {
  background: #f0f9ff;
  border-left: 3px solid #409eff;
}

.notification-content h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.notification-content p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 414px) {
  .wallet-page {
    padding: 15px;
  }
  
  .wallet-card {
    padding: 15px;
  }
  
  .wallet-name {
    font-size: 16px;
  }
  
  .balance-amount {
    font-size: 20px;
  }
}
</style>
