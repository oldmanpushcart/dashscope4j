package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 处理Chat请求中模型为QwenLong的上下文
 */
class ProcessChatMessageContentForQwenLongInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 如果不是 QwenLong 请求，则直接返回
        if (!isRequireProcess(chain.request())) {
            return chain.process(chain.request());
        }

        return processChain(chain);
    }

    /**
     * 是否需要处理
     *
     * @param request 请求
     * @return TRUE | FALSE
     */
    private boolean isRequireProcess(ApiRequest<?> request) {
        if (!(request instanceof ChatRequest)) {
            return false;
        }

        final ChatRequest chatRequest = (ChatRequest) request;
        if (!chatRequest.model().name().equals("qwen-long")) {
            return false;
        }

        return !chatRequest.messages().isEmpty();
    }

    private boolean isRequireProcessMessage(boolean isLast, Message message) {
        return isLast && message.role() == Message.Role.USER;
    }

    private boolean isRequireProcessContent(Content<?> content) {
        return content.type() == Content.Type.FILE
               && content.data() instanceof URI
               && "fileid".equalsIgnoreCase(((URI) content.data()).getScheme());
    }

    private CompletionStage<?> processChain(Chain chain) {
        final ChatRequest request = (ChatRequest) chain.request();
        final List<Message> newMessages = new ArrayList<>();
        final List<Message> messages = request.messages();
        final int size = messages.size();
        for (int index = 0; index < size; index++) {

            final boolean isLast = index == size - 1;
            final Message message = messages.get(index);

            /*
             * 只处理最后一个USER消息
             */
            if (!isRequireProcessMessage(isLast, message)) {
                newMessages.add(message);
                continue;
            }

            /*
             * 拆分消息，将文件类型的消息拆分为系统消息和用户消息
             * 将内容为FILE:fileid://...的内容拆分为System消息，
             * 原有文本消息保留为用户消息
             */
            final List<Message> splitMessages = new ArrayList<>();
            final List<Content<?>> newContents = new ArrayList<>();
            message.contents().forEach(content -> {
                if (!isRequireProcessContent(content)) {
                    newContents.add(content);
                    return;
                }
                splitMessages.add(new Message(Message.Role.SYSTEM, Content.ofText(content.data().toString())));
            });
            splitMessages.add(new Message(Message.Role.USER, newContents));
            newMessages.addAll(splitMessages);

        }

        final ChatRequest newChatRequest = ChatRequest.newBuilder(request)
                .messages(newMessages)
                .build();
        return chain.process(newChatRequest);

    }

}
