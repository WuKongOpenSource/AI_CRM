package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.MinioConfig;
import com.kakarote.ai_crm.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
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
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class MinioFileStorageService implements FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    private volatile boolean bucketInitialized = false;
    private final Object bucketLock = new Object();

    private void ensureBucketExists() {
        if (bucketInitialized) {
            return;
        }
        synchronized (bucketLock) {
            if (bucketInitialized) {
                return;
            }
            try {
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
                    log.info("MinIO bucket created: {}", minioConfig.getBucket());
                } else {
                    log.info("MinIO bucket already exists: {}", minioConfig.getBucket());
                }
                bucketInitialized = true;
            } catch (Exception e) {
                log.error("MinIO bucket initialization failed: {}", e.getMessage(), e);
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
            log.info("file upload to MinIO success: bucket={}, path={}", minioConfig.getBucket(), path);
            return path;
        } catch (Exception e) {
            log.error("file upload to MinIO failed: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public String upload(InputStream inputStream, long size, String path, String contentType) {
        ensureBucketExists();
        try (InputStream stream = inputStream) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(path)
                            .stream(stream, size, -1)
                            .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream"))
                            .build()
            );
            log.info("stream upload to MinIO success: bucket={}, path={}", minioConfig.getBucket(), path);
            return path;
        } catch (Exception e) {
            log.error("stream upload to MinIO failed: path={}, error={}", path, e.getMessage(), e);
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
            log.info("file delete from MinIO success: bucket={}, path={}", minioConfig.getBucket(), path);
        } catch (Exception e) {
            log.error("file delete from MinIO failed: path={}, error={}", path, e.getMessage(), e);
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
                log.error("get MinIO presigned url failed: path={}, error={}", path, e.getMessage(), e);
                throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "获取文件链接失败");
            }
        }
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
            log.error("get MinIO file stream failed: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件下载失败");
        }
    }

    @Override
    public String getLocalPath(String path) {
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
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucket())
                            .object(path)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build()
            );
            log.info("generate presigned upload url success: bucket={}, path={}, expiry={}s",
                    minioConfig.getBucket(), path, expiry);
            return new PresignedUploadInfo(
                    toPublicUrl(uploadUrl),
                    "PUT",
                    minioConfig.getBucket(),
                    path,
                    expiry
            );
        } catch (Exception e) {
            log.error("generate presigned upload url failed: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "生成上传签名失败");
        }
    }

    private String toPublicUrl(String internalUrl) {
        String publicEndpoint = minioConfig.getPublicEndpoint();
        if (StrUtil.isBlank(publicEndpoint)) {
            return internalUrl;
        }
        return internalUrl.replace(minioConfig.getEndpoint(), publicEndpoint);
    }
}
