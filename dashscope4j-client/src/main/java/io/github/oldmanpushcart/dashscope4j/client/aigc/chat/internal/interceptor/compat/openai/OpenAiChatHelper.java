package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.openai;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

class OpenAiChatHelper {

    public static OpenAiChatRequest toOpenAiChatRequest(AigcRequest<Input, ?> request) {
        return new OpenAiChatRequest(request);
    }

    /**
     * {@code OpenAi}兼容模式问答应答转换为通义千问应答
     *
     * @param response {@code OpenAi}兼容模式问答应答
     * @return 通义千问问答应答
     */
    public static AigcResponse<Output> toAigcResponse(OpenAiChatResponse response) {
        final var choices = response.choices()
                .stream()
                .map(OpenAiChatHelper::toChoice)
                .toList();
        final var output = new Output(
                null,
                choices
        );
        return new AigcResponse<>(
                response.request().ref(),
                response.uuid(),
                response.code(),
                response.desc(),
                response.usage(),
                output
        );
    }

    private static Output.Choice toChoice(OpenAiChatResponse.Choice choice) {
        final var message = toMessage(choice.message());
        return new Output.Choice(choice.finish(), message);
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

        // Tool Call
        final var calls = null == message.calls()
                ? Collections.<Tool.Call>emptyList()
                : message.calls();

        return AssistantMessage.newBuilder()
                .contents(contents)
                .calls(calls)
                .build();
    }

}
