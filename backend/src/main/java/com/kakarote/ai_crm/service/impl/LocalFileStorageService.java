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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    /**
     * 上传本地文件存储。
     */
    @Override
    public String upload(MultipartFile file, String path) {
        try {
            File targetFile = resolveTargetFile(path);

            FileUtil.mkdir(targetFile.getParentFile());
            file.transferTo(targetFile);

            log.info("file upload success: path={}", path);
            return path;
        } catch (Exception e) {
            log.error("file upload failed: path={}, error={}", path, e.getMessage(), e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文件上传失败");
        }
    }

    /**
     * 上传本地文件存储。
     */
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

    /**
     * 获取地址。
     */
    @Override
    public String getUrl(String path) {
        return "/knowledge/download/" + path;
    }

    /**
     * 获取文件流。
     */
    @Override
    public InputStream getFileStream(String path) {
        try {
            File targetFile = resolveTargetFile(path);
            if (!targetFile.exists()) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件不存在");
            }
            return new FileInputStream(targetFile);
        } catch (FileNotFoundException e) {
            log.error("file not found: path={}", path);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件不存在");
        }
    }

    /**
     * 获取文件范围流。
     */
    @Override
    public InputStream getFileRangeStream(String path, long offset, long length) {
        if (offset < 0 || length < 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "invalid file range");
        }
        try {
            File targetFile = resolveTargetFile(path);
            if (!targetFile.exists()) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "file not found");
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "r");
            randomAccessFile.seek(offset);
            return new BoundedRandomAccessFileInputStream(randomAccessFile, length);
        } catch (FileNotFoundException e) {
            log.error("file not found: path={}", path);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "file not found");
        } catch (IOException e) {
            log.error("read local file range failed: path={}, offset={}, length={}", path, offset, length, e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "file read failed");
        }
    }

    @Override
    public String getLocalPath(String path) {
        File targetFile = resolveTargetFile(path);
        return targetFile.getAbsolutePath();
    }

    private File resolveTargetFile(String path) {
        try {
            File baseDir = new File(uploadPath).getCanonicalFile();
            File targetFile = new File(baseDir, path).getCanonicalFile();
            if (!targetFile.toPath().startsWith(baseDir.toPath())) {
                log.warn("blocked local file path outside upload dir: path={}", path);
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "invalid file path");
            }
            return targetFile;
        } catch (IOException e) {
            log.error("resolve local file path failed: path={}", path, e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "file path resolve failed");
        }
    }

    private static class BoundedRandomAccessFileInputStream extends InputStream {

        private final RandomAccessFile file;
        private long remaining;

        private BoundedRandomAccessFileInputStream(RandomAccessFile file, long length) {
            this.file = file;
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = file.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int bytesToRead = (int) Math.min(len, remaining);
            int bytesRead = file.read(b, off, bytesToRead);
            if (bytesRead > 0) {
                remaining -= bytesRead;
            }
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            file.close();
        }
    }

    /**
     * 获取存储类型。
     */
    @Override
    public String getStorageType() {
        return "local";
    }

    /**
     * 获取PresignedUpload地址。
     */
    @Override
    public PresignedUploadInfo getPresignedUploadUrl(String path, String contentType, int expiry) {
        throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "本地存储不支持预签名上传，请启用 MinIO 存储");
    }

}
