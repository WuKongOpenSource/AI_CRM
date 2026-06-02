<template>
  <div class="wecom-customers-view">
    <header class="wecom-customers-view__header">
      <div>
        <h2>企微客户</h2>
        <p>{{ totalLabel }}</p>
      </div>
      <div class="wecom-customers-view__filters">
        <el-input
          v-model="keyword"
          clearable
          class="wecom-customers-view__search"
          placeholder="搜索企微客户"
          @clear="loadCustomers"
          @keyup.enter="loadCustomers"
        >
          <template #prefix>
            <span class="material-symbols-outlined text-[18px] text-slate-400">search</span>
          </template>
        </el-input>
        <el-select v-model="bindStatus" clearable placeholder="绑定状态" @change="loadCustomers">
          <el-option label="已绑定" value="BOUND" />
          <el-option label="未绑定" value="UNBOUND" />
        </el-select>
        <el-button :loading="loading" @click="loadCustomers">
          <span class="material-symbols-outlined mr-1 text-[18px]">refresh</span>
          刷新
        </el-button>
      </div>
    </header>

    <section class="wecom-customers-view__body">
      <el-table v-loading="loading" :data="customers" height="100%" row-key="id">
        <el-table-column label="企微客户" min-width="260">
          <template #default="{ row }">
            <div class="wecom-customers-view__customer">
              <img v-if="row.avatar" :src="row.avatar" alt="" />
              <span v-else>{{ initials(row.name || row.externalUserId) }}</span>
              <div class="min-w-0">
                <strong class="truncate">{{ row.name || row.externalUserId }}</strong>
                <p class="truncate">{{ row.corpName || row.corpFullName || row.externalUserId }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="绑定客户" min-width="220">
          <template #default="{ row }">
            <span v-if="row.customerName" class="font-medium text-slate-800">{{ row.customerName }}</span>
            <span v-else class="text-slate-400">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.bindStatus === 'BOUND' ? 'success' : 'info'" effect="plain">
              {{ row.bindStatus === 'BOUND' ? '已绑定' : '未绑定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="同步时间" width="180">
          <template #default="{ row }">{{ formatDate(row.syncedAt) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="right">
          <template #default="{ row }">
            <el-button v-if="row.bindStatus !== 'BOUND'" link type="primary" @click="openBindDialog(row)">
              绑定
            </el-button>
            <el-button v-else link type="danger" @click="handleUnbind(row)">
              解绑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <footer class="wecom-customers-view__pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="limit"
        :total="total"
        :page-sizes="[15, 30, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadCustomers"
        @size-change="loadCustomers"
      />
    </footer>

    <el-dialog v-model="bindDialogVisible" title="绑定系统客户" width="760px" class="wecom-bind-dialog">
      <div class="wecom-bind-dialog__toolbar">
        <el-input
          v-model="customerKeyword"
          clearable
          placeholder="搜索系统客户"
          @clear="loadCrmCustomers"
          @keyup.enter="loadCrmCustomers"
        >
          <template #prefix>
            <span class="material-symbols-outlined text-[18px] text-slate-400">search</span>
          </template>
        </el-input>
        <el-button :loading="crmCustomerLoading" @click="loadCrmCustomers">搜索</el-button>
      </div>
      <el-table
        v-loading="crmCustomerLoading"
        :data="crmCustomers"
        height="360"
        row-key="customerId"
        highlight-current-row
        @row-click="selectedCrmCustomerId = $event.customerId"
      >
        <el-table-column label="客户名称" prop="companyName" min-width="260" show-overflow-tooltip />
        <el-table-column label="负责人" prop="ownerName" width="140" show-overflow-tooltip />
        <el-table-column label="阶段" prop="stage" width="120" />
      </el-table>
      <template #footer>
        <div class="wecom-bind-dialog__footer">
          <el-button @click="bindDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!selectedCrmCustomerId" :loading="binding" @click="handleBind">
            绑定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bindWecomCustomer, queryWecomCustomers, unbindWecomCustomer } from '@/api/wecom'
import { queryCustomerList } from '@/api/customer'
import type { CustomerListVO } from '@/types/customer'
import type { WecomExternalCustomerVO } from '@/types/wecom'

const keyword = ref('')
const route = useRoute()
const bindStatus = ref('')
const page = ref(1)
const limit = ref(15)
const total = ref(0)
const loading = ref(false)
const customers = ref<WecomExternalCustomerVO[]>([])
const bindDialogVisible = ref(false)
const selectedWecomCustomer = ref<WecomExternalCustomerVO | null>(null)
const selectedCrmCustomerId = ref('')
const customerKeyword = ref('')
const crmCustomers = ref<CustomerListVO[]>([])
const crmCustomerLoading = ref(false)
const binding = ref(false)

const totalLabel = computed(() => `${total.value} 个企微客户`)
const preferredCustomerId = computed(() => {
  const raw = route.query.customerId
  return typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] || '' : ''
})

onMounted(loadCustomers)

async function loadCustomers() {
  loading.value = true
  try {
    const data = await queryWecomCustomers({
      page: page.value,
      limit: limit.value,
      keyword: keyword.value.trim() || undefined,
      bindStatus: bindStatus.value || undefined
    })
    customers.value = data.list || []
    total.value = data.totalRow || 0
  } finally {
    loading.value = false
  }
}

async function openBindDialog(row: WecomExternalCustomerVO) {
  selectedWecomCustomer.value = row
  selectedCrmCustomerId.value = preferredCustomerId.value || ''
  bindDialogVisible.value = true
  await loadCrmCustomers()
}

async function loadCrmCustomers() {
  crmCustomerLoading.value = true
  try {
    const data = await queryCustomerList({
      page: 1,
      limit: 20,
      keyword: customerKeyword.value.trim() || undefined
    })
    crmCustomers.value = data.list || []
  } finally {
    crmCustomerLoading.value = false
  }
}

async function handleBind() {
  if (!selectedWecomCustomer.value || !selectedCrmCustomerId.value) return
  binding.value = true
  try {
    await bindWecomCustomer({
      customerId: selectedCrmCustomerId.value,
      externalCustomerId: selectedWecomCustomer.value.id
    })
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
    await loadCustomers()
  } finally {
    binding.value = false
  }
}

async function handleUnbind(row: WecomExternalCustomerVO) {
  await ElMessageBox.confirm('确认解除该企微客户绑定？', '提示', {
    type: 'warning',
    confirmButtonText: '解绑',
    cancelButtonText: '取消'
  })
  await unbindWecomCustomer({ customerId: row.customerId, externalCustomerId: row.id })
  ElMessage.success('已解绑')
  await loadCustomers()
}

function initials(value?: string) {
  return (value || '?').trim().slice(0, 1).toUpperCase()
}

function formatDate(value?: string) {
  if (!value) return ''
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
.wecom-customers-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #f8fafc;
  padding: 20px 24px;
}

.wecom-customers-view__header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.wecom-customers-view__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
  line-height: 30px;
}

.wecom-customers-view__header p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 13px;
}

.wecom-customers-view__filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.wecom-customers-view__search {
  width: 260px;
}

.wecom-customers-view__body {
  min-height: 0;
  flex: 1;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.wecom-customers-view__customer {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.wecom-customers-view__customer img,
.wecom-customers-view__customer > span {
  display: flex;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #dcfce7;
  color: #166534;
  font-size: 13px;
  font-weight: 700;
  object-fit: cover;
}

.wecom-customers-view__customer strong {
  display: block;
  color: #0f172a;
  font-size: 14px;
}

.wecom-customers-view__customer p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 12px;
}

.wecom-customers-view__pager {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding-top: 12px;
}

.wecom-bind-dialog__toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.wecom-bind-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 760px) {
  .wecom-customers-view {
    padding: 14px;
  }

  .wecom-customers-view__header,
  .wecom-customers-view__filters {
    align-items: stretch;
    flex-direction: column;
  }

  .wecom-customers-view__search {
    width: 100%;
  }
}
</style>
