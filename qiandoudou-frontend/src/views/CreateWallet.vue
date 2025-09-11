<template>
  <div class="mobile-container">
    <!-- 头部 -->
    <div class="header">
      <el-icon class="back-btn" @click="$router.back()">
        <ArrowLeft />
      </el-icon>
      <div class="header-title">创建钱包</div>
      <div></div>
    </div>

    <!-- 创建表单 -->
    <div class="create-form">
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="80px"
      >
        <!-- 钱包名称 -->
        <el-form-item label="钱包名称" prop="name">
          <el-input 
            v-model="form.name" 
            placeholder="给你的钱包起个名字"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <!-- 钱包类型 -->
        <el-form-item label="钱包类型" prop="type">
          <div class="wallet-type-container">
            <div 
              class="wallet-type-card"
              :class="{ active: form.type === 1 }"
              @click="form.type = 1"
            >
              <div class="type-icon personal">👤</div>
              <div class="type-info">
                <div class="type-name">个人钱包</div>
                <div class="type-desc">专属于你的储蓄空间</div>
              </div>
            </div>
            
            <div 
              class="wallet-type-card"
              :class="{ active: form.type === 2 }"
              @click="form.type = 2"
            >
              <div class="type-icon couple">💕</div>
              <div class="type-info">
                <div class="type-name">情侣钱包</div>
                <div class="type-desc">与AI伴侣一起储蓄</div>
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- AI伴侣选择（情侣钱包专用） -->
        <el-form-item 
          v-if="form.type === 2" 
          label="AI伴侣" 
          prop="aiPartnerId"
        >
          <div class="ai-partners">
            <div 
              v-for="partner in aiPartners" 
              :key="partner.id"
              class="partner-option"
              :class="{ active: form.aiPartnerId === partner.id }"
              @click="form.aiPartnerId = partner.id"
            >
              <el-avatar :src="partner.avatar" :size="50">
                {{ partner.name.charAt(0) }}
              </el-avatar>
              <div class="partner-name">{{ partner.name }}</div>
              <div class="partner-desc">{{ partner.personality.substring(0, 20) }}...</div>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <!-- 创建按钮 -->
      <div class="form-footer">
        <el-button 
          type="primary" 
          size="large" 
          class="create-btn"
          :loading="loading"
          @click="handleCreate"
        >
          创建钱包
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'

export default {
  name: 'CreateWallet',
  setup() {
    const router = useRouter()
    const store = useStore()
    
    const formRef = ref(null)
    const loading = ref(false)
    const aiPartners = ref([])
    
    // 表单数据
    const form = reactive({
      name: '',
      type: 1,
      backgroundImage: '',
      aiPartnerId: null
    })
    
    // 表单验证规则
    const rules = {
      name: [
        { required: true, message: '请输入钱包名称', trigger: 'blur' },
        { min: 2, max: 20, message: '钱包名称长度为2-20个字符', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择钱包类型', trigger: 'change' }
      ],
      aiPartnerId: [
        {
          validator: (rule, value, callback) => {
            if (form.type === 2 && !value) {
              callback(new Error('请选择AI伴侣'))
            } else {
              callback()
            }
          },
          trigger: 'change'
        }
      ]
    }
    
    // 选中的AI伴侣
    const selectedPartner = computed(() => {
      return aiPartners.value.find(p => p.id === form.aiPartnerId)
    })
    
    // 处理创建
    const handleCreate = async () => {
      if (!formRef.value) return
      
      await formRef.value.validate(async (valid) => {
        if (valid) {
          loading.value = true
          
          const walletData = {
            name: form.name,
            type: form.type,
            backgroundImage: form.type === 1 ? 'gradient1' : 'gradient2', // 个人钱包用蓝色，情侣钱包用粉色
            aiPartnerId: form.type === 2 ? form.aiPartnerId : null
          }
          
          const result = await store.dispatch('createWallet', walletData)
          
          if (result.success) {
            ElMessage.success('钱包创建成功')
            router.back()
          } else {
            ElMessage.error(result.message)
          }
          
          loading.value = false
        }
      })
    }
    
    // 加载AI伴侣列表
    const loadAiPartners = () => {
      // 模拟AI伴侣数据
      aiPartners.value = [
        {
          id: 1,
          name: '小雅',
          avatar: '/img/ai_partners/xiaoya.png',
          personality: '温柔体贴，善解人意，喜欢鼓励用户储蓄',
          gender: 2
        },
        {
          id: 2,
          name: '阿俊',
          avatar: '/img/ai_partners/ajun.png',
          personality: '阳光帅气，幽默风趣，擅长理财规划',
          gender: 1
        },
        {
          id: 3,
          name: '小萌',
          avatar: '/img/ai_partners/xiaomeng.png',
          personality: '可爱活泼，充满活力，喜欢和用户分享生活小窍门',
          gender: 2
        },
        {
          id: 4,
          name: '子轩',
          avatar: '/img/ai_partners/zixuan.png',
          personality: '成熟稳重，理性分析，专业的理财顾问风格',
          gender: 1
        }
      ]
      
      // 默认选择第一个
      if (aiPartners.value.length > 0) {
        form.aiPartnerId = aiPartners.value[0].id
      }
    }
    
    onMounted(() => {
      loadAiPartners()
    })
    
    return {
      formRef,
      loading,
      form,
      rules,
      aiPartners,
      selectedPartner,
      handleCreate
    }
  }
}
</script>

<style scoped>
.back-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.back-btn:hover {
  color: #409eff;
}

.create-form {
  padding: 20px;
  padding-bottom: 100px;
}

.wallet-type-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
  width: 100%;
}

.wallet-type-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.wallet-type-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.wallet-type-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.type-icon.personal {
  background: linear-gradient(135deg, #fa6402 0%, #764ba2 100%);
}

.type-icon.couple {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.type-info {
  flex: 1;
}

.type-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.type-desc {
  font-size: 14px;
  color: #909399;
}

.ai-partners {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.partner-option {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 15px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.partner-option:hover {
  border-color: #409eff;
}

.partner-option.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.partner-name {
  font-size: 14px;
  font-weight: 600;
  margin: 8px 0 4px 0;
  color: #303133;
}

.partner-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.background-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.bg-option {
  aspect-ratio: 16/9;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
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
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.bg-option:hover .bg-overlay {
  opacity: 1;
}

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

.wallet-preview {
  width: 100%;
  height: 140px;
  border-radius: 16px;
  padding: 20px;
  color: white;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #fa6402 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
}

.wallet-preview.couple {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
}

.preview-content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.preview-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.preview-type {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 8px;
}

.preview-balance {
  margin: 10px 0;
}

.balance-label {
  display: block;
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.balance-amount {
  font-size: 24px;
  font-weight: 700;
}

.preview-partner {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.form-footer {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 414px;
  padding: 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.create-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  border-radius: 25px;
  background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
  border: none;
}

@media (max-width: 414px) {
  .create-form {
    padding: 15px;
  }
  
  .ai-partners {
    grid-template-columns: 1fr;
  }
  
  .background-options {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
