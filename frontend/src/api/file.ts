import { post } from '@/utils/request'

export interface PresignedUploadVO {
  uploadUrl: string
  method: string
  bucket: string
  objectKey: string
  expiry: number
  accessUrl: string
}

/**
 * 获取预签名上传URL
 */
export function getPresignedUploadUrl(fileName: string, contentType?: string): Promise<PresignedUploadVO> {
  return post('/file/presigned-upload', {
    fileName,
    contentType: contentType || 'application/octet-stream'
  })
}

/**
 * 通过预签名URL直传文件到MinIO
 */
export async function uploadToMinIO(file: File, uploadUrl: string): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type || 'application/octet-stream'
    },
    body: file
  })
  if (!response.ok) {
    throw new Error(`上传失败: ${response.status}`)
  }
}
