// pages/wallet-messages/wallet-messages.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    messages: [],
    loading: true,
    currentPage: 1,
    hasMore: true
  },

  onLoad() {
    this.loadMessages()
  },

  onShow() {
    // 每次显示页面时刷新数据
    this.refreshMessages()
    
    // 标记消息为已读
    this.markMessagesAsRead()
  },

  // 返回上一页
  goBack() {
    wx.navigateBack()
  },

  // 加载互动消息
  loadMessages() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      this.setData({ loading: false })
      return
    }

    this.setData({ loading: true })

    walletAPI.getUserInteractionMessages(userId, this.data.currentPage)
      .then(result => {
        const messages = result.data || []
        const processedMessages = this.processMessages(messages)
        
        this.setData({
          messages: this.data.currentPage === 1 ? processedMessages : [...this.data.messages, ...processedMessages],
          loading: false,
          hasMore: messages.length >= 20 // 假设每页20条
        })
      })
      .catch(error => {
        
        this.setData({
          messages: [],
          loading: false,
          hasMore: false
        })
      })
  },

  // 处理消息数据
  processMessages(messages) {
    return messages.map(message => {
      return {
        ...message,
        timeText: this.formatTime(message.createdAt || message.timestamp),
        user: {
          ...message.user,
          nickname: message.user?.nickname || message.userName || '用户',
          avatar: message.user?.avatar || message.userAvatar || ''
        }
      }
    })
  },

  // 格式化时间
  formatTime(timestamp) {
    if (!timestamp) return '刚刚'
    
    const now = new Date()
    const messageTime = new Date(timestamp)
    const diff = now - messageTime
    
    const minutes = Math.floor(diff / (1000 * 60))
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    
    if (minutes < 1) {
      return '刚刚'
    } else if (minutes < 60) {
      return `${minutes}分钟前`
    } else if (hours < 24) {
      return `${hours}小时前`
    } else if (days === 1) {
      return '昨天'
    } else if (days < 7) {
      return `${days}天前`
    } else {
      // 显示具体时间
      const month = messageTime.getMonth() + 1
      const date = messageTime.getDate()
      const hour = messageTime.getHours()
      const minute = messageTime.getMinutes()
      return `${month}月${date}日 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
    }
  },

  // 刷新消息
  refreshMessages() {
    this.setData({
      currentPage: 1,
      hasMore: true
    })
    this.loadMessages()
  },

  // 加载更多消息
  loadMoreMessages() {
    if (!this.data.hasMore || this.data.loading) {
      return
    }
    
    this.setData({
      currentPage: this.data.currentPage + 1
    })
    this.loadMessages()
  },

  // 点击消息项
  onMessageTap(e) {
    const message = e.currentTarget.dataset.message
    
    if (message.walletId) {
      // 跳转到对应的钱包详情页
      wx.navigateTo({
        url: `/pages/wallet-detail/wallet-detail?id=${message.walletId}&isOthers=false`
      })
    } else if (message.type === 'follow') {
      // 跳转到用户个人主页
      wx.navigateTo({
        url: `/pages/user-social-profile/user-social-profile?userId=${message.user.id}`
      })
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.refreshMessages()
    wx.stopPullDownRefresh()
  },

  // 触底加载更多
  onReachBottom() {
    this.loadMoreMessages()
  },

  // 标记消息为已读
  markMessagesAsRead() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      return
    }

    walletAPI.markMessagesAsRead(userId)
      .then(result => {
      })
      .catch(error => {
      })
  }
})
