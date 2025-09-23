// pages/dream-travel-select/dream-travel-select.js
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    searchKeyword: '',
    dreamItems: [],
    filteredItems: [],
    selectedItem: null,
    bubbleAnimations: [], // 存储气泡动画数据
    
    // 弹框相关数据
    showModal: false,
    modalData: {
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
    this.loadDreamItems()
  },

  onReady() {
    this.startBubbleAnimations()
  },

  /**
   * 加载旅行目的地列表
   */
  async loadDreamItems() {
    try {
      wx.showLoading({ title: '加载中...' })
      
      // 模拟旅行目的地数据
      const mockData = [
        { id: 1, displayName: '北京', imageUrl: '/static/icon/paopao/travel/编组 2.png', suggestedAmount: 5000 },
        { id: 2, displayName: '西安', imageUrl: '/static/icon/paopao/travel/编组 3.png', suggestedAmount: 3000 },
        { id: 3, displayName: '乌鲁木齐', imageUrl: '/static/icon/paopao/travel/编组 4.png', suggestedAmount: 8000 },
        { id: 4, displayName: '悉尼', imageUrl: '/static/icon/paopao/travel/编组 5.png', suggestedAmount: 15000 },
        { id: 5, displayName: '东京', imageUrl: '/static/icon/paopao/travel/编组 6.png', suggestedAmount: 12000 }
      ]
      
      this.setData({
        dreamItems: mockData,
        filteredItems: mockData
      })
      this.initBubbleAnimations()
    } catch (error) {
      console.error('加载旅行目的地失败:', error)
      wx.showToast({
        title: '网络错误',
        icon: 'none'
      })
    } finally {
      wx.hideLoading()
    }
  },

  /**
   * 搜索目的地
   */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })
    this.filterItems(keyword)
  },

  /**
   * 过滤目的地
   */
  filterItems(keyword) {
    if (!keyword.trim()) {
      this.setData({ filteredItems: this.data.dreamItems })
      return
    }

    const filtered = this.data.dreamItems.filter(item => 
      item.displayName.includes(keyword) || 
      item.name?.includes(keyword) ||
      (item.description && item.description.includes(keyword))
    )
    
    this.setData({ filteredItems: filtered })
  },

  /**
   * 选择目的地
   */
  selectItem(e) {
    const itemId = e.currentTarget.dataset.id
    const item = this.data.dreamItems.find(i => i.id === itemId)
    
    if (item) {
      this.setData({ 
        selectedItem: item,
        showModal: true,
        'modalData.itemId': item.id,
        'modalData.itemName': item.displayName,
        'modalData.itemIcon': item.imageUrl,
        'modalData.walletName': `${item.displayName}之旅`,
        'modalData.destinationCity': item.displayName
      })
    }
  },

  /**
   * 初始化气泡动画
   */
  initBubbleAnimations() {
    const itemsWithStyle = this.data.filteredItems.map((item, index) => {
      const animation = {
        x: Math.random() * 300 + 50, // 随机X位置
        y: Math.random() * 400 + 100, // 随机Y位置
        scale: 0.8 + Math.random() * 0.4, // 随机缩放
        rotation: Math.random() * 360, // 随机旋转
        animationDelay: index * 200 // 动画延迟
      }
      
      return {
        ...item,
        bubbleStyle: `
          left: ${animation.x}rpx;
          top: ${animation.y}rpx;
          transform: scale(${animation.scale}) rotate(${animation.rotation}deg);
          animation-delay: ${animation.animationDelay}ms;
        `
      }
    })
    
    this.setData({ 
      filteredItems: itemsWithStyle,
      bubbleAnimations: itemsWithStyle.map(item => ({
        id: item.id,
        x: parseFloat(item.bubbleStyle.match(/left: ([\d.]+)rpx/)[1]),
        y: parseFloat(item.bubbleStyle.match(/top: ([\d.]+)rpx/)[1])
      }))
    })
  },

  /**
   * 开始气泡动画
   */
  startBubbleAnimations() {
    // 每3秒更新一次气泡位置
    setInterval(() => {
      const animations = this.data.bubbleAnimations.map(bubble => ({
        ...bubble,
        x: Math.max(50, Math.min(300, bubble.x + (Math.random() - 0.5) * 100)),
        y: Math.max(100, Math.min(500, bubble.y + (Math.random() - 0.5) * 100)),
        rotation: bubble.rotation + (Math.random() - 0.5) * 90
      }))
      
      this.setData({ bubbleAnimations: animations })
    }, 3000)
  },


  /**
   * 关闭弹框
   */
  closeModal() {
    this.setData({ showModal: false })
  },

  /**
   * 输入钱包名称
   */
  onWalletNameInput(e) {
    this.setData({
      'modalData.walletName': e.detail.value
    })
  },

  /**
   * 选择标签
   */
  selectTag(e) {
    const tag = e.currentTarget.dataset.tag
    this.setData({
      'modalData.selectedTag': tag
    })
  },

  /**
   * 输入出发地
   */
  onDepartureCityInput(e) {
    this.setData({
      'modalData.departureCity': e.detail.value
    })
  },

  /**
   * 输入目的地
   */
  onDestinationCityInput(e) {
    this.setData({
      'modalData.destinationCity': e.detail.value
    })
  },

  /**
   * 选择快捷城市
   */
  selectQuickCity(e) {
    const city = e.currentTarget.dataset.city
    this.setData({
      'modalData.destinationCity': city
    })
  },

  /**
   * 输入旅游天数
   */
  onTravelDaysInput(e) {
    this.setData({
      'modalData.travelDays': parseInt(e.detail.value) || 1
    })
  },

  /**
   * 确认创建梦想钱包
   */
  confirmCreate() {
    const { modalData } = this.data
    
    if (!modalData.walletName.trim()) {
      wx.showToast({
        title: '请输入梦想攒的名称',
        icon: 'none'
      })
      return
    }

    if (!modalData.destinationCity.trim()) {
      wx.showToast({
        title: '请输入目的地',
        icon: 'none'
      })
      return
    }

    if (!modalData.travelDays || modalData.travelDays < 1) {
      wx.showToast({
        title: '请输入有效的旅游天数',
        icon: 'none'
      })
      return
    }

    // 跳转到创建页面
    wx.navigateTo({
      url: `/pages/dream-create/dream-create?dreamType=2&itemId=${modalData.itemId}&itemName=${encodeURIComponent(modalData.itemName)}&walletName=${encodeURIComponent(modalData.walletName)}&departureCity=${encodeURIComponent(modalData.departureCity)}&destinationCity=${encodeURIComponent(modalData.destinationCity)}&travelDays=${modalData.travelDays}&selectedTag=${encodeURIComponent(modalData.selectedTag)}`
    })
    
    this.closeModal()
  }
})