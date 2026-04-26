<template>
  <div class="explore-toast-host">
    <article v-for="toast in toasts" :key="toast.id" class="toast">
      <div>
        <strong>{{ toast.title }}</strong>
        <p>{{ toast.text }}</p>
      </div>
      <button class="btn btn-dark toast-btn" type="button" @click="emit('dismiss', toast.id)">知道了</button>
    </article>
  </div>
</template>

<script setup>
defineProps({
  toasts: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['dismiss']);
</script>

<style scoped>
.toast {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(8, 24, 34, 0.92);
  color: #effff9;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.26);
}

.toast strong {
  display: block;
  margin-bottom: 4px;
}

.toast p {
  margin: 0;
  color: rgba(239, 255, 249, 0.78);
  font-size: 13px;
  line-height: 1.78;
}

.toast-btn {
  flex-shrink: 0;
}

@media (max-width: 760px) {
  .toast {
    flex-direction: column;
    border-radius: 22px;
  }
}
</style>
