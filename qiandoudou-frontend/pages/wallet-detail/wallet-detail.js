// pages/wallet-detail/wallet-detail.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: null,
    wallet: {},
    walletBackgroundStyle: '',
    transactions: [],
    showTransferModal: false,
    showBackgroundModal: false,
    transferType: 'in', // 'in' 或 'out'
    transferForm: {
      amount: '',
      description: ''
    },
    transferLoading: false,
    backgroundLoading: false,
    selectedBackground: '',
    selectedImageUrl: '', // OSS上传的图片URL
    activeTab: 'bill', // 'bill' 或 'stats'
    showPromoCard: true,
    showNameEditModal: false,
    editingName: '',
    showBackgroundHint: false,
    isOwnWallet: true, // 是否为自己的钱包
    fromSocial: false, // 是否来自社交圈
    showCommentModal: false, // 评论模态框
    currentTransaction: null, // 当前评论的交易
    currentComments: [], // 当前交易的评论列表
    commentText: '', // 评论输入内容
    commentLoading: false, // 评论发送中
    socialStats: { // 社交统计数据 - 新钱包强制显示0
      fansCount: 0,  // 新钱包粉丝数为0
      likesCount: 0, // 新钱包获赞数为0
      viewsCount: 0  // 新钱包浏览数为0
    },
    isFollowing: false, // 是否已关注
    // AI语音播放相关
    currentPlayingVoice: null, // 当前播放的语音
    voiceContext: null, // 语音上下文
    // 统计功能相关
    currentYear: new Date().getFullYear(), // 当前年份
    currentMonth: new Date().getMonth() + 1, // 当前月份
    currentDate: '', // 当前日期字符串，用于picker
    monthlyStats: { // 月度统计数据
      monthlyIncome: '0.00',
      monthlyExpense: '0.00',
      monthlyProfit: '0.00',
      transactionCount: 0
    },
    statsLoading: false, // 统计数据加载状态
    backgroundOptions: [
      { value: 'gradient1', name: '蓝紫渐变', gradient: 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);' },
      { value: 'gradient2', name: '粉红渐变', gradient: 'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);' },
      { value: 'gradient3', name: '绿色渐变', gradient: 'background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);' },
      { value: 'gradient4', name: '橙色渐变', gradient: 'background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);' },
      { value: 'gradient5', name: '紫色渐变', gradient: 'background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);' },
      { value: 'gradient6', name: '金色渐变', gradient: 'background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);' }
    ],
    showWalletSettings: false, // 显示钱包设置模态框
    userScriptProgress: null, // 用户在"重新养小时候的自己"剧本的进度
    scriptInfo: null // "重新养小时候的自己"剧本信息
  },

  onLoad(options) {
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    const walletId = options.id
    const fromSocial = options.fromSocial === 'true'

    // 从URL参数中读取社交信息
    const socialInfo = fromSocial ? {
      owner_nickname: decodeURIComponent(options.ownerNickname || ''),
      title: decodeURIComponent(options.title || ''),
      fansCount: parseInt(options.fansCount || '0'),
      likeCount: parseInt(options.likeCount || '0')
    } : null

    // 初始化当前日期
    const now = new Date()
    const currentDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    
    if (walletId) {
      this.setData({ 
        walletId,
        fromSocial: fromSocial,
        socialInfo: socialInfo,
        isOwnWallet: !fromSocial, // 如果来自社交圈，则不是自己的钱包
        currentDate: currentDate
      })
      this.loadWalletDetail()
      this.loadTransactions()
      
      // 加载社交统计数据（钱包的粉丝、获赞、浏览数）
      this.loadSocialStats()
      
      // 如果不是自己的钱包，记录浏览
      if (fromSocial) {
        this.recordWalletView()
      }
      
      // 如果是自己的钱包，加载统计数据
      if (!fromSocial) {
        this.loadMonthlyStats()
      }
      
      // 如果不是自己的钱包，检查关注状态
      if (!this.data.isOwnWallet) {
        this.checkFollowStatus()
      }
    }
  },

  onShow() {

    // 重置音频播放状态，避免状态不一致
    this.resetAudioState()
    
    // 页面显示时重新加载钱包详情、交易记录和背景样式
    if (this.data.walletId) {

      this.loadWalletDetail() // 重新加载钱包详情（包括余额）
      this.loadTransactions() // 刷新交易记录
      
      // 如果已经有钱包数据，更新背景样式
      if (this.data.wallet && this.data.wallet.id) {
        this.updateBackgroundStyle()
      }
      
      // 如果是自己的钱包且当前在统计标签页，刷新统计数据
      if (this.data.isOwnWallet && this.data.activeTab === 'stats') {

        this.loadMonthlyStats()
      }
    }
    
    // 如果不是自己的钱包，重新检查关注状态
    if (!this.data.isOwnWallet && this.data.walletId) {

      this.checkFollowStatus()
    }
    
    // 延迟输出状态，确保数据已加载
    setTimeout(() => {

    }, 1000)
  },

  // 加载钱包详情
  loadWalletDetail() {
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

        // 验证是否为自己的钱包
        const currentUserId = app.globalData.userInfo?.id
        const isOwnWallet = (wallet.userId == currentUserId || wallet.user_id == currentUserId)
        
        // 如果来自社交圈，需要获取钱包所有者的信息
        if (this.data.fromSocial && !isOwnWallet) {
          // 从公开钱包API获取的数据可能已经包含owner信息，直接使用

        }
        
        // 如果来自社交圈，即使不是自己的钱包也要显示详情
        if (!isOwnWallet && !this.data.fromSocial) {

          wx.redirectTo({
            url: `/pages/user-profile/user-profile?userId=${wallet.userId || wallet.user_id}&walletId=${walletId}`
          })
          return
        }
        
        // 如果来自社交圈，显示为只读模式
        if (!isOwnWallet && this.data.fromSocial) {

          // 使用URL参数传递的社交信息
          const socialInfo = this.data.socialInfo

          if (socialInfo) {
            // 合并钱包数据和社交信息
            wallet.owner_nickname = socialInfo.owner_nickname
            wallet.followers_count = socialInfo.fansCount
            wallet.likes_count = socialInfo.likeCount
            wallet.comments_count = 0 // 暂时设为0
            
            // 初始设置社交统计数据为0，确保新钱包显示正确
            this.setData({
              socialStats: {
                fansCount: 0,  // 新钱包粉丝数为0
                likesCount: 0, // 新钱包获赞数为0
                viewsCount: 0  // 新钱包浏览数为0
              },
              isFollowing: false // 默认未关注，实际应该从API获取
            })
            
            // 从后端获取真实的社交统计数据
            this.loadSocialStats()
          }
        } else if (isOwnWallet) {
          // 如果是自己的钱包，加载社交统计数据
          this.loadSocialStats()
        } else if (this.data.fromSocial) {
          // 如果来自社交圈但不是自己的钱包，也要加载钱包所有者的社交统计数据
          this.loadSocialStats()
        }
        
        // 如果来自社交圈但没有社交信息，从后端获取真实统计数据
        if (this.data.fromSocial && !this.data.socialInfo) {
          this.loadSocialStats()
        }
        
        // 确保钱包公开状态有正确的默认值
        if (wallet.is_public === null || wallet.is_public === undefined) {

          wallet.is_public = 1
        }

        this.setData({
          wallet,
          selectedBackground: wallet.backgroundImage || 'gradient1',
          isOwnWallet: isOwnWallet
        })
        
        // 更新背景样式
        this.updateBackgroundStyle()
        
        // 如果是自己的钱包，加载"重新养小时候的自己"剧本进度
        if (isOwnWallet) {
          this.loadUserScriptProgress()
        }
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '加载钱包详情失败',
          icon: 'none'
        })
      })
  },

  // 加载交易记录
  loadTransactions() {
    const walletId = this.data.walletId
    if (!walletId) {
      return
    }

    walletAPI.getWalletTransactions(walletId)
      .then(result => {
        const transactions = result.data || []
        
        // 格式化交易记录
        const formattedTransactions = transactions.map((transaction, index) => {
          // 副标题是交易类型
          const subtitle = transaction.type === 1 ? '转入' : (transaction.type === 2 ? '转出' : '剧本攒钱')
          
          // 处理时间字段 - 确保正确解析
          let createTime = transaction.createTime || transaction.create_time
          let formattedTime = '未知时间'

          if (createTime) {
            try {
              // 如果是字符串，直接解析；如果是时间戳，转换
              const timeObj = typeof createTime === 'string' ? new Date(createTime) : new Date(createTime)
              
              if (!isNaN(timeObj.getTime())) {
                formattedTime = this.formatTime(timeObj.getTime())
              } else {
                // 对于新创建的交易，使用当前时间
                formattedTime = '刚刚'
              }
            } catch (e) {

              formattedTime = '刚刚'
            }
          } else {
            // 没有时间数据时使用当前时间
            formattedTime = '刚刚'
          }
          
          // 检查是否为AI伴侣交易
          const isAiTransaction = transaction.aiPartnerId || transaction.ai_partner_id || transaction.isAiTransaction

          return {
            ...transaction,
            createTime: formattedTime,
            create_time: formattedTime,
            amount: parseFloat(transaction.amount).toFixed(2),
            subtitle: subtitle, // 副标题（灰色显示，如"转入"）
            category: subtitle,
            // AI伴侣相关数据
            isAiTransaction: isAiTransaction,
            aiPartnerName: transaction.aiPartnerName || transaction.ai_partner_name,
            aiPartnerAvatar: transaction.aiPartnerAvatar || transaction.ai_partner_avatar,
            aiMessage: transaction.aiMessage || transaction.ai_message || transaction.description,
            voiceUrl: transaction.voiceUrl || transaction.voice_url,
            voiceDuration: transaction.voiceDuration || transaction.voice_duration || '12s',
            isPlaying: false,
            // 剧本攒钱相关数据
            scriptCoverImage: transaction.scriptCoverImage,
            scriptTitle: transaction.scriptTitle,
            // 初始化社交互动数据，稍后从后端加载
            likeCount: null, // 设为null表示正在加载中
            commentCount: null,
            isLiked: false,
            comments: []
          }
        })

        this.setData({
          transactions: formattedTransactions
        })
        
        // 加载每个交易的真实社交数据（点赞状态、评论数等）
        this.loadTransactionsSocialData(formattedTransactions)
      })
      .catch(error => {

        // 如果加载失败，显示空数组
        this.setData({
          transactions: []
        })
      })
  },

  // 加载交易的社交数据
  loadTransactionsSocialData(transactions) {
    const currentUserId = app.globalData.userInfo?.id
    
    // 为每个交易并行加载社交数据
    const socialDataPromises = transactions.map(transaction => {
      return walletAPI.getTransactionSocialData(transaction.id, currentUserId)
        .then(response => {

          if (response.success && response.data) {
            return {
              transactionId: transaction.id,
              socialData: response.data
            }
          }
          return null
        })
        .catch(error => {

          return null
        })
    })
    
    // 等待所有社交数据加载完成
    Promise.all(socialDataPromises).then(results => {
      const updatedTransactions = [...this.data.transactions]
      
      results.forEach((result, index) => {
        if (result) {
          const transactionIndex = updatedTransactions.findIndex(t => t.id === result.transactionId)
          if (transactionIndex !== -1) {
            // 成功获取到数据，更新社交数据
            updatedTransactions[transactionIndex].likeCount = result.socialData.likeCount || 0
            updatedTransactions[transactionIndex].commentCount = result.socialData.commentCount || 0
            updatedTransactions[transactionIndex].isLiked = result.socialData.isLiked || false
          }
        } else {
          // 如果获取失败，将对应的交易数据设置为默认值
          if (index < updatedTransactions.length && updatedTransactions[index].likeCount === null) {
            updatedTransactions[index].likeCount = 0
            updatedTransactions[index].commentCount = 0
          }
        }
      })

      this.setData({
        transactions: updatedTransactions
      })
    })
  },

  // 更新背景样式
  updateBackgroundStyle() {
    const wallet = this.data.wallet
    
    // 获取背景设置（兼容不同的字段名）
    let currentBackground = wallet.backgroundImage || wallet.background_image || 'gradient1'

    let backgroundStyle = ''
    
    if (!currentBackground) {
      // 没有背景设置，使用默认背景
      backgroundStyle = wallet.type === 2 ? 
        'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);' : 
        'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'
    } else if (currentBackground.startsWith('http')) {
      // OSS图片URL背景
      backgroundStyle = `background-image: url('${currentBackground}'); background-size: cover; background-position: center;`

    } else if (currentBackground.startsWith('data:')) {
      // base64图片背景（向后兼容）
      backgroundStyle = `background-image: url('${currentBackground}'); background-size: cover; background-position: center;`

    } else if (currentBackground.startsWith('custom_bg_')) {
      // 本地存储的自定义图片背景（向后兼容）
      const customImages = wx.getStorageSync('custom_images') || {}
      const imagePath = customImages[currentBackground]
      if (imagePath) {
        backgroundStyle = `background-image: url('${imagePath}'); background-size: cover; background-position: center;`

      } else {
        // 图片不存在，使用默认背景

        backgroundStyle = wallet.type === 2 ? 
          'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);' : 
          'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'
      }
    } else {
      // 预设渐变背景
      const bg = this.data.backgroundOptions.find(bg => bg.value === currentBackground)
      if (bg) {
        backgroundStyle = bg.gradient

      } else {
        // 未知背景类型，使用默认背景

        backgroundStyle = wallet.type === 2 ? 
          'background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);' : 
          'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);'
      }
    }
    
    this.setData({
      walletBackgroundStyle: backgroundStyle
    })
  },

  // 获取钱包背景样式（兼容方法）
  getWalletBackgroundStyle() {
    return this.data.walletBackgroundStyle
  },

  // 显示转账对话框
  showTransferDialog(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      showTransferModal: true,
      transferType: type,
      transferForm: {
        amount: '',
        description: ''
      }
    })
  },

  // 隐藏转账对话框
  hideTransferModal() {
    this.setData({
      showTransferModal: false
    })
  },

  // 输入金额
  onAmountInput(e) {
    this.setData({
      'transferForm.amount': e.detail.value
    })
  },

  // 输入说明
  onDescriptionInput(e) {
    this.setData({
      'transferForm.description': e.detail.value
    })
  },

  // 处理转账
  handleTransfer() {
    const { transferType, transferForm, wallet } = this.data

    if (!transferForm.amount || parseFloat(transferForm.amount) <= 0) {
      wx.showToast({
        title: '请输入有效金额',
        icon: 'none'
      })
      return
    }

    if (transferType === 'out' && parseFloat(transferForm.amount) > wallet.balance) {
      wx.showToast({
        title: '余额不足',
        icon: 'none'
      })
      return
    }

    this.setData({ transferLoading: true })

    const amount = parseFloat(transferForm.amount)
    const description = transferForm.description || (transferType === 'in' ? '转入' : '转出')

    // 调用后端API（暂时不传递图片和备注，保持简单）
    const apiCall = transferType === 'in' ? 
      walletAPI.transferIn(wallet.id, amount, description, null, null) :
      walletAPI.transferOut(wallet.id, amount, description, null, null)

    apiCall
      .then(result => {
        wx.showToast({
          title: `${transferType === 'in' ? '转入' : '转出'}成功`,
          icon: 'success'
        })

        // 重新加载钱包详情和交易记录
        this.loadWalletDetail()
        this.loadTransactions()
        
        // 如果当前在统计标签页，刷新统计数据
        if (this.data.activeTab === 'stats') {
          this.loadMonthlyStats()
        }

        this.setData({
          showTransferModal: false,
          transferLoading: false,
          transferForm: {
            amount: '',
            description: ''
          }
        })
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '转账失败',
          icon: 'none'
        })
        this.setData({ transferLoading: false })
      })
  },

  // 显示背景选择对话框
  showBackgroundModal() {
    const currentBackground = this.data.wallet.backgroundImage || this.data.wallet.background_image || 'gradient1'

    this.setData({
      showBackgroundModal: true,
      showBackgroundHint: false,
      selectedBackground: currentBackground
    })
  },

  // 隐藏背景选择对话框
  hideBackgroundModal() {
    this.setData({
      showBackgroundModal: false,
      selectedBackground: '', // 清空预设背景选择
      selectedImageUrl: ''     // 清空OSS图片选择
    })
  },

  // 编辑钱包名称
  editWalletName() {
    this.setData({
      showNameEditModal: true,
      editingName: this.data.wallet.name || ''
    })
  },

  // 隐藏名称编辑对话框
  hideNameEditModal() {
    this.setData({
      showNameEditModal: false,
      editingName: ''
    })
  },

  // 钱包名称输入
  onNameInput(e) {
    this.setData({
      editingName: e.detail.value
    })
  },

  // 确认修改钱包名称
  confirmNameEdit() {
    const newName = this.data.editingName.trim()
    
    if (!newName) {
      wx.showToast({
        title: '钱包名称不能为空',
        icon: 'none'
      })
      return
    }

    if (newName === this.data.wallet.name) {
      this.hideNameEditModal()
      return
    }

    // 调用后端API更新钱包名称
    walletAPI.updateWalletName(this.data.wallet.id, newName)
      .then(result => {
        // 更新本地数据
        const updatedWallet = { ...this.data.wallet, name: newName }
        this.setData({
          wallet: updatedWallet,
          showNameEditModal: false,
          editingName: ''
        })

        // 通知首页刷新钱包列表
        const pages = getCurrentPages()
        if (pages.length > 1) {
          const prevPage = pages[pages.length - 2]
          if (prevPage && prevPage.loadWallets) {
            setTimeout(() => {
              prevPage.loadWallets()
            }, 100)
          }
        }

        wx.showToast({
          title: '钱包名称修改成功',
          icon: 'success'
        })
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '修改失败',
          icon: 'none'
        })
      })
  },

  // 选择背景
  selectBackground(e) {
    const value = e.currentTarget.dataset.value

    // 直接更新选中状态
    this.setData({
      selectedBackground: value
    })

  },

  // 上传自定义图片到OSS
  uploadCustomImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'], // 使用压缩图片
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0]

        // 显示加载提示
        wx.showLoading({
          title: '上传图片中...'
        })
        
        // 检查文件大小
        wx.getFileInfo({
          filePath: tempFilePath,
          success: (fileInfo) => {

            // 如果文件大于5MB，提示用户
            if (fileInfo.size > 5 * 1024 * 1024) {
              wx.hideLoading()
              wx.showModal({
                title: '图片过大',
                content: '图片大小不能超过5MB，请选择更小的图片',
                showCancel: false,
                confirmText: '知道了'
              })
              return
            }
            
            // 上传图片到后端OSS
            this.uploadImageToOSS(tempFilePath)
          },
          fail: (error) => {

            // 如果获取文件信息失败，直接尝试上传
            this.uploadImageToOSS(tempFilePath)
          }
        })
      },
      fail: (error) => {

        wx.showToast({
          title: '选择图片失败',
          icon: 'error'
        })
      }
    })
  },

  // 上传图片到OSS
  uploadImageToOSS(filePath) {
    const api = require('../../utils/api.js')
    
    // 使用API工具函数上传文件
    api.uploadFile(filePath, '/wallet/upload-background', {
      walletId: this.data.walletId
    }).then((response) => {

      wx.hideLoading()
      
      if (response.data && response.data.imageUrl) {
        const ossUrl = response.data.imageUrl
        
        // 设置选中的背景为OSS URL
        this.setData({
          selectedBackground: '', // 清空预设背景选择
          selectedImageUrl: ossUrl // 设置自定义图片URL
        })
        
        wx.showToast({
          title: '图片上传成功',
          icon: 'success'
        })
      } else {
        wx.showToast({
          title: '上传响应异常',
          icon: 'error'
        })
      }
    }).catch((error) => {

      wx.hideLoading()
      wx.showToast({
        title: error.message || '图片上传失败',
        icon: 'error'
      })
    })
  },

  // 将图片转换为base64格式存储（已废弃，保留用于向后兼容）
  convertImageToBase64(filePath) {
    wx.getFileSystemManager().readFile({
      filePath: filePath,
      encoding: 'base64',
      success: (res) => {
        const base64Data = 'data:image/jpeg;base64,' + res.data

        // 检查base64数据大小（微信小程序存储限制）
        if (base64Data.length > 2 * 1024 * 1024) { // 2MB限制
          wx.hideLoading()
          wx.showModal({
            title: '图片过大',
            content: '图片处理后仍然过大，请选择更小的图片或使用预设背景',
            showCancel: false,
            confirmText: '知道了'
          })
          return
        }
        
        const customImageKey = `custom_bg_${this.data.walletId}_${Date.now()}`
        
        // 保存base64数据到缓存
        try {
          const customImages = wx.getStorageSync('custom_images') || {}
          customImages[customImageKey] = base64Data
          wx.setStorageSync('custom_images', customImages)
          
          // 更新钱包背景
          this.applyCustomBackground(customImageKey, base64Data)
          
          wx.hideLoading()
          wx.showToast({
            title: '背景设置成功',
            icon: 'success'
          })
          
          // 关闭模态框
          this.hideBackgroundModal()
        } catch (error) {

          wx.hideLoading()
          
          if (error.message && error.message.includes('exceed')) {
            wx.showModal({
              title: '存储空间不足',
              content: '本地存储空间不足，请清理一些数据或使用预设背景',
              showCancel: true,
              cancelText: '取消',
              confirmText: '清理数据',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  this.clearStorageData()
                }
              }
            })
          } else {
            wx.showToast({
              title: '图片保存失败',
              icon: 'error'
            })
          }
        }
      },
      fail: (error) => {

        wx.hideLoading()
        wx.showToast({
          title: '图片处理失败',
          icon: 'error'
        })
      }
    })
  },

  // 清理存储数据（已废弃，保留用于向后兼容）
  clearStorageData() {
    wx.showActionSheet({
      itemList: ['清理自定义背景图片', '清理所有缓存数据'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 只清理自定义图片
          wx.removeStorageSync('custom_images')
          wx.showToast({
            title: '背景图片已清理',
            icon: 'success'
          })
        } else if (res.tapIndex === 1) {
          // 清理所有缓存
          wx.showModal({
            title: '确认清理',
            content: '这将清理所有本地数据，包括创建的钱包和设置，确定继续吗？',
            success: (modalRes) => {
              if (modalRes.confirm) {
                try {
                  wx.clearStorageSync()
                  wx.showToast({
                    title: '数据已清理',
                    icon: 'success'
                  })
                  // 重新加载页面
                  setTimeout(() => {
                    wx.navigateBack()
                  }, 1500)
                } catch (error) {

                  wx.showToast({
                    title: '清理失败',
                    icon: 'error'
                  })
                }
              }
            }
          })
        }
      }
    })
  },

  // 应用自定义背景（已废弃，保留用于向后兼容）
  applyCustomBackground(imageKey, base64Data) {
    const wallet = this.data.wallet
    
    // 调用后端API更新背景
    walletAPI.updateWalletBackground(wallet.id, imageKey)
      .then(result => {
        // 更新当前页面显示
        const updatedWallet = { ...wallet, backgroundImage: imageKey }
        this.setData({
          wallet: updatedWallet,
          selectedBackground: imageKey
        })
        this.updateBackgroundStyle()
        
        // 通知首页刷新
        const pages = getCurrentPages()
        if (pages.length > 1) {
          const prevPage = pages[pages.length - 2]
          if (prevPage.loadWallets) {
            setTimeout(() => {
              prevPage.loadWallets()
            }, 50)
          }
        }
      })
      .catch(error => {

        wx.showToast({
          title: '保存背景失败',
          icon: 'none'
        })
      })
  },

  // 更换背景
  changeBackground() {
    const { wallet, selectedBackground, selectedImageUrl } = this.data
    
    // 确定要保存的背景：优先使用OSS URL，然后是预设背景，最后是当前背景
    let backgroundToSave
    if (selectedImageUrl) {
      // 使用OSS上传的图片URL
      backgroundToSave = selectedImageUrl
    } else if (selectedBackground) {
      // 使用预设渐变背景
      backgroundToSave = selectedBackground
    } else {
      // 使用当前背景
      backgroundToSave = wallet.backgroundImage || wallet.background_image || 'gradient1'
    }

    this.setData({ backgroundLoading: true })
    
    walletAPI.updateWalletBackground(wallet.id, backgroundToSave)
      .then(result => {
        // 更新本地钱包数据
        const updatedWallet = { ...wallet, backgroundImage: backgroundToSave }
        
        this.setData({
          wallet: updatedWallet,
          showBackgroundModal: false,
          backgroundLoading: false
        })
        
        // 更新背景样式
        this.updateBackgroundStyle()

        // 通知首页刷新钱包列表
        const pages = getCurrentPages()

        // 查找首页并强制刷新
        const homePage = pages.find(page => 
          (page.route && page.route.includes('home')) || 
          (page.__route__ && page.__route__.includes('home'))
        )
        
        if (homePage) {

          if (homePage.loadWallets) {
            homePage.loadWallets()
          }
          if (homePage.forceRefreshWallets) {
            homePage.forceRefreshWallets()
          }
        } else {

          if (pages.length > 1) {
            const prevPage = pages[pages.length - 2]
            if (prevPage && prevPage.loadWallets) {
              prevPage.loadWallets()
            }
          }
        }

        wx.showToast({
          title: '背景更换成功',
          icon: 'success'
        })
      })
      .catch(error => {

        wx.showToast({
          title: error.message || '更换背景失败',
          icon: 'none'
        })
        this.setData({ backgroundLoading: false })
      })
  },

  // 跳转到转入页面
  goToTransferIn() {
    wx.navigateTo({
      url: `/pages/transfer-in/transfer-in?id=${this.data.walletId}`
    })
  },

  // 跳转到转出页面
  goToTransferOut() {
    wx.navigateTo({
      url: `/pages/transfer-out/transfer-out?id=${this.data.walletId}`
    })
  },

  // 跳转到剧本页面
  goToScripts() {
    wx.navigateTo({
      url: '/pages/script-detail/script-detail?walletId=' + this.data.walletId
    })
  },

  // 切换标签
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({
      activeTab: tab
    })
    
    // 如果切换到统计标签，加载统计数据
    if (tab === 'stats' && this.data.isOwnWallet) {
      this.loadMonthlyStats()
    }
  },

  // 显示过滤选项
  showFilter() {
    wx.showActionSheet({
      itemList: ['全部', '转入', '转出', '剧本攒钱'],
      success: (res) => {

        // TODO: 实现过滤逻辑
      }
    })
  },

  // 关闭推广卡片
  closePromo() {
    this.setData({
      showPromoCard: false
    })
  },

  // 切换点赞状态
  toggleLike(e) {
    // 检查用户是否已登录
    const app = getApp()
    if (!app.isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }

    const transaction = e.currentTarget.dataset.transaction
    const transactions = this.data.transactions
    const index = transactions.findIndex(t => t.id === transaction.id)
    
    if (index !== -1) {
      // 防止重复点击
      if (transactions[index].liking) {

        return
      }
      
      // 防止点赞数为null时的操作（数据还在加载中）
      if (transactions[index].likeCount === null) {

        wx.showToast({
          title: '加载中，请稍后',
          icon: 'none'
        })
        return
      }
      const isLiked = !transactions[index].isLiked
      const userId = app.globalData.userInfo?.id
      const transactionId = transaction.id
      
      // 设置loading状态
      transactions[index].liking = true
      
      // 先更新UI，提供即时反馈
      transactions[index].isLiked = isLiked
      transactions[index].likeCount = isLiked ? 
        (transactions[index].likeCount || 0) + 1 : 
        Math.max((transactions[index].likeCount || 0) - 1, 0)
      
      this.setData({ transactions })
      
      // 调用API同步到后端（后端会自动切换点赞状态）
      walletAPI.likeTransaction(userId, transactionId)
        .then(result => {
          // 重新获取交易的社交数据，确保数据同步
          walletAPI.getTransactionSocialData(transactionId, userId)
            .then(response => {

              if (response.success && response.data) {
                transactions[index].likeCount = response.data.likeCount || 0
                transactions[index].isLiked = response.data.isLiked || false
              }
            })
            .catch(error => {

            })
            .finally(() => {
              // 清除loading状态
              transactions[index].liking = false
              this.setData({ transactions })
            })
          
          wx.showToast({
            title: isLiked ? '已点赞' : '取消点赞',
            icon: 'none'
          })
        })
        .catch(error => {

          // 如果API失败，回滚UI状态
          transactions[index].isLiked = !isLiked
          transactions[index].likeCount = isLiked ? 
            Math.max((transactions[index].likeCount || 0) - 1, 0) :
            (transactions[index].likeCount || 0) + 1
          // 清除loading状态
          transactions[index].liking = false
          this.setData({ transactions })
          
          // 如果是404错误，说明API接口不存在，使用模拟模式
          if (error.message && error.message.includes('404')) {
            wx.showToast({
              title: isLiked ? '已点赞' : '取消点赞',
              icon: 'none'
            })

          } else {
            wx.showToast({
              title: '操作失败',
              icon: 'none'
            })
          }
        })
    }
  },

  // 显示评论
  showComments(e) {
    const transaction = e.currentTarget.dataset.transaction

    // 从后端获取真实评论数据
    this.loadTransactionComments(transaction.id)
    
    this.setData({
      showCommentModal: true,
      currentTransaction: transaction,
      currentComments: [],
      commentText: ''
    })
  },

  // 加载交易评论
  loadTransactionComments(transactionId) {

    walletAPI.getTransactionComments(transactionId)
      .then(response => {

        if (response.success && response.data) {
          // 处理评论数据格式
          const comments = response.data.map(comment => ({
            id: comment.id,
            userName: comment.user_nickname || comment.userName || '匿名用户',
            content: comment.content,
            time: this.formatTime(comment.create_time || comment.createTime),
            userId: comment.user_id || comment.userId
          }))
          
          this.setData({
            currentComments: comments
          })
        } else {

          this.setData({
            currentComments: []
          })
        }
      })
      .catch(error => {

        this.setData({
          currentComments: []
        })
      })
  },

  // 格式化时间
  formatTime(timeInput) {
    if (!timeInput) return '刚刚'
    
    try {
      let commentTime
      
      // 处理Java LocalDateTime数组格式 [year, month, day, hour, minute, second]
      if (Array.isArray(timeInput)) {
        const [year, month, day, hour, minute, second] = timeInput
        // 注意：JavaScript的月份是从0开始的，所以要减1
        commentTime = new Date(year, month - 1, day, hour, minute, second || 0)
      } else if (typeof timeInput === 'string') {
        commentTime = new Date(timeInput.replace(/-/g, '/')) // 兼容iOS
      } else if (typeof timeInput === 'number') {
        commentTime = new Date(timeInput)
      } else {
        commentTime = new Date(timeInput)
      }
      
      // 检查日期是否有效
      if (isNaN(commentTime.getTime())) {

        return '刚刚'
      }
      
      const now = new Date()
      const diffMs = now - commentTime
      const diffMins = Math.floor(diffMs / (1000 * 60))
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
      
      if (diffMins < 1) return '刚刚'
      if (diffMins < 60) return `${diffMins}分钟前`
      if (diffHours < 24) return `${diffHours}小时前`
      if (diffDays < 7) return `${diffDays}天前`
      
      return commentTime.toLocaleDateString()
    } catch (e) {

      return '刚刚'
    }
  },

  // 隐藏评论模态框
  hideCommentModal() {
    this.setData({
      showCommentModal: false,
      currentTransaction: null,
      currentComments: [],
      commentText: ''
    })
  },

  // 评论输入
  onCommentInput(e) {
    const value = e.detail.value

    this.setData({
      commentText: value
    })
  },

  // 发送评论
  sendComment() {

    const { commentText, currentTransaction } = this.data

    if (!commentText || !commentText.trim()) {
      wx.showToast({
        title: '请输入评论内容',
        icon: 'none'
      })
      return
    }

    this.setData({ commentLoading: true })

    const userId = app.globalData.userInfo?.id
    const transactionId = currentTransaction.id
    const content = commentText.trim()

    // 调用真实API发送评论
    walletAPI.commentTransaction(userId, transactionId, content)
      .then(response => {

        if (response.success) {
          const newComment = {
            id: response.data?.id || Date.now(),
            userName: app.globalData.userInfo?.nickname || '我',
            content: content,
            time: '刚刚',
            userId: userId
          }

          this.updateCommentData(newComment)
        } else {
          throw new Error(response.message || '发送评论失败')
        }
      })
      .catch(error => {

        this.setData({ commentLoading: false })
        wx.showToast({
          title: '发送评论失败',
          icon: 'error'
        })
      })
  },

  // 模拟评论发送
  simulateCommentSend(content) {
    setTimeout(() => {
      const newComment = {
        id: Date.now(),
        userName: app.globalData.userInfo?.nickname || '我',
        content: content,
        time: '刚刚'
      }
      this.updateCommentData(newComment)
    }, 1000)
  },

  // 更新评论数据
  updateCommentData(newComment) {
    const updatedComments = [...this.data.currentComments, newComment]
    const currentUserId = app.globalData.userInfo?.id
    const transactionId = this.data.currentTransaction.id
    
    // 从后端重新获取最新的社交数据，确保评论数量准确
    walletAPI.getTransactionSocialData(transactionId, currentUserId)
      .then(response => {
        debugger

        if (response.success && response.data) {
          // 更新交易的评论数和点赞数
          const transactions = this.data.transactions
          const index = transactions.findIndex(t => t.id === transactionId)
          if (index !== -1) {
            transactions[index].commentCount = response.data.commentCount || 0
            transactions[index].likeCount = response.data.likeCount || 0
            transactions[index].isLiked = response.data.isLiked || false
            
            // 更新评论列表
            if (!transactions[index].comments) {
              transactions[index].comments = []
            }
            transactions[index].comments.push(newComment)
          }

          this.setData({
            currentComments: updatedComments,
            transactions: transactions,
            commentText: '',
            commentLoading: false,
            showCommentModal: false
          })
        }
      })
      .catch(error => {

        // 如果API失败，使用本地计算
        const transactions = this.data.transactions
        const index = transactions.findIndex(t => t.id === transactionId)
        if (index !== -1) {
          transactions[index].commentCount = (transactions[index].commentCount || 0) + 1
          if (!transactions[index].comments) {
            transactions[index].comments = []
          }
          transactions[index].comments.push(newComment)
        }

        this.setData({
          currentComments: updatedComments,
          transactions: transactions,
          commentText: '',
          commentLoading: false,
          showCommentModal: false
        })
      })

    wx.showToast({
      title: '评论成功',
      icon: 'success'
    })
  },

  // 与AI聊天
  chatWithAI() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat?walletId=' + this.data.walletId
    })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  },

  // 格式化时间
  formatTime(timestamp) {
    try {
      let date
      
      // 处理不同的时间格式
      if (Array.isArray(timestamp)) {
        // 处理Java LocalDateTime数组格式 [year, month, day, hour, minute, second]
        const [year, month, day, hour, minute, second] = timestamp
        date = new Date(year, month - 1, day, hour, minute, second || 0)
      } else if (typeof timestamp === 'string') {
        // 处理字符串格式的时间，如 "2024-01-15 10:30:00"
        date = new Date(timestamp.replace(/-/g, '/')) // 兼容iOS
      } else if (typeof timestamp === 'number') {
        // 处理时间戳
        date = new Date(timestamp)
      } else {
        // 如果已经是Date对象
        date = timestamp
      }
      
      // 检查日期是否有效
      if (isNaN(date.getTime())) {

        return '时间格式错误'
      }
      
    const now = new Date()
    const diff = now - date

    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      return `${Math.floor(diff / 60000)}分钟前`
    } else if (diff < 86400000) { // 1天内
      return `${Math.floor(diff / 3600000)}小时前`
      } else if (diff < 86400000 * 2) { // 昨天
        return `昨天 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    } else {
        // 显示月日格式
        const month = date.getMonth() + 1
        const day = date.getDate()
        return `${month}月${day}日`
      }
    } catch (error) {

      return '时间解析失败'
    }
  },

  // 加载社交统计数据
  loadSocialStats() {
    const walletId = this.data.walletId
    
    if (!walletId) {
      return
    }

    // 获取钱包的社交统计数据（不是用户的）
    walletAPI.getWalletSocialStats(walletId)
      .then(result => {

        const stats = result.data || {}
        const socialStats = {
          fansCount: stats.fansCount || 0,  // 钱包粉丝数
          likesCount: stats.likesCount || 0, // 钱包获赞数
          viewsCount: stats.viewsCount || 0  // 钱包浏览数
        }

        this.setData({ socialStats })
      })
      .catch(error => {

        // API失败时使用真实的0值，不使用模拟数据
        const socialStats = {
          fansCount: 0,  // 新钱包真实数据为0
          likesCount: 0, // 新钱包真实数据为0
          viewsCount: 0  // 新钱包真实数据为0
        }
        this.setData({ socialStats })
        
        // 如果是网络错误，给用户友好提示
        if (error.message && error.message.includes('网络')) {

        }
      })
  },

  // 记录钱包浏览
  recordWalletView() {
    const userId = app.globalData.userInfo?.id
    const walletId = this.data.walletId
    
    if (!userId || !walletId) {
      return
    }

    // 记录浏览（不阻塞界面，静默执行）
    walletAPI.recordWalletView(userId, walletId)
      .then(() => {

      })
      .catch(error => {

      })
  },

  // 检查关注状态
  checkFollowStatus() {
    const currentUserId = app.globalData.userInfo?.id
    const walletId = this.data.walletId
    
    if (!currentUserId || !walletId) {
      this.setData({ isFollowing: false })
      return
    }

    // 先获取钱包所有者ID
    walletAPI.getWalletOwnerId(walletId)
      .then(result => {
        const walletOwnerId = result.data

        if (!walletOwnerId || walletOwnerId === currentUserId) {
          // 如果是自己的钱包

          this.setData({ 
            isFollowing: false,
            isOwnWallet: true
          })
          return
        }
        
        // 设置为别人的钱包

        this.setData({ isOwnWallet: false })
        
        // 检查是否已关注该用户

        return walletAPI.checkFollowStatus(currentUserId, walletOwnerId)
      })
      .then(result => {
        if (result) {
          const isFollowing = result.data || false

          this.setData({ isFollowing })

        }
      })
      .catch(error => {

        this.setData({ isFollowing: false })
      })
  },

  // 切换关注状态
  toggleFollow() {
    if (this.data.isOwnWallet) {
      return // 不能关注自己
    }

    const currentUserId = app.globalData.userInfo?.id
    const walletUserId = this.data.wallet.userId || this.data.wallet.user_id

    if (!currentUserId || !walletUserId) {
      wx.showToast({
        title: '操作失败',
        icon: 'none'
      })
      return
    }

    const isFollowing = this.data.isFollowing
    
    // 先更新UI状态，提供即时反馈
    const socialStats = { ...this.data.socialStats }
    socialStats.fansCount += isFollowing ? -1 : 1
    
    this.setData({
      isFollowing: !isFollowing,
      socialStats
    })

    const apiCall = isFollowing ? 
      walletAPI.unfollowWallet(currentUserId, this.data.walletId) :
      walletAPI.followWallet(currentUserId, this.data.walletId)

    apiCall
      .then(result => {
        wx.showToast({
          title: isFollowing ? '取消关注' : '关注成功',
          icon: 'success'
        })
        
        // 通知用户社交主页刷新关注列表
        this.notifyUserSocialPageRefresh()
      })
      .catch(error => {

        // 如果API失败，回滚UI状态
        const revertStats = { ...this.data.socialStats }
        revertStats.fansCount += isFollowing ? 1 : -1
        
        this.setData({
          isFollowing: isFollowing,
          socialStats: revertStats
        })
        
        // 如果是404错误，说明API接口不存在，使用模拟模式
        if (error.message && error.message.includes('404')) {
          wx.showToast({
            title: isFollowing ? '取消关注' : '关注成功',
            icon: 'success'
          })

          // 即使是模拟模式，也要通知用户社交主页刷新
          this.notifyUserSocialPageRefresh()
        } else {
          wx.showToast({
            title: error.message || '操作失败',
            icon: 'none'
          })
        }
      })
  },

  // 通知用户社交主页刷新关注列表
  notifyUserSocialPageRefresh() {
    const pages = getCurrentPages()
    
    // 查找用户社交主页并刷新数据
    const userSocialPage = pages.find(page => 
      (page.route && page.route.includes('user-social-profile')) || 
      (page.__route__ && page.__route__.includes('user-social-profile'))
    )
    
    if (userSocialPage && userSocialPage.loadUserData) {

      const userId = app.globalData.userInfo?.id
      if (userId) {
        userSocialPage.loadUserData(userId)
      }
    }
    
    // 如果当前关注操作成功，模拟添加到关注列表
    if (!this.data.isFollowing) { // 刚刚关注了
      const newFollowedWallet = {
        id: this.data.walletId,
        title: this.data.wallet.name || '未命名钱包',
        amount: parseFloat(this.data.wallet.balance || 0).toFixed(2),
        participantCount: 1,
        backgroundStyle: this.data.walletBackgroundStyle
      }
      
      // 将关注的钱包信息存储到本地，供用户社交主页使用
      const followedWallets = wx.getStorageSync('followedWallets') || []
      const existingIndex = followedWallets.findIndex(w => w.id === this.data.walletId)
      
      if (existingIndex === -1) {
        followedWallets.push(newFollowedWallet)
        wx.setStorageSync('followedWallets', followedWallets)

      }
    } else { // 刚刚取消关注了
      const followedWallets = wx.getStorageSync('followedWallets') || []
      const filteredWallets = followedWallets.filter(w => w.id !== this.data.walletId)
      wx.setStorageSync('followedWallets', filteredWallets)

    }
  },

  // 关注钱包（只读模式下使用）- 保持向后兼容
  followWallet() {
    this.toggleFollow()
  },

  // AI语音播放功能
  playAiVoice(e) {
    // 最基础的调试日志 - 确保事件能触发

    // 检查事件对象
    if (!e || !e.currentTarget) {

      return
    }
    
    // 检查数据绑定
    const dataset = e.currentTarget.dataset

    if (!dataset || !dataset.transaction) {

      return
    }
    
    const transaction = dataset.transaction
    const transactionId = transaction.id

    // 强制清理任何现有的音频状态，避免状态不一致
    if (this.data.currentPlayingVoice || this.data.voiceContext) {

      this.forceResetAudioState()
      
      // 添加延迟确保清理完成
      setTimeout(() => {

        this.startVoicePlayback(transaction)
      }, 200)
      return
    }
    
    // 直接开始播放
    this.startVoicePlayback(transaction)
  },
  
  // 开始语音播放的核心逻辑
  startVoicePlayback(transaction) {
    const transactionId = transaction.id

    // 检查是否有语音URL
    if (!transaction.voiceUrl || transaction.voiceUrl === 'undefined' || transaction.voiceUrl === 'null' || transaction.voiceUrl.startsWith('mock_voice_')) {

      this.simulateVoicePlay(transaction)
      return
    }
    
    // 检查URL是否有效
    if (typeof transaction.voiceUrl !== 'string' || transaction.voiceUrl.trim() === '') {

      this.simulateVoicePlay(transaction)
      return
    }
    
    // 强制清理任何残留的音频上下文
    if (this.data.voiceContext) {

      try {
        this.data.voiceContext.destroy()
      } catch (error) {

      }
    }
    
    // 创建全新的音频上下文
    const voiceContext = wx.createInnerAudioContext()

    // 设置音频属性
    voiceContext.src = transaction.voiceUrl
    voiceContext.autoplay = false
    voiceContext.loop = false
    voiceContext.volume = 1.0
    voiceContext.playbackRate = 1.0

    // 先更新状态，再设置事件监听
    this.setData({
      currentPlayingVoice: transactionId,
      voiceContext: voiceContext
    })
    
    this.updateTransactionPlayingState(transactionId, true)
    
    // 设置事件监听
    voiceContext.onCanplay(() => {

      if (this.data.currentPlayingVoice === transactionId) {

        voiceContext.play()
      } else {

      }
    })
    
    voiceContext.onPlay(() => {

      wx.showToast({
        title: '语音播放中...',
        icon: 'none',
        duration: 1000
      })
    })
    
    voiceContext.onEnded(() => {

      this.cleanupVoiceContext(transactionId)
      wx.showToast({
        title: '播放完成',
        icon: 'success',
        duration: 1000
      })
    })
    
    voiceContext.onError((error) => {

      wx.showModal({
        title: '语音播放失败',
        content: `错误信息: ${error.errMsg || '未知错误'}\n错误代码: ${error.errCode || 'N/A'}\n\n可能原因:\n1. 网络连接问题\n2. 音频文件损坏\n3. 音频上下文冲突\n\n建议重新点击播放`,
        showCancel: false,
        confirmText: '确定'
      })
      
      this.cleanupVoiceContext(transactionId)
    })
    
    voiceContext.onWaiting(() => {

    })
    
    voiceContext.onStop(() => {

    })
    
    // 设置超时保护
    const timeoutId = setTimeout(() => {
      if (this.data.currentPlayingVoice === transactionId) {

        this.cleanupVoiceContext(transactionId)
        wx.showToast({
          title: '播放超时，请重试',
          icon: 'none'
        })
      }
    }, 15000) // 15秒超时
    
    // 将超时ID保存到音频上下文，以便清理时取消
    voiceContext._timeoutId = timeoutId

  },
  
  // 停止当前播放的语音
  stopCurrentVoice() {

    const currentId = this.data.currentPlayingVoice
    
    if (this.data.voiceContext) {
      try {
        // 清除超时定时器
        if (this.data.voiceContext._timeoutId) {
          clearTimeout(this.data.voiceContext._timeoutId)

        }
        
        // 停止并销毁音频上下文
        this.data.voiceContext.stop()
        this.data.voiceContext.destroy()

      } catch (error) {

      }
    }
    
    // 重置状态
    if (currentId) {
      this.updateTransactionPlayingState(currentId, false)
    }
    
    this.setData({
      currentPlayingVoice: null,
      voiceContext: null
    })

  },
  
  // 清理音频上下文的统一方法
  cleanupVoiceContext(transactionId) {

    // 只清理指定的音频上下文，避免清理错误的上下文
    if (this.data.currentPlayingVoice === transactionId && this.data.voiceContext) {
      try {
        // 清除超时定时器
        if (this.data.voiceContext._timeoutId) {
          clearTimeout(this.data.voiceContext._timeoutId)

        }
        
        // 销毁音频上下文
        this.data.voiceContext.destroy()

      } catch (error) {

      }
      
      // 重置状态
      this.setData({
        currentPlayingVoice: null,
        voiceContext: null
      })
    }
    
    // 更新UI状态
    this.updateTransactionPlayingState(transactionId, false)

  },
  
  // 更新交易的播放状态
  updateTransactionPlayingState(transactionId, isPlaying) {
    const transactions = this.data.transactions
    const index = transactions.findIndex(t => t.id === transactionId)
    
    if (index !== -1 && transactions[index].isPlaying !== isPlaying) {
      // 只有状态真正改变时才更新，避免不必要的重渲染
      transactions[index].isPlaying = isPlaying
      
      // 使用精确的数据路径更新，减少页面闪烁
      this.setData({
        [`transactions[${index}].isPlaying`]: isPlaying
      })

    }
  },
  
  // 生成AI语音（模拟功能）
  generateAiVoice(transaction) {
    wx.showToast({
      title: '正在生成语音...',
      icon: 'loading',
      duration: 1000
    })
    
    // 模拟语音生成过程
    setTimeout(() => {
      // 模拟语音播放
      this.simulateVoicePlay(transaction)
    }, 1000)
  },
  
  // 模拟语音播放
  simulateVoicePlay(transaction) {
    const transactionId = transaction.id

    // 更新UI显示正在播放
    this.updateTransactionPlayingState(transactionId, true)
    this.setData({ currentPlayingVoice: transactionId })
    
    // 显示友好的提示信息
    wx.showToast({
      title: '正在朗读AI消息...',
      icon: 'none',
      duration: 1500
    })
    
    // 根据消息长度计算模拟播放时长
    const messageLength = transaction.aiMessage ? transaction.aiMessage.length : 20
    const estimatedDuration = Math.max(3000, Math.min(messageLength * 200, 15000)) // 3-15秒

    // 模拟播放过程
    setTimeout(() => {
      if (this.data.currentPlayingVoice === transactionId) {
        this.updateTransactionPlayingState(transactionId, false)
        this.setData({ currentPlayingVoice: null })

        wx.showToast({
          title: '播放完成',
          icon: 'success',
          duration: 800
        })
      }
    }, estimatedDuration)
  },

  // 页面卸载时清理语音资源
  onUnload() {

    this.stopCurrentVoice()
  },

  // 页面隐藏时暂停语音
  onHide() {

    if (this.data.voiceContext && this.data.currentPlayingVoice) {
      try {
        this.data.voiceContext.pause()

      } catch (error) {

      }
    }
  },

  // 重置音频播放状态
  resetAudioState() {

    // 如果有音频上下文，清理它
    if (this.data.voiceContext) {
      try {
        // 清除超时定时器
        if (this.data.voiceContext._timeoutId) {
          clearTimeout(this.data.voiceContext._timeoutId)
        }
        this.data.voiceContext.destroy()

      } catch (error) {

      }
    }
    
    // 重置所有交易的播放状态
    const transactions = this.data.transactions || []
    let hasPlayingTransaction = false
    
    transactions.forEach((transaction, index) => {
      if (transaction.isPlaying) {
        hasPlayingTransaction = true
        this.setData({
          [`transactions[${index}].isPlaying`]: false
        })
      }
    })
    
    if (hasPlayingTransaction) {

    }
    
    // 重置全局状态
    this.setData({
      currentPlayingVoice: null,
      voiceContext: null
    })

  },
  
  // 返回上一页
  goBack() {
    // 清理音频资源
    this.stopCurrentVoice()
    
    wx.navigateBack({
      delta: 1
    })
  },

  // 强制重置音频状态
  forceResetAudioState() {

    // 强制清理所有音频资源
    if (this.data.voiceContext) {
      try {
        if (this.data.voiceContext._timeoutId) {
          clearTimeout(this.data.voiceContext._timeoutId)
        }
        this.data.voiceContext.stop()
        this.data.voiceContext.destroy()
      } catch (error) {

      }
    }
    
    // 重置所有状态
    this.setData({
      currentPlayingVoice: null,
      voiceContext: null
    })
    
    // 重置所有交易的播放状态
    const transactions = this.data.transactions || []
    transactions.forEach((transaction, index) => {
      if (transaction.isPlaying) {
        this.setData({
          [`transactions[${index}].isPlaying`]: false
        })
      }
    })

  },

  // 返回钱包列表页面
  goBackToWalletList() {
    // 如果是自己的钱包，返回主页
    if (this.data.isOwnWallet) {
      wx.switchTab({
        url: '/pages/home/home'
      })
    } else {
      // 如果是从社交圈来的，返回社交圈
      if (this.data.fromSocial) {
        wx.navigateTo({
          url: '/pages/social/social'
        })
      } else {
        // 否则返回上一页
        wx.navigateBack({
          delta: 1
        })
      }
    }
  },

  // 加载月度统计数据
  loadMonthlyStats() {
    const { walletId, currentYear, currentMonth } = this.data
    
    if (!walletId) {

      return
    }

    this.setData({ statsLoading: true })

    walletAPI.getWalletMonthlyStats(walletId, currentYear, currentMonth)
      .then(result => {

        const stats = result.data || {}
        
        // 格式化数据，确保显示两位小数
        const formattedStats = {
          monthlyIncome: parseFloat(stats.monthlyIncome || 0).toFixed(2),
          monthlyExpense: parseFloat(stats.monthlyExpense || 0).toFixed(2),
          monthlyProfit: parseFloat(stats.monthlyProfit || 0).toFixed(2),
          transactionCount: stats.transactionCount || 0
        }

        this.setData({
          monthlyStats: formattedStats,
          statsLoading: false
        })
      })
      .catch(error => {

        this.setData({ 
          statsLoading: false,
          monthlyStats: {
            monthlyIncome: '0.00',
            monthlyExpense: '0.00',
            monthlyProfit: '0.00',
            transactionCount: 0
          }
        })
        wx.showToast({
          title: '统计数据加载失败',
          icon: 'none'
        })
      })
  },

  // 月份选择器变化事件
  onMonthChange(e) {
    const selectedDate = e.detail.value // 格式: YYYY-MM

    const [year, month] = selectedDate.split('-')
    const selectedYear = parseInt(year)
    const selectedMonth = parseInt(month)

    this.setData({
      currentYear: selectedYear,
      currentMonth: selectedMonth,
      currentDate: selectedDate
    })

    // 加载新选择月份的统计数据
    this.loadMonthlyStats()
  },

  // 显示钱包设置
  showWalletSettings() {
    this.setData({
      showWalletSettings: true
    })
  },

  // 隐藏钱包设置
  hideWalletSettings() {
    this.setData({
      showWalletSettings: false
    })
  },

  // 切换钱包公开/私密状态
  toggleWalletPublic(e) {
    const isPublic = e.detail.value ? 1 : 0
    const walletId = this.data.walletId

    // 先更新UI状态
    const wallet = { ...this.data.wallet }
    wallet.is_public = isPublic
    this.setData({ wallet })

    // 调用API更新后端状态
    walletAPI.setWalletPublic(walletId, isPublic)
      .then(result => {

        wx.showToast({
          title: isPublic ? '已设为公开' : '已设为私密',
          icon: 'success'
        })
      })
      .catch(error => {

        // 恢复原状态
        wallet.is_public = isPublic ? 0 : 1
        this.setData({ wallet })
        
        wx.showToast({
          title: '设置失败，请重试',
          icon: 'none'
        })
      })
  },

  // 播放评论语音
  playCommentVoice(e) {
    const comment = e.currentTarget.dataset.comment

    // 如果正在播放其他语音，先停止
    if (this.data.currentPlayingVoice) {
      this.stopCurrentVoice()
    }
    
    // 如果当前评论正在播放，则停止
    if (comment.isPlayingVoice) {
      this.stopCommentVoice(comment)
      return
    }
    
    // 检查语音URL
    if (!comment.voiceUrl || comment.voiceUrl === '' || comment.voiceUrl === 'null') {

      this.simulateCommentVoicePlay(comment)
      return
    }
    
    // 播放真实评论语音
    this.playRealCommentVoice(comment)
  },

  // 停止评论语音
  stopCommentVoice(comment) {
    // 找到并更新评论的播放状态
    const transactions = this.data.transactions
    transactions.forEach((transaction, tIndex) => {
      if (transaction.comments) {
        transaction.comments.forEach((c, cIndex) => {
          if (c.id === comment.id) {
            this.setData({
              [`transactions[${tIndex}].comments[${cIndex}].isPlayingVoice`]: false
            })
          }
        })
      }
    })
    
    // 停止音频播放
    if (this.data.voiceContext) {
      try {
        this.data.voiceContext.stop()
        this.data.voiceContext.destroy()
      } catch (error) {

      }
    }
    
    this.setData({
      currentPlayingVoice: null,
      voiceContext: null
    })
  },

  // 播放真实评论语音
  playRealCommentVoice(comment) {
    try {
      // 创建音频上下文
      const voiceContext = wx.createInnerAudioContext()
      voiceContext.src = comment.voiceUrl
      voiceContext.autoplay = true
      
      // 更新评论播放状态
      this.updateCommentPlayingState(comment.id, true)
      this.setData({ 
        currentPlayingVoice: `comment_${comment.id}`,
        voiceContext: voiceContext 
      })

      voiceContext.onPlay(() => {

        wx.showToast({
          title: '正在播放评论语音',
          icon: 'none',
          duration: 1000
        })
      })
      
      voiceContext.onEnded(() => {

        this.updateCommentPlayingState(comment.id, false)
        this.setData({ currentPlayingVoice: null, voiceContext: null })
      })
      
      voiceContext.onError((error) => {

        this.updateCommentPlayingState(comment.id, false)
        this.setData({ currentPlayingVoice: null, voiceContext: null })
        wx.showToast({
          title: '语音播放失败',
          icon: 'error'
        })
      })
      
    } catch (error) {

      wx.showToast({
        title: '播放失败',
        icon: 'error'
      })
    }
  },

  // 模拟评论语音播放
  simulateCommentVoicePlay(comment) {

    // 更新UI显示正在播放
    this.updateCommentPlayingState(comment.id, true)
    this.setData({ currentPlayingVoice: `comment_${comment.id}` })
    
    // 显示友好的提示信息
    wx.showToast({
      title: '正在朗读AI评论...',
      icon: 'none',
      duration: 1500
    })
    
    // 根据评论长度计算模拟播放时长
    const commentLength = comment.content ? comment.content.length : 20
    const estimatedDuration = Math.max(2000, Math.min(commentLength * 150, 10000)) // 2-10秒

    // 模拟播放过程
    setTimeout(() => {
      if (this.data.currentPlayingVoice === `comment_${comment.id}`) {
        this.updateCommentPlayingState(comment.id, false)
        this.setData({ currentPlayingVoice: null })

        wx.showToast({
          title: '播放完成',
          icon: 'success',
          duration: 800
        })
      }
    }, estimatedDuration)
  },

  // 更新评论播放状态
  updateCommentPlayingState(commentId, isPlaying) {
    const transactions = this.data.transactions
    let updated = false
    
    transactions.forEach((transaction, tIndex) => {
      if (transaction.comments) {
        transaction.comments.forEach((comment, cIndex) => {
          if (comment.id === commentId) {
            this.setData({
              [`transactions[${tIndex}].comments[${cIndex}].isPlayingVoice`]: isPlaying
            })
            updated = true
          }
        })
      }
    })
    
    if (updated) {

    }
  },

  // 加载用户剧本进度
  loadUserScriptProgress() {
    const userId = app.globalData.userInfo?.id
    const walletId = this.data.walletId
    const scriptId = 3 // "重新养小时候的自己"剧本ID

    if (!userId || !walletId) {

      return
    }

    // 先获取剧本信息
    wx.request({
      url: `${app.globalData.baseUrl}/scripts/${scriptId}`,
      method: 'GET',
      success: (res) => {

        if (res.data && res.data.code === 200 && res.data.data) {
          this.setData({
            scriptInfo: res.data.data
          })
        }
      },
      fail: (error) => {

      }
    })

    // 调用后端API获取用户剧本进度
    wx.request({
      url: `${app.globalData.baseUrl}/scripts/progress`,
      method: 'GET',
      data: {
        userId: userId,
        scriptId: scriptId,
        walletId: walletId
      },
      success: (res) => {

        if (res.data && res.data.code === 200 && res.data.data) {
          const progressData = res.data.data
          this.setData({
            userScriptProgress: progressData
          })
          
          // 获取当前章节的标题信息
          this.loadCurrentChapterTitle(scriptId, progressData.currentChapter || 1)
        } else {
          // 如果没有进度记录，设置默认值
          const defaultProgress = {
            currentChapter: 1,
            status: 1
          }
          this.setData({
            userScriptProgress: defaultProgress
          })
          
          // 获取第1集的标题信息
          this.loadCurrentChapterTitle(scriptId, 1)
        }
      },
      fail: (error) => {

        // 设置默认值
        const defaultProgress = {
          currentChapter: 1,
          status: 1
        }
        this.setData({
          userScriptProgress: defaultProgress
        })
        
        // 获取第1集的标题信息
        this.loadCurrentChapterTitle(scriptId, 1)
      }
    })
  },

  // 加载当前章节标题
  async loadCurrentChapterTitle(scriptId, chapterNumber) {
    const userId = app.globalData.userInfo?.id
    const walletId = this.data.walletId
    
    if (!userId || !walletId) {
      return
    }

    try {
      // 引入scriptAPI
      const { scriptAPI } = require('../../utils/api.js')
      
      // 使用正确的API方法获取章节内容
      const response = await scriptAPI.getChapterContent(scriptId, chapterNumber, userId, walletId)
      
      if (response.code === 200 && response.data) {
        const chapterData = response.data.chapter || response.data
        this.setData({
          currentChapterTitle: chapterData.title || ''
        })
      } else {
        // 如果获取失败，使用默认标题
        this.setData({
          currentChapterTitle: ''
        })
      }
    } catch (error) {
      console.error('获取章节标题失败:', error)
      // 如果获取失败，使用默认标题
      this.setData({
        currentChapterTitle: ''
      })
    }
  },

  // 跳转到剧本当前集
  goToScriptChapter() {
    // 先跳转到剧本列表页面，然后由剧本列表页面处理具体的剧本跳转
    wx.navigateTo({
      url: `/pages/script-detail/script-detail?walletId=${this.data.walletId}&autoPlay=3`
    })
  }

})