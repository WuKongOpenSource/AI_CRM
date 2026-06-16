package com.kakarote.ai_crm.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAiParseBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.BO.FollowUpUpdateBO;
import com.kakarote.ai_crm.entity.VO.FollowUpAttachmentVO;
import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.service.AiAudioTranscriptionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IFollowUpService;
import com.kakarote.ai_crm.utils.DocToHtmlConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 跟进记录控制器
 */
@RestController
@RequestMapping("/followup")
@Tag(name = "跟进记录")
public class FollowUpController {

    @Autowired
    private IFollowUpService followUpService;

    @Autowired
    private AiAudioTranscriptionService aiAudioTranscriptionService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/add")
    @Operation(summary = "添加跟进记录")
    @RequirePermission("followup:create")
    public Result<Long> add(@Valid @RequestBody FollowUpAddBO followUpAddBO) {
        Long followUpId = followUpService.addFollowUp(followUpAddBO);
        return Result.ok(followUpId);
    }

    @PostMapping("/update")
    @Operation(summary = "Update follow-up record")
    @RequirePermission("followup:edit")
    public Result<String> update(@Valid @RequestBody FollowUpUpdateBO followUpUpdateBO) {
        followUpService.updateFollowUp(followUpUpdateBO);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询跟进记录")
    @RequirePermission("followup:view")
    public Result<BasePage<FollowUpVO>> queryPageList(@RequestBody FollowUpQueryBO queryBO) {
        return Result.ok(followUpService.queryPageList(queryBO));
    }

    @PostMapping("/queryByCustomer")
    @Operation(summary = "按客户查询跟进记录")
    @RequirePermission("followup:view")
    public Result<List<FollowUpVO>> queryByCustomer(
            @Parameter(description = "客户ID") @RequestParam Long customerId) {
        return Result.ok(followUpService.queryByCustomer(customerId));
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除跟进记录")
    @RequirePermission("followup:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        followUpService.deleteFollowUp(id);
        return Result.ok();
    }

    @PostMapping("/ai-parse")
    @Operation(summary = "AI 解析跟进内容")
    @RequirePermission("followup:create")
    public Result<FollowUpAiParseVO> aiParse(@Valid @RequestBody FollowUpAiParseBO parseBO) {
        return Result.ok(followUpService.aiParseFollowUp(parseBO));
    }

    @PostMapping("/attachment/{attachmentId}/ai-analyze")
    @Operation(summary = "AI analyze follow-up attachment")
    @RequirePermission("followup:view")
    public Result<FollowUpAttachmentVO> aiAnalyzeAttachment(@PathVariable Long attachmentId) {
        return Result.ok(followUpService.analyzeAttachment(attachmentId));
    }

    @PostMapping("/ai-transcribe")
    @Operation(summary = "AI audio transcription")
    @RequirePermission("followup:create")
    public Result<String> aiTranscribe(@RequestPart("file") MultipartFile file) {
        return Result.ok(aiAudioTranscriptionService.transcribe(file));
    }

    @PostMapping("/attachment/upload")
    @Operation(summary = "Upload follow-up attachment")
    @RequirePermission("followup:create")
    public Result<ChatSendBO.AttachmentDTO> uploadAttachment(@RequestPart("file") MultipartFile file) {
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "attachment");
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = FileUtil.extName(originalName);
        String storedName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(ext) ? "." + ext : "");
        String objectKey = "followup/" + datePath + "/" + storedName;
        fileStorageService.upload(file, objectKey);

        ChatSendBO.AttachmentDTO dto = new ChatSendBO.AttachmentDTO();
        dto.setFileName(originalName);
        dto.setFilePath(objectKey);
        dto.setFileSize(file.getSize());
        dto.setMimeType(file.getContentType());
        return Result.ok(dto);
    }

    @GetMapping("/attachment/{attachmentId}/preview-html")
    @Operation(summary = "Preview follow-up attachment as HTML")
    @RequirePermission("followup:view")
    public Result<String> previewAttachmentHtml(@PathVariable Long attachmentId) {
        FollowUpAttachmentVO attachment = followUpService.getAttachment(attachmentId);
        String fileName = StrUtil.blankToDefault(attachment.getFileName(), "");
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Only .doc files support HTML preview");
        }
        if (StrUtil.isBlank(attachment.getFilePath())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment file path is empty");
        }

        try (InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath())) {
            return Result.ok(DocToHtmlConverter.convertToHtml(inputStream));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Document conversion failed");
        }
    }

    @GetMapping("/attachment/{attachmentId}/download")
    @Operation(summary = "Download follow-up attachment")
    @RequirePermission("followup:view")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        FollowUpAttachmentVO attachment = followUpService.getAttachment(attachmentId);
        InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath());
        Resource resource = new InputStreamResource(inputStream);

        MediaType mediaType = MediaTypeFactory.getMediaType(attachment.getFileName())
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (StrUtil.isNotBlank(attachment.getMimeType())) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getMimeType());
            } catch (Exception ignored) {
                // fallback to filename-based media type
            }
        }

        String encodedFilename = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
            .replace("+", "%20");
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        if (attachment.getFileSize() != null && attachment.getFileSize() >= 0) {
            builder.contentLength(attachment.getFileSize());
        }
        return builder.body(resource);
    }

    @PostMapping("/attachment/{attachmentId}/delete")
    @Operation(summary = "Delete follow-up attachment")
    @RequirePermission("followup:delete")
    public Result<String> deleteAttachment(@PathVariable Long attachmentId) {
        followUpService.deleteAttachment(attachmentId);
        return Result.ok();
    }
}
