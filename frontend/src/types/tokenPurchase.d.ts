export type PaymentChannel = 'wechat' | 'alipay'

export type TokenPurchaseOrderStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CLOSED' | 'EXPIRED'

export interface TokenPurchasePlan {
  id: string
  name: string
  description?: string
  tokenAmount: number
  priceFen: number
}

export interface TokenPurchaseChannel {
  code: PaymentChannel
  label: string
  enabled: boolean
  unavailableReason?: string | null
}

export interface TokenPurchaseOptions {
  enabled: boolean
  orderExpireMinutes: number
  giftTokenRemaining: number
  purchasedTokenRemaining: number
  tokenRemaining: number
  plans: TokenPurchasePlan[]
  channels: TokenPurchaseChannel[]
}

export interface TokenPurchaseOrder {
  orderNo: string
  planId: string
  planName: string
  tokenAmount: number
  amountFen: number
  amountDisplay: string
  paymentChannel: PaymentChannel
  paymentChannelLabel: string
  status: TokenPurchaseOrderStatus
  qrCodeContent?: string | null
  qrCodeImage?: string | null
  expireTime?: string
  paidTime?: string
  createTime?: string
}

export interface TokenPurchaseCreateBO {
  planId: string
  paymentChannel: PaymentChannel
}
