package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库管理")
public class KnowledgeController {

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<Long> upload(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "类型") @RequestParam(required = false) String type,
            @Parameter(description = "关联客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "摘要") @RequestParam(required = false) String summary) {
        Long knowledgeId = knowledgeService.uploadFile(file, type, customerId, summary);
        return Result.ok(knowledgeId);
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除知识库文件")
    public Result<String> delete(@PathVariable("id") Long id) {
        knowledgeService.deleteKnowledge(id);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询知识库")
    public Result<BasePage<KnowledgeVO>> queryPageList(@RequestBody KnowledgeQueryBO queryBO) {
        return Result.ok(knowledgeService.queryPageList(queryBO));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取知识库详情")
    public Result<KnowledgeVO> detail(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.getKnowledgeDetail(id));
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载文件")
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

    @GetMapping("/url/{id}")
    @Operation(summary = "获取文件访问URL")
    public Result<String> getFileUrl(@PathVariable("id") Long id) {
        KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(id);
        String url = fileStorageService.getUrl(knowledge.getFilePath());
        return Result.ok(url);
    }

    @PostMapping("/reparse/{id}")
    @Operation(summary = "重新解析知识库文件")
    public Result<String> reparse(@PathVariable("id") Long id) {
        knowledgeService.reparseKnowledge(id);
        return Result.ok();
    }

    @PostMapping("/addTag")
    @Operation(summary = "添加标签")
    public Result<String> addTag(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeId,
            @Parameter(description = "标签名称") @RequestParam String tagName) {
        knowledgeService.addTag(knowledgeId, tagName);
        return Result.ok();
    }

    @PostMapping("/{id}/ai-analyze")
    @Operation(summary = "AI分析文档内容")
    public Result<KnowledgeAiAnalyzeVO> aiAnalyze(@PathVariable("id") Long id) {
        return Result.ok(knowledgeService.aiAnalyzeDocument(id));
    }

    @PostMapping(value = "/{id}/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "向AI提问文档内容（流式响应）")
    public Flux<ServerSentEvent<String>> askDocument(
            @PathVariable("id") Long id,
            @RequestBody KnowledgeAskBO askBO) {
        return knowledgeService.askDocumentQuestion(id, askBO)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
