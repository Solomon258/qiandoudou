// app.js
App({
  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    console.log('小程序启动')

    // 检查登录状态
    this.checkLoginStatus()
    
    // 检查token是否过期
    this.checkTokenExpiry()
  },

  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://localhost:8080/api' // 后端API地址
  },

  // 检查登录状态
  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      console.log('已登录用户:', userInfo)
    } else {
      console.log('未登录')
    }
  },

  // 清除登录信息
  clearLoginInfo() {
    this.globalData.token = null
    this.globalData.userInfo = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    wx.removeStorageSync('tokenTime')
  },

  // 检查是否已登录
  isLoggedIn() {
    return !!(this.globalData.token && this.globalData.userInfo)
  },

  // 检查token是否过期
  checkTokenExpiry() {
    const token = this.globalData.token
    if (!token) return

    // 简单的token过期检查（实际项目中可以解析JWT获取过期时间）
    const tokenTime = wx.getStorageSync('tokenTime')
    if (tokenTime) {
      const now = Date.now()
      const expireTime = tokenTime + 24 * 60 * 60 * 1000 // 24小时
      
      if (now > expireTime) {
        console.log('Token已过期，清除登录信息')
        this.clearLoginInfo()
      }
    }
  },

  // 更新setLoginInfo方法，记录token时间
  setLoginInfo(token, userInfo) {
    this.globalData.token = token
    this.globalData.userInfo = userInfo
    wx.setStorageSync('token', token)
    wx.setStorageSync('userInfo', userInfo)
    wx.setStorageSync('tokenTime', Date.now()) // 记录token获取时间
    
    // 安全地获取用户名称
    const userName = userInfo && (userInfo.nickname || userInfo.username) || '未知用户'
    console.log('登录信息已保存:', userName)
  }
})
