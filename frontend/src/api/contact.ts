import { post } from '@/utils/request'
import type { Contact, ContactAddBO, ContactQueryBO } from '@/types/customer'
import type { PageResult } from '@/types/api'

/**
 * Add contact
 */
export function addContact(data: ContactAddBO): Promise<string> {
  return post('/contact/add', data)
}

/**
 * Update contact
 */
export function updateContact(data: ContactAddBO & { contactId: string }): Promise<void> {
  return post('/contact/update', data)
}

/**
 * Delete contact
 */
export function deleteContact(id: string): Promise<void> {
  return post(`/contact/delete/${id}`)
}

/**
 * Query contacts by customer
 */
export function queryContactsByCustomer(customerId: string): Promise<Contact[]> {
  return post('/contact/queryByCustomer', null, { params: { customerId } })
}

/**
 * Set contact as primary
 */
export function setPrimaryContact(id: string): Promise<void> {
  return post(`/contact/setPrimary/${id}`)
}

/**
 * Query contacts with pagination
 */
export function queryContactPageList(query: ContactQueryBO): Promise<PageResult<Contact>> {
  return post('/contact/queryPageList', query)
}
