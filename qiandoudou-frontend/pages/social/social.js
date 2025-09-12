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
        bgImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png',
        backgroundStyle: 'background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);',
        walletId: 1001,
        participantCount: 2,
        comments: [
          {
            userId: 201,
            username: '冲动的',
            message: '来啦记得回',
            avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png'
          },
          {
            userId: 202,
            username: '足呱呱',
            message: '好漂亮',
            avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png'
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
        bgImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png',
        backgroundStyle: 'background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);',
        walletId: 1002,
        participantCount: 2,
        comments: [
          {
            userId: 203,
            username: '朱敏多',
            message: '今天在垃圾桶捡到五块',
            avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png',
            amount: '+¥100.00'
          },
          {
            userId: 202,
            username: '足呱呱',
            message: '来啦来啦',
            avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png'
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
    this.loadUserInfo(); // 重新加载用户信息，包括头像
    this.loadPublicWallets();
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo') || app.globalData.userInfo;
    
    if (userInfo) {
      // 有本地用户信息，直接使用
      const displayUserInfo = {
        nickname: userInfo.nickname || '钱兜兜用户',
        avatar: userInfo.avatar || '',
        description: userInfo.description || '这个人很懒，什么都没留下',
        hasCustomAvatar: !!(userInfo.avatar && userInfo.hasCustomAvatar)
      }



      
      this.setData({
        userInfo: displayUserInfo
      });
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
          avatar: serverUserInfo.avatar || '',
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
          avatar: '',
          description: '这个人很懒，什么都没留下',
          hasCustomAvatar: false
        }
        
        this.setData({
          userInfo: defaultUserInfo
        })

      })
  },

  // 加载钱包数据
  loadWallets() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {

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

  // 点击评论用户头像跳转到该用户的主页
  navigateToCommentUserProfile(e) {
    const userId = e.currentTarget.dataset.userId;
    const username = e.currentTarget.dataset.username;
    
    console.log('点击评论用户头像，用户ID:', userId, '用户名:', username);
    
    if (userId) {
      // 跳转到用户主页，传入用户ID
      wx.navigateTo({
        url: `/pages/user-social-profile/user-social-profile?userId=${userId}&username=${username}`
      });
    } else {
      wx.showToast({
        title: '用户信息获取失败',
        icon: 'none'
      });
    }
  },

  // 显示用户菜单（保留原有功能）
  showUserMenu() {
    wx.showActionSheet({
      itemList: ['个人信息', '设置'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 跳转到个人信息页面

        } else if (res.tapIndex === 1) {
          // 跳转到设置页面

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

    
    // 根据postId获取对应的用户信息
    const post = this.data.posts.find(p => p.id == postId);
    if (post && walletId) {
      const userId = post.userId || 1;

      
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

    
    walletAPI.getPublicWallets(1, 20)
      .then(response => {

        if (response.success && response.data && response.data.list) {
          const publicWallets = response.data.list
          
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
              bgImage: wallet.backgroundImage || 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/bg.png',
              backgroundStyle: backgroundStyles[index % backgroundStyles.length],
              participantCount: wallet.type === 2 ? 2 : 1,
              fansCount: 0, // 新钱包粉丝数为0，稍后从API获取真实数据
              comments: recentTransactions.slice(0, 2).map(tx => ({
                username: wallet.owner_nickname || '用户',
                message: tx.description || '攒钱记录',
                avatar: wallet.owner_avatar || 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/usages/user-avatar.png',
                amount: tx.type === 1 ? `+¥${tx.amount}` : undefined
              }))
            }
          })

          
          this.setData({ posts })
          
          // 为每个钱包获取真实的社交统计数据
          this.loadSocialStatsForPosts(posts)
        } else {

          // 如果API失败，保留原有的模拟数据
          this.updatePostsWithSocialStats()
        }
      })
      .catch(error => {

        // 如果网络错误，保留原有的模拟数据
        this.updatePostsWithSocialStats()
      })
  },

  // 为钱包列表加载真实的社交统计数据
  loadSocialStatsForPosts(posts) {

    
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

    
    this.setData({ posts })
  }
})