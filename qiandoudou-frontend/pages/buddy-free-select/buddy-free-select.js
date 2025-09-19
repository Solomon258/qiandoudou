// pages/buddy-free-select/buddy-free-select.js
const app = getApp()

Page({
  data: {
    currentIndex: 0, // 当前选中的搭子索引
    buddyCharacters: [], // 所有可选搭子
    buddyCharactersWithSelection: [], // 带有选择状态的搭子列表
    selectedBuddies: [], // 已选择的搭子
    loading: true,
    voicePlaying: false, // 语音播放状态
    playingIndex: -1 // 当前播放语音的搭子索引
  },

  onLoad(options) {
    // 先测试基础API
    this.testBuddyApi()
    this.loadBuddyCharacters()
  },

  // 测试搭子API是否可用
  testBuddyApi() {
    wx.request({
      url: app.globalData.apiUrl + '/buddy/test',
      method: 'GET',
      success: (res) => {
        console.log('搭子API测试结果:', res)
      },
      fail: (error) => {
        console.error('搭子API测试失败:', error)
      }
    })
  },

  // 加载搭子角色数据
  loadBuddyCharacters() {
    wx.showLoading({
      title: '加载中...'
    })

    wx.request({
      url: app.globalData.apiUrl + '/buddy/characters/free',
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

  // 点击选择框
  onSelectTap(e) {
    const index = e.currentTarget.dataset.index
    const buddy = this.data.buddyCharacters[index]
    let selectedBuddies = [...this.data.selectedBuddies]

    // 检查是否已选择
    const existIndex = selectedBuddies.findIndex(item => item.id === buddy.id)
    
    if (existIndex >= 0) {
      // 已选择，移除
      selectedBuddies.splice(existIndex, 1)
    } else {
      // 未选择，添加
      if (selectedBuddies.length >= 6) {
        wx.showToast({
          title: '最多选择6个搭子',
          icon: 'none'
        })
        return
      }
      selectedBuddies.push(buddy)
    }

    this.setData({
      selectedBuddies
    })
  },

  // 移除已选择的搭子
  onRemoveBuddy(e) {
    const buddyId = e.currentTarget.dataset.id
    let selectedBuddies = this.data.selectedBuddies.filter(buddy => buddy.id !== buddyId)
    
    this.setData({
      selectedBuddies
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

  // 确认选择
  onConfirmSelection() {
    const selectedBuddies = this.data.selectedBuddies
    
    if (selectedBuddies.length < 2) {
      wx.showToast({
        title: '至少选择2个搭子',
        icon: 'none'
      })
      return
    }

    // 直接创建搭子钱包
    this.createBuddyWallet(selectedBuddies, null)
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
    const walletName = `${selectedBuddies.map(b => b.name).join('、')}的搭子攒钱`
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
        backgroundImage: "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dazi/dazizan_beijingtu.jpg"
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          wx.showToast({
            title: '搭子钱包创建成功',
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
  },

  // 检查搭子是否已选择（用于模板中的条件判断）
  isBuddySelected(buddyId) {
    return this.data.selectedBuddies.some(buddy => buddy.id === buddyId)
  },

  // 获取带有选择状态的搭子列表
  getBuddyCharactersWithSelection() {
    const selectedIds = this.data.selectedBuddies.map(buddy => buddy.id)
    return this.data.buddyCharacters.map(buddy => ({
      ...buddy,
      isSelected: selectedIds.includes(buddy.id)
    }))
  }
})
