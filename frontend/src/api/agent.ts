import { post, get } from '@/utils/request'
import type { AiAgent, AiAgentAddBO, AiAgentUpdateBO } from '@/types/common'

/**
 * Create agent
 */
export function addAgent(data: AiAgentAddBO): Promise<string> {
  return post('/agent/add', data)
}

/**
 * Update agent
 */
export function updateAgent(data: AiAgentUpdateBO): Promise<void> {
  return post('/agent/update', data)
}

/**
 * Delete agent
 */
export function deleteAgent(id: string): Promise<void> {
  return post(`/agent/delete/${id}`)
}

/**
 * Query enabled agents
 */
export function queryEnabledAgents(): Promise<AiAgent[]> {
  return get('/agent/queryEnabled')
}

/**
 * Query all agents
 */
export function queryAllAgents(): Promise<AiAgent[]> {
  return get('/agent/queryAll')
}
