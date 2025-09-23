// pages/create-wallet/create-wallet.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    name: '',
    balance: '',
    walletType: 1, // 1: 个人钱包, 2: AI情侣攒, 3: 搭子攒钱, 4: 梦想攒钱
    selectedPartner: null,
    selectedBuddies: [], // 选中的搭子列表
    buddyGroupId: null, // 搭子圈子ID
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

    // 检查是否从搭子选择页面返回
    const selectedBuddies = wx.getStorageSync('selectedBuddies')
    const buddyGroupId = wx.getStorageSync('buddyGroupId')
    if (selectedBuddies && selectedBuddies.length > 0) {
      this.setData({
        selectedBuddies,
        buddyGroupId
      })
      // 清除临时存储
      wx.removeStorageSync('selectedBuddies')
      wx.removeStorageSync('buddyGroupId')
    }
  },

  // 选择钱包类型
  selectWalletType(e) {
    const type = parseInt(e.currentTarget.dataset.type)
    this.setData({
      walletType: type,
      selectedPartner: type === 2 ? this.data.selectedPartner : null,
      selectedBuddies: type === 3 ? this.data.selectedBuddies : [],
      buddyGroupId: type === 3 ? this.data.buddyGroupId : null
    })
  },

  // 跳转到AI伴侣选择页面
  goToSelectPartner() {
    if (this.data.walletType === 2) {
      // 情侣攒钱
      wx.navigateTo({
        url: '/pages/ai-partner/ai-partner'
      })
    } else if (this.data.walletType === 3) {
      // 搭子攒钱
      wx.navigateTo({
        url: '/pages/buddy-mode-select/buddy-mode-select'
      })
    }
  },

  // 跳转到梦想攒钱选择页面
  goToDreamSelect() {
    wx.navigateTo({
      url: '/pages/dream-type-select/dream-type-select'
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
    const { name, balance, walletType, selectedPartner, selectedBuddies, buddyGroupId } = this.data
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

    if (walletType === 3 && (!selectedBuddies || selectedBuddies.length < 2)) {
      wx.showToast({
        title: '请选择至少2个搭子',
        icon: 'none'
      })
      return
    }

    if (walletType === 4) {
      // 梦想攒钱，跳转到梦想选择页面
      this.goToDreamSelect()
      return
    }

    this.setData({ loading: true })

    // 根据钱包类型调用不同的创建接口
    if (walletType === 3) {
      // 创建搭子钱包
      this.createBuddyWallet(userId, name.trim(), selectedBuddies, buddyGroupId)
    } else {
      // 创建个人或情侣钱包
      const aiPartnerId = walletType === 2 && selectedPartner ? selectedPartner.id : null
      this.createNormalWallet(userId, name.trim(), walletType, aiPartnerId)
    }
  },

  // 创建普通钱包（个人或情侣）
  createNormalWallet(userId, name, walletType, aiPartnerId) {
    walletAPI.createWallet(userId, name, walletType, null, aiPartnerId)
      .then(result => {
        this.handleCreateSuccess()
      })
      .catch(error => {
        this.handleCreateError(error)
      })
  },

  // 创建搭子钱包
  createBuddyWallet(userId, name, selectedBuddies, buddyGroupId) {
    const buddyCharacterIds = selectedBuddies.map(buddy => buddy.id)
    
    // 调用搭子钱包创建接口
    wx.request({
      url: app.globalData.apiUrl + '/buddy/wallet/create',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      data: {
        userId: userId,
        walletName: name,
        buddyCharacterIds: buddyCharacterIds,
        groupId: buddyGroupId
      },
      success: (res) => {
        if (res.data.code === 200) {
          this.handleCreateSuccess()
        } else {
          this.handleCreateError({ message: res.data.message || '创建搭子钱包失败' })
        }
      },
      fail: (error) => {
        this.handleCreateError({ message: '网络请求失败' })
      }
    })
  },

  // 处理创建成功
  handleCreateSuccess() {
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
      selectedBuddies: [],
      buddyGroupId: null,
      loading: false
    })
    
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  },

  // 处理创建失败
  handleCreateError(error) {
    wx.showToast({
      title: error.message || '创建钱包失败',
      icon: 'none'
    })
    this.setData({ loading: false })
  }
})