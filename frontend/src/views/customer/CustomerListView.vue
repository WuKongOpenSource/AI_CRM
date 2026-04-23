<template>
  <div ref="pageRootRef" data-customer-page-root class="flex flex-col gap-6 px-6 py-6">
    <!-- Header -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold text-slate-900">客户列表</h2>
        <p class="text-sm text-slate-500">管理您的客户关系并查看 AI 驱动的业务洞察。</p>
      </div>
      <div class="flex flex-nowrap sm:flex-wrap items-center gap-3 flex-wrap">
        <!-- Search -->
        <div class="relative group flex items-center">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-primary transition-colors">search</span>
          <input
            v-model="customerStore.queryParams.keyword"
            type="text"
            placeholder="搜索公司名称、联系人、电话、标签..."
            class="pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm w-full sm:w-80 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all shadow-sm"
            @keydown.enter="handleSearch"
            @input="debouncedSearch"
          />
        </div>
        <el-popover
          v-model:visible="showAiSearchPopover"
          trigger="click"
          placement="bottom-end"
          :width="420"
          popper-class="wk-ai-search-popover"
        >
          <template #reference>
            <button
              class="h-10 px-4 bg-white border border-primary/30 text-primary rounded-xl text-sm font-bold whitespace-nowrap hover:bg-primary/5 transition-all shadow-sm flex items-center gap-2"
              type="button"
            >
              <WkIcon name="ai" class="text-sm" />
              AI 搜索
            </button>
          </template>

          <div class="space-y-4">
            <div class="space-y-2">
              <p class="text-sm font-bold text-slate-900">AI 搜索场景示例</p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="example in AI_SEARCH_EXAMPLES"
                  :key="example"
                  type="button"
                  class="px-3 py-1.5 rounded-full bg-slate-50 text-slate-600 text-xs font-medium hover:bg-primary/10 hover:text-primary transition-colors"
                  @click="applyAiSearchExample(example)"
                >
                  {{ example }}
                </button>
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wide">场景描述</label>
              <textarea
                v-model="aiSearchInput"
                rows="3"
                placeholder="例如：30 天未跟进的制造业客户"
                class="w-full px-3 py-2.5 bg-white border border-slate-200 rounded-xl text-sm text-slate-700 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all resize-none"
                @keydown.enter.exact.prevent="handleAiSearch"
              />
              <p class="text-xs text-slate-500 leading-5">{{ aiSearchStatusText }}</p>
            </div>

            <div class="flex items-center justify-between gap-3">
              <button
                v-if="hasAiSearchState"
                class="text-xs font-medium text-slate-500 hover:text-red-500 transition-colors"
                type="button"
                @click="clearAiSearch"
              >
                清空 AI 条件
              </button>
              <div class="flex items-center gap-2 ml-auto">
                <button
                  class="px-3 py-2 text-sm font-medium text-slate-500 hover:text-slate-800 transition-colors"
                  type="button"
                  @click="showAiSearchPopover = false"
                >
                  取消
                </button>
                <button
                  class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  :disabled="!aiSearchInput.trim() || aiSearchLoading"
                  type="button"
                  @click="handleAiSearch"
                >
                  {{ aiSearchLoading ? '解析中...' : 'AI 搜索' }}
                </button>
              </div>
            </div>
          </div>
        </el-popover>
        <!-- Import/Export - desktop only -->
        <div v-if="!isMobile" class="flex items-center gap-1.5 border-r border-slate-200 pr-3 mr-1">
          <button class="h-10 px-4 text-sm font-medium text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors flex items-center gap-2" @click="showImportDialog = true">
            <span class="material-symbols-outlined text-[18px] leading-none">upload</span>
            导入
          </button>
          <button class="h-10 px-4 text-sm font-medium text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors flex items-center gap-2" :disabled="exporting" @click="handleExport">
            <span class="material-symbols-outlined text-[18px] leading-none">download</span>
            导出
          </button>
        </div>
        <!-- Add Customer -->
        <button
          class="h-10 px-4 bg-primary text-white rounded-xl text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2"
          @click="showAddDialog = true"
        >
          <span class="material-symbols-outlined wk-plus-button-icon">person_add</span>
          <span v-if="!isMobile">新增客户</span>
        </button>
      </div>
    </div>

    <div v-if="hasAiSearchState" class="bg-white border border-primary/10 rounded-2xl px-4 py-3 shadow-sm">
      <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div class="flex flex-wrap items-center gap-2">
          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold">
            <WkIcon name="ai" class="text-[12px]" />
            AI 筛选
          </span>
          <el-tag
            v-for="chip in aiSearchState?.displayChips || []"
            :key="chip.key"
            closable
            size="small"
            class="wk-ai-chip"
            @close="handleRemoveAiChip(chip.key)"
          >
            {{ chip.label }}
          </el-tag>
        </div>

        <button
          class="text-xs font-medium text-slate-500 hover:text-red-500 transition-colors self-start"
          type="button"
          @click="clearAiSearch"
        >
          清空
        </button>
      </div>
      <p v-if="aiSearchExplanation" class="mt-2 text-xs text-slate-500 leading-5">
        {{ aiSearchExplanation }}
      </p>
    </div>

    <!-- Main Content: Table + AI Sidebar -->
    <div class="flex flex-col xl:flex-row gap-6 xl:items-start relative overflow-x-hidden">
      <!-- Table Area -->
      <div class="w-full min-w-0 flex-1 space-y-6">
        <div
          ref="tableCardRef"
          class="bg-white border border-slate-200 rounded-xl shadow-sm flex flex-col overflow-hidden"
          v-loading="customerStore.loading"
        >
          <div v-if="!isMobile">
            <el-table
              :data="customerStore.customerList"
              :height="tableHeight"
              row-key="customerId"
              table-layout="fixed"
              class="wk-customer-table"
              empty-text="暂无客户数据"
              @row-click="handleCustomerRowClick"
            >
              <template v-for="field in listFields" :key="field.fieldId">
                <el-table-column
                  :label="field.fieldLabel"
                  :fixed="getFieldFixed(field)"
                  :min-width="getFieldMinWidth(field)"
                  :align="getFieldAlign(field)"
                >
                  <template #header>
                    <span class="normal-case tracking-normal">{{ getListFieldLabel(field) }}</span>
                  </template>
                  <template #default="{ row }">
                    <template v-if="field.fieldSource === 'custom'">
                      <template v-if="field.fieldType === 'checkbox'">
                        <span
                          v-if="getCustomFieldCheckboxState(row.customFields?.[field.fieldName]) !== null"
                          class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap"
                          :class="getCustomFieldCheckboxState(row.customFields?.[field.fieldName])
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-slate-100 text-slate-600'"
                        >
                          <span
                            class="size-1.5 rounded-full"
                            :class="getCustomFieldCheckboxState(row.customFields?.[field.fieldName])
                              ? 'bg-emerald-500'
                              : 'bg-slate-400'"
                          ></span>
                          {{ getCustomFieldCheckboxState(row.customFields?.[field.fieldName]) ? '开启' : '关闭' }}
                        </span>
                        <span v-else class="text-sm text-slate-300 whitespace-nowrap">-</span>
                      </template>
                      <span
                        v-else
                        class="block text-sm text-slate-600 truncate"
                        :title="formatCustomFieldValue(field, row.customFields?.[field.fieldName])"
                      >
                        {{ formatCustomFieldValue(field, row.customFields?.[field.fieldName]) }}
                      </span>
                    </template>
                    <template v-else-if="field.fieldName === 'companyName'">
                      <div class="flex items-center gap-3 min-w-0">
                        <div class="size-8 rounded overflow-hidden border border-slate-200 bg-white flex items-center justify-center flex-shrink-0">
                          <img
                            v-if="row.logoUrl"
                            :src="row.logoUrl"
                            :alt="row.companyName || 'company logo'"
                            class="size-full object-contain"
                          />
                          <span v-else class="bg-primary/10 text-primary flex size-full items-center justify-center font-bold text-xs">
                            {{ row.companyName?.charAt(0) || '?' }}
                          </span>
                        </div>
                        <span class="text-sm font-semibold text-slate-900 truncate block transition-colors">{{ row.companyName || '-' }}</span>
                      </div>
                    </template>
                    <template v-else-if="field.fieldName === 'aiStatusDetection'">
                      <div class="flex items-center justify-center py-1">
                        <span
                          v-if="getAiStatusMeta(row.aiStatusDetection)"
                          class="inline-flex items-center gap-2 rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap"
                          :class="getAiStatusMeta(row.aiStatusDetection)?.badgeClass"
                        >
                          <span class="size-1.5 rounded-full mr-1.5" :class="getAiStatusMeta(row.aiStatusDetection)?.dotClass"></span>
                          {{ getAiStatusMeta(row.aiStatusDetection)?.label }}
                        </span>
                        <span v-else class="text-sm text-slate-300 whitespace-nowrap">-</span>
                      </div>
                    </template>
                    <template v-else-if="field.fieldName === 'aiInsight'">
                      <div v-if="row.aiInsight" class="py-1">
                        <el-tooltip
                          placement="top"
                          effect="light"
                          :show-after="150"
                          popper-class="wk-ai-insight-tooltip"
                        >
                          <template #content>
                            <div class="wk-ai-insight-tooltip__content">
                              {{ row.aiInsight }}
                            </div>
                          </template>
                          <p class="wk-ai-insight-text text-sm text-slate-600">
                            {{ getAiInsightPreview(row.aiInsight) }}
                          </p>
                        </el-tooltip>
                      </div>
                      <span v-else class="text-sm text-slate-300 whitespace-nowrap">-</span>
                    </template>
                    <template v-else-if="field.fieldName === 'level'">
                      <span
                        v-if="row.level"
                        class="inline-flex items-center justify-center h-6 min-w-[2.5rem] px-2 rounded-lg font-bold text-xs"
                        :class="getLevelBadgeClass(row.level)"
                      >
                        {{ getLevelLabel(field, row.level) }}
                      </span>
                      <span v-else class="text-slate-300">-</span>
                    </template>
                    <template v-else-if="field.fieldName === 'stage'">
                      <span
                        v-if="row.stage"
                        class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium whitespace-nowrap"
                        :class="getStageBadgeClass(row.stage)"
                      >
                        <span class="size-1.5 rounded-full mr-1.5" :class="getStageDotClass(row.stage)"></span>
                        {{ getConfiguredStageLabel(field, row.stage) }}
                      </span>
                      <span v-else class="text-slate-300">-</span>
                    </template>
                    <span
                      v-else
                      class="block text-sm text-slate-600 truncate whitespace-nowrap"
                      :title="getListFieldDisplayValue(field, row)"
                    >
                      {{ getListFieldDisplayValue(field, row) }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column v-if="field.fieldName === 'companyName'" :key="`${field.fieldId}-contact`" label="联系人" min-width="140">
                  <template #default="{ row }">
                    <div v-if="row.primaryContactName" class="text-sm text-slate-600 whitespace-nowrap">
                      <div>{{ row.primaryContactName }}</div>
                      <div v-if="row.primaryContactPosition" class="text-xs text-slate-400">{{ row.primaryContactPosition }}</div>
                    </div>
                    <span v-else class="text-sm text-slate-300">-</span>
                  </template>
                </el-table-column>

                <el-table-column v-if="field.fieldName === 'companyName'" :key="`${field.fieldId}-phone`" label="电话" min-width="140">
                  <template #default="{ row }">
                    <span class="text-sm text-slate-600 font-mono whitespace-nowrap">{{ row.primaryContactPhone || '-' }}</span>
                  </template>
                </el-table-column>
              </template>

              <el-table-column label="负责人" min-width="140">
                <template #default="{ row }">
                  <div class="flex items-center gap-2" data-row-action="true" @click.stop>
                    <div class="size-6 rounded-full bg-slate-100 flex items-center justify-center text-xs font-bold text-slate-500 flex-shrink-0">
                      {{ row.ownerName?.charAt(0) || '?' }}
                    </div>
                    <el-popover trigger="click" :width="220" @show="loadUserList">
                      <template #reference>
                        <span class="cursor-pointer hover:text-primary transition-colors truncate max-w-[100px] inline-block align-middle text-sm text-slate-600">
                          {{ row.ownerName || '-' }}
                        </span>
                      </template>
                      <div>
                        <el-input v-model="ownerSearch" placeholder="搜索用户" size="small" clearable class="mb-2" />
                        <div class="max-h-48 overflow-auto">
                          <div
                            v-for="u in filteredUserList"
                            :key="u.userId"
                            class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer hover:bg-slate-100 transition-colors"
                            :class="{ 'bg-primary/5': String(u.userId) === String(row.ownerId) }"
                            @click="handleTransfer(row, u)"
                          >
                            <div class="size-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold flex-shrink-0">
                              {{ u.realname?.charAt(0) || '?' }}
                            </div>
                            <span class="text-sm truncate">{{ u.realname }}</span>
                            <span v-if="String(u.userId) === String(row.ownerId)" class="material-symbols-outlined ml-auto text-primary text-sm">check</span>
                          </div>
                          <div v-if="filteredUserList.length === 0" class="text-center text-sm text-slate-400 py-3">无匹配用户</div>
                        </div>
                      </div>
                    </el-popover>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="操作" fixed="right" width="132" align="right">
                <template #default="{ row }">
                  <div class="flex justify-end" data-row-action="true" @click.stop>
                    <button
                      class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-primary/10 text-primary text-xs font-bold rounded-lg hover:bg-primary/20 transition-colors"
                      @click="handleAiFollowUp(row)"
                    >
                      <WkIcon name="ai" class="text-sm" />
                      AI 跟进
                    </button>
                  </div>
                </template>
              </el-table-column>

              <template #empty>
                <div class="text-center py-16">
                  <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
                    <span class="material-symbols-outlined text-4xl">group</span>
                  </div>
                  <p class="text-slate-400 text-sm font-medium">暂无客户数据</p>
                </div>
              </template>
            </el-table>
          </div>

          <div v-else class="wk-customer-cards px-3 py-3">
            <div v-if="customerStore.customerList.length === 0" class="text-center py-16">
              <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
                <span class="material-symbols-outlined text-4xl">group</span>
              </div>
              <p class="text-slate-400 text-sm font-medium">暂无客户数据</p>
            </div>

            <div v-else class="flex flex-col gap-3">
              <div
                v-for="row in customerStore.customerList"
                :key="row.customerId"
                role="button"
                tabindex="0"
                class="wk-customer-card w-full text-left bg-white border border-slate-200 rounded-xl px-4 py-3 shadow-sm hover:bg-slate-50 active:bg-slate-100 transition-colors cursor-pointer"
                @click="handleRowClick(row)"
                @keydown.enter.prevent="handleRowClick(row)"
                @keydown.space.prevent="handleRowClick(row)"
              >
                <div class="flex items-start gap-3">
                  <div class="size-10 rounded-lg overflow-hidden border border-slate-200 bg-white flex items-center justify-center flex-shrink-0">
                    <img
                      v-if="row.logoUrl"
                      :src="row.logoUrl"
                      :alt="row.companyName || 'company logo'"
                      class="size-full object-contain"
                    />
                    <span v-else class="bg-primary/10 text-primary flex size-full items-center justify-center font-bold text-sm">
                      {{ row.companyName?.charAt(0) || '?' }}
                    </span>
                  </div>

                  <div class="min-w-0 flex-1">
                    <div class="flex items-start gap-2">
                      <div class="min-w-0 flex-1">
                        <div class="text-sm font-bold text-slate-900 truncate">
                          {{ row.companyName || '-' }}
                        </div>
                      </div>
                      <span class="material-symbols-outlined text-slate-300 text-base leading-none pt-0.5">chevron_right</span>
                    </div>

                    <div class="mt-2 grid grid-cols-2 gap-x-3 gap-y-2">
                      <div class="min-w-0">
                        <div class="text-[11px] font-bold text-slate-400 tracking-wide uppercase">industry</div>
                        <div class="text-sm text-slate-600 truncate" :title="getMobileIndustryText(row)">
                          {{ getMobileIndustryText(row) }}
                        </div>
                      </div>

                      <div class="min-w-0">
                        <div class="text-[11px] font-bold text-slate-400 tracking-wide uppercase">level</div>
                        <div class="mt-0.5">
                          <span
                            v-if="row.level"
                            class="inline-flex items-center justify-center h-6 min-w-[2.5rem] px-2 rounded-lg font-bold text-xs"
                            :class="getLevelBadgeClass(row.level)"
                          >
                            {{ getMobileLevelText(row) }}
                          </span>
                          <span v-else class="text-sm text-slate-300">-</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div
            v-if="customerStore.totalCount > 0"
            ref="paginationBarRef"
            class="shrink-0 px-6 py-4 bg-slate-50/50 flex items-center justify-between border-t border-slate-200"
          >
            <span class="text-sm text-slate-500">
              共 {{ customerStore.totalCount }} 条客户数据
            </span>
            <div class="flex items-center gap-1">
              <button
                class="size-8 flex items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50"
                :disabled="(customerStore.queryParams.page || 1) <= 1"
                @click="handlePageChange((customerStore.queryParams.page || 1) - 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_left</span>
              </button>
              <button
                v-for="pageNum in visiblePages"
                :key="pageNum"
                class="size-8 flex items-center justify-center rounded border text-xs font-bold"
                :class="pageNum === (customerStore.queryParams.page || 1)
                  ? 'border-primary bg-primary text-white'
                  : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'"
                @click="handlePageChange(pageNum)"
              >{{ pageNum }}</button>
              <span v-if="totalPages > 5" class="px-1 text-slate-400 text-xs">...</span>
              <button
                class="size-8 flex items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50"
                :disabled="(customerStore.queryParams.page || 1) >= totalPages"
                @click="handlePageChange((customerStore.queryParams.page || 1) + 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_right</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <CustomerInsightSidebar
        v-if="!isMobile"
        :negotiation-count="negotiationCount"
        :overdue-count="overdueCount"
        :closed-count="closedCount"
        :conversion-rate="conversionRate"
      />
    </div>

    <!-- Add/Edit Dialog -->
    <CustomerUpsertDialog
      v-model="showAddDialog"
      :mode="editingCustomer ? 'edit' : 'create'"
      :customer="editingCustomer"
      @success="handleUpsertSuccess"
    />

    <!-- Import Dialog -->
    <CustomerImportDialog
      v-model="showImportDialog"
      @success="handleImportSuccess"
    />

    <!-- AI Follow-up Drawer -->
    <AiFollowUpDrawer
      v-model="showAiFollowUpDrawer"
      :customer="aiFollowUpCustomer"
      @saved="handleAiFollowUpSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ref, computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { onBeforeRouteLeave, useRouter, useRoute } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  CustomerAiSearchDisplayChip,
  CustomerAiSearchParseVO,
  CustomerAiSearchQuery,
  CustomerExportBO,
  CustomerListVO,
  CustomerImportResult,
  CustomerQueryBO
} from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { getListFieldsByEntity } from '@/api/customField'
import { aiParseCustomerSearch, transferCustomer, exportCustomers } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'
import CustomerImportDialog from '@/views/customer/components/CustomerImportDialog.vue'
import CustomerInsightSidebar from '@/views/customer/components/CustomerInsightSidebar.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import { appEvents, APP_EVENT } from '@/utils/events'
import { getCustomerAiStatusMeta } from '@/utils/customerAi'
import { formatCustomFieldValue, getCustomFieldCheckboxState } from '@/utils/customFieldDisplay'

const router = useRouter()
const route = useRoute()
const customerStore = useCustomerStore()
const { aiSearchState } = storeToRefs(customerStore)
const { isMobile } = useResponsive()
const pageRootRef = ref<HTMLElement | null>(null)
const tableCardRef = ref<HTMLElement | null>(null)
const paginationBarRef = ref<HTMLElement | null>(null)
const tableHeight = ref<number | undefined>(undefined)
let layoutObserver: ResizeObserver | null = null
let tableHeightRaf = 0

const showAddDialog = ref(false)
const editingCustomer = ref<CustomerListVO | null>(null)
const listFields = ref<CustomField[]>([])

const AI_CUSTOMER_LIST_FIELDS: CustomField[] = [
  {
    fieldId: '__ai_status_detection__',
    entityType: 'customer',
    fieldName: 'aiStatusDetection',
    fieldLabel: 'AI状态探测',
    fieldType: 'text',
    fieldSource: 'system',
    columnName: 'ai_status_detection',
    isRequired: false,
    isSearchable: true,
    isShowInList: true,
    sortOrder: 900,
    status: 1
  },
  {
    fieldId: '__ai_insight__',
    entityType: 'customer',
    fieldName: 'aiInsight',
    fieldLabel: 'AI洞察',
    fieldType: 'textarea',
    fieldSource: 'system',
    columnName: 'ai_insight',
    isRequired: false,
    isSearchable: true,
    isShowInList: true,
    sortOrder: 910,
    status: 1
  }
]
const PINNED_LIST_FIELD_ORDER = ['companyName', 'aiStatusDetection', 'aiInsight'] as const

// Import/Export state
const exporting = ref(false)
const showImportDialog = ref(false)
const AI_SEARCH_EXAMPLES = [
  '报价大于 50 万的高价值客户',
  '30 天未跟进的制造业客户',
  '最近一周新增的客户',
  '活跃阶段的 A 级客户'
]
const showAiSearchPopover = ref(false)
const aiSearchInput = ref('')
const aiSearchLoading = ref(false)

const hasAiSearchState = computed(() => {
  return Boolean(aiSearchState.value?.displayChips.length || aiSearchState.value?.explanation)
})

function buildAiSearchExplanation(chips: CustomerAiSearchDisplayChip[] = []) {
  if (!chips.length) return ''

  const filterLabels = chips
    .filter(chip => chip.key !== 'sort' && chip.label)
    .map(chip => chip.label.trim())
  const sortLabel = chips.find(chip => chip.key === 'sort' && chip.label)?.label.trim()

  const parts: string[] = []
  if (filterLabels.length) {
    parts.push(`已识别筛选条件：${filterLabels.join('，')}`)
  }
  if (sortLabel) {
    parts.push(`已识别${sortLabel.replace(/^排序[:：]\s*/, '排序规则：')}`)
  }
  return parts.join('；')
}

const aiSearchExplanation = computed(() => {
  const state = aiSearchState.value
  if (!state) return ''
  if (state.fallbackKeywordSearch) return state.explanation || '本次已回退为关键词搜索'
  return buildAiSearchExplanation(state.displayChips || []) || state.explanation || ''
})

const aiSearchStatusText = computed(() => {
  if (aiSearchLoading.value) return '正在解析自然语言并生成筛选条件...'
  if (aiSearchExplanation.value) return aiSearchExplanation.value
  return '支持“30天未跟进的制造业客户”这类自然语言描述'
})

function normalizeCustomerQuery(query: CustomerAiSearchQuery): Partial<CustomerQueryBO> {
  const normalized: Partial<CustomerQueryBO> = {}
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    if (Array.isArray(value) && value.length === 0) continue
    normalized[key as keyof CustomerQueryBO] = value as never
  }
  return normalized
}

function buildExportPayload(): CustomerExportBO {
  return {
    ...customerStore.queryParams
  }
}

async function applyAiSearchResult(result: CustomerAiSearchParseVO) {
  aiSearchState.value = result
  aiSearchInput.value = result.normalizedQuery || result.originalQuery || aiSearchInput.value
  const normalizedQuery = normalizeCustomerQuery(result.parsedQuery || {})
  customerStore.replaceQueryParams(normalizedQuery)
  await customerStore.fetchCustomerList(true)
  showAiSearchPopover.value = false
}

async function handleAiSearch() {
  const query = aiSearchInput.value.trim()
  if (!query || aiSearchLoading.value) return
  aiSearchLoading.value = true
  try {
    const result = await aiParseCustomerSearch({ query })
    await applyAiSearchResult(result)
  } finally {
    aiSearchLoading.value = false
  }
}

function applyAiSearchExample(example: string) {
  aiSearchInput.value = example
  void handleAiSearch()
}

function syncAiKeywordChip(keyword: string) {
  if (!aiSearchState.value) return
  const chips = aiSearchState.value.displayChips.filter(chip => chip.key !== 'keyword')
  const parsedQuery = { ...aiSearchState.value.parsedQuery }

  if (keyword) {
    parsedQuery.keyword = keyword
    chips.unshift({ key: 'keyword', label: `关键词: ${keyword}` })
  } else {
    delete parsedQuery.keyword
  }

  aiSearchState.value = {
    ...aiSearchState.value,
    parsedQuery,
    displayChips: chips
  }
}

function resetAiSearchFilters() {
  aiSearchState.value = null
  aiSearchInput.value = ''
  customerStore.resetQueryParams()
}

async function clearAiSearch() {
  resetAiSearchFilters()
  await customerStore.fetchCustomerList(true)
}

function clearAiQueryField(query: CustomerAiSearchQuery, key: string) {
  switch (key) {
    case 'keyword':
      delete query.keyword
      break
    case 'industry':
      delete query.industry
      break
    case 'level':
      delete query.level
      break
    case 'stage':
      delete query.stage
      delete query.stages
      break
    case 'stages':
      delete query.stages
      delete query.stage
      break
    case 'tag':
      delete query.tag
      break
    case 'source':
      delete query.source
      break
    case 'quotation':
      delete query.quotationMin
      delete query.quotationMax
      break
    case 'contractAmount':
      delete query.contractAmountMin
      delete query.contractAmountMax
      break
    case 'revenue':
      delete query.revenueMin
      delete query.revenueMax
      break
    case 'lastContact':
      delete query.lastContactStart
      delete query.lastContactEnd
      delete query.includeNoLastContact
      break
    case 'nextFollow':
      delete query.nextFollowStart
      delete query.nextFollowEnd
      break
    case 'createTime':
      delete query.createTimeStart
      delete query.createTimeEnd
      break
    case 'contactCount':
      delete query.contactCountMin
      delete query.contactCountMax
      break
    case 'sort':
      delete query.sortBy
      delete query.sortOrder
      break
  }
}

async function handleRemoveAiChip(key: string) {
  if (!aiSearchState.value) return

  const parsedQuery: CustomerAiSearchQuery = { ...aiSearchState.value.parsedQuery }
  clearAiQueryField(parsedQuery, key)

  const displayChips = aiSearchState.value.displayChips.filter(chip => chip.key !== key)
  aiSearchState.value = {
    ...aiSearchState.value,
    parsedQuery,
    displayChips
  }

  if (displayChips.length === 0) {
    await clearAiSearch()
    return
  }

  customerStore.replaceQueryParams(normalizeCustomerQuery(parsedQuery))
  await customerStore.fetchCustomerList(true)
}

function handleUpsertSuccess(payload: { mode: 'create' | 'edit'; customerId?: string }) {
  // keep original behavior: refresh list after submit
  customerStore.fetchCustomerList(true)
  if (payload.mode === 'create' && payload.customerId) {
    router.push(`/customer/${payload.customerId}`)
    return
  }
  if (payload.mode === 'edit') {
    editingCustomer.value = null
  }
}

// AI Follow-up Drawer
const showAiFollowUpDrawer = ref(false)
const aiFollowUpCustomer = ref<CustomerListVO | null>(null)
const ROW_INTERACTIVE_SELECTOR = '[data-row-action="true"], button, a, input, textarea, select, .el-input, .el-textarea, .el-button'

function getTableNaturalHeight() {
  const tableRoot = tableCardRef.value?.querySelector('.wk-customer-table') as HTMLElement | null
  if (!tableRoot) return undefined

  const headerHeight = tableRoot.querySelector('.el-table__header-wrapper')?.getBoundingClientRect().height ?? 0
  const bodyHeight = tableRoot.querySelector('.el-table__body tbody')?.getBoundingClientRect().height
    ?? tableRoot.querySelector('.el-table__body')?.getBoundingClientRect().height
    ?? 0
  const emptyHeight = tableRoot.querySelector('.el-table__empty-block')?.getBoundingClientRect().height ?? 0
  const horizontalScrollbarHeight = tableRoot.querySelector('.el-scrollbar__bar.is-horizontal')?.getBoundingClientRect().height ?? 0
  const naturalHeight = headerHeight + Math.max(bodyHeight, emptyHeight) + horizontalScrollbarHeight

  return naturalHeight > 0 ? Math.ceil(naturalHeight) : undefined
}

function updateTableHeight() {
  const tableCardEl = tableCardRef.value
  if (!tableCardEl) return

  const scrollContainer = tableCardEl.closest('main')
  const pagePaddingBottom = pageRootRef.value
    ? parseFloat(window.getComputedStyle(pageRootRef.value).paddingBottom || '0')
    : 0
  const viewportBottom = scrollContainer instanceof HTMLElement
    ? scrollContainer.getBoundingClientRect().bottom
    : window.innerHeight
  const paginationHeight = paginationBarRef.value?.offsetHeight ?? 0
  const cardBorderHeight = tableCardEl.offsetHeight - tableCardEl.clientHeight
  const availableHeight = Math.floor(
    viewportBottom - tableCardEl.getBoundingClientRect().top - paginationHeight - pagePaddingBottom - cardBorderHeight
  )
  const naturalHeight = getTableNaturalHeight()

  if (!naturalHeight) {
    tableHeight.value = undefined
    return
  }

  if (availableHeight <= 0) {
    tableHeight.value = naturalHeight
    return
  }

  tableHeight.value = Math.min(naturalHeight, availableHeight)
}

function queueTableHeightUpdate() {
  if (tableHeightRaf) cancelAnimationFrame(tableHeightRaf)
  tableHeightRaf = window.requestAnimationFrame(() => {
    tableHeightRaf = 0
    updateTableHeight()
  })
}

function handleAiFollowUp(customer: CustomerListVO) {
  aiFollowUpCustomer.value = customer
  showAiFollowUpDrawer.value = true
}

function handleAiFollowUpSaved() {
  customerStore.fetchCustomerList(true)
}

// Owner transfer
const userList = ref<any[]>([])
const ownerSearch = ref('')
const userListLoaded = ref(false)

const filteredUserList = computed(() => {
  if (!ownerSearch.value) return userList.value
  const keyword = ownerSearch.value.toLowerCase()
  return userList.value.filter((u: any) =>
    u.realname?.toLowerCase().includes(keyword) || u.username?.toLowerCase().includes(keyword)
  )
})

// Pagination
const totalPages = computed(() => {
  return Math.ceil(customerStore.totalCount / (customerStore.queryParams.limit || 10))
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = customerStore.queryParams.page || 1
  const pages: number[] = []
  const maxVisible = 5
  let start = Math.max(1, current - Math.floor(maxVisible / 2))
  const end = Math.min(total, start + maxVisible - 1)
  start = Math.max(1, end - maxVisible + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const industryField = computed(() => listFields.value.find(field => field.fieldName === 'industry') || null)
const levelField = computed(() => listFields.value.find(field => field.fieldName === 'level') || null)

function getMobileIndustryText(row: CustomerListVO): string {
  if (!industryField.value) return (row.industry as unknown as string) || '-'
  return getListFieldDisplayValue(industryField.value, row)
}

function getMobileLevelText(row: CustomerListVO): string {
  if (!row.level) return '-'
  if (!levelField.value) return String(row.level)
  return getLevelLabel(levelField.value, String(row.level))
}

// Computed
const closedCount = computed(() => {
  return customerStore.customerList.filter(c => c.stage === 'closed').length
})

const conversionRate = computed(() => {
  const total = customerStore.totalCount
  if (total === 0) return '0.0'
  return ((closedCount.value / total) * 100).toFixed(1)
})

const negotiationCount = computed(() => {
  return customerStore.customerList.filter(c => c.stage === 'negotiation').length
})

const overdueCount = computed(() => {
  const sevenDaysAgo = new Date()
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
  return customerStore.customerList.filter(c => {
    if (!c.lastContactTime) return true
    return new Date(c.lastContactTime) < sevenDaysAgo
  }).length
})

function getFieldFixed(field: CustomField): 'left' | undefined {
  return field.fieldName === 'companyName' ? 'left' : undefined
}

function getFieldAlign(field: CustomField): 'left' | 'center' | 'right' {
  if (field.fieldName === 'aiStatusDetection') return 'center'
  if (field.fieldName === 'level') return 'center'
  if (field.fieldType === 'number' || ['quotation', 'contractAmount', 'revenue'].includes(field.fieldName)) return 'right'
  return 'left'
}

function getFieldMinWidth(field: CustomField): number {
  const widthMap: Record<string, number> = {
    companyName: 240,
    aiStatusDetection: 160,
    aiInsight: 320,
    industry: 120,
    stage: 130,
    level: 110,
    source: 140,
    website: 180,
    quotation: 130,
    contractAmount: 140,
    revenue: 140,
    lastContactTime: 120,
    nextFollowTime: 160,
    address: 180,
    remark: 180
  }

  return widthMap[field.fieldName] || 140
}

function getListFieldRawValue(field: CustomField, row: CustomerListVO): unknown {
  if (field.fieldSource === 'custom') {
    return row.customFields?.[field.fieldName]
  }

  return (row as unknown as Record<string, unknown>)[field.fieldName]
}

function getFieldOptionLabel(field: CustomField, value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  const normalizedValue = String(value)
  return field.options?.find(option => option.value === normalizedValue)?.label || normalizedValue
}

function getListFieldLabel(field: CustomField): string {
  if (field.fieldName === 'aiStatusDetection') return 'AI 状态探测'
  if (field.fieldName === 'aiInsight') return 'AI 洞察摘要'
  return field.fieldLabel
}

function getLevelLabel(field: CustomField, level: string): string {
  return getFieldOptionLabel(field, level)
}

function formatMoney(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  if (Number.isNaN(amount)) return String(value)

  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(amount)
}

function getListFieldDisplayValue(field: CustomField, row: CustomerListVO): string {
  const rawValue = getListFieldRawValue(field, row)

  if (rawValue === null || rawValue === undefined || rawValue === '') {
    return '-'
  }

  if (field.fieldName === 'lastContactTime') {
    return formatRelativeTime(String(rawValue))
  }

  if (['quotation', 'contractAmount', 'revenue'].includes(field.fieldName)) {
    return formatMoney(rawValue)
  }

  return formatCustomFieldValue(field, rawValue)
}

function getAiStatusMeta(value: string | undefined | null) {
  return getCustomerAiStatusMeta(value)
}

function getAiInsightPreview(value: string | undefined | null): string {
  const normalized = String(value || '').replace(/\s+/g, ' ').trim()
  return normalized || '-'
}

function getPinnedFieldOrder(fieldName: string): number {
  const index = PINNED_LIST_FIELD_ORDER.indexOf(fieldName as typeof PINNED_LIST_FIELD_ORDER[number])
  return index >= 0 ? index : Number.MAX_SAFE_INTEGER
}

function sortListFields(fields: CustomField[]): CustomField[] {
  return [...fields].sort((left, right) => {
    const leftPinnedOrder = getPinnedFieldOrder(left.fieldName)
    const rightPinnedOrder = getPinnedFieldOrder(right.fieldName)

    if (leftPinnedOrder !== rightPinnedOrder) {
      return leftPinnedOrder - rightPinnedOrder
    }

    const leftSortOrder = Number(left.sortOrder ?? Number.MAX_SAFE_INTEGER)
    const rightSortOrder = Number(right.sortOrder ?? Number.MAX_SAFE_INTEGER)
    if (leftSortOrder !== rightSortOrder) {
      return leftSortOrder - rightSortOrder
    }

    return left.fieldName.localeCompare(right.fieldName)
  })
}

async function loadListCustomFields() {
  try {
    const serverFields = await getListFieldsByEntity('customer')
    const existingFieldNames = new Set(serverFields.map(field => field.fieldName))
    listFields.value = sortListFields([
      ...serverFields,
      ...AI_CUSTOMER_LIST_FIELDS.filter(field => !existingFieldNames.has(field.fieldName))
    ])
  } catch {
    // Error handled by interceptor
  }
}

onMounted(async () => {
  aiSearchInput.value = aiSearchState.value?.normalizedQuery || aiSearchState.value?.originalQuery || ''
  queueTableHeightUpdate()
  window.addEventListener('resize', queueTableHeightUpdate, { passive: true })

  layoutObserver = new ResizeObserver(() => {
    queueTableHeightUpdate()
  })

  if (pageRootRef.value) layoutObserver.observe(pageRootRef.value)
  if (tableCardRef.value) layoutObserver.observe(tableCardRef.value)

  await Promise.all([
    loadListCustomFields(),
    customerStore.fetchCustomerList(true)
  ])

  await nextTick()
  queueTableHeightUpdate()

  if (route.query.action === 'create') {
    showAddDialog.value = true
    router.replace({ path: route.path, query: {} })
  }
})

onBeforeRouteLeave((to) => {
  if (to.path.startsWith('/customer')) return
  if (!aiSearchState.value) return
  resetAiSearchFilters()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', queueTableHeightUpdate)
  if (layoutObserver) layoutObserver.disconnect()
  if (tableHeightRaf) cancelAnimationFrame(tableHeightRaf)
  if (searchTimer) clearTimeout(searchTimer)
})

watch(() => [customerStore.totalCount, customerStore.customerList.length], async () => {
  await nextTick()
  queueTableHeightUpdate()
})

const offCustomerListRefresh = appEvents.on(APP_EVENT.CUSTOMER_LIST_REFRESH, () => {
  customerStore.fetchCustomerList(true)
})

onBeforeUnmount(() => {
  offCustomerListRefresh()
})

function handleSearch() {
  syncAiKeywordChip((customerStore.queryParams.keyword || '').trim())
  customerStore.queryParams.page = 1
  customerStore.fetchCustomerList(true)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
function debouncedSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => handleSearch(), 500)
}

function handlePageChange(page: number) {
  if (page < 1 || page > totalPages.value) return
  if (customerStore.queryParams.page === page) return
  customerStore.queryParams.page = page
  customerStore.fetchCustomerList(false)
}

function handleCustomerRowClick(row: CustomerListVO, _column: unknown, event: Event) {
  const target = event.target
  if (target instanceof HTMLElement && target.closest(ROW_INTERACTIVE_SELECTOR)) return
  handleRowClick(row)
}

function handleRowClick(row: CustomerListVO) {
  router.push(`/customer/${row.customerId}`)
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

function getConfiguredStageLabel(field: CustomField, stage: string): string {
  const optionLabel = getFieldOptionLabel(field, stage)
  return optionLabel !== stage ? optionLabel : getStageLabel(stage)
}

function getLevelBadgeClass(level: string): string {
  const classes: Record<string, string> = {
    A: 'bg-emerald-50 text-emerald-600',
    B: 'bg-blue-50 text-blue-600',
    C: 'bg-slate-100 text-slate-500'
  }
  return classes[level] || 'bg-slate-100 text-slate-500'
}

function getStageBadgeClass(stage: string): string {
  const classes: Record<string, string> = {
    lead: 'bg-slate-100 text-slate-800',
    qualified: 'bg-blue-100 text-blue-800',
    proposal: 'bg-amber-100 text-amber-800',
    negotiation: 'bg-purple-100 text-purple-800',
    closed: 'bg-green-100 text-green-800',
    lost: 'bg-red-100 text-red-800'
  }
  return classes[stage] || 'bg-slate-100 text-slate-800'
}

function getStageDotClass(stage: string): string {
  const classes: Record<string, string> = {
    lead: 'bg-slate-400',
    qualified: 'bg-blue-500',
    proposal: 'bg-amber-500',
    negotiation: 'bg-purple-500',
    closed: 'bg-green-500',
    lost: 'bg-red-500'
  }
  return classes[stage] || 'bg-slate-400'
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
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

async function loadUserList() {
  if (userListLoaded.value) return
  try {
    const res = await queryUserList()
    userList.value = (res.list || []).filter((u: any) => u.status === 1)
    userListLoaded.value = true
  } catch {
    // Error handled by interceptor
  }
}

async function handleTransfer(customer: CustomerListVO, user: any) {
  if (String(user.userId) === String(customer.ownerId)) return
  try {
    await ElMessageBox.confirm(
      `确定将客户「${customer.companyName}」的负责人变更为「${user.realname}」吗？`,
      '变更负责人',
      { type: 'warning' }
    )
    await transferCustomer([customer.customerId], String(user.userId))
    ElMessage.success('负责人变更成功')
    await customerStore.fetchCustomerList(true)
  } catch {
    // Cancelled or error handled
  }
}

// ==================== Import / Export ====================

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportCustomers(buildExportPayload())
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const today = new Date().toISOString().slice(0, 10)
    a.download = `客户数据_${today}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // Error handled by interceptor
  } finally {
    exporting.value = false
  }
}

async function handleImportSuccess(_result: CustomerImportResult) {
  await customerStore.fetchCustomerList(true)
}
</script>

<style scoped>
.wk-customer-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.wk-customer-table :deep(.el-table__border-left-patch),
.wk-customer-table :deep(.el-table__fixed-right-patch) {
  background: #f8fafc;
}

.wk-customer-table :deep(th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 16px 0;
  border-bottom: 1px solid #e2e8f0;
}

.wk-customer-table :deep(td.el-table__cell) {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}

.wk-customer-table :deep(.el-table__row) {
  cursor: pointer;
}

.wk-customer-table :deep(.el-table__body tr:hover > td.el-table__cell),
.wk-customer-table :deep(.el-table__body tr.hover-row > td.el-table__cell) {
  background: #eff6ff;
}

.wk-customer-table :deep(.el-table__empty-block) {
  min-height: 220px;
}

.wk-ai-insight-text {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

:deep(.wk-ai-insight-tooltip) {
  max-width: 420px;
}

:deep(.wk-ai-insight-tooltip__content) {
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
}

.wk-ai-chip :deep(.el-tag__content) {
  color: #137fec;
}

:deep(.wk-ai-search-popover) {
  border-radius: 20px;
  border: 1px solid #dbeafe;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
  padding: 16px;
}

.wk-customer-card {
  -webkit-tap-highlight-color: transparent;
}
</style>
