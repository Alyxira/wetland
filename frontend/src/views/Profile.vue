<template>
  <div class="profile-page dark-theme" ref="pageContainer" @mousemove="handleMouseMove">
    <div class="custom-cursor-aura" :style="{ transform: `translate3d(${mouseX}px, ${mouseY}px, 0)` }"></div>

    <div class="background-layer">
      <div class="bg-image"></div>
      <div class="bg-overlay"></div>
    </div>

    <SystemNav />

    <main class="page-shell">
      <section class="hero-section gs-reveal">
        <div class="hero-copy">
          <span class="chapter">05 — Personal Archive</span>
          <h1>把个人页做成一份可阅读的自然档案。</h1>
          <p>
            这里不只是账户设置页，而是一份关于你如何抵达、观察、记录并参与湿地保护的数字档案。
            我把它收口到当前整站的 editorial 语言里，同时保留你原来最有辨识度的通行证设计。
          </p>
        </div>

        <div class="identity-panel">
          <div class="identity-head">
            <el-avatar
              :size="88"
              :src="profileUser.avatar || defaultAvatar"
              class="user-avatar"
            />
            <div class="identity-copy">
              <span class="role-label">01 — Explorer</span>
              <h2>{{ displayName }}</h2>
              <p>{{ profileSubline }}</p>
            </div>
          </div>

          <div class="identity-meta">
            <div v-for="item in identityMeta" :key="item.label" class="meta-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
      </section>

      <section class="content-grid">
        <section class="left-column">
          <article class="ticket-panel gs-reveal">
            <div class="panel-head">
              <span class="chapter">06 — Wetland Pass</span>
              <h3>数字通行证</h3>
            </div>

            <div class="digital-ticket-wrapper" @mousemove="tiltCard" @mouseleave="resetTilt">
              <div class="digital-ticket">
                <div class="ticket-top">
                  <span class="ticket-label">Wetland OS Pass</span>
                  <el-icon class="ticket-icon"><WindPower /></el-icon>
                </div>
                <div class="ticket-middle">
                  <h2>终身通行证</h2>
                  <p>授权探索千万顷湿地保护区，并参与公开生态计划。</p>
                </div>
                <div class="ticket-bottom">
                  <div class="barcode">|| | ||| || || | | ||| || || |</div>
                  <span class="ticket-id">NO. 8839-2026-X</span>
                </div>
              </div>
            </div>
          </article>

          <article class="field-panel gs-reveal">
            <div class="panel-head">
              <span class="chapter">07 — Field Summary</span>
              <h3>探索摘要</h3>
            </div>
            <div class="summary-list">
              <div v-for="item in summaries" :key="item.title" class="summary-item">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </div>
          </article>
        </section>

        <section class="right-column gs-reveal">
          <div class="stats-grid">
            <article v-for="item in stats" :key="item.label" class="stat-card">
              <span class="stat-label">{{ item.label }}</span>
              <strong class="stat-num">{{ item.value }}</strong>
              <p class="stat-note">{{ item.note }}</p>
            </article>
          </div>

          <article class="management-panel">
            <div class="panel-head">
              <span class="chapter">08 — Management</span>
              <h3>档案管理</h3>
            </div>

            <ul class="editorial-list">
              <li v-for="item in managementItems" :key="item.title">
                <div class="list-left">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.desc }}</p>
                </div>
                <el-icon class="list-arrow"><ArrowRight /></el-icon>
              </li>
            </ul>
          </article>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import SystemNav from '../components/SystemNav.vue'
import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { WindPower, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import gsap from 'gsap'
import api from '../utils/api'

const router = useRouter()
const pageContainer = ref(null)
let ctx

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const profileUser = ref({})
const userPosts = ref([])
const userComments = ref([])
const identityMeta = ref([
  { label: '身份等级', value: '档案加载中' },
  { label: '当前角色', value: '等待同步' },
  { label: '设备状态', value: '账号接入中' }
])

const stats = ref([
  { label: '发布动态', value: '0', note: '正在统计你的社区发布记录。' },
  { label: '评论记录', value: '0', note: '正在同步你在社区中的评论内容。' },
  { label: '获得点赞', value: '0', note: '正在汇总你发布内容收到的互动。' },
  { label: '最近互动', value: '--', note: '最近一次评论时间会显示在这里。' }
])

const summaries = ref([
  { title: '最新评论载入中', desc: '正在从后端同步你的真实评论记录。' },
  { title: '个人记录同步中', desc: '稍后这里会展示你最近在社区中的留言内容。' },
  { title: '系统建议', desc: '你可以继续发布动态或评论，与更多湿地观察者建立连接。' }
])

const managementItems = [
  { title: '编辑个人档案', desc: '更新昵称、简介、联络方式与个人标签。' },
  { title: '我的旅拍足迹', desc: '查看你在不同湿地区域留下的影像与时间线。' },
  { title: '生态贡献证书', desc: '按季度整理你的活动参与、问答与守护记录。' },
  { title: '安全与隐私设置', desc: '管理登录方式、授权设备以及账号保护选项。' }
]

const displayName = computed(() => profileUser.value.realName || profileUser.value.username || '自然守望者')
const profileSubline = computed(() => {
  const userId = profileUser.value.id ? `ID WTLD-${String(profileUser.value.id).padStart(3, '0')}` : 'ID WTLD-000'
  return `${profileUser.value.email || '未绑定邮箱'} / ${userId}`
})

const goTo = (path) => router.push(path)

const handleLogout = () => {
  localStorage.removeItem('auth_token')
  localStorage.removeItem('user_info')
  localStorage.removeItem('isLoggedIn')
  ElMessage.success('已安全退出')
  router.push('/')
}

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

const tiltCard = (e) => {
  const card = e.currentTarget.querySelector('.digital-ticket')
  const rect = e.currentTarget.getBoundingClientRect()
  const x = e.clientX - rect.left - rect.width / 2
  const y = e.clientY - rect.top - rect.height / 2

  const rotateX = -(y / rect.height) * 18
  const rotateY = (x / rect.width) * 18

  gsap.to(card, {
    rotateX,
    rotateY,
    scale: 1.03,
    duration: 0.45,
    ease: 'power2.out'
  })
}

const resetTilt = (e) => {
  const card = e.currentTarget.querySelector('.digital-ticket')
  gsap.to(card, {
    rotateX: 0,
    rotateY: 0,
    scale: 1,
    duration: 1.1,
    ease: 'elastic.out(1, 0.35)'
  })
}

const truncateText = (value, limit = 68) => {
  if (!value) return ''
  return value.length > limit ? `${value.slice(0, limit)}...` : value
}

const loadProfileData = async () => {
  try {
    const [profileResponse, commentsResponse, postsResponse] = await Promise.all([
      api.get('/users/me'),
      api.get('/users/me/comments'),
      api.get('/posts')
    ])

    profileUser.value = profileResponse.data?.user || {}
    const allPosts = postsResponse.data?.posts || []
    userPosts.value = allPosts.filter((post) => post.userId === profileUser.value.id)
    userComments.value = commentsResponse.data?.comments || []

    const totalLikes = userPosts.value.reduce((sum, post) => sum + (post.likes || 0), 0)
    const latestComment = userComments.value[0]

    identityMeta.value = [
      { label: '身份等级', value: userComments.value.length >= 5 ? '活跃记录者' : '新晋观察者' },
      { label: '当前角色', value: profileUser.value.bio || '生态记录贡献者' },
      { label: '联系邮箱', value: profileUser.value.email || '未设置' }
    ]

    stats.value = [
      { label: '发布动态', value: String(userPosts.value.length), note: '来自真实社区发帖数据。' },
      { label: '评论记录', value: String(userComments.value.length), note: '来自后端 Comment 的实际数量。' },
      { label: '获得点赞', value: String(totalLikes), note: '汇总你已发布内容当前获得的点赞。' },
      { label: '最近互动', value: latestComment?.time || '--', note: latestComment ? `最近评论于《${latestComment.postTitle}》` : '你还没有留下评论。' }
    ]

    summaries.value = userComments.value.length
      ? userComments.value.slice(0, 3).map((comment, index) => ({
          title: `评论 ${index + 1} · ${comment.postTitle}`,
          desc: truncateText(comment.content, 96)
        }))
      : [
          { title: '还没有评论记录', desc: '去社区页参与互动后，这里会展示你的最新评论。' },
          { title: '发布第一条评论', desc: '你可以在社区动态下留下观察笔记、拍摄心得或路线建议。' },
          { title: '个人记录即将形成', desc: '你的评论会逐步沉淀成可回看的个人湿地档案。' }
        ]
  } catch (error) {
    console.error('个人档案加载失败:', error)
    ElMessage.error('个人档案加载失败')
  }
}

onMounted(() => {
  loadProfileData()
  nextTick(() => {
    ctx = gsap.context(() => {
      gsap.fromTo(
        '.gs-reveal',
        { opacity: 0, y: 60 },
        { opacity: 1, y: 0, duration: 1.2, stagger: 0.14, ease: 'power3.out', delay: 0.1 }
      )
    }, pageContainer.value)
  })
})

onUnmounted(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (ctx) ctx.revert()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600&family=Inter:wght@300;400;500;600&display=swap');

* { margin: 0; padding: 0; box-sizing: border-box; }

.dark-theme {
  --text-primary: #1f3147;
  --text-secondary: rgba(58, 76, 102, 0.74);
  --line-soft: rgba(111, 140, 186, 0.14);
  --panel: rgba(255, 255, 255, 0.58);
  --panel-strong: rgba(245, 248, 253, 0.9);
  --font-serif: 'Playfair Display', 'Songti SC', serif;
  --font-sans: 'Inter', 'PingFang SC', sans-serif;
  color: var(--text-primary);
  font-family: var(--font-sans);
}

.profile-page { min-height: 100vh; position: relative; background: #f3f7fc; overflow-x: hidden; }

.custom-cursor-aura {
  position: fixed;
  top: 0;
  left: 0;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(128, 163, 221, 0.18) 0%, transparent 60%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 9999;
  transition: transform 0.1s ease-out;
}

.background-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-image {
  position: absolute;
  inset: -4%;
  background: url('https://picsum.photos/id/1050/1920/1080') center/cover no-repeat;
  filter: grayscale(35%);
  transform: scale(1.04);
}
.bg-overlay { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(255,255,255,0.24) 0%, rgba(239,245,251,0.94) 82%); }
.top-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 20px;
  align-items: center;
  padding: 28px 5vw;
  backdrop-filter: blur(18px);
  background: rgba(255,255,255,0.68);
  border-bottom: 1px solid var(--line-soft);
}

.back-btn,
.nav-title,
.logout-btn {
  letter-spacing: 2px;
  text-transform: uppercase;
  font-size: 0.78rem;
}

.back-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  display: inline-flex;
  gap: 14px;
  cursor: pointer;
  transition: color 0.3s, transform 0.3s;
}

.back-btn:hover { color: var(--text-primary); transform: translateX(-4px); }
.nav-title { color: var(--text-secondary); justify-self: center; }

.logout-btn {
  background: transparent;
  border: 1px solid rgba(111,140,186,0.14);
  color: var(--text-secondary);
  padding: 10px 16px;
  cursor: pointer;
  transition: color 0.3s, border-color 0.3s;
}

.logout-btn:hover {
  color: #d96262;
  border-color: rgba(255, 107, 107, 0.5);
}

.page-shell {
  position: relative;
  z-index: 2;
  width: min(1380px, 100% - 10vw);
  margin: 0 auto;
  padding: 208px 0 110px;
}

.chapter {
  display: inline-block;
  margin-bottom: 18px;
  color: var(--text-secondary);
  font-size: 0.78rem;
  letter-spacing: 3px;
  text-transform: uppercase;
}

.hero-section {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 28px;
  min-height: 52vh;
  align-items: end;
}

.hero-copy h1,
.identity-copy h2,
.panel-head h3,
.list-left h4 {
  font-family: var(--font-serif);
  font-weight: 400;
}

.hero-copy h1 {
  font-size: clamp(3rem, 5.6vw, 5.2rem);
  line-height: 1.04;
  max-width: 9ch;
}

.hero-copy p {
  margin-top: 22px;
  max-width: 560px;
  color: var(--text-secondary);
  line-height: 1.9;
  font-size: 1.02rem;
}

.identity-panel,
.ticket-panel,
.field-panel,
.stat-card,
.management-panel {
  background: var(--panel);
  border: 1px solid var(--line-soft);
  backdrop-filter: blur(18px);
}

.identity-panel { padding: 28px; }
.identity-head {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.user-avatar { border: 1px solid rgba(111,140,186,0.14); }
.role-label {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 0.78rem;
  letter-spacing: 2px;
  text-transform: uppercase;
}
.identity-copy h2 { font-size: 2.2rem; }
.identity-copy p { margin-top: 6px; color: var(--text-secondary); }
.identity-meta {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.meta-item {
  padding: 16px;
  background: rgba(255,255,255,0.48);
  border: 1px solid rgba(111,140,186,0.1);
}
.meta-item span {
  display: block;
  margin-bottom: 12px;
  color: var(--text-secondary);
  font-size: 0.8rem;
}
.meta-item strong { font-size: 0.96rem; font-weight: 500; }

.content-grid {
  margin-top: 34px;
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 22px;
  align-items: start;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.ticket-panel,
.field-panel,
.management-panel { padding: 28px; }
.panel-head { margin-bottom: 24px; }
.panel-head h3 { font-size: 2.2rem; }

.digital-ticket-wrapper { perspective: 1000px; cursor: crosshair; }
.digital-ticket {
  width: 100%;
  min-height: 360px;
  border: 1px solid rgba(111,140,186,0.14);
  padding: 34px;
  background: linear-gradient(135deg, rgba(255,255,255,0.82) 0%, rgba(224,232,245,0.92) 100%);
  backdrop-filter: blur(20px);
  transform-style: preserve-3d;
  position: relative;
  overflow: hidden;
}
.digital-ticket::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 18%, rgba(132,164,214,0.16) 0%, transparent 45%);
  pointer-events: none;
}
.ticket-top,
.ticket-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ticket-top { margin-bottom: 70px; }
.ticket-label {
  font-size: 0.78rem;
  letter-spacing: 4px;
  text-transform: uppercase;
  color: var(--text-secondary);
}
.ticket-icon { font-size: 1.5rem; color: var(--text-primary); }
.ticket-middle { margin-bottom: 80px; }
.ticket-middle h2 {
  font-family: var(--font-serif);
  font-size: 2.5rem;
  font-weight: 400;
  margin-bottom: 12px;
}
.ticket-middle p {
  max-width: 280px;
  color: var(--text-secondary);
  line-height: 1.8;
}
.barcode {
  font-family: 'Courier New', Courier, monospace;
  font-size: 1.4rem;
  letter-spacing: 2px;
  color: rgba(58, 76, 102, 0.8);
}
.ticket-id {
  color: var(--text-secondary);
  font-size: 0.76rem;
  letter-spacing: 3px;
}

.summary-list { display: flex; flex-direction: column; gap: 18px; }
.summary-item {
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(111,140,186,0.12);
}
.summary-item strong {
  display: block;
  margin-bottom: 10px;
  font-size: 1rem;
}
.summary-item p {
  color: var(--text-secondary);
  line-height: 1.75;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.stat-card {
  min-height: 210px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.stat-label {
  color: var(--text-secondary);
  font-size: 0.8rem;
  letter-spacing: 2px;
  text-transform: uppercase;
}
.stat-num {
  font-family: var(--font-serif);
  font-size: 3rem;
  font-weight: 400;
}
.stat-note {
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 0.92rem;
}

.editorial-list { list-style: none; }
.editorial-list li {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 24px 0;
  border-bottom: 1px solid rgba(111,140,186,0.12);
  cursor: pointer;
  transition: transform 0.3s, border-color 0.3s;
}
.editorial-list li:hover {
  transform: translateX(8px);
  border-bottom-color: rgba(111,140,186,0.28);
}
.list-left h4 {
  font-size: 1.45rem;
  margin-bottom: 8px;
}
.list-left p {
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 0.9rem;
}
.list-arrow {
  font-size: 1.2rem;
  color: var(--text-secondary);
  transition: transform 0.3s, color 0.3s;
}
.editorial-list li:hover .list-arrow {
  transform: translateX(5px);
  color: var(--text-primary);
}

@media (max-width: 1080px) {
  .hero-section,
  .content-grid,
  .identity-meta,
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .top-nav {
    grid-template-columns: 1fr auto;
    padding: 22px 20px;
  }
  .nav-title { display: none; }
  .page-shell { width: calc(100% - 40px); padding: 132px 0 80px; }
  .identity-panel,
  .ticket-panel,
  .field-panel,
  .management-panel,
  .stat-card { padding: 20px; }
  .identity-head { align-items: flex-start; flex-direction: column; }
  .digital-ticket { padding: 24px; min-height: 320px; }
  .ticket-top,
  .ticket-bottom { flex-direction: column; align-items: flex-start; gap: 14px; }
  .ticket-top { margin-bottom: 44px; }
  .ticket-middle { margin-bottom: 54px; }
  .list-left h4 { font-size: 1.2rem; }
}
</style>
