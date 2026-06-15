<template>
  <div class="mail-view flex h-full min-h-0 flex-col bg-[var(--wk-bg-page,#f6f7f9)]">
    <div class="shrink-0 border-b border-[var(--wk-border-subtle,#e5e7eb)] bg-white px-4 py-3 md:px-6">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="min-w-0">
          <h2 class="text-xl font-semibold text-slate-950">邮箱</h2>
          <p class="mt-1 text-sm text-slate-500">{{ currentMailboxLabel }}</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <el-input
            v-model="keyword"
            clearable
            class="mail-search-input"
            :placeholder="searchPlaceholder"
            :prefix-icon="Search"
            @clear="loadActiveTab"
            @keyup.enter="loadActiveTab"
          />
          <el-select v-if="activeTab === 'inbox'" v-model="readStatusFilter" class="mail-filter-select" clearable placeholder="阅读状态" @change="loadActiveTab">
            <el-option label="未读" value="unread" />
            <el-option label="已读" value="read" />
          </el-select>
          <el-select v-if="activeTab === 'templates'" v-model="templateCategoryFilter" class="mail-filter-select" clearable placeholder="模板分类" @change="loadActiveTab">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
          <el-button v-if="activeTab === 'templates'" @click="openTemplate()">新建模板</el-button>
          <el-dropdown v-if="authStatus.authorized" trigger="click" @command="handleAccountCommand">
            <el-button>
              <span class="max-w-[220px] truncate">{{ currentAccountEmailLabel }}</span>
              <span class="material-symbols-outlined ml-1 text-[18px]">expand_more</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="account in authStatus.accounts" :key="account.accountId" :command="`default:${account.accountId}`">
                  <span class="flex min-w-[220px] items-center justify-between gap-3">
                    <span class="truncate">{{ account.emailAddress }}</span>
                    <el-tag v-if="account.isDefault" size="small" type="success">默认</el-tag>
                    <el-tag v-else-if="account.connectionStatus === 'expired'" size="small" type="warning">过期</el-tag>
                  </span>
                </el-dropdown-item>
                <el-dropdown-item divided command="add">添加邮箱</el-dropdown-item>
                <el-dropdown-item v-if="authStatus.currentAccount" command="sync">同步当前邮箱</el-dropdown-item>
                <el-dropdown-item v-if="authStatus.currentAccount" command="disconnect">取消授权</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="primary" :icon="EditPen" @click="handleCompose">写邮件</el-button>
        </div>
      </div>
    </div>

    <div v-if="authLoading" class="flex flex-1 items-center justify-center text-slate-400">
      <span class="material-symbols-outlined mr-2 animate-spin">progress_activity</span>
      加载邮箱状态
    </div>

    <div v-else-if="!authStatus.authorized" class="flex flex-1 items-center justify-center px-6">
      <div class="w-full max-w-md rounded-lg border border-dashed border-slate-300 bg-white px-8 py-10 text-center">
        <span class="material-symbols-outlined text-[46px] text-slate-300">mail</span>
        <h3 class="mt-4 text-lg font-semibold text-slate-950">连接您的邮箱，开始邮件沟通</h3>
        <p class="mt-2 text-sm leading-6 text-slate-500">授权后可同步收件箱、管理草稿和模板，并在客户沟通中复用邮件内容。</p>
        <el-button class="mt-6" type="primary" size="large" @click="connectDialogVisible = true">立即授权</el-button>
      </div>
    </div>

    <div v-else class="flex min-h-0 flex-1 flex-col px-4 py-4 md:px-6">
      <el-tabs v-model="activeTab" class="mail-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="草稿箱" name="drafts" />
        <el-tab-pane label="发件箱" name="sent" />
        <el-tab-pane label="收件箱" name="inbox" />
        <el-tab-pane label="邮件模板" name="templates" />
      </el-tabs>

      <div class="min-h-0 flex-1 overflow-hidden rounded-lg border border-[var(--wk-border-subtle,#e5e7eb)] bg-white">
        <el-table
          v-if="activeTab === 'drafts'"
          v-loading="loading"
          :data="drafts"
          height="100%"
          row-key="draftId"
          empty-text="暂无草稿"
        >
          <el-table-column prop="subject" label="主题" min-width="260" show-overflow-tooltip />
          <el-table-column prop="toAddresses" label="收件人" min-width="220" show-overflow-tooltip />
          <el-table-column label="最近编辑" width="170">
            <template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="accountEmail" label="邮箱账号" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="draftStatusType(row.status)">{{ draftStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDraft(row)">编辑</el-button>
              <el-button link type="success" @click="sendDraft(row)">发送</el-button>
              <el-button link type="danger" @click="removeDraft(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table
          v-else-if="activeTab === 'sent'"
          v-loading="loading"
          :data="sentMessages"
          height="100%"
          row-key="messageId"
          empty-text="暂无已发送邮件"
          @row-click="openMessage"
        >
          <el-table-column prop="fromAddress" label="发件邮箱" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.fromAddress || '-' }}</template>
          </el-table-column>
          <el-table-column prop="toAddresses" label="收件人" min-width="220" show-overflow-tooltip />
          <el-table-column prop="subject" label="主题" min-width="260" show-overflow-tooltip />
          <el-table-column label="发送时间" width="180">
            <template #default="{ row }">{{ formatDate(row.sentTime || row.receivedTime || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default>
              <el-tag size="small" type="success">成功</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right" align="right">
            <template #default="{ row }">
              <el-button class="mail-table-action-primary" link type="primary" @click.stop="openMessage(row)">详情</el-button>
              <el-button link @click.stop="reuseMessage(row)">转发</el-button>
              <el-button link type="danger" @click.stop="removeMessage(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table
          v-else-if="activeTab === 'inbox'"
          v-loading="loading"
          :data="inboxMessages"
          height="100%"
          row-key="messageId"
          empty-text="暂无收件箱邮件"
          @row-click="openMessage"
        >
          <el-table-column label="" width="56">
            <template #default="{ row }">
              <button class="mail-icon-button" type="button" @click.stop="toggleStar(row)">
                <span class="material-symbols-outlined text-[19px]" :class="row.starred ? 'text-amber-500' : 'text-slate-300'">
                  {{ row.starred ? 'star' : 'star_border' }}
                </span>
              </button>
            </template>
          </el-table-column>
          <el-table-column label="发件人" min-width="210" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="min-w-0">
                <p :class="row.readStatus !== 'read' ? 'font-semibold text-slate-950' : 'text-slate-700'">
                  {{ row.fromName || row.fromAddress || '-' }}
                </p>
                <p v-if="row.fromAddress && row.fromName" class="mt-0.5 truncate text-xs text-slate-400">{{ row.fromAddress }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="收件邮箱" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.toAddresses || '-' }}</template>
          </el-table-column>
          <el-table-column label="标题与摘要" min-width="320">
            <template #default="{ row }">
              <div class="min-w-0">
                <p :class="row.readStatus !== 'read' ? 'font-semibold text-slate-950' : 'text-slate-800'">{{ row.subject || '(无主题)' }}</p>
                <p class="mt-0.5 truncate text-xs text-slate-400">{{ row.summary || row.bodyText || '-' }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="接收时间" width="180">
            <template #default="{ row }">{{ formatDate(row.receivedTime || row.sentTime || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.readStatus === 'read' ? 'info' : 'primary'">{{ row.readStatus === 'read' ? '已读' : '未读' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right" align="right">
            <template #default="{ row }">
              <el-button class="mail-table-action-primary" link type="primary" @click.stop="replyMessage(row, false)">回复</el-button>
              <el-button link @click.stop="replyMessage(row, true)">全部回复</el-button>
              <el-button link @click.stop="reuseMessage(row)">转发</el-button>
              <el-button link type="danger" @click.stop="removeMessage(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table
          v-else
          v-loading="loading"
          :data="templates"
          height="100%"
          row-key="templateId"
          empty-text="暂无邮件模板"
        >
          <el-table-column prop="name" label="模板名称" min-width="220" show-overflow-tooltip />
          <el-table-column label="分类" width="140">
            <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
          </el-table-column>
          <el-table-column prop="subject" label="主题" min-width="260" show-overflow-tooltip />
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="常用" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.isCommon" size="small" type="success">常用</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTemplate(row)">编辑</el-button>
              <el-button link @click="insertTemplate(row)">插入</el-button>
              <el-button link @click="copyTemplate(row)">复制</el-button>
              <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mt-3 flex shrink-0 items-center justify-between text-sm text-slate-500">
        <span>共 {{ totalRow }} 条</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.limit"
          small
          background
          :page-sizes="[15, 30, 50, 100]"
          :layout="isMobile ? 'prev, pager, next' : 'sizes, prev, pager, next, jumper'"
          :total="totalRow"
          @size-change="handlePageSizeChange"
          @current-change="loadActiveTab"
        />
      </div>
    </div>

    <el-drawer v-model="detailDrawerVisible" :size="isMobile ? '100%' : '600px'" title="邮件详情" class="mail-detail-drawer">
      <div v-if="selectedMessage" class="mail-detail">
        <div class="mail-detail-head">
          <h3 class="mail-detail-subject">{{ selectedMessage.subject || '(无主题)' }}</h3>
          <div class="mail-detail-meta">
            <div class="mail-detail-meta-row">
              <span class="mail-detail-meta-label">发件人</span>
              <span class="mail-detail-meta-value">{{ formatAddressLabel(selectedMessage.fromName, selectedMessage.fromAddress) }}</span>
            </div>
            <div class="mail-detail-meta-row">
              <span class="mail-detail-meta-label">收件人</span>
              <span class="mail-detail-meta-value">{{ selectedMessage.toAddresses || '-' }}</span>
            </div>
            <div v-if="selectedMessage.ccAddresses" class="mail-detail-meta-row">
              <span class="mail-detail-meta-label">抄送</span>
              <span class="mail-detail-meta-value">{{ selectedMessage.ccAddresses }}</span>
            </div>
            <div class="mail-detail-meta-row">
              <span class="mail-detail-meta-label">时间</span>
              <span class="mail-detail-meta-value">{{ formatDate(selectedMessage.receivedTime || selectedMessage.sentTime || selectedMessage.createTime) }}</span>
            </div>
          </div>
          <div v-if="messageHasRemoteImages" class="mail-detail-image-tip">
            <span class="material-symbols-outlined">image</span>
            <span>{{ showRemoteImages ? '已显示邮件中的远程图片' : '已屏蔽远程图片' }}</span>
            <el-button link type="primary" @click="showRemoteImages = !showRemoteImages">
              {{ showRemoteImages ? '隐藏' : '显示' }}
            </el-button>
          </div>
        </div>
        <div class="mail-detail-body">
          <div v-if="detailLoading" class="mail-detail-loading">
            <span class="material-symbols-outlined animate-spin">progress_activity</span>
            正在加载邮件内容...
          </div>
          <iframe
            v-else
            :srcdoc="messageFrameSrcDoc"
            sandbox="allow-popups allow-popups-to-escape-sandbox"
            referrerpolicy="no-referrer"
            class="mail-detail-frame"
          />
        </div>
        <div class="mail-detail-actions">
          <el-button v-if="selectedMessage.direction !== 'sent'" type="primary" @click="replyMessage(selectedMessage, false)">回复</el-button>
          <el-button @click="reuseMessage(selectedMessage)">转发</el-button>
          <el-button @click="toggleRead(selectedMessage)">{{ selectedMessage.readStatus === 'read' ? '标为未读' : '标为已读' }}</el-button>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="composeVisible" :size="isMobile ? '100%' : '680px'" :title="composeForm.draftId ? '编辑邮件' : '写邮件'" @opened="syncEditorFromForm">
      <el-form label-position="top" class="mail-compose-form">
        <el-form-item label="发件邮箱">
          <el-select v-model="composeForm.accountId" class="w-full" placeholder="默认邮箱">
            <el-option v-for="account in enabledAccounts" :key="account.accountId" :label="account.emailAddress" :value="account.accountId" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人">
          <el-input v-model="composeForm.toAddresses" placeholder="name@example.com, other@example.com" />
        </el-form-item>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <el-form-item label="抄送">
            <el-input v-model="composeForm.ccAddresses" placeholder="可选" />
          </el-form-item>
          <el-form-item label="密送">
            <el-input v-model="composeForm.bccAddresses" placeholder="可选" />
          </el-form-item>
        </div>
        <el-form-item label="主题">
          <el-input v-model="composeForm.subject" placeholder="请输入邮件主题" />
        </el-form-item>

        <div class="mb-3 flex flex-wrap items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-2 py-2">
          <el-tooltip content="加粗"><button class="mail-tool-button" type="button" @click="runEditorCommand('bold')"><span class="material-symbols-outlined">format_bold</span></button></el-tooltip>
          <el-tooltip content="斜体"><button class="mail-tool-button" type="button" @click="runEditorCommand('italic')"><span class="material-symbols-outlined">format_italic</span></button></el-tooltip>
          <el-tooltip content="下划线"><button class="mail-tool-button" type="button" @click="runEditorCommand('underline')"><span class="material-symbols-outlined">format_underlined</span></button></el-tooltip>
          <el-tooltip content="列表"><button class="mail-tool-button" type="button" @click="runEditorCommand('insertUnorderedList')"><span class="material-symbols-outlined">format_list_bulleted</span></button></el-tooltip>
          <el-tooltip content="链接"><button class="mail-tool-button" type="button" @click="insertLink"><span class="material-symbols-outlined">link</span></button></el-tooltip>
          <el-tooltip content="表格"><button class="mail-tool-button" type="button" @click="insertTable"><span class="material-symbols-outlined">table</span></button></el-tooltip>
          <span class="mx-1 h-5 w-px bg-slate-200" />
          <el-select v-model="aiIntent" class="mail-ai-select" size="small" placeholder="AI 生成邮件">
            <el-option label="跟进客户" value="followup" />
            <el-option label="产品报价" value="quote" />
            <el-option label="售后回复" value="service" />
            <el-option label="商务邀约" value="invite" />
          </el-select>
          <el-button size="small" :icon="MagicStick" @click="generateMailWithAi">生成</el-button>
          <el-dropdown trigger="click" @command="optimizeMail">
            <el-button size="small">
              AI优化
              <span class="material-symbols-outlined ml-1 text-[16px]">expand_more</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="professional">专业化润色</el-dropdown-item>
                <el-dropdown-item command="polite">更礼貌</el-dropdown-item>
                <el-dropdown-item command="concise">更简洁</el-dropdown-item>
                <el-dropdown-item command="sales">更强销售转化</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div ref="editorRef" class="mail-editor" contenteditable="true" @input="handleEditorInput" @blur="handleEditorInput" />
        <div class="mt-2 flex items-center justify-between text-xs text-slate-400">
          <span>{{ autoSaveLabel }}</span>
          <span>支持模板变量：{{ variableHints }}</span>
        </div>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="composeVisible = false">取消</el-button>
          <el-button :loading="savingDraft" @click="saveCurrentDraft(false)">保存草稿</el-button>
          <el-button @click="previewVisible = true">预览</el-button>
          <el-button type="primary" :loading="sending" @click="sendCurrentDraft">发送</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="connectDialogVisible" class="mail-connect-dialog" :width="isMobile ? '92%' : '760px'">
      <template #header>
        <div>
          <h3 class="m-0 text-lg font-bold text-slate-950">绑定邮箱账号</h3>
          <p class="mt-1 text-xs text-slate-500">绑定后可在系统内同步、查看和发送客户邮件</p>
        </div>
      </template>

      <div class="mb-4 grid grid-cols-1 gap-3 md:grid-cols-2">
        <el-button class="h-11" @click="handleOAuth('gmail')">
          <span class="flex items-center gap-2">
            <span class="text-lg font-bold text-blue-500">G</span>
            Gmail 一键授权
          </span>
        </el-button>
        <el-button class="h-11" @click="handleOAuth('outlook')">
          <span class="flex items-center gap-2">
            <span class="material-symbols-outlined text-base text-blue-600">mail</span>
            Outlook / Microsoft 365
          </span>
        </el-button>
      </div>

      <div class="mb-4 flex items-center gap-2 rounded-lg bg-blue-50 px-3 py-2 text-xs text-blue-700">
        <span class="material-symbols-outlined text-base">verified_user</span>
        推荐使用 OAuth 授权；其他邮箱请填写客户端授权码并确认已开启 IMAP/SMTP 服务。
      </div>

      <el-form class="mail-connect-form" label-position="top">
        <el-form-item label="邮箱类型">
          <el-select
            v-model="selectedMailProviderPreset"
            class="w-full"
            placeholder="选择常用邮箱厂商"
            filterable
            @change="applyMailProviderPreset"
          >
            <el-option
              v-for="preset in mailProviderPresets"
              :key="preset.value"
              :label="preset.label"
              :value="preset.value"
            >
              <div class="mail-provider-option">
                <span>{{ preset.label }}</span>
                <span v-if="preset.domainHint" class="text-xs text-slate-400">{{ preset.domainHint }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <el-form-item label="邮箱地址">
            <el-input v-model="imapForm.emailAddress" placeholder="example@163.com" @blur="syncUsernameFromEmail" />
          </el-form-item>
          <el-form-item label="授权码 / 邮箱密码">
            <el-input v-model="imapForm.password" type="password" show-password />
          </el-form-item>
        </div>
        <div class="mb-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
          <span>登录账号默认使用邮箱地址。</span>
          <el-link type="primary" :underline="false" @click="showAuthCodeHelp">如何获取授权码?</el-link>
        </div>

        <el-collapse v-model="connectAdvancedPanels" class="mail-connect-advanced">
          <el-collapse-item name="advanced">
            <template #title>
              <span class="inline-flex items-center gap-2 font-semibold text-slate-700">
                <span class="material-symbols-outlined text-base">settings</span>
                高级设置
              </span>
            </template>
            <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
              <el-form-item label="显示名称"><el-input v-model="imapForm.displayName" /></el-form-item>
              <el-form-item label="登录账号"><el-input v-model="imapForm.username" /></el-form-item>
              <el-form-item label="IMAP 主机"><el-input v-model="imapForm.imapHost" placeholder="imap.example.com" /></el-form-item>
              <el-form-item label="IMAP 端口"><el-input-number v-model="imapForm.imapPort" class="w-full" :min="1" :max="65535" /></el-form-item>
              <el-form-item label="SMTP 主机"><el-input v-model="imapForm.smtpHost" placeholder="smtp.example.com" /></el-form-item>
              <el-form-item label="SMTP 端口"><el-input-number v-model="imapForm.smtpPort" class="w-full" :min="1" :max="65535" /></el-form-item>
            </div>
            <div class="flex flex-wrap gap-4">
              <el-checkbox v-model="imapForm.imapSsl">开启 IMAP SSL</el-checkbox>
              <el-checkbox v-model="imapForm.smtpSsl">开启 SMTP SSL</el-checkbox>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <div class="mail-connect-footer">
          <el-button @click="connectDialogVisible = false">取消</el-button>
          <el-button :loading="testingConnection" @click="testImapConnect">测试连接</el-button>
          <el-button type="primary" :loading="connecting" @click="connectImap">保存并连接</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialogVisible" :title="templateForm.templateId ? '编辑模板' : '新建模板'" :width="isMobile ? '92%' : '620px'">
      <el-form label-position="top">
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <el-form-item label="模板名称"><el-input v-model="templateForm.name" /></el-form-item>
          <el-form-item label="分类">
            <el-select v-model="templateForm.category" class="w-full">
              <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="主题"><el-input v-model="templateForm.subject" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="templateForm.bodyText" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="变量"><el-input v-model="templateForm.variables" placeholder="{{客户姓名}},{{公司名称}}" /></el-form-item>
        <el-checkbox v-model="templateForm.isCommon">设为常用</el-checkbox>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingTemplate" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="邮件预览" :width="isMobile ? '92%' : '620px'">
      <h3 class="mb-3 text-base font-semibold text-slate-950">{{ composeForm.subject || '(无主题)' }}</h3>
      <p class="mb-3 text-sm text-slate-500">收件人：{{ composeForm.toAddresses || '-' }}</p>
      <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-700" v-html="composeForm.bodyText || '暂无正文'" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import { useResponsive } from '@/composables/useResponsive'
import {
  connectImapMailbox,
  copyMailTemplate,
  createMailTemplate,
  deleteDraft,
  deleteMailMessage,
  deleteMailTemplate,
  disconnectMailbox,
  getMailAuthStatus,
  getMailMessage,
  listMailboxSyncLogs,
  markMailRead,
  queryDrafts,
  queryInbox,
  queryMailTemplates,
  querySent,
  saveDraft,
  sendMail,
  setDefaultMailbox,
  starMail,
  startMailOAuth,
  syncMailbox,
  testImapMailbox,
  updateDraft,
  updateMailTemplate,
  type MailAuthStatus,
  type MailDraft,
  type MailDraftPayload,
  type MailImapConnectPayload,
  type MailMessage,
  type MailSyncLog,
  type MailTemplate,
  type MailTemplatePayload,
} from '@/api/mail'

type MailTab = 'drafts' | 'sent' | 'inbox' | 'templates'
type AutoSaveState = 'idle' | 'saving' | 'saved' | 'failed'
const SYNC_STATUS_MAX_LOGS = 5

type MailProviderPreset = {
  value: string
  label: string
  domainHint?: string
  imapHost?: string
  imapPort?: number
  imapSsl?: boolean
  smtpHost?: string
  smtpPort?: number
  smtpSsl?: boolean
}

const { isMobile } = useResponsive()

const activeTab = ref<MailTab>('drafts')
const keyword = ref('')
const readStatusFilter = ref('')
const templateCategoryFilter = ref('')
const authLoading = ref(false)
const loading = ref(false)
const connecting = ref(false)
const testingConnection = ref(false)
const savingDraft = ref(false)
const sending = ref(false)
const savingTemplate = ref(false)
const composeVisible = ref(false)
const connectDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const showRemoteImages = ref(true)
const templateDialogVisible = ref(false)
const previewVisible = ref(false)
const editorRef = ref<HTMLDivElement | null>(null)
const selectedMessage = ref<MailMessage | null>(null)
const drafts = ref<MailDraft[]>([])
const sentMessages = ref<MailMessage[]>([])
const inboxMessages = ref<MailMessage[]>([])
const templates = ref<MailTemplate[]>([])
const totalRow = ref(0)
const aiIntent = ref('followup')
const autoSaveState = ref<AutoSaveState>('idle')
const selectedMailProviderPreset = ref('custom')
const connectAdvancedPanels = ref(['advanced'])
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null
let suppressAutoSave = false

const pagination = reactive({
  page: 1,
  limit: 15,
})

const authStatus = reactive<MailAuthStatus>({
  authorized: false,
  accounts: [],
})

const composeForm = reactive<{
  draftId?: string
  accountId?: string
  toAddresses: string
  ccAddresses: string
  bccAddresses: string
  subject: string
  bodyText: string
}>({
  toAddresses: '',
  ccAddresses: '',
  bccAddresses: '',
  subject: '',
  bodyText: '',
})

const imapForm = reactive<MailImapConnectPayload>({
  emailAddress: '',
  displayName: '',
  imapHost: '',
  imapPort: 993,
  imapSsl: true,
  smtpHost: '',
  smtpPort: 465,
  smtpSsl: true,
  username: '',
  password: '',
  testConnection: true,
})

const templateForm = reactive<MailTemplatePayload>({
  name: '',
  category: 'custom',
  subject: '',
  bodyText: '',
  variables: '{{客户姓名}},{{公司名称}},{{销售姓名}},{{当前日期}}',
  isCommon: false,
})

const categoryOptions = [
  { label: '客户跟进', value: 'followup' },
  { label: '商务邀约', value: 'invite' },
  { label: '售后服务', value: 'service' },
  { label: '产品报价', value: 'quote' },
  { label: '节日问候', value: 'holiday' },
  { label: '自定义', value: 'custom' },
]

const variableHints = '{{客户姓名}} {{公司名称}} {{销售姓名}} {{当前日期}}'

const mailProviderPresets: MailProviderPreset[] = [
  {
    value: 'qq',
    label: 'QQ邮箱',
    domainHint: '@qq.com',
    imapHost: 'imap.qq.com',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.qq.com',
    smtpPort: 465,
    smtpSsl: true,
  },
  {
    value: 'netease-163',
    label: '网易163邮箱',
    domainHint: '@163.com',
    imapHost: 'imap.163.com',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.163.com',
    smtpPort: 465,
    smtpSsl: true,
  },
  {
    value: 'netease-126',
    label: '网易126邮箱',
    domainHint: '@126.com',
    imapHost: 'imap.126.com',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.126.com',
    smtpPort: 465,
    smtpSsl: true,
  },
  {
    value: 'netease-yeah',
    label: '网易Yeah邮箱',
    domainHint: '@yeah.net',
    imapHost: 'imap.yeah.net',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.yeah.net',
    smtpPort: 465,
    smtpSsl: true,
  },
  {
    value: 'tencent-exmail',
    label: '腾讯企业邮箱',
    domainHint: '企业域名',
    imapHost: 'imap.exmail.qq.com',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.exmail.qq.com',
    smtpPort: 465,
    smtpSsl: true,
  },
  {
    value: 'aliyun',
    label: '阿里邮箱',
    domainHint: '企业域名',
    imapHost: 'imap.qiye.aliyun.com',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.qiye.aliyun.com',
    smtpPort: 465,
    smtpSsl: true,
  },
  {
    value: 'sina',
    label: '新浪邮箱',
    domainHint: '@sina.com',
    imapHost: 'imap.sina.com',
    imapPort: 993,
    imapSsl: true,
    smtpHost: 'smtp.sina.com',
    smtpPort: 465,
    smtpSsl: true,
  },
  { value: 'custom', label: '自定义', domainHint: '手动填写' },
]

const enabledAccounts = computed(() => authStatus.accounts.filter(account => account.enabled))

const currentAccountEmailLabel = computed(() => normalizeAccountEmailLabel(authStatus.currentAccount?.emailAddress) || '邮箱账号')

const searchPlaceholder = computed(() =>
  activeTab.value === 'templates' ? '搜索模板名称' : '搜索主题、收件人、发件人或正文'
)

const currentMailboxLabel = computed(() => {
  if (authLoading.value) return '正在检查邮箱授权状态'
  if (!authStatus.authorized) return '尚未连接邮箱账号'
  const account = authStatus.currentAccount
  if (!account) return '请选择邮箱账号'
  const status = account.connectionStatus || 'connected'
  return `${account.emailAddress} · ${accountStatusLabel(status)}`
})

const autoSaveLabel = computed(() => {
  if (autoSaveState.value === 'saving') return '正在保存草稿'
  if (autoSaveState.value === 'saved') return '草稿已保存'
  if (autoSaveState.value === 'failed') return '草稿保存失败'
  return canPersistDraft.value ? '停止输入 3 秒后自动保存' : '填写收件人、主题和正文后自动保存'
})

const canPersistDraft = computed(() =>
  Boolean(composeForm.toAddresses.trim() && composeForm.subject.trim() && stripHtml(composeForm.bodyText).trim())
)

const messageInnerHtml = computed(() => {
  const message = selectedMessage.value
  if (!message) return ''
  if (message.bodyHtml && message.bodyHtml.trim()) return message.bodyHtml
  return plainTextToHtml(message.bodyText || message.summary || '')
})

const messageHasRemoteImages = computed(() => {
  const html = selectedMessage.value?.bodyHtml
  return Boolean(html && /<img[^>]+src=["']?https?:/i.test(html))
})

const messageFrameSrcDoc = computed(() => {
  const imgSrc = showRemoteImages.value ? 'img-src http: https: data: cid:;' : 'img-src data:;'
  const csp = `default-src 'none'; style-src 'unsafe-inline'; ${imgSrc} font-src data: http: https:; media-src 'none'; frame-src 'none'; script-src 'none';`
  return [
    '<!doctype html><html><head><meta charset="utf-8">',
    `<meta http-equiv="Content-Security-Policy" content="${csp}">`,
    '<base target="_blank">',
    `<style>${MAIL_FRAME_STYLE}</style>`,
    `</head><body>${messageInnerHtml.value}</body></html>`,
  ].join('')
})

watch(
  () => [composeForm.accountId, composeForm.toAddresses, composeForm.ccAddresses, composeForm.bccAddresses, composeForm.subject, composeForm.bodyText],
  () => scheduleAutoSave()
)

watch(keyword, () => {
  pagination.page = 1
  window.setTimeout(() => loadActiveTab(), 240)
})

onMounted(async () => {
  await loadAuthStatus()
  if (authStatus.authorized) {
    await loadActiveTab()
  }
})

onBeforeUnmount(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
})

async function loadAuthStatus() {
  authLoading.value = true
  try {
    const status = await getMailAuthStatus()
    authStatus.authorized = status.authorized
    authStatus.currentAccount = status.currentAccount
    authStatus.accounts = status.accounts || []
  } finally {
    authLoading.value = false
  }
}

async function loadActiveTab() {
  if (!authStatus.authorized) return
  loading.value = true
  const query = {
    page: pagination.page,
    limit: pagination.limit,
    accountId: authStatus.currentAccount?.accountId,
    keyword: keyword.value.trim() || undefined,
  }
  const templateQuery = {
    page: pagination.page,
    limit: pagination.limit,
    keyword: keyword.value.trim() || undefined,
  }
  try {
    if (activeTab.value === 'drafts') {
      const result = await queryDrafts(query)
      drafts.value = result.list || []
      totalRow.value = result.totalRow || 0
    } else if (activeTab.value === 'sent') {
      const result = await querySent(query)
      sentMessages.value = result.list || []
      totalRow.value = result.totalRow || 0
    } else if (activeTab.value === 'inbox') {
      const result = await queryInbox({ ...query, readStatus: readStatusFilter.value || undefined })
      inboxMessages.value = result.list || []
      totalRow.value = result.totalRow || 0
    } else {
      const result = await queryMailTemplates({ ...templateQuery, category: templateCategoryFilter.value || undefined })
      templates.value = result.list || []
      totalRow.value = result.totalRow || 0
    }
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pagination.page = 1
  void loadActiveTab()
}

function handlePageSizeChange() {
  pagination.page = 1
  void loadActiveTab()
}

async function handleRefresh() {
  if (activeTab.value !== 'templates' && authStatus.currentAccount?.accountId) {
    await syncCurrentMailbox(false)
  }
  await loadActiveTab()
}

async function syncCurrentMailbox(notifyNoNew: boolean) {
  const account = authStatus.currentAccount
  if (!account?.accountId) return false

  const result = await syncMailbox(account.accountId)
  account.lastSyncStatus = result.status || 'success'
  account.lastSyncTime = new Date().toISOString()
  if (result.errorMessage) {
    account.lastSyncError = result.errorMessage
  }

  await loadAuthStatus()
  const latestLog = await findLatestMailboxSyncLog(account.accountId, result.logId)
  const status = latestLog?.status || result.status
  const savedCount = latestLog?.savedCount ?? result.savedCount ?? 0
  const errorMessage = latestLog?.errorMessage || result.errorMessage

  if (status === 'running') {
    ElMessage.info('邮箱同步已开始，稍后刷新可查看结果')
    return true
  }

  if (status === 'failed') {
    ElMessage.error(errorMessage || '邮箱同步失败，请稍后重试')
    return false
  }
  if (savedCount > 0) {
    ElMessage.success(`邮箱同步完成，新增 ${savedCount} 封邮件`)
  } else if (notifyNoNew) {
    ElMessage.info('邮箱同步完成，暂无新邮件')
  }
  return true
}

async function findLatestMailboxSyncLog(accountId: string, logId?: string): Promise<MailSyncLog | null> {
  try {
    const logs = await listMailboxSyncLogs(accountId, SYNC_STATUS_MAX_LOGS)
    if (!logs.length) return null
    if (!logId) return logs[0]
    return logs.find(log => String(log.logId) === String(logId)) || logs[0]
  } catch {
    return null
  }
}

function handleCompose() {
  if (!authStatus.authorized) {
    connectDialogVisible.value = true
    return
  }
  resetCompose()
  composeForm.accountId = authStatus.currentAccount?.accountId
  composeVisible.value = true
}

function resetCompose() {
  suppressAutoSave = true
  composeForm.draftId = undefined
  composeForm.accountId = undefined
  composeForm.toAddresses = ''
  composeForm.ccAddresses = ''
  composeForm.bccAddresses = ''
  composeForm.subject = ''
  composeForm.bodyText = ''
  autoSaveState.value = 'idle'
  void nextTick(() => {
    syncEditorFromForm()
    suppressAutoSave = false
  })
}

function openDraft(draft: MailDraft) {
  suppressAutoSave = true
  composeForm.draftId = draft.draftId
  composeForm.accountId = draft.accountId || authStatus.currentAccount?.accountId
  composeForm.toAddresses = draft.toAddresses || ''
  composeForm.ccAddresses = draft.ccAddresses || ''
  composeForm.bccAddresses = draft.bccAddresses || ''
  composeForm.subject = draft.subject || ''
  composeForm.bodyText = draft.bodyText || ''
  composeVisible.value = true
  autoSaveState.value = 'idle'
  void nextTick(() => {
    syncEditorFromForm()
    suppressAutoSave = false
  })
}

async function saveCurrentDraft(silent: boolean) {
  if (!canPersistDraft.value) {
    if (!silent) ElMessage.warning('请先填写收件人、主题和正文')
    return
  }
  savingDraft.value = !silent
  autoSaveState.value = 'saving'
  try {
    const payload = buildDraftPayload()
    const saved = composeForm.draftId
      ? await updateDraft(composeForm.draftId, payload)
      : await saveDraft(payload)
    composeForm.draftId = saved.draftId
    autoSaveState.value = 'saved'
    if (!silent) ElMessage.success('草稿已保存')
    if (activeTab.value === 'drafts') await loadActiveTab()
  } catch (error) {
    autoSaveState.value = 'failed'
    if (!silent) throw error
  } finally {
    savingDraft.value = false
  }
}

async function sendCurrentDraft() {
  if (!canPersistDraft.value) {
    ElMessage.warning('请先填写收件人、主题和正文')
    return
  }
  sending.value = true
  try {
    if (!composeForm.draftId) {
      await saveCurrentDraft(true)
    }
    await sendMail(composeForm.draftId ? { draftId: composeForm.draftId } : { draft: buildDraftPayload() })
    ElMessage.success('邮件已发送')
    closeComposeAfterSent()
    activeTab.value = 'sent'
    pagination.page = 1
    await loadActiveTab()
  } finally {
    sending.value = false
  }
}

function closeComposeAfterSent() {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
    autoSaveTimer = null
  }
  previewVisible.value = false
  composeVisible.value = false
}

async function sendDraft(draft: MailDraft) {
  await sendMail({ draftId: draft.draftId })
  ElMessage.success('邮件已发送')
  activeTab.value = 'sent'
  await loadActiveTab()
}

async function removeDraft(draft: MailDraft) {
  await ElMessageBox.confirm('确定删除这封草稿吗？', '提示', { type: 'warning' })
  await deleteDraft(draft.draftId)
  ElMessage.success('草稿已删除')
  await loadActiveTab()
}

async function openMessage(row: MailMessage) {
  selectedMessage.value = row
  showRemoteImages.value = true
  detailDrawerVisible.value = true
  detailLoading.value = true
  try {
    const detail = await getMailMessage(row.messageId)
    selectedMessage.value = detail
    if (detail.direction !== 'sent' && detail.readStatus !== 'read') {
      await markMailRead(detail.messageId, true)
      detail.readStatus = 'read'
      row.readStatus = 'read'
    }
  } finally {
    detailLoading.value = false
  }
}

function replyMessage(message: MailMessage, replyAll: boolean) {
  resetCompose()
  composeForm.accountId = authStatus.currentAccount?.accountId
  composeForm.toAddresses = message.fromAddress || ''
  composeForm.ccAddresses = replyAll ? message.ccAddresses || '' : ''
  composeForm.subject = message.subject?.startsWith('Re:') ? message.subject : `Re: ${message.subject || ''}`
  composeForm.bodyText = `<p></p><blockquote>${message.bodyHtml || escapeHtml(message.bodyText || message.summary || '')}</blockquote>`
  composeVisible.value = true
}

function reuseMessage(message: MailMessage) {
  resetCompose()
  composeForm.accountId = authStatus.currentAccount?.accountId
  composeForm.subject = message.subject?.startsWith('Fwd:') ? message.subject : `Fwd: ${message.subject || ''}`
  composeForm.bodyText = `<p></p><blockquote>${message.bodyHtml || escapeHtml(message.bodyText || message.summary || '')}</blockquote>`
  composeVisible.value = true
}

async function toggleRead(message: MailMessage) {
  const read = message.readStatus !== 'read'
  await markMailRead(message.messageId, read)
  message.readStatus = read ? 'read' : 'unread'
  ElMessage.success(read ? '已标为已读' : '已标为未读')
}

async function toggleStar(message: MailMessage) {
  const starred = !message.starred
  await starMail(message.messageId, starred)
  message.starred = starred
}

async function removeMessage(message: MailMessage) {
  await ElMessageBox.confirm('确定删除这封邮件吗？', '提示', { type: 'warning' })
  await deleteMailMessage(message.messageId)
  ElMessage.success('邮件已删除')
  await loadActiveTab()
}

function openTemplate(template?: MailTemplate) {
  templateForm.templateId = template?.templateId
  templateForm.name = template?.name || ''
  templateForm.category = template?.category || 'custom'
  templateForm.subject = template?.subject || ''
  templateForm.bodyText = template?.bodyText || ''
  templateForm.variables = template?.variables || variableHints.split(' ').join(',')
  templateForm.isCommon = Boolean(template?.isCommon)
  templateDialogVisible.value = true
}

function insertTemplate(template: MailTemplate) {
  composeForm.subject = replaceTemplateVariables(template.subject || composeForm.subject)
  composeForm.bodyText = replaceTemplateVariables(template.bodyText || '')
  composeVisible.value = true
  void nextTick(syncEditorFromForm)
}

async function saveTemplate() {
  if (!templateForm.name?.trim() || !templateForm.subject?.trim() || !templateForm.bodyText?.trim()) {
    ElMessage.warning('请填写模板名称、主题和正文')
    return
  }
  savingTemplate.value = true
  try {
    if (templateForm.templateId) {
      await updateMailTemplate(templateForm.templateId, templateForm)
    } else {
      await createMailTemplate(templateForm)
    }
    ElMessage.success('模板已保存')
    templateDialogVisible.value = false
    activeTab.value = 'templates'
    await loadActiveTab()
  } finally {
    savingTemplate.value = false
  }
}

async function copyTemplate(template: MailTemplate) {
  await copyMailTemplate(template.templateId)
  ElMessage.success('模板已复制')
  await loadActiveTab()
}

async function removeTemplate(template: MailTemplate) {
  await ElMessageBox.confirm('确定删除这个模板吗？', '提示', { type: 'warning' })
  await deleteMailTemplate(template.templateId)
  ElMessage.success('模板已删除')
  await loadActiveTab()
}

async function handleOAuth(provider: 'gmail' | 'outlook') {
  const result = await startMailOAuth(provider)
  window.open(result.authorizeUrl, '_blank', 'noopener,noreferrer,width=760,height=760')
  ElMessage.info('授权完成后请回到本页面刷新邮箱状态')
}

function applyMailProviderPreset(value: string) {
  const preset = mailProviderPresets.find(item => item.value === value)
  if (!preset || preset.value === 'custom') return

  imapForm.imapHost = preset.imapHost || ''
  imapForm.imapPort = preset.imapPort || 993
  imapForm.imapSsl = preset.imapSsl ?? true
  imapForm.smtpHost = preset.smtpHost || ''
  imapForm.smtpPort = preset.smtpPort || 465
  imapForm.smtpSsl = preset.smtpSsl ?? true
  syncUsernameFromEmail()
}

function syncUsernameFromEmail() {
  const email = imapForm.emailAddress.trim()
  if (email) {
    imapForm.username = email
  }
}

function showAuthCodeHelp() {
  ElMessage.info('请在邮箱设置中开启 IMAP/SMTP 服务，并生成客户端授权码或专用密码。')
}

function validateImapConnectForm() {
  syncUsernameFromEmail()
  if (!imapForm.emailAddress || !imapForm.imapHost || !imapForm.smtpHost || !imapForm.password) {
    ElMessage.warning('请填写邮箱地址、IMAP/SMTP 主机和授权码')
    return false
  }
  const imapPort = Number(imapForm.imapPort)
  const smtpPort = Number(imapForm.smtpPort)
  if (!Number.isInteger(imapPort) || !Number.isInteger(smtpPort) || imapPort < 1 || imapPort > 65535 || smtpPort < 1 || smtpPort > 65535) {
    ElMessage.warning('请填写有效的 IMAP/SMTP 端口')
    return false
  }
  imapForm.imapPort = imapPort
  imapForm.smtpPort = smtpPort
  return true
}

async function testImapConnect() {
  if (!validateImapConnectForm()) return
  testingConnection.value = true
  try {
    await testImapMailbox(imapForm)
    ElMessage.success('连接测试成功')
  } finally {
    testingConnection.value = false
  }
}

async function connectImap() {
  if (!validateImapConnectForm()) return
  connecting.value = true
  try {
    await connectImapMailbox(imapForm)
    ElMessage.success('邮箱已连接')
    connectDialogVisible.value = false
    await loadAuthStatus()
    await loadActiveTab()
  } finally {
    connecting.value = false
  }
}

async function handleAccountCommand(command: string) {
  if (command === 'add') {
    connectDialogVisible.value = true
    return
  }
  if (command === 'sync' && authStatus.currentAccount?.accountId) {
    await syncCurrentMailbox(true)
    await loadActiveTab()
    return
  }
  if (command === 'disconnect' && authStatus.currentAccount?.accountId) {
    await ElMessageBox.confirm('确定取消当前邮箱授权吗？', '提示', { type: 'warning' })
    await disconnectMailbox(authStatus.currentAccount.accountId)
    await loadAuthStatus()
    await loadActiveTab()
    return
  }
  if (command.startsWith('default:')) {
    await setDefaultMailbox(command.slice('default:'.length))
    await loadAuthStatus()
    pagination.page = 1
    await loadActiveTab()
  }
}

function buildDraftPayload(): MailDraftPayload {
  return {
    accountId: composeForm.accountId || authStatus.currentAccount?.accountId,
    toAddresses: composeForm.toAddresses.trim(),
    ccAddresses: composeForm.ccAddresses.trim() || undefined,
    bccAddresses: composeForm.bccAddresses.trim() || undefined,
    subject: composeForm.subject.trim(),
    bodyText: composeForm.bodyText,
  }
}

function scheduleAutoSave() {
  if (suppressAutoSave || !composeVisible.value) return
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  if (!canPersistDraft.value) return
  autoSaveState.value = 'idle'
  autoSaveTimer = setTimeout(() => {
    void saveCurrentDraft(true)
  }, 3000)
}

function syncEditorFromForm() {
  if (editorRef.value && editorRef.value.innerHTML !== composeForm.bodyText) {
    editorRef.value.innerHTML = composeForm.bodyText
  }
}

function handleEditorInput() {
  composeForm.bodyText = editorRef.value?.innerHTML || ''
}

function runEditorCommand(command: string) {
  editorRef.value?.focus()
  document.execCommand(command)
  handleEditorInput()
}

async function insertLink() {
  const url = window.prompt('链接地址')
  if (!url) return
  editorRef.value?.focus()
  document.execCommand('createLink', false, url)
  handleEditorInput()
}

function insertTable() {
  editorRef.value?.focus()
  document.execCommand('insertHTML', false, '<table><tbody><tr><td>项目</td><td>说明</td></tr><tr><td></td><td></td></tr></tbody></table><p></p>')
  handleEditorInput()
}

function generateMailWithAi() {
  const map: Record<string, string> = {
    followup: '<p>{{客户姓名}}您好，</p><p>想跟进一下我们上次沟通的事项。若您方便，我可以继续补充资料或安排下一步沟通。</p><p>祝好，<br>{{销售姓名}}</p>',
    quote: '<p>{{客户姓名}}您好，</p><p>根据贵司需求，我们整理了产品报价和交付建议，核心方案如下：</p><ul><li>方案：</li><li>报价：</li><li>交付周期：</li></ul><p>期待您的反馈。</p>',
    service: '<p>{{客户姓名}}您好，</p><p>您反馈的问题我们已经收到，会优先协助排查。请您补充相关截图或发生时间，方便我们快速定位。</p>',
    invite: '<p>{{客户姓名}}您好，</p><p>想邀请您本周安排一次简短沟通，围绕合作目标和后续推进节奏做一次对齐。</p>',
  }
  composeForm.bodyText = replaceTemplateVariables(map[aiIntent.value] || map.followup)
  syncEditorFromForm()
}

function optimizeMail(command: string) {
  const prefixMap: Record<string, string> = {
    professional: '以下内容已按更专业的商务语气整理：',
    polite: '以下内容已按更礼貌的表达整理：',
    concise: '以下内容已按更简洁的表达整理：',
    sales: '以下内容已按更强调价值和下一步行动的表达整理：',
  }
  const text = composeForm.bodyText || ''
  composeForm.bodyText = `<p>${prefixMap[command] || '优化后的邮件内容：'}</p>${text}`
  syncEditorFromForm()
}

function replaceTemplateVariables(value: string) {
  const today = new Date().toLocaleDateString()
  return value
    .split('{{客户姓名}}').join('客户')
    .split('{{公司名称}}').join('贵司')
    .split('{{销售姓名}}').join('我')
    .split('{{当前日期}}').join(today)
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

function accountStatusLabel(status: string) {
  const map: Record<string, string> = {
    connected: '已连接',
    expired: '授权过期',
    syncing: '同步中',
    disconnected: '已断开',
    error: '异常',
  }
  return map[status] || status
}

function draftStatusLabel(status: string) {
  const map: Record<string, string> = {
    draft: '草稿',
    sent: '已发送',
    send_failed: '发送失败',
  }
  return map[status] || status
}

function draftStatusType(status: string) {
  if (status === 'sent') return 'success'
  if (status === 'send_failed') return 'danger'
  return 'info'
}

function categoryLabel(value?: string) {
  return categoryOptions.find(item => item.value === value)?.label || value || '自定义'
}

function stripHtml(value: string) {
  return value.replace(/<[^>]+>/g, ' ').replace(/&nbsp;/g, ' ')
}

function formatAddressLabel(name?: string, address?: string) {
  const trimmedName = name?.trim()
  const trimmedAddress = address?.trim()
  if (trimmedName && trimmedAddress) return `${trimmedName} <${trimmedAddress}>`
  return trimmedAddress || trimmedName || '-'
}

function normalizeAccountEmailLabel(email?: string) {
  return (email || '').trim().replace(/^@+/, '')
}

function plainTextToHtml(raw: string) {
  const cleaned = raw.replace(/\[image:[^\]]*\]/gi, '')
  const urlPattern = /<?(https?:\/\/[^\s<>]+)>?/g
  let html = ''
  let lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = urlPattern.exec(cleaned)) !== null) {
    html += escapeHtml(cleaned.slice(lastIndex, match.index))
    const safeUrl = escapeHtml(match[1])
    html += `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer">${safeUrl}</a>`
    lastIndex = match.index + match[0].length
  }
  html += escapeHtml(cleaned.slice(lastIndex))
  return html
    .split(/\n{2,}/)
    .map(block => `<p>${block.replace(/\n/g, '<br>')}</p>`)
    .join('')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const MAIL_FRAME_STYLE =
  "html,body{margin:0;padding:16px;color:#1f2933;background:#ffffff;" +
  "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'PingFang SC','Microsoft YaHei',sans-serif;" +
  "font-size:14px;line-height:1.7;word-break:break-word;overflow-wrap:anywhere;}" +
  "img{max-width:100%;height:auto;}a{color:#2563eb;}table{max-width:100%;border-collapse:collapse;}" +
  "blockquote{margin:0 0 0 8px;padding-left:12px;border-left:3px solid #e2e8f0;color:#475569;}"
</script>

<style scoped>
.mail-search-input {
  width: min(360px, 100%);
}

.mail-filter-select {
  width: 132px;
}

.mail-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.mail-icon-button,
.mail-tool-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #64748b;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.mail-icon-button:hover,
.mail-tool-button:hover {
  background-color: #eef2f7;
  color: #0f172a;
}

.mail-tool-button .material-symbols-outlined {
  font-size: 20px;
}

.mail-ai-select {
  width: 132px;
}

.mail-editor {
  min-height: 260px;
  max-height: 48vh;
  overflow: auto;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  background: #ffffff;
  padding: 14px;
  color: #0f172a;
  line-height: 1.7;
  outline: none;
}

.mail-editor:focus {
  border-color: #94a3b8;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.16);
}

.mail-editor:empty::before {
  color: #94a3b8;
  content: '输入邮件正文';
}

.mail-editor :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.mail-editor :deep(td),
.mail-editor :deep(th) {
  border: 1px solid #cbd5e1;
  padding: 8px;
}

.mail-provider-option,
.mail-connect-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.mail-connect-footer {
  justify-content: flex-end;
}

.mail-connect-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.mail-connect-advanced {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.mail-connect-advanced :deep(.el-collapse-item__header),
.mail-connect-advanced :deep(.el-collapse-item__wrap) {
  border: 0;
  background: transparent;
}

.mail-connect-advanced :deep(.el-collapse-item__content) {
  padding: 0 16px 16px;
}

.mail-table-action-primary {
  border-radius: 7px !important;
  background: #171717 !important;
  padding: 6px 12px !important;
  color: #ffffff !important;
  font-weight: 700 !important;
}

.mail-detail-drawer :deep(.el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.mail-detail {
  display: flex;
  min-height: 0;
  height: 100%;
  flex-direction: column;
}

.mail-detail-head {
  flex-shrink: 0;
  padding: 4px 20px 16px;
  border-bottom: 1px solid #ebe6dd;
}

.mail-detail-subject {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.mail-detail-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mail-detail-meta-row {
  display: flex;
  gap: 10px;
  font-size: 13px;
  line-height: 1.5;
}

.mail-detail-meta-label {
  flex-shrink: 0;
  width: 44px;
  color: #94a3b8;
}

.mail-detail-meta-value {
  min-width: 0;
  color: #334155;
  word-break: break-all;
}

.mail-detail-image-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 6px 10px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
}

.mail-detail-image-tip .material-symbols-outlined {
  font-size: 16px;
}

.mail-detail-body {
  position: relative;
  min-height: 0;
  flex: 1;
  background: #ffffff;
}

.mail-detail-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}

.mail-detail-loading {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  font-size: 14px;
}

.mail-detail-actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid #ebe6dd;
}
</style>
