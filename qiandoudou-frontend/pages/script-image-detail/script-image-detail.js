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

  // 处理系统回退按钮
  onUnload() {
    // 页面卸载时不需要特殊处理，使用默认行为
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
          showTransferButton: !isLastChapter
        }, () => {
          // 加载完章节后更新按钮显示状态
          this.updateButtonVisibility()
        })

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

  // 选择章节
  selectChapter(e) {
    const chapterNumber = parseInt(e.currentTarget.dataset.chapter)
    const currentProgress = this.data.userProgress?.currentChapter || 1

    // 检查是否尝试访问未解锁的章节
    if (chapterNumber > currentProgress) {
      wx.showToast({
        title: '待解锁，请先完成当前剧情',
        icon: 'none',
        duration: 2000
      })
      return
    }

    if (chapterNumber !== this.data.currentChapter) {
      this.setData({
        currentChapter: chapterNumber
      })
      this.loadChapterContent(this.data.selectedScript.id, chapterNumber)
    }
  },

  // 更新按钮显示状态
  updateButtonVisibility() {
    const { currentChapter, userProgress, selectedScript } = this.data

    console.log('========== updateButtonVisibility 调用 ==========')
    console.log('currentChapter:', currentChapter)
    console.log('userProgress.currentChapter:', userProgress?.currentChapter)
    console.log('selectedScript.totalChapters:', selectedScript?.totalChapters)

    // 只在以下情况显示按钮：
    // 当前章节是用户正在进行的章节（currentChapter === userProgress.currentChapter）
    // 包括最后一集，只要还没完成就显示
    const isCurrentUnfinishedChapter = currentChapter === userProgress.currentChapter

    console.log('isCurrentUnfinishedChapter:', isCurrentUnfinishedChapter)
    console.log('showTransferButton 将设置为:', isCurrentUnfinishedChapter)
    console.log('=========================================')

    this.setData({
      showTransferButton: isCurrentUnfinishedChapter
    })
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

      console.log('========== processChoice 后端返回数据 ==========')
      console.log('完整response:', JSON.stringify(response, null, 2))
      console.log('response.code:', response.code)
      console.log('response.success:', response.success)
      console.log('response.isCompleted:', response.isCompleted)
      console.log('response.nextChapter:', response.nextChapter)
      console.log('response.message:', response.message)
      console.log('=========================================')

      if (response.code === 200 && response.success) {
        wx.showToast({
          title: response.message,
          icon: 'success'
        })

        console.log('开始处理后端响应逻辑...')
        console.log('response.isCompleted:', response.isCompleted)

        // 检查是否完成：根据 isCompleted 或 status 字段
        // status: 2 表示已完成
        const isScriptCompleted = response.isCompleted === true || response.status === 2
        console.log('isScriptCompleted:', isScriptCompleted)

        // 处理基于nextId的跳转逻辑
        if (isScriptCompleted) {
          console.log('检测到脚本已完成，开始刷新进度')
          // 刷新用户进度后显示完成提示
          try {
            await this.refreshUserProgress()
          } catch (error) {
            console.error('刷新进度失败:', error)
          }

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

          // 刷新用户进度
          try {
            await this.refreshUserProgress()
          } catch (error) {
            console.error('刷新进度失败:', error)
          }
        } else {
          // 其他情况也刷新进度
          try {
            await this.refreshUserProgress()
          } catch (error) {
            console.error('刷新进度失败:', error)
          }
        }

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

  // 刷新用户进度 - 返回Promise以支持await
  refreshUserProgress() {
    return new Promise((resolve, reject) => {
      try {
        scriptAPI.getUserProgress(this.data.userId, this.data.selectedScript.id, this.data.walletId)
          .then(response => {
            console.log('========== refreshUserProgress 返回数据 ==========')
            console.log('完整response:', JSON.stringify(response, null, 2))
            console.log('response.data:', response.data)
            console.log('currentChapter:', this.data.currentChapter)
            console.log('=========================================')

            if (response.code === 200) {
              console.log('setData 前的 userProgress:', this.data.userProgress)
              this.setData({
                userProgress: response.data
              }, () => {
                console.log('setData 完成，新的 userProgress:', this.data.userProgress)
                console.log('currentChapter:', this.data.currentChapter)
                console.log('调用 updateButtonVisibility()')
                // 更新进度后重新计算按钮显示状态
                this.updateButtonVisibility()
                resolve(response.data)
              })
            } else {
              resolve(null)
            }
          })
          .catch(error => {
            console.error('刷新用户进度失败:', error)
            reject(error)
          })
      } catch (error) {
        console.error('刷新用户进度异常:', error)
        reject(error)
      }
    })
  },

  // 返回剧本列表页面
  goBackToWallet() {
    // 获取页面栈
    const pages = getCurrentPages()
    
    // 检查页面栈中是否有剧本列表页面
    let hasScriptListPage = false
    for (let i = pages.length - 2; i >= 0; i--) {
      if (pages[i].route === 'pages/script-detail/script-detail') {
        hasScriptListPage = true
        break
      }
    }
    
    if (hasScriptListPage) {
      // 如果页面栈中有剧本列表页面，直接返回
      wx.navigateBack()
    } else {
      // 如果没有剧本列表页面，跳转到剧本列表页面
      wx.redirectTo({
        url: `/pages/script-detail/script-detail?walletId=${this.data.walletId}`
      })
    }
  }
})
