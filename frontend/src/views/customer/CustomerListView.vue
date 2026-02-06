<template>
  <div class="h-full flex flex-col bg-gray-50">
    <!-- Header -->
    <div class="px-6 py-4 bg-white border-b border-gray-200">
      <div class="flex items-start justify-between">
        <div>
          <h1 class="text-lg font-semibold text-gray-900">客户管理</h1>
          <p class="text-sm text-gray-500 mt-1">查看和管理所有客户信息与商机</p>
        </div>
        <div class="text-sm text-gray-500">
          您的权限: <span class="text-gray-700">{{ userStore.realname || '销售经理' }}，完整权限</span>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="px-6 py-4 grid grid-cols-4 gap-4 bg-gray-50">
      <!-- 总客户数 -->
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">总客户数</span>
          <el-icon class="text-gray-400 text-lg"><User /></el-icon>
        </div>
        <div class="mt-2 text-2xl font-semibold">{{ statistics?.totalCustomers || customerStore.totalCount }}</div>
        <div class="mt-1 text-xs text-gray-500">活跃客户</div>
      </div>

      <!-- 总报价金额 -->
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">总报价金额</span>
          <el-icon class="text-green-500 text-lg"><TrendCharts /></el-icon>
        </div>
        <div class="mt-2 text-2xl font-semibold text-green-600">{{ formatMoney(totalQuotation) }}</div>
        <div class="mt-1 text-xs text-green-500">↑12% 本月</div>
      </div>

      <!-- 总回款金额 -->
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">总回款金额</span>
          <el-icon class="text-orange-500 text-lg"><Coin /></el-icon>
        </div>
        <div class="mt-2 text-2xl font-semibold text-orange-600">{{ formatMoney(totalRevenue) }}</div>
        <div class="mt-1 text-xs text-orange-500">已回款</div>
      </div>

      <!-- 成交转化率 -->
      <div class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">成交转化率</span>
          <el-icon class="text-primary-500 text-lg"><Aim /></el-icon>
        </div>
        <div class="mt-2 text-2xl font-semibold">{{ conversionRate }}%</div>
        <div class="mt-1 text-xs text-gray-500">{{ closedCount }}个已成交</div>
      </div>
    </div>

    <!-- Search and Filter Bar -->
    <div class="px-6 py-3 bg-white flex items-center gap-4">
      <el-input
        v-model="customerStore.queryParams.keyword"
        placeholder="搜索客户公司或联系人..."
        :prefix-icon="Search"
        clearable
        class="flex-1 max-w-md"
        @change="handleSearch"
      />
      <div class="flex-1"></div>
      <el-button :icon="Filter">筛选</el-button>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">添加客户</el-button>
    </div>

    <!-- Stage Tabs -->
    <div class="px-6 py-3 bg-white flex gap-2 flex-wrap border-b border-gray-200">
      <el-button
        v-for="tab in stageTabs"
        :key="tab.value"
        :type="currentStage === tab.value ? 'primary' : 'default'"
        :plain="currentStage !== tab.value"
        round
        size="small"
        @click="handleStageFilter(tab.value)"
      >
        {{ tab.label }} ({{ tab.count }})
      </el-button>
    </div>

    <!-- Main Content Area -->
    <div class="flex-1 flex overflow-hidden">
      <!-- Left: Customer Card List -->
      <div ref="scrollContainer" class="flex-1 overflow-auto p-6" v-loading="customerStore.loading">
        <div class="space-y-4">
          <div
            v-for="customer in customerStore.customerList"
            :key="customer.customerId"
            class="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow cursor-pointer"
            @click="handleRowClick(customer)"
          >
            <!-- Row 1: Company name + Level + Stage + Actions -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0">
                  <el-icon class="text-primary-500 text-xl"><OfficeBuilding /></el-icon>
                </div>
                <span class="font-medium text-base">{{ customer.companyName }}</span>
                <el-tag :type="getLevelType(customer.level)" size="small" round>
                  {{ customer.level }}级
                </el-tag>
                <el-tag
                  size="small"
                  round
                  :style="{
                    backgroundColor: getStageColor(customer.stage) + '20',
                    color: getStageColor(customer.stage),
                    borderColor: getStageColor(customer.stage)
                  }"
                >
                  {{ getStageLabel(customer.stage) }}
                </el-tag>
              </div>
              <div class="flex items-center gap-2" @click.stop>
                <el-popconfirm
                  :title="`确定要删除客户「${customer.companyName}」吗？`"
                  confirm-button-text="删除"
                  cancel-button-text="取消"
                  confirm-button-type="danger"
                  @confirm="handleDelete(customer)"
                >
                  <template #reference>
                    <el-button type="danger" text size="small" :icon="Delete">删除</el-button>
                  </template>
                </el-popconfirm>
                <el-icon class="text-gray-400"><ArrowRight /></el-icon>
              </div>
            </div>

            <!-- Row 2: Industry + Contact count + Last contact time -->
            <div class="mt-3 flex items-center text-sm text-gray-500 gap-4">
              <span class="flex items-center gap-1">
                <el-icon><OfficeBuilding /></el-icon>
                {{ customer.industry || '未分类' }}
              </span>
              <span class="flex items-center gap-1">
                <el-icon><User /></el-icon>
                {{ customer.contactCount || 0 }}个联系人
              </span>
              <span class="flex items-center gap-1">
                <el-icon><Calendar /></el-icon>
                {{ formatRelativeTime(customer.lastContactTime) }}
              </span>
            </div>

            <!-- Row 3: Primary contact -->
            <div v-if="customer.primaryContactName" class="mt-3 text-sm text-gray-600">
              主要联系人:
              <span class="inline-flex items-center gap-1">
                <el-icon><User /></el-icon>
                {{ customer.primaryContactName }}
                <span v-if="customer.primaryContactPosition" class="text-gray-400">·{{ customer.primaryContactPosition }}</span>
              </span>
            </div>

            <!-- Row 4: Tags -->
            <div v-if="customer.tags?.length" class="mt-3 flex gap-2 flex-wrap">
              <el-tag
                v-for="tag in customer.tags"
                :key="tag"
                size="small"
                effect="plain"
                class="!bg-gray-50"
              >
                {{ tag }}
              </el-tag>
            </div>

            <!-- Row 5: Owner + Financial info -->
            <div class="mt-3 flex items-center justify-between">
              <div class="flex items-center text-sm text-gray-500">
                <span>负责人:</span>
                <el-avatar :size="24" class="mx-2 bg-primary-100 text-primary-600">
                  {{ customer.ownerName?.charAt(0) || '?' }}
                </el-avatar>
                <span class="text-gray-700">{{ customer.ownerName }}</span>
              </div>
              <div class="text-right text-sm space-y-1">
                <div v-if="customer.quotation" class="text-gray-500">
                  报价金额 <span class="text-primary-600 font-medium">{{ formatMoney(customer.quotation) }}</span>
                </div>
                <div v-if="customer.contractAmount" class="text-gray-500">
                  合同金额 <span class="text-primary-600 font-medium">{{ formatMoney(customer.contractAmount) }}</span>
                </div>
                <div v-if="customer.revenue" class="text-gray-500">
                  回款金额 <span class="text-green-600 font-medium">{{ formatMoney(customer.revenue) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-if="!customerStore.loading && customerStore.customerList.length === 0" class="text-center py-12 text-gray-500">
            <el-icon class="text-4xl mb-2"><Document /></el-icon>
            <p>暂无客户数据</p>
          </div>
        </div>

        <!-- Infinite scroll trigger -->
        <div
          v-if="customerStore.hasMore && customerStore.customerList.length > 0"
          ref="loadMoreTrigger"
          class="flex justify-center py-4"
        >
          <el-icon v-if="customerStore.loading" class="is-loading text-gray-400" :size="24"><Loading /></el-icon>
          <span v-else class="text-sm text-gray-400">向下滚动加载更多</span>
        </div>

        <!-- Pagination -->
        <div v-if="customerStore.totalCount > 0" class="mt-4 flex justify-center pb-4">
          <el-pagination
            v-model:current-page="customerStore.queryParams.page"
            v-model:page-size="customerStore.queryParams.limit"
            :total="customerStore.totalCount"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <!-- Right: Sidebar -->
      <div class="w-80 border-l border-gray-200 bg-white overflow-auto flex-shrink-0">
        <!-- Funnel Chart -->
        <div class="p-4 border-b border-gray-200">
          <h3 class="font-medium flex items-center gap-2">
            <el-icon class="text-primary-500"><DataAnalysis /></el-icon>
            商机漏斗
          </h3>
          <div class="mt-4 space-y-3">
            <div
              v-for="stage in funnelStages"
              :key="stage.value"
              class="flex items-center gap-2"
            >
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :style="{ backgroundColor: stage.color }"
              ></span>
              <span class="text-sm text-gray-600 flex-1">{{ stage.label }}</span>
              <span class="text-sm font-medium w-6 text-right">{{ stage.count }}</span>
              <div class="w-20 h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all"
                  :style="{
                    width: getStagePercentage(stage.count) + '%',
                    backgroundColor: stage.color
                  }"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activities -->
        <div class="p-4 border-b border-gray-200">
          <h3 class="font-medium flex items-center gap-2">
            <el-icon class="text-orange-500"><Bell /></el-icon>
            最近动态
          </h3>
          <div class="mt-4 space-y-4">
            <div
              v-for="(activity, index) in recentActivities"
              :key="index"
              class="flex items-start gap-2"
            >
              <span
                class="w-2 h-2 rounded-full mt-2 flex-shrink-0"
                :class="activity.color"
              ></span>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-gray-800 truncate">{{ activity.companyName }}</div>
                <div class="text-xs text-gray-500 truncate">{{ activity.description }}</div>
                <div class="text-xs text-gray-400 mt-1">{{ activity.user }} · {{ activity.time }}</div>
              </div>
            </div>
            <div v-if="recentActivities.length === 0" class="text-center text-sm text-gray-400 py-4">
              暂无动态
            </div>
          </div>
        </div>

        <!-- AI Insights -->
        <div class="p-4">
          <h3 class="font-medium flex items-center gap-2">
            <el-icon class="text-primary-500"><MagicStick /></el-icon>
            AI智能洞察
          </h3>
          <div class="mt-4 space-y-3">
            <div class="p-3 bg-primary-50 rounded-lg text-sm">
              <div class="text-primary-700 font-medium">客户跟进提醒</div>
              <div class="text-primary-600 mt-1">有 {{ overdueCount }} 个客户超过7天未跟进，建议尽快安排跟进计划</div>
            </div>
            <div class="p-3 bg-green-50 rounded-lg text-sm">
              <div class="text-green-700 font-medium">成交机会</div>
              <div class="text-green-600 mt-1">谈判中的客户转化率较高，当前有 {{ negotiationCount }} 个客户处于谈判阶段</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingCustomer ? '编辑客户' : '新建客户'"
      width="600px"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="formData.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="行业" prop="industry">
          <el-input v-model="formData.industry" placeholder="请输入行业" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户级别" prop="level">
              <el-select v-model="formData.level" class="w-full">
                <el-option label="A级客户" value="A" />
                <el-option label="B级客户" value="B" />
                <el-option label="C级客户" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商机阶段" prop="stage">
              <el-select v-model="formData.stage" class="w-full">
                <el-option label="线索" value="lead" />
                <el-option label="资格审查" value="qualified" />
                <el-option label="方案报价" value="proposal" />
                <el-option label="谈判中" value="negotiation" />
                <el-option label="已成交" value="closed" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">联系人信息</el-divider>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="联系人姓名" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="contactEmail">
              <el-input v-model="formData.contactEmail" placeholder="邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Dynamic Custom Fields -->
        <DynamicFieldForm
          ref="dynamicFieldFormRef"
          entity-type="customer"
          v-model="customFieldValues"
          title="扩展信息"
        />
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingCustomer ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import {
  Plus,
  Search,
  Filter,
  User,
  Calendar,
  OfficeBuilding,
  ArrowRight,
  TrendCharts,
  Coin,
  Aim,
  DataAnalysis,
  Bell,
  MagicStick,
  Document,
  Delete,
  Loading
} from '@element-plus/icons-vue'
import type { CustomerListVO, CustomerAddBO, CustomerStage } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { getEnabledFieldsByEntity } from '@/api/customField'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'

const router = useRouter()
const customerStore = useCustomerStore()
const userStore = useUserStore()

const showAddDialog = ref(false)
const editingCustomer = ref<CustomerListVO | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const dynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()
const customFieldValues = ref<Record<string, any>>({})
const listCustomFields = ref<CustomField[]>([])
const currentStage = ref('')
const statistics = ref<any>(null)
const loadMoreTrigger = ref<HTMLElement>()
const scrollContainer = ref<HTMLElement>()
let observer: IntersectionObserver | null = null

const formData = reactive<CustomerAddBO>({
  companyName: '',
  industry: '',
  level: 'B',
  stage: 'lead',
  contactName: '',
  contactPhone: '',
  contactEmail: ''
})

const formRules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}

// Stage color mapping
const stageColors: Record<string, string> = {
  lead: '#6b7280',
  qualified: '#3b82f6',
  proposal: '#f59e0b',
  negotiation: '#8b5cf6',
  closed: '#22c55e',
  lost: '#ef4444'
}

// Computed: Stage tabs with counts
const stageTabs = computed(() => {
  const counts = getStageCounts()
  return [
    { value: '', label: '全部', count: customerStore.totalCount },
    { value: 'lead', label: '线索', count: counts.lead },
    { value: 'qualified', label: '资格审查', count: counts.qualified },
    { value: 'proposal', label: '方案报价', count: counts.proposal },
    { value: 'negotiation', label: '谈判中', count: counts.negotiation },
    { value: 'closed', label: '已成交', count: counts.closed },
    { value: 'lost', label: '已流失', count: counts.lost }
  ]
})

// Computed: Funnel stages for sidebar
const funnelStages = computed(() => {
  const counts = getStageCounts()
  return [
    { value: 'lead', label: '线索', count: counts.lead, color: stageColors.lead },
    { value: 'qualified', label: '资格审查', count: counts.qualified, color: stageColors.qualified },
    { value: 'proposal', label: '方案报价', count: counts.proposal, color: stageColors.proposal },
    { value: 'negotiation', label: '谈判中', count: counts.negotiation, color: stageColors.negotiation },
    { value: 'closed', label: '已成交', count: counts.closed, color: stageColors.closed },
    { value: 'lost', label: '已流失', count: counts.lost, color: stageColors.lost }
  ]
})

// Computed: Total quotation
const totalQuotation = computed(() => {
  return customerStore.customerList.reduce((sum, c) => sum + (c.quotation || 0), 0)
})

// Computed: Total revenue
const totalRevenue = computed(() => {
  return statistics.value?.totalRevenue || customerStore.customerList.reduce((sum, c) => sum + (c.revenue || 0), 0)
})

// Computed: Closed count
const closedCount = computed(() => {
  return customerStore.customerList.filter(c => c.stage === 'closed').length
})

// Computed: Conversion rate
const conversionRate = computed(() => {
  const total = customerStore.totalCount
  if (total === 0) return '0.0'
  return ((closedCount.value / total) * 100).toFixed(1)
})

// Computed: Negotiation count
const negotiationCount = computed(() => {
  return customerStore.customerList.filter(c => c.stage === 'negotiation').length
})

// Computed: Overdue count (customers not contacted in 7 days)
const overdueCount = computed(() => {
  const sevenDaysAgo = new Date()
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
  return customerStore.customerList.filter(c => {
    if (!c.lastContactTime) return true
    return new Date(c.lastContactTime) < sevenDaysAgo
  }).length
})

// Computed: Recent activities (mock data based on customers)
const recentActivities = computed(() => {
  return customerStore.customerList.slice(0, 3).map((c, i) => ({
    companyName: c.companyName,
    description: `联系人${c.primaryContactName || '未知'} - ${getActivityType(i)}`,
    user: c.ownerName || '系统',
    time: formatRelativeTime(c.lastContactTime),
    color: i === 0 ? 'bg-green-500' : i === 1 ? 'bg-blue-500' : 'bg-orange-500'
  }))
})

// Helper: Get stage counts from statistics or customer list
function getStageCounts() {
  if (statistics.value?.customersByStage) {
    const counts: Record<string, number> = {
      lead: 0, qualified: 0, proposal: 0, negotiation: 0, closed: 0, lost: 0
    }
    statistics.value.customersByStage.forEach((s: any) => {
      counts[s.stage] = s.count
    })
    return counts
  }
  // Fallback: count from current list (may not be accurate for filtered data)
  const counts: Record<string, number> = {
    lead: 0, qualified: 0, proposal: 0, negotiation: 0, closed: 0, lost: 0
  }
  customerStore.customerList.forEach(c => {
    if (c.stage && counts[c.stage] !== undefined) {
      counts[c.stage]++
    }
  })
  return counts
}

// Helper: Get activity type text
function getActivityType(index: number): string {
  const types = ['提交了产品方案', '完成了产品演示', '签署了合作合同']
  return types[index % types.length]
}

// Setup IntersectionObserver for infinite scroll
function setupObserver() {
  observer?.disconnect()
  if (loadMoreTrigger.value) {
    observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && customerStore.hasMore && !customerStore.loading) {
        customerStore.loadMore()
      }
    }, { threshold: 0.1 })
    observer.observe(loadMoreTrigger.value)
  }
}

// Re-observe when the trigger element appears/disappears
watch(() => customerStore.hasMore, () => {
  nextTick(() => setupObserver())
})

onMounted(async () => {
  // Load custom fields that should be shown in list
  try {
    const allFields = await getEnabledFieldsByEntity('customer')
    listCustomFields.value = allFields.filter(f => f.isShowInList)
  } catch {
    // Error handled by interceptor
  }

  // Load statistics
  try {
    await customerStore.fetchStatistics()
    statistics.value = customerStore.statistics
  } catch {
    // Statistics loading failed, continue with list
  }

  await customerStore.fetchCustomerList(true)
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

function handleSearch() {
  customerStore.queryParams.page = 1
  customerStore.fetchCustomerList(true)
}

function handlePageChange(page: number) {
  if (customerStore.queryParams.page === page) return
  customerStore.queryParams.page = page
  customerStore.fetchCustomerList(false)  // Replace with current page data
  scrollContainer.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleSizeChange(size: number) {
  customerStore.queryParams.limit = size
  customerStore.queryParams.page = 1
  customerStore.fetchCustomerList(false)  // Replace with new page
  scrollContainer.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleStageFilter(stage: string) {
  currentStage.value = stage
  customerStore.queryParams.stage = (stage || undefined) as CustomerStage | undefined
  customerStore.queryParams.page = 1
  customerStore.fetchCustomerList(true)
}

function handleRowClick(row: CustomerListVO) {
  router.push(`/customer/${row.customerId}`)
}

async function handleDelete(row: CustomerListVO) {
  try {
    await ElMessageBox.confirm(`确定要删除客户「${row.companyName}」吗？`, '提示', {
      type: 'warning'
    })
    await customerStore.removeCustomer(row.customerId)
    ElMessage.success('删除成功')
  } catch {
    // Cancelled
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

  // Validate custom fields
  if (dynamicFieldFormRef.value) {
    const missingFields = dynamicFieldFormRef.value.getRequiredFieldLabels()
    if (missingFields.length > 0) {
      ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
      return
    }
  }

  submitting.value = true
  try {
    const submitData = {
      ...formData,
      customFields: customFieldValues.value
    }
    if (editingCustomer.value) {
      await customerStore.editCustomer({
        ...submitData,
        customerId: editingCustomer.value.customerId
      })
      ElMessage.success('更新成功')
    } else {
      await customerStore.createCustomer(submitData)
      ElMessage.success('创建成功')
    }
    showAddDialog.value = false
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  editingCustomer.value = null
  Object.assign(formData, {
    companyName: '',
    industry: '',
    level: 'B',
    stage: 'lead',
    contactName: '',
    contactPhone: '',
    contactEmail: ''
  })
  customFieldValues.value = {}
}

function getLevelType(level: string): 'success' | 'primary' | 'info' {
  switch (level) {
    case 'A': return 'success'
    case 'B': return 'primary'
    case 'C': return 'info'
    default: return 'info'
  }
}

function getStageLabel(stage: string): string {
  const labels: Record<string, string> = {
    lead: '线索',
    qualified: '资格审查',
    proposal: '方案报价',
    negotiation: '谈判中',
    closed: '已成交',
    lost: '已流失'
  }
  return labels[stage] || stage
}

function getStageColor(stage: string): string {
  return stageColors[stage] || '#6b7280'
}

function getStagePercentage(count: number): number {
  const total = customerStore.totalCount
  if (total === 0) return 0
  return Math.min(100, (count / total) * 100)
}

function formatMoney(value: number | undefined): string {
  if (!value) return '¥0'
  if (value >= 10000) {
    return `¥${(value / 10000).toFixed(1)}万`
  }
  return `¥${value.toLocaleString()}`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function formatRelativeTime(dateStr: string | undefined): string {
  if (!dateStr) return '暂无'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60))

  if (diff < 0) return '刚刚'
  if (diff < 1) return '刚刚'
  if (diff < 24) return `${diff}小时前`
  const days = Math.floor(diff / 24)
  if (days < 30) return `${days}天前`
  return formatDate(dateStr)
}
</script>
