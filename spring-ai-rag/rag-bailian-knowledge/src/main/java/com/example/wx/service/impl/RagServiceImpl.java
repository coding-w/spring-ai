package com.example.wx.service.impl;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentCloudReader;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.example.wx.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author wangxiang
 * @description
 * @create 2025/7/13 14:26
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);

    private static final String indexName = "learn";

    @Value("classpath:/data/spring_ai_alibaba_quickstart.pdf")
    private Resource springAiResource;

    private static final String retrievalSystemTemplate = """
			Context information is below.
			---------------------
			{question_answer_context}
			---------------------
			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""";

    private final ChatClient chatClient;

    private final DashScopeApi dashscopeApi;

    public RagServiceImpl(ChatClient.Builder builder, DashScopeApi dashscopeApi) {
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
                DashScopeDocumentRetrieverOptions.builder().withIndexName(indexName).build());

        this.dashscopeApi = dashscopeApi;
        this.chatClient = builder
                .defaultAdvisors(new DocumentRetrievalAdvisor(retriever, new SystemPromptTemplate(retrievalSystemTemplate)))
                .build();
    }

    @Override
    public void importDocuments() {
        String path = saveToTempFile(springAiResource);
        // 1. import and split documents
        DocumentReader reader = new DashScopeDocumentCloudReader(path, dashscopeApi, null);
        List<Document> documentList = reader.get();
        logger.info("{} documents loaded and split", documentList.size());
        // 2. add documents to DashScope cloud storage
        VectorStore vectorStore = new DashScopeCloudStore(dashscopeApi, new DashScopeStoreOptions(indexName));
        vectorStore.add(documentList);
        logger.info("{} documents added to dashscope cloud vector store", documentList.size());
    }

    @Override
    public Flux<ChatResponse> retrieve(String message) {
        return chatClient.prompt().system(s->s.param("question_answer_context", message)).user(message).stream().chatResponse();
    }

    private String saveToTempFile(Resource springAiResource) {
        try {
            File tempFile = File.createTempFile("spring_ai_alibaba_quickstart", ".pdf");
            tempFile.deleteOnExit();

            try (InputStream inputStream = springAiResource.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile.getAbsolutePath();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
