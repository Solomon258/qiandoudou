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
    isUploadingAvatar: false // 头像上传状态
  },

  onLoad() {
    this.loadUserInfo()
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
      itemList: ['拍照', '从相册选择', '删除头像'],
      success: (res) => {
        if (res.tapIndex === 0 || res.tapIndex === 1) {
          // 拍照或从相册选择
          const sourceType = res.tapIndex === 0 ? 'camera' : 'album'
          this.selectImage(sourceType)
        } else if (res.tapIndex === 2) {
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
        console.error('选择图片失败:', error)
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
        console.error('获取图片信息失败:', error)
        this.saveAvatar(imagePath) // 即使获取信息失败也尝试保存
      }
    })
  },

  // 保存头像到OSS
  saveAvatar(imagePath) {
    this.setData({ isUploadingAvatar: true })

    // 上传到OSS
    const { uploadUserImage } = require('../../utils/api.js')
    uploadUserImage(imagePath, 'avatar')
      .then(response => {
        if (response.data && response.data.imageUrl) {
          const ossUrl = response.data.imageUrl
          console.log('头像上传OSS成功:', ossUrl)
          
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
          
          console.log('头像上传后更新的用户信息:', app.globalData.userInfo)

          // 同时更新后端数据库中的头像URL
          this.updateAvatarToServer(ossUrl)

          wx.showToast({
            title: '头像更新成功',
            icon: 'success'
          })
        }
      })
      .catch(error => {
        console.error('头像上传失败:', error)
        this.setData({ isUploadingAvatar: false })
        
        // 上传失败时，尝试使用本地保存作为备用方案
        wx.saveFile({
          tempFilePath: imagePath,
          success: (res) => {
            const savedFilePath = res.savedFilePath
            console.log('头像本地保存成功:', savedFilePath)
            
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
    console.log('准备更新头像到服务器 - 用户ID:', userId, '头像URL:', avatarUrl)
    
    authAPI.updateAvatar(avatarUrl, userId)
      .then(result => {
        console.log('头像URL已同步到服务器:', avatarUrl)
      })
      .catch(error => {
        console.error('同步头像URL到服务器失败:', error)
        // 不影响用户体验，静默失败
      })
  },

  // 上传头像到服务器（旧方法，保留兼容性）
  uploadAvatarToServer(filePath) {
    const userId = app.globalData.userInfo?.id
    if (!userId) {
      console.log('用户ID不存在，跳过服务器上传')
      return
    }

    // 这里可以调用文件上传接口
    // walletAPI.uploadAvatar(userId, filePath)
    console.log('头像上传到服务器功能待实现')
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
    this.openEditModal('nickname', this.data.userInfo.nickname)
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
      console.log('用户ID不存在，跳过服务器保存')
      return
    }

    // 调用API保存用户信息
    walletAPI.updateUserInfo(userId, userInfo)
      .then(result => {
        console.log('用户信息保存结果:', result.message || '保存成功')
      })
      .catch(error => {
        console.log('用户信息保存失败:', error.message)
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
      console.log('用户ID不存在，跳过服务器保存')
      return
    }

    walletAPI.updateUserSettings(userId, settings)
      .then(result => {
        console.log('设置保存结果:', result.message || '保存成功')
      })
      .catch(error => {
        console.log('设置保存失败:', error.message)
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
