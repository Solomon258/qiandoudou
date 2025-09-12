// pages/edit-profile/edit-profile.js
const app = getApp()
const { walletAPI } = require('../../utils/api.js')

Page({
  data: {
    userInfo: {
      nickname: '昆虫记',
      gender: '',
      description: '每100个粉丝，打卡一个5A级景区',
      avatar: '', // 用户头像路径
      hasCustomAvatar: false // 是否有自定义头像
    },
    settings: {
      showPublicWallets: true,
      showFollowedWallets: true
    },
    showEditModal: false,
    editType: '', // 'nickname', 'gender', 'description'
    editValue: '',
    originalValue: '',
    isSaving: false, // 添加保存状态标识
    isUploadingAvatar: false, // 头像上传状态
    showWechatAvatarOption: false, // 是否显示微信头像选择选项
    isFirstLogin: false, // 是否为首次登录
    shouldSetWechatAvatar: false, // 是否应该自动设置微信头像
    isUpdatingNickname: false, // 昵称更新状态
    showWechatAvatarPicker: false, // 显示微信头像选择器
    showWechatNicknamePicker: false // 显示微信昵称选择器
  },

  onLoad(options) {
    // 检查是否为首次登录和是否需要设置微信头像
    const isFirstLogin = options.firstLogin === 'true'
    const shouldSetWechatAvatar = options.setWechatAvatar === 'true'
    
    this.setData({
      isFirstLogin: isFirstLogin,
      shouldSetWechatAvatar: shouldSetWechatAvatar,
      showWechatAvatarOption: isFirstLogin || shouldSetWechatAvatar
    })
    
    this.loadUserInfo()
    
    // 如果是首次登录且需要设置微信头像，显示引导提示
    if (isFirstLogin && shouldSetWechatAvatar) {
      setTimeout(() => {
        wx.showToast({
          title: '可设置微信头像和昵称',
          icon: 'none',
          duration: 3000
        })
      }, 500)
    }
  },

  onUnload() {
    // 清理所有定时器
    if (this.saveTimer) {
      clearTimeout(this.saveTimer)
    }
    if (this.toastTimer) {
      clearTimeout(this.toastTimer)
    }
    // 重置toast状态
    this.isToastShowing = false
  },

  // 加载用户信息
  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo') || app.globalData.userInfo
    const settings = wx.getStorageSync('userSettings') || {
      showPublicWallets: true,
      showFollowedWallets: true
    }

    if (userInfo) {
      this.setData({
        userInfo: {
          nickname: userInfo.nickname || '昆虫记',
          gender: userInfo.gender || '',
          description: userInfo.description || '每100个粉丝，打卡一个5A级景区',
          avatar: userInfo.avatar || '',
          hasCustomAvatar: !!(userInfo.avatar)
        },
        settings: settings
      })
    }
  },

  // 返回上一页
  goBack() {
    wx.navigateBack()
  },

  // 更换头像
  changeAvatar() {
    wx.showActionSheet({
      itemList: ['拍照', '从相册选择', '使用微信头像', '删除头像'],
      success: (res) => {
        if (res.tapIndex === 0 || res.tapIndex === 1) {
          // 拍照或从相册选择
          const sourceType = res.tapIndex === 0 ? 'camera' : 'album'
          this.selectImage(sourceType)
        } else if (res.tapIndex === 2) {
          // 使用微信头像
          this.chooseWechatAvatar()
        } else if (res.tapIndex === 3) {
          // 删除头像
          this.removeAvatar()
        }
      }
    })
  },

  // 选择图片
  selectImage(sourceType) {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'], // 使用压缩图片
      sourceType: [sourceType],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0]
        this.cropImage(tempFilePath)
      },
      fail: (error) => {

        wx.showToast({
          title: '选择图片失败',
          icon: 'none'
        })
      }
    })
  },

  // 裁剪图片
  cropImage(imagePath) {
    // 获取图片信息
    wx.getImageInfo({
      src: imagePath,
      success: (imageInfo) => {
        // 计算裁剪参数，制作正方形头像
        const { width, height } = imageInfo
        const size = Math.min(width, height)
        const x = (width - size) / 2
        const y = (height - size) / 2

        // 创建画布裁剪图片
        const canvas = wx.createCanvasContext('avatarCanvas', this)
        
        // 由于小程序限制，我们直接使用原图并保存
        this.saveAvatar(imagePath)
      },
      fail: (error) => {

        this.saveAvatar(imagePath) // 即使获取信息失败也尝试保存
      }
    })
  },

  // 保存头像到OSS
  saveAvatar(imagePath) {
    console.log('开始保存头像，路径:', imagePath)
    this.setData({ isUploadingAvatar: true })

    // 检查是否为网络URL（微信头像）
    if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
      console.log('检测到网络URL，需要先下载到本地')
      // 先下载微信头像到本地
      wx.downloadFile({
        url: imagePath,
        success: (res) => {
          console.log('微信头像下载成功:', res.tempFilePath)
          // 使用下载的临时文件路径上传
          this.uploadAvatarToOSS(res.tempFilePath)
        },
        fail: (error) => {
          console.error('微信头像下载失败:', error)
          wx.hideLoading()
          this.setData({ isUploadingAvatar: false })
          wx.showToast({
            title: '头像下载失败',
            icon: 'error'
          })
        }
      })
    } else {
      console.log('本地文件路径，直接上传')
      // 本地文件，直接上传
      this.uploadAvatarToOSS(imagePath)
    }
  },

  // 上传头像到OSS
  uploadAvatarToOSS(filePath) {
    console.log('上传头像到OSS，文件路径:', filePath)
    const { uploadUserImage } = require('../../utils/api.js')
    uploadUserImage(filePath, 'avatar')
      .then(response => {
        console.log('OSS上传响应:', response)
        if (response.data && response.data.imageUrl) {
          const ossUrl = response.data.imageUrl

          
          // 更新用户信息
          const userInfo = { ...this.data.userInfo }
          userInfo.avatar = ossUrl
          userInfo.hasCustomAvatar = true

          this.setData({ 
            userInfo,
            isUploadingAvatar: false
          })

          // 保存到本地存储
          const storedUserInfo = wx.getStorageSync('userInfo') || {
            id: 1,
            nickname: '钱兜兜用户',
            description: '这个人很懒，什么都没留下'
          }
          storedUserInfo.avatar = ossUrl
          storedUserInfo.hasCustomAvatar = true
          wx.setStorageSync('userInfo', storedUserInfo)

          // 更新全局数据
          app.globalData.userInfo = app.globalData.userInfo || {}
          app.globalData.userInfo.avatar = ossUrl
          app.globalData.userInfo.hasCustomAvatar = true
          app.globalData.userInfo.nickname = app.globalData.userInfo.nickname || storedUserInfo.nickname
          app.globalData.userInfo.id = app.globalData.userInfo.id || storedUserInfo.id


          // 同时更新后端数据库中的头像URL
          this.updateAvatarToServer(ossUrl)

          wx.showToast({
            title: '头像更新成功',
            icon: 'success'
          })
        }
      })
      .catch(error => {
        console.error('OSS上传失败:', error)
        this.setData({ isUploadingAvatar: false })
        
        // 上传失败时，尝试使用本地保存作为备用方案
        wx.saveFile({
          tempFilePath: filePath,
          success: (res) => {
            const savedFilePath = res.savedFilePath

            
            // 更新用户信息
            const userInfo = { ...this.data.userInfo }
            userInfo.avatar = savedFilePath
            userInfo.hasCustomAvatar = true

            this.setData({ 
              userInfo,
              isUploadingAvatar: false
            })

            // 保存到本地存储
            const storedUserInfo = wx.getStorageSync('userInfo') || {}
            storedUserInfo.avatar = savedFilePath
            storedUserInfo.hasCustomAvatar = true
            wx.setStorageSync('userInfo', storedUserInfo)

            // 更新全局数据
            if (app.globalData.userInfo) {
              app.globalData.userInfo.avatar = savedFilePath
              app.globalData.userInfo.hasCustomAvatar = true
            }

            wx.showToast({
              title: '头像已保存到本地',
              icon: 'success'
            })
          },
          fail: () => {
            wx.showToast({
              title: '头像保存失败',
              icon: 'error'
            })
          }
        })
      })
  },

  // 更新头像URL到服务器
  updateAvatarToServer(avatarUrl) {
    const { authAPI } = require('../../utils/api.js')
    
    // 获取当前用户ID
    const userId = app.globalData.userInfo?.id || wx.getStorageSync('userInfo')?.id

    
    authAPI.updateAvatar(avatarUrl, userId)
      .then(result => {

      })
      .catch(error => {

        // 不影响用户体验，静默失败
      })
  },

  // 处理微信头像选择
  onChooseWechatAvatar(e) {
    console.log('微信头像选择事件:', e)
    const { avatarUrl } = e.detail
    console.log('选择的微信头像:', avatarUrl)
    
    if (!avatarUrl) {
      console.error('未获取到微信头像URL')
      wx.showToast({
        title: '未选择头像',
        icon: 'none'
      })
      return
    }
    
    // 显示加载提示
    wx.showLoading({
      title: '设置头像中...'
    })
    
    this.setData({ isUploadingAvatar: true })
    
    console.log('开始上传微信头像到OSS...')
    // 直接使用微信头像路径上传到OSS
    this.saveAvatar(avatarUrl)
    
    // 如果是首次登录，设置完头像后引导用户
    if (this.data.isFirstLogin) {
      setTimeout(() => {
        wx.hideLoading()
        wx.showModal({
          title: '设置成功',
          content: '微信头像设置成功！现在去探索钱兜兜吧~',
          showCancel: false,
          confirmText: '开始使用',
          success: () => {
            wx.redirectTo({
              url: '/pages/home/home'
            })
          }
        })
      }, 2000)
    } else {
      setTimeout(() => {
        wx.hideLoading()
      }, 1000)
    }
  },

  // 处理微信昵称输入
  onWechatNicknameInput(e) {
    const nickname = e.detail.value
    console.log('获取到微信昵称:', nickname)
    
    if (!nickname || nickname.trim() === '') {
      console.log('昵称为空，忽略')
      return
    }
    
    // 显示加载状态
    this.setData({ isUpdatingNickname: true })
    
    // 更新昵称
    this.updateNickname(nickname.trim())
  },

  // 更新用户昵称
  updateNickname(nickname) {
    console.log('开始更新昵称:', nickname)
    
    // 更新本地显示
    const userInfo = { ...this.data.userInfo }
    userInfo.nickname = nickname
    
    this.setData({ 
      userInfo,
      isUpdatingNickname: false
    })
    
    // 保存到本地存储
    const storedUserInfo = wx.getStorageSync('userInfo') || {}
    storedUserInfo.nickname = nickname
    wx.setStorageSync('userInfo', storedUserInfo)
    
    // 更新全局数据
    if (app.globalData.userInfo) {
      app.globalData.userInfo.nickname = nickname
    }
    
    // 同时更新后端数据库
    this.updateNicknameToServer(nickname)
    
    wx.showToast({
      title: '昵称更新成功',
      icon: 'success'
    })
    
    console.log('昵称更新完成:', nickname)
  },

  // 更新昵称到服务器
  updateNicknameToServer(nickname) {
    const { authAPI } = require('../../utils/api.js')
    
    // 获取当前用户ID
    const userId = app.globalData.userInfo?.id || wx.getStorageSync('userInfo')?.id
    
    if (!userId) {
      console.error('无法获取用户ID，跳过服务器更新')
      return
    }
    
    // 调用更新昵称的API
    authAPI.updateNickname(nickname, userId)
      .then(result => {
        console.log('服务器昵称更新成功')
      })
      .catch(error => {
        console.error('服务器昵称更新失败:', error)
        // 不影响用户体验，静默失败
      })
  },

  // 选择微信头像
  chooseWechatAvatar() {
    // 显示一个隐藏的按钮来触发微信头像选择
    this.setData({
      showWechatAvatarPicker: true
    })
    
    // 延迟一下，确保DOM更新完成
    setTimeout(() => {
      // 模拟点击微信头像选择按钮
      const query = wx.createSelectorQuery()
      query.select('#wechat-avatar-picker').boundingClientRect()
      query.exec((res) => {
        if (res[0]) {
          // 触发微信头像选择
          wx.showToast({
            title: '请点击"选择微信头像"按钮',
            icon: 'none',
            duration: 2000
          })
        }
      })
    }, 100)
  },

  // 选择微信昵称
  chooseWechatNickname() {
    // 显示一个隐藏的输入框来触发微信昵称选择
    this.setData({
      showWechatNicknamePicker: true
    })
    
    // 延迟一下，确保DOM更新完成
    setTimeout(() => {
      wx.showToast({
        title: '请点击"获取微信昵称"输入框',
        icon: 'none',
        duration: 2000
      })
    }, 100)
  },

  // 处理微信昵称选择（从隐藏输入框）
  onWechatNicknameSelected(e) {
    const nickname = e.detail.value
    if (nickname && nickname.trim()) {
      console.log('选择的微信昵称:', nickname)
      
      // 更新昵称
      this.updateNickname(nickname.trim())
      
      // 隐藏选择器
      this.setData({
        showWechatNicknamePicker: false
      })
    }
  },

  // 处理微信头像选择（从隐藏按钮）
  onWechatAvatarSelected(e) {
    console.log('微信头像选择事件:', e)
    const { avatarUrl } = e.detail
    console.log('选择的微信头像:', avatarUrl)
    
    if (!avatarUrl) {
      console.error('未获取到微信头像URL')
      wx.showToast({
        title: '未选择头像',
        icon: 'none'
      })
      return
    }
    
    // 显示加载提示
    wx.showLoading({
      title: '设置头像中...'
    })
    
    this.setData({ 
      isUploadingAvatar: true,
      showWechatAvatarPicker: false
    })
    
    // 处理微信头像上传
    this.processWechatAvatar(avatarUrl)
  },

  // 处理微信头像上传
  processWechatAvatar(avatarUrl) {
    // 检查是否为模拟头像（已经是OSS链接）
    if (avatarUrl.includes('qiandoudou.oss-cn-guangzhou.aliyuncs.com')) {
      console.log('使用现有OSS头像，直接更新用户资料')
      // 直接更新用户资料，不需要重新上传
      this.updateAvatarInfo(avatarUrl)
      return
    }
    
    // 先下载微信头像到本地
    console.log('开始下载微信头像:', avatarUrl)
    wx.downloadFile({
      url: avatarUrl,
      success: (downloadRes) => {
        console.log('微信头像下载成功:', downloadRes.tempFilePath)
        
        // 上传头像到OSS
        console.log('开始上传头像到OSS...')
        const { uploadUserImage } = require('../../utils/api.js')
        uploadUserImage(downloadRes.tempFilePath, 'avatar')
          .then(response => {
            console.log('头像上传OSS响应:', response)
            if (response.data && response.data.imageUrl) {
              const ossAvatarUrl = response.data.imageUrl
              this.updateAvatarInfo(ossAvatarUrl)
            } else {
              throw new Error('头像上传失败')
            }
          })
          .catch(error => {
            console.error('头像上传失败:', error)
            wx.hideLoading()
            wx.showToast({
              title: '头像上传失败',
              icon: 'error'
            })
            this.setData({ isUploadingAvatar: false })
          })
      },
      fail: (error) => {
        console.error('微信头像下载失败:', error)
        wx.hideLoading()
        wx.showToast({
          title: '头像下载失败',
          icon: 'error'
        })
        this.setData({ isUploadingAvatar: false })
      }
    })
  },

  // 更新头像信息
  updateAvatarInfo(avatarUrl) {
    // 更新本地数据
    const userInfo = { ...this.data.userInfo }
    userInfo.avatar = avatarUrl
    userInfo.hasCustomAvatar = true
    
    this.setData({
      userInfo,
      isUploadingAvatar: false
    })
    
    // 保存到本地存储
    const storedUserInfo = wx.getStorageSync('userInfo') || {}
    storedUserInfo.avatar = avatarUrl
    storedUserInfo.hasCustomAvatar = true
    wx.setStorageSync('userInfo', storedUserInfo)
    
    // 更新全局数据
    if (app.globalData.userInfo) {
      app.globalData.userInfo.avatar = avatarUrl
      app.globalData.userInfo.hasCustomAvatar = true
    }
    
    // 更新服务器
    this.updateAvatarToServer(avatarUrl)
    
    wx.hideLoading()
    wx.showToast({
      title: '头像设置成功',
      icon: 'success'
    })
  },

  // 更新头像到服务器
  updateAvatarToServer(avatarUrl) {
    const { authAPI } = require('../../utils/api.js')
    const userId = app.globalData.userInfo?.id || wx.getStorageSync('userInfo')?.id
    
    if (!userId) {
      console.error('无法获取用户ID，跳过服务器更新')
      return
    }
    
    authAPI.updateAvatar(avatarUrl, userId)
      .then(result => {
        console.log('服务器头像更新成功')
      })
      .catch(error => {
        console.error('服务器头像更新失败:', error)
        // 不影响用户体验，静默失败
      })
  },

  // 隐藏微信头像选择器
  hideWechatAvatarPicker() {
    this.setData({
      showWechatAvatarPicker: false
    })
  },

  // 隐藏微信昵称选择器
  hideWechatNicknamePicker() {
    this.setData({
      showWechatNicknamePicker: false
    })
  },

  // 上传头像到服务器（旧方法，保留兼容性）
  uploadAvatarToServer(filePath) {
    const userId = app.globalData.userInfo?.id
    if (!userId) {

      return
    }

    // 这里可以调用文件上传接口
    // walletAPI.uploadAvatar(userId, filePath)

  },

  // 删除头像
  removeAvatar() {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除当前头像吗？',
      success: (res) => {
        if (res.confirm) {
          // 更新用户信息
          const userInfo = { ...this.data.userInfo }
          userInfo.avatar = ''
          userInfo.hasCustomAvatar = false

          this.setData({ userInfo })

          // 保存到本地存储
          const storedUserInfo = wx.getStorageSync('userInfo') || {}
          storedUserInfo.avatar = ''
          storedUserInfo.hasCustomAvatar = false
          wx.setStorageSync('userInfo', storedUserInfo)

          // 更新全局数据
          if (app.globalData.userInfo) {
            app.globalData.userInfo.avatar = ''
            app.globalData.userInfo.hasCustomAvatar = false
          }

          // 保存到服务器
          this.saveUserInfoToServer(userInfo)

          wx.showToast({
            title: '头像已删除',
            icon: 'success'
          })
        }
      }
    })
  },

  // 编辑昵称
  editNickname() {
    wx.showActionSheet({
      itemList: ['手动输入昵称', '使用微信昵称'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 手动输入昵称
          this.openEditModal('nickname', this.data.userInfo.nickname)
        } else if (res.tapIndex === 1) {
          // 使用微信昵称
          this.chooseWechatNickname()
        }
      }
    })
  },

  // 编辑性别
  editGender() {
    this.openEditModal('gender', this.data.userInfo.gender)
  },

  // 编辑简介
  editDescription() {
    this.openEditModal('description', this.data.userInfo.description)
  },

  // 打开编辑弹窗
  openEditModal(type, value) {
    this.setData({
      showEditModal: true,
      editType: type,
      editValue: value || '',
      originalValue: value || ''
    })
  },

  // 关闭编辑弹窗
  closeEditModal() {
    this.setData({
      showEditModal: false,
      editType: '',
      editValue: '',
      originalValue: ''
    })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 空函数，用于阻止事件冒泡
  },

  // 输入框内容变化
  onEditInput(e) {
    this.setData({
      editValue: e.detail.value
    })
  },

  // 选择性别
  selectGender(e) {
    const gender = e.currentTarget.dataset.gender
    this.setData({
      editValue: gender
    })
  },

  // 确认编辑
  confirmEdit() {
    const { editType, editValue } = this.data

    if (editType === 'nickname' && !editValue.trim()) {
      wx.showToast({
        title: '昵称不能为空',
        icon: 'none'
      })
      return
    }

    // 更新本地数据
    const userInfo = { ...this.data.userInfo }
    userInfo[editType] = editValue

    this.setData({ userInfo })

    // 保存到本地存储
    const storedUserInfo = wx.getStorageSync('userInfo') || {}
    storedUserInfo[editType] = editValue
    wx.setStorageSync('userInfo', storedUserInfo)

    // 更新全局数据
    if (app.globalData.userInfo) {
      app.globalData.userInfo[editType] = editValue
    }

    // 调用API保存到服务器
    this.saveUserInfoToServer(userInfo)

    this.closeEditModal()

    wx.showToast({
      title: '保存成功',
      icon: 'success'
    })
  },

  // 保存用户信息到服务器
  saveUserInfoToServer(userInfo) {
    const userId = app.globalData.userInfo?.id
    if (!userId) {

      return
    }

    // 调用API保存用户信息
    walletAPI.updateUserInfo(userId, userInfo)
      .then(result => {

      })
      .catch(error => {

        // 其他类型的错误（非404）才需要特殊处理
      })
  },

  // 公开钱包开关切换
  onPublicWalletsToggle(e) {
    const checked = e.detail.value
    const settings = { ...this.data.settings }
    settings.showPublicWallets = checked

    this.setData({ settings })
    this.saveSettingsDebounced(settings)
  },

  // 关注钱包开关切换
  onFollowedWalletsToggle(e) {
    const checked = e.detail.value
    const settings = { ...this.data.settings }
    settings.showFollowedWallets = checked

    this.setData({ settings })
    this.saveSettingsDebounced(settings)
  },

  // 防抖保存设置
  saveSettingsDebounced(settings) {
    // 清除之前的定时器
    if (this.saveTimer) {
      clearTimeout(this.saveTimer)
    }
    if (this.toastTimer) {
      clearTimeout(this.toastTimer)
    }

    // 立即保存到本地存储
    wx.setStorageSync('userSettings', settings)

    // 设置保存状态
    this.setData({ isSaving: true })

    // 延迟保存到服务器，避免频繁请求
    this.saveTimer = setTimeout(() => {
      this.saveSettingsToServer(settings)
    }, 300)

    // 延迟显示成功提示，避免频繁弹出
    this.toastTimer = setTimeout(() => {
      this.setData({ isSaving: false })
      this.showSaveSuccessToast()
    }, 800)
  },

  // 保存设置到服务器
  saveSettingsToServer(settings) {
    const userId = app.globalData.userInfo?.id
    if (!userId) {

      return
    }

    walletAPI.updateUserSettings(userId, settings)
      .then(result => {

      })
      .catch(error => {

        this.setData({ isSaving: false })
      })
  },

  // 显示保存成功提示（防抖）
  showSaveSuccessToast() {
    // 如果已经有toast在显示，就不再显示新的
    if (this.isToastShowing) {
      return
    }
    
    this.isToastShowing = true
    wx.showToast({
      title: '已保存',
      icon: 'success',
      duration: 1000,
      complete: () => {
        setTimeout(() => {
          this.isToastShowing = false
        }, 1000)
      }
    })
  }
})
