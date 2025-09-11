// pages/couple-savings/couple-savings.js
const app = getApp()
const { walletAPI, request } = require('../../utils/api.js')

Page({
  data: {
    currentIndex: 0, // 当前选中的人物索引
    partners: [
      {
        id: 1,
        name: '黎初言',
        tags: ['娱乐圈小明星'],
        description: '明珠本蛋美人，超爱撒娇',
        fullImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/柳如烟_背景图.jpg', // 全身照
        avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/柳如烟_人像.jpg', // 人像
        background: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      },
      {
        id: 2,
        name: '江梦溪',
        tags: ['温柔知性'],
        description: '优雅迷人，理财达人',
        fullImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/江梦溪_背景图.jpg', // 全身照
        avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/江梦溪_人像.jpg', // 头像
        background: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      },
      {
        id: 3,
        name: '苏晨曦',
        tags: ['温柔知性'],
        description: '优雅迷人，理财达人',
        fullImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/李晓月_背景图.jpg', // 全身照
        avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/李晓月_人像.jpg', // 头像
        background: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      },
      {
        id: 4,
        name: '林若汐',
        tags: ['活泼可爱'],
        description: '青春洋溢，节约小能手',
        fullImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/吴梦菲_背景图.jpg', // 全身照
        avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/吴梦菲_人像.jpg', // 头像
        background: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      },
      {
        id: 5,
        name: '方子逸',
        tags: ['成熟稳重'],
        description: '放浪不羁，八块腹肌',
        fullImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/方子逸_背景图.jpg', // 全身照
        avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/方子逸_人像.jpg', // 头像
        background: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      },
      {
        id: 6,
        name: '王玉玄',
        tags: ['邻家大男孩'],
        description: '温柔暖男，爱心呵护',
        fullImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/王玉玄_背景图.jpg', // 全身照
        avatar: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/王玉玄_人像.jpg', // 头像
        background: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
      }
    ]
  },

  onLoad(options) {
    // 检查登录状态
    if (!app.isLoggedIn()) {
      // 为了测试，设置一个模拟用户
      const testUser = {
        id: 1,
        username: 'testuser',
        nickname: '测试用户',
        phone: '13800138000'
      }
      app.setLoginInfo('test-token', testUser)
    } else {
    }
  },

  // 处理下方轮播滑动
  onSwiperChange(e) {
    // 添加边界检查，确保 e 存在
    if (!e) {
      return;
    }
    
    const current = e.detail?.current ?? 0;
    this.setData({
      currentIndex: current
    });
  },

  // 点击头像切换
  onAvatarTap(e) {
    const index = e.currentTarget.dataset.index
    this.setData({
      currentIndex: index
    })
  },

  // 点击邀请按钮
  onInviteTap(e) {
    const currentPartner = this.data.partners[this.data.currentIndex]
    
    wx.showModal({
      title: '邀请AI伴侣',
      content: `确定邀请${currentPartner.name}作为你的AI伴侣一起攒钱吗？`,
      confirmText: '邀请',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          // 保存选择的AI伴侣
          wx.setStorageSync('selectedAiPartner', currentPartner)
          
          // 创建情侣钱包并让AI伴侣自动转入
          this.createCoupleWalletWithAiTransfer(currentPartner)
        }
      }
    })
  },

  // 创建情侣钱包并让AI伴侣自动转入
  createCoupleWalletWithAiTransfer(partner) {
    const userId = app.globalData.userInfo?.id
    
    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    const walletName = `与${partner.name}的爱情基金`
    
    wx.showLoading({
      title: '创建钱包中...'
    })
    
    // 使用后端API创建情侣钱包
    walletAPI.createWallet(userId, walletName, 2, null, partner.id)
      .then(result => {
        const newWallet = result.data
        
        // 让AI伴侣自动转入0.01元
        return this.aiPartnerTransferIn(newWallet.id, partner)
      })
      .then(() => {
        wx.hideLoading()
        wx.showToast({
          title: '邀请成功！AI伴侣已转入0.01元',
          icon: 'success',
          duration: 2000
        })

        // 延迟跳转到钱包详情页
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/wallet-detail/wallet-detail?id=${this.data.createdWalletId}`
          })
        }, 2000)
      })
      .catch(error => {
        wx.hideLoading()
        wx.showToast({
          title: error.message || '创建钱包失败',
          icon: 'none'
        })
      })
  },

  // AI伴侣自动转入资金
  aiPartnerTransferIn(walletId, partner) {
    this.setData({ createdWalletId: walletId })
    
    // 生成AI伴侣的转账文案
    const aiMessages = [
      `叮！你家大明星的分"巨款"到账！速回"谢谢宝贝"买断今天和我聊天的特权！快啊～`,
      `小钱钱来啦～我先投个0.01元表示诚意，咱们一起攒钱买小房子！`,
      `第一笔资金到账！虽然只有一分钱，但这是我们爱情的开始呢～`,
      `投资我们的未来，从0.01元开始！宝贝，你准备好和我一起理财了吗？`,
      `叮咚～AI伴侣专属红包已到账！这一分钱代表我对你满满的爱意！`
    ]
    
    const randomMessage = aiMessages[Math.floor(Math.random() * aiMessages.length)]
    
    // 检查walletAPI对象
    
    // 如果方法不存在，使用临时的直接API调用
    if (typeof walletAPI.aiPartnerTransferIn !== 'function') {
      return this.directAiPartnerTransfer(walletId, partner.id, 0.01, randomMessage, partner.name, partner.avatar, partner.name)
    }
    
    // 调用AI伴侣转账API，传入人物名称用于TTS声音选择
    return walletAPI.aiPartnerTransferIn(walletId, partner.id, 0.01, randomMessage, partner.name, partner.avatar, partner.name)
  },

  // 直接API调用（临时解决方案）
  directAiPartnerTransfer(walletId, aiPartnerId, amount, message, aiPartnerName, aiPartnerAvatar, characterName) {
    return request({
      url: '/wallet/ai-partner-transfer',
      method: 'POST',
      data: { 
        walletId, 
        aiPartnerId, 
        amount, 
        message, 
        aiPartnerName, 
        aiPartnerAvatar,
        characterName  // 新增人物名称参数
      }
    })
  }

})
