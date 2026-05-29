package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
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
import com.kakarote.ai_crm.service.IMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/mail", "/email"})
@Tag(name = "Mail")
public class MailController {

    @Autowired
    private IMailService mailService;

    @GetMapping("/auth/status")
    @Operation(summary = "Get mailbox authorization status")
    @RequirePermission("mail:view")
    public Result<MailAuthStatusVO> getAuthStatus() {
        return Result.ok(mailService.getAuthStatus());
    }

    @PostMapping("/auth/connect")
    @Operation(summary = "Connect a custom IMAP/SMTP mailbox")
    @RequirePermission("mail:manage")
    public Result<MailAccountVO> connectMailbox(@Valid @RequestBody MailImapBindBO bindBO) {
        return Result.ok(mailService.bindImapAccount(bindBO));
    }

    @PostMapping("/accounts/imap")
    @Operation(summary = "Bind IMAP mailbox")
    @RequirePermission("mail:manage")
    public Result<MailAccountVO> bindImap(@Valid @RequestBody MailImapBindBO bindBO) {
        return Result.ok(mailService.bindImapAccount(bindBO));
    }

    @GetMapping("/oauth/{provider}/authorize")
    @Operation(summary = "Start mailbox OAuth")
    @RequirePermission("mail:manage")
    public Result<MailOAuthStartVO> startOAuth(@PathVariable String provider) {
        return Result.ok(mailService.startOAuth(provider));
    }

    @GetMapping("/oauth/{provider}/callback")
    @Operation(summary = "Handle mailbox OAuth callback")
    public Result<MailAccountVO> handleOAuthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        return Result.ok(mailService.handleOAuthCallback(provider, code, state));
    }

    @GetMapping("/accounts")
    @Operation(summary = "List mailbox accounts")
    @RequirePermission("mail:view")
    public Result<List<MailAccountVO>> listAccounts() {
        return Result.ok(mailService.listAccounts());
    }

    @PostMapping("/accounts/{accountId}/default")
    @Operation(summary = "Set default mailbox account")
    @RequirePermission("mail:manage")
    public Result<MailAccountVO> setDefaultAccount(@PathVariable Long accountId) {
        return Result.ok(mailService.setDefaultAccount(accountId));
    }

    @PostMapping("/accounts/{accountId}/policy")
    @Operation(summary = "Update mailbox sync policy")
    @RequirePermission("mail:manage")
    public Result<MailAccountVO> updatePolicy(@PathVariable Long accountId,
                                              @RequestBody MailSyncPolicyBO policyBO) {
        policyBO.setAccountId(accountId);
        return Result.ok(mailService.updateAccountPolicy(policyBO));
    }

    @PostMapping("/accounts/{accountId}/sync")
    @Operation(summary = "Sync mailbox account")
    @RequirePermission("mail:sync")
    public Result<MailSyncResultVO> syncAccount(@PathVariable Long accountId) {
        return Result.ok(mailService.syncAccount(accountId));
    }

    @GetMapping("/accounts/{accountId}/sync-logs")
    @Operation(summary = "List mailbox sync logs")
    @RequirePermission("mail:view")
    public Result<List<MailSyncLogVO>> listSyncLogs(@PathVariable Long accountId,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(mailService.listSyncLogs(accountId, limit));
    }

    @PostMapping("/accounts/{accountId}/disconnect")
    @Operation(summary = "Disconnect mailbox account")
    @RequirePermission("mail:delete")
    public Result<String> disconnectAccount(@PathVariable Long accountId) {
        mailService.disconnectAccount(accountId);
        return Result.ok();
    }

    @PostMapping("/messages/queryPageList")
    @Operation(summary = "Query synced mail messages")
    @RequirePermission("mail:view")
    public Result<BasePage<MailMessageVO>> queryMessages(@RequestBody MailMessageQueryBO queryBO) {
        return Result.ok(mailService.queryMessages(queryBO));
    }

    @GetMapping("/inbox")
    @Operation(summary = "Query inbox messages")
    @RequirePermission("mail:view")
    public Result<BasePage<MailMessageVO>> queryInbox(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "15") Integer limit,
                                                      @RequestParam(required = false) Long accountId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String readStatus,
                                                      @RequestParam(required = false) Boolean starred) {
        MailMessageQueryBO queryBO = new MailMessageQueryBO();
        queryBO.setPage(page);
        queryBO.setLimit(limit);
        queryBO.setAccountId(accountId);
        queryBO.setKeyword(keyword);
        queryBO.setReadStatus(readStatus);
        queryBO.setStarred(starred);
        queryBO.setDirection("received");
        return Result.ok(mailService.queryMessages(queryBO));
    }

    @GetMapping("/sent")
    @Operation(summary = "Query sent messages")
    @RequirePermission("mail:view")
    public Result<BasePage<MailMessageVO>> querySent(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "15") Integer limit,
                                                     @RequestParam(required = false) Long accountId,
                                                     @RequestParam(required = false) String keyword) {
        MailMessageQueryBO queryBO = new MailMessageQueryBO();
        queryBO.setPage(page);
        queryBO.setLimit(limit);
        queryBO.setAccountId(accountId);
        queryBO.setKeyword(keyword);
        queryBO.setDirection("sent");
        return Result.ok(mailService.queryMessages(queryBO));
    }

    @GetMapping("/messages/{messageId}")
    @Operation(summary = "Get synced mail detail")
    @RequirePermission("mail:view")
    public Result<MailMessageVO> getMessageDetail(@PathVariable Long messageId) {
        return Result.ok(mailService.getMessageDetail(messageId));
    }

    @GetMapping("/messages/{messageId}/thread")
    @Operation(summary = "Query a synced mail thread")
    @RequirePermission("mail:view")
    public Result<List<MailMessageVO>> queryThread(@PathVariable Long messageId) {
        return Result.ok(mailService.queryThread(messageId));
    }

    @GetMapping("/customers/{customerId}/timeline")
    @Operation(summary = "Query customer mail timeline")
    @RequirePermission("mail:view")
    public Result<List<MailMessageVO>> queryCustomerTimeline(@PathVariable Long customerId,
                                                             @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(mailService.queryCustomerTimeline(customerId, limit));
    }

    @PostMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "Download retained mail attachment on demand")
    @RequirePermission("mail:view")
    public Result<MailAttachmentVO> downloadAttachment(@PathVariable Long attachmentId) {
        return Result.ok(mailService.downloadAttachment(attachmentId));
    }

    @PostMapping("/drafts")
    @Operation(summary = "Create a mail draft")
    @RequirePermission("mail:manage")
    public Result<MailDraftVO> createDraft(@Valid @RequestBody MailDraftCreateBO draftBO) {
        return Result.ok(mailService.createDraft(draftBO));
    }

    @GetMapping("/drafts")
    @Operation(summary = "Query mail drafts")
    @RequirePermission("mail:view")
    public Result<BasePage<MailDraftVO>> queryDrafts(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "15") Integer limit,
                                                     @RequestParam(required = false) Long accountId,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String status) {
        MailDraftQueryBO queryBO = new MailDraftQueryBO();
        queryBO.setPage(page);
        queryBO.setLimit(limit);
        queryBO.setAccountId(accountId);
        queryBO.setKeyword(keyword);
        queryBO.setStatus(status);
        return Result.ok(mailService.queryDrafts(queryBO));
    }

    @PostMapping("/draft/save")
    @Operation(summary = "Save a mail draft")
    @RequirePermission("mail:manage")
    public Result<MailDraftVO> saveDraft(@Valid @RequestBody MailDraftCreateBO draftBO) {
        return Result.ok(mailService.createDraft(draftBO));
    }

    @PostMapping("/drafts/{draftId}")
    @Operation(summary = "Update a mail draft")
    @RequirePermission("mail:manage")
    public Result<MailDraftVO> updateDraft(@PathVariable Long draftId,
                                           @Valid @RequestBody MailDraftCreateBO draftBO) {
        return Result.ok(mailService.updateDraft(draftId, draftBO));
    }

    @PostMapping("/drafts/{draftId}/delete")
    @Operation(summary = "Delete a mail draft")
    @RequirePermission("mail:delete")
    public Result<String> deleteDraft(@PathVariable Long draftId) {
        mailService.deleteDraft(draftId);
        return Result.ok();
    }

    @PostMapping("/send")
    @Operation(summary = "Send a mail draft")
    @RequirePermission("mail:manage")
    public Result<MailMessageVO> sendMail(@Valid @RequestBody MailSendBO sendBO) {
        return Result.ok(mailService.sendMail(sendBO));
    }

    @PostMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message read or unread")
    @RequirePermission("mail:manage")
    public Result<String> markRead(@PathVariable Long messageId,
                                   @RequestParam(defaultValue = "true") boolean read) {
        mailService.markMessageRead(messageId, read);
        return Result.ok();
    }

    @PostMapping("/messages/{messageId}/star")
    @Operation(summary = "Star or unstar a message")
    @RequirePermission("mail:manage")
    public Result<String> starMessage(@PathVariable Long messageId,
                                      @RequestParam(defaultValue = "true") boolean starred) {
        mailService.starMessage(messageId, starred);
        return Result.ok();
    }

    @PostMapping("/messages/{messageId}/delete")
    @Operation(summary = "Delete a message")
    @RequirePermission("mail:delete")
    public Result<String> deleteMessage(@PathVariable Long messageId) {
        mailService.deleteMessage(messageId);
        return Result.ok();
    }

    @GetMapping("/templates")
    @Operation(summary = "Query mail templates")
    @RequirePermission("mail:view")
    public Result<BasePage<MailTemplateVO>> queryTemplates(@RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "15") Integer limit,
                                                           @RequestParam(required = false) String category,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Boolean commonOnly) {
        MailTemplateQueryBO queryBO = new MailTemplateQueryBO();
        queryBO.setPage(page);
        queryBO.setLimit(limit);
        queryBO.setCategory(category);
        queryBO.setKeyword(keyword);
        queryBO.setCommonOnly(commonOnly);
        return Result.ok(mailService.queryTemplates(queryBO));
    }

    @PostMapping("/template/create")
    @Operation(summary = "Create a mail template")
    @RequirePermission("mail:manage")
    public Result<MailTemplateVO> createTemplate(@Valid @RequestBody MailTemplateSaveBO saveBO) {
        saveBO.setTemplateId(null);
        return Result.ok(mailService.saveTemplate(saveBO));
    }

    @PostMapping("/templates/{templateId}")
    @Operation(summary = "Update a mail template")
    @RequirePermission("mail:manage")
    public Result<MailTemplateVO> updateTemplate(@PathVariable Long templateId,
                                                 @Valid @RequestBody MailTemplateSaveBO saveBO) {
        saveBO.setTemplateId(templateId);
        return Result.ok(mailService.saveTemplate(saveBO));
    }

    @PostMapping("/templates/{templateId}/copy")
    @Operation(summary = "Copy a mail template")
    @RequirePermission("mail:manage")
    public Result<MailTemplateVO> copyTemplate(@PathVariable Long templateId) {
        return Result.ok(mailService.copyTemplate(templateId));
    }

    @PostMapping("/templates/{templateId}/delete")
    @Operation(summary = "Delete a mail template")
    @RequirePermission("mail:delete")
    public Result<String> deleteTemplate(@PathVariable Long templateId) {
        mailService.deleteTemplate(templateId);
        return Result.ok();
    }

    @PostMapping("/messages/associate-customer")
    @Operation(summary = "Associate mail with customer")
    @RequirePermission("mail:manage")
    public Result<String> associateCustomer(@Valid @RequestBody MailAssociateCustomerBO associateBO) {
        mailService.associateCustomer(associateBO);
        return Result.ok();
    }
}
