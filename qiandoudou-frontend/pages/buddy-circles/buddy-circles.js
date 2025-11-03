// pages/buddy-circles/buddy-circles.js
const app = getApp()
const { request } = require('../../utils/api.js')

Page({
  data: {
    walletName: '',
    circles: [],
    loading: true
  },

  onLoad(options) {
    // 检查登录状态
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    // 获取钱包名称
    const walletName = options.walletName || '我的搭子钱包'
    this.setData({
      walletName: decodeURIComponent(walletName)
    })

    // 加载圈子列表
    this.loadCircles()
  },

  // 加载圈子列表
  loadCircles() {
    this.setData({ loading: true })

    request({
      url: '/buddy/circles',
      method: 'GET'
    }).then(result => {
      console.log('圈子列表响应:', result)
      
      if (result.success && result.data) {
        // 处理圈子数据，添加成员预览
        const circles = result.data.map(circle => {
          return {
            ...circle,
            memberPreview: this.generateMemberPreview(circle.id)
          }
        })
        
        this.setData({
          circles: circles,
          loading: false
        })
      } else {
        this.setData({ loading: false })
        wx.showToast({
          title: '加载圈子列表失败',
          icon: 'none'
        })
      }
    }).catch(error => {
      console.error('加载圈子列表失败:', error)
      this.setData({ loading: false })
      wx.showToast({
        title: '网络错误，请重试',
        icon: 'none'
      })
    })
  },

  // 生成成员预览数据（临时方案，实际应该从后端获取）
  generateMemberPreview(circleId) {
    const previews = {
      1: [ // 西游记
        { name: '孙悟空', avatar: '/characters/wukong_avatar.png' },
        { name: '猪八戒', avatar: '/characters/bajie_avatar.png' },
        { name: '沙僧', avatar: '/characters/shaseng_avatar.png' },
        { name: '唐僧', avatar: '/characters/tangseng_avatar.png' }
      ],
      2: [ // 水浒传
        { name: '宋江', avatar: '/characters/songjiang_avatar.png' },
        { name: '林冲', avatar: '/characters/linchong_avatar.png' },
        { name: '武松', avatar: '/characters/wusong_avatar.png' }
      ],
      3: [ // 三国
        { name: '刘备', avatar: '/characters/liubei_avatar.png' },
        { name: '关羽', avatar: '/characters/guanyu_avatar.png' },
        { name: '张飞', avatar: '/characters/zhangfei_avatar.png' },
        { name: '曹操', avatar: '/characters/caocao_avatar.png' }
      ],
      4: [ // 盗墓笔记
        { name: '吴邪', avatar: '/characters/wuxie_avatar.png' },
        { name: '张起灵', avatar: '/characters/zhangqiling_avatar.png' },
        { name: '王胖子', avatar: '/characters/wangpangzi_avatar.png' }
      ]
    }
    
    return previews[circleId] || []
  },

  // 选择圈子
  selectCircle(e) {
    const circle = e.currentTarget.dataset.circle
    console.log('选择圈子:', circle)
    
    // 跳转到圈子成员页面
    wx.navigateTo({
      url: `/pages/buddy-characters/buddy-characters?mode=circle&circleId=${circle.id}&circleName=${encodeURIComponent(circle.name)}&walletName=${encodeURIComponent(this.data.walletName)}`
    })
  },

  // 返回上一页
  goBack() {
    wx.navigateBack({
      delta: 1
    })
  }
})
