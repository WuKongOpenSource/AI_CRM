<template>
  <main class="legal-page">
    <section class="legal-shell">
      <div class="legal-brand">
        <img :src="logoImg" alt="悟空CRM" class="legal-brand__logo" />
        <span class="legal-brand__name">悟空CRM</span>
      </div>

      <article class="legal-document">
        <p class="legal-document__eyebrow">Legal</p>
        <h1>{{ documentTitle }}</h1>
        <p class="legal-document__intro">
          {{ documentIntro }}
        </p>
        <div class="legal-document__content" aria-live="polite">
          <p v-if="isLoading" class="legal-document__state">正在加载{{ documentTitle }}...</p>
          <pre v-else-if="documentContent" class="legal-document__text">{{ documentContent }}</pre>
          <div v-else class="legal-document__state legal-document__state--error">
            <p>{{ loadError || '文档加载失败' }}</p>
            <button type="button" @click="loadDocument">重新加载</button>
          </div>
        </div>
      </article>

      <button type="button" class="legal-page__back" @click="handleBackToLogin">返回登录</button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { InAppBrowser } from '@capacitor/inappbrowser'
import logoImg from '@/assets/images/logo.png'

type LegalDocumentType = 'agreement' | 'privacy'

const props = defineProps<{
  documentType: LegalDocumentType
}>()

const route = useRoute()
const router = useRouter()
const IN_APP_BROWSER_QUERY_KEY = 'inAppBrowser'

const LEGAL_DOCUMENT_URLS: Record<LegalDocumentType, string> = {
  agreement: 'https://file.72crm.com/static/law/72crm_ai_service.txt',
  privacy: 'https://file.72crm.com/static/law/72crm_ai_privacy.txt'
}

const documentTitle = computed(() => (props.documentType === 'privacy' ? '隐私声明' : '用户协议'))
const documentIntro = computed(() => `请仔细阅读以下${documentTitle.value}内容。`)
const documentUrl = computed(() => LEGAL_DOCUMENT_URLS[props.documentType])
const shouldCloseInAppBrowser = computed(() => route.query[IN_APP_BROWSER_QUERY_KEY] === '1')
const backRoute = computed(() => {
  const query: Record<string, string> = {}
  if (route.query.agreementDialog === '1') {
    query.agreementDialog = '1'
  }
  if (typeof route.query.redirect === 'string' && route.query.redirect) {
    query.redirect = route.query.redirect
  }
  return { name: 'Login', query }
})
const documentContent = ref('')
const isLoading = ref(false)
const loadError = ref('')

async function loadDocument(): Promise<void> {
  isLoading.value = true
  loadError.value = ''

  try {
    const response = await fetch(documentUrl.value, { cache: 'no-store' })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    documentContent.value = await response.text()
  } catch (error) {
    documentContent.value = ''
    loadError.value = error instanceof Error ? `文档加载失败：${error.message}` : '文档加载失败'
  } finally {
    isLoading.value = false
  }
}

async function handleBackToLogin(): Promise<void> {
  if (shouldCloseInAppBrowser.value) {
    try {
      await InAppBrowser.close()
      return
    } catch (error) {
      console.warn('Failed to close InAppBrowser:', error)
    }
  }

  await router.push(backRoute.value)
}

watch(
  () => props.documentType,
  () => {
    void loadDocument()
  },
  { immediate: true }
)
</script>

<style scoped>
.legal-page {
  height: 100vh;
  height: 100dvh;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #f8fafc;
  color: #0f172a;
}

.legal-shell {
  width: min(100%, 760px);
  min-height: 100%;
  margin: 0 auto;
  padding: 2rem 1.25rem 3rem;
  box-sizing: border-box;
}

.legal-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.legal-brand__logo {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  background: #fff;
  object-fit: contain;
  padding: 0.3rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.legal-brand__name {
  font-size: 1.1rem;
  font-weight: 800;
}

.legal-document {
  /* border: 1px solid #e2e8f0;
  border-radius: 0.5rem; */
  background: #fff;
  padding: clamp(1.5rem, 5vw, 2.5rem);
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.06);
}

.legal-document__eyebrow {
  margin: 0 0 0.6rem;
  color: #137fec;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.legal-document h1 {
  margin: 0;
  font-size: clamp(1.8rem, 8vw, 2.5rem);
  line-height: 1.2;
}

.legal-document__intro {
  margin: 1rem 0 0;
  color: #475569;
  font-size: 1rem;
  line-height: 1.8;
}

.legal-document__content {
  margin-top: 1.75rem;
  max-height: min(52vh, 34rem);
  overflow-y: auto;
  overscroll-behavior: contain;
  /* border: 1px solid #e2e8f0;
  border-radius: 0.5rem; */
  background: #fff;
  /* padding: clamp(1rem, 4vw, 1.5rem); */
  scrollbar-color: #cbd5e1 #fff;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  -webkit-overflow-scrolling: touch;
}

.legal-document__content::-webkit-scrollbar {
  width: 0.5rem;
}

.legal-document__content::-webkit-scrollbar-track {
  background: #fff;
}

.legal-document__content::-webkit-scrollbar-thumb {
  border: 0.125rem solid #fff;
  border-radius: 999px;
  background: #cbd5e1;
}

.legal-document__content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.legal-document__text {
  margin: 0;
  color: #0f172a;
  font: inherit;
  font-size: 0.96rem;
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.legal-document__state {
  display: flex;
  min-height: 12rem;
  align-items: center;
  justify-content: center;
  margin: 0;
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.7;
  text-align: center;
}

.legal-document__state--error {
  flex-direction: column;
  gap: 0.9rem;
}

.legal-document__state--error p {
  margin: 0;
}

.legal-document__state--error button {
  border: 0;
  border-radius: 0.5rem;
  background: #137fec;
  color: #fff;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 700;
  padding: 0.5rem 1rem;
}

.legal-document__state--error button:hover {
  background: #0f6fd2;
}

.legal-document__fallback {
  margin: 0.9rem 0 0;
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.7;
}

.legal-document__fallback a {
  color: #137fec;
  font-weight: 700;
  text-decoration: none;
}

.legal-document__fallback a:hover {
  text-decoration: underline;
}

.legal-page__back {
  display: inline-flex;
  margin-top: 1.5rem;
  padding: 0;
  border: 0;
  background: transparent;
  color: #137fec;
  cursor: pointer;
  font: inherit;
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
}

.legal-page__back:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .legal-shell {
    padding: 1.5rem 1.25rem 2rem;
  }

  .legal-document__content {
    max-height: 50dvh;
  }
}
</style>
