package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeAiSearchBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeTargetedScriptBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiSearchVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * 知识库服务接口
 */
public interface IKnowledgeService extends IService<Knowledge> {

    /**
     * 上传文件到知识库
     */
    Long uploadFile(MultipartFile file, String type, Long customerId, String summary);

    /**
     * 上传文件到知识库，可关联员工对象。
     */
    Long uploadFile(MultipartFile file, String type, Long customerId, Long employeeId, String summary);

    /**
     * 上传文件到知识库，可关联客户、员工或关系人对象。
     */
    Long uploadFile(MultipartFile file, String type, Long customerId, Long employeeId, Long relationId, String summary);

    /**
     * 上传文件到知识库，可关联客户、员工、关系人或候选人对象。
     */
    Long uploadFile(MultipartFile file, String type, Long customerId, Long employeeId,
                    Long relationId, Long candidateId, String summary);

    /**
     * 将已经上传到文件存储的文件归档到知识库/文档中心。
     */
    Long archiveExistingFile(String fileName, String filePath, Long fileSize, String mimeType, Long customerId, String summary);

    /**
     * Archive an already uploaded standalone file to the knowledge base.
     */
    Long archiveExistingStandaloneFile(String fileName, String filePath, Long fileSize, String mimeType, Long customerId, String summary);

    /**
     * 将已经上传到文件存储的文件归档到员工对象知识库。
     */
    Long archiveExistingEmployeeFile(String fileName, String filePath, Long fileSize, String mimeType, Long employeeId, String summary);

    /**
     * 将已经上传到文件存储的文件归档到关系人知识库。
     */
    Long archiveExistingRelationFile(String fileName, String filePath, Long fileSize, String mimeType, Long relationId, String summary);

    /**
     * 将已经上传到文件存储的文件归档到候选人知识库。
     */
    Long archiveExistingCandidateFile(String fileName, String filePath, Long fileSize, String mimeType, Long candidateId, String summary);

    /**
     * Archive generated text as a knowledge document.
     */
    Long archiveText(String fileName, String contentText, String type, Long customerId, String summary);

    /**
     * 删除知识库文件
     */
    void deleteKnowledge(Long knowledgeId);

    /**
     * 分页查询知识库
     */
    BasePage<KnowledgeVO> queryPageList(KnowledgeQueryBO queryBO);

    /**
     * 获取知识库详情
     */
    KnowledgeVO getKnowledgeDetail(Long knowledgeId);

    /**
     * 重新解析知识库文件
     */
    void reparseKnowledge(Long knowledgeId);

    /**
     * 添加知识库标签
     */
    void addTag(Long knowledgeId, String tagName);

    /**
     * 更新客户。
     */
    void updateCustomer(Long knowledgeId, Long customerId);

    /**
     * AI分析文档内容（核心提炼、推荐话术、关联实体）
     */
    KnowledgeAiAnalyzeVO aiAnalyzeDocument(Long knowledgeId, boolean forceRefresh);

    /**
     * 向AI提问文档内容（流式响应）
     */
    Flux<String> askDocumentQuestion(Long knowledgeId, KnowledgeAskBO askBO);

    /**
     * AI search knowledge base and return answer with referenced documents
     */
    KnowledgeAiSearchVO aiSearch(KnowledgeAiSearchBO searchBO);

    /**
     * Stream targeted sales script content as it is generated.
     */
    Flux<String> streamTargetedScript(KnowledgeTargetedScriptBO scriptBO);
}
