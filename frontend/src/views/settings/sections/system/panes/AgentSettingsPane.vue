<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h3 class="font-medium">AI 智能体管理</h3>
      <el-button type="primary" @click="showAgentDialog = true">
        <el-icon class="mr-1 wk-plus-button-icon"><Plus /></el-icon>
        添加智能体
      </el-button>
    </div>
    <div v-if="agentStore.loading" class="text-center py-8">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    <div v-else-if="agentStore.allAgents.length === 0" class="text-center py-8 text-slate-400">
      暂无智能体
    </div>
    <div v-else class="space-y-3">
      <div
        v-for="agent in agentStore.allAgents"
        :key="agent.agentId"
        class="flex items-center justify-between p-4 bg-white rounded-lg border border-slate-200"
      >
        <div class="flex items-center">
          <el-icon :size="24" class="text-primary"><Promotion /></el-icon>
          <div class="ml-3">
            <div class="font-medium">{{ agent.label }}</div>
            <div class="text-sm text-slate-500 truncate max-w-md">{{ agent.prompt }}</div>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <el-switch
            :model-value="!!agent.enabled"
            :disabled="agentStore.updating"
            @change="(value: boolean) => handleToggleAgent(agent.agentId, value)"
          />
          <el-button text @click="handleEditAgent(agent)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button text type="danger" @click="handleDeleteAgent(agent)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <AgentDialog
      :visible="showAgentDialog"
      :is-mobile="isMobile"
      :editing-agent="editingAgent"
      :agent-form="agentForm"
      :submitting="submitting"
      @update:visible="showAgentDialog = $event"
      @save="handleSaveAgent"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Loading, Plus, Promotion } from '@element-plus/icons-vue'
import { useResponsive } from '@/composables/useResponsive'
import { useAgentStore } from '@/stores/agent'
import type { AiAgent } from '@/types/common'
import AgentDialog from './components/AgentDialog.vue'

const agentStore = useAgentStore()
const { isMobile } = useResponsive()

const showAgentDialog = ref(false)
const submitting = ref(false)
const editingAgent = ref<AiAgent | null>(null)
const agentForm = reactive({
  label: '',
  prompt: '',
  iconName: 'Promotion',
  enabled: true
})

onMounted(() => {
  agentStore.fetchAllAgents()
})

function handleEditAgent(agent: AiAgent) {
  editingAgent.value = agent
  Object.assign(agentForm, {
    label: agent.label,
    prompt: agent.prompt,
    iconName: agent.iconName,
    enabled: !!agent.enabled
  })
  showAgentDialog.value = true
}

async function handleDeleteAgent(agent: AiAgent) {
  try {
    await ElMessageBox.confirm(`确定要删除智能体「${agent.label}」吗？`, '提示', { type: 'warning' })
    await agentStore.removeAgent(agent.agentId)
    ElMessage.success('删除成功')
  } catch {
    // cancelled
  }
}

async function handleToggleAgent(agentId: string, enabled: boolean) {
  await agentStore.toggleAgentEnabled(agentId, enabled)
}

async function handleSaveAgent() {
  if (!agentForm.label || !agentForm.prompt) {
    ElMessage.warning('请填写名称和提示词')
    return
  }

  submitting.value = true
  try {
    if (editingAgent.value) {
      await agentStore.editAgent({
        ...agentForm,
        agentId: editingAgent.value.agentId
      } as any)
      ElMessage.success('更新成功')
    } else {
      await agentStore.createAgent(agentForm as any)
      ElMessage.success('创建成功')
    }
    showAgentDialog.value = false
    resetAgentForm()
  } finally {
    submitting.value = false
  }
}

function resetAgentForm() {
  editingAgent.value = null
  Object.assign(agentForm, { label: '', prompt: '', iconName: 'Promotion', enabled: true })
}
</script>
