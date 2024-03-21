package io.github.oldmanpushcart.internal.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

import static java.util.stream.Collectors.toMap;

final class ChatRequestImpl extends AlgoRequestImpl<ChatResponse> implements ChatRequest {

    private final List<Message> messages;
    private final List<ChatPlugin> plugins;
    private final List<FunctionTool> functionTools;

    ChatRequestImpl(ChatModel model, Option option, Duration timeout, List<Message> messages, List<ChatPlugin> plugins, List<FunctionTool> functionTools) {
        super(model, new Input(messages), option, timeout, ChatResponseImpl.class);
        this.messages = messages;
        this.plugins = plugins;
        this.functionTools = functionTools;
    }


    private record Input(
            @JsonProperty("messages")
            List<Message> messages
    ) {

    }

    public List<Message> messages() {
        return messages;
    }

    public List<ChatPlugin> plugins() {
        return plugins;
    }

    public List<FunctionTool> functionTools() {
        return functionTools;
    }

    @Override
    public ChatModel model() {
        return (ChatModel) super.model();
    }

    @Override
    public Option option() {
        final var clone = new Option();
        super.option().export().forEach(clone::option);

        // 插件必选参数
        if (!plugins.isEmpty()) {
            clone.option("result_format", "message");
        }

        // 工具必选参数
        if (!functionTools.isEmpty()) {
            clone.option("result_format", "message");
            clone.option("tools", functionTools);
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
                            ChatPlugin::name,
                            ChatPlugin::arguments,
                            (a, b) -> b
                    ));
            final var pluginArgJson = JacksonUtils.toJson(pluginArgMap);
            builder.header(HttpHeader.HEADER_X_DASHSCOPE_PLUGIN, pluginArgJson);
        }

        return builder.build();
    }

    public static ChatRequestImpl of(ChatModel model, Option option, Duration timeout, List<Message> messages, List<ChatPlugin> plugins, List<ChatFunction<?, ?>> functions) {

        // 转换为函数工具
        final var functionTools = functions.stream()
                .map(FunctionTool::of)
                .toList();

        // 构造对话请求
        return new ChatRequestImpl(model, option, timeout, messages, plugins, functionTools);

    }

}
