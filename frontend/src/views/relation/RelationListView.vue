<template>
  <div class="flex h-full flex-col gap-6 px-4 py-6 md:px-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div class="min-w-0">
        <h2 class="text-[22px] font-bold text-slate-900">关系</h2>
        <p class="mt-1 text-[13px] text-slate-500">外部联系人与私人关系记录</p>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div class="relative group flex items-center">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-primary">search</span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索姓名、手机号、微信、邮箱或公司"
            class="h-10 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm shadow-sm outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20 sm:w-80"
            @input="debouncedLoadRelations"
            @keydown.enter="loadRelations"
          />
        </div>
        <select
          v-model="relationType"
          class="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-600 shadow-sm outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
          @change="loadRelations"
        >
          <option value="">全部类型</option>
          <option v-for="option in relationTypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <button
          v-if="canCreateRelation"
          type="button"
          class="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-primary px-4 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90"
          @click="openCreateDialog"
        >
          <span class="material-symbols-outlined text-[18px] leading-none">person_add</span>
          新建关系
        </button>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm" v-loading="loading">
      <div class="min-h-0 flex-1">
        <el-table
          v-if="!isMobile"
          :data="relations"
          height="100%"
          row-key="relationId"
          table-layout="fixed"
          class="wk-customer-table wk-relation-table"
          empty-text="暂无关系人"
          @row-click="openDetail"
        >
          <el-table-column label="关系人" min-width="220">
            <template #header>
              <span class="normal-case tracking-normal">关系人</span>
            </template>
            <template #default="{ row }">
              <div class="flex min-w-0 items-center gap-3">
                <div class="flex size-8 shrink-0 items-center justify-center overflow-hidden rounded border border-slate-200 bg-white">
                  <img v-if="avatarUrl(row)" :src="avatarUrl(row)" class="size-full object-cover" alt="avatar" />
                  <span v-else class="flex size-full items-center justify-center bg-primary/10 text-xs font-bold text-primary">
                    {{ relationInitial(row) }}
                  </span>
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-semibold text-slate-900 transition-colors hover:text-primary hover:underline hover:decoration-primary underline-offset-2">{{ row.name }}</p>
                  <p class="truncate text-xs text-slate-400">{{ row.phone || row.email || '-' }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="关系类型" width="130">
            <template #header>
              <span class="normal-case tracking-normal">关系类型</span>
            </template>
            <template #default="{ row }">
              <span class="inline-flex shrink-0 items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600 whitespace-nowrap">
                {{ relationTypeLabel(row.relationType, row.relationTypeName) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="所属公司" min-width="180">
            <template #header>
              <span class="normal-case tracking-normal">所属公司</span>
            </template>
            <template #default="{ row }">
              <span class="block truncate text-sm text-slate-600">{{ row.company || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="120">
            <template #header>
              <span class="normal-case tracking-normal">来源</span>
            </template>
            <template #default="{ row }">
              <span class="text-sm text-slate-500">{{ sourceLabel(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #header>
              <span class="normal-case tracking-normal">更新时间</span>
            </template>
            <template #default="{ row }">
              <span class="text-sm text-slate-500">{{ formatDateTime(row.updateTime || row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="canEditRelation || canDeleteRelation" label="操作" width="112" fixed="right">
            <template #header>
              <span class="normal-case tracking-normal">操作</span>
            </template>
            <template #default="{ row }">
              <div class="flex items-center gap-1" @click.stop>
                <button v-if="canEditRelation" class="relation-icon-btn" title="编辑" @click="openEditDialog(row)">
                  <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
                </button>
                <button v-if="canDeleteRelation" class="relation-icon-btn text-rose-500 hover:bg-rose-50" title="删除" @click="handleDelete(row)">
                  <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
                </button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <div class="text-center py-16">
              <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
                <span class="material-symbols-outlined text-4xl">diversity_3</span>
              </div>
              <p class="text-slate-400 text-sm font-medium">{{ keyword.trim() ? '未找到匹配关系人' : '暂无关系人' }}</p>
            </div>
          </template>
        </el-table>

        <div v-else class="wk-customer-cards h-full overflow-y-auto px-3 py-3">
          <div v-if="relations.length === 0" class="text-center py-16">
            <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
              <span class="material-symbols-outlined text-4xl">diversity_3</span>
            </div>
            <p class="text-slate-400 text-sm font-medium">{{ keyword.trim() ? '未找到匹配关系人' : '暂无关系人' }}</p>
          </div>

          <div v-else class="flex flex-col gap-3">
            <button
              v-for="relation in relations"
              :key="relation.relationId"
              type="button"
              class="wk-customer-card w-full cursor-pointer rounded-xl border border-slate-200 bg-white px-4 py-3 text-left shadow-sm transition-colors hover:bg-slate-50 active:bg-slate-100"
              @click="openDetail(relation)"
            >
              <div class="flex items-start gap-3">
                <div class="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                  <img v-if="avatarUrl(relation)" :src="avatarUrl(relation)" class="size-full object-cover" alt="avatar" />
                  <span v-else class="flex size-full items-center justify-center bg-primary/10 text-sm font-bold text-primary">
                    {{ relationInitial(relation) }}
                  </span>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex items-start gap-2">
                    <div class="min-w-0 flex-1">
                      <div class="truncate text-sm font-bold text-slate-900">{{ relation.name }}</div>
                      <p class="mt-0.5 truncate text-xs text-slate-400">{{ relationTypeLabel(relation.relationType, relation.relationTypeName) }} · {{ relation.company || '-' }}</p>
                    </div>
                    <button type="button" class="relation-icon-btn shrink-0" title="AI 对话" @click.stop="openChat(relation)">
                      <AiDialogIcon :size="24" :sparkle-size="14" />
                    </button>
                  </div>
                  <div class="mt-3 grid grid-cols-1 gap-1 text-sm text-slate-600">
                    <p class="truncate">{{ relation.phone || relation.email || relation.wechat || '-' }}</p>
                    <p class="line-clamp-2 text-xs text-slate-400">{{ relation.remark || '暂无备注' }}</p>
                  </div>
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <div v-if="total > 0" class="flex shrink-0 items-center justify-between border-t border-slate-200 bg-slate-50/50 px-6 py-4 text-sm text-slate-500">
        <span>共 {{ total }} 条<span class="hidden md:inline">关系人数据</span></span>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建关系' : '编辑关系'"
      :width="isMobile ? '92vw' : '560px'"
      destroy-on-close
    >
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label class="relation-field sm:col-span-2">
          <span>姓名</span>
          <el-input v-model="form.name" maxlength="100" />
        </label>
        <label class="relation-field">
          <span>关系类型</span>
          <el-select v-model="form.relationType" class="w-full">
            <el-option v-for="option in relationTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </label>
        <label class="relation-field">
          <span>所属公司</span>
          <el-input v-model="form.company" maxlength="255" />
        </label>
        <label class="relation-field">
          <span>手机号</span>
          <el-input v-model="form.phone" maxlength="50" />
        </label>
        <label class="relation-field">
          <span>微信号</span>
          <el-input v-model="form.wechat" maxlength="100" />
        </label>
        <label class="relation-field">
          <span>邮箱</span>
          <el-input v-model="form.email" maxlength="100" />
        </label>
        <label class="relation-field">
          <span>头像 URL</span>
          <el-input v-model="form.avatar" maxlength="500" />
        </label>
        <label class="relation-field sm:col-span-2">
          <span>备注</span>
          <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </label>
        <DynamicFieldForm
          ref="dynamicFieldFormRef"
          v-model="customFieldValues"
          entity-type="relation"
          mode="custom"
          :entity-id="dialogMode === 'edit' ? editingRelationId : null"
          class="contents"
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitRelation">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      :size="isMobile ? '100%' : '520px'"
      title="关系详情"
      destroy-on-close
    >
      <div v-if="detailLoading" class="flex h-64 items-center justify-center">
        <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
      </div>
      <div v-else-if="currentDetail?.relation" class="space-y-5">
        <section class="rounded-lg border border-slate-200 p-4">
          <div class="flex items-start gap-3">
            <img v-if="avatarUrl(currentDetail.relation)" :src="avatarUrl(currentDetail.relation)" class="size-12 rounded-full object-cover" alt="avatar" />
            <div v-else class="flex size-12 shrink-0 items-center justify-center rounded-full bg-slate-900 text-base font-bold text-white">
              {{ relationInitial(currentDetail.relation) }}
            </div>
            <div class="min-w-0 flex-1">
              <h3 class="truncate text-base font-bold text-slate-900">{{ currentDetail.relation.name }}</h3>
              <p class="mt-1 text-sm text-slate-500">{{ relationTypeLabel(currentDetail.relation.relationType, currentDetail.relation.relationTypeName) }}</p>
            </div>
            <button class="relation-icon-btn" title="AI 对话" @click="openChat(currentDetail.relation)">
              <AiDialogIcon :size="24" :sparkle-size="14" />
            </button>
          </div>
          <div class="mt-4 grid grid-cols-1 gap-3 text-sm">
            <p><span class="text-slate-400">手机号：</span>{{ currentDetail.relation.phone || '-' }}</p>
            <p><span class="text-slate-400">微信号：</span>{{ currentDetail.relation.wechat || '-' }}</p>
            <p><span class="text-slate-400">邮箱：</span>{{ currentDetail.relation.email || '-' }}</p>
            <p><span class="text-slate-400">所属公司：</span>{{ currentDetail.relation.company || '-' }}</p>
            <p><span class="text-slate-400">来源：</span>{{ sourceLabel(currentDetail.relation) }}</p>
            <p v-if="currentDetail.relation.sourceCustomerName"><span class="text-slate-400">来源客户：</span>{{ currentDetail.relation.sourceCustomerName }}</p>
          </div>
          <p class="mt-4 whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-sm leading-6 text-slate-600">
            {{ currentDetail.relation.remark || '暂无备注' }}
          </p>
        </section>

        <section v-for="section in detailSections" :key="section.key" class="rounded-lg border border-slate-200 p-4">
          <div class="mb-3 flex items-center justify-between">
            <h4 class="text-sm font-bold text-slate-900">{{ section.title }}</h4>
            <span class="text-xs text-slate-400">{{ section.items.length }}</span>
          </div>
          <div v-if="section.items.length === 0" class="py-5 text-center text-xs text-slate-400">
            暂无数据
          </div>
          <div v-else class="space-y-2">
            <div v-for="item in section.items" :key="item.key" class="rounded-lg bg-slate-50 px-3 py-2">
              <p class="truncate text-sm font-semibold text-slate-800">{{ item.title }}</p>
              <p v-if="item.subtitle" class="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">{{ item.subtitle }}</p>
            </div>
          </div>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addRelation, deleteRelation, getRelationDetail, queryRelationList, updateRelation } from '@/api/relation'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'
import AiDialogIcon from '@/components/common/AiDialogIcon.vue'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { RelationAddBO, RelationDetailVO, RelationType, RelationUpdateBO, RelationVO } from '@/types/relation'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const relationTypeOptions: Array<{ value: RelationType; label: string }> = [
  { value: 'friend', label: '朋友' },
  { value: 'family', label: '家人' },
  { value: 'relative', label: '亲戚' },
  { value: 'partner', label: '合作伙伴' },
  { value: 'customer_contact', label: '客户联系人' },
  { value: 'supplier', label: '供应商' },
  { value: 'investor', label: '投资人' },
  { value: 'other', label: '其他' }
]

const relations = ref<RelationVO[]>([])
const loading = ref(false)
const keyword = ref('')
const relationType = ref('')
const page = ref(1)
const limit = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const editingRelationId = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<RelationDetailVO | null>(null)
const dynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm> | null>(null)
const customFieldValues = ref<Record<string, unknown>>({})
let searchTimer: number | null = null

const form = reactive<RelationAddBO & { relationId?: string }>({
  name: '',
  avatar: '',
  phone: '',
  wechat: '',
  email: '',
  relationType: 'other',
  company: '',
  remark: ''
})

const canCreateRelation = computed(() => userStore.hasPermission('relation:create'))
const canEditRelation = computed(() => userStore.hasPermission('relation:edit'))
const canDeleteRelation = computed(() => userStore.hasPermission('relation:delete'))
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

const detailSections = computed(() => {
  const detail = currentDetail.value
  return [
    {
      key: 'tasks',
      title: '相关任务',
      items: (detail?.tasks || []).map(task => ({
        key: `task-${task.taskId}`,
        title: task.title,
        subtitle: [task.statusName || task.status, task.dueDate ? `截止 ${formatDateTime(task.dueDate)}` : ''].filter(Boolean).join(' · ')
      }))
    },
    {
      key: 'schedules',
      title: '相关日程',
      items: (detail?.schedules || []).map(schedule => ({
        key: `schedule-${schedule.scheduleId}`,
        title: schedule.title,
        subtitle: [schedule.startTime ? formatDateTime(schedule.startTime) : '', schedule.location || ''].filter(Boolean).join(' · ')
      }))
    },
    {
      key: 'attachments',
      title: '相关附件',
      items: (detail?.attachments || []).map(knowledge => ({
        key: `knowledge-${knowledge.knowledgeId}`,
        title: knowledge.name,
        subtitle: [knowledge.summary || '', knowledge.createTime ? formatDateTime(knowledge.createTime) : ''].filter(Boolean).join(' · ')
      }))
    },
    {
      key: 'histories',
      title: '历史记录',
      items: (detail?.histories || []).map(history => ({
        key: `history-${history.followUpId}`,
        title: history.content,
        subtitle: [history.typeName || history.type, history.followTime ? formatDateTime(history.followTime) : ''].filter(Boolean).join(' · ')
      }))
    }
  ]
})

onMounted(() => {
  void loadRelations()
  void applyOpenRelationQuery()
})

watch(
  () => route.query.openRelationId,
  () => {
    void applyOpenRelationQuery()
  }
)

function debouncedLoadRelations() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    void loadRelations()
  }, 260)
}

async function loadRelations() {
  loading.value = true
  try {
    const result = await queryRelationList({
      keyword: keyword.value.trim() || undefined,
      relationType: relationType.value || undefined,
      page: page.value,
      limit: limit.value
    })
    relations.value = result.list || []
    total.value = Number(result.totalRow || 0)
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.warning('关系列表加载失败')
    }
    relations.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  void loadRelations()
}

function resetForm() {
  editingRelationId.value = ''
  form.name = ''
  form.avatar = ''
  form.phone = ''
  form.wechat = ''
  form.email = ''
  form.relationType = 'other'
  form.company = ''
  form.remark = ''
  customFieldValues.value = {}
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(relation: RelationVO) {
  dialogMode.value = 'edit'
  editingRelationId.value = relation.relationId
  form.name = relation.name || ''
  form.avatar = relation.avatar || relation.avatarUrl || ''
  form.phone = relation.phone || ''
  form.wechat = relation.wechat || ''
  form.email = relation.email || ''
  form.relationType = relation.relationType || 'other'
  form.company = relation.company || ''
  form.remark = relation.remark || ''
  customFieldValues.value = { ...(relation.customFields || {}) }
  dialogVisible.value = true
}

async function submitRelation() {
  const name = form.name.trim()
  if (!name) {
    ElMessage.warning('请填写姓名')
    return
  }
  const missingFields = dynamicFieldFormRef.value?.getRequiredFieldLabels() || []
  if (missingFields.length > 0) {
    ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
    return
  }
  const uniqueValid = await dynamicFieldFormRef.value?.validateUniqueFields()
  if (uniqueValid === false) {
    return
  }
  submitting.value = true
  try {
    const payload = normalizeRelationPayload()
    if (dialogMode.value === 'create') {
      await addRelation(payload)
      ElMessage.success('关系人已创建')
    } else {
      await updateRelation({ ...payload, relationId: editingRelationId.value } as RelationUpdateBO)
      ElMessage.success('关系人已更新')
    }
    dialogVisible.value = false
    await loadRelations()
    if (currentDetail.value?.relation.relationId === editingRelationId.value) {
      await loadDetail(editingRelationId.value)
    }
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('保存失败')
    }
  } finally {
    submitting.value = false
  }
}

function normalizeRelationPayload(): RelationAddBO {
  return {
    name: form.name.trim(),
    avatar: form.avatar?.trim() || undefined,
    phone: form.phone?.trim() || undefined,
    wechat: form.wechat?.trim() || undefined,
    email: form.email?.trim() || undefined,
    relationType: form.relationType || 'other',
    company: form.company?.trim() || undefined,
    remark: form.remark?.trim() || undefined,
    customFields: customFieldValues.value
  }
}

async function handleDelete(relation: RelationVO) {
  try {
    await ElMessageBox.confirm(`确定删除关系人「${relation.name}」吗？`, '删除关系', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRelation(relation.relationId)
    ElMessage.success('已删除')
    await loadRelations()
    if (currentDetail.value?.relation.relationId === relation.relationId) {
      detailVisible.value = false
      currentDetail.value = null
    }
  } catch {
    // ignore cancel
  }
}

async function openDetail(relation: RelationVO) {
  detailVisible.value = true
  await loadDetail(relation.relationId)
}

async function loadDetail(relationId: string) {
  detailLoading.value = true
  try {
    currentDetail.value = await getRelationDetail(relationId)
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.warning('关系详情加载失败')
    }
  } finally {
    detailLoading.value = false
  }
}

async function openChat(relation: RelationVO) {
  await chatStore.openRelationChat(relation)
  await router.push({ path: '/chat', query: { relationId: relation.relationId } })
}

async function applyOpenRelationQuery() {
  const raw = route.query.openRelationId
  const relationId = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  if (!relationId) return
  detailVisible.value = true
  await loadDetail(relationId)
}

function relationInitial(relation: RelationVO) {
  return relation.name?.trim().charAt(0) || '?'
}

function avatarUrl(relation: RelationVO) {
  return relation.avatarUrl || relation.avatar || ''
}

function relationTypeLabel(type?: string, fallback?: string) {
  if (fallback) return fallback
  return relationTypeOptions.find(option => option.value === type)?.label || type || '其他'
}

function sourceLabel(relation: RelationVO) {
  if (relation.sourceName) return relation.sourceName
  if (relation.source === 'customer_contact') return '客户联系人'
  if (relation.source === 'manual') return '手动创建'
  return relation.source || '-'
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

.relation-icon-btn {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #64748b;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.relation-icon-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.relation-field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.wk-customer-card {
  -webkit-tap-highlight-color: transparent;
}
</style>
