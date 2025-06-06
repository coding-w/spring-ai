package com.example.wx.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * @author wangxiang
 * @description
 * @create 2025/5/25 16:51
 */
@RestController
@RequestMapping("prompt")
public class RoleController {
    private final ChatClient chatClient;
    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    public RoleController(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem("中文回答").build();
    }

    @GetMapping("/roles")
    public Flux<String> generate(
            @RequestParam(
                    value = "message",
                    required = false,
                    defaultValue = "Tell me about three famous pirates from the Golden Age of Piracy and why they did.  Write at least a sentence for each pirate.") String message,
            @RequestParam(value = "name", required = false, defaultValue = "Bob") String name,
            @RequestParam(value = "voice", required = false, defaultValue = "pirate") String voice
    ) {

        // 用户输入
        UserMessage userMessage = new UserMessage(message);

        // 使用 System prompt tmpl
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        // 填充 System prompt 中的变量值
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", name, "voice", voice));

        // 调用大模型
        return chatClient.prompt(
                        new Prompt(List.of(
                                userMessage,
                                systemMessage)))
                .stream().content();
    }
}
