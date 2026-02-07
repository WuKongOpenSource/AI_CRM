<template>
  <div class="h-full flex flex-col bg-gray-50">
    <!-- Page Header -->
    <div class="px-4 md:px-6 py-4 bg-white border-b border-gray-200">
      <div class="flex items-start justify-between">
        <div>
          <h1 class="text-lg font-semibold">系统设置</h1>
          <p class="text-sm text-gray-500 mt-1 hidden md:block">管理系统配置和偏好设置</p>
        </div>
        <div class="text-sm text-gray-500 hidden md:block">
          您的权限: <span class="text-gray-700">{{ userStore.realname || '用户' }}，完整权限</span>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-auto p-3 md:p-6">
      <div class="bg-white rounded-lg shadow-sm">
        <!-- Card Header -->
        <div class="px-6 py-4 border-b border-gray-200">
          <div class="flex items-center">
            <div class="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center mr-3">
              <el-icon :size="20" class="text-primary-500"><Setting /></el-icon>
            </div>
            <div>
              <h2 class="text-base font-medium">系统设置</h2>
              <p class="text-sm text-gray-500">管理系统配置和偏好设置</p>
            </div>
          </div>
        </div>

        <!-- Tab Navigation -->
        <el-tabs v-model="activeTab" class="settings-tabs">
          <el-tab-pane name="profile">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><User /></el-icon>
                个人资料
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="team">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><UserFilled /></el-icon>
                团队管理
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="role">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Key /></el-icon>
                角色权限
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="stage">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><TrendCharts /></el-icon>
                商机阶段
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="agent">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><MagicStick /></el-icon>
                智能体
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="notification">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Bell /></el-icon>
                通知设置
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="data">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Coin /></el-icon>
                数据管理
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="api">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Connection /></el-icon>
                API/AI
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="storage">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Box /></el-icon>
                对象存储
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="weknora">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Reading /></el-icon>
                知识库服务
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="customField">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Grid /></el-icon>
                自定义字段
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="workflow">
            <template #label>
              <span class="flex items-center gap-1.5">
                <el-icon><Share /></el-icon>
                工作流
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>

        <!-- Tab Content -->
        <div class="p-6">
          <!-- Profile Tab -->
          <div v-if="activeTab === 'profile'" class="space-y-6">
            <!-- Profile Card -->
            <el-card shadow="never" class="!border-gray-200">
              <!-- Avatar Section -->
              <div class="flex items-center justify-between pb-6 border-b border-gray-200">
                <div class="flex items-center">
                  <el-avatar :size="80" class="bg-primary-500 text-2xl font-medium">
                    {{ profileForm.realname?.charAt(0) || userStore.realname?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="ml-4">
                    <div class="text-xl font-medium">{{ profileForm.realname || userStore.realname }}</div>
                    <div class="text-gray-500">{{ profileForm.position || '员工' }}</div>
                  </div>
                </div>
                <el-button>更换头像</el-button>
              </div>

              <!-- Profile Form -->
              <el-form :model="profileForm" label-position="top" class="mt-6">
                <el-row :gutter="24">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="姓名">
                      <el-input v-model="profileForm.realname" placeholder="请输入姓名" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="邮箱">
                      <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="24">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="电话">
                      <el-input v-model="profileForm.phone" placeholder="请输入电话" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="部门">
                      <el-input v-model="profileForm.department" placeholder="请输入部门" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="24">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="职位">
                      <el-input v-model="profileForm.position" placeholder="请输入职位" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <div class="flex gap-3 pt-4">
                  <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">
                    <el-icon class="mr-1"><Document /></el-icon>
                    保存更改
                  </el-button>
                  <el-button @click="resetProfileForm">取消</el-button>
                </div>
              </el-form>
            </el-card>

            <!-- Password Change Card -->
            <el-card shadow="never" class="!border-gray-200">
              <template #header>
                <span class="font-medium">密码修改</span>
              </template>
              <el-form :model="passwordForm" label-position="top" class="max-w-md">
                <el-form-item label="当前密码">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认密码">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-button type="primary" :loading="submitting" @click="handleChangePassword">修改密码</el-button>
              </el-form>
            </el-card>
          </div>

          <!-- AI Agent Tab -->
          <div v-else-if="activeTab === 'agent'">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-medium">AI 智能体管理</h3>
              <el-button type="primary" @click="showAgentDialog = true">
                <el-icon class="mr-1"><Plus /></el-icon>
                添加智能体
              </el-button>
            </div>
            <div v-if="agentStore.loading" class="text-center py-8">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else-if="agentStore.allAgents.length === 0" class="text-center py-8 text-gray-400">
              暂无智能体
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="agent in agentStore.allAgents"
                :key="agent.agentId"
                class="flex items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-200"
              >
                <div class="flex items-center">
                  <el-icon :size="24" class="text-primary-500"><Promotion /></el-icon>
                  <div class="ml-3">
                    <div class="font-medium">{{ agent.label }}</div>
                    <div class="text-sm text-gray-500 truncate max-w-md">{{ agent.prompt }}</div>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <el-switch
                    :model-value="!!agent.enabled"
                    :disabled="agentStore.updating"
                    @change="(val: boolean) => handleToggleAgent(agent.agentId, val)"
                  />
                  <el-button text @click="handleEditAgent(agent)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button text type="danger" @click="handleDeleteAgent(agent)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- Custom Field Tab -->
          <div v-else-if="activeTab === 'customField'">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-medium">自定义字段管理</h3>
              <el-button type="primary" @click="handleOpenFieldDialog">
                <el-icon class="mr-1"><Plus /></el-icon>
                添加字段
              </el-button>
            </div>

            <!-- Entity Type Tabs -->
            <el-tabs v-model="activeEntityType" class="mb-4" @tab-change="loadCustomFields">
              <el-tab-pane label="客户字段" name="customer" />
              <el-tab-pane label="联系人字段" name="contact" />
            </el-tabs>

            <div v-if="loadingFields" class="text-center py-8">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else-if="customFields.length === 0" class="text-center py-8 text-gray-400">
              暂无自定义字段
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="field in customFields"
                :key="field.fieldId"
                class="flex items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-200"
              >
                <div class="flex items-center flex-1">
                  <div class="mr-4">
                    <div class="font-medium">
                      {{ field.fieldLabel }}
                      <el-tag v-if="field.isRequired" size="small" type="danger" class="ml-2">必填</el-tag>
                    </div>
                    <div class="text-sm text-gray-500 mt-1">
                      <span>标识: {{ field.fieldName }}</span>
                      <el-tag size="small" class="ml-2">{{ getFieldTypeLabel(field.fieldType) }}</el-tag>
                      <span v-if="field.options && field.options.length > 0" class="ml-2">
                        选项: {{ field.options.map(o => o.label).join(', ') }}
                      </span>
                    </div>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <el-switch
                    :model-value="field.status === 1"
                    @change="(val: boolean) => handleToggleFieldStatus(field, val)"
                  />
                  <el-button text @click="handleEditField(field)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-popconfirm
                    title="删除字段将同时删除数据库列，确定继续吗？"
                    confirm-button-text="删除"
                    cancel-button-text="取消"
                    @confirm="handleDeleteField(field)"
                  >
                    <template #reference>
                      <el-button text type="danger">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>

          <!-- API/AI Tab -->
          <div v-else-if="activeTab === 'api'" class="space-y-6">
            <el-card shadow="never" class="!border-gray-200">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-medium">AI 大模型配置</span>
                  <el-tag v-if="aiConfigForm.updateTime" size="small" type="info">
                    最后更新: {{ formatTime(aiConfigForm.updateTime) }}
                  </el-tag>
                </div>
              </template>

              <el-form :model="aiConfigForm" label-position="top" class="max-w-2xl">
                <!-- AI 服务提供商 -->
                <el-form-item label="AI 服务提供商">
                  <el-select
                    v-model="aiConfigForm.provider"
                    class="w-full"
                    @change="handleProviderChange"
                  >
                    <el-option
                      v-for="preset in providerPresets"
                      :key="preset.value"
                      :label="preset.label"
                      :value="preset.value"
                    />
                  </el-select>
                </el-form-item>

                <!-- API 地址 -->
                <el-form-item label="API 基础地址">
                  <el-input
                    v-model="aiConfigForm.apiUrl"
                    placeholder="https://api.openai.com/v1"
                  >
                    <template #prepend>URL</template>
                  </el-input>
                  <div class="text-xs text-gray-400 mt-1">
                    OpenAI 兼容接口的基础 URL，末尾不要加斜杠
                  </div>
                </el-form-item>

                <!-- API Key -->
                <el-form-item label="API 密钥">
                  <div class="flex gap-2 w-full">
                    <el-input
                      v-model="aiConfigForm.apiKey"
                      :type="showApiKey ? 'text' : 'password'"
                      placeholder="sk-xxxxxx"
                      class="flex-1"
                    >
                      <template #prepend>Key</template>
                      <template #suffix>
                        <el-icon
                          class="cursor-pointer"
                          @click="showApiKey = !showApiKey"
                        >
                          <View v-if="showApiKey" />
                          <Hide v-else />
                        </el-icon>
                      </template>
                    </el-input>
                    <el-button
                      :loading="testingConnection"
                      @click="handleTestConnection"
                    >
                      <el-icon class="mr-1"><Connection /></el-icon>
                      测试连接
                    </el-button>
                  </div>
                  <div v-if="connectionTestResult" class="mt-2">
                    <el-alert
                      :type="connectionTestResult.success ? 'success' : 'error'"
                      :closable="false"
                      show-icon
                    >
                      <template #title>
                        {{ connectionTestResult.success ? '连接成功' : '连接失败' }}
                        <span class="text-gray-500 ml-2">
                          ({{ connectionTestResult.responseTime }}ms)
                        </span>
                      </template>
                      <template #default>
                        {{ connectionTestResult.message }}
                      </template>
                    </el-alert>
                  </div>
                </el-form-item>

                <!-- 模型选择 -->
                <el-form-item label="模型">
                  <el-select
                    v-model="aiConfigForm.model"
                    class="w-full"
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="model in currentProviderModels"
                      :key="model"
                      :label="model"
                      :value="model"
                    />
                  </el-select>
                  <div class="text-xs text-gray-400 mt-1">
                    可以从列表选择或直接输入自定义模型名称
                  </div>
                </el-form-item>

                <!-- Temperature -->
                <el-form-item label="Temperature (创造性)">
                  <div class="flex items-center gap-4 w-full">
                    <el-slider
                      v-model="aiConfigForm.temperature"
                      :min="0"
                      :max="2"
                      :step="0.1"
                      :format-tooltip="(val: number) => val.toFixed(1)"
                      class="flex-1"
                    />
                    <el-input-number
                      v-model="aiConfigForm.temperature"
                      :min="0"
                      :max="2"
                      :step="0.1"
                      :precision="1"
                      class="w-24"
                    />
                  </div>
                  <div class="text-xs text-gray-400 mt-1">
                    值越低回复越确定，值越高回复越有创造性。推荐值：0.7
                  </div>
                </el-form-item>

                <!-- Max Tokens -->
                <el-form-item label="最大 Token 数">
                  <el-input-number
                    v-model="aiConfigForm.maxTokens"
                    :min="100"
                    :max="128000"
                    :step="100"
                    class="w-full"
                  />
                  <div class="text-xs text-gray-400 mt-1">
                    单次对话允许的最大 Token 数量，包括输入和输出
                  </div>
                </el-form-item>

                <!-- 操作按钮 -->
                <div class="flex gap-3 pt-4 border-t border-gray-200">
                  <el-button
                    type="primary"
                    :loading="savingAiConfig"
                    @click="handleSaveAiConfig"
                  >
                    <el-icon class="mr-1"><Document /></el-icon>
                    保存 AI 配置
                  </el-button>
                  <el-button @click="loadAiConfig">重置</el-button>
                </div>
              </el-form>
            </el-card>

            <!-- 配置说明卡片 -->
            <el-card shadow="never" class="!border-gray-200">
              <template #header>
                <span class="font-medium">配置说明</span>
              </template>
              <div class="text-sm text-gray-600 space-y-2">
                <p><strong>OpenAI:</strong> 使用 OpenAI 官方 API，需要有效的 API Key</p>
                <p><strong>阿里云 DashScope:</strong> 使用阿里云通义千问系列模型，API 地址为 https://dashscope.aliyuncs.com/compatible-mode/</p>
                <p><strong>自定义:</strong> 任何 OpenAI 兼容的 API 服务，如 LocalAI、Ollama 等</p>
              </div>
            </el-card>
          </div>

          <!-- Object Storage Tab -->
          <div v-else-if="activeTab === 'storage'" class="space-y-6">
            <el-card shadow="never" class="!border-gray-200">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-medium">MinIO 对象存储</span>
                  <el-tag v-if="minioConfig.enabled" type="success" size="small">已启用</el-tag>
                  <el-tag v-else type="info" size="small">未启用</el-tag>
                </div>
              </template>

              <div v-if="loadingMinioConfig" class="text-center py-8">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <div v-else class="space-y-6">
                <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
                  <div class="w-12 h-12 rounded-lg bg-orange-100 flex items-center justify-center">
                    <el-icon :size="24" class="text-orange-500"><Box /></el-icon>
                  </div>
                  <div class="flex-1">
                    <div class="font-medium">MinIO 管理控制台</div>
                    <div class="text-sm text-gray-500 mt-1">
                      {{ minioConfig.consoleUrl || '未配置' }}
                    </div>
                  </div>
                  <el-button
                    type="primary"
                    :disabled="!minioConfig.consoleUrl"
                    @click="handleOpenMinioConsole"
                  >
                    <el-icon class="mr-1"><Link /></el-icon>
                    进入管理后台
                  </el-button>
                </div>

                <el-alert type="info" :closable="false" show-icon>
                  <template #title>SSO 单点登录</template>
                  <template #default>
                    系统已配置 OIDC 单点登录。登录 CRM 系统后，点击上方按钮可直接进入 MinIO 管理后台，无需再次输入密码。
                  </template>
                </el-alert>

                <div class="text-sm text-gray-500">
                  <p class="mb-2"><strong>说明：</strong></p>
                  <ul class="list-disc list-inside space-y-1">
                    <li>MinIO 提供 S3 兼容的对象存储服务</li>
                    <li>用于存储知识库文档、附件等文件</li>
                    <li>如需修改存储配置，请联系系统管理员</li>
                  </ul>
                </div>
              </div>
            </el-card>
          </div>

          <!-- WeKnora Knowledge Service Tab -->
          <div v-else-if="activeTab === 'weknora'" class="space-y-6">
            <el-card shadow="never" class="!border-gray-200">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="font-medium">WeKnora 知识库服务配置</span>
                  <el-tag v-if="weknoraConfigForm.updateTime" size="small" type="info">
                    最后更新: {{ formatTime(weknoraConfigForm.updateTime) }}
                  </el-tag>
                </div>
              </template>

              <div v-if="loadingWeknoraConfig" class="text-center py-8">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <el-form v-else :model="weknoraConfigForm" label-position="top" class="max-w-2xl">
                <!-- 启用开关 -->
                <el-form-item label="启用 WeKnora">
                  <div class="flex items-center gap-4">
                    <el-switch v-model="weknoraConfigForm.enabled" />
                    <span class="text-sm text-gray-500">
                      {{ weknoraConfigForm.enabled ? '已启用 - 文档将同步到知识库' : '已禁用 - 仅使用本地存储' }}
                    </span>
                  </div>
                </el-form-item>

                <!-- API 基础地址 -->
                <el-form-item label="API 基础地址">
                  <el-input
                    v-model="weknoraConfigForm.baseUrl"
                    placeholder="http://localhost:8080/api/v1"
                    :disabled="!weknoraConfigForm.enabled"
                  >
                    <template #prepend>URL</template>
                  </el-input>
                  <div class="text-xs text-gray-400 mt-1">
                    WeKnora 服务的 API 地址，末尾不要加斜杠
                  </div>
                </el-form-item>

                <!-- API Key -->
                <el-form-item label="API 密钥">
                  <div class="flex gap-2 w-full">
                    <el-input
                      v-model="weknoraConfigForm.apiKey"
                      :type="showWeknoraApiKey ? 'text' : 'password'"
                      placeholder="sk-xxxxxx"
                      class="flex-1"
                      :disabled="!weknoraConfigForm.enabled"
                    >
                      <template #prepend>Key</template>
                      <template #suffix>
                        <el-icon
                          class="cursor-pointer"
                          @click="showWeknoraApiKey = !showWeknoraApiKey"
                        >
                          <View v-if="showWeknoraApiKey" />
                          <Hide v-else />
                        </el-icon>
                      </template>
                    </el-input>
                    <el-button
                      :loading="testingWeknoraConnection"
                      :disabled="!weknoraConfigForm.enabled"
                      @click="handleTestWeknoraConnection"
                    >
                      <el-icon class="mr-1"><Connection /></el-icon>
                      测试连接
                    </el-button>
                  </div>
                  <div v-if="weknoraConnectionTestResult" class="mt-2">
                    <el-alert
                      :type="weknoraConnectionTestResult.success ? 'success' : 'error'"
                      :closable="false"
                      show-icon
                    >
                      <template #title>
                        {{ weknoraConnectionTestResult.success ? '连接成功' : '连接失败' }}
                        <span class="text-gray-500 ml-2">
                          ({{ weknoraConnectionTestResult.responseTime }}ms)
                        </span>
                      </template>
                      <template #default>
                        {{ weknoraConnectionTestResult.message }}
                      </template>
                    </el-alert>
                  </div>
                </el-form-item>

                <!-- 知识库 ID -->
                <el-form-item label="默认知识库 ID">
                  <el-input
                    v-model="weknoraConfigForm.knowledgeBaseId"
                    placeholder="请输入知识库 ID"
                    :disabled="!weknoraConfigForm.enabled"
                  />
                  <div class="text-xs text-gray-400 mt-1">
                    CRM 文档将上传到此知识库，可从 WeKnora 管理界面获取
                  </div>
                </el-form-item>

                <!-- 搜索配置 -->
                <el-divider content-position="left">搜索配置</el-divider>

                <!-- 最大匹配数 -->
                <el-form-item label="最大匹配结果数">
                  <el-input-number
                    v-model="weknoraConfigForm.matchCount"
                    :min="1"
                    :max="50"
                    :disabled="!weknoraConfigForm.enabled"
                    class="w-full"
                  />
                  <div class="text-xs text-gray-400 mt-1">
                    语义搜索返回的最大文档片段数量
                  </div>
                </el-form-item>

                <!-- 向量相似度阈值 -->
                <el-form-item label="向量相似度阈值">
                  <div class="flex items-center gap-4 w-full">
                    <el-slider
                      v-model="weknoraConfigForm.vectorThreshold"
                      :min="0"
                      :max="1"
                      :step="0.05"
                      :format-tooltip="(val: number) => val.toFixed(2)"
                      :disabled="!weknoraConfigForm.enabled"
                      class="flex-1"
                    />
                    <el-input-number
                      v-model="weknoraConfigForm.vectorThreshold"
                      :min="0"
                      :max="1"
                      :step="0.05"
                      :precision="2"
                      :disabled="!weknoraConfigForm.enabled"
                      class="w-24"
                    />
                  </div>
                  <div class="text-xs text-gray-400 mt-1">
                    只返回相似度高于此阈值的结果，值越高匹配越精确
                  </div>
                </el-form-item>

                <!-- 自动 RAG 开关 -->
                <el-form-item label="自动 RAG">
                  <div class="flex items-center gap-4">
                    <el-switch
                      v-model="weknoraConfigForm.autoRagEnabled"
                      :disabled="!weknoraConfigForm.enabled"
                    />
                    <span class="text-sm text-gray-500">
                      {{ weknoraConfigForm.autoRagEnabled ? '启用 - 每次对话自动检索相关文档' : '禁用 - 需手动触发知识库搜索' }}
                    </span>
                  </div>
                </el-form-item>

                <!-- 操作按钮 -->
                <div class="flex gap-3 pt-4 border-t border-gray-200">
                  <el-button
                    type="primary"
                    :loading="savingWeknoraConfig"
                    @click="handleSaveWeknoraConfig"
                  >
                    <el-icon class="mr-1"><Document /></el-icon>
                    保存配置
                  </el-button>
                  <el-button @click="loadWeknoraConfig">重置</el-button>
                </div>
              </el-form>
            </el-card>

            <!-- 配置说明卡片 -->
            <el-card shadow="never" class="!border-gray-200">
              <template #header>
                <span class="font-medium">配置说明</span>
              </template>
              <div class="text-sm text-gray-600 space-y-2">
                <p><strong>WeKnora:</strong> 基于向量数据库的知识库服务，支持文档语义搜索和 RAG（检索增强生成）</p>
                <p><strong>自动 RAG:</strong> 启用后，AI 对话时会自动检索知识库中的相关文档，提供更准确的回答</p>
                <p><strong>相似度阈值:</strong> 用于过滤低相关性的搜索结果，建议设置在 0.5-0.7 之间</p>
              </div>
            </el-card>
          </div>

          <!-- Placeholder for other tabs -->
          <div v-else class="text-center py-16 text-gray-400">
            <el-icon :size="48" class="mb-4"><Tools /></el-icon>
            <p class="text-lg">功能开发中</p>
            <p class="text-sm mt-2">该模块正在建设中，敬请期待</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Agent Dialog -->
    <el-dialog
      v-model="showAgentDialog"
      :title="editingAgent ? '编辑智能体' : '添加智能体'"
      :width="isMobile ? '95%' : '500px'"
      :fullscreen="isMobile"
    >
      <el-form :model="agentForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="agentForm.label" placeholder="智能体名称" />
        </el-form-item>
        <el-form-item label="提示词">
          <el-input v-model="agentForm.prompt" type="textarea" :rows="4" placeholder="AI 提示词或快捷指令" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="agentForm.iconName" placeholder="图标名称（如 Promotion）" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="agentForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAgentDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveAgent">保存</el-button>
      </template>
    </el-dialog>

    <!-- Custom Field Dialog -->
    <el-dialog
      v-model="showFieldDialog"
      :title="editingField ? '编辑自定义字段' : '添加自定义字段'"
      :width="isMobile ? '95%' : '550px'"
      :fullscreen="isMobile"
    >
      <el-form :model="fieldForm" label-width="100px">
        <el-form-item label="字段标签" required>
          <el-input v-model="fieldForm.fieldLabel" placeholder="显示名称，如：合同类型" />
        </el-form-item>
        <el-form-item v-if="!editingField" label="字段标识" required>
          <el-input v-model="fieldForm.fieldName" placeholder="英文标识，如：contractType" />
          <div class="text-xs text-gray-400 mt-1">只能包含字母、数字、下划线，以字母开头</div>
        </el-form-item>
        <el-form-item v-if="!editingField" label="字段类型" required>
          <el-select v-model="fieldForm.fieldType" class="w-full" @change="handleFieldTypeChange">
            <el-option label="单行文本" value="text" />
            <el-option label="多行文本" value="textarea" />
            <el-option label="数字" value="number" />
            <el-option label="日期" value="date" />
            <el-option label="日期时间" value="datetime" />
            <el-option label="单选下拉" value="select" />
            <el-option label="多选下拉" value="multiselect" />
            <el-option label="开关" value="checkbox" />
          </el-select>
        </el-form-item>
        <el-form-item label="占位提示">
          <el-input v-model="fieldForm.placeholder" placeholder="输入框提示文字" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="fieldForm.defaultValue" placeholder="字段默认值" />
        </el-form-item>

        <!-- Options for select types -->
        <el-form-item v-if="['select', 'multiselect'].includes(fieldForm.fieldType)" label="选项配置">
          <div class="w-full space-y-2">
            <div v-for="(opt, idx) in fieldForm.options" :key="idx" class="flex gap-2">
              <el-input v-model="opt.value" placeholder="值" class="w-1/3" />
              <el-input v-model="opt.label" placeholder="显示文字" class="flex-1" />
              <el-button text type="danger" @click="fieldForm.options.splice(idx, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button text @click="fieldForm.options.push({ value: '', label: '' })">
              + 添加选项
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="必填">
          <el-switch v-model="fieldForm.isRequired" />
        </el-form-item>
        <el-form-item label="可搜索">
          <el-switch v-model="fieldForm.isSearchable" />
        </el-form-item>
        <el-form-item label="列表显示">
          <el-switch v-model="fieldForm.isShowInList" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFieldDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveField">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAgentStore } from '@/stores/agent'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, UserFilled, Key, MagicStick, Plus, Promotion, Edit, Delete, Loading, Grid,
  Setting, Bell, Coin, Connection, Share, TrendCharts, Document, Tools, View, Hide, Box, Link, Reading
} from '@element-plus/icons-vue'
import type { AiAgent } from '@/types/common'
import type { CustomField, EntityType, FieldType, FieldOption } from '@/types/customField'
import type { AiConfigUpdateBO, AiConnectionTestResult, AiProviderPreset, AiProvider, WeKnoraConfigUpdateBO, WeKnoraConnectionTestResult } from '@/types/systemConfig'
import {
  getFieldsByEntity,
  addCustomField,
  updateCustomField,
  deleteCustomField,
  enableCustomField,
  disableCustomField
} from '@/api/customField'
import { getLoginUserDetail, updateProfile, changePassword } from '@/api/auth'
import { getAiConfig, updateAiConfig, testAiConnection, getMinioConsoleUrl, getMinioSsoUrl, getWeKnoraConfig, updateWeKnoraConfig, testWeKnoraConnection } from '@/api/systemConfig'
import type { MinioConsoleConfig } from '@/types/systemConfig'

const userStore = useUserStore()
const agentStore = useAgentStore()
const { isMobile } = useResponsive()

// Tab state
const activeTab = ref('profile')

// Profile form
const savingProfile = ref(false)
const profileForm = reactive({
  realname: '',
  email: '',
  phone: '',
  department: '',
  position: ''
})

// Password form
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Agent state
const showAgentDialog = ref(false)
const submitting = ref(false)
const editingAgent = ref<AiAgent | null>(null)
const agentForm = reactive({
  label: '',
  prompt: '',
  iconName: 'Promotion',
  enabled: true
})

// Custom field state
const showFieldDialog = ref(false)
const activeEntityType = ref<EntityType>('customer')
const customFields = ref<CustomField[]>([])
const loadingFields = ref(false)
const editingField = ref<CustomField | null>(null)
const fieldForm = reactive({
  fieldLabel: '',
  fieldName: '',
  fieldType: 'text' as FieldType,
  placeholder: '',
  defaultValue: '',
  isRequired: false,
  isSearchable: false,
  isShowInList: true,
  options: [] as FieldOption[]
})

// Field type labels
const FIELD_TYPE_LABELS: Record<FieldType, string> = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  date: '日期',
  datetime: '日期时间',
  select: '单选下拉',
  multiselect: '多选下拉',
  checkbox: '开关'
}

// AI Config state
const showApiKey = ref(false)
const savingAiConfig = ref(false)
const testingConnection = ref(false)
const connectionTestResult = ref<AiConnectionTestResult | null>(null)
const aiConfigLoaded = ref(false)

const aiConfigForm = reactive<AiConfigUpdateBO & { updateTime?: string }>({
  provider: 'dashscope',
  apiUrl: '',
  apiKey: '',
  model: '',
  temperature: 0.7,
  maxTokens: 2048,
  updateTime: undefined
})

// MinIO Config state
const loadingMinioConfig = ref(false)
const minioConfig = reactive<MinioConsoleConfig>({
  enabled: false,
  consoleUrl: ''
})

// WeKnora Config state
const showWeknoraApiKey = ref(false)
const savingWeknoraConfig = ref(false)
const testingWeknoraConnection = ref(false)
const loadingWeknoraConfig = ref(false)
const weknoraConfigLoaded = ref(false)
const weknoraConnectionTestResult = ref<WeKnoraConnectionTestResult | null>(null)

const weknoraConfigForm = reactive<WeKnoraConfigUpdateBO & { updateTime?: string }>({
  enabled: false,
  baseUrl: '',
  apiKey: '',
  knowledgeBaseId: '',
  matchCount: 5,
  vectorThreshold: 0.5,
  autoRagEnabled: true,
  updateTime: undefined
})

// 预设的 AI 服务提供商
const providerPresets: AiProviderPreset[] = [
  {
    label: 'OpenAI',
    value: 'openai',
    baseUrl: 'https://api.openai.com/v1',
    models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-4', 'gpt-3.5-turbo']
  },
  {
    label: '阿里云 DashScope (通义千问)',
    value: 'dashscope',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/',
    models: ['qwen-max', 'qwen-plus', 'qwen-turbo', 'qwen-long']
  },
  {
    label: '自定义 OpenAI 兼容服务',
    value: 'custom',
    baseUrl: '',
    models: []
  }
]

// 当前提供商的模型列表
const currentProviderModels = computed(() => {
  const preset = providerPresets.find(p => p.value === aiConfigForm.provider)
  return preset?.models || []
})

function getFieldTypeLabel(type: FieldType): string {
  return FIELD_TYPE_LABELS[type] || type
}

onMounted(async () => {
  // Load user detail info from API
  try {
    const detail = await getLoginUserDetail()
    profileForm.realname = detail.realname || ''
    profileForm.email = detail.email || ''
    profileForm.phone = detail.mobile || ''
    profileForm.position = detail.post || ''
  } catch {
    // Fallback to store data
    const info = userStore.userInfo as any
    profileForm.realname = userStore.realname || ''
    profileForm.email = info?.email || ''
    profileForm.phone = info?.mobile || ''
    profileForm.position = info?.post || ''
  }

  agentStore.fetchAllAgents()
  loadCustomFields()

  // 预加载 MinIO 配置，确保切换到对象存储 Tab 时数据已就绪
  loadMinioConfig()
})

// Profile methods
function resetProfileForm() {
  const info = userStore.userInfo as any
  profileForm.realname = userStore.realname || ''
  profileForm.email = info?.email || ''
  profileForm.phone = info?.mobile || ''
  profileForm.department = ''
  profileForm.position = info?.post || ''
}

async function handleSaveProfile() {
  savingProfile.value = true
  try {
    await updateProfile({
      realname: profileForm.realname,
      mobile: profileForm.phone
    })
    await userStore.fetchUserInfo()
    ElMessage.success('个人资料保存成功')
  } catch {
    // Error handled by interceptor
  } finally {
    savingProfile.value = false
  }
}

// Password methods
async function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('密码修改成功')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

// Custom field methods
async function loadCustomFields() {
  loadingFields.value = true
  try {
    customFields.value = await getFieldsByEntity(activeEntityType.value)
  } catch {
    // Error handled by interceptor
  } finally {
    loadingFields.value = false
  }
}

function handleOpenFieldDialog() {
  editingField.value = null
  resetFieldForm()
  showFieldDialog.value = true
}

function handleEditField(field: CustomField) {
  editingField.value = field
  Object.assign(fieldForm, {
    fieldLabel: field.fieldLabel,
    fieldName: field.fieldName,
    fieldType: field.fieldType,
    placeholder: field.placeholder || '',
    defaultValue: field.defaultValue || '',
    isRequired: field.isRequired,
    isSearchable: field.isSearchable,
    isShowInList: field.isShowInList,
    options: field.options ? [...field.options] : []
  })
  showFieldDialog.value = true
}

function handleFieldTypeChange(newType: FieldType) {
  if (!['select', 'multiselect'].includes(newType)) {
    fieldForm.options = []
  } else if (fieldForm.options.length === 0) {
    fieldForm.options = [{ value: '', label: '' }]
  }
}

async function handleSaveField() {
  if (!fieldForm.fieldLabel.trim()) {
    ElMessage.warning('请输入字段标签')
    return
  }
  if (!editingField.value && !fieldForm.fieldName.trim()) {
    ElMessage.warning('请输入字段标识')
    return
  }
  if (!editingField.value && !/^[a-zA-Z][a-zA-Z0-9_]*$/.test(fieldForm.fieldName)) {
    ElMessage.warning('字段标识只能包含字母数字下划线，且以字母开头')
    return
  }

  if (['select', 'multiselect'].includes(fieldForm.fieldType)) {
    const validOptions = fieldForm.options.filter(o => o.value.trim() && o.label.trim())
    if (validOptions.length === 0) {
      ElMessage.warning('请至少添加一个有效选项')
      return
    }
    fieldForm.options = validOptions
  }

  submitting.value = true
  try {
    if (editingField.value) {
      await updateCustomField({
        fieldId: editingField.value.fieldId,
        fieldLabel: fieldForm.fieldLabel,
        placeholder: fieldForm.placeholder || undefined,
        defaultValue: fieldForm.defaultValue || undefined,
        isRequired: fieldForm.isRequired,
        isSearchable: fieldForm.isSearchable,
        isShowInList: fieldForm.isShowInList,
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段更新成功')
    } else {
      await addCustomField({
        entityType: activeEntityType.value,
        fieldName: fieldForm.fieldName,
        fieldLabel: fieldForm.fieldLabel,
        fieldType: fieldForm.fieldType,
        placeholder: fieldForm.placeholder || undefined,
        defaultValue: fieldForm.defaultValue || undefined,
        isRequired: fieldForm.isRequired,
        isSearchable: fieldForm.isSearchable,
        isShowInList: fieldForm.isShowInList,
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段添加成功')
    }
    showFieldDialog.value = false
    resetFieldForm()
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleToggleFieldStatus(field: CustomField, enabled: boolean) {
  try {
    if (enabled) {
      await enableCustomField(field.fieldId)
    } else {
      await disableCustomField(field.fieldId)
    }
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  }
}

async function handleDeleteField(field: CustomField) {
  try {
    await deleteCustomField(field.fieldId)
    ElMessage.success('字段删除成功')
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  }
}

function resetFieldForm() {
  editingField.value = null
  Object.assign(fieldForm, {
    fieldLabel: '',
    fieldName: '',
    fieldType: 'text',
    placeholder: '',
    defaultValue: '',
    isRequired: false,
    isSearchable: false,
    isShowInList: true,
    options: []
  })
}

// Agent methods
function handleEditAgent(agent: AiAgent) {
  editingAgent.value = agent
  Object.assign(agentForm, {
    label: agent.label,
    prompt: agent.prompt,
    iconName: agent.iconName,
    enabled: !!agent.enabled
  })
  showAgentDialog.value = true
}

async function handleDeleteAgent(agent: AiAgent) {
  try {
    await ElMessageBox.confirm(`确定要删除智能体「${agent.label}」吗？`, '提示', { type: 'warning' })
    await agentStore.removeAgent(agent.agentId)
    ElMessage.success('删除成功')
  } catch {
    // Cancelled
  }
}

async function handleToggleAgent(agentId: string, newEnabled: boolean) {
  await agentStore.toggleAgentEnabled(agentId, newEnabled)
}

async function handleSaveAgent() {
  if (!agentForm.label || !agentForm.prompt) {
    ElMessage.warning('请填写名称和提示词')
    return
  }

  submitting.value = true
  try {
    if (editingAgent.value) {
      await agentStore.editAgent({
        ...agentForm,
        agentId: editingAgent.value.agentId
      } as any)
      ElMessage.success('更新成功')
    } else {
      await agentStore.createAgent(agentForm as any)
      ElMessage.success('创建成功')
    }
    showAgentDialog.value = false
    resetAgentForm()
  } finally {
    submitting.value = false
  }
}

function resetAgentForm() {
  editingAgent.value = null
  Object.assign(agentForm, { label: '', prompt: '', iconName: 'Promotion', enabled: true })
}

// AI Config methods
function handleProviderChange(provider: AiProvider) {
  const preset = providerPresets.find(p => p.value === provider)
  if (preset) {
    aiConfigForm.apiUrl = preset.baseUrl
    if (preset.models.length > 0) {
      aiConfigForm.model = preset.models[0]
    }
  }
  connectionTestResult.value = null
}

async function loadAiConfig() {
  try {
    const config = await getAiConfig()
    Object.assign(aiConfigForm, {
      provider: (config.provider || 'dashscope') as AiProvider,
      apiUrl: config.apiUrl || '',
      apiKey: '', // API Key 不回显完整值，需要用户重新输入修改
      model: config.model || '',
      temperature: config.temperature ?? 0.7,
      maxTokens: config.maxTokens ?? 2048,
      updateTime: config.updateTime
    })
    connectionTestResult.value = null
    aiConfigLoaded.value = true
  } catch {
    // Error handled by interceptor
  }
}

async function handleTestConnection() {
  if (!aiConfigForm.apiUrl || !aiConfigForm.apiKey) {
    ElMessage.warning('请先填写 API 地址和密钥')
    return
  }

  testingConnection.value = true
  connectionTestResult.value = null

  try {
    const result = await testAiConnection({
      provider: aiConfigForm.provider,
      apiUrl: aiConfigForm.apiUrl,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens
    })
    connectionTestResult.value = result
  } catch (error: any) {
    connectionTestResult.value = {
      success: false,
      responseTime: 0,
      message: error.message || '连接测试失败'
    }
  } finally {
    testingConnection.value = false
  }
}

async function handleSaveAiConfig() {
  if (!aiConfigForm.apiUrl) {
    ElMessage.warning('请填写 API 地址')
    return
  }
  if (!aiConfigForm.apiKey) {
    ElMessage.warning('请填写 API 密钥')
    return
  }
  if (!aiConfigForm.model) {
    ElMessage.warning('请选择或输入模型名称')
    return
  }

  savingAiConfig.value = true
  try {
    await updateAiConfig({
      provider: aiConfigForm.provider,
      apiUrl: aiConfigForm.apiUrl,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens
    })
    ElMessage.success('AI 配置保存成功，已立即生效')
    await loadAiConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingAiConfig.value = false
  }
}

function formatTime(time: string | undefined): string {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

// 监听 tab 切换，懒加载 AI 配置
watch(activeTab, async (newTab) => {
  if (newTab === 'api' && !aiConfigLoaded.value) {
    await loadAiConfig()
  }
  if (newTab === 'storage') {
    await loadMinioConfig()
  }
  if (newTab === 'weknora' && !weknoraConfigLoaded.value) {
    await loadWeknoraConfig()
  }
})

// MinIO methods
async function loadMinioConfig() {
  loadingMinioConfig.value = true
  try {
    const config = await getMinioConsoleUrl()
    console.log('MinIO config loaded:', config)
    Object.assign(minioConfig, config)
  } catch (error) {
    console.error('Failed to load MinIO config:', error)
  } finally {
    loadingMinioConfig.value = false
  }
}

async function handleOpenMinioConsole() {
  if (!minioConfig.enabled) {
    ElMessage.warning('MinIO 未启用')
    return
  }

  try {
    // 获取带 session_token 的 SSO URL
    const { ssoUrl } = await getMinioSsoUrl()
    if (!ssoUrl) {
      ElMessage.warning('无法获取 SSO 登录地址')
      return
    }
    const win = window.open(ssoUrl, '_blank')
    if (!win) {
      ElMessage.warning('浏览器阻止了弹窗，请允许弹窗后重试')
    }
  } catch (error) {
    console.error('Failed to get SSO URL:', error)
    ElMessage.error('获取 SSO 登录地址失败')
  }
}

// WeKnora methods
async function loadWeknoraConfig() {
  loadingWeknoraConfig.value = true
  try {
    const config = await getWeKnoraConfig()
    Object.assign(weknoraConfigForm, {
      enabled: config.enabled ?? false,
      baseUrl: config.baseUrl || '',
      apiKey: '', // API Key 不回显完整值，需要用户重新输入修改
      knowledgeBaseId: config.knowledgeBaseId || '',
      matchCount: config.matchCount ?? 5,
      vectorThreshold: config.vectorThreshold ?? 0.5,
      autoRagEnabled: config.autoRagEnabled ?? true,
      updateTime: config.updateTime
    })
    weknoraConnectionTestResult.value = null
    weknoraConfigLoaded.value = true
  } catch {
    // Error handled by interceptor
  } finally {
    loadingWeknoraConfig.value = false
  }
}

async function handleTestWeknoraConnection() {
  if (!weknoraConfigForm.baseUrl || !weknoraConfigForm.apiKey) {
    ElMessage.warning('请先填写 API 地址和密钥')
    return
  }

  testingWeknoraConnection.value = true
  weknoraConnectionTestResult.value = null

  try {
    const result = await testWeKnoraConnection({
      enabled: weknoraConfigForm.enabled,
      baseUrl: weknoraConfigForm.baseUrl,
      apiKey: weknoraConfigForm.apiKey,
      knowledgeBaseId: weknoraConfigForm.knowledgeBaseId,
      matchCount: weknoraConfigForm.matchCount,
      vectorThreshold: weknoraConfigForm.vectorThreshold,
      autoRagEnabled: weknoraConfigForm.autoRagEnabled
    })
    weknoraConnectionTestResult.value = result
  } catch (error: any) {
    weknoraConnectionTestResult.value = {
      success: false,
      responseTime: 0,
      message: error.message || '连接测试失败'
    }
  } finally {
    testingWeknoraConnection.value = false
  }
}

async function handleSaveWeknoraConfig() {
  savingWeknoraConfig.value = true
  try {
    await updateWeKnoraConfig({
      enabled: weknoraConfigForm.enabled,
      baseUrl: weknoraConfigForm.baseUrl,
      apiKey: weknoraConfigForm.apiKey || undefined,
      knowledgeBaseId: weknoraConfigForm.knowledgeBaseId,
      matchCount: weknoraConfigForm.matchCount,
      vectorThreshold: weknoraConfigForm.vectorThreshold,
      autoRagEnabled: weknoraConfigForm.autoRagEnabled
    })
    ElMessage.success('WeKnora 配置保存成功')
    await loadWeknoraConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingWeknoraConfig.value = false
  }
}
</script>

<style scoped>
.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  padding: 0 24px;
}

.settings-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.settings-tabs :deep(.el-tabs__item) {
  height: 50px;
  line-height: 50px;
  padding: 0 16px;
}

.settings-tabs :deep(.el-tabs__content) {
  display: none;
}
</style>
