package com.kakarote.ai_crm.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.PresignedUploadBO;
import com.kakarote.ai_crm.entity.VO.PresignedUploadVO;
import com.kakarote.ai_crm.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件存储控制器
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件存储")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${minio.enabled:false}")
    private boolean minioEnabled;

    @Value("${minio.presigned-expiry:3600}")
    private int defaultExpiry;

    @PostMapping("/presigned-upload")
    @Operation(summary = "获取预签名上传URL", description = "前端直传MinIO时使用，返回预签名URL")
    public Result<PresignedUploadVO> getPresignedUploadUrl(@Valid @RequestBody PresignedUploadBO uploadBO) {
        // 生成文件存储路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = FileUtil.extName(uploadBO.getFileName());
        String fileName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(ext) ? "." + ext : "");

        String prefix = StrUtil.isNotBlank(uploadBO.getPrefix()) ? uploadBO.getPrefix() : datePath;
        String objectKey = prefix + "/" + fileName;

        // 确定签名有效期
        int expiry = uploadBO.getExpiry() != null && uploadBO.getExpiry() > 0
                ? uploadBO.getExpiry()
                : defaultExpiry;

        // 确定content type
        String contentType = StrUtil.isNotBlank(uploadBO.getContentType())
                ? uploadBO.getContentType()
                : "application/octet-stream";

        // 获取预签名上传URL
        FileStorageService.PresignedUploadInfo uploadInfo =
                fileStorageService.getPresignedUploadUrl(objectKey, contentType, expiry);

        // 构建返回对象
        PresignedUploadVO vo = new PresignedUploadVO();
        vo.setUploadUrl(uploadInfo.uploadUrl());
        vo.setMethod(uploadInfo.method());
        vo.setBucket(uploadInfo.bucket());
        vo.setObjectKey(uploadInfo.objectKey());
        vo.setExpiry(uploadInfo.expiry());
        // 上传完成后的访问URL
        vo.setAccessUrl(fileStorageService.getUrl(objectKey));

        return Result.ok(vo);
    }

    @GetMapping("/storage-info")
    @Operation(summary = "获取存储配置信息")
    public Result<Map<String, Object>> getStorageInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("enabled", minioEnabled);
        info.put("storageType", fileStorageService.getStorageType());
        info.put("supportPresignedUpload", minioEnabled);
        return Result.ok(info);
    }
}
