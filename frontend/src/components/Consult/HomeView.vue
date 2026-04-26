<template>
  <main class="home-page">
    <section v-if="loading" class="state-card glass-light">
      <strong>正在进入景区首页</strong>
      <div>正在连接景点、分类与精选内容。</div>
    </section>

    <section v-else-if="error" class="state-card glass-light">
      <strong>首页加载失败</strong>
      <div>{{ error }}</div>
    </section>

    <template v-else>
      <section class="hero-section">
        <div class="hero-background"></div>
        <div class="hero-pattern"></div>
        <div class="hero-main">
          <div class="hero-topbar">
            <RouterLink class="hero-return" :to="consultPath">返回导览总览</RouterLink>
            <div class="hero-kicker"></div>
          </div>

          <div class="hero-grid">
            <div class="hero-copy">
              <h1>
                {{ homeData.scenicName || scenicName }}
                <span>探索你的旅行节奏</span>
              </h1>
              <p>{{ homeData.description || '从这里开始：先看精选看点，再选路线，最后进入地图探索。' }}</p>

              <div class="hero-actions-frame">
                <div class="hero-actions-caption"></div>
                <div class="hero-actions">
                  <RouterLink class="hero-action-card hero-action-card--primary" :to="explorePath">
                    <span class="hero-action-card__eyebrow"></span>
                    <strong>进入探索</strong>
                    <small>前往地图探索、路线聚焦与实时漫游</small>
                  </RouterLink>
                  <RouterLink class="hero-action-card hero-action-card--secondary" :to="cloudPath">
                    <span class="hero-action-card__eyebrow"></span>
                    <strong>沉浸云游</strong>
                    <small>切换全景场景，先建立景区空间印象</small>
                  </RouterLink>
                </div>
              </div>
            </div>

            <div class="hero-stats">
              <article>
                <span>收录景点</span>
                <strong>{{ summary.total || spots.length }}</strong>
              </article>
              <article>
                <span>已探索</span>
                <strong>{{ summary.visited || 0 }}</strong>
              </article>
              <article>
                <span>精选推荐</span>
                <strong>{{ featuredCards.length }}</strong>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="home-wrap">
        <div class="category-row">
          <button
            v-for="item in categories"
            :key="item.value"
            type="button"
            :class="['category-chip', { active: activeCategory === item.value }]"
            @click="activeCategory = item.value"
          >
            {{ item.label }}
          </button>
        </div>

        <section v-if="featuredCards.length" class="block-section">
          <div class="block-head">
            <h2>当季推荐</h2>
            <RouterLink class="head-link" :to="explorePath">去地图里看 →</RouterLink>
          </div>
          <div class="featured-grid">
            <article
              v-for="item in featuredCards.slice(0, 3)"
              :key="item.id"
              class="featured-card"
              @click="openSpot(item.id)"
            >
              <img :src="item.imageUrl || item.image" :alt="item.name" />
              <div class="featured-mask"></div>
              <div class="featured-content">
                <span>{{ item.category || '精选推荐' }}</span>
                <h3>{{ item.name }}</h3>
                <p>{{ item.description || '点击查看该看点详情。' }}</p>
              </div>
            </article>
          </div>
        </section>

        <section class="block-section">
          <div class="block-head">
            <h2>全部景点</h2>
            <span class="result-count">共 {{ filteredSpots.length }} 个</span>
          </div>

          <div v-if="!filteredSpots.length" class="empty-card">
            暂无可展示景点，试试切换分类。
          </div>

          <div v-else class="spot-grid">
            <article
              v-for="item in filteredSpots"
              :key="item.id"
              class="spot-card"
            >
              <button class="spot-cover-btn" type="button" @click="openSpot(item.id)">
                <img :src="item.imageUrl || item.image" :alt="item.name" />
              </button>
              <div class="spot-body">
                <div class="spot-meta">
                  <span class="badge">{{ item.category || '景点' }}</span>
                  <span v-if="item.isVisited" class="badge visited">已探索</span>
                </div>
                <h3>{{ item.name }}</h3>
                <p>{{ item.description || '暂无描述' }}</p>
                <div class="spot-footer">
                  <small>{{ item.location?.address || '景区内' }}</small>
                  <button class="btn btn-light mini" type="button" @click="openSpot(item.id)">详情</button>
                </div>
              </div>
            </article>
          </div>
        </section>
      </section>

      <div v-if="selectedSpot" class="detail-mask" @click.self="closeSpot">
        <article class="detail-card">
          <button class="detail-close" type="button" @click="closeSpot">×</button>
          <div class="detail-hero">
            <img :src="selectedSpot.imageUrl || selectedSpot.image" :alt="selectedSpot.name" />
          </div>
          <div class="detail-body">
            <div class="detail-badges">
              <span class="badge">{{ selectedSpot.category || '景点' }}</span>
              <span class="badge">评分 {{ Number(selectedSpot.rating || 0).toFixed(1) }}</span>
            </div>
            <h3>{{ selectedSpot.name }}</h3>
            <p>{{ selectedSpot.longDescription || selectedSpot.description || '暂无详细介绍。' }}</p>
            <div class="detail-info">
              <span>开放时间：{{ selectedSpot.openHours || '全天开放' }}</span>
              <span>门票：{{ selectedSpot.ticketPrice || '免费' }}</span>
              <span>位置：{{ selectedSpot.location?.address || '景区内' }}</span>
            </div>
            <div class="detail-actions">
              <RouterLink class="btn btn-primary" :to="buildSpotExploreLink(selectedSpot.id)">去地图聚焦</RouterLink>
              <RouterLink class="btn btn-light" :to="cloudPath">进入云游</RouterLink>
            </div>
          </div>
        </article>
      </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { API, request, useSiteStore } from '../stores/site';
import { buildScenicPagePath, normalizeScenicId } from '../utils/scenic';

const store = useSiteStore();
const route = useRoute();

const loading = ref(true);
const error = ref('');
const activeCategory = ref('');

const summary = ref({
  total: 0,
  visited: 0,
  categories: [],
});
const featuredSpots = ref([]);
const spots = ref([]);
const selectedSpotId = ref('');

const scenicId = computed(() => normalizeScenicId(route.params.scenicId));
const scenicName = computed(() => store.scenicById(scenicId.value)?.name || '寻境');
const homeData = computed(() => store.homeById(scenicId.value) || {
  scenicName: scenicName.value,
  description: '',
});

const explorePath = computed(() => ({
  path: buildScenicPagePath(scenicId.value, 'explore'),
  query: { entry: 'home' },
}));
const cloudPath = computed(() => buildScenicPagePath(scenicId.value, 'cloud-tour'));
const consultPath = computed(() => '/consult');

const categories = computed(() => {
  const fromSummary = Array.isArray(summary.value.categories)
    ? summary.value.categories.map((item) => ({
      label: `${item.name} (${item.count})`,
      value: item.name,
    }))
    : [];

  if (fromSummary.length) {
    return [{ label: '全部', value: '' }, ...fromSummary];
  }

  const unique = Array.from(new Set(spots.value.map((item) => item.category).filter(Boolean)));
  return [
    { label: '全部', value: '' },
    ...unique.map((name) => ({ label: name, value: name })),
  ];
});

const featuredCards = computed(() => {
  if (featuredSpots.value.length) return featuredSpots.value;
  return spots.value.filter((item) => item.isFeatured).slice(0, 3);
});

const filteredSpots = computed(() => {
  return spots.value.filter((item) => {
    const categoryMatched = !activeCategory.value || item.category === activeCategory.value;
    if (!categoryMatched) return false;
    return true;
  });
});

const selectedSpot = computed(() => spots.value.find((item) => item.id === selectedSpotId.value) || null);

function buildSpotExploreLink(spotId) {
  return {
    path: buildScenicPagePath(scenicId.value, 'explore'),
    query: { entry: 'home', spot: spotId },
  };
}

function openSpot(spotId) {
  selectedSpotId.value = spotId;
}

function closeSpot() {
  selectedSpotId.value = '';
}

async function loadPage() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([
      store.ensureScenic(scenicId.value),
      store.ensureHome(scenicId.value),
    ]);

    const [summaryData, featuredData, spotData] = await Promise.all([
      request(API.spotsSummary(scenicId.value)).catch(() => null),
      request(API.featuredSpots(scenicId.value)).catch(() => []),
      request(API.spots(scenicId.value)).catch(() => []),
    ]);

    summary.value = summaryData || { total: 0, visited: 0, categories: [] };
    featuredSpots.value = Array.isArray(featuredData) ? featuredData : [];
    spots.value = Array.isArray(spotData) ? spotData : [];
  } catch (err) {
    error.value = err?.message || '首页数据加载失败';
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.scenicId,
  () => {
    selectedSpotId.value = '';
    activeCategory.value = '';
    loadPage();
  },
);

onMounted(() => {
  loadPage();
});
</script>

<style scoped>
.home-page {
  min-height: 100%;
}

.hero-section {
  position: relative;
  overflow: hidden;
  padding: 42px 20px 84px;
  color: var(--text-light);
}

.hero-background {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(var(--brand-rgb), 0.14), transparent 24%),
    radial-gradient(circle at bottom right, rgba(var(--brand-soft-rgb), 0.18), transparent 28%),
    linear-gradient(180deg, rgba(247, 244, 238, 0.92) 0%, rgba(242, 237, 227, 0.96) 100%);
}

.hero-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.34;
  background-image: linear-gradient(90deg, rgba(var(--brand-rgb), 0.08) 0, rgba(var(--brand-rgb), 0.08) 1px, transparent 1px, transparent 120px);
}

.hero-main {
  position: relative;
  z-index: 1;
  width: min(1260px, 100%);
  margin: 0 auto;
  padding: 28px 30px 34px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  box-shadow: 0 24px 48px rgba(20, 42, 49, 0.08);
}

.hero-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 26px;
}

.hero-return,
.hero-kicker {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-size: 0.74rem;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(20, 42, 49, 0.06);
}

.hero-return {
  color: var(--text-light);
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.hero-return:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.92);
}

.hero-kicker {
  color: var(--muted-light);
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 24px;
  align-items: start;
}

.hero-copy {
  min-width: 0;
}

.hero-main h1 {
  margin: 0;
  font-family: 'Bodoni MT', 'Didot', 'Cormorant Garamond', 'Songti SC', serif;
  font-size: clamp(40px, 6vw, 72px);
  line-height: 1.06;
  letter-spacing: 0.02em;
  color: var(--brand-deep);
}

.hero-main h1 span {
  display: block;
  margin-top: 8px;
  color: rgba(var(--brand-rgb), 0.82);
  font-size: clamp(18px, 3vw, 30px);
  font-weight: 600;
}

.hero-main p {
  margin: 16px 0 0;
  max-width: 720px;
  color: var(--muted-light);
  line-height: 1.86;
  font-size: 1rem;
}

.hero-stats {
  display: grid;
  gap: 12px;
}

.hero-stats article {
  min-width: 120px;
  padding: 18px 18px 16px;
  background: rgba(255, 255, 255, 0.76);
  display: grid;
  gap: 8px;
  box-shadow: 0 14px 30px rgba(20, 42, 49, 0.05);
}

.hero-stats span {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--muted-light);
}

.hero-stats strong {
  display: block;
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
  font-size: clamp(30px, 5vw, 40px);
  line-height: 1;
  color: var(--brand-deep);
}

.hero-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.hero-actions-frame {
  margin-top: 26px;
  display: grid;
  gap: 14px;
  width: min(100%, 620px);
  padding: 18px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 16px 34px rgba(20, 42, 49, 0.05);
}

.hero-actions-caption {
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--muted-light);
}

.hero-action-card {
  min-width: 0;
  min-height: 132px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.92);
  display: grid;
  align-content: space-between;
  gap: 10px;
  box-shadow: 0 16px 34px rgba(20, 42, 49, 0.06);
  transition: transform 0.24s ease, box-shadow 0.24s ease, background 0.24s ease;
}

.hero-action-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 42px rgba(var(--brand-rgb), 0.14);
}

.hero-action-card__eyebrow {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--muted-light);
}

.hero-action-card strong {
  font-family: 'Bodoni MT', 'Didot', 'Cormorant Garamond', 'Songti SC', serif;
  font-size: clamp(1.6rem, 2.4vw, 2rem);
  line-height: 1.05;
  color: var(--brand-deep);
}

.hero-action-card small {
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--muted-light);
}

.hero-action-card--primary {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(var(--brand-rgb), 0.08));
}

.hero-action-card--secondary {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(var(--brand-soft-rgb), 0.12));
}

.home-wrap {
  width: min(1320px, calc(100% - 20px));
  margin: -42px auto 34px;
  position: relative;
  z-index: 2;
}

.category-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 38px rgba(20, 42, 49, 0.06);
}

.category-chip {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(51, 63, 56, 0.14);
  background: rgba(255, 255, 255, 0.76);
  color: var(--muted-light);
  transition: all 0.18s ease;
}

.category-chip.active {
  color: #fff9ef;
  border-color: rgba(var(--brand-rgb), 0.4);
  background: linear-gradient(135deg, rgba(var(--brand-rgb), 0.92), rgba(var(--brand-deep-rgb), 0.92));
  box-shadow: 0 10px 24px rgba(var(--brand-rgb), 0.24);
}

.block-section {
  margin-top: 16px;
  padding: 22px;
  border-radius: 0;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 38px rgba(20, 42, 49, 0.06);
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.block-head h2 {
  margin: 0;
  font-family: 'Bodoni MT', 'Didot', 'Cormorant Garamond', 'Songti SC', serif;
  font-size: clamp(30px, 4vw, 42px);
  color: var(--brand-deep);
}

.head-link,
.result-count {
  font-size: 13px;
  color: var(--muted-light);
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.featured-card {
  position: relative;
  min-height: 280px;
  border-radius: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease;
}

.featured-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 44px rgba(var(--brand-rgb), 0.18);
}

.featured-card img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(11, 16, 13, 0.1), rgba(11, 16, 13, 0.72));
}

.featured-content {
  position: absolute;
  inset: auto 16px 16px;
  color: #fff8ea;
  display: grid;
  gap: 8px;
}

.featured-content span {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.14);
  font-size: 11px;
}

.featured-content h3 {
  margin: 0;
  font-size: 26px;
  line-height: 1.08;
}

.featured-content p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 248, 234, 0.84);
  line-height: 1.8;
  max-width: 32ch;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.spot-card {
  border-radius: 0;
  background: #fffdf8;
  overflow: hidden;
  transition: transform 0.28s ease, box-shadow 0.28s ease;
}

.spot-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 44px rgba(var(--brand-rgb), 0.12);
}

.spot-cover-btn {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.spot-cover-btn img {
  width: 100%;
  height: 168px;
  object-fit: cover;
}

.spot-body {
  padding: 18px;
}

.spot-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  border: 1px solid rgba(51, 63, 56, 0.14);
  background: rgba(var(--brand-rgb), 0.08);
  color: var(--brand-deep);
}

.badge.visited {
  background: rgba(var(--brand-soft-rgb), 0.18);
}

.spot-body h3 {
  margin: 8px 0 6px;
  font-family: 'Bodoni MT', 'Didot', 'Cormorant Garamond', 'Songti SC', serif;
  font-size: 24px;
  color: var(--brand-deep);
}

.spot-body p {
  margin: 0;
  color: var(--muted-light);
  font-size: 12px;
  line-height: 1.7;
  min-height: 42px;
}

.spot-footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.spot-footer small {
  color: var(--muted-light);
  font-size: 11px;
  line-height: 1.5;
}

.btn.mini {
  min-height: 30px;
  padding: 0 12px;
}

.empty-card {
  padding: 18px;
  border-radius: 14px;
  border: 1px dashed rgba(51, 63, 56, 0.2);
  color: var(--muted-light);
}

.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(11, 16, 13, 0.58);
}

.detail-card {
  width: min(920px, 100%);
  max-height: calc(100vh - 36px);
  overflow: auto;
  border-radius: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(251, 245, 234, 0.9));
  box-shadow: 0 24px 56px rgba(11, 16, 13, 0.3);
  position: relative;
}

.detail-close {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(51, 63, 56, 0.2);
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-light);
  font-size: 22px;
}

.detail-hero img {
  width: 100%;
  height: 300px;
  object-fit: cover;
}

.detail-body {
  padding: 16px;
}

.detail-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-body h3 {
  margin: 10px 0 10px;
  font-size: clamp(24px, 3vw, 34px);
}

.detail-body p {
  margin: 0;
  color: var(--muted-light);
  line-height: 1.9;
}

.detail-info {
  margin-top: 14px;
  display: grid;
  gap: 6px;
  color: var(--text-light);
  font-size: 13px;
}

.detail-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1100px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .featured-grid {
    grid-template-columns: 1fr;
  }

  .spot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-section {
    padding: 22px 12px 62px;
  }

  .home-wrap {
    width: min(100% - 14px, 1180px);
    margin-top: -26px;
  }

  .hero-main {
    padding: 18px;
  }

  .hero-topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .block-section,
  .category-row {
    border-radius: 18px;
    padding: 14px;
  }

  .hero-actions {
    grid-template-columns: 1fr;
  }

  .hero-action-card {
    min-height: 116px;
    padding: 16px;
  }

  .spot-grid {
    grid-template-columns: 1fr;
  }

  .detail-hero img {
    height: 220px;
  }
}
</style>
