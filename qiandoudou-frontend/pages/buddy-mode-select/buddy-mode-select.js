// pages/buddy-mode-select/buddy-mode-select.js
Page({
  data: {
    
  },

  onLoad(options) {
    
  },

  // 选择自由选搭模式
  selectFreeMode() {
    wx.navigateTo({
      url: '/pages/buddy-free-select/buddy-free-select'
    })
  },

  // 选择加入圈子模式
  selectGroupMode() {
    wx.navigateTo({
      url: '/pages/buddy-group-select/buddy-group-select'
    })
  }
})
