<template>
  <el-dialog
    :model-value="modelValue"
    width="420px"
    title="安全验证"
    destroy-on-close
    :close-on-click-modal="!loading && !checking"
    :close-on-press-escape="!loading && !checking"
    @close="handleClose"
  >
    <div class="space-y-4">
      <p class="text-sm text-slate-500">
        拖动滑块到拼图缺口处，验证通过后继续发送邮箱验证码。
      </p>

      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-3">
        <div class="relative overflow-hidden rounded-xl bg-slate-100">
          <img
            ref="backgroundRef"
            :src="backgroundImage"
            alt="验证码背景"
            class="block w-full select-none pointer-events-none"
            @load="measureImages"
          />
          <img
            v-if="puzzleImage"
            ref="puzzleRef"
            :src="puzzleImage"
            alt="滑块拼图"
            class="absolute inset-y-0 left-0 h-full max-w-none select-none pointer-events-none drop-shadow-md"
            :style="{ transform: `translateX(${puzzleLeft}px)` }"
            @load="measureImages"
          />
          <div
            v-if="loading || checking"
            class="absolute inset-0 flex items-center justify-center bg-white/70 text-sm text-slate-600"
          >
            {{ loading ? '正在加载验证码...' : '正在校验，请稍候...' }}
          </div>
        </div>
      </div>

      <div
        ref="trackRef"
        class="relative h-12 overflow-hidden rounded-full border border-slate-200 bg-slate-100 select-none"
      >
        <div
          class="absolute inset-y-0 left-0 rounded-full bg-primary/15 transition-all"
          :style="{ width: `${progressWidth}px` }"
        />
        <div class="absolute inset-0 flex items-center justify-center px-16 text-sm text-slate-500">
          {{ sliderTip }}
        </div>
        <button
          type="button"
          class="absolute left-0 top-1/2 flex h-12 w-[52px] -translate-y-1/2 items-center justify-center rounded-full bg-primary text-white shadow-lg shadow-primary/20 transition-transform active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none"
          :style="{ transform: `translate(${handleLeft}px, -50%)` }"
          :disabled="loading || checking || !hasCaptcha"
          @pointerdown.prevent="startDrag"
        >
          <span class="material-symbols-outlined text-xl">chevron_right</span>
        </button>
      </div>

      <div class="flex items-center justify-between text-xs text-slate-500">
        <span>看不清可以刷新验证码</span>
        <button
          type="button"
          class="font-medium text-primary transition-colors hover:text-primary/80 disabled:cursor-not-allowed disabled:text-slate-400"
          :disabled="loading || checking"
          @click="refreshCaptcha"
        >
          换一张
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { checkCaptcha, getCaptcha, type CaptchaData } from '@/api/auth'

const HANDLE_WIDTH = 52
const DEFAULT_POINT_Y = 5

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'verified', value: string): void
}>()

const backgroundRef = ref<HTMLImageElement>()
const puzzleRef = ref<HTMLImageElement>()
const trackRef = ref<HTMLDivElement>()

const loading = ref(false)
const checking = ref(false)
const dragging = ref(false)
const handleLeft = ref(0)
const dragStartX = ref(0)
const dragStartLeft = ref(0)
const backgroundWidth = ref(0)
const puzzleWidth = ref(0)

const captcha = reactive<CaptchaData>({
  originalImageBase64: '',
  jigsawImageBase64: '',
  token: '',
  secretKey: '',
  captchaType: 'blockPuzzle'
})

const hasCaptcha = computed(() => Boolean(captcha.token && captcha.originalImageBase64 && captcha.jigsawImageBase64))
const backgroundImage = computed(() => captcha.originalImageBase64 ? `data:image/png;base64,${captcha.originalImageBase64}` : '')
const puzzleImage = computed(() => captcha.jigsawImageBase64 ? `data:image/png;base64,${captcha.jigsawImageBase64}` : '')
const trackMax = computed(() => Math.max((trackRef.value?.clientWidth ?? 0) - HANDLE_WIDTH, 0))
const puzzleMax = computed(() => Math.max(backgroundWidth.value - puzzleWidth.value, 0))
const puzzleLeft = computed(() => {
  if (!trackMax.value || !puzzleMax.value) return 0
  return (handleLeft.value / trackMax.value) * puzzleMax.value
})
const progressWidth = computed(() => Math.min(handleLeft.value + HANDLE_WIDTH / 2, trackRef.value?.clientWidth ?? 0))
const sliderTip = computed(() => {
  if (loading.value) return '正在加载验证码'
  if (checking.value) return '正在校验，请稍候'
  if (!hasCaptcha.value) return '点击右侧刷新按钮加载验证码'
  if (dragging.value || handleLeft.value > 0) return '松开滑块开始校验'
  return '按住滑块并拖动到缺口位置'
})

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      await refreshCaptcha()
      return
    }
    resetSlider()
  }
)

function resetSlider() {
  handleLeft.value = 0
  dragging.value = false
}

function measureImages() {
  backgroundWidth.value = backgroundRef.value?.clientWidth ?? 0
  puzzleWidth.value = puzzleRef.value?.clientWidth ?? 0
}

async function refreshCaptcha() {
  resetSlider()
  loading.value = true
  try {
    const data = await getCaptcha()
    captcha.originalImageBase64 = data.originalImageBase64
    captcha.jigsawImageBase64 = data.jigsawImageBase64
    captcha.token = data.token
    captcha.secretKey = data.secretKey || ''
    captcha.captchaType = data.captchaType || 'blockPuzzle'
    await nextTick()
    measureImages()
  } finally {
    loading.value = false
  }
}

function startDrag(event: PointerEvent) {
  if (!hasCaptcha.value || loading.value || checking.value) return
  dragging.value = true
  dragStartX.value = event.clientX
  dragStartLeft.value = handleLeft.value
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

function onPointerMove(event: PointerEvent) {
  if (!dragging.value) return
  const deltaX = event.clientX - dragStartX.value
  const nextLeft = dragStartLeft.value + deltaX
  handleLeft.value = Math.min(Math.max(nextLeft, 0), trackMax.value)
}

async function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  removePointerListeners()

  if (!handleLeft.value) {
    return
  }
  await verifyCaptcha()
}

async function verifyCaptcha() {
  if (!captcha.token || checking.value) return
  checking.value = true
  try {
    const result = await checkCaptcha({
      token: captcha.token,
      pointX: Math.round(puzzleLeft.value),
      pointY: DEFAULT_POINT_Y,
      secretKey: captcha.secretKey,
      captchaType: captcha.captchaType || 'blockPuzzle'
    })
    emit('verified', result.captchaVerification)
    emit('update:modelValue', false)
  } catch (_error) {
    await refreshCaptcha()
  } finally {
    checking.value = false
  }
}

function handleClose() {
  removePointerListeners()
  emit('update:modelValue', false)
}

function removePointerListeners() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
}

onBeforeUnmount(() => {
  removePointerListeners()
})
</script>
