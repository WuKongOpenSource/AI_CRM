export interface AccountScopedMailQueryOptions {
  page: number
  limit: number
  keyword?: string
  accountId?: string
}

export interface AccountScopedMailQuery {
  page: number
  limit: number
  keyword?: string
  accountId?: string
}

export interface MailSyncStatusLike {
  status?: string | null
}

export function buildAccountScopedMailQuery(options: AccountScopedMailQueryOptions): AccountScopedMailQuery {
  const query: AccountScopedMailQuery = {
    page: options.page,
    limit: options.limit,
  }
  const keyword = options.keyword?.trim()
  const accountId = options.accountId?.trim()
  if (keyword) query.keyword = keyword
  if (accountId) query.accountId = accountId
  return query
}

export function isMailSyncRunning(log: MailSyncStatusLike | null | undefined) {
  return log?.status === 'running'
}
