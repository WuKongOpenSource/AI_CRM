import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  queryEnabledAgents,
  queryAllAgents,
  addAgent,
  updateAgent,
  deleteAgent
} from '@/api/agent'
import type { AiAgent, AiAgentAddBO, AiAgentUpdateBO } from '@/types/common'

export const useAgentStore = defineStore('agent', () => {
  // State
  const enabledAgents = ref<AiAgent[]>([])
  const allAgents = ref<AiAgent[]>([])
  const loading = ref(false)
  const updating = ref(false) // Prevent concurrent updates

  // Actions
  async function fetchEnabledAgents() {
    loading.value = true
    try {
      enabledAgents.value = await queryEnabledAgents()
    } finally {
      loading.value = false
    }
  }

  async function fetchAllAgents() {
    loading.value = true
    try {
      allAgents.value = await queryAllAgents()
    } finally {
      loading.value = false
    }
  }

  async function createAgent(data: AiAgentAddBO): Promise<string> {
    const agentId = await addAgent(data)
    await fetchAllAgents()
    await fetchEnabledAgents()
    return agentId
  }

  async function editAgent(data: AiAgentUpdateBO): Promise<void> {
    await updateAgent(data)
    await fetchAllAgents()
    await fetchEnabledAgents()
  }

  async function removeAgent(agentId: string): Promise<void> {
    await deleteAgent(agentId)
    await fetchAllAgents()
    await fetchEnabledAgents()
  }

  async function toggleAgentEnabled(agentId: string, newEnabled: boolean): Promise<void> {
    // Prevent concurrent updates causing infinite loop
    if (updating.value) return

    updating.value = true
    try {
      await updateAgent({
        agentId,
        enabled: newEnabled
      } as any)
      await fetchAllAgents()
      await fetchEnabledAgents()
    } finally {
      updating.value = false
    }
  }

  return {
    // State
    enabledAgents,
    allAgents,
    loading,
    updating,
    // Actions
    fetchEnabledAgents,
    fetchAllAgents,
    createAgent,
    editAgent,
    removeAgent,
    toggleAgentEnabled
  }
})
