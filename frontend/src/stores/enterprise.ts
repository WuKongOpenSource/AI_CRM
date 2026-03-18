import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getEnterpriseConfig } from '@/api/systemConfig'
import type { EnterpriseConfig } from '@/types/systemConfig'

export const useEnterpriseStore = defineStore('enterprise', () => {
  const name = ref<string | null>(null)
  const logo = ref<string | null>(null)
  const logoUrl = ref<string | null>(null)
  const description = ref<string | null>(null)
  const loaded = ref(false)

  async function loadConfig() {
    if (loaded.value) return
    try {
      const config = await getEnterpriseConfig()
      name.value = config.name
      logo.value = config.logo
      logoUrl.value = config.logoUrl
      description.value = config.description
      loaded.value = true
    } catch {
      // 加载失败使用默认值
    }
  }

  function updateLocal(config: EnterpriseConfig) {
    name.value = config.name
    logo.value = config.logo
    logoUrl.value = config.logoUrl
    description.value = config.description
  }

  function reset() {
    name.value = null
    logo.value = null
    logoUrl.value = null
    description.value = null
    loaded.value = false
  }

  const displayName = computed(() => name.value || '悟空AI CRM')
  const hasLogo = computed(() => !!logoUrl.value)
  const displayDescription = computed(() => {
    const text = description.value || 'WUKONG AI CRM'
    return text.length > 12 ? text.slice(0, 12) + '...' : text
  })

  return { name, logo, logoUrl, description, loaded, loadConfig, updateLocal, reset, displayName, hasLogo, displayDescription }
})
