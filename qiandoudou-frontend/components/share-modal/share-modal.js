// components/share-modal/share-modal.js
Component({
  /**
   * 组件的属性列表
   */
  properties: {
    show: {
      type: Boolean,
      value: false,
      observer: function(newVal, oldVal) {
      }
    },
    shareImageUrl: {
      type: String,
      value: '',
      observer: function(newVal, oldVal) {
        // 重置图片状态
        this.setData({
          imageLoaded: false,
          imageError: false
        })
      }
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
    imageLoaded: false,
    imageError: false
  },

  /**
   * 组件的方法列表
   */
  methods: {
    // 关闭弹窗
    onClose() {
      this.triggerEvent('close')
    },

    // 点击遮罩层关闭
    onOverlayTap() {
      this.onClose()
    },

    // 阻止事件冒泡
    stopPropagation() {
      // 空函数，用于阻止事件冒泡
    },

    // 图片加载完成
    onImageLoad(e) {
      this.setData({
        imageLoaded: true,
        imageError: false
      })
    },

    // 图片加载失败
    onImageError(e) {
      this.setData({
        imageLoaded: false,
        imageError: true
      })
      wx.showToast({
        title: '图片加载失败',
        icon: 'none'
      })
    },

    // 分享到朋友圈
    onShareToFriends() {
      // 微信小程序中分享到朋友圈需要用户主动触发
      wx.showToast({
        title: '请先保存图片，然后在朋友圈发布',
        icon: 'none'
      })
    },

    // 分享到抖音
    onShareToDouyin() {
      wx.showToast({
        title: '请先保存图片，然后在抖音中发布',
        icon: 'none'
      })
    },

    // 分享到小红书
    onShareToXiaohongshu() {
      wx.showToast({
        title: '请先保存图片，然后在小红书中发布',
        icon: 'none'
      })
    },

    // 分享到微博
    onShareToWeibo() {
      wx.showToast({
        title: '请先保存图片，然后在微博中发布',
        icon: 'none'
      })
    },

    // 分享到快手
    onShareToKuaishou() {
      wx.showToast({
        title: '请先保存图片，然后在快手中发布',
        icon: 'none'
      })
    },

    // 保存图片到本地
    onSave() {
      const { shareImageUrl } = this.properties
      
      if (!shareImageUrl) {
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
        url: shareImageUrl,
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
                this.triggerEvent('save', { success: true })
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
                              this.onSave()
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
                this.triggerEvent('save', { success: false, error: saveError })
              }
            })
          } else {
            wx.hideLoading()
            wx.showToast({
              title: '下载图片失败',
              icon: 'none'
            })
            this.triggerEvent('save', { success: false, error: '下载失败' })
          }
        },
        fail: (downloadError) => {
          wx.hideLoading()
          wx.showToast({
            title: '下载图片失败',
            icon: 'none'
          })
          this.triggerEvent('save', { success: false, error: downloadError })
        }
      })
    }
  }
})
