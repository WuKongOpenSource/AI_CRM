import { get, post } from '@/utils/request'
import type {
  WecomConfigVO,
  WecomConversationPage,
  WecomConversationQuery,
  WecomConversationTabVO,
  WecomCustomerBindPayload,
  WecomCustomerBindingVO,
  WecomCustomerPage,
  WecomCustomerQuery,
  WecomCustomerUnbindPayload,
  WecomEmployeePage,
  WecomEmployeeSessionQuery,
  WecomExternalCustomerVO,
  WecomMessagePage,
  WecomSyncStatusVO
} from '@/types/wecom'

export function getWecomConfig(): Promise<WecomConfigVO> {
  return get('/wecom/config')
}

export function saveWecomConfig(data: Partial<WecomConfigVO>): Promise<WecomConfigVO> {
  return post('/wecom/config', data)
}

export function runWecomSync(data = {}): Promise<WecomSyncStatusVO> {
  return post('/wecom/sync/run', data)
}

export function getWecomSyncStatus(): Promise<WecomSyncStatusVO> {
  return get('/wecom/sync/status')
}

export function queryWecomEmployees(query: WecomEmployeeSessionQuery): Promise<WecomEmployeePage> {
  return post('/wecom/scrm/employees', query)
}

export function queryWecomConversations(query: WecomConversationQuery): Promise<WecomConversationPage> {
  return post('/wecom/scrm/conversations', query)
}

export function getWecomConversationMessages(
  conversationId: string,
  page = 1,
  limit = 100
): Promise<WecomMessagePage> {
  return get(`/wecom/scrm/conversations/${conversationId}/messages`, { params: { page, limit } })
}

export function queryWecomCustomers(query: WecomCustomerQuery): Promise<WecomCustomerPage> {
  return post('/wecom/customers/queryPageList', query)
}

export function getWecomCustomer(id: string): Promise<WecomExternalCustomerVO> {
  return get(`/wecom/customers/${id}`)
}

export function bindWecomCustomer(data: WecomCustomerBindPayload): Promise<WecomCustomerBindingVO> {
  return post('/wecom/customers/bind', data)
}

export function unbindWecomCustomer(data: WecomCustomerUnbindPayload): Promise<void> {
  return post('/wecom/customers/unbind', data)
}

export function getCustomerWecomBindings(customerId: string): Promise<WecomCustomerBindingVO[]> {
  return get(`/customer/${customerId}/wecom-bindings`)
}

export function getCustomerWecomConversationTabs(customerId: string): Promise<WecomConversationTabVO[]> {
  return get(`/customer/${customerId}/wecom-conversation-tabs`)
}

export function getCustomerWecomConversationMessages(
  customerId: string,
  conversationId: string,
  page = 1,
  limit = 100
): Promise<WecomMessagePage> {
  return get(`/customer/${customerId}/wecom-conversations/${conversationId}/messages`, {
    params: { page, limit }
  })
}
