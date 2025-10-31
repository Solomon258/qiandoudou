// pages/car-share/car-share.js
const { walletAPI, dreamImageAPI } = require('../../utils/api.js')

Page({
  data: {
    walletId: '',
    carShareImage: ''
  },

  onLoad(options) {
    const { walletId } = options

    if (walletId) {
      this.setData({ walletId })
      this.loadShareImage()
    }
  },

  // 加载分享图片
  async loadShareImage() {
    try {
      wx.showLoading({
        title: '加载中...'
      })

      // 获取钱包详情
      const walletResult = await walletAPI.getDreamWalletDetail(this.data.walletId)
      console.log('[car-share] 钱包详情返回:', walletResult)

      if (walletResult && walletResult.data) {
        const walletData = walletResult.data.wallet || walletResult.data
        const dreamItemName = walletData.dreamItemName
        console.log('[car-share] dreamItemName:', dreamItemName)

        if (!dreamItemName) {
          console.warn('[car-share] dreamItemName为空，使用默认图片')
          wx.hideLoading()
          this.setData({
            carShareImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/car_share_compressed.jpg'
          })
          return
        }

        try {
          // 从后端API获取分享图片URL
          console.log('[car-share] 调用API获取分享图片, dreamType=1, itemName=' + dreamItemName)
          const shareImageResult = await dreamImageAPI.getShareImage(1, dreamItemName)
          console.log('[car-share] API返回结果:', shareImageResult)

          if (shareImageResult && shareImageResult.data) {
            console.log('[car-share] 成功获取分享图片:', shareImageResult.data)
            this.setData({
              carShareImage: shareImageResult.data
            })
          } else {
            console.warn('[car-share] API返回数据为空，使用降级方案')
            // 降级方案
            this.setData({
              carShareImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/car_share_compressed.jpg'
            })
          }
        } catch (apiError) {
          console.error('[car-share] 获取分享图片配置失败，使用默认图片:', apiError)
          // 降级方案
          this.setData({
            carShareImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/car_share_compressed.jpg'
          })
        }

        wx.hideLoading()
      } else {
        wx.hideLoading()
        this.setData({
          carShareImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/car_share_compressed.jpg'
        })
      }
    } catch (error) {
      wx.hideLoading()
      console.error('加载分享图片失败:', error)
      // 加载失败时使用默认图片
      this.setData({
        carShareImage: 'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/dream/car/car_share_compressed.jpg'
      })
    }
  },

  // 关闭页面
  closePage() {
    wx.navigateBack()
  },

  // 保存图片到本地
  onSaveImage() {
    const imageUrl = this.data.carShareImage

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
      title: '我的梦想计划正在实现中！',
      path: `/pages/dream-detail/dream-detail?walletId=${this.data.walletId}`,
      imageUrl: this.data.carShareImage
    }
  }
})
