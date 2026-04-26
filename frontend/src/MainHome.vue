<template>
  <div class="home-page" ref="pageContainer" @mousemove="handleMouseMove">
    <div class="home-background-layer" aria-hidden="true">
      <div class="home-background-image"></div>
      <div class="home-background-overlay"></div>
    </div>

    <transition name="toast-fade">
      <div v-if="toast.show" class="home-toast" :class="toast.type">
        {{ toast.msg }}
      </div>
    </transition>

    <transition name="top-button-fade">
      <button
        v-if="showBackToTop"
        class="back-to-top-button"
        @click="scrollToTop"
        aria-label="回到顶部"
      >
        <span class="back-to-top-button__label">回到顶部</span>
        <span class="back-to-top-button__hint">进入系统</span>
      </button>
    </transition>

    <div class="custom-cursor-aura" :style="{ transform: `translate3d(${mouseX}px, ${mouseY}px, 0)` }"></div>

    <header :class="['home-nav', { 'is-scrolled': isNavSolid }]">
      <button class="brand-mark" @click="scrollToTop" aria-label="返回顶部">
        <span class="nav-logo-mark">W</span>
      </button>

      <div class="nav-actions">
        <template v-if="isLoggedIn">
          <button class="nav-link" @click="goTo('/profile')">个人中心</button>
        </template>
        <template v-else>
          <button class="nav-link" @click="goToAuth('login')">登录</button>
          <button class="nav-link" @click="goToAuth('register')">注册</button>
        </template>
      </div>
    </header>

    <main>
      <section class="hero-section home-panel" ref="heroSection">
        <video
          class="hero-video"
          :src="heroVideo"
          autoplay
          muted
          loop
          playsinline
          preload="auto"
        ></video>
        <div class="hero-noise"></div>

        <div class="hero-content page-shell">
          <div class="hero-copy reveal-up">
            <h1 class="editorial-title hero-title">
              <span class="title-line title-line-main" v-y-slice>让湿地的呼吸</span>
              <span class="title-line title-line-accent" v-y-slice>先被看见</span>
            </h1>

            <div class="hero-actions">
              <button class="text-entry-link" @click="handleProtectedNavigation">
                <span>点击进入系统</span>
              </button>
            </div>
          </div>
        </div>

        <button class="scroll-cue" @click="scrollToIntro" aria-label="滚动到总览">
          <span>下滑</span>
          <span class="scroll-line"></span>
        </button>
      </section>

      <section class="project-intro-section home-panel" ref="introSection">
        <HomeMediaNewsSection
          :video-src="introFeaturedVideo"
          :news-links="externalNewsLinks"
        />
      </section>

      <section
        v-for="(section, index) in projectSections"
        :key="section.kicker"
        :class="[
          section.layout === 'process'
            ? 'wetland-process-section home-panel'
            : section.layout === 'communityCarousel'
            ? 'community-comments-section home-panel'
            : section.layout === 'wildlifeGallery'
            ? 'wildlife-gallery-section home-panel'
            : section.layout === 'cards'
            ? 'immersive-gallery-section home-panel'
            : 'story-section home-panel',
          { 'is-reversed': index % 2 === 1 && !section.layout }
        ]"
        :ref="index === 0 ? setFirstStorySection : undefined"
      >
        <div
          v-if="section.layout === 'process'"
          class="page-shell wetland-process-shell"
        >
          <div ref="processSectionRef" class="wetland-process-stage">
            <div class="wetland-process-viewport">
              <div ref="processTitleRef" class="wetland-process-intro">
                <span class="section-kicker wetland-process-kicker">{{ section.kicker }}</span>
                <h2>{{ section.title }}</h2>
              </div>

              <div ref="processCardsWrapperRef" class="wetland-process-cards">
                <article
                  v-for="(card, cardIndex) in processCards"
                  :key="card.title"
                  :ref="(el) => setProcessCardRef(el, cardIndex)"
                  class="wetland-process-card"
                >
                  <div class="wetland-process-card__inner">
                    <div
                      class="wetland-process-card__face wetland-process-card__face--front"
                      :style="{
                        '--card-image': `url(${processPanoramaImage})`,
                        '--card-position': card.imagePosition
                      }"
                    >
                      <div class="wetland-process-card__overlay"></div>
                      <div class="wetland-process-card__front-copy">
                        <span class="wetland-process-card__index">{{ card.index }}</span>
                        <span class="wetland-process-card__eyebrow">{{ card.eyebrow }}</span>
                        <h3>{{ card.title }}</h3>
                      </div>
                    </div>

                    <div class="wetland-process-card__face wetland-process-card__face--back">
                      <span class="wetland-process-card__index">{{ card.index }}</span>
                      <span class="wetland-process-card__eyebrow">{{ card.eyebrow }}</span>
                      <h3>{{ card.title }}</h3>
                      <p>{{ card.description }}</p>
                      <strong>{{ card.metric }}</strong>
                    </div>
                  </div>
                </article>
              </div>

            </div>
          </div>
        </div>

        <div v-else-if="section.layout === 'communityCarousel'" class="page-shell community-comments-panel">
          <div class="community-comments-header reveal-up">
            <span class="section-kicker">{{ section.kicker }}</span>
            <h2>{{ section.title }}</h2>
            <p>{{ section.desc }}</p>
          </div>

          <div class="community-comments-stage reveal-up">
            <div class="community-comments-line" aria-hidden="true"></div>

            <div class="community-comment-frame">
              <div
                class="community-comment-content"
                :style="{
                  '--comment-accent': activeCommunityComment.accent,
                  '--comment-fg': activeCommunityComment.textColor
                }"
              >
                <div class="community-comment-media">
                  <img
                    :src="activeCommunityComment.image"
                    :alt="activeCommunityComment.title"
                    class="community-comments-image"
                  />
                </div>

                <div class="community-comment-copy">
                  <div class="community-comment-topline">
                    <span>0{{ currentCommunityCommentIndex + 1 }}</span>
                    <span>{{ activeCommunityComment.tag }}</span>
                  </div>

                  <h3>{{ activeCommunityComment.title }}</h3>
                  <p>{{ activeCommunityComment.desc }}</p>

                  <div class="community-comment-meta">
                    <strong>{{ activeCommunityComment.author }}</strong>
                    <span>{{ activeCommunityComment.note }}</span>
                  </div>

                  <div class="community-comment-controls">
                    <div class="community-comment-dots" aria-hidden="true">
                      <span
                        v-for="(comment, commentIndex) in communityReviewCards"
                        :key="comment.title"
                        :class="['comment-dot', { 'is-active': commentIndex === currentCommunityCommentIndex }]"
                      ></span>
                    </div>
                    <div class="community-comment-buttons">
                      <button class="comment-nav-button comment-nav-button--square" @click="showPrevComment" aria-label="Previous comment">
                        &larr;
                      </button>
                      <button class="comment-nav-button comment-nav-button--square" @click="showNextComment" aria-label="Next comment">
                        &rarr;
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="community-comments-line" aria-hidden="true"></div>
          </div>
        </div>

        <div v-else-if="section.layout === 'wildlifeGallery'" class="page-shell wildlife-gallery-panel">
          <div class="wildlife-gallery-header reveal-up">
            <span class="section-kicker">{{ section.kicker }}</span>
            <h2>{{ section.title }}</h2>
            <p>{{ section.desc }}</p>
          </div>

          <div class="wildlife-gallery-stage reveal-up">
            <div class="wildlife-hero">
              <img
                :src="section.featuredSpecies?.image || section.image"
                :alt="section.featuredSpecies?.name || section.title"
                class="wildlife-hero__image"
              />
              <div class="wildlife-hero__overlay">
                <em v-if="section.featuredSpecies?.category">{{ section.featuredSpecies.category }}</em>
                <h3>{{ section.featuredSpecies?.name || section.title }}</h3>
              </div>
            </div>

            <div class="wildlife-species-grid">
              <article
                v-for="species in section.species"
                :key="species.name"
                class="wildlife-species-card"
              >
                <div class="wildlife-species-card__media">
                  <img :src="species.image" :alt="species.name" />
                </div>
                <div class="wildlife-species-card__body">
                  <span>{{ species.category }}</span>
                  <h3>{{ species.name }}</h3>
                  <strong>{{ species.feature }}</strong>
                </div>
              </article>
            </div>
          </div>

        </div>

        <div v-else class="page-shell story-grid">
          <div class="story-media reveal-up">
            <div class="story-visual">
              <img :src="section.image" :alt="section.title" />
            </div>
          </div>

          <div class="story-copy reveal-up">
            <span class="section-kicker">{{ section.kicker }}</span>
            <h2>{{ section.title }}</h2>
            <p>{{ section.desc }}</p>
            <div class="story-meta">
              <span>{{ section.overlay }}</span>
              <strong>{{ section.overlayValue }}</strong>
            </div>

            <div class="story-highlights">
              <article
                v-for="highlight in section.highlights"
                :key="highlight.label"
                class="highlight-card"
              >
                <span>{{ highlight.label }}</span>
                <strong>{{ highlight.value }}</strong>
                <p>{{ highlight.note }}</p>
              </article>
            </div>
          </div>
        </div>
      </section>

      <footer class="system-footer reveal-up">
        <div class="system-footer__inner">
          <div class="entry-copy" data-y-slice-skip>
            <span class="section-kicker footer-kicker">System Information</span>
            <h2 v-y-slice>Wetland 2026</h2>
            <p class="footer-copy">生态影像、数字导览与公众参与的湿地信息系统。</p>
          </div>

          <div class="system-info-list" data-y-slice-skip>
            <div class="system-info-item">
              <span>联系电话</span>
              <strong>12345678</strong>
            </div>
            <div class="system-info-item">
              <span>系统编号</span>
              <strong>Wetland2026</strong>
            </div>
            <div class="system-info-item">
              <span>出品单位</span>
              <strong>长江大学 Yangtze University</strong>
            </div>
          </div>
        </div>
      </footer>

    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import Lenis from 'lenis'
import HomeMediaNewsSection from './components/HomeMediaNewsSection.vue'
import heroVideo from './resource/wetlend.mp4'
import introFeaturedVideo from './resource/湿地讲话.mp4'
import api from './utils/api'
import { resolveAssetUrl } from './utils/assets'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()
const pageContainer = ref(null)
const heroSection = ref(null)
const introSection = ref(null)
const firstStorySection = ref(null)
const processSectionRef = ref(null)
const processTitleRef = ref(null)
const processCardsWrapperRef = ref(null)
const processCardRefs = ref([])

const isNavSolid = ref(false)
const showBackToTop = ref(false)
const isLoggedIn = ref(!!localStorage.getItem('auth_token'))
const toast = ref({ show: false, msg: '', type: 'success' })
const mouseX = ref(-100)
const mouseY = ref(-100)

let toastTimer = null
let ctx = null
let animationFrameId = null
let lenis = null
let lenisRaf = null

// 首屏下方媒体与新闻区：后续直接在这里追加新闻对象即可
const externalNewsLinks = ref([
  {
    title: '面向全球：珍爱湿地 守护未来 推进湿地保护全球行动',
    source: '《湿地公约》第十四届缔约方大会视频致辞',
    date: '2022年11月5日',
    summary: '这里填写这条新闻的简短说明，建议控制在一到两句话。',
    tag: 'External Link',
    url: 'https://www.gov.cn/xinwen/2022-11/05/content_5724843.htm?f_link_type=f_linkinlinenote&flow_extra=eyJpbmxpbmVfZGlzcGxheV9wb3NpdGlvbiI6MCwiZG9jX3Bvc2l0aW9uIjowLCJkb2NfaWQiOiI2MTYxNjg2MTkwMTljNjk1LWZjZGNhYmQyYmE4OTBkZTIifQ%3D%3D'
  },
  {
    title: '考察西溪湿地：以生态保护为先，让湿地成为群众共享绿意空间',
    source: '考察杭州西溪国家湿地公园',
    date: '2020年3月31日',
    summary: '继续添加外部新闻时，保持 title、source、date、summary、tag、url 这几个字段即可。',
    tag: 'External Link',
    url: 'https://news.cctv.cn/special/whsdygyh/?f_link_type=f_linkinlinenote&flow_extra=eyJkb2NfcG9zaXRpb24iOjAsImRvY19pZCI6ImE4YThjZDRhMmZmZTY0YmUtMmNkM2Q0Yzc2NmE4MmQ1YyIsImlubGluZV9kaXNwbGF5X3Bvc2l0aW9uIjowfQ%3D%3D'
  },
  {
    title: '党的二十大报告：推行湿地休养生息，践行绿色发展理念',
    source: '党的二十大报告',
    date: '2022年10月',
    summary: '如果之后接后端接口，也可以把这个数组替换成接口返回的数据。',
    tag: 'External Link',
    url: 'https://www.gov.cn/xinwen/2022-11/08/content_5725240.htm?f_link_type=f_linkinlinenote&flow_extra=eyJpbmxpbmVfZGlzcGxheV9wb3NpdGlvbiI6MCwiZG9jX3Bvc2l0aW9uIjowLCJkb2NfaWQiOiI0NTBhMzhlOTExYmUyMWZmLWU3NTUwOTdjNzUxNzJjMjcifQ%3D%3D'
  },
  {
    title: '守住湿地生态安全边界，留大美湿地予子孙',
    source: '2026年世界湿地日',
    date: '2026年2月2日',
    summary: '这里是新增的第四条新闻占位，后续可直接替换成真实外链。',
    tag: 'External Link',
    url: 'https://news.cctv.cn/2026/02/02/ARTIlmPlDPG0SAbwuEeC1t3Y260202.shtml?f_link_type=f_linkinlinenote&flow_extra=eyJpbmxpbmVfZGlzcGxheV9wb3NpdGlvbiI6MCwiZG9jX3Bvc2l0aW9uIjowLCJkb2NfaWQiOiIxZGY5MDBmMDdhOTM1ZjUzLWU4NjFlYTFjZjEwYjIyOTkifQ%3D%3D'
  }
])

// --- 项目数据 ---
const projectSections = ref([
  {
    layout: 'process',
    kicker: '湿地体验',
    title: '在系统中，按一条完整的路径领略湿地风光。',
    desc: '我们希望用户看到的不是孤立的图片，而是一种逐步展开的湿地体验。从入口画面、空间浏览到知识理解与个人停留，系统会把风景、信息和互动连接成一条更完整的旅程。'
  },
  {
    layout: 'communityCarousel',
    kicker: 'Community Archive',
    title: '社区与个人记录模块',
    desc: '把用户的分享、收藏、历史与个人档案纳入同一套系统，让湿地体验从浏览扩展到参与、记录与持续连接。',
    image: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1400&q=80',
    overlay: 'Community Signal',
    overlayValue: 'Profile Connected',
    highlights: [
      { label: '互动社区', value: 'Share Feed', note: '沉淀用户动态、图片记录与讨论内容。' },
      { label: '个人档案', value: 'Profile OS', note: '统一管理收藏、历史、足迹与系统身份。' }
    ]
  },
  {
    layout: 'wildlifeGallery',
    kicker: '',
    title: '珍稀动植物科普模块',
    desc: '这一部分聚焦具有代表性的珍稀鸟类、珍贵湿地植物与重点保护动物，让用户在浏览景观之外，也能进一步理解湿地保护真正需要守护的生命对象。',
    image: 'https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1400&q=80',
    featuredSpecies: {
      category: 'Bird',
      name: '东方白鹳 Oriental Stork',
      image: 'https://images.unsplash.com/photo-1444464666168-49d633b86797?auto=format&fit=crop&w=900&q=80'
    },
    species: [
      {
        category: 'Plant',
        name: '水杉 Dawn Redwood',
        image: 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=900&q=80'
      },
      {
        category: 'Animal',
        name: '麋鹿 Pere David\'s Deer',
        image: 'https://images.unsplash.com/photo-1465101046530-73398c7f28ca?auto=format&fit=crop&w=900&q=80'
      },
      {
        category: 'Bird',
        name: '白鹭 Little Egret',
        image: 'https://images.unsplash.com/photo-1552728089-57bdde30beb3?auto=format&fit=crop&w=900&q=80'
      }
    ]
  }
])

const processPanoramaImage = ref('https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=2400&q=86')

const processCards = ref([
  {
    index: '01',
    eyebrow: '湿地总览',
    title: '总览',
    description: '系统整合湿地地理环境、水文特征、生态价值与人文背景，以清晰易懂的形式呈现湿地全貌。让用户快速了解这片湿地的形成历史、生态功能与保护意义，建立对湿地整体的认知，为后续游览与探索打下基础。',
    metric: '总览信息',
    imagePosition: '0% center'
  },
  {
    index: '02',
    eyebrow: '云游展示',
    title: '云游',
    description: '依托高清影像与全景视觉呈现，打造沉浸式虚拟云游体验。无需抵达现场，即可滑动浏览湿地四季风光、水域景观与滩涂风貌，感受湿地在不同时段的光影变化与自然肌理，实现全天候、零距离的云上观景。',
    metric: '云游场景',
    imagePosition: '33.333% center'
  },
  {
    index: '03',
    eyebrow: '路线规划',
    title: '路线',
    description: '根据湿地景点分布、生态节点与游览时长，智能生成多条特色游览路线。用户可按需选择休闲漫步线、生态观光线、科普研学线等，路线串联核心景观与知识点，兼顾观赏性与实用性，让游览更高效、更有目的性。',
    metric: '导览学习',
    imagePosition: '66.666% center'
  },
  {
    index: '04',
    eyebrow: '生态展示',
    title: '生态',
    description: '聚焦湿地本土物种，系统展示鸟类、植物、两栖类等生物的形态特征、生活习性与生态作用。搭配实景图片与通俗解读，让用户在游览过程中认识湿地生灵、理解生态链关系，在欣赏风景的同时收获自然科普知识。',
    metric: '生态档案',
    imagePosition: '100% center'
  }
])

const communityReviewCards = ref([
  {
    title: '清晨的芦苇和薄雾，让我第一次真正感到湿地是活着的。',
    desc: '从总览页进入之后，系统给出的路线和实时画面很自然，我几乎没有学习成本，就能一路看到景观和生态信息之间的联系。',
    image: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80',
    author: '林栖',
    note: '观鸟路线体验者',
    tag: '热门评论',
    color: '#1e3a34',
    textColor: '#f5f0e6',
    accent: '#b8d9c6'
  },
  {
    title: '社区里的分享不是碎片，而是把每一次观察都连成了故事。',
    desc: '我会点开别人的路线、停留点和拍摄时间，再回到自己的记录里对照看，整个体验像在一起共同拼出这片湿地的记忆。',
    image: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80',
    author: 'River Note',
    note: '湿地巡护志愿者',
    tag: '社区精选',
    color: '#e7dbc0',
    textColor: '#181614',
    accent: '#7e6743'
  },
  {
    title: '收藏、历史和动态都被放进同一套界面里，这点特别顺手。',
    desc: '不是看完就结束，而是每次回来都能接着上次的足迹继续浏览。这个系统最打动我的地方，就是它让参与变得持续而自然。',
    image: 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&q=80',
    author: 'Wetland Walker',
    note: '长期社区用户',
    tag: '用户反馈',
    color: '#8d5636',
    textColor: '#fff6ec',
    accent: '#efc7aa'
  }
])

const currentCommunityCommentIndex = ref(0)
const fallbackCommunityComment = {
  title: '社区评论正在加载中。',
  desc: '稍后会展示来自真实用户的评论与记录。',
  image: 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&q=80',
  author: 'Wetland OS',
  note: 'System Feed',
  tag: '实时评论',
  color: '#1e3a34',
  textColor: '#f5f0e6',
  accent: '#b8d9c6'
}

const activeCommunityComment = computed(() => {
  return communityReviewCards.value[currentCommunityCommentIndex.value] ?? communityReviewCards.value[0] ?? fallbackCommunityComment
})

const showPrevComment = () => {
  currentCommunityCommentIndex.value =
    (currentCommunityCommentIndex.value - 1 + communityReviewCards.value.length) % communityReviewCards.value.length
}

const showNextComment = () => {
  currentCommunityCommentIndex.value =
    (currentCommunityCommentIndex.value + 1) % communityReviewCards.value.length
}

const setProcessCardRef = (el, index) => {
  if (el) {
    processCardRefs.value[index] = el
  }
}

const showToast = (msg, type = 'success') => {
  toast.value = { show: true, msg, type }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 2600)
}

const normalizeImage = (value, fallback) => {
  return resolveAssetUrl(value, fallback)
}

const truncateText = (value, fallback, limit = 120) => {
  const source = (value || fallback || '').trim()
  return source.length > limit ? `${source.slice(0, limit)}...` : source
}

const updateWildlifeSection = (records = []) => {
  if (!records.length) return

  const wildlifeSectionIndex = projectSections.value.findIndex((section) => section.layout === 'wildlifeGallery')
  if (wildlifeSectionIndex < 0) return

  const currentSection = projectSections.value[wildlifeSectionIndex]
  const fallbackHeroImage = currentSection.image
  const fallbackSpeciesImage = currentSection.species?.[0]?.image || currentSection.featuredSpecies?.image || fallbackHeroImage
  const visibleRecords = records.slice(0, 4)
  const featuredRecord = visibleRecords[0]
  const listRecords = visibleRecords.slice(1, 4)

  if (!featuredRecord) return

  projectSections.value[wildlifeSectionIndex] = {
    ...currentSection,
    image: normalizeImage(featuredRecord.imagePath, fallbackHeroImage),
    featuredSpecies: {
      category: featuredRecord.wetlandName || '',
      name: featuredRecord.name || '未命名物种',
      image: normalizeImage(featuredRecord.imagePath, fallbackHeroImage)
    },
    species: listRecords.map((item) => ({
      category: item.wetlandName || '',
      name: item.name || '未命名物种',
      image: normalizeImage(item.imagePath, fallbackSpeciesImage)
    }))
  }
}

const loadHomeContent = async () => {
  try {
    const wetlandResponse = await api.get('/wetlands')
    const wetlands = wetlandResponse.data?.wetlands || []

    if (wetlands.length > 0) {
      const floraFaunaResponses = await Promise.all(
        wetlands.map((wetland) =>
          api
            .get(`/wetlands/${wetland.id}/flora-fauna`)
            .then((response) => response.data?.floraFaunas || [])
            .catch(() => [])
        )
      )

      const floraFaunaRecords = floraFaunaResponses
        .flat()
        .filter((item) => item && item.active !== false)
        .sort((a, b) => new Date(b.createdTime || 0).getTime() - new Date(a.createdTime || 0).getTime())

      updateWildlifeSection(floraFaunaRecords)
    }

    const postsResponse = await api.get('/posts')
    const posts = postsResponse.data?.posts || []
    if (posts.length > 0) {
      const communityFallbackImage =
        projectSections.value.find((section) => section.layout === 'communityCarousel')?.image ||
        fallbackCommunityComment.image
      const livePosts = posts
        .slice(0, 6)
        .map((post) => ({
          title: truncateText(post.title, '新的湿地动态', 32),
          desc: truncateText(post.content, '用户分享了一条新的湿地记录。', 100),
          image: normalizeImage(post.image, communityFallbackImage),
          author: post.author || '匿名用户',
          note: `${post.time || '刚刚'} · ${post.likes || 0} 赞`,
          tag: post.tag || '社区动态',
          color: '#1e3a34',
          textColor: '#f5f0e6',
          accent: '#b8d9c6'
        }))
        .slice(0, 3)

      if (livePosts.length > 0) {
        communityReviewCards.value = livePosts
        currentCommunityCommentIndex.value = 0
      }
    }
  } catch (error) {
    console.error('首页动态数据加载失败:', error)
  }
}

const goTo = (path) => router.push(path)
const goToAuth = (mode) => router.push({ path: '/auth', query: { mode } })

const handleProtectedNavigation = () => {
  if (isLoggedIn.value) {
    router.push('/overview')
    return
  }
  showToast('请先登录后进入系统', 'warning')
  router.push({ path: '/auth', query: { mode: 'login' } })
}

const scrollToTop = () => {
  if (lenis) {
    lenis.scrollTo(0, { duration: 1.2, easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)) })
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const scrollToIntro = () => {
  if (lenis && introSection.value) {
    lenis.scrollTo(introSection.value, { duration: 1.35, offset: 0 })
  } else {
    introSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const setFirstStorySection = (el) => {
  firstStorySection.value = el
}

const handleScroll = () => {
  isNavSolid.value = window.scrollY > 48
  showBackToTop.value = window.scrollY > 520
}

const handleMouseMove = (e) => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  animationFrameId = requestAnimationFrame(() => {
    mouseX.value = e.clientX - 150
    mouseY.value = e.clientY - 150
  })
}

onMounted(async () => {
  document.body.style.overflowX = 'hidden'
  await loadHomeContent()
  handleScroll()

  lenis = new Lenis({
    duration: 1.8,
    easing: (t) => 1 - Math.pow(1 - t, 5),
    orientation: 'vertical',
    gestureOrientation: 'vertical',
    smoothWheel: true,
    wheelMultiplier: 0.85,
    touchMultiplier: 1.35,
  })

  lenis.on('scroll', ScrollTrigger.update)

  lenisRaf = (time) => {
    lenis.raf(time * 1000)
  }
  gsap.ticker.add(lenisRaf)
  gsap.ticker.lagSmoothing(0)

  window.addEventListener('scroll', handleScroll, { passive: true })

  nextTick(() => {
    ctx = gsap.context(() => {

      // ===== 1. 锐化背景切割 (干脆利落的画布底色切换) =====
      const bgColors = ['#F5F8F6', '#F4EFEB', '#EDF1F2', '#F4EFEB', '#F5F8F6'];
      
      gsap.utils.toArray('.home-panel').forEach((panel, i) => {
        if (bgColors[i]) {
          ScrollTrigger.create({
            trigger: panel,
            start: "top 60%", // 当新板块冒头时迅速切底色
            onEnter: () => gsap.to(pageContainer.value, { backgroundColor: bgColors[i], duration: 0.5, ease: "power2.out", overwrite: "auto" }),
            onEnterBack: () => gsap.to(pageContainer.value, { backgroundColor: bgColors[i], duration: 0.5, ease: "power2.out", overwrite: "auto" })
          });
        }
      });

      // ===== 2. 终极无BUG版：分离视差与对焦，精准控制元素 =====
      gsap.utils.toArray('.home-panel').forEach((panel, index) => {
        // 排除首屏(视频区)和 3D卡片流程区，避免干扰它们原有的炫酷逻辑
        if (index === 0 || panel.classList.contains('wetland-process-section')) return;

        // 【动作一：底层视差】 只让整个板块产生微弱的上下交错滑动，不碰透明度！
        gsap.to(panel, {
          y: -50,
          ease: 'none',
          scrollTrigger: {
            trigger: panel,
            start: 'top bottom',
            end: 'bottom top',
            scrub: 1.2
          }
        });

        // 【动作二：无损对焦显现】 针对板块内部带有 reveal-up 的具体内容（标题、内容块）进行阻尼显现
        const revealElements = panel.querySelectorAll('.reveal-up');
        
        revealElements.forEach((el) => {
          gsap.fromTo(el,
            { opacity: 0, y: 60, filter: 'blur(16px)', scale: 0.94 },
            {
              opacity: 1,
              y: 0,
              filter: 'blur(0px)',
              scale: 1,
              duration: 1.4,
              ease: 'power3.out',
              scrollTrigger: {
                trigger: el,
                start: 'top 85%', // 元素进入屏幕 85% 位置时开始清晰对焦
                toggleActions: 'play none none reverse' // 向下滚动时播放，回滚到顶部时隐藏，保证每次滑动都有动效
              }
            }
          );
        });
      });

      // --- 基础 UI 动效 ---

      // 导航栏入场
      gsap.fromTo(
        '.home-nav',
        { opacity: 0, y: -24, scale: 0.9, transformOrigin: 'top center' },
        { opacity: 1, y: 0, scale: 1, duration: 1.1, ease: 'power3.out' }
      )

      gsap.fromTo(
        '.brand-mark, .nav-link',
        { opacity: 0, scale: 0.86 },
        {
          opacity: 1,
          scale: 1,
          duration: 0.9,
          stagger: 0.08,
          delay: 0.18,
          ease: 'power3.out'
        }
      )

      // 首屏静态内容的入场（严格限制在 .hero-section 内，防误伤其他元素）
      gsap.fromTo(
        '.hero-section .reveal-up',
        { y: 48, opacity: 0 },
        {
          y: 0,
          opacity: 1,
          duration: 1.2,
          stagger: 0.14,
          ease: 'power3.out'
        }
      )

      // Hero 视频滚动视差缩放
      gsap.to('.hero-video', {
        scale: 1.12,
        ease: 'none',
        scrollTrigger: {
          trigger: heroSection.value,
          start: 'top top',
          end: 'bottom top',
          scrub: true
        }
      })

      // 桌面端：流程卡片 3D 翻转动画 (完美保留)
      ScrollTrigger.matchMedia({
        '(min-width: 901px)': () => {
          const cardInners = processCardRefs.value
            .map((card) => card?.querySelector('.wetland-process-card__inner'))
            .filter(Boolean)

          if (processSectionRef.value && processCardsWrapperRef.value && cardInners.length && processTitleRef.value) {
            gsap.set(processCardsWrapperRef.value, {
              scale: 0.66,
              gap: '0px'
            })
            gsap.set(cardInners, { rotateY: 0 })
            gsap.set(processTitleRef.value, { autoAlpha: 0, y: 30 })

            gsap.timeline({
              scrollTrigger: {
                trigger: processSectionRef.value,
                start: 'top top',
                end: '+=3000',
                scrub: true,
                pin: true
              }
            })
              .to(processCardsWrapperRef.value, {
                scale: 1,
                duration: 1.15,
                ease: 'power2.inOut'
              })
              .to(
                processTitleRef.value,
                {
                  autoAlpha: 1,
                  y: 0,
                  duration: 0.85,
                  ease: 'power2.out'
                },
                '<0.18'
              )
              .to(
                processCardsWrapperRef.value,
                {
                  gap: '2vw',
                  scale: 0.95,
                  duration: 1.5,
                  ease: 'power1.inOut'
                },
                '+=0.2'
              )
              .to(
                cardInners,
                {
                  rotateY: 180,
                  duration: 2,
                  stagger: 0.25,
                  ease: 'power2.inOut'
                },
                '+=0.3'
              )
          }
        },
        // 移动端：降级处理
        '(max-width: 900px)': () => {
          if (!processCardsWrapperRef.value || !processCardRefs.value.length) return

          gsap.set(processCardsWrapperRef.value, {
            clearProps: 'all',
            scale: 1,
            gap: '18px'
          })

          gsap.set(
            processCardRefs.value
              .map((card) => card?.querySelector('.wetland-process-card__inner'))
              .filter(Boolean),
            { rotateY: 0 }
          )
        }
      })

      ScrollTrigger.refresh()
    }, pageContainer.value)
  })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (toastTimer) clearTimeout(toastTimer)
  if (animationFrameId) cancelAnimationFrame(animationFrameId)

  document.body.style.overflowX = ''
  
  if (ctx) ctx.revert()
  
  if (lenis) {
    lenis.destroy()
    if (lenisRaf) gsap.ticker.remove(lenisRaf)
  }
})
</script>
<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  /* 初始背景色：晨露白 */
  background-color: #F5F8F6; 
  /* 核心文本与点缀色定义 */
  --text-main: #1A2B25; 
  --text-muted: rgba(26, 43, 37, 0.65);
  --accent: #537A66;
  --surface-white: #FFFFFF;
  color: var(--text-main);
  /* 兜底GSAP动画的平滑过渡 */
  transition: background-color 0.4s ease; 
}

.home-background-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.home-background-image,
.home-background-overlay {
  position: absolute;
  inset: 0;
}

.home-background-image {
  background:
    url('https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=2200&q=80')
    center/cover no-repeat;
  transform: scale(1.04);
  filter: saturate(0.82) contrast(0.94) brightness(1.04);
}

.home-background-overlay {
  background:
    linear-gradient(180deg, rgba(247, 249, 246, 0.72) 0%, rgba(245, 248, 246, 0.88) 28%, rgba(245, 248, 246, 0.96) 100%),
    radial-gradient(circle at top center, rgba(83, 122, 102, 0.12) 0%, transparent 42%);
}

.home-panel {
  position: relative;
  min-height: 100svh;
  isolation: isolate;
}

.page-shell {
  width: min(1680px, calc(100vw - 36px));
}

.custom-cursor-aura {
  position: fixed;
  top: 0;
  left: 0;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(113, 143, 107, 0.2) 0%, rgba(83, 122, 102, 0.12) 24%, rgba(255, 255, 255, 0.08) 44%, transparent 68%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 90;
  transition: transform 0.1s ease-out;
  mix-blend-mode: multiply;
  opacity: 0.8;
}

.home-toast {
  position: fixed;
  top: 28px;
  left: 50%;
  z-index: 120;
  transform: translateX(-50%);
  padding: 12px 20px;
  border-radius: 999px;
  border: 1px solid rgba(111, 140, 186, 0.18);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 36px rgba(133, 151, 182, 0.18);
  backdrop-filter: blur(14px);
  font-size: 0.88rem;
  letter-spacing: 0.06em;
}

.home-toast.warning {
  border-color: rgba(241, 207, 138, 0.34);
  color: var(--warning);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.38s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -12px);
}

.home-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 26px 24px;
  transform-origin: top center;
  transition: background-color var(--transition-fast), backdrop-filter var(--transition-fast), border-color var(--transition-fast);
}

.home-nav.is-scrolled {
  background: linear-gradient(180deg, rgba(247, 244, 238, 0.9), rgba(245, 241, 234, 0.86));
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  backdrop-filter: blur(16px);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.nav-logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.34);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 1.38rem;
  line-height: 1;
  transition: border-color var(--transition-fast), background-color var(--transition-fast), color var(--transition-fast);
}

.brand-mark:hover {
  transform: translateX(-50%) scale(1.08);
}

.brand-text {
  font-family: var(--font-serif);
  letter-spacing: 0.08em;
  font-size: 2.2rem;
  color: #fff;
  line-height: 1;
}

.nav-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.nav-link,
.scroll-cue,
.text-entry-link {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.nav-link {
  color: #fff;
  font-size: 0.92rem;
  letter-spacing: 0.04em;
  transition: opacity var(--transition-fast), transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.home-nav.is-scrolled .brand-text,
.home-nav.is-scrolled .nav-link,
.home-nav.is-scrolled .nav-logo-mark {
  color: var(--text-main);
}

.home-nav.is-scrolled .nav-logo-mark {
  border-color: rgba(17, 24, 39, 0.16);
  background: rgba(255, 255, 255, 0.98);
}

.nav-link:hover {
  opacity: 0.72;
  transform: scale(1.08);
}

.back-to-top-button {
  position: fixed;
  right: clamp(18px, 2.4vw, 34px);
  bottom: clamp(22px, 3vw, 38px);
  z-index: 30;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  padding: 0;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.94);
  color: #18222a;
  box-shadow:
    0 18px 44px rgba(0, 0, 0, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
  transition:
    transform 0.28s ease,
    border-color 0.28s ease,
    background-color 0.28s ease,
    box-shadow 0.28s ease;
}

.back-to-top-button:hover {
  transform: translateY(-4px);
  border-color: rgba(17, 24, 39, 0.12);
  background: #ffffff;
  box-shadow:
    0 22px 48px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.back-to-top-button__label {
  display: none;
}

.back-to-top-button__hint {
  display: none;
}

.back-to-top-button::before {
  content: '';
  width: 14px;
  height: 14px;
  border-top: 2px solid currentColor;
  border-left: 2px solid currentColor;
  transform: translateY(3px) rotate(45deg);
}

.top-button-fade-enter-active,
.top-button-fade-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.top-button-fade-enter-from,
.top-button-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.hero-section {
  position: relative;
  min-height: 100vh;
  overflow: clip;
}

.hero-video,
.hero-noise {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-video {
  object-fit: cover;
  transform: scale(1.02);
  filter: saturate(0.88) contrast(0.96) brightness(1.12);
}

.hero-noise {
  opacity: 0.08;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.022) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.9), rgba(0, 0, 0, 0.35));
}

.hero-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding-top: 140px;
  padding-bottom: 68px;
  text-align: center;
}

.hero-copy {
  max-width: 1180px;
}

.hero-title {
  max-width: none;
  margin-bottom: 30px;
  font-size: clamp(4.8rem, 9.6vw, 8.8rem);
  font-weight: 700;
  line-height: 1.04;
  letter-spacing: 0.01em;
  color: #fff;
  text-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
  padding-bottom: 0.12em;
  overflow: visible;
}

.title-line {
  display: block;
}

.title-line-main {
  font-size: 0.94em;
  letter-spacing: 0.03em;
}

.title-line-accent {
  margin-top: 0.04em;
  font-size: 1.08em;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title-line-tail {
  margin-top: 0.08em;
  font-size: 0.56em;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.88);
}

.hero-text {
  max-width: 48ch;
  margin: 0 auto;
  color: rgba(255, 255, 255, 0.98);
  font-size: 1.22rem;
  line-height: 1.9;
  text-shadow: 0 14px 32px rgba(0, 0, 0, 0.22);
}

.hero-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 42px;
}

.text-entry-link {
  padding: 0;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  position: relative;
  text-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
  transition: opacity var(--transition-fast), letter-spacing var(--transition-fast);
}

.text-entry-link::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -10px;
  width: 100%;
  height: 1px;
  background: rgba(255, 255, 255, 0.78);
  transition: transform var(--transition-fast), opacity var(--transition-fast);
}

.text-entry-link:hover {
  letter-spacing: 0.22em;
}

.text-entry-link:hover::after {
  transform: scaleX(1.08);
}

.scroll-cue {
  position: absolute;
  left: 32px;
  bottom: 32px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: var(--text-muted);
  font-size: 0.76rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.scroll-line {
  position: relative;
  width: 1px;
  height: 56px;
  background: rgba(110, 138, 180, 0.24);
  overflow: hidden;
}

.scroll-line::after {
  content: '';
  position: absolute;
  top: -18px;
  left: 0;
  width: 100%;
  height: 18px;
  background: var(--accent);
  animation: scroll-drop 1.5s infinite ease-in-out;
}

.story-copy h2,
.entry-copy h2 {
  margin: 20px 0 18px;
  font-size: clamp(2.4rem, 4vw, 4.2rem);
  line-height: 1.1;
  padding-bottom: 0.08em;
}

.story-copy p,
.entry-copy p,
.highlight-card p {
  line-height: 1.85;
}

.entry-panel {
  padding: 0;
}

.story-section {
  display: flex;
  align-items: center;
  min-height: 100svh;
  padding: clamp(72px, 6vw, 104px) 0;
}

/* 剥离背景渐变，改为透明使用全局背景 */
.project-intro-section {
  padding: 0;
  background: transparent;
}

.story-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  gap: clamp(30px, 3vw, 48px);
  align-items: center;
  width: 100%;
}

.community-comments-section {
  min-height: 100svh;
  display: flex;
  align-items: center;
  padding: clamp(72px, 6vw, 104px) 0;
  overflow: hidden;
  background: transparent; 
}

.community-comments-panel {
  width: 100vw;
  margin-left: calc(50% - 50vw);
  margin-right: calc(50% - 50vw);
  min-height: calc(100svh - clamp(118px, 10vw, 170px));
  display: grid;
  grid-template-rows: auto 1fr;
  gap: clamp(18px, 2.6vw, 28px);
  align-items: start;
  padding: 0 clamp(18px, 2.2vw, 30px);
}

.community-comments-header {
  width: min(1520px, 100%);
  margin: 0 auto;
}

.community-comments-header h2 {
  margin: 14px 0 14px;
  font-size: clamp(2rem, 3.2vw, 3.25rem);
  line-height: 1.1;
  color: var(--text-main);
  padding-bottom: 0.08em;
}

.community-comments-header p {
  max-width: 46ch;
  color: var(--text-muted);
  line-height: 1.72;
  font-size: 0.96rem;
}

.community-comments-stage {
  width: 100%;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 16px;
  min-height: min(54vh, 560px);
}

.community-comment-frame {
  width: min(1520px, 100%);
  height: min(46vh, 480px);
  margin: 0 auto;
  border: 1px solid transparent;
  background: transparent;
  overflow: hidden;
}

.community-comments-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.community-comments-line {
  width: 100%;
  height: 1px;
  background: rgba(26, 43, 37, 0.12);
}

.community-comment-content {
  display: grid;
  grid-template-columns: minmax(0, 1.14fr) minmax(0, 0.86fr);
  align-items: stretch;
  width: 100%;
  height: 100%;
  color: var(--text-main);
  gap: clamp(24px, 2.4vw, 34px);
}

.community-comment-media {
  min-height: 100%;
  overflow: hidden;
}

.community-comment-copy {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  overflow: hidden;
  padding: 0 0 74px;
  max-width: 620px;
  position: relative;
}

.community-comment-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  font-size: 0.8rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.community-comment-copy h3 {
  margin: 18px 0 14px;
  font-family: var(--font-serif);
  font-size: clamp(1.65rem, 2.2vw, 2.5rem);
  line-height: 1.12;
  font-weight: 500;
  min-height: 2.35em;
  max-width: 13ch;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.community-comment-copy p {
  margin: 0;
  max-width: 40ch;
  font-size: 0.95rem;
  line-height: 1.68;
  opacity: 0.92;
  min-height: 5.1em;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.community-comment-meta {
  margin-top: auto;
  padding-top: 18px;
}

.community-comment-meta strong,
.community-comment-meta span {
  display: block;
}

.community-comment-meta strong {
  font-size: 1rem;
  letter-spacing: 0.04em;
}

.community-comment-meta span {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 0.84rem;
  letter-spacing: 0.08em;
}

.community-comment-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
  position: absolute;
  right: 0;
  bottom: 0;
}

.community-comment-dots {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: 12px;
}

.community-comment-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-nav-button {
  min-width: 92px;
  padding: 12px 18px;
  border-radius: 0;
  border: 1px solid rgba(26, 43, 37, 0.12);
  background: var(--surface-white);
  color: var(--text-main);
  font-size: 1.2rem;
  letter-spacing: 0;
  text-transform: none;
  transition: transform var(--transition-fast), background-color var(--transition-fast), opacity var(--transition-fast);
}

.comment-nav-button:hover {
  transform: translateY(-1px);
  background: #f0f3f1;
}

.comment-nav-button--square {
  min-width: 54px;
  width: 54px;
  height: 54px;
  padding: 0;
  border-radius: 0;
}

.comment-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(26, 43, 37, 0.18);
  transition: transform var(--transition-fast), background-color var(--transition-fast);
}

.comment-dot.is-active {
  transform: scale(1.3);
  background: var(--text-main);
}

.wildlife-gallery-section {
  min-height: 100svh;
  display: flex;
  align-items: stretch;
  padding: clamp(72px, 6vw, 104px) 0 0;
  background: transparent;
}

.wildlife-gallery-panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: clamp(18px, 2.6vw, 26px);
  min-height: calc(100svh - clamp(110px, 9vw, 156px));
  height: calc(100svh - clamp(110px, 9vw, 156px));
}

.wildlife-gallery-header {
  max-width: 1120px;
}

.wildlife-gallery-header h2 {
  margin: 14px 0 12px;
  font-size: clamp(2.1rem, 3.2vw, 3.25rem);
  line-height: 1.1;
  color: var(--text-main);
  padding-bottom: 0.08em;
}

.wildlife-gallery-header p {
  max-width: 50ch;
  color: var(--text-muted);
  line-height: 1.68;
  font-size: 0.96rem;
}

.wildlife-gallery-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) minmax(0, 0.84fr);
  gap: clamp(18px, 2vw, 24px);
  align-items: stretch;
  min-height: 0;
}

.wildlife-hero {
  position: relative;
  min-height: min(43vh, 430px);
  overflow: hidden;
  border: 1px solid rgba(26, 43, 37, 0.1);
}

.wildlife-hero__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.wildlife-hero__overlay {
  position: absolute;
  right: 18px;
  bottom: 18px;
  display: grid;
  gap: 6px;
  min-width: min(280px, calc(100% - 36px));
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-main);
  box-shadow: 0 16px 40px rgba(17, 24, 39, 0.12);
}

.wildlife-hero__overlay em,
.wildlife-hero__overlay h3 {
  display: block;
}

.wildlife-hero__overlay em {
  font-style: normal;
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.wildlife-hero__overlay h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(1.2rem, 2vw, 1.72rem);
  font-weight: 500;
  line-height: 1.18;
}

.wildlife-species-grid {
  display: grid;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 10px;
  min-height: 0;
}

.wildlife-species-card {
  display: grid;
  grid-template-columns: 156px minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
  background: transparent;
  border-top: 1px solid rgba(26, 43, 37, 0.1);
  padding-top: 10px;
}

.wildlife-species-card__media {
  min-height: 0;
}

.wildlife-species-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.wildlife-species-card__body {
  display: flex;
  flex-direction: column;
  padding: 10px 14px 10px;
  min-height: 0;
}

.wildlife-species-card__body span {
  color: var(--text-muted);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.wildlife-species-card__body h3 {
  margin: 8px 0 6px;
  font-family: var(--font-serif);
  font-size: 1.28rem;
  line-height: 1.18;
  font-weight: 500;
  color: var(--text-main);
  padding-bottom: 0.04em;
}

.wildlife-species-card__body p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.46;
  font-size: 0.84rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wildlife-species-card__body strong {
  margin-top: auto;
  padding-top: 10px;
  color: var(--accent);
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.immersive-gallery-section {
  padding: 112px 0;
  background: transparent;
  color: var(--text-main);
}

.immersive-gallery-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
  gap: 54px;
  align-items: center;
}

.immersive-gallery-copy h2 {
  margin: 20px 0 18px;
  font-size: clamp(2.6rem, 4vw, 4.4rem);
  line-height: 1.04;
  color: var(--text-main);
}

.immersive-gallery-copy p {
  max-width: 48ch;
  color: var(--text-muted);
  line-height: 1.92;
}

.gallery-kicker,
.gallery-meta span,
.gallery-highlight-card span {
  color: var(--text-muted);
}

.gallery-meta strong,
.gallery-highlight-card strong {
  color: var(--text-main);
}

.immersive-gallery-media {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 520px;
}

.gallery-highlights {
  margin-top: 38px;
}

.gallery-highlight-card {
  border-bottom-color: rgba(26, 43, 37, 0.12);
}

.gallery-highlight-card p {
  color: var(--text-muted);
}

.wetland-process-section {
  padding: 0;
  background: transparent;
  color: var(--text-main);
  overflow: visible;
}

.wetland-process-shell {
  position: relative;
  display: block;
  width: 100%;
  max-width: none;
  padding: 0;
}

.wetland-process-intro {
  position: relative;
  top: auto;
  left: auto;
  transform: none;
  z-index: 4;
  display: flex;
  width: min(860px, calc(100vw - 56px));
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  margin-bottom: clamp(30px, 4vh, 46px);
  pointer-events: none;
}

.wetland-process-kicker {
  color: var(--text-muted);
}

.wetland-process-intro h2 {
  margin: 10px 0 0;
  font-size: clamp(1.9rem, 2.55vw, 3.15rem);
  line-height: 1.1;
  color: var(--text-main);
  text-align: center;
  padding-bottom: 0.08em;
}

.wetland-process-intro p {
  max-width: 48ch;
  color: var(--text-muted);
  font-size: 0.92rem;
  line-height: 1.72;
}

.wetland-process-stage {
  position: relative;
  width: 100vw;
  min-height: 100vh;
  margin-left: calc(50% - 50vw);
  margin-right: calc(50% - 50vw);
}

.wetland-process-viewport {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  perspective: 1800px;
  padding: clamp(28px, 4vw, 54px);
}

.wetland-process-cards {
  display: flex;
  gap: 0;
  width: min(84vw, 1480px);
  height: min(58vh, 620px);
  transform-origin: center center;
}

.wetland-process-card {
  flex: 1;
  min-height: 100%;
  perspective: 1200px;
}

.wetland-process-card__inner {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
}

.wetland-process-card__face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  border-radius: 12px;
}

.wetland-process-card__face--front {
  background-image: var(--card-image);
  background-position: var(--card-position);
  background-size: 400% 100%;
  background-repeat: no-repeat;
  background-color: #1a1a1a;
}

.wetland-process-card__face--back {
  padding: clamp(28px, 2.6vw, 42px);
  background: var(--surface-white);
  color: var(--text-main);
  border: 1px solid rgba(26, 43, 37, 0.06);
  box-shadow: 0 24px 48px rgba(26, 43, 37, 0.05);
  transform: rotateY(180deg);
}

.wetland-process-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.04) 0%, rgba(0, 0, 0, 0.7) 100%);
}

.wetland-process-card__front-copy {
  position: absolute;
  right: clamp(20px, 2vw, 28px);
  bottom: clamp(20px, 2vw, 28px);
  left: clamp(20px, 2vw, 28px);
  display: grid;
  gap: 10px;
  z-index: 2;
}

.wetland-process-card__index,
.wetland-process-card__eyebrow {
  display: block;
  font-size: 0.78rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #ffffff;
}

.wetland-process-card__index {
  opacity: 0.6;
}

.wetland-process-card__eyebrow {
  opacity: 0.8;
}

.wetland-process-card__front-copy h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(1.8rem, 2.25vw, 3.1rem);
  line-height: 1.12;
  color: #ffffff;
  padding-bottom: 0.06em;
}

.wetland-process-card__face--back h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(1.8rem, 2.25vw, 3.1rem);
  line-height: 1.12;
  color: var(--text-main);
  padding-bottom: 0.06em;
}

.wetland-process-card__face--back p {
  margin: 18px 0 24px;
  color: var(--text-muted);
  font-size: 1rem;
  line-height: 1.84;
}

.wetland-process-card__face--back strong {
  margin-top: auto;
  color: var(--accent);
  font-size: 1rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.story-section.is-reversed .story-grid {
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1fr);
}

.story-section.is-reversed .story-media {
  order: 2;
}

.story-section.is-reversed .story-copy {
  order: 1;
}

.story-visual {
  position: relative;
  min-height: min(68vh, 680px);
}

.story-visual img {
  width: 100%;
  height: min(68vh, 680px);
  object-fit: cover;
  filter: saturate(0.92) contrast(1.02);
}

.story-meta,
.story-overlay span,
.highlight-card span {
  display: flex;
  align-items: baseline;
  gap: 18px;
  margin-top: 26px;
}

.story-meta span,
.highlight-card span {
  display: block;
  color: var(--text-muted);
  font-size: 0.78rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.story-meta strong,
.highlight-card strong {
  display: block;
  font-family: var(--font-serif);
  font-size: 1.8rem;
  font-weight: 500;
  color: var(--text-main);
}

.story-highlights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 28px;
  margin-top: 42px;
}

.highlight-card {
  padding: 0 0 22px;
  border-bottom: 1px solid rgba(26, 43, 37, 0.14);
}

.highlight-card p {
  margin-top: 10px;
  font-size: 0.94rem;
  color: var(--text-muted);
}

.system-footer {
  margin-top: 0;
  padding: 0;
  background: #000000;
  color: rgba(255, 255, 255, 0.96);
  display: flex;
  align-items: center;
  min-height: 0;
  position: relative;
  z-index: 2;
}

.system-footer__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, auto);
  gap: 20px 42px;
  align-items: end;
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  padding: 30px clamp(20px, 2.4vw, 34px) 26px;
  overflow: visible;
}

.system-footer--embedded {
  width: 100vw;
  margin-top: auto;
  margin-left: calc(50% - 50vw);
  margin-right: calc(50% - 50vw);
  align-self: end;
  justify-self: stretch;
}

.footer-kicker {
  color: rgba(255, 255, 255, 0.56);
}

.footer-copy {
  max-width: 34ch;
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.92rem;
}

.system-info-list {
  display: grid;
  grid-column: auto;
  grid-template-columns: 1fr;
  gap: 14px;
  align-items: end;
  justify-self: end;
  min-width: max-content;
  width: auto;
  padding-top: 0;
  border-top: none;
}

.system-info-item {
  display: grid;
  gap: 4px;
  text-align: right;
}

.system-info-item span {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.system-info-item strong {
  color: rgba(255, 255, 255, 0.92);
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-align: right;
}

@keyframes scroll-drop {
  0% {
    transform: translateY(0);
    opacity: 0;
  }

  25% {
    opacity: 1;
  }

  100% {
    transform: translateY(74px);
    opacity: 0;
  }
}

@media (max-width: 1080px) {
  .hero-content,
  .story-grid,
  .story-section.is-reversed .story-grid,
  .community-comments-panel,
  .community-comments-stage,
  .wildlife-gallery-stage,
  .immersive-gallery-grid,
  .wetland-process-step,
  .system-footer__inner {
    grid-template-columns: 1fr;
  }

  .story-section.is-reversed .story-media,
  .story-section.is-reversed .story-copy {
    order: initial;
  }

  .system-info-list {
    gap: 14px;
  }

  .system-info-item {
    text-align: left;
  }

  .system-info-item strong {
    text-align: left;
  }

  .community-comments-panel {
    min-height: auto;
    width: min(100%, calc(100vw - 32px));
    margin-left: auto;
    margin-right: auto;
    padding: 0;
  }

  .community-comments-stage {
    min-height: auto;
  }

  .community-comment-frame {
    width: 100%;
    height: auto;
  }

  .community-comments-header p,
  .community-comment-copy p {
    max-width: none;
  }

  .community-comment-content {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .wildlife-species-grid {
    grid-template-rows: none;
    grid-template-columns: 1fr;
  }

  .immersive-gallery-copy p {
    max-width: none;
  }

  .wetland-process-stage {
    height: auto;
    margin-left: calc(50% - 50vw);
    margin-right: calc(50% - 50vw);
  }

  .wetland-process-intro {
    position: relative;
    top: auto;
    left: auto;
    transform: none !important;
    max-width: none;
    width: auto;
    text-align: left;
    padding: 88px 22px 24px;
    pointer-events: auto;
  }

  .wetland-process-viewport {
    position: relative;
    height: auto;
    min-height: auto;
    padding: 0 22px 40px;
  }

  .wetland-process-cards {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px !important;
    width: 100%;
    height: auto;
    transform: none !important;
  }

  .wetland-process-card__inner {
    transform: none !important;
  }

  .wetland-process-card {
    min-height: 520px;
  }
}

@media (max-width: 720px) {
  .home-nav {
    padding: 18px 16px;
    justify-content: space-between;
  }

  .back-to-top-button {
    right: 16px;
    bottom: 20px;
    width: 52px;
    height: 52px;
  }

  .brand-text {
    font-size: 1.5rem;
  }

  .brand-mark {
    position: static;
    transform: none;
  }

  .brand-mark:hover {
    transform: scale(1.04);
  }

  .nav-actions {
    gap: 10px;
  }

  .nav-link {
    display: none;
  }

  .hero-content {
    padding-top: 108px;
    padding-bottom: 96px;
  }

  .hero-title {
    font-size: clamp(3.6rem, 12vw, 5.4rem);
  }

  .title-line-tail {
    font-size: 0.62em;
    letter-spacing: 0.16em;
  }

  .hero-actions,
  .system-info-list {
    flex-direction: column;
    align-items: stretch;
  }

  .community-comments-section {
    padding: 84px 0;
  }

  .wildlife-gallery-section {
    padding: 84px 0;
  }

  .wildlife-hero {
    min-height: 360px;
  }

  .wildlife-species-card {
    grid-template-columns: 1fr;
  }

  .wildlife-species-card__media {
    min-height: 220px;
  }

  .community-comment-media {
    min-height: 320px;
  }

  .community-comment-copy {
    padding: 0;
  }

  .community-comment-copy h3 {
    font-size: 1.8rem;
    min-height: auto;
    max-width: none;
  }

  .community-comment-copy p {
    font-size: 0.98rem;
    min-height: auto;
  }

  .community-comment-controls {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .comment-nav-button--square {
    width: 56px;
    height: 56px;
    border-radius: 16px;
  }

  .system-footer__inner {
    padding: 36px 20px 34px;
  }

  .footer-copy,
  .system-info-item strong {
    text-align: left;
  }

  .text-entry-link {
    font-size: 0.86rem;
  }

  .scroll-cue {
    left: 16px;
    bottom: 18px;
  }

  .story-visual {
    min-height: auto;
  }

  .story-visual img {
    height: 420px;
  }

  .story-highlights {
    grid-template-columns: 1fr;
  }

  .immersive-gallery-section {
    padding: 88px 0;
  }

  .immersive-gallery-media {
    min-height: 420px;
  }

  .wetland-process-section {
    padding: 0;
  }

  .wetland-process-intro {
    padding: 88px 18px 22px;
  }

  .wetland-process-viewport {
    padding: 0 16px 28px;
  }

  .wetland-process-cards {
    grid-template-columns: 1fr;
    gap: 16px !important;
  }

  .wetland-process-card {
    min-height: 420px;
  }
}
</style>
