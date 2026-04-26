<template>
  <div class="mersi-auth-container dark-theme" @mousemove="handleMouseMove">
    
    <div class="custom-cursor-aura" :style="{ transform: `translate3d(${mouseX}px, ${mouseY}px, 0)` }"></div>

    <div class="back-home-btn" @click="goTo('/')">
      <el-icon><Back /></el-icon> <span>返回探索</span>
    </div>

    <div class="auth-visual">
      <transition name="img-reveal">
        <div v-if="isLogin" class="visual-bg" style="background-image: url('https://picsum.photos/id/1018/1200/1600')" key="login-bg"></div>
        <div v-else class="visual-bg" style="background-image: url('https://picsum.photos/id/1043/1200/1600')" key="reg-bg"></div>
      </transition>
      
      <div class="visual-overlay"></div>

      <div class="visual-text">
        <div class="brand-logo">
          <el-icon class="brand-icon"><WindPower /></el-icon>
          <span class="brand-name">Wetland OS.</span>
        </div>
        
        <transition name="text-slide" mode="out-in">
          <div v-if="isLogin" key="login-text" class="text-content">
            <h1 class="hero-title">自然<br>唤醒感知.</h1>
            <p class="hero-desc">欢迎归来。继续连接千万顷湿地的每一次晨钟暮鼓。</p>
          </div>
          <div v-else key="reg-text" class="text-content">
            <h1 class="hero-title">成为<br>守望者.</h1>
            <p class="hero-desc">加入自然保护档案馆，记录并分享转瞬即逝的光影之美。</p>
          </div>
        </transition>
      </div>
    </div>

    <div class="auth-form-area">
      <transition name="form-reveal" mode="out-in">
        
        <div v-if="isLogin" class="editorial-form-wrapper" key="login-form">
          <div class="form-header">
            <span class="chapter">01 — Authenticate</span>
            <h2>系统登录</h2>
          </div>

          <el-form class="editorial-form" label-position="top">
            <el-form-item label="邮箱 / 账号">
              <el-input 
                v-model="loginForm.account" 
                placeholder="请输入凭证" 
                class="line-input" 
                :prefix-icon="Message"
              />
            </el-form-item>
            
            <el-form-item label="密码">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="请输入密码" 
                show-password
                class="line-input"
                :prefix-icon="Lock"
              />
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="loginForm.remember" class="custom-checkbox">记住凭证</el-checkbox>
              <a href="#" class="forgot-pwd">遗忘密码？</a>
            </div>

            <button type="button" class="editorial-solid-btn" @click="handleLogin">
              <span>登 录</span>
              <el-icon class="btn-arrow"><ArrowRight /></el-icon>
            </button>
          </el-form>

          <div class="switch-prompt">
            <span class="hint">尚未加入自然档案？</span>
            <span class="action-link" @click="toggleAuthMode">创建账号</span>
          </div>
        </div>

        <div v-else class="editorial-form-wrapper" key="register-form">
          <div class="form-header">
            <span class="chapter">02 — Join Us</span>
            <h2>创建账号</h2>
          </div>

          <el-form class="editorial-form" label-position="top">
            <el-form-item label="用户名">
              <el-input 
                v-model="registerForm.username" 
                placeholder="设置您的自然代号" 
                class="line-input"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item label="邮箱">
              <el-input 
                v-model="registerForm.email" 
                placeholder="输入联络邮箱" 
                class="line-input"
                :prefix-icon="Message"
              />
            </el-form-item>
            
            <el-form-item label="密码">
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                placeholder="至少 8 位安全密钥" 
                show-password
                class="line-input"
                :prefix-icon="Lock"
              />
            </el-form-item>

            <button type="button" class="editorial-solid-btn" @click="handleRegister">
              <span>注 册</span>
              <el-icon class="btn-arrow"><ArrowRight /></el-icon>
            </button>
          </el-form>

          <div class="switch-prompt">
            <span class="hint">已是守望者？</span>
            <span class="action-link" @click="toggleAuthMode">立即登录</span>
          </div>
        </div>

      </transition>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Back, WindPower, ArrowRight, Message, Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const route = useRoute()

const isLogin = ref(true)

onMounted(() => {
  if (route.query.mode === 'register') {
    isLogin.value = false
  }
})

// 表单数据
const loginForm = reactive({
  account: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  username: '',
  email: '',
  password: ''
})

const toggleAuthMode = () => {
  isLogin.value = !isLogin.value
}

const goTo = (path) => {
  router.push(path)
}

const handleLogin = async () => {
  if (!loginForm.account || !loginForm.password) {
    ElMessage.warning('凭证不能为空')
    return
  }

  try {
    const response = await api.post('/auth/login', {
      account: loginForm.account,
      password: loginForm.password
    })

    if (response.data.success) {
      ElMessage.success('欢迎回来')
      if (response.data.token) localStorage.setItem('auth_token', response.data.token)
      if (response.data.user) localStorage.setItem('user_info', JSON.stringify(response.data.user))
      localStorage.setItem('isLoggedIn', 'true')
      router.push('/overview')
    } else {
      ElMessage.error(response.data.message || '凭证验证失败')
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || '网络连接异常，请检查终端'
    ElMessage.error(errorMsg)
  }
}

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    ElMessage.warning('请补全档案信息')
    return
  }

  try {
    const response = await api.post('/auth/register', {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password
    })

    if (response.data.success) {
      ElMessage.success('档案创建成功，请登录')
      isLogin.value = true
      loginForm.account = registerForm.email
      registerForm.password = ''
    } else {
      ElMessage.error(response.data.message || '注册失败')
    }
  } catch (error) {
    const errorMsg = error.response?.data?.message || '网络连接异常'
    ElMessage.error(errorMsg)
  }
}

// --- 🌟 极简光晕跟随 ---
const mouseX = ref(-100)
const mouseY = ref(-100)
let animationFrameId = null

const handleMouseMove = (e) => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  animationFrameId = requestAnimationFrame(() => {
    mouseX.value = e.clientX - 150 
    mouseY.value = e.clientY - 150
  })
}

onBeforeUnmount(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
})
</script>


<style scoped>
/* =========================================
   Mersi 风格重置与字体
   ========================================= */
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400;0,600;1,400&family=Inter:wght@300;400;500&display=swap');

* { margin: 0; padding: 0; box-sizing: border-box; }

.dark-theme {
  --text-primary: #ffffff;
  --text-secondary: rgba(255, 255, 255, 0.5);
  --font-serif: 'Playfair Display', 'Songti SC', 'STSong', serif;
  --font-sans: 'Inter', 'PingFang SC', sans-serif;
  color: var(--text-primary);
  background-color: #050505;
  font-family: var(--font-sans);
  overflow: hidden;
}

.mersi-auth-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  position: relative;
}

/* 全局光晕 */
.custom-cursor-aura {
  position: fixed; top: 0; left: 0; width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 60%);
  border-radius: 50%; pointer-events: none; z-index: 9999; transition: transform 0.1s ease-out;
}

/* 极简返回按钮 */
.back-home-btn {
  position: absolute; top: 40px; left: 60px;
  display: flex; align-items: center; gap: 10px;
  font-family: var(--font-sans); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 2px;
  color: var(--text-secondary); cursor: pointer; z-index: 100;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}
.back-home-btn:hover { color: #fff; transform: translateX(-8px); }

/* =========================================
   左侧：视觉大图区
   ========================================= */
.auth-visual {
  position: relative;
  width: 45%;
  height: 100%;
  overflow: hidden;
  display: flex; flex-direction: column; justify-content: space-between;
  padding: 120px 60px 80px;
}

.visual-bg {
  position: absolute; inset: -5%; width: 110%; height: 110%;
  background-size: cover; background-position: center;
  animation: bgZoom 20s infinite alternate ease-in-out;
  z-index: 1;
}
@keyframes bgZoom { 0% { transform: scale(1); } 100% { transform: scale(1.05); } }

/* 更深的渐变压暗图片，凸显文字 */
.visual-overlay {
  position: absolute; inset: 0; z-index: 2;
  background: linear-gradient(135deg, rgba(5,5,5,0.9) 0%, rgba(5,5,5,0.4) 100%);
}

.visual-text { position: relative; z-index: 10; height: 100%; display: flex; flex-direction: column; justify-content: space-between;}

.brand-logo { display: flex; align-items: center; gap: 10px; font-family: var(--font-serif); font-size: 1.5rem; letter-spacing: 1px; color: #fff; opacity: 0; animation: fadeUp 1s ease 0.5s forwards;}

.text-content { margin-bottom: 40px; }
.hero-title { font-family: var(--font-serif); font-size: 4.5rem; line-height: 1.1; font-weight: 400; margin-bottom: 24px; text-shadow: 0 4px 20px rgba(0,0,0,0.5); letter-spacing: 2px;}
.hero-desc { font-size: 1.1rem; color: rgba(255,255,255,0.8); line-height: 1.8; font-weight: 300; max-width: 400px; }

/* =========================================
   右侧：极简表单区
   ========================================= */
.auth-form-area {
  width: 55%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #050505; /* 纯黑背景，体现极致对比 */
  position: relative;
  z-index: 10;
}

.editorial-form-wrapper { width: 100%; max-width: 440px; padding: 0 20px; }

.form-header { margin-bottom: 50px; }
.chapter { display: block; font-size: 0.75rem; color: var(--text-secondary); letter-spacing: 3px; text-transform: uppercase; margin-bottom: 10px; }
.form-header h2 { font-family: var(--font-serif); font-size: 2.8rem; font-weight: 400; color: #fff; }

/* 🌟 重构的建筑级线条输入框 */
.editorial-form { width: 100%; }

:deep(.el-form-item__label) {
  color: var(--text-secondary) !important;
  font-family: var(--font-sans);
  font-weight: 300;
  font-size: 0.85rem;
  letter-spacing: 1px;
  padding-bottom: 8px;
}

:deep(.line-input .el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important; /* 仅保留底边线 */
  border-radius: 0 !important;
  padding: 0 0 8px 0 !important;
  transition: border-color 0.4s ease;
}
:deep(.line-input .el-input__wrapper.is-focus) { border-bottom-color: #fff !important; }

:deep(.line-input .el-input__inner) {
  color: #fff; font-family: var(--font-sans); font-size: 1.2rem; font-weight: 300; height: 35px; letter-spacing: 1px;
}
:deep(.line-input .el-input__prefix) { color: var(--text-secondary); font-size: 1.2rem; margin-right: 12px;}

/* 选项区 */
.form-options {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 20px; margin-bottom: 40px;
}
:deep(.custom-checkbox .el-checkbox__label) { color: var(--text-secondary); font-weight: 300; font-size: 0.85rem; letter-spacing: 1px;}
:deep(.custom-checkbox .el-checkbox__inner) { background: transparent; border-color: rgba(255, 255, 255, 0.3); border-radius: 0;}
:deep(.custom-checkbox.is-checked .el-checkbox__inner) { background: #fff; border-color: #fff; }
:deep(.custom-checkbox.is-checked .el-checkbox__inner::after) { border-color: #000; }

.forgot-pwd { color: var(--text-secondary); font-size: 0.85rem; text-decoration: none; transition: color 0.3s; letter-spacing: 1px;}
.forgot-pwd:hover { color: #fff; }

/* 🌟 实体锐角按钮 (Editorial Button) */
.editorial-solid-btn {
  width: 100%; background: #fff; color: #000; border: none; 
  padding: 18px 24px; font-family: var(--font-sans); font-size: 1rem; font-weight: 500; letter-spacing: 4px; text-transform: uppercase;
  cursor: pointer; display: flex; align-items: center; justify-content: space-between;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}
.editorial-solid-btn:hover { background: #e0e0e0; padding-right: 15px; } /* 悬浮时按钮内部文字/图标发生相对位移 */
.btn-arrow { font-size: 1.2rem; transition: transform 0.4s ease; }
.editorial-solid-btn:hover .btn-arrow { transform: translateX(5px); }

/* 底部切换指引 */
.switch-prompt { margin-top: 40px; font-size: 0.85rem; letter-spacing: 1px; display: flex; gap: 10px;}
.switch-prompt .hint { color: var(--text-secondary); }
.switch-prompt .action-link { color: #fff; cursor: pointer; border-bottom: 1px solid rgba(255,255,255,0.3); padding-bottom: 2px; transition: border-color 0.3s;}
.switch-prompt .action-link:hover { border-color: #fff; }

/* =========================================
   🌟 电影级转场魔法 (Cinematic Transitions)
   ========================================= */

/* 左侧背景图：缓慢放大淡入淡出 */
.img-reveal-enter-active, .img-reveal-leave-active { transition: all 1.2s ease-in-out; }
.img-reveal-enter-from { opacity: 0; transform: scale(1.05); }
.img-reveal-leave-to { opacity: 0; transform: scale(1); }

/* 左侧文字：平滑推移 */
.text-slide-enter-active, .text-slide-leave-active { transition: all 0.8s cubic-bezier(0.65, 0, 0.076, 1); }
.text-slide-enter-from { opacity: 0; transform: translateX(-30px); }
.text-slide-leave-to { opacity: 0; transform: translateX(30px); }

/* 右侧表单：电影级上下揭示 (Editorial Slide) */
.form-reveal-enter-active, .form-reveal-leave-active { transition: all 0.9s cubic-bezier(0.65, 0, 0.076, 1); }
.form-reveal-enter-from { opacity: 0; transform: translateY(40px); }
.form-reveal-leave-to { opacity: 0; transform: translateY(-40px); }

@keyframes fadeUp { to { opacity: 1; transform: translateY(0); } from { opacity: 0; transform: translateY(20px); } }

/* =========================================
   响应式设计
   ========================================= */
@media (max-width: 900px) {
  .mersi-auth-container { flex-direction: column; }
  .auth-visual { width: 100%; height: 40vh; padding: 40px 30px; }
  .auth-form-area { width: 100%; height: 60vh; align-items: flex-start; padding-top: 40px;}
  .hero-title { font-size: 3rem; margin-bottom: 10px; }
  .hero-desc { display: none; } /* 移动端隐藏过多描述 */
  .back-home-btn { top: 20px; left: 20px; }
}
</style>
