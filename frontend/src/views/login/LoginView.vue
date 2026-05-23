<template>
  <div class="auth-page relative bg-slate-50">
    <!-- Background -->
    <div class="pointer-events-none absolute inset-0 overflow-hidden">
      <div class="absolute -left-24 -top-24 h-96 w-96 rounded-full bg-primary/5 blur-3xl" />
      <div class="absolute -bottom-24 -right-24 h-96 w-96 rounded-full bg-indigo-500/5 blur-3xl" />
    </div>

    <div class="auth-shell relative z-10">
      <div class="auth-card">
        <!-- Branding -->
        <div
          class="relative hidden flex-col justify-between overflow-hidden bg-slate-900 p-12 text-white lg:flex lg:w-[46%]"
        >
          <div class="pointer-events-none absolute inset-0 opacity-20">
            <div class="absolute left-1/4 top-1/4 h-64 w-64 rounded-full bg-primary blur-[100px]" />
            <div class="absolute bottom-1/4 right-1/4 h-64 w-64 rounded-full bg-indigo-500 blur-[100px]" />
          </div>

          <div class="relative z-10">
            <div class="mb-12 flex items-center gap-3">
              <div
                class="flex size-10 items-center justify-center overflow-hidden rounded-xl bg-white shadow-lg shadow-black/20 ring-1 ring-white/20"
              >
                <img
                  :src="logoImg"
                  alt="悟空AI CRM"
                  class="size-[1.65rem] object-contain"
                  width="26"
                  height="26"
                  decoding="async"
                />
              </div>
              <span class="text-xl font-bold tracking-tight">悟空AI CRM</span>
            </div>

            <div class="space-y-6">
              <h1 class="text-4xl font-bold leading-tight">
                赋能销售团队<br />
                <span class="text-white">开启智能管理新时代</span>
              </h1>
              <p class="max-w-md text-lg leading-relaxed text-slate-400">
                集成 AI 智能解析、自动化跟进与深度数据分析，助您轻松掌控每一个商机。
              </p>
            </div>
          </div>

          <div class="relative z-10 space-y-8">
            <div class="flex items-center gap-4">
              <div
                class="flex size-12 items-center justify-center rounded-2xl border border-white/10 bg-white/5"
              >
                <el-icon class="text-white" :size="24"><CircleCheck /></el-icon>
              </div>
              <div>
                <h4 class="text-sm font-bold">企业级安全保障</h4>
                <p class="text-xs text-slate-500">多重加密，守护您的核心客户资产</p>
              </div>
            </div>
            <div class="flex items-center gap-4">
              <div
                class="flex size-12 items-center justify-center rounded-2xl border border-white/10 bg-white/5"
              >
                <el-icon class="text-white" :size="24"><MagicStick /></el-icon>
              </div>
              <div>
                <h4 class="text-sm font-bold">AI 驱动的洞察</h4>
                <p class="text-xs text-slate-500">自动识别需求，智能生成跟进建议</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Forms -->
        <div class="auth-form-panel lg:w-[54%]">
          <div ref="formScrollRef" class="auth-form-scroll">
            <div class="auth-form-content">
              <!-- Mobile brand -->
              <div class="mb-8 flex items-center justify-center gap-3 lg:hidden">
                <img
                  :src="logoImg"
                  alt="悟空AI CRM"
                  class="size-10 rounded-xl bg-white object-contain p-1 shadow-lg shadow-slate-300/40 ring-1 ring-slate-200/80"
                />
                <span class="text-lg font-bold text-slate-900">悟空AI CRM</span>
              </div>

              <!-- 双层叠放 + 高度过渡；WebKit 上高度改为瞬时更新，避免布局动画卡顿 -->
              <div
                ref="stageRef"
                class="auth-form-stage"
                :class="{ 'auth-form-stage--instant-height': prefersInstantStageHeight }"
              >
                <div
                  ref="loginLayerRef"
                  class="auth-form-layer"
                  :class="{ 'auth-form-layer--active': isLogin }"
                  :aria-hidden="!isLogin"
                >
                  <template v-if="loginStep === 'credentials'">
                    <div class="mb-10">
                      <h2 class="mb-2 text-2xl font-bold text-slate-900">欢迎回来</h2>
                      <p class="text-sm text-slate-500">请输入您的账号信息以登录系统</p>
                    </div>

                    <el-form
                      ref="loginFormRef"
                      :model="loginForm"
                      :rules="loginRules"
                      class="auth-form space-y-5"
                      label-position="top"
                      hide-required-asterisk
                      @submit.prevent="handleLogin"
                    >
                      <el-form-item prop="username">
                        <template #label>
                          <span class="label-upper">用户名</span>
                        </template>
                        <el-input
                          v-model="loginForm.username"
                          size="large"
                          placeholder="请输入用户名"
                          class="auth-el-input"
                          @keyup.enter="handleLogin"
                        >
                          <template #prefix>
                            <el-icon class="text-slate-400"><User /></el-icon>
                          </template>
                        </el-input>
                      </el-form-item>

                      <el-form-item prop="password">
                        <template #label>
                          <span class="label-upper">密码</span>
                        </template>
                        <el-input
                          v-model="loginForm.password"
                          type="password"
                          size="large"
                          placeholder="••••••••"
                          show-password
                          class="auth-el-input"
                          @keyup.enter="handleLogin"
                        >
                          <template #prefix>
                            <el-icon class="text-slate-400"><Lock /></el-icon>
                          </template>
                        </el-input>
                      </el-form-item>

                      <div class="flex justify-end">
                        <button
                          type="button"
                          class="text-sm font-medium text-primary transition-colors hover:text-primary/80 hover:underline"
                          @click="openForgotPasswordDialog"
                        >
                          忘记密码？
                        </button>
                      </div>

                      <el-form-item class="!mb-0">
                        <button
                          type="button"
                          class="group flex w-full items-center justify-center gap-2 rounded-2xl bg-primary py-4 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                          :disabled="loading"
                          @click="handleLogin"
                        >
                          <span
                            v-if="loading"
                            class="size-5 animate-spin rounded-full border-2 border-white/30 border-t-white"
                          />
                          <template v-else>
                            立即登录
                            <el-icon class="transition-transform group-hover:translate-x-1"><Right /></el-icon>
                          </template>
                        </button>
                      </el-form-item>
                    </el-form>
                  </template>

                  <div v-else class="tenant-selection">
                    <button type="button" class="tenant-selection__back" @click="handleBackToCredentials">
                      <el-icon :size="16"><ArrowLeft /></el-icon>
                      返回上一步
                    </button>

                    <div class="tenant-selection__hero">
                      <span class="tenant-selection__badge">多企业账号</span>
                      <div class="space-y-2">
                        <h2 class="text-2xl font-bold text-slate-900">选择登录企业</h2>
                        <p class="text-sm leading-6 text-slate-500">
                          检测到当前账号密码同时匹配多个企业，请选择这次要进入的企业工作台。
                        </p>
                      </div>
                    </div>

                    <div class="tenant-selection__summary">
                      <div class="tenant-selection__summary-icon">
                        <el-icon :size="20"><User /></el-icon>
                      </div>
                      <div class="min-w-0">
                        <p class="tenant-selection__summary-label">当前登录账号</p>
                        <p class="tenant-selection__summary-value">{{ loginForm.username }}</p>
                      </div>
                    </div>

                    <div class="tenant-option-list">
                      <button
                        v-for="option in tenantOptions"
                        :key="option.tenantId"
                        type="button"
                        class="tenant-option-card"
                        :disabled="loading"
                        @click="handleTenantLogin(option)"
                      >
                        <div class="tenant-option-card__icon">
                          <el-icon :size="20"><OfficeBuilding /></el-icon>
                        </div>
                        <div class="tenant-option-card__content">
                          <div class="tenant-option-card__title-row">
                            <span class="tenant-option-card__title">{{ option.tenantName }}</span>
                            <span class="tenant-option-card__tag">可登录</span>
                          </div>
                          <p class="tenant-option-card__meta">
                            {{ option.tenantName || '当前企业' }}
                          </p>
                          <p class="tenant-option-card__hint">点击进入该企业工作台</p>
                        </div>
                        <span class="tenant-option-card__action">
                          <span
                            v-if="loading && pendingTenantId === option.tenantId"
                            class="size-5 animate-spin rounded-full border-2 border-primary/20 border-t-primary"
                          />
                          <el-icon v-else :size="18"><Right /></el-icon>
                        </span>
                      </button>
                    </div>
                  </div>
                </div>

                <div
                  ref="registerLayerRef"
                  class="auth-form-layer"
                  :class="{ 'auth-form-layer--active': !isLogin }"
                  :aria-hidden="isLogin"
                >
                  <div class="mb-10">
                    <h2 class="mb-2 text-2xl font-bold text-slate-900">开启您的智能 CRM 之旅</h2>
                    <p class="text-sm text-slate-500">填写以下信息完成注册</p>
                  </div>

                  <el-form
                    ref="registerFormRef"
                    :model="registerForm"
                    :rules="registerRules"
                    class="auth-form space-y-5"
                    label-position="top"
                    hide-required-asterisk
                    @submit.prevent="handleRegister"
                  >
                    <el-form-item prop="companyName">
                      <template #label>
                        <span class="label-upper">公司名称</span>
                      </template>
                      <el-input
                        v-model="registerForm.companyName"
                        size="large"
                        placeholder="请输入公司名称"
                        class="auth-el-input"
                      >
                        <template #prefix>
                          <el-icon class="text-slate-400"><OfficeBuilding /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item prop="realname">
                      <template #label>
                        <span class="label-upper">联系人姓名</span>
                      </template>
                      <el-input
                        v-model="registerForm.realname"
                        size="large"
                        placeholder="选填"
                        class="auth-el-input"
                      >
                        <template #prefix>
                          <el-icon class="text-slate-400"><User /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item prop="email">
                      <template #label>
                        <span class="label-upper">邮箱</span>
                      </template>
                      <el-input
                        v-model="registerForm.email"
                        size="large"
                        placeholder="用作登录账号"
                        class="auth-el-input"
                      >
                        <template #prefix>
                          <el-icon class="text-slate-400"><Message /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item prop="password">
                      <template #label>
                        <span class="label-upper">密码</span>
                      </template>
                      <el-input
                        v-model="registerForm.password"
                        type="password"
                        size="large"
                        placeholder="6-20 位"
                        show-password
                        class="auth-el-input"
                      >
                        <template #prefix>
                          <el-icon class="text-slate-400"><Lock /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item prop="confirmPassword">
                      <template #label>
                        <span class="label-upper">确认密码</span>
                      </template>
                      <el-input
                        v-model="registerForm.confirmPassword"
                        type="password"
                        size="large"
                        placeholder="请再次输入密码"
                        show-password
                        class="auth-el-input"
                        @keyup.enter="handleRegister"
                      >
                        <template #prefix>
                          <el-icon class="text-slate-400"><Lock /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>

                    <el-form-item prop="verificationCode">
                      <template #label>
                        <span class="label-upper">验证码</span>
                      </template>
                      <div class="flex w-full gap-3">
                        <el-input
                          v-model="registerForm.verificationCode"
                          size="large"
                          placeholder="请输入验证码"
                          class="auth-el-input flex-1"
                          @keyup.enter="handleRegister"
                        />
                        <button
                          type="button"
                          class="auth-send-code-btn shrink-0 px-4 text-sm font-medium text-slate-700 transition-colors hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
                          :disabled="sendingCode || countdown > 0"
                          @click="handleSendCode"
                        >
                          {{ sendCodeText }}
                        </button>
                      </div>
                    </el-form-item>

                    <el-form-item class="!mb-0">
                      <button
                        type="button"
                        class="group flex w-full items-center justify-center gap-2 rounded-2xl bg-primary py-4 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="registerLoading"
                        @click="handleRegister"
                      >
                        <span
                          v-if="registerLoading"
                          class="size-5 animate-spin rounded-full border-2 border-white/30 border-t-white"
                        />
                        <template v-else>
                          免费注册
                          <el-icon class="transition-transform group-hover:translate-x-1"><Right /></el-icon>
                        </template>
                      </button>
                    </el-form-item>
                  </el-form>
                </div>
              </div>

              <div class="mt-6 text-center">
                <p v-if="isLogin && loginStep === 'tenant-selection'" class="text-sm text-slate-500">
                  需要使用其他账号？
                  <button
                    type="button"
                    class="ml-2 font-bold text-primary hover:underline"
                    @click="handleBackToCredentials"
                  >
                    返回登录
                  </button>
                </p>
                <p v-else class="text-sm text-slate-500">
                  {{ isLogin ? '还没有账号？' : '已经有账号了？' }}
                  <button
                    type="button"
                    class="ml-2 font-bold text-primary hover:underline"
                    @click="toggleMode"
                  >
                    {{ isLogin ? '立即注册' : '返回登录' }}
                  </button>
                </p>
                <!-- <p v-if="isLogin" class="mt-3 text-xs text-slate-400">测试账号: admin / 123456a</p> -->
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showForgotPasswordDialog"
      title="找回密码"
      width="480px"
      destroy-on-close
      align-center
      @closed="handleForgotPasswordDialogClosed"
    >
      <div class="space-y-5">
        <p class="text-sm leading-6 text-slate-500">
          通过邮箱验证码重置登录密码。若该邮箱加入了多个企业，重置后会同步更新该邮箱下的所有账号密码。
        </p>

        <el-form
          ref="forgotPasswordFormRef"
          :model="forgotPasswordForm"
          :rules="forgotPasswordRules"
          class="auth-form space-y-5"
          label-position="top"
          hide-required-asterisk
          @submit.prevent="handleForgotPasswordReset"
        >
          <el-form-item prop="email">
            <template #label>
              <span class="label-upper">邮箱</span>
            </template>
            <el-input
              v-model="forgotPasswordForm.email"
              size="large"
              placeholder="请输入注册邮箱"
              class="auth-el-input"
            >
              <template #prefix>
                <el-icon class="text-slate-400"><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <template #label>
              <span class="label-upper">新密码</span>
            </template>
            <el-input
              v-model="forgotPasswordForm.password"
              type="password"
              size="large"
              placeholder="6-20 位新密码"
              show-password
              class="auth-el-input"
            >
              <template #prefix>
                <el-icon class="text-slate-400"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <template #label>
              <span class="label-upper">确认新密码</span>
            </template>
            <el-input
              v-model="forgotPasswordForm.confirmPassword"
              type="password"
              size="large"
              placeholder="请再次输入新密码"
              show-password
              class="auth-el-input"
              @keyup.enter="handleForgotPasswordReset"
            >
              <template #prefix>
                <el-icon class="text-slate-400"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="verificationCode">
            <template #label>
              <span class="label-upper">验证码</span>
            </template>
            <div class="flex w-full gap-3">
              <el-input
                v-model="forgotPasswordForm.verificationCode"
                size="large"
                placeholder="请输入验证码"
                class="auth-el-input flex-1"
                @keyup.enter="handleForgotPasswordReset"
              />
              <button
                type="button"
                class="auth-send-code-btn shrink-0 px-4 text-sm font-medium text-slate-700 transition-colors hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
                :disabled="forgotSendingCode || forgotCountdown > 0"
                @click="handleForgotSendCode"
              >
                {{ forgotSendCodeText }}
              </button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="showForgotPasswordDialog = false">取消</el-button>
          <el-button type="primary" :loading="forgotPasswordLoading" @click="handleForgotPasswordReset">
            重置密码
          </el-button>
        </div>
      </template>
    </el-dialog>

    <SliderCaptchaDialog v-model="showCaptchaDialog" @verified="handleCaptchaVerified" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ArrowLeft,
  CircleCheck,
  Lock,
  MagicStick,
  Message,
  OfficeBuilding,
  Right,
  User
} from '@element-plus/icons-vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import logoImg from '@/assets/images/logo.png'
import { getOidcSessionToken, register, resetPassword, sendEmailCode } from '@/api/auth'
import SliderCaptchaDialog from '@/components/auth/SliderCaptchaDialog.vue'
import type { LoginTenantOption, LoginType } from '@/types/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLogin = ref(true)
type EmailCodeScene = 'register' | 'reset-password'

const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const forgotPasswordFormRef = ref<FormInstance>()
const loading = ref(false)
const registerLoading = ref(false)
const forgotPasswordLoading = ref(false)
const sendingCode = ref(false)
const forgotSendingCode = ref(false)
const showCaptchaDialog = ref(false)
const showForgotPasswordDialog = ref(false)
const formScrollRef = ref<HTMLElement>()
const stageRef = ref<HTMLElement>()
const loginLayerRef = ref<HTMLElement>()
const registerLayerRef = ref<HTMLElement>()
const countdown = ref(0)
const forgotCountdown = ref(0)
const tenantOptions = ref<LoginTenantOption[]>([])
const loginStep = ref<'credentials' | 'tenant-selection'>('credentials')
const pendingTenantId = ref('')
const pendingEmailCodeScene = ref<EmailCodeScene | ''>('')
let countdownTimer: number | undefined
let forgotCountdownTimer: number | undefined

const reduceMotion =
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

function resolveLoginType(): LoginType {
  if (typeof window !== 'undefined' && window.innerWidth < 768) {
    return 'MOBILE'
  }
  return 'PC'
}

/** Safari / iOS 等对 height 逐帧插值开销大，仅保留 opacity + transform 过渡更顺 */
function isWebKitWithoutChromium(): boolean {
  if (typeof navigator === 'undefined') return false
  const ua = navigator.userAgent
  if (/Chrome|Chromium|CriOS|EdgA|EdgiOS|Edg\/|OPR\//i.test(ua)) return false
  return /AppleWebKit/i.test(ua)
}

const prefersInstantStageHeight = reduceMotion || isWebKitWithoutChromium()

function measureActiveLayerHeight(): number {
  const el = isLogin.value ? loginLayerRef.value : registerLayerRef.value
  if (!el) return 0
  return Math.ceil(el.scrollHeight)
}

async function syncStageHeight(animate: boolean) {
  await nextTick()
  await nextTick()
  const stage = stageRef.value
  if (!stage) return
  const next = measureActiveLayerHeight()
  if (next <= 0) return

  if (!animate || prefersInstantStageHeight) {
    stage.style.height = `${next}px`
    return
  }

  const current = stage.getBoundingClientRect().height
  const from = current > 0 ? current : next
  stage.style.height = `${from}px`
  void stage.offsetHeight
  stage.style.height = `${next}px`
}

function snapStageHeightForResize() {
  const stage = stageRef.value
  if (!stage) return
  const h = measureActiveLayerHeight()
  if (h <= 0) return
  const prevTransition = stage.style.transition
  stage.style.transition = 'none'
  stage.style.height = `${h}px`
  void stage.offsetHeight
  stage.style.transition = prevTransition
}

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  companyName: '',
  realname: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const forgotPasswordForm = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateForgotConfirmPassword = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== forgotPasswordForm.password) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const forgotPasswordRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateForgotConfirmPassword, trigger: 'blur' }
  ],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const sendCodeText = computed(() => {
  if (sendingCode.value) return '发送中...'
  if (countdown.value > 0) return `${countdown.value}s 后重试`
  return '发送验证码'
})

const forgotSendCodeText = computed(() => {
  if (forgotSendingCode.value) return '发送中...'
  if (forgotCountdown.value > 0) return `${forgotCountdown.value}s 后重试`
  return '发送验证码'
})

function resetTenantSelection(clearOptions = true) {
  loginStep.value = 'credentials'
  pendingTenantId.value = ''
  if (clearOptions) {
    tenantOptions.value = []
  }
}

watch(
  () => [route.name, route.query.register],
  ([name, registerQuery]) => {
    isLogin.value = name !== 'Register' && registerQuery !== '1' && registerQuery !== 'true'
    if (!isLogin.value) {
      resetTenantSelection()
    }
  },
  { immediate: true }
)

watch([isLogin, loginStep], async () => {
  await syncStageHeight(true)
  formScrollRef.value?.scrollTo({
    top: 0,
    behavior: prefersInstantStageHeight ? 'auto' : 'smooth'
  })
})

watch(
  () => [loginForm.username, loginForm.password],
  () => {
    resetTenantSelection()
  }
)

function toggleMode() {
  resetTenantSelection()
  showForgotPasswordDialog.value = false
  const nextIsLogin = !isLogin.value
  const rest = { ...route.query }
  delete rest.register
  router.replace({
    name: nextIsLogin ? 'Login' : 'Register',
    query: rest
  })
}

async function handleLogin() {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true
    pendingTenantId.value = ''

    const result = await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      loginType: resolveLoginType()
    })

    if (result.requiresTenantSelection) {
      tenantOptions.value = result.tenantOptions || []
      loginStep.value = 'tenant-selection'
      ElMessage.info('请选择要登录的企业')
      return
    }

    await completeLoginRedirect()
  } catch (error) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}

function openForgotPasswordDialog() {
  Object.assign(forgotPasswordForm, {
    email: loginForm.username.trim(),
    password: '',
    confirmPassword: '',
    verificationCode: ''
  })
  showForgotPasswordDialog.value = true
  nextTick(() => {
    forgotPasswordFormRef.value?.clearValidate()
  })
}

function handleForgotPasswordDialogClosed() {
  pendingEmailCodeScene.value = ''
  Object.assign(forgotPasswordForm, {
    email: '',
    password: '',
    confirmPassword: '',
    verificationCode: ''
  })
  forgotPasswordFormRef.value?.clearValidate()
}

function handleBackToCredentials() {
  resetTenantSelection(false)
}

async function handleTenantLogin(option: LoginTenantOption) {
  try {
    loading.value = true
    pendingTenantId.value = option.tenantId

    const result = await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      tenantId: option.tenantId,
      loginType: resolveLoginType()
    })

    if (result.requiresTenantSelection) {
      tenantOptions.value = result.tenantOptions || []
      loginStep.value = 'tenant-selection'
      ElMessage.info('请选择要登录的企业')
      return
    }

    await completeLoginRedirect()
  } catch (error) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
    pendingTenantId.value = ''
  }
}

async function completeLoginRedirect() {
  ElMessage.success('登录成功')

  let redirect = (route.query.redirect as string) || '/'

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

  if (redirect.startsWith('http://') || redirect.startsWith('https://')) {
    window.location.href = redirect
  } else {
    router.push(redirect)
  }
}

async function handleRegister() {
  if (!registerFormRef.value) return

  try {
    await registerFormRef.value.validate()
    registerLoading.value = true

    await register({
      companyName: registerForm.companyName.trim(),
      realname: registerForm.realname.trim() || undefined,
      email: registerForm.email.trim(),
      password: registerForm.password,
      verificationCode: registerForm.verificationCode.trim()
    })

    ElMessage.success('注册成功，请登录')
    isLogin.value = true
    Object.assign(registerForm, {
      companyName: '',
      realname: '',
      email: '',
      password: '',
      confirmPassword: '',
      verificationCode: ''
    })
    registerFormRef.value?.resetFields()
    const rest = { ...route.query }
    delete rest.register
    router.replace({ name: 'Login', query: rest })
  } catch (error) {
    console.error('Register error:', error)
  } finally {
    registerLoading.value = false
  }
}

async function handleSendCode() {
  if (!registerFormRef.value || sendingCode.value || countdown.value > 0) return

  try {
    await registerFormRef.value.validateField('email')
    pendingEmailCodeScene.value = 'register'
    showCaptchaDialog.value = true
  } catch {
    return
  }
}

async function handleForgotSendCode() {
  if (!forgotPasswordFormRef.value || forgotSendingCode.value || forgotCountdown.value > 0) return

  try {
    await forgotPasswordFormRef.value.validateField('email')
    pendingEmailCodeScene.value = 'reset-password'
    showCaptchaDialog.value = true
  } catch {
    return
  }
}

async function handleCaptchaVerified(captchaVerification: string) {
  const currentScene = pendingEmailCodeScene.value
  pendingEmailCodeScene.value = ''

  if (currentScene === 'reset-password') {
    forgotSendingCode.value = true
    try {
      await sendEmailCode({
        email: forgotPasswordForm.email.trim(),
        type: 2,
        captchaVerification
      })
      ElMessage.success('找回密码验证码已发送，请查收邮箱')
      startCountdown('reset-password')
    } finally {
      forgotSendingCode.value = false
    }
    return
  }

  sendingCode.value = true
  try {
    await sendEmailCode({
      email: registerForm.email.trim(),
      type: 1,
      captchaVerification
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown('register')
  } finally {
    sendingCode.value = false
  }
}

async function handleForgotPasswordReset() {
  if (!forgotPasswordFormRef.value) return

  try {
    await forgotPasswordFormRef.value.validate()
    forgotPasswordLoading.value = true

    await resetPassword({
      email: forgotPasswordForm.email.trim(),
      password: forgotPasswordForm.password,
      verificationCode: forgotPasswordForm.verificationCode.trim()
    })

    loginForm.username = forgotPasswordForm.email.trim()
    loginForm.password = forgotPasswordForm.password
    resetTenantSelection()
    showForgotPasswordDialog.value = false
    ElMessage.success('密码已重置，请使用新密码登录')
  } catch (error) {
    console.error('Reset password error:', error)
  } finally {
    forgotPasswordLoading.value = false
  }
}

function startCountdown(scene: EmailCodeScene) {
  const isRegisterScene = scene === 'register'
  const countdownRef = isRegisterScene ? countdown : forgotCountdown

  countdownRef.value = 60

  if (isRegisterScene) {
    if (countdownTimer) {
      window.clearInterval(countdownTimer)
    }
    countdownTimer = window.setInterval(() => {
      countdownRef.value -= 1
      if (countdownRef.value <= 0 && countdownTimer) {
        window.clearInterval(countdownTimer)
        countdownTimer = undefined
      }
    }, 1000)
    return
  }

  if (forgotCountdownTimer) {
    window.clearInterval(forgotCountdownTimer)
  }
  forgotCountdownTimer = window.setInterval(() => {
    countdownRef.value -= 1
    if (countdownRef.value <= 0 && forgotCountdownTimer) {
      window.clearInterval(forgotCountdownTimer)
      forgotCountdownTimer = undefined
    }
  }, 1000)
}

onMounted(async () => {
  await syncStageHeight(false)
  window.addEventListener('resize', snapStageHeightForResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', snapStageHeightForResize)
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  if (forgotCountdownTimer) {
    window.clearInterval(forgotCountdownTimer)
  }
})
</script>

<style scoped>
.auth-page {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
}

.auth-shell {
  display: flex;
  min-height: 100%;
  align-items: center;
  justify-content: center;
  padding: clamp(1rem, 2vw, 2rem);
}

.auth-card {
  position: relative;
  display: flex;
  width: min(100%, 1040px);
  max-height: calc(100vh - clamp(2rem, 4vw, 3rem));
  margin-inline: auto;
  overflow: hidden;
  border: 1px solid rgb(241 245 249);
  border-radius: 32px;
  background: rgb(255 255 255);
  box-shadow: 0 28px 80px rgba(148, 163, 184, 0.22);
}

.auth-form-panel {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1 1 auto;
  background: rgb(255 255 255);
}

.auth-form-panel::before,
.auth-form-panel::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  z-index: 2;
  height: 28px;
  pointer-events: none;
}

.auth-form-panel::before {
  top: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0) 100%);
}

.auth-form-panel::after {
  bottom: 0;
  background: linear-gradient(0deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0) 100%);
}

.auth-form-scroll {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  scrollbar-gutter: stable;
}

.auth-form-content {
  width: min(100%, 456px);
  margin: 0 auto;
  padding: 2rem 1.5rem;
  /* 与主题变量对齐，深层样式仍用 box-shadow 覆盖 EP 默认 inset 边框 */
  --el-input-border-radius: 16px;
}

.auth-form-stage {
  position: relative;
  width: 100%;
  min-height: 200px;
  overflow: hidden;
  transition: height 0.52s cubic-bezier(0.25, 0.46, 0.45, 1);
}

.auth-form-stage--instant-height {
  transition: none;
}

/* 失活为终点时：尽快淡出，减轻与下一层叠字虚影 */
.auth-form-layer {
  position: absolute;
  z-index: 0;
  left: 0;
  right: 0;
  top: 0;
  width: 100%;
  opacity: 0;
  pointer-events: none;
  backface-visibility: hidden;
  isolation: isolate;
  transition:
    opacity 0.2s cubic-bezier(0.4, 0, 0.85, 1),
    transform 0.24s cubic-bezier(0.4, 0, 0.85, 1);
  transform: translate3d(0, 6px, 0);
}

/* 激活为终点时：略延迟再淡入，等上一层基本退干净 */
.auth-form-layer--active {
  z-index: 1;
  opacity: 1;
  pointer-events: auto;
  transform: translate3d(0, 0, 0);
  transition:
    opacity 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) 0.12s,
    transform 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) 0.12s;
}

@media (prefers-reduced-motion: reduce) {
  .auth-form-stage,
  .auth-form-stage--instant-height {
    transition: none;
  }

  .auth-form-layer,
  .auth-form-layer--active {
    transition: none;
    transform: none;
  }
}

.label-upper {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: rgb(148 163 184);
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.auth-form :deep(.el-form-item__label) {
  margin-bottom: 4px;
  line-height: 1.2;
}

.tenant-selection {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.tenant-selection__back {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 0.4rem;
  border: none;
  background: transparent;
  padding: 0;
  color: #64748b;
  font-size: 0.95rem;
  font-weight: 600;
  transition: color 0.2s ease;
}

.tenant-selection__back:hover {
  color: #0f172a;
}

.tenant-selection__hero {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.tenant-selection__badge {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(19, 127, 236, 0.12), rgba(15, 23, 42, 0.06));
  color: #137fec;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.tenant-selection__summary {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  padding: 1rem 1.1rem;
  border: 1px solid #e2e8f0;
  border-radius: 1.25rem;
  background: linear-gradient(180deg, #f8fbff 0%, #f8fafc 100%);
}

.tenant-selection__summary-icon {
  display: flex;
  width: 2.75rem;
  height: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  background: #ffffff;
  color: #137fec;
  box-shadow: 0 12px 30px rgba(19, 127, 236, 0.12);
}

.tenant-selection__summary-label {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #94a3b8;
}

.tenant-selection__summary-value {
  margin: 0.2rem 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-size: 1rem;
  font-weight: 700;
}

.tenant-option-list {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.tenant-option-card {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 0.95rem;
  border: 1px solid #e2e8f0;
  border-radius: 1.4rem;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  padding: 1rem 1.05rem;
  text-align: left;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.tenant-option-card:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(19, 127, 236, 0.35);
  box-shadow: 0 18px 35px rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f3f8ff 100%);
}

.tenant-option-card:disabled {
  cursor: not-allowed;
}

.tenant-option-card__icon {
  display: flex;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  background: #eaf4ff;
  color: #137fec;
}

.tenant-option-card__content {
  min-width: 0;
  flex: 1 1 auto;
}

.tenant-option-card__title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.tenant-option-card__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-size: 1rem;
  font-weight: 700;
}

.tenant-option-card__tag {
  flex: 0 0 auto;
  border-radius: 999px;
  background: rgba(22, 163, 74, 0.12);
  padding: 0.22rem 0.55rem;
  color: #15803d;
  font-size: 0.72rem;
  font-weight: 700;
}

.tenant-option-card__meta,
.tenant-option-card__hint {
  margin: 0;
}

.tenant-option-card__meta {
  margin-top: 0.35rem;
  color: #334155;
  font-size: 0.92rem;
}

.tenant-option-card__hint {
  margin-top: 0.25rem;
  color: #94a3b8;
  font-size: 0.82rem;
}

.tenant-option-card__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

/*
 * Element Plus 输入框用 inset box-shadow 模拟 1px 边框，普通 border 会叠两层或无效。
 * 这里统一改为：细 inset 描边 + 大圆角 + focus 外环（与设计稿 rounded-2xl + ring-primary/5 一致）
 */
.auth-form :deep(.el-input.auth-el-input .el-input__wrapper) {
  min-height: 46px;
  border-radius: 1rem !important;
  background-color: #f8fafc !important;
  border: none !important;
  box-shadow: 0 0 0 1px #e2e8f0 inset !important;
  padding-left: 12px !important;
  transition:
    box-shadow 0.2s ease,
    background-color 0.2s ease !important;
}

.auth-form :deep(.el-input.auth-el-input .el-input__wrapper:hover) {
  background-color: #f1f5f9 !important;
  box-shadow: 0 0 0 1px #cbd5e1 inset !important;
}

.auth-form :deep(.el-input.auth-el-input .el-input__wrapper.is-focus) {
  background-color: #fff !important;
  box-shadow:
    0 0 0 4px rgba(19, 127, 236, 0.06),
    0 0 0 1px #137fec inset !important;
}

.auth-send-code-btn {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  align-self: stretch;
  padding-inline: 1rem;
  border-radius: 1rem;
  border: 1px solid #e2e8f0;
  background-color: #fff;
  box-shadow: none;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.auth-send-code-btn:hover:not(:disabled) {
  background-color: #f8fafc;
}

@media (min-width: 1024px) {
  .auth-card {
    min-height: 620px;
  }

  .auth-form-content {
    padding: 3rem 3.5rem;
  }
}

@media (max-width: 1023px) {
  .auth-page {
    overflow: hidden;
  }

  .auth-shell {
    min-height: 100vh;
    height: 100vh;
    padding: 0;
    align-items: stretch;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .auth-card {
    flex-direction: column;
    max-height: none;
    min-height: 100%;
    border-radius: 0;
  }

  .auth-form-panel::before,
  .auth-form-panel::after {
    display: none;
  }

  .auth-form-content {
    width: 100%;
    max-width: 480px;
    padding-inline: 1.5rem;
  }

  .tenant-option-card {
    align-items: flex-start;
  }

  .tenant-option-card__title-row {
    flex-wrap: wrap;
  }
}
</style>
