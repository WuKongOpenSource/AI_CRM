package com.kakarote.ai_crm.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProjectAiCommandParserTest {

    @Test
    void parsesImplicitDeadlineSentenceAsTaskCreation() {
        ProjectAiCommandParser.ParsedCommand command = ProjectAiCommandParser.parse("测试新开发的需求，本周完成");

        assertEquals(ProjectAiCommandParser.Action.CREATE_TASK, command.action());
        assertEquals("测试新开发的需求", command.title());
    }

    @Test
    void doesNotTreatProjectQuestionsAsTaskCreation() {
        ProjectAiCommandParser.ParsedCommand command = ProjectAiCommandParser.parse("这个项目本周能完成吗？");

        assertFalse(command.action() == ProjectAiCommandParser.Action.CREATE_TASK);
    }
}
