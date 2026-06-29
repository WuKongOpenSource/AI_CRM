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
import com.kakarote.ai_crm.entity.VO.KnowledgePreviewTokenVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.KnowledgePreviewTokenService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.DocToHtmlConverter;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

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

    @Autowired
    private KnowledgePreviewTokenService previewTokenService;

    @PostMapping("/upload")
    @Operation(summary = "Upload knowledge file")
    @RequirePermission("knowledge:upload")
    public Result<Long> upload(
            @Parameter(description = "File") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Type") @RequestParam(required = false) String type,
            @Parameter(description = "Customer ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "Employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Relation ID") @RequestParam(required = false) Long relationId,
            @Parameter(description = "Candidate ID") @RequestParam(required = false) Long candidateId,
            @Parameter(description = "Summary") @RequestParam(required = false) String summary) {
        Long knowledgeId = knowledgeService.uploadFile(file, type, customerId, employeeId, relationId, candidateId, summary);
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

        MediaType mediaType = resolveMediaType(knowledge.getName(), knowledge.getMimeType());
        String encodedFilename = encodeFilename(knowledge.getName());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        if (knowledge.getFileSize() != null && knowledge.getFileSize() >= 0) {
            builder.contentLength(knowledge.getFileSize());
        }

        return builder.body(resource);
    }

    @PostMapping("/updateCustomer")
    @Operation(summary = "Update knowledge related customer")
    @RequirePermission("knowledge:upload")
    public Result<String> updateCustomer(
            @Parameter(description = "Knowledge ID") @RequestParam Long knowledgeId,
            @Parameter(description = "Customer ID") @RequestParam(required = false) Long customerId) {
        knowledgeService.updateCustomer(knowledgeId, customerId);
        return Result.ok();
    }

    @PostMapping("/{id}/preview-token")
    @Operation(summary = "Create knowledge media preview token")
    @RequirePermission("knowledge:view")
    public Result<KnowledgePreviewTokenVO> createPreviewToken(@PathVariable("id") Long id) {
        Knowledge knowledge = requireKnowledge(id);
        if (!isPreviewableMedia(knowledge.getName(), knowledge.getMimeType())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "File type does not support media preview");
        }
        if (StrUtil.isBlank(knowledge.getFilePath())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "File path is empty");
        }

        KnowledgePreviewTokenService.PreviewToken token =
                previewTokenService.createToken(id, UserUtil.getUserId());
        KnowledgePreviewTokenVO vo = new KnowledgePreviewTokenVO();
        vo.setUrl("/knowledge/preview-range/" + id
                + "?previewToken=" + URLEncoder.encode(token.token(), StandardCharsets.UTF_8));
        vo.setExpiresAt(token.expiresAt().toString());
        vo.setExpiresInSeconds(token.expiresInSeconds());
        return Result.ok(vo);
    }

    @GetMapping("/preview-range/{id}")
    @Operation(summary = "Preview knowledge media with range requests")
    public ResponseEntity<Resource> previewRange(
            @PathVariable("id") Long id,
            @RequestParam("previewToken") String previewToken,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        if (!previewTokenService.validateToken(previewToken, id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Knowledge knowledge = knowledgeService.getById(id);
        if (knowledge == null || StrUtil.isBlank(knowledge.getFilePath())
                || !isPreviewableMedia(knowledge.getName(), knowledge.getMimeType())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        MediaType mediaType = resolveMediaType(knowledge.getName(), knowledge.getMimeType());
        long fileSize = knowledge.getFileSize() == null ? -1 : knowledge.getFileSize();
        if (fileSize <= 0 || StrUtil.isBlank(rangeHeader)) {
            return buildFullMediaResponse(knowledge, mediaType, fileSize);
        }

        ResolvedRange range = resolveSingleRange(rangeHeader, fileSize);
        if (range == null) {
            return buildRangeNotSatisfiableResponse(fileSize);
        }

        InputStream inputStream = fileStorageService.getFileRangeStream(
                knowledge.getFilePath(), range.start(), range.length());
        Resource resource = new InputStreamResource(inputStream);
        HttpHeaders headers = buildMediaHeaders(knowledge, mediaType);
        headers.set("Content-Range", "bytes " + range.start() + "-" + range.end() + "/" + fileSize);
        headers.setContentLength(range.length());

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(resource);
    }

    @GetMapping("/preview-html/{id}")
    @Operation(summary = "Preview knowledge document as HTML")
    @RequirePermission("knowledge:view")
    public Result<String> previewHtml(@PathVariable("id") Long id) {
        Knowledge knowledge = requireKnowledge(id);
        if (StrUtil.isBlank(knowledge.getFilePath())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "File path is empty");
        }

        try (InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath())) {
            return Result.ok(DocToHtmlConverter.convertToHtml(inputStream));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Document conversion failed");
        }
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

        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodeFilename(knowledge.getName()));

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
    public Result<KnowledgeAiAnalyzeVO> aiAnalyze(@PathVariable("id") Long id,
                                                  @RequestParam(defaultValue = "false") boolean forceRefresh) {
        return Result.ok(knowledgeService.aiAnalyzeDocument(id, forceRefresh));
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

    private Knowledge requireKnowledge(Long id) {
        Knowledge knowledge = knowledgeService.getById(id);
        if (knowledge == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Knowledge file does not exist");
        }
        return knowledge;
    }

    private ResponseEntity<Resource> buildFullMediaResponse(Knowledge knowledge, MediaType mediaType, long fileSize) {
        InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath());
        Resource resource = new InputStreamResource(inputStream);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .headers(buildMediaHeaders(knowledge, mediaType));
        if (fileSize >= 0) {
            builder.contentLength(fileSize);
        }
        return builder.body(resource);
    }

    private HttpHeaders buildMediaHeaders(Knowledge knowledge, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("Accept-Ranges", "bytes");
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename*=UTF-8''" + encodeFilename(knowledge.getName()));
        return headers;
    }

    private ResponseEntity<Resource> buildRangeNotSatisfiableResponse(long fileSize) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Ranges", "bytes");
        headers.set("Content-Range", "bytes */" + fileSize);
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .headers(headers)
                .build();
    }

    private ResolvedRange resolveSingleRange(String rangeHeader, long fileSize) {
        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.isEmpty()) {
                return null;
            }
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(fileSize);
            long end = Math.min(range.getRangeEnd(fileSize), fileSize - 1);
            if (start < 0 || end < start || start >= fileSize) {
                return null;
            }
            return new ResolvedRange(start, end, end - start + 1);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private MediaType resolveMediaType(String fileName, String mimeType) {
        if (StrUtil.isNotBlank(mimeType)) {
            try {
                return MediaType.parseMediaType(mimeType);
            } catch (Exception ignored) {
                // Fallback to filename-based detection.
            }
        }
        return MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private boolean isPreviewableMedia(String fileName, String mimeType) {
        String normalizedMime = StrUtil.blankToDefault(mimeType, "").toLowerCase(Locale.ROOT);
        if (normalizedMime.startsWith("audio/") || normalizedMime.startsWith("video/")) {
            return true;
        }
        String lowerName = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lowerName.matches(".*\\.(mp3|wav|m4a|aac|ogg|oga|opus|flac|weba|mp4|webm|mov|m4v|avi|mkv|ogv|3gp)$");
    }

    private String encodeFilename(String fileName) {
        return URLEncoder.encode(StrUtil.blankToDefault(fileName, "file"), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private record ResolvedRange(long start, long end, long length) {
    }
}
