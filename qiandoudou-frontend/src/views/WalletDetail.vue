<template>
  <div class="mobile-container">
    <!-- 头部 -->
    <div class="header">
      <el-icon class="back-btn" @click="$router.back()">
        <ArrowLeft />
      </el-icon>
      <div class="header-title">{{ wallet?.name || '钱包详情' }}</div>
      <el-button type="text" class="change-bg-btn" @click="showBackgroundModal = true">
        更换背景
      </el-button>
    </div>

    <!-- 钱包卡片 -->
    <div v-if="wallet" class="wallet-hero">
      <div 
        class="wallet-card large"
        :class="{ 'couple': wallet.type === 2 }"
        :style="{ background: getWalletBackgroundStyle() }"
      >
        
        <div class="wallet-info">
          <div class="wallet-header">
            <h2 class="wallet-name">{{ wallet.name }}</h2>
            <div class="wallet-type">
              {{ wallet.type === 1 ? '个人钱包' : '情侣钱包' }}
            </div>
          </div>
          
          <div class="wallet-balance">
            <span class="balance-label">当前余额</span>
            <span class="balance-amount">¥{{ formatAmount(wallet.balance) }}</span>
          </div>
          
          <!-- AI伴侣信息 -->
          <div v-if="wallet.type === 2 && aiPartner" class="ai-partner">
            <el-avatar :src="aiPartner.avatar" :size="32">
              {{ aiPartner.name?.charAt(0) }}
            </el-avatar>
            <div class="partner-info">
              <div class="partner-name">{{ aiPartner.name }}</div>
              <div class="partner-status">在线</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮 - 微信小程序风格 -->
    <div class="action-buttons-mobile">
      <button 
        class="action-btn-mobile transfer-in"
        @click="showTransferDialog('in')"
      >
        <span class="action-icon">+</span>
        <span class="action-text">转入</span>
      </button>
      
      <button 
        class="action-btn-mobile transfer-out"
        @click="showTransferDialog('out')"
      >
        <span class="action-icon">-</span>
        <span class="action-text">转出</span>
      </button>
      
      <button 
        class="action-btn-mobile script-save"
        @click="goToScripts"
      >
        <span class="action-icon">📖</span>
        <span class="action-text">剧本攒</span>
      </button>
    </div>

    <!-- 交易记录 -->
    <div class="transactions-section">
      <div class="section-header">
        <h3>交易记录</h3>
        <el-button type="text" size="small">查看全部</el-button>
      </div>
      
      <div v-loading="transactionsLoading" class="transactions-list">
        <div v-if="transactions.length === 0" class="empty-state">
          <el-icon class="icon"><DocumentCopy /></el-icon>
          <div class="text">暂无交易记录</div>
        </div>
        
        <div 
          v-for="transaction in transactions" 
          :key="transaction.id"
          class="transaction-item"
        >
          <div class="transaction-icon">
            <el-icon v-if="transaction.type === 1" class="income-icon">
              <Plus />
            </el-icon>
            <el-icon v-else-if="transaction.type === 2" class="expense-icon">
              <Minus />
            </el-icon>
            <el-icon v-else class="script-icon">
              <Reading />
            </el-icon>
          </div>
          
          <div class="transaction-info">
            <div class="transaction-desc">{{ transaction.description }}</div>
            <div class="transaction-time">{{ formatTime(transaction.create_time) }}</div>
          </div>
          
          <div class="transaction-amount" :class="getAmountClass(transaction.type)">
            {{ getAmountPrefix(transaction.type) }}¥{{ formatAmount(transaction.amount) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 转账对话框 -->
    <el-dialog 
      v-model="showTransferModal" 
      :title="transferType === 'in' ? '转入资金' : '转出资金'"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="transferFormRef" 
        :model="transferForm" 
        :rules="transferRules"
        label-width="80px"
      >
        <el-form-item label="金额" prop="amount">
          <el-input 
            v-model="transferForm.amount" 
            type="number" 
            placeholder="请输入金额"
            :prefix-icon="transferType === 'in' ? 'Plus' : 'Minus'"
          >
            <template #append>元</template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="说明" prop="description">
          <el-input 
            v-model="transferForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入转账说明"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <div v-if="transferType === 'out'" class="balance-tip">
          当前余额：¥{{ formatAmount(wallet?.balance || 0) }}
        </div>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showTransferModal = false">取消</el-button>
          <el-button 
            type="primary" 
            :loading="transferLoading"
            @click="handleTransfer"
          >
            确认{{ transferType === 'in' ? '转入' : '转出' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 更换背景对话框 -->
    <el-dialog 
      v-model="showBackgroundModal" 
      title="选择钱包背景" 
      width="90%"
      :close-on-click-modal="false"
    >
      <div class="background-upload-section">
        <!-- 图片上传区域 -->
        <el-upload
          ref="uploadRef"
          class="background-uploader"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          :before-upload="handleBeforeUpload"
          :on-change="handleImageChange"
          :disabled="!!selectedImageUrl"
        >
          <div v-if="!selectedImageUrl" class="upload-placeholder">
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              点击或拖拽上传背景图片
            </div>
            <div class="el-upload__tip">
              支持 JPG、PNG 格式，建议尺寸 16:9
            </div>
          </div>
        </el-upload>

        <!-- 图片预览和裁剪区域（独立于上传组件） -->
        <div v-if="selectedImageUrl" class="image-preview-section">
          <div class="image-crop-container">
            <img 
              ref="cropImageRef"
              :src="selectedImageUrl" 
              alt="预览图"
              :style="getImageCropStyle()"
              @mousedown="startDrag"
              @wheel="handleWheel"
            />
          </div>
          <div class="crop-controls">
            <div class="control-row">
              <span>缩放:</span>
              <el-slider 
                v-model="imageScale" 
                :min="50" 
                :max="200" 
                :step="5"
                @input="updateImageScale"
                @click.stop
              />
              <span>{{ imageScale }}%</span>
            </div>
            <div class="control-row">
              <span>位置:</span>
              <el-button size="small" @click.stop="resetPosition">重置</el-button>
              <el-button type="text" @click.stop="removeImage">
                <el-icon><Delete /></el-icon>
                重新选择
              </el-button>
            </div>
          </div>
        </div>

        <!-- 预设背景选项 -->
        <el-divider>或选择预设背景</el-divider>
        <div class="preset-backgrounds">
          <div 
            v-for="bg in backgroundOptions" 
            :key="bg.value"
            class="preset-option"
            :class="{ active: selectedBackground === bg.value && !selectedImageUrl }"
            :style="{ background: bg.gradient }"
            @click="selectPresetBackground(bg.value)"
          >
            <div class="preset-overlay">
              <el-icon v-if="selectedBackground === bg.value && !selectedImageUrl" class="check-icon">
                <Check />
              </el-icon>
            </div>
            <div class="preset-name">{{ bg.name }}</div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelBackgroundChange">取消</el-button>
          <el-button 
            type="primary" 
            :loading="backgroundLoading"
            :disabled="!selectedImageUrl && !selectedBackground"
            @click="changeBackground"
          >
            确认更换
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

export default {
  name: 'WalletDetail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    
    const walletId = computed(() => {
      const idStr = route.params.id
      console.log('路由参数ID:', idStr, '类型:', typeof idStr)
      
      // 检查是否是有效的数字字符串
      if (!idStr || !/^\d+$/.test(idStr)) {
        console.log('钱包ID无效，路由参数:', idStr)
        return null
      }
      
      // 完全使用字符串，避免任何数字转换导致的精度丢失
      console.log('最终使用的钱包ID字符串:', idStr)
      return idStr
    })
    const wallet = ref(null)
    const aiPartner = ref(null)
    const transactions = ref([])
    const transactionsLoading = ref(false)
    
    const showTransferModal = ref(false)
    const transferType = ref('in') // 'in' | 'out'
    const transferFormRef = ref(null)
    const transferLoading = ref(false)
    
    // 背景相关
    const showBackgroundModal = ref(false)
    const selectedBackground = ref('')
    const backgroundLoading = ref(false)
    const selectedImageUrl = ref('')
    const selectedImageFile = ref(null)
    const uploadRef = ref(null)
    const cropImageRef = ref(null)
    
    // 图片裁剪相关
    const imageScale = ref(100)
    const imagePositionX = ref(0)
    const imagePositionY = ref(0)
    const isDragging = ref(false)
    const dragStartX = ref(0)
    const dragStartY = ref(0)
    const startPositionX = ref(0)
    const startPositionY = ref(0)
    
    // 背景选项
    const backgroundOptions = [
      { value: 'gradient1', name: '蓝紫渐变', gradient: 'linear-gradient(135deg, #fa6402 0%, #764ba2 100%)' },
      { value: 'gradient2', name: '粉红渐变', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
      { value: 'gradient3', name: '绿色渐变', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
      { value: 'gradient4', name: '橙色渐变', gradient: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)' },
      { value: 'gradient5', name: '紫色渐变', gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' },
      { value: 'gradient6', name: '金色渐变', gradient: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)' }
    ]
    
    // 转账表单
    const transferForm = reactive({
      amount: '',
      description: ''
    })
    
    // 转账表单验证规则
    const transferRules = computed(() => {
      const rules = {
        amount: [
          { required: true, message: '请输入金额', trigger: 'blur' },
          { 
            validator: (rule, value, callback) => {
              const amount = parseFloat(value)
              if (isNaN(amount) || amount <= 0) {
                callback(new Error('金额必须大于0'))
              } else if (transferType.value === 'out' && amount > parseFloat(wallet.value?.balance || 0)) {
                callback(new Error('余额不足'))
              } else {
                callback()
              }
            }, 
            trigger: 'blur' 
          }
        ],
        description: [
          { required: true, message: '请输入说明', trigger: 'blur' }
        ]
      }
      return rules
    })
    
    // 格式化金额
    const formatAmount = (amount) => {
      return parseFloat(amount).toFixed(2)
    }
    
    // 格式化时间
    const formatTime = (time) => {
      return new Date(time).toLocaleString()
    }
    
    // 获取金额样式类
    const getAmountClass = (type) => {
      switch (type) {
        case 1: return 'income'
        case 2: return 'expense'
        case 3: return 'script'
        default: return ''
      }
    }
    
    // 获取金额前缀
    const getAmountPrefix = (type) => {
      switch (type) {
        case 1: return '+'
        case 2: return '-'
        case 3: return '+'
        default: return ''
      }
    }
    
    // 显示转账对话框
    const showTransferDialog = (type) => {
      transferType.value = type
      transferForm.amount = ''
      transferForm.description = ''
      showTransferModal.value = true
    }
    
    // 处理转账
    const handleTransfer = async () => {
      if (!transferFormRef.value) return
      
      await transferFormRef.value.validate(async (valid) => {
        if (valid) {
          transferLoading.value = true
          
          console.log('转账参数:', {
            walletId: walletId.value,
            amount: parseFloat(transferForm.amount),
            description: transferForm.description,
            type: transferType.value
          })
          
          const action = transferType.value === 'in' ? 'transferIn' : 'transferOut'
          const result = await store.dispatch(action, {
            walletId: walletId.value,
            amount: parseFloat(transferForm.amount),
            description: transferForm.description
          })
          
          if (result.success) {
            ElMessage.success(`${transferType.value === 'in' ? '转入' : '转出'}成功`)
            showTransferModal.value = false
            // 刷新钱包信息和交易记录
            await loadWalletDetail()
            await loadTransactions()
          } else {
            ElMessage.error(result.message)
          }
          
          transferLoading.value = false
        }
      })
    }
    
    // 跳转到剧本页面
    const goToScripts = () => {
      ElMessage.info('剧本攒功能开发中...')
      // router.push(`/scripts/${walletId.value}`)
    }
    
    // 获取钱包背景样式
    const getWalletBackgroundStyle = () => {
      if (!wallet.value?.background_image) {
        return wallet.value?.type === 2 ? 
          'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' : 
          'linear-gradient(135deg, #fa6402 0%, #764ba2 100%)'
      }
      
      // 检查是否是预设渐变
      const bg = backgroundOptions.find(bg => bg.value === wallet.value.background_image)
      if (bg) {
        return bg.gradient
      }
      
      // 如果是图片URL，使用背景图片
      if (wallet.value.background_image.startsWith('data:') || wallet.value.background_image.startsWith('http') || wallet.value.background_image.startsWith('/uploads/')) {
        return `url(${wallet.value.background_image})`
      }
      
      return 'linear-gradient(135deg, #fa6402 0%, #764ba2 100%)'
    }
    
    // 处理图片上传前的验证
    const handleBeforeUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt5M) {
        ElMessage.error('图片大小不能超过 5MB!')
        return false
      }
      return true
    }
    
    // 处理图片选择
    const handleImageChange = (file) => {
      console.log('选择的文件:', file)
      
      if (file.raw) {
        selectedImageFile.value = file.raw
        
        // 重置裁剪参数
        imageScale.value = 100
        imagePositionX.value = 0
        imagePositionY.value = 0
        
        // 创建图片预览URL
        const reader = new FileReader()
        reader.onload = (e) => {
          selectedImageUrl.value = e.target.result
          selectedBackground.value = '' // 清空预设背景选择
          console.log('图片预览URL创建成功')
        }
        reader.readAsDataURL(file.raw)
      }
    }
    
    // 获取图片裁剪样式
    const getImageCropStyle = () => {
      return {
        transform: `translate(${imagePositionX.value}px, ${imagePositionY.value}px) scale(${imageScale.value / 100})`,
        transformOrigin: 'center center',
        cursor: isDragging.value ? 'grabbing' : 'grab',
        transition: isDragging.value ? 'none' : 'transform 0.2s ease'
      }
    }
    
    // 更新图片缩放
    const updateImageScale = (newScale) => {
      imageScale.value = newScale
      // 缩放时重新计算边界限制
      constrainPosition()
    }
    
    // 开始拖拽
    const startDrag = (e) => {
      e.preventDefault()
      isDragging.value = true
      dragStartX.value = e.clientX
      dragStartY.value = e.clientY
      startPositionX.value = imagePositionX.value
      startPositionY.value = imagePositionY.value
      
      // 添加全局事件监听
      document.addEventListener('mousemove', onDrag)
      document.addEventListener('mouseup', stopDrag)
    }
    
    // 拖拽中
    const onDrag = (e) => {
      if (!isDragging.value) return
      
      const deltaX = e.clientX - dragStartX.value
      const deltaY = e.clientY - dragStartY.value
      
      imagePositionX.value = startPositionX.value + deltaX
      imagePositionY.value = startPositionY.value + deltaY
      
      // 限制在边界内
      constrainPosition()
    }
    
    // 停止拖拽
    const stopDrag = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', onDrag)
      document.removeEventListener('mouseup', stopDrag)
    }
    
    // 处理滚轮缩放
    const handleWheel = (e) => {
      e.preventDefault()
      const delta = e.deltaY > 0 ? -5 : 5
      const newScale = Math.max(50, Math.min(200, imageScale.value + delta))
      imageScale.value = newScale
      constrainPosition()
    }
    
    // 限制图片位置在容器边界内
    const constrainPosition = () => {
      if (!cropImageRef.value) return
      
      const container = cropImageRef.value.parentElement
      const containerRect = container.getBoundingClientRect()
      const imageRect = cropImageRef.value.getBoundingClientRect()
      
      const containerWidth = containerRect.width
      const containerHeight = containerRect.height
      const imageWidth = imageRect.width
      const imageHeight = imageRect.height
      
      // 计算最大偏移量
      const maxOffsetX = Math.max(0, (imageWidth - containerWidth) / 2)
      const maxOffsetY = Math.max(0, (imageHeight - containerHeight) / 2)
      
      // 限制位置
      imagePositionX.value = Math.max(-maxOffsetX, Math.min(maxOffsetX, imagePositionX.value))
      imagePositionY.value = Math.max(-maxOffsetY, Math.min(maxOffsetY, imagePositionY.value))
    }
    
    // 重置位置
    const resetPosition = () => {
      imageScale.value = 100
      imagePositionX.value = 0
      imagePositionY.value = 0
    }
    
    // 移除选中的图片
    const removeImage = () => {
      selectedImageUrl.value = ''
      selectedImageFile.value = null
      selectedBackground.value = wallet.value?.background_image || 'gradient1'
    }
    
    // 选择预设背景
    const selectPresetBackground = (bgValue) => {
      selectedBackground.value = bgValue
      selectedImageUrl.value = '' // 清空图片选择
      selectedImageFile.value = null
    }
    
    // 取消背景更换
    const cancelBackgroundChange = () => {
      showBackgroundModal.value = false
      selectedImageUrl.value = ''
      selectedImageFile.value = null
      selectedBackground.value = wallet.value?.background_image || 'gradient1'
    }
    
    // 更换背景
    const changeBackground = async () => {
      if (!selectedImageUrl.value && !selectedBackground.value) {
        ElMessage.warning('请选择背景图片或预设背景')
        return
      }
      
      try {
        backgroundLoading.value = true
        
        let backgroundImage = ''
        
        if (selectedImageUrl.value) {
          // 如果选择了本地图片，直接使用base64数据（demo版本）
          backgroundImage = selectedImageUrl.value
        } else {
          // 使用预设背景
          backgroundImage = selectedBackground.value
        }
        
        // 调用后端API更新钱包背景
        console.log('更换背景请求参数:', {
          walletId: walletId.value,
          backgroundImage: backgroundImage.substring(0, 50) + '...'
        })
        
        const response = await api.put('/wallet/update-background', {
          walletId: walletId.value,
          backgroundImage: backgroundImage
        })
        
        if (response.data.code === 200) {
          // 更新本地钱包信息
          if (wallet.value) {
            wallet.value.background_image = backgroundImage
          }
          
          // 同步更新Vuex store中的钱包数据
          store.commit('UPDATE_WALLET_BACKGROUND', {
            walletId: walletId.value,
            backgroundImage: backgroundImage
          })
          
          ElMessage.success('背景更换成功')
          showBackgroundModal.value = false
          
          // 清空选择
          selectedImageUrl.value = ''
          selectedImageFile.value = null
          selectedBackground.value = ''
        } else {
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        ElMessage.error('更换背景失败')
        console.error('更换背景错误:', error)
      } finally {
        backgroundLoading.value = false
      }
    }
    
    // 加载钱包详情
    const loadWalletDetail = async () => {
      try {
        const response = await api.get('/wallet/detail', {
          params: { walletId: walletId.value }
        })
        
        if (response.data.code === 200) {
          wallet.value = response.data.data
          
          // 初始化选中的背景
          selectedBackground.value = wallet.value.background_image || 'gradient1'
          
          // 如果是情侣钱包，加载AI伴侣信息
          if (wallet.value.type === 2 && wallet.value.ai_partner_id) {
            // TODO: 加载AI伴侣信息
            aiPartner.value = {
              id: wallet.value.ai_partner_id,
              name: '小雅',
              avatar: '/img/ai_partners/xiaoya.png'
            }
          }
        }
      } catch (error) {
        console.error('加载钱包详情失败:', error)
        ElMessage.error('加载钱包详情失败')
      }
    }
    
    // 加载交易记录
    const loadTransactions = async () => {
      try {
        transactionsLoading.value = true
        const response = await api.get('/wallet/transactions', {
          params: { walletId: walletId.value }
        })
        
        if (response.data.code === 200) {
          transactions.value = response.data.data.slice(0, 10) // 只显示最近10条
        }
      } catch (error) {
        console.error('加载交易记录失败:', error)
      } finally {
        transactionsLoading.value = false
      }
    }
    
    // 监听钱包ID变化
    watch(walletId, () => {
      if (walletId.value) {
        loadWalletDetail()
        loadTransactions()
      }
    }, { immediate: true })
    
    onMounted(() => {
      if (walletId.value) {
        loadWalletDetail()
        loadTransactions()
      }
    })
    
    return {
      wallet,
      aiPartner,
      transactions,
      transactionsLoading,
      showTransferModal,
      transferType,
      transferFormRef,
      transferForm,
      transferRules,
      transferLoading,
      // 背景相关
      showBackgroundModal,
      selectedBackground,
      backgroundLoading,
      selectedImageUrl,
      selectedImageFile,
      uploadRef,
      cropImageRef,
      // 裁剪相关
      imageScale,
      imagePositionX,
      imagePositionY,
      getImageCropStyle,
      updateImageScale,
      startDrag,
      handleWheel,
      resetPosition,
      // 背景选项和函数
      backgroundOptions,
      getWalletBackgroundStyle,
      handleBeforeUpload,
      handleImageChange,
      removeImage,
      selectPresetBackground,
      cancelBackgroundChange,
      changeBackground,
      // 其他函数
      formatAmount,
      formatTime,
      getAmountClass,
      getAmountPrefix,
      showTransferDialog,
      handleTransfer,
      goToScripts
    }
  }
}
</script>

<style scoped>
.back-btn, .more-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.back-btn:hover, .more-btn:hover {
  color: #409eff;
}

.wallet-hero {
  padding: 20px;
}

.wallet-card.large {
  min-height: 180px;
  padding: 25px;
}

.wallet-card.large .wallet-name {
  font-size: 22px;
  margin-bottom: 20px;
}

.wallet-card.large .balance-amount {
  font-size: 32px;
}

.ai-partner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.partner-info {
  flex: 1;
}

.partner-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 2px;
}

.partner-status {
  font-size: 12px;
  opacity: 0.8;
}

/* 微信小程序风格的操作按钮 */
.action-buttons-mobile {
  display: flex;
  gap: 8px;
  padding: 0 16px;
  margin-bottom: 20px;
}

.action-btn-mobile {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 400;
  color: white;
}

.action-btn-mobile:active {
  transform: scale(0.95);
}

.action-icon {
  font-size: 16px;
  font-weight: 600;
}

.action-text {
  font-size: 11px;
}

.transfer-in {
  background: #07c160;
}

.transfer-in:active {
  background: #06ad56;
}

.transfer-out {
  background: #ff6b6b;
}

.transfer-out:active {
  background: #ff5252;
}

.script-save {
  background: #409eff;
}

.script-save:active {
  background: #337ecc;
}

.transactions-section {
  background: #fff;
  border-radius: 20px 20px 0 0;
  padding: 20px;
  flex: 1;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.transactions-list {
  max-height: 400px;
  overflow-y: auto;
}

.transaction-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.transaction-item:last-child {
  border-bottom: none;
}

.transaction-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.income-icon {
  color: #67c23a;
  background: #f0f9ff;
}

.expense-icon {
  color: #f56c6c;
  background: #fef0f0;
}

.script-icon {
  color: #409eff;
  background: #ecf5ff;
}

.transaction-info {
  flex: 1;
}

.transaction-desc {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.transaction-time {
  font-size: 12px;
  color: #909399;
}

.transaction-amount {
  font-size: 16px;
  font-weight: 600;
}

.transaction-amount.income {
  color: #67c23a;
}

.transaction-amount.expense {
  color: #f56c6c;
}

.transaction-amount.script {
  color: #409eff;
}

.balance-tip {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 10px;
}

.change-bg-btn {
  color: #409eff !important;
  font-size: 14px;
}

.background-upload-section {
  padding: 20px 0;
}

.background-uploader {
  width: 100%;
  margin-bottom: 20px;
}

.upload-placeholder {
  text-align: center;
  padding: 40px 20px;
}

.el-icon--upload {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

.el-upload__text {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.el-upload__tip {
  font-size: 12px;
  color: #909399;
}

.image-preview-section {
  margin-top: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.image-crop-container {
  width: 100%;
  height: 200px;
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: #f0f0f0;
  user-select: none;
}

.image-crop-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
}

.crop-controls {
  padding: 15px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.control-row:last-child {
  margin-bottom: 0;
}

.control-row span {
  font-size: 14px;
  color: #606266;
  min-width: 50px;
}

.control-row .el-slider {
  flex: 1;
  margin: 0 10px;
}

.preset-backgrounds {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.preset-option {
  aspect-ratio: 16/9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.preset-option:hover {
  transform: scale(1.05);
}

.preset-option.active {
  border-color: #409eff;
}

.preset-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.preset-option:hover .preset-overlay,
.preset-option.active .preset-overlay {
  opacity: 1;
}

.preset-name {
  position: absolute;
  bottom: 5px;
  left: 0;
  right: 0;
  text-align: center;
  color: white;
  font-size: 10px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.background-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin: 20px 0;
}

.bg-option {
  aspect-ratio: 16/9;
  border-radius: 12px;
  cursor: pointer;
  position: relative;
  border: 3px solid transparent;
  transition: all 0.3s;
}

.bg-option:hover {
  transform: scale(1.05);
}

.bg-option.active {
  border-color: #409eff;
}

.bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.bg-option:hover .bg-overlay,
.bg-option.active .bg-overlay {
  opacity: 1;
}

.check-icon {
  color: white;
  font-size: 24px;
}

.bg-name {
  position: absolute;
  bottom: 5px;
  left: 0;
  right: 0;
  text-align: center;
  color: white;
  font-size: 10px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.dialog-footer {
  text-align: right;
}

@media (max-width: 414px) {
  .wallet-hero {
    padding: 15px;
  }
  
  .action-buttons {
    padding: 0 15px;
  }
  
  .transactions-section {
    padding: 15px;
  }
}
</style>
