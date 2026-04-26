<template>
  <div v-if="visible" class="dialog-mask" @click.self="emit('close')">
    <div class="dialog-card">
      <div class="dialog-head">
        <div class="dialog-title-wrap">
          <h2 class="dialog-title">路线定制</h2>
          <p>按照你的节奏、同行方式和兴趣，生成更贴合的游览路线。</p>
        </div>
        <button class="dialog-close" type="button" @click="emit('close')">×</button>
      </div>

      <section class="dialog-section">
        <h3>游玩时长</h3>
        <div class="option-list">
          <label class="option-item"><input v-model="form.duration" type="radio" value="short" />90 分钟左右</label>
          <label class="option-item"><input v-model="form.duration" type="radio" value="medium" />2 小时左右</label>
          <label class="option-item"><input v-model="form.duration" type="radio" value="half" />半天轻松游</label>
          <label class="option-item"><input v-model="form.duration" type="radio" value="full" />全天深度游</label>
        </div>
      </section>

      <section class="dialog-section">
        <h3>游览节奏</h3>
        <div class="option-list">
          <label class="option-item"><input v-model="form.pace" type="radio" value="relax" />慢慢散步</label>
          <label class="option-item"><input v-model="form.pace" type="radio" value="photo" />拍照优先</label>
          <label class="option-item"><input v-model="form.pace" type="radio" value="family" />亲子友好</label>
          <label class="option-item"><input v-model="form.pace" type="radio" value="learn" />科普观察</label>
        </div>
      </section>

      <section class="dialog-section">
        <h3>同行方式</h3>
        <div class="option-list">
          <label class="option-item"><input v-model="form.group" type="radio" value="solo" />一个人漫游</label>
          <label class="option-item"><input v-model="form.group" type="radio" value="pair" />朋友 / 情侣</label>
          <label class="option-item"><input v-model="form.group" type="radio" value="family" />带家人 / 孩子</label>
        </div>
      </section>

      <section class="dialog-section">
        <h3>感兴趣的内容</h3>
        <div class="option-list">
          <label class="option-item"><input v-model="form.interests" type="checkbox" value="animal" />动物观察</label>
          <label class="option-item"><input v-model="form.interests" type="checkbox" value="plant" />植物生态</label>
          <label class="option-item"><input v-model="form.interests" type="checkbox" value="photo" />拍照取景</label>
          <label class="option-item"><input v-model="form.interests" type="checkbox" value="collect" />图签收集</label>
          <label class="option-item"><input v-model="form.interests" type="checkbox" value="story" />科普故事</label>
        </div>
      </section>

      <div class="modal-footer">
        <button class="btn btn-dark" type="button" @click="emit('close')">取消</button>
        <button class="btn btn-primary" type="button" @click="emit('submit')">生成路线</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  form: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['close', 'submit']);
</script>

<style scoped>
:global(.route-drawer-enter-active),
:global(.route-drawer-leave-active) {
  transition: opacity 0.28s ease, transform 0.34s cubic-bezier(0.22, 1, 0.36, 1), filter 0.28s ease;
}

:global(.route-drawer-enter-from),
:global(.route-drawer-leave-to) {
  opacity: 0;
  transform: translateX(34px);
  filter: blur(10px);
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: stretch;
  justify-content: flex-end;
  padding: 14px 14px 14px 0;
  background:
    radial-gradient(circle at 12% 20%, rgba(var(--brand-soft-rgb), 0.2), transparent 36%),
    rgba(21, 28, 21, 0.58);
  backdrop-filter: blur(3px);
}

.dialog-card {
  width: min(420px, calc(100vw - 24px));
  height: calc(100vh - 28px);
  max-height: calc(100vh - 28px);
  overflow: auto;
  padding: 20px;
  border-radius: 26px;
  color: var(--text-light);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(252, 246, 235, 0.92)),
    radial-gradient(circle at top right, rgba(var(--brand-soft-rgb), 0.2), transparent 40%);
  border: 1px solid rgba(51, 63, 56, 0.16);
  box-shadow: 0 26px 60px rgba(18, 21, 17, 0.26);
}

.dialog-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-title-wrap p {
  margin: 8px 0 0;
  color: var(--muted-light);
  font-size: 12px;
  line-height: 1.7;
}

.dialog-title {
  margin: 0;
  line-height: 1.15;
  font-size: 30px;
}

.dialog-section {
  margin-top: 16px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(51, 63, 56, 0.12);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 8px 20px rgba(35, 38, 31, 0.07);
}

.dialog-section h3 {
  margin: 0 0 10px;
  font-size: 17px;
}

.option-list,
.modal-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.option-item {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid rgba(51, 63, 56, 0.16);
  background: rgba(255, 255, 255, 0.72);
  transition: transform 0.16s ease, border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-light);
}

.option-item:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--brand-rgb), 0.3);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 18px rgba(35, 69, 54, 0.1);
}

.option-item input {
  margin-right: 8px;
  accent-color: var(--brand);
}

.option-item:has(input:checked) {
  color: #fff9ef;
  border-color: rgba(var(--brand-rgb), 0.48);
  background: linear-gradient(135deg, rgba(var(--brand-rgb), 0.92), rgba(var(--brand-deep-rgb), 0.92));
  box-shadow: 0 10px 22px rgba(var(--brand-rgb), 0.28);
}

.dialog-close {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  color: var(--text-light);
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(51, 63, 56, 0.16);
}

.modal-footer {
  justify-content: flex-end;
  margin-top: 18px;
}

.modal-footer .btn-dark {
  color: var(--text-light);
  background: rgba(255, 255, 255, 0.78);
  border-color: rgba(51, 63, 56, 0.14);
}

@media (max-width: 760px) {
  .dialog-card {
    width: 100%;
    border-radius: 20px;
  }

  .dialog-mask {
    padding: 10px;
  }
}
</style>
