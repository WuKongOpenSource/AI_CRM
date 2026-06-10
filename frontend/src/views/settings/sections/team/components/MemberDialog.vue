<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingMember ? '编辑员工' : '新增员工'"
    :width="isMobile ? 'calc(100vw - 2rem)' : '650px'"
    append-to-body
    class="wk-dialog--flush wk-member-dialog wk-crm-el-field-scope wk-mobile-inset-dialog"
  >
    <el-form :model="memberForm" label-position="top">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="姓名" required>
            <el-input v-model="memberForm.realname" placeholder="请输入员工姓名" class="w-full wk-crm-el-field-input" size="large" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="职位">
            <el-input v-model="memberForm.post" placeholder="如：销售经理" class="w-full wk-crm-el-field-input" size="large" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="用户名" :required="!editingMember">
            <el-input
              v-model="memberForm.username"
              :disabled="!!editingMember"
              :placeholder="editingMember ? '' : '用于系统登录'"
              class="w-full wk-crm-el-field-input"
              size="large"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="登录密码" :required="!editingMember">
            <el-input
              v-model="memberForm.password"
              type="password"
              show-password
              :placeholder="editingMember ? '留空则不修改' : '默认密码 123456'"
              class="w-full wk-crm-el-field-input"
              size="large"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="所属部门">
            <el-tree-select
              v-model="memberForm.deptId"
              :data="deptTree"
              :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
              placeholder="请选择部门"
              clearable
              check-strictly
              :render-after-expand="false"
              class="w-full wk-crm-el-field-select"
              size="large"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="直属上级">
            <el-select
              v-model="memberForm.parentId"
              placeholder="无 (顶级员工)"
              clearable
              filterable
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option :value="null" label="无 (顶级员工)" />
              <el-option
                v-for="user in parentOptions"
                :key="user.userId"
                :label="user.realname || user.username"
                :value="user.userId"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="电子邮箱">
            <el-input v-model="memberForm.email" placeholder="example@wukong.ai" class="w-full wk-crm-el-field-input" size="large" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="手机号码">
            <el-input v-model="memberForm.mobile" placeholder="请输入手机号" class="w-full wk-crm-el-field-input" size="large" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
      <div>
        <label class="text-sm font-medium text-slate-700 mb-2 block">员工状态</label>
        <el-radio-group v-model="memberForm.employeeStatus">
          <el-radio value="active">在职</el-radio>
          <el-radio value="resigned">离职</el-radio>
          <el-radio value="disabled">停用</el-radio>
        </el-radio-group>
      </div>
      <div>
        <label class="text-sm font-medium text-slate-700 mb-2 block">账号状态</label>
        <el-radio-group v-model="memberForm.status" :disabled="isEditingTenantCreator">
          <el-radio :value="1">活跃</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </div>
    </div>

    <div class="mt-4">
      <div class="flex items-center justify-between mb-3">
        <label class="text-sm font-bold text-slate-900">系统角色权限</label>
        <span class="text-xs text-primary">已选择 {{ memberForm.roleIds.length }} 个角色</span>
      </div>
      <div class="wk-member-dialog__role-scroll border border-slate-200 rounded-xl p-4 max-h-48 overflow-y-auto bg-slate-50/50">
        <el-checkbox-group v-model="memberForm.roleIds" :disabled="isEditingTenantCreator">
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-3">
            <el-checkbox
              v-for="role in allRoleOptions"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
              class="!mr-0"
            />
          </div>
        </el-checkbox-group>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3 w-full">
        <el-button class="flex-1" size="large" @click="dialogVisible = false">取消</el-button>
        <el-button class="flex-1" size="large" type="primary" :loading="submitting" @click="$emit('save')">保存员工信息</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DeptVO } from '@/types/dept'
import type { RoleVO } from '@/types/role'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  editingMember: any
  memberForm: {
    username: string
    realname: string
    password: string
    mobile: string
    email: string
    post: string
    deptId: number | string | null
    parentId: number | string | null
    status: number
    employeeStatus: string
    roleIds: string[]
  }
  deptTree: DeptVO[]
  parentOptions: any[]
  allRoleOptions: RoleVO[]
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

const isEditingTenantCreator = computed(() => Boolean(props.editingMember?.tenantCreator))
</script>

<style>
/* 类名挂在 Teleport 后的 .el-dialog 上，用双类限定作用域；flex + 仅 body 滚动，避免滚动链带动整块弹窗晃动 */
.el-dialog.wk-member-dialog {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  margin-top: 5vh !important;
  overscroll-behavior: none;
}

.el-dialog.wk-member-dialog.is-fullscreen {
  max-height: none;
  height: 100%;
  margin: 0 !important;
}

.el-dialog.wk-member-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.el-dialog.wk-member-dialog .wk-member-dialog__role-scroll {
  overscroll-behavior: contain;
}

@media (max-width: 767px) {
  .el-dialog.wk-member-dialog.wk-mobile-inset-dialog {
    --wk-member-dialog-top: max(96px, calc(var(--wk-safe-top) + 72px));
    --wk-member-dialog-bottom: max(16px, var(--wk-safe-bottom));

    max-height: calc(100dvh - var(--wk-member-dialog-top) - var(--wk-member-dialog-bottom));
    margin: var(--wk-member-dialog-top) auto var(--wk-member-dialog-bottom) !important;
  }
}
</style>
