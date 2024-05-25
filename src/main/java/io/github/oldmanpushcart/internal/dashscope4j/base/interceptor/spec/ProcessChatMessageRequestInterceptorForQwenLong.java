package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProcessChatMessageRequestInterceptorForQwenLong implements RequestInterceptor {

    @Override
    public CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {
        if ((request instanceof ChatRequest chatRequest)
            && Objects.equals(ChatModel.QWEN_LONG.name(), chatRequest.model().name())) {
            return processChatRequest(context, chatRequest);
        }
        return CompletableFuture.completedFuture(request);
    }

    private CompletableFuture<ApiRequest<?>> processChatRequest(InvocationContext context, ChatRequest request) {
        final var messages = request.messages();
        final var fsMessage = removeFileSystemMessage(messages);
        final var fContents = removeFileContent(messages);
        return CompletableFutureUtils.thenForEachCompose(fContents, content -> processFileContent(context, content))
                .thenApply(contents -> {

                    final var segments = contents.stream()
                            .map(Content::data)
                            .map(URI::toString)
                            .toList();

                    final var merged = new ArrayList<>(segments);
                    if (null != fsMessage) {
                        merged.addAll(List.of(fsMessage.text().split(",")));
                    }

                    messages.add(0, Message.ofSystem(String.join(",", merged)));
                    return ChatRequest.newBuilder(request)
                            .messages(false, messages)
                            .build();
                });
    }

    private Message removeFileSystemMessage(List<Message> messages) {
        final var messageIt = messages.iterator();
        while (messageIt.hasNext()) {
            final var message = messageIt.next();
            if (message.role() == Message.Role.SYSTEM
                && message.text().matches("(fileid://file-fe-\\\\w+)(,fileid://file-fe-\\\\w+)*")) {
                messageIt.remove();
                return message;
            }
        }
        return null;
    }

    private List<Content<URI>> removeFileContent(List<Message> messages) {
        final var contents = new ArrayList<Content<URI>>();
        final var messageIt = messages.iterator();
        while (messageIt.hasNext()) {
            final var message = messageIt.next();

            final var contentIt = message.contents().iterator();
            while (contentIt.hasNext()) {
                final var content = contentIt.next();

                // 将所有的FILE:URI类型的消息内容提取出来
                if ((content.type() == Content.Type.FILE)
                    && (content.data() instanceof URI)) {
                    //noinspection unchecked
                    contents.add((Content<URI>) content);
                    contentIt.remove();
                }

            }

            // 如果提取后消息内容为空，则移除该消息
            if (message.contents().isEmpty()) {
                messageIt.remove();
            }

        }
        return contents;
    }

    private CompletableFuture<Content<URI>> processFileContent(InvocationContext context, Content<URI> content) {
        final var resource = content.data();
        final var filename = content.data().getPath();
        return context.client().base().resource()
                .upload(resource, filename)
                .thenApply(meta -> Content.of(Content.Type.FILE, URI.create("fileid://%s".formatted(meta.id()))));
    }

}
