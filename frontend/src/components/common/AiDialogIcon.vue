<template>
  <span
    class="wk-ai-dialog-icon"
    :style="{ width: `${props.size}px`, height: `${props.size}px` }"
    aria-hidden="true"
  >
    <span
      class="material-symbols-outlined wk-ai-dialog-icon__sparkle"
      :style="{ fontSize: `${props.sparkleSize}px` }"
    >
      auto_awesome
    </span>
  </span>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    size?: number
    sparkleSize?: number
  }>(),
  {
    size: 28,
    sparkleSize: 16,
  }
)
</script>

<style scoped>
.wk-ai-dialog-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #202020;
  color: #fff;
  box-shadow: 0 12px 30px rgb(15 23 42 / 0.14);
}

.wk-ai-dialog-icon__sparkle {
  line-height: 1;
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>
