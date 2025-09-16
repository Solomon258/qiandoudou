// pages/login/login.js
const app = getApp()
const { authAPI } = require('../../utils/api.js')

Page({
  data: {
    loading: false,
    loginType: '', // 'wechat', 'phone', 'register'
    showRegisterForm: false,
    showLoginForm: false,
    showAuthPanel: false, // 保留授权面板代码，但不再使用
    userId: null, // 保留用户ID
    tempAvatar: '', // 保留临时头像URL
    tempNickname: '', // 保留临时昵称
    
    // 新登录页面数据
    phoneNumber: '',
    verificationCode: '',
    agreeTerms: false,
    isPhoneValid: false,
    canLogin: false,
    codeCountdown: 0,
    countdownTimer: null,
    
    registerForm: {
      username: '',
      password: '',
      confirmPassword: '',
      nickname: '',
      phone: ''
    },
    loginForm: {
      username: '',
      password: ''
    }
  },

  onLoad(options) {
    // 检查是否已登录
    if (app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/home/home'
      })
    }
  },

  onUnload() {
    // 清理定时器
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
    }
  },

  // 手机号输入处理
  onPhoneInput(e) {
    const phoneNumber = e.detail.value
    const isPhoneValid = this.validatePhone(phoneNumber)
    
    this.setData({
      phoneNumber,
      isPhoneValid,
      canLogin: isPhoneValid && this.data.verificationCode.length === 6 && this.data.agreeTerms
    })
  },

  // 验证码输入处理
  onCodeInput(e) {
    const verificationCode = e.detail.value
    
    this.setData({
      verificationCode,
      canLogin: this.data.isPhoneValid && verificationCode.length === 6 && this.data.agreeTerms
    })
  },

  // 手机号格式验证
  validatePhone(phone) {
    const phoneRegex = /^1[3-9]\d{9}$/
    return phoneRegex.test(phone)
  },

  // 获取验证码
  getVerificationCode() {
    if (!this.data.isPhoneValid) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }

    // 生成6位随机验证码
    const code = Math.floor(100000 + Math.random() * 900000).toString()
    
    // 模拟填入验证码
    this.setData({
      verificationCode: code,
      canLogin: this.data.isPhoneValid && code.length === 6 && this.data.agreeTerms,
      codeCountdown: 60
    })

    // 开始倒计时
    this.startCountdown()

    wx.showToast({
      title: `验证码：${code}`,
      icon: 'none',
      duration: 3000
    })
  },

  // 开始倒计时
  startCountdown() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
    }

    const timer = setInterval(() => {
      const countdown = this.data.codeCountdown - 1
      if (countdown <= 0) {
        clearInterval(timer)
        this.setData({
          codeCountdown: 0,
          countdownTimer: null
        })
      } else {
        this.setData({
          codeCountdown: countdown
        })
      }
    }, 1000)

    this.setData({
      countdownTimer: timer
    })
  },

  // 切换协议同意状态
  toggleAgreeTerms() {
    const newAgreeTerms = !this.data.agreeTerms
    
    if (!newAgreeTerms) {
      // 如果取消勾选，震动提醒
      wx.vibrateShort({
        type: 'medium'
      })
      wx.showToast({
        title: '请勾选协议',
        icon: 'none'
      })
    }

    this.setData({
      agreeTerms: newAgreeTerms,
      canLogin: this.data.isPhoneValid && this.data.verificationCode.length === 6 && newAgreeTerms
    })
  },

  // 手机号登录
  handlePhoneLogin() {
    console.log('handlePhoneLogin方法被调用')
    console.log('当前数据状态:', {
      agreeTerms: this.data.agreeTerms,
      isPhoneValid: this.data.isPhoneValid,
      phoneNumber: this.data.phoneNumber,
      verificationCode: this.data.verificationCode,
      canLogin: this.data.canLogin
    })
    
    if (!this.data.agreeTerms) {
      wx.vibrateShort({
        type: 'medium'
      })
      wx.showToast({
        title: '请勾选协议',
        icon: 'none'
      })
      return
    }

    if (!this.data.isPhoneValid) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }

    if (this.data.verificationCode.length !== 6) {
      wx.showToast({
        title: '请输入6位验证码',
        icon: 'none'
      })
      return
    }

    this.setData({ 
      loading: true,
      loginType: 'phone'
    })

    // 调用后端手机号登录接口
    console.log('开始调用手机号登录接口', {
      phone: this.data.phoneNumber,
      code: this.data.verificationCode
    })
    
    authAPI.phoneLogin(this.data.phoneNumber, this.data.verificationCode)
      .then(result => {
        console.log('手机号登录成功', result)
        const { token, user } = result.data
        app.setLoginInfo(token, user)
        
        wx.showToast({
          title: '登录成功',
          icon: 'success'
        })

        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/home/home'
          })
        }, 1500)
      })
      .catch(error => {
        console.error('手机号登录失败', error)
        wx.showToast({
          title: error.message || '登录失败',
          icon: 'none'
        })
        this.setData({ loading: false, loginType: '' })
      })
  },

  // 微信登录
  handleWechatLogin() {
    if (!this.data.agreeTerms) {
      wx.vibrateShort({
        type: 'medium'
      })
      wx.showToast({
        title: '请勾选协议',
        icon: 'none'
      })
      return
    }

    this.setData({ 
      loading: true,
      loginType: 'wechat'
    })

    
    // 调用微信登录接口获取code
    wx.login({
      success: (res) => {

        
        if (res.code) {
          // 发送code到后端进行微信登录

          authAPI.wechatLogin(res.code)
            .then(result => {
              // 检查返回数据结构
              if (!result.data) {
                throw new Error('后端返回数据格式错误')
              }
              
              const { token, user } = result.data
              
              // 验证必要字段
              if (!token) {
                throw new Error('未获取到登录token')
              }
              
              if (!user) {
                throw new Error('未获取到用户信息')
              }
              
              // 保存登录信息
              app.setLoginInfo(token, user)
              
              // 检查是否为首次登录且没有自定义头像
              const isFirstLogin = !user.avatar || 
                                   user.avatar.includes('default_avatar.png') || 
                                   user.avatar.includes('usages/user-avatar.png')
              
              console.log('微信登录成功 - 用户:', user.nickname || user.username, '首次登录:', isFirstLogin)
              
              // 不论是否为首次登录，都直接跳转到首页
              wx.showToast({
                title: '微信登录成功',
                icon: 'success'
              })
              
              // 直接跳转首页
              setTimeout(() => {
                wx.redirectTo({
                  url: '/pages/home/home'
                })
              }, 1500)
            })
            .catch(error => {
              console.error('微信登录失败:', error.message)
              
              wx.showToast({
                title: error.message || '微信登录失败',
                icon: 'none',
                duration: 3000
              })
              this.setData({ loading: false, loginType: '' })
            })
        } else {

          wx.showToast({
            title: '获取微信授权失败',
            icon: 'none'
          })
          this.setData({ loading: false, loginType: '' })
        }
      },
      fail: (error) => {

        wx.showToast({
          title: '微信登录失败',
          icon: 'none'
        })
        this.setData({ loading: false, loginType: '' })
      }
    })
  },


  // 隐藏登录表单
  hideLoginForm() {
    this.setData({ 
      showLoginForm: false 
    })
  },

  // 处理登录表单输入
  onLoginInput(e) {
    const { field } = e.currentTarget.dataset
    const value = e.detail.value
    this.setData({
      [`loginForm.${field}`]: value
    })
  },

  // 提交登录
  submitLogin() {
    const { username, password } = this.data.loginForm
    
    if (!username || !password) {
      wx.showToast({
        title: '请填写用户名和密码',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    authAPI.login(username, password)
      .then(result => {
        const { token, user } = result.data
        app.setLoginInfo(token, user)
        
        wx.showToast({
          title: '登录成功',
          icon: 'success'
        })

        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/home/home'
          })
        }, 1500)
      })
      .catch(error => {
        // 不在控制台输出错误，只显示用户友好的提示
        wx.showToast({
          title: error.message || '登录失败',
          icon: 'none'
        })
        this.setData({ loading: false })
      })
  },

  // 显示注册表单
  handleRegister() {
    this.setData({ 
      showRegisterForm: true,
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        phone: ''
      }
    })
  },

  // 隐藏注册表单
  hideRegisterForm() {
    this.setData({ 
      showRegisterForm: false 
    })
  },

  // 处理注册表单输入
  onRegisterInput(e) {
    const { field } = e.currentTarget.dataset
    const value = e.detail.value
    this.setData({
      [`registerForm.${field}`]: value
    })
  },

  // 提交注册
  submitRegister() {
    const { username, password, confirmPassword, nickname, phone } = this.data.registerForm
    
    if (!username || !password || !nickname) {
      wx.showToast({
        title: '请填写必要信息',
        icon: 'none'
      })
      return
    }

    if (password !== confirmPassword) {
      wx.showToast({
        title: '两次密码不一致',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    // 处理手机号：空字符串转为null，避免后端验证问题
    const phoneToSubmit = phone && phone.trim() ? phone.trim() : null

    authAPI.register(username, password, nickname, phoneToSubmit)
      .then(result => {
        wx.showToast({
          title: '注册成功，请登录',
          icon: 'success'
        })
        
        // 注册成功后自动填入登录表单
        this.setData({
          showRegisterForm: false,
          showLoginForm: true,
          loginForm: {
            username: username,
            password: password
          },
          loading: false
        })
      })
      .catch(error => {
        // 不在控制台输出错误，只显示用户友好的提示
        wx.showToast({
          title: error.message || '注册失败',
          icon: 'none'
        })
        this.setData({ loading: false })
      })
  },

  // 处理微信头像选择
  onChooseAvatar(e) {
    console.log('用户选择微信头像:', e.detail)
    const { avatarUrl } = e.detail
    console.log('获取到的微信头像URL:', avatarUrl)
    
    this.setData({
      tempAvatar: avatarUrl
    })
    
    wx.showToast({
      title: '头像选择成功',
      icon: 'success'
    })
  },

  // 处理昵称输入
  onNicknameInput(e) {
    const nickname = e.detail.value
    console.log('用户输入昵称:', nickname)
    
    this.setData({
      tempNickname: nickname
    })
  },

  // 保存用户资料
  handleSaveProfile() {
    const { tempAvatar, tempNickname, userId } = this.data
    
    if (!tempAvatar || !tempNickname) {
      wx.showToast({
        title: '请完善头像和昵称',
        icon: 'none'
      })
      return
    }
    
    console.log('保存用户资料:', { tempNickname, tempAvatar, userId })
    
    // 上传头像和昵称
    this.uploadWechatProfile(tempNickname, tempAvatar, userId)
  },

  // 跳转到手动设置页面
  handleManualSetup() {
    console.log('用户选择手动设置')
    wx.redirectTo({
      url: '/pages/edit-profile/edit-profile?firstLogin=true'
    })
  },

  // 上传微信头像和昵称到后端
  uploadWechatProfile(nickName, avatarUrl, userId) {
    console.log('开始上传微信资料:', { nickName, avatarUrl, userId })
    
    wx.showLoading({
      title: '设置个人资料中...'
    })
    
    // 检查是否为模拟头像（已经是OSS链接）
    if (avatarUrl.includes('qiandoudou.oss-cn-guangzhou.aliyuncs.com')) {
      console.log('使用现有OSS头像，直接更新用户资料')
      // 直接更新用户资料，不需要重新上传
      this.updateUserProfile(nickName, avatarUrl, userId)
      return
    }
    
    // 先下载微信头像到本地
    console.log('开始下载微信头像:', avatarUrl)
    wx.downloadFile({
      url: avatarUrl,
      success: (downloadRes) => {
        console.log('微信头像下载成功:', downloadRes.tempFilePath)
        console.log('下载响应详情:', downloadRes)
        
        // 检查下载的文件
        wx.getFileInfo({
          filePath: downloadRes.tempFilePath,
          success: (fileInfo) => {
            console.log('下载文件信息:', fileInfo)
          },
          fail: (error) => {
            console.error('获取文件信息失败:', error)
          }
        })
        
        // 上传头像到OSS
        console.log('开始上传头像到OSS...')
        const { uploadUserImage } = require('../../utils/api.js')
        uploadUserImage(downloadRes.tempFilePath, 'avatar')
          .then(response => {
            console.log('头像上传OSS响应:', response)
            if (response.data && response.data.imageUrl) {
              const ossAvatarUrl = response.data.imageUrl
              
              // 同时更新头像和昵称到后端
              this.updateUserProfile(nickName, ossAvatarUrl, userId)
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
            // 失败也跳转到首页
            setTimeout(() => {
              wx.redirectTo({
                url: '/pages/home/home'
              })
            }, 2000)
          })
      },
      fail: (error) => {
        console.error('微信头像下载失败:', error)
        wx.hideLoading()
        wx.showToast({
          title: '头像下载失败',
          icon: 'error'
        })
        // 失败也跳转到首页
        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/home/home'
          })
        }, 2000)
      }
    })
  },

  // 更新用户资料到后端
  updateUserProfile(nickName, avatarUrl, userId) {
    console.log('更新用户资料到后端:', { nickName, avatarUrl, userId })
    
    const { authAPI } = require('../../utils/api.js')
    
    // 调用后端接口同时更新头像和昵称
    authAPI.updateProfile(nickName, avatarUrl, userId)
      .then(result => {
        console.log('用户资料更新成功')
        wx.hideLoading()
        
        // 更新本地存储和全局数据
        const userInfo = wx.getStorageSync('userInfo') || {}
        userInfo.nickname = nickName
        userInfo.avatar = avatarUrl
        userInfo.hasCustomAvatar = true
        wx.setStorageSync('userInfo', userInfo)
        console.log('本地存储更新后的userInfo:', userInfo)
        
        // 更新全局数据
        if (app.globalData.userInfo) {
          app.globalData.userInfo.nickname = nickName
          app.globalData.userInfo.avatar = avatarUrl
          app.globalData.userInfo.hasCustomAvatar = true
          console.log('全局数据更新后的userInfo:', app.globalData.userInfo)
        } else {
          // 如果全局数据不存在，创建它
          app.globalData.userInfo = {
            id: userId,
            nickname: nickName,
            avatar: avatarUrl,
            hasCustomAvatar: true
          }
          console.log('创建全局数据userInfo:', app.globalData.userInfo)
        }
        
        wx.showToast({
          title: '个人资料设置成功',
          icon: 'success'
        })
        
        // 跳转到首页
        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/home/home'
          })
        }, 1500)
      })
      .catch(error => {
        console.error('用户资料更新失败:', error)
        wx.hideLoading()
        wx.showToast({
          title: '资料更新失败',
          icon: 'error'
        })
        // 失败也跳转到首页
        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/home/home'
          })
        }, 2000)
      })
  },

  // 显示用户协议
  showAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '这里是用户协议内容...',
      showCancel: false,
      confirmText: '我知道了'
    })
  },

  // 显示隐私政策
  showPrivacy() {
    wx.showModal({
      title: '隐私政策',
      content: '这里是隐私政策内容...',
      showCancel: false,
      confirmText: '我知道了'
    })
  },

})