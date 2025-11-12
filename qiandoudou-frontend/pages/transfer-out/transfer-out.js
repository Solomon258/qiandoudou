// pages/transfer-out/transfer-out.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: null,
    wallet: {},
    transferAmount: '',
    transferNote: '',
    noteLength: 0,
    uploadedImage: '', // OSS图片URL（用于显示）
    uploadedImageLocal: '', // 本地图片路径（用于AI分析）
    selectedBankCard: 'pingan',
    selectedOtherWallet: null, // 选中的其他钱包ID
    selectedWalletName: '', // 选中的钱包名称
    availableAmount: '0.00',
    transferLoading: false,
    // 光标控制相关
    noteCursor: -1,
    noteSelectionStart: -1,
    noteSelectionEnd: -1,
    isUserTyping: false,
    // 其他钱包相关
    otherWallets: [], // 用户的其他钱包列表
    showWalletList: false // 是否显示钱包列表
  },

  onLoad(options) {

    const walletId = options.id || options.walletId


    
    this.setData({
      walletId: walletId
    })
    
    this.loadWalletInfo()
  },

  // 加载钱包信息
  loadWalletInfo() {
    const walletId = this.data.walletId
    if (!walletId) {

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

        this.setData({
          wallet: wallet,
          availableAmount: wallet.balance
        })

        // 加载用户的其他钱包列表（排除当前钱包）
        this.loadOtherWallets(wallet.userId)
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '加载钱包信息失败',
          icon: 'none'
        })
      })
  },

  // 加载用户的其他钱包列表
  loadOtherWallets(userId) {
    walletAPI.getUserWallets(userId, false)
      .then(result => {
        let wallets = result.data || []

        // 过滤出除了当前钱包以外的钱包
        wallets = wallets.filter(w => w.id !== this.data.walletId)

        this.setData({
          otherWallets: wallets
        })
      })
      .catch(error => {
        console.error('加载其他钱包失败:', error)
        // 加载失败不影响主流程，只显示错误日志
      })
  },

  // 选择银行卡
  selectBankCard(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      selectedBankCard: type,
      selectedOtherWallet: null, // 选择银行卡时清除钱包选择
      selectedWalletName: '', // 清除钱包名称
      showWalletList: false
    })
  },

  // 显示其他钱包列表
  toggleWalletList() {
    this.setData({
      showWalletList: !this.data.showWalletList,
      selectedBankCard: null // 选择钱包时清除银行卡选择
    })
  },

  // 选择其他钱包
  selectOtherWallet(e) {
    const walletId = e.currentTarget.dataset.walletId
    const walletIndex = this.data.otherWallets.findIndex(w => w.id === walletId)

    if (walletIndex !== -1) {
      const selectedWallet = this.data.otherWallets[walletIndex]
      this.setData({
        selectedOtherWallet: walletId,
        selectedWalletName: selectedWallet.name,
        showWalletList: false
      })
    }
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

  // 全部转出
  transferAll() {
    this.setData({
      transferAmount: this.data.availableAmount
    })
  },

  // 备注输入
  onNoteInput(e) {
    let value = e.detail.value
    let cursor = e.detail.cursor

    // 限制备注最多200字
    if (value.length > 200) {
      value = value.substring(0, 200)
      // 如果光标位置超过200，调整光标位置
      if (cursor > 200) {
        cursor = 200
      }
    }

    this.setData({
      transferNote: value,
      noteLength: value.length,
      noteCursor: cursor,
      isUserTyping: true
    })
  },

  // 备注获得焦点
  onNoteFocus(e) {

    this.setData({
      isUserTyping: true
    })
  },

  // 备注失去焦点
  onNoteBlur(e) {

    this.setData({
      isUserTyping: false
    })
  },

  // 重新生成备注
  retryNote() {
    // 如果用户正在输入，先提示
    if (this.data.isUserTyping) {
      wx.showToast({
        title: '请先完成当前输入',
        icon: 'none'
      })
      return
    }
    
    // 如果有上传的图片，调用AI接口生成文案
    if (this.data.uploadedImage) {
      this.generateNoteFromImage()
    } else {
      // 如果没有图片，使用默认的随机文案
      const notes = [
        ''
      ]
      const randomNote = notes[Math.floor(Math.random() * notes.length)]
      
      // 先清空，再设置新内容，并将光标设置到末尾
      this.setData({
        transferNote: '',
        noteLength: 0,
        noteCursor: -1
      })
      
      // 延迟设置新内容，确保光标正确定位
      setTimeout(() => {
        this.setData({
          transferNote: randomNote,
          noteLength: randomNote.length,
          noteCursor: randomNote.length
        })
      }, 50)
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
        
        // 先清空，再设置新内容，并将光标设置到末尾
        this.setData({
          transferNote: '',
          noteLength: 0,
          noteCursor: -1
        })
        
        // 延迟设置新内容，确保光标正确定位
        setTimeout(() => {
          this.setData({
            transferNote: generatedText,
            noteLength: generatedText.length,
            noteCursor: generatedText.length
          })
        }, 50)
        
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
          transferNote: '',
          noteLength: 0,
          noteCursor: -1
        })
        setTimeout(() => {
          this.setData({
            transferNote: defaultNote,
            noteLength: defaultNote.length,
            noteCursor: defaultNote.length
          })
        }, 50)
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
        uploadUserImage(tempFilePath, 'transfer_out', userId)
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

  // 确认转出
  confirmTransferOut() {
    const { transferAmount, transferNote, wallet, selectedBankCard, selectedOtherWallet } = this.data

    // 校验1：金额是否为空
    if (!transferAmount || parseFloat(transferAmount) <= 0) {
      wx.showToast({
        title: '请输入转出金额',
        icon: 'error'
      })
      return
    }

    // 校验2：金额上限检查（不能超过 10,000,000）
    const maxAmount = 10000000
    const amount = parseFloat(transferAmount)
    if (amount > maxAmount) {
      wx.showModal({
        title: '金额超过限制',
        content: `单次转出金额不能超过 ¥${maxAmount.toLocaleString()}，请重新输入。`,
        showCancel: false,
        confirmText: '确定'
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

    // 检查是否选择了转出目标（银行卡或其他钱包）
    if (!selectedBankCard && !selectedOtherWallet) {
      wx.showToast({
        title: '请选择转出目标',
        icon: 'error'
      })
      return
    }

    if (amount > wallet.balance) {
      wx.showToast({
        title: '转出金额不能超过余额',
        icon: 'error'
      })
      return
    }

    // 所有验证通过后才设置loading
    this.setData({ transferLoading: true })

    const description = transferNote || '转出'
    const imageUrl = this.data.uploadedImage || null
    const note = transferNote || null

    // 如果选择的是其他钱包，调用转账到钱包的API
    // 如果选择的是银行卡，调用原有的转出API
    const transferPromise = selectedOtherWallet ?
      walletAPI.transferToWallet(wallet.id, selectedOtherWallet, amount, description, imageUrl, note) :
      walletAPI.transferOut(wallet.id, amount, description, imageUrl, note)

    transferPromise
      .then(result => {
        // 使用 showModal 显示完整的转出金额（showToast title 最长7个字符会被截断）
        wx.showModal({
          title: '转出成功',
          content: `成功转出 ¥${amount}`,
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

        this.setData({ transferLoading: false })
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '转出失败',
          icon: 'none'
        })
        this.setData({ transferLoading: false })
      })
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
