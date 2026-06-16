<template>
  <div class="flex wk-screen bg-background-light">
    <aside
      v-if="!isMobile"
      class="wk-primary-sidebar relative z-[110] flex wk-screen flex-shrink-0 flex-col border-r border-[#ececec] bg-white transition-[width] duration-200 ease-in-out"
      :class="[
        primarySidebarCollapsed ? 'w-[52px]' : 'w-72',
        primarySidebarTransitioning ? 'overflow-hidden' : 'overflow-x-visible overflow-y-visible',
      ]"
      @mouseenter="onCollapsedPrimarySidebarEnter"
      @mouseleave="onCollapsedPrimarySidebarLeave"
    >
      <!-- 展开：logo + 标题 + 折叠按钮；折叠：仅顶栏切换（ChatGPT 式） -->
      <div
        v-if="!primarySidebarContentCollapsed"
        class="relative z-30 flex items-start justify-between gap-2 overflow-visible px-3 pb-1 pt-3"
      >
        <div class="flex min-w-0 flex-1 items-start gap-2.5 pl-[10px]">
          <div
            v-if="enterpriseStore.hasLogo"
            class="size-8 shrink-0 overflow-hidden rounded-lg bg-transparent"
          >
            <img :src="enterpriseStore.logoUrl!" class="h-full w-full object-cover" alt="logo" />
          </div>
          <div v-else class="size-8 shrink-0 overflow-hidden rounded-lg bg-transparent">
            <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
          </div>
          <div v-if="!primarySidebarContentCollapsed" class="min-w-0 flex-1 pr-1 pt-0.5">
            <h1 class="line-clamp-2 text-[19px] font-semibold leading-snug text-[#0d0d0d]">
              {{ enterpriseStore.displayName }}
            </h1>
            <p class="mt-0.5 line-clamp-1 text-[11px] text-slate-500">
              {{ enterpriseStore.displayDescription }}
            </p>
          </div>
        </div>
        <div class="relative shrink-0 pt-0.5">
          <button
            type="button"
            class="group/sb-toggle relative flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#efefef]"
            aria-label="关闭边栏"
            @click="primarySidebarCollapsed = true"
          >
            <WkIcon name="fold" :size="18" class="shrink-0" />
            <span
              class="pointer-events-none absolute left-full top-1/2 z-[200] ml-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/sb-toggle:opacity-100"
              role="tooltip"
            >
              关闭边栏
            </span>
          </button>
        </div>
      </div>
      <div v-else class="relative z-30 flex justify-center overflow-visible py-2 px-0 pl-[0px]">
        <button
          type="button"
          class="group/sidebar-logo relative flex size-8 items-center justify-center overflow-visible rounded-lg bg-[#f5f5f5] text-[#0d0d0d] transition-colors hover:bg-[#ececec]"
          aria-label="展开边栏"
          @click="primarySidebarCollapsed = false"
        >
          <span
            class="absolute inset-0 flex items-center justify-center transition-opacity duration-150"
            :class="collapsedSidebarAsideHovered ? 'opacity-0' : 'opacity-100'"
            aria-hidden="true"
          >
            <span class="size-7 shrink-0 overflow-hidden rounded-lg border border-[#ececec] bg-transparent">
              <img
                v-if="enterpriseStore.hasLogo"
                :src="enterpriseStore.logoUrl!"
                class="h-full w-full object-cover"
                alt="logo"
              />
              <img v-else :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
            </span>
          </span>
          <span
            class="absolute inset-0 flex items-center justify-center transition-opacity duration-150"
            :class="collapsedSidebarAsideHovered ? 'opacity-100' : 'opacity-0'"
            aria-hidden="true"
          >
            <WkIcon name="fold" :size="18" class="shrink-0" />
          </span>
          <span
            class="pointer-events-none absolute left-full top-1/2 z-[200] ml-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/sidebar-logo:opacity-100"
            role="tooltip"
          >
            展开边栏
          </span>
        </button>
      </div>
      <!-- pl-[10px] pr-[10px] py-[6px] mt-[0px] ml-[2px] mr-[6px] -->
      <!-- Chat: New session (fixed, not scroll with nav) -->
      <div class="px-3 pt-1 mb-[0px] pr-[10px]" :class="primarySidebarContentCollapsed ? '!px-2' : ''">
        <button
          class="flex items-center rounded-lg py-2 text-sm font-normal text-[#0d0d0d] transition-colors"
          :class="[
            chatStore.isNewSessionPending && route.path.startsWith('/chat') ? 'bg-[#f3f3f3]' : 'hover:bg-[#f9f9f9]',
            primarySidebarContentCollapsed
              ? 'mx-auto w-[35px] shrink-0 justify-center px-0'
              : 'ml-[2px] mr-[6px] w-full gap-2 pl-[10px] pr-[10px] max-w-[248px]',
          ]"
          :title="primarySidebarContentCollapsed ? '新对话' : undefined"
          @click="handleNewSession()"
        >
          <!-- <span class="material-symbols-outlined wk-plus-button-icon text-[18px] leading-none">edit_square</span> -->
          <WkIcon name="new-chat" :size="18" class="shrink-0" />
          <span style="margin-left: 2px;" v-if="!primarySidebarContentCollapsed">新对话</span>
        </button>
      </div>

      <div class="ml-0 h-px w-full shrink-0 transition-colors duration-150 mr-2">
        <div
          v-if="primaryNavHasScrollbar"
          class="ml-0 h-px w-full shrink-0 transition-colors duration-150"
          :class="primaryNavScrolling ? 'bg-slate-100' : 'bg-white'"
        />
      </div>

      <div class="relative min-h-0 flex-1">
        <nav
          ref="primaryNavRef"
          class="wk-primary-nav-scroll wk-scrollbar-gutter-stable flex h-full w-full overflow-y-auto pb-4"
          :class="primarySidebarContentCollapsed ? '!px-2' : 'px-3'"
          @scroll.passive="onPrimaryNavScroll"
        >
          <div
            class="wk-primary-nav-sections flex min-h-full w-full flex-col"
            :class="primarySidebarContentCollapsed ? 'items-center' : ''"
          >

          <template v-for="group in pcMainNavGroups" :key="group.title || 'default'">
            <div v-if="group.title && !primarySidebarContentCollapsed" class="pt-[8px] pb-[3px]" style="margin-top: 8px; margin-bottom: 0px;">
              <p class="px-3 text-[14px] uppercase font-semibold tracking-tight text-[#0d0d0d]">{{ group.title }}</p>
            </div>
            <button
              v-for="item in group.items"
              :key="item.key"
              :title="primarySidebarContentCollapsed ? item.label : undefined"
              @click="handlePrimaryNavClick(item)"
              class="flex items-center rounded-lg pt-[8px] pb-[8px] transition-colors mt-[1px]"
              :class="[
                isPrimaryActive(item) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] hover:bg-[#f9f9f9]',
                primarySidebarContentCollapsed
                  ? 'mx-auto w-[35px] shrink-0 justify-center px-0'
                  : 'ml-[2px] mr-[2px] w-full gap-2 pl-[10px] pr-[8px]',
              ]"
            >
              <span
                v-if="item.materialIcon"
                class="material-symbols-outlined inline-flex size-5 shrink-0 items-center justify-center text-[20px] leading-none"
              >
                {{ item.materialIcon }}
              </span>
              <WkIcon v-else :name="item.icon" :box-size="20" class="shrink-0" />
              <span v-if="!primarySidebarContentCollapsed" class="truncate text-sm font-normal">{{ item.label }}</span>
              <span
                v-if="item.children?.length && !primarySidebarContentCollapsed"
                class="material-symbols-outlined ml-auto inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none"
                :class="isPrimarySelected(item) ? 'text-[#c9c9c9]' : 'text-[#c9c9c9]'"
              >
                chevron_right
              </span>
            </button>
          </template>

          <div
            v-if="!primarySidebarContentCollapsed && sidebarSortMode"
            class="wk-sidebar-sort-section space-y-1 pt-1"
            :style="{ order: DYNAMIC_SIDEBAR_SECTION_ORDER_BASE }"
          >
            <div
              v-for="(moduleKey, moduleIndex) in sidebarDraftModuleOrder"
              :key="moduleKey"
              class="wk-sidebar-sort-row flex w-full cursor-grab items-center gap-2 rounded-lg pl-3 pr-2 py-[7px] mt-[8px] text-left active:cursor-grabbing"
              :class="{
                'is-dragging': sidebarDraggingModuleKey === moduleKey,
                'is-drag-over': sidebarDragOverModuleKey === moduleKey && sidebarDraggingModuleKey !== moduleKey
              }"
              draggable="true"
              @dragstart="handleSidebarSortDragStart($event, moduleKey)"
              @dragover.prevent="handleSidebarSortDragOver($event, moduleKey)"
              @drop.prevent="handleSidebarSortDrop($event, moduleKey)"
              @dragend="handleSidebarSortDragEnd"
            >
              <span class="wk-sidebar-sort-row__order inline-flex size-5 shrink-0 items-center justify-center rounded-md text-[11px] font-bold tabular-nums">
                {{ moduleIndex + 1 }}
              </span>
              <span class="material-symbols-outlined inline-flex size-5 shrink-0 items-center justify-center text-[19px] leading-none text-[#8f8f8f]">
                {{ getSidebarModuleIcon(moduleKey) }}
              </span>
              <span class="min-w-0 flex-1 truncate text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">
                {{ getSidebarModuleLabel(moduleKey) }}
              </span>
              <span class="material-symbols-outlined inline-flex size-6 shrink-0 items-center justify-center text-[20px] leading-none text-[#c9c9c9]">
                drag_indicator
              </span>
            </div>
            <div class="flex items-center gap-1 px-1.5 pt-2">
              <button
                type="button"
                class="wk-sidebar-sort-save-button flex h-8 flex-1 items-center justify-center rounded-lg bg-[#0d0d0d] px-2 text-[13px] font-medium text-white disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="sidebarSavingModuleOrder"
                @click="saveSidebarModuleOrder"
              >
                保存
              </button>
              <button
                type="button"
                class="flex h-8 flex-1 items-center justify-center rounded-lg px-2 text-[13px] font-medium text-[#0d0d0d] transition-colors hover:bg-[#f3f3f3] disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="sidebarSavingModuleOrder"
                @click="cancelSidebarSortMode"
              >
                取消
              </button>
              <button
                type="button"
                class="flex h-8 flex-1 items-center justify-center rounded-lg px-2 text-[13px] font-medium text-[#8f8f8f] transition-colors hover:bg-[#f3f3f3] hover:text-[#0d0d0d] disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="sidebarSavingModuleOrder"
                @click="restoreDefaultSidebarDraftOrder"
              >
                恢复默认
              </button>
            </div>
          </div>

          <div
            v-if="!primarySidebarContentCollapsed && !sidebarSortMode && showSidebarProjects"
            class="wk-sidebar-project-section space-y-1 pt-1"
            :style="{ order: getSidebarModuleRenderOrder('project') }"
          >
            <div
              class="wk-project-header-row group/project-header flex w-full items-center gap-2 rounded-lg pl-3 pr-1 py-[6px] mt-[12px] mb-[0px] hover:!bg-[#f9f9f9]"
              :style="{ backgroundColor: projectHeaderHovered ? '#f9f9f9' : 'transparent' }"
              :title="sidebarProjectsExpanded ? '收起项目列表' : '展开项目列表'"
              @mouseenter="projectHeaderHovered = true"
              @mouseleave="projectHeaderHovered = false"
            >
              <button
                type="button"
                class="wk-project-header-toggle group flex min-w-0 flex-1 items-center gap-1 text-left"
                :aria-expanded="sidebarProjectsExpanded"
                aria-controls="sidebar-projects-panel"
                @click="sidebarProjectsExpanded = !sidebarProjectsExpanded"
              >
                <span class="min-w-0 truncate text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">项目</span>
                <span class="flex size-6 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150" aria-hidden="true">
                  <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                    {{ sidebarProjectsExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                  </span>
                </span>
              </button>
              <div class="ml-auto flex shrink-0 items-center justify-end gap-1">
                <button
                  type="button"
                  class="wk-project-header-action group/project-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="新增项目"
                  @click.stop="openCreateProjectDialog"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">add</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/project-action:opacity-100"
                    role="tooltip"
                  >
                    新增项目
                  </span>
                </button>
                <button
                  type="button"
                  class="wk-project-header-action group/project-list-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="查看项目列表"
                  @click.stop="navigateTo('/project')"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">format_list_bulleted</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/project-list-action:opacity-100"
                    role="tooltip"
                  >
                    查看项目列表
                  </span>
                </button>
              </div>
            </div>
            <div id="sidebar-projects-panel" v-show="sidebarProjectsExpanded">
              <div v-if="projectStore.loading && sidebarProjects.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarProjects.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
                暂无项目数据
              </div>
              <template v-else>
                <div
                  v-for="project in sidebarProjects"
                  :key="project.projectId"
                  role="button"
                  tabindex="0"
                  class="group/project-row flex w-full min-w-0 cursor-pointer items-center gap-2 overflow-hidden rounded-[8px] pl-[10px] pr-[8px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isProjectActive(project.projectId) ? 'bg-[#f3f3f3]' : 'hover:bg-[#f9f9f9]'"
                  @click="handleStartProjectConversation(project.projectId)"
                  @keydown.enter.self.prevent="handleStartProjectConversation(project.projectId)"
                  @keydown.space.self.prevent="handleStartProjectConversation(project.projectId)"
                >
                  <span class="material-symbols-outlined flex size-[20px] shrink-0 items-center justify-center text-[18px] leading-none text-[#8f8f8f]">
                    folder
                  </span>
                  <span class="block min-w-0 flex-1 truncate text-sm leading-5 text-[#0d0d0d]" :title="project.name">
                    {{ project.name }}
                  </span>
                </div>
              </template>
            </div>
          </div>

          <!-- Chat: recent sessions under Knowledge menu -->
          <template v-if="!primarySidebarContentCollapsed">
            <!-- <div class="pt-2" /> -->
            <!-- <div class="mx-1 h-px bg-slate-100" /> -->
            <div
              v-if="!sidebarSortMode"
              class="wk-sidebar-recent-section space-y-1 pt-0"
              :style="{ order: getSidebarModuleRenderOrder('recent') }"
            >
              <button
                type="button"
                class="group flex shrink-0 items-center gap-1 rounded-lg pl-3 pr-1 text-left transition-colors mt-[12px] mb-[0px] w-full py-[6px]"
                :title="recentChatSessionsExpanded ? '收起最近对话' : '展开最近对话'"
                :aria-expanded="recentChatSessionsExpanded"
                aria-controls="recent-chat-sessions-panel"
                @click="recentChatSessionsExpanded = !recentChatSessionsExpanded"
              >
                <span class="min-w-0 text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">最近</span>
                <span
                  class="flex size-6 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150"
                  :class="recentChatSessionsExpanded ? 'opacity-0 group-hover:opacity-100' : 'opacity-100'"
                  aria-hidden="true"
                >
                  <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                    {{ recentChatSessionsExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                  </span>
                </span>
              </button>

              <div id="recent-chat-sessions-panel" v-show="recentChatSessionsExpanded">
                <div v-if="chatStore.sessionsLoading && sidebarVisibleChatSessions.length === 0" class="flex justify-center py-6">
                  <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
                </div>

                <div v-else-if="sidebarVisibleChatSessions.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
                  暂无对话记录
                </div>

                <template v-else>
                  <button
                    v-for="session in limitedRecentChatSessions"
                    :key="session.sessionId"
                    class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                    :class="isSessionActive(session.sessionId)
                      ? 'bg-[#f3f3f3]'
                      : 'hover:bg-[#f9f9f9]'"
                    @click="handleSelectSession(session.sessionId)"
                  >
                    <ChatSessionActionsPopover
                      :session="session"
                      :active="isSessionActive(session.sessionId)"
                      @pin="handlePinChatSession"
                      @share="handleShareChatSession"
                      @delete="handleDeleteSession"
                    />
                  </button>
                  <button
                    v-if="sidebarVisibleChatSessions.length > RECENT_CHAT_SESSION_LIMIT"
                    type="button"
                    class="group flex w-full min-w-0 items-center gap-2 rounded-[8px] pl-[10px] pr-[10px] py-[7px] mt-[1px] ml-[2px] mr-[6px] text-left text-[14px] text-[#0d0d0d] transition-all hover:bg-[#f9f9f9]"
                    @click="recentChatSessionsMoreVisible = true"
                  >
                    <span class="material-symbols-outlined text-[20px] leading-none text-[#0d0d0d]">more_horiz</span>
                    <span>更多</span>
                  </button>
                </template>
              </div>
          </div>

          <div
            v-if="!sidebarSortMode && showSidebarCustomers"
            class="wk-sidebar-customer-section space-y-1 pt-1"
            :style="{ order: getSidebarModuleRenderOrder('customer') }"
          >
            <div
              class="wk-customer-header-row group/customer-header flex w-full items-center gap-2 rounded-lg pl-3 pr-1 py-[6px] mt-[12px] mb-[0px] hover:!bg-[#f9f9f9]"
              :style="{ backgroundColor: customerHeaderHovered ? '#f9f9f9' : 'transparent' }"
              :title="sidebarCustomersExpanded ? '收起客户列表' : '展开客户列表'"
              @mouseenter="customerHeaderHovered = true"
              @mouseleave="customerHeaderHovered = false"
            >
              <button
                type="button"
                class="wk-customer-header-toggle group flex min-w-0 flex-1 items-center gap-1 text-left"
                :aria-expanded="sidebarCustomersExpanded"
                aria-controls="sidebar-customers-panel"
                @click="sidebarCustomersExpanded = !sidebarCustomersExpanded"
              >
                <span class="min-w-0 truncate text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">客户列表</span>
                <span
                  class="flex size-6 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150"
                  :class="sidebarCustomersExpanded ? 'opacity-100' : 'opacity-100'"
                  aria-hidden="true"
                >
                  <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                    {{ sidebarCustomersExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                  </span>
                </span>
              </button>
              <div class="ml-auto flex shrink-0 items-center justify-end gap-1">
                <button
                  type="button"
                  class="wk-customer-header-action group/customer-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="新建客户"
                  @click.stop="showCreateCustomer = true"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">person_add</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/customer-action:opacity-100"
                    role="tooltip"
                  >
                    新建客户
                  </span>
                </button>
                <button
                  type="button"
                  class="wk-customer-header-action group/customer-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="查看客户列表"
                  @click.stop="navigateTo('/customer')"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">format_list_bulleted</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/customer-action:opacity-100"
                    role="tooltip"
                  >
                    查看客户列表
                  </span>
                </button>
              </div>
            </div>
            <div id="sidebar-customers-panel" v-show="sidebarCustomersExpanded">
              <div v-if="sidebarCustomersLoading && sidebarCustomers.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarCustomers.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
                {{ sidebarCustomerKeyword.trim() ? '暂无匹配客户' : '暂无客户数据' }}
              </div>
              <template v-else>
                <button
                  v-for="customer in sidebarCustomers"
                  :key="customer.customerId"
                  type="button"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isCustomerActive(customer.customerId) ? 'bg-[#f3f3f3]' : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectCustomerChat(customer)"
                >
                  <div class="flex min-w-0 w-full items-center gap-2" style="height: 24px !important">
                    <div class="flex size-[20px] shrink-0 items-center justify-center overflow-hidden rounded border border-slate-200 bg-white">
                      <img
                        v-if="customer.logoUrl"
                        :src="customer.logoUrl"
                        :alt="customer.companyName || 'company logo'"
                        class="size-full object-contain"
                      />
                      <span v-else class="flex size-full items-center justify-center bg-primary/10 text-xs font-bold text-primary">
                        {{ customer.companyName?.charAt(0) || '?' }}
                      </span>
                    </div>
                    <span class="block min-w-0 flex-1 truncate text-sm leading-5 text-[#0d0d0d]" :title="customer.companyName">
                      {{ customer.companyName }}
                    </span>
                  </div>
                </button>
                <div v-if="sidebarCustomersLoading" class="flex justify-center py-3">
                  <span class="material-symbols-outlined animate-spin text-[18px] leading-none text-slate-300">progress_activity</span>
                </div>
                <button
                  v-else-if="sidebarCustomersHasMore"
                  type="button"
                  class="group flex w-full min-w-0 items-center justify-center rounded-[8px] py-[7px] text-left text-[13px] text-[#8f8f8f] transition-all hover:bg-[#f9f9f9] hover:text-[#5f5f5f]"
                  @click="loadMoreSidebarCustomers"
                >
                  加载更多
                </button>
              </template>
            </div>
          </div>

          <div
            v-if="!sidebarSortMode && showSidebarProducts"
            class="wk-sidebar-product-section space-y-1 pt-1"
            :style="{ order: getSidebarModuleRenderOrder('product') }"
          >
            <div
              class="wk-customer-header-row group/product-header flex w-full items-center gap-2 rounded-lg pl-3 pr-1 py-[6px] mt-[12px] mb-[0px] hover:!bg-[#f9f9f9]"
              :style="{ backgroundColor: productHeaderHovered ? '#f9f9f9' : 'transparent' }"
              :title="sidebarProductsExpanded ? '收起产品列表' : '展开产品列表'"
              @mouseenter="productHeaderHovered = true"
              @mouseleave="productHeaderHovered = false"
            >
              <button
                type="button"
                class="wk-customer-header-toggle group flex min-w-0 flex-1 items-center gap-1 text-left"
                :aria-expanded="sidebarProductsExpanded"
                aria-controls="sidebar-products-panel"
                @click="sidebarProductsExpanded = !sidebarProductsExpanded"
              >
                <span class="min-w-0 truncate text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">产品列表</span>
                <span class="flex size-6 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150" aria-hidden="true">
                  <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                    {{ sidebarProductsExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                  </span>
                </span>
              </button>
              <div class="ml-auto flex shrink-0 items-center justify-end gap-1">
                <button
                  type="button"
                  class="wk-customer-header-action group/product-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="查看产品列表"
                  @click.stop="navigateTo('/product')"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">format_list_bulleted</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/product-action:opacity-100"
                    role="tooltip"
                  >
                    查看产品列表
                  </span>
                </button>
              </div>
            </div>
            <div id="sidebar-products-panel" v-show="sidebarProductsExpanded">
              <div v-if="sidebarProductsLoading && sidebarProducts.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarProducts.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
                暂无产品数据
              </div>
              <template v-else>
                <button
                  v-for="product in sidebarProducts"
                  :key="product.productId"
                  type="button"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isProductActive(product.productId) ? 'bg-[#f3f3f3]' : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectProductChat(product)"
                >
                  <div class="flex min-w-0 w-full items-center gap-2" style="height: 24px !important">
                    <div class="flex size-[20px] shrink-0 items-center justify-center overflow-hidden rounded border border-slate-200 bg-white">
                      <img
                        v-if="product.mainImageUrl"
                        :src="product.mainImageUrl"
                        :alt="productName(product)"
                        class="size-full object-cover"
                      />
                      <span v-else class="material-symbols-outlined flex size-full items-center justify-center bg-amber-50 text-[15px] leading-none text-amber-600">
                        inventory_2
                      </span>
                    </div>
                    <span class="block min-w-0 flex-1 truncate text-sm leading-5 text-[#0d0d0d]" :title="productName(product)">
                      {{ productName(product) }}
                    </span>
                  </div>
                </button>
                <div v-if="sidebarProductsLoading" class="flex justify-center py-3">
                  <span class="material-symbols-outlined animate-spin text-[18px] leading-none text-slate-300">progress_activity</span>
                </div>
                <button
                  v-else-if="sidebarProductsHasMore"
                  type="button"
                  class="group flex w-full min-w-0 items-center justify-center rounded-[8px] py-[7px] text-left text-[13px] text-[#8f8f8f] transition-all hover:bg-[#f9f9f9] hover:text-[#5f5f5f]"
                  @click="loadMoreSidebarProducts"
                >
                  加载更多
                </button>
              </template>
            </div>
          </div>

          <div
            v-if="!sidebarSortMode && showSidebarAddressBook"
            class="wk-sidebar-address-section space-y-1 pt-1"
            :style="{ order: getSidebarModuleRenderOrder('addressBook') }"
          >
            <div
              class="wk-customer-header-row group/address-book-header flex w-full items-center gap-2 rounded-lg pl-3 pr-1 py-[6px] mt-[12px] mb-[0px] hover:!bg-[#f9f9f9]"
              :style="{ backgroundColor: addressBookHeaderHovered ? '#f9f9f9' : 'transparent' }"
              :title="sidebarAddressBookExpanded ? '收起通讯录' : '展开通讯录'"
              @mouseenter="addressBookHeaderHovered = true"
              @mouseleave="addressBookHeaderHovered = false"
            >
              <button
                type="button"
                class="wk-customer-header-toggle group flex min-w-0 flex-1 items-center gap-1 text-left"
                :aria-expanded="sidebarAddressBookExpanded"
                aria-controls="sidebar-address-book-panel"
                @click="sidebarAddressBookExpanded = !sidebarAddressBookExpanded"
              >
                <span class="min-w-0 truncate text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">通讯录</span>
                <span class="flex size-6 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150" aria-hidden="true">
                  <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                    {{ sidebarAddressBookExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                  </span>
                </span>
              </button>
              <div class="ml-auto flex shrink-0 items-center justify-end gap-1">
                <button
                  type="button"
                  class="wk-customer-header-action group/address-book-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="查看通讯录"
                  @click.stop="navigateTo('/address-book')"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">format_list_bulleted</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/address-book-action:opacity-100"
                    role="tooltip"
                  >
                    查看通讯录
                  </span>
                </button>
              </div>
            </div>
            <div id="sidebar-address-book-panel" v-show="sidebarAddressBookExpanded">
              <div v-if="sidebarEmployeesLoading && sidebarEmployees.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarEmployees.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
                暂无员工数据
              </div>
              <template v-else>
                <button
                  v-for="employee in sidebarEmployees"
                  :key="employee.userId"
                  type="button"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isEmployeeActive(employee.userId) ? 'bg-[#f3f3f3]' : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectEmployeeChat(employee)"
                >
                  <div class="flex min-w-0 w-full items-center gap-2" style="height: 24px !important">
                    <div class="flex size-[20px] shrink-0 items-center justify-center overflow-hidden rounded border border-slate-200 bg-white">
                      <img
                        v-if="employeeAvatarUrl(employee)"
                        :src="employeeAvatarUrl(employee)"
                        :alt="employeeName(employee)"
                        class="size-full object-cover"
                      />
                      <span v-else class="flex size-full items-center justify-center bg-emerald-50 text-xs font-bold text-emerald-600">
                        {{ employeeInitial(employee) }}
                      </span>
                    </div>
                    <span class="block min-w-0 flex-1 truncate text-sm leading-5 text-[#0d0d0d]" :title="employeeName(employee)">
                      {{ employeeName(employee) }}
                    </span>
                  </div>
                </button>
                <div v-if="sidebarEmployeesLoading" class="flex justify-center py-3">
                  <span class="material-symbols-outlined animate-spin text-[18px] leading-none text-slate-300">progress_activity</span>
                </div>
                <button
                  v-else-if="sidebarEmployeesHasMore"
                  type="button"
                  class="group flex w-full min-w-0 items-center justify-center rounded-[8px] py-[7px] text-left text-[13px] text-[#8f8f8f] transition-all hover:bg-[#f9f9f9] hover:text-[#5f5f5f]"
                  @click="loadMoreSidebarEmployees"
                >
                  加载更多
                </button>
              </template>
            </div>
          </div>

          <div
            v-if="!sidebarSortMode && showSidebarRelations"
            class="wk-sidebar-relation-section space-y-1 pt-1"
            :style="{ order: getSidebarModuleRenderOrder('relation') }"
          >
            <div
              class="wk-customer-header-row group/relation-header flex w-full items-center gap-2 rounded-lg pl-3 pr-1 py-[6px] mt-[12px] mb-[0px] hover:!bg-[#f9f9f9]"
              :style="{ backgroundColor: relationHeaderHovered ? '#f9f9f9' : 'transparent' }"
              :title="sidebarRelationsExpanded ? '收起关系' : '展开关系'"
              @mouseenter="relationHeaderHovered = true"
              @mouseleave="relationHeaderHovered = false"
            >
              <button
                type="button"
                class="wk-customer-header-toggle group flex min-w-0 flex-1 items-center gap-1 text-left"
                :aria-expanded="sidebarRelationsExpanded"
                aria-controls="sidebar-relations-panel"
                @click="sidebarRelationsExpanded = !sidebarRelationsExpanded"
              >
                <span class="min-w-0 truncate text-[14px] font-semibold uppercase tracking-tight text-[#0d0d0d]">关系</span>
                <span class="flex size-6 shrink-0 items-center justify-center rounded-md text-slate-400 transition-all duration-150" aria-hidden="true">
                  <span class="material-symbols-outlined inline-flex h-5 shrink-0 items-center justify-center self-center text-[18px] leading-none text-[#c9c9c9]">
                    {{ sidebarRelationsExpanded ? 'keyboard_arrow_down' : 'chevron_right' }}
                  </span>
                </span>
              </button>
              <div class="ml-auto flex shrink-0 items-center justify-end gap-1">
                <button
                  type="button"
                  class="wk-customer-header-action group/relation-create-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="新建关系"
                  @click.stop="openCreateRelationDialog"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">person_add</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/relation-create-action:opacity-100"
                    role="tooltip"
                  >
                    新建关系
                  </span>
                </button>
                <button
                  type="button"
                  class="wk-customer-header-action group/relation-action relative flex size-6 items-center justify-center rounded-md text-[#8f8f8f] transition-colors hover:text-[#0d0d0d]"
                  aria-label="查看关系列表"
                  @click.stop="navigateTo('/relation')"
                >
                  <span class="material-symbols-outlined text-[18px] leading-none">format_list_bulleted</span>
                  <span
                    class="pointer-events-none absolute right-0 top-full z-[200] mt-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/relation-action:opacity-100"
                    role="tooltip"
                  >
                    查看关系列表
                  </span>
                </button>
              </div>
            </div>
            <div id="sidebar-relations-panel" v-show="sidebarRelationsExpanded">
              <div v-if="sidebarRelationsLoading && sidebarRelations.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarRelations.length === 0" class="px-3 py-6 text-center text-xs text-slate-400">
                暂无关系数据
              </div>
              <template v-else>
                <button
                  v-for="relation in sidebarRelations"
                  :key="relation.relationId"
                  type="button"
                  class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                  :class="isRelationActive(relation.relationId) ? 'bg-[#f3f3f3]' : 'hover:bg-[#f9f9f9]'"
                  @click="handleSelectRelationChat(relation)"
                >
                  <div class="flex min-w-0 w-full items-center gap-2" style="height: 24px !important">
                    <div class="flex size-[20px] shrink-0 items-center justify-center overflow-hidden rounded border border-slate-200 bg-white">
                      <img
                        v-if="relationAvatarUrl(relation)"
                        :src="relationAvatarUrl(relation)"
                        :alt="relationName(relation)"
                        class="size-full object-contain"
                      />
                      <span v-else class="flex size-full items-center justify-center bg-indigo-50 text-xs font-bold text-indigo-600">
                        {{ relationInitial(relation) }}
                      </span>
                    </div>
                    <span class="block min-w-0 flex-1 truncate text-sm leading-5 text-[#0d0d0d]" :title="relationName(relation)">
                      {{ relationName(relation) }}
                    </span>
                  </div>
                </button>
                <div v-if="sidebarRelationsLoading" class="flex justify-center py-3">
                  <span class="material-symbols-outlined animate-spin text-[18px] leading-none text-slate-300">progress_activity</span>
                </div>
                <button
                  v-else-if="sidebarRelationsHasMore"
                  type="button"
                  class="group flex w-full min-w-0 items-center justify-center rounded-[8px] py-[7px] text-left text-[13px] text-[#8f8f8f] transition-all hover:bg-[#f9f9f9] hover:text-[#5f5f5f]"
                  @click="loadMoreSidebarRelations"
                >
                  加载更多
                </button>
              </template>
            </div>
          </div>
          </template>

          </div>
        </nav>

        <div
          v-if="!primarySidebarContentCollapsed && !sidebarSortMode"
          class="pointer-events-none absolute bottom-2 right-3 z-30"
        >
          <button
            type="button"
            class="group/sidebar-sort-action pointer-events-auto relative flex size-7 items-center justify-center rounded-lg border border-[#ececec] bg-white/95 text-[#8f8f8f] shadow-[0_4px_12px_rgb(0,0,0,0.08)] backdrop-blur transition-colors hover:bg-[#f3f3f3] hover:text-[#0d0d0d]"
            aria-label="设置排序"
            @click.stop="openSidebarSortMode()"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">swap_vert</span>
            <span
              class="pointer-events-none absolute bottom-full right-0 z-[200] mb-2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/sidebar-sort-action:opacity-100"
              role="tooltip"
            >
              设置排序
            </span>
          </button>
        </div>
      </div>

      <Transition name="drawer-panel">
        <div
          v-if="recentChatSessionsMoreVisible && !primarySidebarCollapsed"
          class="absolute inset-0 z-50 flex flex-col bg-white"
        >
          <div class="flex h-12 shrink-0 items-center justify-between border-b border-[#ececec] px-3">
            <button
              type="button"
              class="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-medium text-[#0d0d0d] transition-colors hover:bg-[#f5f5f5]"
              @click="recentChatSessionsMoreVisible = false"
            >
              <span class="material-symbols-outlined text-[18px] leading-none text-[#8f8f8f]">arrow_back</span>
              最近
            </button>
            <button
              type="button"
              class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#f5f5f5] hover:text-[#0d0d0d]"
              @click="recentChatSessionsMoreVisible = false"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">close</span>
            </button>
          </div>
          <div class="shrink-0 px-3 py-2">
            <el-input
              v-model="recentHistoryKeyword"
              clearable
              placeholder="搜索对话"
            />
          </div>
          <div class="wk-primary-nav-scroll min-h-0 flex-1 overflow-y-auto px-3 pb-2">
            <div v-if="filteredHistorySessions.length === 0" class="px-3 py-8 text-center text-sm text-slate-400">
              暂无匹配对话
            </div>
            <template v-else>
              <template v-for="group in historySessionGroups" :key="group.key">
                <div v-if="group.sessions.length > 0" class="mb-3">
                  <p class="px-1 pb-1 text-xs font-semibold text-slate-400">{{ group.label }}</p>
                  <button
                    v-for="session in group.sessions"
                    :key="session.sessionId"
                    type="button"
                    class="group w-full min-w-0 overflow-hidden rounded-[8px] pl-[10px] pr-[10px] py-[6px] mt-[1px] ml-[2px] mr-[6px] text-left transition-all"
                    :class="isSessionActive(session.sessionId)
                      ? 'bg-[#f3f3f3]'
                      : 'hover:bg-[#f9f9f9]'"
                    @click="handleSelectSessionFromMore(session.sessionId)"
                  >
                    <ChatSessionActionsPopover
                      :session="session"
                      :active="isSessionActive(session.sessionId)"
                      @pin="handlePinChatSession"
                      @share="handleShareChatSession"
                      @delete="handleDeleteSession"
                    />
                  </button>
                </div>
              </template>
            </template>
          </div>
        </div>
      </Transition>

      <div v-if="false && showConfigSection" class="shrink-0 border-t border-slate-200 px-4 py-3">
        <div class="pb-2 pt-1">
          <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">配置与服务</p>
        </div>
        <button
          v-for="item in configNavItems"
          :key="item.route"
          @click="navigateTo(item.route, item.query)"
          class="flex w-full items-center gap-2 rounded-lg px-3 py-2 transition-colors"
          :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-[#0d0d0d] hover:bg-slate-100'"
        >
          <WkIcon :name="item.icon" :size="22" class="shrink-0" />
          <span class="truncate text-sm font-medium">{{ item.label }}</span>
        </button>
      </div>

      <div class="border-t border-[#ececec] px-2 py-1.5" :class="primarySidebarContentCollapsed ? '!px-2 !py-1.5' : ''">
        <div class="space-y-0.5">
          <button
            v-if="false"
            type="button"
            class="flex w-full items-center rounded-xl text-slate-600 transition-colors hover:bg-slate-50 hover:text-primary dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-primary"
            :class="primarySidebarContentCollapsed ? 'justify-center p-2' : 'gap-3 p-2'"
            :title="themeButtonLabel"
            :aria-label="themeButtonLabel"
            @click="toggleTheme"
          >
            <span class="material-symbols-outlined text-[20px] leading-none">{{ themeIcon }}</span>
            <span v-if="!primarySidebarContentCollapsed" class="text-xs font-semibold">{{ themeButtonLabel }}</span>
          </button>
          <div
            class="flex cursor-pointer items-center rounded-xl transition-colors hover:bg-[#f9f9f9] dark:bg-slate-900 dark:hover:bg-slate-800"
            :class="primarySidebarContentCollapsed ? 'justify-center' : 'gap-2 px-1.5 py-2.5'"
            :title="primarySidebarContentCollapsed ? (userStore.realname || userStore.username || '用户') : undefined"
            @click="showUserMenu = !showUserMenu"
          >
            <div v-if="userStore.avatar" class="size-7 overflow-hidden rounded-full">
              <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
            </div>
            <div v-else class="flex size-7 items-center justify-center rounded-full bg-primary text-[11px] font-bold text-white">
              {{ userStore.realname?.charAt(0) || 'U' }}
            </div>
            <template v-if="!primarySidebarContentCollapsed">
              <div class="min-w-0 flex-1">
                <p class="truncate text-[13px] font-semibold leading-5 text-slate-900">{{ userStore.realname || userStore.username }}</p>
              </div>
            </template>
          </div>
        </div>
      </div>

      <Transition name="profile-drawer">
        <div
          v-if="showUserMenu"
          class="absolute bottom-0 left-0 right-0 z-40 overflow-hidden rounded-t-2xl border-t border-slate-200 bg-white shadow-[0_-8px_30px_rgb(0,0,0,0.12)]"
        >
          <div class="p-4">
            <div class="space-y-1">
              <div class="flex items-center justify-between px-3 py-2">
                <p class="text-[12px] font-bold uppercase tracking-wider text-slate-400">个人账户</p>
                <button class="text-slate-300 transition-colors hover:text-slate-500" @click="showUserMenu = false">
                  <span class="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              <div class="mb-4 flex items-center gap-3 px-3 py-2">
                <div v-if="userStore.avatar" class="size-10 overflow-hidden rounded-full bg-slate-300">
                  <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                </div>
                <div v-else class="flex size-10 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
                  {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ userStore.realname || userStore.username }}</p>
                  <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
                </div>
              </div>

              <div class="mx-3 my-1 h-px bg-slate-100" />

              <button
                class="group flex h-9 w-full items-center gap-3 rounded-[8px] px-3 text-left text-slate-700 transition-colors hover:bg-slate-50"
                @click="handleOpenAccountSettings"
              >
                <WkIcon name="set" :size="24" class="text-slate-400 transition-colors group-hover:text-primary" />
                <span class="text-sm font-medium">账号设置</span>
              </button>

              <button
                v-for="item in configNavItems"
                :key="item.route"
                class="group flex h-9 w-full items-center gap-3 rounded-[8px] px-3 text-left transition-colors"
                :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                @click="navigateTo(item.route, item.query)"
              >
                <WkIcon
                  :name="item.icon"
                  :size="24"
                  class="transition-colors"
                  :class="isActive(item.route, item.query) ? 'text-primary' : 'text-slate-400 group-hover:text-primary'"
                />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>

              <button
                class="group flex h-9 w-full items-center gap-3 rounded-[8px] px-3 text-left text-rose-600 transition-colors hover:bg-slate-50"
                @click="handleLogout"
              >
                <span class="material-symbols-outlined inline-flex size-6 shrink-0 items-center justify-center text-[24px] leading-none text-rose-400 transition-colors group-hover:text-rose-600">logout</span>
                <span class="text-sm font-medium">退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </aside>

    <Transition name="secondary-panel">
      <aside
        v-if="!isMobile && showSecondaryPanel"
        class="relative z-10 flex wk-screen w-64 flex-shrink-0 flex-col border-r border-slate-200 bg-white"
      >
        <div v-if="secondaryTitle" class="flex items-center justify-between px-4 py-5">
          <p class="truncate text-xs font-bold tracking-wider text-slate-400">{{ secondaryTitle }}</p>
          <!-- <button class="text-slate-300 transition-colors hover:text-slate-500" @click="selectedPrimaryKey = ''">
            <span class="material-symbols-outlined text-sm">close</span>
          </button> -->
        </div>

        <div class="flex-1 overflow-y-auto px-4 pb-4" :class="secondaryTitle ? '' : 'pt-3'">
          <div class="space-y-1">
            <button
              v-for="child in activeSecondaryItems"
              :key="child.key"
              class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
              :class="isActive(child.route, child.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
              @click="navigateTo(child.route, child.query)"
            >
              <WkIcon
                v-if="child.icon"
                :name="child.icon"
                :size="22"
                class="shrink-0"
                :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-400'"
              />
              <span v-else class="material-symbols-outlined shrink-0 text-[20px]" :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-300'">
                subdirectory_arrow_right
              </span>
              <span class="truncate text-sm font-medium">{{ child.label }}</span>
            </button>
          </div>
        </div>
      </aside>
    </Transition>

    <div v-if="showMobileTopBar" class="wk-mobile-top-bar fixed left-0 right-0 top-0 z-50 flex items-center gap-2 border-b border-slate-200 bg-white px-4">
      <button
        @click="openMobileDrawer"
        class="flex size-10 shrink-0 items-center justify-center rounded-lg text-[#0d0d0d] hover:bg-slate-100"
      >
        <span class="material-symbols-outlined">menu</span>
      </button>

      <div class="flex min-w-0 flex-1 items-center gap-2">
        <div ref="searchPanelRef" class="relative w-full min-w-0">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-slate-400">search</span>
          <input
            v-model="globalSearchKeyword"
            type="text"
            class="w-full rounded-lg border-none bg-slate-100 py-2 pl-10 pr-4 text-sm outline-none transition-all focus:ring-2 focus:ring-primary/50"
            placeholder="搜索客户1、关系、任务、日程、知识库..."
            @focus="handleGlobalSearchFocus"
            @keydown.enter.prevent="handleGlobalSearchEnter"
            @keydown.down.prevent="handleGlobalSearchArrow(1)"
            @keydown.up.prevent="handleGlobalSearchArrow(-1)"
            @keydown.esc.prevent="closeGlobalSearchDropdown"
            @input="debouncedGlobalSearch"
          />

          <Transition name="dropdown">
            <div
              v-if="showGlobalSearchDropdown"
              class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-[210] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-900/10"
              @mousedown.prevent
            >
              <div v-if="globalSearchLoading" class="flex items-center gap-3 px-4 py-4 text-sm text-slate-500">
                <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
                正在搜索...
              </div>

              <template v-else-if="globalSearchResults.length > 0">
                <button
                  v-for="(result, index) in globalSearchResults"
                  :key="`${result.entityType}-${result.entityId}`"
                  type="button"
                  class="flex w-full items-start gap-3 px-4 py-3 text-left transition-colors"
                  :class="index === activeSearchResultIndex ? 'bg-primary/5' : 'hover:bg-slate-50'"
                  @mouseenter="activeSearchResultIndex = index"
                  @click="navigateToSearchResult(result)"
                >
                  <div class="mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                    <WkIcon :name="getSearchResultIcon(result.entityType)" :size="18" />
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-semibold text-slate-900">{{ result.title }}</p>
                    <p v-if="result.subtitle" class="mt-0.5 truncate text-xs text-slate-500">{{ result.subtitle }}</p>
                    <p v-if="result.summary" class="line-clamp-2 mt-1 text-xs leading-5 text-slate-400">{{ result.summary }}</p>
                  </div>
                  <span class="material-symbols-outlined mt-1 shrink-0 text-base text-slate-300">chevron_right</span>
                </button>
                <div class="border-t border-slate-100 px-4 py-2 text-center text-xs text-slate-400">
                  显示前 {{ globalSearchResults.length }} 条，共 {{ globalSearchTotal }} 条结果
                </div>
              </template>

              <div v-else class="px-4 py-5 text-center text-sm text-slate-400">
                没有找到匹配结果
              </div>
            </div>
          </Transition>
        </div>


      </div>

      <div class="flex shrink-0 items-center gap-2">
        <!-- <button class="flex size-10 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100">
          <span class="material-symbols-outlined">notifications</span>
        </button> -->
        <!-- <button
          type="button"
          class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-100"
          @click="showUserMenu = !showUserMenu"
        >
          <img v-if="userStore.avatar" :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
          <span v-else class="text-xs font-bold text-primary">{{ userStore.realname?.charAt(0) || 'U' }}</span>
        </button> -->
      </div>
    </div>

    <Teleport to="body">
      <input
        v-if="isMobile"
        ref="mobileCustomerSearchKeyboardProxyRef"
        type="search"
        class="wk-mobile-customer-search-keyboard-proxy"
        tabindex="-1"
        autocomplete="off"
        autocapitalize="off"
        enterkeyhint="search"
        aria-hidden="true"
      />
      <Transition name="drawer-overlay">
        <div
          v-if="mobileDrawerRendered"
          class="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm"
          :style="mobileDrawerOverlayStyle"
          @click="closeMobileDrawer"
        ></div>
      </Transition>
      <Transition name="drawer-panel">
        <aside
          v-if="mobileDrawerRendered"
          class="wk-mobile-drawer-panel fixed inset-y-0 left-0 right-0 z-[101] flex w-screen max-w-none touch-pan-y flex-col bg-white shadow-2xl"
          :style="mobileDrawerPanelStyle"
          @touchstart.passive="handleMobileDrawerTouchStart"
          @touchmove="handleMobileDrawerTouchMove"
          @touchend.passive="handleMobileDrawerTouchEnd"
          @touchcancel.passive="handleMobileDrawerTouchCancel"
        >
          <div class="wk-mobile-drawer-header flex items-center justify-between gap-4 px-4 pb-4">
            <div class="flex min-w-0 flex-1 items-center gap-3">
              <div v-if="enterpriseStore.hasLogo" class="size-9 flex-shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-transparent">
                <img :src="enterpriseStore.logoUrl!" class="h-full w-full object-cover" alt="logo" />
              </div>
              <div v-else class="size-9 flex-shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-transparent">
                <img :src="defaultLogoImg" class="h-full w-full object-cover" alt="logo" />
              </div>
              <h1 class="min-w-0 truncate text-[22px] font-bold leading-tight text-[#0d0d0d]">{{ enterpriseStore.displayName }}</h1>
            </div>
            <div class="flex h-10 shrink-0 items-center gap-6 rounded-full bg-white px-1.5 shadow-[0_12px_34px_rgba(15,23,42,0.12)]">
              <button
                type="button"
                class="flex size-8 items-center justify-center rounded-full text-[#0d0d0d] transition-colors active:bg-slate-50"
                aria-label="搜索客户"
                @click="openMobileCustomerSearchDialog"
              >
                <span class="material-symbols-outlined text-[24px] leading-none">search</span>
              </button>
              <button
                type="button"
                class="flex size-8 items-center justify-center overflow-hidden rounded-full bg-slate-100 text-[#0d0d0d] transition-colors active:bg-slate-200"
                aria-label="个人账户"
                @click="showUserMenu = true"
              >
                <img v-if="userStore.avatar" :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                <span v-else class="text-xs font-bold text-primary">
                  {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                </span>
              </button>
            </div>
          </div>
          <nav class="wk-mobile-drawer-nav wk-scrollbar-gutter-stable flex-1 space-y-2 overflow-y-auto px-3 pt-2">
            <template v-for="group in mobileMainNavGroups" :key="group.title || 'default'">
              <div v-if="group.title" class="pb-2.5 pt-5">
                <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">{{ group.title }}</p>
              </div>
              <div v-for="item in group.items" :key="item.key" class="space-y-1.5">
                <button
                  class="flex w-full items-center gap-4 rounded-[8px] px-3 py-[9px] text-left transition-colors"
                  :class="isPrimaryActive(item) ? 'bg-primary/10 text-primary' : 'text-[#0d0d0d] hover:bg-slate-100'"
                  @click="handleMobilePrimaryNavClick(item)"
                >
                  <span
                    v-if="item.materialIcon"
                    class="material-symbols-outlined inline-flex size-6 shrink-0 items-center justify-center text-[20px] leading-none"
                  >
                    {{ item.materialIcon }}
                  </span>
                  <WkIcon v-else :name="item.icon" :box-size="20" class="shrink-0" />
                  <span class="inline-flex min-w-0 flex-1 items-center truncate text-[1rem] font-semibold leading-6">{{ item.label }}</span>
                  <span
                    v-if="item.children?.length"
                    class="material-symbols-outlined ml-auto shrink-0 text-base transition-transform"
                    :class="isMobilePrimaryExpanded(item.key) ? 'rotate-90 text-primary' : 'text-slate-300'"
                  >
                    chevron_right
                  </span>
                </button>

                <div v-if="item.children?.length && isMobilePrimaryExpanded(item.key)" class="ml-3 space-y-1 border-l border-slate-100 pl-3">
                  <button
                    v-for="child in item.children"
                    :key="child.key"
                    class="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition-colors"
                    :class="isActive(child.route, child.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                    @click="mobileNavigate(child.route, child.query)"
                  >
                    <WkIcon
                      v-if="child.icon"
                      :name="child.icon"
                      :size="20"
                      class="shrink-0"
                      :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-400'"
                    />
                    <span v-else class="material-symbols-outlined shrink-0 text-[18px]" :class="isActive(child.route, child.query) ? 'text-primary' : 'text-slate-300'">
                      subdirectory_arrow_right
                    </span>
                    <span class="truncate text-sm font-medium">{{ child.label }}</span>
                  </button>
                </div>
              </div>
            </template>

            <div v-if="showSidebarProjects" class="pt-3">
              <div class="flex items-center justify-between px-3 pb-2.5">
                <p class="text-[1rem] font-bold leading-7 text-[#0d0d0d]">项目</p>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-slate-100"
                  aria-label="新增项目"
                  @click="openMobileCreateProjectDialog"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">add</span>
                </button>
              </div>
              <div v-if="projectStore.loading && sidebarProjects.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarProjects.length === 0" class="px-3 py-[6px] text-sm text-slate-400">
                暂无项目数据
              </div>
              <template v-else>
                <div
                  v-for="project in sidebarProjects"
                  :key="project.projectId"
                  role="button"
                  tabindex="0"
                  class="group flex w-full min-w-0 cursor-pointer items-center gap-3 rounded-[8px] px-3 py-[9px] text-left transition-colors"
                  :class="isProjectActive(project.projectId) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] active:bg-slate-100'"
                  @click="handleMobileStartProjectConversation(project.projectId)"
                  @keydown.enter.self.prevent="handleMobileStartProjectConversation(project.projectId)"
                  @keydown.space.self.prevent="handleMobileStartProjectConversation(project.projectId)"
                >
                  <span class="material-symbols-outlined flex size-7 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white text-[18px] leading-none text-[#8f8f8f]">
                    folder
                  </span>
                  <span class="block min-w-0 flex-1 truncate text-[1rem] leading-6">{{ project.name }}</span>
                </div>
              </template>
            </div>

            <div class="pt-3">
              <p class="px-3 pb-2.5 text-[1rem] font-bold leading-7 text-[#0d0d0d]">最近</p>
              <div v-if="chatStore.sessionsLoading && mobileRecentChatSessions.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="mobileRecentChatSessions.length === 0" class="px-3 py-[6px] text-sm text-slate-400">
                暂无对话记录
              </div>
              <template v-else>
                <button
                  v-for="session in mobileRecentChatSessions"
                  :key="session.sessionId"
                  type="button"
                  class="mobileRecentChatSessionRow group flex w-full min-w-0 select-none items-center rounded-xl px-3 py-[9px] text-left transition-colors"
                  :class="isSessionActive(session.sessionId) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] active:bg-slate-100'"
                  @click="handleMobileSelectSession(session.sessionId)"
                >
                  <ChatSessionActionsPopover
                    :session="session"
                    :active="isSessionActive(session.sessionId)"
                    always-visible
                    :menu-shift-x="0"
                    @pin="handlePinChatSession"
                    @share="handleShareChatSession"
                    @delete="handleDeleteSession"
                  />
                </button>
                <button
                  v-if="sidebarVisibleChatSessions.length > MOBILE_RECENT_CHAT_SESSION_LIMIT"
                  type="button"
                  class="mt-1 flex w-full min-w-0 items-center gap-2 rounded-[8px] px-3 py-[9px] text-left text-[1rem] text-[#0d0d0d] transition-colors active:bg-slate-100"
                  @click="recentChatSessionsMoreVisible = true"
                >
                  <span class="material-symbols-outlined text-[22px] leading-none text-[#0d0d0d]">more_horiz</span>
                  <span class="min-w-0 flex-1 truncate leading-6">更多</span>
                </button>
              </template>
            </div>

            <div v-if="showSidebarCustomers" class="pt-3">
              <div class="flex items-center gap-3 px-3 pb-2.5">
                <p class="min-w-0 flex-1 text-[1rem] font-bold leading-7 text-[#0d0d0d]">客户列表</p>
                <button
                  type="button"
                  class="flex size-8 shrink-0 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-slate-100 active:text-[#0d0d0d]"
                  aria-label="移动端新建客户"
                  title="新建客户"
                  @click.stop="handleMobileCreateCustomer"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">person_add</span>
                </button>
                <button
                  type="button"
                  class="flex size-8 shrink-0 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-slate-100 active:text-[#0d0d0d]"
                  aria-label="移动端查看客户列表"
                  title="查看客户列表"
                  @click.stop="mobileNavigate('/customer')"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">format_list_bulleted</span>
                </button>
              </div>
              <div v-if="sidebarCustomersLoading && sidebarCustomers.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarCustomers.length === 0" class="px-3 py-[6px] text-sm text-slate-400">
                暂无客户数据
              </div>
              <template v-else>
                <button
                  v-for="customer in sidebarCustomers"
                  :key="customer.customerId"
                  type="button"
                  class="group flex w-full min-w-0 items-center gap-3 rounded-[8px] px-3 py-[9px] text-left transition-colors"
                  :class="isCustomerActive(customer.customerId) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] active:bg-slate-100'"
                  @click="handleMobileSelectCustomerChat(customer)"
                >
                  <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                    <img
                      v-if="customer.logoUrl"
                      :src="customer.logoUrl"
                      :alt="customer.companyName || 'company logo'"
                      class="size-full object-contain"
                    />
                    <span v-else class="flex size-full items-center justify-center bg-primary/10 text-xs font-bold text-primary">
                      {{ customer.companyName?.charAt(0) || '?' }}
                    </span>
                  </div>
                  <span class="block min-w-0 flex-1 truncate text-[1rem] leading-6">{{ customer.companyName }}</span>
                </button>
                <button
                  v-if="sidebarCustomersHasMore"
                  type="button"
                  class="mt-1 flex w-full items-center justify-center rounded-xl px-3 py-2.5 text-sm font-medium text-[#8f8f8f] transition-colors active:bg-slate-100"
                  :disabled="sidebarCustomersLoading"
                  @click="loadMoreSidebarCustomers"
                >
                  {{ sidebarCustomersLoading ? '加载中...' : '加载更多客户' }}
                </button>
              </template>
            </div>

            <div v-if="showSidebarProducts" class="pt-3">
              <div class="flex items-center gap-3 px-3 pb-2.5">
                <p class="min-w-0 flex-1 text-[1rem] font-bold leading-7 text-[#0d0d0d]">产品列表</p>
                <button
                  type="button"
                  class="flex size-8 shrink-0 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-slate-100 active:text-[#0d0d0d]"
                  aria-label="移动端查看产品列表"
                  title="查看产品列表"
                  @click.stop="mobileNavigate('/product')"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">format_list_bulleted</span>
                </button>
              </div>
              <div v-if="sidebarProductsLoading && sidebarProducts.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarProducts.length === 0" class="px-3 py-[6px] text-sm text-slate-400">
                暂无产品数据
              </div>
              <template v-else>
                <button
                  v-for="product in sidebarProducts"
                  :key="product.productId"
                  type="button"
                  class="group flex w-full min-w-0 items-center gap-3 rounded-[8px] px-3 py-[9px] text-left transition-colors"
                  :class="isProductActive(product.productId) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] active:bg-slate-100'"
                  @click="handleMobileSelectProductChat(product)"
                >
                  <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                    <img
                      v-if="product.mainImageUrl"
                      :src="product.mainImageUrl"
                      :alt="productName(product)"
                      class="size-full object-cover"
                    />
                    <span v-else class="material-symbols-outlined flex size-full items-center justify-center bg-amber-50 text-[18px] leading-none text-amber-600">
                      inventory_2
                    </span>
                  </div>
                  <span class="block min-w-0 flex-1 truncate text-[1rem] leading-6">{{ productName(product) }}</span>
                </button>
                <button
                  v-if="sidebarProductsHasMore"
                  type="button"
                  class="mt-1 flex w-full items-center justify-center rounded-xl px-3 py-2.5 text-sm font-medium text-[#8f8f8f] transition-colors active:bg-slate-100"
                  :disabled="sidebarProductsLoading"
                  @click="loadMoreSidebarProducts"
                >
                  {{ sidebarProductsLoading ? '加载中...' : '加载更多产品' }}
                </button>
              </template>
            </div>

            <div v-if="showSidebarAddressBook" class="pt-3">
              <p class="px-3 pb-2.5 text-[1rem] font-bold leading-7 text-[#0d0d0d]">通讯录</p>
              <div v-if="sidebarEmployeesLoading && sidebarEmployees.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarEmployees.length === 0" class="px-3 py-[6px] text-sm text-slate-400">
                暂无员工数据
              </div>
              <template v-else>
                <button
                  v-for="employee in sidebarEmployees"
                  :key="employee.userId"
                  type="button"
                  class="group flex w-full min-w-0 items-center gap-3 rounded-[8px] px-3 py-[9px] text-left transition-colors"
                  :class="isEmployeeActive(employee.userId) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] active:bg-slate-100'"
                  @click="handleMobileSelectEmployeeChat(employee)"
                >
                  <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                    <img
                      v-if="employeeAvatarUrl(employee)"
                      :src="employeeAvatarUrl(employee)"
                      :alt="employeeName(employee)"
                      class="size-full object-cover"
                    />
                    <span v-else class="flex size-full items-center justify-center bg-emerald-50 text-xs font-bold text-emerald-600">
                      {{ employeeInitial(employee) }}
                    </span>
                  </div>
                  <span class="block min-w-0 flex-1 truncate text-[1rem] leading-6">{{ employeeName(employee) }}</span>
                </button>
                <button
                  v-if="sidebarEmployeesHasMore"
                  type="button"
                  class="mt-1 flex w-full items-center justify-center rounded-xl px-3 py-2.5 text-sm font-medium text-[#8f8f8f] transition-colors active:bg-slate-100"
                  :disabled="sidebarEmployeesLoading"
                  @click="loadMoreSidebarEmployees"
                >
                  {{ sidebarEmployeesLoading ? '加载中...' : '加载更多员工' }}
                </button>
              </template>
            </div>

            <div v-if="showSidebarRelations" class="pt-3">
              <div class="flex items-center gap-3 px-3 pb-2.5">
                <p class="min-w-0 flex-1 text-[1rem] font-bold leading-7 text-[#0d0d0d]">关系</p>
                <button
                  type="button"
                  class="flex size-8 shrink-0 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-slate-100 active:text-[#0d0d0d]"
                  aria-label="移动端新建关系"
                  title="新建关系"
                  @click.stop="handleMobileCreateRelation"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">person_add</span>
                </button>
                <button
                  type="button"
                  class="flex size-8 shrink-0 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-slate-100 active:text-[#0d0d0d]"
                  aria-label="移动端查看关系列表"
                  title="查看关系列表"
                  @click.stop="mobileNavigate('/relation')"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">format_list_bulleted</span>
                </button>
              </div>
              <div v-if="sidebarRelationsLoading && sidebarRelations.length === 0" class="flex justify-center py-6">
                <span class="material-symbols-outlined animate-spin text-slate-300">progress_activity</span>
              </div>
              <div v-else-if="sidebarRelations.length === 0" class="px-3 py-[6px] text-sm text-slate-400">
                暂无关系数据
              </div>
              <template v-else>
                <button
                  v-for="relation in sidebarRelations"
                  :key="relation.relationId"
                  type="button"
                  class="group flex w-full min-w-0 items-center gap-3 rounded-[8px] px-3 py-[9px] text-left transition-colors"
                  :class="isRelationActive(relation.relationId) ? 'bg-[#f3f3f3]' : 'text-[#0d0d0d] active:bg-slate-100'"
                  @click="handleMobileSelectRelationChat(relation)"
                >
                  <div class="flex size-7 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-white">
                    <img
                      v-if="relationAvatarUrl(relation)"
                      :src="relationAvatarUrl(relation)"
                      :alt="relationName(relation)"
                      class="size-full object-contain"
                    />
                    <span v-else class="flex size-full items-center justify-center bg-indigo-50 text-xs font-bold text-indigo-600">
                      {{ relationInitial(relation) }}
                    </span>
                  </div>
                  <span class="block min-w-0 flex-1 truncate text-[1rem] leading-6">{{ relationName(relation) }}</span>
                </button>
                <button
                  v-if="sidebarRelationsHasMore"
                  type="button"
                  class="mt-1 flex w-full items-center justify-center rounded-xl px-3 py-2.5 text-sm font-medium text-[#8f8f8f] transition-colors active:bg-slate-100"
                  :disabled="sidebarRelationsLoading"
                  @click="loadMoreSidebarRelations"
                >
                  {{ sidebarRelationsLoading ? '加载中...' : '加载更多关系' }}
                </button>
              </template>
            </div>

            <div v-if="showMobileConfigSection" class="pb-2 pt-2">
              <p class="px-3 text-xs font-bold uppercase tracking-wider text-slate-400">配置与服务</p>
            </div>

            <template v-if="showMobileConfigSection">
              <button
                v-for="item in configNavItems"
                :key="item.route"
                @click="mobileNavigate(item.route, item.query)"
                class="flex w-full items-center gap-3 rounded-lg px-3 py-[1px] transition-colors"
                :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-[#0d0d0d] hover:bg-slate-100'"
              >
                <WkIcon :name="item.icon" :size="20" class="shrink-0" />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>
            </template>
          </nav>

          <Transition name="drawer-panel">
            <div
              v-if="recentChatSessionsMoreVisible"
              class="absolute inset-0 z-50 flex flex-col bg-white"
            >
              <div class="flex min-h-[calc(56px_+_var(--safe-area-inset-top))] shrink-0 items-center justify-between border-b border-[#ececec] px-4 pt-[var(--safe-area-inset-top)]">
                <button
                  type="button"
                  class="flex items-center gap-2 rounded-lg px-2 py-2 text-[15px] font-semibold text-[#0d0d0d] transition-colors active:bg-[#f5f5f5]"
                  @click="recentChatSessionsMoreVisible = false"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none text-[#8f8f8f]">arrow_back</span>
                  最近
                </button>
                <button
                  type="button"
                  class="flex size-9 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors active:bg-[#f5f5f5] active:text-[#0d0d0d]"
                  aria-label="关闭对话历史"
                  @click="recentChatSessionsMoreVisible = false"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">close</span>
                </button>
              </div>
              <div class="shrink-0 px-4 py-3">
                <el-input
                  v-model="recentHistoryKeyword"
                  clearable
                  class="wk-mobile-recent-history-input"
                  placeholder="搜索对话"
                />
              </div>
              <div class="min-h-0 flex-1 overflow-y-auto px-4 pb-[calc(18px_+_var(--safe-area-inset-bottom))]">
                <div v-if="filteredHistorySessions.length === 0" class="px-3 py-8 text-center text-sm text-slate-400">
                  暂无匹配对话
                </div>
                <template v-else>
                  <template v-for="group in historySessionGroups" :key="group.key">
                    <div v-if="group.sessions.length > 0" class="mb-4">
                      <p class="px-1 pb-1.5 text-xs font-semibold text-slate-400">{{ group.label }}</p>
                      <button
                        v-for="session in group.sessions"
                        :key="session.sessionId"
                        type="button"
                        class="group w-full min-w-0 overflow-hidden rounded-[8px] px-3 py-[9px] text-left transition-colors"
                        :class="isSessionActive(session.sessionId)
                          ? 'bg-[#f3f3f3]'
                          : 'active:bg-[#f9f9f9]'"
                        @click="handleMobileSelectSessionFromMore(session.sessionId)"
                      >
                        <ChatSessionActionsPopover
                          :session="session"
                          :active="isSessionActive(session.sessionId)"
                          always-visible
                          :menu-shift-x="0"
                          @pin="handlePinChatSession"
                          @share="handleShareChatSession"
                          @delete="handleDeleteSession"
                        />
                      </button>
                    </div>
                  </template>
                </template>
              </div>
            </div>
          </Transition>

          <FloatingActionButton
            v-if="showMobileDrawerNewChatButton"
            placement="menu"
            @new-chat="handleFloatingNewChat"
          />

          <Transition name="profile-drawer">
            <div
              v-if="isMobile && drawerVisible && showUserMenu"
              class="absolute bottom-0 left-0 right-0 z-20 flex h-[80%] w-full flex-col overflow-hidden rounded-t-[34px] border-t border-white/70 bg-[#f4f4f7] text-[14px] shadow-[0_-18px_50px_rgba(15,23,42,0.18)]"
            >
              <div class="relative flex h-14 shrink-0 items-center justify-center px-4 pt-2">
                <h2 class="text-[16px] font-bold text-[#0d0d0d]">设置</h2>
                <button
                  type="button"
                  class="absolute right-4 top-3.5 flex size-9 items-center justify-center rounded-full bg-white text-[#0d0d0d] shadow-[0_8px_22px_rgba(15,23,42,0.08)] transition-colors active:bg-slate-50"
                  aria-label="关闭个人账户"
                  @click="showUserMenu = false"
                >
                  <span class="material-symbols-outlined text-[24px] leading-none">close</span>
                </button>
              </div>

              <div class="flex-1 overflow-y-auto px-4 pb-[calc(1rem+var(--safe-area-inset-bottom))]">
                <section class="flex flex-col items-center pb-6 pt-5 text-center">
                  <div class="flex size-20 items-center justify-center overflow-hidden rounded-full bg-[#a5ab87] text-[28px] font-medium text-white">
                    <img v-if="userStore.avatar" :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                    <span v-else>{{ userAvatarInitials }}</span>
                  </div>
                  <h3 class="mt-3 max-w-full truncate text-[20px] font-bold leading-tight text-[#0d0d0d]">
                    {{ userDisplayName }}
                  </h3>
                  <p class="mt-1 max-w-full truncate text-[14px] text-[#6b6b6b]">
                    {{ userAccountName }}
                  </p>
                  <button
                    type="button"
                    class="mt-4 rounded-full border border-[#d7d7dc] bg-transparent px-5 py-2 text-[14px] font-medium text-[#0d0d0d] transition-colors active:bg-white"
                    @click="handleOpenAccountSettings"
                  >
                    编辑个人资料
                  </button>
                </section>

                <section class="space-y-2.5">
                  <p class="px-2 text-[14px] font-semibold text-[#8a8a92]">账户</p>
                  <div class="overflow-hidden rounded-[22px] bg-white px-4">
                    <button
                      v-for="item in configNavItems"
                      :key="item.route"
                      type="button"
                      class="flex w-full items-center gap-3 border-b border-[#ededed] py-3.5 text-left text-[#0d0d0d] last:border-b-0"
                      @click="mobileProfileNavigate(item.route, item.query)"
                    >
                      <WkIcon :name="item.icon" :box-size="20" class="shrink-0" />
                      <span class="min-w-0 flex-1 truncate text-[1rem] font-medium">{{ item.label }}</span>
                      <span class="material-symbols-outlined shrink-0 text-[20px] leading-none text-[#c7c7cc]">chevron_right</span>
                    </button>

                    <button
                      type="button"
                      class="flex w-full items-center gap-3 py-3.5 text-left text-rose-600"
                      @click="handleLogout"
                    >
                      <span class="material-symbols-outlined inline-flex size-5 shrink-0 items-center justify-center text-[20px] leading-none">logout</span>
                      <span class="min-w-0 flex-1 truncate text-[1rem] font-medium">退出登录</span>
                    </button>

                    <button
                      type="button"
                      class="flex w-full items-center justify-between border-t border-[#ededed] py-3.5 text-[#8a8a92] transition-colors active:text-[#0d0d0d] disabled:cursor-wait disabled:opacity-70"
                      :disabled="checkingAppUpdate"
                      @click="handleManualUpdateCheck"
                    >
                      <span class="text-[0.95rem]">当前版本</span>
                      <span class="text-[0.95rem] tabular-nums">{{ checkingAppUpdate ? '检查中...' : `v${currentAppVersion}` }}</span>
                    </button>
                  </div>
                </section>
              </div>
            </div>
          </Transition>
        </aside>
      </Transition>

      <Transition name="drawer-overlay">
        <div
          v-if="isMobile && !drawerVisible && showUserMenu"
          class="fixed inset-0 z-[110] bg-slate-900/40 backdrop-blur-sm"
          @click="showUserMenu = false"
        />
      </Transition>
      <Transition name="profile-drawer">
        <div
          v-if="isMobile && !drawerVisible && showUserMenu"
          class="fixed bottom-0 left-0 right-0 z-[111] overflow-hidden rounded-t-2xl border-t border-slate-200 bg-white shadow-[0_-8px_30px_rgb(0,0,0,0.12)]"
        >
          <div class="p-4 pb-[calc(1rem+var(--safe-area-inset-bottom))]">
            <div class="space-y-1">
              <div class="flex items-center justify-between px-3 py-2">
                <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">个人账户</p>
                <button class="text-slate-300 transition-colors hover:text-slate-500" @click="showUserMenu = false">
                  <span class="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              <div class="mb-4 flex items-center gap-3 px-3 py-2">
                <div v-if="userStore.avatar" class="size-10 overflow-hidden rounded-full bg-slate-300">
                  <img :src="userStore.avatar" class="h-full w-full object-cover" alt="avatar" />
                </div>
                <div v-else class="flex size-10 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
                  {{ userStore.realname?.charAt(0) || userStore.username?.charAt(0) || 'U' }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ userStore.realname || userStore.username }}</p>
                  <p class="truncate text-xs text-slate-500">{{ userStore.userInfo?.deptName || '用户' }}</p>
                </div>
              </div>

              <div class="mx-3 my-1 h-px bg-slate-100" />

              <button
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-slate-700 transition-colors hover:bg-slate-50"
                @click="handleOpenAccountSettings"
              >
                <WkIcon name="set" :size="24" class="text-slate-400 transition-colors group-hover:text-primary" />
                <span class="text-sm font-medium">账号设置</span>
              </button>

              <button
                v-for="item in configNavItems"
                :key="item.route"
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors"
                :class="isActive(item.route, item.query) ? 'bg-primary/10 text-primary' : 'text-slate-700 hover:bg-slate-50'"
                @click="navigateTo(item.route, item.query)"
              >
                <WkIcon
                  :name="item.icon"
                  :size="24"
                  class="transition-colors"
                  :class="isActive(item.route, item.query) ? 'text-primary' : 'text-slate-400 group-hover:text-primary'"
                />
                <span class="text-sm font-medium">{{ item.label }}</span>
              </button>

              <button
                class="group flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-rose-600 transition-colors hover:bg-rose-50"
                @click="handleLogout"
              >
                <span class="material-symbols-outlined text-rose-400 transition-colors group-hover:text-rose-600">logout</span>
                <span class="text-sm font-medium">退出登录</span>
              </button>

              <button
                type="button"
                class="mx-3 flex items-center justify-between rounded-xl px-3 py-2 text-xs text-slate-400 transition-colors hover:bg-slate-50 hover:text-slate-600 disabled:cursor-wait disabled:opacity-70"
                :disabled="checkingAppUpdate"
                @click="handleManualUpdateCheck"
              >
                <span>当前版本</span>
                <span class="font-medium tabular-nums">{{ checkingAppUpdate ? '检查中...' : `v${currentAppVersion}` }}</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div
      ref="mainContentColumnRef"
      class="flex flex-1 flex-col overflow-hidden"
      :class="{ 'wk-mobile-top-bar-spacer': showMobileTopBar }"
    >
      <header
        v-if="showDesktopHeader"
        class="relative z-[100] flex h-[45px] shrink-0 items-center bg-white px-[10px] md:pl-8 md:pr-[24px]"
        :class="showDesktopHeader ? 'justify-between border-b border-slate-200' : 'border-b-0'"
      >
        <template v-if="showDesktopHeader">
        <div class="flex flex-1 items-center gap-4 mr-1">
          <div ref="searchPanelRef" class="relative w-full max-w-md">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-xl text-slate-400">search</span>
            <input
              v-model="globalSearchKeyword"
              type="text"
              class="w-full rounded-lg border-none bg-slate-100 py-2 pl-10 pr-4 text-sm outline-none transition-all focus:ring-2 focus:ring-primary/50"
              placeholder="搜索客户、关系、任务、日程、知识库..."
              @focus="handleGlobalSearchFocus"
              @keydown.enter.prevent="handleGlobalSearchEnter"
              @keydown.down.prevent="handleGlobalSearchArrow(1)"
              @keydown.up.prevent="handleGlobalSearchArrow(-1)"
              @keydown.esc.prevent="closeGlobalSearchDropdown"
              @input="debouncedGlobalSearch"
            />
            <Transition name="dropdown">
              <div
                v-if="showGlobalSearchDropdown"
                class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-[210] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-900/10"
                @mousedown.prevent
              >
                <div v-if="globalSearchLoading" class="flex items-center gap-3 px-4 py-4 text-sm text-slate-500">
                  <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
                  正在搜索...
                </div>

                <template v-else-if="globalSearchResults.length > 0">
                  <button
                    v-for="(result, index) in globalSearchResults"
                    :key="`${result.entityType}-${result.entityId}`"
                    type="button"
                    class="flex w-full items-start gap-3 px-4 py-3 text-left transition-colors"
                    :class="index === activeSearchResultIndex ? 'bg-primary/5' : 'hover:bg-slate-50'"
                    @mouseenter="activeSearchResultIndex = index"
                    @click="navigateToSearchResult(result)"
                  >
                    <div class="mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                      <WkIcon :name="getSearchResultIcon(result.entityType)" :size="18" />
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="truncate text-sm font-semibold text-slate-900">{{ result.title }}</p>
                      <p v-if="result.subtitle" class="mt-0.5 truncate text-xs text-slate-500">{{ result.subtitle }}</p>
                      <p v-if="result.summary" class="line-clamp-2 mt-1 text-xs leading-5 text-slate-400">{{ result.summary }}</p>
                    </div>
                    <span class="material-symbols-outlined mt-1 shrink-0 text-base text-slate-300">chevron_right</span>
                  </button>
                  <div class="border-t border-slate-100 px-4 py-2 text-center text-xs text-slate-400">
                    显示前 {{ globalSearchResults.length }} 条，共 {{ globalSearchTotal }} 条结果
                  </div>
                </template>

                <div v-else class="px-4 py-5 text-center text-sm text-slate-400">
                  没有找到匹配结果
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <button class="flex size-10 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            @click="showCreateCustomer = true"
            class="hidden md:flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-white shadow-sm shadow-primary/20 transition-colors hover:bg-primary/90"
          >
            <span class="material-symbols-outlined wk-plus-button-icon">add</span>
            新增客户
          </button>
        </div>
        </template>
      </header>

      <main
        class="flex-1 wk-safe-bottom"
        :class="isChatRoute ? 'overflow-hidden' : 'overflow-y-auto'"
      >
        <router-view v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>

    <CustomerUpsertDialog v-model="showCreateCustomer" mode="create" @success="handleCreateCustomerSuccess" />
    <RelationUpsertDialog
      v-model="showCreateRelation"
      @saved="handleCreateRelationSuccess"
    />
    <ProjectUpsertDialog
      v-model="showCreateProject"
      :editing-project="null"
      @submit="handleCreateProject"
    />
    <AccountSettingsModal v-model="showAccountSettingsModal" />
    <Teleport to="body">
      <Transition name="customer-search-dialog">
        <div
          v-if="customerSearchDialogVisible"
          class="wk-customer-search-dialog-shell fixed inset-0 z-[300]"
          :class="isMobile ? 'items-start justify-start bg-white px-0 py-0' : 'items-center justify-center bg-transparent px-4 py-8'"
          @click="closeCustomerSearchDialog"
        >
          <div
            class="flex w-full flex-col overflow-hidden bg-white"
            :class="isMobile
              ? 'h-[100dvh] max-h-none max-w-none rounded-none border-0 shadow-none'
              : 'max-h-[min(560px,calc(100vh-4rem))] max-w-[680px] rounded-2xl border border-[#d9d9d9] shadow-[0_24px_80px_rgb(15,23,42,0.18)]'"
            role="dialog"
            aria-modal="true"
            aria-label="搜索客户"
            @click.stop
          >
            <div
              v-if="isMobile"
              class="flex shrink-0 items-center gap-3 px-4 pb-2 pt-[calc(10px+var(--safe-area-inset-top))]"
            >
              <div class="flex h-10 min-w-0 flex-1 items-center gap-3 rounded-full bg-white px-4 shadow-[0_10px_28px_rgba(15,23,42,0.12)]">
                <span class="material-symbols-outlined shrink-0 text-[30px] leading-none text-[#0d0d0d]">search</span>
                <input
                  ref="customerSearchInputRef"
                  v-model="customerSearchKeyword"
                  type="search"
                  class="h-8 min-w-0 flex-1 border-none bg-transparent text-[16px] font-medium text-[#0d0d0d] outline-none placeholder:text-[#8f8f8f]"
                  placeholder="搜索客户"
                  autocomplete="off"
                  autocapitalize="off"
                  enterkeyhint="search"
                  @input="handleCustomerSearchInput"
                  @keydown.esc.prevent="closeCustomerSearchDialog"
                />
                <button
                  v-if="customerSearchKeyword"
                  type="button"
                  class="flex text-[16px] shrink-0 items-center justify-center rounded-full bg-[#0d0d0d] text-white transition-colors active:bg-[#333]"
                  aria-label="清空搜索"
                  @click="clearCustomerSearchKeyword"
                >
                  <span class="material-symbols-outlined text-[16px] leading-none">close</span>
                </button>
              </div>
              <button
                type="button"
                class="flex size-10 shrink-0 items-center justify-center rounded-full bg-white text-[#0d0d0d] shadow-[0_10px_28px_rgba(15,23,42,0.10)] transition-colors active:bg-slate-50"
                aria-label="关闭搜索"
                title="关闭搜索"
                @click="closeCustomerSearchDialog"
              >
                <span class="material-symbols-outlined text-[28px] leading-none">close</span>
              </button>
            </div>
            <div v-else class="flex h-16 shrink-0 items-center gap-3 border-b border-[#ececec] px-6">
              <input
                ref="customerSearchInputRef"
                v-model="customerSearchKeyword"
                type="text"
                class="h-12 min-w-0 flex-1 border-none bg-transparent text-[16px] font-medium text-[#0d0d0d] outline-none placeholder:text-[#8f8f8f]"
                placeholder="搜索客户..."
                @input="handleCustomerSearchInput"
                @keydown.esc.prevent="closeCustomerSearchDialog"
              />
              <button
                type="button"
                class="flex size-8 shrink-0 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#f3f3f3] hover:text-[#0d0d0d]"
                aria-label="关闭搜索"
                title="关闭搜索"
                @click="closeCustomerSearchDialog"
              >
                <span class="material-symbols-outlined text-[22px] leading-none">close</span>
              </button>
            </div>

            <div
              ref="customerSearchScrollRef"
              class="flex-1 overflow-y-auto"
              :class="isMobile ? 'min-h-0 px-5 pb-[calc(1rem+var(--safe-area-inset-bottom))] pt-2' : 'min-h-[320px] px-4 py-3'"
              @scroll="handleCustomerSearchScroll"
              @touchstart.passive="handleCustomerSearchTouchStart"
              @touchmove="handleCustomerSearchTouchMove"
              @touchend.passive="handleCustomerSearchTouchEnd"
              @touchcancel.passive="handleCustomerSearchTouchCancel"
            >
              <div
                v-if="isMobile"
                class="wk-mobile-customer-search-pull-indicator"
                :class="{ 'is-active': customerSearchPullDistance > 0 || customerSearchRefreshing }"
                :style="{ height: `${customerSearchPullIndicatorHeight}px` }"
              >
                <span class="material-symbols-outlined" :class="{ 'animate-spin': customerSearchRefreshing }">
                  {{ customerSearchRefreshing ? 'progress_activity' : 'keyboard_arrow_down' }}
                </span>
                <span>{{ customerSearchPullLabel }}</span>
              </div>
              <div v-if="customerSearchLoading && customerSearchCustomers.length === 0" class="flex h-56 items-center justify-center">
                <span class="material-symbols-outlined animate-spin text-2xl leading-none text-[#c9c9c9]">progress_activity</span>
              </div>
              <div v-else-if="customerSearchCustomers.length === 0" class="flex h-56 flex-col items-center justify-center text-center">
                <span class="material-symbols-outlined mb-2 text-4xl leading-none text-[#d8d8d8]">search_off</span>
                <p class="text-sm text-[#8f8f8f]">{{ customerSearchKeyword.trim() ? '没有找到匹配客户' : '暂无客户数据' }}</p>
              </div>
              <template v-else>
                <p class="px-2 pb-2 pt-1 text-[13px] font-medium text-[#8f8f8f]">
                  {{ customerSearchKeyword.trim() ? '搜索结果' : '' }}
                </p>
                <button
                  v-for="customer in customerSearchCustomers"
                  :key="customer.customerId"
                  type="button"
                  class="group flex w-full min-w-0 items-center gap-3 rounded-xl text-left transition-colors hover:bg-[#f9f9f9]"
                  :class="isMobile ? 'px-0 py-4' : 'px-3 py-3'"
                  @click="handleSelectCustomerFromSearch(customer)"
                >
                  <div class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full border border-[#ececec] bg-white">
                    <img
                      v-if="customer.logoUrl"
                      :src="customer.logoUrl"
                      :alt="customer.companyName || 'company logo'"
                      class="size-full object-contain"
                    />
                    <span v-else class="flex size-full items-center justify-center bg-primary/10 text-sm font-semibold text-primary">
                      {{ customer.companyName?.charAt(0) || '?' }}
                    </span>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-[15px] font-medium leading-5 text-[#0d0d0d]">{{ customer.companyName }}</p>
                    <p class="mt-1 truncate text-xs leading-4 text-[#8f8f8f]">
                      {{ getCustomerSearchSubtitle(customer) }}
                    </p>
                  </div>
                  <!-- <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-[#c9c9c9] transition-colors group-hover:text-[#8f8f8f]">chevron_right</span> -->
                </button>
                <div v-if="customerSearchAppending" class="flex justify-center py-3">
                  <span class="material-symbols-outlined animate-spin text-[18px] leading-none text-[#c9c9c9]">progress_activity</span>
                </div>
                <div
                  v-if="isMobile && !customerSearchLoading"
                  class="px-2 py-4 text-center text-xs font-medium text-[#8f8f8f]"
                >
                  {{ customerSearchHasMore ? '上滑加载更多' : '没有更多客户了' }}
                </div>
              </template>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
    <FloatingActionButton
      v-if="showFloatingNewChatButton"
      @new-chat="handleFloatingNewChat"
    />
    <AiChatDrawer />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import defaultLogoImg from '@/assets/images/logo.png'
import FloatingActionButton from '@/components/common/FloatingActionButton.vue'
import AiChatDrawer from '@/components/common/AiChatDrawer.vue'
import AccountSettingsModal from '@/views/profile/components/AccountSettingsModal.vue'
import ChatSessionActionsPopover from '@/components/layout/ChatSessionActionsPopover.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'
import RelationUpsertDialog from '@/views/relation/components/RelationUpsertDialog.vue'
import ProjectUpsertDialog from '@/views/project/components/ProjectUpsertDialog.vue'
import type { WkIconName } from '@/components/common/wkIcon'
import { queryGlobalSearch, type GlobalSearchResult } from '@/api/search'
import { queryCustomerList } from '@/api/customer'
import { queryProductList } from '@/api/product'
import { queryAddressBook } from '@/api/addressBook'
import { queryRelationList } from '@/api/relation'
import { useChatDrawer } from '@/composables/useChatDrawer'
import { useEnterpriseStore } from '@/stores/enterprise'
import { useResponsive } from '@/composables/useResponsive'
import { useTheme } from '@/composables/useTheme'
import { useChatStore } from '@/stores/chat'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { useEnumStore } from '@/stores/enums'
import { appEvents, APP_EVENT } from '@/utils/events'
import { confirmDeleteChatSession } from '@/utils/confirmDeleteChatSession'
import { shouldCloseMobileDrawerFromSwipe, type SwipePoint } from '@/utils/mobileDrawerSwipe'
import { isNativeMobileRuntime } from '@/utils/nativeMobileRuntime'
import {
  DEFAULT_SIDEBAR_MODULE_ORDER,
  normalizeSidebarModuleOrder,
  readStoredSidebarModuleOrder,
  writeStoredSidebarModuleOrder,
  type SidebarModuleKey
} from '@/utils/sidebarModuleOrder'
import type { ChatSession } from '@/types/common'
import type { CustomerListVO } from '@/types/customer'
import type { ProductVO } from '@/types/product'
import type { ProjectEntity } from '@/types/project'
import type { AddressBookEmployee } from '@/types/addressBook'
import type { RelationVO } from '@/types/relation'

const DEFAULT_CURRENT_VERSION = '1.0.0'

type CapacitorUpdateModule = {
  checkForUpdates: () => Promise<'updated' | 'none' | 'unsupported' | 'failed'>
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const enterpriseStore = useEnterpriseStore()
const chatStore = useChatStore()
const projectStore = useProjectStore()
const enumStore = useEnumStore()
enumStore.ensureCustomerStage()
const { isMobile } = useResponsive()
const { isOpen: chatDrawerOpen } = useChatDrawer()
const { isDark, toggleTheme } = useTheme()
const themeIcon = computed(() => (isDark.value ? 'light_mode' : 'dark_mode'))
const themeButtonLabel = computed(() => (isDark.value ? '浅色模式' : '深色模式'))

/**
 * 主内容列（顶栏 + router-view 所在 flex 列）在侧栏展开时的宽度小于该值且仍为 PC 时，自动收起左侧一级导航；
 * 宽度变回足够时恢复进入窄区前的展开/收起状态（用户在窄区内手动切换侧栏会更新该状态）。
 *
 * 恢复时用「收起侧栏下的主列宽度 − 侧栏宽度差」估算展开后的主列宽度，与阈值比较，避免收起后主列变宽立刻触发恢复导致抖动。
 */
const CHAT_COMPOSER_MIN_WIDTH_PX = 468
const PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX = CHAT_COMPOSER_MIN_WIDTH_PX
/** 与模板 `w-64` / `w-[52px]` 一致，用于主列宽度滞回 */
const PRIMARY_SIDEBAR_WIDTH_EXPANDED_PX = 256
const PRIMARY_SIDEBAR_WIDTH_COLLAPSED_PX = 52
const PRIMARY_SIDEBAR_WIDTH_DELTA_PX = PRIMARY_SIDEBAR_WIDTH_EXPANDED_PX - PRIMARY_SIDEBAR_WIDTH_COLLAPSED_PX
const PRIMARY_SIDEBAR_TRANSITION_MS = 200
const DYNAMIC_SIDEBAR_SECTION_ORDER_BASE = 20
const SIDEBAR_STORAGE_KEYS = {
  primaryCollapsed: 'wk_ai_crm:main_layout:primary_sidebar_collapsed:v1',
  recentChatExpanded: 'wk_ai_crm:main_layout:recent_chat_sessions_expanded:v1',
  sidebarProjectsExpanded: 'wk_ai_crm:main_layout:sidebar_projects_expanded:v1',
  sidebarProjectCache: 'wk_ai_crm:main_layout:sidebar_project_cache:v1',
  sidebarCustomersExpanded: 'wk_ai_crm:main_layout:sidebar_customers_expanded:v1',
  sidebarProductsExpanded: 'wk_ai_crm:main_layout:sidebar_products_expanded:v1',
  sidebarAddressBookExpanded: 'wk_ai_crm:main_layout:sidebar_address_book_expanded:v1',
  sidebarRelationsExpanded: 'wk_ai_crm:main_layout:sidebar_relations_expanded:v1'
} as const

const SIDEBAR_MODULE_LABELS: Record<SidebarModuleKey, string> = {
  recent: '最近',
  customer: '客户',
  product: '产品',
  project: '项目',
  relation: '关系',
  addressBook: '通讯录'
}

const SIDEBAR_MODULE_ICONS: Record<SidebarModuleKey, string> = {
  recent: 'forum',
  customer: 'business_center',
  product: 'inventory_2',
  project: 'folder',
  relation: 'account_tree',
  addressBook: 'contacts'
}

function mainContentWidthAsIfSidebarExpanded(sidebarCollapsed: boolean, mainColumnWidth: number) {
  return sidebarCollapsed ? mainColumnWidth - PRIMARY_SIDEBAR_WIDTH_DELTA_PX : mainColumnWidth
}

function readStoredBoolean(key: string, fallback: boolean): boolean {
  if (typeof window === 'undefined') return fallback
  try {
    const raw = window.localStorage.getItem(key)
    if (raw === '1') return true
    if (raw === '0') return false
  } catch {
    // Ignore storage failures.
  }
  return fallback
}

function writeStoredBoolean(key: string, value: boolean) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(key, value ? '1' : '0')
  } catch {
    // Ignore storage failures.
  }
}

function getCurrentSidebarOrderUserId() {
  return userStore.userInfo?.userId || userStore.userId || undefined
}

function getInitialSidebarModuleOrder(): SidebarModuleKey[] {
  const stored = readStoredSidebarModuleOrder(getCurrentSidebarOrderUserId())
  if (stored) return stored
  return normalizeSidebarModuleOrder(userStore.userInfo?.preferences?.sidebarModuleOrder)
}

const sidebarModuleOrder = ref<SidebarModuleKey[]>(getInitialSidebarModuleOrder())
const sidebarDraftModuleOrder = ref<SidebarModuleKey[]>([...sidebarModuleOrder.value])
const sidebarSortMode = ref(false)
const sidebarSavingModuleOrder = ref(false)
const sidebarDraggingModuleKey = ref<SidebarModuleKey | null>(null)
const sidebarDragOverModuleKey = ref<SidebarModuleKey | null>(null)

function getSidebarModuleLabel(moduleKey: SidebarModuleKey) {
  return SIDEBAR_MODULE_LABELS[moduleKey]
}

function getSidebarModuleIcon(moduleKey: SidebarModuleKey) {
  return SIDEBAR_MODULE_ICONS[moduleKey]
}

function getSidebarModuleRenderOrder(moduleKey: SidebarModuleKey) {
  const orderIndex = sidebarModuleOrder.value.indexOf(moduleKey)
  if (orderIndex >= 0) return DYNAMIC_SIDEBAR_SECTION_ORDER_BASE + orderIndex
  const defaultIndex = DEFAULT_SIDEBAR_MODULE_ORDER.indexOf(moduleKey)
  return DYNAMIC_SIDEBAR_SECTION_ORDER_BASE + Math.max(defaultIndex, 0)
}

function applySidebarModuleOrder(order: readonly SidebarModuleKey[]) {
  sidebarModuleOrder.value = normalizeSidebarModuleOrder([...order])
  if (!sidebarSortMode.value) {
    sidebarDraftModuleOrder.value = [...sidebarModuleOrder.value]
  }
}

function hydrateSidebarModuleOrderFromUserInfo() {
  const info = userStore.userInfo
  if (!info) {
    applySidebarModuleOrder(DEFAULT_SIDEBAR_MODULE_ORDER)
    return
  }

  const serverOrder = info.preferences?.sidebarModuleOrder
  if (Array.isArray(serverOrder)) {
    const normalized = normalizeSidebarModuleOrder(serverOrder)
    applySidebarModuleOrder(normalized)
    writeStoredSidebarModuleOrder(normalized, info.userId)
    return
  }

  const stored = readStoredSidebarModuleOrder(info.userId)
  applySidebarModuleOrder(stored || DEFAULT_SIDEBAR_MODULE_ORDER)
}

function openSidebarSortMode() {
  if (primarySidebarContentCollapsed.value) return
  recentChatSessionsMoreVisible.value = false
  sidebarDraftModuleOrder.value = [...sidebarModuleOrder.value]
  sidebarSortMode.value = true
}

function cancelSidebarSortMode() {
  sidebarDraftModuleOrder.value = [...sidebarModuleOrder.value]
  sidebarDraggingModuleKey.value = null
  sidebarDragOverModuleKey.value = null
  sidebarSortMode.value = false
}

function restoreDefaultSidebarDraftOrder() {
  sidebarDraftModuleOrder.value = [...DEFAULT_SIDEBAR_MODULE_ORDER]
}

function handleSidebarSortDragStart(event: DragEvent, moduleKey: SidebarModuleKey) {
  sidebarDraggingModuleKey.value = moduleKey
  sidebarDragOverModuleKey.value = moduleKey
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', moduleKey)
  }
}

function moveSidebarDraftModule(sourceKey: SidebarModuleKey, targetKey: SidebarModuleKey, insertAfterTarget: boolean) {
  if (sourceKey === targetKey) return false
  if (!DEFAULT_SIDEBAR_MODULE_ORDER.includes(sourceKey) || !DEFAULT_SIDEBAR_MODULE_ORDER.includes(targetKey)) return false

  const nextOrder = [...sidebarDraftModuleOrder.value]
  const sourceIndex = nextOrder.indexOf(sourceKey)
  const targetIndex = nextOrder.indexOf(targetKey)
  if (sourceIndex < 0 || targetIndex < 0) return false

  nextOrder.splice(sourceIndex, 1)
  const targetIndexAfterRemoval = nextOrder.indexOf(targetKey)
  if (targetIndexAfterRemoval < 0) return false

  const insertIndex = targetIndexAfterRemoval + (insertAfterTarget ? 1 : 0)
  if (sourceIndex === insertIndex) return false

  nextOrder.splice(insertIndex, 0, sourceKey)
  const normalizedOrder = normalizeSidebarModuleOrder(nextOrder)
  if (normalizedOrder.join('|') === sidebarDraftModuleOrder.value.join('|')) return false

  sidebarDraftModuleOrder.value = normalizedOrder
  return true
}

function getSidebarSortInsertAfter(event: DragEvent) {
  const targetElement = event.currentTarget instanceof HTMLElement ? event.currentTarget : null
  return targetElement
    ? event.clientY > targetElement.getBoundingClientRect().top + targetElement.getBoundingClientRect().height / 2
    : false
}

function getSidebarSortSourceKey(event: DragEvent): SidebarModuleKey | null {
  const sourceKey = sidebarDraggingModuleKey.value || event.dataTransfer?.getData('text/plain') as SidebarModuleKey
  return sourceKey && DEFAULT_SIDEBAR_MODULE_ORDER.includes(sourceKey) ? sourceKey : null
}

function handleSidebarSortDragOver(event: DragEvent, targetKey: SidebarModuleKey) {
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  sidebarDragOverModuleKey.value = targetKey
  const sourceKey = getSidebarSortSourceKey(event)
  if (!sourceKey) return
  moveSidebarDraftModule(sourceKey, targetKey, getSidebarSortInsertAfter(event))
}

function handleSidebarSortDrop(event: DragEvent, targetKey: SidebarModuleKey) {
  const sourceKey = getSidebarSortSourceKey(event)
  if (sourceKey) {
    moveSidebarDraftModule(sourceKey, targetKey, getSidebarSortInsertAfter(event))
  }
  sidebarDragOverModuleKey.value = null
}

function handleSidebarSortDragEnd() {
  sidebarDraggingModuleKey.value = null
  sidebarDragOverModuleKey.value = null
}

async function saveSidebarModuleOrder() {
  if (sidebarSavingModuleOrder.value) return
  const previousOrder = [...sidebarModuleOrder.value]
  const nextOrder = normalizeSidebarModuleOrder(sidebarDraftModuleOrder.value)
  sidebarSavingModuleOrder.value = true
  try {
    const preferences = await userStore.updatePreferences({ sidebarModuleOrder: nextOrder })
    const savedOrder = normalizeSidebarModuleOrder(preferences.sidebarModuleOrder)
    applySidebarModuleOrder(savedOrder)
    writeStoredSidebarModuleOrder(savedOrder, getCurrentSidebarOrderUserId())
    sidebarSortMode.value = false
    ElMessage.success('排序已保存')
  } catch (error) {
    console.error('Save sidebar module order failed:', error)
    applySidebarModuleOrder(previousOrder)
    ElMessage.error('排序保存失败')
  } finally {
    sidebarSavingModuleOrder.value = false
    sidebarDraggingModuleKey.value = null
    sidebarDragOverModuleKey.value = null
  }
}

function normalizeSidebarProjectItem(value: unknown): SidebarProjectItem | null {
  if (!value || typeof value !== 'object') return null
  const record = value as Partial<Record<'projectId' | 'name', unknown>>
  const projectId = typeof record.projectId === 'string' ? record.projectId : String(record.projectId || '')
  const name = typeof record.name === 'string' ? record.name : String(record.name || '')
  if (!projectId || !name) return null
  return { projectId, name }
}

function readStoredSidebarProjects(): SidebarProjectItem[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(SIDEBAR_STORAGE_KEYS.sidebarProjectCache)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .map(normalizeSidebarProjectItem)
      .filter((project): project is SidebarProjectItem => Boolean(project))
      .slice(0, 12)
  } catch {
    return []
  }
}

function writeStoredSidebarProjects(projects: SidebarProjectItem[]) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(SIDEBAR_STORAGE_KEYS.sidebarProjectCache, JSON.stringify(projects.slice(0, 12)))
  } catch {
    // Ignore storage failures.
  }
}

const mainContentColumnRef = ref<HTMLElement | null>(null)
const mainContentColumnWidth = ref(0)
const mainContentColumnMeasured = ref(false)

const layoutNarrowAutoCollapseActive = ref(false)
/** 解除窄区限制后是否保持侧栏展开（非 collapsed） */
const layoutNarrowSavedExpanded = ref(true)
const layoutNarrowProgrammatic = ref(false)

function updateMainContentColumnWidth() {
  const el = mainContentColumnRef.value
  if (!el) return
  mainContentColumnWidth.value = el.getBoundingClientRect().width
  mainContentColumnMeasured.value = true
}

function runLayoutNarrowProgrammatic(fn: () => void) {
  layoutNarrowProgrammatic.value = true
  try {
    fn()
  } finally {
    queueMicrotask(() => {
      layoutNarrowProgrammatic.value = false
    })
  }
}

const drawerVisible = ref(false)
const mobileDrawerDragProgress = ref(0)
const mobileDrawerDragActive = ref(false)
const mobileDrawerDragSettling = ref(false)
const mobileDrawerClosing = ref(false)
const mobileMainMenuGlobalSwipeStart = ref<SwipePoint | null>(null)
const mobileDrawerTouchStart = ref<SwipePoint | null>(null)
/** PC：一级侧栏收起为图标栏 */
const primarySidebarCollapsed = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.primaryCollapsed, false))
const primarySidebarContentCollapsed = ref(primarySidebarCollapsed.value)
const primarySidebarTransitioning = ref(false)
let primarySidebarTransitionTimer: ReturnType<typeof setTimeout> | null = null
/** 一级侧栏收起时：鼠标在整块 aside 内则顶栏 logo 区显示为折叠图标 */
const collapsedSidebarAsideHovered = ref(false)
const MOBILE_MAIN_MENU_TOUCH_WIDTH_PX = 28
const MOBILE_MAIN_MENU_MIN_SWIPE_PX = 64
const MOBILE_MAIN_MENU_MAX_VERTICAL_PX = 88
const MOBILE_MAIN_MENU_DIRECTION_RATIO = 1.25
const MOBILE_MAIN_MENU_MIN_PREVIEW_PX = 6
const MOBILE_DRAWER_TRANSITION_MS = 180

function onCollapsedPrimarySidebarEnter() {
  if (primarySidebarCollapsed.value) collapsedSidebarAsideHovered.value = true
}

function onCollapsedPrimarySidebarLeave() {
  collapsedSidebarAsideHovered.value = false
}

function blurMobileKeyboardTarget() {
  if (typeof document === 'undefined') return
  const activeElement = document.activeElement
  if (!(activeElement instanceof HTMLElement)) return
  if (!activeElement.matches('input, textarea, select, [contenteditable]:not([contenteditable="false"])')) return
  activeElement.blur()
}

function openMobileDrawer() {
  if (!isMobile.value) return
  blurMobileKeyboardTarget()
  clearMobileDrawerClosingTimer()
  mobileDrawerClosing.value = false
  resetMobileDrawerDragPreview()
  drawerVisible.value = true
}

function closeMobileDrawer() {
  const shouldKeepClosingLayer = isMobile.value && drawerVisible.value
  if (shouldKeepClosingLayer) {
    scheduleMobileDrawerClosingState()
  }
  drawerVisible.value = false
  recentChatSessionsMoreVisible.value = false
  showUserMenu.value = false
  resetGlobalMobileMainMenuSwipe()
  resetMobileDrawerSwipe()
  if (!shouldKeepClosingLayer) {
    resetMobileDrawerDragPreview()
  }
}

const mobileDrawerRendered = computed(() =>
  isMobile.value && (
    drawerVisible.value ||
    mobileDrawerDragActive.value ||
    mobileDrawerDragSettling.value ||
    mobileDrawerClosing.value
  )
)

const mobileDrawerResolvedProgress = computed(() => {
  const dragProgress = Math.min(Math.max(mobileDrawerDragProgress.value, 0), 1)
  if (mobileDrawerDragActive.value || mobileDrawerDragSettling.value) return dragProgress
  return drawerVisible.value ? 1 : 0
})

const mobileDrawerOverlayStyle = computed(() => ({
  opacity: mobileDrawerResolvedProgress.value,
  transition: mobileDrawerDragActive.value ? 'none' : 'opacity 0.18s ease',
}))

const mobileDrawerPanelStyle = computed(() => ({
  transform: `translate3d(${(mobileDrawerResolvedProgress.value - 1) * 100}%, 0, 0)`,
  transition: mobileDrawerDragActive.value ? 'none' : 'transform 0.18s ease',
}))

function clearMobileDrawerDragSettleTimer() {
  if (mobileDrawerDragSettleTimer) {
    clearTimeout(mobileDrawerDragSettleTimer)
    mobileDrawerDragSettleTimer = null
  }
}

function clearMobileDrawerClosingTimer() {
  if (mobileDrawerClosingTimer) {
    clearTimeout(mobileDrawerClosingTimer)
    mobileDrawerClosingTimer = null
  }
}

function scheduleMobileDrawerClosingState() {
  clearMobileDrawerClosingTimer()
  mobileDrawerClosing.value = true
  mobileDrawerClosingTimer = setTimeout(() => {
    mobileDrawerClosing.value = false
    mobileDrawerClosingTimer = null
    resetMobileDrawerDragPreview()
  }, MOBILE_DRAWER_TRANSITION_MS)
}

function settleMobileDrawerDragClosed() {
  clearMobileDrawerDragSettleTimer()
  mobileDrawerDragActive.value = false
  mobileDrawerDragSettling.value = true
  mobileDrawerDragProgress.value = 0
  mobileDrawerDragSettleTimer = setTimeout(() => {
    mobileDrawerDragSettling.value = false
    mobileDrawerDragSettleTimer = null
  }, 180)
}

function settleMobileDrawerDragOpen() {
  clearMobileDrawerDragSettleTimer()
  mobileDrawerDragActive.value = false
  mobileDrawerDragSettling.value = true
  mobileDrawerDragProgress.value = 1
  mobileDrawerDragSettleTimer = setTimeout(() => {
    mobileDrawerDragSettling.value = false
    mobileDrawerDragProgress.value = 0
    mobileDrawerDragSettleTimer = null
  }, 180)
}

function settleMobileDrawerSwipeClosed() {
  clearMobileDrawerDragSettleTimer()
  mobileDrawerDragActive.value = false
  mobileDrawerDragSettling.value = true
  mobileDrawerDragProgress.value = 0
  drawerVisible.value = false
  recentChatSessionsMoreVisible.value = false
  showUserMenu.value = false
  resetGlobalMobileMainMenuSwipe()
  resetMobileDrawerSwipe()
  mobileDrawerDragSettleTimer = setTimeout(() => {
    mobileDrawerDragSettling.value = false
    mobileDrawerDragSettleTimer = null
  }, 180)
}

function resetMobileDrawerDragPreview() {
  clearMobileDrawerDragSettleTimer()
  mobileDrawerDragActive.value = false
  mobileDrawerDragSettling.value = false
  mobileDrawerDragProgress.value = 0
}

function handleMobileMainMenuDrag(payload?: MobileMainMenuDragPayload) {
  if (!isMobile.value) return
  const progress = Math.min(Math.max(Number(payload?.progress ?? 0), 0), 1)
  if (payload?.phase === 'end') {
    if (payload.open) {
      openMobileDrawer()
      return
    }
    settleMobileDrawerDragClosed()
    return
  }

  if (drawerVisible.value) return
  if (!mobileDrawerDragActive.value && progress > 0) {
    blurMobileKeyboardTarget()
  }
  clearMobileDrawerDragSettleTimer()
  mobileDrawerDragActive.value = true
  mobileDrawerDragSettling.value = false
  mobileDrawerDragProgress.value = progress
}

function resetGlobalMobileMainMenuSwipe() {
  mobileMainMenuGlobalSwipeStart.value = null
}

function canStartGlobalMobileMainMenuSwipe(target: EventTarget | null) {
  if (!isMobile.value || drawerVisible.value || showUserMenu.value || customerSearchDialogVisible.value) return false
  if (chatDrawerOpen.value) return false
  if (!(target instanceof Element)) return true
  return !target.closest('textarea, input, select, button, a, [role="button"], [contenteditable="true"], .el-overlay, .el-popper, .el-message, .el-message-box')
}

function getGlobalMobileMainMenuSwipeProgress(start: SwipePoint, point: SwipePoint) {
  const deltaX = Math.max(0, point.clientX - start.clientX)
  const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 360
  return Math.min(deltaX / Math.max(viewportWidth, 1), 1)
}

function shouldOpenGlobalMobileMainMenu(start: SwipePoint, end: SwipePoint) {
  if (start.clientX > MOBILE_MAIN_MENU_TOUCH_WIDTH_PX) return false
  const deltaX = end.clientX - start.clientX
  const deltaY = end.clientY - start.clientY
  const horizontalDistance = Math.abs(deltaX)
  const verticalDistance = Math.abs(deltaY)

  return deltaX >= MOBILE_MAIN_MENU_MIN_SWIPE_PX
    && verticalDistance <= MOBILE_MAIN_MENU_MAX_VERTICAL_PX
    && horizontalDistance >= verticalDistance * MOBILE_MAIN_MENU_DIRECTION_RATIO
}

function handleGlobalMobileMainMenuTouchStart(event: TouchEvent) {
  const point = getTouchPoint(event.touches[0])
  if (!point || point.clientX > MOBILE_MAIN_MENU_TOUCH_WIDTH_PX || !canStartGlobalMobileMainMenuSwipe(event.target)) {
    resetGlobalMobileMainMenuSwipe()
    return
  }
  mobileMainMenuGlobalSwipeStart.value = point
}

function handleGlobalMobileMainMenuTouchMove(event: TouchEvent) {
  const start = mobileMainMenuGlobalSwipeStart.value
  const point = getTouchPoint(event.touches[0])
  if (!start || !point) return

  const deltaX = point.clientX - start.clientX
  const deltaY = point.clientY - start.clientY
  const horizontalDistance = Math.abs(deltaX)
  const verticalDistance = Math.abs(deltaY)

  if (deltaX <= 0) {
    handleMobileMainMenuDrag({ progress: 0, phase: 'move' })
    return
  }
  if (
    verticalDistance > MOBILE_MAIN_MENU_MAX_VERTICAL_PX
    || (verticalDistance > MOBILE_MAIN_MENU_MIN_PREVIEW_PX && verticalDistance > horizontalDistance)
  ) {
    handleMobileMainMenuDrag({ progress: 0, phase: 'end' })
    resetGlobalMobileMainMenuSwipe()
    return
  }
  if (horizontalDistance < MOBILE_MAIN_MENU_MIN_PREVIEW_PX) return
  if (horizontalDistance < verticalDistance * MOBILE_MAIN_MENU_DIRECTION_RATIO) return

  if (event.cancelable) {
    event.preventDefault()
  }
  handleMobileMainMenuDrag({
    progress: getGlobalMobileMainMenuSwipeProgress(start, point),
    phase: 'move'
  })
}

function handleGlobalMobileMainMenuTouchEnd(event: TouchEvent) {
  const start = mobileMainMenuGlobalSwipeStart.value
  const end = getTouchPoint(event.changedTouches[0])
  resetGlobalMobileMainMenuSwipe()
  if (!start || !end) return

  const shouldOpen = shouldOpenGlobalMobileMainMenu(start, end)
  handleMobileMainMenuDrag({
    progress: shouldOpen ? 1 : 0,
    phase: 'end',
    open: shouldOpen
  })
}

function handleGlobalMobileMainMenuTouchCancel() {
  if (mobileMainMenuGlobalSwipeStart.value) {
    handleMobileMainMenuDrag({ progress: 0, phase: 'end' })
  }
  resetGlobalMobileMainMenuSwipe()
}

function getTouchPoint(touch: Touch | undefined): SwipePoint | null {
  if (!touch) return null
  return {
    clientX: touch.clientX,
    clientY: touch.clientY,
  }
}

function resetMobileDrawerSwipe() {
  mobileDrawerTouchStart.value = null
}

function handleMobileDrawerTouchStart(event: TouchEvent) {
  mobileDrawerTouchStart.value = getTouchPoint(event.touches[0])
}

function handleMobileDrawerTouchMove(event: TouchEvent) {
  const start = mobileDrawerTouchStart.value
  const point = getTouchPoint(event.touches[0])
  if (!drawerVisible.value || !start || !point) return

  const deltaX = point.clientX - start.clientX
  const deltaY = point.clientY - start.clientY
  const horizontalDistance = Math.abs(deltaX)
  const verticalDistance = Math.abs(deltaY)

  if (deltaX >= 0) {
    if (mobileDrawerDragActive.value) {
      mobileDrawerDragProgress.value = 1
    }
    return
  }

  if (
    verticalDistance > MOBILE_MAIN_MENU_MAX_VERTICAL_PX
    || (verticalDistance > MOBILE_MAIN_MENU_MIN_PREVIEW_PX && verticalDistance > horizontalDistance)
  ) {
    if (mobileDrawerDragActive.value) settleMobileDrawerDragOpen()
    resetMobileDrawerSwipe()
    return
  }

  if (horizontalDistance < MOBILE_MAIN_MENU_MIN_PREVIEW_PX) return
  if (horizontalDistance < verticalDistance * MOBILE_MAIN_MENU_DIRECTION_RATIO) return

  if (event.cancelable) {
    event.preventDefault()
  }

  const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 360
  clearMobileDrawerDragSettleTimer()
  mobileDrawerDragActive.value = true
  mobileDrawerDragSettling.value = false
  mobileDrawerDragProgress.value = Math.min(Math.max(1 - horizontalDistance / Math.max(viewportWidth, 1), 0), 1)
}

function handleMobileDrawerTouchEnd(event: TouchEvent) {
  const start = mobileDrawerTouchStart.value
  const end = getTouchPoint(event.changedTouches[0])
  resetMobileDrawerSwipe()
  if (!start || !end) return

  if (shouldCloseMobileDrawerFromSwipe({ start, end })) {
    if (mobileDrawerDragActive.value) {
      settleMobileDrawerSwipeClosed()
      return
    }
    closeMobileDrawer()
    return
  }

  if (mobileDrawerDragActive.value) {
    settleMobileDrawerDragOpen()
  }
}

function handleMobileDrawerTouchCancel() {
  resetMobileDrawerSwipe()
  if (mobileDrawerDragActive.value) {
    settleMobileDrawerDragOpen()
  }
}

const primaryNavRef = ref<HTMLElement | null>(null)
const primaryNavHasScrollbar = ref(false)
const primaryNavScrolling = ref(false)
let primaryNavScrollEndTimer: ReturnType<typeof setTimeout> | null = null

function onPrimaryNavScroll() {
  maybeLoadMoreSidebarObjects()
  if (!primaryNavHasScrollbar.value) return
  primaryNavScrolling.value = true
  if (primaryNavScrollEndTimer) clearTimeout(primaryNavScrollEndTimer)
  primaryNavScrollEndTimer = setTimeout(() => {
    primaryNavScrollEndTimer = null
    primaryNavScrolling.value = false
  }, 400)
}

function getPrimaryNavScrollTop(): number {
  return primaryNavRef.value?.scrollTop ?? 0
}

async function restorePrimaryNavScrollTop(scrollTop: number) {
  await nextTick()
  const el = primaryNavRef.value
  if (!el) return
  const maxScrollTop = Math.max(0, el.scrollHeight - el.clientHeight)
  el.scrollTop = Math.min(scrollTop, maxScrollTop)
}
/** PC 侧栏「最近」对话列表折叠 */
const recentChatSessionsExpanded = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.recentChatExpanded, true))
const recentHistoryKeyword = ref('')
const sidebarProjectsExpanded = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarProjectsExpanded, true))
const sidebarCustomersExpanded = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarCustomersExpanded, true))
const sidebarProductsExpanded = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarProductsExpanded, true))
const projectHeaderHovered = ref(false)
const sidebarAddressBookExpanded = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarAddressBookExpanded, true))
const sidebarRelationsExpanded = ref(readStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarRelationsExpanded, true))
const customerHeaderHovered = ref(false)
const productHeaderHovered = ref(false)
const addressBookHeaderHovered = ref(false)
const relationHeaderHovered = ref(false)
const sidebarCustomerKeyword = ref('')
const recentChatSessionsMoreVisible = ref(false)
const RECENT_CHAT_SESSION_LIMIT = 5
const MOBILE_RECENT_CHAT_SESSION_LIMIT = 8
const SIDEBAR_CUSTOMER_LIMIT = 10
const SIDEBAR_PRODUCT_LIMIT = 10
const SIDEBAR_EMPLOYEE_LIMIT = 10
const SIDEBAR_RELATION_LIMIT = 10
const CUSTOMER_SEARCH_LIMIT = 10
const SIDEBAR_CUSTOMER_SCROLL_THRESHOLD_PX = 120
const CUSTOMER_SEARCH_SCROLL_THRESHOLD_PX = 96
const CUSTOMER_SEARCH_PULL_REFRESH_THRESHOLD_PX = 56
const CUSTOMER_SEARCH_PULL_MAX_PX = 78
const sidebarCustomers = ref<CustomerListVO[]>([])
const sidebarCustomersLoading = ref(false)
const sidebarCustomersPage = ref(1)
const sidebarCustomersTotal = ref(0)
const sidebarCustomersHasMore = ref(true)
const sidebarProducts = ref<ProductVO[]>([])
const sidebarProductsLoading = ref(false)
const sidebarProductsPage = ref(1)
const sidebarProductsTotal = ref(0)
const sidebarProductsHasMore = ref(true)
const sidebarEmployees = ref<AddressBookEmployee[]>([])
const sidebarEmployeesLoading = ref(false)
const sidebarEmployeesPage = ref(1)
const sidebarEmployeesTotal = ref(0)
const sidebarEmployeesHasMore = ref(true)
const sidebarRelations = ref<RelationVO[]>([])
const sidebarRelationsLoading = ref(false)
const sidebarRelationsPage = ref(1)
const sidebarRelationsTotal = ref(0)
const sidebarRelationsHasMore = ref(true)
const showUserMenu = ref(false)
const currentAppVersion = ref(DEFAULT_CURRENT_VERSION)
const checkingAppUpdate = ref(false)
const showAccountSettingsModal = ref(false)
const showCreateCustomer = ref(false)
const showCreateRelation = ref(false)
const showCreateProject = ref(false)
const customerSearchDialogVisible = ref(false)
const customerSearchKeyword = ref('')
const customerSearchCustomers = ref<CustomerListVO[]>([])
const customerSearchLoading = ref(false)
const customerSearchRefreshing = ref(false)
const customerSearchAppending = ref(false)
const customerSearchPage = ref(1)
const customerSearchTotal = ref(0)
const customerSearchHasMore = ref(true)
const customerSearchTouchStartY = ref<number | null>(null)
const customerSearchPullStartY = ref<number | null>(null)
const customerSearchPullDistance = ref(0)
const customerSearchInputRef = ref<HTMLInputElement | null>(null)
const customerSearchScrollRef = ref<HTMLElement | null>(null)
const mobileCustomerSearchKeyboardProxyRef = ref<HTMLInputElement | null>(null)

const globalSearchKeyword = ref('')
const globalSearchLoading = ref(false)
const showGlobalSearchDropdown = ref(false)
const globalSearchResults = ref<GlobalSearchResult[]>([])
const globalSearchTotal = ref(0)
const activeSearchResultIndex = ref(-1)
const searchPanelRef = ref<HTMLElement | null>(null)

let globalSearchTimer: ReturnType<typeof setTimeout> | null = null
let customerSearchTimer: ReturnType<typeof setTimeout> | null = null
let globalSearchRequestId = 0
let customerSearchRequestId = 0
let customerSearchFocusRequestId = 0
let removeChatComposerNarrowListener: (() => void) | null = null
let removeCustomerListRefreshListener: (() => void) | null = null
let removeCustomerSidebarRefreshListener: (() => void) | null = null
let removeProductSidebarRefreshListener: (() => void) | null = null
let removeRelationSidebarRefreshListener: (() => void) | null = null
let removeProjectSidebarRefreshListener: (() => void) | null = null
let removeMobileMainMenuOpenListener: (() => void) | null = null
let removeMobileMainMenuDragListener: (() => void) | null = null
let mobileDrawerDragSettleTimer: ReturnType<typeof setTimeout> | null = null
let mobileDrawerClosingTimer: ReturnType<typeof setTimeout> | null = null

type ChatComposerNarrowPayload = {
  narrow?: boolean
  width?: number
  minWidth?: number
}

type SidebarRefreshPayload = {
  preserveScroll?: boolean
}

type ProjectSidebarRefreshPayload = {
  alreadyRefreshed?: boolean
}

type MobileMainMenuDragPayload = {
  progress?: number
  phase?: 'move' | 'end'
  open?: boolean
}

type SidebarProjectItem = Pick<ProjectEntity, 'projectId' | 'name'>

function refreshSidebarCustomersFromEvent(payload?: SidebarRefreshPayload) {
  void fetchSidebarCustomers({ reset: true, preserveScroll: payload?.preserveScroll !== false })
}

function refreshSidebarRelationsFromEvent(payload?: SidebarRefreshPayload) {
  void fetchSidebarRelations({ reset: true, preserveScroll: payload?.preserveScroll !== false })
}

function refreshSidebarProductsFromEvent(payload?: SidebarRefreshPayload) {
  void fetchSidebarProducts({ reset: true, preserveScroll: payload?.preserveScroll !== false })
}

function refreshSidebarProjectsFromEvent(payload?: ProjectSidebarRefreshPayload) {
  if (payload?.alreadyRefreshed) return
  if (!showSidebarProjects.value) return
  void projectStore.ensureInitialized(true)
}

const chatComposerNarrow = ref(false)
const chatComposerAutoCollapseActive = ref(false)
const isChatRoute = computed(() => route.path.startsWith('/chat'))
const showMobileTopBar = computed(() => isMobile.value && !isChatRoute.value)

function getChatComposerWidthIfSidebarExpanded(width: number): number {
  return primarySidebarCollapsed.value ? width - PRIMARY_SIDEBAR_WIDTH_DELTA_PX : width
}

function restorePrimarySidebarForWideComposer(width: number, minWidth: number) {
  if (!chatComposerAutoCollapseActive.value || !primarySidebarCollapsed.value) return
  if (getChatComposerWidthIfSidebarExpanded(width) < minWidth) return
  runLayoutNarrowProgrammatic(() => {
    primarySidebarCollapsed.value = false
    chatComposerAutoCollapseActive.value = false
  })
}

function collapsePrimarySidebarForNarrowComposer() {
  if (isMobile.value || !chatComposerNarrow.value || primarySidebarCollapsed.value) return
  runLayoutNarrowProgrammatic(() => {
    chatComposerAutoCollapseActive.value = true
    primarySidebarCollapsed.value = true
  })
}

function handleChatComposerNarrowChange(payload?: ChatComposerNarrowPayload) {
  const width = Number(payload?.width || 0)
  const minWidth = Number(payload?.minWidth || CHAT_COMPOSER_MIN_WIDTH_PX)
  chatComposerNarrow.value = Boolean(payload?.narrow)

  if (width <= 0 && chatComposerAutoCollapseActive.value) {
    runLayoutNarrowProgrammatic(() => {
      primarySidebarCollapsed.value = false
      chatComposerAutoCollapseActive.value = false
    })
    return
  }

  if (isMobile.value) {
    chatComposerAutoCollapseActive.value = false
    return
  }

  if (chatComposerNarrow.value) {
    collapsePrimarySidebarForNarrowComposer()
    return
  }

  restorePrimarySidebarForWideComposer(width, minWidth)
  if (!primarySidebarCollapsed.value) {
    chatComposerAutoCollapseActive.value = false
  }
}

const showSidebarProjects = computed(() => userStore.hasPermission('task'))
const sidebarProjectCache = ref<SidebarProjectItem[]>(readStoredSidebarProjects())
const liveSidebarProjects = computed<SidebarProjectItem[]>(() =>
  projectStore.accessibleProjectSummaries
    .slice(0, 12)
    .map(project => ({
      projectId: project.projectId,
      name: project.name
    }))
)
const sidebarProjects = computed<SidebarProjectItem[]>(() =>
  liveSidebarProjects.value.length > 0 ? liveSidebarProjects.value : sidebarProjectCache.value
)
const showSidebarCustomers = computed(() => userStore.hasPermission('customer:view'))
const showSidebarProducts = computed(() => userStore.hasPermission('product:view'))
const showSidebarAddressBook = computed(() => userStore.hasPermission('addressBook:list'))
const showSidebarRelations = computed(() => true)

watch(showSidebarProjects, visible => {
  if (visible) {
    void projectStore.ensureInitialized()
  }
}, { immediate: true })

watch(showSidebarCustomers, visible => {
  if (visible && sidebarCustomers.value.length === 0) {
    void fetchSidebarCustomers({ reset: true })
  }
  if (!visible) {
    resetSidebarCustomers()
  }
})

watch(showSidebarProducts, visible => {
  if (visible && sidebarProducts.value.length === 0) {
    void fetchSidebarProducts({ reset: true })
  }
  if (!visible) {
    resetSidebarProducts()
  }
})

watch(showSidebarAddressBook, visible => {
  if (visible && sidebarEmployees.value.length === 0) {
    void fetchSidebarEmployees({ reset: true })
  }
  if (!visible) {
    resetSidebarEmployees()
  }
})

watch(showSidebarRelations, visible => {
  if (visible && sidebarRelations.value.length === 0) {
    void fetchSidebarRelations({ reset: true })
  }
  if (!visible) {
    resetSidebarRelations()
  }
})

type MainNavItem = {
  key: string
  icon: WkIconName
  materialIcon?: string
  label: string
  route: string
  permission: string
  action?: 'customerSearch'
  groupTitle?: string
  secondaryTitle?: string
  children?: SecondaryNavItem[]
}

type ConfigNavItem = {
  icon: WkIconName
  label: string
  route: string
  permission: string[]
  query?: Record<string, string>
}

type SecondaryNavItem = {
  key: string
  icon?: WkIconName
  label: string
  route: string
  query?: Record<string, string>
}

const allMainNavItems: MainNavItem[] = [
  { key: 'chat', icon: 'ai', label: 'AI 助手', route: '/chat', permission: 'chat', groupTitle: 'AI 助手' },
  { key: 'knowledge', icon: 'knowledge-1', label: '知识库', route: '/knowledge', permission: 'knowledge' },
  { key: 'customer-search', icon: 'search', label: '搜索客户', route: '', permission: 'customer:view', action: 'customerSearch' },
  { key: 'task', icon: 'task-1', label: '项目', route: '/project', permission: 'task' },
  { key: 'calendar', icon: 'event', label: '日程', route: '/calendar', permission: 'schedule' },
  { key: 'work-task', icon: 'task-1', label: '任务', route: '/task', permission: 'task:view' },
  { key: 'mail', icon: 'event', materialIcon: 'mail', label: '邮箱', route: '/mail', permission: 'mail:view' },
]

const allConfigNavItems: ConfigNavItem[] = [
  // 暂时隐藏数据同步入口，功能页面与路由先保留便于后续恢复。
  // { icon: 'import', label: '数据同步', route: '/sync', permission: ['config'] },
  // { icon: 'set', label: '系统设置', route: '/settings', permission: ['user', 'role', 'config', 'dept', 'customField'] },
  { icon: 'set', label: '系统设置', route: '/settings/team', permission: ['user', 'role', 'config', 'dept', 'customField'], query: { scope: 'profile' } },
]

const mainNavItems = computed(() =>
  allMainNavItems.filter(item => item.permission === 'chat' || item.key === 'address-book' || userStore.hasPermission(item.permission))
)

const pcMainNavItems = computed(() =>
  mainNavItems.value.filter(item => item.key !== 'chat' && (primarySidebarContentCollapsed.value || item.key !== 'task'))
)

type MainNavGroup = {
  title?: string
  items: MainNavItem[]
}

function groupMainNavItems(items: MainNavItem[]): MainNavGroup[] {
  const groups: MainNavGroup[] = []
  for (const item of items) {
    const title = item.groupTitle?.trim() || ''
    const last = groups[groups.length - 1]
    if (!last || (last.title || '') !== title) {
      groups.push({ title: title || undefined, items: [item] })
    } else {
      last.items.push(item)
    }
  }
  return groups
}

const pcMainNavGroups = computed(() => groupMainNavItems(pcMainNavItems.value))
const mobilePrimaryNavItems = computed<MainNavItem[]>(() =>
  mainNavItems.value
    .filter(item => item.key !== 'customer-search')
    .map(item => {
      if (item.key !== 'chat') return item
      return {
        ...item,
        icon: 'new-chat',
        label: '新对话',
        groupTitle: undefined,
      }
    })
)
const mobileMainNavGroups = computed(() => groupMainNavItems(mobilePrimaryNavItems.value))

const configNavItems = computed(() =>
  allConfigNavItems.filter(item => item.permission.some(permission => userStore.hasPermission(permission)))
)

const MOBILE_CONFIG_SECTION_VISIBLE = false
const showConfigSection = computed(() => configNavItems.value.length > 0)
const showMobileConfigSection = computed(() => MOBILE_CONFIG_SECTION_VISIBLE && showConfigSection.value)
const userDisplayName = computed(() => userStore.realname || userStore.username || '用户')
const userAccountName = computed(() => userStore.username || userStore.userInfo?.email || userStore.userInfo?.mobile || '用户')
const userAvatarInitials = computed(() => userDisplayName.value.trim().slice(0, 2).toUpperCase() || 'U')

function getConfiguredCurrentVersion(): string {
  const appVersion = import.meta.env.VITE_APP_VERSION
  return typeof appVersion === 'string' && appVersion.trim()
    ? appVersion.trim()
    : DEFAULT_CURRENT_VERSION
}

async function loadCurrentAppVersion() {
  try {
    currentAppVersion.value = getConfiguredCurrentVersion()
  } catch (error) {
    console.warn('获取当前版本号失败:', error)
    currentAppVersion.value = DEFAULT_CURRENT_VERSION
  }
}

async function loadCapacitorUpdateModule(): Promise<CapacitorUpdateModule | null> {
  if (!isNativeMobileRuntime()) return null

  try {
    return await import('@/utils/capacitorUpdate')
  } catch (error) {
    console.warn('Failed to load Capacitor update module:', error)
    return null
  }
}

async function handleManualUpdateCheck() {
  if (checkingAppUpdate.value) return

  checkingAppUpdate.value = true
  try {
    await loadCurrentAppVersion()
    const capacitorUpdateModule = await loadCapacitorUpdateModule()
    if (!capacitorUpdateModule) {
      ElMessage.info('当前环境暂不支持应用内更新检查')
      return
    }

    const result = await capacitorUpdateModule.checkForUpdates()

    if (result === 'none') {
      ElMessage.success('当前已是最新版本')
      return
    }

    if (result === 'unsupported') {
      ElMessage.info('当前环境暂不支持应用内更新检查')
      return
    }

    if (result === 'failed') {
      ElMessage.error('检查更新失败，请稍后重试')
    }
  } finally {
    checkingAppUpdate.value = false
  }
}

const showMobileDrawerNewChatButton = computed(() =>
  isMobile.value && drawerVisible.value && !chatDrawerOpen.value && !customerSearchDialogVisible.value && !showUserMenu.value
)

const showFloatingNewChatButton = computed(() => {
  if (chatDrawerOpen.value || mobileDrawerRendered.value) return false
  if (route.path.startsWith('/chat')) return false
  if (route.name === 'ProjectDetail') {
    const rawView = route.query.view
    const view = typeof rawView === 'string' ? rawView : Array.isArray(rawView) ? rawView[0] : ''
    const taskId = route.query.taskId
    const hasTaskConversation = typeof taskId === 'string'
      ? taskId.trim().length > 0
      : Array.isArray(taskId) && taskId.some(value => typeof value === 'string' && value.trim().length > 0)
    return !hasTaskConversation && view !== 'ai' && view !== 'task_ai'
  }
  return true
})

const customerSearchPullIndicatorHeight = computed(() =>
  customerSearchRefreshing.value
    ? 48
    : Math.ceil(customerSearchPullDistance.value)
)

const customerSearchPullLabel = computed(() => {
  if (customerSearchRefreshing.value) return '正在刷新'
  if (customerSearchPullDistance.value >= CUSTOMER_SEARCH_PULL_REFRESH_THRESHOLD_PX) return '松开刷新'
  return '下拉刷新'
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/customer')) return '/customer'
  if (path.startsWith('/product')) return '/product'
  if (path.startsWith('/scrm')) return '/scrm'
  if (path.startsWith('/address-book')) return '/address-book'
  if (path.startsWith('/project')) return '/project'
  if (path.startsWith('/relation')) return '/relation'
  if (path.startsWith('/sync')) return '/sync'
  if (path.startsWith('/settings')) return '/settings'
  return path
})

const showDesktopHeader = computed(() => {
  if (isMobile.value) return false
  return route.name !== 'ProjectDetail' && !route.path.startsWith('/knowledge') && !route.path.startsWith('/chat')
})

const selectedPrimaryKey = ref<string>('')

const selectedPrimaryItem = computed(() => mainNavItems.value.find(item => item.key === selectedPrimaryKey.value) || null)

const activeSecondaryItems = computed(() => selectedPrimaryItem.value?.children || [])

const showSecondaryPanel = computed(() => activeSecondaryItems.value.length > 0)

watch(showSecondaryPanel, open => {
  if (!isMobile.value && open) {
    runLayoutNarrowProgrammatic(() => {
      primarySidebarCollapsed.value = true
    })
  }
})

const secondaryTitle = computed(() => {
  if (!selectedPrimaryItem.value) return ''
  if (Object.prototype.hasOwnProperty.call(selectedPrimaryItem.value, 'secondaryTitle')) {
    return selectedPrimaryItem.value.secondaryTitle || ''
  }
  return `${selectedPrimaryItem.value.label} / 二级菜单`
})

const expandedMobilePrimaryKeys = ref<string[]>([])

watch(
  () => drawerVisible.value,
  isOpen => {
    if (!isOpen) return
    // 默认直接展开所有带二级菜单的一级菜单
    expandedMobilePrimaryKeys.value = mainNavItems.value.filter(item => item.children?.length).map(item => item.key)
  }
)

function isMobilePrimaryExpanded(key: string) {
  return expandedMobilePrimaryKeys.value.includes(key)
}

function toggleMobilePrimaryExpanded(key: string) {
  if (isMobilePrimaryExpanded(key)) {
    expandedMobilePrimaryKeys.value = expandedMobilePrimaryKeys.value.filter(k => k !== key)
    return
  }
  expandedMobilePrimaryKeys.value = [...expandedMobilePrimaryKeys.value, key]
}

function handleMobilePrimaryNavClick(item: MainNavItem) {
  if (item.key === 'chat') {
    closeMobileDrawer()
    void handleNewSession()
    return
  }
  if (item.action === 'customerSearch') {
    closeMobileDrawer()
    openMobileCustomerSearchDialog()
    return
  }
  if (item.children?.length) {
    toggleMobilePrimaryExpanded(item.key)
    return
  }
  mobileNavigate(item.route)
}

function routeQueryString(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value)) return typeof value[0] === 'string' ? value[0] : ''
  return ''
}

function externalProviderName(provider: string): string {
  if (provider === 'wechat') return '微信'
  if (provider === 'google') return 'Google'
  if (provider === 'outlook') return 'Microsoft'
  return '第三方'
}

async function clearExternalAuthReturnQuery() {
  const query = { ...route.query }
  delete query.externalBind
  delete query.externalAuthError
  delete query.provider
  delete query.message
  await router.replace({ path: route.path, query, hash: route.hash })
}

async function handleExternalAuthBindingReturn() {
  const bindResult = routeQueryString(route.query.externalBind)
  const authError = routeQueryString(route.query.externalAuthError)
  if (!bindResult && !authError) return

  const provider = routeQueryString(route.query.provider)
  const providerName = externalProviderName(provider)
  const message = routeQueryString(route.query.message)
  if (bindResult === 'success') {
    ElMessage.success(`${providerName}绑定成功`)
  } else {
    ElMessage.error(message ? `${providerName}绑定失败：${message}` : `${providerName}绑定失败`)
  }
  showAccountSettingsModal.value = true
  await clearExternalAuthReturnQuery()
}

watch(
  () => route.fullPath,
  () => {
    showUserMenu.value = false
    closeGlobalSearchDropdown()
    if (selectedPrimaryKey.value && !selectedPrimaryItem.value) {
      selectedPrimaryKey.value = ''
    }
    const scopeRaw = route.query.scope
    const scope = Array.isArray(scopeRaw) ? scopeRaw[0] : scopeRaw
    if (route.path === '/settings/team' && scope === 'profile') {
      selectedPrimaryKey.value = ''
    }
    void handleExternalAuthBindingReturn()
  }
)

watch(
  () => drawerVisible.value,
  isOpen => {
    if (isOpen) {
      if (!mobileDrawerDragActive.value && !mobileDrawerDragSettling.value) {
        resetMobileDrawerDragPreview()
      }
      return
    }
    if (!isOpen) {
      showUserMenu.value = false
      recentChatSessionsMoreVisible.value = false
      resetGlobalMobileMainMenuSwipe()
      resetMobileDrawerSwipe()
      if (mobileDrawerDragSettling.value || mobileDrawerClosing.value) return
      resetMobileDrawerDragPreview()
    }
  }
)

watch(showUserMenu, open => {
  if (open && !isMobile.value && primarySidebarCollapsed.value) {
    runLayoutNarrowProgrammatic(() => {
      primarySidebarCollapsed.value = false
    })
  }
})

watch(
  () => userStore.userInfo,
  () => {
    hydrateSidebarModuleOrderFromUserInfo()
  },
  { immediate: true }
)

watch(primarySidebarCollapsed, collapsed => {
  if (layoutNarrowProgrammatic.value) return
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.primaryCollapsed, collapsed)
})

watch(recentChatSessionsExpanded, expanded => {
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.recentChatExpanded, expanded)
})

watch(sidebarProjectsExpanded, expanded => {
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarProjectsExpanded, expanded)
})

watch(
  () => [projectStore.initialized, projectStore.loading, liveSidebarProjects.value] as const,
  ([initialized, loading, projects]) => {
    if (!initialized || loading) return
    sidebarProjectCache.value = projects
    writeStoredSidebarProjects(projects)
  },
  { deep: true, immediate: true }
)

watch(sidebarCustomersExpanded, expanded => {
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarCustomersExpanded, expanded)
})

watch(sidebarProductsExpanded, expanded => {
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarProductsExpanded, expanded)
})

watch(sidebarAddressBookExpanded, expanded => {
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarAddressBookExpanded, expanded)
})

watch(sidebarRelationsExpanded, expanded => {
  writeStoredBoolean(SIDEBAR_STORAGE_KEYS.sidebarRelationsExpanded, expanded)
})

watch(primarySidebarCollapsed, collapsed => {
  if (!collapsed) collapsePrimarySidebarForNarrowComposer()
})

watch(
  () => [isMobile.value, mainContentColumnWidth.value, mainContentColumnMeasured.value, primarySidebarCollapsed.value] as const,
  ([mobile, w, measured, collapsed]) => {
    if (mobile) {
      if (layoutNarrowAutoCollapseActive.value) {
        runLayoutNarrowProgrammatic(() => {
          primarySidebarCollapsed.value = !layoutNarrowSavedExpanded.value
          layoutNarrowAutoCollapseActive.value = false
        })
      }
      return
    }
    if (!measured) return

    const mainIfExpanded = mainContentWidthAsIfSidebarExpanded(collapsed, w)

    if (!collapsed && mainIfExpanded < PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX) {
      runLayoutNarrowProgrammatic(() => {
        layoutNarrowSavedExpanded.value = true
        primarySidebarCollapsed.value = true
        layoutNarrowAutoCollapseActive.value = true
      })
      return
    }

    if (layoutNarrowAutoCollapseActive.value && collapsed && mainIfExpanded >= PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX) {
      runLayoutNarrowProgrammatic(() => {
        primarySidebarCollapsed.value = !layoutNarrowSavedExpanded.value
        layoutNarrowAutoCollapseActive.value = false
        if (showSecondaryPanel.value) {
          primarySidebarCollapsed.value = true
        }
      })
    }
  },
  { immediate: true }
)

watch(primarySidebarCollapsed, collapsed => {
  if (primarySidebarTransitionTimer) {
    clearTimeout(primarySidebarTransitionTimer)
    primarySidebarTransitionTimer = null
  }

  primarySidebarTransitioning.value = true

  if (!collapsed) {
    primarySidebarContentCollapsed.value = false
  }

  primarySidebarTransitionTimer = setTimeout(() => {
    primarySidebarTransitionTimer = null
    primarySidebarContentCollapsed.value = collapsed
    primarySidebarTransitioning.value = false
    queueMicrotask(() => updatePrimaryNavScrollbar())
  }, PRIMARY_SIDEBAR_TRANSITION_MS)
})

watch(primarySidebarCollapsed, collapsed => {
  collapsedSidebarAsideHovered.value = false
  if (layoutNarrowProgrammatic.value) return
  if (isMobile.value || !layoutNarrowAutoCollapseActive.value) return
  if (!mainContentColumnMeasured.value) return
  const w = mainContentColumnWidth.value
  const mainIfExpanded = mainContentWidthAsIfSidebarExpanded(collapsed, w)
  if (mainIfExpanded >= PC_PRIMARY_SIDEBAR_AUTO_COLLAPSE_BELOW_PX) return
  layoutNarrowSavedExpanded.value = !collapsed
})

let primaryNavResizeObserver: ResizeObserver | null = null
let mainContentColumnResizeObserver: ResizeObserver | null = null

function updatePrimaryNavScrollbar() {
  const el = primaryNavRef.value
  if (!el) {
    primaryNavHasScrollbar.value = false
    primaryNavScrolling.value = false
    return
  }
  // +1 to avoid off-by-one from subpixel layout
  const hasScrollbar = el.scrollHeight > el.clientHeight + 1
  primaryNavHasScrollbar.value = hasScrollbar
  if (!hasScrollbar) primaryNavScrolling.value = false
}

onMounted(() => {
  enterpriseStore.loadConfig()
  void chatStore.fetchSessions()
  void loadCurrentAppVersion()
  void handleExternalAuthBindingReturn()
  if (showSidebarCustomers.value) {
    void fetchSidebarCustomers({ reset: true })
  }
  if (showSidebarProducts.value) {
    void fetchSidebarProducts({ reset: true })
  }
  if (showSidebarAddressBook.value) {
    void fetchSidebarEmployees({ reset: true })
  }
  if (showSidebarRelations.value) {
    void fetchSidebarRelations({ reset: true })
  }
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('touchstart', handleGlobalMobileMainMenuTouchStart, { passive: true })
  document.addEventListener('touchmove', handleGlobalMobileMainMenuTouchMove, { passive: false })
  document.addEventListener('touchend', handleGlobalMobileMainMenuTouchEnd, { passive: true })
  document.addEventListener('touchcancel', handleGlobalMobileMainMenuTouchCancel, { passive: true })
  removeChatComposerNarrowListener = appEvents.on<ChatComposerNarrowPayload>(
    APP_EVENT.CHAT_COMPOSER_NARROW_CHANGE,
    handleChatComposerNarrowChange
  )
  removeCustomerListRefreshListener = appEvents.on<SidebarRefreshPayload>(
    APP_EVENT.CUSTOMER_LIST_REFRESH,
    refreshSidebarCustomersFromEvent
  )
  removeCustomerSidebarRefreshListener = appEvents.on<SidebarRefreshPayload>(
    APP_EVENT.CUSTOMER_SIDEBAR_REFRESH,
    refreshSidebarCustomersFromEvent
  )
  removeProductSidebarRefreshListener = appEvents.on<SidebarRefreshPayload>(
    APP_EVENT.PRODUCT_SIDEBAR_REFRESH,
    refreshSidebarProductsFromEvent
  )
  removeRelationSidebarRefreshListener = appEvents.on<SidebarRefreshPayload>(
    APP_EVENT.RELATION_SIDEBAR_REFRESH,
    refreshSidebarRelationsFromEvent
  )
  removeProjectSidebarRefreshListener = appEvents.on<ProjectSidebarRefreshPayload>(
    APP_EVENT.PROJECT_SIDEBAR_REFRESH,
    refreshSidebarProjectsFromEvent
  )
  removeMobileMainMenuOpenListener = appEvents.on(
    APP_EVENT.MOBILE_MAIN_MENU_OPEN,
    openMobileDrawer
  )
  removeMobileMainMenuDragListener = appEvents.on<MobileMainMenuDragPayload>(
    APP_EVENT.MOBILE_MAIN_MENU_DRAG,
    handleMobileMainMenuDrag
  )

  if (typeof ResizeObserver !== 'undefined') {
    primaryNavResizeObserver = new ResizeObserver(() => updatePrimaryNavScrollbar())
    if (primaryNavRef.value) primaryNavResizeObserver.observe(primaryNavRef.value)
  }
  // Initial measure
  queueMicrotask(() => updatePrimaryNavScrollbar())
})

watch(
  mainContentColumnRef,
  el => {
    if (typeof ResizeObserver === 'undefined') return
    if (!mainContentColumnResizeObserver) {
      mainContentColumnResizeObserver = new ResizeObserver(() => updateMainContentColumnWidth())
    }
    mainContentColumnResizeObserver.disconnect()
    if (el) {
      mainContentColumnResizeObserver.observe(el)
      queueMicrotask(() => updateMainContentColumnWidth())
    } else {
      mainContentColumnWidth.value = 0
      mainContentColumnMeasured.value = false
    }
  },
  { flush: 'post', immediate: true }
)

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('touchstart', handleGlobalMobileMainMenuTouchStart)
  document.removeEventListener('touchmove', handleGlobalMobileMainMenuTouchMove)
  document.removeEventListener('touchend', handleGlobalMobileMainMenuTouchEnd)
  document.removeEventListener('touchcancel', handleGlobalMobileMainMenuTouchCancel)
  removeChatComposerNarrowListener?.()
  removeChatComposerNarrowListener = null
  removeCustomerListRefreshListener?.()
  removeCustomerListRefreshListener = null
  removeCustomerSidebarRefreshListener?.()
  removeCustomerSidebarRefreshListener = null
  removeProductSidebarRefreshListener?.()
  removeProductSidebarRefreshListener = null
  removeRelationSidebarRefreshListener?.()
  removeRelationSidebarRefreshListener = null
  removeProjectSidebarRefreshListener?.()
  removeProjectSidebarRefreshListener = null
  removeMobileMainMenuOpenListener?.()
  removeMobileMainMenuOpenListener = null
  removeMobileMainMenuDragListener?.()
  removeMobileMainMenuDragListener = null
  if (mainContentColumnResizeObserver) {
    mainContentColumnResizeObserver.disconnect()
    mainContentColumnResizeObserver = null
  }
  if (primaryNavResizeObserver) {
    primaryNavResizeObserver.disconnect()
    primaryNavResizeObserver = null
  }
  if (primaryNavScrollEndTimer) {
    clearTimeout(primaryNavScrollEndTimer)
    primaryNavScrollEndTimer = null
  }
  if (primarySidebarTransitionTimer) {
    clearTimeout(primarySidebarTransitionTimer)
    primarySidebarTransitionTimer = null
  }
  clearMobileDrawerDragSettleTimer()
  clearMobileDrawerClosingTimer()
  if (globalSearchTimer) {
    clearTimeout(globalSearchTimer)
    globalSearchTimer = null
  }
  if (customerSearchTimer) {
    clearTimeout(customerSearchTimer)
    customerSearchTimer = null
  }
})

watch(
  () => [
    isMobile.value,
    primarySidebarCollapsed.value,
    recentChatSessionsExpanded.value,
    sidebarCustomersExpanded.value,
    sidebarProductsExpanded.value,
    sidebarAddressBookExpanded.value,
    sidebarRelationsExpanded.value,
    pcMainNavGroups.value.length,
    chatStore.sessions.length,
    chatStore.sessionsLoading,
    sidebarProjects.value.length,
    projectStore.loading,
    sidebarCustomers.value.length,
    sidebarCustomersLoading.value,
    sidebarProducts.value.length,
    sidebarProductsLoading.value,
    sidebarEmployees.value.length,
    sidebarEmployeesLoading.value,
    sidebarRelations.value.length,
    sidebarRelationsLoading.value,
    sidebarSortMode.value,
    sidebarModuleOrder.value.join('|'),
    sidebarDraftModuleOrder.value.join('|'),
  ],
  () => {
    queueMicrotask(() => updatePrimaryNavScrollbar())
    queueMicrotask(() => maybeLoadMoreSidebarObjects())
  },
  { flush: 'post' }
)

function isActive(routePath: string, itemQuery?: Record<string, string>) {
  const pathMatches = routePath.startsWith('/settings')
    ? route.path.startsWith('/settings')
    : activeMenu.value === routePath
  if (!pathMatches) return false

  const expectedScope = itemQuery?.scope
  const currentScope = typeof route.query.scope === 'string' ? route.query.scope : ''
  if (expectedScope !== undefined) {
    return currentScope === expectedScope
  }
  if (routePath.startsWith('/settings') && currentScope) {
    return false
  }
  return true
}

function isPrimarySelected(item: MainNavItem) {
  return selectedPrimaryKey.value === item.key
}

function isPrimaryActive(item: MainNavItem) {
  if (item.action === 'customerSearch') return customerSearchDialogVisible.value
  if (isPrimarySelected(item)) return true
  if (item.children?.some(child => isActive(child.route, child.query))) return true
  return isActive(item.route)
}

function handlePrimaryNavClick(item: MainNavItem) {
  if (item.action === 'customerSearch') {
    selectedPrimaryKey.value = ''
    openCustomerSearchDialog()
    return
  }
  if (item.children?.length) {
    selectedPrimaryKey.value = item.key
    const firstChild = item.children[0]
    if (firstChild?.route) {
      navigateTo(firstChild.route, firstChild.query)
    }
    return
  }
  selectedPrimaryKey.value = ''
  navigateTo(item.route)
}

type ChatSessionGroups = {
  today: ChatSession[]
  yesterday: ChatSession[]
  earlier: ChatSession[]
}

function groupSessionsByTime(sessions: ChatSession[]): ChatSessionGroups {
  const today: ChatSession[] = []
  const yesterday: ChatSession[] = []
  const earlier: ChatSession[] = []
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const yesterdayStart = todayStart - 86400000

  for (const session of sessions) {
    const time = new Date(session.updateTime || session.createTime).getTime()
    if (time >= todayStart) {
      today.push(session)
    } else if (time >= yesterdayStart) {
      yesterday.push(session)
    } else {
      earlier.push(session)
    }
  }

  return { today, yesterday, earlier }
}

function isUnboundChatSession(session: ChatSession): boolean {
  return !String(session.customerId || '').trim()
    && !String(session.employeeId || '').trim()
    && !String(session.relationId || '').trim()
    && !String(session.productId || '').trim()
    && !chatStore.isProjectContextSession(session)
}

const sidebarVisibleChatSessions = computed(() =>
  chatStore.sessions.filter(isUnboundChatSession)
)

const limitedRecentChatSessions = computed(() =>
  sidebarVisibleChatSessions.value.slice(0, RECENT_CHAT_SESSION_LIMIT)
)

const mobileRecentChatSessions = computed(() =>
  sidebarVisibleChatSessions.value.slice(0, MOBILE_RECENT_CHAT_SESSION_LIMIT)
)

const filteredHistorySessions = computed(() => {
  const keyword = recentHistoryKeyword.value.trim().toLowerCase()
  if (!keyword) return sidebarVisibleChatSessions.value
  return sidebarVisibleChatSessions.value.filter(session => {
    const title = (session.title || '').toLowerCase()
    const customerName = (session.customerName || '').toLowerCase()
    const relationName = (session.relationName || '').toLowerCase()
    const productName = (session.productName || '').toLowerCase()
    return title.includes(keyword) || customerName.includes(keyword) || relationName.includes(keyword) || productName.includes(keyword)
  })
})

function resetSidebarCustomers(options: { keepItems?: boolean } = {}) {
  if (!options.keepItems) {
    sidebarCustomers.value = []
  }
  sidebarCustomersPage.value = 1
  sidebarCustomersTotal.value = 0
  sidebarCustomersHasMore.value = true
}

function resetSidebarProducts(options: { keepItems?: boolean } = {}) {
  if (!options.keepItems) {
    sidebarProducts.value = []
  }
  sidebarProductsPage.value = 1
  sidebarProductsTotal.value = 0
  sidebarProductsHasMore.value = true
}

function resetSidebarEmployees(options: { keepItems?: boolean } = {}) {
  if (!options.keepItems) {
    sidebarEmployees.value = []
  }
  sidebarEmployeesPage.value = 1
  sidebarEmployeesTotal.value = 0
  sidebarEmployeesHasMore.value = true
}

function resetSidebarRelations(options: { keepItems?: boolean } = {}) {
  if (!options.keepItems) {
    sidebarRelations.value = []
  }
  sidebarRelationsPage.value = 1
  sidebarRelationsTotal.value = 0
  sidebarRelationsHasMore.value = true
}

function shouldLoadMoreSidebarCustomers(): boolean {
  const el = primaryNavRef.value
  if (!el) return false
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  return distanceToBottom <= SIDEBAR_CUSTOMER_SCROLL_THRESHOLD_PX
}

function maybeLoadMoreSidebarObjects() {
  maybeLoadMoreSidebarCustomers()
  maybeLoadMoreSidebarProducts()
  maybeLoadMoreSidebarEmployees()
  maybeLoadMoreSidebarRelations()
}

function maybeLoadMoreSidebarCustomers() {
  if (!showSidebarCustomers.value || !sidebarCustomersExpanded.value || primarySidebarContentCollapsed.value) return
  if (sidebarCustomersLoading.value || !sidebarCustomersHasMore.value) return
  if (!shouldLoadMoreSidebarCustomers()) return
  void fetchSidebarCustomers()
}

function maybeLoadMoreSidebarProducts() {
  if (!showSidebarProducts.value || !sidebarProductsExpanded.value || primarySidebarContentCollapsed.value) return
  if (sidebarProductsLoading.value || !sidebarProductsHasMore.value) return
  if (!shouldLoadMoreSidebarCustomers()) return
  void fetchSidebarProducts()
}

function maybeLoadMoreSidebarEmployees() {
  if (!showSidebarAddressBook.value || !sidebarAddressBookExpanded.value || primarySidebarContentCollapsed.value) return
  if (sidebarEmployeesLoading.value || !sidebarEmployeesHasMore.value) return
  if (!shouldLoadMoreSidebarCustomers()) return
  void fetchSidebarEmployees()
}

function maybeLoadMoreSidebarRelations() {
  if (!showSidebarRelations.value || !sidebarRelationsExpanded.value || primarySidebarContentCollapsed.value) return
  if (sidebarRelationsLoading.value || !sidebarRelationsHasMore.value) return
  if (!shouldLoadMoreSidebarCustomers()) return
  void fetchSidebarRelations()
}

function loadMoreSidebarCustomers() {
  void fetchSidebarCustomers()
}

function loadMoreSidebarProducts() {
  void fetchSidebarProducts()
}

function loadMoreSidebarEmployees() {
  void fetchSidebarEmployees()
}

function loadMoreSidebarRelations() {
  void fetchSidebarRelations()
}

async function fetchSidebarCustomers(options: { reset?: boolean; preserveScroll?: boolean } = {}) {
  if (sidebarCustomersLoading.value) return
  const preservedScrollTop = options.preserveScroll ? getPrimaryNavScrollTop() : null
  if (!showSidebarCustomers.value) {
    resetSidebarCustomers()
    if (preservedScrollTop != null) {
      void restorePrimaryNavScrollTop(preservedScrollTop)
    }
    return
  }
  if (options.reset) {
    resetSidebarCustomers({ keepItems: options.preserveScroll })
  }
  if (!sidebarCustomersHasMore.value) return

  const page = sidebarCustomersPage.value
  sidebarCustomersLoading.value = true
  try {
    const result = await queryCustomerList({
      page,
      limit: SIDEBAR_CUSTOMER_LIMIT,
      keyword: sidebarCustomerKeyword.value.trim() || undefined
    })
    const nextCustomers = result.list || []
    if (page === 1) {
      sidebarCustomers.value = nextCustomers
    } else {
      const existingIds = new Set(sidebarCustomers.value.map(customer => String(customer.customerId)))
      sidebarCustomers.value = [
        ...sidebarCustomers.value,
        ...nextCustomers.filter(customer => !existingIds.has(String(customer.customerId))),
      ]
    }
    sidebarCustomersTotal.value = result.totalRow || sidebarCustomers.value.length
    sidebarCustomersPage.value = page + 1
    sidebarCustomersHasMore.value = nextCustomers.length > 0 && sidebarCustomers.value.length < sidebarCustomersTotal.value
  } catch (error) {
    console.error('Load sidebar customers failed:', error)
    sidebarCustomersHasMore.value = false
    if (page === 1) {
      sidebarCustomers.value = []
    }
  } finally {
    sidebarCustomersLoading.value = false
    if (preservedScrollTop != null) {
      await restorePrimaryNavScrollTop(preservedScrollTop)
    }
    queueMicrotask(() => {
      updatePrimaryNavScrollbar()
      maybeLoadMoreSidebarCustomers()
    })
  }
}

async function fetchSidebarProducts(options: { reset?: boolean; preserveScroll?: boolean } = {}) {
  if (sidebarProductsLoading.value) return
  const preservedScrollTop = options.preserveScroll ? getPrimaryNavScrollTop() : null
  if (!showSidebarProducts.value) {
    resetSidebarProducts()
    if (preservedScrollTop != null) {
      void restorePrimaryNavScrollTop(preservedScrollTop)
    }
    return
  }
  if (options.reset) {
    resetSidebarProducts({ keepItems: options.preserveScroll })
  }
  if (!sidebarProductsHasMore.value) return

  const page = sidebarProductsPage.value
  sidebarProductsLoading.value = true
  try {
    const result = await queryProductList({
      page,
      limit: SIDEBAR_PRODUCT_LIMIT
    })
    const nextProducts = result.list || []
    if (page === 1) {
      sidebarProducts.value = nextProducts
    } else {
      const existingIds = new Set(sidebarProducts.value.map(product => String(product.productId)))
      sidebarProducts.value = [
        ...sidebarProducts.value,
        ...nextProducts.filter(product => !existingIds.has(String(product.productId))),
      ]
    }
    sidebarProductsTotal.value = result.totalRow || sidebarProducts.value.length
    sidebarProductsPage.value = page + 1
    sidebarProductsHasMore.value = nextProducts.length > 0 && sidebarProducts.value.length < sidebarProductsTotal.value
  } catch (error) {
    console.error('Load sidebar products failed:', error)
    sidebarProductsHasMore.value = false
    if (page === 1) {
      sidebarProducts.value = []
    }
  } finally {
    sidebarProductsLoading.value = false
    if (preservedScrollTop != null) {
      await restorePrimaryNavScrollTop(preservedScrollTop)
    }
    queueMicrotask(() => {
      updatePrimaryNavScrollbar()
      maybeLoadMoreSidebarObjects()
    })
  }
}

async function fetchSidebarEmployees(options: { reset?: boolean; preserveScroll?: boolean } = {}) {
  if (sidebarEmployeesLoading.value) return
  const preservedScrollTop = options.preserveScroll ? getPrimaryNavScrollTop() : null
  if (!showSidebarAddressBook.value) {
    resetSidebarEmployees()
    if (preservedScrollTop != null) {
      void restorePrimaryNavScrollTop(preservedScrollTop)
    }
    return
  }
  if (options.reset) {
    resetSidebarEmployees({ keepItems: options.preserveScroll })
  }
  if (!sidebarEmployeesHasMore.value) return

  const page = sidebarEmployeesPage.value
  sidebarEmployeesLoading.value = true
  try {
    const result = await queryAddressBook({
      page,
      limit: SIDEBAR_EMPLOYEE_LIMIT
    })
    const nextEmployees = result.list || []
    if (page === 1) {
      sidebarEmployees.value = nextEmployees
    } else {
      const existingIds = new Set(sidebarEmployees.value.map(employee => String(employee.userId)))
      sidebarEmployees.value = [
        ...sidebarEmployees.value,
        ...nextEmployees.filter(employee => !existingIds.has(String(employee.userId))),
      ]
    }
    sidebarEmployeesTotal.value = result.totalRow || sidebarEmployees.value.length
    sidebarEmployeesPage.value = page + 1
    sidebarEmployeesHasMore.value = nextEmployees.length > 0 && sidebarEmployees.value.length < sidebarEmployeesTotal.value
  } catch (error) {
    console.error('Load sidebar employees failed:', error)
    sidebarEmployeesHasMore.value = false
    if (page === 1) {
      sidebarEmployees.value = []
    }
  } finally {
    sidebarEmployeesLoading.value = false
    if (preservedScrollTop != null) {
      await restorePrimaryNavScrollTop(preservedScrollTop)
    }
    queueMicrotask(() => {
      updatePrimaryNavScrollbar()
      maybeLoadMoreSidebarObjects()
    })
  }
}

async function fetchSidebarRelations(options: { reset?: boolean; preserveScroll?: boolean } = {}) {
  if (sidebarRelationsLoading.value) return
  const preservedScrollTop = options.preserveScroll ? getPrimaryNavScrollTop() : null
  if (!showSidebarRelations.value) {
    resetSidebarRelations()
    if (preservedScrollTop != null) {
      void restorePrimaryNavScrollTop(preservedScrollTop)
    }
    return
  }
  if (options.reset) {
    resetSidebarRelations({ keepItems: options.preserveScroll })
  }
  if (!sidebarRelationsHasMore.value) return

  const page = sidebarRelationsPage.value
  sidebarRelationsLoading.value = true
  try {
    const result = await queryRelationList({
      page,
      limit: SIDEBAR_RELATION_LIMIT
    })
    const nextRelations = result.list || []
    if (page === 1) {
      sidebarRelations.value = nextRelations
    } else {
      const existingIds = new Set(sidebarRelations.value.map(relation => String(relation.relationId)))
      sidebarRelations.value = [
        ...sidebarRelations.value,
        ...nextRelations.filter(relation => !existingIds.has(String(relation.relationId))),
      ]
    }
    sidebarRelationsTotal.value = result.totalRow || sidebarRelations.value.length
    sidebarRelationsPage.value = page + 1
    sidebarRelationsHasMore.value = nextRelations.length > 0 && sidebarRelations.value.length < sidebarRelationsTotal.value
  } catch (error) {
    console.error('Load sidebar relations failed:', error)
    sidebarRelationsHasMore.value = false
    if (page === 1) {
      sidebarRelations.value = []
    }
  } finally {
    sidebarRelationsLoading.value = false
    if (preservedScrollTop != null) {
      await restorePrimaryNavScrollTop(preservedScrollTop)
    }
    queueMicrotask(() => {
      updatePrimaryNavScrollbar()
      maybeLoadMoreSidebarObjects()
    })
  }
}

const pinnedHistorySessions = computed(() =>
  filteredHistorySessions.value.filter(session => Boolean(session.pinned))
)

const unpinnedHistorySessions = computed(() =>
  filteredHistorySessions.value.filter(session => !session.pinned)
)

const groupedHistoryChatSessions = computed(() => groupSessionsByTime(unpinnedHistorySessions.value))

const historySessionGroups = computed(() => [
  { key: 'pinned', label: '置顶', sessions: pinnedHistorySessions.value },
  { key: 'today', label: '今天', sessions: groupedHistoryChatSessions.value.today },
  { key: 'yesterday', label: '昨天', sessions: groupedHistoryChatSessions.value.yesterday },
  { key: 'earlier', label: '更早', sessions: groupedHistoryChatSessions.value.earlier },
])

async function handleNewSession(appCode = 'general') {
  selectedPrimaryKey.value = ''
  await router.push('/chat')
  // HMR can keep an old store instance; fall back to legacy behavior if method missing.
  const api = chatStore as unknown as {
    beginNewSessionDraft?: (title?: string, agentId?: string, customerId?: string, appCode?: string) => void
    startNewSessionIfNeeded?: (title?: string, agentId?: string, customerId?: string, appCode?: string) => Promise<unknown>
    clearMessages?: () => void
    setSelectedAppCode?: (appCode: string) => void
  }
  if (api.beginNewSessionDraft) {
    api.beginNewSessionDraft('新对话', undefined, undefined, appCode)
  } else if (api.startNewSessionIfNeeded) {
    await api.startNewSessionIfNeeded('新对话', undefined, undefined, appCode)
  } else {
    api.clearMessages?.()
    api.setSelectedAppCode?.(appCode)
  }
  if (!isMobile.value) {
    chatStore.requestComposerFocus()
  }
}

async function handleFloatingNewChat() {
  if (isMobile.value) {
    closeMobileDrawer()
  }
  if (route.name === 'ProjectDetail') {
    const projectId = String(route.params.id || '').trim()
    if (projectId) {
      selectedPrimaryKey.value = ''
      await router.push({ name: 'ProjectDetail', params: { id: projectId }, query: { view: 'ai' } })
      return
    }
  }
  if (route.name === 'CustomerDetail') {
    const customerId = String(route.params.id || '').trim()
    if (customerId) {
      selectedPrimaryKey.value = ''
      await router.push({ path: '/chat', query: { customerId } })
      if (!isMobile.value) {
        chatStore.requestComposerFocus()
      }
      return
    }
  }
  if (route.name === 'CustomerList') {
    await handleNewSession('crm')
    return
  }
  if (route.name === 'ProjectList') {
    await handleNewSession('project')
    return
  }
  await handleNewSession()
}

async function handleSelectSession(sessionId: string, options: { focusComposer?: boolean } = {}) {
  selectedPrimaryKey.value = ''
  await router.push('/chat')
  await chatStore.selectSession(sessionId)
  if (options.focusComposer !== false) {
    chatStore.requestComposerFocus()
  }
}

async function handleSelectSessionFromMore(sessionId: string) {
  recentChatSessionsMoreVisible.value = false
  await handleSelectSession(sessionId)
}

async function handleMobileSelectSession(sessionId: string) {
  closeMobileDrawer()
  await handleSelectSession(sessionId, { focusComposer: false })
}

async function handleMobileSelectSessionFromMore(sessionId: string) {
  closeMobileDrawer()
  await handleSelectSession(sessionId, { focusComposer: false })
}

function isSessionActive(sessionId: string): boolean {
  return route.path.startsWith('/chat') && chatStore.currentSessionId === sessionId
}

function isCustomerActive(customerId: string): boolean {
  const raw = route.query.customerId
  const current = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  return route.path.startsWith('/chat') && String(current) === String(customerId)
}

function isProductActive(productId: string): boolean {
  const raw = route.query.productId
  const current = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  return route.path.startsWith('/chat') && String(current) === String(productId)
}

function isProjectActive(projectId: string): boolean {
  return route.path.startsWith('/project') && String(route.params.id || '') === String(projectId)
}

async function handleStartProjectConversation(projectId: string) {
  selectedPrimaryKey.value = ''
  await router.push({ name: 'ProjectDetail', params: { id: projectId }, query: { view: 'ai' } })
}

async function handleMobileStartProjectConversation(projectId: string) {
  closeMobileDrawer()
  await handleStartProjectConversation(projectId)
}

function isEmployeeActive(employeeId: string): boolean {
  const raw = route.query.employeeId
  const current = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  return route.path.startsWith('/chat') && String(current) === String(employeeId)
}

function isRelationActive(relationId: string): boolean {
  const raw = route.query.relationId
  const current = typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
  return route.path.startsWith('/chat') && String(current) === String(relationId)
}

async function handleSelectCustomerChat(customer: CustomerListVO) {
  selectedPrimaryKey.value = ''
  await router.push({ path: '/chat', query: { customerId: customer.customerId } })
  if (!isMobile.value) {
    chatStore.requestComposerFocus()
  }
}

async function handleMobileSelectCustomerChat(customer: CustomerListVO) {
  closeMobileDrawer()
  await handleSelectCustomerChat(customer)
}

async function handleSelectProductChat(product: ProductVO) {
  selectedPrimaryKey.value = ''
  const productId = String(product.productId)
  await chatStore.openProductChat({
    productId,
    productName: product.productName
  })
  await router.push({ path: '/chat', query: { productId } })
  if (!isMobile.value) {
    chatStore.requestComposerFocus()
  }
}

async function handleMobileSelectProductChat(product: ProductVO) {
  closeMobileDrawer()
  await handleSelectProductChat(product)
}

async function handleSelectEmployeeChat(employee: AddressBookEmployee) {
  selectedPrimaryKey.value = ''
  await router.push({ path: '/chat', query: { employeeId: employee.userId } })
  if (!isMobile.value) {
    chatStore.requestComposerFocus()
  }
}

async function handleMobileSelectEmployeeChat(employee: AddressBookEmployee) {
  closeMobileDrawer()
  await handleSelectEmployeeChat(employee)
}

async function handleSelectRelationChat(relation: RelationVO) {
  selectedPrimaryKey.value = ''
  await router.push({ path: '/chat', query: { relationId: relation.relationId } })
  if (!isMobile.value) {
    chatStore.requestComposerFocus()
  }
}

async function handleMobileSelectRelationChat(relation: RelationVO) {
  closeMobileDrawer()
  await handleSelectRelationChat(relation)
}

function employeeName(employee: AddressBookEmployee): string {
  return employee.realname || '未命名员工'
}

function employeeInitial(employee: AddressBookEmployee): string {
  return employeeName(employee).trim().charAt(0) || '?'
}

function employeeAvatarUrl(employee: AddressBookEmployee): string {
  return employee.imgUrl || employee.img || ''
}

function relationName(relation: RelationVO): string {
  return relation.name || '未命名关系'
}

function relationInitial(relation: RelationVO): string {
  return relationName(relation).trim().charAt(0) || '?'
}

function productName(product: ProductVO): string {
  return product.productName || '未命名产品'
}

function relationAvatarUrl(relation: RelationVO): string {
  return relation.avatarUrl || ''
}

function focusCustomerSearchInput() {
  customerSearchInputRef.value?.focus({ preventScroll: true })
}

function primeMobileCustomerSearchKeyboard() {
  if (!isMobile.value) return
  const input = mobileCustomerSearchKeyboardProxyRef.value
  if (!input) return
  input.value = ''
  input.focus({ preventScroll: true })
}

function openMobileCustomerSearchDialog() {
  primeMobileCustomerSearchKeyboard()
  openCustomerSearchDialog()
}

function openCustomerSearchDialog() {
  if (!showSidebarCustomers.value) return
  const focusRequestId = ++customerSearchFocusRequestId
  selectedPrimaryKey.value = ''
  showUserMenu.value = false
  closeGlobalSearchDropdown()
  customerSearchKeyword.value = ''
  customerSearchDialogVisible.value = true
  void fetchCustomerSearchCustomers()
  void nextTick(() => {
    if (focusRequestId !== customerSearchFocusRequestId || !customerSearchDialogVisible.value) return
    focusCustomerSearchInput()
  })
}

function closeCustomerSearchDialog() {
  customerSearchFocusRequestId++
  customerSearchDialogVisible.value = false
  resetCustomerSearchPull()
  if (customerSearchTimer) {
    clearTimeout(customerSearchTimer)
    customerSearchTimer = null
  }
  if (isMobile.value) {
    customerSearchInputRef.value?.blur()
    mobileCustomerSearchKeyboardProxyRef.value?.blur()
  }
}

function resetCustomerSearchPull() {
  customerSearchTouchStartY.value = null
  customerSearchPullStartY.value = null
  customerSearchPullDistance.value = 0
}

function handleCustomerSearchInput() {
  if (customerSearchTimer) {
    clearTimeout(customerSearchTimer)
  }
  customerSearchTimer = setTimeout(() => {
    void fetchCustomerSearchCustomers()
  }, 250)
}

function clearCustomerSearchKeyword() {
  customerSearchKeyword.value = ''
  if (customerSearchTimer) {
    clearTimeout(customerSearchTimer)
    customerSearchTimer = null
  }
  void fetchCustomerSearchCustomers()
  void nextTick(() => customerSearchInputRef.value?.focus())
}

function handleCustomerSearchScroll() {
  if (!isMobile.value) return
  maybeLoadMoreCustomerSearchCustomers()
}

function isCustomerSearchNearBottom() {
  const el = customerSearchScrollRef.value
  if (!el) return false
  const remaining = el.scrollHeight - el.scrollTop - el.clientHeight
  return remaining <= CUSTOMER_SEARCH_SCROLL_THRESHOLD_PX
}

function maybeLoadMoreCustomerSearchCustomers() {
  if (!isMobile.value || customerSearchLoading.value || !customerSearchHasMore.value) return
  if (!isCustomerSearchNearBottom()) return
  void loadMoreCustomerSearchCustomers()
}

function handleCustomerSearchTouchStart(event: TouchEvent) {
  if (!isMobile.value || customerSearchLoading.value) return
  const pointY = event.touches[0]?.clientY ?? null
  customerSearchTouchStartY.value = pointY
  const el = customerSearchScrollRef.value
  if (!el || el.scrollTop > 0) {
    customerSearchPullStartY.value = null
    customerSearchPullDistance.value = 0
    return
  }
  customerSearchPullStartY.value = pointY
}

function handleCustomerSearchTouchMove(event: TouchEvent) {
  if (!isMobile.value || customerSearchLoading.value) return
  const startY = customerSearchPullStartY.value
  const pointY = event.touches[0]?.clientY
  const el = customerSearchScrollRef.value
  if (startY == null || pointY == null || !el || el.scrollTop > 0) return

  const deltaY = pointY - startY
  if (deltaY <= 0) {
    customerSearchPullDistance.value = 0
    return
  }

  customerSearchPullDistance.value = Math.min(CUSTOMER_SEARCH_PULL_MAX_PX, deltaY * 0.45)
  if (event.cancelable) {
    event.preventDefault()
  }
}

function handleCustomerSearchTouchEnd(event?: TouchEvent) {
  if (!isMobile.value) {
    resetCustomerSearchPull()
    return
  }
  if (customerSearchPullDistance.value >= CUSTOMER_SEARCH_PULL_REFRESH_THRESHOLD_PX) {
    void refreshCustomerSearchCustomers()
    return
  }
  const endY = event?.changedTouches[0]?.clientY ?? null
  const startY = customerSearchTouchStartY.value
  const isSwipeUp = startY != null && endY != null ? startY - endY > 8 : true
  const shouldLoadMore = isSwipeUp && isCustomerSearchNearBottom()
  resetCustomerSearchPull()
  if (shouldLoadMore) {
    maybeLoadMoreCustomerSearchCustomers()
  }
}

function handleCustomerSearchTouchCancel() {
  resetCustomerSearchPull()
}

async function refreshCustomerSearchCustomers() {
  await fetchCustomerSearchCustomers({ refreshing: true })
}

async function loadMoreCustomerSearchCustomers() {
  if (customerSearchLoading.value || !customerSearchHasMore.value) return
  await fetchCustomerSearchCustomers({ append: true })
}

function scheduleCustomerSearchFillCheck() {
  if (!isMobile.value || !customerSearchDialogVisible.value) return
  void nextTick(() => {
    maybeLoadMoreCustomerSearchCustomers()
  })
}

async function fetchCustomerSearchCustomers(options: { append?: boolean; refreshing?: boolean } = {}) {
  const append = Boolean(options.append)
  if (append && customerSearchLoading.value) return
  const refreshing = Boolean(options.refreshing)
  const page = append ? customerSearchPage.value : 1
  const requestId = ++customerSearchRequestId
  customerSearchLoading.value = true
  customerSearchRefreshing.value = refreshing
  customerSearchAppending.value = append
  try {
    const result = await queryCustomerList({
      page,
      limit: CUSTOMER_SEARCH_LIMIT,
      keyword: customerSearchKeyword.value.trim() || undefined,
    })
    if (requestId !== customerSearchRequestId) return
    const nextCustomers = result.list || []
    if (append) {
      const existingIds = new Set(customerSearchCustomers.value.map(customer => String(customer.customerId)))
      customerSearchCustomers.value = [
        ...customerSearchCustomers.value,
        ...nextCustomers.filter(customer => !existingIds.has(String(customer.customerId)))
      ]
    } else {
      customerSearchCustomers.value = nextCustomers
      customerSearchScrollRef.value?.scrollTo({ top: 0 })
    }
    customerSearchTotal.value = result.totalRow || customerSearchCustomers.value.length
    customerSearchPage.value = page + 1
    customerSearchHasMore.value = nextCustomers.length > 0 && customerSearchCustomers.value.length < customerSearchTotal.value
    scheduleCustomerSearchFillCheck()
  } catch (error) {
    if (requestId !== customerSearchRequestId) return
    console.error('Search customers failed:', error)
    if (!append) {
      customerSearchCustomers.value = []
      customerSearchPage.value = 1
      customerSearchTotal.value = 0
      customerSearchHasMore.value = true
    }
    ElMessage.warning('客户搜索失败')
  } finally {
    if (requestId === customerSearchRequestId) {
      customerSearchLoading.value = false
      customerSearchRefreshing.value = false
      customerSearchAppending.value = false
      resetCustomerSearchPull()
    }
  }
}

async function handleSelectCustomerFromSearch(customer: CustomerListVO) {
  closeCustomerSearchDialog()
  closeMobileDrawer()
  await handleSelectCustomerChat(customer)
}

function getCustomerSearchSubtitle(customer: CustomerListVO): string {
  const parts = [
    customer.industry,
    enumStore.stageLabel(customer.stage),
    customer.ownerName ? `负责人 ${customer.ownerName}` : '',
  ].filter(Boolean)
  return parts.join(' · ') || '点击进入客户对话'
}

async function handleDeleteSession(sessionId: string) {
  try {
    await confirmDeleteChatSession()
    await chatStore.removeSession(sessionId)
    ElMessage.success('对话已删除')
  } catch {
    // User cancelled
  }
}

async function handlePinChatSession(session: ChatSession) {
  const nextPinned = !Boolean(session.pinned)
  try {
    await chatStore.setSessionPinned(session.sessionId, nextPinned)
    ElMessage.success(nextPinned ? '已置顶' : '已取消置顶')
  } catch (error) {
    console.error('Pin chat session failed:', error)
    ElMessage.error(nextPinned ? '置顶失败' : '取消置顶失败')
  }
}

async function handleShareChatSession(session: ChatSession) {
  const resolved = router.resolve({ name: 'Chat', query: { sessionId: session.sessionId } })
  const url = new URL(resolved.href, window.location.href).href
  const title = session.title || '新对话'
  if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
    try {
      await navigator.share({ title, text: title, url })
      return
    } catch (e) {
      if ((e as { name?: string })?.name === 'AbortError') return
    }
  }
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('对话链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制链接')
  }
}

function navigateTo(path: string, query?: Record<string, string>) {
  if (query) {
    void router.push({ path, query })
  } else {
    void router.push(path)
  }
  showUserMenu.value = false
}

function mobileNavigate(path: string, query?: Record<string, string>) {
  if (query) {
    void router.push({ path, query })
  } else {
    void router.push(path)
  }
  closeMobileDrawer()
}

function mobileProfileNavigate(path: string, query?: Record<string, string>) {
  mobileNavigate(path, query)
}

function handleOpenAccountSettings() {
  showUserMenu.value = false
  showAccountSettingsModal.value = true
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node | null
  if (!target) return
  if (searchPanelRef.value?.contains(target)) return
  closeGlobalSearchDropdown()
}

function clearGlobalSearchResults() {
  globalSearchResults.value = []
  globalSearchTotal.value = 0
  activeSearchResultIndex.value = -1
}

function closeGlobalSearchDropdown() {
  showGlobalSearchDropdown.value = false
  activeSearchResultIndex.value = -1
}

async function runGlobalSearch(keyword: string) {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    clearGlobalSearchResults()
    closeGlobalSearchDropdown()
    return
  }

  const requestId = ++globalSearchRequestId
  globalSearchLoading.value = true
  showGlobalSearchDropdown.value = true

  try {
    const result = await queryGlobalSearch({
      keyword: normalizedKeyword,
      page: 1,
      limit: 8,
    })

    if (requestId !== globalSearchRequestId) return

    globalSearchResults.value = result.list || []
    globalSearchTotal.value = result.totalRow || 0
    activeSearchResultIndex.value = globalSearchResults.value.length > 0 ? 0 : -1
  } catch (error) {
    if (requestId !== globalSearchRequestId) return
    console.error('Global search failed:', error)
    clearGlobalSearchResults()
  } finally {
    if (requestId === globalSearchRequestId) {
      globalSearchLoading.value = false
    }
  }
}

function debouncedGlobalSearch() {
  if (globalSearchTimer) {
    clearTimeout(globalSearchTimer)
  }

  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) {
    clearGlobalSearchResults()
    closeGlobalSearchDropdown()
    return
  }

  globalSearchTimer = setTimeout(() => {
    void runGlobalSearch(keyword)
  }, 250)
}

function handleGlobalSearchFocus() {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) return

  if (globalSearchResults.value.length > 0 || globalSearchLoading.value) {
    showGlobalSearchDropdown.value = true
    return
  }

  void runGlobalSearch(keyword)
}

function handleGlobalSearchArrow(direction: 1 | -1) {
  if (!showGlobalSearchDropdown.value) {
    handleGlobalSearchFocus()
    return
  }
  if (!globalSearchResults.value.length) return

  const total = globalSearchResults.value.length
  const nextIndex = activeSearchResultIndex.value + direction
  if (nextIndex < 0) {
    activeSearchResultIndex.value = total - 1
    return
  }
  if (nextIndex >= total) {
    activeSearchResultIndex.value = 0
    return
  }
  activeSearchResultIndex.value = nextIndex
}

function handleGlobalSearchEnter() {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) {
    closeGlobalSearchDropdown()
    return
  }

  const target = globalSearchResults.value[activeSearchResultIndex.value] || globalSearchResults.value[0]
  if (target) {
    navigateToSearchResult(target)
    return
  }

  void runGlobalSearch(keyword)
}

function navigateToSearchResult(result: GlobalSearchResult) {
  globalSearchKeyword.value = result.title || globalSearchKeyword.value
  closeGlobalSearchDropdown()
  void router.push(result.routePath)
}

function getSearchResultIcon(entityType: GlobalSearchResult['entityType']): WkIconName {
  switch (entityType) {
    case 'customer':
      return 'customer'
    case 'contact':
      return 'profile'
    case 'relation':
      return 'customer'
    case 'product':
      return 'crm'
    case 'task':
      return 'task'
    case 'schedule':
      return 'event'
    case 'knowledge':
    default:
      return 'knowledge'
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    showUserMenu.value = false
    drawerVisible.value = false
    await userStore.logout()
    void router.push('/login')
  } catch {
    // User cancelled
  }
}

function handleCreateCustomerSuccess(payload: { mode: 'create' | 'edit'; customerId?: string }) {
  appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
  if (payload.mode === 'create' && payload.customerId) {
    void router.push(`/customer/${payload.customerId}`)
  }
}

function openCreateRelationDialog() {
  showCreateRelation.value = true
}

function handleCreateRelationSuccess() {
  sidebarRelationsExpanded.value = true
  appEvents.emit(APP_EVENT.RELATION_SIDEBAR_REFRESH, { preserveScroll: true })
}

function openCreateProjectDialog() {
  showCreateProject.value = true
}

function handleMobileCreateCustomer() {
  closeMobileDrawer()
  showCreateCustomer.value = true
}

function handleMobileCreateRelation() {
  closeMobileDrawer()
  showCreateRelation.value = true
}

function openMobileCreateProjectDialog() {
  closeMobileDrawer()
  openCreateProjectDialog()
}

async function handleCreateProject(payload: {
  name: string
  description?: string
  customerId?: string
  customerName?: string
  ownerId?: string
  ownerName?: string
  startDate?: string
  dueDate?: string
  status: ProjectEntity['status']
}) {
  const project = await projectStore.createProject(payload)
  showCreateProject.value = false
  ElMessage.success('项目创建成功')
  await router.push({ name: 'ProjectDetail', params: { id: project.projectId }, query: { view: 'board' } })
}
</script>

<style scoped>
.wk-mobile-top-bar {
  box-sizing: border-box;
  height: calc(3.5rem + var(--safe-area-inset-top));
  padding-top: var(--safe-area-inset-top);
}

.wk-mobile-top-bar-spacer {
  padding-top: 3.5rem;
}

.wk-customer-search-dialog-shell {
  display: flex;
}

.wk-mobile-drawer-panel {
  box-sizing: border-box;
  padding-right: var(--safe-area-inset-right);
  padding-left: var(--safe-area-inset-left);
}

.wk-mobile-drawer-header {
  box-sizing: border-box;
  min-height: calc(64px + var(--safe-area-inset-top));
  padding-top: calc(12px + var(--safe-area-inset-top));
}

.wk-mobile-drawer-nav {
  padding-bottom: calc(112px + var(--safe-area-inset-bottom));
}

.wk-mobile-customer-search-keyboard-proxy {
  position: fixed;
  left: 16px;
  top: calc(1rem + var(--safe-area-inset-top));
  z-index: 0;
  width: 1px;
  height: 1px;
  padding: 0;
  border: 0;
  opacity: 0.01;
  color: transparent;
  caret-color: transparent;
  background: transparent;
  font-size: 16px;
  pointer-events: none;
  outline: none;
}

.wk-mobile-customer-search-pull-indicator {
  display: flex;
  height: 0;
  align-items: center;
  justify-content: center;
  gap: 6px;
  overflow: hidden;
  color: #8f8f8f;
  font-size: 12px;
  font-weight: 600;
  transition: height 160ms cubic-bezier(0.22, 1, 0.36, 1), opacity 140ms ease;
  opacity: 0;
}

.wk-mobile-customer-search-pull-indicator.is-active {
  opacity: 1;
}

.wk-mobile-customer-search-pull-indicator .material-symbols-outlined {
  font-size: 18px;
  line-height: 1;
}

.wk-primary-sidebar {
  border-color: var(--wk-border-subtle) !important;
  background: linear-gradient(180deg, var(--wk-bg-surface) 0%, var(--wk-bg-sidebar) 100%) !important;
  color: var(--wk-text-primary);
}

.wk-primary-nav-sections {
  display: flex;
  flex-direction: column;
}

.wk-sidebar-recent-section {
  order: 10;
}

.wk-sidebar-customer-section {
  order: 20;
}

.wk-sidebar-project-section {
  order: 30;
}

.wk-sidebar-address-section {
  order: 40;
}

.wk-sidebar-relation-section {
  order: 50;
}

.wk-primary-sidebar :deep(button) {
  color: var(--wk-text-primary);
}

.wk-primary-sidebar :deep(button:hover) {
  background-color: var(--wk-bg-surface-hover) !important;
}

.wk-sidebar-sort-row {
  position: relative;
  transform: translateZ(0);
  transition:
    background-color 150ms ease,
    box-shadow 150ms ease,
    opacity 150ms ease,
    transform 180ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.wk-sidebar-sort-row:hover,
.wk-sidebar-sort-row.is-drag-over {
  background: var(--wk-bg-surface-hover);
}

.wk-sidebar-sort-row.is-dragging {
  background: #f3f3f3;
  box-shadow: inset 0 0 0 1px #ececec;
  opacity: 0.72;
  transform: scale(0.985);
}

.wk-sidebar-sort-row__order {
  background: #f3f3f3;
  color: #8f8f8f;
  transition:
    background-color 150ms ease,
    color 150ms ease,
    transform 180ms cubic-bezier(0.22, 1, 0.36, 1);
}

.wk-sidebar-sort-row.is-dragging .wk-sidebar-sort-row__order,
.wk-sidebar-sort-row.is-drag-over .wk-sidebar-sort-row__order {
  background: #0d0d0d;
  color: #fff;
  transform: scale(1.04);
}

.wk-primary-sidebar .wk-sidebar-sort-save-button,
.wk-primary-sidebar .wk-sidebar-sort-save-button:hover,
.wk-primary-sidebar .wk-sidebar-sort-save-button:focus,
.wk-primary-sidebar .wk-sidebar-sort-save-button:active,
.wk-primary-sidebar .wk-sidebar-sort-save-button:disabled {
  background-color: #0d0d0d !important;
  color: #fff !important;
}

.wk-primary-sidebar .wk-customer-header-row:hover,
.wk-primary-sidebar .wk-project-header-row:hover {
  background: #f9f9f9 !important;
}

.wk-primary-sidebar .wk-customer-header-row :deep(button:hover),
.wk-primary-sidebar .wk-project-header-row :deep(button:hover) {
  background: transparent !important;
}

.wk-primary-sidebar .wk-customer-header-row .wk-customer-header-toggle:hover,
.wk-primary-sidebar .wk-customer-header-row .wk-customer-header-action:hover,
.wk-primary-sidebar .wk-project-header-row .wk-project-header-toggle:hover,
.wk-primary-sidebar .wk-project-header-row .wk-project-header-action:hover {
  background: transparent !important;
}

.wk-primary-sidebar :deep(.wk-customer-header-toggle:hover),
.wk-primary-sidebar :deep(.wk-project-header-toggle:hover) {
  background: transparent !important;
}

.wk-primary-sidebar :deep(.bg-\[\#f3f3f3\]) {
  background-color: var(--wk-bg-surface-active) !important;
}

.wk-primary-sidebar :deep(.bg-\[\#f9f9f9\]) {
  background-color: var(--wk-bg-surface-hover) !important;
}

.wk-primary-sidebar :deep(.border-\[\#ececec\]),
.wk-primary-sidebar :deep(.border-slate-200),
.wk-primary-sidebar :deep(.border-slate-100) {
  border-color: var(--wk-border-subtle) !important;
}

.wk-primary-sidebar :deep(.text-slate-900),
.wk-primary-sidebar :deep(.text-\[\#0d0d0d\]) {
  color: var(--wk-text-primary) !important;
}

.wk-primary-sidebar :deep(.text-slate-500),
.wk-primary-sidebar :deep(.text-slate-400),
.wk-primary-sidebar :deep(.text-\[\#8f8f8f\]),
.wk-primary-sidebar :deep(.text-\[\#c9c9c9\]) {
  color: var(--wk-text-muted) !important;
}

.wk-primary-sidebar :deep(input) {
  border-color: var(--wk-border-subtle) !important;
  background-color: var(--wk-bg-surface) !important;
  color: var(--wk-text-primary) !important;
}

.wk-primary-sidebar :deep(input:focus) {
  border-color: var(--wk-border-muted) !important;
  background-color: var(--wk-bg-surface) !important;
}

.wk-mobile-recent-history-input :deep(.el-input__wrapper) {
  border-radius: 8px !important;
}

.drawer-overlay-enter-active,
.drawer-overlay-leave-active {
  transition: opacity 0.3s ease;
}

.drawer-overlay-enter-from,
.drawer-overlay-leave-to {
  opacity: 0;
}

.drawer-panel-enter-active,
.drawer-panel-leave-active {
  transition: transform 0.3s ease;
}

.drawer-panel-enter-from,
.drawer-panel-leave-to {
  transform: translateX(-100%);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.profile-drawer-enter-active,
.profile-drawer-leave-active {
  transition: transform 0.28s ease, opacity 0.2s ease;
}

.profile-drawer-enter-from,
.profile-drawer-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

.secondary-panel-enter-active,
.secondary-panel-leave-active {
  transition: transform 0.2s ease, opacity 0.15s ease;
}

.secondary-panel-enter-from,
.secondary-panel-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.customer-search-dialog-enter-active,
.customer-search-dialog-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.customer-search-dialog-enter-from,
.customer-search-dialog-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

.line-clamp-2 {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

</style>
