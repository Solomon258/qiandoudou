// pages/transfer-in/transfer-in.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: null,
    wallet: {},
    dreamMode: false, // 是否是梦想钱包模式
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
    console.log('transfer-in onLoad options:', options)
    const walletId = options.id || options.walletId
    const dreamMode = options.dreamMode === 'true'
    
    console.log('transfer-in - walletId:', walletId, 'dreamMode:', dreamMode)
    
    this.setData({
      walletId: walletId,
      dreamMode: dreamMode
    })
    
    this.loadWalletInfo()
  },

  // 加载钱包信息
  loadWalletInfo() {
    const walletId = this.data.walletId
    const dreamMode = this.data.dreamMode
    
    if (!walletId) {
      console.error('钱包ID为空')
      return
    }

    console.log('loadWalletInfo - walletId:', walletId, 'dreamMode:', dreamMode)
    
    // 根据是否是梦想模式选择不同的API
    const apiCall = dreamMode ? 
      walletAPI.getDreamWalletDetail(walletId) : 
      walletAPI.getWalletDetail(walletId)
    
    apiCall
      .then(result => {
        console.log('transfer-in API返回数据:', result.data)
        
        // 根据是否是梦想模式处理不同的数据结构
        let wallet
        if (dreamMode) {
          // 梦想钱包API返回：{wallet: {...}, currentProgress: 0, ...}
          const responseData = result.data
          wallet = responseData.wallet || responseData
        } else {
          // 普通钱包API直接返回钱包数据
          wallet = result.data
        }
        
        console.log('处理后的钱包数据:', wallet)
        
        if (!wallet || !wallet.id) {
          wx.showToast({
            title: '钱包不存在',
            icon: 'none'
          })
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
          return
        }

        this.setData({
          wallet: wallet
        })
      })
      .catch(error => {

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
    let value = e.detail.value

    // 只允许数字和一个小数点，小数点后最多2位
    // 先过滤掉非数字和非小数点的字符
    value = value.replace(/[^\d.]/g, '')

    // 只保留第一个小数点，删除多余的小数点
    if (value.indexOf('.') !== value.lastIndexOf('.')) {
      const dotIndex = value.indexOf('.')
      value = value.substring(0, dotIndex + 1) + value.substring(dotIndex + 1).replace(/\./g, '')
    }

    // 如果存在小数点，限制小数点后只有2位
    if (value.includes('.')) {
      const parts = value.split('.')
      if (parts[1] && parts[1].length > 2) {
        value = parts[0] + '.' + parts[1].substring(0, 2)
      }
    }

    this.setData({
      transferAmount: value
    })
  },

  // 备注输入
  onNoteInput(e) {
    let value = e.detail.value

    // 限制备注最多200字
    if (value.length > 200) {
      value = value.substring(0, 200)
    }

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
        const userId = app.globalData.userInfo?.id
        uploadUserImage(tempFilePath, 'transfer_in', userId)
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
            } else {
              // 响应格式不正确，视为失败
              throw new Error('图片上传失败：服务器响应格式错误')
            }
          })
          .catch(error => {
            wx.hideLoading()
            // 清除本地图片路径，防止误导用户
            this.setData({
              uploadedImageLocal: '',
              uploadedImage: ''
            })
            wx.showToast({
              title: error.message || '图片上传失败',
              icon: 'error'
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
      itemList: ['一直攒', '1个月', '3个月', '6个月', '1年'],
      success: (res) => {
        const durations = ['一直攒', '1个月', '3个月', '6个月', '1年']
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
    if (duration === '1个月') days = 30
    else if (duration === '3个月') days = 90
    else if (duration === '6个月') days = 180
    else if (duration === '1年') days = 365
    
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
    console.log(this.data.transferLoading,'this.data.transferLoadingthis.data.transferLoading')
    if(this.data.transferLoading) {
      return
    }
    this.setData({ transferLoading: true })
    if (this.data.activeTab === 'auto') {
      // 自动攒逻辑
      this.confirmAutoSave()
      return
    }
    
    // 直接转入逻辑
    const { transferAmount, transferNote, wallet, dreamMode } = this.data
    
    console.log('confirmTransfer 调试信息:')
    console.log('- transferAmount:', transferAmount)
    console.log('- wallet:', wallet)
    console.log('- wallet.id:', wallet?.id)
    console.log('- dreamMode:', dreamMode)
    
    // 校验1：金额是否为空
    if (!transferAmount || parseFloat(transferAmount) <= 0) {
      wx.showToast({
        title: '请输入转入金额',
        icon: 'error'
      })
      this.setData({ transferLoading: false })
      return
    }

    // 校验2：金额上限检查（不能超过 10,000,000）
    const maxAmount = 10000000
    const amount = parseFloat(transferAmount)
    if (amount > maxAmount) {
      wx.showModal({
        title: '金额超过限制',
        content: `单次转入金额不能超过 ¥${maxAmount.toLocaleString()}，请重新输入。`,
        showCancel: false,
        confirmText: '确定',
        success: (res) => {
          this.setData({ transferLoading: false })
        }
      })
      return
    }

    if (!wallet || !wallet.id) {
      console.error('钱包信息错误 - wallet:', wallet)
      wx.showToast({
        title: '钱包信息错误',
        icon: 'error'
      })
      this.setData({ transferLoading: false })
      return
    }

    const description = transferNote || '转入'
    const imageUrl = this.data.uploadedImage || null
    const note = transferNote || null

    // 根据是否是梦想模式调用不同的API或添加特殊参数
    console.log('准备调用转入API - dreamMode:', dreamMode, 'walletId:', wallet.id)
    
    const transferPromise = dreamMode ? 
      // 梦想钱包可能需要特殊处理，先使用通用API
      walletAPI.transferIn(wallet.id, amount, description, imageUrl, note) :
      walletAPI.transferIn(wallet.id, amount, description, imageUrl, note)
    
    transferPromise
      .then(result => {
        console.log('转入API返回结果:', result)
        // 使用 showModal 显示完整的转入金额（showToast title 最长7个字符会被截断）
        wx.showModal({
          title: '转入成功',
          content: `成功转入 ¥${amount}`,
          showCancel: false,
          confirmText: '完成',
          success: (res) => {
            // 用户点击完成后的处理在then中继续
          }
        })

        // 自动生成AI评论，并在完成后刷新页面
        if (result.data && result.data.id) {
          this.generateAiAutoComment(result.data.id).then(() => {
            // AI评论生成完成后，刷新页面
            this.refreshAndGoBack()
          }).catch(() => {
            // AI评论生成失败，也要刷新页面
            this.refreshAndGoBack()
          })
        } else {
          // 没有交易ID，直接刷新
          setTimeout(() => {
            this.refreshAndGoBack()
          }, 1500)
        }
      })
      .catch(error => {

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
  },

  // 生成AI自动评论
  generateAiAutoComment(transactionId) {
    console.log('开始生成AI评论，交易ID:', transactionId)
    
    return new Promise((resolve, reject) => {
      // 设置超时，避免无限等待
      const timeout = setTimeout(() => {
        console.log('AI评论生成超时')
        resolve() // 超时也算完成，不阻塞页面刷新
      }, 8000) // 8秒超时
      
      walletAPI.generateAiAutoComment(transactionId, this.data.walletId)
        .then(response => {
          clearTimeout(timeout)
          console.log('AI评论生成成功:', response.data)
          // 显示AI评论生成成功的提示
          wx.showToast({
            title: 'AI伴侣已评论',
            icon: 'none',
            duration: 1500
          })
          resolve(response)
        })
        .catch(error => {
          clearTimeout(timeout)
          console.log('AI评论生成失败:', error)
          // 静默失败，不影响用户体验
          resolve() // 失败也算完成，继续刷新页面
        })
    })
  },

  // 刷新页面并返回
  refreshAndGoBack() {
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
  }

})
