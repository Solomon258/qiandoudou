// pages/dream-detail/dream-detail.js
const app = getApp()
const { walletAPI, dreamImageAPI } = require('../../utils/api.js')

Page({
  /**
   * 页面的初始数据
   */
  data: {
    isFixedTop: false,
    lastScrollTop: 0,
    // 加载状态
    loading: true,
    loadingRecords: false,

    // 限时挑战弹窗
    showChallengeModal: false,
    countdown: '1 天 01:08:03',
    countdownTimer: null,

    // 梦想钱包信息
    dreamWallet: {
      id: '',
      name: '',
      targetAmount: 0,
      currentAmount: 0,
      dreamType: 1, // 1: 购物, 2: 旅行
      dreamItinerary: '', // 旅行行程（仅旅行类型）
      itemInfo: {
        id: '',
        name: '',
        imageUrl: '',
        price: 0
      }
    },
    
    // 进度计算
    progress: {
      percentage: 0,
      stage: 0, // 当前阶段 0-5
      completed: false
    },
    
    // 进度徽章配置
    progressBadges: [
      {
        position: 20,
        completed: false,
        activeIcon: '/static/icon/car/right.png',
        inactiveIcon: '/static/icon/car/blakc_right.png',
        achieveDate: null // 达成日期
      },
      {
        position: 40,
        completed: false,
        activeIcon: '/static/icon/car/right.png',
        inactiveIcon: '/static/icon/car/blakc_right.png',
        achieveDate: null
      },
      {
        position: 60,
        completed: false,
        activeIcon: '/static/icon/car/gift.png',
        inactiveIcon: '/static/icon/car/black_gift.png',
        achieveDate: null
      },
      {
        position: 80,
        completed: false,
        activeIcon: '/static/icon/car/right.png',
        inactiveIcon: '/static/icon/car/blakc_right.png',
        achieveDate: null
      },
      {
        position: 100,
        completed: false,
        activeIcon: '/static/icon/car/why_gift.png',
        inactiveIcon: '/static/icon/car/balck_why_gift.png',
        achieveDate: null
      }
    ],
    
    // 存入记录
    records: [],
    hasMoreRecords: true,
    currentPage: 1,
    pageSize: 20
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    const { walletId } = options
    if (!walletId) {
      wx.showToast({
        title: '参数错误',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }
    
    this.setData({ 'dreamWallet.id': walletId })
    this.loadDreamWalletDetail(walletId)
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 页面显示时刷新数据（只有在不是首次加载时才刷新）
    console.log('onShow - 准备刷新数据')
    console.log('- dreamWallet.id:', this.data.dreamWallet.id)
    console.log('- loading:', this.data.loading)
    
    if (this.data.dreamWallet.id && !this.data.loading) {
      console.log('开始刷新梦想钱包数据')
      this.loadDreamWalletDetail(this.data.dreamWallet.id)
    }
  },

  /**
   * 加载梦想钱包详情
   */
  async loadDreamWalletDetail(walletId) {
    try {
      this.setData({ loading: true })
      
      console.log('开始加载梦想钱包详情，钱包ID:', walletId)
      
      // 调用API获取梦想钱包详情
      let result
      try {
        result = await walletAPI.getDreamWalletDetail(walletId)
        console.log('梦想钱包详情API返回:', result)
      } catch (error) {
        console.log('梦想钱包详情API失败，尝试使用通用钱包接口:', error.message)
        // 如果梦想钱包详情接口失败，使用通用钱包详情接口
        result = await walletAPI.getWalletDetail(walletId)
        console.log('通用钱包详情API返回:', result)
      }
      
      if (result && result.data) {
        const responseData = result.data
        console.log('API返回的完整数据:', responseData)
        console.log('responseData的所有字段:', Object.keys(responseData))
        
        // 梦想钱包API返回的数据结构：{wallet: {...}, currentProgress: 0, ...}
        const walletData = responseData.wallet || responseData
        console.log('提取的钱包数据:', walletData)
        
        const walletId = walletData.id
        console.log('提取的钱包ID:', walletId)
        
        const targetAmount = walletData.dreamTargetAmount || walletData.targetAmount || responseData.targetAmount || 0
        // 尝试从更多字段获取当前金额
        const currentAmount = walletData.balance || 
                             walletData.currentAmount || 
                             responseData.currentAmount || 
                             responseData.currentBalance ||
                             responseData.balance ||
                             (responseData.targetAmount - responseData.remainingAmount) || // 目标金额 - 剩余金额
                             0
        
        console.log('数据提取调试:')
        console.log('- walletData.dreamTargetAmount:', walletData.dreamTargetAmount)
        console.log('- walletData.targetAmount:', walletData.targetAmount)
        console.log('- responseData.targetAmount:', responseData.targetAmount)
        console.log('- 最终targetAmount:', targetAmount)
        console.log('- walletData.balance:', walletData.balance)
        console.log('- walletData.currentAmount:', walletData.currentAmount)
        console.log('- responseData.currentAmount:', responseData.currentAmount)
        console.log('- responseData.currentBalance:', responseData.currentBalance)
        console.log('- responseData.balance:', responseData.balance)
        console.log('- responseData.remainingAmount:', responseData.remainingAmount)
        console.log('- 计算的当前金额 (目标-剩余):', responseData.targetAmount - responseData.remainingAmount)
        console.log('- 最终currentAmount:', currentAmount)
        
        // 根据梦想类型选择图片
        const dreamType = walletData.dreamType || 1
        const dreamProgress = responseData.currentProgress || walletData.dreamProgress || 0
        let imageUrl

        if (dreamType === 2) {
          // 旅行类型：从后端获取图片或使用降级方案
          imageUrl = responseData.currentStageImage || walletData.dreamItemImage

          // 如果都没有，尝试异步加载
          if (!imageUrl) {
            try {
              imageUrl = await this.getCarImageByProgress(dreamProgress)
            } catch (error) {
              console.error('加载旅行图片失败:', error)
              imageUrl = '/static/icon/default-travel.png'  // 默认图片
            }
          }
        } else {
          // 购物类型：优先从后端获取，然后尝试异步加载
          imageUrl = responseData.currentStageImage || walletData.dreamItemImage

          // 如果都没有，异步加载最新的进度图片
          if (!imageUrl) {
            try {
              imageUrl = await this.getCarImageByProgress(dreamProgress)
            } catch (error) {
              console.error('加载购物图片失败:', error)
              imageUrl = '/static/icon/default-shopping.png'  // 默认图片
            }
          }
        }

        // 格式化行程文本（转换Markdown为HTML）
        let itinerary = walletData.dreamItinerary || 'AI正在为您生成详细的旅行行程，请稍后刷新查看...'

        // 提取主标题（# 开头的一级标题）
        let itineraryTitle = ''
        const titleMatch = itinerary.match(/^#\s+(.+?)$/m)
        if (titleMatch) {
          itineraryTitle = titleMatch[1]
          // 从原文中移除标题行
          itinerary = itinerary.replace(/^#\s+.+?$\n?/m, '')
        }

        let itineraryHtml = this.markdownToHtml(itinerary)

        this.setData({
          dreamWallet: {
            id: walletId,
            name: walletData.name,
            targetAmount: targetAmount,
            currentAmount: currentAmount,
            formattedTargetAmount: this.formatAmount(targetAmount),
            formattedCurrentAmount: this.formatAmount(currentAmount),
            dreamType: dreamType,
            dreamItemName: walletData.dreamItemName || '',
            dreamDestination: walletData.dreamDestination || '',
            dreamItinerary: itinerary,
            dreamItineraryTitle: itineraryTitle,
            dreamItineraryHtml: itineraryHtml,
            itemInfo: {
              id: walletData.itemId || responseData.itemId || '',
              name: walletData.dreamItemName || walletData.itemName || responseData.itemName || '梦想商品',
              imageUrl: imageUrl,
              price: targetAmount
            }
          }
        }, async () => {
          // 在setData完成后执行
          console.log('setData完成，准备加载存入记录，walletId:', walletId)
          console.log('setData完成后，this.data.dreamWallet.id:', this.data.dreamWallet.id)
          console.log('setData完成后，this.data.dreamWallet:', this.data.dreamWallet)

          // 计算进度
          console.log('准备计算进度...')
          await this.calculateProgress()

          // 加载存入记录 - 使用确定的walletId
          this.loadDepositRecords(1, true, walletId)
        })
      }
    } catch (error) {
      console.error('加载梦想钱包详情失败:', error)
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
      wx.stopPullDownRefresh()
    }
  },

  /**
   * 计算进度和更新徽章
   */
  async calculateProgress() {
    const { currentAmount, targetAmount } = this.data.dreamWallet

    console.log('calculateProgress 调试信息:')
    console.log('- currentAmount:', currentAmount, typeof currentAmount)
    console.log('- targetAmount:', targetAmount, typeof targetAmount)

    if (targetAmount <= 0) {
      console.log('目标金额为0或负数，跳过进度计算')
      return
    }

    const percentage = Math.min((parseFloat(currentAmount) / parseFloat(targetAmount)) * 100, 100)
    const completed = percentage >= 100

    console.log('- 计算的进度百分比:', percentage)

    // 计算当前阶段 (0-5)
    let stage = 0
    if (percentage >= 80) stage = 4
    else if (percentage >= 60) stage = 3
    else if (percentage >= 40) stage = 2
    else if (percentage >= 20) stage = 1
    else stage = 0

    const finalPercentage = Math.round(percentage * 100) / 100

    console.log('- 最终进度百分比:', finalPercentage)
    console.log('- 阶段:', stage)

    // 构建更新对象
    const updateData = {
      progress: {
        percentage: finalPercentage,
        stage: stage,
        completed: completed
      }
    }

    // 只有购物类型和旅行类型都需要根据进度更新图片
    if (this.data.dreamWallet.dreamType === 1 || this.data.dreamWallet.dreamType === 2) {
      try {
        const imageUrl = await this.getCarImageByProgress(finalPercentage)
        if (imageUrl) {
          updateData['dreamWallet.itemInfo.imageUrl'] = imageUrl
          console.log('进度图片已更新:', imageUrl)
        }
      } catch (error) {
        console.error('更新进度图片失败:', error)
      }
    }

    this.setData(updateData)

    console.log('进度更新完成，当前progress:', this.data.progress)

    // 强制触发页面更新
    setTimeout(() => {
      console.log('延迟检查进度数据:', this.data.progress)
    }, 100)

    // 更新徽章状态
    this.updateProgressBadges(percentage)
  },

  /**
   * 更新进度徽章状态
   */
  updateProgressBadges(percentage) {
    const walletId = this.data.dreamWallet.id
    const storageKey = `badge_dates_${walletId}`

    // 从缓存中读取已点亮徽章的日期
    let savedBadgeDates = {}
    try {
      const savedData = wx.getStorageSync(storageKey)
      if (savedData) {
        savedBadgeDates = JSON.parse(savedData)
      }
    } catch (error) {
      console.error('读取徽章日期失败:', error)
    }

    // 记录新点亮的徽章
    const newlyUnlockedBadges = []
    const currentDate = this.formatDateForBadge(new Date())

    const badges = this.data.progressBadges.map((badge, index) => {
      const requiredProgress = (index + 1) * 20 // 20%, 40%, 60%, 80%, 100%
      const isCompleted = percentage >= requiredProgress
      const wasCompleted = badge.completed

      // 如果徽章首次点亮
      if (isCompleted && !wasCompleted && !savedBadgeDates[index]) {
        savedBadgeDates[index] = currentDate
        newlyUnlockedBadges.push({
          index: index,
          progress: requiredProgress,
          date: currentDate
        })
      }

      return {
        ...badge,
        completed: isCompleted,
        achieveDate: savedBadgeDates[index] || null
      }
    })

    // 保存徽章日期到缓存
    try {
      wx.setStorageSync(storageKey, JSON.stringify(savedBadgeDates))
    } catch (error) {
      console.error('保存徽章日期失败:', error)
    }

    this.setData({ progressBadges: badges })

    // 如果有新点亮的徽章，自动打开分享页面（只打开第一个）
    if (newlyUnlockedBadges.length > 0) {
      const firstUnlocked = newlyUnlockedBadges[0]
      setTimeout(() => {
        this.openBadgeShare(firstUnlocked.index, firstUnlocked.progress, firstUnlocked.date)
      }, 500)
    }
  },

  /**
   * 徽章点击事件
   */
  onBadgeTap(e) {
    const { index, badge } = e.currentTarget.dataset

    // 只有已点亮的徽章才能点击查看
    if (!badge.completed) {
      wx.showToast({
        title: '该徽章尚未解锁',
        icon: 'none',
        duration: 2000
      })
      return
    }

    // 打开分享页面
    const progress = (parseInt(index) + 1) * 20
    const achieveDate = badge.achieveDate || this.formatDateForBadge(new Date())
    this.openBadgeShare(index, progress, achieveDate)
  },

  /**
   * 打开徽章分享页面
   */
  openBadgeShare(badgeIndex, badgeProgress, achieveDate) {
    wx.navigateTo({
      url: `/pages/badge-share/badge-share?walletId=${this.data.dreamWallet.id}&badgeIndex=${badgeIndex}&badgeProgress=${badgeProgress}&achieveDate=${achieveDate}`
    })
  },

  /**
   * 格式化日期用于徽章
   */
  formatDateForBadge(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}/${month}/${day}`
  },

  /**
   * 加载存入记录
   */
  async loadDepositRecords(page = 1, refresh = false, walletId = null) {
    if (this.data.loadingRecords && !refresh) {
      return
    }
    
    // 使用传入的walletId或者从data中获取
    const targetWalletId = walletId || this.data.dreamWallet.id
    
    console.log('loadDepositRecords 调试信息:')
    console.log('- 传入的walletId:', walletId)
    console.log('- this.data.dreamWallet.id:', this.data.dreamWallet.id)
    console.log('- 最终的targetWalletId:', targetWalletId)
    console.log('- this.data.dreamWallet:', this.data.dreamWallet)
    
    if (!targetWalletId) {
      console.error('钱包ID为空，无法加载存入记录')
      return
    }
    
    try {
      this.setData({ loadingRecords: true })
      
      // 调用API获取存入记录
      const result = await walletAPI.getDreamWalletRecords(
        targetWalletId, 
        page, 
        this.data.pageSize
      )
      
      if (result && result.data) {
        const newRecords = result.data.records || []
        const hasMore = result.data.hasMore !== false
        
        // 格式化记录数据
        const formattedRecords = newRecords.map(record => {
          const isAutoDeposit = record.aiPartnerId || record.autoDeposit
          const isWithdraw = record.type === 2 || record.amount < 0
          
          let typeText = ''
          let type = 'in'
          let amountText = ''
          
          if (isWithdraw) {
            typeText = '主动为梦想支出'
            type = 'out'
            amountText = `-${Math.abs(record.amount)}`
          } else if (isAutoDeposit) {
            typeText = '自动为梦想存入'
            type = 'in'
            amountText = `+${record.amount}`
          } else {
            typeText = '主动为梦想存入'
            type = 'in'
            amountText = `+${record.amount}`
          }
          
          return {
            id: record.id,
            amount: record.amount,
            type: type,
            typeText: typeText,
            amountText: amountText,
            createTime: this.formatTime(record.createTime),
            description: record.description || typeText
          }
        })
        
        this.setData({
          records: refresh ? formattedRecords : [...this.data.records, ...formattedRecords],
          hasMoreRecords: hasMore,
          currentPage: page
        })
      }
    } catch (error) {
      console.error('加载存入记录失败:', error)
      // 如果是首次加载失败，显示空状态而不是错误提示
      if (page === 1) {
        this.setData({ records: [] })
      }
    } finally {
      this.setData({ loadingRecords: false })
    }
  },

  /**
   * 滚动到底部加载更多记录
   */
  loadMoreRecords() {
    if (this.data.hasMoreRecords && !this.data.loadingRecords) {
      this.loadDepositRecords(this.data.currentPage + 1, false)
    }
  },

  /**
   * 返回上一页
   */
  goBack() {
    wx.navigateBack()
  },

  /**
   * 预览商品图片
   */
  previewImage() {
    const imageUrl = this.data.dreamWallet.itemInfo.imageUrl
    if (imageUrl) {
      wx.previewImage({
        urls: [imageUrl],
        current: imageUrl
      })
    }
  },

  /**
   * 跳转到存入页面
   */
  goToDeposit() {
    console.log('goToDeposit 调试信息:')
    console.log('- this.data.dreamWallet:', this.data.dreamWallet)
    console.log('- this.data.dreamWallet.id:', this.data.dreamWallet.id)
    
    const walletId = this.data.dreamWallet.id
    
    if (!walletId) {
      console.error('钱包ID为空，无法跳转到存入页面')
      wx.showToast({
        title: '钱包信息异常',
        icon: 'none'
      })
      return
    }
    
    console.log('准备跳转到transfer-in页面，walletId:', walletId)
    wx.navigateTo({
      url: `/pages/transfer-in/transfer-in?walletId=${walletId}&dreamMode=true`
    })
  },

  /**
   * 根据进度获取图片
   * 优先从数据库配置加载，降级到硬编码图片
   */
  async getCarImageByProgress(progress) {
    const dreamWallet = this.data.dreamWallet
    const dreamType = dreamWallet.dreamType || 1

    console.log('getCarImageByProgress 调试信息:')
    console.log('- 进度百分比:', progress)
    console.log('- dreamType:', dreamType)
    console.log('- dreamItemName:', dreamWallet.dreamItemName)
    console.log('- dreamDestination:', dreamWallet.dreamDestination)

    // 购物梦想用itemName，旅游梦想用destination
    let itemName = null
    if (dreamType === 1) {
      itemName = dreamWallet.dreamItemName
    } else {
      itemName = dreamWallet.dreamDestination
    }

    console.log('- 最终itemName:', itemName)

    if (itemName) {
      try {
        // 尝试从后端获取进度图片列表
        console.log('准备调用API获取进度图片，dreamType=' + dreamType + ', itemName=' + itemName)
        const result = await dreamImageAPI.getProgressImages(dreamType, itemName)
        console.log('API返回结果:', result)
        if (result && result.data && result.data.length > 0) {
          const progressImages = result.data
          // 根据进度百分比选择对应的图片
          let selectedImage
          if (progress >= 80) selectedImage = progressImages[4]
          else if (progress >= 60) selectedImage = progressImages[3]
          else if (progress >= 40) selectedImage = progressImages[2]
          else if (progress >= 20) selectedImage = progressImages[1]
          else selectedImage = progressImages[0]

          console.log('选中的图片:', selectedImage)
          return selectedImage
        }
      } catch (error) {
        console.warn('获取进度图片配置失败，使用本地硬编码图片:', error)
      }
    }

    // 降级方案：使用硬编码的图片
    console.log('使用硬编码的图片')
    const carImages = [
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段1@3x.png', // 0-20%
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段2@3x.png', // 20-40%
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段3@3x.png', // 40-60%
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段4@3x.png', // 60-80%
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段5@3x.png'  // 80-100%
    ]

    let selectedImage
    if (progress >= 80) selectedImage = carImages[4]
    else if (progress >= 60) selectedImage = carImages[3]
    else if (progress >= 40) selectedImage = carImages[2]
    else if (progress >= 20) selectedImage = carImages[1]
    else selectedImage = carImages[0]

    console.log('最终返回的硬编码图片:', selectedImage)
    return selectedImage
  },

  /**
   * 格式化金额显示
   */
  formatAmount(amount) {
    if (!amount && amount !== 0) return '0'
    return amount.toLocaleString()
  },

  /**
   * 格式化时间
   */
  formatTime(timestamp) {
    if (!timestamp) return ''
    
    console.log('formatTime 输入时间戳:', timestamp, typeof timestamp)
    
    // 处理不同格式的时间戳
    let date
    if (Array.isArray(timestamp)) {
      // 数组格式：[年, 月, 日, 时, 分, 秒]
      // 注意：JavaScript的月份是从0开始的，所以需要减1
      const [year, month, day, hour, minute, second] = timestamp
      date = new Date(year, month - 1, day, hour, minute, second)
    } else if (typeof timestamp === 'string') {
      // 如果是字符串，尝试解析
      date = new Date(timestamp)
    } else if (typeof timestamp === 'number') {
      // 如果是数字，检查是否需要转换为毫秒
      if (timestamp.toString().length === 10) {
        // 10位时间戳，转换为毫秒
        date = new Date(timestamp * 1000)
      } else {
        // 13位时间戳，直接使用
        date = new Date(timestamp)
      }
    } else {
      date = new Date(timestamp)
    }
    
    console.log('formatTime 解析后的日期:', date)
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.error('无效的时间戳:', timestamp)
      return '时间格式错误'
    }
    
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    
    // 小于1分钟
    if (diff < 60 * 1000) {
      return '刚刚'
    }
    
    // 小于1小时
    if (diff < 60 * 60 * 1000) {
      return `${Math.floor(diff / (60 * 1000))}分钟前`
    }
    
    // 小于1天
    if (diff < 24 * 60 * 60 * 1000) {
      return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
    }
    
    // 小于7天
    if (diff < 7 * 24 * 60 * 60 * 1000) {
      return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
    }
    
    // 超过7天显示具体日期
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    
    if (year === now.getFullYear()) {
      return `${month}-${day}`
    } else {
      return `${year}-${month}-${day}`
    }
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadDreamWalletDetail(this.data.dreamWallet.id)
  },

  /**
   * 将Markdown文本转换为HTML
   */
  markdownToHtml(text) {
    if (!text) return ''

    let html = text

    // 去掉括号及其中的内容（如：（亲爱的...）、(xxx)等）
    html = html.replace(/[（(][^）)]*[）)]/g, '')

    // 转义HTML特殊字符
    html = html.replace(/&/g, '&amp;')
    html = html.replace(/</g, '&lt;')
    html = html.replace(/>/g, '&gt;')

    // 先处理一级标题 # - 主标题（如：西安7日深度游行程规划）
    html = html.replace(/^#\s+(.+?)$/gm, '<h1>$1</h1>')

    // 处理四级标题 #### - Day标题
    html = html.replace(/####\s*\*\*(.+?)\*\*/g, '<h4>$1</h4>')
    html = html.replace(/####\s+(.+?)$/gm, '<h4>$1</h4>')

    // 处理三级标题 ###
    html = html.replace(/###\s*\*\*(.+?)\*\*/g, '<h3>$1</h3>')
    html = html.replace(/###\s+(.+?)$/gm, '<h3>$1</h3>')

    // 处理粗体 **文本**
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

    // 处理斜体 *文本* (但不要匹配**中的*)
    html = html.replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em>$1</em>')

    // 处理列表项 - **标题**: 内容
    html = html.replace(/^[\s]*[-◆•]\s*<strong>(.+?)<\/strong>[:：]\s*(.+?)$/gm,
      '<li-item><bullet>• </bullet><label>$1:</label> $2</li-item>')

    // 处理简单列表项 - 内容
    html = html.replace(/^[\s]*[-◆•]\s+(.+?)$/gm,
      '<li-simple><bullet>• </bullet>$1</li-simple>')

    // 处理普通段落
    html = html.replace(/^(?!<[h3|h4|li])(.+?)$/gm, '<p>$1</p>')

    // 清理连续的段落标签
    html = html.replace(/<\/p>\s*<p>/g, '<br/>')

    // 替换自定义标签为带样式的div
    // h1是主标题（如：西安7日深度游行程规划）- 一级标题，最大字号
    html = html.replace(/<h1>(.+?)<\/h1>/g,
      '<div style="font-size:44rpx;font-weight:bold;color:#000;margin:0 0 24rpx 0;line-height:1.3;">$1</div>')

    // h3是三级标题
    html = html.replace(/<h3>(.+?)<\/h3>/g,
      '<div style="font-size:32rpx;font-weight:bold;color:#FF6B35;margin:24rpx 0 12rpx 0;line-height:1.4;">$1</div>')

    // h4是Day标题 - 橙色，上边距大一些实现换行
    html = html.replace(/<h4>(.+?)<\/h4>/g,
      '<div style="font-size:30rpx;font-weight:600;color:#FF6B35;margin:28rpx 0 12rpx 0;line-height:1.4;">$1</div>')

    html = html.replace(/<li-item>(.+?)<\/li-item>/g,
      '<div style="margin:8rpx 0 8rpx 16rpx;line-height:2.2;">$1</div>')

    html = html.replace(/<li-simple>(.+?)<\/li-simple>/g,
      '<div style="margin:6rpx 0 6rpx 16rpx;line-height:2.2;">$1</div>')

    html = html.replace(/<p>(.+?)<\/p>/g,
      '<div style="margin:8rpx 0;line-height:2.0;color:#666;">$1</div>')

    html = html.replace(/<strong>(.+?)<\/strong>/g,
      '<span style="font-weight:600;color:#333;">$1</span>')

    html = html.replace(/<em>(.+?)<\/em>/g,
      '<span style="font-style:italic;">$1</span>')

    html = html.replace(/<bullet>/g,
      '<span style="color:#FF6B35;">')
    html = html.replace(/<\/bullet>/g, '</span>')

    html = html.replace(/<label>(.+?)<\/label>/g,
      '<span style="font-weight:600;">$1</span>')

    // 清理多余的空行
    html = html.replace(/(<div[^>]*>[\s]*<\/div>)/g, '')

    return html
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    const { dreamWallet, progress } = this.data

    return {
      title: `我的${dreamWallet.itemInfo.name}攒钱计划已完成${progress.percentage}%`,
      path: `/pages/dream-detail/dream-detail?walletId=${dreamWallet.id}`,
      imageUrl: dreamWallet.itemInfo.imageUrl
    }
  },

  /**
   * 打开限时挑战弹窗
   */
  openChallengeModal() {
    this.setData({ showChallengeModal: true })
    this.startCountdown()
  },

  /**
   * 关闭限时挑战弹窗
   */
  closeChallengeModal() {
    this.setData({ showChallengeModal: false })
    this.stopCountdown()
  },

  /**
   * 阻止冒泡
   */
  stopPropagation() {
    // 阻止点击弹窗内容时关闭弹窗
  },

  /**
   * 阻止滚动穿透
   */
  preventTouchMove() {
    return false
  },

  /**
   * 开始倒计时
   */
  startCountdown() {
    // 设置结束时间为明天的同一时刻
    const now = new Date()
    const endTime = new Date(now.getTime() + 25 * 60 * 60 * 1000 + 8 * 60 * 1000 + 3 * 1000) // 1天1小时8分3秒后

    const updateCountdown = () => {
      const now = new Date()
      const diff = endTime - now

      if (diff <= 0) {
        this.setData({ countdown: '活动已结束' })
        this.stopCountdown()
        return
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((diff % (1000 * 60)) / 1000)

      let countdownText = ''
      if (days > 0) {
        countdownText = `${days} 天 ${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
      } else {
        countdownText = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
      }

      this.setData({ countdown: countdownText })
    }

    // 立即更新一次
    updateCountdown()

    // 每秒更新
    const timer = setInterval(updateCountdown, 1000)
    this.setData({ countdownTimer: timer })
  },

  /**
   * 停止倒计时
   */
  stopCountdown() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
      this.setData({ countdownTimer: null })
    }
  },

  /**
   * 打开分享页面
   */
  openSharePage() {
    const dreamType = this.data.dreamWallet.dreamType

    // 根据梦想类型跳转到不同的分享页面
    // dreamType === 2 表示旅行梦想攒，跳转到 dream-share
    // dreamType === 1 表示购物梦想攒，跳转到 car-share
    let sharePageUrl = ''

    if (dreamType === 2) {
      // 旅行梦想攒
      sharePageUrl = `/pages/dream-share/dream-share?walletId=${this.data.dreamWallet.id}`
    } else {
      // 购物梦想攒（默认）
      sharePageUrl = `/pages/car-share/car-share?walletId=${this.data.dreamWallet.id}`
    }

    wx.navigateTo({
      url: sharePageUrl,
      fail: (err) => {
        console.error('打开分享页面失败:', err)
        // 如果页面不存在，使用系统分享
        wx.showShareMenu({
          withShareTicket: true,
          menus: ['shareAppMessage', 'shareTimeline']
        })
      }
    })
  },

  /**
   * 跳转到剧本攒列表
   */
  goToScriptList() {
    // 跳转到剧本攒页面
    wx.navigateTo({
      url: `/pages/script-detail/script-detail?walletId=${this.data.dreamWallet.id}`,
      fail: (err) => {
        console.error('跳转到剧本攒列表失败:', err)
        wx.showToast({
          title: '跳转失败',
          icon: 'none',
          duration: 2000
        })
      }
    })
  },
  onPageScroll(e) {
    console.log(e,'用户外部滚动了1111')
    const scrollTop = e.scrollTop
    const threshod = 100 //阀值
    console.log(this.data.isFixedTop,'this.data.isFixedTopthis.data.isFixedTop')
    if(this.data.isFixedTop && ( scrollTop < 20)) {
        this.setData({
          isFixedTop: false
        })
      }
    // if(scrollTop > threshod && !this.data.isFixedTop) {
    //   this.setData({
    //     isFixedTop: true
    //   })
    // }
  },
  onScrollContent(e) {
    console.log(e,'用户文章部分滚动了')
    const scrollTop = e.detail.scrollTop
    const threshod = 30 //阀值
    const lastScrollTop = this.data.lastScrollTop
    if(scrollTop >= lastScrollTop && scrollTop > threshod) {
      this.setData({
        isFixedTop: true
      })
    }
     else if(scrollTop < lastScrollTop && ( scrollTop < 10)) {
      this.setData({
        isFixedTop: false
      })
    }
    //更新上一次滚动位置
    this.setData({
      lastScrollTop: scrollTop
    })

  }
})