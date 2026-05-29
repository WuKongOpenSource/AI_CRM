package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.MailAssociateCustomerBO;
import com.kakarote.ai_crm.entity.BO.MailDraftCreateBO;
import com.kakarote.ai_crm.entity.BO.MailDraftQueryBO;
import com.kakarote.ai_crm.entity.BO.MailImapBindBO;
import com.kakarote.ai_crm.entity.BO.MailMessageQueryBO;
import com.kakarote.ai_crm.entity.BO.MailSendBO;
import com.kakarote.ai_crm.entity.BO.MailSyncPolicyBO;
import com.kakarote.ai_crm.entity.BO.MailTemplateQueryBO;
import com.kakarote.ai_crm.entity.BO.MailTemplateSaveBO;
import com.kakarote.ai_crm.entity.VO.MailAccountVO;
import com.kakarote.ai_crm.entity.VO.MailAttachmentVO;
import com.kakarote.ai_crm.entity.VO.MailAuthStatusVO;
import com.kakarote.ai_crm.entity.VO.MailDraftVO;
import com.kakarote.ai_crm.entity.VO.MailMessageVO;
import com.kakarote.ai_crm.entity.VO.MailOAuthStartVO;
import com.kakarote.ai_crm.entity.VO.MailSyncLogVO;
import com.kakarote.ai_crm.entity.VO.MailSyncResultVO;
import com.kakarote.ai_crm.entity.VO.MailTemplateVO;

import java.util.List;

public interface IMailService {

    MailAccountVO bindImapAccount(MailImapBindBO bindBO);

    MailOAuthStartVO startOAuth(String provider);

    MailAccountVO handleOAuthCallback(String provider, String code, String state);

    MailAuthStatusVO getAuthStatus();

    List<MailAccountVO> listAccounts();

    MailAccountVO setDefaultAccount(Long accountId);

    MailAccountVO updateAccountPolicy(MailSyncPolicyBO policyBO);

    MailSyncResultVO syncAccount(Long accountId);

    void syncDueAccounts();

    BasePage<MailMessageVO> queryMessages(MailMessageQueryBO queryBO);

    MailMessageVO getMessageDetail(Long messageId);

    List<MailMessageVO> queryThread(Long messageId);

    List<MailMessageVO> queryCustomerTimeline(Long customerId, int limit);

    List<MailSyncLogVO> listSyncLogs(Long accountId, int limit);

    MailAttachmentVO downloadAttachment(Long attachmentId);

    MailDraftVO createDraft(MailDraftCreateBO draftBO);

    BasePage<MailDraftVO> queryDrafts(MailDraftQueryBO queryBO);

    MailDraftVO updateDraft(Long draftId, MailDraftCreateBO draftBO);

    void deleteDraft(Long draftId);

    MailMessageVO sendMail(MailSendBO sendBO);

    void markMessageRead(Long messageId, boolean read);

    void starMessage(Long messageId, boolean starred);

    void deleteMessage(Long messageId);

    BasePage<MailTemplateVO> queryTemplates(MailTemplateQueryBO queryBO);

    MailTemplateVO saveTemplate(MailTemplateSaveBO saveBO);

    void deleteTemplate(Long templateId);

    MailTemplateVO copyTemplate(Long templateId);

    void associateCustomer(MailAssociateCustomerBO associateBO);

    void disconnectAccount(Long accountId);
}
