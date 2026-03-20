<template>
  <div class="flex flex-col h-full bg-background-light">
    <SettingsMainTabs v-model:active-tab="activeMainTab" />

    <div class="flex-1 min-h-0" :class="activeMainTab === 'team' ? 'overflow-hidden' : 'overflow-auto'">
      <div :class="activeMainTab === 'team' ? 'h-full min-h-0 flex flex-col' : 'p-4 md:p-6'">
        <KeepAlive>
          <component
            :is="activeSectionComponent"
            v-bind="activeSectionProps"
          />
        </KeepAlive>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SettingsMainTabs from './components/SettingsMainTabs.vue'
import { SYSTEM_SETTINGS_TABS } from './constants'
import type { SettingsMainTab, SystemSettingsTab } from './types'
import TeamManagementSection from './sections/team/TeamManagementSection.vue'
import RolePermissionSection from './sections/role/RolePermissionSection.vue'
import SystemSettingsSection from './sections/system/SystemSettingsSection.vue'

const route = useRoute()
const router = useRouter()
const lastSystemTab = ref<SystemSettingsTab>('enterprise')
const systemTabValues = new Set<SystemSettingsTab>(SYSTEM_SETTINGS_TABS.map((tab) => tab.value))

const sectionMap = {
  team: TeamManagementSection,
  role: RolePermissionSection,
  system: SystemSettingsSection
} as const

const routeMainTab = computed<SettingsMainTab>(() => {
  if (route.path.startsWith('/settings/system')) return 'system'
  if (route.path.startsWith('/settings/role')) return 'role'
  return 'team'
})

const routeSystemTab = computed<SystemSettingsTab>(() => {
  const rawValue = route.params.systemTab
  const systemTab = Array.isArray(rawValue) ? rawValue[0] : rawValue
  if (systemTab && systemTabValues.has(systemTab as SystemSettingsTab)) {
    return systemTab as SystemSettingsTab
  }
  return lastSystemTab.value
})

watch(routeSystemTab, (value) => {
  if (routeMainTab.value === 'system') {
    lastSystemTab.value = value
  }
}, { immediate: true })

const activeMainTab = computed<SettingsMainTab>({
  get: () => routeMainTab.value,
  set: (value) => {
    if (value === routeMainTab.value) return
    if (value === 'system') {
      router.push(`/settings/system/${lastSystemTab.value}`)
      return
    }
    router.push(`/settings/${value}`)
  }
})

const activeSystemTab = computed<SystemSettingsTab>({
  get: () => routeSystemTab.value,
  set: (value) => {
    lastSystemTab.value = value
    if (route.path === `/settings/system/${value}`) return
    router.push(`/settings/system/${value}`)
  }
})

const activeSectionComponent = computed(() => sectionMap[routeMainTab.value])
const activeSectionProps = computed(() => {
  if (routeMainTab.value !== 'system') {
    return {}
  }
  return {
    activeTab: activeSystemTab.value,
    'onUpdate:activeTab': (value: SystemSettingsTab) => {
      activeSystemTab.value = value
    }
  }
})
</script>
