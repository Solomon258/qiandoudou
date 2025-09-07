// pages/script-image-detail/script-image-detail.js
const { scriptAPI } = require('../../utils/api.js')

Page({
  data: {
    userId: null,
    walletId: null,
    selectedScript: null,
    
    // 剧本播放相关
    currentChapter: 1,
    chapterContent: null,
    userProgress: null,
    selectedChoice: null,
    transferButtonEnabled: false,
    showTransferButton: true
  },

  onLoad(options) {
    const userInfo = wx.getStorageSync('userInfo')
    const walletId = options.walletId
    
    this.setData({
      userId: userInfo.id,
      walletId: walletId
    })

    // 直接加载id=3的剧本详情
    this.loadScriptDetail(3)
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
          showTransferButton: !isLastChapter
        })

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

  // 选择章节
  selectChapter(e) {
    const chapterNumber = parseInt(e.currentTarget.dataset.chapter)
    if (chapterNumber !== this.data.currentChapter) {
      this.setData({
        currentChapter: chapterNumber
      })
      this.loadChapterContent(this.data.selectedScript.id, chapterNumber)
    }
  },

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

  // 返回钱包
  goBackToWallet() {
    wx.navigateBack()
  }
})
