package com.kakarote.ai_crm.controller;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.VO.ImContactVO;
import com.kakarote.ai_crm.entity.VO.ImConversationVO;
import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.im.ws.ImPresenceService;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImMessageService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "即时通讯")
@RestController
@RequestMapping("/im")
public class ImController {

    @Autowired
    private ImConversationService conversationService;

    @Autowired
    private ImMessageService messageService;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private ImPresenceService presenceService;

    @PostMapping("/conversations/direct")
    @Operation(summary = "查找或创建与某用户的私聊会话")
    @RequirePermission("im")
    public Result<Map<String, String>> openDirect(@RequestBody Map<String, Object> body) {
        Long peerId = parseLong(body.get("userId"));
        ImConversation conv = conversationService.getOrCreateDirect(UserUtil.getUserId(), peerId);
        return Result.ok(Map.of("conversationId", String.valueOf(conv.getId())));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "会话历史消息")
    @RequirePermission("im")
    public Result<List<ImMessageVO>> history(@PathVariable("id") Long conversationId,
                                             @RequestParam(value = "beforeId", required = false) Long beforeId,
                                             @RequestParam(value = "limit", defaultValue = "30") int limit) {
        conversationService.assertMember(conversationId, UserUtil.getUserId());
        return Result.ok(messageService.history(conversationId, beforeId, limit));
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    @RequirePermission("im")
    public Result<ImMessageVO> send(@RequestBody ImSendMessageBO bo) {
        return Result.ok(messageService.send(UserUtil.getUserId(), bo));
    }

    @PostMapping("/messages/{id}/recall")
    @Operation(summary = "撤回消息")
    @RequirePermission("im")
    public Result<ImMessageVO> recall(@PathVariable("id") Long messageId) {
        return Result.ok(messageService.recall(messageId, UserUtil.getUserId()));
    }

    @PostMapping("/conversations/{id}/read")
    @Operation(summary = "标记已读")
    @RequirePermission("im")
    public Result<String> read(@PathVariable("id") Long conversationId) {
        messageService.markRead(conversationId, UserUtil.getUserId());
        return Result.ok("ok");
    }

    @GetMapping("/conversations")
    @Operation(summary = "我的会话列表")
    @RequirePermission("im")
    public Result<List<ImConversationVO>> myConversations() {
        return Result.ok(conversationService.listMyConversations(UserUtil.getUserId()));
    }

    @GetMapping("/contacts")
    @Operation(summary = "可发起私聊的通讯录联系人")
    @RequirePermission("im")
    public Result<List<ImContactVO>> contacts(@RequestParam(value = "keyword", required = false) String keyword) {
        Long me = UserUtil.getUserId();
        UserQueryBO query = new UserQueryBO();
        query.setSearch(StrUtil.trimToNull(keyword));
        query.setPage(1);
        query.setLimit(200);
        List<ImContactVO> list = new ArrayList<>();
        for (ManageUserVO u : manageUserService.queryPageList(query).getRecords()) {
            if (u.getUserId() == null || u.getUserId().equals(me)) {
                continue;
            }
            ImContactVO vo = new ImContactVO();
            vo.setUserId(String.valueOf(u.getUserId()));
            vo.setName(StrUtil.blankToDefault(u.getRealname(), u.getUsername()));
            vo.setAvatarUrl(u.getImgUrl());
            vo.setDeptName(u.getDeptName());
            vo.setOnline(presenceService.isOnline(String.valueOf(u.getUserId())));
            list.add(vo);
        }
        return Result.ok(list);
    }

    private Long parseLong(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return StrUtil.isBlank(s) ? null : Long.valueOf(s);
    }
}
