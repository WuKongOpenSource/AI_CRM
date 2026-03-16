import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getEnterpriseConfig } from '@/api/systemConfig'
import type { EnterpriseConfig } from '@/types/systemConfig'

export const useEnterpriseStore = defineStore('enterprise', () => {
  const name = ref<string | null>(null)
  const logo = ref<string | null>(null)
  const logoUrl = ref<string | null>(null)
  const loaded = ref(false)

  async function loadConfig() {
    if (loaded.value) return
    try {
      const config = await getEnterpriseConfig()
      name.value = config.name
      logo.value = config.logo
      logoUrl.value = config.logoUrl
      loaded.value = true
    } catch {
      // 加载失败使用默认值
    }
  }

  function updateLocal(config: EnterpriseConfig) {
    name.value = config.name
    logo.value = config.logo
    logoUrl.value = config.logoUrl
  }

  const displayName = computed(() => name.value || 'Nexus AI')
  const hasLogo = computed(() => !!logoUrl.value)

  return { name, logo, logoUrl, loaded, loadConfig, updateLocal, displayName, hasLogo }
})
