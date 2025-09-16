<template>
  <div class="mobile-login-container">
    <!-- 顶部背景区域 -->
    <div class="login-header">
      <div class="logo-section">
        <div class="app-logo">💰</div>
        <h1 class="app-title">钱兜兜</h1>
        <p class="app-subtitle">智能理财助手</p>
      </div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card-mobile">
      <!-- 登录表单 -->
      <div class="login-form-mobile">
        <div class="form-item">
          <input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            class="mobile-input"
            type="text"
          />
        </div>
        
        <div class="form-item">
          <input
            v-model="loginForm.password"
            placeholder="请输入密码"
            class="mobile-input"
            type="password"
            @keyup.enter="handleLogin"
          />
        </div>
        
        <button 
          class="mobile-btn primary"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </div>

      <!-- 演示登录 -->
      <div class="demo-section-mobile">
        <div class="divider-text">或者</div>
        <button 
          class="mobile-btn demo"
          :disabled="loading"
          @click="handleDemoLogin"
        >
          演示登录
        </button>
        <p class="demo-tip-mobile">点击演示登录可以直接体验应用功能</p>
      </div>

      <!-- 注册链接 -->
      <div class="register-section-mobile">
        <span>还没有账号？</span>
        <button class="link-btn" @click="showRegisterDialog = true">立即注册</button>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog 
      v-model="showRegisterDialog" 
      title="用户注册" 
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="registerFormRef" 
        :model="registerForm" 
        :rules="registerRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password 
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="请确认密码" 
            show-password 
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showRegisterDialog = false">取消</el-button>
          <el-button type="primary" @click="handleRegister">注册</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

export default {
  name: 'Login',
  setup() {
    const store = useStore()
    const router = useRouter()
    
    const loginFormRef = ref(null)
    const registerFormRef = ref(null)
    const loading = ref(false)
    const showRegisterDialog = ref(false)
    
    // 登录表单
    const loginForm = reactive({
      username: '',
      password: ''
    })
    
    // 注册表单
    const registerForm = reactive({
      username: '',
      nickname: '',
      phone: '',
      password: '',
      confirmPassword: ''
    })
    
    // 登录表单验证规则
    const loginRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }
    
    // 注册表单验证规则
    const registerRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
      ],
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== registerForm.password) {
              callback(new Error('两次输入的密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    // 处理登录
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      await loginFormRef.value.validate(async (valid) => {
        if (valid) {
          loading.value = true
          const result = await store.dispatch('login', {
            username: loginForm.username,
            password: loginForm.password
          })
          
          if (result.success) {
            ElMessage.success('登录成功')
            router.push('/home')
          } else {
            ElMessage.error(result.message)
          }
          loading.value = false
        }
      })
    }
    
    // 处理演示登录
    const handleDemoLogin = async () => {
      loading.value = true
      const result = await store.dispatch('demoLogin')
      
      if (result.success) {
        ElMessage.success('演示登录成功')
        router.push('/home')
      } else {
        ElMessage.error(result.message)
      }
      loading.value = false
    }
    
    // 处理注册
    const handleRegister = async () => {
      if (!registerFormRef.value) return
      
      await registerFormRef.value.validate(async (valid) => {
        if (valid) {
          try {
            const response = await api.post('/auth/register', {
              username: registerForm.username,
              nickname: registerForm.nickname,
              phone: registerForm.phone,
              password: registerForm.password
            })
            
            if (response.data.code === 200) {
              ElMessage.success('注册成功，请登录')
              showRegisterDialog.value = false
              // 清空注册表单
              Object.keys(registerForm).forEach(key => {
                registerForm[key] = ''
              })
            } else {
              ElMessage.error(response.data.message)
            }
          } catch (error) {
            ElMessage.error('注册失败')
          }
        }
      })
    }
    
    return {
      loginFormRef,
      registerFormRef,
      loading,
      showRegisterDialog,
      loginForm,
      registerForm,
      loginRules,
      registerRules,
      handleLogin,
      handleDemoLogin,
      handleRegister
    }
  }
}
</script>

<style scoped>
/* 移动端登录样式 - 微信小程序风格 */
.mobile-login-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #fa6402 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  position: relative;
}

.login-header {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px 40px;
}

.logo-section {
  text-align: center;
  color: white;
}

.app-logo {
  font-size: 80px;
  margin-bottom: 20px;
  display: block;
}

.app-title {
  font-size: 32px;
  font-weight: 300;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.app-subtitle {
  font-size: 16px;
  opacity: 0.8;
  margin: 0;
  font-weight: 300;
}

.login-card-mobile {
  background: white;
  border-radius: 20px 20px 0 0;
  padding: 40px 30px 60px;
  min-height: 400px;
}

.login-form-mobile {
  margin-bottom: 40px;
}

.form-item {
  margin-bottom: 20px;
}

.mobile-input {
  width: 100%;
  height: 50px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 16px;
  background: #f8f9fa;
  transition: all 0.3s;
  box-sizing: border-box;
}

.mobile-input:focus {
  outline: none;
  border-color: #409eff;
  background: white;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.mobile-input::placeholder {
  color: #c0c4cc;
  font-size: 14px;
}

.mobile-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.mobile-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mobile-btn.primary {
  background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
}

.mobile-btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

.mobile-btn.primary:active {
  transform: translateY(0);
}

.demo-section-mobile {
  margin-bottom: 30px;
}

.divider-text {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin: 20px 0;
  position: relative;
}

.divider-text::before,
.divider-text::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: #e4e7ed;
}

.divider-text::before {
  left: 0;
}

.divider-text::after {
  right: 0;
}

.mobile-btn.demo {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}

.mobile-btn.demo:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

.demo-tip-mobile {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin: 12px 0 0 0;
  line-height: 1.4;
}

.register-section-mobile {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 20px;
}

.link-btn {
  background: none;
  border: none;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  padding: 0 4px;
  text-decoration: underline;
}

.link-btn:hover {
  color: #66b3ff;
}

/* 注册对话框样式 */
.dialog-footer {
  text-align: right;
  padding-top: 20px;
}

/* 响应式适配 */
@media (max-width: 375px) {
  .login-card-mobile {
    padding: 30px 20px 50px;
  }
  
  .app-logo {
    font-size: 60px;
  }
  
  .app-title {
    font-size: 28px;
  }
  
  .mobile-input {
    height: 44px;
    font-size: 16px;
  }
  
  .mobile-btn {
    height: 44px;
    font-size: 15px;
  }
}

/* 微信小程序风格的阴影 */
.login-card-mobile {
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
}

/* 输入框聚焦动画 */
.mobile-input:focus {
  animation: inputFocus 0.3s ease;
}

@keyframes inputFocus {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
  100% {
    transform: scale(1);
  }
}

/* 按钮点击动画 */
.mobile-btn:active {
  animation: buttonPress 0.1s ease;
}

@keyframes buttonPress {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(0.98);
  }
  100% {
    transform: scale(1);
  }
}
</style>
