<template>
  <div ref="hostRef" class="pdf-preview">
    <div v-if="loading && !hasRendered" class="pdf-preview__loading">
      <span class="material-symbols-outlined animate-spin text-3xl text-slate-300">
        progress_activity
      </span>
    </div>
    <div ref="pagesRef" class="pdf-preview__pages" :class="{ 'pdf-preview__pages--hidden': loading && !hasRendered }" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getDocument, GlobalWorkerOptions } from 'pdfjs-dist'
import workerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url'

GlobalWorkerOptions.workerSrc = workerSrc

const props = defineProps<{
  blob: Blob | null
}>()

const emit = defineEmits<{
  rendered: []
  error: [error: unknown]
}>()

const hostRef = ref<HTMLElement | null>(null)
const pagesRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const hasRendered = ref(false)

const WIDTH_CHANGE_THRESHOLD = 2

let resizeObserver: ResizeObserver | null = null
let rerenderTimer: ReturnType<typeof setTimeout> | null = null
let renderGeneration = 0
let observedWidth = 0
let renderedWidth = 0
let queuedDocumentReload = false
let sourceBlob: Blob | null = null
let loadingTask: { destroy: () => Promise<void>; promise: Promise<unknown> } | null = null
let pdfDocument: { numPages: number; getPage: (pageNumber: number) => Promise<any>; destroy: () => Promise<void> } | null = null
let renderTasks: Array<{ cancel: () => void }> = []

function clearPendingRerender() {
  if (rerenderTimer) {
    clearTimeout(rerenderTimer)
    rerenderTimer = null
  }
}

function cancelRenderTasks() {
  for (const task of renderTasks) {
    try {
      task.cancel()
    } catch {
      // Ignore cancelled render tasks.
    }
  }
  renderTasks = []
}

function clearPages() {
  cancelRenderTasks()
  if (pagesRef.value) {
    pagesRef.value.replaceChildren()
  }
}

async function destroyDocument() {
  cancelRenderTasks()

  if (loadingTask) {
    try {
      await loadingTask.destroy()
    } catch {
      // Ignore teardown errors from stale loading tasks.
    }
    loadingTask = null
  }

  if (pdfDocument) {
    try {
      await pdfDocument.destroy()
    } catch {
      // Ignore teardown errors from stale documents.
    }
    pdfDocument = null
  }

  sourceBlob = null
  renderedWidth = 0
}

function scheduleRender(options: { reloadDocument?: boolean } = {}) {
  if (options.reloadDocument) {
    queuedDocumentReload = true
  }
  clearPendingRerender()
  rerenderTimer = setTimeout(() => {
    const reloadDocument = queuedDocumentReload
    queuedDocumentReload = false
    void renderPdf(reloadDocument)
  }, 120)
}

function getRenderPixelRatio() {
  const deviceRatio = window.devicePixelRatio || 1
  // Slight oversampling keeps PDFs crisper on 1x displays without exploding memory usage.
  return Math.min(Math.max(deviceRatio, 1.5), 2)
}

async function loadPdfDocument(blob: Blob, generation: number) {
  if (pdfDocument && sourceBlob === blob) {
    return pdfDocument
  }

  await destroyDocument()

  const data = new Uint8Array(await blob.arrayBuffer())
  if (generation !== renderGeneration) {
    return null
  }

  loadingTask = getDocument({
    data,
    useSystemFonts: true
  })

  const loadedDocument = await loadingTask.promise as {
    numPages: number
    getPage: (pageNumber: number) => Promise<any>
    destroy: () => Promise<void>
  }

  if (generation !== renderGeneration) {
    await loadedDocument.destroy()
    return null
  }

  pdfDocument = loadedDocument
  sourceBlob = blob
  return loadedDocument
}

async function renderPdf(reloadDocument = false) {
  const generation = ++renderGeneration
  clearPendingRerender()
  loading.value = true

  try {
    await nextTick()

    const blob = props.blob
    const host = hostRef.value
    const pagesContainer = pagesRef.value
    if (!blob || !host || !pagesContainer) {
      clearPages()
      hasRendered.value = false
      loading.value = false
      return
    }

    const containerWidth = Math.round(host.clientWidth)
    if (!containerWidth) {
      loading.value = false
      return
    }

    const documentChanged = sourceBlob !== blob
    const widthChanged = Math.abs(containerWidth - renderedWidth) >= WIDTH_CHANGE_THRESHOLD
    if (!reloadDocument && !documentChanged && hasRendered.value && !widthChanged) {
      loading.value = false
      return
    }

    const loadedDocument = await loadPdfDocument(blob, generation)
    if (!loadedDocument || generation !== renderGeneration || !pagesRef.value) {
      return
    }

    clearPages()
    const pixelRatio = getRenderPixelRatio()

    for (let pageNumber = 1; pageNumber <= loadedDocument.numPages; pageNumber += 1) {
      if (generation !== renderGeneration || !pagesRef.value) {
        return
      }

      const page = await loadedDocument.getPage(pageNumber)
      const baseViewport = page.getViewport({ scale: 1 })
      const cssScale = containerWidth / baseViewport.width
      const viewport = page.getViewport({ scale: cssScale })

      const pageElement = document.createElement('section')
      pageElement.className = 'pdf-preview__page'
      pageElement.dataset.pageNumber = String(pageNumber)
      pageElement.style.width = `${viewport.width}px`
      pageElement.style.height = `${viewport.height}px`

      const canvas = document.createElement('canvas')
      canvas.className = 'pdf-preview__canvas'
      canvas.width = Math.ceil(viewport.width * pixelRatio)
      canvas.height = Math.ceil(viewport.height * pixelRatio)
      canvas.style.width = `${viewport.width}px`
      canvas.style.height = `${viewport.height}px`

      const context = canvas.getContext('2d')
      if (!context) {
        throw new Error('Canvas 2D context is unavailable.')
      }

      pageElement.appendChild(canvas)
      pagesRef.value.appendChild(pageElement)

      const renderTask = page.render({
        canvasContext: context,
        viewport,
        transform: pixelRatio === 1 ? undefined : [pixelRatio, 0, 0, pixelRatio, 0, 0]
      })

      renderTasks.push(renderTask)

      await renderTask.promise
      page.cleanup()

      if (generation !== renderGeneration) {
        return
      }
    }

    if (generation !== renderGeneration) {
      return
    }

    renderedWidth = containerWidth
    observedWidth = containerWidth
    hasRendered.value = true
    emit('rendered')
  } catch (error) {
    if (generation === renderGeneration) {
      hasRendered.value = false
      emit('error', error)
    }
  } finally {
    if (generation === renderGeneration) {
      loading.value = false
      renderTasks = []
    }
  }
}

watch(
  () => props.blob,
  () => {
    hasRendered.value = false
    scheduleRender({ reloadDocument: true })
  },
  { immediate: true }
)

onMounted(() => {
  const host = hostRef.value
  if (!host) return

  observedWidth = Math.round(host.clientWidth)

  resizeObserver = new ResizeObserver(() => {
    const nextWidth = Math.round(host.clientWidth)
    if (!nextWidth || Math.abs(nextWidth - observedWidth) < WIDTH_CHANGE_THRESHOLD) {
      return
    }

    observedWidth = nextWidth
    if (hasRendered.value && !loading.value) {
      scheduleRender()
    }
  })
  resizeObserver.observe(host)
})

onBeforeUnmount(async () => {
  renderGeneration += 1
  clearPendingRerender()
  resizeObserver?.disconnect()
  resizeObserver = null
  clearPages()
  await destroyDocument()
})
</script>

<style scoped>
.pdf-preview {
  position: relative;
  min-height: 100%;
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

.pdf-preview__pages {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  align-items: center;
  gap: 0;
  background: #fff;
}

.pdf-preview__pages--hidden {
  visibility: hidden;
}

.pdf-preview__page {
  overflow: hidden;
  background: #fff;
}

.pdf-preview__canvas {
  display: block;
}
</style>
