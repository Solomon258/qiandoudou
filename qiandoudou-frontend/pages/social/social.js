// pages/social/social.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    userInfo: {
      nickname: '用户昵称',
      avatar: ''
    },
    wallets: [],
    posts: [
      {
        id: 1,
        userId: 101,
        title: '宝儿的锦鲤小岛😈',
        amount: '2221.21',
        tags: ['生活', '攒钱'],
        description: '一年每天自动存一块已到期（说真的，突然…',
        bgImage: 'https://8.148.206.18:443/res/image/include_images/bg.png',
        backgroundStyle: 'background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);',
        walletId: 1001,
        participantCount: 2,
        comments: [
          {
            username: '冲动的',
            message: '来啦记得回',
            avatar: 'https://8.148.206.18:443/res/image/include_images/user-avatar.png'
          },
          {
            username: '足呱呱',
            message: '好漂亮',
            avatar: 'https://8.148.206.18:443/res/image/include_images/user-avatar.png'
          }
        ]
      },
      {
        id: 2,
        userId: 102,
        title: '给朱敏攒钱了',
        amount: '21231.21',
        tags: ['情感', '校园'],
        description: '给发哥攒钱买车',
        bgImage: 'https://8.148.206.18:443/res/image/include_images/bg.png',
        backgroundStyle: 'background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);',
        walletId: 1002,
        participantCount: 2,
        comments: [
          {
            username: '朱敏多',
            message: '今天在垃圾桶捡到五块',
            avatar: 'https://8.148.206.18:443/res/image/include_images/user-avatar.png',
            amount: '+¥100.00'
          },
          {
            username: '足呱呱',
            message: '来啦来啦',
            avatar: 'https://8.148.206.18:443/res/image/include_images/user-avatar.png'
          }
        ]
      }
    ]
  },

  onLoad() {
    this.loadUserInfo();
    this.loadWallets();
    this.loadPublicWallets(); // 从后端加载真实的公开钱包数据
  },

  onShow() {
    // 页面显示时刷新社交数据，确保与详情页数据同步
    this.loadPublicWallets();
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        userInfo: userInfo
      });
    }
  },

  // 加载钱包数据
  loadWallets() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      console.error('用户ID不存在')
      return
    }

    walletAPI.getUserWallets(userId)
      .then(result => {
        const wallets = result.data || []
        this.setData({
          wallets: wallets
        })
        
        // 加载钱包数据后，更新动态的背景样式
        this.updatePostsWithWalletBackgrounds()
      })
      .catch(error => {
        console.error('加载钱包失败:', error)
      })
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

  // 更新动态的钱包背景样式
  updatePostsWithWalletBackgrounds() {
    const posts = this.data.posts
    const wallets = this.data.wallets
    
    const updatedPosts = posts.map(post => {
      // 根据walletId找到对应的钱包
      const wallet = wallets.find(w => w.id === post.walletId) || wallets[0]
      if (wallet) {
        const backgroundStyle = this.getWalletBackground(wallet)
        return {
          ...post,
          backgroundStyle: backgroundStyle
        }
      }
      return post
    })
    
    this.setData({
      posts: updatedPosts
    })
  },

  // 加载动态列表
  loadPosts() {
    // 这里可以调用API获取真实数据
    console.log('加载动态列表');
    
    // 如果钱包数据已经加载，立即更新背景样式
    if (this.data.wallets.length > 0) {
      this.updatePostsWithWalletBackgrounds()
    }
    
    // 更新社交统计数据
    this.updatePostsWithSocialStats()
  },

  // 返回上一页
  goBack() {
    wx.navigateBack();
  },

  // 跳转到用户个人社交圈主页
  navigateToUserSocialProfile() {
    console.log('🔥 点击了顶部导航栏用户头像！！！');
    wx.showModal({
      title: '测试',
      content: '你点击了正确的头像！即将跳转到个人社交圈主页',
      showCancel: false,
      success: () => {
        wx.navigateTo({
          url: '/pages/user-social-profile/user-social-profile'
        });
      }
    });
  },

  // 显示用户菜单（保留原有功能）
  showUserMenu() {
    wx.showActionSheet({
      itemList: ['个人信息', '设置'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 跳转到个人信息页面
          console.log('跳转到个人信息');
        } else if (res.tapIndex === 1) {
          // 跳转到设置页面
          console.log('跳转到设置');
        }
      }
    });
  },

  // 显示通知
  showNotifications() {
    wx.showToast({
      title: '暂无新通知',
      icon: 'none'
    });
  },

  // 切换标签页
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === 'wallet') {
      wx.switchTab({
        url: '/pages/home/home'
      });
    }
  },

  // 加载更多
  loadMore() {
    console.log('加载更多动态');
  },

  // 点击动态卡片跳转到用户详情页面
  navigateToUserProfile(e) {
    const postId = e.currentTarget.dataset.postId;
    const walletId = e.currentTarget.dataset.walletId;
    
    // 根据postId获取对应的用户信息
    const post = this.data.posts.find(p => p.id == postId);
    if (post) {
      // 这里应该从post中获取真实的userId，暂时使用模拟数据
      const userId = post.userId || 1; // 假设每个post都有userId字段
      
      wx.navigateTo({
        url: `/pages/user-profile/user-profile?userId=${userId}&walletId=${walletId}`
      });
    }
  },

  // 点击钱包卡片跳转到用户详情页（别人的钱包）
  navigateToWalletDetail(e) {
    const walletId = e.currentTarget.dataset.walletId;
    const postId = e.currentTarget.dataset.postId;
    
    console.log('社交圈点击钱包卡片，walletId:', walletId, 'postId:', postId);
    
    // 根据postId获取对应的用户信息
    const post = this.data.posts.find(p => p.id == postId);
    if (post && walletId) {
      const userId = post.userId || 1;
      
      console.log('跳转到用户详情页，userId:', userId, 'walletId:', walletId);
      
      wx.navigateTo({
        url: `/pages/user-profile/user-profile?userId=${userId}&walletId=${walletId}`
      });
    } else {
      wx.showToast({
        title: '钱包信息无效',
        icon: 'none'
      });
    }
  },

  // 测试别人钱包详情页功能
  testWalletDetail() {
    wx.navigateTo({
      url: `/pages/user-profile/user-profile?userId=101&walletId=1001`
    });
  },

  // 从后端加载真实的公开钱包数据
  loadPublicWallets() {
    console.log('开始加载公开钱包数据...')
    
    walletAPI.getPublicWallets()
      .then(response => {
        console.log('获取公开钱包数据成功:', response)
        if (response.success && response.data) {
          const publicWallets = response.data
          
          // 将后端数据转换为前端需要的格式
          const posts = publicWallets.map((wallet, index) => {
            // 解析最新交易记录
            let recentTransactions = []
            if (wallet.recent_transactions) {
              try {
                recentTransactions = typeof wallet.recent_transactions === 'string' 
                  ? JSON.parse(wallet.recent_transactions) 
                  : wallet.recent_transactions
              } catch (e) {
                console.error('解析交易记录失败:', e)
                recentTransactions = []
              }
            }
            
            // 生成背景样式
            const backgroundStyles = [
              'background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);',
              'background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);',
              'background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);',
              'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'
            ]
            
            return {
              id: wallet.id,
              userId: wallet.user_id,
              walletId: wallet.id,
              wallet_id: wallet.id,
              title: wallet.name || '钱兜兜',
              amount: wallet.balance || '0.00',
              tags: [wallet.type === 2 ? '情侣' : '个人', '攒钱', wallet.ai_partner_name || '理财'],
              description: recentTransactions.length > 0 
                ? recentTransactions[0].description || '开始攒钱之旅'
                : '开始攒钱之旅',
              bgImage: wallet.backgroundImage || 'https://8.148.206.18:443/res/image/include_images/bg.png',
              backgroundStyle: backgroundStyles[index % backgroundStyles.length],
              participantCount: wallet.type === 2 ? 2 : 1,
              fansCount: 0, // 初始值，稍后从API获取
              comments: recentTransactions.slice(0, 2).map(tx => ({
                username: wallet.owner_nickname || '用户',
                message: tx.description || '攒钱记录',
                avatar: wallet.owner_avatar || 'https://8.148.206.18:443/res/image/include_images/user-avatar.png',
                amount: tx.type === 1 ? `+¥${tx.amount}` : undefined
              }))
            }
          })
          
          console.log('转换后的钱包数据:', posts)
          
          this.setData({ posts })
          
          // 为每个钱包获取真实的社交统计数据
          this.loadSocialStatsForPosts(posts)
        } else {
          console.error('获取公开钱包数据失败:', response.message)
          // 如果API失败，保留原有的模拟数据
          this.updatePostsWithSocialStats()
        }
      })
      .catch(error => {
        console.error('加载公开钱包数据出错:', error)
        // 如果网络错误，保留原有的模拟数据
        this.updatePostsWithSocialStats()
      })
  },

  // 为钱包列表加载真实的社交统计数据
  loadSocialStatsForPosts(posts) {
    console.log('开始为钱包加载社交统计数据...')
    
    // 为每个钱包并行获取社交统计数据
    const socialStatsPromises = posts.map(post => {
      return walletAPI.getUserSocialStats(post.userId)
        .then(response => {
          if (response.success && response.data) {
            return {
              walletId: post.walletId,
              socialStats: response.data
            }
          }
          return null
        })
        .catch(error => {
          console.error(`获取钱包${post.walletId}社交数据失败:`, error)
          return null
        })
    })
    
    Promise.all(socialStatsPromises).then(results => {
      const updatedPosts = [...this.data.posts]
      
      results.forEach(result => {
        if (result) {
          const index = updatedPosts.findIndex(p => p.walletId === result.walletId)
          if (index !== -1) {
            updatedPosts[index].fansCount = result.socialStats.fansCount || 0
          }
        }
      })
      
      console.log('更新后的钱包粉丝数据:', updatedPosts.map(p => ({
        id: p.id,
        walletId: p.walletId,
        fansCount: p.fansCount
      })))
      
      this.setData({ posts: updatedPosts })
    })
  },

  // 获取钱包的社交统计数据（已废弃，应使用loadSocialStatsForPosts获取真实数据）
  getWalletSocialStats(walletId) {
    // 移除硬编码的测试数据，统一返回0值，等待真实API数据
    return { fansCount: 0, likesCount: 0, viewsCount: 0 }
  },

  // 更新动态列表的社交统计数据
  updatePostsWithSocialStats() {
    const posts = this.data.posts.map(post => {
      const socialStats = this.getWalletSocialStats(post.walletId)
      return {
        ...post,
        fansCount: socialStats.fansCount
      }
    })
    
    console.log('更新动态列表的粉丝数据:', posts.map(p => ({ id: p.id, walletId: p.walletId, fansCount: p.fansCount })))
    
    this.setData({ posts })
  }
})