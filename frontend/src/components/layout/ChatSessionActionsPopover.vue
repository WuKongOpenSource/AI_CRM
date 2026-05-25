<template>
  <div class="flex min-w-0 w-full items-center gap-2" style="height: 24px !important">
    <span
      class="block min-w-0 flex-1 truncate text-sm leading-5 text-[#0d0d0d]"
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
          <button type="button" class="wk-chat-session-menu__item" @click="onPin">
            <span class="material-symbols-outlined wk-chat-session-menu__icon">vertical_align_top</span>
            <span class="wk-chat-session-menu__label">置顶</span>
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

const sessionMenuPopperOptions = {
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
          po.x += MENU_SHIFT_X_PX
        }
      },
    },
    { name: 'flip', enabled: false },
  ],
}

const props = defineProps<{
  session: ChatSession
  active: boolean
}>()

const emit = defineEmits<{
  share: [session: ChatSession]
  pin: [session: ChatSession]
  delete: [sessionId: string]
}>()

const menuVisible = ref(false)

const displayTitle = computed(() => props.session.title || '新对话')

const visibilityClass = computed(() =>
  props.active
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
  border:unset !important;
  /* border: 1px solid rgba(15, 23, 42, 0.08) !important; */
  background: #fff !important;
  box-shadow: 0 0 #0000, 0 0 #0000, 0 0 #0000, 0 0 #0000, 0px 8px 12px 0px #00000014, 0px 0px 1px 0px #0000009e !important;
  /* box-sizing: border-box; */
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
  color: #0d0d0d;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.wk-chat-session-menu__item:hover {
  background-color: #f9f9f9;
}

.wk-chat-session-menu__icon {
  flex-shrink: 0;
  font-size: 22px;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
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
  background-color: #FFE1E0;
}

.wk-chat-session-menu__divider {
  height: 1px;
  margin: 4px 6px;
  background: rgba(15, 23, 42, 0.08);
}
</style>
