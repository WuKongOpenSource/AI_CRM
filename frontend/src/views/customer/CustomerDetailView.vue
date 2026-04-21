<template>
  <div class="h-full flex flex-col">
    <!-- Loading -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <span class="material-symbols-outlined text-slate-300 text-4xl animate-spin">progress_activity</span>
    </div>

    <!-- Content -->
    <template v-else-if="customer">
      <!-- Sticky Header -->
      <div class="sticky top-0 z-20 bg-background-light/90 backdrop-blur-md px-4 md:px-8 pt-4 pb-4 border-b border-slate-200/50 shrink-0">
        <!-- Breadcrumb -->
        <div class="flex items-center gap-2 text-sm text-slate-500 mb-3">
          <button @click="router.push('/customer')" class="hover:text-primary flex items-center gap-1 transition-colors">
            <WkIcon name="customer" :size="14" />
            客户管理
          </button>
          <span class="material-symbols-outlined text-xs">chevron_right</span>
          <span class="text-slate-900 font-medium">客户详情</span>
        </div>

        <!-- Customer Info Card -->
        <div class="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
          <div class="flex justify-between">
            <div class="flex gap-4 min-w-0">
              <div class="size-14 bg-slate-100 rounded-lg flex items-center justify-center border border-slate-200 overflow-hidden shrink-0">
                <img
                  v-if="customer.logoUrl"
                  :src="customer.logoUrl"
                  :alt="customer.companyName || 'company logo'"
                  class="size-full object-contain bg-white"
                />
                <span v-else class="text-2xl font-bold text-slate-400">{{ customer.companyName?.charAt(0) || '?' }}</span>
              </div>
              <div class="min-w-0 space-y-2">
                <div class="flex items-center gap-3 flex-wrap">
                  <h2 class="text-xl font-bold text-slate-900 truncate">{{ customer.companyName }}</h2>
                  <!-- <span
                    class="px-2 py-0.5 text-xs font-bold rounded uppercase"
                    :class="getStageBadgeClass(customer.stage)"
                  >{{ getStageLabel(customer.stage) }}</span> -->
                  <span v-if="customer.level"
                    class="px-2 py-0.5 text-xs font-bold rounded"
                    :class="{
                      'bg-emerald-100 text-emerald-700': customer.level === 'A',
                      'bg-blue-100 text-blue-700': customer.level === 'B',
                      'bg-slate-100 text-slate-600': customer.level === 'C'
                    }"
                  >{{ customer.level }}级客户</span>
                </div>
                <div class="flex w-full min-w-0 flex-wrap items-center gap-x-3 gap-y-2 text-sm">
                  <div class="flex min-w-0 flex-wrap items-center gap-x-4 gap-y-1">
                    <div class="flex items-center gap-1 shrink-0">
                      <span class="text-slate-400">联系人:</span>
                      <span class="text-slate-600 font-medium">{{ primaryContact?.name || '-' }}</span>
                    </div>
                    <div class="flex items-center gap-1 shrink-0">
                      <span class="text-slate-400">手机:</span>
                      <span class="text-slate-600 font-mono font-medium">{{ primaryContact?.phone || '-' }}</span>
                    </div>
                    <div class="flex items-center gap-1 shrink-0">
                      <span class="text-slate-400">状态:</span>
                      <span class="text-primary font-bold">{{ getStageLabel(customer.stage) }}</span>
                    </div>
                  </div>
                  <div
                    v-if="customer.tags?.length || canEditCustomerTags"
                    class="ml-2 flex min-w-0 flex-wrap items-center justify-end gap-2"
                  >
                    <span
                      v-for="tag in customer.tags"
                      :key="tag.tagId"
                      class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-100 text-slate-700 group"
                    >
                      {{ tag.tagName }}
                      <span
                        v-if="canEditCustomerTags"
                        class="material-symbols-outlined text-xs text-slate-400 hover:text-red-500 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
                        @click="handleRemoveTag(tag)"
                      >close</span>
                    </span>
                    <button
                      v-if="canEditCustomerTags"
                      type="button"
                      class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-bold text-primary border border-dashed border-primary/30 hover:bg-primary/5 transition-colors"
                      @click="showAddTagDialog = true"
                    >
                      <span class="wk-plus-button-mark" aria-hidden="true">+</span>
                      <span>添加标签</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex gap-2 shrink-0">
              <el-popover
                v-if="canTransferCustomer"
                :visible="showTransferPopover"
                trigger="manual"
                virtual-triggering
                :virtual-ref="headerMoreButtonRef"
                placement="bottom-end"
                :width="260"
                @show="handleTransferPopoverShow"
                @hide="handleTransferPopoverHide"
                @update:visible="showTransferPopover = $event"
              >
                <div class="space-y-3">
                  <el-input
                    v-model="ownerSearch"
                    placeholder="搜索用户"
                    size="small"
                    clearable
                  />
                  <div v-loading="ownerListLoading" class="max-h-56 overflow-auto space-y-1">
                    <button
                      v-for="user in filteredTransferUserList"
                      :key="user.userId"
                      type="button"
                      class="w-full flex items-center gap-2 px-2 py-2 rounded-lg text-left transition-colors hover:bg-slate-100"
                      :class="{ 'bg-primary/5': String(user.userId) === String(customer.ownerId) }"
                      @click="handleTransferOwner(user)"
                    >
                      <div class="size-7 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-bold shrink-0">
                        {{ user.realname?.charAt(0) || '?' }}
                      </div>
                      <div class="min-w-0 flex-1">
                        <p class="text-sm font-medium text-slate-700 truncate">{{ user.realname }}</p>
                        <p class="text-xs text-slate-400 truncate">{{ user.username || '-' }}</p>
                      </div>
                      <span
                        v-if="String(user.userId) === String(customer.ownerId)"
                        class="material-symbols-outlined text-primary text-sm shrink-0"
                      >check</span>
                    </button>
                    <p v-if="!ownerListLoading && filteredTransferUserList.length === 0" class="py-4 text-center text-sm text-slate-400">
                      暂无匹配用户
                    </p>
                  </div>
                </div>
              </el-popover>
              <button v-if="canEditCustomer" class="h-8 px-4 inline-flex items-center border border-slate-200 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors" @click="handleEdit">编辑资料</button>
              <button v-if="canCreateFollowUps" class="h-8 px-4 bg-primary/10 text-primary border border-primary/20 rounded-lg text-sm font-bold flex items-center gap-1.5 hover:bg-primary/20 transition-colors" @click="handleAiFollowUp">
                <WkIcon name="ai" class="text-sm" />
                AI 跟进
              </button>
              <button
                type="button"
                class="h-8 shrink-0 inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 text-sm font-semibold text-white shadow-md shadow-primary/25 transition-colors hover:bg-primary/90 whitespace-nowrap"
                @click="showBasicInfoDrawer = true"
              >
                <span class="material-symbols-outlined text-base leading-none">info</span>
                <span>{{ viewBasicInfoButtonText }}</span>
              </button>
              <el-dropdown
                v-if="canTransferCustomer || canDeleteCustomer"
                trigger="click"
                @visible-change="onHeaderMoreDropdownVisible"
              >
                <button
                  ref="headerMoreButtonRef"
                  type="button"
                  class="h-8 w-8 shrink-0 inline-flex items-center justify-center rounded-lg border border-solid border-slate-200 text-slate-400 hover:bg-slate-50 hover:text-slate-600 transition-colors"
                  title="更多操作"
                >
                  <span class="material-symbols-outlined text-lg">more_horiz</span>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="canTransferCustomer" @click="openTransferPopoverFromMenu">
                      <span class="flex items-center gap-2">
                        <span class="material-symbols-outlined text-sm">swap_horiz</span>
                        转移负责人
                      </span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="canDeleteCustomer" :divided="!!canTransferCustomer" @click="handleDeleteCustomerConfirm">
                      <span class="flex items-center gap-2 text-red-500">
                        <span class="material-symbols-outlined text-sm">delete</span>
                        删除
                      </span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <!-- Stage Stepper (inside same card) -->
          <div class="mt-5 pt-4 border-t border-slate-100">
            <!-- <div class="flex items-center gap-2 mb-6">
              <span :class="sectionIconBoxClass" :style="getSectionIconStyle('customerStage')">
                <WkIcon name="stage" :size="15" />
              </span>
              <h3 class="text-sm font-bold text-slate-900">客户阶段</h3>
            </div> -->
            <div class="relative overflow-visible">
              <!-- Chevron segments (wrap layout, no scroll) -->
              <div class="relative flex flex-wrap items-stretch gap-y-2">
                <template v-for="(stage, idx) in stageFlow" :key="stage">
                  <el-popover
                    v-if="isTerminalStage(stage)"
                    :visible="showTerminalStageMenu"
                    trigger="click"
                    placement="bottom-end"
                    :offset="8"
                    :width="144"
                    :show-arrow="false"
                    popper-class="wk-stage-result-popover"
                    :disabled="!canChangeStage"
                    @update:visible="showTerminalStageMenu = $event"
                  >
                    <div class="wk-stage-result-popover__inner">
                      <div class="wk-stage-result-popover__head">
                        <p class="wk-stage-result-popover__title">选择推进结果</p>
                      </div>
                      <button
                        type="button"
                        class="wk-stage-result-popover__item wk-stage-result-popover__item--won"
                        :class="{ 'is-active': customer?.stage === 'closed' }"
                        @click="handleTerminalStageSelect('closed')"
                      >
                        <span class="material-symbols-outlined wk-stage-result-popover__icon">handshake</span>
                        已成交
                      </button>
                      <button
                        type="button"
                        class="wk-stage-result-popover__item wk-stage-result-popover__item--lost"
                        :class="{ 'is-active': customer?.stage === 'lost' }"
                        @click="handleTerminalStageSelect('lost')"
                      >
                        <span class="material-symbols-outlined wk-stage-result-popover__icon">block</span>
                        已流失
                      </button>
                      <button
                        v-if="customer?.stage === 'closed' || customer?.stage === 'lost'"
                        type="button"
                        class="wk-stage-result-popover__item wk-stage-result-popover__item--reopen"
                        @click="handleReopenOpportunity"
                      >
                        <span class="material-symbols-outlined wk-stage-result-popover__icon">settings_backup_restore</span>
                        重新开启商机
                      </button>
                    </div>
                    <template #reference>
                      <div
                        class="relative h-8 flex-none w-[180px] group"
                        :class="canChangeStage ? 'cursor-pointer' : 'cursor-default'"
                        :title="getStepperLabel(stage)"
                        :style="{ zIndex: getStepperZIndex(stage, idx) }"
                      >
                        <div
                          class="absolute inset-0 transition-all duration-300"
                          :class="getStepperSegmentBgClass(stage, idx)"
                          :style="{ clipPath: getStepperClipPath(idx) }"
                        ></div>
                        <div
                          class="absolute inset-0 opacity-0 transition-opacity duration-200 group-hover:opacity-100"
                          :class="getStepperHoverOverlayClass(stage, idx)"
                          :style="{ clipPath: getStepperClipPath(idx) }"
                        ></div>
                        <div class="relative z-10 flex h-full items-center justify-center overflow-hidden px-4 transition-transform duration-200 group-hover:scale-[1.02]">
                          <div class="flex min-w-0 max-w-full items-center justify-center gap-2">
                            <span
                              class="material-symbols-outlined shrink-0 text-[14px] font-bold transition-colors"
                              :class="getStepperLabelClass(stage, idx)"
                            >{{ getStepperStageIcon(stage) }}</span>
                            <span
                              class="block min-w-0 truncate text-[14px] font-bold tracking-wider transition-colors"
                              :class="getStepperLabelClass(stage, idx)"
                            >{{ getStepperLabel(stage) }}</span>
                            <span
                              v-if="canChangeStage"
                              class="material-symbols-outlined shrink-0 text-[16px] leading-none transition-all duration-200"
                              :class="[getStepperLabelClass(stage, idx), showTerminalStageMenu ? 'rotate-180' : 'rotate-0']"
                            >expand_more</span>
                          </div>
                        </div>
                      </div>
                    </template>
                  </el-popover>

                  <div
                    v-else
                    class="relative h-8 flex-none w-[180px] group"
                    :class="canChangeStage ? 'cursor-pointer' : 'cursor-default'"
                    @click="handleStageChange(stage)"
                    :title="getStepperLabel(stage)"
                    :style="{ zIndex: getStepperZIndex(stage, idx) }"
                  >
                    <div
                      class="absolute inset-0 transition-all duration-300"
                      :class="getStepperSegmentBgClass(stage, idx)"
                      :style="{ clipPath: getStepperClipPath(idx) }"
                    ></div>
                    <div
                      class="absolute inset-0 opacity-0 transition-opacity duration-200 group-hover:opacity-100"
                      :class="getStepperHoverOverlayClass(stage, idx)"
                      :style="{ clipPath: getStepperClipPath(idx) }"
                    ></div>
                    <div class="relative z-10 flex h-full items-center justify-center overflow-hidden px-4 transition-transform duration-200 group-hover:scale-[1.02]">
                      <div class="flex min-w-0 max-w-full items-center justify-center gap-2">
                        <span
                          class="material-symbols-outlined shrink-0 text-[14px] font-bold transition-colors"
                          :class="getStepperLabelClass(stage, idx)"
                        >{{ getStepperStageIcon(stage) }}</span>
                        <span
                          class="block min-w-0 truncate text-[14px] font-bold tracking-wider transition-colors"
                          :class="getStepperLabelClass(stage, idx)"
                        >{{ getStepperLabel(stage) }}</span>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3-Column Content -->
      <div class="flex-1 overflow-auto px-8 pb-8 pt-6">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-4">
          <!-- Left Column: Basic Info (col-span-3) -->
          <div class="lg:col-span-3 space-y-4">
            <section class="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
              <div class="mb-4 space-y-1">
                <!-- 上：图标 + 标题（整行展示，可换行）+ 刷新 -->
                <div class="flex items-center justify-between gap-2">
                  <div class="flex min-w-0 flex-1 items-center gap-2">
                    <span
                      :class="sectionIconBoxClass"
                      :style="getSectionIconStyle('basicInfo')"
                    >
                      <WkIcon name="ai" :size="15" />
                    </span>
                    <h3 class="min-w-0 flex-1 text-sm font-bold leading-snug text-slate-900 break-words">
                      {{ savedAiAnalysisTitle }}
                    </h3>
                  </div>
                  <button
                    v-if="canEditCustomer"
                    type="button"
                    class="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="generatingAiReport"
                    :title="generatingAiReport ? '生成中…' : '更新 AI 分析'"
                    :aria-label="generatingAiReport ? '生成中' : '更新 AI 分析'"
                    @click="handleGenerateReport"
                  >
                    <span
                      class="material-symbols-outlined text-[20px] leading-none"
                      :class="{ 'animate-spin': generatingAiReport }"
                    >refresh</span>
                  </button>
                </div>
                <!-- 下：更新时间 -->
                <div class="flex justify-end">
                  <p class="text-right text-xs leading-relaxed text-slate-400">
                    更新于：{{ aiAnalysisDisplayTime }}
                  </p>
                </div>
              </div>

              <div
                v-if="isAiAnalysisPending || isAiAnalysisFailed"
                class="mb-4 rounded-2xl border px-4 py-3"
                :class="isAiAnalysisFailed ? 'border-rose-200 bg-rose-50' : 'border-sky-200 bg-sky-50'"
              >
                <div class="flex items-start gap-3">
                  <div
                    class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-xl"
                    :class="isAiAnalysisFailed ? 'bg-rose-100 text-rose-500' : 'bg-sky-100 text-sky-500'"
                  >
                    <span
                      class="material-symbols-outlined text-[18px] leading-none"
                      :class="{ 'animate-spin': aiAnalysisStatus === 'running' }"
                    >{{ isAiAnalysisFailed ? 'error' : (aiAnalysisStatus === 'running' ? 'progress_activity' : 'schedule') }}</span>
                  </div>
                  <div class="min-w-0">
                    <p class="text-sm font-bold" :class="isAiAnalysisFailed ? 'text-rose-700' : 'text-sky-700'">
                      {{ aiAnalysisStatusLabel }}
                    </p>
                    <p class="mt-1 text-xs leading-5" :class="isAiAnalysisFailed ? 'text-rose-600' : 'text-sky-600'">
                      {{ aiAnalysisStatusDescription }}
                    </p>
                  </div>
                </div>
              </div>

              <AiParseInsightSidebar
                :result="savedAiParseResult"
                :show-tip="false"
                :empty-title="aiAnalysisEmptyTitle"
                :empty-description="aiAnalysisEmptyDescription"
              >
                <template v-if="canEditCustomer" #empty-extra>
                  <div class="mt-5 flex w-full justify-center px-1">
                    <button
                      type="button"
                      class="inline-flex max-w-full items-center justify-center gap-1.5 rounded-full bg-primary px-4 py-2 text-xs font-bold text-white shadow-[0_8px_22px_-6px_rgba(37,99,235,0.55)] transition-[filter,transform] hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:brightness-100 disabled:active:scale-100"
                      :disabled="generatingAiReport"
                      :title="generatingAiReport ? '生成中…' : '获取 AI 分析报告'"
                      @click="handleGenerateReport"
                    >
                      <span
                        v-if="generatingAiReport"
                        class="material-symbols-outlined shrink-0 text-[16px] leading-none animate-spin"
                      >progress_activity</span>
                      <WkIcon v-else name="ai" :size="14" class="shrink-0 text-white" />
                      <span class="text-[12px] truncate">获取 AI 分析报告</span>
                    </button>
                  </div>
                </template>
              </AiParseInsightSidebar>

              <section
                v-if="latestAiReport?.aiStatusDetection || latestAiReport?.aiInsight || customer.aiStatusDetection || customer.aiInsight"
                class="mt-4 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4"
              >
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-base text-primary">auto_awesome</span>
                  <h4 class="text-xs font-bold uppercase tracking-widest text-slate-500">{{ aiReportSummaryTitle }}</h4>
                </div>
                <div class="mt-3">
                  <span
                    v-if="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer.aiStatusDetection)"
                    class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium whitespace-nowrap"
                    :class="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer.aiStatusDetection)?.badgeClass"
                  >
                    <span class="size-1.5 rounded-full mr-1.5" :class="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer.aiStatusDetection)?.dotClass"></span>
                    {{ getAiStatusMeta(latestAiReport?.aiStatusDetection || customer.aiStatusDetection)?.label }}
                  </span>
                  <p
                    v-else
                    class="text-sm leading-6 text-slate-700 whitespace-pre-wrap break-words"
                  >{{ latestAiReport?.aiStatusDetection || customer.aiStatusDetection }}</p>
                </div>
                <p class="mt-3 text-sm leading-6 text-slate-700 whitespace-pre-wrap break-words">
                  {{ latestAiReport?.aiInsight || customer.aiInsight }}
                </p>
              </section>
            </section>
          </div>

          <!-- Center Column: Follow-ups Timeline (col-span-6)；标题图标与时间轴节点共用 28px 轨宽以便纵轴对齐 -->
          <div v-if="canViewFollowUps" class="lg:col-span-6 space-y-4">
            <div class="grid grid-cols-[1.75rem_minmax(0,1fr)] items-center gap-x-2">
              <span
                :class="sectionIconBoxClass"
                class="justify-self-center"
                :style="getSectionIconStyle('recentActivity')"
              >
                <span :class="sectionMaterialIconClass">history</span>
              </span>
              <div class="flex min-w-0 items-center justify-between gap-2">
                <h3 class="truncate text-lg font-bold text-slate-900">最近活动 - AI时间轴</h3>
                <div class="flex shrink-0 items-center gap-3">
                  <!-- <div class="flex bg-slate-100 p-1 rounded-lg">
                    <button class="px-3 py-1 text-xs font-bold rounded bg-white shadow-sm">全部</button>
                    <button class="px-3 py-1 text-xs font-medium text-slate-500">会议摘要</button>
                    <button class="px-3 py-1 text-xs font-medium text-slate-500">重要进展</button>
                  </div> -->
                  <!-- <button
                    v-if="canCreateFollowUps"
                    class="px-4 py-2 bg-primary/10 text-primary border border-primary/20 rounded-lg text-sm font-bold flex items-center gap-2 hover:bg-primary/20 transition-colors"
                    @click="handleOpenFollowUpDialog"
                  >
                    <span class="material-symbols-outlined text-sm">add</span>
                    添加跟进
                  </button> -->
                </div>
              </div>
            </div>

            <div v-if="followUps.length === 0 && !followUpLoading" class="bg-white border border-slate-200 rounded-xl p-12 text-center shadow-sm">
              <span class="material-symbols-outlined text-slate-300 text-4xl mb-3">event_note</span>
              <p class="text-sm text-slate-400">暂无跟进记录</p>
              <p class="text-xs text-slate-300 mt-1">点击上方按钮添加第一条跟进记录</p>
            </div>

            <div v-else v-loading="followUpLoading">
              <div
                v-for="(item, followUpIndex) in followUps"
                :key="item.followUpId"
                class="grid grid-cols-[1.75rem_minmax(0,1fr)] items-stretch gap-x-2"
              >
                <!-- Timeline rail：与标题区同宽 28px，节点与 section 图标同一纵轴 -->
                <div class="relative flex w-full shrink-0 flex-col items-center pt-1.5">
                  <div
                    v-if="followUps.length > 1"
                    class="absolute left-1/2 z-0 w-px -translate-x-1/2 bg-slate-200"
                    :class="followUpTimelineRailClass(followUpIndex)"
                  />
                  <div
                    class="relative z-10 size-3.5 shrink-0 rounded-full border-2 border-primary bg-white shadow-sm ring-2 ring-slate-50"
                    aria-hidden="true"
                  />
                </div>
                <!-- flex-col + 固定高度占位：16px 间距在同行 flex 高度内，避免子项 margin 不撑开行高导致卡片贴在一起；左侧轨道 stretch 后竖线仍连续 -->
                <div class="flex min-w-0 flex-1 flex-col">
                  <div v-if="false" class="bg-white border border-slate-200 rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                    <div class="flex items-center justify-between mb-3">
                      <div class="flex items-center gap-3">
                        <div class="size-8 rounded-lg flex items-center justify-center text-white shadow-sm bg-primary">
                          <span class="material-symbols-outlined text-sm">{{ getFollowUpIcon(item.type) }}</span>
                        </div>
                        <div>
                          <div class="flex items-center gap-2">
                            <h4 class="font-bold text-slate-900 text-sm leading-none">{{ getFollowUpTypeLabel(item.type) }}</h4>
                            <span class="text-xs font-bold px-1.5 py-0.5 bg-slate-50 text-slate-500 rounded border border-slate-100">{{ item.createUserName }}</span>
                          </div>
                        </div>
                      </div>
                      <div class="flex items-center gap-3">
                        <span class="text-xs text-slate-400 uppercase font-bold">{{ formatDateTime(item.followTime || item.createTime) }}</span>
                        <button
                          v-if="canEditFollowUps"
                          type="button"
                          class="text-slate-300 hover:text-primary transition-colors"
                          @click="handleEditFollowUp(item)"
                        >
                          <span class="material-symbols-outlined text-sm">edit</span>
                        </button>
                        <button
                          v-if="canDeleteFollowUps"
                          type="button"
                          class="text-slate-300 hover:text-red-500 transition-colors"
                          @click="confirmDeleteFollowUp(item.followUpId)"
                        >
                          <span class="material-symbols-outlined text-sm">delete</span>
                        </button>
                      </div>
                    </div>
                    <div v-if="item.nextFollowTime" class="flex items-center gap-2 mb-3 text-xs font-bold text-amber-600 bg-amber-50 px-2 py-1 rounded-lg w-fit">
                      <span class="material-symbols-outlined text-xs">event_repeat</span>
                      下次联系: {{ formatDateTime(item.nextFollowTime) }}
                    </div>
                    <p class="text-sm text-slate-600 leading-relaxed">{{ item.content }}</p>
                  </div>
                  <FollowUpCard
                    :item="item"
                    :can-edit="canEditFollowUps"
                    :can-delete="canDeleteFollowUps"
                    @edit="handleEditFollowUp"
                    @delete="confirmDeleteFollowUp"
                  />
                  <div v-if="followUpIndex < followUps.length - 1" class="h-3 shrink-0" aria-hidden="true" />
                </div>
              </div>
            </div>

            <div v-if="followUpTotal > followUpPageSize" class="flex justify-center">
              <el-pagination
                v-model:current-page="followUpPage"
                :page-size="followUpPageSize"
                :total="followUpTotal"
                layout="prev, pager, next"
                small
                @current-change="handleFollowUpPageChange"
              />
            </div>
          </div>

          <!-- Right Column: Related Modules (col-span-3) -->
          <div class="lg:col-span-3 space-y-4">
            <div class="flex items-center justify-between px-1">
              <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                <span :class="sectionIconBoxClass" :style="getSectionIconStyle('relatedBusiness')">
                  <span :class="sectionMaterialIconClass">hub</span>
                </span>
                关联业务模块
              </h3>
              <span class="text-xs font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full uppercase tracking-tighter">3个模块</span>
            </div>

            <!-- Contacts Module -->
            <section v-if="canViewContacts" class="bg-white border border-slate-200 rounded-2xl shadow-sm p-4" v-loading="contactLoading">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('relatedContacts')">
                    <span :class="sectionMaterialIconClass">group</span>
                  </span>
                  关联联系人
                  <span class="text-slate-400 font-normal">({{ contactTotal }})</span>
                </h4>
                <div v-if="canCreateContacts" class="flex items-center gap-2">
                  <button
                    type="button"
                    class="size-7 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all"
                    title="名片上传"
                    aria-label="名片上传"
                    @click="handleAddContactCardUpload"
                  >
                    <span class="material-symbols-outlined text-[18px] leading-none">contact_page</span>
                  </button>
                  <button
                    type="button"
                    class="size-7 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all"
                    title="新建联系人"
                    aria-label="新建联系人"
                    @click="handleAddContact"
                  >
                    <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">person_add</span>
                  </button>
                </div>
              </div>
              <div class="space-y-4">
                <div v-if="contacts.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-8 text-center">
                  <span class="material-symbols-outlined text-3xl leading-none text-slate-200">person_off</span>
                  <p class="mt-2 text-xs font-medium text-slate-400">暂无关联联系人</p>
                </div>
                <div
                  v-for="contact in contacts"
                  :key="contact.contactId"
                  class="group relative cursor-pointer overflow-hidden rounded-2xl border border-slate-200 bg-gradient-to-br from-white via-white to-slate-50/80 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-lg hover:shadow-slate-200/70"
                  @click="handleViewContact(contact)"
                >
                  <div class="pointer-events-none absolute inset-x-0 top-0 h-16 bg-gradient-to-b from-slate-50 to-transparent opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
                  <div class="relative flex flex-col gap-3">
                    <div class="flex items-start justify-between gap-3">
                      <div class="min-w-0 flex flex-1 items-center gap-3">
                        <div class="flex size-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-sm font-bold text-slate-600 transition-colors group-hover:border-primary/20 group-hover:bg-primary/10 group-hover:text-primary">
                          {{ contact.name?.trim()?.charAt(0) || '?' }}
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="flex min-w-0 items-center gap-1.5">
                            <h5 class="min-w-0 truncate text-sm font-bold leading-tight text-slate-900">{{ contact.name }}</h5>
                            <el-tooltip
                              v-if="!contact.isPrimary && canSetPrimaryContacts"
                              content="设为主要联系人"
                              placement="top"
                            >
                              <button
                                type="button"
                                class="flex size-7 shrink-0 items-center justify-center rounded-full bg-amber-50 text-amber-600 transition-all hover:bg-amber-100 opacity-0 pointer-events-none group-hover:opacity-100 group-hover:pointer-events-auto"
                                aria-label="设为主要联系人"
                                @click.stop="handleSetPrimary(contact.contactId)"
                              >
                                <span
                                  class="material-symbols-outlined text-[18px] leading-none"
                                  style="font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 24"
                                >star</span>
                              </button>
                            </el-tooltip>
                          </div>
                          <p class="mt-0.5 truncate text-[11px] font-medium text-slate-500">{{ contact.position || '-' }}</p>
                        </div>
                      </div>
                      <span
                        v-if="contact.isPrimary"
                        class="inline-flex shrink-0 items-center gap-1 rounded-lg bg-primary/10 px-2 py-1 text-[11px] font-bold text-primary"
                      >
                        <span class="material-symbols-outlined text-[12px] leading-none">stars</span>
                        主要联系人
                      </span>
                    </div>

                    <div class="h-px w-full bg-slate-100" />

                    <div class="space-y-2">
                      <div class="flex min-w-0 items-center gap-3 text-slate-600 transition-colors group-hover:text-slate-700">
                        <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-slate-400">call</span>
                        <span
                          :class="contact.phone ? 'font-mono text-xs font-medium tracking-tight text-slate-700' : 'text-xs font-medium text-slate-400'"
                        >{{ contact.phone || '-' }}</span>
                      </div>
                      <div class="flex min-w-0 items-center gap-3 text-slate-600 transition-colors group-hover:text-slate-700">
                        <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-slate-400">mail</span>
                        <span
                          class="min-w-0 truncate text-xs font-medium tracking-tight"
                          :class="contact.email ? 'text-slate-700' : 'text-slate-400'"
                        >{{ contact.email || '-' }}</span>
                      </div>
                      <div class="flex min-w-0 items-center gap-3 text-slate-600 transition-colors group-hover:text-slate-700">
                        <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-slate-400">chat</span>
                        <span
                          class="min-w-0 truncate text-xs font-medium tracking-tight"
                          :class="contact.wechat ? 'text-slate-700' : 'text-slate-400'"
                        >{{ contact.wechat ? `WeChat: ${contact.wechat}` : '-' }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="contactTotal > contactPageSize" class="pt-2 flex justify-center">
                  <el-pagination
                    v-model:current-page="contactPage"
                    :page-size="contactPageSize"
                    :total="contactTotal"
                    layout="prev, pager, next"
                    small
                    @current-change="handleContactPageChange"
                  />
                </div>
              </div>
            </section>

            <!-- Opportunities Module (hidden) -->
            <!-- <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">monetization_on</span>
                  关联商机
                </h4>
              </div>
              <div class="p-4">
                <p class="py-6 text-center text-xs text-slate-400">暂无关联商机</p>
              </div>
            </section> -->

            <!-- Contracts Module (hidden) -->
            <!-- <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">assignment</span>
                  合同管理
                </h4>
              </div>
              <div class="p-4">
                <p class="py-6 text-center text-xs text-slate-400">暂无合同记录</p>
              </div>
            </section> -->

            <!-- Tasks Module -->
            <section v-if="canViewTasks" class="bg-white border border-slate-200 rounded-2xl shadow-sm p-4">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('todoTasks')">
                    <WkIcon name="task" :size="15" />
                  </span>
                  待办任务
                </h4>
                <button
                  v-if="canCreateTasks"
                  type="button"
                  class="size-7 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all"
                  title="新建任务"
                  aria-label="新建任务"
                  @click="handleAddTask"
                >
                  <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add</span>
                </button>
              </div>
              <div class="space-y-4">
                <div v-if="!customer.tasks?.length" class="py-4 text-center">
                  <p class="text-xs text-slate-400">暂无待办任务</p>
                </div>
                <div
                  v-for="task in customer.tasks?.slice(0, 5)"
                  :key="task.taskId"
                  class="flex cursor-pointer items-start gap-3 rounded-xl border p-3 transition-all hover:shadow-sm"
                  :class="selectedCustomerTask?.taskId === task.taskId ? 'border-primary ring-1 ring-primary/20' : 'border-slate-100 bg-white'"
                  @click="handleViewCustomerTask(task)"
                >
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-xs font-bold text-slate-900">{{ task.title }}</p>
                    <div class="mt-1 flex items-center gap-2">
                      <span
                        class="text-xs font-bold uppercase"
                        :class="task.dueDate && isCustomerTaskOverdue(task) ? 'text-red-500' : 'text-slate-400'"
                      >
                        {{ task.dueDate ? formatDate(task.dueDate) : '无截止日期' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- Documents Module -->
            <section v-if="canViewKnowledge" class="bg-white border border-slate-200 rounded-2xl shadow-sm p-4">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('documentCenter')">
                    <WkIcon name="knowledge" :size="15" />
                  </span>
                  文档中心
                </h4>
              </div>
              <div>
                <p class="py-4 text-center text-xs text-slate-400">暂无文档</p>
              </div>
            </section>
          </div>
        </div>
      </div>
    </template>

    <!-- Add Tag Dialog -->
    <el-dialog v-model="showAddTagDialog" title="添加标签" :width="isMobile ? '90%' : '400px'" class="wk-dialog--flush">
      <el-input v-model="newTagName" placeholder="请输入标签名称" />
      <template #footer>
        <el-button @click="showAddTagDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAddTag">添加</el-button>
      </template>
    </el-dialog>

    <FollowUpUpsertDialog
      v-model="showAddFollowUpDialog"
      :customer-id="customer?.customerId || ''"
      :editing-follow-up="editingFollowUpForDialog"
      :submitting="submitting"
      @submit="handleFollowUpDialogSubmit"
    />

    <ContactUpsertDialog
      v-model="showAddContactDialog"
      :customer-id="customer?.customerId || ''"
      :contact="editingContact"
      :existing-primary-contact="primaryContact"
      :auto-open-ai-image-picker-token="contactAiImagePickerToken"
      @success="handleContactUpsertSuccess"
    />

    <ContactDetailDrawer
      v-model="showContactDetail"
      :contact="currentContact"
      @edit="handleEditContact"
      @delete="handleDeleteContact"
      @set-primary="handleSetPrimary"
    />

    <el-drawer
      v-model="showBasicInfoDrawer"
      direction="rtl"
      :size="isMobile ? '100%' : '420px'"
      :with-header="false"
      destroy-on-close
      class="customer-basic-info-drawer"
    >
      <div v-if="customer" class="flex h-full flex-col bg-white shadow-2xl">
        <div class="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/60 px-6 py-5">
          <div class="flex min-w-0 items-center gap-3">
            <div class="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <span class="material-symbols-outlined text-[20px] leading-none">info</span>
            </div>
            <div class="min-w-0">
              <h3 class="truncate text-base font-bold text-slate-900">{{ basicInfoDrawerTitle }}</h3>
              <p class="truncate text-xs text-slate-400">{{ basicInfoDrawerSubtitle }}</p>
            </div>
          </div>
          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-600"
            @click="showBasicInfoDrawer = false"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">close</span>
          </button>
        </div>

        <div class="min-h-0 flex-1 overflow-y-auto px-6 py-6">
          <div class="mb-6 rounded-2xl border border-slate-200 bg-slate-50 px-5 py-5">
            <div class="flex items-center gap-4">
              <div class="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-white text-2xl font-black text-slate-400">
                <img
                  v-if="customer.logoUrl"
                  :src="customer.logoUrl"
                  :alt="customer.companyName || 'company logo'"
                  class="size-full object-contain"
                />
                <span v-else>{{ customer.companyName?.charAt(0) || '?' }}</span>
              </div>
              <div class="min-w-0">
                <h4 class="truncate text-lg font-bold text-slate-900">{{ customer.companyName }}</h4>
                <div class="mt-2 flex flex-wrap items-center gap-2">
                  <span
                    class="px-2 py-0.5 text-xs font-bold rounded uppercase"
                    :class="getStageBadgeClass(customer.stage)"
                  >{{ getStageLabel(customer.stage) }}</span>
                  <span
                    v-if="customer.level"
                    class="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold"
                    :class="{
                      'bg-emerald-50 text-emerald-600': customer.level === 'A',
                      'bg-blue-50 text-blue-600': customer.level === 'B',
                      'bg-slate-100 text-slate-500': customer.level === 'C'
                    }"
                  >{{ customer.level }}级</span>
                </div>
              </div>
            </div>
          </div>

          <div class="space-y-5">
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">公司全称</p>
              <p class="text-sm text-slate-900 font-medium">{{ customer.companyName }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">所属行业</p>
              <p class="text-sm text-slate-900 font-medium">{{ customer.industry || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户来源</p>
              <p class="text-sm text-slate-900 font-medium">{{ customer.source || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">主要联系人</p>
              <p class="text-sm text-slate-900 font-medium">{{ primaryContact?.name || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">联系电话</p>
              <p class="text-sm text-slate-900 font-medium font-mono">{{ primaryContact?.phone || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">电子邮箱</p>
              <p class="text-sm text-slate-900 font-medium break-all">{{ primaryContact?.email || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户地址</p>
              <p class="text-sm text-slate-900 font-medium whitespace-pre-wrap break-words">{{ customer.address || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">公司网站</p>
              <a
                v-if="customer.website"
                :href="customer.website"
                target="_blank"
                rel="noopener noreferrer"
                class="text-sm font-medium text-primary hover:underline break-all"
              >
                {{ customer.website }}
              </a>
              <p v-else class="text-sm text-slate-900 font-medium">-</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">报价金额</p>
              <p class="text-sm text-slate-900 font-medium">{{ formatAmount(customer.quotation) }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">合同金额</p>
              <p class="text-sm text-slate-900 font-medium">{{ formatAmount(customer.contractAmount) }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">回款金额</p>
              <p class="text-sm text-slate-900 font-medium">{{ formatAmount(customer.revenue) }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">负责人</p>
              <p class="text-sm text-slate-900 font-medium">{{ customer.ownerName || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">创建人</p>
              <p class="text-sm text-slate-900 font-medium">{{ customer.createUserName || customer.createUserId || '-' }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">创建时间</p>
              <p class="text-sm text-slate-900 font-medium">{{ formatDateTime(customer.createTime) }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">更新时间</p>
              <p class="text-sm text-slate-900 font-medium">{{ formatDateTime(customer.updateTime) }}</p>
            </div>
            <div>
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">备注</p>
              <p class="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap break-words">{{ customer.remark || '暂无备注' }}</p>
            </div>
            <div v-for="field in customFields" :key="`drawer-${field.fieldId}`">
              <p class="text-xs font-bold text-slate-400 tracking-wider mb-1">{{ field.fieldLabel }}</p>
              <p class="text-sm text-slate-900 font-medium whitespace-pre-wrap break-words">{{ formatCustomFieldDisplayValue(field, customer.customFields?.[field.fieldName]) }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- Edit Customer Dialog -->
    <CustomerUpsertDialog
      v-model="showEditDialog"
      mode="edit"
      :customer="customer"
      @success="handleEditSuccess"
    />

    <el-dialog
      v-model="showAiReportDialog"
      title="AI 分析报告"
      width="680px"
      class="wk-dialog--flush"
      destroy-on-close
    >
      <div class="space-y-4">
        <section class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-4">
          <p class="text-xs font-bold uppercase tracking-wider text-slate-500">AI 状态探测</p>
          <div class="mt-3">
            <span
              v-if="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)"
              class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium whitespace-nowrap"
              :class="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)?.badgeClass"
            >
              <span class="size-1.5 rounded-full mr-1.5" :class="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)?.dotClass"></span>
              {{ getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)?.label }}
            </span>
            <p v-else class="text-sm leading-6 text-slate-700">{{ latestAiReport?.aiStatusDetection || customer?.aiStatusDetection || '暂无 AI 状态探测' }}</p>
          </div>
        </section>
        <section class="rounded-xl border border-slate-200 bg-white px-4 py-4">
          <p class="text-xs font-bold uppercase tracking-wider text-slate-500">AI 洞察</p>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700">{{ latestAiReport?.aiInsight || customer?.aiInsight || '暂无 AI 洞察' }}</p>
        </section>
      </div>
    </el-dialog>

    <!-- AI Follow-up Drawer -->
    <AiFollowUpDrawer
      v-model="showAiFollowUpDrawer"
      :customer="customer"
      @saved="handleAiFollowUpSaved"
    />

    <TaskDetailDrawer
      v-model="showCustomerTaskDetail"
      :task="selectedCustomerTask"
      :is-mobile="isMobile"
      :can-edit="canCreateTasks"
      @edit="handleCustomerTaskEditFromDetail"
      @mutated="refreshCustomerAfterTaskMutation"
    />

    <TaskEditDialog
      v-model="showTaskEditDialog"
      :is-mobile="isMobile"
      :editing-task="editingCustomerTask"
      :submitting="taskFormSubmitting"
      :ai-parsing="taskAiParsing"
      v-model:ai-parse-input="taskAiParseInput"
      :form-data="taskFormData"
      v-model:selected-participants="taskSelectedParticipants"
      :user-options="taskUserOptions"
      :user-search-loading="taskUserSearchLoading"
      :customer-options="taskCustomerOptions"
      :customer-search-loading="taskCustomerSearchLoading"
      :search-users="searchTaskUsers"
      :search-customers="searchTaskCustomers"
      @ai-parse="handleCustomerTaskAiParse"
      @submit="handleTaskDialogSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useTaskStore } from '@/stores/task'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { addCustomerTag, generateCustomerAiReport, queryCustomerList, removeCustomerTag, transferCustomer, updateCustomerStage } from '@/api/customer'
import { aiParseTask } from '@/api/task'
import type { CustomerAiParseVO } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import { addFollowUp, deleteFollowUp, queryFollowUpPageList, updateFollowUp } from '@/api/followup'
import { deleteContact, queryContactPageList, queryContactsByCustomer, setPrimaryContact } from '@/api/contact'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Task, TaskAddBO, TaskStatus } from '@/types/common'
import type { Contact, CustomerAiReportVO, CustomerTag, FollowUp, FollowUpAddBO, FollowUpUpdateBO } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { getCustomerAiStatusMeta } from '@/utils/customerAi'
import { formatCustomFieldValue as formatCustomFieldDisplayValue } from '@/utils/customFieldDisplay'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'
import FollowUpUpsertDialog from '@/components/customer/FollowUpUpsertDialog.vue'
import type { FollowUpUpsertSubmitPayload } from '@/components/customer/FollowUpUpsertDialog.vue'
import AiParseInsightSidebar from '@/components/crm/AiParseInsightSidebar.vue'
import FollowUpCard from '@/components/customer/FollowUpCard.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import ContactUpsertDialog from '@/views/contact/components/ContactUpsertDialog.vue'
import ContactDetailDrawer from '@/views/contact/components/ContactDetailDrawer.vue'
import TaskDetailDrawer from '@/views/task/components/TaskDetailDrawer.vue'
import TaskEditDialog from '@/views/task/components/TaskEditDialog.vue'
import { appEvents, APP_EVENT } from '@/utils/events'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()
const taskStore = useTaskStore()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const loading = ref(false)
const submitting = ref(false)
const showAddTagDialog = ref(false)
const showAddFollowUpDialog = ref(false)
const showAddContactDialog = ref(false)
const showContactDetail = ref(false)
const currentContact = ref<Contact | null>(null)
const editingContact = ref<Contact | null>(null)
const editingFollowUpForDialog = ref<FollowUp | null>(null)
const contactAiImagePickerToken = ref(0)
const showEditDialog = ref(false)
const showBasicInfoDrawer = ref(false)
const showAiFollowUpDrawer = ref(false)
const showAiReportDialog = ref(false)
const showTerminalStageMenu = ref(false)
const showTransferPopover = ref(false)
const headerMoreButtonRef = ref<HTMLElement | null>(null)
const newTagName = ref('')
const followUps = ref<FollowUp[]>([])
const followUpTotal = ref(0)
const followUpPage = ref(1)
const followUpPageSize = ref(5)
const followUpLoading = ref(false)
const contacts = ref<Contact[]>([])
const contactTotal = ref(0)
const contactPage = ref(1)
const contactPageSize = ref(5)
const contactLoading = ref(false)
const customFields = ref<CustomField[]>([])
const generatingAiReport = ref(false)
const latestAiReport = ref<CustomerAiReportVO | null>(null)
const ownerSearch = ref('')
const ownerListLoading = ref(false)
const userListLoaded = ref(false)

const selectedCustomerTask = ref<Task | null>(null)
const showCustomerTaskDetail = computed({
  get: () => !!selectedCustomerTask.value,
  set: (val: boolean) => {
    if (!val) selectedCustomerTask.value = null
  }
})
const showTaskEditDialog = ref(false)
const editingCustomerTask = ref<Task | null>(null)
const taskFormSubmitting = ref(false)
const taskAiParseInput = ref('')
const taskAiParsing = ref(false)
const taskCustomerOptions = ref<{ value: string; label: string }[]>([])
const taskCustomerSearchLoading = ref(false)
const taskUserOptions = ref<{ value: string; label: string }[]>([])
const taskUserSearchLoading = ref(false)
const taskSelectedParticipants = ref<string[]>([])
const taskFormData = reactive<TaskAddBO & { status?: TaskStatus; assignedToName?: string }>({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: undefined,
  status: undefined,
  taskType: '',
  customerId: '',
  assignedToName: ''
})

interface TransferUserOption {
  userId: string
  realname: string
  username?: string
  status?: number
}

const transferUserList = ref<TransferUserOption[]>([])

const sectionIconBoxClass = 'inline-flex size-7 shrink-0 items-center justify-center rounded-lg text-white shadow-[0_8px_18px_rgba(15,23,42,0.08)]'
const sectionMaterialIconClass = 'material-symbols-outlined text-[16px] leading-none'
const sectionIconBgColors = {
  customerStage: '#0052CC',
  basicInfo: '#5243AA',
  recentActivity: '#FF991F',
  relatedBusiness: '#00A3BF',
  relatedContacts: '#DE350B',
  todoTasks: '#00875A',
  documentCenter: '#0052CC',
} as const

const savedAiAnalysisTitle = '\u0041\u0049 \u667a\u80fd\u5206\u6790'
const emptyAiAnalysisTitle = '\u6682\u65e0 AI \u5206\u6790'
const emptyAiAnalysisDescription = '\u4fdd\u5b58\u5ba2\u6237\u540e\uff0c\u7cfb\u7edf\u4f1a\u81ea\u52a8\u89e6\u53d1 AI \u5206\u6790\uff0c\u7ed3\u679c\u4f1a\u5c55\u793a\u5728\u8fd9\u91cc\u3002'
const aiReportSummaryTitle = 'AI \u62a5\u544a\u6458\u8981'
const viewBasicInfoButtonText = '\u57fa\u672c\u4fe1\u606f'
const basicInfoDrawerTitle = '\u57fa\u672c\u4fe1\u606f'
const basicInfoDrawerSubtitle = '\u67e5\u770b\u5e76\u7ef4\u62a4\u5ba2\u6237\u8be6\u7ec6\u8d44\u6599'
const AI_ANALYSIS_POLL_INTERVAL_MS = 2500
const AI_ANALYSIS_POLL_MAX_ATTEMPTS = 24

type SectionIconKey = keyof typeof sectionIconBgColors

function getSectionIconStyle(key: SectionIconKey): { backgroundColor: string } {
  return { backgroundColor: sectionIconBgColors[key] }
}

function getAiStatusMeta(value: string | undefined | null) {
  return getCustomerAiStatusMeta(value)
}

/** Vertical rail segment: line starts at first dot center, ends at last dot center; full height between. */
function followUpTimelineRailClass(index: number): string {
  const n = followUps.value.length
  if (n <= 1) return ''
  const first = index === 0
  const last = index === n - 1
  if (first && !last) return 'top-[calc(0.375rem+0.4375rem)] bottom-0'
  if (!first && last) return 'top-0 h-[calc(0.375rem+0.4375rem)]'
  return 'top-0 bottom-0'
}

function isPrimaryContact(contact?: Pick<Contact, 'isPrimary'> | null): boolean {
  const value = contact?.isPrimary as boolean | number | string | undefined
  return value === true || value === 1 || value === '1'
}

function normalizeContact(contact: Contact): Contact {
  return {
    ...contact,
    contactId: String(contact.contactId),
    customerId: String(contact.customerId),
    isPrimary: isPrimaryContact(contact)
  }
}

function syncCurrentContact(customerId: string) {
  if (!currentContact.value) return

  const matchedContact = contacts.value.find(contact => contact.contactId === currentContact.value?.contactId)
  if (matchedContact) {
    currentContact.value = matchedContact
    return
  }

  if (currentContact.value.customerId === customerId) {
    currentContact.value = null
  }
}

async function maybeOpenContactFromQuery(customerId: string) {
  const openContactId = typeof route.query.openContactId === 'string' ? route.query.openContactId : ''
  if (!openContactId || !canViewContacts.value) return

  let matchedContact = contacts.value.find(contact => contact.contactId === openContactId)
  if (!matchedContact) {
    try {
      const allContacts = await queryContactsByCustomer(customerId)
      matchedContact = allContacts.map(contact => normalizeContact(contact)).find(contact => contact.contactId === openContactId)
    } catch (error) {
      console.error('Failed to load contact by route query:', error)
    }
  }

  if (!matchedContact) return

  currentContact.value = matchedContact
  showContactDetail.value = true

  const nextQuery = { ...route.query }
  delete nextQuery.openContactId
  await router.replace({ path: route.path, query: nextQuery })
}

async function refreshCustomerContext(customerId: string, options: { resetContacts?: boolean } = {}) {
  const tasks: Promise<any>[] = [customerStore.fetchCustomerDetail(customerId)]
  if (canViewContacts.value) {
    tasks.push(fetchContacts(customerId, options.resetContacts))
  }
  await Promise.all(tasks)
}

async function refreshFollowUpContext(customerId: string, options: { resetFollowUps?: boolean } = {}) {
  const tasks: Promise<any>[] = [customerStore.fetchCustomerDetail(customerId)]
  if (canViewFollowUps.value) {
    tasks.push(fetchFollowUps(customerId, options.resetFollowUps))
  }
  await Promise.all(tasks)
}

function applyPrimaryContactLocally(contactId: string) {
  const normalizedContactId = String(contactId)
  contacts.value = contacts.value.map(contact => ({
    ...contact,
    isPrimary: contact.contactId === normalizedContactId
  }))

  if (currentContact.value) {
    currentContact.value = {
      ...currentContact.value,
      isPrimary: currentContact.value.contactId === normalizedContactId
    }
  }
}

const primaryContact = computed(() => contacts.value.find(contact => isPrimaryContact(contact)) || contacts.value[0] || null)
const savedAiParseResult = computed<CustomerAiParseVO | null>(() => {
  const snapshot = customer.value?.aiParseSnapshot
  if (!snapshot) return null

  try {
    const parsed = JSON.parse(snapshot)
    return parsed && typeof parsed === 'object' ? parsed as CustomerAiParseVO : null
  } catch {
    return null
  }
})
const aiAnalysisStatus = computed(() => customer.value?.aiAnalysisStatus || '')
const isAiAnalysisPending = computed(() => aiAnalysisStatus.value === 'pending' || aiAnalysisStatus.value === 'running')
const isAiAnalysisFailed = computed(() => aiAnalysisStatus.value === 'failed')
const aiAnalysisDisplayTime = computed(() => formatDateTime(customer.value?.aiAnalysisRequestedAt || customer.value?.updateTime))
const aiAnalysisStatusLabel = computed(() => {
  switch (aiAnalysisStatus.value) {
    case 'pending':
      return 'AI 分析排队中'
    case 'running':
      return 'AI 分析生成中'
    case 'failed':
      return 'AI 分析未完成'
    case 'success':
      return 'AI 分析已更新'
    default:
      return ''
  }
})
const aiAnalysisStatusDescription = computed(() => {
  switch (aiAnalysisStatus.value) {
    case 'pending':
      return '客户已保存，系统正在排队处理最新一次 AI 分析任务。'
    case 'running':
      return '系统正在基于最新客户资料生成画像和推进建议，结果会自动刷新到当前页面。'
    case 'failed':
      return '本次自动分析未成功，可以重新保存客户资料，或点击右上角手动生成 AI 分析报告。'
    default:
      return ''
  }
})
const aiAnalysisEmptyTitle = computed(() => {
  if (isAiAnalysisPending.value) return 'AI 分析生成中'
  if (isAiAnalysisFailed.value) return 'AI 分析未完成'
  return emptyAiAnalysisTitle
})
const aiAnalysisEmptyDescription = computed(() => {
  if (isAiAnalysisPending.value) {
    return '客户资料已保存，系统正在后台生成 AI 客户画像和推进建议，请稍候自动刷新。'
  }
  if (isAiAnalysisFailed.value) {
    return '最近一次自动分析没有成功生成结果。你可以重新保存客户，或手动点击“生成 AI 分析报告”。'
  }
  return emptyAiAnalysisDescription
})

let aiAnalysisPollTimer: ReturnType<typeof setTimeout> | null = null
let aiAnalysisPollAttempts = 0

function clearAiAnalysisPolling(resetAttempts = true) {
  if (aiAnalysisPollTimer) {
    clearTimeout(aiAnalysisPollTimer)
    aiAnalysisPollTimer = null
  }
  if (resetAttempts) {
    aiAnalysisPollAttempts = 0
  }
}

function scheduleAiAnalysisPolling(customerId?: string, resetAttempts = false) {
  if (!customerId) return
  if (!isAiAnalysisPending.value) {
    clearAiAnalysisPolling(resetAttempts)
    return
  }
  if (resetAttempts) {
    clearAiAnalysisPolling()
  }
  if (aiAnalysisPollTimer || aiAnalysisPollAttempts >= AI_ANALYSIS_POLL_MAX_ATTEMPTS) {
    return
  }

  aiAnalysisPollTimer = setTimeout(async () => {
    aiAnalysisPollTimer = null
    if (String(route.params.id || '') !== customerId) {
      clearAiAnalysisPolling()
      return
    }

    aiAnalysisPollAttempts += 1
    try {
      await customerStore.fetchCustomerDetail(customerId)
    } catch (error) {
      console.error('Failed to poll customer ai analysis status:', error)
    }

    if (String(route.params.id || '') !== customerId) {
      clearAiAnalysisPolling()
      return
    }

    if (isAiAnalysisPending.value && aiAnalysisPollAttempts < AI_ANALYSIS_POLL_MAX_ATTEMPTS) {
      scheduleAiAnalysisPolling(customerId)
      return
    }

    clearAiAnalysisPolling()
  }, AI_ANALYSIS_POLL_INTERVAL_MS)
}


const customer = computed(() => customerStore.currentCustomer)
const canEditCustomer = computed(() => userStore.hasPermission('customer:edit'))
const canTransferCustomer = computed(() => userStore.hasPermission('customer:transfer'))
const canDeleteCustomer = computed(() => userStore.hasPermission('customer:delete'))
const canChangeStage = computed(() => userStore.hasPermission('customer:change_stage'))
const canEditCustomerTags = computed(() => userStore.hasPermission('customer:edit'))
const canViewContacts = computed(() => userStore.hasPermission('contact:view'))
const canCreateContacts = computed(() => userStore.hasPermission('contact:create'))
const canEditContacts = computed(() => userStore.hasPermission('contact:edit'))
const canDeleteContacts = computed(() => userStore.hasPermission('contact:delete'))
const canSetPrimaryContacts = computed(() => userStore.hasPermission('contact:set_primary'))
const canViewFollowUps = computed(() => userStore.hasPermission('followup:view'))
const canCreateFollowUps = computed(() => userStore.hasPermission('followup:create'))
const canEditFollowUps = computed(() => userStore.hasPermission('followup:edit'))
const canDeleteFollowUps = computed(() => userStore.hasPermission('followup:delete'))
const canViewTasks = computed(() => userStore.hasPermission('task:view'))
const canCreateTasks = computed(() => userStore.hasPermission('task:create'))
const canViewKnowledge = computed(() => userStore.hasPermission('knowledge:view'))
const filteredTransferUserList = computed(() => {
  const keyword = ownerSearch.value.trim().toLowerCase()
  if (!keyword) return transferUserList.value
  return transferUserList.value.filter(user =>
    [user.realname, user.username]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(keyword))
  )
})

watch(showAddFollowUpDialog, (visible) => {
  if (!visible) {
    editingFollowUpForDialog.value = null
  }
})

onMounted(async () => {
  const customerId = route.params.id as string
  if (customerId) {
    loading.value = true

    const fetchTasks: Promise<any>[] = [
      customerStore.fetchCustomerDetail(customerId).catch(err => {
        console.error('Failed to fetch customer detail:', err)
      }),
      getEnabledFieldsByEntity('customer').then(data => {
        customFields.value = data.filter(field => field.fieldSource !== 'system')
      }).catch(err => {
        console.error('Failed to fetch custom fields:', err)
        customFields.value = []
      })
    ]

    if (canViewFollowUps.value) {
      fetchTasks.push(fetchFollowUps(customerId))
    } else {
      followUps.value = []
      followUpTotal.value = 0
    }

    if (canViewContacts.value) {
      fetchTasks.push(fetchContacts(customerId))
    } else {
      contacts.value = []
      contactTotal.value = 0
    }

    await Promise.all(fetchTasks)
    loading.value = false
  }
})

watch(
  () => [customer.value?.customerId || '', customer.value?.aiAnalysisStatus || ''] as const,
  ([customerId, status], previousValue) => {
    const previousCustomerId = previousValue?.[0] || ''
    const previousStatus = previousValue?.[1] || ''
    if (!customerId) {
      clearAiAnalysisPolling()
      return
    }
    if (customerId !== previousCustomerId) {
      clearAiAnalysisPolling()
    }
    if (status === 'pending' || status === 'running') {
      scheduleAiAnalysisPolling(
        customerId,
        customerId !== previousCustomerId || (previousStatus !== 'pending' && previousStatus !== 'running')
      )
      return
    }
    clearAiAnalysisPolling()
  }
)

onBeforeUnmount(() => {
  clearAiAnalysisPolling()
})

async function fetchFollowUps(customerId: string, reset = false) {
  if (!canViewFollowUps.value) {
    followUps.value = []
    followUpTotal.value = 0
    return
  }
  if (reset) followUpPage.value = 1
  followUpLoading.value = true
  try {
    const result = await queryFollowUpPageList({ customerId, page: followUpPage.value, limit: followUpPageSize.value })
    followUps.value = result.list
    followUpTotal.value = result.totalRow
  } catch (err) {
    console.error('Failed to fetch follow-ups:', err)
    followUps.value = []
    followUpTotal.value = 0
  } finally {
    followUpLoading.value = false
  }
}

function handleFollowUpPageChange(page: number) {
  followUpPage.value = page
  if (customer.value) fetchFollowUps(customer.value.customerId)
}

async function fetchContacts(customerId: string, reset = false) {
  if (!canViewContacts.value) {
    contacts.value = []
    contactTotal.value = 0
    return
  }
  if (reset) contactPage.value = 1
  contactLoading.value = true
  try {
    const result = await queryContactPageList({ customerId, page: contactPage.value, limit: contactPageSize.value })
    contacts.value = result.list.map(contact => normalizeContact(contact))
    contactTotal.value = result.totalRow
  } catch (err) {
    console.error('Failed to fetch contacts:', err)
    contacts.value = []
    contactTotal.value = 0
  } finally {
    syncCurrentContact(customerId)
    await maybeOpenContactFromQuery(customerId)
    contactLoading.value = false
  }
}

function handleContactPageChange(page: number) {
  contactPage.value = page
  if (customer.value) fetchContacts(customer.value.customerId)
}

function handleEdit() {
  if (!canEditCustomer.value) return
  showEditDialog.value = true
}

async function loadTransferUserList() {
  if (userListLoaded.value || ownerListLoading.value) return
  ownerListLoading.value = true
  try {
    const res = await queryUserList({ limit: 500 })
    const list = res?.list || res?.records || []
    transferUserList.value = list
      .filter((user: any) => user.status === 1)
      .map((user: any) => ({
        userId: String(user.userId),
        realname: user.realname,
        username: user.username,
        status: user.status
      }))
    userListLoaded.value = true
  } catch (err) {
    console.error('Failed to load transfer users:', err)
  } finally {
    ownerListLoading.value = false
  }
}

function handleTransferPopoverShow() {
  ownerSearch.value = ''
  loadTransferUserList()
}

function handleTransferPopoverHide() {
  ownerSearch.value = ''
}

function onHeaderMoreDropdownVisible(visible: boolean) {
  if (visible) {
    showTransferPopover.value = false
  }
}

function openTransferPopoverFromMenu() {
  nextTick(() => {
    showTransferPopover.value = true
  })
}

async function handleTransferOwner(user: TransferUserOption) {
  if (!canTransferCustomer.value) return
  if (!customer.value) return
  if (String(user.userId) === String(customer.value.ownerId)) return

  try {
    await ElMessageBox.confirm(
      `确定将客户「${customer.value.companyName}」的负责人变更为「${user.realname}」吗？`,
      '转移负责人',
      {
        type: 'warning',
        confirmButtonText: '确认转移',
        cancelButtonText: '取消'
      }
    )

    await transferCustomer([customer.value.customerId], user.userId)
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
    showTransferPopover.value = false
    ElMessage.success('负责人转移成功')
  } catch {
    // Cancelled or error handled by interceptor
  }
}

async function handleEditSuccess(payload: { mode: 'create' | 'edit'; customerId?: string }) {
  if (payload.mode !== 'edit') return
  if (customer.value?.customerId) {
    await customerStore.fetchCustomerDetail(customer.value.customerId)
  }
}

async function handleDeleteCustomer() {
  if (!canDeleteCustomer.value) return
  if (!customer.value) return
  try {
    await customerStore.removeCustomer(customer.value.customerId)
    ElMessage.success('客户已删除')
    router.push('/customer')
  } catch {
    // Error handled by interceptor
  }
}

async function handleDeleteCustomerConfirm() {
  if (!customer.value) return
  try {
    await ElMessageBox.confirm(
      '确定要删除此客户吗？删除后不可恢复。',
      '提示',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await handleDeleteCustomer()
  } catch {
    // cancelled
  }
}

function handleAiFollowUp() {
  if (!canCreateFollowUps.value) return
  showAiFollowUpDrawer.value = true
}

async function handleAiFollowUpSaved() {
  if (!customer.value) return
  await refreshFollowUpContext(customer.value.customerId, { resetFollowUps: true })
}

function handleOpenFollowUpDialog() {
  if (!canCreateFollowUps.value) return
  editingFollowUpForDialog.value = null
  showAddFollowUpDialog.value = true
}

function handleEditFollowUp(followUp: FollowUp) {
  if (!canEditFollowUps.value) return
  editingFollowUpForDialog.value = followUp
  showAddFollowUpDialog.value = true
}

async function handleGenerateReport() {
  if (!canEditCustomer.value) return
  if (!customer.value || generatingAiReport.value) return

  generatingAiReport.value = true
  try {
    latestAiReport.value = await generateCustomerAiReport(customer.value.customerId)
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
    showAiReportDialog.value = true
    ElMessage.success('AI 分析报告已生成并保存')
  } catch {
    // Error handled by interceptor
  } finally {
    generatingAiReport.value = false
  }
}

function handleAddContact() {
  if (!canCreateContacts.value) return
  if (customer.value) {
    editingContact.value = null
    showAddContactDialog.value = true
  }
}

function handleAddContactCardUpload() {
  if (!canCreateContacts.value) return
  if (customer.value) {
    editingContact.value = null
    contactAiImagePickerToken.value += 1
    showAddContactDialog.value = true
  }
}

function handleViewContact(contact: Contact) {
  if (!canViewContacts.value) return
  currentContact.value = contact
  showContactDetail.value = true
}

function handleEditContact(contact: Contact) {
  if (!canEditContacts.value) return
  editingContact.value = contact
  showAddContactDialog.value = true
}

async function handleContactUpsertSuccess(payload: { mode: 'create' | 'edit' }) {
  if (!customer.value) return
  await refreshCustomerContext(customer.value.customerId, {
    resetContacts: payload.mode === 'create'
  })
}

// async function confirmDeleteContact(contactId: string) {
//   if (!canDeleteContacts.value) return
//   try {
//     await ElMessageBox.confirm('确定删除该联系人吗？', '提示', {
//       type: 'warning',
//       confirmButtonText: '删除',
//       cancelButtonText: '取消',
//       confirmButtonClass: 'el-button--danger'
//     })
//   } catch {
//     return
//   }
//   await handleDeleteContact(contactId)
// }

async function handleDeleteContact(contactId: string) {
  if (!canDeleteContacts.value) return
  if (!customer.value) return
  try {
    await deleteContact(contactId)
    await refreshCustomerContext(customer.value.customerId)
    ElMessage.success('联系人已删除')
  } catch { /* Error handled */ }
}

async function handleSetPrimary(contactId: string) {
  if (!canSetPrimaryContacts.value) return
  if (!customer.value) return
  const customerId = customer.value.customerId
  const previousContacts = contacts.value.map(contact => ({ ...contact }))
  const previousCurrentContact = currentContact.value ? { ...currentContact.value } : null

  applyPrimaryContactLocally(contactId)
  try {
    await setPrimaryContact(contactId)
    await refreshCustomerContext(customerId)
    ElMessage.success('已设为主联系人')
  } catch {
    contacts.value = previousContacts
    currentContact.value = previousCurrentContact
  }
}

function formatTaskDateTimeLocal(dateStr: string): string {
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function refreshCustomerAfterTaskMutation() {
  if (!customer.value) return
  await customerStore.fetchCustomerDetail(customer.value.customerId)
  const id = selectedCustomerTask.value?.taskId
  if (id && customer.value.tasks) {
    selectedCustomerTask.value = customer.value.tasks.find((t: Task) => t.taskId === id) || null
  }
}

function isCustomerTaskOverdue(task: Task): boolean {
  if (!task.dueDate || task.status === 'COMPLETED') return false
  return new Date(task.dueDate) < new Date()
}

function resetCustomerTaskForm() {
  editingCustomerTask.value = null
  taskAiParseInput.value = ''
  taskSelectedParticipants.value = []
  taskCustomerOptions.value = []
  taskUserOptions.value = []
  Object.assign(taskFormData, {
    title: '',
    description: '',
    priority: 'MEDIUM',
    dueDate: undefined,
    status: undefined,
    taskType: '',
    customerId: '',
    assignedToName: ''
  })
  const c = customer.value
  if (c) {
    taskFormData.customerId = String(c.customerId)
    taskCustomerOptions.value = [{ value: String(c.customerId), label: c.companyName || '' }]
  }
}

async function searchTaskCustomers(query: string) {
  if (!query.trim()) {
    taskCustomerOptions.value = []
    return
  }
  taskCustomerSearchLoading.value = true
  try {
    const res = await queryCustomerList({ keyword: query, page: 1, limit: 20 })
    taskCustomerOptions.value = (res.list || []).map((c: { customerId: string; companyName?: string }) => ({
      value: String(c.customerId),
      label: c.companyName || ''
    }))
  } catch (e) {
    console.warn('客户搜索失败:', e)
    taskCustomerOptions.value = []
  } finally {
    taskCustomerSearchLoading.value = false
  }
}

async function searchTaskUsers(query: string) {
  if (!query.trim()) {
    taskUserOptions.value = []
    return
  }
  taskUserSearchLoading.value = true
  try {
    const res = await queryUserList({ search: query })
    taskUserOptions.value = (res.list || []).map((u: { realname?: string; username?: string }) => ({
      value: u.realname || u.username || '',
      label: u.realname || u.username || ''
    }))
  } catch (e) {
    console.warn('用户搜索失败:', e)
    taskUserOptions.value = []
  } finally {
    taskUserSearchLoading.value = false
  }
}

async function handleCustomerTaskAiParse() {
  if (!taskAiParseInput.value.trim()) return
  taskAiParsing.value = true
  try {
    const result = await aiParseTask(taskAiParseInput.value)
    if (result.title) taskFormData.title = result.title
    if (result.dueDate) taskFormData.dueDate = result.dueDate
    if (result.priority) taskFormData.priority = result.priority.toUpperCase() as Task['priority']
    if (result.taskType) taskFormData.taskType = result.taskType
    if (result.customerName) {
      const res = await queryCustomerList({ keyword: result.customerName, page: 1, limit: 5 })
      const list = res.list || []
      if (list.length > 0) {
        taskCustomerOptions.value = list.map((c: { customerId: string; companyName?: string }) => ({
          value: String(c.customerId),
          label: c.companyName || ''
        }))
        taskFormData.customerId = String(list[0].customerId)
      }
    }
    if (result.participantNames) {
      taskSelectedParticipants.value = result.participantNames.split(/[,，]\s*/).filter(Boolean)
      taskUserOptions.value = taskSelectedParticipants.value.map(name => ({ value: name, label: name }))
    }
    if (result.description) taskFormData.description = result.description
    if (result.assignedToName) taskFormData.assignedToName = result.assignedToName
    ElMessage.success('AI 解析完成，请确认并补充信息')
  } catch (error) {
    console.error('AI parse task failed:', error)
  } finally {
    taskAiParsing.value = false
  }
}

function handleViewCustomerTask(task: Task) {
  if (!canViewTasks.value) return
  selectedCustomerTask.value = task
}

function handleAddTask() {
  if (!canCreateTasks.value) return
  if (!customer.value) return
  resetCustomerTaskForm()
  showTaskEditDialog.value = true
}

function handleEditCustomerTask(task: Task) {
  if (!canCreateTasks.value) return
  editingCustomerTask.value = task
  Object.assign(taskFormData, {
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    dueDate: task.dueDate ? formatTaskDateTimeLocal(task.dueDate) : undefined,
    status: task.status,
    taskType: task.taskType || '',
    customerId: task.customerId || '',
    assignedToName: task.assignedToName || ''
  })
  if (task.customerId && task.customerName) {
    taskCustomerOptions.value = [{ value: String(task.customerId), label: task.customerName }]
  }
  taskSelectedParticipants.value = task.participantNames ? task.participantNames.split(/[,，]\s*/).filter(Boolean) : []
  taskUserOptions.value = taskSelectedParticipants.value.map(name => ({ value: name, label: name }))
  showTaskEditDialog.value = true
}

function handleCustomerTaskEditFromDetail(task: Task) {
  handleEditCustomerTask(task)
  if (isMobile.value) selectedCustomerTask.value = null
}

async function handleTaskDialogSubmit() {
  if (!taskFormData.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  if (!taskFormData.dueDate) {
    ElMessage.warning('请选择截止时间')
    return
  }
  taskFormSubmitting.value = true
  try {
    const submitData = {
      title: taskFormData.title,
      description: taskFormData.description,
      priority: taskFormData.priority,
      dueDate: taskFormData.dueDate,
      taskType: taskFormData.taskType,
      participantNames: taskSelectedParticipants.value.join(', '),
      customerId: taskFormData.customerId || undefined
    }
    if (editingCustomerTask.value) {
      await taskStore.editTask({ ...submitData, taskId: editingCustomerTask.value.taskId, status: taskFormData.status })
      ElMessage.success('更新成功')
    } else {
      await taskStore.createTask(submitData)
      ElMessage.success('创建成功')
    }
    showTaskEditDialog.value = false
    resetCustomerTaskForm()
    await refreshCustomerAfterTaskMutation()
  } finally {
    taskFormSubmitting.value = false
  }
}

async function handleAddTag() {
  if (!canEditCustomerTags.value) return
  if (!newTagName.value.trim() || !customer.value) return
  submitting.value = true
  try {
    await addCustomerTag(customer.value.customerId, newTagName.value.trim())
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    showAddTagDialog.value = false
    newTagName.value = ''
    ElMessage.success('标签添加成功')
  } finally {
    submitting.value = false
  }
}

async function handleRemoveTag(tag: CustomerTag) {
  if (!canEditCustomerTags.value) return
  if (!customer.value) return
  try {
    await removeCustomerTag(customer.value.customerId, tag.tagId)
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    ElMessage.success('标签已删除')
  } catch { /* Error handled */ }
}

async function handleFollowUpDialogSubmit(payload: FollowUpUpsertSubmitPayload) {
  const isEdit = payload.mode === 'edit'
  if (isEdit ? !canEditFollowUps.value : !canCreateFollowUps.value) return
  submitting.value = true
  try {
    const body = {
      type: payload.type,
      content: payload.content,
      followTime: payload.followTime,
      nextFollowTime: payload.nextFollowTime
    }
    if (isEdit) {
      await updateFollowUp({
        followUpId: payload.followUpId!,
        ...body
      } as FollowUpUpdateBO)
    } else {
      await addFollowUp({
        customerId: payload.customerId,
        ...body
      } as FollowUpAddBO)
    }
    const successMessage = isEdit ? '跟进记录已更新' : '跟进记录添加成功'
    await refreshFollowUpContext(payload.customerId, { resetFollowUps: !isEdit })
    showAddFollowUpDialog.value = false
    ElMessage.success(successMessage)
  } catch { /* Error handled */ } finally {
    submitting.value = false
  }
}

async function confirmDeleteFollowUp(followUpId: string) {
  if (!canDeleteFollowUps.value) return
  try {
    await ElMessageBox.confirm('确定删除这条跟进记录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  await handleDeleteFollowUp(followUpId)
}

async function handleDeleteFollowUp(followUpId: string) {
  if (!canDeleteFollowUps.value) return
  try {
    if (followUps.value.length === 1 && followUpPage.value > 1) {
      followUpPage.value -= 1
    }
    await deleteFollowUp(followUpId)
    if (customer.value) await refreshFollowUpContext(customer.value.customerId)
    ElMessage.success('跟进记录已删除')
  } catch { /* Error handled */ }
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

function getStageLabel(stage: string): string {
  const labels: Record<string, string> = {
    lead: '线索', qualified: '已验证', proposal: '方案',
    negotiation: '谈判', closed: '成交', lost: '流失'
  }
  return labels[stage] || stage
}

function getStageIndex(stage: string): number {
  const stageIndexMap: Record<string, number> = {
    lead: 0,
    qualified: 1,
    proposal: 2,
    negotiation: 3,
    closed: 4,
    lost: 4
  }
  return stageIndexMap[stage] ?? -1
}

const stageFlow = ['lead', 'qualified', 'proposal', 'negotiation', 'closed']
const stageOptions = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
]
const STEPPER_SEGMENT_WIDTH = 180
const STEPPER_SEGMENT_HEIGHT = 32
const STEPPER_CHEVRON_SIZE = 12
const STEPPER_END_RADIUS = STEPPER_SEGMENT_HEIGHT / 2

function getStageLabelFull(stage: string): string {
  return stageOptions.find(s => s.value === stage)?.label || stage
}

function getStepperClipPath(idx: number): string {
  const isFirstNode = idx === 0
  const isLastNode = idx === stageFlow.length - 1

  // Keep the existing chevron join, but soften the outer ends into half-round caps.
  if (isFirstNode) {
    return `path('M ${STEPPER_END_RADIUS} 0 H ${STEPPER_SEGMENT_WIDTH - STEPPER_CHEVRON_SIZE} L ${STEPPER_SEGMENT_WIDTH} ${STEPPER_SEGMENT_HEIGHT / 2} L ${STEPPER_SEGMENT_WIDTH - STEPPER_CHEVRON_SIZE} ${STEPPER_SEGMENT_HEIGHT} H ${STEPPER_END_RADIUS} Q 0 ${STEPPER_SEGMENT_HEIGHT} 0 ${STEPPER_SEGMENT_HEIGHT / 2} Q 0 0 ${STEPPER_END_RADIUS} 0 Z')`
  }
  if (isLastNode) {
    return `path('M 0 0 H ${STEPPER_SEGMENT_WIDTH - STEPPER_END_RADIUS} Q ${STEPPER_SEGMENT_WIDTH} 0 ${STEPPER_SEGMENT_WIDTH} ${STEPPER_SEGMENT_HEIGHT / 2} Q ${STEPPER_SEGMENT_WIDTH} ${STEPPER_SEGMENT_HEIGHT} ${STEPPER_SEGMENT_WIDTH - STEPPER_END_RADIUS} ${STEPPER_SEGMENT_HEIGHT} H 0 L ${STEPPER_CHEVRON_SIZE} ${STEPPER_SEGMENT_HEIGHT / 2} Z')`
  }

  return 'polygon(0% 0%, calc(100% - 12px) 0%, 100% 50%, calc(100% - 12px) 100%, 0% 100%, 12px 50%)'
}

function getStepperSegmentBgClass(stage: string, idx: number): string {
  const state = getStepperVisualState(stage, idx)
  const classes: Record<string, string> = {
    completed: 'bg-gradient-to-r from-[#22c55e] to-[#16c458] shadow-[inset_0_1px_0_rgba(255,255,255,0.18)]',
    current: 'bg-gradient-to-r from-[#113f98] to-[#194fa8] shadow-[0_8px_20px_rgba(17,63,152,0.18)]',
    closed: 'bg-gradient-to-r from-[#22c55e] to-[#16c458] shadow-[0_8px_20px_rgba(34,197,94,0.16)]',
    lost: 'bg-gradient-to-r from-[#f43f5e] to-[#e11d48] shadow-[0_8px_20px_rgba(244,63,94,0.18)]',
    pending: 'bg-[#d9d9d9]'
  }
  return classes[state]
}

function getStepperStageIcon(stage: string): string {
  if (isTerminalStage(stage)) {
    if (customer.value?.stage === 'closed') return 'handshake'
    if (customer.value?.stage === 'lost') return 'block'
    return 'flag'
  }

  const icons: Record<string, string> = {
    lead: 'person_search',
    qualified: 'verified',
    proposal: 'description',
    negotiation: 'forum'
  }
  return icons[stage] || 'lens'
}

function getStepperLabel(stage: string): string {
  if (isTerminalStage(stage)) {
    const cs = customer.value?.stage
    if (cs === 'closed') return '已成交'
    if (cs === 'lost') return '已流失'
    return '推进结单'
  }
  return getStageLabelFull(stage)
}

function isTerminalStage(stage: string): boolean {
  return stage === 'closed'
}

function getStepperZIndex(stage: string, idx: number): number {
  const cs = customer.value?.stage
  if (isTerminalStage(stage) && (cs === 'closed' || cs === 'lost')) return 10
  if (cs === stage) return 10
  return 5 - idx
}

function getStepperLabelClass(stage: string, idx: number): string {
  const state = getStepperVisualState(stage, idx)
  return state === 'pending' ? 'text-slate-600' : 'text-white'
}

function getStepperHoverOverlayClass(stage: string, idx: number): string {
  const state = getStepperVisualState(stage, idx)
  return state === 'pending' ? 'bg-white/35' : 'bg-white/10'
}

function getStepperVisualState(stage: string, idx: number): 'completed' | 'current' | 'closed' | 'lost' | 'pending' {
  const cs = customer.value?.stage
  if (!cs) return 'pending'

  if (isTerminalStage(stage) && cs === 'lost') return 'lost'
  if (isTerminalStage(stage) && cs === 'closed') return 'closed'

  const isCompleted = getStageIndex(cs) > idx
  if (cs === stage) return 'current'
  if (isCompleted) return 'completed'
  return 'pending'
}

async function handleTerminalStageSelect(stage: 'closed' | 'lost') {
  showTerminalStageMenu.value = false
  await handleStageChange(stage)
}

async function handleReopenOpportunity() {
  showTerminalStageMenu.value = false
  await handleStageChange('negotiation')
}

async function handleStageChange(newStage: string) {
  showTerminalStageMenu.value = false
  if (!canChangeStage.value) return
  if (!customer.value || customer.value.stage === newStage) return
  try {
    await updateCustomerStage(customer.value.customerId, newStage)
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    ElMessage.success(`商机阶段已更新为「${getStageLabelFull(newStage)}」`)
  } catch { /* Error handled */ }
}

function getFollowUpTypeLabel(type: string): string {
  const labels: Record<string, string> = { call: '电话', meeting: '会议', email: '邮件', visit: '拜访', other: '其他' }
  return labels[type] || type
}

function getFollowUpIcon(type: string): string {
  const icons: Record<string, string> = { call: 'call', meeting: 'groups', email: 'mail', visit: 'location_on', other: 'edit_note' }
  return icons[type] || 'edit_note'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatAmount(value?: number | null): string {
  if (value === null || value === undefined) return '-'
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '-'
  return `¥ ${amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  })}`
}

/* Legacy helper kept only as a reference during the custom field display refactor.
function formatCustomFieldValue(field: CustomField, value: any): string {
  if (value === null || value === undefined || value === '') return '-'
  switch (field.fieldType) {
    case 'checkbox': return value ? '是' : '否'
    case 'select': {
      const option = field.options?.find(opt => opt.value === value)
      return option?.label || String(value)
    }
    case 'multiselect':
      if (Array.isArray(value)) {
        return value.map(v => field.options?.find(opt => opt.value === v)?.label || v).join(', ') || '-'
      }
      return String(value)
    case 'date': return formatDate(value)
    case 'datetime': return formatDateTime(value)
    default: return String(value)
  }
} */

</script>

<style>
.customer-basic-info-drawer .el-drawer__body {
  padding: 0 !important;
}

.wk-stage-result-popover.el-popper,
.wk-stage-result-popover.el-popover {
  padding: 0;
  border: 1px solid rgb(226 232 240);
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow:
    0 25px 50px -12px rgb(0 0 0 / 0.25),
    0 0 0 1px rgb(15 23 42 / 0.04);
  z-index: 60 !important;
  animation: wk-stage-result-popover-in 0.2s ease-out;
}

@keyframes wk-stage-result-popover-in {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.wk-stage-result-popover__inner {
  padding: 0.375rem 0;
  background: #fff;
}

.wk-stage-result-popover__head {
  margin-bottom: 0.25rem;
  padding: 0.25rem 0.75rem;
  border-bottom: 1px solid rgb(248 250 252);
}

.wk-stage-result-popover__title {
  margin: 0;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgb(148 163 184);
}

.wk-stage-result-popover__item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1.25;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.wk-stage-result-popover__icon {
  flex-shrink: 0;
  font-size: 0.875rem;
  line-height: 1;
}

.wk-stage-result-popover__item--won {
  color: rgb(5 150 105);
}

.wk-stage-result-popover__item--won:hover,
.wk-stage-result-popover__item--won.is-active {
  background: rgb(236 253 245);
}

.wk-stage-result-popover__item--lost {
  color: rgb(225 29 72);
}

.wk-stage-result-popover__item--lost:hover,
.wk-stage-result-popover__item--lost.is-active {
  background: rgb(255 241 242);
}

.wk-stage-result-popover__item--reopen {
  margin-top: 0.25rem;
  padding-top: 0.625rem;
  border-top: 1px solid rgb(241 245 249);
  color: rgb(71 85 105);
}

.wk-stage-result-popover__item--reopen:hover {
  background: rgb(248 250 252);
}
</style>
