package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 跟进记录控制器
 */
@Slf4j
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

    /**
     * 添加跟进记录。
     */
    @PostMapping("/add")
    @Operation(summary = "添加跟进记录")
    @RequirePermission("followup:create")
    public Result<Long> add(@Valid @RequestBody FollowUpAddBO followUpAddBO) {
        Long followUpId = followUpService.addFollowUp(followUpAddBO);
        return Result.ok(followUpId);
    }

    /**
     * 更新跟进记录。
     */
    @PostMapping("/update")
    @Operation(summary = "更新跟进记录")
    @RequirePermission("followup:edit")
    public Result<String> update(@Valid @RequestBody FollowUpUpdateBO followUpUpdateBO) {
        followUpService.updateFollowUp(followUpUpdateBO);
        return Result.ok();
    }

    /**
     * 分页查询跟进记录。
     */
    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询跟进记录")
    @RequirePermission("followup:view")
    public Result<BasePage<FollowUpVO>> queryPageList(@RequestBody FollowUpQueryBO queryBO) {
        return Result.ok(followUpService.queryPageList(queryBO));
    }

    /**
     * 按客户查询跟进记录。
     */
    @PostMapping("/queryByCustomer")
    @Operation(summary = "按客户查询跟进记录")
    @RequirePermission("followup:view")
    public Result<List<FollowUpVO>> queryByCustomer(
            @Parameter(description = "客户ID") @RequestParam Long customerId) {
        return Result.ok(followUpService.queryByCustomer(customerId));
    }

    /**
     * 删除跟进记录。
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除跟进记录")
    @RequirePermission("followup:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        followUpService.deleteFollowUp(id);
        return Result.ok();
    }

    /**
     * AI 解析跟进内容。
     */
    @PostMapping("/ai-parse")
    @Operation(summary = "AI 解析跟进内容")
    @RequirePermission("followup:create")
    public Result<FollowUpAiParseVO> aiParse(@Valid @RequestBody FollowUpAiParseBO parseBO) {
        return Result.ok(followUpService.aiParseFollowUp(parseBO));
    }

    /**
     * 使用 AI 分析附件。
     */
    @PostMapping("/attachment/{attachmentId}/ai-analyze")
    @Operation(summary = "AI analyze follow-up attachment")
    @RequirePermission("followup:view")
    public Result<FollowUpAttachmentVO> aiAnalyzeAttachment(@PathVariable Long attachmentId) {
        return Result.ok(followUpService.analyzeAttachment(attachmentId));
    }

    /**
     * 处理aiTranscribe方法逻辑。
     */
    @PostMapping("/ai-transcribe")
    @Operation(summary = "AI audio transcription")
    @RequirePermission("followup:create")
    public Result<String> aiTranscribe(@RequestPart("file") MultipartFile file) {
        return Result.ok(aiAudioTranscriptionService.transcribe(file));
    }

    /**
     * 预览附件HTML。
     */
    @GetMapping("/attachment/{attachmentId}/preview-html")
    @Operation(summary = "Preview follow-up .doc attachment as HTML")
    @RequirePermission("followup:view")
    public Result<String> previewAttachmentHtml(@PathVariable Long attachmentId) {
        FollowUpAttachmentVO attachment = followUpService.getAttachment(attachmentId);
        String fileName = attachment.getFileName();
        String fileNameLower = fileName != null ? fileName.toLowerCase() : "";
        if (!fileNameLower.endsWith(".doc") || fileNameLower.endsWith(".docx")) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "该文件不是 .doc 格式");
        }
        try (InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath())) {
            return Result.ok(DocToHtmlConverter.convertToHtml(inputStream));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert .doc follow-up attachment to HTML, attachmentId={}", attachmentId, e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文档转换失败，请下载后查看");
        }
    }

    /**
     * 下载附件。
     */
    @GetMapping("/attachment/{attachmentId}/download")
    @Operation(summary = "Download follow-up attachment")
    @RequirePermission("followup:view")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        FollowUpAttachmentVO attachment = followUpService.getAttachment(attachmentId);
        InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath());
        Resource resource = new InputStreamResource(inputStream);

        MediaType mediaType = MediaTypeFactory.getMediaType(attachment.getFileName())
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (attachment.getMimeType() != null && !attachment.getMimeType().isBlank()) {
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
}
