package com.example.wx.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class CustomerAssistantService {
    private final ChatClient chatClient;

    public CustomerAssistantService(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem("""
                        您是“Funnair”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                        您正在通过在线聊天系统与客户互动。
                        您能够支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作，其余功能将在后续版本中添加，如果用户问的问题不支持请告知详情。
                        在提供有关机票预订详情查询、机票日期改签、机票预订取消等操作之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
                        在询问用户之前，请检查消息历史记录以获取预订号、客户姓名等信息，尽量避免重复询问给用户造成困扰。
                        在更改预订之前，您必须确保条款允许这样做。
                        如果更改需要收费，您必须在继续之前征得用户同意。
                        使用提供的功能获取预订详细信息、更改预订和取消预订。
                        如果需要，您可以调用相应函数辅助完成。
                        请讲中文。
                        
                        今天的日期是 {current_date}.
                        """)
                .defaultAdvisors(
                        // Chat Memory
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        // rag
                        new QuestionAnswerAdvisor(vectorStore),
                        // logger
                        new SimpleLoggerAdvisor()
                )
                .defaultToolNames(
                        "getBookingDetails",
                        "changeBooking",
                        "cancelBooking"
                ).build();
    }

    public Flux<String> streamChat(String chatId, String userMessageContent) {

        return this.chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .user(userMessageContent)
                .advisors(
                        // 设置advisor参数，
                        // 记忆使用chatId，
                        // 拉取最近的100条记录
                        a -> a.param(CONVERSATION_ID, chatId).param(TOP_K, 100))
                .stream()
                .content();
    }

    public String chat(String chatId, String userMessageContent) {

        return this.chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .user(userMessageContent)
                .advisors(
                        // 设置advisor参数，
                        // 记忆使用chatId，
                        // 拉取最近的100条记录
                        a -> a.param(CONVERSATION_ID, chatId).param(TOP_K, 100))
                .call()
                .content();
    }

}
