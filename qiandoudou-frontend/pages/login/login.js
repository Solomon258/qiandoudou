// pages/login/login.js
const app = getApp()
const { authAPI } = require('../../utils/api.js')

Page({
  data: {
    loading: false,
    loginType: '', // 'wechat', 'phone', 'register'
    showRegisterForm: false,
    showLoginForm: false,
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

  // 微信登录
  handleWechatLogin() {
    this.setData({ 
      loading: true,
      loginType: 'wechat'
    })

    wx.login({
      success: (res) => {
        if (res.code) {
          // 发送code到后端进行微信登录
          authAPI.wechatLogin(res.code)
            .then(result => {
              const { token, user } = result.data
              app.setLoginInfo(token, user)
              
              wx.showToast({
                title: '微信登录成功',
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
                title: error.message || '微信登录失败',
                icon: 'none'
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
      fail: () => {
        wx.showToast({
          title: '微信登录失败',
          icon: 'none'
        })
        this.setData({ loading: false, loginType: '' })
      }
    })
  },

  // 显示登录表单
  handlePhoneLogin() {
    this.setData({ 
      showLoginForm: true,
      loginForm: {
        username: '',
        password: ''
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

    authAPI.register(username, password, nickname, phone)
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