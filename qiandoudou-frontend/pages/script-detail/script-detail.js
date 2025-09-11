// pages/script-detail/script-detail.js
const { scriptAPI, shareImageAPI } = require('../../utils/api.js')

Page({
  data: {
    userId: null,
    walletId: null,
    selectedScript: null,
    selectedCategory: 1,
    categories: [
      { id: 1, name: '推荐' },
      { id: 2, name: '旅行' },
      { id: 3, name: '购物' },
      { id: 4, name: '学习' },
      { id: 5, name: '健身' }
    ],
    scripts: [],
    filteredScripts: [],
    
    // 剧本播放相关
    currentChapter: 1,
    chapterContent: null,
    userProgress: null,
    selectedChoice: null,
    transferButtonEnabled: false,
    showTransferButton: true,
    
    // 章节选择器已移除 - 改为基于选择跳转
    
    // 视频相关
    videoLoadError: false,
    videoContext: null,
    isFullscreen: false,
    videoUrl: 'https://example.com/video.mp4',
    posterUrl: 'https://example.com/poster.jpg',
    isFullscreen: false,
    showShareModal: false, // 是否显示分享弹窗
    shareImageUrl: '' // 分享图片地址
  },
  onReady() {
    this.videoContext = wx.createVideoContext('chapterVideo', this)
  },

  onFullscreenChange(e) {
    this.setData({
      isFullscreen: e.detail.fullScreen
    })
  },

  playVideo() {
    this.videoContext.play()
  },

  pauseVideo() {
    this.videoContext.pause()
  },

  requestFullscreen() {
    this.videoContext.requestFullScreen({
      direction: 0
    })
  },

  exitFullscreen() {
    this.videoContext.exitFullScreen()
  },

  onPlay() {

  },

  onPause() {

  },

  onError(e) {

  },

  onLoaded() {

  },
  onLoad(options) {
    const userInfo = wx.getStorageSync('userInfo')
    const walletId = options.walletId
    const autoPlay = options.autoPlay // 自动播放参数
    
    this.setData({
      userId: userInfo.id,
      walletId: walletId
    })

    // 加载剧本列表
    this.loadScripts()

    // 如果有scriptId参数，直接显示该剧本详情
    const scriptId = options.id
    if (scriptId) {
      this.loadScriptDetail(scriptId)
    }
    
    // 如果有autoPlay参数，自动跳转到指定剧本的详情页面
    if (autoPlay) {
      setTimeout(() => {
        this.autoPlayScript(parseInt(autoPlay))
      }, 500) // 延迟500ms确保页面加载完成
    }
  },

  onReady() {

    // 延迟创建视频上下文，确保DOM已完全渲染
    setTimeout(() => {
      this.initVideoContextForce()
    }, 300)
  },

  // 强制初始化视频上下文
  initVideoContextForce() {

    const videoContext = wx.createVideoContext('chapterVideo', this)
    
    if (videoContext) {
      this.setData({
        videoContext: videoContext
      })

      
      // 验证上下文方法

    } else {

      // 延迟重试
      setTimeout(() => {
        this.initVideoContextForce()
      }, 500)
    }
  },

  onShow() {
    // 页面显示时的处理

  },

  onHide() {
    // 页面隐藏时，退出全屏状态
    if (this.data.isFullscreen && this.data.videoContext) {
      try {
        this.data.videoContext.exitFullScreen()

      } catch (error) {

      }
    }
  },

  // 加载剧本列表
  async loadScripts() {
    try {
      wx.showLoading({ title: '加载中...' })
      const response = await scriptAPI.getScriptList(this.data.selectedCategory === 1 ? null : this.data.selectedCategory)
      
      if (response.code === 200) {
        this.setData({
          scripts: response.data,
          filteredScripts: response.data
        })
      }
    } catch (error) {

      wx.showToast({
        title: '加载失败',
        icon: 'error'
      })
    } finally {
      wx.hideLoading()
    }
  },

  // 加载剧本详情
  async loadScriptDetail(scriptId) {
    try {

      wx.showLoading({ title: '加载中...' })
      
      // 获取剧本详情

      const scriptResponse = await scriptAPI.getScriptDetail(scriptId)

      if (scriptResponse.code !== 200) {
        throw new Error('获取剧本详情失败')
      }

      // 获取或创建用户进度
      let progressResponse = await scriptAPI.getUserProgress(this.data.userId, scriptId, this.data.walletId)
      if (progressResponse.code !== 200 || !progressResponse.data) {
        // 开始新剧本
        const startResponse = await scriptAPI.startScript(this.data.userId, this.data.walletId, scriptId)
        if (startResponse.code === 200) {
          progressResponse = { code: 200, data: startResponse.data }
        }
      }

      const script = scriptResponse.data
      const progress = progressResponse.data

      this.setData({
        selectedScript: script,
        userProgress: progress,
        currentChapter: progress.currentChapter
      })

      // 加载当前章节内容
      this.loadChapterContent(scriptId, progress.currentChapter)

    } catch (error) {

      wx.showToast({
        title: '加载失败',
        icon: 'error'
      })
    } finally {
      wx.hideLoading()
    }
  },

  // 加载章节内容
  async loadChapterContent(scriptId, chapterNumber) {
    try {
      const response = await scriptAPI.getChapterContent(scriptId, chapterNumber, this.data.userId, this.data.walletId)

      
      if (response.code === 200) {
        const responseData = response.data
        let chapterData = null
        
        // 处理新的数据结构：可能是直接的章节数据，也可能是包含chapter和userProgress的对象
        if (responseData.chapter) {
          // 新的数据结构
          chapterData = responseData.chapter
          
          // 如果有用户进度数据，更新状态
          if (responseData.userProgress) {
            this.setData({
              userProgress: responseData.userProgress
            })

          }
        } else {
          // 兼容旧的数据结构
          chapterData = responseData
        }
        
        // 检查章节数据是否存在
        if (!chapterData) {

          wx.showToast({
            title: '章节数据不存在',
            icon: 'error'
          })
          return
        }
        
        // 解析choices JSON
        let choicesList = []
        if (chapterData.choices) {
          try {
            choicesList = JSON.parse(chapterData.choices)
          } catch (e) {

            choicesList = []
          }
        }
        
        chapterData.choicesList = choicesList

        
        // 检查是否为最后一集（没有选项或所有选项的nextId都为null）
        const isLastChapter = !choicesList || choicesList.length === 0 || 
                            choicesList.every(choice => choice.nextId === null || choice.nextId === undefined)
        
        this.setData({
          chapterContent: chapterData,
          selectedChoice: null,
          transferButtonEnabled: false,
          videoLoadError: false,
          showTransferButton: !isLastChapter
        })

        // 处理视频URL
        if (chapterData && chapterData.videoUrl) {

          
          // 添加时间戳参数避免缓存问题
          const timestamp = new Date().getTime()
          const separator = chapterData.videoUrl.includes('?') ? '&' : '?'
          chapterData.videoUrl = chapterData.videoUrl + separator + 'v=' + timestamp

          
          // 重置视频相关状态
          this.setData({
            videoContext: null,
            isFullscreen: false
          })

        }
      } else {

        wx.showToast({
          title: response.message || '获取章节内容失败',
          icon: 'error'
        })
      }
    } catch (error) {

      wx.showToast({
        title: '加载章节失败',
        icon: 'error'
      })
    }
  },

  // 选择分类
  selectCategory(e) {
    const categoryId = parseInt(e.currentTarget.dataset.id)
    this.setData({
      selectedCategory: categoryId
    })
    this.loadScripts()
  },

  // 选择剧本
  selectScript(e) {
    const script = e.currentTarget.dataset.script


    
    // 根据剧本类型跳转到不同页面
    if (script.id == 3 || script.id === '3') {
      // 图文类型剧本，跳转到新页面

      wx.navigateTo({
        url: '/pages/script-image-detail/script-image-detail?walletId=' + this.data.walletId
      })
    } else {
      // 视频类型剧本，继续使用当前页面

      this.loadScriptDetail(script.id)
    }
  },

  // 章节选择功能已移除 - 改为基于选择跳转

  // 选择剧情选项
  selectChoice(e) {
    const choice = e.currentTarget.dataset.choice
    this.setData({
      selectedChoice: choice,
      transferButtonEnabled: true
    })
  },

  // 向钱兜兜转入
  async transferToWallet() {
    const { selectedChoice, selectedScript, currentChapter, userId } = this.data
    
    if (!selectedChoice) {
      wx.showToast({
        title: '请先选择一个选项',
        icon: 'error'
      })
      return
    }

    try {
      // 解析选择项获取金额
      const choices = this.data.chapterContent.choicesList
      const choiceObj = choices.find(c => c.selection === selectedChoice)
      
      if (!choiceObj) {
        throw new Error('选择项无效')
      }

      const amount = choiceObj.cost

      wx.showModal({
        title: '确认转入',
        content: `您选择了"${selectedChoice}"，需要向钱兜兜转入¥${amount}元来解锁下一集`,
        confirmText: '确认转入',
        cancelText: '取消',
        success: async (res) => {
          if (res.confirm) {
            await this.processChoice(selectedChoice, amount)
          }
        }
      })

    } catch (error) {

      wx.showToast({
        title: '处理失败',
        icon: 'error'
      })
    }
  },

  // 处理用户选择
  async processChoice(selectedChoice, amount) {
    try {
      wx.showLoading({ title: '处理中...' })

      const response = await scriptAPI.makeChoice(
        this.data.userId,
        this.data.walletId,
        this.data.selectedScript.id,
        this.data.currentChapter,
        selectedChoice,
        amount
      )

      if (response.code === 200 && response.success) {
        wx.showToast({
          title: response.message,
          icon: 'success'
        })

        // 处理基于nextId的跳转逻辑
        if (response.isCompleted) {
          wx.showModal({
            title: '恭喜',
            content: '您已完成整个剧本！',
            showCancel: false
          })
        } else if (response.nextChapter) {
          // 直接使用后端返回的章节号跳转
          const nextChapterNumber = response.nextChapter
          
          this.setData({
            currentChapter: nextChapterNumber,
            transferButtonEnabled: false
          })
          
          // 加载指定的章节内容
          this.loadChapterContent(this.data.selectedScript.id, nextChapterNumber)
          
          wx.showToast({
            title: `跳转到第${nextChapterNumber}集`,
            icon: 'success'
          })
        }

        // 刷新用户进度
        this.refreshUserProgress()

      } else {
        throw new Error(response.message || '处理失败')
      }

    } catch (error) {

      wx.showToast({
        title: error.message || '处理失败',
        icon: 'error'
      })
    } finally {
      wx.hideLoading()
    }
  },

  // 刷新用户进度
  async refreshUserProgress() {
    try {
      const response = await scriptAPI.getUserProgress(this.data.userId, this.data.selectedScript.id, this.data.walletId)
      if (response.code === 200) {
        this.setData({
          userProgress: response.data
        })
      }
    } catch (error) {

    }
  },

  // 返回列表
  goBack() {
    this.setData({
      selectedScript: null,
      chapterContent: null,
      selectedChoice: null,
      transferButtonEnabled: false
    })
  },

  // 返回钱包
  goBackToWallet() {
    wx.navigateBack()
  },

  // 自动播放指定剧本
  autoPlayScript(scriptId) {
    // 跳转到剧本图片详情页面
    wx.navigateTo({
      url: `/pages/script-image-detail/script-image-detail?scriptId=${scriptId}&walletId=${this.data.walletId}`
    })
  },

  // 视频播放事件
  onVideoPlay(e) {



  },

  // 视频加载成功事件
  onVideoLoaded(e) {

    this.setData({
      videoLoadError: false
    })
  },

  // 视频可以播放事件
  onVideoCanPlay(e) {


    
    // 强制重新创建视频上下文，确保功能正常

    
    setTimeout(() => {
      const videoContext = wx.createVideoContext('chapterVideo', this)
      if (videoContext) {
        this.setData({
          videoContext: videoContext
        })

        
        // 验证上下文是否可用

      } else {

      }
    }, 200)
  },

  // 视频错误事件
  onVideoError(e) {


    
    this.setData({
      videoLoadError: true
    })
    
    // 显示详细错误信息用于调试
    const errorMsg = e.detail ? JSON.stringify(e.detail) : '未知错误'

    
    wx.showModal({
      title: '视频加载失败',
      content: `请检查视频文件是否存在\n路径: ${this.data.chapterContent?.videoUrl}\n错误: ${errorMsg}`,
      showCancel: false
    })
  },

  // 重试视频加载
  retryVideo() {
    this.setData({
      videoLoadError: false
    })
    
    // 重新初始化视频上下文并播放
    this.initVideoContext()
    
    // 延迟播放，确保上下文已初始化
    setTimeout(() => {
      if (this.data.videoContext) {
        try {
          this.data.videoContext.play()
        } catch (error) {

        }
      }
    }, 500)
  },

  // 全屏状态变化事件
  onFullscreenChange(e) {

    
    const newFullscreenState = e.detail.fullScreen || e.detail.fullscreen
    
    // 避免频繁的状态更新导致闪烁，只在状态真正改变时更新
    if (this.data.isFullscreen !== newFullscreenState) {
      this.setData({
        isFullscreen: newFullscreenState
      })
      
      if (newFullscreenState) {

        wx.showToast({
          title: '已进入全屏',
          icon: 'success',
          duration: 1000
        })
      } else {

        wx.showToast({
          title: '已退出全屏',
          icon: 'success', 
          duration: 1000
        })
      }
    }
  },

  // 视频暂停事件
  onVideoPause(e) {

  },

  // 视频播放结束事件
  onVideoEnded(e) {

  },

  // 视频缓冲事件
  onVideoWaiting(e) {

  },

  // 视频进度事件
  onVideoProgress(e) {

  },

  // 视频时间更新事件
  onTimeUpdate(e) {
    // 可以用来跟踪播放进度
    // console.log('播放进度:', e.detail.currentTime, '/', e.detail.duration)
  },

  // 切换全屏状态（自定义按钮点击）
  toggleFullscreen() {



    
    // 详细检查视频上下文
    if (!this.data.videoContext) {

      this.initVideoContextForce()
      
      // 延迟执行全屏操作
      setTimeout(() => {
        if (this.data.videoContext) {

          this.requestFullscreen()
        } else {

          wx.showToast({
            title: '视频上下文初始化失败',
            icon: 'error'
          })
        }
      }, 600)
      return
    }
    
    if (this.data.isFullscreen) {
      this.exitFullscreen()
    } else {
      this.requestFullscreen()
    }
  },

  // 手动请求全屏
  requestFullscreen() {



    
    if (!this.data.videoContext) {

      wx.showToast({
        title: '视频上下文未初始化',
        icon: 'error'
      })
      return
    }

    // 检查上下文方法是否可用
    if (typeof this.data.videoContext.requestFullScreen !== 'function') {

      wx.showToast({
        title: '全屏方法不可用',
        icon: 'error'
      })
      return
    }

    try {

      // 使用微信小程序的全屏API
      this.data.videoContext.requestFullScreen({
        direction: 0 // 0: 默认方向
      })

    } catch (error) {

      wx.showToast({
        title: '全屏功能调用失败',
        icon: 'error'
      })
    }
  },

  // 退出全屏
  exitFullscreen() {

    
    if (this.data.videoContext) {
      try {
        this.data.videoContext.exitFullScreen()

      } catch (error) {

        // 如果API调用失败，手动设置状态
        this.setData({
          isFullscreen: false
        })
      }
    } else {

      // 手动设置状态
      this.setData({
        isFullscreen: false
      })
    }
  },

  // 初始化视频上下文的独立方法
  initVideoContext() {
    setTimeout(() => {
      const videoContext = wx.createVideoContext('chapterVideo', this)
      if (videoContext) {
        this.setData({
          videoContext: videoContext
        })

      }
    }, 200)
  },

  // 清除视频缓存并重新加载
  clearVideoCache() {

    
    // 先停止当前视频
    if (this.data.videoContext) {
      try {
        this.data.videoContext.stop()
      } catch (error) {

      }
    }
    
    // 重置视频状态
    this.setData({
      videoContext: null,
      videoLoadError: false,
      isFullscreen: false
    })
    
    // 重新加载当前章节
    setTimeout(() => {
      this.loadChapterContent(this.data.selectedScript.id, this.data.currentChapter)
    }, 300)
  },

  // 剧本分享
  onScriptShare(e) {

    const script = e.currentTarget.dataset.script


    
    wx.showLoading({
      title: '加载分享图片...'
    })
    
    // 获取剧本分享图片

    shareImageAPI.getScriptShareImage(script.id)
      .then(result => {
        wx.hideLoading()





        
        if (result.data && result.data.imageUrl) {

          this.setData({
            showShareModal: true,
            shareImageUrl: result.data.imageUrl
          })

        } else {

          wx.showToast({
            title: '分享图片数据无效',
            icon: 'none'
          })
        }
      })
      .catch(error => {
        wx.hideLoading()




        wx.showModal({
          title: '剧本分享功能错误',
          content: `错误信息：${error.message || '未知错误'}`,
          showCancel: false
        })
      })
  },

  // 关闭分享弹窗
  onShareModalClose() {

    this.setData({
      showShareModal: false,
      shareImageUrl: ''
    })
  },

  // 分享图片保存回调
  onShareImageSave(e) {

    if (e.detail.success) {
      // 保存成功后可以关闭弹窗
      setTimeout(() => {
        this.onShareModalClose()
      }, 1000)
    }
  },

})