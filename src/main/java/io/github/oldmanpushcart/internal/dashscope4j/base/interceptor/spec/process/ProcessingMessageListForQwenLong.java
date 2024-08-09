package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessMessageListInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.internal.dashscope4j.util.StringUtils.matches;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * QwenLong 模型的对话请求消息列表进行处理
 * <p>
 * QwenLong 模型对话时会将提取用户对话中的文件信息，上传到模型所认的文件空间中，并将上传后的URI存入到一个特殊的SYSTEM消息。
 * 所以这里必须实现这个隐藏的逻辑，以便用户无差别的使用 QwenLong 模型。
 * </p>
 */
public class ProcessingMessageListForQwenLong implements ProcessMessageListInterceptor.Processor {

    // QwenLong 模型的对话请求消息文本内容正则表达式
    private static final String qwenLongMessageTextContentRegex = Pattern
            .compile("(fileid://file-fe-\\w+)(,fileid://file-fe-\\w+)*", CASE_INSENSITIVE)
            .pattern();

    @Override
    public CompletableFuture<? extends List<Message>> process(InvocationContext context, ApiRequest request, List<Message> messages) {

        // 仅处理 QwenLong 模型的对话请求
        if (!(request instanceof ChatRequest chatRequest)
            || !Objects.equals(chatRequest.model().name(), ChatModel.QWEN_LONG.name())) {
            return CompletableFuture.completedFuture(messages);
        }

        // 新的 QwenLong 系统消息
        final var newQwenLongSystemMessage = new QwenLongSystemMessageImpl(new ArrayList<>());

        // 新的对话消息列表
        final var newMessages = new ArrayList<Message>();

        // 处理现有的消息列表到新的对话消息列表中
        messages.forEach(message -> {

            /*
             * 如果消息列表中已经有了 QwenLong 系统消息，则合并到新的系统消息中。
             * 本条消息将不会加入到新的对话消息列表中，后边会再重建加回
             */
            if (message instanceof QwenLongSystemMessageImpl original) {
                newQwenLongSystemMessage.contents().addAll(original.contents());
                return;
            }

            /*
             * 如果消息列表中已经有了符合 QwenLong 模型的系统消息格式，则合并到新的系统消息中。
             * 本条消息将不会加入到新的对话消息列表中，后边会再重建加回
             */
            if (isQwenLongSystemMessageFormat(message)) {
                Arrays.stream(message.text().split(","))
                        .map(URI::create)
                        .map(Content::ofFile)
                        .forEach(newQwenLongSystemMessage.contents()::add);
                return;
            }

            /*
             * 处理普通消息
             * 找出消息中所有可以处理的内容，并添加到待处理内容列表中。
             */
            message.contents().stream()
                    .filter(ProcessingMessageListForQwenLong::isQwenLongSupportContent)
                    .forEach(newQwenLongSystemMessage.contents()::add);

            // 其他消息直接添加到新的对话消息列表
            newMessages.add(message);

        });

        // 重建并返回消息列表
        return CompletableFuture.completedFuture(rebuildMessageList(newMessages, newQwenLongSystemMessage));
    }


    /**
     * 验证消息是否符合 QwenLong 模型的系统消息格式
     * <p>特殊系统消息的内容为(fileid://或者fileid://,fileid://,...)，且符合URI的格式规范</p>
     *
     * @param message 要验证的消息
     * @return TRUE | FALSE
     */
    private static boolean isQwenLongSystemMessageFormat(Message message) {
        return message.role() == Message.Role.SYSTEM
               && matches(message.text(), qwenLongMessageTextContentRegex);
    }

    /**
     * 验证内容是否为 QwenLong 支持的内容
     *
     * @param content 内容
     * @return TRUE | FALSE
     */
    private static boolean isQwenLongSupportContent(Content<?> content) {

        // 只支持声明为<File,URI>类型的内容
        if (content.type() != Content.Type.FILE || !(content.data() instanceof URI resource)) {
            return false;
        }

        final var schema = resource.getScheme();

        /*
         * QwenLong 是一个文件处理模型，所以只支持以下协议
         *
         * 1. fileid:// 协议是 dashscope 的特殊文件协议，被 QwenLong 所支持。
         * 2. file:// 协议是 java 提供的文件协议，需要后续被转换为 fileid:// 后才能被 QwenLong使用
         */
        return CommonUtils.isIn(schema, Set.of("fileid", "file"));
    }

    /**
     * 重建消息列表
     *
     * @param messages              消息列表
     * @param qwenLongSystemMessage QwenLong 模型的系统消息
     * @return 重建后的消息列表
     */
    private static List<Message> rebuildMessageList(List<Message> messages, Message qwenLongSystemMessage) {

        // 将 QwenLong 系统消息作为最后一个SystemMessage注入到聊天列表中
        if (!qwenLongSystemMessage.contents().isEmpty()) {

            // 找到最后一个系统消息
            int found = -1;
            for (int index = 0; index < messages.size(); index++) {
                final var message = messages.get(index);
                if (message.role() == Message.Role.SYSTEM) {
                    found = index;
                }
            }

            // 插入到最后一个系统消息之后
            messages.add(found + 1, qwenLongSystemMessage);

        }

        // 返回重建后的消息列表
        return messages;
    }

    /**
     * 特殊系统消息
     */
    private static class QwenLongSystemMessageImpl extends MessageImpl {

        public QwenLongSystemMessageImpl(List<Content<?>> contents) {
            super(Role.SYSTEM, contents);
        }

        @Override
        public String text() {
            return contents().stream()
                    .map(Content::data)
                    .map(Object::toString)
                    .distinct()
                    .collect(Collectors.joining(","));
        }

    }

}
