export type ChatCurrentView = 'chat' | 'notifications'
export type ChatMobilePanel = 'sessions' | 'chat'

type MobileCustomerSummaryActionState = {
  isMobile: boolean
  currentView: ChatCurrentView
  mobilePanel: ChatMobilePanel
  hasCustomerContext: boolean
}

export function shouldShowMobileCustomerSummaryAction(state: MobileCustomerSummaryActionState): boolean {
  return state.isMobile
    && state.currentView === 'chat'
    && state.mobilePanel === 'chat'
    && state.hasCustomerContext
}
