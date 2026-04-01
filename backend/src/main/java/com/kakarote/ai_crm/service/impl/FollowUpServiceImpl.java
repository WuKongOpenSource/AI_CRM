package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAiParseBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.service.IFollowUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 跟进记录服务实现
 */
@Slf4j
@Service
public class FollowUpServiceImpl extends ServiceImpl<FollowUpMapper, FollowUp> implements IFollowUpService {

    @Autowired
    private CustomerMapper customerMapper;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_PARSE_PROMPT_TEMPLATE = """
        你是一个专业的 CRM 助手。请分析以下跟进记录，提取关键信息并以 JSON 格式返回。

        客户名称: %s
        当前时间: %s

        跟进内容:
        %s

        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "summary": "简明扼要的摘要（1-2句话）",
          "type": "跟进类型，只能是以下之一: call, meeting, email, visit, other",
          "followTime": "跟进发生的时间，格式 yyyy-MM-dd HH:mm（如未提及则用当前时间）",
          "nextFollowTime": "建议的下次跟进时间，格式 yyyy-MM-dd HH:mm（根据内容合理推断，通常3-7天后）",
          "keyPoints": ["要点1", "要点2"],
          "todos": ["待办事项1", "待办事项2"]
        }
        """;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFollowUp(FollowUpAddBO followUpAddBO) {
        FollowUp followUp = BeanUtil.copyProperties(followUpAddBO, FollowUp.class);
        if (followUp.getFollowTime() == null) {
            followUp.setFollowTime(new Date());
        }
        save(followUp);

        // Update customer's last contact time
        Customer customer = customerMapper.selectById(followUpAddBO.getCustomerId());
        if (customer != null) {
            customer.setLastContactTime(new Date());
            if (followUpAddBO.getNextFollowTime() != null) {
                customer.setNextFollowTime(followUpAddBO.getNextFollowTime());
            }
            customerMapper.updateById(customer);
        }

        return followUp.getFollowUpId();
    }

    @Override
    public void deleteFollowUp(Long followUpId) {
        FollowUp followUp = getById(followUpId);
        if (ObjectUtil.isNull(followUp)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "跟进记录不存在");
        }
        removeById(followUpId);
    }

    @Override
    public List<FollowUpVO> queryByCustomer(Long customerId) {
        List<FollowUp> followUps = lambdaQuery()
            .eq(FollowUp::getCustomerId, customerId)
            .orderByDesc(FollowUp::getFollowTime)
            .list();
        return BeanUtil.copyToList(followUps, FollowUpVO.class);
    }

    @Override
    public BasePage<FollowUpVO> queryPageList(FollowUpQueryBO queryBO) {
        BasePage<FollowUpVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        return page;
    }

    @Override
    public FollowUpAiParseVO aiParseFollowUp(FollowUpAiParseBO parseBO) {
        String customerName = StrUtil.blankToDefault(parseBO.getCustomerName(), "未知客户");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String prompt = String.format(AI_PARSE_PROMPT_TEMPLATE, customerName, now, parseBO.getContent());

        try {
            String response = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("AI 跟进解析原始响应: {}", response);
            return parseAiResponse(response, parseBO.getContent(), now);
        } catch (Exception e) {
            log.error("AI 跟进解析失败，返回默认值", e);
            return buildFallbackResult(parseBO.getContent(), now);
        }
    }

    private FollowUpAiParseVO parseAiResponse(String response, String originalContent, String now) {
        try {
            // Strip markdown code block markers if present
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            FollowUpAiParseVO vo = new FollowUpAiParseVO();
            vo.setSummary(getTextOrDefault(root, "summary", originalContent.length() > 100 ? originalContent.substring(0, 100) + "..." : originalContent));
            vo.setType(getTextOrDefault(root, "type", "other"));
            vo.setFollowTime(getTextOrDefault(root, "followTime", now));
            vo.setNextFollowTime(getTextOrDefault(root, "nextFollowTime", ""));

            List<String> keyPoints = new ArrayList<>();
            if (root.has("keyPoints") && root.get("keyPoints").isArray()) {
                root.get("keyPoints").forEach(n -> keyPoints.add(n.asText()));
            }
            vo.setKeyPoints(keyPoints);

            List<String> todos = new ArrayList<>();
            if (root.has("todos") && root.get("todos").isArray()) {
                root.get("todos").forEach(n -> todos.add(n.asText()));
            }
            vo.setTodos(todos);

            return vo;
        } catch (Exception e) {
            log.warn("AI 响应 JSON 解析失败: {}", e.getMessage());
            return buildFallbackResult(originalContent, now);
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        if (root.has(field) && !root.get(field).isNull()) {
            return root.get(field).asText();
        }
        return defaultValue;
    }

    private FollowUpAiParseVO buildFallbackResult(String content, String now) {
        FollowUpAiParseVO vo = new FollowUpAiParseVO();
        vo.setSummary(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        vo.setType("other");
        vo.setFollowTime(now);
        vo.setNextFollowTime("");
        vo.setKeyPoints(List.of());
        vo.setTodos(List.of());
        return vo;
    }
}
