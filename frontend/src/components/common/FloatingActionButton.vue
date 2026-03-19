<template>
  <button
    class="z-50 size-14 bg-primary text-white rounded-full shadow-2xl shadow-primary/30 flex items-center justify-center hover:bg-primary/90 hover:scale-110 active:scale-95 transition-all group cursor-grab active:cursor-grabbing touch-none fixed"
    :style="buttonStyle"
    @click="handleClick"
    @pointerdown="handlePointerDown"
    title="AI 助手"
  >
    <span class="material-symbols-outlined text-2xl group-hover:rotate-12 transition-transform">auto_awesome</span>
    <!-- Pulse ring -->
    <span class="absolute inset-0 rounded-full bg-primary/20 animate-ping pointer-events-none"></span>
  </button>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useChatDrawer } from '@/composables/useChatDrawer'

const { openChatDrawer } = useChatDrawer()

type Point = { x: number; y: number }

const STORAGE_KEY = 'wk_ai_crm:floating_ai_button_pos:v1'
const BUTTON_SIZE = 56 // tailwind size-14 => 3.5rem => 56px
const EDGE_PADDING = 20 // roughly match prototype constraints (20px)

const isDragging = ref(false)
const draggedDuringPointer = ref(false)
const dragStartPointer = ref<Point | null>(null)
const dragStartPos = ref<Point | null>(null)
const position = ref<Point | null>(null)

function clamp(n: number, min: number, max: number) {
  return Math.min(max, Math.max(min, n))
}

function clampToViewport(pos: Point): Point {
  const maxX = Math.max(EDGE_PADDING, window.innerWidth - BUTTON_SIZE - EDGE_PADDING)
  const maxY = Math.max(EDGE_PADDING, window.innerHeight - BUTTON_SIZE - EDGE_PADDING)
  return {
    x: clamp(pos.x, EDGE_PADDING, maxX),
    y: clamp(pos.y, EDGE_PADDING, maxY),
  }
}

function defaultPosition(): Point {
  // visually matches `bottom-6 right-6` (24px) while keeping constraints consistent
  const x = window.innerWidth - BUTTON_SIZE - 24
  const y = window.innerHeight - BUTTON_SIZE - 24
  return clampToViewport({ x, y })
}

function loadPosition(): Point {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return defaultPosition()
    const parsed = JSON.parse(raw) as Partial<Point>
    if (typeof parsed?.x !== 'number' || typeof parsed?.y !== 'number') return defaultPosition()
    return clampToViewport({ x: parsed.x, y: parsed.y })
  } catch {
    return defaultPosition()
  }
}

function savePosition(pos: Point) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(pos))
  } catch {
    // ignore (private mode / disabled storage)
  }
}

const buttonStyle = computed(() => {
  const pos = position.value
  if (!pos) return { right: '24px', bottom: '24px' }
  return { left: `${pos.x}px`, top: `${pos.y}px` }
})

function handleClick() {
  if (draggedDuringPointer.value) return
  openChatDrawer()
}

function handlePointerDown(e: PointerEvent) {
  // Left mouse button only; allow touch/pen.
  if (e.pointerType === 'mouse' && e.button !== 0) return

  ;(e.currentTarget as HTMLElement | null)?.setPointerCapture?.(e.pointerId)

  isDragging.value = true
  draggedDuringPointer.value = false
  dragStartPointer.value = { x: e.clientX, y: e.clientY }
  dragStartPos.value = position.value ?? defaultPosition()

  window.addEventListener('pointermove', handlePointerMove, { passive: false })
  window.addEventListener('pointerup', handlePointerUp, { passive: true })
  window.addEventListener('pointercancel', handlePointerUp, { passive: true })
}

function handlePointerMove(e: PointerEvent) {
  if (!isDragging.value || !dragStartPointer.value || !dragStartPos.value) return
  e.preventDefault()

  const dx = e.clientX - dragStartPointer.value.x
  const dy = e.clientY - dragStartPointer.value.y

  // Small threshold to avoid treating clicks as drags
  if (!draggedDuringPointer.value && (Math.abs(dx) > 3 || Math.abs(dy) > 3)) {
    draggedDuringPointer.value = true
  }

  const next = clampToViewport({
    x: dragStartPos.value.x + dx,
    y: dragStartPos.value.y + dy,
  })
  position.value = next
}

function handlePointerUp() {
  if (!isDragging.value) return
  isDragging.value = false

  if (position.value) savePosition(position.value)

  // Prevent click immediately after drag end
  if (draggedDuringPointer.value) {
    window.setTimeout(() => {
      draggedDuringPointer.value = false
    }, 100)
  }

  cleanupPointerListeners()
}

function cleanupPointerListeners() {
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  window.removeEventListener('pointercancel', handlePointerUp)
}

function handleResize() {
  if (!position.value) return
  const next = clampToViewport(position.value)
  position.value = next
  savePosition(next)
}

onMounted(() => {
  position.value = loadPosition()
  window.addEventListener('resize', handleResize, { passive: true })
})

onBeforeUnmount(() => {
  cleanupPointerListeners()
  window.removeEventListener('resize', handleResize)
})
</script>
