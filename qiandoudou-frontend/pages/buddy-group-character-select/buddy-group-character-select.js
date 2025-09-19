// pages/buddy-group-character-select/buddy-group-character-select.js
const app = getApp()

Page({
  data: {
    groupId: null,
    groupName: '',
    backgroundImage: '',
    currentIndex: 0,
    buddyCharacters: [],
    loading: true,
    voicePlaying: false,
    playingIndex: -1
  },

  onLoad(options) {
    const groupId = parseInt(options.groupId)
    const groupName = decodeURIComponent(options.groupName || '')
    const backgroundImage = decodeURIComponent(options.backgroundImage || '')
    
    this.setData({
      groupId,
      groupName,
      backgroundImage
    })
    
    this.loadGroupCharacters(groupId)
  },

  // 加载圈子角色数据
  loadGroupCharacters(groupId) {
    wx.showLoading({
      title: '加载中...'
    })

    wx.request({
      url: app.globalData.apiUrl + `/buddy/characters/group/${groupId}`,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          this.setData({
            buddyCharacters: res.data.data || [],
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

  // 处理轮播滑动
  onSwiperChange(e) {
    if (!e || !e.detail) return
    
    const current = e.detail.current || 0
    this.setData({
      currentIndex: current,
      voicePlaying: false,
      playingIndex: -1
    })
  },

  // 点击头像切换
  onAvatarTap(e) {
    const index = e.currentTarget.dataset.index
    this.setData({
      currentIndex: index,
      voicePlaying: false,
      playingIndex: -1
    })
  },

  // 播放语音介绍
  onPlayVoice() {
    const currentBuddy = this.data.buddyCharacters[this.data.currentIndex]
    
    if (!currentBuddy.voiceSampleUrl) {
      wx.showToast({
        title: '暂无语音样例',
        icon: 'none'
      })
      return
    }

    if (this.data.voicePlaying) {
      // 停止播放
      wx.stopBackgroundAudio()
      this.setData({
        voicePlaying: false,
        playingIndex: -1
      })
    } else {
      // 开始播放
      wx.playBackgroundAudio({
        dataUrl: currentBuddy.voiceSampleUrl,
        title: currentBuddy.name + '的自我介绍'
      })
      
      this.setData({
        voicePlaying: true,
        playingIndex: this.data.currentIndex
      })

      // 监听播放完成
      wx.onBackgroundAudioStop(() => {
        this.setData({
          voicePlaying: false,
          playingIndex: -1
        })
      })
    }
  },

  // 确认加入圈子
  onConfirmJoinGroup() {
    const buddyCharacters = this.data.buddyCharacters
    
    if (buddyCharacters.length === 0) {
      wx.showToast({
        title: '圈子暂无成员',
        icon: 'none'
      })
      return
    }

    // 直接创建搭子钱包
    this.createBuddyWallet(buddyCharacters, this.data.groupId)
  },

  // 创建搭子钱包
  createBuddyWallet(selectedBuddies, buddyGroupId) {
    const userId = app.globalData.userInfo?.id
    
    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    // 生成钱包名称
    const walletName = buddyGroupId ? `${this.data.groupName}攒钱团` : `${selectedBuddies.map(b => b.name).join('、')}的搭子攒钱`
    const buddyCharacterIds = selectedBuddies.map(buddy => buddy.id)
    
    wx.showLoading({
      title: '创建钱包中...'
    })
    
    // 调用搭子钱包创建接口
    wx.request({
      url: app.globalData.apiUrl + '/buddy/wallet/create',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      data: {
        userId: userId,
        walletName: walletName,
        buddyCharacterIds: buddyCharacterIds,
        groupId: buddyGroupId,
        backgroundImage: this.data.backgroundImage || "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dazi/dazizan_beijingtu.jpg"
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          wx.showToast({
            title: `加入${this.data.groupName}成功`,
            icon: 'success'
          })
          
          // 跳转到钱包详情页，清除页面栈确保返回到首页
          setTimeout(() => {
            wx.reLaunch({
              url: `/pages/wallet-detail/wallet-detail?id=${res.data.data.walletId}`
            })
          }, 1500)
        } else {
          wx.showToast({
            title: res.data.message || '创建搭子钱包失败',
            icon: 'none'
          })
        }
      },
      fail: (error) => {
        wx.hideLoading()
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      }
    })
  }
})
