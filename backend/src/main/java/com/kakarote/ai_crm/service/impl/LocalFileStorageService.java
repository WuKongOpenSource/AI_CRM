package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.io.FileUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Override
    public String upload(MultipartFile file, String path) {
        try {
            File baseDir = new File(uploadPath).getAbsoluteFile();
            File targetFile = new File(baseDir, path);

            FileUtil.mkdir(targetFile.getParentFile());
            file.transferTo(targetFile);

            log.info("file upload success: path={}", path);
            return path;
        } catch (Exception e) {
            log.error("file upload failed: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public String upload(InputStream inputStream, long size, String path, String contentType) {
        try (InputStream stream = inputStream) {
            File baseDir = new File(uploadPath).getAbsoluteFile();
            File targetFile = new File(baseDir, path);

            FileUtil.mkdir(targetFile.getParentFile());
            FileUtil.writeFromStream(stream, targetFile);

            log.info("stream upload success: path={}", path);
            return path;
        } catch (Exception e) {
            log.error("stream upload failed: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public void delete(String path) {
        try {
            File baseDir = new File(uploadPath).getAbsoluteFile();
            File targetFile = new File(baseDir, path);
            if (targetFile.exists()) {
                FileUtil.del(targetFile);
                log.info("file delete success: path={}", path);
            }
        } catch (Exception e) {
            log.error("file delete failed: path={}, error={}", path, e.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String path) {
        return "/knowledge/download/" + path;
    }

    @Override
    public InputStream getFileStream(String path) {
        try {
            File baseDir = new File(uploadPath).getAbsoluteFile();
            File targetFile = new File(baseDir, path);
            if (!targetFile.exists()) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件不存在");
            }
            return new FileInputStream(targetFile);
        } catch (FileNotFoundException e) {
            log.error("file not found: path={}", path);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件不存在");
        }
    }

    @Override
    public String getLocalPath(String path) {
        File baseDir = new File(uploadPath).getAbsoluteFile();
        File targetFile = new File(baseDir, path);
        return targetFile.getAbsolutePath();
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    public PresignedUploadInfo getPresignedUploadUrl(String path, String contentType, int expiry) {
        throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "本地存储不支持预签名上传，请启用 MinIO 存储");
    }
}
