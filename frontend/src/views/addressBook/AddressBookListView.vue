<template>
  <div class="flex h-full flex-col gap-6 px-4 py-6 md:px-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div class="min-w-0">
        <h2 class="text-[22px] font-bold text-slate-900">通讯录</h2>
        <p class="mt-1 text-[13px] text-slate-500">企业内部员工与员工对象工作记录</p>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div class="relative group flex items-center">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-primary">search</span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索姓名、部门、职位、手机或邮箱"
            class="h-10 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm shadow-sm outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20 sm:w-80"
            @input="debouncedLoadEmployees"
            @keydown.enter="loadEmployees"
          />
        </div>
        <select
          v-model="employeeStatus"
          class="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-600 shadow-sm outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
          @change="loadEmployees"
        >
          <option value="">全部状态</option>
          <option value="active">在职</option>
          <option value="resigned">离职</option>
          <option value="disabled">停用</option>
        </select>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm" v-loading="loading">
      <div class="min-h-0 flex-1">
        <el-table
          v-if="!isMobile"
          :data="employees"
          height="100%"
          row-key="userId"
          table-layout="fixed"
          class="wk-customer-table wk-address-book-table"
          empty-text="暂无员工数据"
          @row-click="openEmployeeChat"
        >
          <el-table-column label="员工" min-width="240">
            <template #header>
              <span class="normal-case tracking-normal">员工</span>
            </template>
            <template #default="{ row }">
              <div class="flex min-w-0 items-center gap-3">
                <div class="flex size-8 shrink-0 items-center justify-center overflow-hidden rounded border border-slate-200 bg-white">
                  <img v-if="row.imgUrl" :src="row.imgUrl" class="size-full object-cover" alt="avatar" />
                  <span v-else class="flex size-full items-center justify-center bg-primary/10 text-xs font-bold text-primary">
                    {{ employeeInitial(row) }}
                  </span>
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-semibold text-slate-900 transition-colors hover:text-primary hover:underline hover:decoration-primary underline-offset-2">{{ employeeName(row) }}</p>
                  <p class="truncate text-xs text-slate-400">{{ row.email || '-' }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="部门" min-width="150">
            <template #header>
              <span class="normal-case tracking-normal">部门</span>
            </template>
            <template #default="{ row }">
              <span class="block truncate text-sm text-slate-600">{{ row.deptName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="职位" min-width="150">
            <template #header>
              <span class="normal-case tracking-normal">职位</span>
            </template>
            <template #default="{ row }">
              <span class="block truncate text-sm text-slate-600">{{ row.post || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="150">
            <template #header>
              <span class="normal-case tracking-normal">手机号</span>
            </template>
            <template #default="{ row }">
              <span class="block truncate text-sm font-mono text-slate-600">{{ row.mobile || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #header>
              <span class="normal-case tracking-normal">状态</span>
            </template>
            <template #default="{ row }">
              <span class="inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap" :class="employeeStatusClass(row)">
                <span class="mr-1.5 size-1.5 rounded-full" :class="employeeStatusDotClass(row)"></span>
                {{ employeeStatusLabel(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最近相关任务" min-width="160">
            <template #header>
              <span class="normal-case tracking-normal">最近相关任务</span>
            </template>
            <template #default="{ row }">
              <span class="text-sm text-slate-500">{{ formatDateTime(row.recentTaskTime) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100" fixed="right">
            <template #header>
              <span class="normal-case tracking-normal">操作</span>
            </template>
            <template #default="{ row }">
              <el-button size="small" @click.stop="messageEmployee(row.userId)">发消息</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="text-center py-16">
              <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
                <span class="material-symbols-outlined text-4xl">contacts</span>
              </div>
              <p class="text-slate-400 text-sm font-medium">{{ keyword.trim() ? '未找到匹配员工' : '暂无员工数据' }}</p>
            </div>
          </template>
        </el-table>

        <div v-else class="wk-customer-cards h-full overflow-y-auto px-3 py-3">
          <div v-if="employees.length === 0" class="text-center py-16">
            <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
              <span class="material-symbols-outlined text-4xl">contacts</span>
            </div>
            <p class="text-slate-400 text-sm font-medium">{{ keyword.trim() ? '未找到匹配员工' : '暂无员工数据' }}</p>
          </div>

          <div v-else class="flex flex-col gap-3">
            <button
              v-for="employee in employees"
              :key="employee.userId"
              type="button"
              class="wk-customer-card w-full cursor-pointer rounded-xl border border-slate-200 bg-white px-4 py-3 text-left shadow-sm transition-colors hover:bg-slate-50 active:bg-slate-100"
              @click="openEmployeeChat(employee)"
            >
              <div class="flex items-start gap-3">
                <div class="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                  <img v-if="employee.imgUrl" :src="employee.imgUrl" class="size-full object-cover" alt="avatar" />
                  <span v-else class="flex size-full items-center justify-center bg-primary/10 text-sm font-bold text-primary">
                    {{ employeeInitial(employee) }}
                  </span>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex items-start gap-2">
                    <div class="min-w-0 flex-1">
                      <div class="truncate text-sm font-bold text-slate-900">{{ employeeName(employee) }}</div>
                      <p class="mt-0.5 truncate text-xs text-slate-400">{{ employee.post || '员工' }} · {{ employee.deptName || '-' }}</p>
                    </div>
                    <span class="material-symbols-outlined pt-0.5 text-base leading-none text-slate-300">chevron_right</span>
                  </div>

                  <div class="mt-3 grid grid-cols-2 gap-x-3 gap-y-2">
                    <div class="min-w-0">
                      <div class="text-[11px] font-bold uppercase tracking-wide text-slate-400">手机</div>
                      <div class="truncate text-sm text-slate-600">{{ employee.mobile || '-' }}</div>
                    </div>
                    <div class="min-w-0">
                      <div class="text-[11px] font-bold uppercase tracking-wide text-slate-400">状态</div>
                      <div class="mt-0.5">
                        <span class="inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap" :class="employeeStatusClass(employee)">
                          <span class="mr-1.5 size-1.5 rounded-full" :class="employeeStatusDotClass(employee)"></span>
                          {{ employeeStatusLabel(employee) }}
                        </span>
                      </div>
                    </div>
                    <div class="col-span-2 min-w-0">
                      <div class="text-[11px] font-bold uppercase tracking-wide text-slate-400">邮箱</div>
                      <div class="truncate text-sm text-slate-600">{{ employee.email || '-' }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <div v-if="total > 0" class="flex shrink-0 items-center justify-between border-t border-slate-200 bg-slate-50/50 px-6 py-4 text-sm text-slate-500">
        <span>共 {{ total }} 条<span class="hidden md:inline">员工数据</span></span>
        <div class="flex items-center gap-1">
          <button class="flex size-8 items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50" :disabled="page <= 1" @click="changePage(page - 1)">
            <span class="material-symbols-outlined text-lg">chevron_left</span>
          </button>
          <template v-if="!isMobile">
            <button
              v-for="pageNum in visiblePages"
              :key="pageNum"
              class="flex size-8 items-center justify-center rounded border text-xs font-bold"
              :class="pageNum === page
                ? 'border-primary bg-primary text-white'
                : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'"
              @click="changePage(pageNum)"
            >
              {{ pageNum }}
            </button>
            <span v-if="totalPages > 5" class="px-1 text-xs text-slate-400">...</span>
          </template>
          <span v-else class="px-2">{{ page }} / {{ totalPages }}</span>
          <button class="flex size-8 items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50" :disabled="page >= totalPages" @click="changePage(page + 1)">
            <span class="material-symbols-outlined text-lg">chevron_right</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryAddressBook } from '@/api/addressBook'
import { useResponsive } from '@/composables/useResponsive'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { AddressBookEmployee } from '@/types/addressBook'

const router = useRouter()
const { isMobile } = useResponsive()

const employees = ref<AddressBookEmployee[]>([])
const loading = ref(false)
const keyword = ref('')
const employeeStatus = ref('')
const page = ref(1)
const limit = ref(20)
const total = ref(0)
let searchTimer: number | null = null

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / limit.value)))
const visiblePages = computed(() => {
  const totalPageCount = totalPages.value
  const current = page.value
  const pages: number[] = []
  const maxVisible = 5
  let start = Math.max(1, current - Math.floor(maxVisible / 2))
  const end = Math.min(totalPageCount, start + maxVisible - 1)
  start = Math.max(1, end - maxVisible + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

onMounted(() => {
  void loadEmployees()
})

function debouncedLoadEmployees() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    void loadEmployees()
  }, 260)
}

async function loadEmployees() {
  loading.value = true
  try {
    const result = await queryAddressBook({
      keyword: keyword.value.trim() || undefined,
      employeeStatus: employeeStatus.value || undefined,
      page: page.value,
      limit: limit.value
    })
    employees.value = result.list || []
    total.value = Number(result.totalRow || 0)
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.warning('通讯录加载失败')
    }
    employees.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  void loadEmployees()
}

function employeeName(employee: AddressBookEmployee) {
  return employee.realname || '未命名员工'
}

function employeeInitial(employee: AddressBookEmployee) {
  return employeeName(employee).charAt(0) || '?'
}

function normalizeStatus(status?: string) {
  return status === 'resigned' || status === 'disabled' ? status : 'active'
}

function employeeStatusLabel(employee: AddressBookEmployee) {
  if (employee.employeeStatusName) return employee.employeeStatusName
  const status = normalizeStatus(employee.employeeStatus)
  if (status === 'resigned') return '离职'
  if (status === 'disabled') return '停用'
  return '在职'
}

function employeeStatusClass(employee: AddressBookEmployee) {
  const status = normalizeStatus(employee.employeeStatus)
  if (status === 'resigned') return 'bg-slate-100 text-slate-600'
  if (status === 'disabled') return 'bg-amber-50 text-amber-700'
  return 'bg-emerald-50 text-emerald-700'
}

function employeeStatusDotClass(employee: AddressBookEmployee) {
  const status = normalizeStatus(employee.employeeStatus)
  if (status === 'resigned') return 'bg-slate-400'
  if (status === 'disabled') return 'bg-amber-500'
  return 'bg-emerald-500'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function openEmployeeChat(employee: AddressBookEmployee) {
  router.push({ path: '/chat', query: { employeeId: employee.userId } })
}

function messageEmployee(userId: string | number) {
  void router.push({ path: '/im', query: { peer: String(userId) } })
}
</script>

<style scoped>
.wk-customer-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.wk-customer-table {
  --el-table-bg-color: var(--wk-bg-surface);
  --el-table-tr-bg-color: var(--wk-bg-surface);
  --el-table-header-bg-color: var(--wk-bg-surface-subtle);
  --el-table-header-text-color: var(--wk-text-muted);
  --el-table-text-color: var(--wk-text-secondary);
  --el-table-border-color: var(--wk-border-subtle);
  --el-table-row-hover-bg-color: transparent;
  --wk-customer-table-row-hover-bg-color: color-mix(in srgb, var(--wk-primary) 11%, var(--wk-bg-surface));
}

.wk-customer-table :deep(.el-table__border-left-patch),
.wk-customer-table :deep(.el-table__fixed-right-patch) {
  background: var(--wk-bg-surface-subtle);
}

.wk-customer-table :deep(th.el-table__cell) {
  background: var(--wk-bg-surface-subtle);
  color: var(--wk-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 16px 0;
  border-bottom: 1px solid var(--wk-border-muted);
}

.wk-customer-table :deep(td.el-table__cell) {
  padding: 16px 0;
  border-bottom: 1px solid var(--wk-border-subtle);
}

.wk-customer-table :deep(.el-table__row) {
  cursor: pointer;
}

.wk-customer-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: var(--wk-customer-table-row-hover-bg-color);
}

.wk-customer-table :deep(.el-table__empty-block) {
  min-height: 220px;
}

.wk-customer-card {
  -webkit-tap-highlight-color: transparent;
}
</style>
