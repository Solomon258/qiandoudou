// pages/transfer-in/transfer-in.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: null,
    wallet: {},
    activeTab: 'direct', // 'direct' 或 'auto'
    transferAmount: '',
    transferNote: '',
    noteLength: 0,
    uploadedImage: '', // OSS图片URL（用于显示）
    uploadedImageLocal: '', // 本地图片路径（用于AI分析）
    amountFocus: false,
    transferLoading: false,
    // 自动攒设置
    autoSaveSettings: {
      method: '每日攒',
      amount: '1',
      duration: '一直攒',
      startTime: '今日开始首次扣款',
      paymentMethod: '投银行卡顺序'
    },
    estimatedAmount: '365.00',
    planName: '',
    selectedTheme: 'custom'
  },

  onLoad(options) {
    console.log('转入页面 - onLoad参数:', options)
    const walletId = options.id || options.walletId
    console.log('转入页面 - 解析的钱包ID:', walletId)
    console.log('转入页面 - 当前token:', app.globalData.token)
    
    this.setData({
      walletId: walletId
    })
    
    this.loadWalletInfo()
  },

  // 加载钱包信息
  loadWalletInfo() {
    const walletId = this.data.walletId
    if (!walletId) {
      console.error('钱包ID不存在')
      return
    }

    walletAPI.getWalletDetail(walletId)
      .then(result => {
        const wallet = result.data
        if (!wallet) {
          wx.showToast({
            title: '钱包不存在',
            icon: 'none'
          })
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
          return
        }
        
        console.log('转入页面 - 从后端加载的钱包数据:', wallet)
        this.setData({
          wallet: wallet
        })
      })
      .catch(error => {
        console.error('加载钱包信息失败:', error)
        wx.showToast({
          title: error.message || '加载钱包信息失败',
          icon: 'none'
        })
      })
  },



  // 切换页签
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({
      activeTab: tab
    })
  },

  // 金额输入
  onAmountInput(e) {
    const value = e.detail.value
    this.setData({
      transferAmount: value
    })
  },

  // 备注输入
  onNoteInput(e) {
    const value = e.detail.value
    this.setData({
      transferNote: value,
      noteLength: value.length
    })
  },

  // 重新生成备注
  retryNote() {
    // 如果有上传的图片，调用AI接口生成文案
    if (this.data.uploadedImage) {
      this.generateNoteFromImage()
    } else {
      // 如果没有图片，使用默认的随机文案
      const notes = [
        ''
      ]
      const randomNote = notes[Math.floor(Math.random() * notes.length)]
      this.setData({
        transferNote: randomNote,
        noteLength: randomNote.length
      })
    }
  },

  // 根据图片生成文案
  generateNoteFromImage() {
    if (!this.data.uploadedImageLocal && !this.data.uploadedImage) {
      wx.showToast({
        title: '请先上传图片',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '正在生成文案...',
      mask: true
    })

    // 优先使用本地路径，如果没有则尝试使用显示路径
    const imagePath = this.data.uploadedImageLocal || this.data.uploadedImage
    
    // 将图片转换为base64
    this.convertImageToBase64(imagePath)
      .then(imageBase64 => {
        return walletAPI.generateTextFromImage(imageBase64, '你是一个朋友圈文案助手，根据图片生成朋友圈的文案，少于100字，不要生成其他内容,不要思考太久')
      })
      .then(result => {
        wx.hideLoading()
        const generatedText = result.data
        this.setData({
          transferNote: generatedText,
          noteLength: generatedText.length
        })
        wx.showToast({
          title: '文案生成成功',
          icon: 'success'
        })
      })
      .catch(error => {
        wx.hideLoading()
        console.error('生成文案失败:', error)
        wx.showToast({
          title: error.message || '生成文案失败',
          icon: 'none'
        })
        // 失败时使用默认文案
        const defaultNote = ''
        this.setData({
          transferNote: defaultNote,
          noteLength: defaultNote.length
        })
      })
  },

  // 将图片转换为base64
  convertImageToBase64(imagePath) {
    return new Promise((resolve, reject) => {
      wx.getFileSystemManager().readFile({
        filePath: imagePath,
        encoding: 'base64',
        success: (res) => {
          resolve(res.data)
        },
        fail: (error) => {
          reject(new Error('图片读取失败'))
        }
      })
    })
  },

  // 上传图片到OSS
  uploadImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0]
        
        // 显示上传进度
        wx.showLoading({
          title: '上传图片中...'
        })
        
        // 先保存本地路径（用于AI分析）
        this.setData({
          uploadedImageLocal: tempFilePath
        })
        
        // 上传到OSS
        const { uploadUserImage } = require('../../utils/api.js')
        uploadUserImage(tempFilePath, 'transfer_in')
          .then(response => {
            wx.hideLoading()
            if (response.data && response.data.imageUrl) {
              this.setData({
                uploadedImage: response.data.imageUrl // OSS URL用于显示
              })
              wx.showToast({
                title: '图片上传成功',
                icon: 'success'
              })
            }
          })
          .catch(error => {
            wx.hideLoading()
            console.error('图片上传失败:', error)
            wx.showToast({
              title: error.message || '图片上传失败',
              icon: 'error'
            })
            // 失败时使用本地路径作为显示备用
            this.setData({
              uploadedImage: tempFilePath
            })
          })
      },
      fail: () => {
        wx.showToast({
          title: '选择图片失败',
          icon: 'error'
        })
      }
    })
  },

  // 显示攒钱方式选项
  showSaveMethodOptions() {
    wx.showActionSheet({
      itemList: ['每日攒', '每周攒', '每月攒'],
      success: (res) => {
        const methods = ['每日攒', '每周攒', '每月攒']
        const selectedMethod = methods[res.tapIndex]
        this.setData({
          'autoSaveSettings.method': selectedMethod
        })
        this.updateEstimatedAmount()
      }
    })
  },

  // 显示金额选项
  showAmountOptions() {
    wx.showActionSheet({
      itemList: ['1元', '5元', '10元', '20元', '50元', '自定义'],
      success: (res) => {
        if (res.tapIndex === 5) {
          // 自定义金额
          wx.showModal({
            title: '自定义金额',
            editable: true,
            placeholderText: '请输入金额',
            success: (modalRes) => {
              if (modalRes.confirm && modalRes.content) {
                this.setData({
                  'autoSaveSettings.amount': modalRes.content
                })
                this.updateEstimatedAmount()
              }
            }
          })
        } else {
          const amounts = ['1', '5', '10', '20', '50']
          this.setData({
            'autoSaveSettings.amount': amounts[res.tapIndex]
          })
          this.updateEstimatedAmount()
        }
      }
    })
  },

  // 显示时间选项
  showDurationOptions() {
    wx.showActionSheet({
      itemList: ['一直攒', '攒1个月', '攒3个月', '攒6个月', '攒1年'],
      success: (res) => {
        const durations = ['一直攒', '攒1个月', '攒3个月', '攒6个月', '攒1年']
        this.setData({
          'autoSaveSettings.duration': durations[res.tapIndex]
        })
        this.updateEstimatedAmount()
      }
    })
  },

  // 显示扣款方式选项
  showPaymentOptions() {
    wx.showActionSheet({
      itemList: ['投银行卡顺序', '平安银行卡', '其他银行卡'],
      success: (res) => {
        const methods = ['投银行卡顺序', '平安银行卡', '其他银行卡']
        this.setData({
          'autoSaveSettings.paymentMethod': methods[res.tapIndex]
        })
      }
    })
  },

  // 更新预估金额
  updateEstimatedAmount() {
    const { method, amount, duration } = this.data.autoSaveSettings
    const dailyAmount = parseFloat(amount)
    
    let days = 365 // 默认一年
    if (duration === '攒1个月') days = 30
    else if (duration === '攒3个月') days = 90
    else if (duration === '攒6个月') days = 180
    else if (duration === '攒1年') days = 365
    
    let multiplier = 1
    if (method === '每周攒') multiplier = 7
    else if (method === '每月攒') multiplier = 30
    
    const totalAmount = (dailyAmount * days / multiplier).toFixed(2)
    this.setData({
      estimatedAmount: totalAmount
    })
  },

  // 计划名称输入
  onPlanNameInput(e) {
    this.setData({
      planName: e.detail.value
    })
  },

  // 选择主题
  selectTheme(e) {
    const theme = e.currentTarget.dataset.theme
    this.setData({
      selectedTheme: theme
    })
  },

  // 确认转入
  confirmTransfer() {
    if (this.data.activeTab === 'auto') {
      // 自动攒逻辑
      this.confirmAutoSave()
      return
    }
    
    // 直接转入逻辑
    const { transferAmount, transferNote, wallet } = this.data
    
    if (!transferAmount || parseFloat(transferAmount) <= 0) {
      wx.showToast({
        title: '请输入转入金额',
        icon: 'error'
      })
      return
    }
    
    if (!wallet || !wallet.id) {
      wx.showToast({
        title: '钱包信息错误',
        icon: 'error'
      })
      return
    }

    this.setData({ transferLoading: true })

    const amount = parseFloat(transferAmount)
    const description = transferNote || '转入'
    const imageUrl = this.data.uploadedImage || null
    const note = transferNote || null

    walletAPI.transferIn(wallet.id, amount, description, imageUrl, note)
      .then(result => {
        wx.showToast({
          title: `成功转入¥${amount}`,
          icon: 'success'
        })

        // 返回钱包详情页并刷新
        setTimeout(() => {
          const pages = getCurrentPages()
          if (pages.length > 1) {
            const prevPage = pages[pages.length - 2]
            if (prevPage && prevPage.loadWalletDetail) {
              prevPage.loadWalletDetail()
            }
            if (prevPage && prevPage.loadTransactions) {
              prevPage.loadTransactions()
            }
          }
          // 同时刷新首页数据
          const homePage = pages.find(page => page.route === 'pages/home/home' || page.__route__ === 'pages/home/home')
          if (homePage && homePage.loadWallets) {
            homePage.loadWallets()
          }
          
          wx.navigateBack({
            delta: 1
          })
        }, 1500)

        this.setData({ transferLoading: false })
      })
      .catch(error => {
        console.error('转入失败:', error)
        wx.showToast({
          title: error.message || '转入失败',
          icon: 'none'
        })
        this.setData({ transferLoading: false })
      })
  },

  // 确认自动攒
  confirmAutoSave() {
    const { autoSaveSettings, planName } = this.data
    
    this.setData({ transferLoading: true })

    // 模拟自动攒设置
    setTimeout(() => {
      // 保存自动攒设置到本地存储
      const autoSavePlans = wx.getStorageSync('auto_save_plans') || []
      const newPlan = {
        id: Date.now(),
        walletId: this.data.walletId,
        ...autoSaveSettings,
        planName: planName || '自动攒钱计划',
        createTime: new Date().toISOString(),
        isActive: true
      }
      
      autoSavePlans.push(newPlan)
      wx.setStorageSync('auto_save_plans', autoSavePlans)

      this.setData({ transferLoading: false })

      wx.showToast({
        title: '自动攒计划已开启',
        icon: 'success'
      })

      // 返回钱包详情页
      setTimeout(() => {
        const pages = getCurrentPages()
        if (pages.length > 1) {
          const prevPage = pages[pages.length - 2]
          if (prevPage && prevPage.loadWalletDetail) {
            prevPage.loadWalletDetail()
            prevPage.loadTransactions() // 刷新交易记录
          }
        }
        wx.navigateBack({
          delta: 1
        })
      }, 1500)
    }, 1000)
  },

  // 返回上一页
  goBack() {
    wx.navigateBack({
      delta: 1
    })
  }

})
