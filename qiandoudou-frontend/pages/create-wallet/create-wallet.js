// pages/create-wallet/create-wallet.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    name: '',
    balance: '',
    walletType: 1, // 1: 个人钱包, 2: AI情侣攒
    selectedPartner: null,
    loading: false
  },

  onLoad() {
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
    }
  },

  onShow() {
    // 检查是否从AI伴侣选择页面返回
    const selectedPartner = wx.getStorageSync('selectedAiPartner')

    if (selectedPartner) {
      this.setData({
        selectedPartner
      })
      // 清除临时存储
      wx.removeStorageSync('selectedAiPartner')
    }
  },

  // 选择钱包类型
  selectWalletType(e) {
    const type = parseInt(e.currentTarget.dataset.type)
    this.setData({
      walletType: type,
      selectedPartner: type === 1 ? null : this.data.selectedPartner
    })
  },

  // 跳转到AI伴侣选择页面
  goToSelectPartner() {
    wx.navigateTo({
      url: '/pages/ai-partner/ai-partner'
    })
  },

  onNameInput(e) {
    this.setData({
      name: e.detail.value
    })
  },

  onBalanceInput(e) {
    this.setData({
      balance: e.detail.value
    })
  },

  handleCreate() {
    const { name, balance, walletType, selectedPartner } = this.data
    const userId = app.globalData.userInfo?.id

    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    if (!name.trim()) {
      wx.showToast({
        title: '请输入钱包名称',
        icon: 'none'
      })
      return
    }

    if (walletType === 2 && !selectedPartner) {
      wx.showToast({
        title: '请选择AI伴侣',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    const aiPartnerId = walletType === 2 && selectedPartner ? selectedPartner.id : null

    walletAPI.createWallet(userId, name.trim(), walletType, null, aiPartnerId)
      .then(result => {
        wx.showToast({
          title: '创建成功',
          icon: 'success'
        })
        
        // 通知首页刷新数据
        const pages = getCurrentPages()
        if (pages.length > 1) {
          const prevPage = pages[pages.length - 2]
          if (prevPage && prevPage.loadWallets) {
            prevPage.loadWallets()
          }
        }
        
        // 清空表单
        this.setData({
          name: '',
          balance: '',
          walletType: 1,
          selectedPartner: null,
          loading: false
        })
        
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '创建钱包失败',
          icon: 'none'
        })
        this.setData({ loading: false })
      })
  }
})