<template>
  <div class="ai-page">
    <div class="background-layer">
      <div class="bg-image"></div>
      <div class="bg-overlay"></div>
    </div>

    <SystemNav />

    <main class="page-shell">
      <section class="ai-page-header">
        <h1>泽畔智语</h1>
        <p>围绕湿地科普、游览路线与系统功能发起对话，让问答像浏览档案一样清晰直接。</p>
      </section>

      <section class="chat-layout">
        <aside class="control-panel">
          <button
            type="button"
            class="panel-toggle"
            :aria-expanded="String(!isPanelCollapsed)"
            @click="isPanelCollapsed = !isPanelCollapsed"
          >
            {{ isPanelCollapsed ? '展开面板' : '收起面板' }}
          </button>

          <div v-if="!isPanelCollapsed" class="control-panel__content">
          <span class="panel-kicker"></span>
          <h1>湿地问答</h1>
          <button class="clear-btn" @click="resetMessages" :disabled="isLoading">清空对话</button>
          </div>
        </aside>

        <section class="chat-stage">
          <header class="chat-header">
            <div>
              <span class="chat-label"></span>
              <strong>{{ isLoading ? '正在回复' : '在线' }}</strong>
            </div>
          </header>

          <div class="chat-body" ref="chatBodyRef">
            <article
              v-for="(item, index) in messages"
              :key="`${item.role}-${index}`"
              :class="['chat-bubble', item.role]"
            >
              <span>{{ item.role === 'assistant' ? '湿地助手' : 'You' }}</span>
              <p>{{ item.content }}</p>

              <div v-if="item.cards?.length" class="reply-cards">
                <button
                  v-for="card in item.cards"
                  :key="`${card.type}-${card.id}-${card.path}`"
                  type="button"
                  class="reply-card"
                  @click="openCard(card)"
                >
                  <div class="reply-card__media">
                    <img :src="card.image" :alt="card.title" />
                  </div>
                  <div class="reply-card__body">
                    <span>{{ card.tag }}</span>
                    <strong>{{ card.title }}</strong>
                    <p>{{ card.description }}</p>
                    <em>{{ card.meta }}</em>
                  </div>
                </button>
              </div>
            </article>

            <article v-if="isLoading" class="chat-bubble assistant is-loading">
              <span>湿地助手</span>
              <p>正在生成回复...</p>
            </article>
          </div>

          <div v-if="errorMessage" class="chat-error">{{ errorMessage }}</div>

          <section v-if="currentSuggestions.length" class="chat-suggestions-panel" data-y-slice-skip>
            <div class="chat-suggestions-panel__topline">
              <span>建议</span>
              <strong>{{ latestIntentLabel }}</strong>
            </div>

            <div class="chat-suggestions-panel__grid">
              <button
                v-for="question in currentSuggestions"
                :key="question"
                type="button"
                class="suggestion-chip"
                :disabled="isLoading"
                @click="sendSuggestedQuestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </section>

          <form class="chat-form" @submit.prevent="sendCurrentInput()">
            <textarea
              ref="textareaRef"
              v-model="inputText"
              class="chat-input"
              rows="4"
              placeholder="输入问题"
              :disabled="isLoading"
              @input="handleInput"
              @compositionstart="isComposing = true"
              @compositionend="isComposing = false"
              @keydown.enter.exact.prevent="handleTextareaEnter"
              @keydown.enter.shift.exact.stop
            ></textarea>

            <div class="chat-actions">
              <button type="button" class="send-btn" :disabled="isLoading" @click="sendCurrentInput()">
                {{ isLoading ? '发送中...' : '发送' }}
              </button>
            </div>
          </form>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import SystemNav from '../components/SystemNav.vue'
import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'

const router = useRouter()
const CARD_FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1473773508845-188df298d2d1?auto=format&fit=crop&w=1200&q=80'
const CHAT_REQUEST_TIMEOUT_MS = 30000

const INITIAL_MESSAGES = [
  {
    role: 'assistant',
    content: '你好，我是湿地 AI 助手。你可以问我湿地科普、游览建议、路线选择或系统使用问题。',
    cards: [],
    suggestedQuestions: [
      '九寨沟有哪些珍稀动植物？',
      '推荐一个适合科普浏览的湿地',
      '湿地里的珍稀动物一般怎么保护？'
    ]
  }
]

const messages = ref([...INITIAL_MESSAGES])
const inputText = ref('')
const isLoading = ref(false)
const isComposing = ref(false)
const errorMessage = ref('')
const chatBodyRef = ref(null)
const textareaRef = ref(null)
const currentSuggestions = ref([...(INITIAL_MESSAGES[0].suggestedQuestions || [])])
const currentIntent = ref('general')
const isPanelCollapsed = ref(false)

const getCurrentInputValue = () => {
  const reactiveValue = typeof inputText.value === 'string' ? inputText.value : String(inputText.value ?? '')
  const domValue = typeof textareaRef.value?.value === 'string' ? textareaRef.value.value : ''
  return domValue || reactiveValue
}

const canSend = computed(() => getCurrentInputValue().trim().length > 0)
const latestIntentLabel = computed(() => {
  const intent = currentIntent.value || 'general'
  const labels = {
    species: 'Species',
    wetland: 'Wetland',
    route: 'Route',
    system: 'System',
    community: 'Community',
    general: ''
  }
  return labels[intent] || ''
})

const scrollToBottom = async () => {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTo({
      top: chatBodyRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

const resetMessages = () => {
  messages.value = [...INITIAL_MESSAGES]
  errorMessage.value = ''
  inputText.value = ''
  if (textareaRef.value) {
    textareaRef.value.value = ''
  }
  currentSuggestions.value = [...(INITIAL_MESSAGES[0].suggestedQuestions || [])]
  currentIntent.value = 'general'
}

const normalizeCards = (cards = []) => {
  return cards.map((card) => ({
    ...card,
    image: resolveAssetUrl(card.image, CARD_FALLBACK_IMAGE)
  }))
}

const openCard = (card) => {
  if (!card?.path) return
  router.push(card.path)
}

const sendMessage = async (presetContent = '') => {
  const resolvedContent = typeof presetContent === 'string'
    ? presetContent
    : getCurrentInputValue()
  const content = resolvedContent.trim()
  if (!content) {
    errorMessage.value = '请输入问题后再发送。'
    return
  }
  if (isLoading.value) return

  errorMessage.value = ''
  messages.value.push({ role: 'user', content })
  if (!presetContent) {
    inputText.value = ''
    if (textareaRef.value) {
      textareaRef.value.value = ''
    }
  }
  currentSuggestions.value = []
  isLoading.value = true
  await scrollToBottom()

  try {
    const response = await api.post(
      '/ai/chat',
      {
        messages: messages.value.map((item) => ({
          role: item.role,
          content: item.content
        }))
      },
      {
        timeout: CHAT_REQUEST_TIMEOUT_MS
      }
    )

    if (response.data?.success && response.data?.reply) {
      messages.value.push({
        role: 'assistant',
        content: response.data.reply,
        intent: response.data.intent || 'general',
        cards: normalizeCards(response.data.cards || []),
        suggestedQuestions: response.data.suggestedQuestions || []
      })
      currentIntent.value = response.data.intent || 'general'
      currentSuggestions.value = [...(response.data.suggestedQuestions || [])]
    } else {
      throw new Error(response.data?.message || 'AI 服务未返回有效内容')
    }
  } catch (error) {
    console.error('AI 聊天失败:', error)
    if (error.code === 'ECONNABORTED') {
      errorMessage.value = `AI 请求超时（>${CHAT_REQUEST_TIMEOUT_MS / 1000} 秒），请检查后端或本地模型服务是否正常运行。`
    } else {
      errorMessage.value = error.response?.data?.message || error.message || 'AI 请求失败，请稍后重试。'
    }
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

const sendSuggestedQuestion = async (question) => {
  await sendMessage(question)
}

const handleInput = (event) => {
  inputText.value = typeof event?.target?.value === 'string' ? event.target.value : ''
}

const sendCurrentInput = async () => {
  const textareaValue = typeof textareaRef.value?.value === 'string' ? textareaRef.value.value : ''
  await sendMessage(textareaValue)
}

const handleTextareaEnter = async (event) => {
  if (event?.isComposing || isComposing.value) {
    return
  }
  await sendCurrentInput()
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.ai-page { min-height: 100vh; position: relative; overflow-x: hidden; background: #edf1f4; color: #172431; font-family: 'Manrope', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.background-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-image, .bg-overlay { position: absolute; inset: 0; }
.bg-image { background: url('https://images.unsplash.com/photo-1495344517868-8ebaf0a2044a?auto=format&fit=crop&w=2000&q=80') center/cover no-repeat; filter: grayscale(24%) saturate(0.75); }
.bg-overlay { background: linear-gradient(180deg, rgba(247, 248, 249, 0.76) 0%, rgba(238, 241, 244, 0.94) 42%, rgba(235, 239, 242, 0.98) 100%), linear-gradient(90deg, rgba(23, 36, 49, 0.06) 0, rgba(23, 36, 49, 0.06) 1px, transparent 1px, transparent 140px); }
.page-shell { position: relative; z-index: 2; width: min(1480px, calc(100% - 48px)); margin: 0 auto; padding: 196px 0 72px; }
.ai-page-header { display: grid; justify-items: center; gap: 10px; margin-bottom: 28px; text-align: center; }
.ai-page-header h1 { font-size: clamp(2.6rem, 4vw, 4.1rem); line-height: 1.14; color: #132838; font-weight: 600; }
.ai-page-header p { max-width: 58ch; font-size: 0.94rem; line-height: 1.76; color: rgba(23, 36, 49, 0.74); }
.chat-layout { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 18px; align-items: start; }
.control-panel, .chat-stage, .chat-error { border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(255, 255, 255, 0.74); }
.control-panel { display: grid; align-content: start; gap: 18px; padding: 20px; }
.panel-toggle { border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(255, 255, 255, 0.84); color: #172431; min-height: 48px; padding: 10px 14px; font-size: 0.9rem; cursor: pointer; text-align: left; }
.control-panel__content { display: grid; gap: 18px; }
.panel-kicker, .chat-label, .chat-bubble span { font-size: 0.82rem; letter-spacing: 0.03em; color: rgba(23, 36, 49, 0.6); }
.control-panel h1 { font-size: clamp(1.7rem, 2vw, 2rem); line-height: 1.2; color: #132838; font-weight: 600; }
.clear-btn, .send-btn { border: 1px solid rgba(23, 36, 49, 0.14); cursor: pointer; font: inherit; }
.clear-btn { min-height: 48px; padding: 12px 16px; background: rgba(255, 255, 255, 0.86); color: #172431; font-size: 0.9rem; font-weight: 600; }
.chat-stage { display: grid; grid-template-rows: auto minmax(0, 1fr) auto auto; height: clamp(780px, 82vh, 980px); overflow: hidden; }
.chat-header { padding: 22px 24px; border-bottom: 1px solid rgba(23, 36, 49, 0.1); }
.chat-header strong { display: block; margin-top: 6px; font-size: 1.06rem; font-weight: 600; }
.chat-body { min-height: 0; overflow-y: auto; padding: 24px; display: flex; flex-direction: column; gap: 16px; scroll-behavior: smooth; overscroll-behavior: contain; }
.chat-bubble { max-width: 82%; padding: 18px 20px; border: 1px solid rgba(23, 36, 49, 0.12); background: rgba(255, 255, 255, 0.9); }
.chat-bubble p { margin-top: 10px; font-size: 0.92rem; line-height: 1.76; font-family: inherit; }
.chat-bubble.user { margin-left: auto; background: rgba(228, 235, 242, 0.92); }
.reply-cards { display: grid; gap: 12px; margin-top: 16px; }
.reply-card { display: grid; grid-template-columns: 96px minmax(0, 1fr); padding: 0; overflow: hidden; border: 1px solid rgba(23, 36, 49, 0.12); background: rgba(244, 247, 250, 0.96); text-align: left; cursor: pointer; transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease; }
.reply-card:hover { transform: translateY(-2px); border-color: rgba(23, 36, 49, 0.28); box-shadow: 0 14px 28px rgba(23, 36, 49, 0.08); }
.reply-card__media { min-height: 100%; background: rgba(207, 218, 226, 0.88); }
.reply-card__media img { width: 100%; height: 100%; object-fit: cover; display: block; }
.reply-card__body { display: grid; gap: 8px; padding: 14px; }
.reply-card__body span, .reply-card__body em { font-style: normal; font-size: 0.76rem; letter-spacing: 0.03em; color: rgba(23, 36, 49, 0.58); }
.reply-card__body strong { font-size: 0.94rem; color: #132838; font-weight: 600; }
.reply-card__body p { margin-top: 0; font-family: 'Manrope', 'PingFang SC', sans-serif; font-size: 0.92rem; line-height: 1.65; color: rgba(23, 36, 49, 0.82); }
.chat-suggestions-panel { margin: 0 24px; padding: 14px 16px 16px; border: 1px solid rgba(23, 36, 49, 0.1); background: rgba(247, 249, 251, 0.92); }
.chat-suggestions-panel__topline { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; margin-bottom: 12px; }
.chat-suggestions-panel__topline span { font-size: 0.76rem; letter-spacing: 0.03em; color: rgba(23, 36, 49, 0.56); }
.chat-suggestions-panel__topline strong { font-size: 0.92rem; color: #153047; }
.chat-suggestions-panel__grid { display: flex; flex-wrap: wrap; gap: 10px; }
.suggestion-chip { border: 1px solid rgba(23, 36, 49, 0.12); background: rgba(233, 239, 244, 0.9); color: #153047; padding: 8px 12px; font: inherit; font-size: 0.9rem; line-height: 1.4; cursor: pointer; transition: background 0.2s ease, border-color 0.2s ease; }
.suggestion-chip:hover { background: rgba(223, 232, 238, 0.96); border-color: rgba(23, 36, 49, 0.26); }
.chat-error { margin: 0 24px; padding: 14px 16px; color: #9a4040; background: rgba(255, 243, 243, 0.86); }
.chat-form { display: grid; gap: 12px; padding: 20px 24px 24px; border-top: 1px solid rgba(23, 36, 49, 0.1); background: rgba(255, 255, 255, 0.92); }
.chat-input { width: 100%; resize: none; min-height: 124px; max-height: 180px; padding: 16px 18px; border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(247, 248, 249, 0.98); color: #172431; font: inherit; font-size: 0.9rem; line-height: 1.72; overflow-y: auto; }
.chat-input:focus { outline: 1px solid rgba(23, 36, 49, 0.22); }
.chat-actions { display: flex; justify-content: flex-end; }
.send-btn { min-height: 48px; padding: 12px 24px; background: #1f3d58; color: #fff; border-color: #1f3d58; font-size: 0.9rem; font-weight: 600; }
.clear-btn:disabled, .send-btn:disabled, .chat-input:disabled { opacity: 0.6; cursor: not-allowed; }
@media (max-width: 980px) { .chat-layout { grid-template-columns: 1fr; } .control-panel { padding: 18px; } }
@media (max-width: 720px) { .page-shell { width: calc(100% - 24px); padding: 176px 0 64px; } .control-panel, .chat-header, .chat-body, .chat-form { padding-left: 18px; padding-right: 18px; } .chat-error, .chat-suggestions-panel { margin-left: 18px; margin-right: 18px; } .chat-bubble { max-width: 100%; } .reply-card { grid-template-columns: 1fr; } .reply-card__media { min-height: 170px; } .chat-suggestions-panel__topline { flex-direction: column; align-items: flex-start; } .chat-stage { height: min(84vh, 980px); } .ai-page-header h1 { font-size: clamp(2rem, 9vw, 3rem); } }
</style>
