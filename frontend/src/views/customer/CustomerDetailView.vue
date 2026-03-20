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
            <span class="material-symbols-outlined text-sm">group</span>
            客户管理
          </button>
          <span class="material-symbols-outlined text-xs">chevron_right</span>
          <span class="text-slate-900 font-medium">客户详情</span>
        </div>

        <!-- Customer Info Card -->
        <div class="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4 min-w-0">
              <div class="size-14 bg-slate-100 rounded-lg flex items-center justify-center border border-slate-200 overflow-hidden shrink-0">
                <span class="text-2xl font-bold text-slate-400">{{ customer.companyName?.charAt(0) || '?' }}</span>
              </div>
              <div class="min-w-0 space-y-1">
                <div class="flex items-center gap-3 flex-wrap">
                  <h2 class="text-xl font-bold text-slate-900 truncate">{{ customer.companyName }}</h2>
                  <span
                    class="px-2 py-0.5 text-xs font-bold rounded uppercase"
                    :class="getStageBadgeClass(customer.stage)"
                  >{{ getStageLabel(customer.stage) }}</span>
                  <span v-if="customer.level"
                    class="px-2 py-0.5 text-xs font-bold rounded"
                    :class="{
                      'bg-emerald-100 text-emerald-700': customer.level === 'A',
                      'bg-blue-100 text-blue-700': customer.level === 'B',
                      'bg-slate-100 text-slate-600': customer.level === 'C'
                    }"
                  >{{ customer.level }}级客户</span>
                </div>
                <div class="flex items-center gap-4 text-sm flex-wrap">
                  <p class="text-slate-500">{{ customer.level ? customer.level + '级' : '普通' }}客户 · {{ customer.industry || '未分类' }}</p>
                  <div class="h-3 w-px bg-slate-200 hidden sm:block"></div>
                  <div class="flex items-center gap-4">
                    <div class="flex items-center gap-1">
                      <span class="text-slate-400">联系人:</span>
                      <span class="text-slate-600 font-medium">{{ primaryContact?.name || '-' }}</span>
                    </div>
                    <div class="flex items-center gap-1">
                      <span class="text-slate-400">手机:</span>
                      <span class="text-slate-600 font-mono font-medium">{{ primaryContact?.phone || '-' }}</span>
                    </div>
                    <div class="flex items-center gap-1">
                      <span class="text-slate-400">状态:</span>
                      <span class="text-primary font-bold">{{ getStageLabel(customer.stage) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex gap-2 shrink-0">
              <button class="h-8 px-4 inline-flex items-center border border-slate-200 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors" @click="handleEdit">编辑资料</button>
              <button class="h-8 px-4 bg-primary/10 text-primary border border-primary/20 rounded-lg text-sm font-bold flex items-center gap-1.5 hover:bg-primary/20 transition-colors" @click="handleAiFollowUp">
                <WkIcon name="ai" class="text-sm" />
                AI 跟进
              </button>
              <button class="h-8 px-4 bg-primary text-white rounded-lg text-sm font-bold shadow-lg shadow-primary/20 flex items-center gap-1.5 hover:bg-primary/90 transition-colors" @click="handleGenerateReport">
                <WkIcon name="ai" class="text-sm" />
                生成 AI 分析报告
              </button>
              <button
                class="size-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                title="删除客户"
                type="button"
                @click="handleDeleteCustomerConfirm"
              >
                <span class="material-symbols-outlined text-base">delete</span>
              </button>
            </div>
          </div>

          <!-- Stage Stepper (inside same card) -->
          <div class="mt-5 pt-4 border-t border-slate-100">
            <div class="flex items-center gap-2 mb-6">
              <span :class="sectionIconBoxClass" :style="getSectionIconStyle('customerStage')">
                <WkIcon name="stage" :size="15" />
              </span>
              <h3 class="text-sm font-bold text-slate-900">客户阶段</h3>
            </div>
            <div class="relative overflow-x-auto">
              <!-- Chevron segments (fixed width) -->
              <div class="relative flex h-9 w-max min-w-[1080px] items-stretch">
                <div
                  v-for="(stage, idx) in stageFlow"
                  :key="stage"
                  class="relative h-9 flex-none w-[180px] group cursor-pointer"
                  @click="handleStageChange(stage)"
                  :title="getStepperLabel(stage)"
                  :style="{
                    marginLeft: idx === 0 ? '0px' : `-${STEPPER_CHEVRON_SIZE - STEPPER_SEGMENT_GAP}px`,
                    zIndex: customer.stage === stage ? 10 : 5 - idx
                  }"
                >
                  <!-- Segment background (clip-path chevron) -->
                  <div
                    class="absolute inset-0 transition-all duration-300"
                    :class="getStepperSegmentBgClass(stage, idx)"
                    :style="{ clipPath: getStepperClipPath(idx) }"
                  ></div>
                  <!-- Hover overlay -->
                  <div
                    class="absolute inset-0 opacity-0 transition-opacity duration-200 group-hover:opacity-100"
                    :class="getStepperHoverOverlayClass(stage, idx)"
                    :style="{ clipPath: getStepperClipPath(idx) }"
                  ></div>

                  <!-- Content -->
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
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3-Column Content -->
      <div class="flex-1 overflow-auto p-4 md:p-8">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <!-- Left Column: Basic Info (col-span-3) -->
          <div class="lg:col-span-3 space-y-6">
            <!-- Basic Info -->
            <section class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 class="text-sm font-bold text-slate-900 mb-6 flex items-center gap-2">
                <span :class="sectionIconBoxClass" :style="getSectionIconStyle('basicInfo')">
                  <WkIcon name="profile" :size="15" />
                </span>
                基本信息
              </h3>
              <div class="space-y-5">
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">公司全称</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.companyName }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">所属行业</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.industry || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户来源</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.source || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户级别</p>
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold"
                    :class="{
                      'bg-emerald-50 text-emerald-600': customer.level === 'A',
                      'bg-blue-50 text-blue-600': customer.level === 'B',
                      'bg-slate-100 text-slate-500': customer.level === 'C'
                    }"
                  >{{ customer.level }}级</span>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">主要联系人</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ primaryContact?.name || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">联系电话</p>
                  <p class="text-sm text-slate-900 font-medium font-mono px-2 py-1 -ml-2 truncate">{{ primaryContact?.phone || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">电子邮箱</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ primaryContact?.email || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户地址</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.address || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">公司网站</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.website || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">负责人</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.ownerName || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">创建人</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.ownerName || '-' }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">创建时间</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2">{{ formatDate(customer.createTime) }}</p>
                </div>
                <div>
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户背景</p>
                  <p class="text-sm text-slate-600 leading-relaxed px-2 py-1 -ml-2">{{ customer.description || '暂无描述' }}</p>
                </div>
              </div>
            </section>

            <!-- Tags -->
            <section class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 class="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
                <span :class="sectionIconBoxClass" :style="getSectionIconStyle('tags')">
                  <span :class="sectionMaterialIconClass">sell</span>
                </span>
                标签
              </h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="tag in customer.tags"
                  :key="tag.tagId"
                  class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-medium bg-slate-100 text-slate-700 group"
                >
                  {{ tag.tagName }}
                  <span
                    class="material-symbols-outlined text-xs text-slate-400 hover:text-red-500 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity"
                    @click="handleRemoveTag(tag)"
                  >close</span>
                </span>
                <button class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-bold text-primary border border-dashed border-primary/30 hover:bg-primary/5 transition-colors" @click="showAddTagDialog = true">
                  <span class="wk-plus-button-mark" aria-hidden="true">+</span>
                  <span>添加标签</span>
                </button>
              </div>
            </section>

            <!-- Custom Fields -->
            <section v-if="customFields.length > 0" class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 class="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
                <span :class="sectionIconBoxClass" :style="getSectionIconStyle('customFields')">
                  <span :class="sectionMaterialIconClass">tune</span>
                </span>
                扩展信息
              </h3>
              <div class="space-y-5">
                <div v-for="field in customFields" :key="field.fieldId">
                  <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">{{ field.fieldLabel }}</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2">{{ formatCustomFieldValue(field, customer.customFields?.[field.fieldName]) }}</p>
                </div>
              </div>
            </section>
          </div>

          <!-- Center Column: Follow-ups Timeline (col-span-6) -->
          <div class="lg:col-span-6 space-y-6">
            <div class="flex items-center justify-between">
              <h3 class="flex items-center gap-2 text-lg font-bold text-slate-900">
                <span :class="sectionIconBoxClass" :style="getSectionIconStyle('recentActivity')">
                  <span :class="sectionMaterialIconClass">history</span>
                </span>
                最近活动 - 智能跟进时间轴
              </h3>
              <div class="flex items-center gap-3">
                <!-- <div class="flex bg-slate-100 p-1 rounded-lg">
                  <button class="px-3 py-1 text-xs font-bold rounded bg-white shadow-sm">全部</button>
                  <button class="px-3 py-1 text-xs font-medium text-slate-500">会议摘要</button>
                  <button class="px-3 py-1 text-xs font-medium text-slate-500">重要进展</button>
                </div>
                <button
                  class="px-4 py-2 bg-primary/10 text-primary border border-primary/20 rounded-lg text-sm font-bold flex items-center gap-2 hover:bg-primary/20 transition-colors"
                  @click="handleOpenFollowUpDialog"
                >
                  <span class="material-symbols-outlined text-sm">add</span>
                  添加跟进
                </button> -->
              </div>
            </div>

            <div v-if="followUps.length === 0 && !followUpLoading" class="bg-white border border-slate-200 rounded-xl p-12 text-center shadow-sm">
              <span class="material-symbols-outlined text-slate-300 text-4xl mb-3">event_note</span>
              <p class="text-sm text-slate-400">暂无跟进记录</p>
              <p class="text-xs text-slate-300 mt-1">点击上方按钮添加第一条跟进记录</p>
            </div>

            <div v-else class="space-y-6" v-loading="followUpLoading">
              <div v-for="item in followUps" :key="item.followUpId" class="relative pl-8 before:absolute before:left-0 before:top-0 before:bottom-0 before:w-px before:bg-slate-200">
                <div class="absolute left-0 top-1 -translate-x-1/2 size-4 rounded-full bg-white border-2 border-primary"></div>
                <div class="bg-white border border-slate-200 rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
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
                      <el-popconfirm title="确定删除这条跟进记录吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDeleteFollowUp(item.followUpId)">
                        <template #reference>
                          <button class="text-slate-300 hover:text-red-500 transition-colors">
                            <span class="material-symbols-outlined text-sm">delete</span>
                          </button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </div>
                  <div v-if="item.nextFollowTime" class="flex items-center gap-2 mb-3 text-xs font-bold text-amber-600 bg-amber-50 px-2 py-1 rounded-lg w-fit">
                    <span class="material-symbols-outlined text-xs">event_repeat</span>
                    下次联系: {{ formatDateTime(item.nextFollowTime) }}
                  </div>
                  <p class="text-sm text-slate-600 leading-relaxed">{{ item.content }}</p>
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
          <div class="lg:col-span-3 space-y-6">
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
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm p-6" v-loading="contactLoading">
              <div class="mb-6 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('relatedContacts')">
                    <span :class="sectionMaterialIconClass">group</span>
                  </span>
                  关联联系人
                  <span class="text-slate-400 font-normal">({{ contactTotal }})</span>
                </h4>
                <button class="size-6 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all" @click="handleAddContact">
                  <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">person_add</span>
                </button>
              </div>
              <div class="space-y-3">
                <div v-if="contacts.length === 0" class="py-6 text-center">
                  <p class="text-xs text-slate-400">暂无联系人</p>
                </div>
                <div
                  v-for="contact in contacts"
                  :key="contact.contactId"
                  class="p-3 bg-white border border-slate-100 rounded-xl group hover:border-primary/30 hover:shadow-sm transition-all cursor-pointer"
                  @click="handleViewContact(contact)"
                >
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-2">
                      <div class="size-8 rounded-lg bg-slate-100 flex items-center justify-center text-xs font-bold text-slate-500 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                        {{ contact.name?.charAt(0) }}
                      </div>
                      <div>
                        <h5 class="text-xs font-bold text-slate-900 truncate max-w-[120px]">{{ contact.name }}</h5>
                        <p class="text-xs text-slate-400 truncate max-w-[120px]">{{ contact.position || '联系人' }}</p>
                      </div>
                    </div>
                    <span v-if="contact.isPrimary" class="px-1.5 py-0.5 bg-primary/10 text-primary text-xs font-black rounded uppercase">主要</span>
                  </div>
                  <div class="flex items-center gap-3 text-xs text-slate-500">
                    <div v-if="contact.phone" class="flex items-center gap-1">
                      <span class="material-symbols-outlined text-xs">call</span>
                      <span class="font-mono">{{ contact.phone }}</span>
                    </div>
                    <div v-if="contact.email" class="flex items-center gap-1">
                      <span class="material-symbols-outlined text-xs">mail</span>
                      <span class="truncate max-w-[80px]">{{ contact.email }}</span>
                    </div>
                  </div>
                  <div class="flex items-center gap-1.5 mt-2 pt-2 border-t border-slate-50" @click.stop>
                    <button class="text-xs font-bold text-primary hover:underline" @click="handleEditContact(contact)">编辑</button>
                    <span class="text-slate-200">|</span>
                    <button v-if="!contact.isPrimary" class="text-xs font-bold text-slate-400 hover:text-primary" @click="handleSetPrimary(contact.contactId)">设为主要</button>
                    <span v-if="!contact.isPrimary" class="text-slate-200">|</span>
                    <el-popconfirm title="确定删除该联系人吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDeleteContact(contact.contactId)">
                      <template #reference>
                        <button class="text-xs font-bold text-red-400 hover:text-red-500">删除</button>
                      </template>
                    </el-popconfirm>
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
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm p-6">
              <div class="mb-6 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('todoTasks')">
                    <WkIcon name="task" :size="15" />
                  </span>
                  待办任务
                </h4>
                <button class="size-6 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all" @click="handleAddTask">
                  <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add</span>
                </button>
              </div>
              <div class="space-y-3">
                <div v-if="!customer.tasks?.length" class="py-6 text-center">
                  <p class="text-xs text-slate-400">暂无待办任务</p>
                </div>
                <div
                  v-for="task in customer.tasks?.slice(0, 5)"
                  :key="task.taskId"
                  class="flex items-start gap-3 p-3 bg-white border border-slate-100 rounded-xl hover:shadow-sm transition-all"
                >
                  <div class="flex-1 min-w-0">
                    <p class="text-xs font-bold text-slate-900 truncate">{{ task.title }}</p>
                    <div class="flex items-center gap-2 mt-1">
                      <span class="text-xs font-bold uppercase" :class="task.dueDate && isOverdue(task.dueDate) ? 'text-red-500' : 'text-slate-400'">
                        {{ task.dueDate ? formatDate(task.dueDate) : '无截止日期' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- Documents Module -->
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm p-6">
              <div class="mb-6 flex items-center justify-between">
                <h4 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('documentCenter')">
                    <WkIcon name="knowledge" :size="15" />
                  </span>
                  文档中心
                </h4>
              </div>
              <div>
                <p class="py-6 text-center text-xs text-slate-400">暂无文档</p>
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

    <!-- Add Follow-up Dialog -->
    <el-dialog v-model="showAddFollowUpDialog" title="添加跟进记录" :width="isMobile ? '95%' : '500px'" :fullscreen="isMobile">
      <el-form :model="followUpForm" label-width="80px">
        <el-form-item label="跟进类型">
          <el-select v-model="followUpForm.type" class="w-full">
            <el-option label="电话" value="call" />
            <el-option label="会议" value="meeting" />
            <el-option label="邮件" value="email" />
            <el-option label="拜访" value="visit" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进时间">
          <el-date-picker v-model="followUpForm.followTime" type="datetime" class="w-full" placeholder="选择跟进时间" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="followUpForm.content" type="textarea" :rows="4" placeholder="请输入跟进内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddFollowUpDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitFollowUp">添加</el-button>
      </template>
    </el-dialog>

    <!-- Add/Edit Contact Dialog -->
    <el-dialog v-model="showAddContactDialog" :title="editingContact ? '编辑联系人' : '添加联系人'" :width="isMobile ? '95%' : '500px'" :fullscreen="isMobile">
      <el-form :model="contactForm" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="contactForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="contactForm.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="contactForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="contactForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="微信">
          <el-input v-model="contactForm.wechat" placeholder="请输入微信号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contactForm.notes" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="主联系人">
          <el-switch v-model="contactForm.isPrimary" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddContactDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitContact">{{ editingContact ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- Contact Detail Drawer -->
    <el-drawer v-model="showContactDetail" title="联系人详情" :size="isMobile ? '100%' : '400px'">
      <template v-if="currentContact">
        <div class="space-y-4">
          <div class="flex items-center gap-3 mb-6">
            <div class="size-14 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold text-xl">{{ currentContact.name?.charAt(0) }}</div>
            <div>
              <h3 class="font-bold text-lg text-slate-900">{{ currentContact.name }}</h3>
              <p v-if="currentContact.position" class="text-sm text-slate-500">{{ currentContact.position }}</p>
              <span v-if="currentContact.isPrimary" class="text-xs font-bold px-2 py-0.5 bg-emerald-50 text-emerald-600 rounded">主联系人</span>
            </div>
          </div>
          <div class="space-y-3">
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-xs text-slate-400 uppercase font-bold mb-1">电话</p>
              <p class="text-sm text-slate-900">{{ currentContact.phone || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-xs text-slate-400 uppercase font-bold mb-1">邮箱</p>
              <p class="text-sm text-slate-900">{{ currentContact.email || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-xs text-slate-400 uppercase font-bold mb-1">微信</p>
              <p class="text-sm text-slate-900">{{ currentContact.wechat || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-xs text-slate-400 uppercase font-bold mb-1">备注</p>
              <p class="text-sm text-slate-900">{{ currentContact.notes || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-xs text-slate-400 uppercase font-bold mb-1">创建时间</p>
              <p class="text-sm text-slate-900">{{ currentContact.createTime ? formatDateTime(currentContact.createTime) : '-' }}</p>
            </div>
          </div>
          <div class="mt-6 flex gap-2">
            <button class="px-4 py-2 bg-primary text-white rounded-xl text-xs font-bold hover:bg-primary/90 transition-all" @click="handleEditContact(currentContact); showContactDetail = false">编辑</button>
            <button v-if="!currentContact.isPrimary" class="px-4 py-2 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-700 hover:bg-slate-50 transition-all" @click="handleSetPrimary(currentContact.contactId); showContactDetail = false">设为主联系人</button>
            <el-popconfirm title="确定删除该联系人吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDeleteContact(currentContact!.contactId); showContactDetail = false">
              <template #reference>
                <button class="px-4 py-2 bg-white border border-red-200 rounded-xl text-xs font-bold text-red-500 hover:bg-red-50 transition-all">删除</button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </template>
    </el-drawer>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useResponsive } from '@/composables/useResponsive'
import { addCustomerTag, removeCustomerTag, updateCustomerStage } from '@/api/customer'
import { addFollowUp, deleteFollowUp, queryFollowUpPageList } from '@/api/followup'
import { addContact, updateContact, deleteContact, setPrimaryContact, queryContactPageList } from '@/api/contact'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CustomerTag, FollowUp, Contact } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()
const { isMobile } = useResponsive()

const loading = ref(false)
const submitting = ref(false)
const showAddTagDialog = ref(false)
const showAddFollowUpDialog = ref(false)
const showAddContactDialog = ref(false)
const showContactDetail = ref(false)
const currentContact = ref<Contact | null>(null)
const editingContact = ref<Contact | null>(null)
const showEditDialog = ref(false)
const showAiFollowUpDrawer = ref(false)
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

const sectionIconBoxClass = 'inline-flex size-7 shrink-0 items-center justify-center rounded-lg text-white shadow-[0_8px_18px_rgba(15,23,42,0.08)]'
const sectionMaterialIconClass = 'material-symbols-outlined text-[16px] leading-none'
const sectionIconBgColors = {
  customerStage: '#0052CC',
  basicInfo: '#5243AA',
  tags: '#DE350B',
  customFields: '#00875A',
  recentActivity: '#FF991F',
  relatedBusiness: '#00A3BF',
  relatedContacts: '#DE350B',
  todoTasks: '#00875A',
  documentCenter: '#0052CC',
} as const

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

async function refreshCustomerContext(customerId: string, options: { resetContacts?: boolean } = {}) {
  await Promise.all([
    customerStore.fetchCustomerDetail(customerId),
    fetchContacts(customerId, options.resetContacts)
  ])
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


function formatDateForApi(date: Date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const followUpForm = reactive({
  customerId: '',
  type: 'call',
  content: '',
  followTime: formatDateForApi()
})

const contactForm = reactive({
  customerId: '',
  name: '',
  position: '',
  phone: '',
  email: '',
  wechat: '',
  notes: '',
  isPrimary: false
})

const customer = computed(() => customerStore.currentCustomer)

onMounted(async () => {
  const customerId = route.params.id as string
  if (customerId) {
    loading.value = true
    followUpForm.customerId = customerId
    contactForm.customerId = customerId

    const fetchTasks = [
      customerStore.fetchCustomerDetail(customerId).catch(err => {
        console.error('Failed to fetch customer detail:', err)
      }),
      fetchFollowUps(customerId),
      fetchContacts(customerId),
      getEnabledFieldsByEntity('customer').then(data => {
        customFields.value = data
      }).catch(err => {
        console.error('Failed to fetch custom fields:', err)
        customFields.value = []
      })
    ]

    await Promise.all(fetchTasks)
    loading.value = false
  }
})

async function fetchFollowUps(customerId: string, reset = false) {
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
    contactLoading.value = false
  }
}

function handleContactPageChange(page: number) {
  contactPage.value = page
  if (customer.value) fetchContacts(customer.value.customerId)
}

function handleEdit() {
  showEditDialog.value = true
}

async function handleEditSuccess(payload: { mode: 'create' | 'edit'; customerId?: string }) {
  if (payload.mode !== 'edit') return
  if (customer.value?.customerId) {
    await customerStore.fetchCustomerDetail(customer.value.customerId)
  }
}

async function handleDeleteCustomer() {
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
  showAiFollowUpDrawer.value = true
}

function handleAiFollowUpSaved() {
  if (customer.value) fetchFollowUps(customer.value.customerId, true)
}

function handleGenerateReport() {
  ElMessage.info('AI 分析报告功能开发中')
}

function resetContactForm() {
  contactForm.name = ''
  contactForm.position = ''
  contactForm.phone = ''
  contactForm.email = ''
  contactForm.wechat = ''
  contactForm.notes = ''
  contactForm.isPrimary = false
}

function handleAddContact() {
  if (customer.value) {
    editingContact.value = null
    contactForm.customerId = customer.value.customerId
    resetContactForm()
    showAddContactDialog.value = true
  }
}

function handleViewContact(contact: Contact) {
  currentContact.value = contact
  showContactDetail.value = true
}

function handleEditContact(contact: Contact) {
  editingContact.value = contact
  contactForm.customerId = contact.customerId
  contactForm.name = contact.name || ''
  contactForm.position = contact.position || ''
  contactForm.phone = contact.phone || ''
  contactForm.email = contact.email || ''
  contactForm.wechat = contact.wechat || ''
  contactForm.notes = contact.notes || ''
  contactForm.isPrimary = isPrimaryContact(contact)
  showAddContactDialog.value = true
}

async function handleDeleteContact(contactId: string) {
  if (!customer.value) return
  try {
    await deleteContact(contactId)
    await refreshCustomerContext(customer.value.customerId)
    ElMessage.success('联系人已删除')
  } catch { /* Error handled */ }
}

async function handleSetPrimary(contactId: string) {
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

function handleAddTask() {
  router.push('/task')
}

async function handleAddTag() {
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
  if (!customer.value) return
  try {
    await removeCustomerTag(customer.value.customerId, tag.tagId)
    await customerStore.fetchCustomerDetail(customer.value.customerId)
    ElMessage.success('标签已删除')
  } catch { /* Error handled */ }
}

async function handleSubmitFollowUp() {
  if (!followUpForm.content.trim()) {
    ElMessage.warning('请输入跟进内容')
    return
  }
  if (!followUpForm.followTime) {
    ElMessage.warning('请选择跟进时间')
    return
  }
  submitting.value = true
  try {
    await addFollowUp({ customerId: followUpForm.customerId, type: followUpForm.type, content: followUpForm.content, followTime: followUpForm.followTime } as any)
    await fetchFollowUps(followUpForm.customerId, true)
    showAddFollowUpDialog.value = false
    followUpForm.content = ''
    followUpForm.followTime = formatDateForApi()
    ElMessage.success('跟进记录添加成功')
  } catch { /* Error handled */ } finally {
    submitting.value = false
  }
}

async function handleDeleteFollowUp(followUpId: string) {
  try {
    await deleteFollowUp(followUpId)
    if (customer.value) await fetchFollowUps(customer.value.customerId)
    ElMessage.success('跟进记录已删除')
  } catch { /* Error handled */ }
}

async function handleSubmitContact() {
  if (!contactForm.name.trim()) {
    ElMessage.warning('请输入联系人姓名')
    return
  }
  // 若勾选主联系人且已有主联系人，弹窗确认是否替换
  if (contactForm.isPrimary) {
    const existingPrimary = contacts.value.find(contact => isPrimaryContact(contact))
    if (existingPrimary && existingPrimary.contactId !== editingContact.value?.contactId) {
      try {
        await ElMessageBox.confirm(
          `当前主要联系人为「${existingPrimary.name}」，是否替换为新联系人？`,
          '替换主要联系人',
          { type: 'warning', confirmButtonText: '确定替换', cancelButtonText: '取消' }
        )
      } catch {
        return
      }
    }
  }
  submitting.value = true
  try {
    const submitData = {
      customerId: contactForm.customerId,
      name: contactForm.name,
      position: contactForm.position,
      phone: contactForm.phone,
      email: contactForm.email,
      wechat: contactForm.wechat,
      notes: contactForm.notes,
      isPrimary: contactForm.isPrimary ? 1 : 0
    }
    if (editingContact.value) {
      await updateContact({ ...submitData, contactId: editingContact.value.contactId } as any)
      ElMessage.success('联系人更新成功')
    } else {
      await addContact(submitData as any)
      ElMessage.success('联系人添加成功')
    }
    await refreshCustomerContext(contactForm.customerId, { resetContacts: !editingContact.value })
    showAddContactDialog.value = false
    editingContact.value = null
    resetContactForm()
  } catch { /* Error handled */ } finally {
    submitting.value = false
  }
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
  const stages = ['lead', 'qualified', 'proposal', 'negotiation', 'closed', 'lost']
  return stages.indexOf(stage)
}

const stageFlow = ['lead', 'qualified', 'proposal', 'negotiation', 'closed', 'lost']
const stageOptions = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '结单' },
  { value: 'lost', label: '已流失' }
]
const STEPPER_SEGMENT_WIDTH = 180
const STEPPER_SEGMENT_HEIGHT = 36
const STEPPER_CHEVRON_SIZE = 12
const STEPPER_SEGMENT_GAP = 8
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
    lost: 'bg-gradient-to-r from-[#64748b] to-[#475569] shadow-[0_8px_20px_rgba(71,85,105,0.16)]',
    pending: 'bg-[#d9d9d9]'
  }
  return classes[state]
}

function getStepperStageIcon(stage: string): string {
  // Keep icon mapping purely presentational
  const icons: Record<string, string> = {
    lead: 'person_search',
    qualified: 'verified',
    proposal: 'description',
    negotiation: 'forum',
    closed: 'handshake',
    lost: 'block'
  }
  return icons[stage] || 'lens'
}

function getStepperLabel(stage: string): string {
  if (stage === 'lost') return '已流失'
  if (stage === 'closed') {
    const cs = customer.value?.stage
    if (cs === 'closed') return '已成交'
    return '结单'
  }
  return getStageLabelFull(stage)
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

  if (stage === 'lost' && cs === 'lost') return 'lost'
  if (stage === 'closed' && cs === 'closed') return 'closed'

  const isCompleted = getStageIndex(cs) > idx && cs !== 'lost'
  if (cs === stage) return 'current'
  if (isCompleted) return 'completed'
  return 'pending'
}

async function handleStageChange(newStage: string) {
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

function isOverdue(dateStr: string): boolean {
  return new Date(dateStr) < new Date()
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

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
}
</script>
