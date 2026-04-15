import { get, post } from '@/utils/request'
import type {
  TokenPurchaseCreateBO,
  TokenPurchaseOptions,
  TokenPurchaseOrder
} from '@/types/tokenPurchase'

export function getTokenPurchaseOptions(): Promise<TokenPurchaseOptions> {
  return get('/tokenPurchase/options')
}

export function createTokenPurchaseOrder(data: TokenPurchaseCreateBO): Promise<TokenPurchaseOrder> {
  return post('/tokenPurchase/orders', data)
}

export function getTokenPurchaseOrder(orderNo: string): Promise<TokenPurchaseOrder> {
  return get(`/tokenPurchase/orders/${orderNo}`)
}

export function listRecentTokenPurchaseOrders(limit = 10): Promise<TokenPurchaseOrder[]> {
  return get('/tokenPurchase/orders', { params: { limit } })
}
