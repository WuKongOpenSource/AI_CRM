package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.MailMessageQueryBO;
import com.kakarote.ai_crm.entity.PO.MailMessage;
import com.kakarote.ai_crm.entity.VO.MailMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MailMessageMapper extends BaseMapper<MailMessage> {

    IPage<MailMessageVO> queryPageList(IPage<MailMessageVO> page,
                                       @Param("query") MailMessageQueryBO query,
                                       @Param("tenantId") Long tenantId,
                                       @Param("userId") Long userId);

    MailMessageVO selectDetail(@Param("messageId") Long messageId,
                               @Param("tenantId") Long tenantId,
                               @Param("userId") Long userId);

    java.util.List<MailMessageVO> selectThread(@Param("tenantId") Long tenantId,
                                               @Param("userId") Long userId,
                                               @Param("accountId") Long accountId,
                                               @Param("threadId") String threadId,
                                               @Param("fallbackMessageId") Long fallbackMessageId);

    java.util.List<MailMessageVO> selectCustomerTimeline(@Param("tenantId") Long tenantId,
                                                        @Param("userId") Long userId,
                                                        @Param("customerId") Long customerId,
                                                        @Param("limit") int limit);
}
