<template>
  <div class="flex h-full flex-col overflow-hidden bg-[#f6f7f4]">
    <div v-if="project && canViewProject" class="flex flex-1 flex-col overflow-hidden">
      <header v-if="!isProjectChatView" class="border-b border-slate-200 bg-white px-4 py-2 md:px-6">
        <div class="flex w-full flex-col gap-2">
          <div class="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between">
            <div class="min-w-0">
              <button
                v-if="isTaskConversation"
                type="button"
                class="inline-flex items-center gap-1 text-sm font-medium text-slate-500 transition-colors hover:text-slate-800"
                @click="handleBackClick"
              >
                <span class="material-symbols-outlined text-[16px]">arrow_back</span>
                {{ isTaskConversation ? '返回项目' : '返回项目列表' }}
              </button>

              <div class="mt-1.5 flex flex-wrap items-center gap-2">
                <template v-if="isTaskConversation && currentTaskConversation">
                  <h1 class="text-2xl font-black tracking-tight text-slate-900">
                    当前对话对象：任务 - {{ currentTaskConversation.title }}
                  </h1>
                  <span class="inline-flex rounded-full px-3 py-1 text-sm font-bold bg-white border border-slate-200 text-slate-600">
                    所属项目：{{ project.name }}
                  </span>
                </template>
                <template v-else>
                  <h1 class="text-[15px] font-semibold leading-5 text-slate-900">{{ project.name }}</h1>
                  <span class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-normal" :class="projectStatusClass(project.status)">
                    {{ projectStatusLabel(project.status) }}
                  </span>
                </template>
              </div>

              <div v-if="isTaskConversation && currentTaskConversation" class="mt-1.5 flex flex-wrap gap-2 text-xs text-slate-500">
                <span class="inline-flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px]">view_kanban</span>
                  {{ currentTaskConversation.status }}
                </span>
                <span class="inline-flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px]">person</span>
                  {{ currentTaskConversation.ownerName || '未指定负责人' }}
                </span>
                <span class="inline-flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px]">schedule</span>
                  {{ formatDateTime(currentTaskConversation.dueDate) }}
                </span>
              </div>

              <div v-else class="mt-1.5 flex flex-wrap gap-2 text-xs text-slate-500">
                <span class="inline-flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px]">corporate_fare</span>
                  {{ project.customerName || '未关联客户' }}
                </span>
                <span class="inline-flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px]">person</span>
                  {{ project.ownerName || '未指定负责人' }}
                </span>
                <span class="inline-flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-[16px]">schedule</span>
                  更新于 {{ formatDateTime(project.updateTime) }}
                </span>
              </div>
            </div>

            <div class="flex flex-wrap items-center gap-2">
              <template v-if="isTaskConversation">
                <button
                  type="button"
                  class="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                  @click="backToProject"
                >
                  返回项目
                </button>
                <button
                  v-if="currentTaskConversation"
                  type="button"
                  class="rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                  @click="openTaskDetail(currentTaskConversation)"
                >
                  查看任务详情
                </button>
              </template>
              <template v-else>
                <div class="flex flex-col items-start gap-2 sm:items-end">
                  <div class="flex flex-wrap items-center gap-2">
                    <button
                      v-if="canCreateTask"
                      type="button"
                      class="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3.5 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-slate-700"
                      @click="openCreateTask()"
                    >
                      <span class="material-symbols-outlined text-[17px]">add</span>
                      新建任务
                    </button>
                    <el-dropdown trigger="click" @command="handleProjectMoreAction">
                      <button
                        type="button"
                        class="flex size-9 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-900"
                        aria-label="更多项目操作"
                      >
                        <span class="material-symbols-outlined text-[20px]">more_horiz</span>
                      </button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item
                            v-if="canEditProject"
                            command="edit-project"
                          >
                            <span class="inline-flex items-center gap-2">
                              <span class="material-symbols-outlined text-[16px]">edit</span>
                              <span>编辑项目</span>
                            </span>
                          </el-dropdown-item>
                          <el-dropdown-item command="members">
                            <span class="inline-flex items-center gap-2">
                              <span class="material-symbols-outlined text-[16px]">group</span>
                              <span>项目成员</span>
                            </span>
                          </el-dropdown-item>
                          <el-dropdown-item
                            v-if="canArchiveProject && project.status !== 'ARCHIVED'"
                            command="archive-project"
                          >
                            <span class="inline-flex items-center gap-2">
                              <span class="material-symbols-outlined text-[16px]">inventory_2</span>
                              <span>归档项目</span>
                            </span>
                          </el-dropdown-item>
                          <el-dropdown-item
                            v-if="canDeleteProject"
                            command="delete-project"
                          >
                            <span class="inline-flex items-center gap-2 text-red-600">
                              <span class="material-symbols-outlined text-[16px]">delete</span>
                              <span>删除项目</span>
                            </span>
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </header>

      <main class="flex flex-1 flex-col overflow-hidden">
        <div v-if="viewMode === 'ai'" class="wk-project-chat-shell">
          <section class="flex min-h-0 flex-1 flex-col overflow-hidden">
            <div class="wk-project-chat-header">
              <div class="wk-project-chat-header__inner">
                <span class="wk-project-chat-badge">项目</span>
                <h2 class="min-w-0 truncate text-[15px] font-semibold leading-5 text-[#0d0d0d]">
                  {{ project.name }}
                </h2>
              </div>
              <div class="flex min-w-0 items-center gap-2">
                <span class="material-symbols-outlined inline-flex size-8 shrink-0 items-center justify-center rounded-xl bg-[#f4f4f4] text-[18px] text-[#5d5d5d]">
                  folder
                </span>
                <div class="min-w-0">
                  <p class="text-[15px] font-semibold leading-5 text-[#0d0d0d]">项目对话</p>
                  <p class="mt-0.5 truncate text-[12px] leading-4 text-[#8f8f8f]">{{ project.name }}</p>
                </div>
              </div>
            </div>

            <div
              v-if="false"
              class="border-b border-slate-100 bg-slate-50/60 px-5 py-4"
            >
              <div class="mx-auto grid max-w-4xl gap-3 md:grid-cols-2">
                <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-bold text-slate-800">项目日程</p>
                    <span class="text-xs font-semibold text-slate-400">{{ projectSchedules.length }} 项</span>
                  </div>
                  <div v-if="projectSchedules.length" class="mt-3 flex flex-col gap-2">
                    <div
                      v-for="item in projectSchedules.slice(0, 3)"
                      :key="item.scheduleId"
                      class="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-600"
                    >
                      <p class="font-semibold text-slate-800">{{ item.title }}</p>
                      <p class="mt-1 text-xs text-slate-400">{{ formatDateTime(item.scheduleTime || item.createTime) }}</p>
                    </div>
                  </div>
                  <p v-else class="mt-3 text-sm text-slate-400">暂无项目日程</p>
                </div>

                <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-bold text-slate-800">项目附件</p>
                    <span class="text-xs font-semibold text-slate-400">{{ projectAttachments.length }} 项</span>
                  </div>
                  <div v-if="projectAttachments.length" class="mt-3 flex flex-col gap-2">
                    <div
                      v-for="item in projectAttachments.slice(0, 3)"
                      :key="item.attachmentId"
                      class="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-600"
                    >
                      <p class="font-semibold text-slate-800">{{ item.name }}</p>
                      <p class="mt-1 text-xs text-slate-400">{{ item.createdByName || '项目 AI' }} · {{ formatDateTime(item.createTime) }}</p>
                    </div>
                  </div>
                  <p v-else class="mt-3 text-sm text-slate-400">暂无项目附件</p>
                </div>
              </div>
            </div>

            <div class="wk-project-chat-messages">
              <div class="wk-project-chat-messages__inner px-4 md:px-8">
                <div
                  v-for="message in project.chatMessages"
                  :key="message.messageId"
                  class="wk-project-chat-message mx-auto message-enter"
                  :class="message.role === 'user' ? 'wk-project-chat-message--user' : 'wk-project-chat-message--assistant'"
                >
                  <div v-if="message.role === 'user'" class="group flex w-full flex-row-reverse pb-0">
                    <div class="min-w-[50px] max-w-[72%] space-y-3">
                      <div class="rounded-[24px] bg-[#f4f4f4] px-4 py-2.5 text-[15px] leading-7 text-[#0d0d0d]">
                        <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                      </div>
                    </div>
                  </div>
                  <div v-else class="group flex w-full gap-3 pb-4 md:gap-4 md:pb-8">
                    <div class="min-w-0 flex-1 space-y-3">
                      <div class="max-w-full text-left text-[16px] leading-7 text-[#0d0d0d]">
                        <div class="wk-markdown" v-html="renderAssistantMessage(message.content)" />
                      </div>
                      <div v-if="message.content.trim()" class="flex h-8 items-center">
                        <button
                          type="button"
                          class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-all hover:bg-[#f1f1f1] hover:text-[#0d0d0d]"
                          aria-label="复制内容"
                          @click="copyProjectAssistantMessage(message.content)"
                        >
                          <WkIcon name="copy" :box-size="18" class="shrink-0 material-symbols-outlined leading-none" title="复制内容" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="wk-project-chat-composer-wrap">
              <div class="mx-auto w-[calc(100%-20px)] max-w-4xl md:w-full">
                <template v-if="canUseAiChat">
                  <ProjectChatComposer
                    v-model="aiInput"
                    placeholder="发消息..."
                    :context-label="project.name"
                    context-icon="task"
                    @send="handleSendAiMessage"
                  />
                  <div v-if="false" class="wk-project-chat-composer relative flex min-w-0 items-center rounded-[28px] p-[6px]">
                    <div class="w-full min-w-0">
                      <textarea
                        ref="projectAiComposerInputRef"
                        v-model="aiInput"
                        rows="1"
                        class="wk-project-chat-textarea"
                        placeholder="输入项目指令"
                        @input="handleProjectChatComposerInput"
                        @keydown.enter.exact.prevent="handleSendAiMessage"
                      ></textarea>
                      <div class="flex min-w-0 items-center justify-end w-full px-1 pb-1 select-none mt-1">
                        <button
                          type="button"
                          class="wk-project-chat-send-button"
                          :disabled="!aiInput.trim()"
                          aria-label="发送"
                          @click="handleSendAiMessage"
                        >
                          <span class="material-symbols-outlined">arrow_upward</span>
                        </button>
                      </div>
                    </div>
                  </div>
                  <el-input
                    v-if="false"
                    v-model="aiInput"
                    type="textarea"
                    :rows="4"
                    resize="none"
                    class="wk-crm-el-field-input"
                    placeholder="输入项目指令，例如：这个项目和北京百度公司合作，明天下午三点给客户汇报需求文档和设计方案，帮我创建一个任务。"
                    @keydown.enter.exact.prevent="handleSendAiMessage"
                  />
                  <div v-if="false" class="mt-3 flex items-center justify-end gap-3">
                    <p class="hidden text-xs text-slate-400">AI 可创建任务、泳道、项目日程和项目附件，并会在执行前校验项目权限。</p>
                    <button
                      type="button"
                      class="wk-project-chat-send-button"
                      :disabled="!aiInput.trim()"
                      @click="handleSendAiMessage"
                    >
                      <span class="material-symbols-outlined">arrow_upward</span>
                      发送
                    </button>
                  </div>
                </template>
                <div v-else class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-700">
                  当前账号没有使用项目 AI 对话的权限。
                </div>
              </div>
            </div>
          </section>
        </div>

        <div v-else-if="viewMode === 'task_ai' && currentTaskConversation" class="wk-project-chat-shell wk-project-chat-shell--task relative">
          <section class="flex min-w-0 flex-1 flex-col overflow-hidden">
            <div class="wk-project-task-chat-header">
              <div class="wk-project-task-chat-header__inner">
                <div class="flex min-w-0 flex-1 items-center gap-2">
                  <span class="wk-project-chat-badge">任务</span>
                  <div class="min-w-0 flex-1">
                    <h2 class="truncate text-[15px] font-semibold leading-5 text-[#0d0d0d]">
                      {{ currentTaskConversation.title }}
                    </h2>
                    <p class="mt-0.5 truncate text-[12px] leading-4 text-[#8f8f8f]">
                      {{ project.name }}
                    </p>
                  </div>
                </div>
              </div>
              <button
                v-if="showTaskDetailPanelShell && taskDetailPanelVisible"
                type="button"
                class="wk-project-object-panel-toggle wk-project-object-panel-toggle--inside"
                aria-label="收起任务详情栏"
                @click="taskDetailPanelVisible = false"
              >
                <span class="material-symbols-outlined text-[20px] leading-none">dock_to_right</span>
                <span class="wk-project-object-panel-tooltip" role="tooltip">收起任务详情栏</span>
              </button>
            </div>

            <div class="wk-project-task-chat-messages">
              <div class="wk-project-task-chat-messages__inner px-4 md:px-8">
                <div
                  v-for="message in currentTaskConversation.chatMessages"
                  :key="message.messageId"
                  class="wk-project-task-chat-message mx-auto message-enter"
                  :class="message.role === 'user' ? 'wk-project-task-chat-message--user' : 'wk-project-task-chat-message--assistant'"
                >
                  <div v-if="message.role === 'user'" class="group flex w-full flex-row-reverse pb-0">
                    <div class="min-w-[50px] max-w-[72%] space-y-3">
                      <div class="rounded-[24px] bg-[#f4f4f4] px-4 py-2.5 text-[15px] leading-7 text-[#0d0d0d]">
                        <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                      </div>
                    </div>
                  </div>
                  <div v-else class="group flex w-full gap-3 pb-4 md:gap-4 md:pb-8">
                    <div class="min-w-0 flex-1 space-y-3">
                      <div class="max-w-full text-left text-[16px] leading-7 text-[#0d0d0d]">
                        <div class="wk-markdown" v-html="renderTaskAssistantMessage(message.content)" />
                      </div>
                      <div v-if="message.content.trim()" class="flex h-8 items-center">
                        <button
                          type="button"
                          class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-all hover:bg-[#f1f1f1] hover:text-[#0d0d0d]"
                          aria-label="复制内容"
                          @click="copyProjectAssistantMessage(message.content)"
                        >
                          <WkIcon name="copy" :box-size="18" class="shrink-0 material-symbols-outlined leading-none" title="复制内容" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="wk-project-task-chat-composer-wrap">
              <div class="mx-auto w-[calc(100%-20px)] max-w-4xl md:w-full">
                <template v-if="canUseCurrentTaskAi">
                  <ProjectChatComposer
                    v-model="taskAiInput"
                    placeholder="发消息..."
                    :context-label="currentTaskConversation.title"
                    context-icon="task-1"
                    @send="handleSendTaskAiMessage"
                  />
                  <div v-if="false" class="wk-project-chat-composer relative flex min-w-0 items-center rounded-[28px] p-[6px]">
                    <div class="w-full min-w-0">
                      <textarea
                        ref="taskAiComposerInputRef"
                        v-model="taskAiInput"
                        rows="1"
                        class="wk-project-chat-textarea"
                        placeholder="输入任务指令"
                        @input="handleTaskChatComposerInput"
                        @keydown.enter.exact.prevent="handleSendTaskAiMessage"
                      ></textarea>
                      <div class="flex min-w-0 items-center justify-end w-full px-1 pb-1 select-none mt-1">
                        <button
                          type="button"
                          class="wk-project-chat-send-button"
                          :disabled="!taskAiInput.trim()"
                          aria-label="发送"
                          @click="handleSendTaskAiMessage"
                        >
                          <span class="material-symbols-outlined">arrow_upward</span>
                        </button>
                      </div>
                    </div>
                  </div>
                  <div v-if="false" class="wk-project-task-chat-composer mx-auto flex min-w-0 items-end rounded-[28px] p-[6px]">
                    <textarea
                      v-model="taskAiInput"
                      rows="1"
                      class="wk-project-task-chat-textarea"
                      placeholder="输入任务指令，例如：把这个任务改到明天下午三点，或帮我生成一个汇报大纲。"
                      @keydown.enter.exact.prevent="handleSendTaskAiMessage"
                    />
                    <button
                      type="button"
                      class="wk-project-task-chat-send-button"
                      :disabled="!taskAiInput.trim()"
                      @click="handleSendTaskAiMessage"
                    >
                      <span class="material-symbols-outlined">arrow_upward</span>
                    </button>
                  </div>
                </template>
                <div v-else class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-700">
                  当前账号没有使用该任务 AI 对话的权限。
                </div>
              </div>
            </div>
          </section>
          <button
            v-if="showTaskDetailPanelShell && !taskDetailPanelVisible"
            type="button"
            class="wk-project-object-panel-toggle wk-project-object-panel-toggle--floating"
            aria-label="展开任务详情栏"
            @click="taskDetailPanelVisible = true"
          >
            <span class="material-symbols-outlined text-[20px] leading-none">dock_to_left</span>
            <span class="wk-project-object-panel-tooltip" role="tooltip">展开任务详情栏</span>
          </button>
          <aside
            v-if="showTaskDetailPanelShell && taskDetailPanelVisible"
            class="wk-project-task-detail-panel relative flex shrink-0 flex-col border-l"
          >
            <div class="min-h-0 flex-1 overflow-y-auto px-5 pb-5 pt-4">
              <div class="mb-5 flex items-start gap-3">
                <span class="material-symbols-outlined flex size-9 shrink-0 items-center justify-center rounded-xl bg-[#f5f5f5] text-[20px] text-[#8f8f8f]">
                  task_alt
                </span>
                <div class="min-w-0 flex-1">
                  <p class="text-[12px] font-medium leading-4 text-[#8f8f8f]">任务详情</p>
                  <h3 class="mt-1 break-words text-[16px] font-semibold leading-6 text-[#0d0d0d]">
                    {{ currentTaskConversation.title }}
                  </h3>
                </div>
                <div class="ml-auto flex shrink-0 items-center gap-1" @click.stop>
                  <button
                    v-if="canEditTask(currentTaskConversation)"
                    type="button"
                    class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#f5f5f5] hover:text-[#0d0d0d]"
                    aria-label="编辑任务"
                    title="编辑任务"
                    @click="handleEditTask(currentTaskConversation)"
                  >
                    <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
                  </button>
                  <button
                    v-if="canDeleteTask(currentTaskConversation)"
                    type="button"
                    class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-red-50 hover:text-red-500"
                    aria-label="删除任务"
                    title="删除任务"
                    @click="handleDeleteTask(currentTaskConversation)"
                  >
                    <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
                  </button>
                </div>
              </div>

              <div class="space-y-4">
                <section class="wk-project-task-detail-section">
                  <p class="wk-project-task-detail-section__title">基本信息</p>
                  <dl class="mt-3 space-y-2">
                    <div class="wk-project-task-detail-row">
                      <dt>状态</dt>
                      <dd>{{ currentTaskConversation.status || '未设置' }}</dd>
                    </div>
                    <div class="wk-project-task-detail-row">
                      <dt>优先级</dt>
                      <dd>
                        <span class="inline-flex rounded-lg px-2 py-0.5 text-[12px] font-medium" :class="projectTaskPriorityClass(currentTaskConversation.priority)">
                          {{ projectTaskPriorityLabel(currentTaskConversation.priority) }}
                        </span>
                      </dd>
                    </div>
                    <div class="wk-project-task-detail-row">
                      <dt>泳道</dt>
                      <dd>{{ laneName(currentTaskConversation.laneId) }}</dd>
                    </div>
                    <div class="wk-project-task-detail-row">
                      <dt>负责人</dt>
                      <dd>{{ currentTaskConversation.ownerName || '未指定' }}</dd>
                    </div>
                    <div class="wk-project-task-detail-row">
                      <dt>截止时间</dt>
                      <dd>{{ formatDateTime(currentTaskConversation.dueDate) }}</dd>
                    </div>
                    <div class="wk-project-task-detail-row">
                      <dt>关联客户</dt>
                      <dd>{{ currentTaskConversation.customerName || project.customerName || '未关联' }}</dd>
                    </div>
                  </dl>
                </section>

                <section class="wk-project-task-detail-section">
                  <p class="wk-project-task-detail-section__title">参与人</p>
                  <div v-if="currentTaskConversation.participantNames?.length" class="mt-3 flex flex-wrap gap-2">
                    <span
                      v-for="name in currentTaskConversation.participantNames"
                      :key="name"
                      class="rounded-lg bg-[#f5f5f5] px-2.5 py-1 text-[12px] font-medium text-[#5f5f5f]"
                    >
                      {{ name }}
                    </span>
                  </div>
                  <p v-else class="mt-3 text-[13px] leading-5 text-[#8f8f8f]">暂无参与人</p>
                </section>

                <section class="wk-project-task-detail-section">
                  <p class="wk-project-task-detail-section__title">任务描述</p>
                  <p class="mt-3 whitespace-pre-wrap text-[13px] leading-6 text-[#5f5f5f]">
                    {{ currentTaskConversation.description || '暂无任务描述' }}
                  </p>
                </section>

                <section class="wk-project-task-detail-section">
                  <p class="wk-project-task-detail-section__title">执行资料</p>
                  <div class="mt-3 grid grid-cols-3 gap-2">
                    <div class="wk-project-task-detail-stat">
                      <span>{{ currentTaskConversation.attachments.length }}</span>
                      <p>附件</p>
                    </div>
                    <div class="wk-project-task-detail-stat">
                      <span>{{ currentTaskConversation.schedules.length }}</span>
                      <p>日程</p>
                    </div>
                    <div class="wk-project-task-detail-stat">
                      <span>{{ currentTaskConversation.notes.length }}</span>
                      <p>备注</p>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </aside>
        </div>

        <div v-if="showTaskViewToolbar" class="shrink-0 px-4 pt-4 md:px-6">
          <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div class="relative w-full max-w-sm">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[18px] leading-none text-[#8aa1c2]">search</span>
              <input
                v-model="projectTaskSearchKeyword"
                type="text"
                class="h-9 w-full rounded-lg border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] pl-9 pr-9 text-sm text-[var(--wk-text-primary)] outline-none transition-colors placeholder:text-[var(--wk-text-faint)] focus:border-[var(--wk-input-border-hover)]"
                placeholder="模糊搜索任务"
              />
              <span
                v-if="projectTaskSearchLoading"
                class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 animate-spin text-[18px] leading-none text-[#8aa1c2]"
              >
                progress_activity
              </span>
            </div>
            <div class="inline-flex h-9 shrink-0 items-center rounded-lg border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] p-1">
              <button
                v-for="tab in projectTabs"
                :key="tab.value"
                type="button"
                :class="[
                  'inline-flex size-7 items-center justify-center rounded-md transition-all',
                  viewMode === tab.value ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-[#8aa1c2] hover:text-primary'
                ]"
                :title="tab.label"
                :aria-label="tab.label"
                @click="switchProjectView(tab.value)"
              >
                <span class="material-symbols-outlined text-[20px] leading-none">{{ tab.icon }}</span>
              </button>
            </div>
          </div>
        </div>

        <div v-if="viewMode === 'board'" class="min-h-0 flex-1 overflow-hidden px-4 pb-4 pt-3 md:px-6">
          <div class="flex h-full w-full flex-col overflow-hidden">
            <div class="flex-1 overflow-x-auto overflow-y-hidden py-5">
              <div class="flex h-full min-w-max gap-4">
                <section
                  v-for="lane in orderedLanes"
                  :key="lane.laneId"
                  class="flex h-full min-h-0 w-[320px] flex-col rounded-xl border border-slate-200 bg-[#f8f8f6] p-3"
                  @dragover.prevent="handleLaneDragOver(lane.laneId, $event)"
                  @drop.prevent="handleLaneDrop(lane.laneId)"
                >
                  <header class="mb-3 flex items-start justify-between gap-2 px-2">
                    <div>
                      <div class="flex items-center gap-2">
                        <h2 class="text-sm font-bold text-slate-900">{{ lane.name }}</h2>
                        <span class="inline-flex size-5 items-center justify-center rounded-full bg-white text-[10px] font-bold text-slate-500">
                          {{ tasksByLane(lane.laneId).length }}
                        </span>
                      </div>
                      <p class="mt-1 text-xs text-slate-400">{{ lane.system ? '系统默认泳道' : '自定义泳道' }}</p>
                    </div>
                    <div class="flex items-center gap-1">
                      <button
                        v-if="canCreateTask"
                        type="button"
                        class="flex size-8 items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-white hover:text-slate-700"
                        @click="openCreateTask(lane.laneId)"
                      >
                        <span class="material-symbols-outlined text-[18px]">add</span>
                      </button>
                      <button
                        v-if="canEditLane"
                        type="button"
                        class="flex size-8 items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-white hover:text-slate-700"
                        @click="promptRenameLane(lane.laneId)"
                      >
                        <span class="material-symbols-outlined text-[18px]">edit</span>
                      </button>
                      <button
                        v-if="canDeleteLane && !lane.system"
                        type="button"
                        class="flex size-8 items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-white hover:text-red-500"
                        @click="handleDeleteLane(lane.laneId)"
                      >
                        <span class="material-symbols-outlined text-[18px]">delete</span>
                      </button>
                    </div>
                  </header>

                  <div class="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto px-1 pb-1">
                    <article
                      v-for="task in tasksByLane(lane.laneId)"
                      :key="task.taskId"
                      :draggable="canMoveTask(task)"
                      class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition-all hover:border-primary/30 hover:shadow-md"
                      :class="[
                        draggingTaskId === task.taskId ? 'opacity-60 ring-1 ring-primary/20' : '',
                        canMoveTask(task) ? 'cursor-grab active:cursor-grabbing' : ''
                      ]"
                      @click="enterTaskConversation(task)"
                      @dragstart="handleTaskDragStart(task.taskId, $event)"
                      @dragend="handleTaskDragEnd"
                    >
                      <div class="flex items-start justify-between gap-3">
                        <h3 class="line-clamp-2 text-sm font-bold leading-6 text-slate-900">{{ task.title }}</h3>
                        <span class="shrink-0 rounded-full px-2 py-1 text-xs font-bold" :class="projectTaskPriorityClass(task.priority)">
                          {{ projectTaskPriorityLabel(task.priority) }}
                        </span>
                      </div>

                      <div class="mt-3 space-y-2 text-xs text-slate-500">
                        <p class="inline-flex items-center gap-1.5">
                          <span class="material-symbols-outlined text-[14px]">schedule</span>
                          {{ task.dueDate ? formatDateTime(task.dueDate) : '未设置截止时间' }}
                        </p>
                        <p class="inline-flex items-center gap-1.5">
                          <span class="material-symbols-outlined text-[14px]">person</span>
                          {{ task.ownerName || '未指定负责人' }}
                        </p>
                        <p class="inline-flex items-center gap-1.5">
                          <span class="material-symbols-outlined text-[14px]">group</span>
                          {{ task.participantNames?.length ? task.participantNames.join('、') : '无参与人' }}
                        </p>
                        <p class="inline-flex items-center gap-1.5">
                          <span class="material-symbols-outlined text-[14px]">corporate_fare</span>
                          {{ task.customerName || project.customerName || '未关联客户' }}
                        </p>
                      </div>

                      <div class="mt-4 flex items-center justify-between">
                        <div class="flex items-center gap-2 text-slate-400">
                          <span
                            class="inline-flex items-center gap-1 rounded-full border border-slate-200 px-2 py-1"
                            :class="task.hasAttachments ? 'text-primary' : ''"
                          >
                            <span class="material-symbols-outlined text-[14px]">attach_file</span>
                            附件
                          </span>
                          <span
                            class="inline-flex items-center gap-1 rounded-full border border-slate-200 px-2 py-1"
                            :class="task.hasSchedule ? 'text-emerald-600' : ''"
                          >
                            <span class="material-symbols-outlined text-[14px]">calendar_month</span>
                            日程
                          </span>
                        </div>
                        <span v-if="task.generatedByAi" class="inline-flex items-center gap-1 text-primary">
                          <span class="material-symbols-outlined text-[14px]">auto_awesome</span>
                          AI
                        </span>
                      </div>
                    </article>

                    <div
                      class="flex min-h-28 flex-col items-center justify-center rounded-[22px] border border-dashed text-center text-sm"
                      :class="dragOverLaneId === lane.laneId ? 'border-primary bg-primary/5 text-primary' : 'border-slate-200 bg-white/70 text-slate-400'"
                    >
                      <span class="material-symbols-outlined mb-2 text-2xl">move_to_inbox</span>
                      <p>{{ dragOverLaneId === lane.laneId ? '松开即可移动到该泳道' : '拖动任务到这里切换状态' }}</p>
                    </div>
                  </div>
                </section>

                <button
                  v-if="canAddLane"
                  type="button"
                  class="group flex h-full min-h-[420px] w-[260px] shrink-0 flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-white/70 p-6 text-center text-slate-400 transition-all hover:border-primary/40 hover:bg-primary/5 hover:text-primary"
                  @click="promptAddLane"
                >
                  <span class="material-symbols-outlined text-4xl transition-transform group-hover:scale-110">add</span>
                  <span class="mt-3 text-sm font-bold">新建泳道</span>
                  <span class="mt-1 text-xs">点击在项目中新增一列</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="viewMode === 'list'" class="min-h-0 flex-1 overflow-y-auto px-4 pb-4 pt-3 md:px-6">
          <div class="wk-project-task-list-table-shell flex w-full flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
            <div v-if="filteredProjectTasks.length === 0" class="px-6 py-20 text-center text-slate-400">
              <span class="material-symbols-outlined text-5xl">task_alt</span>
              <p class="mt-4 text-sm">{{ projectTaskSearchKeyword.trim() ? '没有匹配的任务。' : '当前项目还没有可查看的任务。' }}</p>
            </div>

            <div v-else class="overflow-x-auto">
              <table class="wk-project-task-list-table min-w-[1080px] text-sm">
                <thead class="text-left">
                  <tr>
                    <th class="px-5 py-3 font-semibold">任务名称</th>
                    <th class="px-5 py-3 font-semibold">泳道</th>
                    <th class="px-5 py-3 font-semibold">优先级</th>
                    <th class="px-5 py-3 font-semibold">负责人</th>
                    <th class="px-5 py-3 font-semibold">截止时间</th>
                    <th class="px-5 py-3 font-semibold">关联客户</th>
                    <th class="px-5 py-3 font-semibold">资料</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="task in filteredProjectTasks"
                    :key="task.taskId"
                    class="cursor-pointer transition-colors"
                    @click="enterTaskConversation(task)"
                  >
                    <td class="px-5 py-4">
                      <p class="font-semibold text-slate-900">{{ task.title }}</p>
                      <p v-if="task.description" class="mt-1 line-clamp-1 text-xs text-slate-400">{{ task.description }}</p>
                    </td>
                    <td class="px-5 py-4 text-slate-600">{{ laneName(task.laneId) }}</td>
                    <td class="px-5 py-4">
                      <span class="rounded-full px-2.5 py-1 text-xs font-bold" :class="projectTaskPriorityClass(task.priority)">
                        {{ projectTaskPriorityLabel(task.priority) }}
                      </span>
                    </td>
                    <td class="px-5 py-4 text-slate-600">{{ task.ownerName || '未指定' }}</td>
                    <td class="px-5 py-4 text-slate-600">{{ task.dueDate ? formatDateTime(task.dueDate) : '未设置' }}</td>
                    <td class="px-5 py-4 text-slate-600">{{ task.customerName || project.customerName || '未关联' }}</td>
                    <td class="px-5 py-4 text-slate-500">
                      <div class="flex items-center gap-2">
                        <span class="inline-flex items-center gap-1" :class="task.hasAttachments ? 'text-primary' : 'text-slate-300'">
                          <span class="material-symbols-outlined text-[15px]">attach_file</span>
                        </span>
                        <span class="inline-flex items-center gap-1" :class="task.hasSchedule ? 'text-emerald-600' : 'text-slate-300'">
                          <span class="material-symbols-outlined text-[15px]">calendar_month</span>
                        </span>
                        <span v-if="task.generatedByAi" class="inline-flex items-center gap-1 text-primary">
                          <span class="material-symbols-outlined text-[15px]">auto_awesome</span>
                        </span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div v-else-if="viewMode === 'cards'" class="min-h-0 flex-1 overflow-y-auto px-4 pb-4 pt-3 md:px-6">
          <div class="flex w-full flex-col gap-4">
            <div v-if="filteredProjectTasks.length === 0" class="rounded-[30px] border border-[var(--wk-input-border)] bg-white px-6 py-20 text-center text-slate-400 shadow-sm">
              <span class="material-symbols-outlined text-5xl">view_cozy</span>
              <p class="mt-4 text-sm">{{ projectTaskSearchKeyword.trim() ? '没有匹配的任务。' : '当前项目还没有可查看的任务。' }}</p>
            </div>

            <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              <article
                v-for="task in filteredProjectTasks"
                :key="task.taskId"
                class="group cursor-pointer rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-lg hover:shadow-slate-200/60"
                @click="enterTaskConversation(task)"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <h3 class="line-clamp-2 text-base font-bold leading-6 text-slate-900 transition-colors group-hover:text-primary">
                      {{ task.title }}
                    </h3>
                    <p class="mt-2 text-xs text-slate-400">{{ laneName(task.laneId) }} · {{ task.status }}</p>
                  </div>
                  <span class="shrink-0 rounded-full px-2.5 py-1 text-xs font-bold" :class="projectTaskPriorityClass(task.priority)">
                    {{ projectTaskPriorityLabel(task.priority) }}
                  </span>
                </div>
                <p class="mt-4 line-clamp-3 min-h-[4.5rem] text-sm leading-6 text-slate-500">
                  {{ task.description || '暂无任务描述，可进入任务对话继续补充资料和执行方案。' }}
                </p>
                <div class="mt-5 grid grid-cols-2 gap-2 text-xs text-slate-500">
                  <div class="rounded-2xl bg-slate-50 px-3 py-2">
                    <p class="text-slate-400">负责人</p>
                    <p class="mt-1 truncate font-semibold text-slate-800">{{ task.ownerName || '未指定' }}</p>
                  </div>
                  <div class="rounded-2xl bg-slate-50 px-3 py-2">
                    <p class="text-slate-400">截止时间</p>
                    <p class="mt-1 truncate font-semibold text-slate-800">{{ task.dueDate ? formatDateTime(task.dueDate) : '未设置' }}</p>
                  </div>
                </div>
                <div class="mt-4 flex items-center justify-between text-xs text-slate-400">
                  <span class="truncate">{{ task.customerName || project.customerName || '未关联客户' }}</span>
                  <span class="inline-flex items-center gap-2">
                    <span :class="task.hasAttachments ? 'text-primary' : ''" class="material-symbols-outlined text-[15px]">attach_file</span>
                    <span :class="task.hasSchedule ? 'text-emerald-600' : ''" class="material-symbols-outlined text-[15px]">calendar_month</span>
                    <span v-if="task.generatedByAi" class="material-symbols-outlined text-[15px] text-primary">auto_awesome</span>
                  </span>
                </div>
              </article>
            </div>
          </div>
        </div>

        <div v-else-if="viewMode === 'members'" class="h-full overflow-y-auto px-4 py-4 md:px-6">
          <div class="mx-auto flex max-w-7xl flex-col gap-4">
            <section class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <article class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <p class="text-sm text-slate-500">项目成员</p>
                <p class="mt-3 text-3xl font-black text-slate-900">{{ project.members.length }}</p>
              </article>
              <article class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <p class="text-sm text-slate-500">正常成员</p>
                <p class="mt-3 text-3xl font-black text-slate-900">{{ activeMembers.length }}</p>
              </article>
              <article class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <p class="text-sm text-slate-500">成员变更日志</p>
                <p class="mt-3 text-3xl font-black text-slate-900">{{ project.memberLogs.length }}</p>
              </article>
            </section>

            <section class="rounded-[28px] border border-slate-200 bg-white shadow-sm">
              <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4 md:px-6">
                <div>
                  <h2 class="text-lg font-bold text-slate-900">项目成员</h2>
                  <p class="mt-1 text-sm text-slate-500">按角色和权限控制项目可见性、任务协作和 AI 使用范围。</p>
                </div>
                <button
                  v-if="canAddMember"
                  type="button"
                  class="rounded-2xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-slate-700"
                  @click="openAddMember"
                >
                  添加成员
                </button>
              </div>

              <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-slate-100 text-sm">
                  <thead class="bg-slate-50 text-left text-slate-500">
                    <tr>
                      <th class="px-5 py-3 font-semibold">成员姓名</th>
                      <th class="px-5 py-3 font-semibold">所属部门</th>
                      <th class="px-5 py-3 font-semibold">项目角色</th>
                      <th class="px-5 py-3 font-semibold">权限范围</th>
                      <th class="px-5 py-3 font-semibold">加入时间</th>
                      <th class="px-5 py-3 font-semibold">最近操作时间</th>
                      <th class="px-5 py-3 font-semibold">状态</th>
                      <th class="px-5 py-3 font-semibold text-right">操作</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-100">
                    <tr v-for="member in memberRows" :key="member.memberId" class="align-top">
                      <td class="px-5 py-4">
                        <p class="font-semibold text-slate-900">{{ member.memberName }}</p>
                        <p class="mt-1 text-xs text-slate-400">{{ member.account }}</p>
                      </td>
                      <td class="px-5 py-4 text-slate-600">{{ member.deptName || '-' }}</td>
                      <td class="px-5 py-4 text-slate-600">{{ projectRoleLabel(member.role) }}</td>
                      <td class="px-5 py-4 text-slate-600">
                        <p class="max-w-md leading-6">{{ memberPermissionSummary(member) }}</p>
                      </td>
                      <td class="px-5 py-4 text-slate-600">{{ formatDateTime(member.joinedAt) }}</td>
                      <td class="px-5 py-4 text-slate-600">{{ formatDateTime(member.lastActionTime) }}</td>
                      <td class="px-5 py-4">
                        <span class="inline-flex rounded-full px-2.5 py-1 text-xs font-bold" :class="projectMemberStatusClass(member.status)">
                          {{ projectMemberStatusLabel(member.status) }}
                        </span>
                      </td>
                      <td class="px-5 py-4">
                        <div class="flex justify-end gap-2">
                          <button
                            v-if="canModifyMemberPermission && member.role !== 'OWNER'"
                            type="button"
                            class="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                            @click="openEditMember(member)"
                          >
                            修改角色
                          </button>
                          <button
                            v-if="canModifyMemberPermission && member.role !== 'OWNER'"
                            type="button"
                            class="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                            @click="openEditMember(member)"
                          >
                            修改权限
                          </button>
                          <button
                            v-if="canModifyMemberPermission && member.role !== 'OWNER'"
                            type="button"
                            class="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-50"
                            @click="toggleMemberStatus(member)"
                          >
                            {{ member.status === 'DISABLED' ? '启用成员' : '停用成员' }}
                          </button>
                          <button
                            v-if="canRemoveMember && member.role !== 'OWNER' && member.status !== 'REMOVED'"
                            type="button"
                            class="rounded-xl bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-600 transition-colors hover:bg-red-100"
                            @click="removeMember(member)"
                          >
                            移除成员
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="rounded-[28px] border border-slate-200 bg-white shadow-sm">
              <div class="border-b border-slate-100 px-5 py-4 md:px-6">
                <h2 class="text-lg font-bold text-slate-900">成员变更记录</h2>
                <p class="mt-1 text-sm text-slate-500">记录添加成员、移除成员、修改角色、修改权限和状态变更。</p>
              </div>
              <div v-if="project.memberLogs.length === 0" class="px-6 py-14 text-center text-slate-400">
                还没有成员变更记录
              </div>
              <div v-else class="divide-y divide-slate-100">
                <article v-for="log in project.memberLogs" :key="log.logId" class="px-5 py-4 md:px-6">
                  <div class="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                    <div>
                      <p class="text-sm font-semibold text-slate-900">{{ log.operatorName }} 执行了 {{ memberActionLabel(log.actionType) }}</p>
                      <p class="mt-1 text-sm text-slate-500">目标成员：{{ log.targetUserName }}</p>
                      <p v-if="log.beforeSummary" class="mt-2 text-xs text-slate-400">变更前：{{ log.beforeSummary }}</p>
                      <p v-if="log.afterSummary" class="mt-1 text-xs text-slate-400">变更后：{{ log.afterSummary }}</p>
                    </div>
                    <p class="text-xs text-slate-400">{{ formatDateTime(log.createTime) }}</p>
                  </div>
                </article>
              </div>
            </section>
          </div>
        </div>
      </main>
    </div>

    <div v-else class="flex flex-1 items-center justify-center px-6 text-center">
      <div>
        <span class="material-symbols-outlined text-5xl text-slate-300">folder_off</span>
        <p class="mt-4 text-sm text-slate-500">
          {{ project ? '当前账号没有查看该项目的权限。' : '项目不存在或已被删除。' }}
        </p>
        <button
          type="button"
          class="mt-4 rounded-2xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white"
          @click="router.push({ name: 'ProjectList' })"
        >
          返回项目列表
        </button>
      </div>
    </div>

    <ProjectUpsertDialog
      v-model="showProjectDialog"
      :editing-project="project"
      @submit="handleUpdateProject"
    />

    <ProjectTaskDialog
      v-model="showTaskDialog"
      :editing-task="editingTask"
      :lanes="orderedLanes"
      :default-lane-id="defaultLaneId"
      :default-customer-id="project?.customerId"
      :default-customer-name="project?.customerName"
      @submit="handleSubmitTask"
    />

    <ProjectTaskDrawer
      v-model="showTaskDrawer"
      :task="selectedTask"
      :project-name="project?.name"
      :is-mobile="isMobile"
      :can-edit="selectedTask ? canEditTask(selectedTask) : false"
      :can-delete="selectedTask ? canDeleteTask(selectedTask) : false"
      @enter-chat="enterTaskConversation"
      @edit="handleEditTask"
      @delete="handleDeleteTask"
    />

    <ProjectMemberDialog
      v-model="showMemberDialog"
      :editing-member="editingMember"
      @submit="handleSubmitMember"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { useProjectStore } from '@/stores/project'
import { renderMarkdown } from '@/utils/markdown'
import { appEvents, APP_EVENT } from '@/utils/events'
import type {
  ProjectMember,
  ProjectPermission,
  ProjectStatus,
  ProjectTask,
  ProjectViewMode
} from '@/types/project'
import {
  formatDateTime,
  memberPermissionSummary,
  projectMemberStatusClass,
  projectMemberStatusLabel,
  projectRoleLabel,
  projectStatusClass,
  projectStatusLabel,
  projectTaskPriorityClass,
  projectTaskPriorityLabel
} from '@/utils/project'
import ProjectMemberDialog from '@/views/project/components/ProjectMemberDialog.vue'
import ProjectChatComposer from '@/views/project/components/ProjectChatComposer.vue'
import ProjectTaskDialog from '@/views/project/components/ProjectTaskDialog.vue'
import ProjectTaskDrawer from '@/views/project/components/ProjectTaskDrawer.vue'
import ProjectUpsertDialog from '@/views/project/components/ProjectUpsertDialog.vue'

const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()
const projectStore = useProjectStore()

type ProjectTab = {
  value: 'board' | 'list' | 'cards'
  label: string
  icon: string
}

const projectTabs: ProjectTab[] = [
  { value: 'board', label: '泳道视图', icon: 'view_kanban' },
  { value: 'list', label: '列表视图', icon: 'list' },
  { value: 'cards', label: '卡片视图', icon: 'grid_view' }
]

const viewMode = ref<ProjectViewMode>('board')
const lastProjectViewMode = ref<Exclude<ProjectViewMode, 'task_ai'>>('board')
const aiInput = ref('')
const taskAiInput = ref('')
const projectAiComposerInputRef = ref<HTMLTextAreaElement | null>(null)
const taskAiComposerInputRef = ref<HTMLTextAreaElement | null>(null)
const showProjectDialog = ref(false)
const showTaskDialog = ref(false)
const showTaskDrawer = ref(false)
const showMemberDialog = ref(false)
const taskDetailPanelVisible = ref(true)
const draggingTaskId = ref('')
const dragOverLaneId = ref('')
const defaultLaneId = ref('')
const selectedTaskId = ref('')
const editingTask = ref<ProjectTask | null>(null)
const editingMember = ref<ProjectMember | null>(null)
const taskConversationId = ref('')
const projectTaskSearchKeyword = ref('')
const projectTaskSearchLoading = ref(false)
let projectTaskSearchTimer: number | null = null

const projectViewValues = new Set<Exclude<ProjectViewMode, 'task_ai'>>(['board', 'list', 'cards', 'ai', 'members'])

function resolveProjectViewFromRoute(): Exclude<ProjectViewMode, 'task_ai'> {
  const raw = route.query.view
  const value = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  return projectViewValues.has(value as Exclude<ProjectViewMode, 'task_ai'>)
    ? value as Exclude<ProjectViewMode, 'task_ai'>
    : 'board'
}

onMounted(async () => {
  await projectStore.ensureInitialized()
  if (projectId.value) {
    await projectStore.loadProject(projectId.value)
  }
  syncTaskConversationFromRoute()
})

watch(() => [route.query.taskId, route.query.view], () => {
  syncTaskConversationFromRoute()
})

const projectId = computed(() => String(route.params.id || ''))
const project = computed(() => projectStore.getProjectById(projectId.value))

watch(projectTaskSearchKeyword, keyword => {
  if (projectTaskSearchTimer) {
    window.clearTimeout(projectTaskSearchTimer)
  }
  projectTaskSearchTimer = window.setTimeout(() => {
    void reloadProjectWithTaskSearch(keyword)
  }, 300)
})

onBeforeUnmount(() => {
  if (projectTaskSearchTimer) {
    window.clearTimeout(projectTaskSearchTimer)
  }
})

const projectAttachments = computed(() => project.value?.attachments || [])
const projectSchedules = computed(() => project.value?.schedules || [])
const canViewProject = computed(() => projectStore.getUserProjectPermission(projectId.value, 'VIEW_PROJECT'))
const orderedLanes = computed(() =>
  [...(project.value?.lanes || [])].sort((a, b) => a.order - b.order)
)
const activeMembers = computed(() => (project.value?.members || []).filter(member => member.status === 'ACTIVE'))
const memberRows = computed(() =>
  [...(project.value?.members || [])].sort((a, b) => new Date(b.lastActionTime).getTime() - new Date(a.lastActionTime).getTime())
)
const visibleProjectTasks = computed(() =>
  (project.value?.tasks || [])
    .filter(task => projectStore.canCurrentUserViewTask(projectId.value, task))
    .sort((a, b) => new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime())
)
const filteredProjectTasks = computed(() => {
  const keyword = projectTaskSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return visibleProjectTasks.value

  return visibleProjectTasks.value.filter(task => {
    const searchText = [
      task.title,
      task.description,
      laneName(task.laneId),
      task.status,
      task.priority,
      projectTaskPriorityLabel(task.priority),
      task.ownerName,
      task.customerName,
      project.value?.customerName,
      ...(task.participantNames || [])
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    return searchText.includes(keyword)
  })
})

const canEditProject = computed(() => projectStore.getUserProjectPermission(projectId.value, 'EDIT_PROJECT'))
const canDeleteProject = computed(() => projectStore.getUserProjectPermission(projectId.value, 'DELETE_PROJECT'))
const canArchiveProject = computed(() => projectStore.getUserProjectPermission(projectId.value, 'ARCHIVE_PROJECT'))
const canUseAiChat = computed(() => projectStore.getUserProjectPermission(projectId.value, 'USE_AI_CHAT'))
const canCreateTask = computed(() => projectStore.getUserProjectPermission(projectId.value, 'CREATE_TASK'))
const canDeleteTaskPermission = computed(() => projectStore.getUserProjectPermission(projectId.value, 'DELETE_TASK'))
const canAddLane = computed(() => projectStore.getUserProjectPermission(projectId.value, 'ADD_LANE'))
const canEditLane = computed(() => projectStore.getUserProjectPermission(projectId.value, 'EDIT_LANE'))
const canDeleteLane = computed(() => projectStore.getUserProjectPermission(projectId.value, 'DELETE_LANE'))
const canAddMember = computed(() => projectStore.getUserProjectPermission(projectId.value, 'ADD_MEMBER'))
const canRemoveMember = computed(() => projectStore.getUserProjectPermission(projectId.value, 'REMOVE_MEMBER'))
const canModifyMemberPermission = computed(() => projectStore.getUserProjectPermission(projectId.value, 'MODIFY_MEMBER_PERMISSION'))

const selectedTask = computed(() =>
  project.value?.tasks.find(task => task.taskId === selectedTaskId.value) || null
)
const currentTaskConversation = computed(() =>
  project.value?.tasks.find(task => task.taskId === taskConversationId.value) || null
)
const isTaskConversation = computed(() => viewMode.value === 'task_ai')
const isProjectChatView = computed(() => viewMode.value === 'ai' || viewMode.value === 'task_ai')
const showTaskViewToolbar = computed(() => viewMode.value === 'board' || viewMode.value === 'list' || viewMode.value === 'cards')
const canUseCurrentTaskAi = computed(() =>
  currentTaskConversation.value ? projectStore.canCurrentUserUseTaskAi(projectId.value, currentTaskConversation.value) : false
)
const showTaskDetailPanelShell = computed(() => !isMobile.value && isTaskConversation.value && Boolean(currentTaskConversation.value))

function renderAssistantMessage(content: string) {
  return renderMarkdown(content || '')
}

function renderTaskAssistantMessage(content: string) {
  return renderAssistantMessage(content)
}

function htmlToPlainText(html: string): string {
  const div = document.createElement('div')
  div.innerHTML = html
  return (div.textContent || '').replace(/\u00a0/g, ' ').trim()
}

async function copyToClipboard(text: string) {
  const value = text.trim()
  if (!value) return

  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = value
    textarea.setAttribute('readonly', 'true')
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (ok) ElMessage.success('已复制')
    else ElMessage.warning('复制失败')
  }
}

async function copyProjectAssistantMessage(content: string) {
  await copyToClipboard(htmlToPlainText(renderAssistantMessage(content)))
}

function resizeProjectChatTextarea(el: HTMLTextAreaElement | null) {
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 220)}px`
}

function handleProjectChatComposerInput() {
  resizeProjectChatTextarea(projectAiComposerInputRef.value)
}

function handleTaskChatComposerInput() {
  resizeProjectChatTextarea(taskAiComposerInputRef.value)
}

watch(currentTaskConversation, task => {
  if (!task && viewMode.value === 'task_ai') {
    backToProject()
  }
  void nextTick(() => resizeProjectChatTextarea(taskAiComposerInputRef.value))
})

watch(aiInput, () => {
  void nextTick(() => resizeProjectChatTextarea(projectAiComposerInputRef.value))
})

watch(taskAiInput, () => {
  void nextTick(() => resizeProjectChatTextarea(taskAiComposerInputRef.value))
})

function syncTaskConversationFromRoute() {
  const routeTaskId = typeof route.query.taskId === 'string' ? route.query.taskId : ''
  if (routeTaskId && routeTaskId !== taskConversationId.value) {
    taskDetailPanelVisible.value = true
  }
  taskConversationId.value = routeTaskId
  if (routeTaskId) {
    viewMode.value = 'task_ai'
  } else if (viewMode.value === 'task_ai') {
    viewMode.value = resolveProjectViewFromRoute()
    lastProjectViewMode.value = viewMode.value
  } else {
    viewMode.value = resolveProjectViewFromRoute()
    lastProjectViewMode.value = viewMode.value
  }
}

function switchProjectView(mode: Exclude<ProjectViewMode, 'task_ai'>) {
  lastProjectViewMode.value = mode
  viewMode.value = mode
  if (taskConversationId.value) {
    taskConversationId.value = ''
    router.replace({ query: { ...route.query, taskId: undefined, view: mode } })
    return
  }
  router.replace({ query: { ...route.query, view: mode } })
}

function handleProjectMoreAction(command: string | number) {
  const action = String(command)
  if (action === 'edit-project') {
    openEditProject()
    return
  }
  if (action === 'members') {
    switchProjectView('members')
    return
  }
  if (action === 'archive-project') {
    void handleArchiveProject()
    return
  }
  if (action === 'delete-project') {
    void handleDeleteProject()
  }
}

function handleBackClick() {
  if (isTaskConversation.value) {
    backToProject()
    return
  }
  router.push({ name: 'ProjectList' })
}

function backToProject() {
  viewMode.value = lastProjectViewMode.value
  taskConversationId.value = ''
  taskAiInput.value = ''
  router.replace({ query: { ...route.query, taskId: undefined, view: lastProjectViewMode.value } })
}

async function reloadProjectWithTaskSearch(keyword = projectTaskSearchKeyword.value) {
  if (!projectId.value) return
  const normalizedKeyword = keyword.trim()
  projectTaskSearchLoading.value = true
  try {
    await projectStore.loadProject(projectId.value, normalizedKeyword || undefined)
  } finally {
    projectTaskSearchLoading.value = false
  }
}

function tasksByLane(laneId: string) {
  return filteredProjectTasks.value
    .filter(task => task.laneId === laneId)
    .sort((a, b) => new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime())
}

function laneName(laneId: string) {
  return orderedLanes.value.find(lane => lane.laneId === laneId)?.name || '未分配泳道'
}

function canMoveTask(task: ProjectTask) {
  return projectStore.canCurrentUserMoveTask(projectId.value, task)
}

function canEditTask(task: ProjectTask) {
  return projectStore.canCurrentUserEditTask(projectId.value, task)
}

function canDeleteTask(task: ProjectTask) {
  return canDeleteTaskPermission.value && canEditTask(task)
}

function openEditProject() {
  if (!canEditProject.value) return
  showProjectDialog.value = true
}

async function handleUpdateProject(payload: {
  name: string
  description?: string
  customerId?: string
  customerName?: string
  ownerId?: string
  ownerName?: string
  startDate?: string
  dueDate?: string
  status: ProjectStatus
}) {
  if (!project.value || !canEditProject.value) return
  await projectStore.updateProject({
    projectId: project.value.projectId,
    ...payload
  })
  ElMessage.success('项目信息已更新')
}

async function handleArchiveProject() {
  if (!project.value || !canArchiveProject.value) return
  try {
    await ElMessageBox.confirm(`确定归档项目“${project.value.name}”吗？`, '提示', { type: 'warning' })
    await projectStore.archiveProject(project.value.projectId)
    ElMessage.success('项目已归档')
  } catch {
    // noop
  }
}

async function handleDeleteProject() {
  if (!project.value || !canDeleteProject.value) return
  const deletingProject = project.value
  try {
    await ElMessageBox.confirm(
      `确定删除项目“${deletingProject.name}”吗？删除后项目及任务将不可恢复。`,
      '提示',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await projectStore.deleteProject(deletingProject.projectId)
    appEvents.emit(APP_EVENT.PROJECT_SIDEBAR_REFRESH, { source: 'project-detail' })
    ElMessage.success('项目已删除')
    await router.push({ name: 'ProjectList' })
  } catch {
    // noop
  }
}

function openCreateTask(laneId?: string) {
  if (!canCreateTask.value) return
  editingTask.value = null
  defaultLaneId.value = laneId || orderedLanes.value[0]?.laneId || ''
  showTaskDialog.value = true
}

async function handleSubmitTask(payload: {
  title: string
  description?: string
  laneId: string
  dueDate?: string
  ownerId?: string
  ownerName?: string
  participantIds?: string[]
  participantNames?: string[]
  priority: ProjectTask['priority']
  customerId?: string
  customerName?: string
  hasAttachments: boolean
  hasSchedule: boolean
}) {
  if (!project.value) return
  if (editingTask.value) {
    if (!canEditTask(editingTask.value)) return
    await projectStore.updateTask(project.value.projectId, {
      taskId: editingTask.value.taskId,
      ...payload
    })
    ElMessage.success('任务已更新')
  } else {
    if (!canCreateTask.value) return
    await projectStore.createTask(project.value.projectId, payload)
    ElMessage.success('任务创建成功')
  }
}

function openTaskDetail(task: ProjectTask) {
  if (!projectStore.canCurrentUserViewTask(projectId.value, task)) return
  selectedTaskId.value = task.taskId
  showTaskDrawer.value = true
}

function enterTaskConversation(task: ProjectTask) {
  if (!projectStore.canCurrentUserUseTaskAi(projectId.value, task)) {
    ElMessage.warning('当前账号没有使用该任务 AI 对话的权限')
    return
  }
  showTaskDrawer.value = false
  selectedTaskId.value = task.taskId
  taskConversationId.value = task.taskId
  taskDetailPanelVisible.value = true
  if (viewMode.value !== 'task_ai') {
    lastProjectViewMode.value = viewMode.value as Exclude<ProjectViewMode, 'task_ai'>
  }
  viewMode.value = 'task_ai'
  router.replace({ query: { ...route.query, taskId: task.taskId } })
}

function handleEditTask(task: ProjectTask) {
  if (!canEditTask(task)) return
  editingTask.value = task
  showTaskDrawer.value = false
  showTaskDialog.value = true
}

async function handleDeleteTask(task: ProjectTask) {
  if (!project.value || !canDeleteTask(task)) return
  try {
    await ElMessageBox.confirm(`确定删除任务“${task.title}”吗？`, '提示', { type: 'warning' })
    await projectStore.deleteTask(project.value.projectId, task.taskId)
    showTaskDrawer.value = false
    if (taskConversationId.value === task.taskId) {
      backToProject()
    }
    if (selectedTaskId.value === task.taskId) {
      selectedTaskId.value = ''
    }
    ElMessage.success('任务已删除')
  } catch {
    // noop
  }
}

function handleTaskDragStart(taskId: string, event: DragEvent) {
  const task = project.value?.tasks.find(item => item.taskId === taskId)
  if (!task || !canMoveTask(task)) {
    event.preventDefault()
    return
  }
  draggingTaskId.value = taskId
  event.dataTransfer?.setData('text/plain', taskId)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function handleTaskDragEnd() {
  draggingTaskId.value = ''
  dragOverLaneId.value = ''
}

function handleLaneDragOver(laneId: string, event: DragEvent) {
  const task = project.value?.tasks.find(item => item.taskId === draggingTaskId.value)
  if (!task || !canMoveTask(task)) return
  dragOverLaneId.value = laneId
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

async function handleLaneDrop(laneId: string) {
  if (!project.value || !draggingTaskId.value) return
  const task = project.value.tasks.find(item => item.taskId === draggingTaskId.value)
  if (!task || !canMoveTask(task)) return
  await projectStore.moveTask(project.value.projectId, draggingTaskId.value, laneId)
  ElMessage.success('任务状态已更新')
  handleTaskDragEnd()
}

async function promptAddLane() {
  if (!project.value || !canAddLane.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入泳道名称', '新增泳道', {
      inputPlaceholder: '例如：待客户反馈'
    })
    if (!value?.trim()) return
    await projectStore.addLane(project.value.projectId, value)
    ElMessage.success('泳道已新增')
  } catch {
    // noop
  }
}

async function promptRenameLane(laneId: string) {
  if (!project.value || !canEditLane.value) return
  const lane = project.value.lanes.find(item => item.laneId === laneId)
  if (!lane) return
  try {
    const { value } = await ElMessageBox.prompt('修改泳道名称', '编辑泳道', {
      inputValue: lane.name
    })
    if (!value?.trim()) return
    await projectStore.updateLane(project.value.projectId, laneId, value)
    ElMessage.success('泳道已更新')
  } catch {
    // noop
  }
}

async function handleDeleteLane(laneId: string) {
  if (!project.value || !canDeleteLane.value) return
  const lane = project.value.lanes.find(item => item.laneId === laneId)
  if (!lane || lane.system) return
  try {
    await ElMessageBox.confirm(`删除泳道“${lane.name}”后，泳道内任务会自动回到“未开始”。是否继续？`, '提示', {
      type: 'warning'
    })
    await projectStore.deleteLane(project.value.projectId, laneId)
    ElMessage.success('泳道已删除')
  } catch {
    // noop
  }
}

async function handleSendAiMessage() {
  if (!project.value || !aiInput.value.trim() || !canUseAiChat.value) return
  await projectStore.handleAiCommand(project.value.projectId, aiInput.value.trim())
  aiInput.value = ''
  void nextTick(() => resizeProjectChatTextarea(projectAiComposerInputRef.value))
}

async function handleSendTaskAiMessage() {
  if (!project.value || !currentTaskConversation.value || !taskAiInput.value.trim() || !canUseCurrentTaskAi.value) return
  await projectStore.handleTaskAiCommand(project.value.projectId, currentTaskConversation.value.taskId, taskAiInput.value.trim())
  taskAiInput.value = ''
  void nextTick(() => resizeProjectChatTextarea(taskAiComposerInputRef.value))
}

function openAddMember() {
  if (!canAddMember.value) return
  editingMember.value = null
  showMemberDialog.value = true
}

function openEditMember(member: ProjectMember) {
  if (!canModifyMemberPermission.value) return
  editingMember.value = member
  showMemberDialog.value = true
}

async function handleSubmitMember(payload: {
  userId: string
  memberName: string
  account: string
  deptName?: string
  role: ProjectMember['role']
  status: ProjectMember['status']
  permissions: ProjectPermission[]
  remark?: string
}) {
  if (!project.value) return

  if (editingMember.value) {
    const original = editingMember.value
    if (payload.role !== original.role) {
      await projectStore.updateMemberRole(project.value.projectId, original.userId, payload.role)
    }
    if (JSON.stringify([...payload.permissions].sort()) !== JSON.stringify([...original.permissions].sort())) {
      await projectStore.updateMemberPermissions(project.value.projectId, original.userId, payload.permissions)
    }
    if (payload.status !== original.status) {
      await projectStore.updateMemberStatus(project.value.projectId, original.userId, payload.status)
    }

    const metaChanged = payload.memberName !== original.memberName
      || payload.account !== original.account
      || (payload.deptName || '') !== (original.deptName || '')
      || (payload.remark || '') !== (original.remark || '')

    if (metaChanged) {
      await projectStore.addMember(project.value.projectId, payload)
    }
    ElMessage.success('成员信息已更新')
    return
  }

  await projectStore.addMember(project.value.projectId, payload)
  ElMessage.success('成员添加成功')
}

async function removeMember(member: ProjectMember) {
  if (!project.value || !canRemoveMember.value) return
  try {
    await ElMessageBox.confirm(`确定移除成员“${member.memberName}”吗？`, '提示', { type: 'warning' })
    await projectStore.updateMemberStatus(project.value.projectId, member.userId, 'REMOVED')
    ElMessage.success('成员已移除')
  } catch {
    // noop
  }
}

async function toggleMemberStatus(member: ProjectMember) {
  if (!project.value || !canModifyMemberPermission.value) return
  const nextStatus = member.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED'
  await projectStore.updateMemberStatus(project.value.projectId, member.userId, nextStatus)
  ElMessage.success(nextStatus === 'ACTIVE' ? '成员已启用' : '成员已停用')
}

function memberActionLabel(action: string) {
  switch (action) {
    case 'ADD_MEMBER':
      return '添加成员'
    case 'REMOVE_MEMBER':
      return '移除成员'
    case 'UPDATE_ROLE':
      return '修改角色'
    case 'UPDATE_PERMISSION':
      return '修改权限'
    case 'UPDATE_STATUS':
      return '更新状态'
    default:
      return action
  }
}
</script>

<style scoped>
.wk-project-chat-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
  background:
    linear-gradient(180deg, var(--wk-bg-surface) 0%, rgb(var(--wk-bg-page-rgb) / 0.96) 62%, var(--wk-bg-page) 100%);
}

.wk-project-chat-header {
  position: relative;
  flex-shrink: 0;
  border-bottom: 1px solid var(--wk-border-subtle);
  background: color-mix(in srgb, var(--wk-bg-surface) 92%, transparent);
  padding: 0.5rem 0;
  backdrop-filter: blur(14px);
}

.wk-project-chat-header > .flex {
  display: none;
}

.wk-project-chat-header__inner {
  display: flex;
  width: 100%;
  height: 36px;
  min-width: 0;
  align-items: center;
  gap: 8px;
  padding-left: 1rem;
  padding-right: 1rem;
}

.wk-project-chat-badge {
  display: inline-flex;
  height: 24px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--wk-bg-surface-muted);
  padding: 0 8px;
  color: var(--wk-text-secondary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

@media (min-width: 768px) {
  .wk-project-chat-header__inner {
    padding-left: 2rem;
    padding-right: 2rem;
  }
}

.wk-project-chat-messages {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  background: transparent;
  padding-bottom: 1rem;
  padding-top: 1.5rem;
}

.wk-project-chat-messages__inner {
  min-width: 0;
  width: 100%;
}

.wk-project-chat-composer-wrap {
  flex-shrink: 0;
  background: linear-gradient(to top, var(--wk-bg-page) 72%, rgb(var(--wk-bg-page-rgb) / 0));
  padding: 0 0.5rem 0.5rem;
}

.wk-project-chat-composer {
  width: 768px;
  max-width: 100%;
  margin-left: auto;
  margin-right: auto;
  border: 1px solid var(--wk-border-subtle);
  background: var(--wk-bg-surface);
  box-shadow:
    0 20px 70px rgb(var(--wk-shadow-color) / 0.08),
    0 2px 8px rgb(var(--wk-shadow-color) / 0.05);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.wk-project-chat-composer:focus-within {
  border-color: var(--wk-border-muted);
  box-shadow:
    0 22px 78px rgb(var(--wk-shadow-color) / 0.11),
    0 0 0 1px rgb(var(--wk-primary-rgb) / 0.12);
}

.wk-project-chat-textarea {
  min-height: 90px;
  max-height: 220px;
  width: 100%;
  min-width: 0;
  resize: none;
  overflow-x: hidden;
  overflow-y: auto;
  border: 0;
  background: transparent;
  padding: 0.75rem;
  color: #0d0d0d;
  font-size: 16px;
  line-height: 26px;
  outline: none;
}

.wk-project-chat-textarea::placeholder {
  color: #909090;
}

.wk-project-chat-composer-wrap :deep(.wk-crm-el-field-input) {
  display: block;
}

.wk-project-chat-composer-wrap :deep(.el-textarea__inner) {
  min-height: 110px !important;
  resize: none;
  border: 0;
  border-radius: 28px;
  background: #f4f4f4;
  box-shadow: none;
  color: #0d0d0d;
  font-size: 16px;
  line-height: 26px;
  padding: 18px 18px;
}

.wk-project-chat-composer-wrap :deep(.el-textarea__inner:focus) {
  box-shadow: inset 0 0 0 1px rgb(13 13 13 / 0.08);
}

.wk-project-chat-composer-wrap :deep(.el-textarea__inner::placeholder) {
  color: #909090;
}

.wk-project-chat-send-button {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #0d0d0d;
  color: #fff;
  font-size: 0;
  transition:
    background-color 160ms ease,
    transform 160ms ease,
    opacity 160ms ease;
}

.wk-project-chat-send-button .material-symbols-outlined {
  font-size: 20px;
}

.wk-project-chat-send-button:hover:not(:disabled) {
  background: #575757;
}

.wk-project-chat-send-button:active:not(:disabled) {
  transform: scale(0.96);
}

.wk-project-chat-send-button:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

.wk-project-chat-message {
  width: min(100%, 768px);
  min-width: min(100%, 468px);
  max-width: 768px;
  overflow-wrap: anywhere;
}

.wk-project-chat-message--user,
.wk-project-task-chat-message--user {
  margin-bottom: 2rem;
}

.wk-project-chat-message--user :deep(.rounded-\[24px\]),
.wk-project-task-chat-message--user :deep(.rounded-\[24px\]) {
  background-color: var(--wk-bg-surface-muted) !important;
  color: var(--wk-text-primary) !important;
}

.wk-project-chat-message--assistant :deep(.wk-markdown),
.wk-project-task-chat-message--assistant :deep(.wk-markdown) {
  color: var(--wk-text-primary);
  font-size: 15px;
  line-height: 1.75;
}

.wk-project-chat-message--assistant :deep(.wk-markdown > *:first-child),
.wk-project-task-chat-message--assistant :deep(.wk-markdown > *:first-child) {
  margin-top: 0;
}

.wk-project-chat-message--assistant :deep(.wk-markdown > *:last-child),
.wk-project-task-chat-message--assistant :deep(.wk-markdown > *:last-child) {
  margin-bottom: 0;
}

.wk-project-chat-message--assistant :deep(.wk-markdown p),
.wk-project-task-chat-message--assistant :deep(.wk-markdown p) {
  margin: 0 0 0.85em;
}

.wk-project-chat-message--assistant :deep(.wk-markdown ul),
.wk-project-chat-message--assistant :deep(.wk-markdown ol),
.wk-project-task-chat-message--assistant :deep(.wk-markdown ul),
.wk-project-task-chat-message--assistant :deep(.wk-markdown ol) {
  margin: 0.85em 0;
  padding-left: 1.5em;
}

.wk-project-chat-message--assistant :deep(.wk-markdown li),
.wk-project-task-chat-message--assistant :deep(.wk-markdown li) {
  margin: 0.25em 0;
}

.wk-project-chat-message--assistant :deep(.wk-markdown pre),
.wk-project-task-chat-message--assistant :deep(.wk-markdown pre) {
  margin: 1em 0;
  overflow-x: auto;
  border-radius: 12px;
  background: #1f1e1c;
  padding: 14px 16px;
  color: #f7f7f7;
}

.wk-project-chat-message--assistant :deep(.wk-markdown code),
.wk-project-task-chat-message--assistant :deep(.wk-markdown code) {
  border-radius: 6px;
  background: var(--wk-bg-surface-muted);
  padding: 0.15em 0.35em;
  color: var(--wk-text-primary);
  font-size: 0.92em;
}

.wk-project-chat-message--assistant :deep(.wk-markdown pre code),
.wk-project-task-chat-message--assistant :deep(.wk-markdown pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.wk-project-chat-message--assistant :deep(.wk-markdown blockquote),
.wk-project-task-chat-message--assistant :deep(.wk-markdown blockquote) {
  margin: 1em 0;
  border-left: 3px solid var(--wk-border-strong);
  padding-left: 1em;
  color: var(--wk-text-secondary);
}

.wk-project-chat-message--assistant :deep(.wk-markdown table),
.wk-project-task-chat-message--assistant :deep(.wk-markdown table) {
  width: 100%;
  margin: 1em 0;
  border-collapse: collapse;
  font-size: 14px;
}

.wk-project-chat-message--assistant :deep(.wk-markdown th),
.wk-project-chat-message--assistant :deep(.wk-markdown td),
.wk-project-task-chat-message--assistant :deep(.wk-markdown th),
.wk-project-task-chat-message--assistant :deep(.wk-markdown td) {
  border: 1px solid var(--wk-border-subtle);
  padding: 8px 10px;
  text-align: left;
}

.wk-project-chat-message--assistant :deep(.wk-markdown th),
.wk-project-task-chat-message--assistant :deep(.wk-markdown th) {
  background: var(--wk-bg-surface-subtle);
  font-weight: 600;
}

.wk-project-chat-customer-panel {
  width: clamp(300px, 27vw, 420px);
  min-width: 280px;
  background: var(--wk-bg-surface);
  border-color: var(--wk-border-subtle);
}

.wk-project-task-detail-panel {
  width: 380px;
  min-width: min(380px, 50%);
  max-width: 50%;
  background: var(--wk-bg-surface);
  border-color: var(--wk-border-subtle);
}

.wk-project-task-detail-section {
  border-top: 1px solid var(--wk-border-subtle);
  padding-top: 14px;
}

.wk-project-task-detail-section:first-child {
  border-top: 0;
  padding-top: 0;
}

.wk-project-task-detail-section__title {
  color: #0d0d0d;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
}

.wk-project-task-detail-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  color: #5f5f5f;
  font-size: 13px;
  line-height: 22px;
}

.wk-project-task-detail-row dt {
  color: #8f8f8f;
}

.wk-project-task-detail-row dd {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #0d0d0d;
}

.wk-project-task-detail-stat {
  min-width: 0;
  border-radius: 12px;
  background: #f5f5f5;
  padding: 10px 8px;
  text-align: center;
}

.wk-project-task-detail-stat span {
  display: block;
  color: #0d0d0d;
  font-size: 18px;
  font-weight: 700;
  line-height: 22px;
}

.wk-project-task-detail-stat p {
  margin-top: 2px;
  color: #8f8f8f;
  font-size: 12px;
  line-height: 16px;
}

.wk-project-object-panel-toggle {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #8f8f8f;
  transition:
    background-color 160ms ease,
    color 160ms ease;
}

.wk-project-object-panel-toggle:hover {
  background: #efefef;
  color: #0d0d0d;
}

.wk-project-object-panel-toggle--inside {
  position: absolute;
  top: 50%;
  right: 1rem;
  transform: translateY(-50%);
  z-index: 20;
}

.wk-project-object-panel-toggle--floating {
  position: absolute;
  top: 1rem;
  right: 1rem;
  z-index: 20;
  background: rgb(255 255 255 / 0.72);
  box-shadow:
    0 12px 28px rgb(15 23 42 / 0.08),
    inset 0 1px 0 rgb(255 255 255 / 0.92);
  backdrop-filter: blur(16px);
}

.wk-project-object-panel-tooltip {
  pointer-events: none;
  position: absolute;
  right: 100%;
  top: 50%;
  z-index: 200;
  margin-right: 8px;
  transform: translateY(-50%);
  white-space: nowrap;
  border-radius: 8px;
  background: #000;
  padding: 6px 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  opacity: 0;
  box-shadow: 0 8px 20px rgb(15 23 42 / 0.18);
  transition: opacity 150ms ease;
}

.wk-project-object-panel-toggle:hover .wk-project-object-panel-tooltip {
  opacity: 1;
}

.wk-project-task-list-table-shell {
  background: var(--wk-bg-surface);
}

.wk-project-task-list-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: var(--wk-bg-surface);
  color: var(--wk-text-secondary);
}

.wk-project-task-list-table th {
  box-sizing: border-box;
  border-bottom: 1px solid var(--wk-border-muted);
  background: var(--wk-bg-surface-subtle);
  color: var(--wk-text-muted);
  padding: 16px 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
  vertical-align: middle;
}

.wk-project-task-list-table td {
  box-sizing: border-box;
  border-bottom: 1px solid var(--wk-border-subtle);
  padding: 16px 20px;
  color: var(--wk-text-secondary);
  vertical-align: middle;
}

.wk-project-task-list-table th:nth-child(1),
.wk-project-task-list-table td:nth-child(1) {
  width: 360px;
  min-width: 360px;
}

.wk-project-task-list-table th:nth-child(2),
.wk-project-task-list-table td:nth-child(2),
.wk-project-task-list-table th:nth-child(3),
.wk-project-task-list-table td:nth-child(3),
.wk-project-task-list-table th:nth-child(4),
.wk-project-task-list-table td:nth-child(4),
.wk-project-task-list-table th:nth-child(7),
.wk-project-task-list-table td:nth-child(7) {
  width: 100px;
  min-width: 100px;
}

.wk-project-task-list-table th:nth-child(5),
.wk-project-task-list-table td:nth-child(5) {
  width: 170px;
  min-width: 170px;
}

.wk-project-task-list-table th:nth-child(6),
.wk-project-task-list-table td:nth-child(6) {
  width: 150px;
  min-width: 150px;
}

.wk-project-task-list-table tbody tr:last-child td {
  border-bottom: 0;
}

.wk-project-task-list-table tbody tr:hover td {
  background: color-mix(in srgb, var(--wk-primary) 11%, var(--wk-bg-surface));
}

.message-enter {
  animation: messageSlideIn 0.3s ease-out;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.wk-project-chat-shell--task {
  background:
    linear-gradient(180deg, var(--wk-bg-surface) 0%, rgb(var(--wk-bg-page-rgb) / 0.96) 62%, var(--wk-bg-page) 100%);
}

.wk-project-task-chat-header {
  position: relative;
  flex-shrink: 0;
  border-bottom: 1px solid var(--wk-border-subtle);
  background: color-mix(in srgb, var(--wk-bg-surface) 92%, transparent);
  padding: 0.5rem 0;
  backdrop-filter: blur(14px);
}

.wk-project-task-chat-header__inner {
  display: flex;
  height: 44px;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-left: 1rem;
  padding-right: 3.5rem;
}

@media (min-width: 768px) {
  .wk-project-task-chat-header__inner {
    padding-left: 2rem;
    padding-right: 4rem;
  }
}

.wk-project-task-chat-messages {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding-bottom: 1rem;
  padding-top: 1.5rem;
}

.wk-project-task-chat-messages__inner {
  min-width: 0;
  width: 100%;
}

.wk-project-task-chat-message {
  width: min(100%, 768px);
  min-width: min(100%, 468px);
  max-width: 768px;
  overflow-wrap: anywhere;
}

.wk-project-task-chat-message--user :deep(.rounded-\[24px\]) {
  background-color: var(--wk-bg-surface-muted) !important;
  color: var(--wk-text-primary) !important;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown) {
  color: var(--wk-text-primary);
  font-size: 15px;
  line-height: 1.75;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown > *:first-child) {
  margin-top: 0;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown > *:last-child) {
  margin-bottom: 0;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown p) {
  margin: 0 0 0.85em;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown ul),
.wk-project-task-chat-message--assistant :deep(.wk-markdown ol) {
  margin: 0.85em 0;
  padding-left: 1.5em;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown li) {
  margin: 0.25em 0;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown pre) {
  margin: 1em 0;
  overflow-x: auto;
  border-radius: 12px;
  background: #1f1e1c;
  padding: 14px 16px;
  color: #f7f7f7;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown code) {
  border-radius: 6px;
  background: var(--wk-bg-surface-muted);
  padding: 0.15em 0.35em;
  color: var(--wk-text-primary);
  font-size: 0.92em;
}

.wk-project-task-chat-message--assistant :deep(.wk-markdown pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.wk-project-task-chat-composer-wrap {
  flex-shrink: 0;
  background: linear-gradient(to top, var(--wk-bg-page) 72%, rgb(var(--wk-bg-page-rgb) / 0));
  padding: 0 0.5rem 0.5rem;
}

.wk-project-task-chat-composer {
  width: 768px;
  max-width: 100%;
  border: 1px solid var(--wk-border-subtle);
  background: var(--wk-bg-surface);
  box-shadow:
    0 20px 70px rgb(var(--wk-shadow-color) / 0.08),
    0 2px 8px rgb(var(--wk-shadow-color) / 0.05);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.wk-project-task-chat-composer:focus-within {
  border-color: var(--wk-border-muted);
  box-shadow:
    0 22px 78px rgb(var(--wk-shadow-color) / 0.11),
    0 0 0 1px rgb(var(--wk-primary-rgb) / 0.12);
}

.wk-project-task-chat-textarea {
  min-height: 90px;
  width: 100%;
  min-width: 0;
  resize: none;
  overflow-x: hidden;
  overflow-y: auto;
  border: 0;
  background: transparent;
  padding: 0.75rem;
  color: #0d0d0d;
  font-size: 16px;
  line-height: 26px;
  outline: none;
}

.wk-project-task-chat-textarea::placeholder {
  color: #909090;
}

.wk-project-task-chat-send-button {
  display: inline-flex;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  align-items: center;
  align-self: flex-end;
  justify-content: center;
  border-radius: 9999px;
  background: #0d0d0d;
  color: #fff;
  font-size: 0;
  transition:
    background-color 160ms ease,
    transform 160ms ease,
    opacity 160ms ease;
}

.wk-project-task-chat-send-button .material-symbols-outlined {
  font-size: 20px;
}

.wk-project-task-chat-send-button:hover:not(:disabled) {
  background: #575757;
}

.wk-project-task-chat-send-button:active:not(:disabled) {
  transform: scale(0.96);
}

.wk-project-task-chat-send-button:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

@media (max-width: 640px) {
  .wk-project-task-chat-message {
    min-width: 0;
  }

  .wk-project-task-chat-message--user > div {
    max-width: 88%;
  }
}
</style>
