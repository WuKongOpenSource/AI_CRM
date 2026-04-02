<template>
  <div class="space-y-6">
    <el-card shadow="never" class="!border-slate-200">
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
        <div class="flex items-center gap-4 p-4 bg-slate-50 rounded-lg">
          <div class="w-12 h-12 rounded-lg bg-orange-100 flex items-center justify-center">
            <el-icon :size="24" class="text-orange-500"><Box /></el-icon>
          </div>
          <div class="flex-1">
            <div class="font-medium">MinIO 管理控制台</div>
            <div class="text-sm text-slate-500 mt-1">
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

        <div class="text-sm text-slate-500">
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
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Link, Loading } from '@element-plus/icons-vue'
import { getMinioConsoleUrl, getMinioSsoUrl } from '@/api/systemConfig'
import type { MinioConsoleConfig } from '@/types/systemConfig'

const loadingMinioConfig = ref(false)
const minioConfig = reactive<MinioConsoleConfig>({
  enabled: false,
  consoleUrl: ''
})

onMounted(async () => {
  await loadMinioConfig()
})

async function loadMinioConfig() {
  loadingMinioConfig.value = true
  try {
    const config = await getMinioConsoleUrl()
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
  }
}
</script>
