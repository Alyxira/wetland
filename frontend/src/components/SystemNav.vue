<template>
  <nav class="system-nav">
    <div class="nav-top-row">
      <div class="nav-brand-group">
        <button class="nav-brand" @click="goTo('/overview')">
          <span class="nav-logo-mark">W</span>
          <div class="brand-copy">
            <span class="brand-subtitle">{{ currentLabel }}</span>
          </div>
        </button>

        <div class="brand-divider" aria-hidden="true"></div>

        <div class="nav-search-cluster">
          <button class="home-return" @click="goTo('/')">
            <span class="home-return-label">返回首页</span>
          </button>

          <div class="nav-search-slot">
            <div class="nav-search-box">
              <input
                v-model.trim="searchKeyword"
                type="text"
                class="nav-search-input"
                placeholder="搜索湿地、物种与社区内容"
                @focus="showSearchPanel = true"
                @keydown.enter.prevent="runGlobalSearch"
                @keydown.esc="closeSearchPanel"
              />
              <div class="nav-search-actions">
                <button class="nav-search-detail" @click="goToDetailedSearch">详细搜索</button>
                <button class="nav-search-submit" @click="runGlobalSearch" :disabled="searchLoading || !searchKeyword">
                  搜索
                </button>
              </div>
            </div>

            <transition name="search-card-fade">
              <section v-if="showSearchPanel" class="nav-search-panel">
                <div class="nav-search-panel__head">
                  <div class="nav-search-panel__title">
                    <strong>{{ searchLoading ? '搜索中...' : `${searchResults.length} 条相关结果` }}</strong>
                  </div>
                  <button v-if="searchKeyword" class="panel-link" @click="goToDetailedSearch">前往详细搜索</button>
                </div>

                <div v-if="searchError" class="nav-search-state nav-search-state--error">{{ searchError }}</div>
                <div v-else-if="searchLoading" class="nav-search-state">正在检索相关内容...</div>
                <div v-else-if="!searchKeyword" class="nav-search-state">输入关键词后可在任意页面发起全局搜索。</div>
                <div v-else-if="searchResults.length === 0" class="nav-search-state">没有找到相关结果。</div>
                <div v-else class="nav-search-results">
                  <button
                    v-for="item in searchResults"
                    :key="`${item.type}-${item.id}`"
                    class="nav-search-result"
                    @click="openSearchResult(item)"
                  >
                    <img :src="item.image" :alt="item.title" class="nav-search-result__image" />
                    <div class="nav-search-result__body">
                      <div class="nav-search-result__topline">
                        <span>{{ formatType(item.type) }}</span>
                        <span>{{ item.meta || '系统内容' }}</span>
                      </div>
                      <strong>{{ item.title }}</strong>
                      <span class="nav-search-result__tag">{{ item.tag || '未分类' }}</span>
                    </div>
                  </button>
                </div>
              </section>
            </transition>
          </div>
        </div>
      </div>

      <div class="nav-utility-row">
        <div class="profile-entry" :class="{ 'active-profile': route.path === '/profile' }">
          <button class="action-chip profile-chip" aria-label="个人中心">
            <span class="profile-avatar">U</span>
          </button>
          <div class="profile-dropdown">
            <button class="dropdown-item" @click="goTo('/profile')">个人中心</button>
            <button class="dropdown-item danger-item" @click="handleLogout">退出登录</button>
          </div>
        </div>

        <button
          v-for="item in utilityItems"
          :key="item.key"
          class="action-chip utility-chip"
          :class="{ 'is-active': activeUtilityKey === item.key }"
          @click="handleUtility(item)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="nav-bottom-row">
      <button
        v-for="item in navItems"
        :key="item.key"
        class="nav-item"
        :class="{ 'is-active': activeNavKey === item.key }"
        @click="goTo(item.path)"
      >
        {{ item.label }}
      </button>
    </div>
  </nav>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { fetchSearchResults, formatSearchType, resolveSearchResultRoute } from '../utils/search'

const router = useRouter()
const route = useRoute()
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchError = ref('')
const showSearchPanel = ref(false)

const navItems = [
  { key: 'overview', label: '湿地全景', path: '/overview' },
  { key: 'screen', label: '数智泽屏', path: '/screen' },
  { key: 'consult', label: '云游导览', path: '/consult' },
  { key: 'science', label: '生态图鉴', path: '/science' },
  { key: 'community', label: '游踪漫话', path: '/community' },
  { key: 'ai', label: '泽畔智语', path: '/ai' }
]

const utilityItems = [
  { key: 'message', label: '用户消息' },
  { key: 'favorite', label: '收藏' },
  { key: 'history', label: '历史' },
  { key: 'activity', label: '动态' }
]

const activeNavKey = computed(() => navItems.find((item) => item.path === route.path)?.key ?? '')
const activeUtilityKey = computed(() => (route.path === '/profile' ? 'profile' : ''))
const currentLabel = computed(() => {
  if (route.path === '/profile') return '个人中心'
  if (route.path === '/search') return '汀洲觅知'
  return navItems.find((item) => item.path === route.path)?.label ?? '系统首页'
})

let searchTimer = null

const formatType = formatSearchType

const goTo = (path) => {
  if (route.path === path) return
  router.push(path)
}

const closeSearchPanel = () => {
  showSearchPanel.value = false
}

const runGlobalSearch = async () => {
  if (!searchKeyword.value) return

  searchLoading.value = true
  searchError.value = ''
  showSearchPanel.value = true

  try {
    searchResults.value = await fetchSearchResults(api, {
      keyword: searchKeyword.value,
      type: 'all'
    })
  } catch (error) {
    console.error('全局搜索失败:', error)
    searchError.value = error.response?.data?.message || error.message || '搜索失败，请稍后重试。'
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

const goToDetailedSearch = () => {
  const query = searchKeyword.value ? { q: searchKeyword.value } : {}
  closeSearchPanel()
  router.push({ path: '/search', query })
}

const openSearchResult = (item) => {
  closeSearchPanel()
  router.push(resolveSearchResultRoute(item))
}

const handleDocumentClick = (event) => {
  if (!event.target.closest('.nav-search-slot') && !event.target.closest('.nav-search-panel')) {
    closeSearchPanel()
  }
}

const handleUtility = (item) => {
  ElMessage.info(`${item.label} 页面待接入`)
}

const handleLogout = () => {
  localStorage.removeItem('auth_token')
  localStorage.removeItem('user_info')
  localStorage.removeItem('isLoggedIn')
  ElMessage.success('已安全退出')
  router.push('/')
}

watch(searchKeyword, (value) => {
  if (searchTimer) clearTimeout(searchTimer)

  if (!value) {
    searchResults.value = []
    searchError.value = ''
    return
  }

  searchTimer = setTimeout(() => {
    runGlobalSearch()
  }, 260)
})

watch(
  () => route.fullPath,
  () => {
    closeSearchPanel()
    if (route.path === '/search' && typeof route.query.q === 'string') {
      searchKeyword.value = route.query.q
    }
  },
  { immediate: true }
)

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<style scoped>
.system-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  padding: 24px 36px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  z-index: 100;
  color: var(--nav-text, #0f1724);
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--nav-bg, rgba(235, 242, 250, 0.78)) 76%, rgba(247, 244, 238, 0.94) 24%),
      color-mix(in srgb, var(--nav-bg, rgba(235, 242, 250, 0.56)) 72%, rgba(245, 241, 234, 0.9) 28%)
    );
  border-bottom: 1px solid color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.14)) 68%, rgba(255, 255, 255, 0.36));
  backdrop-filter: blur(16px);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--nav-border, rgba(126, 149, 183, 0.18)) 22%, transparent);
  transition: background 0.35s ease, border-color 0.35s ease, color 0.35s ease, box-shadow 0.35s ease;
}

.nav-top-row,
.nav-bottom-row {
  display: flex;
  align-items: center;
}

.nav-top-row {
  justify-content: space-between;
  gap: 20px;
  min-height: 56px;
  position: relative;
  z-index: 2;
}

.nav-bottom-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0;
  padding-top: 10px;
  border-top: 1px solid color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.12)) 85%, transparent);
  position: relative;
  z-index: 1;
}

.nav-brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.nav-brand-group {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
  flex: 1 1 auto;
}

.nav-search-cluster {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex: 1 1 auto;
}

.brand-divider {
  width: 1px;
  height: 34px;
  background: color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.18)) 90%, transparent);
}

.nav-brand:hover {
  transform: scale(1.08);
}

.nav-logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--nav-text, #0b1220) 26%, transparent);
  background: rgba(255, 255, 255, 0.18);
  color: var(--nav-text, #0b1220);
  font-family: var(--font-serif, 'Playfair Display', serif);
  font-size: 1.38rem;
  line-height: 1;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  text-align: left;
}

.brand-text {
  font-family: var(--font-serif, 'Playfair Display', serif);
  font-size: 1.7rem;
  letter-spacing: 0.08em;
  font-weight: 600;
  line-height: 1;
  color: var(--nav-text, #0b1220);
}

.brand-subtitle {
  margin-top: 2px;
  color: color-mix(in srgb, var(--nav-text, #0f1724) 74%, transparent);
  font-size: 0.92rem;
  letter-spacing: 0.04em;
  line-height: 1.2;
}

.home-return {
  border: none;
  background: transparent;
  color: color-mix(in srgb, var(--nav-text, #0f1724) 74%, transparent);
  cursor: pointer;
  padding: 0;
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0;
  transition: color 0.24s ease, transform 0.24s ease;
}

.home-return::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 1px;
  background: color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.24)) 100%, transparent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.24s ease, background 0.24s ease;
}

.home-return-label {
  font-size: 0.92rem;
  letter-spacing: 0.04em;
  line-height: 1.2;
}

.home-return:hover {
  color: var(--nav-text, #0f1724);
  transform: translateY(-1px);
}

.home-return:hover::after {
  transform: scaleX(1);
  background: var(--nav-text, #0f1724);
}

.nav-search-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 50px;
  padding: 6px 8px 6px 16px;
  border: 1px solid color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.22)) 92%, transparent);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(241, 246, 252, 0.58)),
    color-mix(in srgb, var(--nav-bg, rgba(235, 242, 250, 0.62)) 88%, white 12%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.5),
    0 14px 34px color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.14)) 34%, transparent);
  backdrop-filter: blur(16px);
}

.nav-search-slot {
  position: relative;
  min-width: min(540px, 44vw);
  flex: 1 1 auto;
}

.nav-search-input {
  width: 100%;
  min-width: 0;
  border: none;
  background: transparent;
  color: var(--nav-text, #0f1724);
  outline: none;
  font-size: 0.84rem;
  letter-spacing: 0.03em;
  line-height: 1.3;
}

.nav-search-input::placeholder {
  color: color-mix(in srgb, var(--nav-text, #0f1724) 54%, white 46%);
}

.nav-search-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.nav-search-submit,
.nav-search-detail,
.panel-link {
  appearance: none;
  border: none;
  background: transparent;
  cursor: pointer;
  white-space: nowrap;
}

.nav-search-submit {
  padding: 9px 16px;
  background: var(--nav-text, #0f1724);
  color: #fff;
  font-size: 0.82rem;
  letter-spacing: 0.06em;
}

.nav-search-submit:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.nav-search-detail,
.panel-link {
  padding: 9px 12px;
  color: color-mix(in srgb, var(--nav-text, #0f1724) 76%, transparent);
  font-size: 0.82rem;
  letter-spacing: 0.04em;
}

.nav-search-panel {
  position: absolute;
  top: calc(100% + 12px);
  left: 0;
  width: 100%;
  min-width: min(720px, calc(100vw - 72px));
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--nav-border, rgba(111, 140, 186, 0.16)) 92%, transparent);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 248, 253, 0.88)),
    color-mix(in srgb, var(--nav-bg, rgba(235, 242, 250, 0.7)) 85%, white 15%);
  box-shadow:
    0 28px 60px color-mix(in srgb, var(--nav-border, rgba(120, 146, 183, 0.22)) 44%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.56);
  backdrop-filter: blur(18px);
  z-index: 12;
}

.nav-search-panel__head,
.nav-search-result__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nav-search-panel__head {
  margin-bottom: 16px;
  font-size: 0.84rem;
}

.nav-search-panel__title {
  display: grid;
  gap: 4px;
}

.nav-search-panel__eyebrow {
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--nav-text, #0f1724) 62%, white 38%);
}

.nav-search-state {
  padding: 20px 8px 10px;
  color: color-mix(in srgb, var(--nav-text, #0f1724) 72%, transparent);
}

.nav-search-state--error {
  color: #b84d4d;
}

.nav-search-results {
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.nav-search-result {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 14px;
  width: 100%;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--nav-border, rgba(111, 140, 186, 0.14)) 82%, transparent);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(246, 249, 253, 0.8));
  cursor: pointer;
  text-align: left;
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.nav-search-result:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--nav-border, rgba(111, 140, 186, 0.28)) 100%, transparent);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(241, 247, 252, 0.88));
}

.nav-search-result__image {
  width: 96px;
  height: 74px;
  object-fit: cover;
}

.nav-search-result__body {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.nav-search-result__topline,
.nav-search-result__tag {
  color: color-mix(in srgb, var(--nav-text, #0f1724) 66%, transparent);
  font-size: 0.76rem;
  letter-spacing: 0.08em;
}

.nav-search-result__body strong {
  font-family: var(--font-serif, 'Playfair Display', serif);
  font-size: 1.18rem;
  line-height: 1.05;
}

.search-card-fade-enter-active,
.search-card-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.search-card-fade-enter-from,
.search-card-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.nav-utility-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: 12px;
}

.action-chip {
  border: none;
  background: transparent;
  color: color-mix(in srgb, var(--nav-text, #0b1220) 88%, transparent);
  padding: 0 0 6px;
  border-radius: 0;
  cursor: pointer;
  font-size: 0.92rem;
  letter-spacing: 0.04em;
  position: relative;
  transition: color 0.24s ease, transform 0.24s ease;
}

.action-chip:hover {
  color: var(--nav-text, #0f1724);
  transform: translateY(-1px);
}

.action-chip::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 1px;
  background: color-mix(in srgb, var(--nav-border, rgba(96, 122, 164, 0.24)) 100%, transparent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.24s ease, background 0.24s ease;
}

.action-chip:hover::after,
.action-chip.is-active::after,
.profile-entry:hover .profile-chip::after {
  transform: scaleX(1);
  background: var(--nav-text, #0f1724);
}

.profile-entry {
  position: relative;
}

.profile-chip {
  padding: 0;
}

.profile-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--nav-text, #0b1220) 18%, transparent);
  background: rgba(255, 255, 255, 0.62);
  color: var(--nav-text, #0b1220);
  font-family: var(--font-serif, 'Playfair Display', serif);
  font-size: 1rem;
  font-weight: 600;
  line-height: 1;
  transition: transform 0.24s ease, border-color 0.24s ease, background-color 0.24s ease;
}

.profile-chip:hover .profile-avatar,
.profile-entry:hover .profile-avatar,
.active-profile .profile-avatar {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--nav-text, #0b1220) 28%, transparent);
  background: rgba(255, 255, 255, 0.8);
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  min-width: 150px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid color-mix(in srgb, var(--nav-border, rgba(111, 140, 186, 0.12)) 90%, transparent);
  box-shadow: 0 20px 40px color-mix(in srgb, var(--nav-border, rgba(120, 146, 183, 0.18)) 42%, transparent);
  backdrop-filter: blur(18px);
  opacity: 0;
  visibility: hidden;
  transform: translateY(8px);
  z-index: 20;
  transition: opacity 0.24s ease, transform 0.24s ease, visibility 0s linear 0.24s;
}

.profile-entry:hover .profile-dropdown,
.profile-entry:focus-within .profile-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  transition-delay: 0s;
}

.dropdown-item {
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  padding: 10px 12px;
  border-radius: 14px;
  color: var(--nav-text, #0f1724);
  font-size: 0.9rem;
  transition: background 0.24s ease;
}

.dropdown-item:hover {
  background: color-mix(in srgb, var(--nav-border, rgba(94, 136, 216, 0.16)) 60%, white 40%);
}

.danger-item:hover {
  background: rgba(217, 98, 98, 0.1);
  color: #b84d4d;
}

.nav-item {
  appearance: none;
  border: none;
  background: transparent;
  min-height: 48px;
  width: 100%;
  padding: 0 12px;
  font-size: 0.92rem;
  letter-spacing: 0.04em;
  color: color-mix(in srgb, var(--nav-text, #0b1220) 88%, transparent);
  cursor: pointer;
  transition: color 0.28s, transform 0.28s;
  position: relative;
}

.nav-item.is-active,
.nav-item:hover {
  color: var(--nav-text, #0f1724);
  transform: translateY(-1px);
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 24%;
  bottom: 9px;
  width: 52%;
  height: 1px;
  background: var(--nav-text, #0f1724);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.28s ease;
}

.nav-item.is-active::after,
.nav-item:hover::after {
  transform: scaleX(1);
}

@media (max-width: 1180px) {
  .nav-top-row {
    flex-direction: column;
    align-items: stretch;
  }

  .nav-brand-group {
    flex-wrap: wrap;
  }

  .nav-utility-row {
    margin-left: 0;
    justify-content: flex-start;
  }

  .nav-search-slot {
    min-width: min(100%, 680px);
  }

  .nav-search-panel {
    min-width: 100%;
  }
}

@media (max-width: 980px) {
  .system-nav {
    padding: 18px 18px 14px;
  }

  .brand-divider {
    display: none;
  }

  .nav-search-cluster {
    width: 100%;
    flex-wrap: wrap;
  }

  .nav-search-slot {
    min-width: 100%;
  }

  .nav-bottom-row {
    display: flex;
    overflow-x: auto;
  }

  .nav-item {
    flex: 0 0 auto;
    width: auto;
    min-width: 120px;
  }
}

@media (max-width: 680px) {
  .nav-brand-group {
    gap: 12px 18px;
  }

  .brand-subtitle {
    font-size: 0.86rem;
  }

  .nav-search-box {
    grid-template-columns: 1fr;
  }

  .nav-search-actions {
    justify-content: space-between;
  }

  .nav-search-panel {
    width: 100%;
    min-width: 100%;
  }

  .nav-search-result {
    grid-template-columns: 1fr;
  }

  .nav-search-result__image {
    width: 100%;
    height: 124px;
  }
}
</style>
