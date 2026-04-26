<template>
  <section class="distribution-maps">
    <div class="section-header">
      <h3>浓度分布图展示</h3>
      <p>时间轴拖动到某个时期时，这里会自动切换为该时期的两幅浓度分布图。</p>
    </div>

    <div class="picker-row">
      <span class="period-badge">
        当前联动时期：{{ activePeriodLabel }}
      </span>
      <button class="refresh-btn" @click="loadPngOptions" :disabled="loadingPngs">
        {{ loadingPngs ? '刷新中...' : '刷新列表' }}
      </button>
    </div>

    <p class="helper-text" v-if="!loadingPngs && pngOptions.length === 0">
      `frontend/public/distribution-maps/` 目录下暂无 PNG 图片。
    </p>

    <p class="helper-text" v-else-if="!linkedPngs.chla || !linkedPngs.tss">
      当前时期图片不完整：需要同时存在“叶绿素a”和“悬浮物”两幅图。
    </p>

    <div class="linked-grid" v-else>
      <div class="linked-card">
        <div class="card-header">
          <h4>叶绿素a浓度分布图</h4>
          <div class="card-actions">
            <span class="zoom-badge">{{ zoomPercent('chla') }}</span>
            <button class="mini-btn" @click="resetZoom('chla')">重置缩放</button>
            <button class="open-btn" @click="openModalByName(linkedPngs.chla.fileName)">查看详情</button>
          </div>
        </div>
        <div
          class="linked-viewer"
          :class="{ dragging: zoomStates.chla.dragging }"
          @wheel.prevent="onWheel($event)"
          @mousedown="onDragStart($event)"
        >
          <div class="zoom-layer" :class="{ dragging: zoomStates.chla.dragging }" :style="zoomLayerStyle('chla')">
            <img
              class="linked-image"
              :src="linkedPngs.chla.url"
              :alt="linkedPngs.chla.fileName"
              draggable="false"
              @dragstart.prevent
            />
          </div>
        </div>
      </div>
      <div class="linked-card">
        <div class="card-header">
          <h4>悬浮物浓度分布图</h4>
          <div class="card-actions">
            <span class="zoom-badge">{{ zoomPercent('tss') }}</span>
            <button class="mini-btn" @click="resetZoom('tss')">重置缩放</button>
            <button class="open-btn" @click="openModalByName(linkedPngs.tss.fileName)">查看详情</button>
          </div>
        </div>
        <div
          class="linked-viewer"
          :class="{ dragging: zoomStates.tss.dragging }"
          @wheel.prevent="onWheel($event)"
          @mousedown="onDragStart($event)"
        >
          <div class="zoom-layer" :class="{ dragging: zoomStates.tss.dragging }" :style="zoomLayerStyle('tss')">
            <img
              class="linked-image"
              :src="linkedPngs.tss.url"
              :alt="linkedPngs.tss.fileName"
              draggable="false"
              @dragstart.prevent
            />
          </div>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal-dialog" role="dialog" aria-modal="true" aria-label="浓度分布图与反演公式介绍">
        <div class="modal-header">
          <h4>{{ activePng?.fileName || '浓度分布图' }}</h4>
          <button class="close-btn" @click="closeModal" aria-label="关闭弹窗">×</button>
        </div>

        <div class="modal-body" v-if="activePng">
          <div class="map-wrap" v-if="!failedMap[activePng.fileName]">
            <img
              class="map-image"
              :src="activePng.url"
              :alt="activePng.fileName"
              @error="markFailed(activePng.fileName)"
            />
          </div>
          <div class="map-empty" v-else>
            <p>未找到图片：{{ activePng.url }}</p>
            <p>请确认文件存在于 `frontend/public/distribution-maps/`。</p>
          </div>

          <div class="model-card">
            <h5>{{ activeModel.name }}</h5>
            <p><strong>核心公式：</strong>{{ activeModel.coreFormula }}</p>
            <p><strong>参数说明：</strong>{{ activeModel.parameterDescription }}</p>
            <p><strong>公式推导：</strong>{{ activeModel.derivation }}</p>
            <p><strong>{{ activeModel.levelTitle }}：</strong>{{ activeModel.levelDescription }}</p>

            <div v-if="activeModel.models && activeModel.models.length" class="sub-section">
              <p><strong>多模型融合：</strong></p>
              <ol>
                <li v-for="formula in activeModel.models" :key="formula">{{ formula }}</li>
              </ol>
            </div>

            <p v-if="activeModel.featureFusion"><strong>多特征融合优化：</strong>{{ activeModel.featureFusion }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { imageApi } from '../api'

const props = defineProps({
  activePeriodKey: {
    type: String,
    default: ''
  }
})

const models = {
  chla: {
    name: '1. 叶绿素a浓度反演公式',
    coreFormula: 'Chl-a = a × (R_blue / R_green)^b',
    parameterDescription: 'a、b 为经验系数，表征水体光学特性与区域差异，需结合传感器与样本标定。',
    derivation: '先做反射率预处理，计算蓝绿比值后代入幂函数模型，再结合样本校准。',
    levelTitle: '水质等级划分',
    levelDescription: '采用 8 个富营养化等级进行颜色表示。'
  },
  tss: {
    name: '2. 悬浮物浓度反演公式',
    coreFormula: 'SPM 采用多模型融合反演框架。',
    parameterDescription: '参数 a、b、c 由区域样本反演得到，描述反射率与悬浮物浓度关系。',
    derivation: '按浑浊区间选取或加权组合模型，并进行多特征比值修正。',
    models: [
      '幂函数模型：SPM = a × (R_red)^b + c',
      '指数函数模型：SPM = a × exp(b × R_red) + c',
      '高浑浊指数模型：SPM = a × exp(b × R_red) + c × (R_red / R_green)',
      '线性+指数混合模型：SPM = a × R_red × exp(b × R_red) + c',
      '二次函数模型：SPM = a × (R_red)^2 + b × R_red + c'
    ],
    featureFusion: '红绿比值、蓝红比值、蓝绿比值校正。',
    levelTitle: '浑浊度等级划分',
    levelDescription: '采用 7 个浑浊度等级进行颜色表示。'
  }
}

const pngOptions = ref([])
const loadingPngs = ref(false)
const selectedFileName = ref('')
const showModal = ref(false)
const failedMap = ref({})

const activePng = computed(() => pngOptions.value.find((item) => item.fileName === selectedFileName.value) || null)

const activeModel = computed(() => {
  const fileName = (activePng.value?.fileName || '').toLowerCase()
  const isTss = fileName.includes('悬浮') || fileName.includes('tss')
  return isTss ? models.tss : models.chla
})

const activePeriodLabel = computed(() => {
  if (!props.activePeriodKey) return '未选择'
  const [year, season] = props.activePeriodKey.split('-')
  const seasonMap = { spring: '春季', summer: '夏季', autumn: '秋季', winter: '冬季' }
  return `${year || '未知年份'} ${seasonMap[season] || '未知季节'}`
})

const linkedPngs = computed(() => {
  if (!props.activePeriodKey) {
    return { chla: null, tss: null }
  }
  const samePeriod = pngOptions.value.filter(
    (item) => parsePeriodKeyFromFileName(item.fileName) === props.activePeriodKey
  )
  const chla = samePeriod.find((item) => parseMapType(item.fileName) === 'chla') || null
  const tss = samePeriod.find((item) => parseMapType(item.fileName) === 'tss') || null
  return { chla, tss }
})

const MIN_SCALE = 1
const MAX_SCALE = 4
const ZOOM_STEP = 0.12
const zoomStates = ref({
  chla: createZoomState(),
  tss: createZoomState()
})

const dragSyncState = ref({
  active: false,
  lastX: 0,
  lastY: 0
})

onMounted(() => {
  loadPngOptions()
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('mousemove', onGlobalMouseMove)
  window.addEventListener('mouseup', onGlobalMouseUp)
  window.addEventListener('blur', onGlobalBlur)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('mousemove', onGlobalMouseMove)
  window.removeEventListener('mouseup', onGlobalMouseUp)
  window.removeEventListener('blur', onGlobalBlur)
})

watch(
  [() => props.activePeriodKey, linkedPngs],
  () => {
    const preferred = linkedPngs.value.chla || linkedPngs.value.tss
    if (preferred) {
      selectedFileName.value = preferred.fileName
    }
  },
  { immediate: true }
)

async function loadPngOptions() {
  loadingPngs.value = true
  try {
    const response = await imageApi.getDistributionMapPngs()
    pngOptions.value = response.data || []
    if (!selectedFileName.value && pngOptions.value.length > 0) {
      selectedFileName.value = pngOptions.value[0].fileName
    }
  } catch (error) {
    pngOptions.value = []
  } finally {
    loadingPngs.value = false
  }
}

function openModal() {
  if (!activePng.value) return
  showModal.value = true
}

function openModalByName(fileName) {
  if (!fileName) return
  selectedFileName.value = fileName
  openModal()
}

function closeModal() {
  showModal.value = false
}

function markFailed(fileName) {
  failedMap.value = {
    ...failedMap.value,
    [fileName]: true
  }
}

function onKeydown(event) {
  if (event.key === 'Escape' && showModal.value) {
    closeModal()
  }
}

function createZoomState() {
  return {
    scale: 1,
    offsetX: 0,
    offsetY: 0,
    dragging: false,
    lastX: 0,
    lastY: 0
  }
}

function zoomLayerStyle(type) {
  const state = zoomStates.value[type]
  return {
    transform: `translate(${state.offsetX}px, ${state.offsetY}px) scale(${state.scale})`,
    transformOrigin: 'center center'
  }
}

function zoomPercent(type) {
  const state = zoomStates.value[type]
  return `${Math.round(state.scale * 100)}%`
}

function resetZoom(_type) {
  for (const key of ['chla', 'tss']) {
    const state = zoomStates.value[key]
    state.scale = 1
    state.offsetX = 0
    state.offsetY = 0
    state.dragging = false
  }
  dragSyncState.value.active = false
}

function onWheel(event) {
  const currentScale = zoomStates.value.chla.scale
  const direction = event.deltaY < 0 ? 1 : -1
  const next = clamp(currentScale + direction * ZOOM_STEP, MIN_SCALE, MAX_SCALE)
  for (const key of ['chla', 'tss']) {
    const state = zoomStates.value[key]
    state.scale = next
  }

  if (next === MIN_SCALE) {
    for (const key of ['chla', 'tss']) {
      zoomStates.value[key].offsetX = 0
      zoomStates.value[key].offsetY = 0
    }
  }
}

function onDragStart(event) {
  if (event.button !== 0) return
  event.preventDefault()

  dragSyncState.value.active = true
  dragSyncState.value.lastX = event.clientX
  dragSyncState.value.lastY = event.clientY
  zoomStates.value.chla.dragging = true
  zoomStates.value.tss.dragging = true
}

function onGlobalMouseMove(event) {
  if (!dragSyncState.value.active) return
  if ((event.buttons & 1) === 0) {
    onDragEnd()
    return
  }
  event.preventDefault()

  const dx = event.clientX - dragSyncState.value.lastX
  const dy = event.clientY - dragSyncState.value.lastY
  dragSyncState.value.lastX = event.clientX
  dragSyncState.value.lastY = event.clientY

  zoomStates.value.chla.offsetX += dx
  zoomStates.value.chla.offsetY += dy
  zoomStates.value.tss.offsetX += dx
  zoomStates.value.tss.offsetY += dy
}

function onDragEnd() {
  dragSyncState.value.active = false
  zoomStates.value.chla.dragging = false
  zoomStates.value.tss.dragging = false
}

function onGlobalMouseUp() {
  onDragEnd()
}

function onGlobalBlur() {
  onDragEnd()
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function parsePeriodKeyFromFileName(fileName) {
  const name = (fileName || '').replace(/\.png$/i, '')
  const year = name.match(/(\d{4})/)?.[1]
  if (!year) return ''

  if (name.includes('春')) return `${year}-spring`
  if (name.includes('夏')) return `${year}-summer`
  if (name.includes('秋')) return `${year}-autumn`
  if (name.includes('冬')) return `${year}-winter`
  return ''
}

function parseMapType(fileName) {
  const lower = (fileName || '').toLowerCase()
  if (lower.includes('悬浮') || lower.includes('tss') || lower.includes('spm')) return 'tss'
  if (lower.includes('叶绿素') || lower.includes('chla')) return 'chla'
  return 'unknown'
}
</script>

<style scoped>
.distribution-maps {
  background: transparent;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
}

.section-header h3 {
  font-size: 28px;
  color: var(--theme-accent-strong, #173843);
  margin-bottom: 4px;
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.section-header p {
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  font-size: 15px;
  margin-bottom: 18px;
  line-height: 1.7;
}

.picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.period-badge {
  display: inline-flex;
  align-items: center;
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
  color: var(--theme-accent-strong, #173843);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.14);
  border-radius: 999px;
  padding: 10px 14px;
  font-size: 14px;
  font-weight: 600;
}

.png-select {
  min-width: 360px;
  max-width: 760px;
  flex: 1;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 14px;
}

.refresh-btn,
.open-btn,
.mini-btn {
  border-radius: 999px;
  padding: 10px 14px;
  font-size: 13px;
  cursor: pointer;
}

.refresh-btn {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16);
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
  color: var(--theme-accent-strong, #173843);
  font-weight: 700;
}

.open-btn {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.2);
  background: rgb(var(--theme-accent-rgb, 45, 89, 103));
  color: #fff;
  font-weight: 700;
}

.mini-btn {
  background: rgba(255, 255, 255, 0.82);
  color: var(--theme-text-primary, #142a31);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  font-size: 12px;
  padding: 6px 10px;
}

.open-btn:disabled,
.refresh-btn:disabled,
.png-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.helper-text {
  margin-top: 12px;
  font-size: 14px;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
  line-height: 1.7;
}

.linked-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.linked-card {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.62);
  padding: 14px;
  backdrop-filter: blur(10px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 8px;
}

.card-header h4 {
  margin: 0;
  font-size: 20px;
  color: var(--theme-accent-strong, #173843);
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.zoom-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 12px;
  color: var(--theme-accent-strong, #173843);
  background: rgba(var(--theme-accent-rgb, 45, 89, 103), 0.08);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.14);
}

.linked-viewer {
  border-radius: 0;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  overflow: hidden;
  cursor: grab;
  user-select: none;
}

.linked-viewer.dragging {
  cursor: grabbing;
}

.zoom-layer {
  position: relative;
  transition: transform 0.08s linear;
  will-change: transform;
}

.zoom-layer.dragging {
  transition: none;
}

.linked-image {
  width: 100%;
  height: auto;
  display: block;
  user-select: none;
  -webkit-user-drag: none;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 42, 49, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  z-index: 1200;
}

.modal-dialog {
  width: min(1080px, 96vw);
  max-height: 92vh;
  overflow-y: auto;
  background: rgba(248, 246, 240, 0.98);
  border-radius: 0;
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16);
  box-shadow: 0 20px 46px rgba(15, 23, 42, 0.22);
  padding: 18px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.modal-header h4 {
  margin: 0;
  font-size: 28px;
  color: var(--theme-accent-strong, #173843);
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.close-btn {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.16);
  background: rgba(255, 255, 255, 0.8);
  color: var(--theme-text-primary, #142a31);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.modal-body {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 14px;
  align-items: start;
}

.map-wrap {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.7);
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.map-image {
  width: auto;
  max-width: 100%;
  max-height: 72vh;
  display: block;
  object-fit: contain;
}

.map-empty {
  border: 1px dashed rgba(var(--theme-accent-rgb, 45, 89, 103), 0.24);
  border-radius: 0;
  padding: 20px;
  text-align: center;
  color: var(--theme-text-secondary, rgba(20, 42, 49, 0.78));
}

.model-card {
  border: 1px solid rgba(var(--theme-accent-rgb, 45, 89, 103), 0.12);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.72);
  padding: 16px;
}

.model-card h5 {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--theme-accent-strong, #173843);
  font-family: 'Cormorant Garamond', 'Songti SC', serif;
}

.model-card p {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--theme-text-primary, #142a31);
}

.sub-section {
  margin: 8px 0;
}

.sub-section p {
  margin-bottom: 4px;
}

.sub-section ol {
  margin: 0;
  padding-left: 20px;
}

.sub-section li {
  margin-bottom: 4px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--theme-text-primary, #142a31);
}

@media (max-width: 900px) {
  .linked-grid {
    grid-template-columns: 1fr;
  }

  .modal-body {
    grid-template-columns: 1fr;
  }

  .map-wrap {
    min-height: 320px;
  }

  .map-image {
    max-height: 48vh;
  }
}

@media (max-width: 768px) {
  .distribution-maps {
    padding: 14px;
  }

  .png-select {
    min-width: 100%;
  }
}
</style>


