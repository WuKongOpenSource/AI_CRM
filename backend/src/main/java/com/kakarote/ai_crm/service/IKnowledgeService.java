package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库服务接口
 */
public interface IKnowledgeService extends IService<Knowledge> {

    /**
     * 上传文件到知识库
     */
    Long uploadFile(MultipartFile file, String type, Long customerId, String summary);

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
}
