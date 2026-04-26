<template>
  <div class="community-page">
    <div class="background-layer">
      <div class="bg-image"></div>
      <div class="bg-overlay"></div>
    </div>

    <SystemNav />

    <main class="page-shell">
      <section class="community-page-header">
        <h1>社区动态</h1>
        <p>浏览湿地观察者的分享、记录与讨论，让经验和见闻以更清晰的方式沉淀下来。</p>
      </section>

      <section class="community-toolbar">
        <div class="compose-panel">
          <button class="publish-btn" @click="showPublishModal = true">发布动态</button>
        </div>

        <div class="feed-column">
          <div class="community-search">
            <input
              v-model.trim="communityKeyword"
              type="text"
              class="community-search__input"
              placeholder="仅搜索社区帖子内容"
            />
            <span class="community-search__meta">{{ filteredPosts.length }} / {{ posts.length }} 条</span>
          </div>
        </div>
      </section>

      <section class="community-layout">
        <div class="feed-list">
          <article
            v-for="post in filteredPosts"
            :key="post.id"
            :ref="(el) => setPostRef(post.id, el)"
            class="post-card"
          >
            <div class="post-head">
              <div class="author-info">
                <el-avatar :size="42" :src="post.avatar" />
                <div class="author-meta">
                  <strong>{{ post.author }}</strong>
                  <span>{{ post.time }}</span>
                </div>
              </div>
              <span class="tag-chip" :style="{ '--chip-color': getTagColor(post.tag) }">{{ post.tag }}</span>
            </div>

            <div class="post-body">
              <h2>{{ post.title }}</h2>
              <p>{{ post.content }}</p>
              <div v-if="post.images.length" :class="['post-image-gallery', { 'is-single': post.images.length === 1 }]">
                <el-image
                  v-for="(image, index) in post.images"
                  :key="`${post.id}-${index}`"
                  :src="image"
                  fit="cover"
                  lazy
                  class="post-image"
                  :preview-src-list="post.images"
                  :initial-index="index"
                />
              </div>
            </div>

            <div class="post-actions">
              <button :class="['action-btn', { active: post.isLiked }]" @click="toggleLike(post)">
                <el-icon><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
                <span>{{ post.likes }}</span>
              </button>
              <button :class="['action-btn', { active: post.showComments }]" @click="toggleComments(post)">
                <el-icon><ChatLineRound /></el-icon>
                <span>{{ post.comments.length }}</span>
              </button>
              <button class="action-btn">
                <el-icon><Share /></el-icon>
                <span>分享</span>
              </button>
            </div>

            <div v-show="post.showComments" class="comments-panel">
              <div class="comments-list">
                <div v-for="(comment, index) in post.comments" :key="index" class="comment-item">
                  <strong>{{ comment.user }}</strong>
                  <p>{{ comment.text }}</p>
                </div>
              </div>
              <div class="comment-input-box">
                <el-input
                  v-model="post.newComment"
                  placeholder="写评论"
                  class="plain-input"
                  @keyup.enter="submitComment(post)"
                />
                <button class="send-btn" @click="submitComment(post)">发送</button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="posts.length > 0 && filteredPosts.length === 0" class="empty-search-state">
          没有找到包含该关键词的帖子内容。
        </div>
      </section>
    </main>

    <transition name="fade">
      <div v-if="showPublishModal" class="custom-modal-overlay">
        <div class="custom-modal">
          <div class="modal-header">
            <h2>发布动态</h2>
            <el-icon class="close-btn" @click="showPublishModal = false"><Close /></el-icon>
          </div>

          <div class="modal-body">
            <el-input v-model="newPost.title" placeholder="标题" class="plain-input mb-4" />
            <el-input
              v-model="newPost.content"
              type="textarea"
              :rows="5"
              placeholder="内容"
              class="plain-input mb-4"
            />

            <div class="image-selector mb-4">
              <div class="image-selector__head">
                <span>动态图片</span>
                <button v-if="imagePreviewUrl" class="image-selector__clear" @click="clearSelectedImage">移除图片</button>
              </div>
              <label class="image-selector__picker">
                <input type="file" accept="image/*" class="image-selector__input" @change="handleImageChange" />
                <span>{{ imagePreviewUrl ? '重新选择图片' : '选择一张图片' }}</span>
              </label>
              <div v-if="selectedImageName" class="image-selector__name">{{ selectedImageName }}</div>
              <div v-if="imagePreviewUrl" class="image-selector__preview">
                <img :src="imagePreviewUrl" alt="帖子预览图片" />
              </div>
            </div>

            <div class="tag-selector">
              <span>标签</span>
              <el-radio-group v-model="newPost.tag" size="small">
                <el-radio-button label="徒步">徒步</el-radio-button>
                <el-radio-button label="摄影">摄影</el-radio-button>
                <el-radio-button label="科普">科普</el-radio-button>
                <el-radio-button label="求助">求助</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="modal-footer">
            <button class="ghost-btn" @click="showPublishModal = false">取消</button>
            <button class="publish-confirm-btn" @click="publishPost">
              发布
              <el-icon><Position /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import SystemNav from '../components/SystemNav.vue'
import { ref, reactive, onMounted, computed, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'
import {
  Star,
  StarFilled,
  ChatLineRound,
  Share,
  Close,
  Position
} from '@element-plus/icons-vue'

const route = useRoute()

const getTagColor = (tag) => {
  const colors = {
    徒步: 'rgba(64, 106, 143, 0.88)',
    摄影: 'rgba(61, 82, 96, 0.88)',
    科普: 'rgba(68, 128, 101, 0.88)',
    求助: 'rgba(177, 122, 60, 0.88)'
  }
  return colors[tag] || 'rgba(61,82,96,0.8)'
}

const posts = ref([])
const postElementRefs = ref({})
const communityKeyword = ref('')

const filteredPosts = computed(() => {
  const keyword = communityKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return posts.value
  }

  return posts.value.filter((post) => String(post.content || '').toLowerCase().includes(keyword))
})

const normalizePostImages = (post) => {
  const candidates = Array.isArray(post?.images) ? post.images : []
  const fallback = post?.image ? [post.image] : []
  return [...candidates, ...fallback]
    .map((item) => resolveAssetUrl(item, item || ''))
    .filter((item, index, list) => item && list.indexOf(item) === index)
}

const setPostRef = (postId, el) => {
  if (!postId) return
  if (el) {
    postElementRefs.value[postId] = el
    return
  }
  delete postElementRefs.value[postId]
}

const scrollToPost = async (postId) => {
  if (!postId) return
  await nextTick()
  const target = postElementRefs.value[postId]
  if (target && typeof target.scrollIntoView === 'function') {
    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

const openPostFromQuery = async () => {
  const rawPostId = route.query.postId
  const postId = Number(Array.isArray(rawPostId) ? rawPostId[0] : rawPostId)
  if (!Number.isFinite(postId)) {
    return
  }

  const targetPost = posts.value.find((post) => post.id === postId)
  if (!targetPost) {
    return
  }

  if (!targetPost.showComments) {
    await toggleComments(targetPost)
  }
  await scrollToPost(targetPost.id)
}

const loadPosts = async () => {
  try {
    const response = await api.get('/posts')
    if (response.data.success) {
      posts.value = response.data.posts.map((post) => ({
        ...post,
        images: normalizePostImages(post),
        image: normalizePostImages(post)[0] || '',
        showComments: false,
        newComment: '',
        comments: post.comments || []
      }))
      await openPostFromQuery()
    }
  } catch (error) {
    console.error('获取帖子列表失败:', error)
  }
}

const toggleLike = async (post) => {
  try {
    const response = await api.post(`/posts/${post.id}/likes`)
    if (response.data.success) {
      post.isLiked = true
      if (response.data.post) {
        post.likes = response.data.post.likes
      } else {
        post.likes += 1
      }
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

const toggleComments = async (post) => {
  post.showComments = !post.showComments
  if (post.showComments && post.comments.length === 0) {
    try {
      const response = await api.get(`/posts/${post.id}`)
      if (response.data.success) {
        post.images = normalizePostImages(response.data.post)
        post.image = post.images[0] || ''
        post.comments = response.data.post.comments
      }
    } catch (error) {
      console.error('获取评论失败:', error)
    }
  }
}

const submitComment = async (post) => {
  if (!post.newComment.trim()) return

  try {
    const response = await api.post(`/posts/${post.id}/comments`, {
      content: post.newComment
    })

    if (response.data.success) {
      const detailResponse = await api.get(`/posts/${post.id}`)
      if (detailResponse.data.success) {
        post.images = normalizePostImages(detailResponse.data.post)
        post.image = post.images[0] || ''
        post.comments = detailResponse.data.post.comments
        post.newComment = ''
        ElMessage.success('评论发送成功')
      }
    }
  } catch (error) {
    console.error('发表评论失败:', error)
    ElMessage.error('评论失败，请重试')
  }
}

const showPublishModal = ref(false)
const newPost = reactive({ title: '', content: '', tag: '徒步' })
const selectedImageFile = ref(null)
const imagePreviewUrl = ref('')
const selectedImageName = ref('')

const revokePreviewUrl = () => {
  if (imagePreviewUrl.value && imagePreviewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreviewUrl.value)
  }
}

const clearSelectedImage = () => {
  revokePreviewUrl()
  selectedImageFile.value = null
  imagePreviewUrl.value = ''
  selectedImageName.value = ''
}

const handleImageChange = (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    clearSelectedImage()
    return
  }

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    event.target.value = ''
    clearSelectedImage()
    return
  }

  revokePreviewUrl()
  selectedImageFile.value = file
  selectedImageName.value = file.name
  imagePreviewUrl.value = URL.createObjectURL(file)
}

const publishPost = async () => {
  if (!newPost.title || !newPost.content) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  try {
    const formData = new FormData()
    formData.append('title', newPost.title)
    formData.append('content', newPost.content)
    formData.append('tag', newPost.tag)
    if (selectedImageFile.value) {
      formData.append('image', selectedImageFile.value)
    }

    const response = await api.post('/posts', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (response.data.success) {
      await loadPosts()
      newPost.title = ''
      newPost.content = ''
      newPost.tag = '徒步'
      clearSelectedImage()
      showPublishModal.value = false
      ElMessage.success('动态发布成功')
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    console.error('发布帖子失败:', error)
    ElMessage.error('发布失败，请重试')
  }
}

watch(
  () => route.query.postId,
  async () => {
    await openPostFromQuery()
  }
)

onMounted(loadPosts)
onBeforeUnmount(() => {
  revokePreviewUrl()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
.community-page { min-height: 100vh; position: relative; background: #edf1f4; overflow-x: hidden; color: #172431; font-family: 'Manrope', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.background-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
.bg-image, .bg-overlay { position: absolute; inset: 0; }
.bg-image { background: url('https://picsum.photos/id/1050/1920/1080') center/cover no-repeat; filter: grayscale(34%) saturate(0.72); }
.bg-overlay { background: linear-gradient(180deg, rgba(247, 248, 249, 0.76) 0%, rgba(238, 241, 244, 0.94) 44%, rgba(235, 239, 242, 0.98) 100%), linear-gradient(90deg, rgba(23, 36, 49, 0.06) 0, rgba(23, 36, 49, 0.06) 1px, transparent 1px, transparent 140px); }
.page-shell { position: relative; z-index: 2; width: min(1480px, calc(100% - 48px)); margin: 0 auto; padding: 196px 0 72px; }
.community-page-header { display: grid; justify-items: center; gap: 10px; margin-bottom: 28px; text-align: center; }
.community-page-header h1 { font-size: clamp(2.6rem, 4vw, 4.1rem); line-height: 1.14; color: #172d40; font-weight: 600; }
.community-page-header p { max-width: 58ch; font-size: 0.94rem; line-height: 1.76; color: rgba(23, 36, 49, 0.74); }
.community-toolbar,
.community-layout { display: grid; gap: 18px; align-items: start; }
.compose-panel, .feed-column, .post-card, .custom-modal, .empty-search-state { border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(255, 255, 255, 0.74); }
.community-toolbar { grid-template-columns: 220px minmax(0, 1fr); margin-bottom: 20px; }
.community-layout { grid-template-columns: 1fr; }
.compose-panel { display: grid; align-content: center; padding: 18px; }
.publish-btn, .send-btn, .ghost-btn, .publish-confirm-btn { border: 1px solid rgba(23, 36, 49, 0.14); cursor: pointer; font: inherit; }
.publish-btn, .publish-confirm-btn { min-height: 52px; padding: 12px 18px; background: #1f3d58; border-color: #1f3d58; color: #fff; font-size: 0.9rem; font-weight: 600; }
.feed-column { display: grid; gap: 0; padding: 18px; }
.community-search { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; align-items: center; }
.community-search__input { width: 100%; min-height: 52px; padding: 14px 16px; border: 1px solid rgba(23, 36, 49, 0.14); background: rgba(255, 255, 255, 0.82); color: #172431; font-size: 0.9rem; font: inherit; }
.community-search__meta { color: rgba(23, 36, 49, 0.6); font-size: 0.84rem; white-space: nowrap; }
.feed-list { display: grid; grid-template-columns: 1fr; gap: 18px; }
.post-card { padding: 20px; }
.post-head { display: flex; justify-content: space-between; gap: 18px; align-items: start; margin-bottom: 20px; }
.author-info { display: flex; align-items: center; gap: 12px; }
.author-meta { display: flex; flex-direction: column; gap: 4px; }
.author-meta strong { font-size: 0.94rem; font-weight: 600; }
.author-meta span { color: rgba(23, 36, 49, 0.6); font-size: 0.82rem; }
.tag-chip { color: var(--chip-color); border: 1px solid color-mix(in srgb, var(--chip-color) 55%, transparent); padding: 6px 12px; font-size: 0.72rem; font-weight: 600; letter-spacing: 0.03em; }
.post-body h2 { font-size: clamp(1.24rem, 1.55vw, 1.56rem); line-height: 1.32; margin-bottom: 12px; font-weight: 600; }
.post-body p { color: rgba(23, 36, 49, 0.78); line-height: 1.76; font-size: 0.9rem; }
.post-image-gallery { margin-top: 18px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.post-image-gallery.is-single { grid-template-columns: 1fr; }
.post-image { width: 100%; max-height: 320px; display: block; border: 1px solid rgba(23, 36, 49, 0.1); overflow: hidden; }
:deep(.el-image__inner) { object-fit: cover !important; }
.post-actions { margin-top: 18px; display: flex; gap: 18px; padding-top: 16px; border-top: 1px solid rgba(23, 36, 49, 0.1); }
.action-btn { background: transparent; border: none; color: rgba(23, 36, 49, 0.64); display: inline-flex; gap: 8px; align-items: center; cursor: pointer; font-size: 0.86rem; }
.action-btn.active, .action-btn:hover { color: #172431; }
.comments-panel { margin-top: 18px; padding-top: 18px; border-top: 1px solid rgba(23, 36, 49, 0.1); }
.comments-list { display: flex; flex-direction: column; gap: 12px; }
.comment-item { padding: 14px 16px; border: 1px solid rgba(23, 36, 49, 0.1); background: rgba(255, 255, 255, 0.5); }
.comment-item strong { display: block; margin-bottom: 6px; font-size: 0.9rem; }
.comment-item p { color: rgba(23, 36, 49, 0.72); line-height: 1.7; font-size: 0.92rem; }
.comment-input-box { display: flex; gap: 12px; margin-top: 14px; }
.empty-search-state { padding: 18px 20px; color: rgba(23, 36, 49, 0.64); }
.send-btn, .ghost-btn { padding: 0 18px; background: rgba(255, 255, 255, 0.9); color: #172431; }
.custom-modal-overlay { position: fixed; inset: 0; z-index: 40; display: flex; justify-content: center; align-items: center; background: rgba(227, 234, 244, 0.62); backdrop-filter: blur(12px); }
.custom-modal { width: min(620px, calc(100vw - 32px)); padding: 28px; }
.modal-header { display: flex; justify-content: space-between; gap: 20px; align-items: start; margin-bottom: 24px; }
.modal-header h2 { font-size: 1.8rem; font-weight: 600; }
.close-btn { font-size: 1.4rem; color: rgba(23, 36, 49, 0.6); cursor: pointer; }
.modal-body { margin-bottom: 24px; }
.image-selector { display: grid; gap: 10px; }
.image-selector__head { display: flex; justify-content: space-between; gap: 12px; align-items: center; color: rgba(23, 36, 49, 0.74); font-size: 0.9rem; }
.image-selector__clear { border: none; background: transparent; color: #1f3d58; cursor: pointer; font: inherit; }
.image-selector__picker { min-height: 46px; padding: 0 14px; border: 1px dashed rgba(23, 36, 49, 0.18); background: rgba(255, 255, 255, 0.82); display: inline-flex; align-items: center; justify-content: center; cursor: pointer; color: #1f3d58; }
.image-selector__input { display: none; }
.image-selector__name { color: rgba(23, 36, 49, 0.58); font-size: 0.84rem; }
.image-selector__preview { border: 1px solid rgba(23, 36, 49, 0.12); background: rgba(255, 255, 255, 0.9); overflow: hidden; }
.image-selector__preview img { width: 100%; max-height: 280px; object-fit: cover; display: block; }
.tag-selector { display: flex; flex-direction: column; gap: 14px; color: rgba(23, 36, 49, 0.74); font-size: 0.9rem; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding-top: 18px; border-top: 1px solid rgba(23, 36, 49, 0.1); }
.mb-4 { margin-bottom: 16px; }
:deep(.plain-input .el-input__wrapper) { background: rgba(255, 255, 255, 0.92); box-shadow: 0 0 0 1px rgba(23, 36, 49, 0.12) inset; border-radius: 0; }
:deep(.plain-input .el-input__inner), :deep(.plain-input .el-textarea__inner) { color: #172431; }
:deep(.plain-input .el-textarea__inner) { background: rgba(255, 255, 255, 0.92); box-shadow: 0 0 0 1px rgba(23, 36, 49, 0.12) inset; border-radius: 0; resize: none; }
:deep(.el-radio-button__inner) { background: rgba(255, 255, 255, 0.9) !important; color: #172431 !important; border: 1px solid rgba(23, 36, 49, 0.12) !important; border-radius: 0 !important; box-shadow: none !important; }
:deep(.el-radio-button.is-active .el-radio-button__inner) { background: rgba(31, 61, 88, 0.1) !important; color: #1f3d58 !important; border-color: rgba(31, 61, 88, 0.24) !important; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
@media (max-width: 980px) { .community-toolbar { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-shell { width: calc(100% - 24px); padding: 176px 0 64px; } .compose-panel, .feed-column, .post-card, .custom-modal { padding: 18px; } .post-head, .comment-input-box, .modal-footer, .community-search { flex-direction: column; } .community-search { grid-template-columns: 1fr; } .send-btn, .ghost-btn { padding: 12px 18px; } .community-page-header h1 { font-size: clamp(2rem, 9vw, 3rem); } }
</style>
