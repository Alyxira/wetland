<template>
  <div class="detail-page" :style="themeStyle">
    <div class="background-layer">
      <div class="background-image" :style="backgroundImageStyle"></div>
      <div class="background-overlay"></div>
    </div>
    <SystemNav />

    <main class="page-shell">
      <div v-if="loading" class="state-card">
        <p>正在加载湿地详情...</p>
      </div>

      <div v-else-if="errorMessage" class="state-card">
        <p>{{ errorMessage }}</p>
      </div>

      <section v-else-if="wetland" class="detail-layout">
        <button class="back-link" type="button" @click="goBack">返回总览</button>

        <article class="hero-card">
          <div class="hero-media">
            <img :src="wetland.image" :alt="wetland.wetlandName" />
          </div>

          <div class="hero-copy">
            <span class="hero-kicker">湿地详情</span>
            <h1>{{ wetland.wetlandName }}</h1>
            <p class="hero-meta">{{ heroMeta }}</p>

            <div class="tag-list">
              <span v-for="category in wetland.categories" :key="`${wetland.id}-${category}`" class="tag-chip">
                {{ category }}
              </span>
            </div>

            <div class="hero-actions">
              <button class="data-link-button" type="button" @click="openDateDetail">
                查看水体反演数据
              </button>
            </div>
          </div>
        </article>

        <section :class="['overview-layout', { 'overview-layout--feature': hasFeatureLayout }]">
          <article :class="['story-card', { 'story-card--split': hasFeatureLayout }]">
            <div class="story-card__content">
              <div class="section-heading">
                <span>{{ overviewContent.eyebrow }}</span>
                <h2>{{ overviewContent.title }}</h2>
              </div>

              <div class="story-body">
                <p v-for="paragraph in overviewContent.paragraphs" :key="paragraph">
                  {{ paragraph }}
                </p>
              </div>
            </div>

            <div :class="['story-banner', { 'story-banner--feature': hasFeatureLayout }]">
              <img :src="wetland.image" :alt="`${wetland.wetlandName}景观`" />
            </div>
          </article>

          <aside v-if="!hasFeatureLayout" class="species-card">
            <div class="section-heading section-heading--aligned">
              <span>珍稀物种</span>
              <h3>{{ `${wetland.wetlandName}物种卡片` }}</h3>
            </div>

            <div v-if="speciesLoading" class="species-state">
              正在加载珍稀动植物...
            </div>

            <div v-else-if="speciesErrorMessage" class="species-state">
              {{ speciesErrorMessage }}
            </div>

            <div v-else-if="speciesHighlights.length === 0" class="species-state">
              暂无可展示的珍稀动植物数据。
            </div>

            <div v-else class="species-list">
              <button
                v-for="item in speciesHighlights"
                :key="item.id"
                class="species-item"
                type="button"
                @click="openFloraDetail(item)"
              >
                <div class="species-thumb">
                  <img :src="item.image" :alt="item.name" />
                </div>
                <div class="species-copy">
                  <span class="species-type">{{ item.categoryLabel }}</span>
                  <h4>{{ item.name }}</h4>
                </div>
              </button>
            </div>
          </aside>
        </section>

        <section v-if="hasFeatureLayout" class="jiuzhaigou-section">
          <div class="jiuzhaigou-content-layout">
            <article class="story-card jiuzhaigou-feature-card">
              <div class="jiuzhaigou-feature-lead">
                <div class="jiuzhaigou-feature-copy">
                  <div class="section-heading">
                    <span>{{ featureContent.eyebrow }}</span>
                    <h2>{{ featureContent.title }}</h2>
                  </div>

                  <div class="story-body">
                    <p v-for="paragraph in featureContent.paragraphs" :key="paragraph">
                      {{ paragraph }}
                    </p>
                  </div>

                  <div class="jiuzhaigou-feature-tags">
                    <span v-for="tag in featureTags" :key="tag">{{ tag }}</span>
                  </div>
                </div>

                <div class="jiuzhaigou-video-frame">
                  <video
                    class="jiuzhaigou-video"
                    :poster="featureGallery[0].image"
                    controls
                    preload="metadata"
                    playsinline
                  >
                    <source :src="featureVideo" type="video/mp4" />
                  </video>
                </div>
              </div>

              <div class="jiuzhaigou-gallery">
                <figure
                  v-for="item in featureGallery"
                  :key="item.title"
                  class="jiuzhaigou-gallery-item"
                >
                  <img :src="item.image" :alt="item.title" />
                  <figcaption>
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.caption }}</span>
                  </figcaption>
                </figure>
              </div>
            </article>

            <aside class="species-card species-card--feature">
              <div class="section-heading section-heading--aligned">
                <span>珍稀物种</span>
                <h3>{{ `${wetland.wetlandName}物种卡片` }}</h3>
              </div>

              <div v-if="speciesLoading" class="species-state">
                正在加载珍稀动植物...
              </div>

              <div v-else-if="speciesErrorMessage" class="species-state">
                {{ speciesErrorMessage }}
              </div>

              <div v-else-if="speciesHighlights.length === 0" class="species-state">
                暂无可展示的珍稀动植物数据。
              </div>

              <div v-else class="species-list">
                <button
                  v-for="item in speciesHighlights"
                  :key="item.id"
                  class="species-item"
                  type="button"
                  @click="openFloraDetail(item)"
                >
                  <div class="species-thumb">
                    <img :src="item.image" :alt="item.name" />
                  </div>
                  <div class="species-copy">
                    <span class="species-type">{{ item.categoryLabel }}</span>
                    <h4>{{ item.name }}</h4>
                  </div>
                </button>
              </div>
            </aside>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SystemNav from '../components/SystemNav.vue'
import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'
import jiuzhaigouVideo from '../resource/九寨沟/九寨沟.mp4'
import jiuzhaigouImage1 from '../resource/九寨沟/九寨沟1.jpg'
import jiuzhaigouImage2 from '../resource/九寨沟/九寨沟2.jpg'
import jiuzhaigouImage3 from '../resource/九寨沟/九寨沟3.jpg'
import honghaitanVideo from '../resource/红海滩/红海滩.mp4'
import honghaitanImage1 from '../resource/红海滩/红海滩1.jpg'
import honghaitanImage2 from '../resource/红海滩/红海滩2.jpg'
import honghaitanImage3 from '../resource/红海滩/红海滩3.jpg'

const route = useRoute()
const router = useRouter()
const CATEGORY_DEFS = {
  all: { accent: '#2d5967', accentStrong: '#173843', textPrimary: '#142a31', textSecondary: 'rgba(20, 42, 49, 0.78)', accentRgb: '45, 89, 103', navBg: 'rgba(230, 237, 240, 0.88)', navText: '#000000', navBorder: 'rgba(45, 89, 103, 0.18)', background: 'linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%)', backgroundImage: 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1800&q=80' },
  '内陆': { accent: '#4f7a47', accentStrong: '#294625', textPrimary: '#23381f', textSecondary: 'rgba(35, 56, 31, 0.78)', accentRgb: '79, 122, 71', navBg: 'rgba(230, 238, 226, 0.9)', navText: '#000000', navBorder: 'rgba(79, 122, 71, 0.18)', background: 'linear-gradient(180deg, #e5eee0 0%, #f6f2e8 100%)', backgroundImage: 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=1800&q=80' },
  '沿海': { accent: '#2e729e', accentStrong: '#1d4964', textPrimary: '#17394f', textSecondary: 'rgba(23, 57, 79, 0.78)', accentRgb: '46, 114, 158', navBg: 'rgba(226, 236, 244, 0.9)', navText: '#000000', navBorder: 'rgba(46, 114, 158, 0.18)', background: 'linear-gradient(180deg, #e2edf5 0%, #f3f6f8 100%)', backgroundImage: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1800&q=80' },
  '人造': { accent: '#ab6e31', accentStrong: '#6b441e', textPrimary: '#58361a', textSecondary: 'rgba(88, 54, 26, 0.8)', accentRgb: '171, 110, 49', navBg: 'rgba(241, 229, 216, 0.92)', navText: '#000000', navBorder: 'rgba(171, 110, 49, 0.18)', background: 'linear-gradient(180deg, #efe2d3 0%, #f7f1ea 100%)', backgroundImage: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=80' }
}

const FALLBACK_WETLAND_IMAGE = 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&q=80'
const FALLBACK_SPECIES_IMAGE = 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=900&q=80'
const jiuzhaigouGallery = [
  {
    image: jiuzhaigouImage1,
    title: '湖泊与钙华水色',
    caption: '以通透湖水和层叠湖盆展现九寨沟最具辨识度的湿地景观。'
  },
  {
    image: jiuzhaigouImage2,
    title: '森林与沟谷水系',
    caption: '沟谷中的林地、溪流与浅滩共同构成连续的高山湿地生态界面。'
  },
  {
    image: jiuzhaigouImage3,
    title: '山地湿地空间层次',
    caption: '通过不同视角补充九寨沟湿地在地形与水文上的多层次变化。'
  }
]
const honghaitanGallery = [
  {
    image: honghaitanImage1,
    title: '潮滩与红色植被',
    caption: '碱蓬随季节变色，与潮沟和滩涂共同形成红海滩最具代表性的滨海湿地景观。'
  },
  {
    image: honghaitanImage2,
    title: '芦苇湿地与候鸟通道',
    caption: '大面积芦苇沼泽与近海潮滩相连，为迁徙鸟类提供停歇、觅食和栖息空间。'
  },
  {
    image: honghaitanImage3,
    title: '滨海水陆交错界面',
    caption: '海水、泥滩、植被与浅水湿地交错分布，构成红海滩独特的海岸带生态纹理。'
  }
]
const wetland = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const speciesHighlights = ref([])
const speciesLoading = ref(false)
const speciesErrorMessage = ref('')

const normalizeCategories = (tags) => {
  const source = String(tags || '').split(/[、,，/|\s]+/).map((item) => item.trim()).filter(Boolean)
  const categories = []
  const addCategory = (value) => {
    if (CATEGORY_DEFS[value] && !categories.includes(value)) categories.push(value)
  }
  source.forEach((item) => {
    if (item.includes('内陆')) addCategory('内陆')
    if (item.includes('沿海')) addCategory('沿海')
    if (item.includes('人造')) addCategory('人造')
  })
  if (categories.length === 0) addCategory('内陆')
  return categories
}

const formatDateTime = (value) => {
  if (!value) return 'Recent'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Recent'
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

const currentTheme = computed(() => CATEGORY_DEFS[wetland.value?.categories?.[0] || 'all'] || CATEGORY_DEFS.all)
const themeStyle = computed(() => ({
  '--theme-accent': currentTheme.value.accent,
  '--theme-accent-strong': currentTheme.value.accentStrong,
  '--theme-accent-rgb': currentTheme.value.accentRgb,
  '--theme-text-primary': currentTheme.value.textPrimary,
  '--theme-text-secondary': currentTheme.value.textSecondary,
  '--theme-background': currentTheme.value.background,
  '--nav-bg': currentTheme.value.navBg,
  '--nav-text': currentTheme.value.navText,
  '--nav-border': currentTheme.value.navBorder
}))
const backgroundImageStyle = computed(() => ({ backgroundImage: `url(${currentTheme.value.backgroundImage})` }))
const isJiuzhaigou = computed(() => (wetland.value?.wetlandName || '').includes('九寨沟'))
const isHonghaitan = computed(() => (wetland.value?.wetlandName || '').includes('红海滩'))
const hasFeatureLayout = computed(() => isJiuzhaigou.value || isHonghaitan.value)
const heroMeta = computed(() => {
  if (!wetland.value) return ''
  return isJiuzhaigou.value
    ? '四川阿坝州 · 高山湖泊湿地群落 · 世界自然遗产核心景观带'
    : isHonghaitan.value
    ? '辽宁盘锦 · 滨海潮滩湿地系统 · 典型海岸带红色景观'
    : wetland.value.coordinateRange || '坐标信息待补充'
})
const overviewContent = computed(() => {
  if (!wetland.value) {
    return { eyebrow: '', title: '', paragraphs: [] }
  }
  if (isJiuzhaigou.value) {
    return {
      eyebrow: '介绍',
      title: '九寨沟湿地景观概览',
      paragraphs: [
        '九寨沟位于四川省阿坝藏族羌族自治州九寨沟县境内，湿地系统分布在高山峡谷之间，森林、湖泊、溪流、滩地与沼泽相互嵌套，构成层次极为丰富的冷水湿地景观。',
        '以长海、五花海、箭竹海、镜海等湖沼湿地为核心，清澈的钙华水体串联起多级湖盆和沟谷水网，不仅塑造了标志性的色彩景观，也承担着重要的蓄水、补给与生态调节功能。',
        '九寨沟湿地是岷山山系生物多样性的重要载体，为大熊猫、川金丝猴、绿尾虹雉等珍稀动物提供栖息和迁徙通道，同时孕育了箭竹、红景天等适应高海拔环境的湿地植物群落。'
      ]
    }
  }
  if (isHonghaitan.value) {
    return {
      eyebrow: '介绍',
      title: '红海滩湿地景观概览',
      paragraphs: [
        '红海滩位于辽宁省盘锦市滨海区域，是由大面积潮滩、浅海水域、沼泽湿地与芦苇群落共同组成的典型滨海湿地系统。每到生长季，碱蓬植物逐渐转为鲜红色，形成极具辨识度的海岸湿地景观。',
        '这里的潮沟网络、盐沼植被和近海滩涂相互交织，不仅塑造出独特的色彩层次，也在调蓄水体、稳定海岸、维持盐湿环境方面发挥着重要作用。',
        '红海滩湿地还是东亚至澳大利西亚候鸟迁飞通道上的重要停歇地，为丹顶鹤、黑嘴鸥等珍稀鸟类以及多类鱼蟹、底栖生物提供栖息、繁殖和觅食空间。'
      ]
    }
  }
  return {
    eyebrow: '介绍',
    title: `${wetland.value.wetlandName}湿地概览`,
    paragraphs: [
      wetland.value.description || '暂无详细介绍。'
    ]
  }
})
const featureContent = computed(() => {
  if (isJiuzhaigou.value) {
    return {
      eyebrow: '影像补充',
      title: '九寨沟湿地景观影像',
      paragraphs: [
        '九寨沟湿地位于岷山南段高山峡谷地带，湖泊、溪流、滩地与森林沿沟谷连续展开，形成层层递进的冷水湿地景观。清澈水体与钙华地貌共同塑造了九寨沟最具辨识度的自然面貌。',
        '在景观价值之外，这片湿地还承担着重要的水源涵养、生态调节与生境维系功能，为高海拔植物群落和珍稀野生动物提供相对稳定的栖息环境。'
      ]
    }
  }
  if (isHonghaitan.value) {
    return {
      eyebrow: '影像补充',
      title: '红海滩湿地景观影像',
      paragraphs: [
        '红海滩湿地以滨海潮滩和盐沼植被为核心特征，大片碱蓬在季节变化中呈现鲜明的红色，与芦苇荡、潮沟水面和浅滩共同构成极具辨识度的海岸带湿地景观。',
        '这种由海陆交错塑造出的生态空间，不仅拥有鲜明的观赏价值，也维系着鸟类迁徙、鱼蟹繁殖和海岸湿地稳定等多重生态功能。'
      ]
    }
  }
  return { eyebrow: '', title: '', paragraphs: [] }
})
const featureGallery = computed(() => (isJiuzhaigou.value ? jiuzhaigouGallery : honghaitanGallery))
const featureVideo = computed(() => (isJiuzhaigou.value ? jiuzhaigouVideo : honghaitanVideo))
const featureTags = computed(() =>
  isJiuzhaigou.value
    ? ['高山湖泊', '森林水系', '湿地景观']
    : ['滨海潮滩', '盐沼植被', '候鸟栖息']
)
const goBack = () => router.push({ name: 'Overview' })
const openDateDetail = () => {
  if (!wetland.value?.id) return
  router.push({ name: 'DateDetail', params: { id: String(wetland.value.id) } })
}
const openFloraDetail = (item) => {
  if (!item?.id || !wetland.value?.id) return
  router.push({
    name: 'FloraDetail',
    params: { id: String(item.id) },
    query: { wetlandId: String(wetland.value.id) }
  })
}

const shuffleArray = (list) => {
  const next = [...list]
  for (let index = next.length - 1; index > 0; index -= 1) {
    const randomIndex = Math.floor(Math.random() * (index + 1))
    ;[next[index], next[randomIndex]] = [next[randomIndex], next[index]]
  }
  return next
}

const deriveSpeciesCategory = (item) => {
  const source = `${item?.category || ''} ${item?.type || ''} ${item?.tags || ''}`.toLowerCase()
  if (source.includes('鸟')) return '鸟类'
  if (source.includes('兽') || source.includes('哺乳') || source.includes('猴') || source.includes('熊')) return '哺乳动物'
  if (source.includes('鱼')) return '鱼类'
  if (source.includes('两栖')) return '两栖动物'
  if (source.includes('爬行')) return '爬行动物'
  if (source.includes('植物') || source.includes('花') || source.includes('树') || source.includes('竹')) return '植物'
  return '珍稀物种'
}

const loadSpeciesHighlights = async (wetlandId) => {
  if (!wetlandId) {
    speciesHighlights.value = []
    return
  }
  speciesLoading.value = true
  speciesErrorMessage.value = ''
  try {
    const response = await api.get(`/wetlands/${wetlandId}/flora-fauna`)
    const records = response.data?.floraFaunas || []
    const prepared = records
      .filter((item) => item?.id || item?._id)
      .map((item) => ({
        ...item,
        id: item.id || item._id,
        image: resolveAssetUrl(item.imagePath || item.image, FALLBACK_SPECIES_IMAGE),
        categoryLabel: deriveSpeciesCategory(item)
      }))

    const count = prepared.length <= 3 ? prepared.length : Math.min(prepared.length, 3 + Math.floor(Math.random() * 2))
    speciesHighlights.value = shuffleArray(prepared).slice(0, count)
  } catch (error) {
    console.error('加载珍稀动植物失败:', error)
    speciesErrorMessage.value = '珍稀动植物数据加载失败，请稍后重试。'
    speciesHighlights.value = []
  } finally {
    speciesLoading.value = false
  }
}

const loadWetlandDetail = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await api.get('/wetlands')
    const records = response.data?.wetlands || []
    const matched = records.find((item) => String(item.id) === String(route.params.id || ''))
    if (!matched) {
      wetland.value = null
      errorMessage.value = '未找到对应的湿地信息。'
      return
    }
    const categories = normalizeCategories(matched.tags)
    wetland.value = { ...matched, categories, image: resolveAssetUrl(matched.imagePath, FALLBACK_WETLAND_IMAGE), createdTimeText: formatDateTime(matched.createdTime) }
    await loadSpeciesHighlights(matched.id)
  } catch (error) {
    console.error('加载湿地详情失败:', error)
    errorMessage.value = '湿地详情加载失败，请稍后重试。'
    speciesHighlights.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadWetlandDetail)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.detail-page { --theme-accent: #2d5967; --theme-accent-strong: #173843; --theme-accent-rgb: 45, 89, 103; --theme-text-primary: #142a31; --theme-text-secondary: rgba(20, 42, 49, 0.78); --theme-background: linear-gradient(180deg, #eef3f4 0%, #f7f4ee 100%); min-height: 100vh; position: relative; background: var(--theme-background); color: var(--theme-text-primary); font-family: 'Manrope', 'PingFang SC', sans-serif; }
.background-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.background-image, .background-overlay { position: absolute; inset: 0; }
.background-image { background-position: center; background-size: cover; filter: saturate(0.74) contrast(1.02); transform: scale(1.04); }
.background-overlay { background: linear-gradient(180deg, rgba(248, 246, 240, 0.74) 0%, rgba(246, 243, 236, 0.9) 26%, rgba(244, 240, 232, 0.96) 100%), linear-gradient(90deg, rgba(var(--theme-accent-rgb), 0.08) 0, rgba(var(--theme-accent-rgb), 0.08) 1px, transparent 1px, transparent 132px); }
.page-shell { position: relative; z-index: 1; width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 188px 0 72px; }
.detail-layout { display: grid; gap: 20px; }
.back-link { justify-self: start; border: none; background: transparent; color: rgba(var(--theme-accent-rgb), 0.9); font-size: 0.8rem; letter-spacing: 0.18em; text-transform: uppercase; cursor: pointer; }
.state-card, .hero-card, .story-card, .species-card { border: 1px solid rgba(var(--theme-accent-rgb), 0.14); background: rgba(255, 255, 255, 0.76); backdrop-filter: blur(14px); }
.state-card { padding: 24px; }
.hero-card { display: grid; grid-template-columns: minmax(0, 1.04fr) minmax(0, 0.96fr); overflow: hidden; }
.hero-media { min-height: 580px; }
.hero-media img, .story-banner img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-copy { display: grid; align-content: start; gap: 18px; padding: 32px; border-left: 1px solid rgba(var(--theme-accent-rgb), 0.12); }
.hero-kicker, .hero-meta, .tag-chip, .section-heading span, .species-type { font-size: 0.76rem; letter-spacing: 0.16em; text-transform: uppercase; }
.hero-kicker, .hero-meta, .section-heading span, .species-type { color: var(--theme-text-secondary); }
.hero-copy h1, .section-heading h2, .section-heading h3 { font-family: 'Bodoni MT', 'Didot', 'Cormorant Garamond', 'Songti SC', serif; color: var(--theme-accent-strong); }
.hero-copy h1 { font-size: clamp(3.2rem, 5vw, 5.4rem); line-height: 0.92; }
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip { padding: 6px 10px; border: 1px solid rgba(var(--theme-accent-rgb), 0.18); color: var(--theme-accent-strong); }
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.data-link-button { border: 1px solid rgba(var(--theme-accent-rgb), 0.24); background: rgb(var(--theme-accent-rgb)); color: #fff; padding: 12px 18px; font-size: 0.82rem; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; cursor: pointer; transition: transform 0.24s ease, box-shadow 0.24s ease, opacity 0.24s ease; }
.data-link-button:hover { transform: translateY(-2px); box-shadow: 0 12px 24px rgba(var(--theme-accent-rgb), 0.18); }
.overview-layout { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.8fr); gap: 20px; align-items: start; }
.overview-layout--feature { grid-template-columns: 1fr; }
.story-card { display: grid; gap: 22px; padding: 28px; }
.story-card__content { display: grid; gap: 22px; align-content: start; }
.story-card--split { grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr); gap: 24px; align-items: stretch; }
.section-heading { display: grid; gap: 8px; }
.section-heading--aligned { min-height: 82px; align-content: start; }
.section-heading h2 { font-size: clamp(2rem, 3vw, 3rem); line-height: 1; }
.section-heading h3 { font-size: 2rem; line-height: 1.05; }
.story-body { display: grid; gap: 16px; }
.story-body p { font-size: 1rem; line-height: 1.9; color: var(--theme-text-primary); }
.story-banner { overflow: hidden; min-height: 180px; border: 1px solid rgba(var(--theme-accent-rgb), 0.12); }
.story-banner--feature { min-height: 100%; }
.species-card { display: grid; gap: 18px; padding: 24px; }
.species-card--feature { align-content: start; min-height: 100%; padding-left: 18px; padding-right: 18px; }
.species-state { padding: 18px; border: 1px solid rgba(var(--theme-accent-rgb), 0.1); background: rgba(255, 255, 255, 0.68); line-height: 1.8; color: var(--theme-text-primary); }
.species-list { display: grid; gap: 14px; }
.species-item { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 14px; padding: 0; background: rgba(255, 255, 255, 0.7); border: 1px solid rgba(var(--theme-accent-rgb), 0.1); text-align: left; cursor: pointer; transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease; }
.species-item:hover { transform: translateY(-2px); box-shadow: 0 14px 28px rgba(var(--theme-accent-rgb), 0.12); border-color: rgba(var(--theme-accent-rgb), 0.24); }
.species-thumb { min-height: 92px; background: rgba(var(--theme-accent-rgb), 0.08); }
.species-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.species-copy { display: grid; align-content: center; gap: 8px; padding: 14px 14px 14px 0; }
.species-item h4 { font-size: 1.15rem; color: var(--theme-accent-strong); }
.jiuzhaigou-section { display: grid; }
.jiuzhaigou-content-layout { display: grid; grid-template-columns: minmax(0, 1.18fr) minmax(220px, 0.5fr); gap: 20px; align-items: start; }
.jiuzhaigou-feature-card { gap: 24px; }
.jiuzhaigou-feature-lead { display: grid; grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr); gap: 24px; align-items: stretch; }
.jiuzhaigou-video-frame { overflow: hidden; border: 1px solid rgba(var(--theme-accent-rgb), 0.12); background: rgba(var(--theme-accent-rgb), 0.06); min-height: 360px; }
.jiuzhaigou-video { width: 100%; height: 100%; display: block; object-fit: cover; background: #000; }
.jiuzhaigou-feature-copy { display: grid; align-content: start; gap: 18px; padding: 4px 0; }
.jiuzhaigou-feature-tags { display: flex; flex-wrap: wrap; gap: 10px; }
.jiuzhaigou-feature-tags span { padding: 6px 10px; border: 1px solid rgba(var(--theme-accent-rgb), 0.14); background: rgba(255, 255, 255, 0.58); color: var(--theme-accent-strong); font-size: 0.74rem; letter-spacing: 0.14em; text-transform: uppercase; }
.jiuzhaigou-gallery { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; padding-top: 4px; border-top: 1px solid rgba(var(--theme-accent-rgb), 0.1); }
.jiuzhaigou-gallery-item { overflow: hidden; border: 1px solid rgba(var(--theme-accent-rgb), 0.1); background: rgba(255, 255, 255, 0.56); transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease; }
.jiuzhaigou-gallery-item:hover { transform: translateY(-2px); box-shadow: 0 14px 28px rgba(var(--theme-accent-rgb), 0.08); border-color: rgba(var(--theme-accent-rgb), 0.18); }
.jiuzhaigou-gallery-item img { width: 100%; height: 220px; object-fit: cover; display: block; }
.jiuzhaigou-gallery-item figcaption { display: grid; gap: 6px; padding: 14px; }
.jiuzhaigou-gallery-item strong { font-family: 'Bodoni MT', 'Didot', 'Cormorant Garamond', 'Songti SC', serif; font-size: 1.18rem; line-height: 1.08; color: var(--theme-accent-strong); }
.jiuzhaigou-gallery-item span { line-height: 1.7; color: var(--theme-text-primary); font-size: 0.92rem; }
@media (max-width: 960px) { .hero-card, .overview-layout, .story-card--split, .jiuzhaigou-content-layout { grid-template-columns: 1fr; } .hero-copy { border-left: none; border-top: 1px solid rgba(var(--theme-accent-rgb), 0.12); } .hero-media { min-height: 320px; } }
@media (max-width: 960px) { .jiuzhaigou-feature-lead, .jiuzhaigou-gallery { grid-template-columns: 1fr; } .jiuzhaigou-video-frame { min-height: 300px; } .story-banner--feature { min-height: 280px; } }
@media (max-width: 720px) { .page-shell { width: calc(100% - 24px); padding: 176px 0 64px; } .hero-copy, .story-card, .species-card, .state-card { padding: 18px; } .hero-copy h1 { font-size: clamp(2.5rem, 12vw, 4rem); } .section-heading h2, .section-heading h3 { font-size: 1.8rem; } .species-item { grid-template-columns: 78px minmax(0, 1fr); } .species-thumb { min-height: 78px; } .jiuzhaigou-gallery-item img { height: 220px; } }
</style>
