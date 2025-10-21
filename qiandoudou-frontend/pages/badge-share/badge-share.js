// pages/badge-share/badge-share.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: '',
    badgeIndex: 0, // 徽章索引 0-4
    badgeProgress: 20, // 徽章对应的进度百分比
    badgeLabel: '工程图', // 徽章标签
    carImageUrl: '', // 汽车图片
    formattedAmount: '0', // 已攒金额
    daysCount: 0, // 计划天数
    achieveDate: '', // 达成日期
    walletName: '', // 钱包名称
    showShareModal: false, // 是否显示分享弹窗
    shareImageUrl: '' // 分享图片地址
  },

  onLoad(options) {
    const { walletId, badgeIndex, badgeProgress, achieveDate, shareImage } = options

    if (!walletId || badgeIndex === undefined) {
      wx.showToast({
        title: '参数错误',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }

    // 徽章标签映射
    const badgeLabels = ['工程图', '轮胎', '内饰', '外观', '完整版']
    const index = parseInt(badgeIndex)
    const progress = parseInt(badgeProgress)

    this.setData({
      walletId: walletId,
      badgeIndex: index,
      badgeProgress: progress,
      badgeLabel: badgeLabels[index] || '工程图',
      achieveDate: achieveDate || this.formatDate(new Date()),
      shareImageType: shareImage || 'default' // 新增：分享图片类型
    })

    // 加载钱包数据
    this.loadWalletData()
  },

  // 加载钱包数据
  async loadWalletData() {
    try {
      const result = await walletAPI.getDreamWalletDetail(this.data.walletId)

      if (result && result.data) {
        const responseData = result.data
        const walletData = responseData.wallet || responseData

        const targetAmount = walletData.dreamTargetAmount || walletData.targetAmount || responseData.targetAmount || 0
        const currentAmount = walletData.balance || walletData.currentAmount || responseData.currentAmount || 0

        // 获取钱包创建时间计算天数
        const createTime = walletData.createTime || walletData.createdAt
        const daysCount = this.calculateDays(createTime)

        // 获取汽车图片
        const carImageUrl = this.getCarImageByProgress(this.data.badgeProgress)

        this.setData({
          formattedAmount: this.formatAmount(currentAmount),
          daysCount: daysCount,
          carImageUrl: carImageUrl,
          walletName: walletData.name || '梦想钱包'
        })
      }
    } catch (error) {
      console.error('加载钱包数据失败:', error)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    }
  },

  // 计算天数
  calculateDays(createTime) {
    if (!createTime) return 1

    try {
      let date
      if (Array.isArray(createTime)) {
        const [year, month, day] = createTime
        date = new Date(year, month - 1, day)
      } else if (typeof createTime === 'string') {
        date = new Date(createTime)
      } else if (typeof createTime === 'number') {
        date = new Date(createTime)
      } else {
        return 1
      }

      const now = new Date()
      const diff = now.getTime() - date.getTime()
      const days = Math.floor(diff / (24 * 60 * 60 * 1000))

      return days > 0 ? days : 1
    } catch (error) {
      console.error('计算天数失败:', error)
      return 1
    }
  },

  // 根据进度获取汽车图片
  getCarImageByProgress(progress) {
    // 如果是分享图片类型，使用指定的分享图片
    if (this.data.shareImageType === 'car_share') {
      return 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/car_share_compressed.jpg'
    }

    // 默认使用阶段性汽车图片
    const carImages = [
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段1@3x.png',
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段2@3x.png',
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段3@3x.png',
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段4@3x.png',
      'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/汽车阶段5@3x.png'
    ]

    if (progress >= 80) return carImages[4]
    if (progress >= 60) return carImages[3]
    if (progress >= 40) return carImages[2]
    if (progress >= 20) return carImages[1]
    return carImages[0]
  },

  // 格式化金额
  formatAmount(amount) {
    if (!amount && amount !== 0) return '0'
    return amount.toLocaleString()
  },

  // 格式化日期
  formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}/${month}/${day}`
  },

  // 关闭页面
  closePage() {
    wx.navigateBack()
  },

  // 分享到朋友圈
  onShareToFriends() {
    wx.showToast({
      title: '请先保存图片，然后在朋友圈发布',
      icon: 'none',
      duration: 2000
    })
  },

  // 保存图片到本地
  onSaveImage() {
    const imageUrl = this.data.carImageUrl

    if (!imageUrl) {
      wx.showToast({
        title: '图片地址无效',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '保存中...'
    })

    // 先下载图片到本地
    wx.downloadFile({
      url: imageUrl,
      success: (downloadRes) => {
        if (downloadRes.statusCode === 200) {
          // 保存图片到相册
          wx.saveImageToPhotosAlbum({
            filePath: downloadRes.tempFilePath,
            success: () => {
              wx.hideLoading()
              wx.showToast({
                title: '保存成功',
                icon: 'success'
              })
            },
            fail: (saveError) => {
              wx.hideLoading()

              if (saveError.errMsg.includes('auth deny')) {
                // 权限被拒绝，引导用户授权
                wx.showModal({
                  title: '需要相册权限',
                  content: '保存图片需要访问您的相册，请在设置中开启权限',
                  confirmText: '去设置',
                  cancelText: '取消',
                  success: (modalRes) => {
                    if (modalRes.confirm) {
                      wx.openSetting({
                        success: (settingRes) => {
                          if (settingRes.authSetting['scope.writePhotosAlbum']) {
                            // 用户重新授权后，再次尝试保存
                            this.onSaveImage()
                          }
                        }
                      })
                    }
                  }
                })
              } else {
                wx.showToast({
                  title: '保存失败',
                  icon: 'none'
                })
              }
            }
          })
        } else {
          wx.hideLoading()
          wx.showToast({
            title: '下载图片失败',
            icon: 'none'
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({
          title: '下载图片失败',
          icon: 'none'
        })
      }
    })
  },

  // 分享到抖音
  onShareToDouyin() {
    wx.showToast({
      title: '请先保存图片，然后在抖音中发布',
      icon: 'none',
      duration: 2000
    })
  },

  // 分享到小红书
  onShareToXiaohongshu() {
    wx.showToast({
      title: '请先保存图片，然后在小红书中发布',
      icon: 'none',
      duration: 2000
    })
  },

  // 分享到微博
  onShareToWeibo() {
    wx.showToast({
      title: '请先保存图片，然后在微博中发布',
      icon: 'none',
      duration: 2000
    })
  },

  // 分享到快手
  onShareToKuaishou() {
    wx.showToast({
      title: '请先保存图片，然后在快手中发布',
      icon: 'none',
      duration: 2000
    })
  },

  // 微信分享
  onShareAppMessage() {
    return {
      title: `我的梦想计划已完成${this.data.badgeProgress}%！`,
      path: `/pages/dream-detail/dream-detail?walletId=${this.data.walletId}`,
      imageUrl: this.data.carImageUrl
    }
  }
})
