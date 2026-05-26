package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeAiSearchBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeTargetedScriptBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiSearchVO;
import com.kakarote.ai_crm.entity.VO.KnowledgePreviewTokenVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.KnowledgePreviewTokenService;
import com.kakarote.ai_crm.utils.DocToHtmlConverter;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/knowledge")
@Tag(name = "Knowledge")
public class KnowledgeController {

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private KnowledgePreviewTokenService previewTokenService;

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    /**
     * 上传知识。
     */
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

    /**
     * 删除知识。
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "Delete knowledge file")
    @RequirePermission("knowledge:delete")
    public Result<String> delete(@PathVariable("id") Long id) {
        knowledgeService.deleteKnowledge(id);
        return Result.ok();
    }

    /**
     * 分页查询知识列表。
     */
    @PostMapping("/queryPageList")
    @Operation(summary = "Query knowledge page")
    @RequirePermission("knowledge:view")
    public Result<BasePage<KnowledgeVO>> queryPageList(@RequestBody KnowledgeQueryBO queryBO) {
        return Result.ok(knowledgeService.queryPageList(queryBO));
    }

    /**
     * 使用 AI 搜索知识。
     */
    @PostMapping("/ai-search")
    @Operation(summary = "AI search knowledge base")
    @RequirePermission("knowledge:view")
    public Result<KnowledgeAiSearchVO> aiSearch(@RequestBody KnowledgeAiSearchBO searchBO) {
        return Result.ok(knowledgeService.aiSearch(searchBO));
    }

    /**
     * Stream targeted sales script content.
     */
    @PostMapping(value = "/targeted-script/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream targeted sales script and SOP")
    @RequirePermission("knowledge:view")
    public Flux<ServerSentEvent<String>> streamTargetedScript(
            @Valid @RequestBody KnowledgeTargetedScriptBO scriptBO) {
        return knowledgeService.streamTargetedScript(scriptBO)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    /**
     * 处理detail方法逻辑。
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "Get knowledge detail")
    @RequirePermission("knowledge:view")
    public Result<KnowledgeVO> detail(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.getKnowledgeDetail(id));
    }

    /**
     * 下载知识。
     */
    @GetMapping("/download/{id}")
    @Operation(summary = "Download knowledge file")
    @RequirePermission("knowledge:download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(id);

        InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath());
        Resource resource = new InputStreamResource(inputStream);

        String encodedFilename = URLEncoder.encode(knowledge.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(resolveMediaType(knowledge))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);

        if (knowledge.getFileSize() != null && knowledge.getFileSize() >= 0) {
            responseBuilder.contentLength(knowledge.getFileSize());
        }

        return responseBuilder.body(resource);
    }

    @PostMapping("/{id}/preview-token")
    @Operation(summary = "Create short-lived media preview URL")
    @RequirePermission("knowledge:view")
    public Result<KnowledgePreviewTokenVO> createPreviewToken(@PathVariable("id") Long id) {
        KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(id);
        if (!isPreviewableMedia(knowledge.getName(), knowledge.getMimeType())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前文件不是可预览的音视频文件");
        }

        KnowledgePreviewTokenService.CreatedToken createdToken = previewTokenService.createToken(
                id,
                UserUtil.getTenantId(),
                UserUtil.getUserId()
        );
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/knowledge/preview-range/{id}")
                .queryParam("previewToken", createdToken.token())
                .buildAndExpand(id)
                .toUriString();

        KnowledgePreviewTokenVO result = new KnowledgePreviewTokenVO();
        result.setUrl(url);
        result.setExpiresAt(Instant.ofEpochMilli(createdToken.expiresAtEpochMillis()).toString());
        result.setExpiresInSeconds(createdToken.expiresInSeconds());
        return Result.ok(result);
    }

    @GetMapping("/preview-range/{id}")
    @Operation(summary = "Preview media file with HTTP Range support")
    public ResponseEntity<?> previewRange(
            @PathVariable("id") Long id,
            @RequestParam("previewToken") String previewToken,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        return previewTokenService.validateToken(previewToken, id)
                .<ResponseEntity<?>>map(payload -> {
                    TenantContextHolder.setTenantId(payload.getTenantId());
                    try {
                        Knowledge knowledge = knowledgeMapper.selectByIdIgnoreDataPermission(id);
                        if (knowledge == null
                                || knowledge.getFilePath() == null
                                || knowledge.getFilePath().isBlank()
                                || !isPreviewableMedia(knowledge.getName(), knowledge.getMimeType())) {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                        }

                        long totalLength = knowledge.getFileSize() == null ? -1L : knowledge.getFileSize();
                        if (rangeHeader == null || rangeHeader.isBlank() || totalLength < 0) {
                            return buildFullMediaResponse(knowledge, totalLength);
                        }
                        if (totalLength <= 0) {
                            return buildRangeNotSatisfiableResponse(0);
                        }

                        ResolvedRange resolvedRange = resolveSingleRange(rangeHeader, totalLength);
                        if (resolvedRange == null) {
                            return buildRangeNotSatisfiableResponse(totalLength);
                        }

                        long contentLength = resolvedRange.end() - resolvedRange.start() + 1;
                        InputStream inputStream = fileStorageService.getFileRangeStream(
                                knowledge.getFilePath(),
                                resolvedRange.start(),
                                contentLength
                        );
                        Resource resource = new InputStreamResource(inputStream);
                        HttpHeaders headers = buildMediaHeaders(knowledge, contentLength);
                        headers.set(HttpHeaders.CONTENT_RANGE,
                                "bytes " + resolvedRange.start() + "-" + resolvedRange.end() + "/" + totalLength);

                        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                                .headers(headers)
                                .body(resource);
                    } catch (BusinessException e) {
                        log.warn("Knowledge media preview failed: knowledgeId={}, code={}, msg={}",
                                id, e.getCode(), e.getMsg());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    } finally {
                        TenantContextHolder.clear();
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 解析媒体类型。
     */
    private MediaType resolveMediaType(KnowledgeVO knowledge) {
        return resolveMediaType(knowledge.getName(), knowledge.getMimeType());
    }

    private MediaType resolveMediaType(Knowledge knowledge) {
        return resolveMediaType(knowledge.getName(), knowledge.getMimeType());
    }

    private MediaType resolveMediaType(String fileName, String mimeType) {
        try {
            if (mimeType != null && !mimeType.isBlank()) {
                return MediaType.parseMediaType(mimeType);
            }
        } catch (Exception ignored) {
            // Fallback to filename-based resolution below.
        }
        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private ResponseEntity<?> buildFullMediaResponse(Knowledge knowledge, long totalLength) {
        InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath());
        Resource resource = new InputStreamResource(inputStream);
        return ResponseEntity.ok()
                .headers(buildMediaHeaders(knowledge, totalLength))
                .body(resource);
    }

    private HttpHeaders buildMediaHeaders(Knowledge knowledge, long contentLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(knowledge));
        headers.add("Accept-Ranges", "bytes");
        headers.set(HttpHeaders.CACHE_CONTROL, "private, no-store");
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodeFilename(knowledge.getName()));
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Accept-Ranges, Content-Range, Content-Length");
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }
        return headers;
    }

    private ResponseEntity<?> buildRangeNotSatisfiableResponse(long totalLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Ranges", "bytes");
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + Math.max(totalLength, 0));
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Accept-Ranges, Content-Range, Content-Length");
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .headers(headers)
                .build();
    }

    private ResolvedRange resolveSingleRange(String rangeHeader, long totalLength) {
        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.size() != 1) {
                return null;
            }
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(totalLength);
            long end = range.getRangeEnd(totalLength);
            if (start < 0 || end < start || start >= totalLength) {
                return null;
            }
            return new ResolvedRange(start, Math.min(end, totalLength - 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isPreviewableMedia(String fileName, String mimeType) {
        String normalizedMimeType = mimeType == null ? "" : mimeType.split(";", 2)[0].trim().toLowerCase();
        if (normalizedMimeType.startsWith("audio/") || normalizedMimeType.startsWith("video/")) {
            return true;
        }
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        return lowerName.endsWith(".mp3")
                || lowerName.endsWith(".wav")
                || lowerName.endsWith(".m4a")
                || lowerName.endsWith(".aac")
                || lowerName.endsWith(".ogg")
                || lowerName.endsWith(".oga")
                || lowerName.endsWith(".opus")
                || lowerName.endsWith(".flac")
                || lowerName.endsWith(".weba")
                || lowerName.endsWith(".mp4")
                || lowerName.endsWith(".webm")
                || lowerName.endsWith(".mov")
                || lowerName.endsWith(".m4v")
                || lowerName.endsWith(".avi")
                || lowerName.endsWith(".mkv")
                || lowerName.endsWith(".ogv")
                || lowerName.endsWith(".3gp");
    }

    private String encodeFilename(String fileName) {
        return URLEncoder.encode(fileName == null ? "preview" : fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private record ResolvedRange(long start, long end) {}

    /**
     * 预览HTML。
     */
    @GetMapping("/preview-html/{id}")
    @Operation(summary = "Preview .doc file as HTML")
    @RequirePermission("knowledge:view")
    public Result<String> previewHtml(@PathVariable("id") Long id) {
        KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(id);
        String name = knowledge.getName();
        String nameLower = (name != null ? name.toLowerCase() : "");
        if (!nameLower.endsWith(".doc") || nameLower.endsWith(".docx")) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "该文件不是 .doc 格式");
        }
        try (InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath())) {
            String html = DocToHtmlConverter.convertToHtml(inputStream);
            return Result.ok(html);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert .doc to HTML, knowledgeId={}", id, e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "文档转换失败，请下载后查看");
        }
    }

    /**
     * 重新解析知识。
     */
    @PostMapping("/reparse/{id}")
    @Operation(summary = "Reparse knowledge file")
    @RequirePermission("knowledge:upload")
    public Result<String> reparse(@PathVariable("id") Long id) {
        knowledgeService.reparseKnowledge(id);
        return Result.ok();
    }

    /**
     * 新增标签。
     */
    @PostMapping("/addTag")
    @Operation(summary = "Add knowledge tag")
    @RequirePermission("knowledge:upload")
    public Result<String> addTag(
            @Parameter(description = "Knowledge ID") @RequestParam Long knowledgeId,
            @Parameter(description = "Tag name") @RequestParam String tagName) {
        knowledgeService.addTag(knowledgeId, tagName);
        return Result.ok();
    }

    /**
     * 更新客户。
     */
    @PostMapping("/updateCustomer")
    @Operation(summary = "Update knowledge related customer")
    @RequirePermission("knowledge:upload")
    public Result<String> updateCustomer(
            @Parameter(description = "Knowledge ID") @RequestParam Long knowledgeId,
            @Parameter(description = "Customer ID") @RequestParam(required = false) Long customerId) {
        knowledgeService.updateCustomer(knowledgeId, customerId);
        return Result.ok();
    }

    /**
     * 使用 AI 分析知识。
     */
    @PostMapping("/{id}/ai-analyze")
    @Operation(summary = "AI analyze knowledge document")
    @RequirePermission("knowledge:view")
    public Result<KnowledgeAiAnalyzeVO> aiAnalyze(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "false") boolean forceRefresh) {
        return Result.ok(knowledgeService.aiAnalyzeDocument(id, forceRefresh));
    }

    /**
     * 发起问答文档。
     */
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
