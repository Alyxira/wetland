<template>
  <div v-if="config.enabled" class="live2d-root" :class="[`page-${effectiveUiMode}`, { 'is-explore-layout': isExplorePage }]">
    <button
      v-if="ui.collapsed"
      class="live2d-reopen"
      :class="{ 'live2d-reopen-explore': isExplorePage }"
      :style="reopenStyle"
      type="button"
      @click="reopenAssistant"
    >
      <span class="live2d-reopen-icon">☁</span>
      <span>{{ isExplorePage ? `${config.widgetName}助手` : config.widgetName }}</span>
    </button>

    <div v-show="!ui.collapsed" class="live2d-active-layer">
      <template v-if="isExplorePage">
        <aside class="live2d-explore-panel">
          <div class="live2d-explore-stage-wrap">
            <div class="live2d-explore-stage-terrain" aria-hidden="true"></div>
            <div class="live2d-explore-stage-lower-glow" aria-hidden="true"></div>
            <div class="live2d-explore-stage-ripples" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <div class="live2d-explore-stage-reeds" aria-hidden="true"></div>
            <div class="live2d-explore-stage-atmo" aria-hidden="true"></div>
            <div class="live2d-explore-stage-mountains" aria-hidden="true"></div>
            <div class="live2d-explore-stage-fireflies" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
              <span></span>
              <span></span>
              <span></span>
            </div>
            <div
              class="live2d-stage-shell explore-stage-shell"
              :class="{
                adjusting: ui.stage.isAdjusting || ui.model.isAdjusting,
                dragging: dragState.kind === 'stage' || dragState.kind === 'resize' || modelDrag.active,
              }"
              :style="stageStyle"
              @mousedown="startStageDrag"
            >
              <div class="live2d-stage-glow"></div>
              <canvas ref="stageCanvasRef" class="live2d-stage-canvas"></canvas>
              <div v-if="!modelReady" class="live2d-fallback" @click="toggleChatInput">
                <strong>{{ runtimeStatus ? '加载中' : config.widgetName }}</strong>
              </div>

              <button
                v-if="ui.stage.isAdjusting"
                class="live2d-resize-handle"
                type="button"
                @mousedown.stop="startStageResize"
              >
                ↘
              </button>
            </div>
          </div>

          <header class="live2d-explore-head">
            <div class="live2d-explore-assistant">
              <div class="assistant-avatar">✦</div>
              <div>
                <strong>{{ config.widgetName }}</strong>
                <p><span class="assistant-presence"></span>你的专属旅游向导</p>
              </div>
            </div>
            <div class="live2d-explore-actions">
              <button
                type="button"
                class="explore-action-btn"
                :class="{ active: activePanel === 'history' }"
                @click="togglePanel('history')"
                aria-label="聊天记录"
                title="聊天记录"
              >
                对话记录
              </button>
              <button
                type="button"
                class="explore-action-btn"
                :disabled="sending"
                @click="startNewRound"
                aria-label="新一轮"
                title="新一轮"
              >
                新对话
              </button>
              <button type="button" class="explore-action-btn" @click="collapseAssistant" aria-label="收起" title="收起">收起</button>
            </div>
          </header>

          <section v-if="activePanel === 'history'" class="live2d-explore-history">
            <div class="history-tools explore-history-tools">
              <span>共 {{ historyRoundList.length }} 轮</span>
              <div class="history-tool-actions">
                <button class="drawer-ghost small explore-history-chip" type="button" @click="startNewRound">新一轮</button>
                <button class="drawer-ghost small explore-history-chip" type="button" @click="clearHistory">清空记录</button>
              </div>
            </div>
            <div class="round-list live2d-explore-round-list">
              <button
                v-for="round in historyRoundList"
                :key="round.id"
                :class="['round-chip', { active: selectedHistoryRoundId === round.id }]"
                type="button"
                @click="selectHistoryRound(round.id)"
              >
                <strong>第 {{ round.id }} 轮</strong>
                <small>{{ round.preview }}</small>
              </button>
            </div>
            <div class="history-list live2d-explore-history-list">
              <div v-if="!selectedRoundMessages.length" class="history-empty">该轮暂无记录</div>
              <article
                v-for="item in selectedRoundMessages"
                :key="item.id"
                :class="['history-item', `role-${item.role}`]"
              >
                <div class="history-role">{{ roleLabel(item.role) }}</div>
                <div class="history-content">{{ item.content }}</div>
                <div class="history-time">{{ formatTime(item.timestamp) }}</div>
              </article>
            </div>
          </section>

          <template v-else>
            <section class="live2d-explore-chat-shell">
              <div ref="chatThreadRef" class="chat-thread live2d-explore-thread">
                <div v-if="!currentRoundMessages.length" class="chat-thread-empty">
                  你好，我是{{ config.widgetName }}。可以问我景点推荐、路线规划、最佳游览时间等问题。
                </div>
                <article
                  v-for="item in currentRoundMessages"
                  :key="item.id"
                  :class="['chat-item', `role-${item.role}`]"
                >
                  <div class="chat-role">{{ roleLabel(item.role) }}</div>
                  <div class="chat-text">{{ item.content }}</div>
                  <div v-if="item.relatedSpots?.length" class="live2d-output-spots">
                    <button
                      v-for="spot in item.relatedSpots"
                      :key="`${item.id}_${spot.id}`"
                      type="button"
                      class="spot-chip"
                      @click="goToSpot(spot.id)"
                    >
                      {{ spot.name }}
                    </button>
                  </div>
                  <div v-if="item.routePlan" class="live2d-output-actions">
                    <button type="button" class="route-chip" @click="applyAssistantRoute(item.routePlan)">
                      一键应用路线
                    </button>
                  </div>
                  <div class="history-time">{{ formatTime(item.timestamp) }}</div>
                </article>
              </div>

              <div class="live2d-explore-chip-row">
                <button
                  v-for="chip in exploreQuickChips"
                  :key="chip"
                  type="button"
                  class="live2d-explore-chip"
                  :disabled="sending"
                  @click="sendQuickPrompt(chip)"
                >
                  {{ chip }}
                </button>
              </div>

              <form class="live2d-explore-input" @submit.prevent="sendMessage">
                <textarea
                  ref="inputRef"
                  v-model="inputValue"
                  class="live2d-textarea live2d-explore-textarea"
                  rows="1"
                  :disabled="ui.chat.isAdjusting || sending"
                  :placeholder="inputPlaceholder"
                  @input="handleTextareaInput"
                  @keydown="handleTextareaKeydown"
                ></textarea>
                <button class="live2d-send live2d-explore-send" type="submit" :disabled="sending || ui.chat.isAdjusting">
                  {{ sending ? '发送中' : '发送' }}
                </button>
              </form>
              <p class="live2d-explore-disclaimer">AI 可能会出现误差，请以景区官方信息为准</p>
            </section>
          </template>
        </aside>

        <Transition name="intro-fade">
          <div
            v-if="introVisible"
            class="live2d-intro-tip live2d-intro-tip-explore"
            :style="introTipStyle"
            role="status"
            aria-live="polite"
          >
            <strong>{{ config.widgetName }}</strong>
            <p>{{ introMessage }}</p>
            <button type="button" @click="dismissIntroTip">知道了</button>
          </div>
        </Transition>
      </template>

      <template v-else>
      <div
        class="live2d-stage-shell"
        :class="{
          adjusting: ui.stage.isAdjusting || ui.model.isAdjusting,
          dragging: dragState.kind === 'stage' || dragState.kind === 'resize' || modelDrag.active,
        }"
        :style="stageStyle"
        @mousedown="startStageDrag"
      >
        <div class="live2d-stage-glow"></div>
        <canvas ref="stageCanvasRef" class="live2d-stage-canvas"></canvas>
        <div v-if="!modelReady" class="live2d-fallback" @click="toggleChatInput">
          <strong>{{ runtimeStatus ? '加载中' : config.widgetName }}</strong>
        </div>

        <button
          v-if="ui.stage.isAdjusting"
          class="live2d-resize-handle"
          type="button"
          @mousedown.stop="startStageResize"
        >
          ↘
        </button>
      </div>

      <div
        v-if="modelReady && !ui.stage.isAdjusting"
        class="live2d-hit-area"
        :style="hitAreaStyle"
        role="button"
        :aria-label="`操作${config.widgetName}`"
        @pointerdown.prevent.stop="handleStagePointerDown"
        @mousedown.prevent.stop="handleStageMouseDown"
        @touchstart.prevent.stop="handleStageTouchStart"
      ></div>

      <div class="live2d-dock" :style="dockStyle">
        <button class="dock-btn primary" type="button" @click="openInputBubble">对话</button>
        <button class="dock-btn" type="button" @click="togglePanel('history')">聊天记录</button>
        <button class="dock-btn" type="button" @click="collapseAssistant">最小化</button>
      </div>

      <Transition name="intro-fade">
        <div
          v-if="introVisible"
          class="live2d-intro-tip"
          :style="introTipStyle"
          role="status"
          aria-live="polite"
        >
          <strong>{{ config.widgetName }}</strong>
          <p>{{ introMessage }}</p>
          <button type="button" @click="dismissIntroTip">知道了</button>
        </div>
      </Transition>

      <form
        v-if="inputVisible"
        class="live2d-chat-input"
        :class="{ adjusting: ui.chat.isAdjusting }"
        :style="inputBubbleStyle"
        @submit.prevent="sendMessage"
      >
        <div class="chat-panel-head">
          <strong>第 {{ currentRoundId }} 轮对话</strong>
          <div class="chat-panel-actions">
            <button class="chat-mini-btn" type="button" :disabled="sending" @click="startNewRound">新一轮</button>
            <button class="chat-mini-btn" type="button" :disabled="sending" @click="closeInputBubble">关闭</button>
          </div>
        </div>
        <div ref="chatThreadRef" class="chat-thread">
          <div v-if="!currentRoundMessages.length" class="chat-thread-empty">
            这里会持续显示当前轮次对话，点击“新一轮”后会切换到下一轮。
          </div>
          <article
            v-for="item in currentRoundMessages"
            :key="item.id"
            :class="['chat-item', `role-${item.role}`]"
          >
            <div class="chat-role">{{ roleLabel(item.role) }}</div>
            <div class="chat-text">{{ item.content }}</div>
            <div v-if="item.relatedSpots?.length" class="live2d-output-spots">
              <button
                v-for="spot in item.relatedSpots"
                :key="`${item.id}_${spot.id}`"
                type="button"
                class="spot-chip"
                @click="goToSpot(spot.id)"
              >
                {{ spot.name }}
              </button>
            </div>
            <div v-if="item.routePlan" class="live2d-output-actions">
              <button type="button" class="route-chip" @click="applyAssistantRoute(item.routePlan)">
                一键应用路线
              </button>
            </div>
            <div class="history-time">{{ formatTime(item.timestamp) }}</div>
          </article>
        </div>
        <textarea
          ref="inputRef"
          v-model="inputValue"
          class="live2d-textarea"
          rows="1"
          :disabled="ui.chat.isAdjusting || sending"
          :placeholder="inputPlaceholder"
          @input="handleTextareaInput"
          @keydown="handleTextareaKeydown"
        ></textarea>
        <div class="live2d-input-actions">
          <button class="live2d-send" type="submit" :disabled="sending || ui.chat.isAdjusting">
            {{ sending ? '发送中' : '发送' }}
          </button>
        </div>
      </form>

      <aside v-if="activePanel === 'history'" class="live2d-drawer" :style="drawerStyle">
        <div class="drawer-header">
          <strong>对话轮次</strong>
          <button class="drawer-close" type="button" @click="activePanel = ''">×</button>
        </div>

        <section class="drawer-section">
          <div class="history-tools">
            <span>共 {{ historyRoundList.length }} 轮</span>
            <div class="history-tool-actions">
              <button class="drawer-ghost small" type="button" @click="startNewRound">新一轮</button>
              <button class="drawer-ghost small" type="button" @click="clearHistory">清空记录</button>
            </div>
          </div>
          <div class="round-list">
            <button
              v-for="round in historyRoundList"
              :key="round.id"
              :class="['round-chip', { active: selectedHistoryRoundId === round.id }]"
              type="button"
              @click="selectHistoryRound(round.id)"
            >
              <strong>第 {{ round.id }} 轮</strong>
              <small>{{ round.preview }}</small>
            </button>
          </div>
          <div class="history-list">
            <div v-if="!selectedRoundMessages.length" class="history-empty">该轮暂无记录</div>
            <article
              v-for="item in selectedRoundMessages"
              :key="item.id"
              :class="['history-item', `role-${item.role}`]"
            >
              <div class="history-role">{{ roleLabel(item.role) }}</div>
              <div class="history-content">{{ item.content }}</div>
              <div class="history-time">{{ formatTime(item.timestamp) }}</div>
            </article>
          </div>
        </section>
      </aside>

      <aside v-if="activePanel === 'adjust'" class="live2d-drawer" :style="drawerStyle">
        <div class="drawer-header">
          <strong>助手调节</strong>
          <button class="drawer-close" type="button" @click="activePanel = ''">×</button>
        </div>

        <section class="drawer-section">
          <article class="setting-card">
            <div class="setting-row">
              <div>
                <strong>人物拖动模式</strong>
                <p>开启后可直接拖动人物；关闭时可长按人物自动进入拖动。</p>
              </div>
              <button
                type="button"
                :class="['state-toggle', { active: ui.model.isAdjusting }]"
                @click="setAdjustmentMode('model')"
              >
                {{ ui.model.isAdjusting ? '已开启' : '已关闭' }}
              </button>
            </div>
            <div class="setting-row">
              <div>
                <strong>画布调节模式</strong>
                <p>开启后可拖动画布位置，并拖动右下角手柄调整画布大小。</p>
              </div>
              <button
                type="button"
                :class="['state-toggle', { active: ui.stage.isAdjusting }]"
                @click="setAdjustmentMode('stage')"
              >
                {{ ui.stage.isAdjusting ? '已开启' : '已关闭' }}
              </button>
            </div>
          </article>

          <div class="drawer-action-row">
            <button class="drawer-ghost" type="button" @click="setAdjustmentMode('off')">关闭调节</button>
            <button class="drawer-primary" type="button" @click="resetLayout">重置布局</button>
          </div>
        </section>
      </aside>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { API, request, useSiteStore } from '../stores/site';
import { DEFAULT_SCENIC_ID, buildScenicPagePath, buildScenicStorageKey, normalizeScenicId } from '../utils/scenic';

const props = defineProps({
  page: {
    type: String,
    default: 'home',
  },
  uiMode: {
    type: String,
    default: '',
  },
  scenicId: {
    type: String,
    default: DEFAULT_SCENIC_ID,
  },
});

const LEGACY_LIVE2D_UI_KEY_PREFIX = 'wetland_live2d_ui_v5';
const LEGACY_LIVE2D_HISTORY_KEY = 'wetland_live2d_history_v2';
const INPUT_BUBBLE_MAX_WIDTH = 380;
const INPUT_BUBBLE_MAX_HEIGHT = 560;
const DRAWER_WIDTH = 280;
const DEFAULT_WIDGET_NAME = '团子';
const DEFAULT_GREETING = '你好呀，我是团子，可以结合当前景区为你推荐景点与路线。';
const DEFAULT_REPLY = '我可以继续帮你推荐景点和路线。';
const DEFAULT_CLICK_INTRO = '我是团子，我可以给你介绍景区内容、推荐路线和玩法安排。';
const EXPLORE_LEFT_MIGRATION_KEY_SCOPE = 'live2d-explore-left-preset';
const ASSISTANT_ROUTE_EVENT = 'scenic:assistant-route-plan';
const ASSISTANT_ROUTE_STORAGE_SCOPE = 'assistant-route-plan';
const ORIGINAL_STAGE_WIDTH = 300;
const ORIGINAL_STAGE_HEIGHT = 600;
const ORIGINAL_MODEL_SCALE = 0.25;
const ORIGINAL_MODEL_X = -80;
const ORIGINAL_MODEL_Y = 40;
const EXPLORE_QUICK_CHIPS = ['景点推荐', '最佳游览时间', '路线规划', '门票价格', '当地美食'];

const DEFAULT_EXPRESSIONS = {
  Param4: { Name: '白眼', Value: 0 },
  Param5: { Name: '灵动', Value: 0 },
  Param8: { Name: '嘴型', Value: 0 },
  Param6: { Name: '兽耳', Value: 0 },
  Param3: { Name: '长发', Value: 0 },
  Param7: { Name: '唱歌', Value: 0 },
};

const router = useRouter();
const store = useSiteStore();
const resolvedScenicId = computed(() => normalizeScenicId(props.scenicId || DEFAULT_SCENIC_ID));
const effectiveUiMode = computed(() => {
  const raw = String(props.uiMode || props.page || 'home').trim().toLowerCase();
  return raw || 'home';
});
const isExplorePage = computed(() => String(props.page || '').trim().toLowerCase() === 'explore');
const exploreQuickChips = EXPLORE_QUICK_CHIPS;

const stageCanvasRef = ref(null);
const inputRef = ref(null);
const chatThreadRef = ref(null);
const activePanel = ref('');
const sending = ref(false);
const modelReady = ref(false);
const inputVisible = ref(false);
const inputValue = ref('');
const viewportTick = ref(0);
const uiReady = ref(false);
const pageHidden = ref(typeof document !== 'undefined' ? document.hidden : false);
const runtimeStatus = ref('模型加载中...');
const introVisible = ref(false);
const introTriggered = ref(false);
const messages = ref(loadHistory());
const currentRoundId = ref(resolveCurrentRoundId(messages.value));
const selectedHistoryRoundId = ref(currentRoundId.value);

const dragState = reactive({
  kind: null,
  startX: 0,
  startY: 0,
  startLeft: 0,
  startTop: 0,
  startWidth: 0,
  startHeight: 0,
  startModelX: 0,
  startModelY: 0,
  startInputX: 0,
  startInputY: 0,
});

const modelDrag = reactive({
  active: false,
  moved: false,
  pressing: false,
  longPressTriggered: false,
  mode: 'stage',
  pointerId: null,
  startClientX: 0,
  startClientY: 0,
  startStageLeft: 0,
  startStageTop: 0,
  startInputX: 0,
  startInputY: 0,
  startX: 0,
  startY: 0,
  offsetX: 0,
  offsetY: 0,
});

const ui = reactive(createDefaultUi(effectiveUiMode.value));

let pixiApp = null;
let modelInstance = null;
let runtimePromise = null;
let inputHideTimer = null;
let introHideTimer = null;
let introShowTimer = null;
let modelLongPressTimer = null;
let persistTimer = null;
let initRenderTimer = null;
let windowMoveHandler = null;
let windowUpHandler = null;
let modelPointerMoveHandler = null;
let modelPointerUpHandler = null;
let modelMouseMoveHandler = null;
let modelMouseUpHandler = null;
let didResetAfterRenderFail = false;

const config = computed(() => {
  const next = store.live2dConfigById(resolvedScenicId.value) || {};
  const widgetName = normalizeAssistantName(next.widgetName);
  return {
    enabled: next.enabled !== false,
    widgetName,
    greeting: normalizeAssistantGreeting(next.greeting, widgetName),
    modelUrl: next.modelUrl || '/assets/live2d/model/live2d/main.model3.json',
    width: next.width || 240,
    height: next.height || 320,
    scale: next.scale ?? 0.18,
  };
});

const inputPlaceholder = computed(() => {
  if (sending.value) return '正在发送...';
  return '可以问我路线、景点推荐或玩法安排';
});

const introMessage = computed(() => {
  const assistantName = normalizeAssistantName(config.value.widgetName);
  return normalizeDisplayText(
    `我是${assistantName}，我可以给你介绍景区内容、推荐路线和玩法安排。点击“对话”就能开始。`,
    DEFAULT_CLICK_INTRO,
  );
});

const stageStyle = computed(() => {
  if (isExplorePage.value) {
    const metrics = getExplorePanelMetrics();
    return {
      left: `${metrics.stageLeft}px`,
      top: `${metrics.stageTop}px`,
      width: `${metrics.stageWidth}px`,
      height: `${metrics.stageHeight}px`,
    };
  }
  return {
    left: `${ui.stage.left}px`,
    top: `${ui.stage.top}px`,
    width: `${ui.stage.width}px`,
    height: `${ui.stage.height}px`,
  };
});

const hitAreaStyle = computed(() => {
  const compact = window.innerWidth <= 760;
  const widthRatio = compact ? 0.58 : 0.46;
  const heightRatio = compact ? 0.66 : 0.56;
  const width = clamp(ui.stage.width * widthRatio, 88, compact ? 190 : 168);
  const height = clamp(ui.stage.height * heightRatio, 150, compact ? 320 : 280);
  const left = clamp(
    ui.stage.left + (ui.stage.width - width) / 2,
    8,
    Math.max(8, window.innerWidth - width - 8),
  );
  const top = clamp(
    ui.stage.top + ui.stage.height * (compact ? 0.2 : 0.26),
    8,
    Math.max(8, window.innerHeight - height - 8),
  );
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`,
  };
});

const inputBubbleStyle = computed(() => {
  void viewportTick.value;
  const metrics = getInputBubbleMetrics();
  const left = clamp(ui.chat.input.x, 8, Math.max(8, window.innerWidth - metrics.width - 8));
  const top = clamp(ui.chat.input.y, 8, Math.max(8, window.innerHeight - metrics.maxHeight - 8));
  return {
    left: `${left}px`,
    top: `${top}px`,
    maxHeight: `${metrics.maxHeight}px`,
  };
});

const reopenStyle = computed(() => {
  void viewportTick.value;
  if (isExplorePage.value) {
    const metrics = getExplorePanelMetrics();
    const topOffset = window.innerWidth <= 760 ? 78 : 64;
    return {
      left: `${metrics.left + 16}px`,
      top: `${clamp(metrics.top + topOffset, 12, window.innerHeight - 52)}px`,
    };
  }
  const left = clamp(ui.stage.left + Math.max(0, ui.stage.width - 132), 12, window.innerWidth - 148);
  const top = clamp(ui.stage.top + ui.stage.height - 44, 12, window.innerHeight - 52);
  return {
    left: `${left}px`,
    top: `${top}px`,
  };
});

const dockStyle = computed(() => {
  void viewportTick.value;
  const compact = window.innerWidth <= 760;
  const dockWidth = compact ? 286 : 106;
  const dockHeight = compact ? 46 : 134;
  const preferLeft = ui.stage.left >= 70;
  const left = preferLeft
    ? clamp(ui.stage.left - 54, 8, window.innerWidth - dockWidth - 8)
    : clamp(ui.stage.left + ui.stage.width + 12, 8, window.innerWidth - dockWidth - 8);
  const top = clamp(ui.stage.top + 24, 8, window.innerHeight - dockHeight - 8);
  return {
    left: `${left}px`,
    top: `${top}px`,
  };
});

const introTipStyle = computed(() => {
  void viewportTick.value;
  if (isExplorePage.value) {
    const metrics = getExplorePanelMetrics();
    const width = Math.max(236, Math.min(300, window.innerWidth - 16));
    return {
      left: `${clamp(metrics.left + metrics.width + 12, 8, Math.max(8, window.innerWidth - width - 8))}px`,
      top: `${clamp(metrics.top + 68, 8, Math.max(8, window.innerHeight - 152))}px`,
      width: `${width}px`,
    };
  }
  const width = Math.max(236, Math.min(320, window.innerWidth - 16));
  const rightCandidate = ui.stage.left + ui.stage.width + 12;
  const leftCandidate = ui.stage.left - width - 12;
  const canPlaceRight = rightCandidate + width <= window.innerWidth - 8;
  const canPlaceLeft = leftCandidate >= 8;
  const left = canPlaceRight
    ? rightCandidate
    : canPlaceLeft
      ? leftCandidate
      : clamp(
        ui.stage.left + ui.stage.width * 0.5 - width * 0.5,
        8,
        Math.max(8, window.innerWidth - width - 8),
      );
  const top = clamp(ui.stage.top + 16, 8, Math.max(8, window.innerHeight - 152));
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
  };
});

const drawerStyle = computed(() => {
  void viewportTick.value;
  const rightCandidate = ui.stage.left + ui.stage.width + 16;
  const leftCandidate = ui.stage.left - DRAWER_WIDTH - 16;
  const canPlaceRight = rightCandidate + DRAWER_WIDTH <= window.innerWidth - 12;
  const canPlaceLeft = leftCandidate >= 12;
  const left = canPlaceRight
    ? rightCandidate
    : canPlaceLeft
      ? leftCandidate
      : clamp(window.innerWidth - DRAWER_WIDTH - 12, 12, window.innerWidth - DRAWER_WIDTH - 12);
  const top = clamp(ui.stage.top + 12, 12, window.innerHeight - 420);
  return {
    left: `${left}px`,
    top: `${top}px`,
  };
});

const currentRoundMessages = computed(() => messages.value.filter((item) => item.roundId === currentRoundId.value));

const historyRoundList = computed(() => {
  const roundMap = new Map();
  messages.value.forEach((item) => {
    const roundId = toRoundId(item.roundId);
    if (!roundMap.has(roundId)) {
      roundMap.set(roundId, {
        id: roundId,
        preview: '',
        timestamp: item.timestamp || Date.now(),
      });
    }
    const round = roundMap.get(roundId);
    if (!round.preview && item.role === 'user') {
      round.preview = String(item.content || '').slice(0, 26) || '该轮暂无提问';
    }
    round.timestamp = item.timestamp || round.timestamp;
  });

  if (!roundMap.has(currentRoundId.value)) {
    roundMap.set(currentRoundId.value, {
      id: currentRoundId.value,
      preview: '该轮暂无提问',
      timestamp: Date.now(),
    });
  }

  return Array.from(roundMap.values())
    .sort((a, b) => a.id - b.id)
    .map((item) => ({
      ...item,
      preview: item.preview || '该轮暂无提问',
    }));
});

const selectedRoundMessages = computed(() => messages.value
  .filter((item) => item.roundId === selectedHistoryRoundId.value));

watch(
  ui,
  () => {
    if (!uiReady.value) return;
    schedulePersistUi();
  },
  { deep: true },
);

watch(
  () => [ui.model.x, ui.model.y, ui.model.scale],
  () => {
    applyModelPlacement();
  },
);

watch(
  () => ui.expressions,
  () => {
    applyExpressions();
  },
  { deep: true },
);

watch(
  () => [ui.stage.width, ui.stage.height],
  () => {
    resizeStage();
  },
);

watch(
  () => config.value.modelUrl,
  async () => {
    if (!uiReady.value) return;
    scheduleStageRender(80);
  },
);

watch(
  () => ui.collapsed,
  () => {
    syncTickerState();
  },
);

watch(
  pageHidden,
  () => {
    syncTickerState();
  },
);

watch(
  () => props.page,
  async (nextPage, previousPage) => {
    if (!uiReady.value) return;
    if (previousPage) persistUiState();
    clearInputHideTimer();
    cleanupGlobalDrag();
    resetModelPressState(false);
    dismissIntroTip();
    introTriggered.value = false;
    activePanel.value = '';
    applyUiState(loadUiState(effectiveUiMode.value));
    inputVisible.value = String(nextPage || '').trim().toLowerCase() === 'explore';
    setAdjustmentMode('off');
    clampUiWithinViewport();
    viewportTick.value += 1;
    await nextTick();
    scheduleStageRender(getPerformanceProfile(effectiveUiMode.value).initDelay);
    triggerIntroOnExploreEntry();
  },
);

watch(
  () => currentRoundMessages.value.length,
  async () => {
    if (!inputVisible.value) return;
    await nextTick();
    scrollChatThreadToBottom();
  },
);

watch(
  historyRoundList,
  (next) => {
    if (!next.length) {
      selectedHistoryRoundId.value = currentRoundId.value;
      return;
    }
    const exists = next.some((item) => item.id === selectedHistoryRoundId.value);
    if (!exists) {
      selectedHistoryRoundId.value = next[next.length - 1].id;
    }
  },
  { deep: true },
);

function cloneExpressions() {
  return Object.fromEntries(
    Object.entries(DEFAULT_EXPRESSIONS).map(([id, value]) => [id, { ...value }]),
  );
}

function safeParse(raw) {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (error) {
    return null;
  }
}

function toRoundId(value) {
  const roundId = Number.parseInt(String(value || ''), 10);
  return Number.isFinite(roundId) && roundId > 0 ? roundId : 1;
}

function resolveCurrentRoundId(list) {
  if (!Array.isArray(list) || !list.length) return 1;
  return list.reduce((maxId, item) => Math.max(maxId, toRoundId(item?.roundId)), 1);
}

function normalizeHistoryItems(list) {
  if (!Array.isArray(list)) return [];
  let fallbackRoundId = 1;
  return list
    .filter((item) => item && typeof item.content === 'string')
    .map((item) => {
      const normalizedRoundId = toRoundId(item.roundId || fallbackRoundId);
      fallbackRoundId = normalizedRoundId;
      return {
        ...item,
        roundId: normalizedRoundId,
        relatedSpots: Array.isArray(item.relatedSpots) ? item.relatedSpots : [],
        routePlan: item.routePlan || null,
        timestamp: Number.isFinite(Number(item.timestamp)) ? Number(item.timestamp) : Date.now(),
      };
    });
}

function selectHistoryRound(roundId) {
  selectedHistoryRoundId.value = toRoundId(roundId);
}

function scrollChatThreadToBottom() {
  if (!chatThreadRef.value) return;
  chatThreadRef.value.scrollTop = chatThreadRef.value.scrollHeight;
}

function hasChinese(text) {
  return /[\u3400-\u9fff]/.test(String(text || ''));
}

function looksLikeMojibake(text) {
  return /[ÃÂÄÅÇÐÑØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ�]/.test(String(text || ''));
}

function repairUtf8Mojibake(text) {
  const source = String(text || '');
  if (!source) return '';
  try {
    return new TextDecoder('utf-8').decode(Uint8Array.from(source, (char) => char.charCodeAt(0) & 0xff));
  } catch (error) {
    return source;
  }
}

function normalizeDisplayText(text, fallback = '') {
  const value = String(text || '').trim();
  if (!value) return fallback;
  if (hasChinese(value)) return value;

  const repaired = repairUtf8Mojibake(value).trim();
  if (hasChinese(repaired)) return repaired;
  if (looksLikeMojibake(value) || value.length <= 2) return fallback || value;
  return value;
}

function normalizeAssistantName(rawName) {
  const normalized = normalizeDisplayText(rawName, DEFAULT_WIDGET_NAME).trim();
  if (!normalized) return DEFAULT_WIDGET_NAME;
  if (/^小\s*[Yy]$/.test(normalized)) return DEFAULT_WIDGET_NAME;
  return normalized;
}

function normalizeAssistantGreeting(rawGreeting, assistantName = DEFAULT_WIDGET_NAME) {
  const normalized = normalizeDisplayText(rawGreeting, DEFAULT_GREETING);
  return String(normalized || DEFAULT_GREETING).replace(/小\s*[Yy]/g, assistantName);
}

function getOriginalPlacement(width, height, scale = ORIGINAL_MODEL_SCALE) {
  return {
    x: Math.round((ORIGINAL_MODEL_X / ORIGINAL_STAGE_WIDTH) * width),
    y: Math.round((ORIGINAL_MODEL_Y / ORIGINAL_STAGE_HEIGHT) * height),
    scale,
  };
}

function getModelBounds(width, height) {
  return {
    minX: -width * 0.65,
    maxX: width * 0.7,
    minY: -height * 0.12,
    maxY: height * 0.92,
  };
}

function normalizeModelViewportPlacement(forceOriginal = false) {
  const original = getOriginalPlacement(ui.stage.width, ui.stage.height, ui.model.scale);
  const safeMinX = -ui.stage.width * 0.58;
  const safeMaxX = ui.stage.width * 0.38;
  const safeMinY = -ui.stage.height * 0.12;
  const safeMaxY = ui.stage.height * 0.72;
  const invalid = !Number.isFinite(ui.model.x)
    || !Number.isFinite(ui.model.y)
    || ui.model.x < safeMinX
    || ui.model.x > safeMaxX
    || ui.model.y < safeMinY
    || ui.model.y > safeMaxY;

  if (forceOriginal || invalid) {
    ui.model.x = clamp(original.x, safeMinX, safeMaxX);
    ui.model.y = clamp(original.y, safeMinY, safeMaxY);
  }
}

function getPerformanceProfile(page = effectiveUiMode.value) {
  if (page === 'explore') {
    return {
      resolutionCap: 1,
      antialias: false,
      initDelay: 620,
    };
  }

  return {
    resolutionCap: 1.25,
    antialias: true,
    initDelay: 260,
  };
}

function clamp(value, min, max) {
  if (Number.isNaN(Number(value))) return min;
  if (max < min) return min;
  return Math.min(Math.max(Number(value), min), max);
}

function getInputBubbleMetrics() {
  return {
    width: Math.max(220, Math.min(INPUT_BUBBLE_MAX_WIDTH, window.innerWidth - 16)),
    maxHeight: Math.max(220, Math.min(INPUT_BUBBLE_MAX_HEIGHT, Math.floor(window.innerHeight * 0.72))),
  };
}

function getExplorePanelMetrics() {
  const viewportWidth = window.innerWidth || 1440;
  const viewportHeight = window.innerHeight || 900;
  const compact = viewportWidth <= 920;
  const left = compact ? 8 : 14;
  const top = compact ? 8 : 14;
  const bottomInset = compact ? 76 : 14;
  const width = compact
    ? Math.max(248, Math.min(320, viewportWidth - 16))
    : 320;
  const stageHeight = compact ? 176 : 194;
  const stagePadding = compact ? 10 : 12;
  const stageWidth = Math.max(220, width - stagePadding * 2);
  const stageLeft = left + Math.max(0, Math.round((width - stageWidth) / 2));
  const stageTop = top + stagePadding;
  const inputMetrics = getInputBubbleMetrics();
  const inputX = left + 10;
  const inputY = clamp(
    top + stageHeight + 220,
    8,
    Math.max(8, viewportHeight - inputMetrics.maxHeight - bottomInset - 8),
  );
  return {
    left,
    top,
    width,
    bottomInset,
    stageWidth,
    stageHeight,
    stageLeft,
    stageTop,
    inputX,
    inputY,
  };
}

function getBubbleX(stageLeft, stageWidth, bubbleWidth, offset = 24) {
  const preferLeft = stageLeft >= bubbleWidth * 0.72;
  if (preferLeft) {
    return clamp(stageLeft - bubbleWidth + offset, 8, window.innerWidth - bubbleWidth - 8);
  }
  return clamp(stageLeft + stageWidth - offset, 8, window.innerWidth - bubbleWidth - 8);
}

function clampInputBubblePosition(x = ui.chat.input.x, y = ui.chat.input.y) {
  const metrics = getInputBubbleMetrics();
  ui.chat.input.x = clamp(x, 8, Math.max(8, window.innerWidth - metrics.width - 8));
  ui.chat.input.y = clamp(y, 8, Math.max(8, window.innerHeight - metrics.maxHeight - 8));
}

function placeInputBubbleNearStage() {
  const metrics = getInputBubbleMetrics();
  const rightCandidate = ui.stage.left + ui.stage.width + 12;
  const leftCandidate = ui.stage.left - metrics.width - 12;
  const canPlaceRight = rightCandidate + metrics.width <= window.innerWidth - 8;
  const canPlaceLeft = leftCandidate >= 8;
  const nextX = canPlaceRight
    ? rightCandidate
    : canPlaceLeft
      ? leftCandidate
      : ui.chat.input.x;
  const nextY = clamp(ui.stage.top + 16, 8, Math.max(8, window.innerHeight - metrics.maxHeight - 8));
  clampInputBubblePosition(nextX, nextY);
}

function getStageSizeLimits(page = effectiveUiMode.value) {
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  if (pageKey === 'explore') {
    const metrics = getExplorePanelMetrics();
    return {
      minWidth: metrics.stageWidth,
      maxWidth: metrics.stageWidth,
      minHeight: metrics.stageHeight,
      maxHeight: metrics.stageHeight,
    };
  }

  const viewportWidth = window.innerWidth || 1440;
  const viewportHeight = window.innerHeight || 900;
  const compact = viewportWidth < 960 || viewportHeight < 780;

  return {
    minWidth: compact ? 170 : 185,
    maxWidth: compact ? 240 : 270,
    minHeight: compact ? 250 : 280,
    maxHeight: compact ? 380 : 430,
  };
}

function getStageBottomOverflow(page = effectiveUiMode.value, stageHeight = ui?.stage?.height || 0) {
  void stageHeight;
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  if (pageKey !== 'explore') return 0;
  return 0;
}

function getStageTopOverflow(page = effectiveUiMode.value, stageHeight = ui?.stage?.height || 0) {
  void stageHeight;
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  if (pageKey !== 'explore') return 0;
  return 0;
}

function getStageDragBounds(page = effectiveUiMode.value) {
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  if (pageKey === 'explore') {
    const metrics = getExplorePanelMetrics();
    return {
      minLeft: metrics.stageLeft,
      maxLeft: metrics.stageLeft,
      minTop: metrics.stageTop,
      maxTop: metrics.stageTop,
    };
  }

  const topOverflow = getStageTopOverflow(page, ui.stage.height);
  const bottomOverflow = getStageBottomOverflow(page, ui.stage.height);
  const minLeft = 8;
  const maxLeft = Math.max(minLeft, window.innerWidth - ui.stage.width - 8);
  const minTop = 8 - topOverflow;
  const maxTop = Math.max(minTop, window.innerHeight - ui.stage.height - 8 + bottomOverflow);
  return {
    minLeft,
    maxLeft,
    minTop,
    maxTop,
  };
}

function getBasePreset(page = effectiveUiMode.value) {
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  if (pageKey === 'explore') {
    const metrics = getExplorePanelMetrics();
    return {
      width: metrics.stageWidth,
      height: metrics.stageHeight,
      gap: 16,
      scale: 0.04,
    };
  }

  const viewportWidth = window.innerWidth || 1440;
  const viewportHeight = window.innerHeight || 900;
  const compact = viewportWidth < 960 || viewportHeight < 780;

  return {
    width: compact ? 190 : 220,
    height: compact ? 300 : 350,
    gap: 16,
    scale: compact ? 0.18 : 0.2,
  };
}

function createDefaultUi(page = effectiveUiMode.value) {
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  const preset = getBasePreset(page);
  if (pageKey === 'explore') {
    const metrics = getExplorePanelMetrics();
    const placement = getOriginalPlacement(metrics.stageWidth, metrics.stageHeight, preset.scale);
    return {
      collapsed: false,
      stage: {
        left: metrics.stageLeft,
        top: metrics.stageTop,
        width: metrics.stageWidth,
        height: metrics.stageHeight,
        isAdjusting: false,
      },
      model: {
        x: placement.x,
        y: placement.y,
        scale: placement.scale,
        isAdjusting: false,
      },
      chat: {
        input: {
          x: metrics.inputX,
          y: metrics.inputY,
        },
        isAdjusting: false,
      },
      expressions: cloneExpressions(),
    };
  }

  const inputMetrics = getInputBubbleMetrics();
  const left = Math.max(8, window.innerWidth - preset.width - preset.gap);
  const top = Math.max(8, window.innerHeight - preset.height - preset.gap);
  const placement = getOriginalPlacement(preset.width, preset.height, preset.scale);

  return {
    collapsed: false,
    stage: {
      left,
      top,
      width: preset.width,
      height: preset.height,
      isAdjusting: false,
    },
    model: {
      x: placement.x,
      y: placement.y,
      scale: placement.scale,
      isAdjusting: false,
    },
    chat: {
      input: {
        x: getBubbleX(left, preset.width, inputMetrics.width, 34),
        y: clamp(top + 28, 8, Math.max(8, window.innerHeight - inputMetrics.maxHeight - 8)),
      },
      isAdjusting: false,
    },
    expressions: cloneExpressions(),
  };
}

function mergeUi(defaults, saved) {
  return {
    collapsed: saved?.collapsed ?? defaults.collapsed,
    stage: {
      ...defaults.stage,
      ...(saved?.stage || {}),
      isAdjusting: false,
    },
    model: {
      ...defaults.model,
      ...(saved?.model || {}),
      isAdjusting: false,
    },
    chat: {
      ...defaults.chat,
      ...(saved?.chat || {}),
      input: {
        ...defaults.chat.input,
        ...(saved?.chat?.input || {}),
      },
      isAdjusting: false,
    },
    expressions: {
      ...defaults.expressions,
      ...Object.fromEntries(
        Object.entries(saved?.expressions || {}).map(([id, value]) => [id, { ...defaults.expressions[id], ...value }]),
      ),
    },
  };
}

function shouldMigrateLegacyExplorePlacement(saved, page = effectiveUiMode.value) {
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  if (pageKey !== 'explore') return false;
  if (!saved?.stage) return false;
  const stageLeft = Number(saved.stage.left);
  const stageTop = Number(saved.stage.top);
  const stageWidth = Number(saved.stage.width);
  const stageHeight = Number(saved.stage.height);
  if (!Number.isFinite(stageLeft) || !Number.isFinite(stageTop)) return false;
  if (!Number.isFinite(stageWidth) || !Number.isFinite(stageHeight)) return false;

  const preset = getBasePreset(page);
  const legacyDefaultLeft = Math.max(8, window.innerWidth - stageWidth - preset.gap);
  const legacyDefaultTop = Math.max(8, window.innerHeight - stageHeight - preset.gap);
  return Math.abs(stageLeft - legacyDefaultLeft) <= 12 && Math.abs(stageTop - legacyDefaultTop) <= 12;
}

function getLegacyUiStorageKey(page = effectiveUiMode.value) {
  return `${LEGACY_LIVE2D_UI_KEY_PREFIX}_${page}`;
}

function getLegacyScopedUiStorageKey(page = effectiveUiMode.value) {
  return buildScenicStorageKey(resolvedScenicId.value, `live2d-ui:${page}`, 'v5');
}

function getUiStorageKey() {
  return buildScenicStorageKey(resolvedScenicId.value, 'live2d-ui', 'v6');
}

function getExploreLeftMigrationKey() {
  return buildScenicStorageKey(resolvedScenicId.value, EXPLORE_LEFT_MIGRATION_KEY_SCOPE, 'v1');
}

function hasExploreLeftPresetMigration() {
  return localStorage.getItem(getExploreLeftMigrationKey()) === '1';
}

function markExploreLeftPresetMigration() {
  localStorage.setItem(getExploreLeftMigrationKey(), '1');
}

function loadUiState(page = effectiveUiMode.value) {
  const defaults = createDefaultUi(page);
  const pageKey = String(props.page || page || '').trim().toLowerCase();
  let saved = safeParse(localStorage.getItem(getUiStorageKey()));
  if (!saved) {
    saved = safeParse(localStorage.getItem(getLegacyScopedUiStorageKey(page)));
  }
  if (!saved && resolvedScenicId.value === DEFAULT_SCENIC_ID) {
    saved = safeParse(localStorage.getItem(getLegacyUiStorageKey(page)));
  }
  const merged = mergeUi(defaults, saved);
  if (pageKey === 'explore') {
    // Ignore stale persisted model placement on explore page to keep deterministic layout.
    merged.model.scale = defaults.model.scale;
    merged.model.x = defaults.model.x;
    merged.model.y = defaults.model.y;
  }
  const shouldForceExploreLeft = pageKey === 'explore' && !hasExploreLeftPresetMigration();
  if (shouldForceExploreLeft || shouldMigrateLegacyExplorePlacement(saved, page)) {
    merged.stage.left = defaults.stage.left;
    merged.chat.input.x = getBubbleX(defaults.stage.left, merged.stage.width, getInputBubbleMetrics().width, 34);
  }
  if (shouldForceExploreLeft) {
    markExploreLeftPresetMigration();
  }
  return merged;
}

function applyUiState(next) {
  ui.collapsed = !!next.collapsed;
  Object.assign(ui.stage, next.stage);
  Object.assign(ui.model, next.model);
  ui.chat.isAdjusting = !!next.chat.isAdjusting;
  Object.assign(ui.chat.input, next.chat.input);

  Object.keys(ui.expressions).forEach((key) => {
    delete ui.expressions[key];
  });
  Object.entries(next.expressions || {}).forEach(([id, value]) => {
    ui.expressions[id] = { ...value };
  });

  clampUiWithinViewport();
  normalizeModelViewportPlacement();
}

function persistUiState() {
  localStorage.setItem(
    getUiStorageKey(),
    JSON.stringify({
      collapsed: ui.collapsed,
      stage: { ...ui.stage },
      model: { ...ui.model },
      chat: {
        input: { ...ui.chat.input },
        isAdjusting: ui.chat.isAdjusting,
      },
      expressions: Object.fromEntries(
        Object.entries(ui.expressions).map(([id, value]) => [id, { ...value }]),
      ),
    }),
  );
}

function schedulePersistUi() {
  window.clearTimeout(persistTimer);
  persistTimer = window.setTimeout(() => {
    persistUiState();
  }, 120);
}

function getHistoryStorageKey() {
  return buildScenicStorageKey(resolvedScenicId.value, 'live2d-history', 'v2');
}

function getAssistantRouteStorageKey() {
  return buildScenicStorageKey(resolvedScenicId.value, ASSISTANT_ROUTE_STORAGE_SCOPE, 'v1');
}

function loadHistory() {
  let raw = safeParse(localStorage.getItem(getHistoryStorageKey()));
  if (!raw && resolvedScenicId.value === DEFAULT_SCENIC_ID) {
    raw = safeParse(localStorage.getItem(LEGACY_LIVE2D_HISTORY_KEY));
  }
  return normalizeHistoryItems(raw);
}

function persistHistory() {
  localStorage.setItem(getHistoryStorageKey(), JSON.stringify(messages.value));
}

function limitHistoryLength(list, maxLength = 4096) {
  const copied = [...list];
  const totalLength = () => copied.reduce((sum, item) => sum + String(item.content || '').length, 0);
  while (copied.length && totalLength() > maxLength) {
    copied.shift();
  }
  return copied;
}

function normalizeRoutePlan(raw) {
  if (!raw || !Array.isArray(raw.stops)) return null;
  const stops = raw.stops
    .map((item) => ({
      id: item?.id || '',
      name: item?.name || '站点',
      kind: item?.kind || 'spot',
      lng: Number(item?.lng),
      lat: Number(item?.lat),
    }))
    .filter((item) => item.id && Number.isFinite(item.lng) && Number.isFinite(item.lat));

  if (!stops.length) return null;
  return {
    title: String(raw.title || '助手推荐路线'),
    description: String(raw.description || '已根据你的对话内容生成推荐路线。'),
    reasons: Array.isArray(raw.reasons) ? raw.reasons.filter((item) => typeof item === 'string') : [],
    tags: Array.isArray(raw.tags) ? raw.tags.filter((item) => typeof item === 'string') : [],
    stops,
  };
}

function appendMessage(role, content, relatedSpots = [], routePlan = null, roundId = currentRoundId.value) {
  messages.value = limitHistoryLength([
    ...messages.value,
    {
      id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
      role,
      content,
      relatedSpots,
      routePlan,
      roundId: toRoundId(roundId),
      timestamp: Date.now(),
    },
  ]);
  persistHistory();
}

function clearHistory() {
  messages.value = [];
  currentRoundId.value = 1;
  selectedHistoryRoundId.value = 1;
  persistHistory();
}

function roleLabel(role) {
  if (role === 'assistant') return config.value.widgetName;
  if (role === 'user') return '我';
  return '系统';
}

function formatTime(timestamp) {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

function clearInputHideTimer() {
  if (inputHideTimer) {
    window.clearTimeout(inputHideTimer);
    inputHideTimer = null;
  }
}

function clearModelLongPressTimer() {
  if (modelLongPressTimer) {
    window.clearTimeout(modelLongPressTimer);
    modelLongPressTimer = null;
  }
}

function clearIntroHideTimer() {
  if (introHideTimer) {
    window.clearTimeout(introHideTimer);
    introHideTimer = null;
  }
}

function clearIntroShowTimer() {
  if (introShowTimer) {
    window.clearTimeout(introShowTimer);
    introShowTimer = null;
  }
}

function dismissIntroTip() {
  clearIntroShowTimer();
  clearIntroHideTimer();
  introVisible.value = false;
}

function showIntroTip() {
  if (ui.collapsed) return;
  introVisible.value = true;
  clearIntroHideTimer();
  introHideTimer = window.setTimeout(() => {
    introVisible.value = false;
  }, 7200);
}

function triggerIntroOnExploreEntry() {
  if (introTriggered.value || String(props.page || '').trim().toLowerCase() !== 'explore') return;
  introTriggered.value = true;
  clearIntroShowTimer();
  introShowTimer = window.setTimeout(() => {
    if (ui.collapsed) {
      ui.collapsed = false;
    }
    showIntroTip();
  }, 360);
}

function hideInputBubble() {
  clearInputHideTimer();
  if (isExplorePage.value && !ui.collapsed) {
    inputVisible.value = true;
    return;
  }
  inputVisible.value = false;
}

function resizeTextarea() {
  if (!inputRef.value) return;
  inputRef.value.style.height = 'auto';
  inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 200)}px`;
}

function openInputBubble() {
  clearInputHideTimer();
  dismissIntroTip();
  if (!isExplorePage.value) {
    placeInputBubbleNearStage();
  }
  inputVisible.value = true;
  selectedHistoryRoundId.value = currentRoundId.value;
  nextTick(() => {
    resizeTextarea();
    scrollChatThreadToBottom();
    if (!ui.chat.isAdjusting && inputRef.value) {
      inputRef.value.focus();
    }
  });
}

function toggleChatInput() {
  if (ui.chat.isAdjusting) return;
  openInputBubble();
}

function closeInputBubble() {
  if (sending.value) return;
  if (isExplorePage.value) {
    collapseAssistant();
    return;
  }
  hideInputBubble();
}

function startNewRound() {
  if (sending.value) return;
  currentRoundId.value = resolveCurrentRoundId(messages.value) + 1;
  selectedHistoryRoundId.value = currentRoundId.value;
  inputValue.value = '';
  openInputBubble();
}

function sendQuickPrompt(text) {
  if (sending.value || ui.chat.isAdjusting) return;
  inputValue.value = String(text || '');
  sendMessage();
}

function handleTextareaInput() {
  resizeTextarea();
}

function handleTextareaKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    sendMessage();
  }
}

async function sendMessage() {
  const text = inputValue.value.trim();
  if (!text || sending.value || ui.chat.isAdjusting) return;

  appendMessage('user', text);
  inputValue.value = '';
  resizeTextarea();
  sending.value = true;
  const roundHistory = currentRoundMessages.value
    .map((item) => ({ role: item.role, content: item.content }))
    .slice(-12);

  try {
    const data = await request(API.live2dChat(resolvedScenicId.value), {
      method: 'POST',
      data: {
        message: text,
        scenicId: resolvedScenicId.value,
        page: props.page,
        history: roundHistory,
      },
      headers: {
        'Content-Type': 'application/json',
      },
    });

    const replyText = normalizeDisplayText(data.reply, DEFAULT_REPLY);
    const routePlan = normalizeRoutePlan(data.routePlan);
    appendMessage('assistant', replyText, data.relatedSpots || [], routePlan);
    if (!ui.chat.isAdjusting && inputRef.value) {
      inputRef.value.focus();
    }
    await nextTick();
    scrollChatThreadToBottom();
  } catch (error) {
    const reason = String(error?.message || '').trim();
    const fallback = reason
      ? `连接助手失败：${reason}`
      : '连接助手失败，请稍后重试。';
    appendMessage('assistant', fallback, []);
  } finally {
    sending.value = false;
    nextTick(() => {
      resizeTextarea();
    });
  }
}

function goToSpot(spotId) {
  activePanel.value = '';
  router.push({
    path: buildScenicPagePath(resolvedScenicId.value, 'explore'),
    query: { spot: spotId },
  });
}

function persistAssistantRoutePlan(plan) {
  localStorage.setItem(
    getAssistantRouteStorageKey(),
    JSON.stringify({
      scenicId: resolvedScenicId.value,
      createdAt: Date.now(),
      plan,
    }),
  );
}

function dispatchAssistantRoutePlan(plan) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent(ASSISTANT_ROUTE_EVENT, {
    detail: {
      scenicId: resolvedScenicId.value,
      plan,
      source: 'live2d',
    },
  }));
}

function applyAssistantRoute(routePlan) {
  const plan = normalizeRoutePlan(routePlan);
  if (!plan) return;

  persistAssistantRoutePlan(plan);
  dispatchAssistantRoutePlan(plan);
  activePanel.value = '';

  if (props.page !== 'explore') {
    router.push({
      path: buildScenicPagePath(resolvedScenicId.value, 'explore'),
      query: { assistantRouteApply: String(Date.now()) },
    });
  }
}

function togglePanel(panel) {
  if (panel === 'adjust') return;
  activePanel.value = activePanel.value === panel ? '' : panel;
}

function setAdjustmentMode(mode) {
  if (mode === 'model') {
    ui.model.isAdjusting = !ui.model.isAdjusting;
    if (ui.model.isAdjusting) {
      ui.stage.isAdjusting = false;
      resetModelPressState(false);
      hideInputBubble();
    }
    return;
  }

  if (mode === 'stage') {
    ui.stage.isAdjusting = !ui.stage.isAdjusting;
    if (ui.stage.isAdjusting) {
      ui.model.isAdjusting = false;
      resetModelPressState(false);
      hideInputBubble();
    }
    return;
  }

  ui.stage.isAdjusting = false;
  ui.model.isAdjusting = false;
  ui.chat.isAdjusting = false;
  resetModelPressState(false);
}

function resetLayout() {
  applyUiState(createDefaultUi(effectiveUiMode.value));
  setAdjustmentMode('off');
  nextTick(() => {
    resizeStage();
  });
}

function collapseAssistant() {
  activePanel.value = '';
  resetModelPressState(false);
  ui.collapsed = true;
  ui.stage.isAdjusting = false;
  ui.model.isAdjusting = false;
  ui.chat.isAdjusting = false;
  dismissIntroTip();
  hideInputBubble();
  schedulePersistUi();
  syncTickerState();
}

async function reopenAssistant() {
  ui.collapsed = false;
  if (isExplorePage.value) {
    inputVisible.value = true;
  }
  clampUiWithinViewport();
  normalizeModelViewportPlacement(true);
  viewportTick.value += 1;
  await nextTick();
  resizeStage();
  schedulePersistUi();
  scheduleStageRender(40);
  if (modelReady.value) {
    syncTickerState();
  }
}

function cleanupGlobalDrag() {
  clearModelLongPressTimer();
  if (windowMoveHandler) {
    window.removeEventListener('mousemove', windowMoveHandler);
  }
  if (windowUpHandler) {
    window.removeEventListener('mouseup', windowUpHandler);
  }
  windowMoveHandler = null;
  windowUpHandler = null;
  dragState.kind = null;
}

function syncTickerState() {
  if (!pixiApp?.ticker) return;
  const shouldRun = modelReady.value && !ui.collapsed && !pageHidden.value;
  if (shouldRun) {
    pixiApp.ticker.start();
  } else {
    pixiApp.ticker.stop();
  }
}

function scheduleStageRender(delay = getPerformanceProfile().initDelay) {
  window.clearTimeout(initRenderTimer);
  initRenderTimer = window.setTimeout(() => {
    ensureStageRendered();
  }, delay);
}

function handleVisibilityChange() {
  pageHidden.value = document.hidden;
  syncTickerState();
}

function clampUiWithinViewport() {
  if (isExplorePage.value) {
    const metrics = getExplorePanelMetrics();
    const preset = getBasePreset(effectiveUiMode.value);
    ui.stage.width = metrics.stageWidth;
    ui.stage.height = metrics.stageHeight;
    ui.stage.left = metrics.stageLeft;
    ui.stage.top = metrics.stageTop;
    ui.stage.isAdjusting = false;
    ui.model.isAdjusting = false;

    const placement = getOriginalPlacement(metrics.stageWidth, metrics.stageHeight, preset.scale);
    ui.model.scale = preset.scale;
    ui.model.x = placement.x;
    ui.model.y = placement.y;
    clampInputBubblePosition(metrics.inputX, metrics.inputY);
    if (modelInstance) {
      alignModelForExploreStage();
      scheduleExploreAutoCenter(3, 60);
    }
    return;
  }

  const limits = getStageSizeLimits(effectiveUiMode.value);
  const maxWidth = Math.max(limits.minWidth, window.innerWidth - 24);
  const maxHeight = Math.max(limits.minHeight, window.innerHeight - 24);

  ui.stage.width = clamp(ui.stage.width, limits.minWidth, Math.min(limits.maxWidth, maxWidth));
  ui.stage.height = clamp(ui.stage.height, limits.minHeight, Math.min(limits.maxHeight, maxHeight));
  const stageBounds = getStageDragBounds(effectiveUiMode.value);
  ui.stage.left = clamp(ui.stage.left, stageBounds.minLeft, stageBounds.maxLeft);
  ui.stage.top = clamp(ui.stage.top, stageBounds.minTop, stageBounds.maxTop);

  const bounds = getModelBounds(ui.stage.width, ui.stage.height);
  ui.model.x = clamp(ui.model.x, bounds.minX, bounds.maxX);
  ui.model.y = clamp(ui.model.y, bounds.minY, bounds.maxY);

  clampInputBubblePosition();
}

function startStageDrag(event) {
  if (!ui.stage.isAdjusting || event.button !== 0) return;

  cleanupGlobalDrag();
  dragState.kind = 'stage';
  dragState.startX = event.clientX;
  dragState.startY = event.clientY;
  dragState.startLeft = ui.stage.left;
  dragState.startTop = ui.stage.top;
  dragState.startInputX = ui.chat.input.x;
  dragState.startInputY = ui.chat.input.y;

  windowMoveHandler = (moveEvent) => {
    const inputMetrics = getInputBubbleMetrics();
    const dx = moveEvent.clientX - dragState.startX;
    const dy = moveEvent.clientY - dragState.startY;
    const stageBounds = getStageDragBounds(effectiveUiMode.value);
    ui.stage.left = clamp(dragState.startLeft + dx, stageBounds.minLeft, stageBounds.maxLeft);
    ui.stage.top = clamp(dragState.startTop + dy, stageBounds.minTop, stageBounds.maxTop);
    ui.chat.input.x = clamp(dragState.startInputX + dx, 8, Math.max(8, window.innerWidth - inputMetrics.width - 8));
    ui.chat.input.y = clamp(dragState.startInputY + dy, 8, Math.max(8, window.innerHeight - inputMetrics.maxHeight - 8));
  };

  windowUpHandler = () => {
    cleanupGlobalDrag();
    schedulePersistUi();
  };

  window.addEventListener('mousemove', windowMoveHandler);
  window.addEventListener('mouseup', windowUpHandler, { once: true });
}

function startStageResize(event) {
  if (!ui.stage.isAdjusting || event.button !== 0) return;

  cleanupGlobalDrag();
  dragState.kind = 'resize';
  dragState.startX = event.clientX;
  dragState.startY = event.clientY;
  dragState.startWidth = ui.stage.width;
  dragState.startHeight = ui.stage.height;
  dragState.startModelX = ui.model.x;
  dragState.startModelY = ui.model.y;
  dragState.startInputX = ui.chat.input.x;
  dragState.startInputY = ui.chat.input.y;
  dragState.startLeft = ui.stage.left;
  dragState.startTop = ui.stage.top;

  windowMoveHandler = (moveEvent) => {
    const inputMetrics = getInputBubbleMetrics();
    const dx = moveEvent.clientX - dragState.startX;
    const dy = moveEvent.clientY - dragState.startY;
    const limits = getStageSizeLimits(effectiveUiMode.value);
    const maxWidth = Math.max(limits.minWidth, Math.min(limits.maxWidth, window.innerWidth - dragState.startLeft - 8));
    const overflowBottom = getStageBottomOverflow(effectiveUiMode.value, dragState.startHeight);
    const maxHeight = Math.max(
      limits.minHeight,
      Math.min(limits.maxHeight, window.innerHeight - dragState.startTop - 8 + overflowBottom),
    );
    const nextWidth = clamp(dragState.startWidth + dx, limits.minWidth, maxWidth);
    const nextHeight = clamp(dragState.startHeight + dy, limits.minHeight, maxHeight);
    const ratioX = nextWidth / dragState.startWidth;
    const ratioY = nextHeight / dragState.startHeight;

    ui.stage.width = nextWidth;
    ui.stage.height = nextHeight;
    const bounds = getModelBounds(nextWidth, nextHeight);
    ui.model.x = clamp(dragState.startModelX * ratioX, bounds.minX, bounds.maxX);
    ui.model.y = clamp(dragState.startModelY * ratioY, bounds.minY, bounds.maxY);
    ui.chat.input.x = clamp(
      dragState.startLeft + (dragState.startInputX - dragState.startLeft) * ratioX,
      8,
      Math.max(8, window.innerWidth - inputMetrics.width - 8),
    );
    ui.chat.input.y = clamp(
      dragState.startTop + (dragState.startInputY - dragState.startTop) * ratioY,
      8,
      Math.max(8, window.innerHeight - inputMetrics.maxHeight - 8),
    );
  };

  windowUpHandler = () => {
    cleanupGlobalDrag();
    resizeStage();
    schedulePersistUi();
  };

  window.addEventListener('mousemove', windowMoveHandler);
  window.addEventListener('mouseup', windowUpHandler, { once: true });
}

function handleResizeViewport() {
  viewportTick.value += 1;
  clampUiWithinViewport();
  resizeStage();
}

function applyPixiSettings() {
  const PIXI = window.PIXI;
  if (!PIXI?.settings) return;
  PIXI.settings.SCALE_MODE = PIXI.SCALE_MODES.LINEAR;
  if (PIXI.MIPMAP_MODES) {
    PIXI.settings.MIPMAP_TEXTURES = PIXI.MIPMAP_MODES.POW2;
  }
  if (typeof PIXI.settings.ANISOTROPIC_LEVEL === 'number') {
    PIXI.settings.ANISOTROPIC_LEVEL = 16;
  }
  if (PIXI.PRECISION && 'PRECISION_FRAGMENT' in PIXI.settings) {
    PIXI.settings.PRECISION_FRAGMENT = PIXI.PRECISION.HIGH;
  }
  PIXI.settings.ROUND_PIXELS = false;
}

function getResolution() {
  const profile = getPerformanceProfile();
  return Math.min(profile.resolutionCap, Math.max(1, window.devicePixelRatio || 1));
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[data-live2d-src="${src}"]`);
    if (existing) {
      if (existing.dataset.loaded === 'true') {
        resolve();
      } else {
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error(`${src} 加载失败`)), { once: true });
      }
      return;
    }

    const script = document.createElement('script');
    script.src = src;
    script.async = false;
    script.dataset.live2dSrc = src;
    script.onload = () => {
      script.dataset.loaded = 'true';
      resolve();
    };
    script.onerror = () => reject(new Error(`${src} 加载失败`));
    document.head.appendChild(script);
  });
}

async function ensureLive2dRuntime() {
  if (window.PIXI?.live2d?.Live2DModel) return;
  if (!runtimePromise) {
    runtimePromise = (async () => {
      await loadScript('/assets/live2d/live2dcubismcore.min.js');
      await loadScript('/assets/live2d/live2d.min.js');
      await loadScript('/assets/live2d/pixi.min.js');
      await loadScript('/assets/live2d/unsafe-eval.min.js');
      await loadScript('/assets/live2d/index.min.js');
    })().catch((error) => {
      runtimePromise = null;
      throw error;
    });
  }
  await runtimePromise;
}

function applyModelPlacement() {
  if (!modelInstance) return;
  modelInstance.scale.set(ui.model.scale);
  modelInstance.position.set(ui.model.x, ui.model.y);
}

function alignModelForExploreStage() {
  if (!isExplorePage.value || !modelInstance) return false;
  const preset = getBasePreset(effectiveUiMode.value);
  const scale = clamp(preset.scale, 0.02, 0.2);
  const localBounds = getModelLocalBoundsSafe();
  ui.model.scale = scale;

  if (!localBounds) {
    const fallback = getOriginalPlacement(ui.stage.width, ui.stage.height, scale);
    ui.model.x = fallback.x;
    ui.model.y = Math.round(ui.stage.height * 0.42);
    applyModelPlacement();
    return false;
  }

  const targetCenterX = ui.stage.width * 0.43;
  const targetBottomY = ui.stage.height * 1.3;
  ui.model.x = targetCenterX - (localBounds.x + localBounds.width * 0.5) * scale;
  ui.model.y = targetBottomY - (localBounds.y + localBounds.height) * scale;
  applyModelPlacement();
  return true;
}

function scheduleExploreAutoCenter(retries = 8, delay = 90) {
  if (!isExplorePage.value || retries <= 0) return;
  if (alignModelForExploreStage()) return;
  window.setTimeout(() => {
    scheduleExploreAutoCenter(retries - 1, delay);
  }, delay);
}

function getModelLocalBoundsSafe() {
  if (!modelInstance?.getLocalBounds) return null;
  try {
    const bounds = modelInstance.getLocalBounds();
    if (!bounds) return null;
    if (!Number.isFinite(bounds.x) || !Number.isFinite(bounds.y) || !Number.isFinite(bounds.width) || !Number.isFinite(bounds.height)) {
      return null;
    }
    if (bounds.width <= 0 || bounds.height <= 0) return null;
    return bounds;
  } catch (error) {
    return null;
  }
}

function ensureModelVisibleInStage() {
  const bounds = getModelLocalBoundsSafe();
  if (!bounds) return;

  const margin = 10;
  const currentScale = clamp(ui.model.scale, 0.02, 2);
  const width = bounds.width * currentScale;
  const height = bounds.height * currentScale;
  const left = ui.model.x + bounds.x * currentScale;
  const right = ui.model.x + (bounds.x + bounds.width) * currentScale;
  const top = ui.model.y + bounds.y * currentScale;
  const bottom = ui.model.y + (bounds.y + bounds.height) * currentScale;

  const stageWidth = Math.max(32, ui.stage.width);
  const stageHeight = Math.max(32, ui.stage.height);
  const clipped = left < margin || right > stageWidth - margin || top < margin || bottom > stageHeight - margin;
  const tooLarge = width > stageWidth - margin * 2 || height > stageHeight - margin * 2;

  if (!clipped && !tooLarge) {
    ui.model.scale = currentScale;
    applyModelPlacement();
    return;
  }

  const fitScale = clamp(
    Math.min((stageWidth - margin * 2) / bounds.width, (stageHeight - margin * 2) / bounds.height),
    0.02,
    2,
  );

  ui.model.scale = fitScale;
  const targetX = stageWidth * 0.5 - (bounds.x + bounds.width / 2) * fitScale;
  const targetY = stageHeight * 0.5 - (bounds.y + bounds.height / 2) * fitScale;

  const minX = margin - bounds.x * fitScale;
  const maxX = stageWidth - margin - (bounds.x + bounds.width) * fitScale;
  const minY = margin - bounds.y * fitScale;
  const maxY = stageHeight - margin - (bounds.y + bounds.height) * fitScale;

  ui.model.x = minX <= maxX ? clamp(targetX, minX, maxX) : targetX;
  ui.model.y = minY <= maxY ? clamp(targetY, minY, maxY) : targetY;

  applyModelPlacement();
}

function applyExpressions() {
  if (!modelInstance?.internalModel?.coreModel) return;
  const coreModel = modelInstance.internalModel.coreModel;
  Object.entries(ui.expressions).forEach(([id, value]) => {
    try {
      coreModel.setParameterValueById(id, Number(value.Value) || 0);
    } catch (error) {
      // Ignore unsupported parameter ids when switching to a different model.
    }
  });
}

function finishModelDrag() {
  if (!modelDrag.active) return;
  modelDrag.active = false;
  schedulePersistUi();
}

function clearModelPointerListeners() {
  if (modelPointerMoveHandler) {
    window.removeEventListener('pointermove', modelPointerMoveHandler);
  }
  if (modelPointerUpHandler) {
    window.removeEventListener('pointerup', modelPointerUpHandler);
    window.removeEventListener('pointercancel', modelPointerUpHandler);
  }
  if (modelMouseMoveHandler) {
    window.removeEventListener('mousemove', modelMouseMoveHandler);
  }
  if (modelMouseUpHandler) {
    window.removeEventListener('mouseup', modelMouseUpHandler);
  }
  modelPointerMoveHandler = null;
  modelPointerUpHandler = null;
  modelMouseMoveHandler = null;
  modelMouseUpHandler = null;
  modelDrag.pointerId = null;
}

function toStagePoint(clientX, clientY) {
  if (!stageCanvasRef.value) return null;
  const rect = stageCanvasRef.value.getBoundingClientRect();
  if (!rect.width || !rect.height) return null;
  const x = ((clientX - rect.left) / rect.width) * ui.stage.width;
  const y = ((clientY - rect.top) / rect.height) * ui.stage.height;
  if (!Number.isFinite(x) || !Number.isFinite(y)) return null;
  return { x, y };
}

function isPointerOnModel(clientX, clientY) {
  if (!modelInstance) return false;
  const stagePoint = toStagePoint(clientX, clientY);
  if (!stagePoint) return false;
  const localBounds = getModelLocalBoundsSafe();
  if (!localBounds) return true;

  const scale = Math.max(0.02, Number(ui.model.scale) || 0.02);
  const localX = (stagePoint.x - ui.model.x) / scale;
  const localY = (stagePoint.y - ui.model.y) / scale;
  const marginX = Math.max(8, localBounds.width * 0.05);
  const marginY = Math.max(8, localBounds.height * 0.05);

  return localX >= localBounds.x - marginX
    && localX <= localBounds.x + localBounds.width + marginX
    && localY >= localBounds.y - marginY
    && localY <= localBounds.y + localBounds.height + marginY;
}

function resetModelPressState(openChatOnTap = false) {
  clearModelLongPressTimer();
  if (modelDrag.active) {
    finishModelDrag();
  } else if (openChatOnTap && !modelDrag.moved && !ui.model.isAdjusting && !ui.stage.isAdjusting) {
    openInputBubble();
  }
  modelDrag.active = false;
  modelDrag.pressing = false;
  modelDrag.longPressTriggered = false;
  modelDrag.mode = 'stage';
  modelDrag.moved = false;
  clearModelPointerListeners();
}

function handleModelPointerMove(event) {
  if (modelDrag.pointerId !== null && event.pointerId !== modelDrag.pointerId) return;
  const stagePoint = toStagePoint(event.clientX, event.clientY);
  if (!stagePoint) return;

  if (modelDrag.active) {
    if (modelDrag.mode === 'stage') {
      const dx = event.clientX - modelDrag.startClientX;
      const dy = event.clientY - modelDrag.startClientY;
      const inputMetrics = getInputBubbleMetrics();
      const stageBounds = getStageDragBounds(effectiveUiMode.value);
      const nextLeft = clamp(modelDrag.startStageLeft + dx, stageBounds.minLeft, stageBounds.maxLeft);
      const nextTop = clamp(modelDrag.startStageTop + dy, stageBounds.minTop, stageBounds.maxTop);
      ui.stage.left = nextLeft;
      ui.stage.top = nextTop;
      ui.chat.input.x = clamp(modelDrag.startInputX + dx, 8, Math.max(8, window.innerWidth - inputMetrics.width - 8));
      ui.chat.input.y = clamp(modelDrag.startInputY + dy, 8, Math.max(8, window.innerHeight - inputMetrics.maxHeight - 8));
      if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
        modelDrag.moved = true;
      }
      return;
    }

    const nextX = stagePoint.x - modelDrag.offsetX;
    const nextY = stagePoint.y - modelDrag.offsetY;
    if (Math.abs(nextX - ui.model.x) > 1 || Math.abs(nextY - ui.model.y) > 1) {
      modelDrag.moved = true;
    }
    const bounds = getModelBounds(ui.stage.width, ui.stage.height);
    ui.model.x = clamp(nextX, bounds.minX, bounds.maxX);
    ui.model.y = clamp(nextY, bounds.minY, bounds.maxY);
    applyModelPlacement();
    return;
  }

  if (!modelDrag.pressing) return;
  const dx = stagePoint.x - modelDrag.startX;
  const dy = stagePoint.y - modelDrag.startY;
  const distance = Math.hypot(dx, dy);
  if (distance > 5) {
    modelDrag.moved = true;
  }
  if (!modelDrag.longPressTriggered && distance > 56) {
    clearModelLongPressTimer();
  }
}

function handleModelPointerUp(event) {
  if (modelDrag.pointerId !== null && event.pointerId !== modelDrag.pointerId) return;
  // Reduce accidental popups: tap on model won't open chat; use the "聊" button instead.
  resetModelPressState(false);
}

function attachModelPointerListeners() {
  clearModelPointerListeners();
  modelPointerMoveHandler = (event) => {
    handleModelPointerMove(event);
  };
  modelPointerUpHandler = (event) => {
    handleModelPointerUp(event);
  };
  modelMouseMoveHandler = (event) => {
    handleModelPointerMove({
      clientX: event.clientX,
      clientY: event.clientY,
      pointerId: modelDrag.pointerId,
    });
  };
  modelMouseUpHandler = (event) => {
    handleModelPointerUp({
      pointerId: modelDrag.pointerId,
      clientX: event.clientX,
      clientY: event.clientY,
    });
  };
  window.addEventListener('pointermove', modelPointerMoveHandler);
  window.addEventListener('pointerup', modelPointerUpHandler);
  window.addEventListener('pointercancel', modelPointerUpHandler);
  window.addEventListener('mousemove', modelMouseMoveHandler);
  window.addEventListener('mouseup', modelMouseUpHandler);
}

function handleStagePointerDown(event) {
  if (ui.stage.isAdjusting) return;
  if (event.pointerType === 'mouse' && event.button !== 0) return;
  if (!modelReady.value) return;

  const stagePoint = toStagePoint(event.clientX, event.clientY);
  if (!stagePoint) return;
  event.preventDefault();
  event.stopPropagation();
  if (event.target?.setPointerCapture && event.pointerId !== undefined && event.pointerId !== null) {
    try {
      event.target.setPointerCapture(event.pointerId);
    } catch {
      // ignore
    }
  }

  modelDrag.pointerId = event.pointerId ?? null;
  modelDrag.pressing = true;
  modelDrag.moved = false;
  modelDrag.longPressTriggered = false;
  modelDrag.mode = 'stage';
  modelDrag.startClientX = event.clientX;
  modelDrag.startClientY = event.clientY;
  modelDrag.startStageLeft = ui.stage.left;
  modelDrag.startStageTop = ui.stage.top;
  modelDrag.startInputX = ui.chat.input.x;
  modelDrag.startInputY = ui.chat.input.y;
  modelDrag.startX = stagePoint.x;
  modelDrag.startY = stagePoint.y;
  attachModelPointerListeners();

  if (ui.model.isAdjusting) {
    modelDrag.mode = 'model';
    modelDrag.active = true;
    modelDrag.longPressTriggered = true;
    modelDrag.offsetX = modelDrag.startX - ui.model.x;
    modelDrag.offsetY = modelDrag.startY - ui.model.y;
    return;
  }

  clearModelLongPressTimer();
  modelLongPressTimer = window.setTimeout(() => {
    if (!modelDrag.pressing) return;
    modelDrag.mode = 'stage';
    modelDrag.active = true;
    modelDrag.longPressTriggered = true;
  }, 80);
}

function handleStageMouseDown(event) {
  handleStagePointerDown({
    button: event.button ?? 0,
    pointerType: 'mouse',
    pointerId: 1,
    clientX: event.clientX,
    clientY: event.clientY,
    target: event.target,
    preventDefault: () => event.preventDefault(),
    stopPropagation: () => event.stopPropagation(),
  });
}

function handleStageTouchStart(event) {
  const touch = event.touches?.[0];
  if (!touch) return;
  handleStagePointerDown({
    button: 0,
    pointerType: 'touch',
    pointerId: 1,
    clientX: touch.clientX,
    clientY: touch.clientY,
    target: event.target,
    preventDefault: () => event.preventDefault(),
    stopPropagation: () => event.stopPropagation(),
  });
}

function resizeStage() {
  if (!pixiApp) return;
  pixiApp.renderer.resolution = getResolution();
  pixiApp.renderer.resize(ui.stage.width, ui.stage.height);
  applyModelPlacement();
  applyExpressions();
}

async function renderStage() {
  if (!stageCanvasRef.value) return;

  const profile = getPerformanceProfile();
  runtimeStatus.value = '模型加载中...';
  await ensureLive2dRuntime();
  applyPixiSettings();

  if (pixiApp) {
    try {
      pixiApp.destroy(false, {
        children: true,
        texture: true,
        baseTexture: true,
      });
    } catch (error) {
      // ignore
    }
    pixiApp = null;
    modelInstance = null;
  }

  modelReady.value = false;

  const PIXI = window.PIXI;
  pixiApp = new PIXI.Application({
    view: stageCanvasRef.value,
    transparent: true,
    width: ui.stage.width,
    height: ui.stage.height,
    antialias: profile.antialias,
    autoDensity: true,
    resolution: getResolution(),
    powerPreference: 'high-performance',
  });
  pixiApp.renderer.roundPixels = false;

  modelInstance = await PIXI.live2d.Live2DModel.from(config.value.modelUrl);
  pixiApp.stage.addChild(modelInstance);

  applyModelPlacement();
  ensureModelVisibleInStage();
  alignModelForExploreStage();
  scheduleExploreAutoCenter();
  applyExpressions();
  modelReady.value = true;
  runtimeStatus.value = '';
  syncTickerState();
}

async function ensureStageRendered() {
  await nextTick();
  if (!stageCanvasRef.value) return;
  try {
    await renderStage();
    didResetAfterRenderFail = false;
  } catch (error) {
    if (!didResetAfterRenderFail) {
      didResetAfterRenderFail = true;
      applyUiState(createDefaultUi(effectiveUiMode.value));
      try {
        await renderStage();
        didResetAfterRenderFail = false;
        return;
      } catch (retryError) {
        console.error('[Live2D] 重置布局后仍渲染失败', retryError);
      }
    }
    console.error('[Live2D] 模型渲染失败', error);
    modelReady.value = false;
    runtimeStatus.value = normalizeDisplayText(error?.message, '模型加载失败，请刷新页面后重试');
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResizeViewport);
  document.addEventListener('visibilitychange', handleVisibilityChange);
  await store.ensureLive2dConfig(resolvedScenicId.value).catch(() => null);
  applyUiState(loadUiState(effectiveUiMode.value));
  inputVisible.value = isExplorePage.value;
  setAdjustmentMode('off');
  clampUiWithinViewport();
  viewportTick.value += 1;
  uiReady.value = true;
  scheduleStageRender(getPerformanceProfile().initDelay);
  triggerIntroOnExploreEntry();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResizeViewport);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  cleanupGlobalDrag();
  resetModelPressState(false);
  clearInputHideTimer();
  clearIntroShowTimer();
  clearIntroHideTimer();
  window.clearTimeout(persistTimer);
  window.clearTimeout(initRenderTimer);
  if (pixiApp) {
    try {
      pixiApp.destroy(false, {
        children: true,
        texture: true,
        baseTexture: true,
      });
    } catch (error) {
      // ignore
    }
    pixiApp = null;
    modelInstance = null;
  }
});
</script>

<style scoped>
.live2d-root {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 14;
}

.live2d-active-layer {
  pointer-events: none;
}

.live2d-stage-shell,
.live2d-hit-area,
.live2d-chat-input,
.live2d-dock,
.live2d-intro-tip,
.live2d-drawer,
.live2d-reopen,
.live2d-explore-panel,
.live2d-explore-input,
.live2d-explore-chip,
.explore-action-btn {
  pointer-events: auto;
}

.live2d-explore-panel {
  position: fixed;
  left: 14px;
  top: 14px;
  bottom: 14px;
  width: min(320px, calc(100vw - 16px));
  z-index: 16;
  display: flex;
  flex-direction: column;
  border-radius: 26px;
  border: 1px solid rgba(46, 139, 87, 0.18);
  background:
    linear-gradient(180deg, rgba(250, 247, 241, 0.96), rgba(245, 240, 232, 0.92));
  box-shadow:
    0 20px 44px rgba(26, 74, 58, 0.2),
    0 0 0 1px rgba(46, 139, 87, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(20px);
  overflow: hidden;
}

.live2d-explore-stage-wrap {
  position: relative;
  height: 212px;
  border-bottom: 1px solid rgba(46, 139, 87, 0.14);
  background:
    radial-gradient(circle at 50% 12%, rgba(87, 198, 169, 0.45), transparent 52%),
    linear-gradient(180deg, #08281d 0%, #0f3c2d 44%, #246a52 76%, #3b8868 100%);
  overflow: hidden;
}

.live2d-explore-stage-wrap::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1.3px 1.3px at 7% 12%, rgba(255, 255, 255, 0.78) 0%, transparent 100%),
    radial-gradient(1px 1px at 19% 26%, rgba(255, 255, 255, 0.66) 0%, transparent 100%),
    radial-gradient(1.6px 1.6px at 34% 18%, rgba(255, 255, 255, 0.86) 0%, transparent 100%),
    radial-gradient(1px 1px at 46% 10%, rgba(255, 255, 255, 0.62) 0%, transparent 100%),
    radial-gradient(1.4px 1.4px at 58% 28%, rgba(255, 255, 255, 0.74) 0%, transparent 100%),
    radial-gradient(1.2px 1.2px at 73% 16%, rgba(255, 255, 255, 0.72) 0%, transparent 100%),
    radial-gradient(1px 1px at 86% 22%, rgba(255, 255, 255, 0.6) 0%, transparent 100%);
  opacity: 0.76;
  pointer-events: none;
}

.live2d-explore-stage-atmo {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background:
    radial-gradient(58% 36% at 50% 12%, rgba(130, 238, 210, 0.26), transparent 76%),
    radial-gradient(46% 26% at 28% 20%, rgba(102, 216, 186, 0.16), transparent 78%),
    radial-gradient(44% 24% at 72% 18%, rgba(102, 216, 186, 0.15), transparent 80%);
  filter: blur(2px);
  animation: stage-atmo-breathe 6.4s ease-in-out infinite;
}

.live2d-explore-stage-mountains {
  position: absolute;
  left: -8%;
  right: -8%;
  bottom: 42px;
  height: 66px;
  z-index: 3;
  pointer-events: none;
  background:
    linear-gradient(160deg, rgba(169, 226, 202, 0.14), rgba(169, 226, 202, 0.03)),
    linear-gradient(180deg, rgba(20, 71, 55, 0.4), rgba(20, 71, 55, 0.14));
  clip-path: polygon(0% 100%, 8% 72%, 18% 58%, 26% 68%, 38% 42%, 50% 66%, 62% 46%, 74% 70%, 88% 50%, 100% 72%, 100% 100%);
  opacity: 0.74;
}

.live2d-explore-stage-fireflies {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
}

.live2d-explore-stage-fireflies span {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(220, 255, 244, 0.86);
  box-shadow: 0 0 8px rgba(220, 255, 244, 0.48);
  animation: stage-firefly-float 5.8s ease-in-out infinite;
}

.live2d-explore-stage-fireflies span:nth-child(1) {
  left: 14%;
  top: 22%;
  animation-delay: 0s;
}

.live2d-explore-stage-fireflies span:nth-child(2) {
  left: 32%;
  top: 16%;
  animation-delay: 1.2s;
}

.live2d-explore-stage-fireflies span:nth-child(3) {
  left: 58%;
  top: 26%;
  animation-delay: 2s;
}

.live2d-explore-stage-fireflies span:nth-child(4) {
  left: 76%;
  top: 18%;
  animation-delay: 2.8s;
}

.live2d-explore-stage-fireflies span:nth-child(5) {
  left: 24%;
  top: 34%;
  animation-delay: 3.6s;
}

.live2d-explore-stage-fireflies span:nth-child(6) {
  left: 70%;
  top: 34%;
  animation-delay: 4.4s;
}

.live2d-explore-stage-terrain {
  position: absolute;
  left: -8%;
  right: -8%;
  bottom: 8px;
  height: 74px;
  z-index: 5;
  background:
    radial-gradient(120px 52px at 18% 100%, rgba(255, 255, 255, 0.1), transparent 66%),
    radial-gradient(160px 58px at 50% 100%, rgba(255, 255, 255, 0.08), transparent 70%),
    radial-gradient(140px 56px at 82% 100%, rgba(255, 255, 255, 0.1), transparent 68%);
  pointer-events: none;
}

.live2d-explore-stage-lower-glow {
  position: absolute;
  left: -8%;
  right: -8%;
  bottom: -1px;
  height: 120px;
  z-index: 6;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(118, 208, 177, 0) 6%, rgba(118, 208, 177, 0.26) 56%, rgba(95, 188, 156, 0.5) 100%),
    radial-gradient(150px 48px at 24% 100%, rgba(211, 251, 236, 0.48), transparent 72%),
    radial-gradient(220px 64px at 50% 100%, rgba(187, 243, 221, 0.58), transparent 70%),
    radial-gradient(150px 48px at 78% 100%, rgba(211, 251, 236, 0.48), transparent 72%);
  filter: saturate(1.18) brightness(1.04);
  animation: stage-lower-shimmer 5.8s ease-in-out infinite;
}

.live2d-explore-stage-lower-glow::before {
  content: '';
  position: absolute;
  left: 8%;
  right: 8%;
  top: 18px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent 0%, rgba(223, 255, 245, 0.78) 50%, transparent 100%);
  opacity: 0.68;
  filter: blur(0.8px);
  animation: stage-waterline-glint 6.2s ease-in-out infinite;
}

.live2d-explore-stage-lower-glow::after {
  content: '';
  position: absolute;
  left: 14%;
  right: 14%;
  bottom: 10px;
  height: 28px;
  border-radius: 999px;
  background: radial-gradient(ellipse at center, rgba(212, 252, 238, 0.34) 0%, rgba(212, 252, 238, 0) 76%);
  opacity: 0.9;
  filter: blur(1px);
}

.live2d-explore-stage-ripples {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 14px;
  height: 66px;
  z-index: 6;
  pointer-events: none;
}

.live2d-explore-stage-ripples::before {
  content: '';
  position: absolute;
  left: 18%;
  right: 18%;
  bottom: 10px;
  height: 16px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent 0%, rgba(220, 255, 244, 0.72) 50%, transparent 100%);
  opacity: 0.86;
  filter: blur(0.7px);
  animation: stage-ripple-sheen 4.8s ease-in-out infinite;
}

.live2d-explore-stage-ripples::after {
  content: '';
  position: absolute;
  left: 24%;
  right: 24%;
  bottom: 20px;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent 0%, rgba(216, 252, 237, 0.56) 50%, transparent 100%);
  opacity: 0.64;
  filter: blur(0.5px);
}

.live2d-explore-stage-ripples span {
  position: absolute;
  left: 50%;
  top: 50%;
  border: 1.8px solid rgba(214, 252, 237, 0.66);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow:
    0 0 14px rgba(195, 244, 224, 0.34),
    inset 0 0 8px rgba(218, 253, 239, 0.12);
  filter: blur(0.1px);
  animation: stage-ripple-breathe 5.8s ease-in-out infinite;
}

.live2d-explore-stage-ripples span:nth-child(1) {
  width: 156px;
  height: 30px;
  opacity: 0.82;
  animation-delay: 0s;
}

.live2d-explore-stage-ripples span:nth-child(2) {
  width: 216px;
  height: 38px;
  opacity: 0.58;
  animation-delay: 0.8s;
}

.live2d-explore-stage-ripples span:nth-child(3) {
  width: 278px;
  height: 48px;
  opacity: 0.4;
  animation-delay: 1.5s;
}

.live2d-explore-stage-reeds {
  position: absolute;
  left: -4%;
  right: -4%;
  bottom: 0;
  height: 58px;
  z-index: 6;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(61, 152, 124, 0), rgba(61, 152, 124, 0.42) 58%, rgba(28, 95, 74, 0.7)),
    repeating-linear-gradient(
      102deg,
      transparent 0 10px,
      rgba(202, 247, 229, 0.42) 10px 12px,
      transparent 12px 22px
    ),
    repeating-linear-gradient(
      78deg,
      transparent 0 12px,
      rgba(31, 88, 70, 0.36) 12px 14px,
      transparent 14px 26px
    );
  clip-path: polygon(0% 100%, 0% 70%, 6% 76%, 12% 58%, 18% 74%, 24% 62%, 30% 78%, 36% 64%, 42% 80%, 50% 66%, 58% 80%, 64% 64%, 70% 78%, 76% 62%, 82% 74%, 88% 58%, 94% 76%, 100% 70%, 100% 100%);
  opacity: 0.98;
  filter:
    drop-shadow(0 -1px 0 rgba(217, 252, 238, 0.42))
    drop-shadow(0 -5px 8px rgba(27, 84, 65, 0.28));
  animation: stage-reeds-sway 7.2s ease-in-out infinite;
}

.live2d-explore-stage-reeds::before,
.live2d-explore-stage-reeds::after {
  content: '';
  position: absolute;
  bottom: 8px;
  width: 104px;
  height: 38px;
  background: repeating-linear-gradient(
    100deg,
    transparent 0 8px,
    rgba(214, 252, 237, 0.44) 8px 9px,
    transparent 9px 16px
  );
  opacity: 0.56;
}

.live2d-explore-stage-reeds::before {
  left: 7%;
}

.live2d-explore-stage-reeds::after {
  right: 7%;
  transform: scaleX(-1);
}

.live2d-explore-stage-terrain::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -8px;
  height: 42px;
  background: linear-gradient(180deg, rgba(57, 162, 133, 0.42), rgba(57, 162, 133, 0.12));
}

.explore-stage-shell {
  border-radius: 0;
  background: transparent;
  z-index: 7;
}

.live2d-explore-head {
  min-height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 12px 10px;
  border-bottom: 1px solid rgba(46, 139, 87, 0.14);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.52));
}

.live2d-explore-assistant {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.assistant-avatar {
  width: 31px;
  height: 31px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #2f6f58, #3c9570);
  box-shadow: 0 7px 14px rgba(31, 84, 62, 0.28);
}

.live2d-explore-assistant strong {
  display: block;
  color: #1f4a3b;
  font-size: 15px;
  line-height: 1.2;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
}

.live2d-explore-assistant p {
  margin: 0;
  color: rgba(31, 67, 55, 0.62);
  font-size: 11px;
  line-height: 1.3;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.assistant-presence {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #47c183;
  box-shadow: 0 0 0 3px rgba(71, 193, 131, 0.18);
}

.live2d-explore-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.explore-action-btn {
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(46, 139, 87, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: rgba(31, 67, 55, 0.78);
  font-size: 11px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.16s ease, background 0.16s ease, color 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease;
}

.explore-action-btn:disabled {
  opacity: 0.55;
  cursor: wait;
}

.explore-action-btn.active,
.explore-action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  color: #fffaf3;
  border-color: rgba(46, 139, 87, 0.34);
  background: linear-gradient(135deg, #2f6f58, #3b8b6b);
  box-shadow: 0 6px 14px rgba(26, 74, 58, 0.25);
}

.live2d-explore-chat-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.live2d-explore-thread {
  flex: 1;
  min-height: 0;
  max-height: none;
  padding: 12px 10px 8px;
}

.live2d-explore-history {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  overflow: hidden;
}

.explore-history-tools {
  color: rgba(31, 67, 55, 0.66);
  font-size: 12px;
}

.explore-history-chip {
  color: #255241;
  border: 1px solid rgba(46, 139, 87, 0.14);
  background: rgba(255, 255, 255, 0.78);
}

.live2d-explore-round-list {
  max-height: 142px;
  overflow-y: auto;
}

.live2d-explore-history-list {
  flex: 1;
  min-height: 0;
  max-height: none;
}

.live2d-explore-chip-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.live2d-explore-chip-row::-webkit-scrollbar {
  display: none;
}

.live2d-explore-chip {
  flex-shrink: 0;
  min-height: 27px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(46, 139, 87, 0.14);
  background: rgba(255, 255, 255, 0.74);
  color: rgba(31, 67, 55, 0.74);
  font-size: 11px;
}

.live2d-explore-chip:disabled {
  opacity: 0.56;
  cursor: wait;
}

.live2d-explore-chip:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.92);
  color: #1f4a3b;
}

.live2d-explore-input {
  margin: 8px 10px 0;
  padding: 7px 10px;
  border-radius: 16px;
  border: 1px solid rgba(46, 139, 87, 0.14);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.live2d-explore-textarea {
  min-height: 26px;
  max-height: 108px;
  font-size: 13px;
  line-height: 1.55;
}

.live2d-explore-send {
  min-width: 64px;
  min-height: 30px;
  padding: 0 10px;
}

.live2d-explore-disclaimer {
  margin: 5px 10px 9px;
  text-align: center;
  color: rgba(31, 67, 55, 0.38);
  font-size: 9px;
  line-height: 1.3;
}

.live2d-stage-shell {
  position: fixed;
  border: 3px solid rgba(0, 0, 0, 0);
  border-radius: 16px;
  background: transparent;
  overflow: visible;
  touch-action: none;
  pointer-events: none;
}

.live2d-stage-shell.adjusting {
  background: rgba(110, 168, 254, 0.14);
  border-color: #4c7dff;
  border-style: dashed;
  pointer-events: auto;
}

.live2d-stage-shell.dragging {
  cursor: move;
}

.live2d-stage-glow {
  position: absolute;
  left: 10%;
  right: 10%;
  bottom: 8%;
  top: 22%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--brand-soft-rgb), 0.32) 0%, rgba(var(--brand-soft-rgb), 0.12) 45%, transparent 78%);
  filter: blur(16px);
  pointer-events: none;
}

.live2d-stage-canvas {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
}

.live2d-hit-area {
  position: fixed;
  z-index: 15;
  border: none;
  background: transparent;
  border-radius: 42% 42% 32% 32% / 46% 46% 54% 54%;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.live2d-stage-shell.dragging .live2d-hit-area {
  cursor: grabbing;
}

.live2d-fallback {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 18px 26px;
  border-radius: 16px;
  color: rgba(255, 255, 255, 0.94);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 4px;
  text-align: center;
  background: linear-gradient(180deg, rgba(13, 28, 45, 0.2), rgba(8, 20, 34, 0.52));
  cursor: pointer;
}

.live2d-fallback strong {
  font-size: 30px;
}

.live2d-fallback small {
  max-width: 220px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.6;
  letter-spacing: 0;
}

.live2d-resize-handle {
  position: absolute;
  right: -10px;
  bottom: -10px;
  z-index: 3;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #5ec5ff, #4c7dff);
  color: #fff;
  font-size: 16px;
  box-shadow: 0 12px 24px rgba(76, 125, 255, 0.28);
  cursor: nwse-resize;
}

.live2d-dock {
  position: fixed;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 15;
}

.dock-btn {
  min-width: 92px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(8, 20, 34, 0.8);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.14);
}

.dock-btn.primary {
  background: linear-gradient(135deg, var(--brand), var(--brand-soft));
  border-color: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.live2d-intro-tip {
  position: fixed;
  z-index: 16;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(31, 51, 70, 0.14);
  background: rgba(255, 255, 255, 0.97);
  color: #1e3244;
  box-shadow: 0 14px 32px rgba(9, 16, 26, 0.18);
}

.live2d-intro-tip strong {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #20465f;
}

.live2d-intro-tip p {
  margin: 0;
  font-size: 13px;
  line-height: 1.66;
}

.live2d-intro-tip button {
  margin-top: 10px;
  height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--brand), var(--brand-soft));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.intro-fade-enter-active,
.intro-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.intro-fade-enter-from,
.intro-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.live2d-chat-input {
  position: fixed;
  z-index: 16;
  width: min(380px, calc(100vw - 16px));
  max-height: min(72vh, 560px);
  background: #ffffff;
  border: 1px solid #e0e6ed;
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 14px;
  overflow: hidden;
}

.live2d-chat-input.adjusting {
  outline: 2px dashed #4aa3ff;
  cursor: move;
  user-select: none;
}

.chat-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.chat-panel-head strong {
  color: #1d3346;
  font-size: 13px;
}

.chat-panel-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.chat-thread {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 140px;
  max-height: min(48vh, 340px);
  padding-right: 4px;
  overflow-y: auto;
}

.live2d-explore-panel .live2d-explore-thread {
  min-height: 0;
  max-height: none;
  flex: 1;
}

.chat-thread-empty {
  border-radius: 12px;
  background: #f3f6fb;
  color: #637386;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
}

.chat-item {
  border-radius: 14px;
  padding: 10px 12px;
}

.chat-item.role-user {
  background: #edf4ff;
  border-left: 3px solid #4c7dff;
}

.chat-item.role-assistant {
  background: #f5f8fc;
  border-left: 3px solid #26a69a;
}

.chat-role {
  font-size: 12px;
  color: #5e7083;
  margin-bottom: 6px;
}

.chat-text {
  color: #1d3346;
  font-size: 13px;
  line-height: 1.68;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-item .history-time {
  color: #76889a;
}

.live2d-textarea {
  box-shadow: none;
  width: 100%;
  min-height: 24px;
  max-height: 200px;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: #233443;
  font-size: 14px;
  line-height: 1.6;
  overflow: auto;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
}

.live2d-textarea:disabled {
  color: #9aa9b2;
  cursor: not-allowed;
}

.live2d-input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.live2d-input-hint {
  color: #7b8793;
  font-size: 12px;
}

.live2d-send,
.drawer-primary,
.drawer-ghost,
.chat-mini-btn,
.state-toggle,
.expression-chip,
.spot-chip,
.route-chip {
  border: none;
  font: inherit;
  cursor: pointer;
}

.live2d-send,
.drawer-primary {
  min-width: 72px;
  padding: 8px 14px;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-soft));
}

.live2d-send:disabled {
  opacity: 0.7;
  cursor: wait;
}

.chat-mini-btn {
  min-width: 56px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(35, 52, 67, 0.08);
  color: #2d4355;
  font-size: 12px;
}

.chat-mini-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.live2d-output-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.live2d-output-spots {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.live2d-output-actions {
  margin-top: 10px;
}

.spot-chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: #e6f3ff;
  color: #205073;
  font-size: 12px;
}

.route-chip {
  min-height: 30px;
  padding: 6px 14px;
  border-radius: 999px;
  color: #153128;
  background: linear-gradient(135deg, rgba(229, 246, 237, 0.98), rgba(182, 228, 206, 0.98));
  box-shadow: 0 8px 14px rgba(6, 18, 14, 0.22);
  font-size: 12px;
  font-weight: 600;
}

.live2d-drawer {
  position: fixed;
  z-index: 17;
  width: min(280px, calc(100vw - 24px));
  max-height: min(68vh, 520px);
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  background: rgba(8, 21, 34, 0.9);
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.18);
  overflow: auto;
}

.drawer-header,
.history-tools,
.setting-row,
.setting-head,
.drawer-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.drawer-header {
  margin-bottom: 12px;
}

.drawer-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
  cursor: pointer;
}

.drawer-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drawer-action-row {
  gap: 8px;
}

.fit-action-row {
  margin-top: 10px;
}

.drawer-ghost {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
}

.drawer-ghost.small {
  padding: 6px 10px;
  font-size: 12px;
}

.setting-card {
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
}

.setting-row {
  align-items: flex-start;
}

.setting-row + .setting-row {
  margin-top: 12px;
}

.setting-row strong,
.setting-head strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
}

.setting-row p {
  margin: 0;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
  line-height: 1.5;
}

.state-toggle {
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.82);
}

.state-toggle.active,
.expression-chip.active {
  background: linear-gradient(135deg, var(--brand), var(--brand-soft));
  color: #fff;
}

.setting-head.compact {
  margin-bottom: 10px;
}

.size-slider {
  width: 100%;
}

.expression-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.expression-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
}

.round-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.round-chip {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.9);
  text-align: left;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  cursor: pointer;
}

.round-chip.active {
  border-color: rgba(94, 197, 255, 0.72);
  background: rgba(94, 197, 255, 0.18);
}

.round-chip small {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  line-height: 1.4;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 260px;
  overflow-y: auto;
}

.history-empty {
  color: rgba(255, 255, 255, 0.64);
  font-size: 13px;
  line-height: 1.6;
}

.history-item {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}

.history-item.role-user {
  border-left: 3px solid var(--brand-soft);
}

.history-item.role-assistant {
  border-left: 3px solid #5ec5ff;
}

.history-role {
  margin-bottom: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.history-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.history-content p {
  margin: 0;
}

.round-user + .round-assistant {
  margin-top: 6px;
}

.history-time {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.48);
}

.live2d-explore-panel .chat-thread-empty {
  background: rgba(255, 255, 255, 0.72);
  color: rgba(31, 67, 55, 0.64);
  border: 1px solid rgba(46, 139, 87, 0.12);
}

.live2d-explore-panel .chat-item {
  background: rgba(255, 255, 255, 0.76);
}

.live2d-explore-panel .chat-item.role-user {
  border-left-color: rgba(46, 139, 87, 0.7);
  background: rgba(231, 246, 237, 0.84);
}

.live2d-explore-panel .chat-item.role-assistant {
  border-left-color: rgba(47, 95, 75, 0.66);
  background: rgba(255, 255, 255, 0.82);
}

.live2d-explore-panel .chat-role,
.live2d-explore-panel .chat-text,
.live2d-explore-panel .history-time {
  color: rgba(31, 67, 55, 0.74);
}

.live2d-explore-panel .spot-chip {
  background: rgba(223, 241, 232, 0.9);
  color: #1f4a3b;
}

.live2d-explore-panel .route-chip {
  color: #17392f;
  box-shadow: 0 8px 16px rgba(12, 34, 26, 0.14);
}

.live2d-explore-panel .round-chip {
  border: 1px solid rgba(46, 139, 87, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: #1f4a3b;
}

.live2d-explore-panel .round-chip.active {
  border-color: rgba(46, 139, 87, 0.34);
  background: rgba(225, 243, 234, 0.92);
}

.live2d-explore-panel .round-chip small {
  color: rgba(31, 67, 55, 0.56);
}

.live2d-explore-panel .history-empty {
  color: rgba(31, 67, 55, 0.58);
}

.live2d-explore-panel .history-item {
  border-radius: 12px;
  border: 1px solid rgba(46, 139, 87, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.live2d-explore-panel .history-item.role-user {
  border-left-color: rgba(46, 139, 87, 0.7);
}

.live2d-explore-panel .history-item.role-assistant {
  border-left-color: rgba(47, 95, 75, 0.66);
}

.live2d-explore-panel .history-role {
  color: rgba(31, 67, 55, 0.56);
}

.live2d-explore-panel .history-content {
  color: rgba(31, 67, 55, 0.84);
}

.live2d-reopen {
  position: fixed;
  z-index: 15;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(8, 20, 34, 0.84);
  color: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.16);
  backdrop-filter: blur(10px);
  cursor: pointer;
}

.live2d-reopen-icon {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand), var(--brand-soft));
  color: #fff;
  font-size: 12px;
}

.live2d-reopen-explore {
  z-index: 24;
  border-color: rgba(46, 139, 87, 0.22);
  background: rgba(245, 240, 232, 0.96);
  color: #235040;
  box-shadow: 0 8px 20px rgba(26, 74, 58, 0.18);
}

.live2d-reopen-explore .live2d-reopen-icon {
  box-shadow: 0 6px 14px rgba(47, 95, 75, 0.24);
}

.live2d-intro-tip-explore {
  border-color: rgba(46, 139, 87, 0.22);
  background: rgba(255, 252, 245, 0.97);
  color: #1f4a3b;
}

.live2d-intro-tip-explore strong {
  color: #245441;
}

.live2d-root.page-explore .live2d-stage-glow {
  filter: blur(8px);
  opacity: 0.55;
}

.live2d-root.page-explore .dock-btn,
.live2d-root.page-explore .live2d-drawer,
.live2d-root.page-explore .live2d-reopen {
  backdrop-filter: none;
}

@keyframes stage-atmo-breathe {
  0%,
  100% {
    opacity: 0.74;
    transform: scale(1);
  }

  50% {
    opacity: 1;
    transform: scale(1.03);
  }
}

@keyframes stage-firefly-float {
  0%,
  100% {
    opacity: 0.2;
    transform: translate3d(0, 0, 0) scale(0.92);
  }

  30% {
    opacity: 0.92;
    transform: translate3d(-2px, -6px, 0) scale(1.08);
  }

  65% {
    opacity: 0.56;
    transform: translate3d(3px, -10px, 0) scale(0.98);
  }
}

@keyframes stage-ripple-breathe {
  0%,
  100% {
    opacity: 0.38;
    transform: translate(-50%, -50%) scale(0.95);
  }

  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.08);
  }
}

@keyframes stage-reeds-sway {
  0%,
  100% {
    transform: translateX(0) skewX(0deg);
    opacity: 0.88;
  }

  40% {
    transform: translateX(-2px) skewX(-1.3deg);
    opacity: 1;
  }

  70% {
    transform: translateX(1px) skewX(1deg);
    opacity: 0.92;
  }
}

@keyframes stage-lower-shimmer {
  0%,
  100% {
    opacity: 0.96;
    transform: translateY(0);
  }

  50% {
    opacity: 1.12;
    transform: translateY(-1.5px);
  }
}

@keyframes stage-waterline-glint {
  0%,
  100% {
    opacity: 0.5;
    transform: scaleX(0.93);
  }

  50% {
    opacity: 1;
    transform: scaleX(1.06);
  }
}

@keyframes stage-ripple-sheen {
  0%,
  100% {
    opacity: 0.58;
    transform: scaleX(0.96);
  }

  50% {
    opacity: 0.96;
    transform: scaleX(1.05);
  }
}

@media (max-width: 920px) {
  .live2d-explore-panel {
    left: 8px;
    top: 8px;
    bottom: 76px;
    width: min(320px, calc(100vw - 16px));
  }

  .live2d-explore-stage-wrap {
    height: 194px;
  }
}

@media (max-width: 760px) {
  .live2d-explore-panel {
    width: calc(100vw - 16px);
    max-width: none;
  }

  .live2d-explore-head {
    min-height: 56px;
    gap: 6px;
    padding: 10px;
  }

  .live2d-explore-actions {
    gap: 4px;
  }

  .live2d-explore-assistant strong {
    font-size: 14px;
  }

  .live2d-explore-assistant p {
    font-size: 10px;
  }

  .explore-action-btn {
    min-height: 25px;
    padding: 0 8px;
    font-size: 10px;
  }

  .live2d-dock {
    flex-direction: row;
  }

  .dock-btn {
    min-width: 88px;
    height: 34px;
    padding: 0 10px;
  }

  .live2d-drawer {
    max-height: 56vh;
  }
}
</style>
