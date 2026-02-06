<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
    <div class="w-full max-w-md">
      <!-- Logo and Title -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900">AI CRM</h1>
        <p class="mt-2 text-gray-600">智能客户关系管理系统</p>
      </div>

      <!-- Login Card -->
      <el-card class="shadow-lg">
        <template #header>
          <span class="text-lg font-semibold">登录</span>
        </template>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="formData.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="w-full"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="text-center text-sm text-gray-500 mt-4">
          <p>测试账号: admin / 123456a</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { getOidcSessionToken } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    await userStore.login({
      username: formData.username,
      password: formData.password
    })

    ElMessage.success('登录成功')

    // Redirect to original page or home
    let redirect = (route.query.redirect as string) || '/'

    // 如果是 OIDC 授权 URL，需要先获取 session_token 并添加到 URL 中
    if (redirect.includes('/oauth2/authorize')) {
      try {
        const { sessionToken } = await getOidcSessionToken()
        // 将 session_token 添加到 URL 参数中
        const url = new URL(redirect)
        url.searchParams.set('session_token', sessionToken)
        redirect = url.toString()
      } catch (e) {
        console.error('Failed to get OIDC session token:', e)
      }
    }

    // 如果是完整的外部 URL（如 OIDC 授权回调），使用 window.location 跳转
    if (redirect.startsWith('http://') || redirect.startsWith('https://')) {
      window.location.href = redirect
    } else {
      router.push(redirect)
    }
  } catch (error) {
    console.error('Login error:', error)
    // Error is already handled in the interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.el-card {
  border-radius: 12px;
}
</style>
