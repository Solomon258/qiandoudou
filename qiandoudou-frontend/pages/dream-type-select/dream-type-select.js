// pages/dream-type-select/dream-type-select.js
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    currentTab: 0, // 0: 购物, 1: 旅行
    // 购物商品数据
    shoppingItems: [],
    allShoppingItems: [], // 存储所有购物商品（用于搜索）
    // 旅行目的地数据
    travelItems: [],
    allTravelItems: [], // 存储所有旅行目的地（用于搜索）
    // 搜索关键词
    searchKeyword: '',
    // 动画延迟数据
    bubblePositions: [
      { animationDelay: 0 },
      { animationDelay: 0.5 },
      { animationDelay: 1.0 },
      { animationDelay: 1.5 },
      { animationDelay: 2.0 }
    ],

    // 加载状态
    loading: false,
    
    // 弹框相关数据
    showShoppingModal: false,
    showTravelModal: false,
    shoppingModalData: {
      itemId: '',
      itemName: '',
      itemIcon: '',
      modalImage: '', // 弹框中显示的商品图片
      walletName: '',
      selectedOption: '', // 改为通用选项名称
      targetAmount: '',
      options: [], // 动态选项列表（如：['法拉利', '兰博基尼', '特斯拉']）
      quickAmounts: [], // 动态金额列表（根据选中选项变化）
      optionConfig: null // 存储完整的选项配置
    },
    travelModalData: {
      itemId: '',
      itemName: '',
      itemIcon: '',
      modalImage: '', // 弹框中显示的商品图片
      walletName: '',
      selectedTag: '',
      departureCity: '深圳',
      destinationCity: '',
      travelDays: 7,
      targetAmount: '',
      tags: ['说走就走', '去班味', '牛马向往'],
      quickCities: ['北京', '上海', '深圳', '广州'],
      quickAmounts: ['¥3000', '¥5000', '¥10000']
    }
  },

  onLoad(options) {
    // 加载商品和目的地数据
    this.loadShoppingItems()
    this.loadTravelItems()
  },

  onShow() {
    // 页面显示时不需要特殊处理
  },

  /**
   * 加载购物商品列表
   */
  async loadShoppingItems() {
    try {
      const result = await walletAPI.getDreamItems(1) // category = 1 表示购物
      console.log('购物商品API返回:', result)

      if (result && result.data) {
        const items = result.data.map(item => ({
          id: item.id,
          name: item.displayName,
          icon: item.bubbleImageUrl || item.imageUrl,
          modalImage: item.modalImageUrl || item.imageUrl, // 弹框图片，fallback到image_url
          price: item.suggestedAmount ? item.suggestedAmount.toString() : '0',
          optionConfig: item.optionConfig ? JSON.parse(item.optionConfig) : null // 解析选项配置
        }))

        console.log('映射后的购物商品:', items)

        this.setData({
          shoppingItems: items,
          allShoppingItems: items // 保存完整列表用于搜索
        })
      }
    } catch (error) {
      console.error('加载购物商品失败:', error)
      wx.showToast({
        title: '加载购物商品失败',
        icon: 'none'
      })
    }
  },

  /**
   * 加载旅行目的地列表
   */
  async loadTravelItems() {
    try {
      const result = await walletAPI.getDreamItems(2) // category = 2 表示旅行
      console.log('旅行目的地API返回:', result)

      if (result && result.data) {
        const items = result.data.map(item => ({
          id: item.id,
          name: item.displayName,
          icon: item.bubbleImageUrl || item.imageUrl,
          modalImage: item.modalImageUrl || item.imageUrl, // 弹框图片，fallback到image_url
          landmark: item.description || ''
        }))

        console.log('映射后的旅行目的地:', items)

        this.setData({
          travelItems: items,
          allTravelItems: items // 保存完整列表用于搜索
        })
      }
    } catch (error) {
      console.error('加载旅行目的地失败:', error)
      wx.showToast({
        title: '加载旅行目的地失败',
        icon: 'none'
      })
    }
  },

  /**
   * 切换标签页
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab

    // 切换标签时清空搜索框并恢复所有数据
    this.setData({
      currentTab: parseInt(tab),
      searchKeyword: '',
      shoppingItems: this.data.allShoppingItems,
      travelItems: this.data.allTravelItems
    })
  },

  /**
   * 选择购物商品
   */
  selectShoppingItem(e) {
    const itemId = e.currentTarget.dataset.id
    const item = this.data.shoppingItems.find(item => item.id == itemId)

    if (item) {
      console.log('selectShoppingItem - 选中的商品:', item)

      // 设置选项配置
      let options = []
      let quickAmounts = []
      let optionConfig = null

      if (item.optionConfig && item.optionConfig.options) {
        optionConfig = item.optionConfig
        options = item.optionConfig.options.map(opt => opt.label)

        // 默认选中第一个选项，并设置对应的金额
        if (item.optionConfig.options.length > 0) {
          const firstOption = item.optionConfig.options[0]
          quickAmounts = firstOption.suggestedAmounts.map(amount => `¥${amount}`)
          this.setData({
            'shoppingModalData.selectedOption': firstOption.label
          })
        }
      } else {
        // 如果没有配置，使用默认值（向后兼容）
        options = ['法拉利', '兰博基尼', '特斯拉']
        quickAmounts = ['¥10000', '¥50000', '¥100000']
      }

      this.setData({
        showShoppingModal: true,
        'shoppingModalData.itemId': item.id,
        'shoppingModalData.itemName': item.name,
        'shoppingModalData.itemIcon': item.icon,
        'shoppingModalData.modalImage': item.modalImage || item.icon, // 使用modalImage或fallback到icon
        'shoppingModalData.walletName': `我的${item.name}`,
        'shoppingModalData.targetAmount': item.price || '',
        'shoppingModalData.options': options,
        'shoppingModalData.quickAmounts': quickAmounts,
        'shoppingModalData.optionConfig': optionConfig
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
      console.log('selectTravelItem - 选中的目的地:', item)
      this.setData({
        showTravelModal: true,
        'travelModalData.itemId': item.id,
        'travelModalData.itemName': item.name,
        'travelModalData.itemIcon': item.icon,
        'travelModalData.modalImage': item.modalImage || item.icon, // 使用modalImage或fallback到icon
        'travelModalData.walletName': `${item.name}之旅`,
        'travelModalData.destinationCity': item.name
      })
    }
  },

  /**
   * 搜索输入
   */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword
    })
  },

  /**
   * 搜索确认（点击回车）
   */
  onSearchConfirm(e) {
    const keyword = e.detail.value.trim()
    this.performSearch(keyword)
  },

  /**
   * 执行搜索
   */
  performSearch(keyword) {
    console.log('搜索关键词:', keyword)

    if (!keyword) {
      // 如果关键词为空，显示所有数据
      this.setData({
        shoppingItems: this.data.allShoppingItems,
        travelItems: this.data.allTravelItems
      })
      return
    }

    const lowerKeyword = keyword.toLowerCase()

    // 搜索购物商品
    const filteredShopping = this.data.allShoppingItems.filter(item =>
      item.name.toLowerCase().includes(lowerKeyword)
    )

    // 搜索旅行目的地
    const filteredTravel = this.data.allTravelItems.filter(item =>
      item.name.toLowerCase().includes(lowerKeyword) ||
      (item.landmark && item.landmark.toLowerCase().includes(lowerKeyword))
    )

    this.setData({
      shoppingItems: filteredShopping,
      travelItems: filteredTravel
    })

    // 如果当前标签页没有搜索结果，提示用户
    if (this.data.currentTab === 0 && filteredShopping.length === 0) {
      wx.showToast({
        title: '未找到相关商品',
        icon: 'none'
      })
    } else if (this.data.currentTab === 1 && filteredTravel.length === 0) {
      wx.showToast({
        title: '未找到相关目的地',
        icon: 'none'
      })
    }
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
   * 选择产品选项（车型、品牌等）
   */
  selectProductOption(e) {
    const selectedOption = e.currentTarget.dataset.type
    const { optionConfig } = this.data.shoppingModalData

    if (optionConfig && optionConfig.options) {
      // 根据选中的选项找到对应建议金额
      const selectedOptionConfig = optionConfig.options.find(opt => opt.label === selectedOption)
      if (selectedOptionConfig) {
        const quickAmounts = selectedOptionConfig.suggestedAmounts.map(amount => `¥${amount}`)
        this.setData({
          'shoppingModalData.selectedOption': selectedOption,
          'shoppingModalData.quickAmounts': quickAmounts
        })
      }
    } else {
      // 向后兼容：使用默认选项
      this.setData({
        'shoppingModalData.selectedOption': selectedOption
      })
    }
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
    const app = getApp()
    const { walletAPI } = require('../../utils/api.js')
    
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

    const userId = app.globalData.userInfo?.id
    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    // 直接创建梦想钱包
    this.setData({ loading: true })
    
    walletAPI.createDreamWallet(
      userId, 
      1, // 购物类型
      shoppingModalData.walletName.trim(), 
      parseFloat(shoppingModalData.targetAmount), 
      shoppingModalData.itemId, 
      null // 购物类型不需要天数
    )
      .then(result => {
        console.log('创建购物梦想钱包成功，返回数据:', result)
        
        wx.showToast({
          title: '创建成功',
          icon: 'success'
        })
        
        // 通知首页刷新数据
        const pages = getCurrentPages()
        if (pages.length > 1) {
          const homePage = pages[pages.length - 2] // 首页
          if (homePage && homePage.loadWallets) {
            homePage.loadWallets()
          }
        }
        
        // 获取钱包ID
        const walletId = result.data?.walletId || result.data?.id || result.walletId || result.id
        console.log('提取的钱包ID:', walletId)
        
        if (!walletId) {
          console.error('无法获取钱包ID，返回数据:', result)
          wx.showToast({
            title: '创建成功，但跳转失败',
            icon: 'none'
          })
          // 如果没有钱包ID，就返回首页
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
          return
        }
        
        setTimeout(() => {
          // 跳转到梦想详情页
          console.log('准备跳转到详情页，钱包ID:', walletId)
          wx.redirectTo({
            url: `/pages/dream-detail/dream-detail?walletId=${walletId}`
          })
        }, 1500)
      })
      .catch(error => {
        console.error('创建购物梦想钱包失败:', error)
        wx.showToast({
          title: error.message || '创建失败',
          icon: 'none'
        })
      })
      .finally(() => {
        this.setData({ loading: false })
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
      'travelModalData.walletName': tag  // 将标签内容填充到钱包名称，选中状态自动更新
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
   * 选择快捷城市（填充到出发地）
   */
  selectTravelQuickCity(e) {
    const city = e.currentTarget.dataset.city
    this.setData({
      'travelModalData.departureCity': city  // 填充到出发地
    })
  },

  /**
   * 输入旅行天数（只允许正整数）
   */
  onTravelDaysInput(e) {
    let value = e.detail.value
    // 只保留数字
    value = value.replace(/[^\d]/g, '')
    // 转换为整数
    let days = parseInt(value) || ''
    // 确保是正整数
    if (days && days < 1) {
      days = 1
    }
    this.setData({
      'travelModalData.travelDays': days
    })
  },

  /**
   * 输入旅行目标金额
   */
  onTravelTargetAmountInput(e) {
    this.setData({
      'travelModalData.targetAmount': e.detail.value
    })
  },

  /**
   * 选择旅行快捷金额
   */
  selectTravelQuickAmount(e) {
    const amount = e.currentTarget.dataset.amount.replace('¥', '')
    this.setData({
      'travelModalData.targetAmount': amount
    })
  },

  /**
   * 确认创建旅行梦想钱包
   */
  confirmCreateTravel() {
    const { travelModalData } = this.data
    const app = getApp()
    const { walletAPI } = require('../../utils/api.js')

    if (!travelModalData.walletName.trim()) {
      wx.showToast({
        title: '请输入梦想攒的名称',
        icon: 'none'
      })
      return
    }

    if (!travelModalData.departureCity.trim()) {
      wx.showToast({
        title: '请输入出发地',
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

    const userId = app.globalData.userInfo?.id
    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    // 直接创建梦想钱包
    this.setData({ loading: true })

    // 获取目标金额，如果没有则使用建议金额或0
    const targetAmount = travelModalData.targetAmount ? parseFloat(travelModalData.targetAmount) : 0

    walletAPI.createDreamWallet(
      userId,
      2, // 旅行类型
      travelModalData.walletName.trim(),
      targetAmount,
      travelModalData.itemId,
      travelModalData.travelDays
    )
      .then(result => {
        console.log('创建旅行梦想钱包成功，返回数据:', result)
        
        wx.showToast({
          title: '创建成功',
          icon: 'success'
        })
        
        // 通知首页刷新数据
        const pages = getCurrentPages()
        if (pages.length > 1) {
          const homePage = pages[pages.length - 2] // 首页
          if (homePage && homePage.loadWallets) {
            homePage.loadWallets()
          }
        }
        
        // 获取钱包ID
        const walletId = result.data?.walletId || result.data?.id || result.walletId || result.id
        console.log('提取的钱包ID:', walletId)
        
        if (!walletId) {
          console.error('无法获取钱包ID，返回数据:', result)
          wx.showToast({
            title: '创建成功，但跳转失败',
            icon: 'none'
          })
          // 如果没有钱包ID，就返回首页
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
          return
        }
        
        setTimeout(() => {
          // 跳转到梦想详情页
          console.log('准备跳转到详情页，钱包ID:', walletId)
          wx.redirectTo({
            url: `/pages/dream-detail/dream-detail?walletId=${walletId}`
          })
        }, 1500)
      })
      .catch(error => {
        console.error('创建旅行梦想钱包失败:', error)
        wx.showToast({
          title: error.message || '创建失败',
          icon: 'none'
        })
      })
      .finally(() => {
        this.setData({ loading: false })
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
