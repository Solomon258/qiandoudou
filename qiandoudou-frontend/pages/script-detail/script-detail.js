// pages/script-detail/script-detail.js
const { scriptAPI } = require('../../utils/api.js')

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
    isFullscreen: false
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
    console.log('视频开始播放')
  },

  onPause() {
    console.log('视频暂停')
  },

  onError(e) {
    console.error('视频错误:', e.detail)
  },

  onLoaded() {
    console.log('视频加载完成')
  },
  onLoad(options) {
    const userInfo = wx.getStorageSync('userInfo')
    const walletId = options.walletId
    
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
  },

  onReady() {
    console.log('页面Ready，开始初始化')
    // 延迟创建视频上下文，确保DOM已完全渲染
    setTimeout(() => {
      this.initVideoContextForce()
    }, 300)
  },

  // 强制初始化视频上下文
  initVideoContextForce() {
    console.log('强制初始化视频上下文')
    const videoContext = wx.createVideoContext('chapterVideo', this)
    
    if (videoContext) {
      this.setData({
        videoContext: videoContext
      })
      console.log('✅ onReady 视频上下文初始化完成')
      
      // 验证上下文方法
      console.log('上下文方法验证:', {
        play: typeof videoContext.play,
        pause: typeof videoContext.pause,
        requestFullScreen: typeof videoContext.requestFullScreen,
        exitFullScreen: typeof videoContext.exitFullScreen
      })
    } else {
      console.error('❌ onReady 视频上下文创建失败')
      // 延迟重试
      setTimeout(() => {
        this.initVideoContextForce()
      }, 500)
    }
  },

  onShow() {
    // 页面显示时的处理
    console.log('页面显示，当前全屏状态:', this.data.isFullscreen)
  },

  onHide() {
    // 页面隐藏时，退出全屏状态
    if (this.data.isFullscreen && this.data.videoContext) {
      try {
        this.data.videoContext.exitFullScreen()
        console.log('页面隐藏时退出全屏')
      } catch (error) {
        console.error('页面隐藏时退出全屏失败:', error)
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
      console.error('加载剧本列表失败:', error)
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
      console.log('开始加载剧本详情，scriptId:', scriptId, '类型:', typeof scriptId)
      wx.showLoading({ title: '加载中...' })
      
      // 获取剧本详情
      console.log('调用API获取剧本详情...')
      const scriptResponse = await scriptAPI.getScriptDetail(scriptId)
      console.log('剧本详情API响应:', scriptResponse)
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
      console.error('加载剧本详情失败:', error)
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
      const response = await scriptAPI.getChapterContent(scriptId, chapterNumber)
      console.log('章节内容API响应:', response)
      
      if (response.code === 200) {
        const chapterData = response.data
        
        // 检查章节数据是否存在
        if (!chapterData) {
          console.error('章节数据为空，章节可能不存在:', scriptId, chapterNumber)
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
            console.error('解析choices失败:', e)
            choicesList = []
          }
        }
        
        chapterData.choicesList = choicesList
        console.log('章节数据处理完成:', chapterData)
        
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
          console.log('原始视频URL:', chapterData.videoUrl)
          
          // 添加时间戳参数避免缓存问题
          const timestamp = new Date().getTime()
          const separator = chapterData.videoUrl.includes('?') ? '&' : '?'
          chapterData.videoUrl = chapterData.videoUrl + separator + 'v=' + timestamp
          
          console.log('添加防缓存参数后的视频URL:', chapterData.videoUrl)
          
          // 重置视频相关状态
          this.setData({
            videoContext: null,
            isFullscreen: false
          })
          
          console.log('准备初始化视频组件')
        }
      } else {
        console.error('获取章节内容失败，完整响应:', response)
        wx.showToast({
          title: response.message || '获取章节内容失败',
          icon: 'error'
        })
      }
    } catch (error) {
      console.error('加载章节内容失败:', error)
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
    console.log('选择的剧本:', script)
    console.log('剧本ID:', script.id, '类型:', typeof script.id)
    
    // 根据剧本类型跳转到不同页面
    if (script.id == 3 || script.id === '3') {
      // 图文类型剧本，跳转到新页面
      console.log('跳转到图文剧本页面')
      wx.navigateTo({
        url: '/pages/script-image-detail/script-image-detail?walletId=' + this.data.walletId
      })
    } else {
      // 视频类型剧本，继续使用当前页面
      console.log('加载视频剧本详情，scriptId:', script.id)
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
      console.error('转入处理失败:', error)
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
      console.error('处理选择失败:', error)
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
      console.error('刷新进度失败:', error)
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

  // 视频播放事件
  onVideoPlay(e) {
    console.log('视频开始播放:', e)
    console.log('当前视频上下文状态:', !!this.data.videoContext)
    console.log('当前全屏状态:', this.data.isFullscreen)
  },

  // 视频加载成功事件
  onVideoLoaded(e) {
    console.log('视频加载成功:', e)
    this.setData({
      videoLoadError: false
    })
  },

  // 视频可以播放事件
  onVideoCanPlay(e) {
    console.log('视频可以播放:', e)
    console.log('当前视频上下文状态:', this.data.videoContext ? '已存在' : '不存在')
    
    // 强制重新创建视频上下文，确保功能正常
    console.log('视频可播放，强制重新初始化视频上下文')
    
    setTimeout(() => {
      const videoContext = wx.createVideoContext('chapterVideo', this)
      if (videoContext) {
        this.setData({
          videoContext: videoContext
        })
        console.log('✅ 视频上下文重新初始化完成，全屏功能已就绪')
        
        // 验证上下文是否可用
        console.log('验证视频上下文方法:', {
          play: typeof videoContext.play,
          requestFullScreen: typeof videoContext.requestFullScreen,
          exitFullScreen: typeof videoContext.exitFullScreen
        })
      } else {
        console.error('❌ 视频上下文创建失败')
      }
    }, 200)
  },

  // 视频错误事件
  onVideoError(e) {
    console.error('视频播放错误详情:', e.detail)
    console.error('视频播放错误完整信息:', e)
    
    this.setData({
      videoLoadError: true
    })
    
    // 显示详细错误信息用于调试
    const errorMsg = e.detail ? JSON.stringify(e.detail) : '未知错误'
    console.log('当前视频路径:', this.data.chapterContent?.videoUrl)
    
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
          console.error('重试播放失败:', error)
        }
      }
    }, 500)
  },

  // 全屏状态变化事件
  onFullscreenChange(e) {
    console.log('全屏状态变化事件:', e.detail)
    
    const newFullscreenState = e.detail.fullScreen || e.detail.fullscreen
    
    // 避免频繁的状态更新导致闪烁，只在状态真正改变时更新
    if (this.data.isFullscreen !== newFullscreenState) {
      this.setData({
        isFullscreen: newFullscreenState
      })
      
      if (newFullscreenState) {
        console.log('✅ 成功进入全屏模式')
        wx.showToast({
          title: '已进入全屏',
          icon: 'success',
          duration: 1000
        })
      } else {
        console.log('✅ 成功退出全屏模式')
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
    console.log('视频暂停:', e.detail)
  },

  // 视频播放结束事件
  onVideoEnded(e) {
    console.log('视频播放结束:', e.detail)
  },

  // 视频缓冲事件
  onVideoWaiting(e) {
    console.log('视频缓冲中:', e.detail)
  },

  // 视频进度事件
  onVideoProgress(e) {
    console.log('视频缓冲进度:', e.detail)
  },

  // 视频时间更新事件
  onTimeUpdate(e) {
    // 可以用来跟踪播放进度
    // console.log('播放进度:', e.detail.currentTime, '/', e.detail.duration)
  },

  // 切换全屏状态（自定义按钮点击）
  toggleFullscreen() {
    console.log('🔘 自定义全屏按钮被点击')
    console.log('当前全屏状态:', this.data.isFullscreen)
    console.log('视频上下文状态:', this.data.videoContext ? '已初始化' : '未初始化')
    
    // 详细检查视频上下文
    if (!this.data.videoContext) {
      console.error('❌ 视频上下文未初始化，尝试立即创建')
      this.initVideoContextForce()
      
      // 延迟执行全屏操作
      setTimeout(() => {
        if (this.data.videoContext) {
          console.log('✅ 重新创建上下文成功，继续全屏操作')
          this.requestFullscreen()
        } else {
          console.error('❌ 重新创建上下文失败')
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
    console.log('🔄 开始请求全屏')
    console.log('视频上下文状态:', !!this.data.videoContext)
    console.log('当前全屏状态:', this.data.isFullscreen)
    
    if (!this.data.videoContext) {
      console.error('❌ 视频上下文未初始化')
      wx.showToast({
        title: '视频上下文未初始化',
        icon: 'error'
      })
      return
    }

    // 检查上下文方法是否可用
    if (typeof this.data.videoContext.requestFullScreen !== 'function') {
      console.error('❌ requestFullScreen 方法不存在')
      wx.showToast({
        title: '全屏方法不可用',
        icon: 'error'
      })
      return
    }

    try {
      console.log('📱 调用 requestFullScreen API')
      // 使用微信小程序的全屏API
      this.data.videoContext.requestFullScreen({
        direction: 0 // 0: 默认方向
      })
      console.log('✅ 全屏请求已发送，等待状态变化...')
    } catch (error) {
      console.error('❌ 请求全屏失败:', error)
      wx.showToast({
        title: '全屏功能调用失败',
        icon: 'error'
      })
    }
  },

  // 退出全屏
  exitFullscreen() {
    console.log('尝试退出全屏')
    
    if (this.data.videoContext) {
      try {
        this.data.videoContext.exitFullScreen()
        console.log('退出全屏请求已发送')
      } catch (error) {
        console.error('退出全屏失败:', error)
        // 如果API调用失败，手动设置状态
        this.setData({
          isFullscreen: false
        })
      }
    } else {
      console.error('视频上下文未初始化')
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
        console.log('重新初始化视频上下文完成')
      }
    }, 200)
  },

  // 清除视频缓存并重新加载
  clearVideoCache() {
    console.log('清除视频缓存并重新加载')
    
    // 先停止当前视频
    if (this.data.videoContext) {
      try {
        this.data.videoContext.stop()
      } catch (error) {
        console.log('停止视频失败:', error)
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
  }
})