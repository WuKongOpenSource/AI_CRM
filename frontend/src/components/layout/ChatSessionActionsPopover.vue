<template>
  <div class="flex min-w-0 w-full items-center gap-2" style="height: 24px !important">
    <span
      v-if="isPinned"
      class="material-symbols-outlined wk-chat-session-pin-indicator"
      title="已置顶"
      aria-label="已置顶"
    >push_pin</span>
    <span
      class="block min-w-0 flex-1 truncate text-[1rem] leading-5 text-[#0d0d0d] md:text-sm"
      :title="displayTitle"
    >{{ displayTitle }}</span>
    <div class="shrink-0" @click.stop>
      <el-popover
        v-model:visible="menuVisible"
        trigger="click"
        placement="bottom-end"
        width="100"
        :offset="8"
        :teleported="true"
        :popper-options="sessionMenuPopperOptions"
        :show-arrow="false"
        popper-class="wk-chat-session-menu-popper"
      >
        <template #reference>
          <span
            class="material-symbols-outlined inline-flex size-7 shrink-0 items-center justify-center rounded-md text-[18px] leading-none text-[#8f8f8f] transition-all hover:text-[#0d0d0d]"
            :class="visibilityClass"
            tabindex="-1"
            role="button"
            :aria-label="`「${displayTitle}」更多操作`"
          >more_horiz</span>
        </template>
        <div class="wk-chat-session-menu">
          <button
            type="button"
            class="wk-chat-session-menu__item wk-chat-session-menu__item--pin"
            :class="{ 'wk-chat-session-menu__item--pinned': isPinned }"
            @click="onPin"
          >
            <span class="material-symbols-outlined wk-chat-session-menu__icon" :class="{ 'wk-chat-session-menu__icon--active': isPinned }">{{ pinIcon }}</span>
            <span class="wk-chat-session-menu__label">{{ pinActionLabel }}</span>
          </button>
          <!-- <button type="button" class="wk-chat-session-menu__item" @click="menuVisible = false; emit('share', session)">
            <span class="material-symbols-outlined wk-chat-session-menu__icon">upload</span>
            <span class="wk-chat-session-menu__label">分享</span>
          </button> -->
          <div class="wk-chat-session-menu__divider" role="separator" />
          <button type="button" class="wk-chat-session-menu__item wk-chat-session-menu__item--danger" @click="onDelete">
            <span class="material-symbols-outlined wk-chat-session-menu__icon">delete</span>
            <span class="wk-chat-session-menu__label">删除</span>
          </button>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ChatSession } from '@/types/common'

/** 在 popperOffsets 之后把气泡整体向右移（margin 对 Popper 的 transform 定位无效） */
const MENU_SHIFT_X_PX = 100

const props = defineProps<{
  session: ChatSession
  active: boolean
  alwaysVisible?: boolean
  menuShiftX?: number
}>()

const emit = defineEmits<{
  share: [session: ChatSession]
  pin: [session: ChatSession]
  delete: [sessionId: string]
}>()

const menuVisible = ref(false)

const displayTitle = computed(() => props.session.title || '新对话')

const menuShiftX = computed(() => props.menuShiftX ?? MENU_SHIFT_X_PX)

const sessionMenuPopperOptions = computed(() => ({
  strategy: 'fixed' as const,
  modifiers: [
    {
      name: 'wkChatSessionMenuShiftX',
      enabled: true,
      phase: 'main',
      requires: ['popperOffsets'],
      fn({ state }: { state: { modifiersData: Record<string, { x?: number; y?: number } | undefined> } }) {
        const po = state.modifiersData.popperOffsets
        if (po && typeof po.x === 'number') {
          po.x += menuShiftX.value
        }
      },
    },
    { name: 'flip', enabled: false },
  ],
}))
const isPinned = computed(() => Boolean(props.session.pinned))
const pinActionLabel = computed(() => isPinned.value ? '取消置顶' : '置顶')
const pinIcon = computed(() => isPinned.value ? 'vertical_align_bottom' : 'vertical_align_top')

const visibilityClass = computed(() =>
  props.alwaysVisible || props.active
    ? 'pointer-events-auto opacity-100'
    : 'pointer-events-none opacity-0 group-hover:pointer-events-auto group-hover:opacity-100'
)

function onPin() {
  menuVisible.value = false
  emit('pin', props.session)
}

function onDelete() {
  menuVisible.value = false
  emit('delete', props.session.sessionId)
}
</script>

<style>
.wk-chat-session-menu-popper.el-popper .el-popover__content {
  padding: 0 !important;
}

.wk-chat-session-menu-popper.el-popper {
  width: auto !important;
  min-width: 140px;
  max-width: min(260px, calc(100vw - 24px));
  padding: 6px !important;
  border-radius: 16px !important;
  border: 1px solid var(--wk-border-subtle) !important;
  background: var(--wk-bg-surface) !important;
  box-shadow: 0 8px 22px rgb(var(--wk-shadow-color) / 0.32), 0 0 0 1px rgb(0 0 0 / 0.08) !important;
}

.wk-chat-session-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.wk-chat-session-menu__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 6px 12px;
  margin: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 14px;
  line-height: 1.2;
  font-weight: 400;
  color: var(--wk-text-primary);
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.wk-chat-session-menu__item:hover {
  background-color: var(--wk-bg-surface-hover);
}

.wk-chat-session-menu__icon {
  flex-shrink: 0;
  font-size: 22px;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--wk-text-muted);
}

.wk-chat-session-menu__icon--active {
  color: var(--wk-text-primary);
}

.wk-chat-session-pin-indicator {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--wk-text-muted);
  font-size: 16px;
  line-height: 1;
}

.wk-chat-session-menu__label {
  flex: 1;
  min-width: 0;
}

.wk-chat-session-menu__item--danger,
.wk-chat-session-menu__item--danger .wk-chat-session-menu__icon {
  color: #C2403F;
}

.wk-chat-session-menu__item--danger:hover {
  background-color: color-mix(in srgb, #c2403f 16%, var(--wk-bg-surface));
}

.wk-chat-session-menu__divider {
  height: 1px;
  margin: 4px 6px;
  background: var(--wk-border-subtle);
}
</style>
