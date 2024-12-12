package io.github.kszapsza.springairag.adapter.llm.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import io.github.kszapsza.springairag.adapter.application.SystemMessageProperties;
import io.github.kszapsza.springairag.adapter.llm.SystemPromptTemplateProvider;
import io.github.kszapsza.springairag.domain.chat.ChatProvider;
import io.github.kszapsza.springairag.domain.chat.ChatRequest;
import io.github.kszapsza.springairag.domain.chat.ChatResponse;

@Component
public class OpenAiChatProvider implements ChatProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiChatProvider.class);

    private final OpenAiChatModel chatModel;
    private final VectorStore vectorStore;
    private final SystemMessageProperties systemMessageProperties;
    private final SystemPromptTemplate systemPromptTemplate;

    public OpenAiChatProvider(
            OpenAiChatModel chatModel,
            VectorStore vectorStore,
            SystemMessageProperties systemMessageProperties,
            SystemPromptTemplateProvider systemPromptTemplateProvider) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.systemMessageProperties = systemMessageProperties;
        this.systemPromptTemplate = systemPromptTemplateProvider.getSystemPromptTemplate();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (request.message() == null || request.message().trim().isEmpty()) {
            logger.warn("Received a null or empty request message");
            return new ChatResponse.Failure("Request message cannot be null or empty");
        }
        try {
            var content = callChatModel(request.message()).getContent();
            return new ChatResponse.Success(content);
        } catch (Exception ex) {
            return new ChatResponse.Failure(ex.getMessage());
        }
    }

    private Message callChatModel(String userMessageContent) {
        return ChatClient.create(chatModel)
                .prompt()
                .options(buildOptions())
                .advisors(
                        new SimpleLoggerAdvisor(),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
                .system(systemPromptTemplate.createMessage(systemMessageProperties.placeholders()).getContent())
                .user(userMessageContent)
                .call()
                .chatResponse()
                .getResult()
                .getOutput();
    }

    private ChatOptions buildOptions() {
        return OpenAiChatOptions.builder()
                .withFunction("realEstateSearchFunction")
                .build();
    }
}
