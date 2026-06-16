package com.kakarote.ai_crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI-CRM 后台启动类
 * @author zhangzhiwei
 */
@SpringBootApplication(excludeName = {
        "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration"
})
@EnableScheduling
public class ManagerApplication {
    /**
     * 启动后端应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(ManagerApplication.class,args);
    }
}
