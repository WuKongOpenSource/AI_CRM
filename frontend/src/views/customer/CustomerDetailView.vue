<template>
  <div
    class="h-full flex flex-col"
    :class="{
      'wk-customer-detail-embedded': embedded,
      'wk-customer-detail-mobile': isMobile,
      'wk-customer-detail-embedded-mobile': isEmbeddedMobileLayout
    }"
  >
    <!-- Loading -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <span class="material-symbols-outlined text-slate-300 text-4xl animate-spin">progress_activity</span>
    </div>

    <!-- Content -->
    <template v-else-if="customer">
      <div class="min-h-0 flex-1 overflow-auto">
        <!-- Sticky Header -->
        <div
          v-if="!isEmbeddedMobileLayout"
          class="static z-20 bg-background-light/90 backdrop-blur-md px-4 md:px-8 pt-4 pb-4 border-b border-slate-200/50 shrink-0 md:sticky md:top-0"
        >
          <!-- Breadcrumb -->
          <div v-if="!embedded" class="flex items-center gap-2 text-sm text-slate-500 mb-3">
            <button @click="handleBackToCustomerList" class="hover:text-primary flex items-center gap-1 transition-colors">
              <WkIcon name="customer" :size="14" />
              客户管理
            </button>
            <span class="material-symbols-outlined text-xs">chevron_right</span>
            <span class="text-slate-900 font-medium">客户详情</span>
          </div>

          <!-- Customer Info Card -->
          <div class="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
            <div class="flex flex-col gap-3 md:flex-row md:justify-between md:gap-0 justify-between">
              <div class="flex min-w-0 gap-4">
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
                  <div class="flex flex-col items-start gap-1 md:flex-row md:items-center md:gap-3 md:flex-wrap">
                    <h2 class="text-lg md:text-xl font-bold text-slate-900 truncate min-w-0 w-full md:w-auto">
                      {{ customer.companyName }}
                    </h2>
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
                  <div class="hidden md:flex w-full min-w-0 flex-wrap items-center gap-x-3 gap-y-2 text-sm">
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

              <!-- Mobile: move contact/tags row under whole left block -->
              <div class="mt-0 flex w-full min-w-0 flex-wrap items-center gap-x-3 gap-y-2 text-sm md:hidden">
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
                  class="ml-0 flex min-w-0 flex-wrap items-center justify-end gap-2"
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
              <div class="flex w-full flex-nowrap justify-start gap-2 md:overflow-visible md:w-auto md:flex-nowrap md:justify-start shrink-0">
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
                <button v-if="canEditCustomer" class="h-8 px-4 inline-flex items-center border border-slate-200 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors whitespace-nowrap" @click="handleEdit">编辑</button>
                <button v-if="canCreateFollowUps" class="h-8 px-4 bg-primary/10 text-primary border border-primary/20 rounded-lg text-sm font-bold flex items-center gap-1.5 hover:bg-primary/20 transition-colors whitespace-nowrap" @click="handleAiFollowUp">
                  <WkIcon name="ai" class="text-sm" />
                  AI 跟进
                </button>
                <button
                  type="button"
                  class="h-8 shrink-0 inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 text-sm font-semibold text-white shadow-md shadow-primary/25 transition-colors hover:bg-primary/90 whitespace-nowrap"
                  @click="showBasicInfoDrawer = true"
                >
                  <span class="material-symbols-outlined text-base leading-none">description</span>
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
                <WkIcon name="stage" :size="14" />
              </span>
              <h3 class="text-sm font-bold text-slate-900">客户阶段</h3>
            </div> -->
            <div class="relative overflow-visible overflow-x-auto overflow-y-visible md:overflow-visible">
              <!-- Chevron segments (mobile: no wrap + horizontal scroll; desktop: wrap) -->
              <div class="relative flex flex-nowrap md:flex-wrap items-stretch gap-x-2 md:gap-x-0 gap-y-2 min-w-max">
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
      <div class="wk-mobile-px-15 md:px-8 pb-8 pt-3">
        <div v-if="!isEmbeddedMobileLayout" class="lg:hidden mb-4">
          <div class="flex items-center gap-2 p-1 rounded-xl bg-slate-100">
            <button
              type="button"
              class="flex-1 h-9 px-3 rounded-lg text-sm font-bold transition-colors"
              :class="detailTab === 'ai' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
              @click="detailTab = 'ai'"
            >
              AI分析
            </button>
            <button
              v-if="canViewFollowUps"
              type="button"
              class="flex-1 h-9 px-3 rounded-lg text-sm font-bold transition-colors"
              :class="detailTab === 'activity' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
              @click="detailTab = 'activity'"
            >
              最近活动
            </button>
            <button
              type="button"
              class="flex-1 h-9 px-3 rounded-lg text-sm font-bold transition-colors"
              :class="detailTab === 'related' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
              @click="detailTab = 'related'"
            >
              关联模块
            </button>
          </div>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-4">
          <!-- Left Column: Basic Info (col-span-3) -->
          <div :class="[isEmbeddedMobileLayout || detailTab === 'ai' ? 'block' : 'hidden', 'lg:block lg:col-span-3 space-y-4']">
            <section class="group/ai-section bg-white" :class="[!isEmbeddedMobileLayout ? 'rounded-xl border border-slate-200 px-4 shadow-sm py-4' : '']">
              <div class="mb-4 space-y-1">
                <!-- 上：图标 + 标题（整行展示，可换行）+ 更新时间 + 刷新 -->
                <div class="flex items-center justify-between gap-2">
                  <div class="flex min-w-0 flex-1 items-center gap-2">
                    <span
                      :class="sectionIconBoxClass"
                      :style="getSectionIconStyle('basicInfo')"
                    >
                      <WkIcon name="ai" :size="14" />
                    </span>
                    <h3 class="min-w-0 flex-1 text-sm font-bold leading-snug text-slate-900 break-words">
                      {{ savedAiAnalysisTitle }}
                    </h3>
                  </div>
                  <div class="flex shrink-0 items-center justify-end gap-2">
                    <p class="text-right text-xs leading-relaxed text-slate-400">
                      {{ aiAnalysisDisplayTime }}更新
                    </p>
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
                    <button
                      type="button"
                      class="group/module-action relative inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 opacity-0 pointer-events-none transition-[opacity,background-color,color,border-color] hover:bg-[#efefef] hover:text-[#0d0d0d] group-hover/ai-section:pointer-events-auto group-hover/ai-section:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100"
                      :aria-expanded="aiAnalysisExpanded"
                      :aria-label="aiAnalysisExpanded ? '收起 AI分析' : '展开 AI分析'"
                      @click="aiAnalysisExpanded = !aiAnalysisExpanded"
                    >
                      <span class="material-symbols-outlined text-[16px] leading-none">
                        {{ aiAnalysisExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down' }}
                      </span>
                      <span
                        class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                        role="tooltip"
                      >
                        {{ aiAnalysisExpanded ? '收起 AI分析' : '展开 AI分析' }}
                      </span>
                    </button>
                  </div>
                </div>
              </div>

              <div v-show="aiAnalysisExpanded">
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
                  :compact-score="isEmbeddedMobileLayout"
                  unified
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
              </div>

            </section>
          </div>

          <!-- Center Column: Follow-ups Timeline (col-span-6) -->
          <div
            v-if="canViewFollowUps"
            class="wk-customer-detail-activity group/activity"
            :class="[isEmbeddedMobileLayout || detailTab === 'activity' ? 'block' : 'hidden', 'lg:block lg:col-span-6 space-y-4', isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : '']"
          >
            <div class="flex min-w-0 flex-col items-start gap-2 md:flex-row md:items-center md:justify-between">
              <div class="flex min-w-0 items-center gap-2">
                <span
                  :class="sectionIconBoxClass"
                  :style="getSectionIconStyle('recentActivity')"
                >
                  <span :class="sectionMaterialIconClass">history</span>
                </span>
                <h3 class="min-w-0 flex-1 text-sm font-bold leading-snug text-slate-900 break-words">{{ isEmbeddedMobileLayout ? '活动' : '最近活动 - AI时间轴' }}</h3>
                <button
                  v-if="followUps.length > 0"
                  type="button"
                  class="inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 opacity-0 pointer-events-none transition-[opacity,background-color,color,border-color] hover:border-primary/30 hover:bg-primary/5 hover:text-primary group-hover/activity:pointer-events-auto group-hover/activity:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100"
                  :aria-expanded="followUpTimelineExpanded"
                  :aria-label="followUpTimelineExpanded ? '收起跟进记录' : '展开跟进记录'"
                  :title="followUpTimelineExpanded ? '收起跟进记录' : '展开跟进记录'"
                  @click="followUpTimelineExpanded = !followUpTimelineExpanded"
                >
                  <span class="material-symbols-outlined text-[16px] leading-none">
                    {{ followUpTimelineExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down' }}
                  </span>
                </button>
              </div>
              <div
                v-if="!isEmbeddedMobileLayout"
                class="flex w-full justify-start md:w-auto shrink-0 flex-wrap items-center gap-2 md:gap-3"
              >
                <div class="flex flex-wrap items-center gap-2">
                  <button
                    v-for="(option, index) in followUpTypeFilters"
                    :key="option.value || 'all'"
                    type="button"
                    :class="[
                      'rounded-full py-1.5 text-xs font-medium transition-colors md:px-3',
                      isMobile && index === 0 ? 'pl-3 pr-3' : 'px-3',
                      selectedFollowUpType === option.value
                        ? 'bg-primary text-white shadow-sm'
                        : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                    ]"
                    @click="handleFollowUpTypeFilterChange(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
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

            <div
              v-if="followUpTimelineExpanded && followUps.length === 0 && !followUpLoading"
              :class="isEmbeddedMobileLayout
                ? 'rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center'
                : 'bg-white border border-slate-200 rounded-xl p-12 text-center shadow-sm'"
            >
              <span
                class="material-symbols-outlined"
                :class="isEmbeddedMobileLayout ? 'text-3xl leading-none text-slate-200' : 'mb-3 text-4xl text-slate-300'"
              >event_note</span>
              <p :class="isEmbeddedMobileLayout ? 'mt-2 text-xs font-medium text-slate-400' : 'text-sm text-slate-400'">暂无跟进记录</p>
              <p v-if="!isEmbeddedMobileLayout" class="text-xs text-slate-300 mt-1">点击上方按钮添加第一条跟进记录</p>
            </div>

            <div v-else-if="followUpTimelineExpanded" v-loading="followUpLoading">
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
                    :can-toggle-task-complete="canToggleTasks"
                    :compact="isMobile || isEmbeddedMobileLayout"
                    @edit="handleEditFollowUp"
                    @delete="confirmDeleteFollowUp"
                    @task-click="handleViewFollowUpTask"
                    @task-toggle-complete="handleToggleFollowUpTask"
                    @attachment-quote="attachment => handleQuoteFollowUpAttachment(item, attachment)"
                  />
                  <div v-if="followUpIndex < followUps.length - 1" class="h-3 shrink-0" aria-hidden="true" />
                </div>
              </div>
            </div>

            <div v-if="followUpTimelineExpanded && followUpTotal > followUpPageSize" class="flex justify-center">
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
          <div
            class="wk-related-modules"
            :class="[isEmbeddedMobileLayout || detailTab === 'related' ? 'block' : 'hidden', 'lg:block lg:col-span-3', isEmbeddedMobileLayout ? '' : 'space-y-4']"
          >
            <div v-if="!embedded" class="flex items-center justify-between px-1">
              <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
                <span :class="sectionIconBoxClass" :style="getSectionIconStyle('relatedBusiness')">
                  <span :class="sectionMaterialIconClass">hub</span>
                </span>
                关联业务模块
              </h3>
              <span class="text-xs font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full uppercase tracking-tighter">{{ visibleRelatedModuleCount }}个模块</span>
            </div>

            <!-- Contacts Module -->
            <section v-if="canViewContacts" class="wk-related-contacts group/contacts-module bg-white shadow-sm" :class="[isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : 'border border-slate-200 rounded-2xl p-4']" v-loading="contactLoading">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('relatedContacts')">
                    <span :class="sectionMaterialIconClass">group</span>
                  </span>
                  联系人
                  <span class="text-slate-400 font-normal">({{ contactTotal }})</span>
                </h4>
                <div class="flex shrink-0 items-center gap-2">
                  <button
                    v-if="canCreateContacts"
                    type="button"
                    class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
                    aria-label="名片上传"
                    @click="handleAddContactCardUpload"
                  >
                    <span class="material-symbols-outlined text-[18px] leading-none">contact_page</span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      名片上传
                    </span>
                  </button>
                  <button
                    v-if="canCreateContacts"
                    type="button"
                    class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
                    aria-label="新建联系人"
                    @click="handleAddContact"
                  >
                    <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">person_add</span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      新建联系人
                    </span>
                  </button>
                  <button
                    type="button"
                    class="group/module-action relative inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 opacity-0 pointer-events-none transition-[opacity,background-color,color,border-color] hover:bg-[#efefef] hover:text-[#0d0d0d] group-hover/contacts-module:pointer-events-auto group-hover/contacts-module:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100"
                    :aria-expanded="contactsModuleExpanded"
                    :aria-label="contactsModuleExpanded ? '收起联系人' : '展开联系人'"
                    @click="contactsModuleExpanded = !contactsModuleExpanded"
                  >
                    <span class="material-symbols-outlined text-[16px] leading-none">
                      {{ contactsModuleExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down' }}
                    </span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      {{ contactsModuleExpanded ? '收起联系人' : '展开联系人' }}
                    </span>
                  </button>
                </div>
              </div>
              <div v-if="contactsModuleExpanded" class="space-y-4">
                <div v-if="contacts.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
                  <span class="material-symbols-outlined text-2xl leading-none text-slate-200">person_off</span>
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
                            <button
                              v-if="!contact.isPrimary && canSetPrimaryContacts"
                              type="button"
                              class="group/module-action relative flex size-7 shrink-0 items-center justify-center rounded-full bg-amber-50 text-amber-600 opacity-0 pointer-events-none transition-all hover:bg-amber-100 group-hover:opacity-100 group-hover:pointer-events-auto"
                              aria-label="设为主要联系人"
                              @click.stop="handleSetPrimary(contact.contactId)"
                            >
                              <span
                                class="material-symbols-outlined text-[18px] leading-none"
                                style="font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 24"
                              >star</span>
                              <span
                                class="pointer-events-none absolute left-1/2 top-full z-[200] mt-2 -translate-x-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                                role="tooltip"
                              >
                                设为主要联系人
                              </span>
                            </button>
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
            <section v-if="canViewTasks" class="group/tasks-module bg-white shadow-sm" :class="[isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : 'border border-slate-200 rounded-2xl p-4']" v-loading="taskLoading">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('todoTasks')">
                    <WkIcon name="task" :size="14" />
                  </span>
                  任务
                </h4>
                <div class="flex shrink-0 items-center gap-2">
                  <button
                    v-if="canCreateTasks"
                    type="button"
                    class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
                    aria-label="新建任务"
                    @click="handleAddTask"
                  >
                    <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add</span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      新建任务
                    </span>
                  </button>
                  <button
                    type="button"
                    class="group/module-action relative inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 opacity-0 pointer-events-none transition-[opacity,background-color,color,border-color] hover:bg-[#efefef] hover:text-[#0d0d0d] group-hover/tasks-module:pointer-events-auto group-hover/tasks-module:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100"
                    :aria-expanded="tasksModuleExpanded"
                    :aria-label="tasksModuleExpanded ? '收起任务' : '展开任务'"
                    @click="tasksModuleExpanded = !tasksModuleExpanded"
                  >
                    <span class="material-symbols-outlined text-[16px] leading-none">
                      {{ tasksModuleExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down' }}
                    </span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      {{ tasksModuleExpanded ? '收起任务' : '展开任务' }}
                    </span>
                  </button>
                </div>
              </div>
              <div v-if="tasksModuleExpanded" class="space-y-4">
                <div v-if="!customer.tasks?.length" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
                  <span class="material-symbols-outlined text-2xl leading-none text-slate-200">task_alt</span>
                  <p class="mt-2 text-xs font-medium text-slate-400">暂无待办任务</p>
                </div>
                <div
                  v-else
                  :class="embedded ? 'space-y-3' : 'ml-3 space-y-3 border-l-2 border-slate-100 pl-5'"
                >
                  <div
                    v-for="task in customer.tasks?.slice(0, 5)"
                    :key="task.taskId"
                    class="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-3 transition-all hover:shadow-md"
                    :class="selectedCustomerTask?.taskId === task.taskId ? 'border-primary ring-1 ring-primary/20' : ''"
                    @click="handleViewCustomerTask(task)"
                  >
                    <div
                      v-if="!embedded"
                      class="absolute -left-[27px] top-4 size-3 rounded-full border-2 bg-white"
                      :class="task.status === 'COMPLETED' ? 'border-emerald-500' : 'border-slate-300'"
                    />
                    <div class="flex items-start gap-3">
                      <button
                        v-if="canToggleTasks"
                        type="button"
                        class="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded border transition-colors"
                        :class="task.status === 'COMPLETED'
                          ? 'border-emerald-500 bg-emerald-500 text-white'
                          : 'border-slate-300 text-transparent hover:border-primary hover:text-primary/20'"
                        :aria-label="task.status === 'COMPLETED' ? '标记为未完成' : '标记为已完成'"
                        :title="task.status === 'COMPLETED' ? '标记为未完成' : '标记为已完成'"
                        @click.stop="handleToggleCustomerTask(task)"
                      >
                        <span class="material-symbols-outlined text-[14px] font-bold">check</span>
                      </button>
                      <span
                        v-else
                        class="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded border"
                        :class="task.status === 'COMPLETED' ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-slate-300 text-transparent'"
                      >
                        <span class="material-symbols-outlined text-[14px] font-bold">check</span>
                      </span>
                      <div class="min-w-0 flex-1">
                        <h5
                          class="mb-1 truncate text-sm font-bold transition-colors group-hover:text-primary"
                          :class="task.status === 'COMPLETED' ? 'text-slate-400 line-through' : 'text-slate-900'"
                        >
                          {{ task.title }}
                        </h5>
                        <div class="flex flex-wrap items-center gap-2">
                          <span
                            v-if="task.dueDate"
                            class="rounded-full px-2 py-0.5 text-xs font-bold"
                            :class="isCustomerTaskOverdue(task) ? 'bg-red-50 text-red-500' : 'bg-slate-50 text-slate-600'"
                          >
                            截止 {{ formatDate(task.dueDate) }}
                          </span>
                          <span
                            v-if="task.priority"
                            class="rounded-full px-2 py-0.5 text-xs font-bold"
                            :class="getCustomerTaskPriorityClass(task.priority)"
                          >
                            {{ getCustomerTaskPriorityLabel(task.priority) }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- Schedules Module -->
            <section v-if="canViewSchedules" class="group/schedules-module bg-white shadow-sm" :class="[isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : 'border border-slate-200 rounded-2xl p-4']" v-loading="scheduleLoading">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('relatedSchedules')">
                    <span :class="sectionMaterialIconClass">event_note</span>
                  </span>
                  日程
                  <span class="text-slate-400 font-normal">({{ scheduleTotal }})</span>
                </h4>
                <div class="flex shrink-0 items-center gap-2">
                  <button
                    v-if="canCreateSchedules"
                    type="button"
                    class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
                    aria-label="新建日程"
                    @click="handleAddSchedule"
                  >
                    <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add</span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      新建日程
                    </span>
                  </button>
                  <button
                    type="button"
                    class="group/module-action relative inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 opacity-0 pointer-events-none transition-[opacity,background-color,color,border-color] hover:bg-[#efefef] hover:text-[#0d0d0d] group-hover/schedules-module:pointer-events-auto group-hover/schedules-module:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100"
                    :aria-expanded="schedulesModuleExpanded"
                    :aria-label="schedulesModuleExpanded ? '收起日程' : '展开日程'"
                    @click="schedulesModuleExpanded = !schedulesModuleExpanded"
                  >
                    <span class="material-symbols-outlined text-[16px] leading-none">
                      {{ schedulesModuleExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down' }}
                    </span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      {{ schedulesModuleExpanded ? '收起日程' : '展开日程' }}
                    </span>
                  </button>
                </div>
              </div>
              <div v-if="schedulesModuleExpanded" class="space-y-4">
                <div v-if="customerSchedules.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
                  <span class="material-symbols-outlined text-2xl leading-none text-slate-200">event_busy</span>
                  <p class="mt-2 text-xs font-medium text-slate-400">暂无关联日程</p>
                </div>
                <div
                  v-else
                  :class="embedded ? 'space-y-3' : 'ml-3 space-y-3 border-l-2 border-slate-100 pl-5'"
                >
                  <div
                    v-for="schedule in customerSchedules"
                    :key="schedule.scheduleId"
                    class="group relative cursor-pointer rounded-xl border border-slate-200 bg-white p-3 transition-all hover:shadow-md"
                    :class="selectedCustomerSchedule?.scheduleId === schedule.scheduleId ? 'border-primary ring-1 ring-primary/20' : ''"
                    @click="handleViewCustomerSchedule(schedule)"
                  >
                    <div v-if="!embedded" class="absolute -left-[27px] top-4 size-3 rounded-full border-2 border-primary bg-white" />
                    <div class="flex items-start justify-between gap-3">
                      <div class="min-w-0 flex-1">
                        <h5 class="mb-1 truncate text-sm font-bold text-slate-900 transition-colors group-hover:text-primary">{{ schedule.title }}</h5>
                        <div class="flex flex-wrap items-center gap-2">
                          <span v-if="schedule.customerName" class="text-xs font-medium text-slate-500">{{ schedule.customerName }}</span>
                          <span class="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-bold text-primary">
                            {{ getScheduleTypeLabel(schedule) }}
                          </span>
                          <span v-if="schedule.location" class="rounded-full bg-slate-50 px-2 py-0.5 text-xs font-bold text-slate-600">
                            {{ schedule.location }}
                          </span>
                        </div>
                        <p v-if="getScheduleListSummary(schedule)" class="mt-2 line-clamp-2 text-xs leading-relaxed text-slate-500">
                          {{ getScheduleListSummary(schedule) }}
                        </p>
                      </div>
                      <div class="shrink-0 text-right">
                        <span class="inline-block max-w-[9rem] whitespace-normal rounded-lg bg-slate-50 px-2 py-1 text-xs font-bold leading-4 text-slate-600">
                          {{ formatScheduleDateTime(schedule.startTime) }}
                          <template v-if="schedule.endTime"> - {{ formatScheduleEndTime(schedule.startTime, schedule.endTime) }}</template>
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="scheduleTotal > schedulePageSize" class="pt-2 flex justify-center">
                  <el-pagination
                    v-model:current-page="schedulePage"
                    :page-size="schedulePageSize"
                    :total="scheduleTotal"
                    layout="prev, pager, next"
                    small
                    @current-change="handleSchedulePageChange"
                  />
                </div>
              </div>
            </section>

            <!-- Documents Module -->
            <section v-if="canViewKnowledge" class="group/documents-module bg-white shadow-sm" :class="[isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : 'border border-slate-200 rounded-2xl p-4']">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('documentCenter')">
                    <WkIcon name="knowledge" :size="14" />
                  </span>
                  文档
                </h4>
                <div class="flex shrink-0 items-center gap-2">
                  <button
                    v-if="canUploadKnowledge"
                    type="button"
                    class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
                    aria-label="上传文档"
                    @click="openCustomerKnowledgeUpload"
                  >
                    <span class="material-symbols-outlined text-[18px] leading-none">add</span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      上传文档
                    </span>
                  </button>
                  <button
                    type="button"
                    class="group/module-action relative inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 opacity-0 pointer-events-none transition-[opacity,background-color,color,border-color] hover:bg-[#efefef] hover:text-[#0d0d0d] group-hover/documents-module:pointer-events-auto group-hover/documents-module:opacity-100 focus-visible:pointer-events-auto focus-visible:opacity-100"
                    :aria-expanded="documentsModuleExpanded"
                    :aria-label="documentsModuleExpanded ? '收起文档' : '展开文档'"
                    @click="documentsModuleExpanded = !documentsModuleExpanded"
                  >
                    <span class="material-symbols-outlined text-[16px] leading-none">
                      {{ documentsModuleExpanded ? 'keyboard_arrow_up' : 'keyboard_arrow_down' }}
                    </span>
                    <span
                      class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                      role="tooltip"
                    >
                      {{ documentsModuleExpanded ? '收起文档' : '展开文档' }}
                    </span>
                  </button>
                </div>
              </div>
              <div v-if="documentsModuleExpanded">
                <div v-if="customerKnowledgeLoading" class="py-8 text-center">
                  <span class="material-symbols-outlined text-2xl text-slate-300 animate-spin">progress_activity</span>
                </div>
                <div v-else-if="customerKnowledgeList.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
                  <span class="material-symbols-outlined text-2xl leading-none text-slate-200">folder_off</span>
                  <p class="mt-2 text-xs font-medium text-slate-400">{{ getKnowledgeEmptyText() }}</p>
                </div>
                <div v-else class="space-y-3">
                  <div
                    v-for="item in customerKnowledgeList"
                    :key="item.knowledgeId"
                    class="group cursor-pointer rounded-2xl border border-slate-200 bg-gradient-to-br from-white via-white to-slate-50/80 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-lg hover:shadow-slate-200/70"
                    @click="openKnowledgeDetail(item.knowledgeId)"
                  >
                    <div class="flex items-start gap-3">
                      <FileTypeIcon :file-name="item.name" :mime-type="item.mimeType" :knowledge-type="item.type" size="md" />
                      <div class="min-w-0 flex-1">
                        <div class="flex items-start justify-between gap-3">
                          <div class="min-w-0 flex-1">
                            <h5 class="truncate text-sm font-bold text-slate-900 transition-colors group-hover:text-primary">{{ item.name }}</h5>
                            <p class="mt-1 text-[11px] font-medium text-slate-400">{{ getKnowledgeTypeLabel(item.type) }}</p>
                          </div>
                          <button
                            type="button"
                            class="group/module-action relative flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-primary/10 hover:text-primary"
                            aria-label="下载文档"
                            @click.stop="handleKnowledgeDownload(item)"
                          >
                            <span class="material-symbols-outlined text-sm">download</span>
                            <span
                              class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                              role="tooltip"
                            >
                              下载文档
                            </span>
                          </button>
                        </div>
                        <p class="mt-2 line-clamp-2 text-xs leading-relaxed text-slate-500">
                          {{ item.summary || getKnowledgeTypeLabel(item.type) + ' / linked to current customer' }}
                        </p>
                        <p class="mt-3 text-[11px] text-slate-400">{{ formatDate(item.createTime) }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
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

    <KnowledgeUploadDialog
      ref="knowledgeUploadDialogRef"
      v-model="showKnowledgeUploadDialog"
      :customer-id="customer?.customerId"
      headline="上传客户文档"
      subline="支持 PDF、Word、PPT、Excel。上传后将关联当前客户，AI 将自动解析并建立索引。"
      step1-label="1. 选择文档类型"
      success-message="文档上传成功"
      summary-placeholder="例如：客户关切点、关键结论、下一步计划…"
      destroy-on-close
      @success="onCustomerKnowledgeUploadSuccess"
    />

    <KnowledgeDetailModal
      v-model="showKnowledgeDetailModal"
      :knowledge-id="selectedKnowledgeId"
      @summary-updated="handleKnowledgeSummaryUpdated"
    />

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

    <CustomerBasicInfoDrawer
      v-model="showBasicInfoDrawer"
      :customer="customer"
      :contacts="contacts"
      :custom-fields="customFields"
      :latest-ai-report="latestAiReport"
      @contacts-updated="onBasicInfoContactsUpdated"
    />

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
      :width="isMobile ? 'calc(100% - 40px)' : '680px'"
      class="wk-dialog--flush"
      destroy-on-close
    >
      <div
        class="space-y-4"
        :class="isMobile ? 'max-h-[calc(100vh-10rem)] overflow-y-auto pb-[env(safe-area-inset-bottom)]' : ''"
      >
        <section class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-4">
          <p class="text-xs font-bold uppercase tracking-wider text-slate-500">AI 状态探测</p>
          <div class="mt-3">
            <span
              v-if="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)"
              class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold shadow-sm"
              :class="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)?.badgeClass"
            >
              <span class="size-2 rounded-full mr-1.5" :class="getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)?.dotClass"></span>
              {{ getAiStatusMeta(latestAiReport?.aiStatusDetection || customer?.aiStatusDetection)?.label }}
            </span>
            <p v-else class="text-sm leading-6 text-slate-700">{{ latestAiReport?.aiStatusDetection || customer?.aiStatusDetection || '暂无 AI 状态探测' }}</p>
          </div>
        </section>
        <section class="rounded-xl border border-slate-200 bg-white px-4 py-4">
          <p class="text-xs font-bold uppercase tracking-wider text-slate-500">AI 深度分析</p>
          <ul v-if="aiReportDialogDeepInsightSegments.length" class="mt-2 space-y-1.5">
            <li
              v-for="(segment, index) in aiReportDialogDeepInsightSegments"
              :key="`${segment}-${index}`"
              class="flex items-start gap-2 text-sm leading-6 text-slate-700"
            >
              <span class="mt-2 size-1 shrink-0 rounded-full bg-slate-500"></span>
              <span class="min-w-0 break-words">{{ segment }}</span>
            </li>
          </ul>
          <p v-else class="mt-2 text-sm leading-6 text-slate-700">暂无 AI 深度分析</p>
          <div v-if="currentAiNextStep" class="mt-4 border-t border-slate-100 pt-4">
            <p class="text-xs font-bold uppercase tracking-wider text-slate-500">建议下一步行动</p>
            <p class="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{{ currentAiNextStep }}</p>
          </div>
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
      :is-mobile="isMobile && !isEmbeddedMobileLayout"
      :can-edit="canCreateTasks"
      :can-toggle-complete="canToggleTasks"
      @edit="handleCustomerTaskEditFromDetail"
      @mutated="refreshCustomerAfterTaskMutation"
    />

    <TaskEditDialog
      v-model="showTaskEditDialog"
      :editing-task="editingCustomerTask"
      :default-customer="taskDefaultCustomer"
      @saved="handleTaskDialogSaved"
    />

    <ScheduleDetailDrawer
      v-model="showCustomerScheduleDetail"
      :schedule="selectedCustomerSchedule"
      :is-mobile="isMobile"
      :can-edit="canEditSchedules"
      :can-delete="canDeleteSchedules"
      @edit="handleEditCustomerScheduleFromDetail"
      @deleted="handleCustomerScheduleDeleted"
    />

    <ScheduleFormDialog
      v-model="showScheduleFormDialog"
      :editing-schedule="editingCustomerSchedule"
      :default-customer="scheduleDefaultCustomer"
      @created="handleCustomerScheduleCreated"
      @updated="handleCustomerScheduleUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useTaskStore } from '@/stores/task'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { addCustomerTag, generateCustomerAiReport, removeCustomerTag, transferCustomer, updateCustomerStage } from '@/api/customer'
import { queryTaskList } from '@/api/task'
import { queryScheduleList, type ScheduleVO } from '@/api/schedule'
import type { CustomerAiParseVO } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import { addFollowUp, deleteFollowUp, queryFollowUpPageList, updateFollowUp } from '@/api/followup'
import { deleteContact, queryContactPageList, queryContactsByCustomer, setPrimaryContact } from '@/api/contact'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { downloadKnowledge, queryKnowledgeList } from '@/api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Knowledge, Task, TaskStatus } from '@/types/common'
import type { Contact, CustomerAiReportVO, CustomerTag, FollowUp, FollowUpAddBO, FollowUpAttachment, FollowUpTask, FollowUpUpdateBO } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { compactCustomerAiInsight, getCustomerAiStatusMeta } from '@/utils/customerAi'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'
import FollowUpUpsertDialog from '@/components/customer/FollowUpUpsertDialog.vue'
import type { FollowUpUpsertSubmitPayload } from '@/components/customer/FollowUpUpsertDialog.vue'
import AiParseInsightSidebar from '@/components/crm/AiParseInsightSidebar.vue'
import FollowUpCard from '@/components/customer/FollowUpCard.vue'
import FileTypeIcon from '@/components/common/FileTypeIcon.vue'
import CustomerBasicInfoDrawer from '@/views/customer/components/CustomerBasicInfoDrawer.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import ContactUpsertDialog from '@/views/contact/components/ContactUpsertDialog.vue'
import ContactDetailDrawer from '@/views/contact/components/ContactDetailDrawer.vue'
import TaskDetailDrawer from '@/views/task/components/TaskDetailDrawer.vue'
import TaskEditDialog from '@/views/task/components/TaskEditDialog.vue'
import ScheduleDetailDrawer from '@/views/calendar/components/ScheduleDetailDrawer.vue'
import ScheduleFormDialog from '@/views/calendar/components/ScheduleFormDialog.vue'
import KnowledgeDetailModal from '@/components/knowledge/KnowledgeDetailModal.vue'
import KnowledgeUploadDialog from '@/components/knowledge/KnowledgeUploadDialog.vue'
import {
  CUSTOMER_DETAIL_LIST_PAGE_QUERY_KEY,
  CUSTOMER_LIST_PAGE_QUERY_KEY
} from '@/views/customer/constants'
import { appEvents, APP_EVENT } from '@/utils/events'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()
const taskStore = useTaskStore()
const userStore = useUserStore()
const { isMobile: responsiveIsMobile } = useResponsive()

const props = withDefaults(defineProps<{
  customerId?: string
  embedded?: boolean
  forceMobile?: boolean
}>(), {
  customerId: '',
  embedded: false,
  forceMobile: false
})

const emit = defineEmits<{
  (e: 'quote-attachment', value: { followUp: FollowUp; attachment: FollowUpAttachment }): void
}>()

const activeCustomerId = computed(() => props.customerId || String(route.params.id || ''))
const embedded = computed(() => props.embedded)
const isMobile = computed(() => props.forceMobile || responsiveIsMobile.value)
const isEmbeddedMobileLayout = computed(() => props.embedded && props.forceMobile)

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
const detailTab = ref<'ai' | 'activity' | 'related'>('ai')
const newTagName = ref('')
const followUps = ref<FollowUp[]>([])
const followUpTotal = ref(0)
const followUpPage = ref(1)
const followUpPageSize = ref(5)
const followUpLoading = ref(false)
const aiAnalysisExpanded = ref(true)
const followUpTimelineExpanded = ref(true)
const contactsModuleExpanded = ref(true)
const tasksModuleExpanded = ref(true)
const schedulesModuleExpanded = ref(true)
const documentsModuleExpanded = ref(true)
const selectedFollowUpType = ref('')
const contacts = ref<Contact[]>([])
const contactTotal = ref(0)
const contactPage = ref(1)
const contactPageSize = ref(5)
const contactLoading = ref(false)
const taskLoading = ref(false)
const customerSchedules = ref<ScheduleVO[]>([])
const scheduleTotal = ref(0)
const schedulePage = ref(1)
const schedulePageSize = ref(5)
const scheduleLoading = ref(false)
const customerKnowledgeList = ref<Knowledge[]>([])

function parsePositivePageQuery(value: unknown): number | null {
  if (typeof value !== 'string') return null
  const page = Number(value)
  if (!Number.isInteger(page) || page < 1) return null
  return page
}

function buildCustomerListRoute() {
  const listPage = parsePositivePageQuery(route.query[CUSTOMER_DETAIL_LIST_PAGE_QUERY_KEY])
  return {
    path: '/customer',
    query: listPage && listPage > 1
      ? { [CUSTOMER_LIST_PAGE_QUERY_KEY]: String(listPage) }
      : {}
  }
}

function handleBackToCustomerList() {
  router.push(buildCustomerListRoute())
}
const customerKnowledgeLoading = ref(false)
const showKnowledgeUploadDialog = ref(false)
const knowledgeUploadDialogRef = ref<InstanceType<typeof KnowledgeUploadDialog> | null>(null)
const selectedKnowledgeId = ref('')
const showKnowledgeDetailModal = ref(false)
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
const selectedCustomerSchedule = ref<ScheduleVO | null>(null)
const showCustomerScheduleDetail = computed({
  get: () => !!selectedCustomerSchedule.value,
  set: (val: boolean) => {
    if (!val) selectedCustomerSchedule.value = null
  }
})
const showScheduleFormDialog = ref(false)
const editingCustomerSchedule = ref<ScheduleVO | null>(null)
const taskDefaultCustomer = computed(() => customer.value
  ? {
      customerId: customer.value.customerId,
      companyName: customer.value.companyName || ''
    }
  : null
)
const scheduleDefaultCustomer = computed(() => customer.value
  ? {
      customerId: customer.value.customerId,
      companyName: customer.value.companyName || ''
    }
  : null
)

watch(showTaskEditDialog, visible => {
  if (!visible) editingCustomerTask.value = null
})

watch(showScheduleFormDialog, visible => {
  if (!visible) editingCustomerSchedule.value = null
})

interface TransferUserOption {
  userId: string
  realname: string
  username?: string
  status?: number
}

const transferUserList = ref<TransferUserOption[]>([])

const sectionIconBoxClass = 'inline-flex size-6 shrink-0 items-center justify-center rounded-md text-white shadow-[0_6px_14px_rgba(15,23,42,0.08)]'
const sectionMaterialIconClass = 'material-symbols-outlined text-[14px] leading-none'
const sectionIconBgColors = {
  customerStage: '#1f1e1c',
  basicInfo: '#8d4f34',
  recentActivity: '#cf744f',
  relatedBusiness: '#5f584f',
  relatedContacts: '#cf744f',
  todoTasks: '#5f704a',
  relatedSchedules: '#8d4f34',
  documentCenter: '#1f1e1c',
} as const

const savedAiAnalysisTitle = 'AI分析'
const emptyAiAnalysisTitle = '\u6682\u65e0 AI \u5206\u6790'
const emptyAiAnalysisDescription = '\u4fdd\u5b58\u5ba2\u6237\u540e\uff0c\u7cfb\u7edf\u4f1a\u81ea\u52a8\u89e6\u53d1 AI \u5206\u6790\uff0c\u7ed3\u679c\u4f1a\u5c55\u793a\u5728\u8fd9\u91cc\u3002'
const viewBasicInfoButtonText = '\u57fa\u672c\u4fe1\u606f'
const AI_ANALYSIS_POLL_INTERVAL_MS = 2500
const AI_ANALYSIS_POLL_MAX_ATTEMPTS = 24

const followUpTypeFilters = [
  { value: '', label: '全部' },
  { value: 'call', label: '电话' },
  { value: 'meeting', label: '会议' },
  { value: 'email', label: '邮件' },
  { value: 'visit', label: '拜访' },
  { value: 'other', label: '其他' }
] as const

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

function onBasicInfoContactsUpdated(newContacts: Contact[]) {
  contacts.value = newContacts
  if (customer.value) syncCurrentContact(customer.value.customerId)
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
const currentAiInsight = computed(() => compactCustomerAiInsight(latestAiReport.value?.aiInsight || customer.value?.aiInsight))
const savedAiParseResult = computed<CustomerAiParseVO | null>(() => {
  const snapshot = customer.value?.aiParseSnapshot

  if (!snapshot) {
    return null
  }

  try {
    const parsed = JSON.parse(snapshot)
    if (!parsed || typeof parsed !== 'object') {
      return null
    }

    const snapshotResult = parsed as CustomerAiParseVO
    const sidebarResult = { ...snapshotResult }
    if (isSameAsListAiSummary(sidebarResult.summary)) {
      sidebarResult.summary = undefined
    }
    const hasSidebarContent = sidebarResult.score != null
      || !!sidebarResult.summary
      || !!sidebarResult.nextStep
      || (sidebarResult.tags?.length || 0) > 0
      || (sidebarResult.keyPoints?.length || 0) > 0
    return hasSidebarContent ? sidebarResult : null
  } catch {
    return null
  }
})
const currentAiDeepInsight = computed(() => (latestAiReport.value?.aiDeepInsight || savedAiParseResult.value?.summary || '').trim())
const currentAiNextStep = computed(() => (latestAiReport.value?.aiNextStep || savedAiParseResult.value?.nextStep || '').trim())
const aiReportDialogDeepInsightSegments = computed(() => splitAiInsightSegments(currentAiDeepInsight.value))
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

function isSameAsListAiSummary(value?: string | null): boolean {
  const summary = String(value || '').replace(/\s+/g, ' ').trim()
  const listSummary = currentAiInsight.value.replace(/\s+/g, ' ').trim()
  return !!summary && !!listSummary && summary.length <= 90 && summary === listSummary
}

function splitAiInsightSegments(value?: string | null): string[] {
  const normalized = String(value || '')
    .replace(/\\n/g, '\n')
    .replace(/\r/g, '\n')
    .trim()
  if (!normalized) return []

  return normalized
    .split(/\n+/)
    .flatMap(line => line.match(/[^。！？!?；;\n]+[。！？!?；;]?/g) || [line])
    .map(cleanAiInsightSegment)
    .filter(Boolean)
}

function cleanAiInsightSegment(value: string): string {
  return value
    .replace(/^\s*(?:[-*•·]|[0-9]+[.)、]|[一二三四五六七八九十]+[.)、]|（[一二三四五六七八九十]+）)\s*/, '')
    .trim()
}

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
    if (activeCustomerId.value !== customerId) {
      clearAiAnalysisPolling()
      return
    }

    aiAnalysisPollAttempts += 1
    try {
      await customerStore.fetchCustomerDetail(customerId)
    } catch (error) {
      console.error('Failed to poll customer ai analysis status:', error)
    }

    if (activeCustomerId.value !== customerId) {
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
const canToggleTasks = computed(() => userStore.hasPermission('task:update_status'))
const canViewSchedules = computed(() => userStore.hasPermission('schedule:view'))
const canCreateSchedules = computed(() => userStore.hasPermission('schedule:create'))
const canEditSchedules = computed(() => userStore.hasPermission('schedule:edit'))
const canDeleteSchedules = computed(() => userStore.hasPermission('schedule:delete'))
const canViewKnowledge = computed(() => userStore.hasPermission('knowledge:view'))
const canUploadKnowledge = computed(() => userStore.hasPermission('knowledge:upload'))
const visibleRelatedModuleCount = computed(() => [
  canViewContacts.value,
  canViewTasks.value,
  canViewSchedules.value,
  canViewKnowledge.value
].filter(Boolean).length)
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

async function loadCustomerDetailPage() {
  const customerId = activeCustomerId.value
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

    if (canViewKnowledge.value) {
      fetchTasks.push(fetchCustomerKnowledge(customerId))
    } else {
      customerKnowledgeList.value = []
    }

    if (canViewSchedules.value) {
      fetchTasks.push(fetchCustomerSchedules(customerId))
    } else {
      customerSchedules.value = []
      scheduleTotal.value = 0
    }

    await Promise.all(fetchTasks)
    loading.value = false
  }
}

onMounted(loadCustomerDetailPage)

watch(activeCustomerId, () => {
  void loadCustomerDetailPage()
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
    const result = await queryFollowUpPageList({
      customerId,
      page: followUpPage.value,
      limit: followUpPageSize.value,
      type: selectedFollowUpType.value || undefined
    })
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

function handleFollowUpTypeFilterChange(type: string) {
  if (selectedFollowUpType.value === type) return
  selectedFollowUpType.value = type
  if (customer.value) {
    void fetchFollowUps(customer.value.customerId, true)
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

async function fetchCustomerSchedules(customerId: string, reset = false) {
  if (!canViewSchedules.value) {
    customerSchedules.value = []
    scheduleTotal.value = 0
    return
  }
  if (reset) schedulePage.value = 1
  scheduleLoading.value = true
  try {
    const result = await queryScheduleList({
      customerId,
      page: schedulePage.value,
      limit: schedulePageSize.value
    })
    customerSchedules.value = result.list || []
    scheduleTotal.value = result.totalRow || 0
  } catch (error) {
    console.error('Failed to fetch customer schedules:', error)
    customerSchedules.value = []
    scheduleTotal.value = 0
  } finally {
    syncSelectedCustomerSchedule()
    scheduleLoading.value = false
  }
}

function handleSchedulePageChange(page: number) {
  schedulePage.value = page
  if (customer.value) fetchCustomerSchedules(customer.value.customerId)
}

async function fetchCustomerKnowledge(customerId: string) {
  if (!canViewKnowledge.value) {
    customerKnowledgeList.value = []
    return
  }

  customerKnowledgeLoading.value = true
  try {
    const result = await queryKnowledgeList({
      customerId,
      page: 1,
      limit: 8
    })
    customerKnowledgeList.value = result.list || []
  } catch (error) {
    console.error('Failed to fetch customer knowledge:', error)
    customerKnowledgeList.value = []
  } finally {
    customerKnowledgeLoading.value = false
  }
}

function openCustomerKnowledgeUpload() {
  if (!canUploadKnowledge.value) return
  knowledgeUploadDialogRef.value?.openEmpty()
}

function onCustomerKnowledgeUploadSuccess() {
  if (customer.value) {
    void fetchCustomerKnowledge(customer.value.customerId)
  }
}

function openKnowledgeDetail(knowledgeId: string | number) {
  selectedKnowledgeId.value = String(knowledgeId)
  showKnowledgeDetailModal.value = true
}

async function handleKnowledgeDownload(item: Knowledge) {
  try {
    await downloadKnowledge(item.knowledgeId, item.name)
  } catch (error) {
    console.error('Failed to download knowledge:', error)
  }
}

function handleKnowledgeSummaryUpdated(payload: { knowledgeId: string; summary: string }) {
  const summary = payload.summary.trim()
  if (!summary) return

  customerKnowledgeList.value = customerKnowledgeList.value.map(item =>
    item.knowledgeId === payload.knowledgeId
      ? { ...item, summary }
      : item
  )
}

function handleQuoteFollowUpAttachment(followUp: FollowUp, attachment: FollowUpAttachment) {
  emit('quote-attachment', { followUp, attachment })
}

function getKnowledgeEmptyText() {
  return canUploadKnowledge.value ? '暂无文档，点击右上角上传文档' : '暂无文档'
}

function getKnowledgeTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    meeting: '会议纪要',
    email: '邮件',
    recording: '录音',
    document: '文档',
    proposal: '方案',
    contract: '合同'
  }
  return labels[String(type || '').toLowerCase()] || '文档'
}

function getScheduleTypeLabel(schedule: ScheduleVO) {
  if (schedule.typeName) return schedule.typeName
  const labels: Record<string, string> = {
    meeting: '会议',
    call: '电话',
    visit: '拜访',
    other: '其他'
  }
  return labels[String(schedule.type || '').toLowerCase()] || '日程'
}

function getScheduleParticipantsLine(schedule: ScheduleVO) {
  if (schedule.participantUsers?.length) {
    return schedule.participantUsers
      .map(user => (user.realname || user.username || '').trim())
      .filter(Boolean)
      .join('、')
  }
  return (schedule.participantNames || '').trim()
}

function getScheduleListSummary(schedule: ScheduleVO) {
  const participants = getScheduleParticipantsLine(schedule)
  if (participants) return `参与人：${participants}`
  return schedule.description || ''
}

function formatScheduleDateTime(dateStr?: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatScheduleTime(dateStr?: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '-'
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatScheduleEndTime(startDateStr?: string, endDateStr?: string) {
  if (!endDateStr) return ''
  if (!startDateStr) return formatScheduleDateTime(endDateStr)

  const startDate = new Date(startDateStr)
  const endDate = new Date(endDateStr)
  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
    return formatScheduleDateTime(endDateStr)
  }

  const sameDay = startDate.getFullYear() === endDate.getFullYear()
    && startDate.getMonth() === endDate.getMonth()
    && startDate.getDate() === endDate.getDate()
  return sameDay ? formatScheduleTime(endDateStr) : formatScheduleDateTime(endDateStr)
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
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
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
    handleBackToCustomerList()
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

async function refreshCustomerAfterTaskMutation() {
  if (!customer.value) return
  await refreshFollowUpContext(customer.value.customerId)
  const id = selectedCustomerTask.value?.taskId
  if (id && customer.value.tasks) {
    selectedCustomerTask.value = customer.value.tasks.find((t: Task) => String(t.taskId) === String(id)) || null
  }
}

function isCustomerTaskOverdue(task: Task): boolean {
  if (!task.dueDate || task.status === 'COMPLETED') return false
  return new Date(task.dueDate) < new Date()
}

function getCustomerTaskPriorityLabel(priority?: string) {
  const labels: Record<string, string> = {
    HIGH: '高优先级',
    MEDIUM: '中优先级',
    LOW: '低优先级'
  }
  return labels[String(priority || '').toUpperCase()] || '中优先级'
}

function getCustomerTaskPriorityClass(priority?: string) {
  const classes: Record<string, string> = {
    HIGH: 'bg-red-50 text-red-500',
    MEDIUM: 'bg-amber-50 text-amber-500',
    LOW: 'bg-slate-100 text-slate-500'
  }
  return classes[String(priority || '').toUpperCase()] || classes.MEDIUM
}

async function handleToggleCustomerTask(task: Task) {
  if (!canToggleTasks.value) return
  const newStatus: TaskStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  await taskStore.changeTaskStatus(task.taskId, newStatus)
  await refreshCustomerAfterTaskMutation()
}

function handleViewCustomerTask(task: Task) {
  if (!canViewTasks.value) return
  selectedCustomerTask.value = task
}

async function resolveCustomerTaskDetail(taskId: string): Promise<Task | null> {
  const normalizedTaskId = String(taskId)
  const localTask = customer.value?.tasks?.find((task: Task) => String(task.taskId) === normalizedTaskId) || null
  if (localTask) {
    return localTask
  }

  const result = await queryTaskList({
    taskId: normalizedTaskId,
    page: 1,
    limit: 1
  })

  return result.list?.[0] || null
}

async function handleViewFollowUpTask(task: FollowUpTask) {
  if (!canViewTasks.value) return

  const detail = await resolveCustomerTaskDetail(task.taskId)
  if (!detail) {
    ElMessage.warning('任务不存在或已被删除')
    return
  }

  selectedCustomerTask.value = detail
}

async function handleToggleFollowUpTask(task: FollowUpTask) {
  if (!canToggleTasks.value) return

  const detail = await resolveCustomerTaskDetail(task.taskId)
  if (!detail) {
    ElMessage.warning('任务不存在或已被删除')
    return
  }

  const newStatus: TaskStatus = detail.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  await taskStore.changeTaskStatus(detail.taskId, newStatus)
  await refreshCustomerAfterTaskMutation()
  ElMessage.success(newStatus === 'COMPLETED' ? '任务已标记完成' : '任务已重新打开')
}

function handleAddTask() {
  if (!canCreateTasks.value) return
  if (!customer.value) return
  editingCustomerTask.value = null
  showTaskEditDialog.value = true
}

function handleEditCustomerTask(task: Task) {
  if (!canCreateTasks.value) return
  editingCustomerTask.value = task
  showTaskEditDialog.value = true
}

function handleCustomerTaskEditFromDetail(task: Task) {
  handleEditCustomerTask(task)
  if (isMobile.value) selectedCustomerTask.value = null
}

async function handleTaskDialogSaved() {
  editingCustomerTask.value = null
  await refreshCustomerAfterTaskMutation()
}

function syncSelectedCustomerSchedule() {
  const selectedScheduleId = selectedCustomerSchedule.value?.scheduleId
  if (!selectedScheduleId) return
  selectedCustomerSchedule.value = customerSchedules.value.find(schedule =>
    String(schedule.scheduleId) === String(selectedScheduleId)
  ) || selectedCustomerSchedule.value
}

function handleViewCustomerSchedule(schedule: ScheduleVO) {
  if (!canViewSchedules.value) return
  selectedCustomerSchedule.value = schedule
}

function handleAddSchedule() {
  if (!canCreateSchedules.value) return
  if (!customer.value) return
  editingCustomerSchedule.value = null
  showScheduleFormDialog.value = true
}

function handleEditCustomerSchedule(schedule: ScheduleVO) {
  if (!canEditSchedules.value) return
  editingCustomerSchedule.value = schedule
  showScheduleFormDialog.value = true
}

function handleEditCustomerScheduleFromDetail(schedule: ScheduleVO) {
  handleEditCustomerSchedule(schedule)
  if (isMobile.value) selectedCustomerSchedule.value = null
}

async function resolveCustomerScheduleDetail(scheduleId: string): Promise<ScheduleVO | null> {
  const normalizedScheduleId = String(scheduleId)
  const localSchedule = customerSchedules.value.find(schedule =>
    String(schedule.scheduleId) === normalizedScheduleId
  ) || null
  if (localSchedule) return localSchedule

  const result = await queryScheduleList({
    scheduleId: normalizedScheduleId,
    page: 1,
    limit: 1
  })

  return result.list?.[0] || null
}

async function handleCustomerScheduleCreated() {
  if (!customer.value) return
  editingCustomerSchedule.value = null
  await fetchCustomerSchedules(customer.value.customerId, true)
}

async function handleCustomerScheduleUpdated(scheduleId: string) {
  if (!customer.value) return
  editingCustomerSchedule.value = null
  await fetchCustomerSchedules(customer.value.customerId)
  const updatedSchedule = await resolveCustomerScheduleDetail(scheduleId)
  if (updatedSchedule) {
    selectedCustomerSchedule.value = updatedSchedule
  }
}

async function handleCustomerScheduleDeleted() {
  if (!customer.value) return
  await fetchCustomerSchedules(customer.value.customerId)
  selectedCustomerSchedule.value = null
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
.wk-customer-detail-embedded {
  background: var(--wk-bg-surface);
  color: var(--wk-text-primary);
}

.wk-customer-detail-embedded .bg-white,
.wk-customer-detail-embedded-mobile .bg-white {
  background-color: var(--wk-bg-surface) !important;
}

.wk-customer-detail-embedded .bg-slate-50,
.wk-customer-detail-embedded .bg-slate-50\/70,
.wk-customer-detail-embedded .bg-slate-100,
.wk-customer-detail-embedded-mobile .bg-slate-50,
.wk-customer-detail-embedded-mobile .bg-slate-50\/70,
.wk-customer-detail-embedded-mobile .bg-slate-100 {
  background-color: var(--wk-bg-surface-muted) !important;
}

.wk-customer-detail-embedded .border-slate-100,
.wk-customer-detail-embedded .border-slate-200,
.wk-customer-detail-embedded .border-\[\#ececec\],
.wk-customer-detail-embedded-mobile .border-slate-100,
.wk-customer-detail-embedded-mobile .border-slate-200,
.wk-customer-detail-embedded-mobile .border-\[\#ececec\] {
  border-color: var(--wk-border-subtle) !important;
}

.wk-customer-detail-embedded .shadow-sm,
.wk-customer-detail-embedded-mobile .shadow-sm {
  box-shadow: none !important;
}

.wk-customer-detail-embedded .text-slate-900,
.wk-customer-detail-embedded-mobile .text-slate-900 {
  color: var(--wk-text-primary) !important;
}

.wk-customer-detail-embedded .text-slate-700,
.wk-customer-detail-embedded .text-slate-600,
.wk-customer-detail-embedded-mobile .text-slate-700,
.wk-customer-detail-embedded-mobile .text-slate-600 {
  color: var(--wk-text-secondary) !important;
}

.wk-customer-detail-embedded .text-slate-500,
.wk-customer-detail-embedded .text-slate-400,
.wk-customer-detail-embedded-mobile .text-slate-500,
.wk-customer-detail-embedded-mobile .text-slate-400 {
  color: var(--wk-text-muted) !important;
}

.wk-customer-detail-embedded .text-primary,
.wk-customer-detail-embedded-mobile .text-primary {
  color: var(--wk-text-primary) !important;
}

.wk-customer-detail-embedded .bg-primary,
.wk-customer-detail-embedded-mobile .bg-primary {
  background-color: var(--wk-primary) !important;
}

.wk-customer-detail-embedded .bg-primary\/5,
.wk-customer-detail-embedded .bg-primary\/10,
.wk-customer-detail-embedded .bg-primary\/20,
.wk-customer-detail-embedded-mobile .bg-primary\/5,
.wk-customer-detail-embedded-mobile .bg-primary\/10,
.wk-customer-detail-embedded-mobile .bg-primary\/20 {
  background-color: var(--wk-bg-surface-hover) !important;
}

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

@media (max-width: 767.98px) {
  .wk-mobile-px-15 {
    padding-left: 15px;
    padding-right: 15px;
  }
}

.wk-customer-detail-mobile .md\:px-8 {
  padding-left: 1rem !important;
  padding-right: 1rem !important;
}

.wk-customer-detail-mobile .md\:sticky {
  position: static !important;
}

.wk-customer-detail-mobile .md\:flex-row {
  flex-direction: column !important;
}

.wk-customer-detail-mobile .md\:items-center {
  align-items: flex-start !important;
}

.wk-customer-detail-mobile .md\:justify-between {
  justify-content: flex-start !important;
}

.wk-customer-detail-mobile .md\:gap-0 {
  gap: 0.75rem !important;
}

.wk-customer-detail-mobile .md\:gap-3 {
  gap: 0.5rem !important;
}

.wk-customer-detail-mobile .md\:flex-wrap {
  flex-wrap: nowrap !important;
}

.wk-customer-detail-mobile .md\:overflow-visible {
  overflow-x: auto !important;
  overflow-y: visible !important;
}

.wk-customer-detail-mobile .md\:w-auto {
  width: 100% !important;
}

.wk-customer-detail-mobile .md\:text-xl {
  font-size: 1.125rem !important;
  line-height: 1.75rem !important;
}

.wk-customer-detail-mobile .hidden.md\:flex {
  display: none !important;
}

.wk-customer-detail-mobile .md\:hidden {
  display: flex !important;
}

.wk-customer-detail-mobile .lg\:hidden {
  display: block !important;
}

.wk-customer-detail-mobile .lg\:grid-cols-12 {
  grid-template-columns: minmax(0, 1fr) !important;
}

.wk-customer-detail-mobile .lg\:col-span-3,
.wk-customer-detail-mobile .lg\:col-span-6 {
  grid-column: auto !important;
}

.wk-customer-detail-mobile .hidden.lg\:block {
  display: none !important;
}

.wk-customer-detail-mobile .lg\:block {
  display: block;
}

.wk-customer-detail-embedded-mobile .wk-related-modules {
  display: contents !important;
}

.wk-customer-detail-embedded-mobile .wk-related-contacts {
  order: 2;
}

.wk-customer-detail-embedded-mobile .wk-customer-detail-activity {
  order: 3;
}

.wk-customer-detail-embedded-mobile .wk-related-modules > section:not(.wk-related-contacts) {
  order: 4;
}
</style>
