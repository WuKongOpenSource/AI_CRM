package com.kakarote.ai_crm.ai.app;

import java.util.List;

public record ChatApplicationDefinition(
        String code,
        String label,
        String iconName,
        String description,
        String systemPrompt,
        boolean defaultRagEnabled,
        List<String> toolGroups,
        List<String> recommendedQuestions
) {
}
