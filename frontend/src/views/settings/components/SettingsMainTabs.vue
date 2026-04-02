<template>
  <div class="bg-white border-b border-slate-200 px-4 md:px-8">
    <el-tabs v-model="currentTab" class="settings-main-tabs">
      <el-tab-pane
        v-for="tab in SETTINGS_MAIN_TABS"
        :key="tab.value"
        :label="tab.label"
        :name="tab.value"
      />
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { SETTINGS_MAIN_TABS } from '../constants'
import type { SettingsMainTab } from '../types'

const props = defineProps<{
  activeTab: SettingsMainTab
}>()

const emit = defineEmits<{
  (e: 'update:activeTab', value: SettingsMainTab): void
}>()

const currentTab = computed({
  get: () => props.activeTab,
  set: (value: string | number) => emit('update:activeTab', value as SettingsMainTab)
})
</script>

<style scoped>
.settings-main-tabs {
  --el-tabs-header-height: auto;
  --settings-main-tab-color: #137fec;
}

.settings-main-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.settings-main-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.settings-main-tabs :deep(.el-tabs__nav-scroll) {
  padding: 0;
}

.settings-main-tabs :deep(.el-tabs__item) {
  height: auto;
  padding: 1rem 0;
  margin-right: 1rem;
  color: rgb(148 163 184);
  font-size: 0.875rem;
  font-weight: 700;
  line-height: 1.25rem;
  transition: color 0.2s ease;
}

.settings-main-tabs :deep(.el-tabs__item:hover) {
  color: rgb(71 85 105);
}

.settings-main-tabs :deep(.el-tabs__item.is-active) {
  color: var(--settings-main-tab-color);
}

.settings-main-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  background-color: var(--settings-main-tab-color);
}

.settings-main-tabs :deep(.el-tabs__content) {
  display: none;
}

@media (min-width: 768px) {
  .settings-main-tabs :deep(.el-tabs__item) {
    margin-right: 2rem;
  }
}
</style>
