<template>
  <div
    class="min-h-0 md:flex-1 md:overflow-hidden max-md:flex-none max-md:overflow-visible px-4 py-6 md:px-6"
  >
    <div class="w-full min-h-0 flex flex-col gap-6 md:h-full max-md:h-auto">
      <div v-if="isMobile" class="flex items-center gap-2 p-4 border border-slate-100 bg-white rounded-2xl">
        <button
          class="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-xl text-sm max-w-[200px]"
          @click="$emit('open-dept-drawer')"
        >
          <span class="material-symbols-outlined text-slate-400 text-base">account_tree</span>
          <span class="truncate">{{ selectedDept ? selectedDept.deptName : '选择部门' }}</span>
        </button>
        <button
          class="px-4 py-2 bg-primary text-white rounded-xl text-sm font-bold flex items-center gap-1 hover:bg-primary/90 transition-all disabled:opacity-50"
          :disabled="!selectedDept"
          @click="$emit('add-member')"
        >
          <span class="material-symbols-outlined wk-plus-button-icon">person_add</span>
          添加成员
        </button>
      </div>

      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-slate-900 truncate">
            {{ selectedDept ? selectedDept.deptName : '组织员工管理' }}
          </h2>
          <p class="text-sm text-slate-500 mt-1">管理您的团队成员、部门分配及账号状态</p>
        </div>
        <button
          v-if="!isMobile"
          class="px-6 py-2.5 bg-primary text-white rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2 disabled:opacity-50"
          :disabled="!selectedDept"
          @click="$emit('add-member')"
        >
          <span class="material-symbols-outlined wk-plus-button-icon">person_add</span>
          添加员工
        </button>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
        <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
          <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">总员工数</p>
          <p class="text-2xl md:text-3xl font-black text-slate-900">{{ deptMemberList.length }}</p>
        </div>
        <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
          <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">活跃账号</p>
          <p class="text-2xl md:text-3xl font-black text-emerald-500">{{ deptMemberList.filter((member) => member.status === 1).length }}</p>
        </div>
        <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
          <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">待激活</p>
          <p class="text-2xl md:text-3xl font-black text-amber-500">{{ deptMemberList.filter((member) => member.status !== 1 && member.status !== 0).length }}</p>
        </div>
        <div class="bg-white p-4 md:p-6 rounded-2xl border border-slate-200 shadow-sm">
          <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">部门数量</p>
          <p class="text-2xl md:text-3xl font-black text-primary">{{ deptCount }}</p>
        </div>
      </div>

      <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-0 max-md:flex-none md:flex-1">
        <div v-if="loadingMembers" class="flex-1 min-h-0 flex items-center justify-center text-center py-16">
          <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
        </div>
        <template v-else-if="deptMemberList.length === 0">
          <div class="flex-1 min-h-0 flex flex-col items-center justify-center text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-3xl mb-2 opacity-50">group_off</span>
            <p class="text-sm">{{ selectedDept ? '该部门暂无成员' : '请先选择一个部门' }}</p>
          </div>
        </template>
        <template v-else>
          <div class="p-4 border-b border-slate-100 bg-slate-50/50 flex items-center gap-4 shrink-0">
            <div class="relative flex-1">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">search</span>
              <input
                v-model="searchValue"
                type="text"
                placeholder="搜索员工姓名、邮箱或手机号..."
                class="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary/50 outline-none transition-shadow"
              />
            </div>
            <select
              v-model="roleFilterValue"
              class="bg-white border border-slate-200 rounded-xl px-4 py-2 text-sm text-slate-600 outline-none focus:ring-2 focus:ring-primary/50"
            >
              <option :value="'0'">所有角色</option>
              <option v-for="role in allRoleOptions" :key="role.roleId" :value="String(role.roleId)">
                {{ role.roleName }}
              </option>
            </select>
          </div>

          <div class="min-h-0 max-md:flex-none md:flex-1">
            <el-table
              v-if="!isMobile"
              :data="filteredMembers"
              height="100%"
              row-key="userId"
              table-layout="fixed"
              tooltip-effect="dark"
              class="wk-member-table"
              empty-text="未找到匹配的员工"
              @row-click="$emit('row-click', $event)"
            >
              <el-table-column label="员工" min-width="260">
                <template #default="{ row }">
                  <div class="flex items-center gap-3 min-w-0">
                    <img v-if="row.imgUrl" :src="row.imgUrl" class="size-10 rounded-full object-cover shrink-0 shadow-sm" alt="avatar" />
                    <div v-else class="size-10 rounded-full flex items-center justify-center text-white text-sm font-bold shrink-0 shadow-sm" :class="getAvatarColor(row.realname)">
                      {{ (row.realname || row.username || '?').charAt(0) }}
                    </div>
                    <div class="min-w-0">
                      <el-tooltip
                        :key="`member-name-${row.userId}-${getMemberDisplayName(row)}`"
                        :content="getMemberDisplayName(row)"
                        effect="dark"
                        placement="top"
                        popper-class="wk-sidebar-like-tooltip"
                        :show-arrow="false"
                      >
                        <p class="text-sm font-bold text-slate-900 hover:text-primary transition-colors truncate">
                          {{ getMemberDisplayName(row) }}
                        </p>
                      </el-tooltip>
                      <el-tooltip
                        v-if="getMemberEmail(row)"
                        :key="`member-email-${row.userId}-${getMemberEmail(row)}`"
                        :content="getMemberEmail(row)"
                        effect="dark"
                        placement="top"
                        popper-class="wk-sidebar-like-tooltip"
                        :show-arrow="false"
                      >
                        <p class="text-xs text-slate-400 truncate">{{ getMemberEmail(row) }}</p>
                      </el-tooltip>
                      <p v-else class="text-xs text-slate-300 truncate">-</p>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="部门" min-width="160">
                <template #default>
                  <el-tooltip
                    :content="selectedDept?.deptName || '-'"
                    effect="dark"
                    placement="top"
                    popper-class="wk-sidebar-like-tooltip"
                    :show-arrow="false"
                  >
                    <span class="block text-sm text-slate-700 truncate">{{ selectedDept?.deptName || '-' }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>

              <el-table-column label="角色" min-width="220">
                <template #default="{ row }">
                  <div class="flex flex-wrap gap-1 min-w-0">
                    <span v-for="roleName in row.roleNames || []" :key="roleName" class="px-2 py-0.5 bg-amber-50 text-amber-700 rounded text-xs font-bold max-w-full truncate">
                      {{ roleName }}
                    </span>
                    <span v-if="!row.roleNames || row.roleNames.length === 0" class="text-sm text-slate-400">-</span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="联系方式" min-width="180">
                <template #default="{ row }">
                  <div class="min-w-0">
                    <el-tooltip
                      v-if="getMemberMobile(row)"
                      :key="`member-mobile-${row.userId}-${getMemberMobile(row)}`"
                      :content="getMemberMobile(row)"
                      effect="dark"
                      placement="top"
                      popper-class="wk-sidebar-like-tooltip"
                      :show-arrow="false"
                    >
                      <p class="text-sm text-slate-700 truncate">{{ getMemberMobile(row) }}</p>
                    </el-tooltip>
                    <p v-else class="text-sm text-slate-300 truncate">-</p>
                    <el-tooltip
                      v-if="getMemberPost(row)"
                      :key="`member-post-${row.userId}-${getMemberPost(row)}`"
                      :content="getMemberPost(row)"
                      effect="dark"
                      placement="top"
                      popper-class="wk-sidebar-like-tooltip"
                      :show-arrow="false"
                    >
                      <p class="text-xs text-slate-400 truncate">{{ getMemberPost(row) }}</p>
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  <span
                    class="px-2 py-1 rounded-full text-xs font-bold uppercase tracking-widest"
                    :class="row.status === 1 ? 'bg-emerald-50 text-emerald-600' : row.status === 0 ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-400'"
                  >
                    {{ row.status === 1 ? '活跃' : row.status === 0 ? '禁用' : '未激活' }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="152" align="right" fixed="right">
                <template #default="{ row }">
                  <div class="wk-member-actions flex items-center justify-end gap-2 transition-opacity">
                    <button
                      type="button"
                      class="size-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-400 hover:text-primary transition-all"
                      title="编辑"
                      @click.stop="$emit('edit-member', row)"
                    >
                      <span class="material-symbols-outlined text-sm">edit</span>
                    </button>
                    <button
                      type="button"
                      class="size-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-400 hover:text-primary transition-all"
                      title="重置用户名"
                      @click.stop="$emit('reset-username', row)"
                    >
                      <span class="material-symbols-outlined text-sm">manage_accounts</span>
                    </button>
                    <button
                      type="button"
                      class="size-8 flex items-center justify-center rounded-lg hover:bg-white hover:shadow-sm text-slate-400 hover:text-amber-500 transition-all"
                      :title="row.status === 1 ? '停用' : '启用'"
                      @click.stop="$emit('toggle-status', row)"
                    >
                      <span class="material-symbols-outlined text-sm">{{ row.status === 1 ? 'block' : 'check_circle' }}</span>
                    </button>
                  </div>
                </template>
              </el-table-column>

              <template #empty>
                <div class="text-center py-16 text-slate-400">
                  <span class="material-symbols-outlined text-3xl mb-2 opacity-50">search_off</span>
                  <div class="text-sm">未找到匹配的员工</div>
                </div>
              </template>
            </el-table>

            <div v-else class="p-3 space-y-3">
              <div v-if="filteredMembers.length === 0" class="text-center py-16 text-slate-400">
                <span class="material-symbols-outlined text-3xl mb-2 opacity-50">search_off</span>
                <div class="text-sm">未找到匹配的员工</div>
              </div>

              <div
                v-for="row in filteredMembers"
                v-else
                :key="row.userId"
                role="button"
                tabindex="0"
                class="w-full text-left bg-white border border-slate-200 rounded-2xl px-4 py-3 shadow-sm active:bg-slate-50 transition-colors"
                @click="$emit('row-click', row)"
                @keydown.enter.prevent="$emit('row-click', row)"
                @keydown.space.prevent="$emit('row-click', row)"
              >
                <div class="flex items-start gap-3">
                  <img v-if="row.imgUrl" :src="row.imgUrl" class="size-11 rounded-full object-cover shrink-0 shadow-sm" alt="avatar" />
                  <div v-else class="size-11 rounded-full flex items-center justify-center text-white text-sm font-bold shrink-0 shadow-sm" :class="getAvatarColor(row.realname)">
                    {{ (row.realname || row.username || '?').charAt(0) }}
                  </div>

                  <div class="min-w-0 flex-1">
                    <div class="flex items-start justify-between gap-2">
                      <div class="min-w-0">
                        <p class="text-sm font-bold text-slate-900 truncate">
                          {{ row.realname || row.username || '-' }}
                        </p>
                        <p class="text-xs text-slate-400 truncate">
                          {{ row.email || '-' }}
                        </p>
                      </div>
                      <span
                        class="shrink-0 px-2 py-1 rounded-full text-[11px] font-bold uppercase tracking-widest"
                        :class="row.status === 1 ? 'bg-emerald-50 text-emerald-600' : row.status === 0 ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-400'"
                      >
                        {{ row.status === 1 ? '活跃' : row.status === 0 ? '禁用' : '未激活' }}
                      </span>
                    </div>

                    <div class="mt-3 grid grid-cols-2 gap-x-3 gap-y-2 text-sm">
                      <div class="min-w-0">
                        <p class="text-[11px] font-bold text-slate-400 tracking-wide uppercase">department</p>
                        <p class="text-sm text-slate-700 truncate">{{ selectedDept?.deptName || '-' }}</p>
                      </div>
                      <div class="min-w-0">
                        <p class="text-[11px] font-bold text-slate-400 tracking-wide uppercase">mobile</p>
                        <p class="text-sm text-slate-700 truncate">{{ row.mobile || '-' }}</p>
                      </div>
                      <div class="min-w-0 col-span-2">
                        <p class="text-[11px] font-bold text-slate-400 tracking-wide uppercase">roles</p>
                        <div class="mt-1 flex flex-wrap gap-1">
                          <span
                            v-for="roleName in row.roleNames || []"
                            :key="roleName"
                            class="px-2 py-0.5 bg-amber-50 text-amber-700 rounded text-xs font-bold max-w-full truncate"
                          >
                            {{ roleName }}
                          </span>
                          <span v-if="!row.roleNames || row.roleNames.length === 0" class="text-sm text-slate-400">-</span>
                        </div>
                      </div>
                    </div>

                    <div class="mt-3 flex items-center justify-end gap-2">
                      <button
                        type="button"
                        class="h-8 px-3 rounded-xl border border-slate-200 text-slate-600 text-xs font-bold hover:bg-slate-50 transition-colors whitespace-nowrap"
                        @click.stop="$emit('edit-member', row)"
                      >
                        编辑
                      </button>
                      <button
                        type="button"
                        class="h-8 px-3 rounded-xl border border-slate-200 text-slate-600 text-xs font-bold hover:bg-slate-50 transition-colors whitespace-nowrap"
                        @click.stop="$emit('reset-username', row)"
                      >
                        重置用户名
                      </button>
                      <button
                        type="button"
                        class="h-8 px-3 rounded-xl border text-xs font-bold whitespace-nowrap transition-colors"
                        :class="row.status === 1 ? 'border-amber-200 text-amber-600 bg-amber-50 hover:bg-amber-100' : 'border-emerald-200 text-emerald-600 bg-emerald-50 hover:bg-emerald-100'"
                        @click.stop="$emit('toggle-status', row)"
                      >
                        {{ row.status === 1 ? '停用' : '启用' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="memberTotal > 0"
            class="shrink-0 flex items-center justify-between border-t border-slate-200 bg-slate-50/50 px-4 py-4 md:px-6"
          >
            <span class="text-sm text-slate-500">
              共 {{ memberTotal }} 条<span class="hidden md:inline">员工数据</span>
            </span>
            <div class="flex items-center gap-1">
              <button
                class="size-8 flex items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50"
                :disabled="memberPage <= 1"
                @click="$emit('page-change', memberPage - 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_left</span>
              </button>
              <button
                v-for="pageNum in visibleMemberPages"
                :key="pageNum"
                class="size-8 flex items-center justify-center rounded border text-xs font-bold"
                :class="pageNum === memberPage
                  ? 'border-primary bg-primary text-white'
                  : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'"
                @click="$emit('page-change', pageNum)"
              >{{ pageNum }}</button>
              <span v-if="memberTotalPages > 5" class="px-1 text-slate-400 text-xs">...</span>
              <button
                class="size-8 flex items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50"
                :disabled="memberPage >= memberTotalPages"
                @click="$emit('page-change', memberPage + 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_right</span>
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DeptVO } from '@/types/dept'
import type { RoleVO } from '@/types/role'

const props = defineProps<{
  isMobile: boolean
  selectedDept: DeptVO | null
  deptMemberList: any[]
  filteredMembers: any[]
  loadingMembers: boolean
  memberSearch: string
  memberRoleId: string
  memberPage: number
  memberPageSize: number
  memberTotal: number
  memberTotalPages: number
  visibleMemberPages: number[]
  allRoleOptions: RoleVO[]
  deptCount: number
  getAvatarColor: (name: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:memberSearch', value: string): void
  (e: 'update:memberRoleId', value: string): void
  (e: 'page-change', page: number): void
  (e: 'open-dept-drawer'): void
  (e: 'add-member'): void
  (e: 'row-click', member: any): void
  (e: 'edit-member', member: any): void
  (e: 'toggle-status', member: any): void
  (e: 'reset-username', member: any): void
}>()

const searchValue = computed({
  get: () => props.memberSearch,
  set: (value: string) => emit('update:memberSearch', value)
})

const roleFilterValue = computed({
  get: () => props.memberRoleId,
  set: (value: string) => emit('update:memberRoleId', value)
})

function getMemberDisplayName(member: any) {
  return String(member?.realname || member?.username || '-').trim() || '-'
}

function getMemberEmail(member: any) {
  return String(member?.email || '').trim()
}

function getMemberMobile(member: any) {
  return String(member?.mobile || '').trim()
}

function getMemberPost(member: any) {
  return String(member?.post || '').trim()
}
</script>

<style scoped>
:deep(.wk-member-table .el-table__row) {
  cursor: pointer;
}

:deep(.wk-member-table .el-table__body tr:hover .wk-member-actions) {
  opacity: 1;
}

:deep(.wk-member-table .el-table__body tr .wk-member-actions) {
  opacity: 0;
}

:deep(.wk-member-table .el-table__cell) {
  vertical-align: middle;
}

</style>
