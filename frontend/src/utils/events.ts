type Handler<T = any> = (payload: T) => void

class AppEventBus {
  private target = new EventTarget()

  on<T = any>(type: string, handler: Handler<T>) {
    const listener = (e: Event) => handler((e as CustomEvent<T>).detail)
    this.target.addEventListener(type, listener as EventListener)
    return () => this.target.removeEventListener(type, listener as EventListener)
  }

  emit<T = any>(type: string, payload?: T) {
    this.target.dispatchEvent(new CustomEvent(type, { detail: payload }))
  }
}

export const appEvents = new AppEventBus()

export const APP_EVENT = {
  CUSTOMER_LIST_REFRESH: 'customer:list:refresh',
} as const

