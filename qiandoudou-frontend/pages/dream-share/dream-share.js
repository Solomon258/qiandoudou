// pages/dream-share/dream-share.js
const app = getApp()
const { walletAPI, dreamImageAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: '',
    travelShareImage: '', // 旅行分享图片
    transferCount: 0, // 转账次数
    currentAmount: '0.00', // 当前余额
    targetAmount: 0, // 目标金额
    currentDate: '', // 当前日期 格式：MM/DD
    remainingPercent: 0, // 距离目标剩余百分比
    daysCount: 0, // 计划天数
    qrCodeUrl: '/static/icon/share_pic/erweima.jpg' // 二维码图片
  },

  onLoad(options) {
    const { walletId, dreamItemName } = options

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

    this.setData({
      walletId,
      dreamItemName: dreamItemName ? decodeURIComponent(dreamItemName) : '',
      currentDate: this.formatCurrentDate()
    })

    // 加载钱包数据
    this.loadWalletData()
  },

  // 加载钱包数据
  async loadWalletData() {
    try {
      wx.showLoading({
        title: '加载中...'
      })

      // 获取钱包详情
      const result = await walletAPI.getDreamWalletDetail(this.data.walletId)

      if (result && result.data) {
        const responseData = result.data
        const walletData = responseData.wallet || responseData

        // 提取数据
        const targetAmount = walletData.dreamTargetAmount || walletData.targetAmount || responseData.targetAmount || 0
        const currentAmount = walletData.balance || walletData.currentAmount || responseData.currentAmount || 0
        // ⭐ 旅行钱包使用dreamDestination字段获取英文name
        const dreamItemName = this.data.dreamItemName ||
                              walletData.dreamDestination || walletData.dream_destination ||
                              'wulumuqi'

        console.log('dream-share加载数据 - dreamItemName:', dreamItemName)
        console.log('dream-share加载数据 - walletData:', walletData)

        // 获取转账记录
        const transferCountData = await walletAPI.getWalletTransactions(this.data.walletId)
        const transferCount = transferCountData.data ? transferCountData.data.length : 0

        // 计算已攒进度百分比
        const progressPercent = targetAmount > 0 ? (currentAmount / targetAmount) * 100 : 0

        // 计算天数
        const createTime = walletData.createTime || walletData.createdAt
        const daysCount = this.calculateDays(createTime)

        // 获取分享图片URL - 从dream_image_config表中获取share_image_url
        let travelShareImage = ''
        try {
          const imageConfigResult = await dreamImageAPI.getShareImage(2, dreamItemName)
          if (imageConfigResult && imageConfigResult.data) {
            travelShareImage = imageConfigResult.data
          }
        } catch (error) {
          console.warn('获取分享图片配置失败，使用备选方案:', error)
        }

        // 如果API获取失败，使用备选图片URL
        if (!travelShareImage) {
          travelShareImage = `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/${dreamItemName}_share.jpg`
        }

        this.setData({
          travelShareImage,
          transferCount,
          currentAmount: targetAmount.toFixed(2), // 显示目标金额
          targetAmount,
          remainingPercent: progressPercent.toFixed(0), // 改为已攒进度百分比
          daysCount
        })

        wx.hideLoading()
      }
    } catch (error) {
      wx.hideLoading()
      console.error('加载钱包数据失败:', error)

      // 即使加载失败，也使用默认图片和默认数据
      const defaultItemName = this.data.dreamItemName || '乌鲁木齐'
      this.setData({
        travelShareImage: `https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/travel/travel/${defaultItemName}_share.jpg`,
        transferCount: 0,
        currentAmount: '0.00',
        remainingPercent: 0,
        daysCount: 1
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

  // 格式化当前日期 MM/DD
  formatCurrentDate() {
    const now = new Date()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    return `${month}/${day}`
  },

  // 关闭页面
  closePage() {
    wx.navigateBack()
  },

  // 保存图片到本地
  onSaveImage() {
    const imageUrl = this.data.travelShareImage

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
      title: '我的旅行梦想正在实现中！',
      path: `/pages/dream-detail/dream-detail?walletId=${this.data.walletId}`,
      imageUrl: this.data.travelShareImage
    }
  }
})
