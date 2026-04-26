<template>
  <el-dialog
    v-model="visible"
    :width="isMobile ? '95%' : '570px'"
    :fullscreen="isMobile"
    align-center
    :show-close="false"
    class="wk-knowledge-upload-dialog"
    :class="isMobile ? 'is-mobile' : ''"
    modal-class="wk-knowledge-upload-dialog__overlay"
    :destroy-on-close="destroyOnClose"
  >
    <div class="wk-knowledge-upload-dialog__content">
      <div class="mb-8 space-y-3 pt-1 text-center">
        <div
          class="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-primary"
        >
          <span class="material-symbols-outlined text-3xl">cloud_upload</span>
        </div>
        <h3 class="text-xl font-bold text-slate-900">{{ headline }}</h3>
        <p class="text-xs text-slate-500">
          {{ subline }}
        </p>
      </div>

      <div class="space-y-6">
        <section class="space-y-3">
          <p class="text-xs font-bold uppercase tracking-widest text-slate-400">{{ step1Label }}</p>
          <div class="grid grid-cols-2 gap-3 md:grid-cols-3">
            <button
              v-for="cat in typeCategories"
              :key="cat.id"
              type="button"
              :disabled="uploading"
              @click="form.type = cat.id"
              :class="[
                'flex flex-col items-center gap-2 rounded-2xl border bg-white py-4 transition-all',
                form.type === cat.id
                  ? 'border-primary bg-primary/5 text-primary ring-2 ring-primary/20'
                  : 'border-slate-100 text-slate-600 hover:border-slate-200 hover:bg-slate-50',
                uploading ? 'cursor-not-allowed opacity-60' : ''
              ]"
            >
              <span class="material-symbols-outlined text-xl">{{ cat.icon }}</span>
              <span class="text-xs font-bold">{{ cat.label }}</span>
            </button>
          </div>
        </section>

        <section class="space-y-3">
          <div class="flex items-center justify-between gap-3">
            <p class="text-xs font-bold uppercase tracking-widest text-slate-400">{{ step2Label }}</p>
            <span v-if="uploading" class="inline-flex items-center gap-2 text-xs text-slate-500">
              <span
                class="inline-block size-3 animate-spin rounded-full border-2 border-slate-300 border-t-transparent"
              />
              正在上传...
            </span>
          </div>

          <el-upload
            class="w-full"
            :show-file-list="false"
            :before-upload="onBeforeFileSelect"
            :http-request="noopUpload"
            :disabled="uploading"
            drag
            :accept="accept"
          >
            <div
              class="rounded-[2rem] border-2 border-dashed border-slate-200 bg-white p-8 text-center transition-all hover:border-primary hover:bg-primary/5 md:p-10"
            >
              <template v-if="selectedFile">
                <div
                  class="mx-auto flex max-w-xl items-center gap-4 rounded-2xl bg-slate-900 p-4 text-left"
                >
                  <div
                    class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/10 text-white"
                  >
                    <span class="material-symbols-outlined text-lg">
                      {{
                        selectedFile.name.toLowerCase().endsWith('.pdf')
                          ? 'picture_as_pdf'
                          : selectedFile.name.toLowerCase().endsWith('.ppt') ||
                              selectedFile.name.toLowerCase().endsWith('.pptx')
                            ? 'slideshow'
                            : 'description'
                      }}
                    </span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-bold text-white">{{ selectedFile.name }}</p>
                    <p class="mt-0.5 text-xs text-slate-300">
                      {{ `${(selectedFile.size / 1024 / 1024).toFixed(2)} MB` }}
                    </p>
                    <p class="mt-2 text-xs text-slate-400">点击更换文件，或拖拽新文件到此处</p>
                  </div>
                </div>
              </template>
              <template v-else>
                <span class="material-symbols-outlined mb-3 text-4xl text-slate-300">upload_file</span>
                <p class="text-xs font-bold text-slate-500">拖拽文件到此处，或点击浏览</p>
                <p class="mt-1 text-xs text-slate-300">{{ sizeHint }}</p>
              </template>
            </div>
          </el-upload>
        </section>

        <section class="space-y-3">
          <p class="text-xs font-bold uppercase tracking-widest text-slate-400">{{ step3Label }}</p>
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            :disabled="uploading"
            resize="none"
            :placeholder="summaryPlaceholder"
          />
        </section>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3 pt-0">
        <button
          type="button"
          class="flex-1 rounded-2xl border border-slate-200 py-3.5 text-sm font-bold text-slate-600 transition-all hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="uploading"
          @click="close()"
        >
          取消
        </button>
        <button
          type="button"
          class="flex-1 rounded-2xl bg-primary py-3.5 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none"
          :disabled="uploading || !selectedFile"
          @click="confirmUpload"
        >
          <span class="inline-flex items-center justify-center gap-2">
            <span
              v-if="uploading"
              class="inline-block size-4 animate-spin rounded-full border-2 border-white/60 border-t-transparent"
            />
            {{ uploading ? '上传中...' : primaryActionLabel }}
          </span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, UploadRequestOptions } from 'element-plus'
import { uploadKnowledge } from '@/api/knowledge'
import { useResponsive } from '@/composables/useResponsive'

const ACCEPT =
  '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md'
const typeCategories = [
  { id: 'document', label: '产品文档', icon: 'description' },
  { id: 'proposal', label: '方案资料', icon: 'lightbulb' },
  { id: 'meeting', label: '会议记录', icon: 'event_note' },
  { id: 'contract', label: '合同文件', icon: 'assignment' },
  { id: 'email', label: '邮件往来', icon: 'mail' },
  { id: 'recording', label: '录音文件', icon: 'mic' }
] as const

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    /** 关联客户时传入，与知识中心独立上传二选一 */
    customerId?: string
    /** 主标题 */
    headline?: string
    /** 说明文字 */
    subline?: string
    /** 第一步小标题 */
    step1Label?: string
    step2Label?: string
    step3Label?: string
    summaryPlaceholder?: string
    primaryActionLabel?: string
    successMessage?: string
    sizeHint?: string
    accept?: string
    destroyOnClose?: boolean
  }>(),
  {
    headline: '上传业务知识',
    subline: '支持 PDF、Word、PPT、Excel 或网页链接。AI 将自动解析并建立索引。',
    step1Label: '1. 选择知识分类',
    step2Label: '2. 上传文件',
    step3Label: '3. 填写摘要（可选）',
    summaryPlaceholder: '例如：本次会议的关键结论、客户关注点、下一步行动…',
    primaryActionLabel: '开始解析',
    successMessage: '上传成功',
    sizeHint: '支持 PDF、Word、PPT、Excel（最大 50MB）',
    accept: ACCEPT,
    destroyOnClose: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  /** 接口成功并已完成提示与关弹窗后触发，用于刷新列表 */
  success: []
}>()

const { isMobile } = useResponsive()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const uploading = ref(false)
const selectedFile = ref<File | null>(null)
const fileQueuedFromTrigger = ref<File | null>(null)
const form = ref({
  type: 'document',
  summary: ''
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      if (!uploading.value) {
        resetForm()
      }
      return
    }
    form.value.type = 'document'
    form.value.summary = ''
    selectedFile.value = fileQueuedFromTrigger.value
    fileQueuedFromTrigger.value = null
  }
)

function resetForm() {
  selectedFile.value = null
  form.value = { type: 'document', summary: '' }
}

function onBeforeFileSelect(file: File) {
  selectedFile.value = file
  return false
}

function noopUpload(_options: UploadRequestOptions) {
  /* 由 confirmUpload 调接口 */
}

/**
 * 供外侧 `el-upload` 使用：先选择文件并打开本弹窗（不自动上传）
 * @returns false 表示阻止 el-upload 默认行为
 */
function onTriggerBeforeUpload(file: File) {
  fileQueuedFromTrigger.value = file
  visible.value = true
  return false
}

function openEmpty() {
  fileQueuedFromTrigger.value = null
  visible.value = true
}

function close() {
  if (uploading.value) return
  visible.value = false
}

async function confirmUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    await uploadKnowledge(
      selectedFile.value,
      form.value.type,
      props.customerId,
      form.value.summary
    )
    ElMessage.success(props.successMessage)
    visible.value = false
    resetForm()
    emit('success')
  } finally {
    uploading.value = false
  }
}

defineExpose({
  /** 侧栏 / 顶栏「选择文件即打开弹窗」的 beforeUpload 处理 */
  onTriggerBeforeUpload,
  /** 仅打开弹窗，不预选文件 */
  openEmpty
})
</script>

<style lang="scss">
.wk-knowledge-upload-dialog .el-upload {
  width: 100%;
}

.wk-knowledge-upload-dialog.is-mobile {
  width: calc(100vw - 32px);
  margin-top: 16px;
  height: calc(100vh - 32px);
  max-height: calc(100vh - 124px) !important;
}

.wk-knowledge-upload-dialog.el-dialog {
  border-radius: 2rem;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.35);
}

.wk-knowledge-upload-dialog .el-dialog__header {
  display: none;
  padding: 0;
}

.wk-knowledge-upload-dialog .el-dialog__body {
  padding: 16px 20px 20px 20px;
}

.wk-knowledge-upload-dialog .el-dialog__footer {
  padding: 1rem 0 0 !important;
  border-top: none;
  box-shadow: none;
}

.wk-knowledge-upload-dialog__overlay {
  background: rgba(15, 23, 42, 0.6) !important;
  backdrop-filter: blur(10px);
}

.wk-knowledge-upload-dialog .el-textarea__inner {
  border-radius: 14px;
}

.wk-knowledge-upload-dialog .el-input__wrapper {
  border-radius: 14px;
}

.wk-knowledge-upload-dialog .el-upload-dragger {
  padding: 0;
  border: none;
  background: transparent;
}

.wk-knowledge-upload-dialog .el-upload.is-drag {
  width: 100%;
}

.wk-knowledge-upload-dialog.el-dialog {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.wk-knowledge-upload-dialog .el-dialog__body {
  overflow-y: auto;
  flex: 1 1 auto;
  overscroll-behavior: contain;
}

.wk-knowledge-upload-dialog .el-dialog__footer {
  flex: 0 0 auto;
}

</style>
