package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.openai;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;

import java.net.URI;
import java.util.ArrayList;

public class Helper {

    /**
     * 通义千问问答请求转换为{@code OpenAi}兼容模式问答请求
     *
     * @param request 通义千问问答请求
     * @return {@code OpenAi}兼容模式问答请求
     */
    public static OpenAiChatRequest toOpenAiChatRequest(ChatRequest request) {
        return OpenAiChatRequest.newBuilder()
                .ref(request)
                .model(request.model())
                .parameters(request.parameters())
                .messages(request.messages())
                .tools(request.tools())
                .build();
    }

    /**
     * {@code OpenAi}兼容模式问答应答转换为通义千问应答
     *
     * @param response {@code OpenAi}兼容模式问答应答
     * @return 通义千问问答应答
     */
    public static ChatResponse toChatResponse(OpenAiChatResponse response) {
        final var request = toChatRequest(response.request());
        final var choices = response.choices()
                .stream()
                .map(Helper::toChoice)
                .toList();
        final var output = new ChatResponse.Output(
                null,
                choices
        );
        return new ChatResponse(
                request,
                response.uuid(),
                response.code(),
                response.desc(),
                response.usage(),
                output
        );
    }

    private static ChatRequest toChatRequest(OpenAiChatRequest request) {
        return request.ref();
    }

    private static ChatResponse.Choice toChoice(OpenAiChatResponse.Choice choice) {
        final var message = toMessage(choice.message());
        return new ChatResponse.Choice(choice.finish(), message);
    }

    private static AssistantMessage toMessage(OpenAiChatResponse.Message message) {
        final var contents = new ArrayList<Content>();

        // 文本内容
        if (CommonUtils.isNotBlankString(message.content())) {
            contents.add(Content.text(message.content()));
        }

        // 音频内容（Base64 inline）
        if (null != message.audio()
                && CommonUtils.isNotBlankString(message.audio().data())) {
            final var audioURI = URI.create("data:;base64," + message.audio().data());
            contents.add(Content.audio(audioURI));
        }

        return AssistantMessage.newBuilder()
                .contents(contents)
                .build();
    }

}
