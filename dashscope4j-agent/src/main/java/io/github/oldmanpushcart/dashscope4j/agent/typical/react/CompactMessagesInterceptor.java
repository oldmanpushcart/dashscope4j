package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 消息压实拦截器
 * <p>
 * 在发送请求前对消息列表进行压实处理，减少消息数量：
 * <ul>
 *     <li>System 消息保持独立，按原顺序放在最前面</li>
 *     <li>历史对话（除最后一条用户消息外）合并为一个 Assistant 消息</li>
 *     <li>最后一条用户消息保持不变</li>
 * </ul>
 * </p>
 *
 * @since 4.0.0
 */
public class CompactMessagesInterceptor implements ChatInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var originalMessages = request.input().messages();

        // 如果消息数较少，无需压缩
        if (originalMessages.size() <= 2) {
            return chain.proceed();
        }

        // 构建新的请求并继续执行
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> {

                    // 压实消息列表
                    final var compactedMessages = compact(originalMessages);

                    // 重构 Input
                    return Input.newBuilder(input)
                            .messages(compactedMessages)
                            .build();
                })
                .build();

        return chain.proceed(newRequest);
    }

    /**
     * 将消息列表压实
     * <p>
     * 压实逻辑：
     * <ul>
     *     <li>1. System 消息保持独立，按原顺序放在最前面</li>
     *     <li>2. 历史消息（除最后一条用户消息外的所有非 System 消息）合并为一个 AI 消息</li>
     *     <li>3. 最后一条用户消息不做压缩</li>
     * </ul>
     * </p>
     *
     * @param messages 原始消息列表
     * @return 压实后的消息列表
     */
    private List<Message> compact(List<Message> messages) {
        // 分离 System 消息和非 System 消息
        final var systemMessages = new ArrayList<Message>();
        final var nonSystemMessages = new ArrayList<Message>();

        for (final var message : messages) {
            if (message.role() == Message.Role.SYSTEM) {
                systemMessages.add(message);
            } else {
                nonSystemMessages.add(message);
            }
        }

        // 如果没有非 System 消息或只有一条，无需压缩
        if (nonSystemMessages.size() <= 1) {
            return messages;
        }

        // 找到最后一条用户消息
        int lastUserIndex = -1;
        for (int i = nonSystemMessages.size() - 1; i >= 0; i--) {
            if (nonSystemMessages.get(i).role() == Message.Role.USER) {
                lastUserIndex = i;
                break;
            }
        }

        // 如果没有找到用户消息，无需压缩
        if (lastUserIndex == -1) {
            return messages;
        }

        // 构建压实后的消息列表

        // 1. 添加所有 System 消息
        final var result = new ArrayList<>(systemMessages);

        // 2. 合并历史消息（最后一条用户消息之前的所有非 System 消息）
        if (lastUserIndex > 0) {
            final var historyMessages = nonSystemMessages.subList(0, lastUserIndex);
            final var mergedHistory = mergeToAssistantMessage(historyMessages);
            if (mergedHistory != null) {
                result.add(mergedHistory);
            }
        }

        // 3. 添加最后一条用户消息及其之后的消息（通常是 tool 消息）
        result.addAll(nonSystemMessages.subList(lastUserIndex, nonSystemMessages.size()));

        return result;
    }

    /**
     * 将消息列表合并为一个 Assistant 消息
     *
     * @param messages 待合并的消息列表
     * @return 合并后的 Assistant 消息，如果为空则返回 null
     */
    private AssistantMessage mergeToAssistantMessage(List<Message> messages) {
        if (messages.isEmpty()) {
            return null;
        }

        // 收集所有消息的文本内容，每条消息之间用双换行分隔
        final var textParts = messages.stream()
                .map(Message::text)
                .filter(text -> text != null && !text.isEmpty())
                .toList();

        if (textParts.isEmpty()) {
            return null;
        }

        // 使用双换行符分隔每条消息，保持清晰的边界
        final var mergedText = String.join("\n\n", textParts);

        // 创建带缓存控制的文本内容
        final var textContent = Content.text(mergedText).withCache();

        return AssistantMessage.newBuilder()
                .contents(contents -> {
                    contents.add(textContent);
                    return contents;
                })
                .build();
    }

}
