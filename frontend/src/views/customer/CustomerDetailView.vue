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
                    class="px-2 py-0.5 text-[10px] font-bold rounded uppercase"
                    :class="getStageBadgeClass(customer.stage)"
                  >{{ getStageLabel(customer.stage) }}</span>
                  <span v-if="customer.level"
                    class="px-2 py-0.5 text-[10px] font-bold rounded"
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
                      <span class="text-slate-600 font-medium">{{ primaryContact?.name || '未设置' }}</span>
                    </div>
                    <div class="flex items-center gap-1">
                      <span class="text-slate-400">手机:</span>
                      <span class="text-slate-600 font-mono font-medium">{{ primaryContact?.phone || '未填写' }}</span>
                    </div>
                    <div class="flex items-center gap-1">
                      <span class="text-slate-400">状态:</span>
                      <span class="text-primary font-bold">{{ getStatusLabel(customer.status) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex gap-2 shrink-0">
              <button class="px-4 py-2 border border-slate-200 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors" @click="handleEdit">编辑资料</button>
              <button class="px-4 py-2 bg-primary/10 text-primary border border-primary/20 rounded-lg text-sm font-bold flex items-center gap-1.5 hover:bg-primary/20 transition-colors" @click="handleAiFollowUp">
                <span class="material-symbols-outlined text-sm">smart_toy</span>
                AI 跟进
              </button>
              <button class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold shadow-lg shadow-primary/20 flex items-center gap-1.5 hover:bg-primary/90 transition-colors" @click="handleGenerateReport">
                <span class="material-symbols-outlined text-sm">auto_awesome</span>
                生成 AI 分析报告
              </button>
            </div>
          </div>

          <!-- Stage Stepper (inside same card) -->
          <div class="mt-5 pt-4 border-t border-slate-100">
            <div class="flex items-center gap-2 mb-4">
              <span class="material-symbols-outlined text-primary text-lg">trending_up</span>
              <h3 class="text-xs font-bold text-slate-700 uppercase tracking-wider">客户阶段</h3>
            </div>
            <div class="relative max-w-[55%]">
              <!-- Track line -->
              <div class="absolute top-3 left-0 right-0 h-0.5 bg-slate-200"></div>
              <div class="absolute top-3 left-0 h-0.5 bg-primary transition-all duration-500"
                :style="{ width: getProgressWidth() }"
              ></div>
              <!-- Stage nodes -->
              <div class="relative flex justify-between">
                <div
                  v-for="(stage, idx) in stageFlow"
                  :key="stage"
                  class="flex flex-col items-center gap-2 cursor-pointer group"
                  @click="handleStageChange(stage)"
                >
                  <div
                    class="size-6 rounded-full flex items-center justify-center border-2 transition-all duration-300 relative z-10 text-[10px] font-bold"
                    :class="getStepperNodeClass(stage, idx)"
                  >
                    <span v-if="stage === 'lost'" class="material-symbols-outlined text-[12px] font-bold">close</span>
                    <span v-else-if="stage === 'closed' && customer.stage === 'closed'" class="material-symbols-outlined text-[12px] font-bold">handshake</span>
                    <span v-else-if="getStageIndex(customer.stage) > idx && customer.stage !== 'lost'" class="material-symbols-outlined text-[12px] font-bold">check</span>
                    <span v-else>{{ idx + 1 }}</span>
                  </div>
                  <span class="text-[10px] font-bold tracking-wider transition-colors whitespace-nowrap"
                    :class="getStepperLabelClass(stage)"
                  >{{ getStepperLabel(stage) }}</span>
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
                <span class="material-symbols-outlined text-primary text-lg">info</span>
                基本信息
              </h3>
              <div class="space-y-5">
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">公司全称</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.companyName }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">所属行业</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.industry || '未填写' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">客户来源</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.source || '未填写' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">客户级别</p>
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
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">主要联系人</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ primaryContact?.name || '未设置' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">联系电话</p>
                  <p class="text-sm text-slate-900 font-medium font-mono px-2 py-1 -ml-2 truncate">{{ primaryContact?.phone || '未填写' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">电子邮箱</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ primaryContact?.email || '未填写' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">客户地址</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.address || '未填写' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">公司网站</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.website || '未填写' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">负责人</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.ownerName || '-' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">创建人</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2 truncate">{{ customer.createUserName || '-' }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">创建时间</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2">{{ formatDate(customer.createTime) }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">客户背景</p>
                  <p class="text-sm text-slate-600 leading-relaxed px-2 py-1 -ml-2">{{ customer.description || '暂无描述' }}</p>
                </div>
              </div>
            </section>

            <!-- Tags -->
            <section class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 class="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-lg">sell</span>
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
                <button class="px-3 py-1 rounded-lg text-xs font-bold text-primary border border-dashed border-primary/30 hover:bg-primary/5 transition-colors" @click="showAddTagDialog = true">
                  + 添加标签
                </button>
              </div>
            </section>

            <!-- Custom Fields -->
            <section v-if="customFields.length > 0" class="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 class="text-sm font-bold text-slate-900 mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-lg">tune</span>
                扩展信息
              </h3>
              <div class="space-y-5">
                <div v-for="field in customFields" :key="field.fieldId">
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">{{ field.fieldLabel }}</p>
                  <p class="text-sm text-slate-900 font-medium px-2 py-1 -ml-2">{{ formatCustomFieldValue(field, customer.customFields?.[field.fieldName]) }}</p>
                </div>
              </div>
            </section>
          </div>

          <!-- Center Column: Follow-ups Timeline (col-span-6) -->
          <div class="lg:col-span-6 space-y-6">
            <div class="flex items-center justify-between">
              <h3 class="flex items-center gap-2 text-lg font-bold text-slate-900">
                <span class="material-symbols-outlined text-primary">history</span>
                最近活动 - 智能跟进时间轴
              </h3>
              <div class="flex items-center gap-3">
                <div class="flex bg-slate-100 p-1 rounded-lg">
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
                </button>
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
                          <span class="text-[10px] font-bold px-1.5 py-0.5 bg-slate-50 text-slate-500 rounded border border-slate-100">{{ item.createUserName }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="flex items-center gap-3">
                      <span class="text-[10px] text-slate-400 uppercase font-bold">{{ formatDateTime(item.followTime || item.createTime) }}</span>
                      <el-popconfirm title="确定删除这条跟进记录吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDeleteFollowUp(item.followUpId)">
                        <template #reference>
                          <button class="text-slate-300 hover:text-red-500 transition-colors">
                            <span class="material-symbols-outlined text-sm">delete</span>
                          </button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </div>
                  <div v-if="item.nextContactTime" class="flex items-center gap-2 mb-3 text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-1 rounded-lg w-fit">
                    <span class="material-symbols-outlined text-xs">event_repeat</span>
                    下次联系: {{ formatDateTime(item.nextContactTime) }}
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
                <span class="material-symbols-outlined text-primary">hub</span>
                关联业务模块
              </h3>
              <span class="text-[10px] font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full uppercase tracking-tighter">5个模块</span>
            </div>

            <!-- Contacts Module -->
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden" v-loading="contactLoading">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">group</span>
                  关联联系人
                  <span class="text-slate-400 font-normal">({{ contactTotal }})</span>
                </h4>
                <button class="size-6 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all" @click="handleAddContact">
                  <span class="material-symbols-outlined text-sm">person_add</span>
                </button>
              </div>
              <div class="p-4 space-y-3">
                <div v-if="contacts.length === 0" class="py-6 text-center">
                  <p class="text-[10px] text-slate-400">暂无联系人</p>
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
                        <p class="text-[10px] text-slate-400 truncate max-w-[120px]">{{ contact.position || '联系人' }}</p>
                      </div>
                    </div>
                    <span v-if="contact.isPrimary" class="px-1.5 py-0.5 bg-primary/10 text-primary text-[8px] font-black rounded uppercase">主要</span>
                  </div>
                  <div class="flex items-center gap-3 text-[10px] text-slate-500">
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
                    <button class="text-[10px] font-bold text-primary hover:underline" @click="handleEditContact(contact)">编辑</button>
                    <span class="text-slate-200">|</span>
                    <button v-if="!contact.isPrimary" class="text-[10px] font-bold text-slate-400 hover:text-primary" @click="handleSetPrimary(contact.contactId)">设为主要</button>
                    <span v-if="!contact.isPrimary" class="text-slate-200">|</span>
                    <el-popconfirm title="确定删除该联系人吗？" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDeleteContact(contact.contactId)">
                      <template #reference>
                        <button class="text-[10px] font-bold text-red-400 hover:text-red-500">删除</button>
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

            <!-- Opportunities Module -->
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">monetization_on</span>
                  关联商机
                </h4>
              </div>
              <div class="p-4">
                <p class="py-6 text-center text-[10px] text-slate-400">暂无关联商机</p>
              </div>
            </section>

            <!-- Contracts Module -->
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">assignment</span>
                  合同管理
                </h4>
              </div>
              <div class="p-4">
                <p class="py-6 text-center text-[10px] text-slate-400">暂无合同记录</p>
              </div>
            </section>

            <!-- Tasks Module -->
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">task_alt</span>
                  待办任务
                </h4>
                <button class="size-6 flex items-center justify-center rounded-lg bg-white border border-slate-200 text-slate-400 hover:text-primary hover:border-primary/30 transition-all" @click="handleAddTask">
                  <span class="material-symbols-outlined text-sm">add</span>
                </button>
              </div>
              <div class="p-4 space-y-3">
                <div v-if="!customer.tasks?.length" class="py-6 text-center">
                  <p class="text-[10px] text-slate-400">暂无待办任务</p>
                </div>
                <div
                  v-for="task in customer.tasks?.slice(0, 5)"
                  :key="task.taskId"
                  class="flex items-start gap-3 p-3 bg-white border border-slate-100 rounded-xl hover:shadow-sm transition-all"
                >
                  <div class="flex-1 min-w-0">
                    <p class="text-xs font-bold text-slate-900 truncate">{{ task.title }}</p>
                    <div class="flex items-center gap-2 mt-1">
                      <span class="text-[9px] font-bold uppercase" :class="task.dueDate && isOverdue(task.dueDate) ? 'text-red-500' : 'text-slate-400'">
                        {{ task.dueDate ? formatDate(task.dueDate) : '无截止日期' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- Documents Module -->
            <section class="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
              <div class="px-5 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <h4 class="text-xs font-bold text-slate-700 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-lg">folder_open</span>
                  文档中心
                </h4>
              </div>
              <div class="p-4">
                <p class="py-6 text-center text-[10px] text-slate-400">暂无文档</p>
              </div>
            </section>
          </div>
        </div>
      </div>
    </template>

    <!-- Add Tag Dialog -->
    <el-dialog v-model="showAddTagDialog" title="添加标签" :width="isMobile ? '90%' : '400px'">
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
              <span v-if="currentContact.isPrimary" class="text-[10px] font-bold px-2 py-0.5 bg-emerald-50 text-emerald-600 rounded">主联系人</span>
            </div>
          </div>
          <div class="space-y-3">
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-[10px] text-slate-400 uppercase font-bold mb-1">电话</p>
              <p class="text-sm text-slate-900">{{ currentContact.phone || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-[10px] text-slate-400 uppercase font-bold mb-1">邮箱</p>
              <p class="text-sm text-slate-900">{{ currentContact.email || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-[10px] text-slate-400 uppercase font-bold mb-1">微信</p>
              <p class="text-sm text-slate-900">{{ currentContact.wechat || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-[10px] text-slate-400 uppercase font-bold mb-1">备注</p>
              <p class="text-sm text-slate-900">{{ currentContact.notes || '-' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl">
              <p class="text-[10px] text-slate-400 uppercase font-bold mb-1">创建时间</p>
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
    <el-dialog v-model="showEditDialog" title="编辑客户信息" :width="isMobile ? '95%' : '600px'" :fullscreen="isMobile">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="公司名称" required>
          <el-input v-model="editForm.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="行业">
              <el-input v-model="editForm.industry" placeholder="请输入行业" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="客户级别">
              <el-select v-model="editForm.level" class="w-full">
                <el-option label="A级" value="A" />
                <el-option label="B级" value="B" />
                <el-option label="C级" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="商机阶段">
              <el-select v-model="editForm.stage" class="w-full">
                <el-option label="线索" value="lead" />
                <el-option label="资格审查" value="qualified" />
                <el-option label="方案报价" value="proposal" />
                <el-option label="谈判中" value="negotiation" />
                <el-option label="结单" value="closed" />
                <el-option label="已流失" value="lost" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="报价金额">
              <el-input-number v-model="editForm.quotation" :min="0" :precision="2" :controls="false" class="w-full" placeholder="报价金额" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="合同金额">
              <el-input-number v-model="editForm.contractAmount" :min="0" :precision="2" :controls="false" class="w-full" placeholder="合同金额" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="回款金额">
              <el-input-number v-model="editForm.revenue" :min="0" :precision="2" :controls="false" class="w-full" placeholder="回款金额" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="网站">
          <el-input v-model="editForm.website" placeholder="请输入网站地址" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="editForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
        <DynamicFieldForm ref="editDynamicFieldFormRef" entity-type="customer" v-model="editCustomFieldValues" title="扩展信息" />
      </el-form>
      <template #footer>
        <div class="flex justify-between w-full">
          <el-popconfirm
            title="确定要删除此客户吗？删除后不可恢复。"
            confirm-button-text="删除"
            cancel-button-text="取消"
            confirm-button-type="danger"
            @confirm="handleDeleteCustomer"
          >
            <template #reference>
              <el-button type="danger" plain>删除客户</el-button>
            </template>
          </el-popconfirm>
          <div class="flex gap-2">
            <el-button @click="showEditDialog = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSaveEdit">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
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
import { ElMessage } from 'element-plus'
import type { CustomerTag, FollowUp, CustomerUpdateBO, Contact } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'

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
const editCustomFieldValues = ref<Record<string, any>>({})
const editDynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()

const primaryContact = computed(() => contacts.value.find(c => c.isPrimary) || contacts.value[0] || null)

const editForm = reactive({
  customerId: '',
  companyName: '',
  industry: '',
  level: 'B',
  stage: 'lead',
  quotation: undefined as number | undefined,
  contractAmount: undefined as number | undefined,
  revenue: undefined as number | undefined,
  website: '',
  address: '',
  description: ''
})

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
    contacts.value = result.list
    contactTotal.value = result.totalRow
  } catch (err) {
    console.error('Failed to fetch contacts:', err)
    contacts.value = []
    contactTotal.value = 0
  } finally {
    contactLoading.value = false
  }
}

function handleContactPageChange(page: number) {
  contactPage.value = page
  if (customer.value) fetchContacts(customer.value.customerId)
}

function handleEdit() {
  if (!customer.value) return
  editForm.customerId = customer.value.customerId
  editForm.companyName = customer.value.companyName || ''
  editForm.industry = customer.value.industry || ''
  editForm.level = customer.value.level || 'B'
  editForm.stage = customer.value.stage || 'lead'
  editForm.quotation = (customer.value as any).quotation
  editForm.contractAmount = (customer.value as any).contractAmount
  editForm.revenue = (customer.value as any).revenue
  editForm.website = customer.value.website || ''
  editForm.address = customer.value.address || ''
  editForm.description = customer.value.description || ''
  editCustomFieldValues.value = customer.value.customFields ? { ...customer.value.customFields } : {}
  showEditDialog.value = true
}

async function handleSaveEdit() {
  if (!editForm.companyName.trim()) {
    ElMessage.warning('请输入公司名称')
    return
  }
  if (editDynamicFieldFormRef.value) {
    const missingFields = editDynamicFieldFormRef.value.getRequiredFieldLabels()
    if (missingFields.length > 0) {
      ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
      return
    }
  }
  submitting.value = true
  try {
    const updateData: CustomerUpdateBO = {
      customerId: editForm.customerId,
      companyName: editForm.companyName,
      industry: editForm.industry || undefined,
      level: editForm.level as any,
      stage: editForm.stage as any,
      website: editForm.website || undefined,
      address: editForm.address || undefined,
      description: editForm.description || undefined,
      customFields: editCustomFieldValues.value
    }
    await customerStore.editCustomer(updateData)
    await customerStore.fetchCustomerDetail(editForm.customerId)
    showEditDialog.value = false
    ElMessage.success('客户信息更新成功')
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
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

function handleAiFollowUp() {
  router.push('/chat')
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
  contactForm.isPrimary = !!contact.isPrimary
  showAddContactDialog.value = true
}

async function handleDeleteContact(contactId: string) {
  try {
    await deleteContact(contactId)
    if (customer.value) await fetchContacts(customer.value.customerId)
    ElMessage.success('联系人已删除')
  } catch { /* Error handled */ }
}

async function handleSetPrimary(contactId: string) {
  try {
    await setPrimaryContact(contactId)
    if (customer.value) await fetchContacts(customer.value.customerId)
    ElMessage.success('已设为主联系人')
  } catch { /* Error handled */ }
}

function handleAddTask() {
  router.push('/task')
}

function handleOpenFollowUpDialog() {
  followUpForm.followTime = formatDateForApi()
  showAddFollowUpDialog.value = true
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
    await fetchContacts(contactForm.customerId, !editingContact.value)
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

function getNextStage(current: string): string | null {
  const idx = stageFlow.indexOf(current)
  if (idx >= 0 && idx < stageFlow.length - 1) return stageFlow[idx + 1]
  return null
}

function getStageLabelFull(stage: string): string {
  return stageOptions.find(s => s.value === stage)?.label || stage
}

function getProgressWidth(): string {
  const cs = customer.value?.stage
  if (!cs) return '0%'
  // For lost customers, don't show progress line (they dropped out)
  if (cs === 'lost') return '0%'
  const idx = getStageIndex(cs)
  if (idx < 0) return '0%'
  // Progress line spans across 6 nodes (0-5), calculate proportionally
  return `${(idx / (stageFlow.length - 1)) * 100}%`
}

function getStepperNodeClass(stage: string, idx: number): string {
  const cs = customer.value?.stage
  if (!cs) return 'bg-white border-slate-200 text-slate-300'
  // "已流失" node
  if (stage === 'lost') {
    if (cs === 'lost') return 'bg-slate-50 border-slate-500 text-slate-600 shadow-md shadow-slate-500/20 scale-110'
    return 'bg-white border-slate-200 text-slate-300 group-hover:border-slate-400'
  }
  // "结单/已成交" node
  if (stage === 'closed') {
    if (cs === 'closed') return 'bg-emerald-50 border-emerald-500 text-emerald-600 shadow-md shadow-emerald-500/20 scale-110'
    return 'bg-white border-slate-200 text-slate-300 group-hover:border-primary/50'
  }
  // Current stage
  if (cs === stage) return 'bg-white border-primary text-primary shadow-md shadow-primary/20 scale-110'
  // Completed stages (before current, not lost)
  if (getStageIndex(cs) > idx && cs !== 'lost') return 'bg-primary border-primary text-white'
  return 'bg-white border-slate-200 text-slate-300 group-hover:border-primary/50'
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

function getStepperLabelClass(stage: string): string {
  const cs = customer.value?.stage
  if (stage === 'lost' && cs === 'lost') return 'text-slate-600'
  if (stage === 'lost') return 'text-slate-400'
  if (stage === 'closed' && cs === 'closed') return 'text-emerald-600'
  if (cs === stage) return 'text-primary'
  return 'text-slate-400'
}

function getStatusLabel(status: string): string {
  const labels: Record<string, string> = { high: '高意向', active: '活跃', followup: '需跟进', dormant: '休眠' }
  return labels[status] || status || '未设置'
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
