// pages/buddy-group-select/buddy-group-select.js
const app = getApp()

Page({
  data: {
    buddyGroups: [],
    loading: true
  },

  onLoad(options) {
    this.loadBuddyGroups()
  },

  // 加载搭子圈子数据
  loadBuddyGroups() {
    wx.showLoading({
      title: '加载中...'
    })

    wx.request({
      // url: app.globalData.apiUrl + '/buddy/groups',
      url: 'https://heartllo.cn/api/buddy/groups',
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          this.setData({
            buddyGroups: res.data.data || [],
            loading: false
          })
        } else {
          wx.showToast({
            title: res.data.message || '加载失败',
            icon: 'none'
          })
          this.setData({ loading: false })
        }
      },
      fail: (error) => {
        wx.hideLoading()
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        this.setData({ loading: false })
      }
    })
  },

  // 选择圈子
  selectGroup(e) {
    const groupId = e.currentTarget.dataset.id
    const group = this.data.buddyGroups.find(g => g.id === groupId)
    
    if (!group) {
      wx.showToast({
        title: '圈子不存在',
        icon: 'none'
      })
      return
    }

    // 跳转到圈中选角页面
    wx.navigateTo({
      url: `/pages/buddy-group-character-select/buddy-group-character-select?groupId=${groupId}&groupName=${encodeURIComponent(group.name)}&backgroundImage=${encodeURIComponent(group.backgroundImage || '')}`
    })
  }
})
