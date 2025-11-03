// pages/buddy-characters/buddy-characters.js
const app = getApp()
const { request } = require('../../utils/api.js')

Page({
  data: {
    mode: 'free', // free: 自由选搭, circle: 圈子模式
    walletName: '',
    circleId: null,
    circleName: '',
    characters: [],
    selectedCharacters: [],
    selectedIds: [],
    currentIndex: 0,
    selectedCharacter: null,
    loading: true,
    creating: false
  },

  onLoad(options) {
    // 检查登录状态
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    // 获取参数
    const mode = options.mode || 'free'
    const walletName = decodeURIComponent(options.walletName || '我的搭子钱包')
    const circleId = options.circleId ? parseInt(options.circleId) : null
    const circleName = options.circleName ? decodeURIComponent(options.circleName) : ''

    this.setData({
      mode: mode,
      walletName: walletName,
      circleId: circleId,
      circleName: circleName
    })

    // 加载角色列表
    this.loadCharacters()
  },

  // 加载角色列表
  loadCharacters() {
    this.setData({ loading: true })

    let apiUrl = ''
    if (this.data.mode === 'circle' && this.data.circleId) {
      // 圈子模式：加载圈子成员
      apiUrl = `/buddy/circles/${this.data.circleId}/members`
    } else {
      // 自由选搭模式：加载默认角色
      apiUrl = '/buddy/characters/default'
    }

    request({
      url: apiUrl,
      method: 'GET'
    }).then(result => {
      console.log('角色列表响应:', result)
      
      if (result.success && result.data && result.data.length > 0) {
        const characters = result.data.map(char => ({
          ...char,
          tags: typeof char.tags === 'string' ? JSON.parse(char.tags) : char.tags
        }))
        
        this.setData({
          characters: characters,
          selectedCharacter: characters[0],
          loading: false
        })
      } else {
        this.setData({ loading: false })
        wx.showToast({
          title: '暂无可选角色',
          icon: 'none'
        })
      }
    }).catch(error => {
      console.error('加载角色列表失败:', error)
      this.setData({ loading: false })
      wx.showToast({
        title: '网络错误，请重试',
        icon: 'none'
      })
    })
  },

  // 轮播切换
  onSwiperChange(e) {
    const current = e.detail.current
    this.setData({
      currentIndex: current,
      selectedCharacter: this.data.characters[current]
    })
  },

  // 切换角色选择
  toggleCharacter(e) {
    const character = e.currentTarget.dataset.character
    const selectedIds = [...this.data.selectedIds]
    const selectedCharacters = [...this.data.selectedCharacters]

    const index = selectedIds.indexOf(character.id)
    
    if (index > -1) {
      // 已选中，取消选择
      selectedIds.splice(index, 1)
      selectedCharacters.splice(index, 1)
    } else {
      // 未选中，添加选择
      if (selectedIds.length >= 4) {
        wx.showToast({
          title: '最多选择4个搭子',
          icon: 'none'
        })
        return
      }
      selectedIds.push(character.id)
      selectedCharacters.push(character)
    }

    this.setData({
      selectedIds: selectedIds,
      selectedCharacters: selectedCharacters
    })
  },

  // 移除选中的角色
  removeCharacter(e) {
    const characterId = e.currentTarget.dataset.id
    const selectedIds = this.data.selectedIds.filter(id => id !== characterId)
    const selectedCharacters = this.data.selectedCharacters.filter(char => char.id !== characterId)

    this.setData({
      selectedIds: selectedIds,
      selectedCharacters: selectedCharacters
    })
  },

  // 播放介绍语音
  playIntroVoice() {
    const character = this.data.selectedCharacter
    if (!character) return

    wx.showLoading({
      title: '生成语音中...'
    })

    // 调用后端API生成介绍语音
    request({
      url: `/buddy/characters/${character.id}/intro-voice`,
      method: 'GET'
    }).then(result => {
      wx.hideLoading()
      
      if (result.success && result.data) {
        // 播放语音
        const audioContext = wx.createInnerAudioContext()
        audioContext.src = result.data
        audioContext.play()
        
        audioContext.onPlay(() => {
          wx.showToast({
            title: '正在播放介绍',
            icon: 'none'
          })
        })
        
        audioContext.onError((err) => {
          console.error('语音播放失败:', err)
          wx.showToast({
            title: '语音播放失败',
            icon: 'none'
          })
        })
      } else {
        wx.showToast({
          title: '语音生成失败',
          icon: 'none'
        })
      }
    }).catch(error => {
      wx.hideLoading()
      console.error('获取介绍语音失败:', error)
      wx.showToast({
        title: '语音获取失败',
        icon: 'none'
      })
    })
  },

  // 确认选择，创建钱包
  confirmSelection() {
    const selectedCharacters = this.data.selectedCharacters
    
    if (selectedCharacters.length < 3) {
      wx.showToast({
        title: '请至少选择3个搭子',
        icon: 'none'
      })
      return
    }

    if (selectedCharacters.length > 4) {
      wx.showToast({
        title: '最多选择4个搭子',
        icon: 'none'
      })
      return
    }

    // 创建搭子钱包
    this.createBuddyWallet()
  },

  // 创建搭子钱包
  createBuddyWallet() {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      wx.showToast({
        title: '用户信息异常',
        icon: 'none'
      })
      return
    }

    this.setData({ creating: true })

    const requestData = {
      userId: userId,
      name: this.data.walletName,
      circleId: this.data.circleId,
      characterIds: this.data.selectedIds
    }

    console.log('创建搭子钱包请求:', requestData)

    request({
      url: '/buddy/wallets/create',
      method: 'POST',
      data: requestData
    }).then(result => {
      console.log('创建钱包响应:', result)
      
      if (result.success && result.data) {
        wx.showToast({
          title: '搭子钱包创建成功！',
          icon: 'success',
          duration: 2000
        })

        // 延迟跳转到钱包详情页
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/wallet-detail/wallet-detail?id=${result.data.id}`
          })
        }, 2000)
      } else {
        this.setData({ creating: false })
        wx.showToast({
          title: result.message || '创建钱包失败',
          icon: 'none'
        })
      }
    }).catch(error => {
      this.setData({ creating: false })
      console.error('创建搭子钱包失败:', error)
      wx.showToast({
        title: '网络错误，请重试',
        icon: 'none'
      })
    })
  },

  // 返回上一页
  goBack() {
    if (this.data.creating) return // 创建中不允许返回
    
    wx.navigateBack({
      delta: 1
    })
  }
})
