<template>
  <el-dialog
    v-model="open"
    :width="isMobile ? 'calc(100% - 24px)' : '760px'"
    :show-close="false"
    destroy-on-close
    class="wk-dialog--flush wk-project-member-dialog wk-crm-el-field-scope"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="flex size-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-[22px]">group</span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-900">{{ editingMember ? '编辑项目成员' : '添加项目成员' }}</h2>
            <p class="mt-0.5 text-xs text-slate-500">为项目配置成员角色和权限范围。</p>
          </div>
        </div>
        <button
          type="button"
          class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
          @click="open = false"
        >
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
      </div>
    </template>

    <div class="space-y-5 bg-white px-5 pb-6 pt-5 md:px-6 md:pb-7">
      <div>
        <label class="mb-1.5 block text-xs font-bold text-slate-500">成员用户 <span class="text-red-500">*</span></label>
        <el-select
          v-model="form.userId"
          filterable
          remote
          reserve-keyword
          clearable
          default-first-option
          :disabled="!!editingMember"
          placeholder="搜索成员姓名或账号"
          :remote-method="searchUsers"
          :loading="userLoading"
          class="wk-crm-el-field-select w-full"
          size="large"
          @change="syncSelectedUser"
        >
          <el-option
            v-for="item in userOptions"
            :key="item.value"
            :label="`${item.label}${item.account ? ` (${item.account})` : ''}`"
            :value="item.value"
          />
        </el-select>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div>
          <label class="mb-1.5 block text-xs font-bold text-slate-500">项目角色 <span class="text-red-500">*</span></label>
          <el-select v-model="form.role" class="wk-crm-el-field-select w-full" size="large" @change="applyRolePermissions">
            <el-option
              v-for="item in PROJECT_ROLE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <div>
          <label class="mb-1.5 block text-xs font-bold text-slate-500">成员状态</label>
          <el-select v-model="form.status" class="wk-crm-el-field-select w-full" size="large">
            <el-option
              v-for="item in PROJECT_MEMBER_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
      </div>

      <div class="rounded-3xl border border-slate-200 bg-slate-50 p-4">
        <div class="mb-3 flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold text-slate-900">权限范围</p>
            <p class="mt-1 text-xs text-slate-500">默认会按角色带出权限，你也可以单独调整。</p>
          </div>
          <button
            type="button"
            class="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-50"
            @click="applyRolePermissions"
          >
            恢复默认权限
          </button>
        </div>

        <el-checkbox-group v-model="selectedPermissions" class="grid grid-cols-1 gap-2 md:grid-cols-2">
          <label
            v-for="permission in PROJECT_PERMISSION_OPTIONS"
            :key="permission.value"
            class="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-3"
          >
            <el-checkbox :value="permission.value" :aria-label="permission.label" />
            <span class="text-sm text-slate-700">{{ permission.label }}</span>
          </label>
        </el-checkbox-group>
      </div>

      <div>
        <label class="mb-1.5 block text-xs font-bold text-slate-500">备注</label>
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="可补充协作说明、授权边界等备注"
          class="wk-crm-el-field-input"
        />
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3">
        <button
          type="button"
          class="flex-1 rounded-xl bg-slate-100 py-2.5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-200"
          @click="open = false"
        >
          取消
        </button>
        <button
          type="button"
          class="flex-1 rounded-xl bg-primary py-2.5 text-sm font-bold text-white transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="!form.userId || !selectedPermissions.length"
          @click="handleSubmit"
        >
          {{ editingMember ? '保存修改' : '添加成员' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { queryUserList } from '@/api/auth'
import { getProjectRolePermissionConfig } from '@/api/project'
import { useResponsive } from '@/composables/useResponsive'
import type {
  ProjectMember,
  ProjectMemberStatus,
  ProjectPermission,
  ProjectRole
} from '@/types/project'
import {
  PROJECT_MEMBER_STATUS_OPTIONS,
  PROJECT_PERMISSION_OPTIONS,
  PROJECT_ROLE_OPTIONS,
  roleDefaultPermissions,
  setProjectRolePermissions
} from '@/utils/project'

type UserOption = {
  value: string
  label: string
  account: string
  deptName: string
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  editingMember?: ProjectMember | null
}>(), {
  editingMember: null
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submit', value: {
    userId: string
    memberName: string
    account: string
    deptName?: string
    role: ProjectRole
    status: ProjectMemberStatus
    permissions: ProjectPermission[]
    remark?: string
  }): void
}>()

const { isMobile } = useResponsive()

const userLoading = ref(false)
const userOptions = ref<UserOption[]>([])
const selectedPermissions = ref<ProjectPermission[]>([])

const form = reactive({
  userId: '',
  memberName: '',
  account: '',
  deptName: '',
  role: 'MEMBER' as ProjectRole,
  status: 'ACTIVE' as ProjectMemberStatus,
  remark: ''
})

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const editingMember = computed(() => props.editingMember)

watch(
  () => [props.modelValue, props.editingMember?.userId] as const,
  ([visible]) => {
    if (!visible) return
    void prepareForm()
  },
  { immediate: true }
)

async function prepareForm() {
  try {
    const config = await getProjectRolePermissionConfig()
    setProjectRolePermissions(config.rolePermissions)
  } catch {
    // Keep the current in-memory defaults if the config request fails.
  }
  if (!props.modelValue) return
  hydrateForm()
}

function hydrateForm() {
  userOptions.value = []
  if (editingMember.value) {
    form.userId = editingMember.value.userId
    form.memberName = editingMember.value.memberName
    form.account = editingMember.value.account
    form.deptName = editingMember.value.deptName || ''
    form.role = editingMember.value.role
    form.status = editingMember.value.status
    form.remark = editingMember.value.remark || ''
    selectedPermissions.value = [...editingMember.value.permissions]
    userOptions.value = [{
      value: editingMember.value.userId,
      label: editingMember.value.memberName,
      account: editingMember.value.account,
      deptName: editingMember.value.deptName || ''
    }]
    return
  }

  form.userId = ''
  form.memberName = ''
  form.account = ''
  form.deptName = ''
  form.role = 'MEMBER'
  form.status = 'ACTIVE'
  form.remark = ''
  selectedPermissions.value = roleDefaultPermissions('MEMBER')
}

async function searchUsers(query: string) {
  if (!query.trim()) {
    userOptions.value = []
    return
  }
  userLoading.value = true
  try {
    const response = await queryUserList({ search: query.trim(), page: 1, limit: 20 })
    userOptions.value = (response.list || []).map((item: any) => ({
      value: String(item.userId),
      label: item.realname || item.username || '',
      account: item.username || item.email || '',
      deptName: item.deptName || ''
    })).filter((item: UserOption) => item.label)
  } finally {
    userLoading.value = false
  }
}

function syncSelectedUser() {
  const selected = userOptions.value.find(item => item.value === form.userId)
  if (!selected) return
  form.memberName = selected.label
  form.account = selected.account || selected.label
  form.deptName = selected.deptName || ''
}

function applyRolePermissions() {
  selectedPermissions.value = roleDefaultPermissions(form.role)
}

function handleSubmit() {
  if (!form.userId) {
    ElMessage.warning('请选择成员用户')
    return
  }
  if (!selectedPermissions.value.length) {
    ElMessage.warning('请至少选择一项权限')
    return
  }
  emit('submit', {
    userId: form.userId,
    memberName: form.memberName || userOptions.value.find(item => item.value === form.userId)?.label || '',
    account: form.account || userOptions.value.find(item => item.value === form.userId)?.account || '',
    deptName: form.deptName || undefined,
    role: form.role,
    status: form.status,
    permissions: selectedPermissions.value,
    remark: form.remark.trim() || undefined
  })
  open.value = false
}
</script>

<style scoped>
.wk-project-member-dialog :deep(.el-dialog__header) {
  padding: 22px 24px 14px;
  margin-right: 0;
}

.wk-project-member-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.wk-project-member-dialog :deep(.el-dialog__footer) {
  padding: 14px 24px 22px;
}
</style>
