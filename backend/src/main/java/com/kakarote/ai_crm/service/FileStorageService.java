package com.kakarote.ai_crm.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 * 支持本地存储和 MinIO/RustFS 对象存储
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 存储路径/key
     * @return 文件存储路径
     */
    String upload(MultipartFile file, String path);

    /**
     * 删除文件
     *
     * @param path 文件路径/key
     */
    void delete(String path);

    /**
     * 获取文件访问 URL
     *
     * @param path 文件路径/key
     * @return 文件 URL（直接 URL 或预签名 URL）
     */
    String getUrl(String path);

    /**
     * 获取文件流（用于下载）
     *
     * @param path 文件路径/key
     * @return 文件输入流
     */
    InputStream getFileStream(String path);

    /**
     * 获取文件的本地路径（用于 WeKnora 上传等需要本地文件的场景）
     * 对于本地存储返回实际路径，对于 MinIO 返回 null
     *
     * @param path 文件路径/key
     * @return 本地文件绝对路径，如果不支持则返回 null
     */
    String getLocalPath(String path);

    /**
     * 获取存储类型
     *
     * @return 存储类型标识 (local, minio)
     */
    String getStorageType();

    /**
     * 获取预签名上传URL（用于前端直传）
     *
     * @param path 文件存储路径/key
     * @param contentType 文件MIME类型
     * @param expiry 有效期（秒）
     * @return 预签名上传URL和相关信息
     */
    PresignedUploadInfo getPresignedUploadUrl(String path, String contentType, int expiry);

    /**
     * 预签名上传信息
     */
    record PresignedUploadInfo(
            String uploadUrl,
            String method,
            String bucket,
            String objectKey,
            int expiry
    ) {}
}
