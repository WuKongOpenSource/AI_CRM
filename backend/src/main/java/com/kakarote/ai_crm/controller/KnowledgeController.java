package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/addTag")
    @Operation(summary = "添加标签")
    public Result<String> addTag(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeId,
            @Parameter(description = "标签名称") @RequestParam String tagName) {
        knowledgeService.addTag(knowledgeId, tagName);
        return Result.ok();
    }
}
