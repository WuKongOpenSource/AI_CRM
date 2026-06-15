<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="wk-mobile-chat-top-header border-b px-3 py-2"
      :style="fixedStyle"
    >
      <div class="mx-auto flex h-9 w-full items-center justify-between gap-2">
        <button
          type="button"
          class="wk-mobile-customer-header-menu flex size-9 shrink-0 items-center justify-center rounded-full text-[#0d0d0d] transition-colors active:bg-[#f1f1f1]"
          aria-label="打开菜单"
          title="打开菜单"
          @click="emit('menu')"
        >
          <span class="material-symbols-outlined text-[22px] leading-none">menu</span>
        </button>
        <div class="flex min-w-0 flex-1 items-center gap-2">
          <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
            <img
              v-if="avatarUrl"
              :src="avatarUrl"
              :alt="title || avatarAlt"
              :class="avatarClass"
            />
            <span
              v-else-if="kind === 'product'"
              class="material-symbols-outlined text-[17px] leading-none text-slate-400"
            >
              inventory_2
            </span>
            <span
              v-else-if="kind === 'chat'"
              class="material-symbols-outlined text-[17px] leading-none text-primary"
            >
              auto_awesome
            </span>
            <span v-else class="text-xs font-bold text-slate-400">
              {{ title.charAt(0) || '?' }}
            </span>
          </div>
          <button
            type="button"
            class="min-w-[80px] max-w-[190px] truncate text-left text-[15px] font-semibold leading-5 text-[#0d0d0d]"
            :title="title"
            @click="emit('title')"
          >
            {{ title }}
          </button>
        </div>
        <button
          type="button"
          class="wk-mobile-customer-header-detail flex size-9 shrink-0 items-center justify-center rounded-full text-[#0d0d0d] transition-colors active:bg-[#f1f1f1]"
          :aria-label="detailable ? detailLabel : '新建会话'"
          :title="detailable ? detailLabel : '新建会话'"
          @click="detailable ? emit('detail') : emit('newSession')"
        >
          <span class="material-symbols-outlined text-[24px] leading-none">
            {{ detailable ? 'more_horiz' : 'edit_square' }}
          </span>
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  kind: 'chat' | 'customer' | 'employee' | 'relation' | 'product'
  title: string
  avatarUrl?: string
  fixedStyle?: Record<string, string> | undefined
  detailable?: boolean
}>()

const emit = defineEmits<{
  (e: 'menu'): void
  (e: 'title'): void
  (e: 'detail'): void
  (e: 'newSession'): void
}>()

const detailLabel = computed(() => {
  if (props.kind === 'employee') return '通讯录详情'
  if (props.kind === 'relation') return '关系详情'
  if (props.kind === 'product') return '产品详情'
  if (props.kind === 'customer') return '客户详情'
  return '对话详情'
})

const avatarAlt = computed(() => {
  if (props.kind === 'employee') return 'employee avatar'
  if (props.kind === 'relation') return 'relation avatar'
  if (props.kind === 'product') return 'product icon'
  return 'chat avatar'
})

const avatarClass = computed(() =>
  props.kind === 'customer' || props.kind === 'relation' || props.kind === 'product'
    ? 'size-full bg-white object-contain'
    : 'size-full bg-white object-cover'
)
</script>

<style scoped>
.wk-mobile-chat-top-header {
  position: fixed;
  top: 0;
  right: var(--safe-area-inset-right);
  left: var(--safe-area-inset-left);
  z-index: 90;
  padding-top: max(8px, var(--safe-area-inset-top));
  border-color: var(--wk-border-subtle);
  background: color-mix(in srgb, var(--wk-bg-surface) 92%, transparent);
  backdrop-filter: blur(14px);
}
</style>
