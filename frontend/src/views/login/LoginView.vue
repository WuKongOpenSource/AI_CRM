<template>
  <div class="min-h-screen flex items-center justify-center bg-background-light">
    <div class="w-full max-w-md px-4">
      <!-- Logo and Title -->
      <div class="text-center mb-10">
        <img src="/logo.png" alt="AI CRM" class="size-16 rounded-2xl shadow-xl shadow-primary/20 mb-4 inline-block" />
        <h1 class="text-2xl font-bold text-slate-900">悟空AI CRM</h1>
        <p class="mt-1 text-sm text-slate-500">智能客户关系管理系统</p>
      </div>

      <!-- Login Card -->
      <div class="bg-white rounded-2xl border border-slate-200 shadow-xl p-8">
        <h2 class="text-lg font-bold text-slate-900 mb-6">登录</h2>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
          hide-required-asterisk
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="formData.username"
              placeholder="请输入用户名"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item class="mt-2">
            <button
              type="button"
              class="w-full bg-primary text-white py-2.5 rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 flex items-center justify-center gap-2 disabled:opacity-50"
              :disabled="loading"
              @click="handleLogin"
            >
              <span v-if="loading" class="size-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </el-form-item>
        </el-form>

        <div class="text-center text-sm text-slate-500 mt-6 space-y-2">
          <p>还没有账号？<router-link to="/register" class="text-primary font-medium hover:underline">立即注册</router-link></p>
          <p class="text-xs text-slate-400">测试账号: admin / 123456a</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
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
        const url = new URL(redirect)
        url.searchParams.set('session_token', sessionToken)
        redirect = url.toString()
      } catch (e) {
        console.error('Failed to get OIDC session token:', e)
      }
    }

    // 如果是完整的外部 URL，使用 window.location 跳转
    if (redirect.startsWith('http://') || redirect.startsWith('https://')) {
      window.location.href = redirect
    } else {
      router.push(redirect)
    }
  } catch (error) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}
</script>
