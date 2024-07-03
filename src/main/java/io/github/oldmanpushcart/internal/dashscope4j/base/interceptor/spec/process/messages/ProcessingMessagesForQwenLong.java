package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.messages;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessMessagesRequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class ProcessingMessagesForQwenLong implements ProcessMessagesRequestInterceptor.Processor {

    private static final String specificContentRegex = Pattern
            .compile("(fileid://file-fe-\\w+)(,fileid://file-fe-\\w+)*", CASE_INSENSITIVE)
            .pattern();

    @Override
    public CompletableFuture<? extends List<Message>> process(InvocationContext context, ApiRequest<?> request, List<Message> messages) {

        /*
         * 仅处理 QwenLong 模型的对话请求
         */
        if (!(request instanceof AlgoRequest<?> algoRequest)
            || !Objects.equals(ChatModel.QWEN_LONG.name(), algoRequest.model().name())) {
            return CompletableFuture.completedFuture(messages);
        }

        // 等待处理内容
        final var waitingProcessContents = new ArrayList<Content<URI>>();

        // 遍历处理消息
        final var messageIt = messages.iterator();
        while (messageIt.hasNext()) {

            // 消息
            final var message = messageIt.next();

            /*
             * 处理特殊系统消息
             * 特殊系统消息的内容为(fileid://或者fileid://,fileid://,...)，且符合URI的格式规范，
             * 这里需要将 fileid:// 转换为 URI，并添加到待处理内容列表中并移除本条消息，后边会再次加回消息列表中
             */
            if (isSpecificSystemMessage(message)) {
                Stream.of(message.text().split(","))
                        .map(URI::create)
                        .map(Content::ofFile)
                        .forEach(waitingProcessContents::add);
                messageIt.remove();
                continue;
            }

            /*
             * 处理普通消息
             * 找出消息中所有可以处理的内容，并添加到待处理内容列表中。
             */
            message.contents().stream()
                    .filter(ProcessingMessagesForQwenLong::isSpecificContent)
                    .map(content -> Content.ofFile(URI.create(content.data().toString())))
                    .forEach(waitingProcessContents::add);

        }

        // 待处理内容不为空，则处理
        if (!waitingProcessContents.isEmpty()) {

            // 找到最后一个SystemMessage
            int found = -1;
            for (int index = 0; index < messages.size(); index++) {
                final var message = messages.get(index);
                if (message.role() == Message.Role.SYSTEM) {
                    found = index;
                }
            }

            // 构造特殊的SystemMessage
            final var fileIdsText = waitingProcessContents.stream()
                    .map(Content::data)
                    .map(Object::toString)
                    .distinct()
                    .collect(Collectors.joining(","));
            final var specificSystemMessage = Message.ofSystem(fileIdsText);

            // 插入到最后一个SystemMessage之后
            messages.add(found + 1, specificSystemMessage);

        }

        return CompletableFuture.completedFuture(messages);
    }

    /**
     * 是否为 QwenLong 特殊系统消息
     *
     * @param message 消息
     * @return TRUE | FALSE
     */
    private static boolean isSpecificSystemMessage(Message message) {
        return message.role() == Message.Role.SYSTEM
               && message.text().matches(specificContentRegex);
    }

    /**
     * 是否为 QwenLong 特殊内容
     *
     * @param content 内容
     * @return TRUE | FALSE
     */
    private static boolean isSpecificContent(Content<?> content) {
        return content.type() == Content.Type.FILE
               && Objects.nonNull(content.data())
               && content.data().toString().matches(specificContentRegex);
    }

}
