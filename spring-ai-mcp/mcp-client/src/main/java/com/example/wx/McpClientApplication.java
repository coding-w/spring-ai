package com.example.wx;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Set;

/**
 * @author wangxiang
 * @description
 * @create 2025/6/29 16:05
 */
@SpringBootApplication
public class McpClientApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().filename(".env").load();
        Set<String> set = Set.of("DEEPSEEK_API_KEY");
        for (String item : set) {
            System.setProperty(item, dotenv.get(item));
        }
        SpringApplication.run(McpClientApplication.class, args);
    }

    public final static String prompt= "15+12";

    @Bean
    CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder,
                                        ToolCallbackProvider tools,
                                        ConfigurableApplicationContext context) {
        return args -> {
            for (ToolCallback toolCallback : tools.getToolCallbacks()) {
                System.out.println("toolCallback.getToolDefinition().name() = " + toolCallback.getToolDefinition().name());
                System.out.println("toolCallback.getToolDefinition().description() = " + toolCallback.getToolDefinition().description());
            }
            ChatClient chatClient = chatClientBuilder.defaultToolCallbacks(tools).build();
            System.out.println("\n>>> QUESTION: " + prompt);
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(prompt).call().content());
            context.close();
        };
    }
}