package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.isNotEmptyCollection;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.mapTo;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils.thenForEachCompose;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class ProcessChatMessageRequestInterceptorForQwenLong implements RequestInterceptor {

    private static final String FILEID_REGEX_PATTERN = Pattern
            .compile("(fileid://file-fe-\\w+)(,fileid://file-fe-\\w+)*", CASE_INSENSITIVE)
            .pattern();

    @Override
    public CompletableFuture<ApiRequest<?>> preHandle(InvocationContext context, ApiRequest<?> request) {
        if ((request instanceof ChatRequest chatRequest)
            && Objects.equals(ChatModel.QWEN_LONG.name(), chatRequest.model().name())) {
            return processChatRequest(context, chatRequest);
        }
        return CompletableFuture.completedFuture(request);
    }

    private CompletableFuture<ApiRequest<?>> processChatRequest(InvocationContext context, ChatRequest request) {

        final var waitingProcessContents = new LinkedHashSet<Content<?>>();
        final var waitingProcessUris = new LinkedHashSet<URI>();

        // 遍历处理消息
        final var messageIt = request.messages().iterator();
        while (messageIt.hasNext()) {
            final var message = messageIt.next();

            // 合并并移除已有的文件系统消息
            if (message instanceof FileMetaSystemMessage existed) {
                waitingProcessContents.addAll(existed.contents());
                messageIt.remove();
            }

            // 合并并移除所有符合FileMetaSystemMessage格式的系统消息
            else if (message.role() == Message.Role.SYSTEM
                     && message.text().matches(FILEID_REGEX_PATTERN)) {
                Stream.of(message.text().split(","))
                        .map(URI::create)
                        .map(Content::ofFile)
                        .forEach(waitingProcessContents::add);
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
        return thenForEachCompose(waitingProcessUris, uri -> processUri(context, uri))
                .thenApply(uris -> mapTo(uris, Content::ofFile))
                .thenApply(contents -> {
                    waitingProcessContents.addAll(contents);
                    if (isNotEmptyCollection(waitingProcessContents)) {

                        // 插入到最后一个system之后
                        int found = -1;
                        for (int index = 0; index < request.messages().size(); index++) {
                            final var message = request.messages().get(index);
                            if (message.role() == Message.Role.SYSTEM) {
                                found = index;
                            }
                        }

                        request.messages().add(found + 1, new FileMetaSystemMessage(waitingProcessContents));
                    }
                    return request;
                });
    }

    private CompletableFuture<URI> processUri(InvocationContext context, URI uri) {

        // fileid协议类型，不做处理
        if ("fileid".equalsIgnoreCase(uri.getScheme())) {
            return CompletableFuture.completedFuture(uri);
        }

        // 其他协议类型：上传文件
        return context.client().base().files().upload(uri, uri.getPath())
                .thenApply(FileMeta::toURI);
    }

    /**
     * 文件系统消息
     */
    private static class FileMetaSystemMessage extends MessageImpl implements Message {

        public FileMetaSystemMessage(Set<Content<?>> contents) {
            super(Role.SYSTEM, new ArrayList<>(contents));
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
