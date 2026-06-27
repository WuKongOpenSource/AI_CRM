package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImChannelServiceTest {

    @Mock ImConversationMapper conversationMapper;
    @Mock ImConversationMemberMapper memberMapper;
    ImConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImConversationServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        // no existing members by default
        when(memberMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void createChannelSetsTypeOwnerVisibilityAndAddsCreatorPlusMembers() {
        when(conversationMapper.insert(any(ImConversation.class))).thenAnswer(inv -> {
            ((ImConversation) inv.getArgument(0)).setId(500L);
            return 1;
        });

        ImConversation conv = service.createChannel(1L, "  研发组  ", "desc", "private", List.of(2L, 3L, 2L));

        assertThat(conv.getType()).isEqualTo("channel");
        assertThat(conv.getName()).isEqualTo("研发组");
        assertThat(conv.getVisibility()).isEqualTo("private");
        assertThat(conv.getOwnerId()).isEqualTo(1L);
        // creator(1) + 2 + 3 distinct = 3 member rows
        verify(memberMapper, times(3)).insert(any(ImConversationMember.class));
    }

    @Test
    void createChannelRejectsBlankName() {
        assertThatThrownBy(() -> service.createChannel(1L, "  ", null, "public", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void joinRejectsPrivateChannel() {
        ImConversation c = new ImConversation();
        c.setId(7L); c.setType("channel"); c.setVisibility("private");
        when(conversationMapper.selectById(7L)).thenReturn(c);
        assertThatThrownBy(() -> service.joinChannel(9L, 7L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void joinAddsMemberForPublicChannel() {
        ImConversation c = new ImConversation();
        c.setId(7L); c.setType("channel"); c.setVisibility("public");
        when(conversationMapper.selectById(7L)).thenReturn(c);
        service.joinChannel(9L, 7L);
        verify(memberMapper).insert(any(ImConversationMember.class));
    }

    @Test
    void addMembersRequiresActorToBeMember() {
        ImConversation c = new ImConversation();
        c.setId(7L); c.setType("channel"); c.setVisibility("public");
        when(conversationMapper.selectById(7L)).thenReturn(c);
        // assertMember uses memberMapper.selectCount on (conv,user). Make actor NOT a member:
        when(memberMapper.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.addMembers(2L, 7L, List.of(3L)))
                .isInstanceOf(BusinessException.class);
    }
}
