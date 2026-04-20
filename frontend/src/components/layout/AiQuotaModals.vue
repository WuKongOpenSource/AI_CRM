<template>
  <ApiKeySetupModal
    :model-value="isApiKeyModalOpen"
    :loading="savingApiKey"
    :provider-options="apiKeySetupProviderOptions"
    :initial-config="apiKeySetupInitialConfig"
    @update:model-value="handleApiKeyModalVisibleChange"
    @save="handleSaveApiKey"
  />
  <TokenPurchaseDialog
    :model-value="isTokenPurchaseDialogOpen"
    @update:model-value="handleTokenPurchaseDialogVisibleChange"
    @paid="handleTokenPurchasePaid"
  />
</template>

<script setup lang="ts">
import ApiKeySetupModal from '@/components/common/ApiKeySetupModal.vue'
import TokenPurchaseDialog from '@/components/billing/TokenPurchaseDialog.vue'
import { useAiQuota } from '@/composables/useAiQuota'

const {
  isApiKeyModalOpen,
  savingApiKey,
  apiKeySetupProviderOptions,
  apiKeySetupInitialConfig,
  handleApiKeyModalVisibleChange,
  handleSaveApiKey,
  isTokenPurchaseDialogOpen,
  handleTokenPurchaseDialogVisibleChange,
  handleTokenPurchasePaid,
} = useAiQuota()
</script>
