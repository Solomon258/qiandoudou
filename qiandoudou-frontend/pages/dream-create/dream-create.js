// pages/dream-create/dream-create.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    type: '', // shopping 或 travel
    itemId: '',
    itemName: '',
    itemIcon: '',
    suggestedPrice: '',
    landmark: '',
    
    // 表单数据
    walletName: '',
    targetAmount: '',
    travelDays: 1,
    
    loading: false
  },

  onLoad(options) {
    console.log('dream-create页面加载，参数:', options)
    
    const { 
      dreamType, type, itemId, itemName, itemIcon, suggestedAmount, suggestedPrice, landmark,
      walletName, carType, departureCity, destinationCity, travelDays, selectedTag
    } = options
    
    // 确定梦想类型
    const dreamTypeValue = dreamType || type || 'shopping'
    const actualType = dreamTypeValue === '1' ? 'shopping' : dreamTypeValue === '2' ? 'travel' : dreamTypeValue
    
    const decodedData = {
      type: actualType,
      itemId: parseInt(itemId) || 1,
      itemName: decodeURIComponent(itemName || ''),
      itemIcon: decodeURIComponent(itemIcon || '') || '/static/icon/default-product.png',
      suggestedPrice: suggestedAmount || suggestedPrice || '',
      landmark: decodeURIComponent(landmark || ''),
      walletName: walletName ? decodeURIComponent(walletName) : 
                  (actualType === 'shopping' ? `我的${decodeURIComponent(itemName || '')}` : `${decodeURIComponent(itemName || '')}之旅`),
      targetAmount: suggestedAmount || suggestedPrice || '',
      travelDays: parseInt(travelDays) || 1,
      
      // 额外的弹框数据
      carType: carType ? decodeURIComponent(carType) : '',
      departureCity: departureCity ? decodeURIComponent(departureCity) : '',
      destinationCity: destinationCity ? decodeURIComponent(destinationCity) : '',
      selectedTag: selectedTag ? decodeURIComponent(selectedTag) : ''
    }
    
    console.log('解码后的数据:', decodedData)
    
    this.setData(decodedData)
  },

  onShow() {
    console.log('dream-create页面显示，当前数据:', this.data)
  },

  // 输入钱包名称
  onWalletNameInput(e) {
    this.setData({
      walletName: e.detail.value
    })
  },

  // 输入目标金额
  onTargetAmountInput(e) {
    this.setData({
      targetAmount: e.detail.value
    })
  },

  // 输入旅行天数
  onTravelDaysInput(e) {
    this.setData({
      travelDays: parseInt(e.detail.value) || 1
    })
  },

  // 创建梦想钱包
  createDreamWallet() {
    console.log('🚀 用户点击了创建梦想钱包按钮!')
    console.log('开始创建梦想钱包...')
    
    const { walletName, targetAmount, type, itemId, travelDays } = this.data
    const userId = app.globalData.userInfo?.id
    
    console.log('创建参数:', { walletName, targetAmount, type, itemId, travelDays, userId })

    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    if (!walletName.trim()) {
      wx.showToast({
        title: '请输入钱包名称',
        icon: 'none'
      })
      return
    }

    if (!targetAmount || parseFloat(targetAmount) <= 0) {
      wx.showToast({
        title: '请输入有效的目标金额',
        icon: 'none'
      })
      return
    }

    if (type === 'travel' && (!travelDays || travelDays < 1 || travelDays > 30)) {
      wx.showToast({
        title: '旅行天数请输入1-30天',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    // 调用创建梦想钱包接口
    const dreamType = type === 'shopping' ? 1 : 2 // 1: 购物, 2: 旅行
    
    walletAPI.createDreamWallet(
      userId, 
      dreamType, 
      walletName.trim(), 
      parseFloat(targetAmount), 
      itemId, 
      type === 'travel' ? travelDays : null
    )
      .then(result => {
        console.log('创建梦想钱包成功，返回数据:', result)
        
        wx.showToast({
          title: '创建成功',
          icon: 'success'
        })
        
        // 通知首页刷新数据
        const pages = getCurrentPages()
        if (pages.length > 2) {
          const homePage = pages[pages.length - 3] // 首页
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
            wx.navigateBack({ delta: 2 })
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
        wx.showToast({
          title: error.message || '创建失败',
          icon: 'none'
        })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  }
})