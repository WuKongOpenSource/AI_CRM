<template>
  <div class="auth-page relative bg-slate-50">
    <div class="pointer-events-none absolute inset-0 overflow-hidden">
      <div class="absolute -left-24 -top-24 h-96 w-96 rounded-full bg-primary/5 blur-3xl" />
      <div class="absolute -bottom-24 -right-24 h-96 w-96 rounded-full bg-indigo-500/5 blur-3xl" />
    </div>

    <div class="auth-shell relative z-10">
      <div class="auth-card">
        <div class="relative hidden flex-col justify-between overflow-hidden bg-slate-900 p-12 text-white lg:flex lg:w-[46%]">
          <div class="pointer-events-none absolute inset-0 opacity-20">
            <div class="absolute left-1/4 top-1/4 h-64 w-64 rounded-full bg-primary blur-[100px]" />
            <div class="absolute bottom-1/4 right-1/4 h-64 w-64 rounded-full bg-indigo-500 blur-[100px]" />
          </div>

          <div class="relative z-10">
            <div class="mb-12 flex items-center gap-3">
              <div class="flex size-10 items-center justify-center overflow-hidden rounded-xl bg-white shadow-lg shadow-black/20 ring-1 ring-white/20">
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
              <div class="flex size-12 items-center justify-center rounded-2xl border border-white/10 bg-white/5">
                <el-icon class="text-white" :size="24"><CircleCheck /></el-icon>
              </div>
              <div>
                <h4 class="text-sm font-bold">企业级安全保障</h4>
                <p class="text-xs text-slate-500">多重加密，守护您的核心客户资产</p>
              </div>
            </div>
            <div class="flex items-center gap-4">
              <div class="flex size-12 items-center justify-center rounded-2xl border border-white/10 bg-white/5">
                <el-icon class="text-white" :size="24"><MagicStick /></el-icon>
              </div>
              <div>
                <h4 class="text-sm font-bold">AI 驱动的洞察</h4>
                <p class="text-xs text-slate-500">自动识别需求，智能生成跟进建议</p>
              </div>
            </div>
          </div>
        </div>

        <div class="auth-form-panel lg:w-[54%]">
          <div ref="formScrollRef" class="auth-form-scroll">
            <div class="auth-form-content auth-form-content--with-fixed-consent">
              <div class="mb-8 flex items-center justify-center gap-3 lg:hidden">
                <img
                  :src="logoImg"
                  alt="悟空AI CRM"
                  class="size-10 rounded-xl bg-white object-contain p-1 shadow-lg shadow-slate-300/40 ring-1 ring-slate-200/80"
                />
                <span class="text-[1.5rem] font-bold text-slate-900">悟空AI CRM</span>
              </div>

              <div
                ref="stageRef"
                class="auth-form-stage"
                :class="{ 'auth-form-stage--instant-height': prefersInstantStageHeight }"
              >
                <div
                  ref="loginLayerRef"
                  class="auth-form-layer auth-form-layer--active"
                  aria-hidden="false"
                >
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
                        placeholder="请输入密码"
                        show-password
                        class="auth-el-input"
                        @keyup.enter="handleLogin"
                      >
                        <template #prefix>
                          <el-icon class="text-slate-400"><Lock /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>

                    <div class="desktop-agreement-consent">
                      <label class="desktop-agreement-consent__check">
                        <input
                          v-model="agreementAccepted"
                          type="checkbox"
                          aria-label="同意用户协议和隐私声明"
                        />
                        <span class="desktop-agreement-consent__box" aria-hidden="true">
                          <el-icon v-if="agreementAccepted" :size="13"><Check /></el-icon>
                        </span>
                      </label>
                      <p class="desktop-agreement-consent__text">
                        我已阅读并同意
                        <a :href="userAgreementHref">《用户协议》</a>
                        和
                        <a :href="privacyPolicyHref">《隐私声明》</a>
                      </p>
                    </div>

                    <el-form-item class="!mb-0">
                      <button
                        type="submit"
                        class="auth-login-submit group flex w-full items-center justify-center gap-2 rounded-2xl bg-primary text-[1rem] font-bold text-white transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="loading"
                      >
                        <span
                          v-if="loading"
                          class="inline-block size-4 shrink-0 animate-spin rounded-full border-2 border-white/30 border-t-white"
                        />
                        <template v-else>
                          立即登录
                          <el-icon class="transition-transform group-hover:translate-x-1"><Right /></el-icon>
                        </template>
                      </button>
                    </el-form-item>
                  </el-form>

                  <div v-if="enabledExternalProviders.length" class="external-auth-panel">
                    <div class="external-auth-divider">
                      <span>第三方登录</span>
                    </div>
                    <div class="external-auth-grid">
                      <button
                        v-for="provider in enabledExternalProviders"
                        :key="provider.provider"
                        type="button"
                        class="external-auth-btn"
                        :disabled="externalLoadingProvider === provider.provider"
                        @click="startExternalLogin(provider.provider)"
                      >
                        <span class="external-auth-btn__icon" aria-hidden="true">
                          <svg
                            v-if="provider.provider === 'google'"
                            viewBox="0 0 24 24"
                            focusable="false"
                          >
                            <path
                              fill="#4285f4"
                              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                            />
                            <path
                              fill="#34a853"
                              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C4 20.53 7.7 23 12 23z"
                            />
                            <path
                              fill="#fbbc05"
                              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                            />
                            <path
                              fill="#ea4335"
                              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 4 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                            />
                          </svg>
                          <span v-else-if="provider.provider === 'outlook'" class="external-auth-btn__microsoft-icon">
                            <span class="external-auth-btn__microsoft-pane external-auth-btn__microsoft-pane--red" />
                            <span class="external-auth-btn__microsoft-pane external-auth-btn__microsoft-pane--green" />
                            <span class="external-auth-btn__microsoft-pane external-auth-btn__microsoft-pane--blue" />
                            <span class="external-auth-btn__microsoft-pane external-auth-btn__microsoft-pane--yellow" />
                          </span>
                          <span v-else class="external-auth-btn__fallback">{{ providerMark(provider.provider) }}</span>
                        </span>
                        <span>{{ providerDisplayName(provider) }}</span>
                        <span
                          v-if="externalLoadingProvider === provider.provider"
                          class="size-4 shrink-0 animate-spin rounded-full border-2 border-slate-300 border-t-primary"
                        />
                      </button>
                    </div>
                  </div>

                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mobile-agreement-consent lg:hidden">
      <label class="mobile-agreement-consent__check">
        <input
          v-model="agreementAccepted"
          type="checkbox"
          aria-label="同意用户协议和隐私声明"
        />
        <span class="mobile-agreement-consent__box" aria-hidden="true">
          <el-icon v-if="agreementAccepted" :size="13"><Check /></el-icon>
        </span>
      </label>
      <p class="mobile-agreement-consent__text">
        点击登录即表示您已同意并接受
        <a :href="userAgreementHref">《用户协议》</a>
        和
        <a :href="privacyPolicyHref">《隐私声明》</a>
      </p>
    </div>

    <Teleport to="body">
      <div
        v-if="showAgreementDialog"
        class="agreement-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="agreement-modal-title"
      >
        <div class="agreement-modal__backdrop" aria-hidden="true" />
        <div class="agreement-modal__panel">
          <div class="agreement-modal__header">
            <span class="agreement-modal__icon" aria-hidden="true">
              <el-icon :size="22"><Lock /></el-icon>
            </span>
            <div class="agreement-modal__heading">
              <h2 id="agreement-modal-title" class="agreement-modal__title">用户协议与隐私保护</h2>
            </div>
          </div>
          <p class="agreement-modal__copy">
            感谢您选择悟空AI CRM。为保障您的个人权益，请先阅读并同意
            <a :href="agreementDialogUserAgreementHref">《悟空AI CRM用户协议》</a>
            与
            <a :href="agreementDialogPrivacyPolicyHref">《悟空AI CRM隐私声明》</a>
            ，了解我们对个人信息的收集、保存、使用、对外提供和保护方式。
          </p>
          <div class="agreement-modal__actions">
            <button
              type="button"
              class="agreement-modal__button agreement-modal__button--ghost"
              @click="handleAgreementReject"
            >
              拒绝
            </button>
            <button
              type="button"
              class="agreement-modal__button agreement-modal__button--primary"
              :disabled="loading"
              @click="handleAgreementAgreeLogin"
            >
              <span
                v-if="loading"
                class="inline-block size-4 shrink-0 animate-spin rounded-full border-2 border-white/30 border-t-white"
              />
              <span>同意并登录</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <SliderCaptchaDialog v-model="showCaptchaDialog" @verified="handleCaptchaVerified" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, CircleCheck, Lock, MagicStick, Right, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  exchangeExternalLoginTicket,
  getExternalAuthAuthorizeUrl,
  getExternalAuthProviders,
  getOidcSessionToken
} from '@/api/auth'
import logoImg from '@/assets/images/logo.png'
import SliderCaptchaDialog from '@/components/auth/SliderCaptchaDialog.vue'
import { useUserStore } from '@/stores/user'
import type { ExternalAuthProvider, ExternalAuthProviderCode } from '@/types/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const showCaptchaDialog = ref(false)
const showAgreementDialog = ref(false)
const formScrollRef = ref<HTMLElement>()
const stageRef = ref<HTMLElement>()
const loginLayerRef = ref<HTMLElement>()
const externalProviders = ref<ExternalAuthProvider[]>([])
const externalLoadingProvider = ref<ExternalAuthProviderCode | ''>('')
const externalLoginTicketProcessing = ref(false)

const LAST_LOGIN_USERNAME_STORAGE_KEY = 'wk_ai_crm:last_login_username:v1'
const AGREEMENT_ACCEPTED_STORAGE_KEY = 'wk_ai_crm:agreement_accepted:v1'
const AGREEMENT_DIALOG_QUERY_KEY = 'agreementDialog'
const prefersInstantStageHeight = prefersReducedMotion() || isWebKitWithoutChromium()

const loginForm = reactive({
  username: readLastLoginUsername(),
  password: ''
})

const agreementAccepted = ref(readAgreementAccepted())

const enabledExternalProviders = computed(() =>
  externalProviders.value.filter((provider) => provider.enabled && isSupportedExternalProvider(provider))
)
const userAgreementHref = computed(() => router.resolve({ name: 'UserAgreement' }).href)
const privacyPolicyHref = computed(() => router.resolve({ name: 'PrivacyPolicy' }).href)
const agreementDialogUserAgreementHref = computed(() =>
  router.resolve({
    name: 'UserAgreement',
    query: buildAgreementDialogReturnQuery()
  }).href
)
const agreementDialogPrivacyPolicyHref = computed(() =>
  router.resolve({
    name: 'PrivacyPolicy',
    query: buildAgreementDialogReturnQuery()
  }).href
)

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

onMounted(async () => {
  restoreAgreementDialogFromQuery()
  const handledExternalAuth = await handleExternalAuthQuery()
  if (!handledExternalAuth) {
    await loadExternalProviders()
  }
  await syncStageHeight(false)
  window.addEventListener('resize', snapStageHeightForResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', snapStageHeightForResize)
})

watch(agreementAccepted, (accepted) => {
  persistAgreementAccepted(accepted)
})

watch(
  () => enabledExternalProviders.value.length,
  async () => {
    await syncStageHeight(true)
  }
)

function readLastLoginUsername(): string {
  if (typeof window === 'undefined') return ''
  try {
    return window.localStorage.getItem(LAST_LOGIN_USERNAME_STORAGE_KEY)?.trim() || ''
  } catch {
    return ''
  }
}

function rememberSuccessfulLoginUsername() {
  const username = loginForm.username.trim()
  if (!username || typeof window === 'undefined') return
  try {
    window.localStorage.setItem(LAST_LOGIN_USERNAME_STORAGE_KEY, username)
  } catch {
    // Ignore storage failures.
  }
}

function readAgreementAccepted(): boolean {
  if (typeof window === 'undefined') return false
  try {
    return window.localStorage.getItem(AGREEMENT_ACCEPTED_STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

function persistAgreementAccepted(accepted: boolean) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(AGREEMENT_ACCEPTED_STORAGE_KEY, accepted ? '1' : '0')
  } catch {
    // Ignore storage failures.
  }
}

function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function isWebKitWithoutChromium(): boolean {
  if (typeof navigator === 'undefined') return false
  const ua = navigator.userAgent || ''
  if (/Chrome|Chromium|CriOS|EdgA|EdgiOS|Edg\/|OPR\//i.test(ua)) return false
  return /AppleWebKit/i.test(ua)
}

function measureActiveLayerHeight(): number {
  const el = loginLayerRef.value
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
  formScrollRef.value?.scrollTo({
    top: 0,
    behavior: prefersInstantStageHeight ? 'auto' : 'smooth'
  })
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

function isSupportedExternalProvider(provider: ExternalAuthProvider): boolean {
  return provider.provider === 'google' || provider.provider === 'wechat' || provider.provider === 'outlook'
}

function providerMark(provider: ExternalAuthProviderCode): string {
  if (provider === 'google') return 'G'
  if (provider === 'wechat') return '微'
  if (provider === 'outlook') return 'O'
  return '?'
}

function providerDisplayName(provider: ExternalAuthProvider): string {
  if (provider.provider === 'google') return 'Google'
  if (provider.provider === 'wechat') return '微信'
  if (provider.provider === 'outlook') return 'Microsoft'
  return provider.name
}

async function loadExternalProviders() {
  try {
    externalProviders.value = await getExternalAuthProviders()
  } catch (error) {
    console.error('Load external auth providers failed:', error)
    externalProviders.value = []
  }
}

function buildExternalAuthRedirect(): string {
  const query: Record<string, string> = {}
  if (typeof route.query.redirect === 'string') {
    query.redirect = route.query.redirect
  }
  const resolved = router.resolve({ name: 'Login', query })
  return `${window.location.origin}${window.location.pathname}${window.location.search}${resolved.href}`
}

async function startExternalLogin(provider: ExternalAuthProviderCode) {
  externalLoadingProvider.value = provider
  try {
    const { authorizeUrl } = await getExternalAuthAuthorizeUrl(provider, buildExternalAuthRedirect())
    window.location.href = authorizeUrl
  } catch (error) {
    console.error('Start external login failed:', error)
  } finally {
    externalLoadingProvider.value = ''
  }
}

async function handleExternalAuthQuery(): Promise<boolean> {
  const externalError = typeof route.query.externalAuthError === 'string' ? route.query.externalAuthError : ''
  if (externalError) {
    const provider = typeof route.query.provider === 'string' ? route.query.provider : ''
    const providerName = provider === 'wechat'
      ? '微信'
      : provider === 'google'
        ? 'Google'
        : provider === 'outlook'
          ? 'Microsoft'
          : '第三方'
    const messageMap: Record<string, string> = {
      unbound: `当前${providerName}账号尚未绑定本地用户`,
      invalid_state: '第三方登录状态已过期，请重新登录',
      denied: '第三方登录已取消',
      failed: '第三方登录失败'
    }
    ElMessage.error(messageMap[externalError] || `${providerName}登录失败`)
    await clearExternalAuthQuery()
    return true
  }

  const loginTicket = typeof route.query.externalLoginTicket === 'string' ? route.query.externalLoginTicket : ''
  if (!loginTicket) {
    return false
  }

  if (externalLoginTicketProcessing.value) {
    return true
  }

  externalLoginTicketProcessing.value = true
  loading.value = true
  const redirectValue = route.query.redirect
  await clearExternalAuthQuery()

  try {
    const result = await exchangeExternalLoginTicket({ ticket: loginTicket })
    await userStore.applyLoginResult(result)
    await completeLoginRedirect(redirectValue)
  } catch (error) {
    console.error('External login ticket exchange failed:', error)
  } finally {
    loading.value = false
    externalLoginTicketProcessing.value = false
  }
  return true
}

async function clearExternalAuthQuery() {
  const query = { ...route.query }
  delete query.externalAuthError
  delete query.externalLoginTicket
  delete query.message
  delete query.provider
  await router.replace({ path: route.path, query })
}

function buildAgreementDialogReturnQuery(): Record<string, string> {
  const query: Record<string, string> = {
    [AGREEMENT_DIALOG_QUERY_KEY]: '1'
  }
  if (typeof route.query.redirect === 'string' && route.query.redirect) {
    query.redirect = route.query.redirect
  }
  return query
}

function restoreAgreementDialogFromQuery() {
  if (route.query[AGREEMENT_DIALOG_QUERY_KEY] !== '1') return
  const query = { ...route.query }
  delete query[AGREEMENT_DIALOG_QUERY_KEY]
  void router.replace({ path: route.path, query })
  if (!agreementAccepted.value) {
    showAgreementDialog.value = true
  }
}

async function handleLogin() {
  if (loading.value || !loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
  } catch (error) {
    console.error('Login validation error:', error)
    return
  }

  if (!agreementAccepted.value) {
    showAgreementDialog.value = true
    return
  }

  showCaptchaDialog.value = true
}

function handleAgreementAgreeLogin() {
  if (loading.value) return
  agreementAccepted.value = true
  showAgreementDialog.value = false
  showCaptchaDialog.value = true
}

function handleAgreementReject() {
  showAgreementDialog.value = false
}

async function handleCaptchaVerified(captchaVerification: string) {
  try {
    loading.value = true
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      captchaVerification
    })
    await completeLoginRedirect(route.query.redirect)
  } catch (error) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}

function normalizeLoginRedirect(value: unknown): string {
  const raw = typeof value === 'string' && value.trim() ? value.trim() : '/'
  return raw === '/login' || raw.startsWith('/login?') ? '/' : raw
}

function isLazyRouteLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error || '')
  return (
    message.includes('Failed to fetch dynamically imported module') ||
    message.includes('Importing a module script failed') ||
    message.includes('error loading dynamically imported module') ||
    message.includes('Loading chunk')
  )
}

function reloadToHashRoute(redirect: string): void {
  const target = redirect.startsWith('/') ? redirect : '/'
  window.location.assign(`${window.location.origin}${window.location.pathname}${window.location.search}#${target}`)
}

async function completeLoginRedirect(redirectValue: unknown) {
  rememberSuccessfulLoginUsername()

  let redirect = normalizeLoginRedirect(redirectValue)

  if (redirect.includes('/oauth2/authorize')) {
    try {
      const { sessionToken } = await getOidcSessionToken()
      const url = new URL(redirect)
      url.searchParams.set('session_token', sessionToken)
      redirect = url.toString()
    } catch (error) {
      console.error('Failed to get OIDC session token:', error)
    }
  }

  if (redirect.startsWith('http://') || redirect.startsWith('https://')) {
    ElMessage.success('登录成功')
    window.location.href = redirect
    return
  }

  try {
    await router.push(redirect)
    ElMessage.success('登录成功')
  } catch (error) {
    console.error('Login redirect failed:', error)
    if (isLazyRouteLoadError(error)) {
      reloadToHashRoute(redirect)
      return
    }
    throw error
  }
}
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
  transform: translate3d(0, 6px, 0);
  transition:
    opacity 0.2s cubic-bezier(0.4, 0, 0.85, 1),
    transform 0.24s cubic-bezier(0.4, 0, 0.85, 1);
}

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
    transform: none;
    transition: none;
  }
}

.label-upper {
  color: rgb(148 163 184);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.auth-form :deep(.el-form-item__label) {
  margin-bottom: 4px;
  line-height: 1.2;
}

.auth-login-submit {
  box-sizing: border-box;
  height: 48px;
  min-height: 48px;
  padding: 0;
  line-height: 1;
}

.auth-form :deep(.el-input.auth-el-input .el-input__wrapper) {
  height: 46px;
  min-height: 46px;
  align-items: center;
  border: none !important;
  border-radius: var(--wk-input-radius) !important;
  background-color: var(--wk-input-bg) !important;
  box-shadow:
    0 0 0 1px var(--wk-input-border) inset,
    var(--wk-input-shadow) !important;
  padding-left: 12px !important;
  transition:
    box-shadow 0.2s ease,
    background-color 0.2s ease !important;
}

.auth-form :deep(.el-input.auth-el-input .el-input__wrapper:hover) {
  background-color: var(--wk-input-bg) !important;
  box-shadow:
    0 0 0 1px var(--wk-input-border-hover) inset,
    var(--wk-input-shadow) !important;
}

.auth-form :deep(.el-input.auth-el-input .el-input__wrapper.is-focus) {
  background-color: var(--wk-input-bg) !important;
  box-shadow:
    0 0 0 1px var(--wk-input-border-focus) inset,
    var(--wk-input-focus-shadow) !important;
}

.auth-form :deep(.el-input.auth-el-input .el-input__inner) {
  height: 46px !important;
  line-height: 46px !important;
}

.auth-form :deep(.el-input.auth-el-input .el-input__prefix),
.auth-form :deep(.el-input.auth-el-input .el-input__suffix) {
  min-height: 46px;
  align-items: center;
}

.desktop-agreement-consent {
  display: flex;
  align-items: flex-start;
  gap: 0.55rem;
  color: #64748b;
  font-size: 0.86rem;
  font-weight: 500;
  line-height: 1.55;
}

.desktop-agreement-consent__check,
.mobile-agreement-consent__check {
  position: relative;
  display: inline-flex;
  width: 1.25rem;
  height: 1.25rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.desktop-agreement-consent__check input,
.mobile-agreement-consent__check input {
  position: absolute;
  inset: 0;
  margin: 0;
  cursor: pointer;
  opacity: 0;
}

.desktop-agreement-consent__box,
.mobile-agreement-consent__box {
  display: inline-flex;
  width: 1.05rem;
  height: 1.05rem;
  align-items: center;
  justify-content: center;
  border: 1px solid #d1d5db;
  border-radius: 0.25rem;
  background: #fff;
  color: #137fec;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.desktop-agreement-consent__check input:checked + .desktop-agreement-consent__box,
.mobile-agreement-consent__check input:checked + .mobile-agreement-consent__box {
  border-color: #137fec;
  background: #eef6ff;
}

.desktop-agreement-consent__check input:focus-visible + .desktop-agreement-consent__box,
.mobile-agreement-consent__check input:focus-visible + .mobile-agreement-consent__box {
  box-shadow: 0 0 0 3px rgba(19, 127, 236, 0.14);
}

.desktop-agreement-consent__text,
.mobile-agreement-consent__text {
  margin: 0;
}

.desktop-agreement-consent__text a,
.mobile-agreement-consent__text a,
.agreement-modal__copy a {
  color: #1d5fc4;
  text-decoration: none;
}

.desktop-agreement-consent__text a:hover,
.mobile-agreement-consent__text a:hover,
.agreement-modal__copy a:hover {
  text-decoration: underline;
}

.external-auth-panel {
  margin-top: 1.25rem;
}

.external-auth-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
  color: #94a3b8;
  font-size: 0.78rem;
  font-weight: 700;
}

.external-auth-divider::before,
.external-auth-divider::after {
  content: '';
  flex: 1 1 auto;
  height: 1px;
  background: #e2e8f0;
}

.external-auth-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
}

.external-auth-btn {
  display: inline-flex;
  min-width: 132px;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 0.9rem;
  background: #fff;
  color: #0f172a;
  font-size: 0.86rem;
  font-weight: 700;
  padding: 0.625rem 1rem;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    color 0.2s ease;
}

.external-auth-btn:hover:not(:disabled) {
  border-color: rgba(19, 127, 236, 0.45);
  color: #137fec;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.external-auth-btn:disabled {
  cursor: wait;
  opacity: 0.72;
}

.external-auth-btn__icon {
  display: inline-flex;
  width: 1.45rem;
  height: 1.45rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
}

.external-auth-btn__icon svg {
  width: 1.1rem;
  height: 1.1rem;
}

.external-auth-btn__fallback {
  display: inline-flex;
  width: 1.45rem;
  height: 1.45rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #f1f5f9;
  color: #137fec;
  font-size: 0.78rem;
  font-weight: 800;
}

.external-auth-btn__microsoft-icon {
  display: grid;
  width: 1rem;
  height: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.12rem;
}

.external-auth-btn__microsoft-pane {
  display: block;
}

.external-auth-btn__microsoft-pane--red {
  background: #f25022;
}

.external-auth-btn__microsoft-pane--green {
  background: #7fba00;
}

.external-auth-btn__microsoft-pane--blue {
  background: #00a4ef;
}

.external-auth-btn__microsoft-pane--yellow {
  background: #ffb900;
}

.mobile-agreement-consent {
  display: none;
}

.mobile-agreement-consent__text {
  color: #8f96a3;
  font-size: 0.82rem;
  font-weight: 500;
  line-height: 1.55;
}

.agreement-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.agreement-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(2px);
}

.agreement-modal__panel {
  position: relative;
  width: min(100%, 27rem);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 1.5rem;
  background: #fff;
  padding: 1.75rem;
  color: #0f172a;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.28);
}

.agreement-modal__header {
  display: flex;
  align-items: center;
  gap: 0.95rem;
}

.agreement-modal__icon {
  display: inline-flex;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  background: linear-gradient(135deg, rgba(19, 127, 236, 0.14), rgba(19, 127, 236, 0.06));
  color: #137fec;
  box-shadow: 0 12px 24px rgba(19, 127, 236, 0.12);
}

.agreement-modal__heading {
  min-width: 0;
}

.agreement-modal__title {
  margin: 0;
  color: #0f172a;
  font-size: 1.18rem;
  font-weight: 800;
  line-height: 1.35;
}

.agreement-modal__copy {
  margin: 1.35rem 0 0;
  color: #334155;
  font-size: 0.96rem;
  font-weight: 500;
  line-height: 1.8;
}

.agreement-modal__actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0.9rem;
  margin-top: 1.5rem;
}

.agreement-modal__button {
  display: inline-flex;
  min-width: 0;
  min-height: 3rem;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  border-radius: 1rem;
  font-size: 0.95rem;
  font-weight: 800;
  line-height: 1.2;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease,
    opacity 0.2s ease,
    box-shadow 0.2s ease;
}

.agreement-modal__button--ghost {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
}

.agreement-modal__button--ghost:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
}

.agreement-modal__button--primary {
  border: 1px solid #0f172a;
  background: #0f172a;
  color: #fff;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.18);
}

.agreement-modal__button--primary:hover:not(:disabled) {
  border-color: #1e293b;
  background: #1e293b;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.24);
}

.agreement-modal__button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
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
    align-items: stretch;
    overflow-y: auto;
    padding: 0;
    -webkit-overflow-scrolling: touch;
  }

  .auth-card {
    min-height: 100%;
    max-height: none;
    flex-direction: column;
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
}

@media (max-width: 767px) {
  .auth-form-content--with-fixed-consent {
    padding-bottom: calc(5.5rem + env(safe-area-inset-bottom));
  }

  .desktop-agreement-consent {
    display: none;
  }

  .mobile-agreement-consent {
    position: fixed;
    z-index: 40;
    left: 50%;
    bottom: calc(1.5rem + env(safe-area-inset-bottom));
    display: flex;
    width: min(calc(100% - 2rem), 480px);
    align-items: flex-start;
    gap: 0.55rem;
    margin-top: 0;
    transform: translateX(-50%);
  }

  .agreement-modal {
    padding: 1.75rem 1.35rem;
  }

  .agreement-modal__panel {
    width: min(100%, 23.5rem);
    border-radius: 1.25rem;
    padding: 1.4rem;
  }

  .agreement-modal__icon {
    width: 2.75rem;
    height: 2.75rem;
    border-radius: 0.9rem;
  }

  .agreement-modal__title {
    font-size: 1.12rem;
  }

  .agreement-modal__copy {
    font-size: 0.92rem;
    line-height: 1.75;
  }

  .agreement-modal__actions {
    gap: 0.75rem;
  }
}
</style>
