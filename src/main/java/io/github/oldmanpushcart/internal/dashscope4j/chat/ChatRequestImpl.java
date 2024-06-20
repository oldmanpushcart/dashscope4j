package io.github.oldmanpushcart.internal.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

import static java.util.stream.Collectors.toMap;

final class ChatRequestImpl extends SpecifyModelAlgoRequestImpl<ChatModel, ChatResponse> implements ChatRequest {

    private final List<Message> messages;
    private final List<Plugin> plugins;
    private final List<Tool> tools;

    ChatRequestImpl(ChatModel model, Option option, Duration timeout, List<Message> messages, List<Plugin> plugins, List<Tool> tools) {
        super(model, option, timeout, ChatResponseImpl.class);
        this.messages = messages;
        this.plugins = plugins;
        this.tools = tools;
    }

    @Override
    public String suite() {
        return "/dashscope";
    }

    @Override
    public String type() {
        return "chat";
    }

    private record Input(

            @JsonProperty("messages")
            List<Message> messages

    ) {

    }

    @Override
    public List<Message> messages() {
        return messages;
    }

    @Override
    public List<Plugin> plugins() {
        return plugins;
    }

    @Override
    public List<Tool> tools() {
        return tools;
    }

    @Override
    public Object input() {
        return new Input(messages);
    }

    @Override
    public Option option() {
        final var clone = super.option().clone();

        // 插件必选参数
        if (!plugins.isEmpty()) {
            clone.option("result_format", "message");
        }

        // 工具必选参数
        if (!tools.isEmpty()) {
            clone.option("result_format", "message");
            clone.option("tools", tools);
        }

        return clone;
    }

    @Override
    public HttpRequest newHttpRequest() {

        // 为消息设置模型
        messages.stream()
                .filter(message -> message instanceof MessageImpl)
                .map(message -> (MessageImpl) message)
                .forEach(message -> {

                    // 默认是文本消息格式
                    var format = MessageImpl.Format.TEXT_MESSAGE;

                    // 如果是多模态模型，则需要设置多模态的消息格式
                    if (model().mode() == ChatModel.Mode.MULTIMODAL) {
                        format = MessageImpl.Format.MULTIMODAL_MESSAGE;
                    }

                    /*
                     * 如果是PDF提取插件，而且消息中包含了file内容
                     * 这种情况下需要转成多模态消息格式
                     */
                    {
                        final var hasPdfExtractPlugin = plugins.stream()
                                .anyMatch(plugin -> plugin.name().equals(ChatPlugin.PDF_EXTRACTER.name()));
                        final var hasFileContent = message.contents().stream()
                                .anyMatch(content -> content.type() == Content.Type.FILE);
                        if (hasPdfExtractPlugin && hasFileContent) {
                            format = MessageImpl.Format.MULTIMODAL_MESSAGE;
                        }
                    }

                    // 设置消息格式
                    message.format(format);

                });

        // 构造HTTP请求
        final var builder = HttpRequest.newBuilder(super.newHttpRequest(), (k, v) -> true);

        // 添加插件
        if (!plugins.isEmpty()) {
            final var pluginArgMap = plugins.stream()
                    .collect(toMap(
                            Plugin::name,
                            Plugin::arguments,
                            (a, b) -> b
                    ));
            final var pluginArgJson = JacksonUtils.toJson(pluginArgMap);
            builder.header(HttpHeader.HEADER_X_DASHSCOPE_PLUGIN, pluginArgJson);
        }

        return builder.build();
    }

}
