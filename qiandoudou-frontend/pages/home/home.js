// pages/home/home.js
const app = getApp()
const { walletAPI, shareImageAPI } = require('../../utils/api.js')

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
    unreadMessageCount: 0, // 未读消息数量
    showShareModal: false, // 是否显示分享弹窗
    shareImageUrl: '', // 分享图片地址
    navHeight: 0,
    // 分页相关
    currentPage: 1,
    pageSize: 10,
    hasMorePosts: true,
    loadingMore: false,
    socialDataLoaded: false // 标记社交数据是否已加载
  },

  onLoad() {
    // 检查登录状态
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
   // 计算导航栏高度
   const systemInfo = wx.getSystemInfoSync();
   const statusBarHeight = systemInfo.statusBarHeight;
   // 导航栏内容高度一般自定义为44px
   const navContentHeight = 44;
   this.setData({
     navHeight: statusBarHeight + navContentHeight
   });
    this.setData({
      userInfo: app.globalData.userInfo
    })

    // 加载钱兜兜数据
    this.loadData()
    
    // 加载未读消息数量
    this.loadUnreadMessageCount()
    
    // 监听钱包公开状态变化事件
    this.walletStatusChangeHandler = (data) => {
      console.log('收到钱包状态变化事件:', data)
      // 如果当前在社交页面，刷新数据
      if (this.data.currentTab === 'social') {
        this.loadPosts(true)
        this.setData({ socialDataLoaded: true })
      }
    }
    app.globalData.eventBus.on('walletPublicStatusChanged', this.walletStatusChangeHandler)
  },
  // onBack() {
  //   // 显示模态框
  //   wx.showModal({
  //     title: '确认退出',
  //     content: '确定要退出登录吗？',
  //     success: (modalRes) => {
  //       if (modalRes.confirm) {
  //         app.clearLoginInfo()
  //         wx.redirectTo({
  //           url: '/pages/login/login'
  //         })
  //       }
  //     }
  //   })
  // },
  onShow() {
    // 每次显示页面时刷新数据
    if (app.isLoggedIn()) {

      this.loadData()
      
      // 强制刷新钱兜兜列表以获取最新的背景设置
      this.loadWallets()
      
      // 只在社交数据未加载时才刷新
      if (!this.data.socialDataLoaded) {
        this.loadPosts(true)
        this.setData({ socialDataLoaded: true })
      }
      
      // 加载未读消息数量
      this.loadUnreadMessageCount()
    }
    
    // 重置刷新标记
    this.setData({ shouldRefresh: false })
  },

  onUnload() {
    // 移除事件监听器
    if (this.walletStatusChangeHandler) {
      const app = getApp()
      app.globalData.eventBus.off('walletPublicStatusChanged', this.walletStatusChangeHandler)
    }
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
    // 如果当前在社交页面，立即加载数据避免闪烁
    if (this.data.currentTab === 'social' && !this.data.socialDataLoaded) {
      // 稍微延迟以确保页面结构已渲染
      setTimeout(() => {
        this.loadPosts(true)
        this.setData({ socialDataLoaded: true })
      }, 100)
    }
  },

  // 加载钱兜兜列表
  loadWallets() {
    const userId = app.globalData.userInfo?.id




    
    if (!userId) {


      const localUserInfo = wx.getStorageSync('userInfo')

      
      if (localUserInfo && localUserInfo.id) {

        app.globalData.userInfo = localUserInfo
        this.loadWallets() // 递归调用
        return
      }

      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    this.setData({ loading: true })

    walletAPI.getUserWallets(userId)
      .then(result => {

        const wallets = result.data || []


        
        // 检查是否为新用户（没有钱包）
        if (wallets.length === 0) {

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
          this.loadPosts(true) // 重新加载使用刷新模式
          this.setData({ socialDataLoaded: true })
        }
      })
      .catch(error => {





        
        wx.showToast({
          title: '加载钱包失败: ' + (error.message || '未知错误'),
          icon: 'none',
          duration: 3000
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
            createTime: this.formatTime(transaction.createTime),
            amount: parseFloat(transaction.amount).toFixed(2),
            type: transaction.type === 1 ? 'INCOME' : 'EXPENSE'
          }))
          
          this.setData({
            transactions: formattedTransactions
          })
        })
        .catch(error => {

          // 如果加载失败，显示空数组
          this.setData({
            transactions: []
          })
        })
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
    
    walletAPI.createWallet(userId, walletName, 1, null, null)
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

    this.loadWallets()
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
  loadPosts(isRefresh = false) {
    // 如果是刷新，重置分页数据但不立即清空posts避免闪烁
    if (isRefresh) {
      this.setData({
        currentPage: 1,
        hasMorePosts: true
      })
    }

    // 如果没有更多数据或正在加载中，直接返回
    if (!this.data.hasMorePosts || this.data.loadingMore) {
      return
    }

    this.setData({ loadingMore: true })
    
    console.log(`开始加载兜圈圈数据 - 当前页: ${this.data.currentPage}, 每页: ${this.data.pageSize}, 刷新模式: ${isRefresh}`)
    
    walletAPI.getPublicWallets(this.data.currentPage, this.data.pageSize)
      .then(result => {
        console.log('API响应数据:', result)
        
        const responseData = result.data || {}
        const publicWallets = responseData.list || []
        const hasMore = responseData.hasMore || false
        
        console.log('解析后的分页信息:', {
          本页数据量: publicWallets.length,
          总数: responseData.total,
          当前页: responseData.page,
          每页大小: responseData.size,
          还有更多: hasMore
        })

        // 检查是否有数据
        if (!publicWallets || publicWallets.length === 0) {
          if (this.data.currentPage === 1) {
            // 第一页没有数据，显示空状态
            this.setData({ 
              posts: [],
              hasMorePosts: false,
              loadingMore: false
            })
          } else {
            // 后续页没有数据，更新状态
            this.setData({ 
              hasMorePosts: false,
              loadingMore: false
            })
          }
          return
        }
        
        // 将公开钱包数据转换为兜圈圈显示格式
        const socialPosts = publicWallets.map((wallet, index) => {

          
          // 解析最新交易记录
          let recentTransactions = []
          try {
            if (wallet.recent_transactions) {
              recentTransactions = typeof wallet.recent_transactions === 'string' 
                ? JSON.parse(wallet.recent_transactions) 
                : wallet.recent_transactions
            }
          } catch (e) {

            recentTransactions = []
          }
          
          // 确保recentTransactions是数组
          if (!Array.isArray(recentTransactions)) {

            recentTransactions = []
          }

          
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
            fansCount: 0, // 新钱包粉丝数为0，从后端获取真实数据
            participantCount: recentTransactions.length,
            like_count: 0, // 新钱包点赞数为0，从后端获取真实数据  
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

          return socialPost
        }).filter(post => post && post.id) // 过滤掉无效的钱包数据

        // 处理分页数据
        const isFirstPage = this.data.currentPage === 1
        const currentPosts = isFirstPage && isRefresh ? [] : this.data.posts
        const newPosts = [...currentPosts, ...socialPosts]
        
        console.log('分页数据处理:', {
          当前页码: this.data.currentPage,
          是否首页: isFirstPage,
          本页新数据: socialPosts.length,
          原有数据: currentPosts.length,
          合并后总数: newPosts.length,
          还有更多: hasMore
        })
        
        this.setData({
          posts: newPosts,
          currentPage: this.data.currentPage + 1, // 为下次请求准备页码
          hasMorePosts: hasMore,
          loadingMore: false
        })
      })
      .catch(error => {
        console.error('加载公开钱包失败:', error)
        this.setData({
          loadingMore: false
        })
        
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
      // 处理iOS不兼容的日期格式
      let processedTimeStr = timeStr
      
      // 如果包含微秒，去掉微秒部分 (例如: "2025-09-09 18:12:24.000000" -> "2025-09-09 18:12:24")
      if (typeof timeStr === 'string' && timeStr.includes('.')) {
        processedTimeStr = timeStr.split('.')[0]
      }
      
      // 将空格替换为T，符合ISO格式 (例如: "2025-09-09 18:12:24" -> "2025-09-09T18:12:24")
      if (typeof processedTimeStr === 'string' && processedTimeStr.includes(' ')) {
        processedTimeStr = processedTimeStr.replace(' ', 'T')
      }
      
      const time = new Date(processedTimeStr)
      const now = new Date()
      
      // 检查时间是否有效
      if (isNaN(time.getTime())) {
        console.warn('日期解析失败:', timeStr, '处理后:', processedTimeStr)
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
      console.error('formatTime错误:', e, '输入:', timeStr)
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
    const previousTab = this.data.currentTab
    
    wx.setNavigationBarTitle({
      title: tab === 'social' ? '兜圈圈' : '钱兜兜'
    })
    this.setData({
      currentTab: tab
    })
    
    // 只在标签页真正切换时才加载数据，避免重复加载
    if (previousTab !== tab) {
      if (tab === 'social' && !this.data.socialDataLoaded) {
        // 切换到兜圈圈页面且数据未加载时才加载
        this.loadPosts(true)
        this.setData({ socialDataLoaded: true })
      } else if (tab === 'wallet') {
        // 切换到钱兜兜页面时刷新钱包数据
        this.loadWallets()
      }
    }
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
  // handleLogout() {
  //   wx.showModal({
  //     title: '确认退出',
  //     content: '确定要退出登录吗？',
  //     success: (modalRes) => {
  //       if (modalRes.confirm) {
  //         app.clearLoginInfo()
  //         wx.redirectTo({
  //           url: '/pages/login/login'
  //         })
  //       }
  //     }
  //   })
  // },

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

      })
      .catch(error => {

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

        this.setData({ unreadMessageCount: 0 })
      })
      .catch(error => {

      })
  },

  // 上拉加载更多动态
  loadMorePosts() {
    // 如果当前不在社交页面，不加载
    if (this.data.currentTab !== 'social') {
      return
    }
    
    // 调用loadPosts来加载下一页，不使用刷新模式
    this.loadPosts(false)
  },

  // 触底加载更多
  onReachBottom() {
    this.loadMorePosts()
  },

  // 跳转到钱包详情页（从社交圈）
  goToWalletDetail(e) {
    const walletId = e.currentTarget.dataset.walletId
    const postIndex = e.currentTarget.dataset.index


    
    // 获取完整的钱包信息
    const post = this.data.posts.find(p => p.wallet_id == walletId)

    
    // 从社交圈跳转，使用wallet-detail页面但传递社交参数
    if (walletId) {
      let url = `/pages/wallet-detail/wallet-detail?id=${walletId}&fromSocial=true`
      
      // 通过URL参数传递关键信息，避免存储过大数据
      if (post) {
        const ownerNickname = encodeURIComponent(post.owner_nickname || '')
        const title = encodeURIComponent(post.title || '')
        // 确保新钱包的社交数据为0，不传递任何可能的模拟数据
        const fansCount = 0  // 新钱包粉丝数应该为0
        const likeCount = 0  // 新钱包获赞数应该为0
        
        url += `&ownerNickname=${ownerNickname}&title=${title}&fansCount=${fansCount}&likeCount=${likeCount}`
      }

      
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

    
    // 更新失败的图片为默认图片
    const posts = this.data.posts
    if (posts[index]) {
      posts[index].bgImage = 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png'
      this.setData({ posts })
    }
  },


  // 钱包分享
  onWalletShare(e) {

    const wallet = e.currentTarget.dataset.wallet

    
    wx.showLoading({
      title: '加载分享图片...'
    })
    
    // 获取钱兜兜分享图片

    shareImageAPI.getWalletShareImage()
      .then(result => {
        wx.hideLoading()





        
        if (result.data && result.data.imageUrl) {

          this.setData({
            showShareModal: true,
            shareImageUrl: result.data.imageUrl
          })

        } else {

          wx.showToast({
            title: '分享图片数据无效',
            icon: 'none'
          })
        }
      })
      .catch(error => {
        wx.hideLoading()




        wx.showModal({
          title: '分享功能错误',
          content: `错误信息：${error.message || '未知错误'}`,
          showCancel: false
        })
      })
  },

  // 关闭分享弹窗
  onShareModalClose() {

    this.setData({
      showShareModal: false,
      shareImageUrl: ''
    })
  },

  // 分享图片保存回调
  onShareImageSave(e) {

    if (e.detail.success) {
      // 保存成功后可以关闭弹窗
      setTimeout(() => {
        this.onShareModalClose()
      }, 1000)
    }
  },

})