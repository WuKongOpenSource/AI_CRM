<template>
  <el-dialog
    v-model="open"
    :title="isEdit ? '编辑联系人' : '添加联系人'"
    :width="isMobile ? '95%' : '500px'"
    :fullscreen="isMobile"
    destroy-on-close
    class="wk-dialog--flush wk-crm-el-field-scope"
  >
    <el-form :model="formData" label-width="80px">
      <el-form-item label="姓名" required>
        <el-input v-model="formData.name" placeholder="请输入姓名" class="wk-crm-el-field-input" />
      </el-form-item>
      <el-form-item label="职位">
        <el-input v-model="formData.position" placeholder="请输入职位" class="wk-crm-el-field-input" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="formData.phone" placeholder="请输入电话" class="wk-crm-el-field-input" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="formData.email" placeholder="请输入邮箱" class="wk-crm-el-field-input" />
      </el-form-item>
      <el-form-item label="微信">
        <el-input v-model="formData.wechat" placeholder="请输入微信号" class="wk-crm-el-field-input" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.notes" type="textarea" :rows="3" placeholder="请输入备注" class="wk-crm-el-field-input" />
      </el-form-item>
      <el-form-item label="主联系人">
        <el-switch v-model="formData.isPrimary" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="open = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { addContact, updateContact } from '@/api/contact'
import type { Contact } from '@/types/customer'

const props = withDefaults(defineProps<{
  modelValue: boolean
  customerId: string
  contact?: Contact | null
  existingPrimaryContact?: Contact | null
}>(), {
  contact: null,
  existingPrimaryContact: null
})

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
      return
    }

    resetForm()
  },
  { immediate: true }
)

function resetForm() {
  formData.name = ''
  formData.position = ''
  formData.phone = ''
  formData.email = ''
  formData.wechat = ''
  formData.notes = ''
  formData.isPrimary = false
}

async function handleSubmit() {
  if (!props.customerId) return
  if (!formData.name.trim()) {
    ElMessage.warning('请输入联系人姓名')
    return
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
      isPrimary: formData.isPrimary ? 1 : 0
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

    open.value = false
    resetForm()
  } finally {
    submitting.value = false
  }
}
</script>
