package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import java.util.List;
import java.util.Set;

public interface ImReactionService extends IService<ImMessageReaction> {

    Set<String> ALLOWED_EMOJI = Set.of("👍", "❤️", "😂", "🎉", "👀", "✅", "🙏", "😄");

    /** Toggle the emoji for (message, caller). Returns the message's aggregated reactions for the caller. */
    List<ImReactionVO> toggle(Long userId, Long messageId, String emoji);

    /** Aggregate a message's reactions from the given viewer's perspective. */
    List<ImReactionVO> aggregate(Long messageId, Long viewerId);
}
