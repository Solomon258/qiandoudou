// pages/user-profile/user-profile.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    userId: null,
    userInfo: {
      nickname: '用户',
      avatar: '',
      description: '',
      hasCustomAvatar: false
    },
    wallet: {},
    walletBackgroundStyle: '',
    transactions: [],
    activeTab: 'bill', // 'bill' 或 'stats'
    // 社交数据
    socialStats: {
      fansCount: 0,  // 初始值为0，等待从后端加载真实数据
      likesCount: 0, // 初始值为0，等待从后端加载真实数据
      viewsCount: 0  // 初始值为0，等待从后端加载真实数据
    },
    isFollowing: false,
    walletOwnerId: null, // 钱包所有者ID
    backgroundOptions: [
      { value: 'gradient1', name: '蓝紫渐变', gradient: 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);' },
      { value: 'gradient2', name: '粉红渐变', gradient: 'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);' },
      { value: 'gradient3', name: '绿色渐变', gradient: 'background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);' },
      { value: 'gradient4', name: '橙色渐变', gradient: 'background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);' },
      { value: 'gradient5', name: '紫色渐变', gradient: 'background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);' },
      { value: 'gradient6', name: '金色渐变', gradient: 'background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);' }
    ]
  },

  onLoad(options) {
    const userId = options.userId
    const walletId = options.walletId
    
    if (userId && walletId) {
      this.setData({ 
        userId: userId,
        walletId: walletId 
      })
      this.loadUserProfile()
      this.loadWalletDetail()
      this.loadTransactions()
      this.loadSocialStats()
      this.checkFollowStatus() // 检查关注状态
    }
  },

  onShow() {
    // 页面显示时重新加载用户信息（确保头像是最新的）
    this.loadUserProfile()
    
    // 页面显示时更新背景样式和交易记录
    if (this.data.wallet && this.data.wallet.id) {
      this.updateWalletBackgroundStyle()
      this.loadTransactions()
    }
  },

  // 加载用户资料
  loadUserProfile() {
    const userId = parseInt(this.data.userId)

    
    // 首先尝试从本地存储获取用户信息（包括头像）
    const localUserInfo = wx.getStorageSync('userInfo') || app.globalData.userInfo
    
    if (localUserInfo && localUserInfo.avatar) {
      // 如果本地有用户信息且有头像，优先使用
      const userInfo = {
        id: localUserInfo.id || userId,
        nickname: localUserInfo.nickname || '用户',
        avatar: localUserInfo.avatar,
        description: localUserInfo.description || '这个人很懒，什么都没留下',
        hasCustomAvatar: !!(localUserInfo.avatar && localUserInfo.hasCustomAvatar),
        tags: ['成长', '生活']
      }

      this.setData({ userInfo })
      return
    }
    
    // 根据userId使用不同的模拟数据
    const mockUsers = {
      101: {
        id: 101,
        nickname: '宝儿',
        avatar: '',
        description: '一年每天自动存一块已到期（说真的，突然…',
        tags: ['生活', '攒钱']
      },
      102: {
        id: 102,
        nickname: '朱敏',
        avatar: '',
        description: '给发哥攒钱买车',
        tags: ['情感', '校园']
      },
      103: {
        id: 103,
        nickname: '小王',
        avatar: '',
        description: '小王的理财之路',
        tags: ['理财', '成长']
      }
    }
    
    const userInfo = mockUsers[userId] || {
      id: userId,
      nickname: '07年小女生攒钱',
      avatar: '',
      description: '07年小女生从2025.2.1开始存钱 目标…',
      hasCustomAvatar: false,
      tags: ['成长', '生活']
    }

    this.setData({ userInfo })
  },

  // 加载钱包详情
  loadWalletDetail() {
    if (!this.data.walletId) {

      return
    }


    // 根据walletId使用不同的模拟数据
    const mockWallets = {
      1001: {
        id: 1001,
        name: '宝儿的锦鲤小岛😈',
        balance: 2221.21,
        type: 1,
        backgroundImage: 'gradient4',
        description: '一年每天自动存一块已到期（说真的，突然…'
      },
      1002: {
        id: 1002,
        name: '给朱敏攒钱了',
        balance: 21231.21,
        type: 1,
        backgroundImage: 'gradient3',
        description: '给发哥攒钱买车'
      },
      1003: {
        id: 1003,
        name: '小王的存钱计划',
        balance: 5678.90,
        type: 1,
        backgroundImage: 'gradient2',
        description: '小王的理财之路'
      }
    }

    const walletId = parseInt(this.data.walletId)
    const mockWallet = mockWallets[walletId] || {
      id: walletId,
      name: '07年小女生攒钱💗',
      balance: 31123.12,
      type: 1,
      backgroundImage: 'gradient1',
      description: '07年小女生从2025.2.1开始存钱 目标…'
    }

    
    this.setData({ wallet: mockWallet })
    this.updateWalletBackgroundStyle()
  },

  // 加载交易记录
  loadTransactions() {
    if (!this.data.walletId) return

    // 使用模拟数据，实际应该调用API
    const mockTransactions = [
      {
        id: 1,
        type: 'expense',
        amount: -25.00,
        description: '剁手的手在学习',
        category: 'App Store & Apple Music；08.24至08.25期间购买',
        date: '昨天 07:21',
        avatar: '',
        likes: 20,
        comments: 12,
        userComment: {
          username: '昆虫记',
          message: '加油'
        }
      },
      {
        id: 2,
        type: 'expense',
        amount: -68.12,
        description: '剁手的手在学习',
        category: '经营码交易',
        date: '08-22 04:12',
        avatar: '',
        likes: 20,
        comments: 12,
        userComment: {
          username: '小王在吃土',
          message: '来啦来啦'
        }
      }
    ]

    this.setData({ transactions: mockTransactions })
  },

  // 加载社交统计数据
  loadSocialStats() {
    const walletId = parseInt(this.data.walletId)

    
    // 根据钱包ID生成固定的社交数据（与social页面保持一致）
    // 移除硬编码的测试数据，统一使用真实的0值
    const defaultStats = { fansCount: 0, likesCount: 0, viewsCount: 0 }
    const socialStats = defaultStats

    
    this.setData({ 
      socialStats: socialStats,
      isFollowing: false // 默认未关注状态
    })
  },

  // 更新钱包背景样式
  updateWalletBackgroundStyle() {
    const wallet = this.data.wallet
    if (!wallet) return

    let backgroundStyle = this.getWalletBackground(wallet)
    this.setData({ walletBackgroundStyle: backgroundStyle })
  },

  // 获取钱包背景样式
  getWalletBackground(wallet) {
    const backgroundOptions = {
      'gradient1': 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);',
      'gradient2': 'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);',
      'gradient3': 'background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);',
      'gradient4': 'background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);',
      'gradient5': 'background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);',
      'gradient6': 'background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);'
    }

    // 获取背景设置（兼容不同的字段名）
    const currentBackground = wallet.backgroundImage || wallet.background_image || 'gradient1'

    if (currentBackground) {
      if (currentBackground.startsWith('custom_bg_')) {
        // 自定义图片背景
        const customImages = wx.getStorageSync('custom_images') || {}
        const imagePath = customImages[currentBackground]
        if (imagePath) {
          return `background-image: url('${imagePath}'); background-size: cover; background-position: center;`
        }
      } else if (backgroundOptions[currentBackground]) {
        // 预设渐变背景
        return backgroundOptions[currentBackground]
      } else if (currentBackground.startsWith('http') || currentBackground.startsWith('/')) {
        // 网络图片或本地路径
        return `background-image: url('${currentBackground}'); background-size: cover; background-position: center;`
      }
    }

    // 默认背景
    return wallet.type === 2 ? 
      'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);' : 
      'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'
  },

  // 切换标签页
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ activeTab: tab })
  },

  // 返回上一页
  goBack() {
    wx.navigateBack()
  },

  // 关注/取消关注
  toggleFollow() {
    const currentFollowing = this.data.isFollowing
    const newFollowing = !currentFollowing
    const currentUserId = app.globalData.userInfo?.id
    const walletId = this.data.walletId
    
    if (!currentUserId) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }

    
    // 先获取钱包所有者ID，然后进行关注操作
    walletAPI.getWalletOwnerId(walletId)
      .then(result => {
        const walletOwnerId = result.data
        
        if (!walletOwnerId) {
          throw new Error('钱包不存在')
        }
        
        if (walletOwnerId === currentUserId) {
          throw new Error('不能关注自己的钱包')
        }
        
        // 先更新本地状态
        this.setData({ isFollowing: newFollowing })
        
        // 调用后端API
        const apiCall = newFollowing ? 
          walletAPI.followWallet(currentUserId, walletId) : 
          walletAPI.unfollowWallet(currentUserId, walletId)
        
        return apiCall
      })
      .then(result => {

        
        // 重新加载社交统计数据
        this.loadSocialStats()
        
        // 显示操作反馈
        wx.showToast({
          title: newFollowing ? '关注成功' : '已取消关注',
          icon: 'success',
          duration: 1500
        })
      })
      .catch(error => {

        
        // 恢复之前的状态
        this.setData({ isFollowing: currentFollowing })
        
        // 处理特殊错误消息
        let errorMessage = error.message || (newFollowing ? '关注失败' : '取消关注失败')
        if (errorMessage.includes('已关注该用户')) {
          errorMessage = '已经关注了该用户'
          // 如果后端说已关注，那就设置为已关注状态
          this.setData({ isFollowing: true })
        } else if (errorMessage.includes('未关注该用户')) {
          errorMessage = '尚未关注该用户'
          // 如果后端说未关注，那就设置为未关注状态
          this.setData({ isFollowing: false })
        }
        
        wx.showToast({
          title: errorMessage,
          icon: 'none'
        })
      })
  },

  // 检查关注状态
  checkFollowStatus() {
    const currentUserId = app.globalData.userInfo?.id
    const walletId = this.data.walletId
    
    if (!currentUserId || !walletId) {
      this.setData({ isFollowing: false })
      return
    }
    
    // 先获取钱包所有者ID
    walletAPI.getWalletOwnerId(walletId)
      .then(result => {
        const walletOwnerId = result.data
        
        if (!walletOwnerId || walletOwnerId === currentUserId) {
          // 如果是自己的钱包，不显示关注按钮
          this.setData({ isFollowing: false })
          return
        }
        
        // 保存钱包所有者ID，供后续使用
        this.setData({ walletOwnerId: walletOwnerId })
        
        // 检查是否已关注该用户
        return walletAPI.checkFollowStatus(currentUserId, walletOwnerId)
      })
      .then(result => {
        if (result) {
          const isFollowing = result.data || false
          this.setData({ isFollowing })

        }
      })
      .catch(error => {

        this.setData({ isFollowing: false })
      })
  },

  // 点赞交易记录
  likeTransaction(e) {
    const transactionId = e.currentTarget.dataset.id
    const index = e.currentTarget.dataset.index
    
    // 这里应该调用API来点赞
    // 暂时只更新本地状态
    const transactions = this.data.transactions
    transactions[index].likes = (transactions[index].likes || 0) + 1
    
    this.setData({ transactions })

    wx.showToast({
      title: '点赞成功',
      icon: 'success',
      duration: 1000
    })
  },

  // 评论交易记录
  commentTransaction(e) {
    const transactionId = e.currentTarget.dataset.id
    
    wx.showToast({
      title: '评论功能开发中',
      icon: 'none',
      duration: 1500
    })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  }
})
