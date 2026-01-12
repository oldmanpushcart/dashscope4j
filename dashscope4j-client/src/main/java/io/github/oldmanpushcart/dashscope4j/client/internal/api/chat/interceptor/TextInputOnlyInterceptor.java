package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class TextInputOnlyInterceptor implements FlowInterceptor, AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {
        return chain.proceed(processApiRequest(chain.request()));
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {
        return chain.proceed(processApiRequest(chain.request()));
    }

    private ApiRequest<?> processApiRequest(ApiRequest<?> request) {

        if (!(request instanceof ChatRequest chatRequest)) {
            return request;
        }

        final var model = chatRequest.model();
        if (!model.tags().contains(ChatModelTags.TEXT_INPUT_ONLY)) {
            return request;
        }

        final var newMessages = chatRequest.messages().stream()
                .map(message -> {
                    if (message instanceof SystemMessage system) {
                        return Message.system(system.text());
                    }
                    if (message instanceof AssistantMessage assistant) {
                        return Message.assistant(assistant.text());
                    }
                    if (message instanceof UserMessage user) {
                        return Message.user(user.text());
                    }
                    return message;
                })
                .toList();

        return ChatRequest.newBuilder(chatRequest)
                .messages(newMessages)
                .build();
    }

}
