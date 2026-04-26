<template>
  <div class="search-page">
    <div class="background-layer">
      <div class="bg-image" :style="searchBgStyle"></div>
      <div class="bg-overlay"></div>
    </div>

    <SystemNav />

    <main class="page-shell">
      <section class="search-layout">
        <aside class="search-sidebar">
          <span class="panel-kicker">Search</span>
          <h1>统一检索</h1>

          <div class="search-filters">
            <button
              v-for="option in typeOptions"
              :key="option.value"
              :class="['filter-chip', { 'is-active': typeFilter === option.value }]"
              @click="typeFilter = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </aside>

        <section class="search-stage">
          <div class="search-box">
            <input
              v-model.trim="keyword"
              type="text"
              class="search-input"
              placeholder="输入关键词"
              @keydown.enter.prevent="runSearch"
            />
            <button class="search-btn" @click="runSearch" :disabled="loading || !keyword">搜索</button>
          </div>

          <div class="search-summary">
            <strong>{{ loading ? '搜索中...' : `${results.length} 条结果` }}</strong>
          </div>

          <section class="results-section">
            <div v-if="errorMessage" class="state-card state-card--error">{{ errorMessage }}</div>
            <div v-else-if="!hasSearched" class="state-card">输入关键词后开始搜索。</div>
            <div v-else-if="loading" class="state-card">正在检索相关内容...</div>
            <div v-else-if="results.length === 0" class="state-card">没有找到相关结果。</div>

            <div v-else class="results-grid">
              <article v-for="item in results" :key="`${item.type}-${item.id}`" class="result-card">
                <div class="result-card__image">
                  <img :src="item.image" :alt="item.title" />
                </div>

                <div class="result-card__body">
                  <div class="result-card__topline">
                    <span class="result-type">{{ formatType(item.type) }}</span>
                    <span class="result-meta">{{ item.meta || '系统内容' }}</span>
                  </div>

                  <h2>{{ item.title }}</h2>
                  <p class="result-description">{{ item.description || '暂无摘要' }}</p>
                  <div class="result-card__footer">
                    <span class="result-tag">{{ item.tag || '未分类' }}</span>
                    <button class="result-link" @click="goToResult(item)">进入</button>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SystemNav from '../components/SystemNav.vue'
import api from '../utils/api'
import { fetchSearchResults, formatSearchType, resolveSearchResultRoute, SEARCH_TYPE_OPTIONS } from '../utils/search'

const route = useRoute()
const router = useRouter()
const searchBgStyle = { backgroundImage: 'url(https://picsum.photos/id/1005/1920/1080)' }
const keyword = ref('')
const typeFilter = ref('all')
const results = ref([])
const loading = ref(false)
const errorMessage = ref('')
const hasSearched = ref(false)

const typeOptions = SEARCH_TYPE_OPTIONS

let searchTimer = null
let syncingFromRoute = false

const formatType = formatSearchType

const goToResult = (item) => {
  router.push(resolveSearchResultRoute(item))
}

const syncQuery = () => {
  const nextQuery = {}
  if (keyword.value) nextQuery.q = keyword.value
  if (typeFilter.value && typeFilter.value !== 'all') nextQuery.type = typeFilter.value
  router.replace({ path: '/search', query: nextQuery })
}

const runSearch = async () => {
  if (!keyword.value) return

  loading.value = true
  errorMessage.value = ''
  hasSearched.value = true

  try {
    results.value = await fetchSearchResults(api, {
      keyword: keyword.value,
      type: typeFilter.value
    })
    if (!syncingFromRoute) syncQuery()
  } catch (error) {
    console.error('搜索失败:', error)
    errorMessage.value = error.response?.data?.message || error.message || '搜索失败，请稍后重试。'
    results.value = []
  } finally {
    loading.value = false
  }
}

watch(typeFilter, () => {
  if (syncingFromRoute) return
  if (!hasSearched.value || !keyword.value) return
  runSearch()
})

watch(keyword, (value) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!value) {
    results.value = []
    hasSearched.value = false
    errorMessage.value = ''
    if (!syncingFromRoute) syncQuery()
    return
  }

  if (syncingFromRoute) return

  searchTimer = setTimeout(() => {
    runSearch()
  }, 300)
})

watch(
  () => route.query,
  (query) => {
    const nextKeyword = typeof query.q === 'string' ? query.q : ''
    const nextType = typeOptions.some((option) => option.value === query.type) ? query.type : 'all'
    const keywordChanged = nextKeyword !== keyword.value
    const typeChanged = nextType !== typeFilter.value

    syncingFromRoute = true
    if (nextKeyword !== keyword.value) keyword.value = nextKeyword
    if (nextType !== typeFilter.value) typeFilter.value = nextType
    syncingFromRoute = false

    if (!nextKeyword) {
      results.value = []
      hasSearched.value = false
      errorMessage.value = ''
      loading.value = false
      return
    }

    if (!hasSearched.value || keywordChanged || typeChanged) {
      runSearch()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.search-page { min-height: 100vh; position: relative; overflow-x: hidden; background: #edf1f4; color: #172431; font-family: 'Manrope', 'PingFang SC', sans-serif; }
.background-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-image, .bg-overlay { position: absolute; inset: 0; }
.bg-image { background-position: center; background-size: cover; background-repeat: no-repeat; filter: grayscale(28%) saturate(0.74); }
.bg-overlay { background: linear-gradient(180deg, rgba(247, 248, 249, 0.78) 0%, rgba(238, 241, 244, 0.94) 40%, rgba(235, 239, 242, 0.98) 100%), linear-gradient(90deg, rgba(23, 36, 49, 0.06) 0, rgba(23, 36, 49, 0.06) 1px, transparent 1px, transparent 140px); }
.page-shell { position: relative; z-index: 2; width: min(1280px, calc(100% - 40px)); margin: 0 auto; padding: 188px 0 72px; }
.search-layout { display: grid; grid-template-columns: 260px minmax(0, 1fr); gap: 18px; align-items: start; }
.search-sidebar, .search-stage, .state-card, .result-card { border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(255, 255, 255, 0.74); }
.search-sidebar { display: grid; align-content: start; gap: 18px; padding: 24px; }
.panel-kicker, .result-type { font-size: 0.76rem; letter-spacing: 0.18em; text-transform: uppercase; color: rgba(23, 36, 49, 0.62); }
.search-sidebar h1, .result-card h2 { font-family: 'Cormorant Garamond', 'Songti SC', serif; }
.search-sidebar h1 { font-size: clamp(2.5rem, 4vw, 3.9rem); line-height: 0.95; color: #172d40; }
.search-filters { display: grid; gap: 10px; }
.filter-chip, .search-btn, .result-link { cursor: pointer; border: 1px solid rgba(23, 36, 49, 0.14); font: inherit; }
.filter-chip { padding: 12px 14px; background: rgba(255, 255, 255, 0.84); color: #172431; text-align: left; }
.filter-chip.is-active { background: rgba(31, 61, 88, 0.1); border-color: rgba(31, 61, 88, 0.24); color: #1f3d58; }
.search-stage { padding: 24px; }
.search-box { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; }
.search-input { width: 100%; padding: 16px 18px; border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(255, 255, 255, 0.92); color: #172431; font: inherit; }
.search-btn { padding: 0 22px; background: #1f3d58; border-color: #1f3d58; color: #fff; }
.search-summary { margin-top: 16px; color: rgba(23, 36, 49, 0.64); font-size: 0.9rem; }
.results-section { margin-top: 22px; }
.state-card { padding: 24px; }
.state-card--error { color: #9a4040; background: rgba(255, 243, 243, 0.86); }
.results-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.result-card { overflow: hidden; }
.result-card__image { height: 220px; }
.result-card__image img { width: 100%; height: 100%; object-fit: cover; display: block; }
.result-card__body { padding: 22px; }
.result-card__topline { display: flex; justify-content: space-between; gap: 14px; align-items: center; }
.result-meta { color: rgba(23, 36, 49, 0.6); font-size: 0.84rem; }
.result-card h2 { margin-top: 16px; font-size: 2rem; line-height: 1; }
.result-description { margin-top: 14px; color: rgba(23, 36, 49, 0.76); line-height: 1.7; font-size: 0.92rem; min-height: 3.4em; }
.result-card__footer { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-top: 18px; padding-top: 14px; border-top: 1px solid rgba(23, 36, 49, 0.1); }
.result-tag { color: #1f3d58; font-size: 0.82rem; letter-spacing: 0.12em; text-transform: uppercase; }
.result-link { padding: 10px 14px; background: rgba(255, 255, 255, 0.84); color: #172431; }
@media (max-width: 980px) { .search-layout, .results-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-shell { width: calc(100% - 24px); padding: 176px 0 64px; } .search-sidebar, .search-stage, .state-card, .result-card__body { padding: 18px; } .search-box { grid-template-columns: 1fr; } .result-card__footer, .result-card__topline { flex-direction: column; align-items: flex-start; } }
</style>
