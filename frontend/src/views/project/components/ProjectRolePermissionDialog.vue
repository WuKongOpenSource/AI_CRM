<template>
  <el-dialog
    v-model="open"
    :width="isMobile ? 'calc(100% - 24px)' : '860px'"
    :show-close="false"
    destroy-on-close
    class="wk-dialog--flush wk-project-role-permission-dialog wk-crm-el-field-scope"
  >
    <template #header>
      <div class="flex items-center justify-between gap-4">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-[22px]">admin_panel_settings</span>
          </div>
          <div class="min-w-0">
            <h2 class="truncate text-lg font-bold text-slate-900">项目角色权限设置</h2>
            <p class="mt-0.5 text-xs text-slate-500">配置新增成员和切换角色时自动带出的默认权限</p>
          </div>
        </div>
        <button
          type="button"
          class="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
          @click="open = false"
        >
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
      </div>
    </template>

    <div class="grid min-h-[480px] grid-cols-1 gap-0 bg-white md:grid-cols-[220px_minmax(0,1fr)]" v-loading="loading">
      <aside class="border-b border-slate-200 bg-slate-50/70 p-4 md:border-b-0 md:border-r">
        <div class="space-y-1">
          <button
            v-for="role in PROJECT_ROLE_OPTIONS"
            :key="role.value"
            type="button"
            class="flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-colors"
            :class="activeRole === role.value ? 'bg-white font-bold text-primary shadow-sm' : 'text-slate-600 hover:bg-white/70 hover:text-slate-900'"
            @click="activeRole = role.value"
          >
            <span class="truncate">{{ role.label }}</span>
            <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
              {{ rolePermissions[role.value].length }}
            </span>
          </button>
        </div>
      </aside>

      <section class="flex min-h-0 flex-col p-5 md:p-6">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div class="min-w-0">
            <h3 class="text-base font-bold text-slate-900">{{ activeRoleLabel }}</h3>
            <p class="mt-1 text-xs text-slate-500">已选择 {{ rolePermissions[activeRole].length }} 项权限</p>
          </div>
          <div class="flex items-center gap-2">
            <button
              type="button"
              class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-50"
              @click="resetCurrentRole"
            >
              恢复当前角色默认
            </button>
            <button
              type="button"
              class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-50"
              @click="resetAllRoles"
            >
              恢复全部默认
            </button>
          </div>
        </div>

        <el-checkbox-group
          v-model="rolePermissions[activeRole]"
          class="grid min-h-0 grid-cols-1 gap-2 overflow-y-auto pr-1 md:grid-cols-2"
          @change="ensureActiveRoleBaseline"
        >
          <label
            v-for="permission in PROJECT_PERMISSION_OPTIONS"
            :key="permission.value"
            class="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-3 transition-colors hover:border-primary/25 hover:bg-primary/[0.03]"
          >
            <el-checkbox
              :value="permission.value"
              :disabled="permission.value === 'VIEW_PROJECT' || activeRole === 'OWNER'"
              :aria-label="permission.label"
            />
            <span class="text-sm leading-5 text-slate-700">{{ permission.label }}</span>
          </label>
        </el-checkbox-group>
      </section>
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
          :disabled="saving || loading"
          @click="handleSave"
        >
          保存配置
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getProjectRolePermissionConfig,
  updateProjectRolePermissionConfig
} from '@/api/project'
import { useResponsive } from '@/composables/useResponsive'
import type {
  ProjectPermission,
  ProjectRole,
  ProjectRolePermissionConfig
} from '@/types/project'
import {
  DEFAULT_ROLE_PERMISSIONS,
  PROJECT_PERMISSION_OPTIONS,
  PROJECT_ROLE_OPTIONS,
  getProjectRolePermissions,
  projectRoleLabel,
  setProjectRolePermissions
} from '@/utils/project'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'saved'): void
}>()

const { isMobile } = useResponsive()

const loading = ref(false)
const saving = ref(false)
const activeRole = ref<ProjectRole>('MEMBER')
const rolePermissions = reactive<ProjectRolePermissionConfig>(getProjectRolePermissions())

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const activeRoleLabel = computed(() => projectRoleLabel(activeRole.value))

watch(
  () => props.modelValue,
  visible => {
    if (!visible) return
    void loadConfig()
  },
  { immediate: true }
)

async function loadConfig() {
  loading.value = true
  try {
    const config = await getProjectRolePermissionConfig()
    setProjectRolePermissions(config.rolePermissions)
    applyLocalRolePermissions(config.rolePermissions)
  } catch {
    applyLocalRolePermissions(getProjectRolePermissions())
  } finally {
    loading.value = false
  }
}

function applyLocalRolePermissions(config: ProjectRolePermissionConfig) {
  PROJECT_ROLE_OPTIONS.forEach(role => {
    rolePermissions[role.value] = [...config[role.value]]
    ensureRoleBaseline(role.value)
  })
}

function resetCurrentRole() {
  rolePermissions[activeRole.value] = [...DEFAULT_ROLE_PERMISSIONS[activeRole.value]]
  ensureActiveRoleBaseline()
}

function resetAllRoles() {
  applyLocalRolePermissions(DEFAULT_ROLE_PERMISSIONS)
}

function ensureActiveRoleBaseline() {
  ensureRoleBaseline(activeRole.value)
}

function ensureRoleBaseline(role: ProjectRole) {
  const permissions = rolePermissions[role]
  if (role === 'OWNER') {
    rolePermissions.OWNER = [...DEFAULT_ROLE_PERMISSIONS.OWNER]
    return
  }
  if (!permissions.includes('VIEW_PROJECT')) {
    permissions.unshift('VIEW_PROJECT')
  }
  rolePermissions[role] = Array.from(new Set(permissions)) as ProjectPermission[]
}

async function handleSave() {
  saving.value = true
  try {
    PROJECT_ROLE_OPTIONS.forEach(role => ensureRoleBaseline(role.value))
    const config = await updateProjectRolePermissionConfig({
      rolePermissions: { ...rolePermissions }
    })
    setProjectRolePermissions(config.rolePermissions)
    applyLocalRolePermissions(config.rolePermissions)
    ElMessage.success('项目角色权限配置已保存')
    emit('saved')
    open.value = false
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.wk-project-role-permission-dialog :deep(.el-dialog__header) {
  padding: 22px 24px 14px;
  margin-right: 0;
}

.wk-project-role-permission-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.wk-project-role-permission-dialog :deep(.el-dialog__footer) {
  padding: 14px 24px 22px;
}
</style>
