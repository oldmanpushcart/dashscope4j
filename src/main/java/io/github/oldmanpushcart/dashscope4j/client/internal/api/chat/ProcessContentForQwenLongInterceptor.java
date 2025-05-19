package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * 处理 {@link ChatModel#QWEN_LONG} 模型对话消息中的多媒体内容
 */
class ProcessContentForQwenLongInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理对话请求
        if (!(chain.request() instanceof ChatRequest)) {
            return chain.process(chain.request());
        }

        // 只处理 qwen-long 模型对话请求
        final ChatRequest request = (ChatRequest) chain.request();
        if (!request.model().name().equals("qwen-long")) {
            return chain.process(chain.request());
        }

        final List<Message> newMessages = new ArrayList<>();
        request.messages()
                .forEach(message -> {

                    // 如果消息中没有文件内容则不需要处理
                    if (message.mediaContents(Content.Type.FILE).isEmpty()) {
                        newMessages.add(message);
                        return;
                    }

                    // 处理消息
                    newMessages.addAll(processMessage(message));

                });

        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .messages(newMessages)
                .build();

        return chain.process(newRequest);
    }


    /*
     * 处理消息
     *
     * 将消息中所有file-id的资源都拆分为一个独立的System消息
     */
    private List<Message> processMessage(Message message) {

        final List<Message> messages = new ArrayList<>();

        /*
         * 将需要拆分的内容进行拆分
         * 将拆分出来的每个内容都作为一个System消息
         */
        message.contents()
                .stream()
                .filter(this::isContentSplitNeeded)
                .map(content -> Message.ofSystem(content.data().toString()))
                .forEach(messages::add);

        /*
         * 将不需要拆分的内容作为用户消息
         */
        final List<Content<?>> normalContents = message.contents()
                .stream()
                .filter(content -> !isContentSplitNeeded(content))
                .collect(Collectors.toList());
        messages.add(Message.ofUser(normalContents));

        return messages;
    }

    /**
     * 是否需要拆分出来的内容
     *
     * @param content 内容
     * @return TRUE | FALSE
     */
    private boolean isContentSplitNeeded(Content<?> content) {
        if (!(content instanceof Content.MediaContent)) {
            return false;
        }
        final Content.MediaContent mediaContent = (Content.MediaContent) content;
        return content.type() == Content.Type.FILE
               && "fileid".equalsIgnoreCase(mediaContent.data().getScheme());
    }

}
