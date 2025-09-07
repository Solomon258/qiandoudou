// pages/home/home.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    userInfo: {},
    wallets: [],
    transactions: [],
    posts: [],
    currentTab: 'wallet',
    loading: false,
    shouldRefresh: false,
    showWalletTypeModal: false,
    selectedWalletType: '',
    isFirstTimeUser: false, // 是否为首次使用用户
    unreadMessageCount: 0 // 未读消息数量
  },

  onLoad() {
    // 检查登录状态
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    this.setData({
      userInfo: app.globalData.userInfo
    })

    // 加载钱包数据
    this.loadData()
    
    // 加载未读消息数量
    this.loadUnreadMessageCount()
  },

  onShow() {
    // 每次显示页面时刷新数据
    if (app.isLoggedIn()) {
      console.log('首页显示，刷新数据')
      this.loadData()
      
      // 强制刷新钱包列表以获取最新的背景设置
      this.loadWallets()
      
      // 强制刷新社交数据
      console.log('强制刷新社交数据')
      this.loadPosts()
      
      // 加载未读消息数量
      this.loadUnreadMessageCount()
    }
    
    // 重置刷新标记
    this.setData({ shouldRefresh: false })
  },

  // 加载数据
  loadData() {
    if (this.data.currentTab === 'wallet') {
      this.loadWallets()
      // 延迟加载交易记录，优化首次加载速度
      setTimeout(() => {
        this.loadTransactions()
      }, 500)
    }
    // 延迟加载社交数据，优化首次加载速度
    setTimeout(() => {
      if (this.data.currentTab === 'social') {
        this.loadPosts()
      }
    }, 1000)
  },

  // 加载钱包列表
  loadWallets() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      console.error('用户ID不存在')
      return
    }

    this.setData({ loading: true })

    walletAPI.getUserWallets(userId)
      .then(result => {
        const wallets = result.data || []
        
        // 检查是否为新用户（没有钱包）
        if (wallets.length === 0) {
          console.log('新用户没有钱包，显示欢迎界面和选择弹框')
          this.setData({
            loading: false,
            isFirstTimeUser: true,
            showWalletTypeModal: true
          })
          
          // 给新用户一个友好的提示
          setTimeout(() => {
            wx.showToast({
              title: '欢迎！请创建您的第一个钱兜兜',
              icon: 'none',
              duration: 3000
            })
          }, 500)
          return
        }
        
        // 为每个钱包计算背景样式
        const walletsWithBackground = wallets.map(wallet => {
          const backgroundStyle = this.getWalletBackground(wallet)
          return {
            ...wallet,
            backgroundStyle: backgroundStyle
          }
        })
        
        this.setData({
          wallets: walletsWithBackground,
          loading: false,
          isFirstTimeUser: false
        })
        
        // 如果当前在社交页面，重新加载动态数据以使用真实钱包ID
        if (this.data.currentTab === 'social') {
          this.loadPosts()
        }
      })
      .catch(error => {
        console.error('加载钱包失败:', error)
        wx.showToast({
          title: '加载钱包失败',
          icon: 'none'
        })
        this.setData({ loading: false })
      })
  },

  // 加载交易记录
  loadTransactions() {
    // 获取最新的几笔交易记录用于首页显示
    const wallets = this.data.wallets
    if (wallets.length === 0) {
      return
    }

    // 获取第一个钱包的交易记录作为首页展示
    const firstWallet = wallets[0]
    if (firstWallet) {
      walletAPI.getWalletTransactions(firstWallet.id)
        .then(result => {
          const transactions = result.data || []
          
          // 格式化交易记录
          const formattedTransactions = transactions.slice(0, 5).map(transaction => ({
            ...transaction,
            createTime: this.formatTime(new Date(transaction.createTime).getTime()),
            amount: parseFloat(transaction.amount).toFixed(2),
            type: transaction.type === 1 ? 'INCOME' : 'EXPENSE'
          }))
          
          this.setData({
            transactions: formattedTransactions
          })
        })
        .catch(error => {
          console.error('加载交易记录失败:', error)
          // 如果加载失败，显示空数组
          this.setData({
            transactions: []
          })
        })
    }
  },

  // 格式化时间
  formatTime(timestamp) {
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now - date

    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      return `${Math.floor(diff / 60000)}分钟前`
    } else if (diff < 86400000) { // 1天内
      return `${Math.floor(diff / 3600000)}小时前`
    } else {
      return `${date.getMonth() + 1}月${date.getDate()}日`
    }
  },

  // 点击钱包
  handleWalletTap(e) {
    const walletId = e.currentTarget.dataset.id
    // 用户点击自己的钱包，跳转到功能完整的钱包详情页
    wx.navigateTo({
      url: `/pages/wallet-detail/wallet-detail?id=${walletId}`
    })
  },





  // 创建钱包
  handleCreateWallet() {
    this.setData({
      showWalletTypeModal: true,
      selectedWalletType: ''
    })
  },

  // 隐藏钱包类型模态框
  hideWalletTypeModal() {
    // 直接关闭弹框，回到钱兜兜列表页
    this.setData({
      showWalletTypeModal: false,
      selectedWalletType: '',
      isFirstTimeUser: false  // 重置首次用户标记，让用户可以正常使用
    })
  },

  // 选择钱包类型
  selectWalletType(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      selectedWalletType: type
    })
  },

  // 确认创建钱包
  confirmCreateWallet() {
    const { selectedWalletType } = this.data
    
    if (!selectedWalletType) {
      wx.showToast({
        title: '请选择钱包类型',
        icon: 'error'
      })
      return
    }

    if (selectedWalletType === 'personal') {
      // 自己攒钱 - 直接创建并进入钱包详情页
      this.createPersonalWallet()
    } else if (selectedWalletType === 'couple') {
      // 情侣攒钱 - 跳转到情侣攒钱选择页面
      this.hideWalletTypeModal()
      wx.navigateTo({
        url: '/pages/couple-savings/couple-savings'
      })
    }
  },

  // 创建个人钱包
  createPersonalWallet() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    // 为新用户创建钱包时使用更友好的名称
    const walletName = this.data.isFirstTimeUser ? 
      '我的第一个钱兜兜' : 
      `我的钱包${this.data.wallets.length + 1}`
    
    walletAPI.createWallet(userId, walletName, 1, 'gradient1', null)
      .then(result => {
        const newWallet = result.data
        
        // 隐藏模态框并重置首次用户标记
        this.setData({
          showWalletTypeModal: false,
          selectedWalletType: '',
          isFirstTimeUser: false
        })

        // 刷新钱包列表
        this.loadWallets()

        // 直接进入新创建的钱包详情页
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/wallet-detail/wallet-detail?id=${newWallet.id}`
          })
        }, 300)

        wx.showToast({
          title: '钱包创建成功',
          icon: 'success'
        })
      })
      .catch(error => {
        console.error('创建钱包失败:', error)
        wx.showToast({
          title: error.message || '创建钱包失败',
          icon: 'none'
        })
      })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  },

  // 强制刷新钱包列表（供其他页面调用）
  forceRefreshWallets() {
    console.log('强制刷新钱包列表')
    this.loadWallets()
  },

  // 强制刷新社交数据（供调试使用）
  forceRefreshSocial() {
    console.log('强制刷新社交数据')
    this.loadPosts()
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



  // 跳转到用户个人社交圈主页
  navigateToUserSocialProfile() {
    console.log('🔥 从首页点击头像跳转到个人社交圈主页');
    wx.navigateTo({
      url: '/pages/user-social-profile/user-social-profile'
    });
  },

  // 处理个人资料（保留原方法）
  handleProfile() {
    wx.showToast({
      title: '个人资料功能开发中',
      icon: 'none'
    })
  },

  // 分享钱包
  shareWallet(e) {
    const wallet = e.currentTarget.dataset.wallet
    if (wallet) {
      wx.showShareMenu({
        withShareTicket: true,
        success: () => {
          wx.showToast({
            title: `分享了${wallet.name}手账`,
            icon: 'success'
          })
        },
        fail: () => {
          wx.showToast({
            title: `分享了${wallet.name}手账`,
            icon: 'success'
          })
        }
      })
    }
  },

  // 加载社交动态（公开钱包）
  loadPosts() {
    console.log('开始加载兜圈圈公开钱包数据...')
    console.log('当前登录状态:', app.isLoggedIn())
    console.log('当前用户信息:', app.globalData.userInfo)
    console.log('当前token:', app.globalData.token ? '存在' : '不存在')
    
    walletAPI.getPublicWallets()
      .then(result => {
        console.log('API返回的完整结果:', result)
        const publicWallets = result.data || []
        console.log('获取到的公开钱包数量:', publicWallets.length)
        console.log('获取到的公开钱包原始数据:', publicWallets)
        
        // 检查是否有数据
        if (!publicWallets || publicWallets.length === 0) {
          console.log('没有获取到公开钱包数据')
          this.setData({ posts: [] })
          return
        }
        
        // 将公开钱包数据转换为兜圈圈显示格式
        const socialPosts = publicWallets.map((wallet, index) => {
          console.log(`处理钱包${index + 1}:`, wallet)
          
          // 解析最新交易记录
          let recentTransactions = []
          try {
            if (wallet.recent_transactions) {
              recentTransactions = typeof wallet.recent_transactions === 'string' 
                ? JSON.parse(wallet.recent_transactions) 
                : wallet.recent_transactions
            }
          } catch (e) {
            console.log('解析交易记录失败:', e, '原始数据:', wallet.recent_transactions)
            recentTransactions = []
          }
          
          // 确保recentTransactions是数组
          if (!Array.isArray(recentTransactions)) {
            console.log('recentTransactions不是数组，重置为空数组:', recentTransactions)
            recentTransactions = []
          }
          
          console.log(`钱包${index + 1}的交易记录:`, recentTransactions)
          
          // 处理钱包类型（可能是布尔值或数字）
          const walletType = wallet.type === true || wallet.type === 'true' || wallet.type === 2 ? 2 : 1
          
          // 构建社交动态数据
          const socialPost = {
            id: wallet.id,
            wallet_id: wallet.id,
            title: wallet.name || '未命名钱包',
            owner_nickname: wallet.owner_nickname || '匿名用户',
            total_amount: parseFloat(wallet.balance || 0).toFixed(2),
            tags: walletType === 2 ? ['情感', '情侣', wallet.ai_partner_name || 'AI伴侣'] : ['生活', '攒钱', '个人'],
            description: this.generateWalletDescription({...wallet, type: walletType}, recentTransactions),
            backgroundStyle: this.getWalletBackground({
              ...wallet,
              type: walletType,
              backgroundImage: wallet.backgroundImage || wallet.background_image
            }),
            fansCount: 0, // 真实粉丝数，暂时设为0，后续可从后端获取
            participantCount: recentTransactions.length,
            like_count: 0, // 真实点赞数，暂时设为0，后续可从后端获取  
            comment_count: recentTransactions.length,
            is_liked: false,
            recent_transactions: recentTransactions.slice(0, 2).map(transaction => ({
              id: transaction.id,
              description: transaction.description || '无描述',
              amount: parseFloat(transaction.amount || 0).toFixed(2),
              type: transaction.type,
              user_nickname: wallet.owner_nickname || '匿名用户',
              comment: transaction.note || transaction.description || '无备注',
              create_time: this.formatTime(transaction.create_time)
            }))
          }
          
          console.log(`钱包${index + 1}转换后的数据:`, socialPost)
          return socialPost
        }).filter(post => post && post.id) // 过滤掉无效的钱包数据
        
        console.log('转换后的社交动态数据:', socialPosts)
        console.log('有效钱包数量:', socialPosts.length)
        
        if (socialPosts.length === 0) {
          console.log('没有有效的公开钱包数据')
          this.setData({ posts: [] })
          return
        }
        
        console.log('每个钱包的详细信息:')
        socialPosts.forEach((post, index) => {
          console.log(`钱包${index + 1}:`, {
            id: post.id,
            title: post.title,
            owner: post.owner_nickname,
            balance: post.total_amount,
            type: post.tags,
            description: post.description,
            transactions: post.recent_transactions.length
          })
        })
        this.setData({ posts: socialPosts })
      })
      .catch(error => {
        console.error('加载公开钱包失败:', error)
        console.error('错误详情:', error.message)
        
        // 显示错误提示
        wx.showToast({
          title: '加载兜圈圈数据失败',
          icon: 'none'
        })
        
        // 如果API失败，显示空状态
        this.setData({ posts: [] })
      })
  },

  // 生成钱包描述
  generateWalletDescription(wallet, transactions) {
    console.log('生成钱包描述 - 钱包:', wallet, '交易:', transactions)
    
    if (transactions && Array.isArray(transactions) && transactions.length > 0) {
      const latestTransaction = transactions[0]
      const typeText = latestTransaction.type === 1 ? '转入' : (latestTransaction.type === 2 ? '转出' : '剧本攒钱')
      const amount = parseFloat(latestTransaction.amount || 0).toFixed(2)
      const description = latestTransaction.description || '无描述'
      return `最新${typeText} ¥${amount} - ${description}`
    }
    
    // 根据钱包类型和AI伴侣信息生成描述
    const walletType = wallet.type === true || wallet.type === 'true' || wallet.type === 2 ? 2 : 1
    if (walletType === 2) {
      const partnerName = wallet.ai_partner_name || '伴侣'
      return `和${partnerName}的情侣钱包，一起攒钱更有趣 💕`
    }
    
    return `个人钱包，当前余额 ¥${parseFloat(wallet.balance || 0).toFixed(2)}`
  },

  // 格式化时间
  formatTime(timeStr) {
    if (!timeStr) return '刚刚'
    
    try {
      const time = new Date(timeStr)
      const now = new Date()
      
      // 检查时间是否有效
      if (isNaN(time.getTime())) {
        return '时间无效'
      }
      
      const diff = now - time
      
      if (diff < 60000) { // 1分钟内
        return '刚刚'
      } else if (diff < 3600000) { // 1小时内
        return Math.floor(diff / 60000) + '分钟前'
      } else if (diff < 86400000) { // 24小时内
        return Math.floor(diff / 3600000) + '小时前'
      } else if (diff < 2592000000) { // 30天内
        return Math.floor(diff / 86400000) + '天前'
      } else {
        // 超过30天显示具体日期
        return `${time.getMonth() + 1}月${time.getDate()}日`
      }
    } catch (e) {
      console.error('时间格式化失败:', e, '原始时间:', timeStr)
      return '时间格式错误'
    }
  },

  // 获取有效的背景图片
  getValidBackgroundImage(backgroundImage) {
    // 如果没有背景图片，使用默认图片
    if (!backgroundImage) {
      return 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png'
    }
    
    // 如果是预设的渐变背景或自定义背景，都使用默认图片
    if (backgroundImage.startsWith('gradient') || 
        backgroundImage.startsWith('/img/backgrounds/') || 
        backgroundImage.startsWith('custom_bg_')) {
      return 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png'
    }
    
    // 如果是完整的URL或base64，直接使用
    if (backgroundImage.startsWith('http') || 
        backgroundImage.startsWith('data:') ||
        backgroundImage.startsWith('/images/')) {
      return backgroundImage
    }
    
    // 其他情况使用默认图片
    return 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png'
  },

  // 处理兜圈圈页面的回退按钮
  handleBack() {
    // 从兜圈圈页面回到钱兜兜页面
    this.setData({
      currentTab: 'wallet'
    })
    this.loadData()
  },

  // 切换标签页
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    console.log('切换到标签页:', tab)
    this.setData({
      currentTab: tab
    })
    this.loadData()
  },

  // 发布动态
  publishPost() {
    wx.showToast({
      title: '发布功能开发中',
      icon: 'none'
    })
  },

  // 点赞/取消点赞
  toggleLike(e) {
    const post = e.currentTarget.dataset.post
    const posts = this.data.posts.map(p => {
      if (p.id === post.id) {
        return {
          ...p,
          is_liked: !p.is_liked,
          like_count: p.is_liked ? p.like_count - 1 : p.like_count + 1
        }
      }
      return p
    })
    this.setData({ posts })
  },

  // 显示评论
  showComments(e) {
    const post = e.currentTarget.dataset.post
    wx.showToast({
      title: '评论功能开发中',
      icon: 'none'
    })
  },

  // 分享动态
  sharePost(e) {
    const post = e.currentTarget.dataset.post
    wx.showToast({
      title: '分享成功',
      icon: 'success'
    })
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (modalRes) => {
        if (modalRes.confirm) {
          app.clearLoginInfo()
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  // 加载未读消息数量
  loadUnreadMessageCount() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      return
    }

    walletAPI.getUnreadMessageCount(userId)
      .then(result => {
        const count = result.data || 0
        this.setData({ unreadMessageCount: count })
        console.log('未读消息数量:', count)
      })
      .catch(error => {
        console.log('获取未读消息数量失败:', error.message)
        // 不影响用户体验，默认为0
        this.setData({ unreadMessageCount: 0 })
      })
  },

  // 兜圈圈相关功能
  showNotifications() {
    // 标记消息为已读
    this.markMessagesAsRead()
    
    wx.navigateTo({
      url: '/pages/wallet-messages/wallet-messages'
    })
  },

  // 标记消息为已读
  markMessagesAsRead() {
    const userId = app.globalData.userInfo?.id
    if (!userId || this.data.unreadMessageCount === 0) {
      return
    }

    walletAPI.markMessagesAsRead(userId)
      .then(result => {
        console.log('消息标记为已读成功')
        this.setData({ unreadMessageCount: 0 })
      })
      .catch(error => {
        console.log('标记消息已读失败:', error.message)
      })
  },

  loadMorePosts() {
    console.log('加载更多动态')
  },

  // 跳转到钱包详情页（从社交圈）
  goToWalletDetail(e) {
    const walletId = e.currentTarget.dataset.walletId
    const postIndex = e.currentTarget.dataset.index
    
    console.log('从社交圈跳转到钱包详情页:', walletId, '类型:', typeof walletId)
    console.log('帖子索引:', postIndex)
    
    // 获取完整的钱包信息
    const post = this.data.posts.find(p => p.wallet_id == walletId)
    console.log('找到的钱包信息:', post)
    
    // 从社交圈跳转，使用wallet-detail页面但传递社交参数
    if (walletId) {
      let url = `/pages/wallet-detail/wallet-detail?id=${walletId}&fromSocial=true`
      
      // 通过URL参数传递关键信息，避免存储过大数据
      if (post) {
        const ownerNickname = encodeURIComponent(post.owner_nickname || '')
        const title = encodeURIComponent(post.title || '')
        const fansCount = post.fansCount || 0
        const likeCount = post.like_count || 0
        
        url += `&ownerNickname=${ownerNickname}&title=${title}&fansCount=${fansCount}&likeCount=${likeCount}`
      }
      
      console.log('跳转URL:', url)
      
      wx.navigateTo({
        url: url
      })
    } else {
      wx.showToast({
        title: '钱包ID无效',
        icon: 'none'
      })
    }
  },

  // 处理图片加载错误
  handleImageError(e) {
    const index = e.currentTarget.dataset.index
    console.log('图片加载失败，索引:', index)
    
    // 更新失败的图片为默认图片
    const posts = this.data.posts
    if (posts[index]) {
      posts[index].bgImage = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png'
      this.setData({ posts })
    }
  },

  // 测试API调用（调试用）
  testAPICall() {
    console.log('=== 开始测试API调用 ===')
    console.log('登录状态:', app.isLoggedIn())
    console.log('用户信息:', app.globalData.userInfo)
    console.log('Token:', app.globalData.token)
    
    // 直接调用API
    walletAPI.getPublicWallets()
      .then(result => {
        console.log('=== API调用成功 ===')
        console.log('完整结果:', result)
        wx.showModal({
          title: 'API测试结果',
          content: `获取到${result.data ? result.data.length : 0}个公开钱包`,
          showCancel: false
        })
      })
      .catch(error => {
        console.error('=== API调用失败 ===')
        console.error('错误:', error)
        wx.showModal({
          title: 'API测试失败',
          content: error.message || '未知错误',
          showCancel: false
        })
      })
  },

  // 清理存储空间
  clearStorage() {
    wx.showModal({
      title: '清理存储',
      content: '是否清理本地存储空间？这将删除缓存的图片和临时数据。',
      success: (res) => {
        if (res.confirm) {
          try {
            // 清理自定义背景图片
            wx.removeStorageSync('custom_images')
            
            // 清理临时的钱包社交信息
            const storageInfo = wx.getStorageInfoSync()
            storageInfo.keys.forEach(key => {
              if (key.startsWith('wallet_social_')) {
                wx.removeStorageSync(key)
              }
            })
            
            wx.showToast({
              title: '存储空间已清理',
              icon: 'success'
            })
          } catch (e) {
            console.error('清理存储失败:', e)
            wx.showToast({
              title: '清理失败',
              icon: 'none'
            })
          }
        }
      }
    })
  }
})