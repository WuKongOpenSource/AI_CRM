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
 * 当 minio.enabled=false 时使用本地存储
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

            // 创建目录
            FileUtil.mkdir(targetFile.getParentFile());

            // 保存文件
            file.transferTo(targetFile);

            log.info("文件上传成功: path={}", path);
            return path;
        } catch (Exception e) {
            log.error("文件上传失败: path={}, error={}", path, e.getMessage(), e);
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
                log.info("文件删除成功: path={}", path);
            }
        } catch (Exception e) {
            log.error("文件删除失败: path={}, error={}", path, e.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String path) {
        // 本地存储返回相对路径，需要通过 Controller 下载接口访问
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
            log.error("文件不存在: path={}", path);
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
        throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "本地存储不支持预签名上传，请启用MinIO存储");
    }
}
