package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

        final var fmMessage = new FileMetaSystemMessage(new ArrayList<>());
        final var waitingProcessUris = new ArrayList<URI>();

        // 遍历处理消息
        final var messageIt = request.messages().iterator();
        while (messageIt.hasNext()) {
            final var message = messageIt.next();

            // 移除文件系统消息
            if (message instanceof FileMetaSystemMessage existed) {
                fmMessage.contents().addAll(existed.contents());
                messageIt.remove();
            }

            // 遍历处理消息内容：只需要处理FILE:URI类型的内容
            final var contentIt = message.contents().iterator();
            while (contentIt.hasNext()) {
                final var content = contentIt.next();
                if (content.type() == Content.Type.FILE && content.data() instanceof URI uri) {
                    waitingProcessUris.add(uri);
                    contentIt.remove();
                }
            }

        }

        // 处理文件内容
        return CompletableFutureUtils.thenForEachCompose(waitingProcessUris, uri -> processUri(context, uri))
                .thenApply(uris -> uris.stream().map(Content::ofFile).toList())
                .thenApply(contents -> {
                    fmMessage.contents().addAll(contents);
                    request.messages().add(0, fmMessage);
                    return request;
                });
    }

    private CompletableFuture<URI> processUri(InvocationContext context, URI uri) {
        return context.client().base().resource().upload(uri, uri.getPath())
                .thenApply(meta -> "fileid://%s".formatted(meta.id()))
                .thenApply(URI::create);
    }

    /**
     * 文件系统消息
     */
    public static class FileMetaSystemMessage extends MessageImpl implements Message {

        public FileMetaSystemMessage(List<Content<?>> contents) {
            super(Role.SYSTEM, contents);
        }

        @Override
        public String text() {
            return contents().stream()
                    .map(Content::data)
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }

    }

}
