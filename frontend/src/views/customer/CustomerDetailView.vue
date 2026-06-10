<template>
  <div
    class="h-full flex flex-col"
    :class="{
      'wk-customer-detail-embedded': embedded,
      'wk-customer-detail-mobile': isMobile,
      'wk-customer-detail-embedded-mobile': isEmbeddedMobileLayout,
      'wk-object-detail-embedded': embedded
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
                    <span
                      v-if="customer.wecomCustomer"
                      class="inline-flex shrink-0 items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-bold text-emerald-700"
                      title="企业微信客户"
                    >
                      <span class="material-symbols-outlined text-[14px] leading-none">forum</span>
                      企微
                    </span>
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
                    >{{ enumStore.levelLabel(customer.level) }}</span>
                  </div>
                  <div class="hidden md:flex w-full min-w-0 flex-wrap items-center gap-x-3 gap-y-2 text-sm">
                    <div class="flex min-w-0 flex-wrap items-center gap-x-4 gap-y-1">
                      <div class="flex items-center gap-1 shrink-0">
                        <span class="text-slate-400">联系人:</span>
                        <span class="text-slate-600 font-medium">{{ primaryContact?.name || '-' }}</span>
                      </div>
                      <div class="flex items-center gap-1 shrink-0">
                        <span class="text-slate-400">手机:</span>
                        <span class="text-slate-600 font-medium">{{ primaryContact?.phone || '-' }}</span>
                      </div>
                      <div class="flex items-center gap-1 shrink-0">
                        <span class="text-slate-400">状态:</span>
                        <span class="text-primary font-bold">{{ getStageLabel(customer.stage) }}</span>
                      </div>
                    </div>
                    <div
                      v-if="customer.tags?.length || canEditCustomerTags"
                      class="ml-0 flex min-w-0 shrink items-center justify-start gap-1.5 overflow-hidden"
                    >
                      <span
                        v-for="tag in customerVisibleTags"
                        :key="tag.tagId"
                        class="group/tag inline-flex h-6 max-w-[88px] shrink-0 items-center gap-1 rounded-lg bg-[var(--wk-bg-surface-muted)] px-2 text-[11px] font-medium text-[var(--wk-text-secondary)]"
                        :title="tag.tagName"
                      >
                        <span class="min-w-0 truncate">{{ tag.tagName }}</span>
                        <button
                          v-if="canEditCustomerTags"
                          type="button"
                          class="hidden shrink-0 text-slate-400 transition-colors hover:text-red-500 group-hover/tag:inline-flex"
                          title="删除标签"
                          aria-label="删除标签"
                          @click.stop="handleRemoveTag(tag)"
                        >
                          <span class="material-symbols-outlined text-[12px] leading-none">close</span>
                        </button>
                      </span>
                      <el-popover
                        v-if="customerHiddenTags.length > 0"
                        trigger="hover"
                        placement="bottom-start"
                        :width="220"
                        popper-class="wk-customer-tags-popover"
                      >
                        <template #reference>
                          <span
                            class="inline-flex h-6 shrink-0 cursor-default items-center rounded-lg bg-[var(--wk-bg-surface-muted)] px-2 text-[11px] font-medium text-[var(--wk-text-muted)]"
                          >
                            +{{ customerHiddenTags.length }}
                          </span>
                        </template>
                        <div class="flex max-h-48 flex-wrap gap-1.5 overflow-y-auto">
                          <span
                            v-for="tag in customerHiddenTags"
                            :key="tag.tagId"
                            class="group/tag inline-flex max-w-full items-center gap-1 rounded-lg bg-[#f4f4f4] px-2 py-1 text-[12px] font-medium text-[#5f5f5f]"
                            :title="tag.tagName"
                          >
                            <span class="min-w-0 truncate">{{ tag.tagName }}</span>
                            <button
                              v-if="canEditCustomerTags"
                              type="button"
                              class="inline-flex shrink-0 text-slate-400 transition-colors hover:text-red-500"
                              title="删除标签"
                              aria-label="删除标签"
                              @click.stop="handleRemoveTag(tag)"
                            >
                              <span class="material-symbols-outlined text-[12px] leading-none">close</span>
                            </button>
                          </span>
                        </div>
                      </el-popover>
                      <button
                        v-if="canEditCustomerTags"
                        type="button"
                        class="inline-flex size-6 shrink-0 items-center justify-center rounded-lg border border-dashed border-[var(--wk-border-muted)] text-[var(--wk-text-primary)] transition-colors hover:bg-[var(--wk-bg-surface-hover)]"
                        title="添加标签"
                        aria-label="添加标签"
                        @click="showAddTagDialog = true"
                      >
                        <span class="material-symbols-outlined text-[15px] leading-none">add</span>
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
                  class="customer-detail-mobile-tags-row ml-0 flex w-full min-w-0 flex-nowrap items-center justify-start gap-2 overflow-hidden"
                >
                  <div class="customer-detail-mobile-tag-list flex min-w-0 flex-nowrap items-center gap-2 overflow-hidden">
                    <span
                      v-for="tag in customerVisibleTags"
                      :key="tag.tagId"
                      class="inline-flex min-w-0 items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-100 text-slate-700 group"
                      :title="tag.tagName"
                    >
                      <span class="min-w-0 truncate">{{ tag.tagName }}</span>
                      <span
                        v-if="canEditCustomerTags"
                        class="material-symbols-outlined text-xs text-slate-400 hover:text-red-500 cursor-pointer transition-colors"
                        @click.stop="handleRemoveTag(tag)"
                      >close</span>
                    </span>
                    <el-popover
                      v-if="customerHiddenTags.length > 0"
                      trigger="click"
                      placement="bottom-start"
                      :width="220"
                      popper-class="wk-customer-tags-popover"
                    >
                      <template #reference>
                        <span
                          class="inline-flex h-7 shrink-0 cursor-pointer items-center rounded-lg bg-slate-100 px-2.5 text-xs font-medium text-slate-500"
                        >
                          +{{ customerHiddenTags.length }}
                        </span>
                      </template>
                      <div class="flex max-h-48 flex-wrap gap-1.5 overflow-y-auto">
                        <span
                          v-for="tag in customerHiddenTags"
                          :key="tag.tagId"
                          class="inline-flex max-w-full items-center gap-1 rounded-lg bg-[#f4f4f4] px-2 py-1 text-[12px] font-medium text-[#5f5f5f]"
                          :title="tag.tagName"
                        >
                          <span class="min-w-0 truncate">{{ tag.tagName }}</span>
                          <button
                            v-if="canEditCustomerTags"
                            type="button"
                            class="inline-flex shrink-0 text-slate-400 transition-colors hover:text-red-500"
                            title="删除标签"
                            aria-label="删除标签"
                            @click.stop="handleRemoveTag(tag)"
                          >
                            <span class="material-symbols-outlined text-[12px] leading-none">close</span>
                          </button>
                        </span>
                      </div>
                    </el-popover>
                  </div>
                  <button
                    v-if="canEditCustomerTags"
                    type="button"
                    class="inline-flex shrink-0 items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-bold text-primary border border-dashed border-primary/30 hover:bg-primary/5 transition-colors"
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
                  <span class="material-symbols-outlined text-base leading-none">keyboard_voice</span>
                  语音识别
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
            <div class="wk-customer-stage-scroll relative overflow-visible overflow-x-auto overflow-y-visible md:overflow-visible">
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
                    <h3 class="min-w-0 text-sm font-bold leading-snug text-slate-900 break-words">
                      {{ savedAiAnalysisTitle }}
                    </h3>
                    <button
                      v-if="showAiAnalysisToggle"
                      type="button"
                      class="group/module-action relative inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 transition-[background-color,color,border-color] hover:bg-[#efefef] hover:text-[#0d0d0d]"
                      :aria-expanded="aiAnalysisExpanded"
                      :aria-label="aiAnalysisExpanded ? '收起 AI分析' : '展开 AI分析'"
                      @click="aiAnalysisExpanded = !aiAnalysisExpanded"
                    >
                      <span class="material-symbols-outlined text-[16px] leading-none">
                        {{ aiAnalysisExpanded ? 'keyboard_arrow_down' : 'keyboard_arrow_right' }}
                      </span>
                      <span
                        class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                        role="tooltip"
                      >
                        {{ aiAnalysisExpanded ? '收起 AI分析' : '展开 AI分析' }}
                      </span>
                    </button>
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

                  </div>
                </div>
              </div>

              <div v-show="isAiAnalysisVisible">
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
              <div class="flex w-full min-w-0 items-center gap-2">
                <span
                  :class="sectionIconBoxClass"
                  :style="getSectionIconStyle('recentActivity')"
                >
                  <span :class="sectionMaterialIconClass">history</span>
                </span>
                <h3 class="min-w-0 text-sm font-bold leading-snug text-slate-900 break-words">{{ isEmbeddedMobileLayout ? '活动' : '最近活动 - AI时间轴' }}</h3>
                <button
                  v-if="showFollowUpTimelineToggle"
                  type="button"
                  class="inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-white text-slate-500 transition-[background-color,color,border-color] hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                  :aria-expanded="followUpTimelineExpanded"
                  :aria-label="followUpTimelineExpanded ? '收起活动' : '展开活动'"
                  :title="followUpTimelineExpanded ? '收起活动' : '展开活动'"
                  @click="followUpTimelineExpanded = !followUpTimelineExpanded"
                >
                  <span class="material-symbols-outlined text-[16px] leading-none">
                    {{ followUpTimelineExpanded ? 'keyboard_arrow_down' : 'keyboard_arrow_right' }}
                  </span>
                </button>
                <button
                  v-if="isEmbeddedMobileLayout && canCreateFollowUps && !hideEmbeddedFollowUpAction"
                  type="button"
                  class="ml-auto inline-flex items-center gap-1.5 px-3 py-1.5 bg-primary/10 text-primary text-xs font-bold rounded-lg hover:bg-primary/20 transition-colors"
                  @click="handleAiFollowUp"
                >
                  <span class="material-symbols-outlined text-base leading-none">keyboard_voice</span>
                  语音识别
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
              v-if="isFollowUpTimelineVisible && followUps.length === 0 && !followUpLoading"
              :class="isEmbeddedMobileLayout
                ? 'rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center'
                : 'bg-white border border-slate-200 rounded-xl p-12 text-center shadow-sm'"
            >
              <span
                class="material-symbols-outlined"
                :class="isEmbeddedMobileLayout ? 'text-2xl leading-none text-slate-400' : 'mb-3 text-4xl text-slate-300'"
              >event_note</span>
              <p :class="isEmbeddedMobileLayout ? 'mt-2 text-xs font-medium text-slate-400' : 'text-sm text-slate-400'">暂无跟进记录</p>
              <p :class="isEmbeddedMobileLayout ? 'mt-1 text-xs text-slate-300' : 'text-xs text-slate-300 mt-1'">点击上方语音识别按钮添加第一条跟进记录</p>
            </div>

            <div
              v-else-if="isFollowUpTimelineVisible && followUpLoading"
              class="space-y-3"
            >
              <div
                v-for="index in 3"
                :key="`follow-up-skeleton-${index}`"
                class="rounded-xl border border-slate-200 bg-white p-3"
              >
                <div class="flex items-start gap-3">
                  <div class="size-9 shrink-0 animate-pulse rounded-xl bg-slate-100" />
                  <div class="min-w-0 flex-1 space-y-2">
                    <div class="flex items-center justify-between gap-3">
                      <div class="h-4 w-24 animate-pulse rounded-full bg-slate-100" />
                      <div class="h-3 w-20 animate-pulse rounded-full bg-slate-100" />
                    </div>
                    <div class="h-3 w-full animate-pulse rounded-full bg-slate-100" />
                    <div class="h-3 w-3/4 animate-pulse rounded-full bg-slate-100" />
                  </div>
                </div>
              </div>
            </div>

            <div v-else-if="isFollowUpTimelineVisible">
              <div
                v-for="(item, followUpIndex) in followUps"
                :key="item.followUpId"
                class="flex min-w-0 flex-col"
              >
                <!-- flex-col + 固定高度占位：16px 间距在同行 flex 高度内，避免子项 margin 不撑开行高导致卡片贴在一起。 -->
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

            <div v-if="isFollowUpTimelineVisible && followUpTotal > followUpPageSize" class="flex justify-center">
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

            <section class="group/wecom-module bg-white shadow-sm" :class="[isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : 'border border-slate-200 rounded-2xl p-4']">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="{ background: '#dcfce7', color: '#166534' }">
                    <span :class="sectionMaterialIconClass">forum</span>
                  </span>
                  企业微信关联
                </h4>
              </div>
              <div v-if="wecomBindingsLoading" class="space-y-2">
                <div v-for="index in 2" :key="`wecom-binding-skeleton-${index}`" class="h-12 animate-pulse rounded-xl bg-slate-100" />
              </div>
              <div v-else-if="wecomBindings.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
                <span class="material-symbols-outlined text-2xl leading-none text-slate-400">link_off</span>
                <p class="mt-2 text-xs font-medium text-slate-400">暂无企微关联</p>
              </div>
              <div v-else class="space-y-2">
                <div
                  v-for="binding in wecomBindings"
                  :key="binding.id"
                  class="flex min-w-0 items-center gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-2"
                >
                  <img
                    v-if="binding.externalCustomerAvatar"
                    :src="binding.externalCustomerAvatar"
                    alt=""
                    class="size-8 shrink-0 rounded-lg object-cover"
                  />
                  <span v-else class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-xs font-bold text-emerald-700">
                    {{ (binding.externalCustomerName || binding.externalUserId || '?').slice(0, 1) }}
                  </span>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-slate-800">{{ binding.externalCustomerName || binding.externalUserId }}</p>
                    <p class="truncate text-xs text-slate-400">{{ binding.bindTime ? formatDateTime(binding.bindTime) : '已关联' }}</p>
                  </div>
                </div>
              </div>
            </section>

            <section v-if="canViewTencentMeetings" class="group/tencent-module bg-white shadow-sm" :class="[isEmbeddedMobileLayout ? 'mt-5 border-t border-slate-100 pt-5' : 'border border-slate-200 rounded-2xl p-4']">
              <div class="mb-4 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="{ background: '#e0f2fe', color: '#075985' }">
                    <span :class="sectionMaterialIconClass">video_camera_front</span>
                  </span>
                  会议记录
                </h4>
                <button
                  type="button"
                  class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
                  aria-label="关联腾讯会议"
                  @click="openTencentMeetingBindingPage"
                >
                  <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add_link</span>
                  <span
                    class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
                    role="tooltip"
                  >
                    关联会议
                  </span>
                </button>
              </div>
              <div v-if="tencentMeetingsLoading" class="space-y-2">
                <div v-for="index in 2" :key="`tencent-meeting-skeleton-${index}`" class="h-14 animate-pulse rounded-xl bg-slate-100" />
              </div>
              <div v-else-if="customerTencentMeetings.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
                <span class="material-symbols-outlined text-2xl leading-none text-slate-400">videocam_off</span>
                <p class="mt-2 text-xs font-medium text-slate-400">暂无会议记录</p>
              </div>
              <div v-else class="space-y-2">
                <button
                  v-for="meeting in customerTencentMeetings.slice(0, 5)"
                  :key="meeting.id"
                  type="button"
                  class="w-full rounded-2xl border border-slate-200 bg-white px-3 py-2 text-left transition-colors hover:bg-slate-50"
                  @click="openTencentMeetingDetail(meeting.id)"
                >
                  <div class="flex min-w-0 items-start gap-2">
                    <span class="material-symbols-outlined mt-0.5 text-[18px] text-sky-600">videocam</span>
                    <div class="min-w-0 flex-1">
                      <p class="truncate text-sm font-semibold text-slate-800">{{ meeting.subject || '腾讯会议' }}</p>
                      <p class="mt-1 truncate text-xs text-slate-400">{{ formatDateTime(meeting.startTime) || '未记录时间' }} · {{ formatMeetingDuration(meeting.durationSeconds) }}</p>
                      <p v-if="meeting.summary" class="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">{{ meeting.summary }}</p>
                    </div>
                  </div>
                </button>
              </div>
            </section>

            <RelatedContactsModule
              :contacts="contacts"
              :loading="contactLoading"
              :visible="canViewContacts"
              :embedded-layout="isEmbeddedMobileLayout"
              :expanded="contactsModuleExpanded"
              :can-create="canCreateContacts"
              :can-set-primary="canSetPrimaryContacts"
              :can-create-relation="canCreateRelation"
              :total="contactTotal"
              :page="contactPage"
              :page-size="contactPageSize"
              @update:expanded="contactsModuleExpanded = $event"
              @update:page="handleContactPageChange"
              @upload-card="handleAddContactCardUpload"
              @add="handleAddContact"
              @view="handleViewContact"
              @set-primary="handleSetPrimary"
              @add-to-relation="handleAddContactToRelation"
            />

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

            <RelatedTasksModule
              :tasks="customerTasks"
              :loading="taskLoading"
              :visible="canViewTasks"
              :embedded-layout="embedded"
              :expanded="tasksModuleExpanded"
              :can-create="canCreateTasks"
              :can-toggle="canToggleTasks"
              :clickable="true"
              :selected-task-id="selectedCustomerTask?.taskId"
              @update:expanded="tasksModuleExpanded = $event"
              @add="handleAddTask"
              @view="handleViewCustomerTask"
              @toggle="handleToggleCustomerTask"
            />

            <RelatedSchedulesModule
              :schedules="customerSchedules"
              :loading="scheduleLoading"
              :visible="canViewSchedules"
              :embedded-layout="embedded"
              :expanded="schedulesModuleExpanded"
              :can-create="canCreateSchedules"
              :clickable="true"
              :selected-schedule-id="selectedCustomerSchedule?.scheduleId"
              :total="scheduleTotal"
              :page="schedulePage"
              :page-size="schedulePageSize"
              @update:expanded="schedulesModuleExpanded = $event"
              @update:page="handleSchedulePageChange"
              @add="handleAddSchedule"
              @view="handleViewCustomerSchedule"
            />

            <RelatedDocumentsModule
              :documents="customerKnowledgeList"
              :loading="customerKnowledgeLoading"
              :visible="canViewKnowledge"
              :embedded-layout="isEmbeddedMobileLayout"
              :expanded="documentsModuleExpanded"
              :can-upload="canUploadKnowledge"
              :clickable="true"
              :empty-text="getKnowledgeEmptyText()"
              @update:expanded="documentsModuleExpanded = $event"
              @upload="openCustomerKnowledgeUpload"
              @open="item => openKnowledgeDetail(item.knowledgeId)"
            />

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
      :latest-ai-report="latestAiReport"
      @contacts-updated="onBasicInfoContactsUpdated"
      @edit="handleBasicInfoEdit"
    />

    <!-- Edit Customer Dialog -->
    <CustomerUpsertDialog
      v-model="showEditDialog"
      mode="edit"
      :customer="customer"
      @success="handleEditSuccess"
    />

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
      @mutated="refreshCustomerTaskArea"
    />

    <TaskEditDialog
      v-model="showTaskEditDialog"
      :editing-task="editingCustomerTask"
      :default-customer="taskDefaultCustomer"
      :refresh-store-after-save="false"
      @saved="handleTaskDialogSaved"
    />

    <ScheduleDetailDrawer
      v-model="showCustomerScheduleDetail"
      :schedule="selectedCustomerSchedule"
      :is-mobile="isMobile && !isEmbeddedMobileLayout"
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
import { useEnumStore } from '@/stores/enums'
import { useResponsive } from '@/composables/useResponsive'
import { addCustomerTag, generateCustomerAiReport, removeCustomerTag, transferCustomer, updateCustomerStage } from '@/api/customer'
import { getCustomerWecomBindings } from '@/api/wecom'
import { getCustomerTencentMeetings } from '@/api/tencentMeeting'
import { queryTaskList } from '@/api/task'
import { queryScheduleList, type ScheduleVO } from '@/api/schedule'
import type { CustomerAiParseVO } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import { addFollowUp, deleteFollowUp, queryFollowUpPageList, updateFollowUp } from '@/api/followup'
import { deleteContact, queryContactPageList, queryContactsByCustomer, setPrimaryContact } from '@/api/contact'
import { addRelationFromContact } from '@/api/relation'
import { queryKnowledgeList } from '@/api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Knowledge, Task, TaskStatus } from '@/types/common'
import type { Contact, CustomerAiReportVO, CustomerDetailVO, CustomerTag, FollowUp, FollowUpAddBO, FollowUpAttachment, FollowUpTask, FollowUpUpdateBO } from '@/types/customer'
import type { WecomCustomerBindingVO } from '@/types/wecom'
import type { TencentMeetingVO } from '@/types/tencentMeeting'
import { compactCustomerAiInsight } from '@/utils/customerAi'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'
import FollowUpUpsertDialog from '@/components/customer/FollowUpUpsertDialog.vue'
import type { FollowUpUpsertSubmitPayload } from '@/components/customer/FollowUpUpsertDialog.vue'
import AiParseInsightSidebar from '@/components/crm/AiParseInsightSidebar.vue'
import FollowUpCard from '@/components/customer/FollowUpCard.vue'
import RelatedContactsModule from '@/components/customer/related/RelatedContactsModule.vue'
import RelatedDocumentsModule from '@/components/customer/related/RelatedDocumentsModule.vue'
import RelatedSchedulesModule from '@/components/customer/related/RelatedSchedulesModule.vue'
import RelatedTasksModule from '@/components/customer/related/RelatedTasksModule.vue'
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
import { isRequestErrorHandled } from '@/utils/requestError'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()
const taskStore = useTaskStore()
const userStore = useUserStore()
const enumStore = useEnumStore()
enumStore.ensureCustomerStage()
enumStore.ensureCustomerLevel()
const { isMobile: responsiveIsMobile } = useResponsive()

const props = withDefaults(defineProps<{
  customerId?: string
  embedded?: boolean
  forceMobile?: boolean
  hideEmbeddedFollowUpAction?: boolean
}>(), {
  customerId: '',
  embedded: false,
  forceMobile: false,
  hideEmbeddedFollowUpAction: false
})

const emit = defineEmits<{
  (e: 'quote-attachment', value: { followUp: FollowUp; attachment: FollowUpAttachment }): void
}>()

type CustomerDetailRefreshModule = 'aiAnalysis' | 'contacts' | 'followUps' | 'tasks' | 'schedules' | 'tencentMeetings'

type CustomerDetailRefreshPayload = {
  customerId?: string | number
  source?: string
  modules?: CustomerDetailRefreshModule[]
}

const activeCustomerId = computed(() => props.customerId || String(route.params.id || ''))
const embedded = computed(() => props.embedded)
const isMobile = computed(() => props.forceMobile || responsiveIsMobile.value)
const isEmbeddedMobileLayout = computed(() => props.embedded && props.forceMobile)
const hideEmbeddedFollowUpAction = computed(() => props.hideEmbeddedFollowUpAction)
const CUSTOMER_DETAIL_REQUEST_LIMIT = 100

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
const showTerminalStageMenu = ref(false)
const showTransferPopover = ref(false)
const headerMoreButtonRef = ref<HTMLElement | null>(null)
const detailTab = ref<'ai' | 'activity' | 'related'>('ai')
const newTagName = ref('')
const followUps = ref<FollowUp[]>([])
const followUpTotal = ref(0)
const followUpPage = ref(1)
const followUpPageSize = computed(() => CUSTOMER_DETAIL_REQUEST_LIMIT)
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
const contactPageSize = computed(() => CUSTOMER_DETAIL_REQUEST_LIMIT)
const contactLoading = ref(false)
const customerTasks = ref<Task[]>([])
const taskLoading = ref(false)
const customerSchedules = ref<ScheduleVO[]>([])
const scheduleTotal = ref(0)
const schedulePage = ref(1)
const schedulePageSize = computed(() => CUSTOMER_DETAIL_REQUEST_LIMIT)
const scheduleLoading = ref(false)
const customerKnowledgeList = ref<Knowledge[]>([])
const wecomBindings = ref<WecomCustomerBindingVO[]>([])
const wecomBindingsLoading = ref(false)
const customerTencentMeetings = ref<TencentMeetingVO[]>([])
const tencentMeetingsLoading = ref(false)

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
const showAiAnalysisToggle = computed(() => !!savedAiParseResult.value)
const isAiAnalysisVisible = computed(() => aiAnalysisExpanded.value || !showAiAnalysisToggle.value)
const showFollowUpTimelineToggle = computed(() => followUpLoading.value || followUps.value.length > 0)
const isFollowUpTimelineVisible = computed(() => followUpTimelineExpanded.value || !showFollowUpTimelineToggle.value)
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
const customerVisibleTags = computed(() => customer.value?.tags?.slice(0, 2) || [])
const customerHiddenTags = computed(() => customer.value?.tags?.slice(2) || [])
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
const canCreateRelation = computed(() => userStore.hasPermission('relation:create'))
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
const canViewTencentMeetings = computed(() => userStore.hasPermission('tencentMeeting:view'))
const visibleRelatedModuleCount = computed(() => [
  canViewTencentMeetings.value,
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

async function refreshCustomerAiAnalysis(customerId: string) {
  try {
    await customerStore.fetchCustomerDetail(customerId)
  } catch (err) {
    console.error('Failed to fetch customer ai analysis:', err)
  }
}

async function refreshCustomerDetailModules(
  customerId: string,
  options: { resetRelatedPages?: boolean } = {}
) {
  const resetRelatedPages = Boolean(options.resetRelatedPages)
  const fetchTasks: Promise<any>[] = [
    customerStore.fetchCustomerDetail(customerId).catch(err => {
      console.error('Failed to fetch customer detail:', err)
    })
  ]

  if (canViewFollowUps.value) {
    fetchTasks.push(fetchFollowUps(customerId, resetRelatedPages))
  } else {
    followUps.value = []
    followUpTotal.value = 0
  }

  if (canViewContacts.value) {
    fetchTasks.push(fetchContacts(customerId, resetRelatedPages))
  } else {
    contacts.value = []
    contactTotal.value = 0
  }

  if (canViewTasks.value) {
    fetchTasks.push(fetchCustomerTasks(customerId))
  } else {
    customerTasks.value = []
  }

  if (canViewKnowledge.value) {
    fetchTasks.push(fetchCustomerKnowledge(customerId))
  } else {
    customerKnowledgeList.value = []
  }

  fetchTasks.push(fetchWecomBindings(customerId))

  if (canViewTencentMeetings.value) {
    fetchTasks.push(fetchCustomerTencentMeetings(customerId))
  } else {
    customerTencentMeetings.value = []
  }

  if (canViewSchedules.value) {
    fetchTasks.push(fetchCustomerSchedules(customerId, resetRelatedPages))
  } else {
    customerSchedules.value = []
    scheduleTotal.value = 0
  }

  await Promise.all(fetchTasks)
}

async function loadCustomerDetailPage() {
  const customerId = activeCustomerId.value
  if (!customerId) return

  loading.value = true
  try {
    await refreshCustomerDetailModules(customerId)
  } finally {
    loading.value = false
  }
}

function handleCustomerDetailRefresh(payload?: CustomerDetailRefreshPayload) {
  if (payload?.source === 'customer-detail-activity') return
  const customerId = payload?.customerId ? String(payload.customerId) : ''
  if (!customerId || customerId !== String(activeCustomerId.value)) return

  if (payload?.modules?.length) {
    void refreshCustomerScopedModules(customerId, payload.modules)
    return
  }

  void refreshCustomerDetailModules(customerId, { resetRelatedPages: true })
}

function emitCustomerActivityRefresh(customerId?: string) {
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
  if (customerId) {
    appEvents.emit(APP_EVENT.CUSTOMER_DETAIL_REFRESH, {
      customerId,
      source: 'customer-detail-activity'
    })
  }
}

async function refreshCustomerScopedModules(
  customerId: string,
  modules: CustomerDetailRefreshModule[]
) {
  const uniqueModules = new Set(modules)
  const requests: Promise<any>[] = []

  if (uniqueModules.has('aiAnalysis')) {
    requests.push(refreshCustomerAiAnalysis(customerId))
  }

  if (uniqueModules.has('contacts')) {
    requests.push(fetchContacts(customerId, true))
  }

  if (uniqueModules.has('followUps')) {
    requests.push(fetchFollowUps(customerId, true))
  }

  if (uniqueModules.has('tasks')) {
    requests.push(fetchCustomerTasks(customerId))
  }

  if (uniqueModules.has('schedules')) {
    requests.push(fetchCustomerSchedules(customerId, true))
  }

  if (uniqueModules.has('tencentMeetings')) {
    requests.push(fetchCustomerTencentMeetings(customerId))
  }

  await Promise.all(requests)
}

let offCustomerDetailRefresh: (() => void) | null = null

onMounted(() => {
  void loadCustomerDetailPage()
  offCustomerDetailRefresh = appEvents.on<CustomerDetailRefreshPayload>(
    APP_EVENT.CUSTOMER_DETAIL_REFRESH,
    handleCustomerDetailRefresh
  )
})

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
  offCustomerDetailRefresh?.()
  offCustomerDetailRefresh = null
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

function syncSelectedCustomerTask() {
  const selectedTaskId = selectedCustomerTask.value?.taskId
  if (!selectedTaskId) return
  selectedCustomerTask.value = customerTasks.value.find(task =>
    String(task.taskId) === String(selectedTaskId)
  ) || selectedCustomerTask.value
}

async function fetchCustomerTasks(customerId: string) {
  if (!canViewTasks.value) {
    customerTasks.value = []
    return
  }
  taskLoading.value = true
  try {
    const result = await queryTaskList({
      customerId,
      page: 1,
      limit: CUSTOMER_DETAIL_REQUEST_LIMIT
    })
    customerTasks.value = result.list || []
  } catch (error) {
    console.error('Failed to fetch customer tasks:', error)
    customerTasks.value = []
  } finally {
    syncSelectedCustomerTask()
    taskLoading.value = false
  }
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
      limit: CUSTOMER_DETAIL_REQUEST_LIMIT
    })
    customerKnowledgeList.value = result.list || []
  } catch (error) {
    console.error('Failed to fetch customer knowledge:', error)
    customerKnowledgeList.value = []
  } finally {
    customerKnowledgeLoading.value = false
  }
}

async function fetchWecomBindings(customerId: string) {
  wecomBindingsLoading.value = true
  try {
    wecomBindings.value = await getCustomerWecomBindings(customerId)
  } catch (error) {
    console.error('Failed to fetch WeCom bindings:', error)
    wecomBindings.value = []
  } finally {
    wecomBindingsLoading.value = false
  }
}

async function fetchCustomerTencentMeetings(customerId: string) {
  if (!canViewTencentMeetings.value) {
    customerTencentMeetings.value = []
    return
  }
  tencentMeetingsLoading.value = true
  try {
    customerTencentMeetings.value = await getCustomerTencentMeetings(customerId)
  } catch (error) {
    console.error('Failed to fetch Tencent meetings:', error)
    customerTencentMeetings.value = []
  } finally {
    tencentMeetingsLoading.value = false
  }
}

function openTencentMeetingBindingPage() {
  if (!customer.value) return
  router.push({
    path: '/tencent-meetings',
    query: {
      bindCustomerId: customer.value.customerId,
      bindStatus: 'UNBOUND'
    }
  })
}

function openTencentMeetingDetail(meetingId: string | number) {
  router.push({
    path: '/tencent-meetings',
    query: {
      customerId: customer.value?.customerId || '',
      meetingId: String(meetingId)
    }
  })
}

function formatMeetingDuration(seconds?: number) {
  if (!seconds) return '未记录时长'
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours > 0) return `${hours}小时${rest}分`
  return `${Math.max(minutes, 1)}分钟`
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

function handleEdit() {
  if (!canEditCustomer.value) return
  showEditDialog.value = true
}

function handleBasicInfoEdit() {
  showBasicInfoDrawer.value = false
  handleEdit()
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

async function handleEditSuccess(payload: { mode: 'create' | 'edit'; customerId?: string; detail?: CustomerDetailVO | null }) {
  if (payload.mode !== 'edit') return
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
  if (!payload.detail && customer.value?.customerId) {
    await customerStore.fetchCustomerDetail(customer.value.customerId)
  }
}

async function handleDeleteCustomer() {
  if (!canDeleteCustomer.value) return
  if (!customer.value) return
  try {
    await customerStore.removeCustomer(customer.value.customerId)
    appEvents.emit(APP_EVENT.CUSTOMER_SIDEBAR_REFRESH)
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

defineExpose({
  openAiFollowUp: handleAiFollowUp
})

async function handleAiFollowUpSaved() {
  if (!customer.value) return
  await refreshFollowUpContext(customer.value.customerId, { resetFollowUps: true })
  emitCustomerActivityRefresh(customer.value.customerId)
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

async function handleAddContactToRelation(contact: Contact) {
  if (!canCreateRelation.value) return
  try {
    await addRelationFromContact(contact.contactId)
    ElMessage.success('已添加到关系')
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('添加到关系失败')
    }
  }
}

async function refreshCustomerTaskArea() {
  if (!customer.value) return
  await fetchCustomerTasks(customer.value.customerId)
  const id = selectedCustomerTask.value?.taskId
  if (id) {
    selectedCustomerTask.value = customerTasks.value.find((t: Task) => String(t.taskId) === String(id)) || null
  }
}

async function handleToggleCustomerTask(task: Task) {
  if (!canToggleTasks.value) return
  const newStatus: TaskStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  await taskStore.changeTaskStatus(task.taskId, newStatus)
  await refreshCustomerTaskArea()
}

function handleViewCustomerTask(task: Task) {
  if (!canViewTasks.value) return
  selectedCustomerTask.value = task
}

async function resolveCustomerTaskDetail(taskId: string): Promise<Task | null> {
  const normalizedTaskId = String(taskId)
  const localTask = customerTasks.value.find((task: Task) => String(task.taskId) === normalizedTaskId) || null
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
  await refreshCustomerTaskArea()
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
  await refreshCustomerTaskArea()
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
    emitCustomerActivityRefresh(payload.customerId)
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
    if (customer.value) {
      await refreshFollowUpContext(customer.value.customerId)
      emitCustomerActivityRefresh(customer.value.customerId)
    }
    ElMessage.success('跟进记录已删除')
  } catch { /* Error handled */ }
}

function getStageLabel(stage: string): string {
  if (!stage) return stage
  // 真相源：/enum/customerStage（回退内置短标签）
  const label = enumStore.stageLabel(stage)
  if (label && label !== stage) return label
  const labels: Record<string, string> = {
    lead: '线索', qualified: '已验证', proposal: '方案',
    negotiation: '谈判', closed: '成交', lost: '流失'
  }
  return labels[stage] || label || stage
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
const STAGE_OPTIONS_FALLBACK = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
]
// 真相源：/enum/customerStage（未加载时回退内置）
const stageOptions = computed(() =>
  enumStore.customerStage.length
    ? enumStore.customerStage.map(o => ({ value: o.value, label: o.label }))
    : STAGE_OPTIONS_FALLBACK
)
const STEPPER_SEGMENT_WIDTH = 180
const STEPPER_SEGMENT_HEIGHT = 32
const STEPPER_CHEVRON_SIZE = 12
const STEPPER_END_RADIUS = STEPPER_SEGMENT_HEIGHT / 2

function getStageLabelFull(stage: string): string {
  return stageOptions.value.find(s => s.value === stage)?.label || stage
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

.wk-customer-stage-scroll {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.wk-customer-stage-scroll::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
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
