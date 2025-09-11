// pages/ai-partner/ai-partner.js
const app = getApp()

Page({
  data: {
    selectedPartnerId: null,
    partners: [
      {
        id: 1,
        name: '小爱',
        description: '温柔体贴的AI女友，擅长理财规划',
        avatar: 'https://via.placeholder.com/100x100/ff69b4/ffffff?text=小爱',
        tags: ['温柔', '理财达人', '贴心']
      },
      {
        id: 2,
        name: '小智',
        description: '聪明睿智的AI男友，投资专家',
        avatar: 'https://via.placeholder.com/100x100/409eff/ffffff?text=小智',
        tags: ['聪明', '投资专家', '可靠']
      },
      {
        id: 3,
        name: '小萌',
        description: '活泼可爱的AI少女，节约小能手',
        avatar: 'https://via.placeholder.com/100x100/67c23a/ffffff?text=小萌',
        tags: ['活泼', '节约', '可爱']
      },
      {
        id: 4,
        name: '小酷',
        description: '冷静理性的AI帅哥，风险控制专家',
        avatar: 'https://via.placeholder.com/100x100/909399/ffffff?text=小酷',
        tags: ['理性', '风控', '专业']
      }
    ]
  },

  onLoad(options) {
    if (!app.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
    }
  },

  // 选择伴侣
  selectPartner(e) {
    const partnerId = parseInt(e.currentTarget.dataset.id)

    this.setData({
      selectedPartnerId: partnerId
    })

  },

  // 确认选择
  confirmSelection() {

    const { selectedPartnerId, partners } = this.data
    const selectedPartner = partners.find(p => p.id === selectedPartnerId)
    
    if (!selectedPartner) {
      wx.showToast({
        title: '请选择一个AI伴侣',
        icon: 'none'
      })
      return
    }

    wx.showModal({
      title: '确认选择',
      content: `确定选择${selectedPartner.name}作为你的AI伴侣吗？`,
      success: (res) => {
        if (res.confirm) {
          // 保存选择的AI伴侣
          wx.setStorageSync('selectedAiPartner', selectedPartner)
          
          wx.showToast({
            title: '选择成功',
            icon: 'success'
          })

          // 返回上一页
          setTimeout(() => {
            const pages = getCurrentPages()
            if (pages.length > 1) {
              wx.navigateBack()
            } else {
              // 如果没有上一页，返回首页
              wx.redirectTo({
                url: '/pages/home/home'
              })
            }
          }, 1500)
        }
      }
    })
  },

  // 阻止事件冒泡
  stopPropagation(e) {
    e.stopPropagation()
  }
})
