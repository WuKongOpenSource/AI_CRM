<template>
  <button
    class="wk-floating-new-chat-button"
    :class="`wk-floating-new-chat-button--${placement}`"
    type="button"
    aria-label="AI对话"
    @click="emit('new-chat')"
  >
    <WkIcon name="new-chat" :size="22" class="wk-floating-new-chat-button__icon" />
    <span class="wk-floating-new-chat-button__tooltip" role="tooltip">AI对话</span>
  </button>
</template>

<script setup lang="ts">
import WkIcon from '@/components/common/WkIcon.vue'

withDefaults(defineProps<{
  placement?: 'viewport' | 'menu'
}>(), {
  placement: 'viewport'
})

const emit = defineEmits<{
  (e: 'new-chat'): void
}>()
</script>

<style scoped>
.wk-floating-new-chat-button {
  display: inline-flex;
  width: 56px;
  height: 56px;
  min-width: 56px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(255 255 255 / 0.12);
  border-radius: 9999px;
  background: #252525;
  color: #fff;
  box-shadow:
    0 18px 42px rgb(15 23 42 / 0.24),
    0 2px 8px rgb(15 23 42 / 0.18);
  cursor: pointer;
  padding: 0;
  transition:
    background-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.wk-floating-new-chat-button--viewport {
  position: fixed;
  right: max(24px, calc(18px + env(safe-area-inset-right)));
  bottom: max(24px, calc(18px + env(safe-area-inset-bottom)));
  z-index: 120;
}

.wk-floating-new-chat-button--menu {
  position: absolute;
  right: max(24px, calc(18px + env(safe-area-inset-right)));
  bottom: max(64px, calc(58px + env(safe-area-inset-bottom)));
  z-index: 20;
  box-shadow:
    0 10px 24px rgb(15 23 42 / 0.18),
    0 0 10px 2px rgb(15 23 42 / 0.18);
}

.wk-floating-new-chat-button:hover {
  background: #1f1f1f;
  box-shadow:
    0 20px 48px rgb(15 23 42 / 0.28),
    0 2px 10px rgb(15 23 42 / 0.2);
  transform: translateY(-1px);
}

.wk-floating-new-chat-button--menu:hover {
  box-shadow:
    0 12px 28px rgb(15 23 42 / 0.22),
    0 0 12px 3px rgb(15 23 42 / 0.2);
}

.wk-floating-new-chat-button:active {
  transform: translateY(1px) scale(0.98);
}

.wk-floating-new-chat-button__icon {
  flex-shrink: 0;
}

.wk-floating-new-chat-button__tooltip {
  pointer-events: none;
  position: absolute;
  right: 50%;
  bottom: calc(100% + 10px);
  transform: translateX(50%) translateY(4px);
  white-space: nowrap;
  border-radius: 8px;
  background: #000;
  padding: 6px 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
  opacity: 0;
  box-shadow: 0 8px 20px rgb(15 23 42 / 0.18);
  transition:
    opacity 150ms ease,
    transform 150ms ease;
}

.wk-floating-new-chat-button:hover .wk-floating-new-chat-button__tooltip,
.wk-floating-new-chat-button:focus-visible .wk-floating-new-chat-button__tooltip {
  opacity: 1;
  transform: translateX(50%) translateY(0);
}

@media (max-width: 640px) {
  .wk-floating-new-chat-button--viewport,
  .wk-floating-new-chat-button--menu {
    right: max(18px, calc(14px + env(safe-area-inset-right)));
  }

  .wk-floating-new-chat-button--viewport {
    bottom: max(20px, calc(16px + env(safe-area-inset-bottom)));
  }

  .wk-floating-new-chat-button--menu {
    bottom: max(60px, calc(56px + env(safe-area-inset-bottom)));
  }

  .wk-floating-new-chat-button {
    width: 52px;
    height: 52px;
    min-width: 52px;
  }
}
</style>
