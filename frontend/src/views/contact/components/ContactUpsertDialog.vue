<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6">
        <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="handleClose" />

        <div
          :class="[
            'relative w-full bg-slate-50 shadow-2xl overflow-hidden flex flex-col wk-crm-el-field-scope',
            isMobile ? 'max-w-full max-h-full rounded-none inset-0' : 'max-w-5xl max-h-[90vh] rounded-[2.5rem]'
          ]"
        >
          <div class="bg-white border-b border-slate-200 px-6 sm:px-8 py-4 sm:py-5 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-3 sm:gap-4">
              <div class="size-10 sm:size-12 rounded-2xl bg-primary/10 flex items-center justify-center text-primary">
                <span class="material-symbols-outlined">{{ isEdit ? 'edit' : 'person_add' }}</span>
              </div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-slate-900">{{ isEdit ? '编辑联系人' : '添加联系人' }}</h2>
                <p class="text-xs text-slate-500">
                  {{ isEdit ? '修改联系人基本信息与备注' : '使用 AI 智能录入或手动填写联系人信息' }}
                </p>
              </div>
            </div>
            <div class="flex items-center gap-2 sm:gap-3">
              <button
                type="button"
                class="px-4 sm:px-6 py-2 sm:py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 transition-colors"
                @click="handleClose"
              >
                取消
              </button>
              <button
                type="button"
                :disabled="submitting"
                class="px-5 sm:px-8 py-2 sm:py-2.5 bg-primary text-white rounded-xl text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2 disabled:opacity-50"
                @click="handleSubmit"
              >
                <span v-if="submitting" class="size-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ isEdit ? '保存' : '添加' }}
              </button>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto p-4 sm:p-6">
            <div class="space-y-5">
              <input
                ref="aiImageInputRef"
                type="file"
                class="hidden"
                accept="image/*"
                @change="handleAiImageChange"
              />

              <AiSmartEntrySection
                v-if="!isEdit"
                v-model="aiInputText"
                placeholder="在此粘贴联系人描述、邮件内容，或直接粘贴 (Ctrl+V) 名片图片..."
                :ai-image-preview="aiImagePreview"
                :ai-parsing="aiParsing"
                :can-extract="Boolean(aiInputText.trim() || aiImageFile)"
                :show-image-hint="Boolean(aiImagePreview && !aiParsing)"
                @paste="handleAiPaste"
                @extract="handleAiExtract"
                @remove-image="removeAiImage"
              >
                <template v-if="isMobile && aiParseResult" #after-actions>
                  <div class="p-3 bg-primary/5 rounded-xl border border-primary/10 text-xs text-slate-600 space-y-1">
                    <div v-if="aiParseResult.score != null" class="font-bold text-primary">潜力评分: {{ aiParseResult.score }}/100</div>
                    <div v-if="aiParseResult.summary">{{ aiParseResult.summary }}</div>
                    <div v-if="aiParseResult.tags?.length" class="flex flex-wrap gap-1 mt-1">
                      <span
                        v-for="tag in aiParseResult.tags"
                        :key="tag"
                        class="px-2 py-0.5 bg-primary/10 text-primary text-xs font-bold rounded-full"
                      >{{ tag }}</span>
                    </div>
                  </div>
                </template>
              </AiSmartEntrySection>

              <section class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 sm:p-6">
                <h3 class="text-xs font-bold text-slate-900 mb-5 flex items-center gap-2 uppercase tracking-wider">
                  <span class="w-1 h-3 bg-primary rounded-full"></span>
                  联系人信息
                </h3>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase ml-1">姓名 <span class="text-red-400">*</span></label>
                    <el-input
                      v-model="formData.name"
                      placeholder="请输入姓名"
                      size="large"
                      class="w-full wk-crm-el-field-input"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase ml-1">职位</label>
                    <el-input
                      v-model="formData.position"
                      placeholder="请输入职位"
                      size="large"
                      class="w-full wk-crm-el-field-input"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase ml-1">电话</label>
                    <el-input
                      v-model="formData.phone"
                      placeholder="请输入电话"
                      size="large"
                      class="w-full wk-crm-el-field-input"
                    />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-slate-500 uppercase ml-1">邮箱</label>
                    <el-input
                      v-model="formData.email"
                      placeholder="请输入邮箱"
                      size="large"
                      class="w-full wk-crm-el-field-input"
                    />
                  </div>
                  <div class="space-y-1.5 md:col-span-2">
                    <label class="text-xs font-bold text-slate-500 uppercase ml-1">微信</label>
                    <el-input
                      v-model="formData.wechat"
                      placeholder="请输入微信号"
                      size="large"
                      class="w-full wk-crm-el-field-input"
                    />
                  </div>
                  <div class="space-y-1.5 md:col-span-2">
                    <label class="text-xs font-bold text-slate-500 uppercase ml-1">备注</label>
                    <el-input
                      v-model="formData.notes"
                      type="textarea"
                      :rows="3"
                      resize="none"
                      placeholder="请输入备注"
                      class="w-full wk-crm-el-field-input"
                    />
                  </div>
                  <div class="space-y-1.5 md:col-span-2 flex items-center gap-3 pt-1">
                    <label
                      class="text-xs font-bold uppercase ml-1 shrink-0 transition-colors"
                      :class="formData.isPrimary ? 'text-primary' : 'text-slate-500'"
                    >主联系人</label>
                    <el-switch
                      v-model="formData.isPrimary"
                      style="--el-switch-on-color: var(--el-color-primary)"
                    />
                  </div>
                </div>
                <DynamicFieldForm
                  ref="dynamicFieldFormRef"
                  entity-type="contact"
                  v-model="customFieldValues"
                  class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4 mt-4"
                />
              </section>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { addContact, updateContact } from '@/api/contact'
import { aiParseCustomer } from '@/api/customer'
import type { CustomerAiParseVO } from '@/api/customer'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { Contact } from '@/types/customer'
import AiSmartEntrySection from '@/components/crm/AiSmartEntrySection.vue'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    customerId: string
    contact?: Contact | null
    existingPrimaryContact?: Contact | null
    autoOpenAiImagePickerToken?: number
  }>(),
  {
    contact: null,
    existingPrimaryContact: null,
    autoOpenAiImagePickerToken: 0
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', payload: { mode: 'create' | 'edit' }): void
}>()

const { isMobile } = useResponsive()

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const isEdit = computed(() => Boolean(props.contact?.contactId))
const submitting = ref(false)
const formData = reactive({
  name: '',
  position: '',
  phone: '',
  email: '',
  wechat: '',
  notes: '',
  isPrimary: false
})

const dynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()
const customFieldValues = ref<Record<string, any>>({})

const aiInputText = ref('')
const aiParsing = ref(false)
const aiParseResult = ref<CustomerAiParseVO | null>(null)
const aiImageFile = ref<File | null>(null)
const aiImagePreview = ref<string | null>(null)
const aiImageInputRef = ref<HTMLInputElement | null>(null)
const lastHandledAiImagePickerToken = ref(0)

function applyAiImageFile(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  removeAiImage()
  aiImageFile.value = file
  aiImagePreview.value = URL.createObjectURL(file)
}

function removeAiImage() {
  if (aiImagePreview.value) URL.revokeObjectURL(aiImagePreview.value)
  aiImageFile.value = null
  aiImagePreview.value = null
}

function resetAiState() {
  aiInputText.value = ''
  aiParsing.value = false
  aiParseResult.value = null
  removeAiImage()
}

function handleAiPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (let i = 0; i < items.length; i++) {
    if (items[i].kind === 'file' && items[i].type.startsWith('image/')) {
      const file = items[i].getAsFile()
      if (file) {
        applyAiImageFile(file)
        break
      }
    }
  }
}

function handleAiImageChange(event: Event) {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (file) {
    applyAiImageFile(file)
  }
  if (input) {
    input.value = ''
  }
}

async function openAiImagePicker() {
  await nextTick()
  aiImageInputRef.value?.click()
}

async function handleAiExtract() {
  if (aiParsing.value) return
  if (!aiInputText.value.trim() && !aiImageFile.value) {
    ElMessage.warning('请输入文本或粘贴图片')
    return
  }

  aiParsing.value = true
  try {
    let imageObjectKey: string | undefined
    let imageMimeType: string | undefined

    if (aiImageFile.value) {
      const presigned = await getPresignedUploadUrl(aiImageFile.value.name, aiImageFile.value.type)
      await uploadToMinIO(aiImageFile.value, presigned.uploadUrl)
      imageObjectKey = presigned.objectKey
      imageMimeType = aiImageFile.value.type
    }

    const result = await aiParseCustomer({
      content: aiInputText.value.trim() || '请从图片中提取联系人信息',
      imageObjectKey,
      imageMimeType
    })
    aiParseResult.value = result

    if (result.contactName) formData.name = result.contactName
    if (result.contactPhone) formData.phone = result.contactPhone
    if (result.contactEmail) formData.email = result.contactEmail
    if (result.contactPosition) formData.position = result.contactPosition
    if (result.remark) formData.notes = result.remark

    ElMessage.success('AI 提取完成，信息已自动填充')
  } catch (error: unknown) {
    console.error('AI parse contact failed:', error)
    if (!isRequestErrorHandled(error)) {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error('AI 解析失败: ' + message)
    }
  } finally {
    aiParsing.value = false
  }
}

function resetForm() {
  formData.name = ''
  formData.position = ''
  formData.phone = ''
  formData.email = ''
  formData.wechat = ''
  formData.notes = ''
  formData.isPrimary = false
  customFieldValues.value = {}
  resetAiState()
}

function handleClose() {
  emit('update:modelValue', false)
}

watch(
  () => [props.modelValue, props.contact?.contactId, props.customerId] as const,
  ([visible]) => {
    if (!visible) {
      resetForm()
      return
    }

    if (props.contact) {
      formData.name = props.contact.name || ''
      formData.position = props.contact.position || ''
      formData.phone = props.contact.phone || ''
      formData.email = props.contact.email || ''
      formData.wechat = props.contact.wechat || ''
      formData.notes = props.contact.notes || ''
      formData.isPrimary = Boolean(props.contact.isPrimary)
      customFieldValues.value = (props.contact as any).customFields
        ? { ...(props.contact as any).customFields }
        : {}
      resetAiState()
      return
    }

    resetForm()
  },
  { immediate: true }
)

watch(
  () => [props.modelValue, props.autoOpenAiImagePickerToken, props.contact?.contactId] as const,
  ([visible, token, contactId]) => {
    if (!visible || token <= 0 || contactId) return
    if (token === lastHandledAiImagePickerToken.value) return
    lastHandledAiImagePickerToken.value = token
    void openAiImagePicker()
  },
  { immediate: true }
)

async function handleSubmit() {
  if (!props.customerId) return
  if (!formData.name.trim()) {
    ElMessage.warning('请输入联系人姓名')
    return
  }

  if (dynamicFieldFormRef.value) {
    const missingFields = dynamicFieldFormRef.value.getRequiredFieldLabels()
    if (missingFields.length > 0) {
      ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
      return
    }
  }

  if (formData.isPrimary) {
    const existingPrimary = props.existingPrimaryContact
    if (existingPrimary && existingPrimary.contactId !== props.contact?.contactId) {
      try {
        await ElMessageBox.confirm(
          `当前主要联系人为「${existingPrimary.name}」，是否替换为新联系人？`,
          '替换主要联系人',
          {
            type: 'warning',
            confirmButtonText: '确定替换',
            cancelButtonText: '取消'
          }
        )
      } catch {
        return
      }
    }
  }

  submitting.value = true
  try {
    const submitData = {
      customerId: props.customerId,
      name: formData.name.trim(),
      position: formData.position.trim(),
      phone: formData.phone.trim(),
      email: formData.email.trim(),
      wechat: formData.wechat.trim(),
      notes: formData.notes.trim(),
      isPrimary: formData.isPrimary ? 1 : 0,
      customFields: customFieldValues.value
    }

    if (props.contact?.contactId) {
      await updateContact({ ...submitData, contactId: props.contact.contactId } as any)
      ElMessage.success('联系人更新成功')
      emit('success', { mode: 'edit' })
    } else {
      await addContact(submitData as any)
      ElMessage.success('联系人添加成功')
      emit('success', { mode: 'create' })
    }

    emit('update:modelValue', false)
    resetForm()
  } finally {
    submitting.value = false
  }
}
</script>
