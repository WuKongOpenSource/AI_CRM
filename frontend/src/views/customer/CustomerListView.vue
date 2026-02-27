<template>
  <div class="h-full flex flex-col bg-gray-50">
    <!-- Header -->
    <div class="px-4 md:px-6 py-4 bg-white border-b border-gray-200">
      <div class="flex items-start justify-between">
        <div>
          <h1 class="text-lg font-semibold text-gray-900">客户管理</h1>
          <p class="text-sm text-gray-500 mt-1 hidden md:block">查看和管理所有客户信息与商机</p>
        </div>
        <div class="text-sm text-gray-500 hidden md:block">
          您的权限: <span class="text-gray-700">{{ userStore.realname || '销售经理' }}，完整权限</span>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="px-4 md:px-6 py-4 grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4 bg-gray-50">
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
    <div class="px-4 md:px-6 py-3 bg-white flex items-center gap-2 md:gap-4">
      <el-input
        v-model="customerStore.queryParams.keyword"
        placeholder="搜索客户公司或联系人..."
        :prefix-icon="Search"
        clearable
        class="flex-1"
        :class="{ 'max-w-md': !isMobile }"
        @change="handleSearch"
      />
      <!-- View mode toggle - desktop only -->
      <el-button-group v-if="!isMobile" class="flex-shrink-0">
        <el-button
          :type="viewMode === 'card' ? 'primary' : 'default'"
          @click="viewMode = 'card'"
        >
          <el-icon class="mr-1"><Grid /></el-icon>
          卡片
        </el-button>
        <el-button
          :type="viewMode === 'table' ? 'primary' : 'default'"
          @click="viewMode = 'table'"
        >
          <el-icon class="mr-1"><Tickets /></el-icon>
          表格
        </el-button>
      </el-button-group>
      <div v-if="!isMobile" class="flex-1"></div>
      <el-button v-if="!isMobile" :icon="Filter">筛选</el-button>
      <el-button v-if="!isMobile" :icon="Download" @click="handleExport" :loading="exporting">导出</el-button>
      <el-button v-if="!isMobile" :icon="Upload" @click="showImportDialog = true">导入</el-button>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
        <span v-if="!isMobile">添加客户</span>
      </el-button>
    </div>

    <!-- Stage Tabs -->
    <div class="px-4 md:px-6 py-3 bg-white flex gap-2 flex-wrap border-b border-gray-200 overflow-x-auto">
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
      <div ref="scrollContainer" class="flex-1 overflow-auto p-4 md:p-6" v-loading="customerStore.loading">

        <!-- ===== Card View ===== -->
        <template v-if="viewMode === 'card' || isMobile">
          <div class="space-y-4">
            <div
              v-for="customer in customerStore.customerList"
              :key="customer.customerId"
              class="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow cursor-pointer"
              @click="handleRowClick(customer)"
            >
              <!-- Row 1: Company name + Level + Stage + Actions -->
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2 md:gap-3 min-w-0 flex-1">
                  <div class="w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0 hidden md:flex">
                    <el-icon class="text-primary-500 text-xl"><OfficeBuilding /></el-icon>
                  </div>
                  <span class="font-medium text-base truncate">{{ customer.companyName }}</span>
                  <el-tag :type="getLevelType(customer.level)" size="small" round class="flex-shrink-0">
                    {{ customer.level }}级
                  </el-tag>
                  <el-tag
                    size="small"
                    round
                    class="flex-shrink-0"
                    :style="{
                      backgroundColor: getStageColor(customer.stage) + '20',
                      color: getStageColor(customer.stage),
                      borderColor: getStageColor(customer.stage)
                    }"
                  >
                    {{ getStageLabel(customer.stage) }}
                  </el-tag>
                </div>
                <div class="flex items-center gap-2 flex-shrink-0" @click.stop>
                  <el-dropdown
                    v-if="customer.stage !== 'closed' && customer.stage !== 'lost'"
                    trigger="click"
                    size="small"
                    @command="(stage: string) => handleAdvanceStage(customer.customerId, stage)"
                  >
                    <el-button type="primary" text size="small" class="hidden md:inline-flex">
                      推进
                      <el-icon class="el-icon--right"><ArrowRight /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-if="getNextStage(customer.stage)"
                          :command="getNextStage(customer.stage)"
                          :style="{ fontWeight: 'bold' }"
                        >
                          推进到「{{ allStageOptions.find(s => s.value === getNextStage(customer.stage))?.label }}」
                        </el-dropdown-item>
                        <el-dropdown-item v-if="getNextStage(customer.stage)" divided />
                        <el-dropdown-item
                          v-for="opt in allStageOptions.filter(s => s.value !== customer.stage)"
                          :key="opt.value"
                          :command="opt.value"
                        >
                          {{ opt.label }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-button type="primary" text size="small" :icon="Edit" class="hidden md:inline-flex" @click="handleEdit(customer)">编辑</el-button>
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
              <div v-if="customer.primaryContactName" class="mt-3 text-sm text-gray-600 flex items-center flex-wrap gap-x-3">
                <span>主要联系人:</span>
                <span class="inline-flex items-center gap-1">
                  <el-icon><User /></el-icon>
                  {{ customer.primaryContactName }}
                  <span v-if="customer.primaryContactPosition" class="text-gray-400">·{{ customer.primaryContactPosition }}</span>
                </span>
                <span v-if="customer.primaryContactPhone" class="inline-flex items-center gap-1 text-gray-500">
                  <el-icon><Phone /></el-icon>
                  {{ customer.primaryContactPhone }}
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
                  <el-popover trigger="click" :width="220" @show="loadUserList">
                    <template #reference>
                      <div class="flex items-center cursor-pointer hover:bg-gray-100 rounded px-1 -mx-1 transition-colors" @click.stop>
                        <el-avatar :size="24" class="mx-2 bg-primary-100 text-primary-600">
                          {{ customer.ownerName?.charAt(0) || '?' }}
                        </el-avatar>
                        <span class="text-gray-700 hover:text-primary-600">{{ customer.ownerName }}</span>
                      </div>
                    </template>
                    <div>
                      <el-input v-model="ownerSearch" placeholder="搜索用户" size="small" clearable class="mb-2" :prefix-icon="Search" />
                      <div class="max-h-48 overflow-auto">
                        <div
                          v-for="u in filteredUserList"
                          :key="u.userId"
                          class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer hover:bg-gray-100 transition-colors"
                          :class="{ 'bg-primary-50': String(u.userId) === String(customer.ownerId) }"
                          @click="handleTransfer(customer, u)"
                        >
                          <el-avatar :size="24" class="bg-primary-100 text-primary-600 flex-shrink-0">{{ u.realname?.charAt(0) || '?' }}</el-avatar>
                          <span class="text-sm truncate">{{ u.realname }}</span>
                          <el-icon v-if="String(u.userId) === String(customer.ownerId)" class="ml-auto text-primary-500"><Select /></el-icon>
                        </div>
                        <div v-if="filteredUserList.length === 0" class="text-center text-sm text-gray-400 py-3">
                          无匹配用户
                        </div>
                      </div>
                    </div>
                  </el-popover>
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

            <!-- Empty State (card) -->
            <div v-if="!customerStore.loading && customerStore.customerList.length === 0" class="text-center py-12 text-gray-500">
              <el-icon class="text-4xl mb-2"><Document /></el-icon>
              <p>暂无客户数据</p>
            </div>
          </div>

          <!-- Infinite scroll trigger (card only) -->
          <div
            v-if="customerStore.hasMore && customerStore.customerList.length > 0"
            ref="loadMoreTrigger"
            class="flex justify-center py-4"
          >
            <el-icon v-if="customerStore.loading" class="is-loading text-gray-400" :size="24"><Loading /></el-icon>
            <span v-else class="text-sm text-gray-400">向下滚动加载更多</span>
          </div>
        </template>

        <!-- ===== Table View ===== -->
        <template v-else>
          <el-table
            :data="customerStore.customerList"
            stripe
            border
            style="width: 100%"
            @row-click="handleRowClick"
            :row-style="{ cursor: 'pointer' }"
            :header-cell-style="{ backgroundColor: '#f5f7fa', color: '#606266' }"
          >
            <el-table-column prop="companyName" label="客户名称" min-width="160" fixed="left" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <div class="w-7 h-7 rounded bg-primary-50 flex items-center justify-center flex-shrink-0">
                    <el-icon class="text-primary-500"><OfficeBuilding /></el-icon>
                  </div>
                  <span class="font-medium">{{ row.companyName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="industry" label="行业" width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.industry || '未分类' }}</template>
            </el-table-column>
            <el-table-column prop="level" label="级别" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="getLevelType(row.level)" size="small" round>{{ row.level }}级</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="stage" label="阶段" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  round
                  :style="{
                    backgroundColor: getStageColor(row.stage) + '20',
                    color: getStageColor(row.stage),
                    borderColor: getStageColor(row.stage)
                  }"
                >
                  {{ getStageLabel(row.stage) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="负责人" width="100" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="flex items-center gap-1">
                  <el-avatar :size="22" class="bg-primary-100 text-primary-600 flex-shrink-0">{{ row.ownerName?.charAt(0) || '?' }}</el-avatar>
                  <span>{{ row.ownerName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="团队成员" width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <template v-if="row.teamMemberNames?.length">
                  <div class="flex items-center gap-0.5">
                    <el-avatar
                      v-for="(name, idx) in row.teamMemberNames.slice(0, 3)"
                      :key="idx"
                      :size="22"
                      class="bg-blue-100 text-blue-600 flex-shrink-0"
                      :style="idx > 0 ? { marginLeft: '-4px' } : {}"
                    >{{ name?.charAt(0) || '?' }}</el-avatar>
                  </div>
                </template>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column label="主要联系人" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <template v-if="row.primaryContactName">
                  <div>
                    {{ row.primaryContactName }}
                    <span v-if="row.primaryContactPosition" class="text-gray-400 text-xs">·{{ row.primaryContactPosition }}</span>
                  </div>
                  <div v-if="row.primaryContactPhone" class="text-xs text-gray-500 flex items-center gap-1">
                    <el-icon :size="12"><Phone /></el-icon>
                    {{ row.primaryContactPhone }}
                  </div>
                </template>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="contactCount" label="联系人数" width="90" align="center">
              <template #default="{ row }">{{ row.contactCount || 0 }}</template>
            </el-table-column>
            <el-table-column label="标签" min-width="150">
              <template #default="{ row }">
                <template v-if="row.tags?.length">
                  <el-tag
                    v-for="tag in row.tags.slice(0, 3)"
                    :key="tag"
                    size="small"
                    effect="plain"
                    class="mr-1 mb-0.5"
                  >{{ tag }}</el-tag>
                  <span v-if="row.tags.length > 3" class="text-xs text-gray-400">+{{ row.tags.length - 3 }}</span>
                </template>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="quotation" label="报价金额" width="110" align="right">
              <template #default="{ row }">
                <span v-if="row.quotation" class="text-primary-600">{{ formatMoney(row.quotation) }}</span>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="contractAmount" label="合同金额" width="110" align="right">
              <template #default="{ row }">
                <span v-if="row.contractAmount" class="text-primary-600">{{ formatMoney(row.contractAmount) }}</span>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="revenue" label="回款金额" width="110" align="right">
              <template #default="{ row }">
                <span v-if="row.revenue" class="text-green-600">{{ formatMoney(row.revenue) }}</span>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="客户来源" width="100" show-overflow-tooltip>
              <template #default="{ row }">{{ row.source || '-' }}</template>
            </el-table-column>
            <!-- Dynamic custom field columns -->
            <el-table-column
              v-for="field in listCustomFields"
              :key="field.fieldId"
              :label="field.fieldLabel"
              width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                {{ row.customFields?.[field.fieldName] ?? '-' }}
              </template>
            </el-table-column>
          </el-table>

          <!-- Empty State (table) -->
          <div v-if="!customerStore.loading && customerStore.customerList.length === 0" class="text-center py-12 text-gray-500">
            <el-icon class="text-4xl mb-2"><Document /></el-icon>
            <p>暂无客户数据</p>
          </div>
        </template>

        <!-- Pagination (shared) -->
        <div v-if="customerStore.totalCount > 0" class="mt-4 flex justify-center pb-4">
          <el-pagination
            v-model:current-page="customerStore.queryParams.page"
            v-model:page-size="customerStore.queryParams.limit"
            :total="customerStore.totalCount"
            :page-sizes="[10, 20, 50]"
            :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next'"
            :small="isMobile"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <!-- Right: Sidebar - desktop only -->
      <div v-if="!isMobile" class="w-80 border-l border-gray-200 bg-white overflow-auto flex-shrink-0">
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
      :width="isMobile ? '95%' : '600px'"
      :fullscreen="isMobile"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="formData.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="行业" prop="industry">
          <el-input v-model="formData.industry" placeholder="请输入行业" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="客户级别" prop="level">
              <el-select v-model="formData.level" class="w-full">
                <el-option label="A级客户" value="A" />
                <el-option label="B级客户" value="B" />
                <el-option label="C级客户" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
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
          <el-col :xs="24" :sm="12">
            <el-form-item label="电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
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

    <!-- Import Dialog -->
    <el-dialog
      v-model="showImportDialog"
      title="导入客户"
      :width="isMobile ? '95%' : '800px'"
      :fullscreen="isMobile"
      @close="resetImport"
    >
      <!-- Step 1: Upload -->
      <div v-if="importStep === 1" class="text-center py-6">
        <el-upload
          ref="importUploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleImportFileChange"
          drag
        >
          <el-icon class="text-4xl text-gray-400 mb-2"><Upload /></el-icon>
          <div class="text-gray-600">将 Excel 文件拖到此处，或<em class="text-primary-500">点击上传</em></div>
          <template #tip>
            <div class="text-xs text-gray-400 mt-2">支持 .xlsx / .xls 格式，表头需包含「公司名称」列</div>
            <div class="mt-2">
              <el-button type="primary" link size="small" @click.stop="handleDownloadTemplate">
                <el-icon class="mr-1"><Download /></el-icon>下载导入模板
              </el-button>
            </div>
          </template>
        </el-upload>
      </div>

      <!-- Step 2: Preview -->
      <div v-if="importStep === 2">
        <!-- Statistics -->
        <div class="flex gap-4 mb-4 flex-wrap">
          <el-tag>总计 {{ importPreview!.totalRows }} 行</el-tag>
          <el-tag type="success">有效 {{ importPreview!.validRows }} 行</el-tag>
          <el-tag v-if="importPreview!.duplicateRows > 0" type="warning">重复 {{ importPreview!.duplicateRows }} 行</el-tag>
          <el-tag v-if="importPreview!.errorRows > 0" type="danger">错误 {{ importPreview!.errorRows }} 行</el-tag>
        </div>

        <!-- Global duplicate handling -->
        <div v-if="importPreview!.duplicateRows > 0" class="mb-4 p-3 bg-yellow-50 rounded-lg">
          <span class="text-sm text-yellow-700 mr-3">重复行统一处理：</span>
          <el-radio-group v-model="globalDuplicateMode" @change="applyGlobalDuplicateMode">
            <el-radio value="skip">全部跳过</el-radio>
            <el-radio value="overwrite">全部覆盖</el-radio>
          </el-radio-group>
        </div>

        <!-- Data table -->
        <el-table
          :data="importPreview!.rows"
          :max-height="400"
          size="small"
          :row-class-name="importRowClassName"
        >
          <el-table-column label="行号" prop="rowNum" width="60" />
          <el-table-column label="公司名称" prop="companyName" min-width="120" />
          <el-table-column label="行业" prop="industry" width="100" />
          <el-table-column label="阶段" prop="stage" width="90">
            <template #default="{ row }">{{ getStageLabel(row.stage) || row.stage }}</template>
          </el-table-column>
          <el-table-column label="联系人" prop="contactName" width="90" />
          <el-table-column label="状态" width="150">
            <template #default="{ row }">
              <el-tag v-if="row.errors && row.errors.length > 0" type="danger" size="small">
                {{ row.errors[0] }}
              </el-tag>
              <template v-else-if="row.duplicate">
                <el-radio-group v-model="row.handleMode" size="small">
                  <el-radio value="skip">跳过</el-radio>
                  <el-radio value="overwrite">覆盖</el-radio>
                </el-radio-group>
              </template>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- Global errors -->
        <div v-if="importPreview!.errors && importPreview!.errors.length > 0" class="mt-3">
          <el-alert
            v-for="(err, idx) in importPreview!.errors"
            :key="idx"
            :title="err"
            type="error"
            :closable="false"
            class="mb-1"
          />
        </div>
      </div>

      <!-- Step 3: Result -->
      <div v-if="importStep === 3" class="text-center py-6">
        <el-icon class="text-5xl text-green-500 mb-3"><Select /></el-icon>
        <h3 class="text-lg font-medium mb-4">导入完成</h3>
        <div class="flex justify-center gap-6 text-sm">
          <div>新增 <span class="text-primary-600 font-bold text-lg">{{ importResult!.imported }}</span> 条</div>
          <div>更新 <span class="text-orange-500 font-bold text-lg">{{ importResult!.updated }}</span> 条</div>
          <div>跳过 <span class="text-gray-500 font-bold text-lg">{{ importResult!.skipped }}</span> 条</div>
        </div>
        <div v-if="importResult!.errors && importResult!.errors.length > 0" class="mt-4 text-left">
          <el-alert
            v-for="(err, idx) in importResult!.errors"
            :key="idx"
            :title="err"
            type="warning"
            :closable="false"
            class="mb-1"
          />
        </div>
      </div>

      <template #footer>
        <template v-if="importStep === 1">
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button type="primary" :loading="importLoading" :disabled="!importFile" @click="handleImportPreview">
            解析文件
          </el-button>
        </template>
        <template v-else-if="importStep === 2">
          <el-button @click="importStep = 1">上一步</el-button>
          <el-button type="primary" :loading="importLoading" @click="handleImportConfirm">
            确认导入
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="showImportDialog = false">完成</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import type { UploadInstance, UploadFile } from 'element-plus'
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
  Edit,
  Loading,
  Select,
  Download,
  Upload,
  Grid,
  Tickets,
  Phone
} from '@element-plus/icons-vue'
import type { CustomerListVO, CustomerAddBO, CustomerStage, CustomerImportPreview, CustomerImportRow, CustomerImportResult } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { transferCustomer, updateCustomerStage, exportCustomers, downloadImportTemplate, importCustomerPreview, confirmCustomerImport } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'

const router = useRouter()
const customerStore = useCustomerStore()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const showAddDialog = ref(false)
const editingCustomer = ref<CustomerListVO | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const dynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()
const customFieldValues = ref<Record<string, any>>({})
const listCustomFields = ref<CustomField[]>([])
const viewMode = ref<'card' | 'table'>('card')
const currentStage = ref('')
const statistics = ref<any>(null)
const loadMoreTrigger = ref<HTMLElement>()
const scrollContainer = ref<HTMLElement>()
let observer: IntersectionObserver | null = null

// Import/Export state
const exporting = ref(false)
const showImportDialog = ref(false)
const importStep = ref(1)
const importFile = ref<File | null>(null)
const importLoading = ref(false)
const importPreview = ref<CustomerImportPreview | null>(null)
const importResult = ref<CustomerImportResult | null>(null)
const globalDuplicateMode = ref('')
const importUploadRef = ref<UploadInstance>()

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

function handleEdit(row: CustomerListVO) {
  editingCustomer.value = row
  Object.assign(formData, {
    companyName: row.companyName,
    industry: row.industry || '',
    level: row.level || 'B',
    stage: row.stage || 'lead',
    contactName: row.primaryContactName || '',
    contactPhone: row.primaryContactPhone || '',
    contactEmail: ''
  })
  customFieldValues.value = row.customFields ? { ...row.customFields } : {}
  showAddDialog.value = true
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

const stageFlow = ['lead', 'qualified', 'proposal', 'negotiation', 'closed']
const allStageOptions = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
]

function getNextStage(current: string): string | null {
  const idx = stageFlow.indexOf(current)
  if (idx >= 0 && idx < stageFlow.length - 1) return stageFlow[idx + 1]
  return null
}

async function handleAdvanceStage(customerId: string, newStage: string) {
  try {
    await updateCustomerStage(customerId, newStage)
    await customerStore.fetchCustomerList(true)
    try { await customerStore.fetchStatistics(); statistics.value = customerStore.statistics } catch { /* ignore */ }
    ElMessage.success(`商机阶段已更新为「${allStageOptions.find(s => s.value === newStage)?.label || newStage}」`)
  } catch {
    // Error handled by interceptor
  }
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

// ==================== Import / Export ====================

async function handleDownloadTemplate() {
  try {
    const blob = await downloadImportTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '客户导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    // Error handled by interceptor
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportCustomers({
      keyword: customerStore.queryParams.keyword || undefined,
      stage: customerStore.queryParams.stage || undefined,
      level: customerStore.queryParams.level || undefined
    })
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

function handleImportFileChange(uploadFile: UploadFile) {
  importFile.value = uploadFile.raw || null
}

async function handleImportPreview() {
  if (!importFile.value) return
  importLoading.value = true
  try {
    importPreview.value = await importCustomerPreview(importFile.value)
    importStep.value = 2
    // Default duplicate handling to 'skip'
    if (importPreview.value.rows) {
      importPreview.value.rows.forEach(row => {
        if (row.duplicate && !row.handleMode) {
          row.handleMode = 'skip'
        }
      })
    }
  } catch {
    // Error handled by interceptor
  } finally {
    importLoading.value = false
  }
}

function applyGlobalDuplicateMode(mode: string) {
  if (!importPreview.value) return
  importPreview.value.rows.forEach(row => {
    if (row.duplicate) {
      row.handleMode = mode as 'skip' | 'overwrite'
    }
  })
}

function importRowClassName({ row }: { row: CustomerImportRow }): string {
  if (row.errors && row.errors.length > 0) return 'bg-red-50'
  if (row.duplicate) return 'bg-yellow-50'
  return ''
}

async function handleImportConfirm() {
  if (!importPreview.value) return
  importLoading.value = true
  try {
    importResult.value = await confirmCustomerImport(importPreview.value.rows)
    importStep.value = 3
    // Refresh customer list
    await customerStore.fetchCustomerList(true)
    try { await customerStore.fetchStatistics(); statistics.value = customerStore.statistics } catch { /* ignore */ }
  } catch {
    // Error handled by interceptor
  } finally {
    importLoading.value = false
  }
}

function resetImport() {
  importStep.value = 1
  importFile.value = null
  importPreview.value = null
  importResult.value = null
  globalDuplicateMode.value = ''
  importUploadRef.value?.clearFiles()
}
</script>
