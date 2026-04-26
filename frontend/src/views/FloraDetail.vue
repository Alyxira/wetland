<template>
  <div class="flora-page">
    <div class="background-layer">
      <div class="bg-image"></div>
      <div class="bg-overlay"></div>
    </div>

    <SystemNav />

    <main class="page-shell">
      <div v-if="loading" class="state-card">
        <p>正在加载物种详情...</p>
      </div>

      <div v-else-if="errorMessage" class="state-card">
        <p>{{ errorMessage }}</p>
      </div>

      <section v-else-if="species" class="detail-layout">
        <div class="top-actions">
          <button class="back-link" type="button" @click="goBack">返回科普页</button>
        </div>

        <article class="feature-card">
          <div class="feature-media">
            <img :src="species.image" :alt="species.name" />
          </div>

          <div class="feature-copy">
            <div class="feature-text-content">
              <div class="feature-masthead">
                <span></span>
                <span>{{ wetland?.wetlandName || '' }}</span>
              </div>
              <h1>{{ species.name }}</h1>
              <p class="feature-date">记录时间 {{ species.createdTimeText }}</p>
              <p class="feature-description">{{ species.description || '暂无详细介绍。' }}</p>
              <div v-if="currentSpeciesQr" class="species-qr-panel">
                <img :src="currentSpeciesQr" :alt="`${species.name}二维码`" />
                <div>
                  <span>物种二维码</span>
                  <strong>{{ species.name }}</strong>
                </div>
              </div>
            </div>

            <div class="inline-graph-section">
              <div class="inline-graph-header">
                <span>生态关联网络</span>
              </div>
              <div ref="graphContainer" class="d3-canvas"></div>
            </div>
          </div>
        </article>

        <section class="detail-grid">
          <article class="detail-card">
            <span>所属景区</span>
            <p>{{ wetland?.wetlandName || '暂无景区信息' }}</p>
          </article>
          <article class="detail-card">
            <span>景区坐标</span>
            <p>{{ wetland?.coordinateRange || '暂无坐标信息' }}</p>
          </article>
          <article class="detail-card">
            <span>生态备注</span>
            <p>{{ wetland?.floraFaunaInfo || '暂无生态备注。' }}</p>
          </article>
        </section>

      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as d3 from 'd3'
import SystemNav from '../components/SystemNav.vue'
import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'
import pandaQr from '../resource/二维码/大熊猫.png'
import monkeyQr from '../resource/二维码/金丝猴.png'
import muskDeerQr from '../resource/二维码/林麝.png'
import takinQr from '../resource/二维码/扭角羚.png'

const route = useRoute()
const router = useRouter()
const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=1200&q=80'
const loading = ref(false)
const errorMessage = ref('')
const species = ref(null)
const wetland = ref(null)

const graphContainer = ref(null)
let simulation = null
const SPECIES_QR = [
  { keywords: ['大熊猫'], qrCode: pandaQr },
  { keywords: ['金丝猴', '川金丝猴'], qrCode: monkeyQr },
  { keywords: ['林麝'], qrCode: muskDeerQr },
  { keywords: ['扭角羚', '牛角岭'], qrCode: takinQr }
]
const currentSpeciesQr = computed(() => {
  const source = String(species.value?.name || '')
  return SPECIES_QR.find((item) => item.keywords.some((keyword) => source.includes(keyword)))?.qrCode || ''
})

const formatDateTime = (value) => {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无时间'
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

const goBack = () => {
  router.push({ name: 'Science', query: wetland.value?.id ? { wetlandId: String(wetland.value.id) } : undefined })
}

const getSafeId = (item) => item.id || item._id || item.speciesId || item.wetlandId || Math.random().toString()

const prepareGraphData = (centerSpecies, currentWetland, allSpeciesInWetland) => {
  const cId = getSafeId(centerSpecies)
  const wId = getSafeId(currentWetland)
  const getImgUrl = (path) => path ? resolveAssetUrl(path, FALLBACK_IMAGE) : FALLBACK_IMAGE

  const center = { 
    id: `s_${cId}`, realId: cId, name: centerSpecies.name, category: 'center', radius: 45, 
    image: getImgUrl(centerSpecies.imagePath || centerSpecies.image) 
  }
  
  const wetlandNode = { 
    id: `w_${wId}`, realId: wId, name: currentWetland.wetlandName, category: 'primary', radius: 35, 
    image: getImgUrl(currentWetland.imagePath || currentWetland.image) 
  }
  
  const neighbors = allSpeciesInWetland
    .filter(s => getSafeId(s) !== cId)
    .map(s => ({ 
      id: `s_${getSafeId(s)}`, realId: getSafeId(s), name: s.name, category: 'leaf', radius: 25, 
      image: getImgUrl(s.imagePath || s.image) 
    }))

  const nodes = [center, wetlandNode, ...neighbors]
  const links = []

  links.push({ source: center.id, target: wetlandNode.id })
  neighbors.forEach(neighbor => {
    links.push({ source: neighbor.id, target: wetlandNode.id })
  })

  return { nodes, links }
}

const drawGraph = (graphData) => {
  if (!graphContainer.value) return

  // 获取容器动态宽高，使其完美适应右侧留白区域
  const width = graphContainer.value.clientWidth || 500
  const height = graphContainer.value.clientHeight || 450 

  d3.select(graphContainer.value).selectAll('*').remove()

  const svg = d3.select(graphContainer.value)
    .append('svg')
    .attr('width', '100%')
    .attr('height', '100%')
    .attr('viewBox', [-width / 2, -height / 2, width, height])

  simulation = d3.forceSimulation(graphData.nodes)
    .force('link', d3.forceLink(graphData.links).id(d => d.id).distance(100))
    .force('charge', d3.forceManyBody().strength(-250)) 
    .force('collide', d3.forceCollide().radius(d => d.radius + 18).iterations(2))
    .force('center', d3.forceCenter(0, 0).strength(0.05))

  const link = svg.append('g')
    .attr('stroke', 'rgba(23, 37, 47, 0.15)')
    .attr('stroke-width', 1.5)
    .selectAll('line')
    .data(graphData.links)
    .join('line')

  const node = svg.append('g')
    .selectAll('g')
    .data(graphData.nodes)
    .join('g')
    // 增加鼠标指针样式，提示可点击
    .style('cursor', d => d.category === 'center' ? 'grab' : 'pointer')
    .call(d3.drag()
      .on('start', dragstarted)
      .on('drag', dragged)
      .on('end', dragended))
// ★ 核心交互：点击跳转逻辑 ★
    .on('click', (event, d) => {
      if (event.defaultPrevented) return; 

      if (d.category === 'leaf') {
        // 点击其他物种：跳转到该物种详情
        router.push({ params: { id: d.realId }, query: { wetlandId: wetland.value?.id } });
      } else if (d.category === 'primary') {
        // ★ 核心修改：名字必须和你 router 里配置的 'Detail' 完全一致 ★
        router.push({ name: 'Detail', params: { id: d.realId } });
      }
    })

  node.append('clipPath')
    .attr('id', d => `clip-${d.id}`)
    .append('circle')
    .attr('r', d => d.radius)

  node.append('image')
    .attr('href', d => d.image)
    .attr('x', d => -d.radius)
    .attr('y', d => -d.radius)
    .attr('width', d => d.radius * 2)
    .attr('height', d => d.radius * 2)
    .attr('clip-path', d => `url(#clip-${d.id})`)
    .attr('preserveAspectRatio', 'xMidYMid slice')

  node.append('circle')
    .attr('r', d => d.radius)
    .attr('fill', 'none')
    .attr('stroke', d => d.category === 'center' ? '#173227' : 'rgba(23, 37, 47, 0.15)')
    .attr('stroke-width', d => d.category === 'center' ? 3 : 1)

  node.append('text')
    .text(d => d.name)
    .attr('font-size', '13px')
    .attr('text-anchor', 'middle')
    .attr('dy', d => d.radius + 18)
    .attr('fill', d => d.category === 'leaf' ? '#17252f' : (d.category === 'center' ? '#173227' : '#17252f'))
    .attr('font-weight', d => d.category === 'center' ? '600' : '400')

  simulation.on('tick', () => {
    link
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y)
    node.attr('transform', d => `translate(${d.x},${d.y})`)
  })

  function dragstarted(event, d) {
    if (!event.active) simulation.alphaTarget(0.3).restart()
    d.fx = d.x; d.fy = d.y
  }
  function dragged(event, d) {
    d.fx = event.x; d.fy = event.y
  }
  function dragended(event, d) {
    if (!event.active) simulation.alphaTarget(0)
    d.fx = null; d.fy = null
  }
}

const loadSpeciesDetail = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const wetlandsResponse = await api.get('/wetlands')
    const wetlands = wetlandsResponse.data?.wetlands || []
    const routeWetlandId = String(route.query.wetlandId || '')
    const speciesId = String(route.params.id || '')
    const candidateWetlands = routeWetlandId ? wetlands.filter((item) => String(item.id) === routeWetlandId) : wetlands
    
    let matchedWetland = null
    let matchedSpecies = null
    let currentWetlandAllSpecies = []

    for (const item of candidateWetlands) {
      const response = await api.get(`/wetlands/${item.id}/flora-fauna`)
      const records = response.data?.floraFaunas || []
      const found = records.find((entry) => String(entry.id || entry._id) === speciesId)
      if (found) {
        matchedWetland = item
        matchedSpecies = found
        currentWetlandAllSpecies = records
        break
      }
    }

    if (!matchedSpecies || !matchedWetland) {
      errorMessage.value = '未找到对应的物种详情。'
      return
    }

    wetland.value = matchedWetland
    species.value = { ...matchedSpecies, image: resolveAssetUrl(matchedSpecies.imagePath, FALLBACK_IMAGE), createdTimeText: formatDateTime(matchedSpecies.createdTime) }

    setTimeout(() => {
      const graphData = prepareGraphData(matchedSpecies, matchedWetland, currentWetlandAllSpecies)
      drawGraph(graphData)
    }, 100)

  } catch (error) {
    console.error('加载详情失败:', error)
    errorMessage.value = '物种详情加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(loadSpeciesDetail)

// ★ 核心修复：监听路由变化，实现点击节点后无刷新加载新物种 ★
watch(() => route.params.id, (newId) => {
  if (newId) {
    if (simulation) {
      simulation.stop() // 加载新数据前停掉旧的物理引擎
    }
    loadSpeciesDetail()
  }
})

onBeforeUnmount(() => {
  if (simulation) {
    simulation.stop()
    simulation = null
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.flora-page { min-height: 100vh; position: relative; background: #eef1ec; color: #17252f; font-family: 'Manrope', 'PingFang SC', sans-serif; }
.background-layer { position: fixed; inset: 0; pointer-events: none; }
.bg-image, .bg-overlay { position: absolute; inset: 0; }
.bg-image { background: url('https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1800&q=80') center/cover no-repeat; filter: saturate(0.72) brightness(0.88); }
.bg-overlay { background: linear-gradient(180deg, rgba(245, 247, 241, 0.84) 0%, rgba(238, 242, 236, 0.94) 40%, rgba(236, 240, 233, 0.98) 100%), linear-gradient(90deg, rgba(23, 37, 47, 0.06) 0, rgba(23, 37, 47, 0.06) 1px, transparent 1px, transparent 140px); }
.page-shell { position: relative; z-index: 1; width: min(1180px, calc(100% - 40px)); margin: 0 auto; padding: 188px 0 72px; }
.detail-layout { display: grid; gap: 20px; }
.top-actions { display: flex; justify-content: flex-start; }
.back-link { border: none; background: transparent; font-size: 0.8rem; letter-spacing: 0.18em; text-transform: uppercase; color: rgba(23, 37, 47, 0.82); cursor: pointer; }
.state-card, .feature-card, .detail-card { border: 1px solid rgba(23, 37, 47, 0.14); background: rgba(255, 255, 255, 0.74); }
.state-card { padding: 24px; }

/* 卡片布局调整 */
.feature-card { display: grid; grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr); overflow: hidden; min-height: 700px; }
.feature-media { min-height: 100%; }
.feature-media img { width: 100%; height: 100%; object-fit: cover; display: block; }

/* 右侧区域变为 Flex 布局，实现上图下空的效果 */
.feature-copy { 
  display: flex; 
  flex-direction: column; 
  padding: 28px; 
  border-left: 1px solid rgba(23, 37, 47, 0.1); 
}
.feature-text-content {
  flex-shrink: 0; /* 文本部分高度由内容决定 */
}
.feature-masthead { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid rgba(23, 37, 47, 0.1); margin-bottom: 18px;}
.feature-masthead span, .feature-date, .detail-card span, .inline-graph-header span { font-size: 0.76rem; letter-spacing: 0.16em; text-transform: uppercase; color: rgba(23, 37, 47, 0.6); }
.feature-copy h1 { font-family: 'Cormorant Garamond', 'Songti SC', serif; font-size: clamp(3.2rem, 5vw, 5.2rem); line-height: 0.94; color: #173227; margin-bottom: 12px;}
.feature-description, .detail-card p { line-height: 1.92; font-size: 1rem; }
.species-qr-panel {
  display: inline-grid;
  grid-template-columns: 96px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding: 12px;
  border: 1px solid rgba(23, 37, 47, 0.12);
  background: rgba(255, 255, 255, 0.68);
}
.species-qr-panel img {
  width: 96px;
  height: 96px;
  object-fit: contain;
  display: block;
  background: #fff;
}
.species-qr-panel div {
  display: grid;
  gap: 8px;
}
.species-qr-panel span {
  font-size: 0.76rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(23, 37, 47, 0.6);
}
.species-qr-panel strong {
  color: #173227;
  font-size: 1rem;
}

/* 内嵌图表样式 */
.inline-graph-section {
  flex-grow: 1; /* 图表部分占据剩余所有高度 */
  display: flex;
  flex-direction: column;
  margin-top: 24px;
  border-top: 1px solid rgba(23, 37, 47, 0.1);
  padding-top: 18px;
  min-height: 350px; /* 给图表一个最小高度保底 */
}
.inline-graph-header {
  margin-bottom: 10px;
}
.d3-canvas {
  width: 100%;
  flex-grow: 1; /* 让 svg 容器撑满可用空间 */
  position: relative;
}

.detail-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.detail-card { display: grid; align-content: start; gap: 12px; padding: 22px; }

@media (max-width: 960px) { 
  .feature-card { grid-template-columns: 1fr; min-height: auto; } 
  .feature-copy { border-left: none; border-top: 1px solid rgba(23, 37, 47, 0.1); } 
  .feature-media { min-height: 400px; } 
  .inline-graph-section { min-height: 450px; }
}
@media (max-width: 720px) { 
  .page-shell { width: calc(100% - 24px); padding: 176px 0 64px; } 
  .state-card, .feature-copy, .detail-card { padding: 18px; } 
  .feature-copy h1 { font-size: clamp(2.5rem, 12vw, 4rem); } 
  .feature-masthead { flex-direction: column; }
}
</style>
