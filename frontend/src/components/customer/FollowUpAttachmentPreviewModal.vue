<template>
  <Teleport to="body">
    <Transition name="follow-up-preview-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[309] bg-slate-950/55 backdrop-blur-sm"
        @click="close"
      />
    </Transition>

    <Transition name="follow-up-preview-scale">
      <div
        v-if="modelValue && attachment"
        class="fixed inset-0 z-[310] flex items-center justify-center p-3 sm:p-6"
      >
        <section
          :class="[
            'flex w-full overflow-hidden bg-white shadow-2xl',
            isMobile ? 'h-full rounded-none' : 'h-[86vh] max-w-5xl rounded-2xl'
          ]"
          @click.stop
        >
          <div class="flex min-w-0 flex-1 flex-col">
            <header class="flex shrink-0 items-center justify-between gap-4 border-b border-slate-200 px-4 py-3 sm:px-5">
              <div class="flex min-w-0 items-center gap-3">
                <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <span class="material-symbols-outlined text-[18px]">{{ previewIcon }}</span>
                </div>
                <div class="min-w-0">
                  <h3 class="truncate text-sm font-bold text-slate-900">{{ attachment.fileName || '附件预览' }}</h3>
                  <p class="mt-0.5 text-xs text-slate-400">{{ formatFileSize(attachment.fileSize) }}</p>
                </div>
              </div>

              <div class="flex shrink-0 items-center gap-1.5">
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-primary"
                  title="下载附件"
                  @click="handleDownload"
                >
                  <span class="material-symbols-outlined text-[18px]">download</span>
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-800"
                  title="关闭"
                  @click="close"
                >
                  <span class="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>
            </header>

            <main class="relative min-h-0 flex-1 overflow-hidden bg-slate-50">
              <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-white">
                <span class="material-symbols-outlined animate-spin text-3xl text-slate-300">progress_activity</span>
              </div>

              <div v-else-if="errorMessage" class="flex h-full items-center justify-center p-6">
                <div class="max-w-sm text-center">
                  <div class="mx-auto mb-3 flex size-12 items-center justify-center rounded-xl bg-amber-50 text-amber-500">
                    <span class="material-symbols-outlined text-2xl">info</span>
                  </div>
                  <p class="text-sm font-medium text-slate-700">{{ errorMessage }}</p>
                  <button
                    type="button"
                    class="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary/90"
                    @click="handleDownload"
                  >
                    <span class="material-symbols-outlined text-[17px]">download</span>
                    下载附件
                  </button>
                </div>
              </div>

              <div v-else-if="previewKind === 'image'" class="flex h-full items-center justify-center overflow-auto bg-slate-950 p-4">
                <img
                  :src="previewUrl || undefined"
                  :alt="attachment.fileName"
                  class="max-h-full max-w-full rounded-lg object-contain"
                >
              </div>

              <div v-else-if="previewKind === 'audio'" class="flex h-full items-center justify-center bg-slate-950 p-6">
                <audio :src="previewUrl || undefined" controls class="w-full max-w-2xl" />
              </div>

              <div v-else-if="previewKind === 'video'" class="flex h-full items-center justify-center bg-slate-950 p-4">
                <video :src="previewUrl || undefined" controls class="max-h-full max-w-full rounded-lg" />
              </div>

              <iframe
                v-else-if="previewKind === 'pdf'"
                :src="previewUrl || undefined"
                title="附件预览"
                class="h-full w-full border-0 bg-white"
              />

              <div v-else-if="previewKind === 'text'" class="h-full overflow-auto bg-white p-5">
                <pre class="whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-700">{{ previewText }}</pre>
              </div>

              <div v-else class="flex h-full items-center justify-center p-6">
                <div class="max-w-sm text-center text-slate-500">
                  <span class="material-symbols-outlined mb-2 text-4xl text-slate-300">description</span>
                  <p class="text-sm font-medium">当前附件暂不支持直接预览</p>
                  <button
                    type="button"
                    class="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary/90"
                    @click="handleDownload"
                  >
                    <span class="material-symbols-outlined text-[17px]">download</span>
                    下载附件
                  </button>
                </div>
              </div>
            </main>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadFollowUpAttachment, getFollowUpAttachmentBlob } from '@/api/followup'
import { useResponsive } from '@/composables/useResponsive'
import type { FollowUpAttachment } from '@/types/customer'

type PreviewKind = 'image' | 'audio' | 'video' | 'pdf' | 'text' | 'unsupported'

const TEXT_PREVIEW_MAX_BYTES = 1024 * 1024 * 2

const props = defineProps<{
  modelValue: boolean
  attachment: FollowUpAttachment | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { isMobile } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')
const previewUrl = ref('')
const previewText = ref('')
const previewKind = ref<PreviewKind>('unsupported')

const previewIcon = computed(() => {
  switch (previewKind.value) {
    case 'image':
      return 'image'
    case 'audio':
      return 'audio_file'
    case 'video':
      return 'movie'
    case 'pdf':
      return 'picture_as_pdf'
    case 'text':
      return 'article'
    default:
      return 'attach_file'
  }
})

watch(
  () => [props.modelValue, props.attachment?.attachmentId],
  () => {
    if (!props.modelValue || !props.attachment) {
      resetPreview()
      return
    }
    void loadPreview(props.attachment)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  resetPreview()
})

function close() {
  emit('update:modelValue', false)
}

async function handleDownload() {
  if (!props.attachment) return
  try {
    await downloadFollowUpAttachment(props.attachment.attachmentId, props.attachment.fileName)
  } catch (error) {
    console.error('Download follow-up attachment failed:', error)
    ElMessage.error('附件下载失败')
  }
}

async function loadPreview(attachment: FollowUpAttachment) {
  resetPreview()
  previewKind.value = resolvePreviewKind(attachment)
  if (previewKind.value === 'unsupported') {
    errorMessage.value = '当前附件类型暂不支持直接预览，请下载后查看。'
    return
  }

  loading.value = true
  try {
    const blob = await getFollowUpAttachmentBlob(attachment.attachmentId)
    if (previewKind.value === 'text') {
      if (blob.size > TEXT_PREVIEW_MAX_BYTES) {
        errorMessage.value = '文本附件较大，请下载后查看。'
        return
      }
      previewText.value = await blob.text()
      return
    }
    previewUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    console.error('Preview follow-up attachment failed:', error)
    errorMessage.value = '附件预览失败，请下载后查看。'
  } finally {
    loading.value = false
  }
}

function resetPreview() {
  loading.value = false
  errorMessage.value = ''
  previewText.value = ''
  previewKind.value = 'unsupported'
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

function resolvePreviewKind(attachment: FollowUpAttachment): PreviewKind {
  const mimeType = (attachment.mimeType || '').toLowerCase()
  const extension = getFileExtension(attachment.fileName)

  if (mimeType.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'].includes(extension)) {
    return 'image'
  }
  if (mimeType.startsWith('audio/') || ['mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac'].includes(extension)) {
    return 'audio'
  }
  if (mimeType.startsWith('video/') || ['mp4', 'webm', 'mov', 'm4v', 'ogg'].includes(extension)) {
    return 'video'
  }
  if (mimeType === 'application/pdf' || extension === 'pdf') {
    return 'pdf'
  }
  if (
    mimeType.startsWith('text/') ||
    ['txt', 'md', 'csv', 'log', 'json', 'xml', 'yaml', 'yml'].includes(extension)
  ) {
    return 'text'
  }
  return 'unsupported'
}

function getFileExtension(fileName?: string): string {
  if (!fileName) return ''
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.slice(index + 1).toLowerCase() : ''
}

function formatFileSize(size?: number): string {
  if (!size || size <= 0) return '未知大小'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.follow-up-preview-fade-enter-active,
.follow-up-preview-fade-leave-active,
.follow-up-preview-scale-enter-active,
.follow-up-preview-scale-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.follow-up-preview-fade-enter-from,
.follow-up-preview-fade-leave-to {
  opacity: 0;
}

.follow-up-preview-scale-enter-from,
.follow-up-preview-scale-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.985);
}
</style>
