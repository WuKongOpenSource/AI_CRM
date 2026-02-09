package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.MinioConfig;
import com.kakarote.ai_crm.service.FileStorageService;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务实现
 * 支持 MinIO 和 RustFS（S3 兼容协议）
 * 当 minio.enabled=true 时使用
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class MinioFileStorageService implements FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 标记 bucket 是否已初始化
     */
    private volatile boolean bucketInitialized = false;
    private final Object bucketLock = new Object();

    /**
     * 懒加载初始化 bucket
     * 在第一次使用 MinIO 时才检查并创建 bucket
     * 解决 CRM 启动时 MinIO 可能尚未就绪的问题
     */
    private void ensureBucketExists() {
        if (bucketInitialized) {
            return;
        }
        synchronized (bucketLock) {
            if (bucketInitialized) {
                return;
            }
            try {
                // 检查 bucket 是否存在，不存在则创建
                boolean found = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .build()
                );
                if (!found) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(minioConfig.getBucket())
                                    .build()
                    );
                    log.info("MinIO bucket 创建成功: {}", minioConfig.getBucket());
                } else {
                    log.info("MinIO bucket 已存在: {}", minioConfig.getBucket());
                }
                bucketInitialized = true;
            } catch (Exception e) {
                log.error("MinIO bucket 初始化失败: {}", e.getMessage(), e);
                throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "MinIO 服务不可用，请稍后重试");
            }
        }
    }

    @Override
    public String upload(MultipartFile file, String path) {
        ensureBucketExists();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(path)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("文件上传到 MinIO 成功: bucket={}, path={}", minioConfig.getBucket(), path);
            return path;
        } catch (Exception e) {
            log.error("文件上传到 MinIO 失败: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public void delete(String path) {
        ensureBucketExists();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(path)
                            .build()
            );
            log.info("从 MinIO 删除文件成功: bucket={}, path={}", minioConfig.getBucket(), path);
        } catch (Exception e) {
            log.error("从 MinIO 删除文件失败: path={}, error={}", path, e.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String path) {
        ensureBucketExists();
        if (minioConfig.isUsePresignedUrl()) {
            try {
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(minioConfig.getBucket())
                                .object(path)
                                .expiry(minioConfig.getPresignedExpiry(), TimeUnit.SECONDS)
                                .build()
                );
                return toPublicUrl(url);
            } catch (Exception e) {
                log.error("获取 MinIO 预签名 URL 失败: path={}, error={}", path, e.getMessage(), e);
                throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "获取文件链接失败");
            }
        }
        // 非预签名模式，返回直接 URL（需要 bucket 设置为 public）
        String baseUrl = StrUtil.isNotBlank(minioConfig.getPublicEndpoint())
                ? minioConfig.getPublicEndpoint()
                : minioConfig.getEndpoint();
        return baseUrl + "/" + minioConfig.getBucket() + "/" + path;
    }

    @Override
    public InputStream getFileStream(String path) {
        ensureBucketExists();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            log.error("从 MinIO 获取文件流失败: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件下载失败");
        }
    }

    @Override
    public String getLocalPath(String path) {
        // MinIO 存储不支持本地路径
        return null;
    }

    @Override
    public String getStorageType() {
        return "minio";
    }

    @Override
    public PresignedUploadInfo getPresignedUploadUrl(String path, String contentType, int expiry) {
        ensureBucketExists();
        try {
            // 使用 MinIO 生成预签名 PUT URL
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucket())
                            .object(path)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build()
            );
            log.info("生成预签名上传URL成功: bucket={}, path={}, expiry={}s",
                    minioConfig.getBucket(), path, expiry);
            return new PresignedUploadInfo(
                    toPublicUrl(uploadUrl),
                    "PUT",
                    minioConfig.getBucket(),
                    path,
                    expiry
            );
        } catch (Exception e) {
            log.error("生成预签名上传URL失败: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "生成上传签名失败");
        }
    }

    /**
     * 将 presigned URL 中的内部 endpoint 替换为外部 publicEndpoint
     * 确保浏览器能访问该 URL
     */
    private String toPublicUrl(String internalUrl) {
        String publicEp = minioConfig.getPublicEndpoint();
        if (StrUtil.isBlank(publicEp)) {
            return internalUrl;
        }
        return internalUrl.replace(minioConfig.getEndpoint(), publicEp);
    }
}
