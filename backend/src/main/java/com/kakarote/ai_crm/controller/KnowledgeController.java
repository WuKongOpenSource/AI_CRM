package com.kakarote.ai_crm.controller;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeAiSearchBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeTargetedScriptBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiSearchVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/knowledge")
@Tag(name = "Knowledge")
public class KnowledgeController {

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WeKnoraClient weKnoraClient;

    @PostMapping("/upload")
    @Operation(summary = "Upload knowledge file")
    @RequirePermission("knowledge:upload")
    public Result<Long> upload(
            @Parameter(description = "File") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Type") @RequestParam(required = false) String type,
            @Parameter(description = "Customer ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "Summary") @RequestParam(required = false) String summary) {
        Long knowledgeId = knowledgeService.uploadFile(file, type, customerId, summary);
        return Result.ok(knowledgeId);
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "Delete knowledge file")
    @RequirePermission("knowledge:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        knowledgeService.deleteKnowledge(id);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "Query knowledge page")
    @RequirePermission("knowledge:view")
    public Result<BasePage<KnowledgeVO>> queryPageList(@RequestBody KnowledgeQueryBO queryBO) {
        return Result.ok(knowledgeService.queryPageList(queryBO));
    }

    @PostMapping("/ai-search")
    @Operation(summary = "AI search knowledge base")
    @RequirePermission("knowledge:view")
    public Result<KnowledgeAiSearchVO> aiSearch(@RequestBody KnowledgeAiSearchBO searchBO) {
        return Result.ok(knowledgeService.aiSearch(searchBO));
    }

    @PostMapping(value = "/targeted-script/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream targeted sales script")
    @RequirePermission("knowledge:view")
    public Flux<ServerSentEvent<String>> streamTargetedScript(
            @Valid @RequestBody KnowledgeTargetedScriptBO scriptBO) {
        return knowledgeService.streamTargetedScript(scriptBO)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "Get knowledge detail")
    @RequirePermission("knowledge:view")
    public Result<KnowledgeVO> detail(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.getKnowledgeDetail(id));
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "Download knowledge file")
    @RequirePermission("knowledge:download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(id);

        InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath());
        Resource resource = new InputStreamResource(inputStream);

        String encodedFilename = URLEncoder.encode(knowledge.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/preview/{id}")
    @Operation(summary = "Preview knowledge file")
    @RequirePermission("knowledge:view")
    public ResponseEntity<Resource> preview(@PathVariable("id") Long id) {
        if (!weKnoraClient.isEnabled()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeKnora preview is not enabled");
        }

        Knowledge knowledge = knowledgeService.getById(id);
        if (knowledge == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Knowledge file does not exist");
        }
        if (StrUtil.isBlank(knowledge.getWeKnoraKnowledgeId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Knowledge file is not synced to WeKnora yet");
        }

        WeKnoraClient.WeKnoraPreviewResult preview = weKnoraClient.getKnowledgePreview(knowledge.getWeKnoraKnowledgeId());
        if (preview == null || preview.getBody() == null || preview.getBody().length == 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Knowledge preview is unavailable");
        }

        ByteArrayResource resource = new ByteArrayResource(preview.getBody());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(preview.getContentType() != null ? preview.getContentType() : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(preview.getBody().length);

        String encodedFilename = URLEncoder.encode(knowledge.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @PostMapping("/reparse/{id}")
    @Operation(summary = "Reparse knowledge file")
    @RequirePermission("knowledge:upload")
    public Result<String> reparse(@PathVariable("id") Long id) {
        knowledgeService.reparseKnowledge(id);
        return Result.ok();
    }

    @PostMapping("/addTag")
    @Operation(summary = "Add knowledge tag")
    @RequirePermission("knowledge:upload")
    public Result<String> addTag(
            @Parameter(description = "Knowledge ID") @RequestParam Long knowledgeId,
            @Parameter(description = "Tag name") @RequestParam String tagName) {
        knowledgeService.addTag(knowledgeId, tagName);
        return Result.ok();
    }

    @PostMapping("/{id}/ai-analyze")
    @Operation(summary = "AI analyze knowledge document")
    @RequirePermission("knowledge:view")
    public Result<KnowledgeAiAnalyzeVO> aiAnalyze(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.aiAnalyzeDocument(id));
    }

    @PostMapping(value = "/{id}/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Ask AI about the document")
    @RequirePermission("knowledge:view")
    public Flux<ServerSentEvent<String>> askDocument(
            @PathVariable("id") Long id,
            @RequestBody KnowledgeAskBO askBO) {
        return knowledgeService.askDocumentQuestion(id, askBO)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
