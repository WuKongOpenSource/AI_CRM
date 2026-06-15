<template>
  <div class="flex h-full flex-col gap-5 bg-slate-50 px-4 py-5 md:px-8">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div class="min-w-0">
        <h1 class="text-xl font-bold text-slate-900">关系</h1>
        <p class="mt-1 text-sm text-slate-500">共 {{ total }} 位外部联系人</p>
      </div>

      <div class="flex flex-col gap-3 md:flex-row md:items-center">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">search</span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索姓名、电话、微信、邮箱或客户"
            class="h-10 w-full rounded-lg border border-slate-200 bg-white pl-10 pr-4 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 md:w-80"
            @input="debouncedLoadRelations"
            @keydown.enter="loadRelations"
          />
        </div>
        <select
          v-model="relationType"
          class="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-600 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
          @change="loadRelations"
        >
          <option value="">全部类型</option>
          <option v-for="item in relationTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <button
          type="button"
          class="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-bold text-white shadow-sm transition hover:bg-primary/90"
          @click="openCreateDialog"
        >
          <span class="material-symbols-outlined text-[18px] leading-none">person_add</span>
          新建关系
        </button>
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-hidden rounded-lg border border-slate-200 bg-white" v-loading="loading">
      <el-table
        v-if="!isMobile"
        :data="relations"
        height="100%"
        row-key="relationId"
        table-layout="fixed"
        empty-text="暂无关系人"
        @row-click="openDetail"
      >
        <el-table-column label="关系人" min-width="230">
          <template #default="{ row }">
            <div class="flex min-w-0 items-center gap-3">
              <div class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                <img v-if="row.avatarUrl" :src="row.avatarUrl" class="size-full object-cover" alt="avatar" />
                <span v-else class="flex size-full items-center justify-center bg-primary/10 text-sm font-bold text-primary">
                  {{ relationInitial(row) }}
                </span>
              </div>
              <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-slate-900">{{ row.name }}</p>
                <p class="truncate text-xs text-slate-400">{{ row.phone || row.email || row.wechat || '-' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <span class="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
              {{ relationTypeLabel(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="关联客户" min-width="190">
          <template #default="{ row }">
            <span class="block truncate text-sm text-slate-600">{{ row.customerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="130">
          <template #default="{ row }">
            <span class="text-sm text-slate-500">{{ sourceLabel(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="220">
          <template #default="{ row }">
            <span class="block truncate text-sm text-slate-500">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            <span class="text-sm text-slate-500">{{ formatDateTime(row.updateTime || row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="112" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1" @click.stop>
              <button class="relation-icon-button" title="编辑" type="button" @click="openEditDialog(row)">
                <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
              </button>
              <button class="relation-icon-button text-rose-500 hover:bg-rose-50" title="删除" type="button" @click="handleDelete(row)">
                <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="h-full overflow-y-auto px-3 py-3">
        <div v-if="relations.length === 0" class="py-16 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl">diversity_3</span>
          <p class="mt-3 text-sm">{{ keyword.trim() ? '未找到匹配关系人' : '暂无关系人' }}</p>
        </div>
        <div v-else class="flex flex-col gap-3">
          <button
            v-for="relation in relations"
            :key="relation.relationId"
            type="button"
            class="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-left transition active:bg-slate-100"
            @click="openDetail(relation)"
          >
            <div class="flex items-start gap-3">
              <div class="flex size-10 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white">
                <span class="flex size-full items-center justify-center bg-primary/10 text-sm font-bold text-primary">
                  {{ relationInitial(relation) }}
                </span>
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-start gap-2">
                  <div class="min-w-0 flex-1">
                    <div class="truncate text-sm font-bold text-slate-900">{{ relation.name }}</div>
                    <p class="mt-0.5 truncate text-xs text-slate-400">
                      {{ relationTypeLabel(relation) }} · {{ relation.customerName || '未关联客户' }}
                    </p>
                  </div>
                  <span class="material-symbols-outlined text-base leading-none text-slate-300">chevron_right</span>
                </div>
                <p class="mt-3 truncate text-sm text-slate-600">{{ relation.phone || relation.email || relation.wechat || '-' }}</p>
                <p class="mt-1 line-clamp-2 text-xs leading-5 text-slate-400">{{ relation.remark || '暂无备注' }}</p>
              </div>
            </div>
          </button>
        </div>
      </div>
    </div>

    <div v-if="total > 0" class="flex items-center justify-between text-sm text-slate-500">
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <div class="flex items-center gap-2">
        <button class="rounded-lg border border-slate-200 bg-white px-3 py-2 disabled:opacity-40" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <button class="rounded-lg border border-slate-200 bg-white px-3 py-2 disabled:opacity-40" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :fullscreen="isMobile"
      :width="isMobile ? '100%' : '560px'"
      :title="editingRelation ? '编辑关系' : '新建关系'"
      destroy-on-close
    >
      <el-form label-position="top" @submit.prevent>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <el-form-item label="姓名" required>
            <el-input v-model="form.name" maxlength="100" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="关系类型">
            <el-select v-model="form.relationType" class="w-full" placeholder="请选择">
              <el-option v-for="item in relationTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" maxlength="50" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="微信号">
            <el-input v-model="form.wechat" maxlength="100" placeholder="请输入微信号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" maxlength="100" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="关联客户">
            <el-select
              v-model="form.customerId"
              class="w-full"
              clearable
              filterable
              remote
              reserve-keyword
              placeholder="搜索客户"
              :remote-method="searchCustomers"
              :loading="customerLoading"
            >
              <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="补充关系背景、偏好或合作线索" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <button type="button" class="rounded-lg px-4 py-2 text-sm font-medium text-slate-500 hover:bg-slate-100" @click="dialogVisible = false">取消</button>
          <button
            type="button"
            class="rounded-lg bg-primary px-4 py-2 text-sm font-bold text-white disabled:opacity-50"
            :disabled="saving"
            @click="saveRelation"
          >
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :size="isMobile ? '100%' : '460px'" title="关系详情" append-to-body>
      <div v-loading="detailLoading" class="space-y-5">
        <template v-if="currentDetail">
          <div class="flex items-center gap-3">
            <div class="flex size-12 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white">
              <span class="flex size-full items-center justify-center bg-primary/10 text-base font-bold text-primary">
                {{ relationInitial(currentDetail) }}
              </span>
            </div>
            <div class="min-w-0">
              <h2 class="truncate text-lg font-bold text-slate-900">{{ currentDetail.name }}</h2>
              <p class="truncate text-sm text-slate-500">{{ relationTypeLabel(currentDetail) }} · {{ sourceLabel(currentDetail) }}</p>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-3 text-sm">
            <div v-for="item in detailRows" :key="item.label" class="rounded-lg bg-slate-50 px-3 py-2">
              <div class="text-xs font-medium text-slate-400">{{ item.label }}</div>
              <div class="mt-1 break-words text-sm font-medium text-slate-700">{{ item.value || '-' }}</div>
            </div>
          </div>

          <div class="flex gap-2 pt-2">
            <button type="button" class="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600" @click="openEditDialog(currentDetail)">编辑</button>
            <button type="button" class="flex-1 rounded-lg border border-rose-200 px-3 py-2 text-sm font-medium text-rose-600" @click="handleDelete(currentDetail)">删除</button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addRelation, deleteRelation, getRelationDetail, queryRelationPageList, updateRelation } from '@/api/relation'
import { queryCustomerList } from '@/api/customer'
import { useResponsive } from '@/composables/useResponsive'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { CustomerListVO } from '@/types/customer'
import type { Relation, RelationForm } from '@/types/relation'

type Option = {
  label: string
  value: string
}

const relationTypeOptions: Option[] = [
  { label: '决策人', value: 'decision_maker' },
  { label: '影响人', value: 'influencer' },
  { label: '合作伙伴', value: 'partner' },
  { label: '客户联系人', value: 'customer_contact' },
  { label: '其他', value: 'other' }
]

const sourceLabels: Record<string, string> = {
  manual: '手动创建',
  customer_contact: '客户联系人'
}

const { isMobile } = useResponsive()
const relations = ref<Relation[]>([])
const loading = ref(false)
const keyword = ref('')
const relationType = ref('')
const page = ref(1)
const limit = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const saving = ref(false)
const editingRelation = ref<Relation | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<Relation | null>(null)
const customerOptions = ref<Option[]>([])
const customerLoading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

const form = reactive<RelationForm>({
  name: '',
  phone: '',
  wechat: '',
  email: '',
  relationType: 'other',
  customerId: '',
  remark: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / limit.value)))
const detailRows = computed(() => {
  const relation = currentDetail.value
  if (!relation) return []
  return [
    { label: '手机号', value: relation.phone || '' },
    { label: '微信号', value: relation.wechat || '' },
    { label: '邮箱', value: relation.email || '' },
    { label: '关联客户', value: relation.customerName || '' },
    { label: '备注', value: relation.remark || '' },
    { label: '更新时间', value: formatDateTime(relation.updateTime || relation.createTime) }
  ]
})

onMounted(() => {
  loadRelations()
})

async function loadRelations() {
  loading.value = true
  try {
    const result = await queryRelationPageList({
      keyword: keyword.value.trim() || undefined,
      relationType: relationType.value || undefined,
      page: page.value,
      limit: limit.value
    })
    relations.value = result.list || []
    total.value = result.totalRow || 0
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('加载关系列表失败')
    }
  } finally {
    loading.value = false
  }
}

function debouncedLoadRelations() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadRelations()
  }, 300)
}

function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  loadRelations()
}

function openCreateDialog() {
  editingRelation.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(relation: Relation) {
  editingRelation.value = relation
  form.relationId = relation.relationId
  form.name = relation.name || ''
  form.phone = relation.phone || ''
  form.wechat = relation.wechat || ''
  form.email = relation.email || ''
  form.relationType = relation.relationType || 'other'
  form.customerId = relation.customerId ? String(relation.customerId) : ''
  form.remark = relation.remark || ''
  ensureCustomerOption(relation)
  dialogVisible.value = true
  detailVisible.value = false
}

function resetForm() {
  form.relationId = undefined
  form.name = ''
  form.phone = ''
  form.wechat = ''
  form.email = ''
  form.relationType = 'other'
  form.customerId = ''
  form.remark = ''
  customerOptions.value = []
}

async function saveRelation() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }

  saving.value = true
  const payload: RelationForm = {
    ...form,
    name: form.name.trim(),
    phone: form.phone?.trim() || undefined,
    wechat: form.wechat?.trim() || undefined,
    email: form.email?.trim() || undefined,
    relationType: form.relationType || 'other',
    customerId: form.customerId || undefined,
    remark: form.remark?.trim() || undefined
  }

  try {
    if (editingRelation.value && form.relationId) {
      await updateRelation({ ...payload, relationId: form.relationId })
      ElMessage.success('关系已更新')
    } else {
      await addRelation(payload)
      ElMessage.success('关系已创建')
    }
    dialogVisible.value = false
    await loadRelations()
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('保存关系失败')
    }
  } finally {
    saving.value = false
  }
}

async function handleDelete(relation: Relation) {
  try {
    await ElMessageBox.confirm(`确定删除“${relation.name}”？`, '删除关系', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRelation(relation.relationId)
    ElMessage.success('关系已删除')
    detailVisible.value = false
    await loadRelations()
  } catch (error) {
    if (!isRequestErrorHandled(error) && error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除关系失败')
    }
  }
}

async function openDetail(relation: Relation) {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = relation
  try {
    const detail = await getRelationDetail(relation.relationId)
    currentDetail.value = detail.relation || relation
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('加载关系详情失败')
    }
  } finally {
    detailLoading.value = false
  }
}

async function searchCustomers(query: string) {
  const keywordText = query.trim()
  if (!keywordText) {
    customerOptions.value = []
    return
  }

  customerLoading.value = true
  try {
    const result = await queryCustomerList({ keyword: keywordText, page: 1, limit: 20 })
    customerOptions.value = (result.list || []).map(toCustomerOption)
  } finally {
    customerLoading.value = false
  }
}

function ensureCustomerOption(relation: Relation) {
  if (!relation.customerId || !relation.customerName) return
  const value = String(relation.customerId)
  if (!customerOptions.value.some(item => item.value === value)) {
    customerOptions.value = [{ label: relation.customerName, value }, ...customerOptions.value]
  }
}

function toCustomerOption(customer: CustomerListVO): Option {
  return {
    label: customer.companyName || `客户 ${customer.customerId}`,
    value: String(customer.customerId)
  }
}

function relationInitial(relation: Relation) {
  return relation.name?.trim()?.charAt(0) || '?'
}

function relationTypeLabel(relation: Relation) {
  if (relation.relationTypeName) return relation.relationTypeName
  return relationTypeOptions.find(item => item.value === relation.relationType)?.label || relation.relationType || '其他'
}

function sourceLabel(relation: Relation) {
  if (relation.sourceName) return relation.sourceName
  return sourceLabels[relation.source || ''] || relation.source || '-'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.relation-icon-button {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: rgb(100 116 139);
  transition: background-color 0.15s ease, color 0.15s ease;
}

.relation-icon-button:hover {
  background: rgb(241 245 249);
  color: rgb(15 23 42);
}
</style>
