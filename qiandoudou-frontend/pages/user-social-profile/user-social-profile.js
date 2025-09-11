// pages/user-social-profile/user-social-profile.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    userInfo: {
      nickname: '昆虫记',
      avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png',
      description: '每100个粉丝，打卡一个5A级景区'
    },
    socialStats: {
      followingCount: 0,
      fansCount: 0,
      likesCount: 0
    },
    publicWallets: [],
    followedWallets: [],
    loading: true,
    userId: null,
    firstWallet: null // 添加第一个钱包信息
  },

  onLoad(options) {
    // 获取用户ID参数（如果有的话）
    const userId = options.userId || app.globalData.userInfo?.id
    if (userId) {
      this.setData({ userId })
      this.loadUserData(userId)
    } else {
      wx.showToast({
        title: '用户信息获取失败',
        icon: 'none'
      })
    }
    this.loadUserInfo()
  },

  onShow() {
    // 页面显示时重新加载用户信息，确保头像等信息是最新的
    this.loadUserInfo()
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo') || app.globalData.userInfo
    
    if (userInfo) {
      // 有本地用户信息，直接使用
      const displayUserInfo = {
        nickname: userInfo.nickname || '钱兜兜用户',
        avatar: userInfo.avatar || 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png',
        description: userInfo.description || '这个人很懒，什么都没留下',
        hasCustomAvatar: !!(userInfo.avatar && userInfo.hasCustomAvatar)
      }
      
      this.setData({
        userInfo: displayUserInfo
      })
    } else {
      // 本地用户信息为空，尝试从后端获取
      this.loadUserInfoFromServer()
    }
  },

  // 从服务器加载用户信息
  loadUserInfoFromServer() {
    const { authAPI } = require('../../utils/api.js')
    
    // 获取当前用户ID，如果没有用户ID则不加载
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      return
    }
    
    authAPI.getCurrentUser(userId)
      .then(result => {
        const serverUserInfo = result.data
        
        // 设置用户信息
        const displayUserInfo = {
          id: serverUserInfo.id || 1,
          nickname: serverUserInfo.nickname || '钱兜兜用户',
          avatar: serverUserInfo.avatar || 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png',
          description: serverUserInfo.description || '这个人很懒，什么都没留下',
          hasCustomAvatar: !!(serverUserInfo.avatar && serverUserInfo.avatar.startsWith('http'))
        }
        
        this.setData({
          userInfo: displayUserInfo
        })
        
        // 同步到本地存储和全局数据
        wx.setStorageSync('userInfo', displayUserInfo)
        app.globalData.userInfo = displayUserInfo
      })
      .catch(error => {
        
        // 使用默认用户信息
        const defaultUserInfo = {
          id: 1,
          nickname: '钱兜兜用户',
          avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png',
          description: '这个人很懒，什么都没留下',
          hasCustomAvatar: false
        }
        
        this.setData({
          userInfo: defaultUserInfo
        })
      })
  },

  // 加载用户数据
  loadUserData(userId) {
    this.setData({ loading: true })
    
    // 并行加载公开钱包和关注钱包
    Promise.all([
      this.loadPublicWallets(userId),
      this.loadFollowedWallets(userId),
      this.loadSocialStats(userId)
    ]).then(() => {
      this.setData({ loading: false })
    }).catch(error => {
      this.setData({ loading: false })
      wx.showToast({
        title: '数据加载失败',
        icon: 'none'
      })
    })
  },

  // 加载用户的公开钱包
  loadPublicWallets(userId) {
    return walletAPI.getUserWallets(userId)
      .then(result => {
        const wallets = result.data || []
        
        // 保存第一个钱包信息用于头像显示
        if (wallets.length > 0) {
          this.setData({ firstWallet: wallets[0] })
        }
        
        // 假设所有钱包都是公开的，后续可以根据实际情况过滤
        const publicWallets = wallets.map(wallet => {
          return {
            id: wallet.id,
            title: wallet.name || '未命名钱包',
            description: wallet.description || '暂无描述',
            amount: (wallet.balance || 0).toFixed(2),
            fansCount: wallet.fansCount || 0,
            likesCount: wallet.likesCount || 0,
            viewsCount: wallet.viewsCount || 0,
            backgroundStyle: this.getWalletBackground(wallet)
          }
        })
        
        this.setData({ publicWallets })
        return publicWallets
      })
      .catch(error => {
        return []
      })
  },

  // 加载用户关注的钱包
  loadFollowedWallets(userId) {
    return walletAPI.getUserFollowedWallets(userId)
      .then(result => {
        const wallets = result.data || []
        const followedWallets = wallets.map(wallet => {
          return {
            id: wallet.id,
            title: wallet.name || '未命名钱包',
            amount: (wallet.balance || 0).toFixed(2),
            participantCount: wallet.participantCount || 1,
            backgroundStyle: this.getWalletBackground(wallet),
            ownerNickname: wallet.owner_nickname || '用户',
            fansCount: wallet.followers_count || 0,
            likesCount: wallet.likes_count || 0,
            viewsCount: wallet.views_count || 0
          }
        })
        
        this.setData({ followedWallets })
        return followedWallets
      })
      .catch(error => {
        
        // API失败时显示空列表
        this.setData({ followedWallets: [] })
        return []
      })
  },

  // 加载社交统计数据
  loadSocialStats(userId) {
    // 这里可以调用API获取真实的社交统计数据
    // 暂时使用模拟数据，后续可以根据实际API调整
    
    // 等待钱包数据加载完成后再计算统计信息
    setTimeout(() => {
      const followedWalletsCount = this.data.followedWallets.length
      const publicWalletsCount = this.data.publicWallets.length
      
      this.setData({
        socialStats: {
          followingCount: followedWalletsCount,
          fansCount: 0, // 使用真实数据，新用户为0
          likesCount: 0  // 使用真实数据，新用户为0
        }
      })
    }, 100)
    
    return Promise.resolve()
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

    // 获取背景设置
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

  // 返回上一页
  goBack() {
    wx.navigateBack()
  },

  // 编辑资料
  editProfile() {
    wx.navigateTo({
      url: '/pages/edit-profile/edit-profile'
    })
  },

  // 点击公开钱包卡片
  onPublicWalletTap(e) {
    const walletId = e.currentTarget.dataset.walletId
    const wallet = this.data.publicWallets.find(w => w.id === walletId)
    
    if (wallet) {
      wx.navigateTo({
        url: `/pages/wallet-detail/wallet-detail?id=${walletId}&isOthers=true`
      })
    }
  },

  // 点击关注的钱包卡片
  onFollowedWalletTap(e) {
    const walletId = e.currentTarget.dataset.walletId
    const wallet = this.data.followedWallets.find(w => w.id === walletId)
    
    if (wallet) {
      // 构建完整的跳转URL，传递必要的社交信息以访问真实数据
      // 确保新钱包的社交数据为0，不传递任何可能的模拟数据
      const url = `/pages/wallet-detail/wallet-detail?id=${walletId}&fromSocial=true&ownerNickname=${encodeURIComponent(wallet.ownerNickname || '用户')}&title=${encodeURIComponent(wallet.title)}&fansCount=0&likeCount=0&viewsCount=0`
      
      wx.navigateTo({
        url: url
      })
    }
  },

  // 查看最近关注
  viewRecentFollows() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    })
  }
})
