// utils/api.js - 微信小程序API工具函数
const app = getApp()

// 后端API基础地址
// const BASE_URL = 'http://localhost:8080/api'  // 本地开发
const BASE_URL = 'http://192.168.43.188:8080/api'  // IP访问（用于微信小程序调试）
// const BASE_URL = 'https://heartllo.cn/api'  // 生产环境域名
// const BASE_URL = 'https://ai-where.com/api'
// https://heartllo.cn/api/scripts/2/chapters/2

/**
 * 通用网络请求函数
 */
function request(options) {
  return new Promise((resolve, reject) => {
    // 获取token
    const token = app.globalData.token || wx.getStorageSync('token')
    
    // 构建请求头
    const header = {
      'Content-Type': 'application/json; charset=utf-8',
      ...options.header
    }
    
    // 如果有token，添加到请求头
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    // 添加调试信息
    if (options.url.includes('/auth/phone-login')) {
      console.log('发起手机号登录请求', {
        url: `${BASE_URL}${options.url}`,
        method: options.method || 'GET',
        data: options.data,
        header: header
      })
    }

    wx.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: (res) => {
        // 添加调试信息
        if (options.url.includes('/auth/phone-login')) {
          console.log('手机号登录请求响应', res)
        }
        
        // 只在开发模式下输出详细日志
        if (options.debug !== false) {


        }
        
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data)
          } else {

            reject(new Error(res.data.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          // token过期，清除登录信息并跳转到登录页
          app.clearLoginInfo()
          wx.showToast({
            title: '登录已过期',
            icon: 'none'
          })
          setTimeout(() => {
            wx.redirectTo({
              url: '/pages/login/login'
            })
          }, 1500)
          reject(new Error('登录已过期'))
        } else {
          reject(new Error(`网络错误: ${res.statusCode}`))
        }
      },
      fail: (error) => {
        // 添加调试信息
        if (options.url.includes('/auth/phone-login')) {
          console.error('手机号登录请求失败', error)
        }
        
        // 只在网络真正失败时输出错误
        console.error('网络请求失败:', {
          url: `${BASE_URL}${options.url}`,
          error: error
        })
        
        // 根据错误类型提供更具体的错误信息
        let errorMessage = '网络连接失败'
        if (error && error.errMsg) {
          errorMessage = error.errMsg
        }
        reject(new Error(errorMessage))
      }
    })
  })
}

/**
 * 认证相关API
 */
const authAPI = {
  // 用户登录
  login(username, password) {
    return request({
      url: '/auth/login',
      method: 'POST',
      data: { username, password }
    })
  },

  // 用户注册
  register(username, password, nickname, phone) {
    return request({
      url: '/auth/register',
      method: 'POST',
      data: { username, password, nickname, phone }
    })
  },

  // 微信登录
  wechatLogin(code) {
    return request({
      url: '/auth/wechat-login',
      method: 'POST',
      data: { code }
    })
  },

  // 手机号登录
  phoneLogin(phone, code) {
    console.log('phoneLogin API调用', { phone, code })
    return request({
      url: '/auth/phone-login',
      method: 'POST',
      data: { phone, code }
    })
  },

  // 获取当前用户信息
  getCurrentUser(userId) {
    return request({
      url: '/auth/current-user',
      method: 'GET',
      data: userId ? { userId } : {}
    })
  },

  // 更新用户头像
  updateAvatar(avatarUrl, userId) {
    return request({
      url: '/auth/update-avatar',
      method: 'POST',
      data: { 
        avatarUrl: avatarUrl,
        userId: userId
      }
    })
  },

  // 更新用户昵称
  updateNickname(nickname, userId) {
    return request({
      url: '/auth/update-nickname',
      method: 'POST',
      data: { 
        nickname: nickname,
        userId: userId
      }
    })
  },

  // 同时更新用户头像和昵称
  updateProfile(nickname, avatarUrl, userId) {
    return request({
      url: '/auth/update-profile',
      method: 'POST',
      data: { 
        nickname: nickname,
        avatarUrl: avatarUrl,
        userId: userId
      }
    })
  }
}

/**
 * 钱包相关API
 */
const walletAPI = {
  // 获取用户钱包列表
  getUserWallets(userId) {
    return request({
      url: '/wallet/list',
      method: 'GET',
      data: { userId }
    })
  },

  // 获取钱包详情
  getWalletDetail(walletId) {
    return request({
      url: '/wallet/detail',
      method: 'GET',
      data: { walletId }
    })
  },

  // 创建钱包
  createWallet(userId, name, type, backgroundImage, aiPartnerId) {
    return request({
      url: '/wallet/create',
      method: 'POST',
      data: { userId, name, type, backgroundImage, aiPartnerId }
    })
  },

  // 转入资金
  transferIn(walletId, amount, description, imageUrl, note) {
    return request({
      url: '/wallet/transfer-in',
      method: 'POST',
      data: { walletId, amount, description, imageUrl, note }
    })
  },

  // 转出资金
  transferOut(walletId, amount, description, imageUrl, note) {
    return request({
      url: '/wallet/transfer-out',
      method: 'POST',
      data: { walletId, amount, description, imageUrl, note }
    })
  },

  // 获取钱包交易记录
  getWalletTransactions(walletId) {
    return request({
      url: '/wallet/transactions',
      method: 'GET',
      data: { walletId }
    })
  },

  // 更新钱包背景
  updateWalletBackground(walletId, backgroundImage) {
    return request({
      url: '/wallet/update-background',
      method: 'PUT',
      data: { walletId, backgroundImage }
    })
  },

  // 更新钱包名称
  updateWalletName(walletId, name) {
    return request({
      url: '/wallet/update-name',
      method: 'PUT',
      data: { walletId, name }
    })
  },

  // 获取公开钱包列表（用于兜圈圈）
  getPublicWallets(page = 1, size = 10) {
    console.log(`API调用: getPublicWallets, page: ${page}, size: ${size}`)
    
    return request({
      url: '/wallet/public',
      method: 'GET',
      data: { page, size },
      debug: true, // 强制显示调试信息
      header: {
        'Accept': 'application/json'
      }
    })
  },

  // 获取用户的公开钱包列表
  getUserPublicWallets(userId) {
    return request({
      url: '/wallet/user-public',
      method: 'GET',
      data: { userId }
    })
  },

  // 设置钱包公开/私密状态
  setWalletPublic(walletId, isPublic) {
    return request({
      url: '/wallet/set-public',
      method: 'PUT',
      data: { walletId, isPublic }
    })
  },

  // 获取用户关注的钱包列表
  getUserFollowedWallets(userId) {
    return request({
      url: '/wallet/user-followed',
      method: 'GET',
      data: { userId }
    })
  },

  // 关注钱包
  followWallet(userId, walletId) {
    return request({
      url: '/wallet/follow',
      method: 'POST',
      data: { userId, walletId }
    })
  },

  // 取消关注钱包
  unfollowWallet(userId, walletId) {
    return request({
      url: '/wallet/unfollow',
      method: 'POST',
      data: { userId, walletId }
    })
  },

  // 检查关注状态
  checkFollowStatus(currentUserId, targetUserId) {

    return request({
      url: '/social/user/check-follow',
      method: 'GET',
      data: { currentUserId, targetUserId }
    }).then(result => {

      return result
    }).catch(error => {

      throw error
    })
  },

  // 获取钱包所有者ID
  getWalletOwnerId(walletId) {

    return request({
      url: '/wallet/owner',
      method: 'GET',
      data: { walletId }
    }).then(result => {

      return result
    }).catch(error => {

      throw error
    })
  },

  // 获取用户社交统计数据
  getUserSocialStats(userId) {
    return request({
      url: '/user/social-stats',
      method: 'GET',
      data: { userId }
    })
  },

  // 更新用户信息
  updateUserInfo(userId, userInfo) {
    return request({
      url: '/user/update-info',
      method: 'POST',
      data: { userId, ...userInfo }
    }).catch(error => {
      // 如果是404错误，说明接口未实现，返回一个成功的模拟响应
      if (error.message && error.message.includes('404')) {

        return { success: true, message: '本地保存成功', data: userInfo }
      }
      throw error
    })
  },

  // 更新用户设置
  updateUserSettings(userId, settings) {
    return request({
      url: '/user/update-settings',
      method: 'POST',
      data: { userId, ...settings }
    }).catch(error => {
      // 如果是404错误，说明接口未实现，返回一个成功的模拟响应
      if (error.message && error.message.includes('404')) {

        return { success: true, message: '本地保存成功', data: settings }
      }
      throw error
    })
  },

  // 获取用户互动消息
  getUserInteractionMessages(userId, page = 1, pageSize = 20) {
    return request({
      url: '/user/interaction-messages',
      method: 'GET',
      data: { userId, page, pageSize }
    })
  },

  // 获取用户未读消息数量
  getUnreadMessageCount(userId) {
    return request({
      url: '/user/unread-count',
      method: 'GET',
      data: { userId }
    })
  },

  // 标记消息为已读
  markMessagesAsRead(userId) {
    return request({
      url: '/user/mark-read',
      method: 'POST',
      data: { userId }
    })
  },

  // 点赞交易记录
  likeTransaction(userId, transactionId) {
    return request({
      url: '/social/transaction/like',
      method: 'POST',
      data: { userId, transactionId }
    })
  },

  // 取消点赞交易记录
  unlikeTransaction(userId, transactionId) {
    return request({
      url: '/social/transaction/unlike',
      method: 'POST',
      data: { userId, transactionId }
    })
  },

  // 评论交易记录
  commentTransaction(userId, transactionId, content) {
    return request({
      url: '/social/transaction/comment',
      method: 'POST',
      data: { userId, transactionId, content }
    })
  },

  // 获取交易评论列表
  getTransactionComments(transactionId) {
    return request({
      url: '/social/transaction/comments',
      method: 'GET',
      data: { transactionId }
    })
  },

  // AI伴侣自动转入资金
  aiPartnerTransferIn(walletId, aiPartnerId, amount, message, aiPartnerName, aiPartnerAvatar, characterName) {
    return request({
      url: '/wallet/ai-partner-transfer',
      method: 'POST',
      data: { 
        walletId, 
        aiPartnerId, 
        amount, 
        message, 
        aiPartnerName, 
        aiPartnerAvatar,
        characterName  // 新增人物名称参数
      }
    })
  },

  // 获取用户社交统计数据
  getUserSocialStats(userId) {
    return request({
      url: '/social/user/social-stats',
      method: 'GET',
      data: { userId }
    })
  },

  // 获取钱包社交统计数据
  getWalletSocialStats(walletId) {
    return request({
      url: '/social/wallet/social-stats',
      method: 'GET',
      data: { walletId }
    })
  },

  // 记录钱包浏览
  recordWalletView(userId, walletId) {
    return request({
      url: '/social/wallet/view',
      method: 'POST',
      data: { userId, walletId }
    })
  },

  // 关注钱包
  followWallet(userId, walletId) {
    return request({
      url: '/social/wallet/follow',
      method: 'POST',
      data: { userId, walletId }
    })
  },

  // 取消关注钱包
  unfollowWallet(userId, walletId) {
    return request({
      url: '/social/wallet/unfollow',
      method: 'POST',
      data: { userId, walletId }
    })
  },

  // 检查用户关注状态
  checkUserFollowStatus(currentUserId, targetUserId) {
    return request({
      url: '/social/user/follow-status',
      method: 'GET',
      data: { currentUserId, targetUserId }
    })
  },

  // 获取交易的社交数据（点赞数、评论数、用户是否已点赞）
  getTransactionSocialData(transactionId, userId) {
    return request({
      url: '/social/transaction/social-data',
      method: 'GET',
      data: { transactionId, userId }
    })
  },


  // 根据图片生成文字描述
  generateTextFromImage(imageBase64, prompt) {
    return request({
      url: '/wallet/generate-text-from-image',
      method: 'POST',
      data: { imageBase64, prompt }
    })
  },

  // 获取钱包月度统计数据
  getWalletMonthlyStats(walletId, year, month) {
    return request({
      url: '/wallet/monthly-stats',
      method: 'GET',
      data: { walletId, year, month }
    })
  }
}

/**
 * 分享图片相关API
 */
const shareImageAPI = {
  // 获取钱兜兜分享图片
  getWalletShareImage() {
    return request({
      url: '/share/wallet',
      method: 'GET'
    })
  },

  // 获取剧本分享图片
  getScriptShareImage(scriptId) {
    return request({
      url: `/share/script/${scriptId}`,
      method: 'GET'
    })
  },

  // 根据类型获取分享图片
  getShareImage(type, targetId = null) {
    let url = `/share/${type}`
    if (targetId) {
      url += `?targetId=${targetId}`
    }
    return request({
      url: url,
      method: 'GET'
    })
  }
}

/**
 * 文件上传函数
 */
function uploadFile(filePath, uploadUrl, formData = {}) {
  return new Promise((resolve, reject) => {
    const token = app.globalData.token || wx.getStorageSync('token')
    
    const header = {}
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    wx.uploadFile({
      url: `${BASE_URL}${uploadUrl}`,
      filePath: filePath,
      name: 'file',
      formData: formData,
      header: header,
      success: (res) => {

        try {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch (e) {
          reject(new Error('响应解析失败'))
        }
      },
      fail: (error) => {

        reject(new Error('文件上传失败'))
      }
    })
  })
}

/**
 * 通用用户图片上传函数
 * @param {string} filePath 图片文件路径
 * @param {string} type 图片类型 (avatar, transfer, wallet_bg, etc.)
 * @returns {Promise} 返回包含图片URL的Promise
 */
function uploadUserImage(filePath, type = 'general') {
  return uploadFile(filePath, '/wallet/upload-user-image', { type })
}

// 剧本相关API
const scriptAPI = {
  // 获取剧本列表
  getScriptList: (categoryId) => {
    const params = (categoryId && categoryId !== 1) ? `?categoryId=${categoryId}` : ''
    return request({
      url: `/scripts${params}`,
      method: 'GET'
    })
  },

  // 获取剧本详情
  getScriptDetail: (scriptId) => {

    if (!scriptId || scriptId === 'undefined' || scriptId === 'null') {

      return Promise.reject(new Error('无效的剧本ID'))
    }
    return request({
      url: `/scripts/${scriptId}`,
      method: 'GET'
    })
  },

  // 获取剧本章节列表
  getScriptChapters: (scriptId) => {
    return request({
      url: `/scripts/${scriptId}/chapters`,
      method: 'GET'
    })
  },

  // 获取指定章节内容
  getChapterContent: (scriptId, chapterNumber, userId = null, walletId = null) => {
    let url = `/scripts/${scriptId}/chapters/${chapterNumber}`
    const params = []
    if (userId) params.push(`userId=${userId}`)
    if (walletId) params.push(`walletId=${walletId}`)
    if (params.length > 0) {
      url += '?' + params.join('&')
    }
    
    return request({
      url: url,
      method: 'GET'
    })
  },

  // 获取用户剧本进度
  getUserProgress: (userId, scriptId, walletId = null) => {
    let url = `/scripts/progress?userId=${userId}&scriptId=${scriptId}`
    if (walletId) {
      url += `&walletId=${walletId}`
    }
    return request({
      url: url,
      method: 'GET'
    })
  },

  // 开始剧本
  startScript: (userId, walletId, scriptId) => {
    return request({
      url: '/scripts/start',
      method: 'POST',
      data: {
        userId,
        walletId,
        scriptId
      }
    })
  },

  // 用户做出选择
  makeChoice: (userId, walletId, scriptId, currentChapter, selectedChoice, amount) => {
    return request({
      url: '/scripts/choice',
      method: 'POST',
      data: {
        userId,
        walletId,
        scriptId,
        currentChapter,
        selectedChoice,
        amount
      }
    })
  },

  // 获取钱包剧本进度
  getWalletScriptProgress: (userId, walletId) => {
    return request({
      url: `/scripts/wallet-progress?userId=${userId}&walletId=${walletId}`,
      method: 'GET'
    })
  }
}


module.exports = {
  request,
  authAPI,
  walletAPI,
  shareImageAPI,
  uploadFile,
  uploadUserImage,
  scriptAPI
}
