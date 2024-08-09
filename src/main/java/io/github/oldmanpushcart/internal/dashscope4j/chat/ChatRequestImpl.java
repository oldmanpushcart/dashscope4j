package io.github.oldmanpushcart.internal.dashscope4j.chat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.HttpAlgoRequestImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.Constants.ENABLE;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_X_DASHSCOPE_PLUGIN;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.isNotEmptyCollection;
import static java.util.stream.Collectors.toMap;

class ChatRequestImpl extends HttpAlgoRequestImpl<ChatModel, ChatResponse> implements ChatRequest {

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
        return "dashscope://chat";
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
    protected Object input() {

        // 默认格式为对话模型所指定的格式
        final var modeRef = new AtomicReference<>(model().mode());

        // 是否有PDFExtract插件
        final var hasPdfExtractPlugin = plugins.stream()
                .anyMatch(plugin -> plugin.name().equals(ChatPlugin.PDF_EXTRACTER.name()));

        // 聊天消息列表中是否包含File类型的内容
        final var hasFileContent = messages.stream()
                .flatMap(message -> message.contents().stream())
                .anyMatch(content -> content.type() == Content.Type.FILE);

        /*
         * PDFExtract插件比较特殊，
         * 他在有File类型的内容时，消息列表格式为为多模态格式，否则则为文本格式
         */
        if (hasPdfExtractPlugin && hasFileContent) {
            modeRef.set(ChatModel.Mode.MULTIMODAL);
        }

        /*
         * 构造消息列表，根据模式的不同构造不同的消息列表格式
         */
        return new HashMap<>() {{
            put("messages", new ArrayList<>() {{
                for (final var message : messages) {

                    /*
                     * 根据模式的不同，构造不同的消息列表格式
                     * 之所以要如此繁琐的原因，是要保持Message无状态实现Json时根据ChatModel的不同而做出不同的序列化结果
                     * 所以在此对message的序列化做精细化控制
                     *
                     * FIX BUG: 2.0.0 版本中此处有严重问题，会丢失PluginCall、Plugin、ToolCall、Tool等信息
                     */
                    final var messageNode = JacksonUtils.toNode(message);
                    if (messageNode instanceof ObjectNode node) {
                        if (modeRef.get() == ChatModel.Mode.TEXT) {
                            node.put("content", message.text());
                        } else {
                            node.putPOJO("content", message.contents());
                        }
                    }

                    /*
                     * 将JsonNode代替原来的Message参与接下来ChatRequest的序列化操作
                     */
                    add(messageNode);

                }
            }});
        }};

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

        // 构造HTTP请求
        final var builder = HttpRequest.newBuilder(super.newHttpRequest(), (k, v) -> true)
                .header(HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE);

        // 添加插件
        if (isNotEmptyCollection(plugins)) {
            final var pluginArgMap = plugins.stream()
                    .collect(toMap(
                            Plugin::name,
                            Plugin::arguments,
                            (a, b) -> b
                    ));
            final var pluginArgJson = JacksonUtils.toJson(pluginArgMap);
            builder.header(HEADER_X_DASHSCOPE_PLUGIN, pluginArgJson);
        }

        return builder.build();
    }

}
