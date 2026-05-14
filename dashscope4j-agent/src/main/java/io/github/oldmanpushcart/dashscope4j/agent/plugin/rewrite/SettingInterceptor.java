package io.github.oldmanpushcart.dashscope4j.agent.plugin.rewrite;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class SettingInterceptor implements ChatInterceptor {

    private static final Message REWRITE_QUESTION_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/REWRITE_QUESTION.md")
                    .build()
                    .render())
            .withCache();

    private final ChatModel model;

    public SettingInterceptor(ChatModel model) {
        this.model = model;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        return rewrite(chain.client(), request)
                .thenCompose(chain::proceed);
    }

    private CompletionStage<AigcRequest<Input, Output>> rewrite(DashscopeClient client, AigcRequest<Input, Output> request) {
        final var userInputMessage = request.input().userInputMessage();
        final var rewriteRequest = AigcRequest.newBuilder(model)
                .input(Input.newBuilder()
                        .messages(List.of(
                                REWRITE_QUESTION_MESSAGE,
                                userInputMessage
                        ))
                        .build())
                .build();
        return client.async(rewriteRequest)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(question ->
                        AigcRequest.newBuilder(request)
                                .input(input -> Input.newBuilder()
                                        .messages(messages -> {
                                            final var newMessages = new ArrayList<Message>();
                                            for (int index = 0; index < messages.size() - 1; index++) {
                                                newMessages.add(messages.get(index));
                                            }
                                            newMessages.add(Message.user(question));
                                            return newMessages;
                                        })
                                        .build())
                                .build())
                ;
    }

}
