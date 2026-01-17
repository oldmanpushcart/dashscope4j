package io.github.oldmanpushcart.dashscope4j.client.internal.api.image.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.image.compat.chat.Text2ImageChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.image.compat.chat.Text2ImageChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class CompatChatInterceptor implements TaskInterceptor {

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof Text2ImageRequest request)) {
            return chain.proceed();
        }

        if (!request.model().tags().contains(Text2ImageModelTags.COMPAT_TASK_CHAT)) {
            return chain.proceed();
        }

        return chain.client().base().api().task(Text2ImageChatRequest.of(request))
                .thenApply(half ->
                        (Task.Half<Text2ImageResponse>) strategy ->
                                half.waitingFor(strategy)
                                        .thenApply(chatResponse -> {
                                            final var items = toItem(chatResponse.code(), chatResponse.desc(), chatResponse);
                                            return new Text2ImageResponse(
                                                    request,
                                                    chatResponse.uuid(),
                                                    chatResponse.code(),
                                                    chatResponse.desc(),
                                                    chatResponse.usage(),
                                                    new Text2ImageResponse.Output(items)
                                            );
                                        }));
    }

    private static List<Text2ImageResponse.Item> toItem(String code, String desc, Text2ImageChatResponse chatResponse) {
        return chatResponse.output()
                .choices()
                .stream()
                .flatMap(choice -> choice.message().contents().stream())
                .filter(ImageContent.class::isInstance)
                .map(ImageContent.class::cast)
                .map(content -> new Text2ImageResponse.Item(
                        code,
                        desc,
                        content.image()
                ))
                .toList();
    }

}
