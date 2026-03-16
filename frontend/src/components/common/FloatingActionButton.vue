<template>
  <button
    class="fixed bottom-6 right-6 z-50 size-14 bg-primary text-white rounded-full shadow-2xl shadow-primary/30 flex items-center justify-center hover:bg-primary/90 hover:scale-110 active:scale-95 transition-all group"
    @click="handleClick"
    title="AI 助手"
  >
    <span class="material-symbols-outlined text-2xl group-hover:rotate-12 transition-transform">smart_toy</span>
    <!-- Pulse ring -->
    <span class="absolute inset-0 rounded-full bg-primary/20 animate-ping pointer-events-none"></span>
  </button>
</template>

<script setup lang="ts">
import { useChatDrawer } from '@/composables/useChatDrawer'

const { openChatDrawer } = useChatDrawer()

function handleClick() {
  openChatDrawer()
}
</script>
