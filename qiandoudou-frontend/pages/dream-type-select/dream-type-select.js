// pages/dream-type-select/dream-type-select.js
Page({
  data: {
    currentTab: 0, // 0: 购物, 1: 旅行
    // 购物商品数据
    shoppingItems: [
      { id: 1, name: '轿车', icon: '/static/icon/paopao/shopping/轿车@3x.png', price: '100000' },
      { id: 2, name: 'SUV', icon: '/static/icon/paopao/shopping/suv@3x.png', price: '150000' },
      { id: 3, name: '超跑', icon: '/static/icon/paopao/shopping/超跑@3x.png', price: '500000' },
      { id: 4, name: '包包', icon: '/static/icon/paopao/shopping/包包@3x.png', price: '5000' },
      { id: 5, name: '劳力士', icon: '/static/icon/paopao/shopping/劳力士@3x.png', price: '50000' }
    ],
    // 旅行目的地数据
    travelItems: [
      { id: 1, name: '北京', icon: '/static/icon/paopao/travel/编组 2.png', landmark: '天安门' },
      { id: 2, name: '西安', icon: '/static/icon/paopao/travel/编组 3.png', landmark: '兵马俑' },
      { id: 3, name: '乌鲁木齐', icon: '/static/icon/paopao/travel/编组 4.png', landmark: '天山' },
      { id: 4, name: '悉尼', icon: '/static/icon/paopao/travel/编组 5.png', landmark: '歌剧院' },
      { id: 5, name: '东京', icon: '/static/icon/paopao/travel/编组 6.png', landmark: '富士山' }
    ],
    // 动画延迟数据
    bubblePositions: [
      { animationDelay: 0 },
      { animationDelay: 0.5 },
      { animationDelay: 1.0 },
      { animationDelay: 1.5 },
      { animationDelay: 2.0 }
    ],
    
    // 弹框相关数据
    showShoppingModal: false,
    showTravelModal: false,
    shoppingModalData: {
      itemId: '',
      itemName: '',
      itemIcon: '',
      walletName: '',
      selectedCarType: '',
      targetAmount: '',
      carTypes: ['法拉利', '兰博基尼', '特斯拉'],
      quickAmounts: ['¥10000', '¥50000', '¥100000']
    },
    travelModalData: {
      itemId: '',
      itemName: '',
      itemIcon: '',
      walletName: '',
      selectedTag: '',
      departureCity: '深圳',
      destinationCity: '',
      travelDays: 7,
      tags: ['说走就走', '去班味', '牛马向往'],
      quickCities: ['悉什', '四勒格', '计算机']
    }
  },

  onLoad(options) {
    // 页面加载时不需要特殊处理
  },

  onShow() {
    // 页面显示时不需要特殊处理
  },

  /**
   * 切换标签页
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({
      currentTab: parseInt(tab)
    })
  },

  /**
   * 选择购物商品
   */
  selectShoppingItem(e) {
    const itemId = e.currentTarget.dataset.id
    const item = this.data.shoppingItems.find(item => item.id == itemId)
    
    if (item) {
      this.setData({ 
        showShoppingModal: true,
        'shoppingModalData.itemId': item.id,
        'shoppingModalData.itemName': item.name,
        'shoppingModalData.itemIcon': item.icon,
        'shoppingModalData.walletName': `我的${item.name}`,
        'shoppingModalData.targetAmount': item.price || ''
      })
    }
  },

  /**
   * 选择旅行目的地
   */
  selectTravelItem(e) {
    const itemId = e.currentTarget.dataset.id
    const item = this.data.travelItems.find(item => item.id == itemId)
    
    if (item) {
      this.setData({ 
        showTravelModal: true,
        'travelModalData.itemId': item.id,
        'travelModalData.itemName': item.name,
        'travelModalData.itemIcon': item.icon,
        'travelModalData.walletName': `${item.name}之旅`,
        'travelModalData.destinationCity': item.name
      })
    }
  },

  /**
   * 搜索功能
   */
  onSearchInput(e) {
    const keyword = e.detail.value.toLowerCase()
    // 这里可以实现搜索逻辑
    console.log('搜索关键词:', keyword)
  },

  // ===== 购物弹框相关方法 =====
  
  /**
   * 关闭购物弹框
   */
  closeShoppingModal() {
    this.setData({ showShoppingModal: false })
  },

  /**
   * 输入购物钱包名称
   */
  onShoppingWalletNameInput(e) {
    this.setData({
      'shoppingModalData.walletName': e.detail.value
    })
  },

  /**
   * 选择车型
   */
  selectCarType(e) {
    const carType = e.currentTarget.dataset.type
    this.setData({
      'shoppingModalData.selectedCarType': carType
    })
  },

  /**
   * 输入购物目标金额
   */
  onShoppingTargetAmountInput(e) {
    this.setData({
      'shoppingModalData.targetAmount': e.detail.value
    })
  },

  /**
   * 选择购物快捷金额
   */
  selectShoppingQuickAmount(e) {
    const amount = e.currentTarget.dataset.amount.replace('¥', '')
    this.setData({
      'shoppingModalData.targetAmount': amount
    })
  },

  /**
   * 确认创建购物梦想钱包
   */
  confirmCreateShopping() {
    const { shoppingModalData } = this.data
    
    if (!shoppingModalData.walletName.trim()) {
      wx.showToast({
        title: '请输入梦想攒的名称',
        icon: 'none'
      })
      return
    }

    if (!shoppingModalData.targetAmount || parseFloat(shoppingModalData.targetAmount) <= 0) {
      wx.showToast({
        title: '请输入有效的金额',
        icon: 'none'
      })
      return
    }

    // 跳转到创建页面
    wx.navigateTo({
      url: `/pages/dream-create/dream-create?dreamType=1&itemId=${shoppingModalData.itemId}&itemName=${encodeURIComponent(shoppingModalData.itemName)}&suggestedAmount=${shoppingModalData.targetAmount}&walletName=${encodeURIComponent(shoppingModalData.walletName)}&carType=${encodeURIComponent(shoppingModalData.selectedCarType)}`
    })
    
    this.closeShoppingModal()
  },

  // ===== 旅行弹框相关方法 =====
  
  /**
   * 关闭旅行弹框
   */
  closeTravelModal() {
    this.setData({ showTravelModal: false })
  },

  /**
   * 输入旅行钱包名称
   */
  onTravelWalletNameInput(e) {
    this.setData({
      'travelModalData.walletName': e.detail.value
    })
  },

  /**
   * 选择旅行标签
   */
  selectTravelTag(e) {
    const tag = e.currentTarget.dataset.tag
    this.setData({
      'travelModalData.selectedTag': tag
    })
  },

  /**
   * 输入出发地
   */
  onDepartureCityInput(e) {
    this.setData({
      'travelModalData.departureCity': e.detail.value
    })
  },

  /**
   * 输入目的地
   */
  onDestinationCityInput(e) {
    this.setData({
      'travelModalData.destinationCity': e.detail.value
    })
  },

  /**
   * 选择快捷城市
   */
  selectTravelQuickCity(e) {
    const city = e.currentTarget.dataset.city
    this.setData({
      'travelModalData.destinationCity': city
    })
  },

  /**
   * 输入旅游天数
   */
  onTravelDaysInput(e) {
    this.setData({
      'travelModalData.travelDays': parseInt(e.detail.value) || 1
    })
  },

  /**
   * 确认创建旅行梦想钱包
   */
  confirmCreateTravel() {
    const { travelModalData } = this.data
    
    if (!travelModalData.walletName.trim()) {
      wx.showToast({
        title: '请输入梦想攒的名称',
        icon: 'none'
      })
      return
    }

    if (!travelModalData.destinationCity.trim()) {
      wx.showToast({
        title: '请输入目的地',
        icon: 'none'
      })
      return
    }

    if (!travelModalData.travelDays || travelModalData.travelDays < 1) {
      wx.showToast({
        title: '请输入有效的旅游天数',
        icon: 'none'
      })
      return
    }

    // 跳转到创建页面
    wx.navigateTo({
      url: `/pages/dream-create/dream-create?dreamType=2&itemId=${travelModalData.itemId}&itemName=${encodeURIComponent(travelModalData.itemName)}&walletName=${encodeURIComponent(travelModalData.walletName)}&departureCity=${encodeURIComponent(travelModalData.departureCity)}&destinationCity=${encodeURIComponent(travelModalData.destinationCity)}&travelDays=${travelModalData.travelDays}&selectedTag=${encodeURIComponent(travelModalData.selectedTag)}`
    })
    
    this.closeTravelModal()
  },

  /**
   * 阻止弹框内容区域点击事件冒泡
   */
  preventModalClose() {
    // 空函数，用于阻止事件冒泡到overlay层
  }
})
