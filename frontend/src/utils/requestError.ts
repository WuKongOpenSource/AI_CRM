const REQUEST_ERROR_HANDLED_FLAG = Symbol('wk-request-error-handled')

export function markRequestErrorHandled<T>(error: T): T {
  if (error && (typeof error === 'object' || typeof error === 'function')) {
    try {
      ;(error as Record<PropertyKey, unknown>)[REQUEST_ERROR_HANDLED_FLAG] = true
    } catch {
      // ignore non-extensible errors
    }
  }

  return error
}

export function isRequestErrorHandled(error: unknown): boolean {
  if (!error || (typeof error !== 'object' && typeof error !== 'function')) {
    return false
  }

  return Boolean((error as Record<PropertyKey, unknown>)[REQUEST_ERROR_HANDLED_FLAG])
}
