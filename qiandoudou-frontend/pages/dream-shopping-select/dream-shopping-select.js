// pages/dream-shopping-select/dream-shopping-select.js
Page({
  data: {
    showModal: false,
    dreamItems: [
      { id: 1, displayName: '轿车', imageUrl: '/static/icon/goods/轿车@3x.png', suggestedAmount: 100000 },
      { id: 2, displayName: 'SUV', imageUrl: '/static/icon/goods/suv@3x.png', suggestedAmount: 150000 },
      { id: 3, displayName: '超跑', imageUrl: '/static/icon/goods/超跑@3x.png', suggestedAmount: 500000 },
      { id: 4, displayName: '包包', imageUrl: '/static/icon/goods/包包@3x.png', suggestedAmount: 20000 },
      { id: 5, displayName: '劳力士', imageUrl: '/static/icon/goods/劳力士@3x.png', suggestedAmount: 50000 }
    ],
    modalData: {
      itemId: '',
      itemName: '',
      itemIcon: '',
      walletName: '',
      selectedCarType: '',
      targetAmount: '',
      carTypes: ['法拉利', '兰博基尼', '特斯拉'],
      quickAmounts: ['¥10000', '¥50000', '¥100000']
    }
  },

  onLoad() {
    console.log('页面加载完成')
  },

  // 选择商品 - 点击气泡触发弹框
  selectItem(e) {
    console.log('点击了商品')
    const itemId = parseInt(e.currentTarget.dataset.id)
    const item = this.data.dreamItems.find(i => i.id === itemId)
    
    if (item) {
      this.setData({ 
        showModal: true,
        'modalData.itemId': item.id,
        'modalData.itemName': item.displayName,
        'modalData.itemIcon': item.imageUrl,
        'modalData.walletName': `我的${item.displayName}`,
        'modalData.targetAmount': item.suggestedAmount || ''
      })
    }
  },

  // 关闭弹框
  closeModal() {
    this.setData({ showModal: false })
  },

  // 输入钱包名称
  onWalletNameInput(e) {
    this.setData({
      'modalData.walletName': e.detail.value
    })
  },

  // 选择车型
  selectCarType(e) {
    const carType = e.currentTarget.dataset.type
    this.setData({
      'modalData.selectedCarType': carType
    })
  },

  // 输入目标金额
  onTargetAmountInput(e) {
    this.setData({
      'modalData.targetAmount': e.detail.value
    })
  },

  // 选择快捷金额
  selectQuickAmount(e) {
    const amount = e.currentTarget.dataset.amount.replace('¥', '')
    this.setData({
      'modalData.targetAmount': amount
    })
  },

  // 确认创建
  confirmCreate() {
    const { modalData } = this.data
    
    if (!modalData.walletName.trim()) {
      wx.showToast({
        title: '请输入梦想攒的名称',
        icon: 'none'
      })
      return
    }

    if (!modalData.targetAmount || parseFloat(modalData.targetAmount) <= 0) {
      wx.showToast({
        title: '请输入有效的金额',
        icon: 'none'
      })
      return
    }

    // 跳转到创建页面
    wx.navigateTo({
      url: `/pages/dream-create/dream-create?dreamType=1&itemId=${modalData.itemId}&itemName=${encodeURIComponent(modalData.itemName)}&suggestedAmount=${modalData.targetAmount}&walletName=${encodeURIComponent(modalData.walletName)}&carType=${encodeURIComponent(modalData.selectedCarType)}`
    })
    
    this.closeModal()
  }
})