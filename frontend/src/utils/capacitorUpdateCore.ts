export const HOT_UPDATE_STATUS = 1
export const DOWNLOAD_UPDATE_STATUS = 2

export type AppUpdateStatus = typeof HOT_UPDATE_STATUS | typeof DOWNLOAD_UPDATE_STATUS
export type SupportedPlatform = 'android' | 'ios'
export type AppUpdateArtifactType = 'manifest' | 'zip'

type UnknownRecord = Record<string, unknown>

export type AppUpdateInfo = {
  status: AppUpdateStatus
  version: string
  url: string
  note?: string
  artifactType?: AppUpdateArtifactType
  bundleId?: string
  checksum?: string
  signature?: string
}

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function readString(record: UnknownRecord, keys: string[]): string | undefined {
  for (const key of keys) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
  }
  return undefined
}

function normalizeStatus(value: unknown): AppUpdateStatus | null {
  const status = typeof value === 'number' ? value : Number(value)
  if (status === HOT_UPDATE_STATUS || status === DOWNLOAD_UPDATE_STATUS) {
    return status
  }
  return null
}

function normalizeArtifactType(value: unknown): AppUpdateArtifactType | undefined {
  return value === 'manifest' || value === 'zip' ? value : undefined
}

function parseVersionPart(part: string | undefined): number {
  if (!part) return 0
  const match = part.trim().match(/^\d+/)
  return match ? Number(match[0]) : 0
}

function unwrapUpdateManifest(payload: unknown): unknown {
  if (!isRecord(payload)) return payload

  const code = Number(payload.code)
  if (code === 0 && 'data' in payload) {
    return payload.data
  }

  return payload
}

export function compareVersion(remote: string, current: string): boolean {
  const remoteParts = remote.split('.')
  const currentParts = current.split('.')
  const maxLength = Math.max(remoteParts.length, currentParts.length)

  for (let index = 0; index < maxLength; index++) {
    const remoteNum = parseVersionPart(remoteParts[index])
    const currentNum = parseVersionPart(currentParts[index])
    if (remoteNum > currentNum) return true
    if (remoteNum < currentNum) return false
  }

  return false
}

export function normalizeUpdateInfo(value: unknown): AppUpdateInfo | null {
  if (!isRecord(value)) return null

  const status = normalizeStatus(value.status)
  const version = readString(value, ['version'])
  const url = readString(value, ['url', 'downloadUrl', 'bundleUrl', 'apkUrl'])

  if (!status || !version || !url) {
    return null
  }

  return {
    status,
    version,
    url,
    note: readString(value, ['note']),
    artifactType: normalizeArtifactType(value.artifactType),
    bundleId: readString(value, ['bundleId']),
    checksum: readString(value, ['checksum']),
    signature: readString(value, ['signature'])
  }
}

export function getPlatformUpdateInfo(
  payload: unknown,
  platform: SupportedPlatform
): AppUpdateInfo | null {
  const manifest = unwrapUpdateManifest(payload)
  if (!isRecord(manifest)) return null

  const platformInfo = manifest[platform]
  const normalizedPlatformInfo = normalizeUpdateInfo(platformInfo)
  if (normalizedPlatformInfo) {
    return normalizedPlatformInfo
  }

  if (isRecord(platformInfo)) {
    return normalizeUpdateInfo(platformInfo.default)
  }

  return null
}

export function shouldApplyUpdate(updateInfo: AppUpdateInfo | null, currentVersion: string): boolean {
  if (!updateInfo) return false
  return compareVersion(updateInfo.version, currentVersion)
}
