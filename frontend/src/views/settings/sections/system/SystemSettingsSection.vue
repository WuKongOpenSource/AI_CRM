<template>
  <div>
    <SystemSubTabs :active-tab="activeTab" @update:active-tab="$emit('update:activeTab', $event)" />

    <KeepAlive>
      <component :is="activePaneComponent" />
    </KeepAlive>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SystemSubTabs from '../../components/SystemSubTabs.vue'
import type { SystemSettingsTab } from '../../types'
import EnterpriseSettingsPane from './panes/EnterpriseSettingsPane.vue'
import AiConfigSettingsPane from './panes/AiConfigSettingsPane.vue'
import AgentSettingsPane from './panes/AgentSettingsPane.vue'
import StorageSettingsPane from './panes/StorageSettingsPane.vue'
import CustomFieldSettingsPane from './panes/CustomFieldSettingsPane.vue'

const props = defineProps<{
  activeTab: SystemSettingsTab
}>()

defineEmits<{
  (e: 'update:activeTab', value: SystemSettingsTab): void
}>()

const paneMap = {
  enterprise: EnterpriseSettingsPane,
  api: AiConfigSettingsPane,
  agent: AgentSettingsPane,
  storage: StorageSettingsPane,
  customField: CustomFieldSettingsPane
} as const

const activePaneComponent = computed(() => paneMap[props.activeTab])
</script>
