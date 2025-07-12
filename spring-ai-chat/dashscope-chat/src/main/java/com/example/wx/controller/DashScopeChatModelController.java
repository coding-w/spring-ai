package com.example.wx.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author wangxiang
 * @description
 * @create 2025/5/21 17:24
 */
@RestController
@RequestMapping("/model")
public class DashScopeChatModelController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。";

    private final ChatModel dashScopeChatModel;

    public DashScopeChatModelController(ChatModel chatModel) {
        this.dashScopeChatModel = chatModel;
    }

    /**
     * 最简单的使用方式，没有任何 LLMs 参数注入。
     * @return String types.
     */
    @GetMapping("/simple/chat")
    public String simpleChat() {

        return dashScopeChatModel.call(new Prompt(DEFAULT_PROMPT, DashScopeChatOptions
                .builder()
                .withModel(DashScopeApi.ChatModel.QWEN_PLUS.getName())
                .build())).getResult().getOutput().getText();
    }

    /**
     * Stream 流式调用。可以使大模型的输出信息实现打字机效果。
     * @return Flux<String> types.
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(HttpServletResponse response) {

        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");

        Flux<ChatResponse> stream = dashScopeChatModel.stream(new Prompt(DEFAULT_PROMPT, DashScopeChatOptions
                .builder()
                .withModel(DashScopeApi.ChatModel.QWEN_PLUS.getName())
                .build()));
        return stream.map(resp -> resp.getResult().getOutput().getText());
    }


    /**
     * 使用编程方式自定义 LLMs ChatOptions 参数， {@link com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions}
     * 优先级高于在 application.yml 中配置的 LLMs 参数！
     */
    @GetMapping("/custom/chat")
    public String customChat() {

        DashScopeChatOptions customOptions = DashScopeChatOptions.builder()
                .withTopP(0.7)
                .withTopK(50)
                .withTemperature(0.8)
                .build();

        return dashScopeChatModel.call(new Prompt(DEFAULT_PROMPT, customOptions)).getResult().getOutput().getText();
    }

}