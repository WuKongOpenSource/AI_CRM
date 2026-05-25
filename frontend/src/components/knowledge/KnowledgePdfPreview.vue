<template>
  <div class="pdf-preview">
    <div v-if="loading" class="pdf-preview__loading">
      <span class="material-symbols-outlined animate-spin text-3xl text-slate-300">
        progress_activity
      </span>
    </div>

    <div v-if="previewUrl" class="pdf-preview__viewport">
      <iframe
        :src="previewUrl"
        class="pdf-preview__frame"
        title="PDF preview"
        @load="handleFrameLoaded"
        @error="handleFrameError"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{
  blob: Blob | null
}>()

const emit = defineEmits<{
  rendered: []
  error: [error: unknown]
}>()

const loading = ref(false)
const previewUrl = ref<string | null>(null)
let objectUrl: string | null = null

function clearPreviewUrl() {
  previewUrl.value = null

  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
    objectUrl = null
  }
}

function createNativePdfPreviewUrl(blob: Blob) {
  objectUrl = URL.createObjectURL(blob)
  return `${objectUrl}#toolbar=0&navpanes=0&pagemode=none&view=FitH`
}

function handleFrameLoaded() {
  loading.value = false
  emit('rendered')
}

function handleFrameError() {
  loading.value = false
  emit('error', new Error('PDF preview failed to load.'))
}

watch(
  () => props.blob,
  blob => {
    clearPreviewUrl()

    if (!blob) {
      loading.value = false
      return
    }

    loading.value = true
    previewUrl.value = createNativePdfPreviewUrl(blob)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearPreviewUrl()
})
</script>

<style scoped>
.pdf-preview {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100%;
  overflow: hidden;
  background: #fff;
}

.pdf-preview__loading {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.92);
}

.pdf-preview__viewport {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: #fff;
}

.pdf-preview__frame {
  position: absolute;
  inset-block: 0;
  left: -6px;
  display: block;
  width: calc(100% + 16px);
  max-width: none;
  height: 100%;
  min-height: 100%;
  border: 0;
  background: #fff;
  color-scheme: light;
}

@supports not (inset-block: 0) {
  .pdf-preview__frame {
    top: 0;
    bottom: 0;
  }
}

@media (max-width: 768px) {
  .pdf-preview__frame {
    left: -4px;
    width: calc(100% + 10px);
  }
}

/* Keep the in-browser PDF plugin from drawing an outline around the embed. */
.pdf-preview__frame:focus {
  outline: none;
}

/* Hidden fallback dimensions for older browser PDF plugins. */
.pdf-preview__frame[hidden] {
  width: 100%;
  height: 100%;
}
</style>
