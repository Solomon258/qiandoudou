// pages/dream-travel-select/dream-travel-select.js
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    searchKeyword: '',
    dreamItems: [],
    filteredItems: [],
    selectedItem: null,

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

  /**
   * 加载旅行目的地列表
   */
  async loadDreamItems() {
    try {
      wx.showLoading({ title: '加载中...' })

      // 从后端API获取旅行目的地数据（category = 2 表示旅行）
      const result = await walletAPI.getDreamItems(2)

      console.log('API返回的数据:', result)

      if (result && result.data) {
        const items = result.data.map(item => {
          console.log('处理item:', item)
          return {
            id: item.id,
            displayName: item.displayName,
            imageUrl: item.bubbleImageUrl || item.imageUrl, // 使用气泡图片
            suggestedAmount: item.suggestedAmount
          }
        })

        console.log('处理后的items:', items)
        console.log('items数量:', items.length)

        this.setData({
          dreamItems: items,
          filteredItems: items
        }, () => {
          console.log('setData完成, dreamItems数量:', this.data.dreamItems.length)
          console.log('setData完成, filteredItems数量:', this.data.filteredItems.length)
        })
      } else {
        console.error('API返回数据格式错误:', result)
        wx.showToast({
          title: 'API返回数据格式错误',
          icon: 'none'
        })
      }
    } catch (error) {
      console.error('加载旅行目的地失败:', error)
      wx.showToast({
        title: '加载失败，请检查后端服务',
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