<template>
  <div class="relative inline-flex">
    <button
      type="button"
      class="group relative flex shrink-0 items-center justify-center overflow-hidden rounded-xl border border-slate-300 bg-slate-100 text-slate-400 shadow-sm transition-all hover:border-primary hover:bg-primary/5 focus:outline-none focus:ring-2 focus:ring-primary/30 disabled:cursor-not-allowed disabled:opacity-70"
      :style="logoStyle"
      :disabled="disabled || uploading"
      @click="triggerFileSelect"
    >
      <img
        v-if="logoUrl"
        :src="logoUrl"
        :alt="alt"
        class="h-full w-full bg-white object-contain"
      />
      <div v-else class="flex h-full w-full flex-col items-center justify-center">
        <span class="material-symbols-outlined text-[22px] leading-none">image</span>
        <span class="mt-0.5 text-[11px] font-bold leading-none">Logo</span>
      </div>

      <span
        v-if="uploading"
        class="absolute inset-0 flex items-center justify-center bg-white/75 text-primary"
      >
        <span class="material-symbols-outlined animate-spin text-[22px] leading-none">progress_activity</span>
      </span>
      <span
        v-else-if="!disabled"
        class="absolute bottom-1.5 right-1.5 flex size-6 items-center justify-center rounded-full bg-white/95 text-primary shadow-sm ring-1 ring-slate-200 transition-transform group-hover:scale-105"
      >
        <span class="material-symbols-outlined text-[15px] leading-none">edit</span>
      </span>
    </button>

    <button
      v-if="logoUrl && !disabled && !uploading"
      type="button"
      class="absolute -right-2 -top-2 flex size-6 items-center justify-center rounded-full bg-white text-slate-400 shadow-sm ring-1 ring-slate-200 transition-colors hover:text-red-500"
      @click.stop="emit('removed')"
    >
      <span class="material-symbols-outlined text-[15px] leading-none">close</span>
    </button>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      class="hidden"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { isRequestErrorHandled } from '@/utils/requestError'

const props = withDefaults(defineProps<{
  logoUrl?: string | null
  alt?: string
  disabled?: boolean
  size?: number
}>(), {
  logoUrl: '',
  alt: '公司 Logo',
  disabled: false,
  size: 64
})

const emit = defineEmits<{
  uploaded: [payload: { logo: string; logoUrl: string }]
  removed: []
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const logoStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))

function triggerFileSelect() {
  if (props.disabled || uploading.value) return
  fileInputRef.value?.click()
}

function isAllowedLogoFile(file: File): boolean {
  return ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!isAllowedLogoFile(file)) {
    ElMessage.warning('仅支持 JPG、PNG、WEBP 格式')
    input.value = ''
    return
  }

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    input.value = ''
    return
  }

  uploading.value = true
  try {
    const presigned = await getPresignedUploadUrl(file.name, file.type)
    await uploadToMinIO(file, presigned.uploadUrl)
    emit('uploaded', {
      logo: presigned.objectKey,
      logoUrl: presigned.accessUrl
    })
  } catch (error) {
    console.error('Customer logo upload failed:', error)
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('Logo 上传失败')
    }
  } finally {
    uploading.value = false
    input.value = ''
  }
}
</script>
