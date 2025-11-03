// pages/select-buddy-mode/select-buddy-mode.js
const app = getApp()

Page({
  data: {
    walletName: '', // 从上一页传递的钱包名称
  },

  onLoad(options) {
    // 检查登录状态
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    // 获取钱包名称
    const walletName = options.walletName || '我的搭子钱包'
    this.setData({
      walletName: walletName
    })
  },

  // 选择自由选搭模式
  selectFreeMode() {
    wx.navigateTo({
      url: `/pages/buddy-characters/buddy-characters?mode=free&walletName=${encodeURIComponent(this.data.walletName)}`
    })
  },

  // 选择加入圈子模式
  selectCircleMode() {
    wx.navigateTo({
      url: `/pages/buddy-circles/buddy-circles?walletName=${encodeURIComponent(this.data.walletName)}`
    })
  },

  // 返回上一页
  goBack() {
    wx.navigateBack({
      delta: 1
    })
  }
})
