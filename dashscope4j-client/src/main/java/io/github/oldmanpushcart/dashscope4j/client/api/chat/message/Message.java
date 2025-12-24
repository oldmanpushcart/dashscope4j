package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatView;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonDeserialize(using = Message.MessageJsonDeserializer.class)
public sealed class Message implements Accumulator<Message> permits PluginMessage, PluginCallMessage, ToolMessage, ToolCallMessage {

    private final Role role;
    private final List<Content<?>> contents;
    private final String reasoningContent;

    public Message(Role role, List<Content<?>> contents, String reasoningContent) {
        this.role = role;
        this.contents = contents;
        this.reasoningContent = reasoningContent;
    }

    public Message(Role role, List<Content<?>> contents) {
        this(role, contents, "");
    }

    public Message(Role role, String text) {
        this(role, List.of(Content.ofText(text)));
    }

    @JsonProperty("role")
    public Role role() {
        return role;
    }

    @JsonProperty("content")
    @JsonSerialize(using = Message.ContentListJsonSerializer.class)
    public List<Content<?>> contents() {
        return contents;
    }

    @JsonProperty("reasoning_content")
    public String reasoningContent() {
        return reasoningContent;
    }

    /**
     * @return 获取纯文本内容
     */
    public String text() {
        return textContents().stream()
                .map(Content.Text::data)
                .collect(Collectors.joining());
    }

    /**
     * 获取文本内容集合
     *
     * @return 文本内容集合
     */
    public List<Content.Text> textContents() {
        return contents.stream()
                .filter(Content.Text.class::isInstance)
                .map(Content.Text.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * 获取多媒体内容集合
     *
     * @param types 多媒体类型
     * @return 多媒体内容集合
     */
    public List<Content.Media> mediaContents(Content.Type... types) {
        return contents.stream()

                // 先过滤掉非多媒体内容
                .filter(Content.Media.class::isInstance)
                .map(Content.Media.class::cast)

                // 再过滤掉不符合类型的多媒体内容
                .filter(content -> {

                    // 如果没有指定类型，则默认为查询全部
                    if (null == types || types.length == 0) {
                        return true;
                    }

                    // 匹配指定类型
                    for (Content.Type type : types) {
                        if (content.type() == type) {
                            return true;
                        }
                    }

                    // 匹配不到
                    return false;

                })
                .collect(Collectors.toList());
    }

    @Override
    public Message accumulate(Message next) {

        if (null == next) {
            return this;
        }

        // 只有角色相同的消息才能合并
        if (role != next.role) {
            throw new IllegalArgumentException("Role not match! expect: %s but was: %s".formatted(
                    role,
                    next.role
            ));
        }

        // 合并所有内容
        final List<Content<?>> newContents = Stream.of(contents, next.contents)
                .flatMap(Collection::stream)
                .toList();

        // 合并理论推理内容
        final String newReasoningContent = StringUtils.concat(reasoningContent, next.reasoningContent);

        // 返回新消息
        return new Message(role, newContents, newReasoningContent);

    }

    /**
     * 创建消息
     *
     * @param role     角色
     * @param contents 内容集合
     * @return 消息
     */
    public static Message of(Role role, List<Content<?>> contents) {
        return new Message(role, contents, "");
    }

    /**
     * 系统消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofSystem(String text) {
        return new Message(Role.SYSTEM, text);
    }

    /**
     * AI消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofAi(String text) {
        return new Message(Role.AI, text);
    }

    /**
     * 用户消息(文本)
     *
     * @param text 文本
     * @return 消息
     */
    public static Message ofUser(String text) {
        return new Message(Role.USER, text);
    }

    /**
     * 用户消息
     *
     * @param contents 内容集合
     * @return 消息
     */
    public static Message ofUser(List<Content<?>> contents) {
        return new Message(Role.USER, contents);
    }

    /**
     * 用户消息
     *
     * @param text          文本
     * @param mediaContents 媒体内容集合
     * @return 消息
     */
    public static Message ofUser(String text, List<Content.Media> mediaContents) {
        final List<Content<?>> contents = new ArrayList<>();
        contents.add(Content.ofText(text));
        contents.addAll(mediaContents);
        return new Message(Role.USER, contents);
    }

    /**
     * 角色
     */
    public enum Role {

        /**
         * 系统
         */
        @JsonProperty("system")
        SYSTEM,

        /**
         * AI
         */
        @JsonProperty("assistant")
        AI,

        /**
         * 用户
         */
        @JsonProperty("user")
        USER,

        /**
         * 插件
         */
        @JsonProperty("plugin")
        PLUGIN,

        /**
         * 工具
         */
        @JsonProperty("tool")
        TOOL

    }

    static class ContentListJsonSerializer extends JsonSerializer<List<Content<?>>> {

        @Override
        public void serialize(List<Content<?>> contents, JsonGenerator gen, SerializerProvider provider) throws IOException {
            final var mapper = (ObjectMapper)gen.getCodec();
            final var view = provider.getActiveView();
            if(ChatView.Text.class.isAssignableFrom(view)) {
                final StringBuilder stringBuf = new StringBuilder();
                for (final Content<?> content : contents) {
                    if (content instanceof Content.Text text) {
                        stringBuf.append(text.data());
                    }
                }
                mapper.writerWithView(view).writeValue(gen, stringBuf.toString());
            } else if(ChatView.Multimodal.class.isAssignableFrom(view)) {
                mapper.writerWithView(view).writeValue(gen, contents);
            } else {
                throw new IllegalArgumentException("Unsupported view: " + view);
            }
        }

    }

    static class MessageJsonDeserializer extends JsonDeserializer<Message> {

        @Override
        public Message deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {

            // -- 读取相关JSON节点
            final JsonNode rootNode = ctx.readTree(parser);
            final JsonNode roleNode = rootNode.required("role");

            // -- 解析相关节点为对应类型对象
            final Message.Role role = ctx.readTreeAsValue(roleNode, Message.Role.class);

            // 处理插件应答消息
            if (role == Message.Role.PLUGIN) {
                return ctx.readTreeAsValue(rootNode, PluginMessage.class);
            }

            // 处理插件请求消息
            else if (role == Message.Role.AI && rootNode.hasNonNull("plugin_call")) {
                return ctx.readTreeAsValue(rootNode, PluginCallMessage.class);
            }

            // 处理工具应答消息
            else if (role == Message.Role.TOOL) {
                return ctx.readTreeAsValue(rootNode, ToolMessage.class);
            }

            // 处理工具请求消息
            else if (role == Message.Role.AI && rootNode.hasNonNull("tool_calls")) {
                return ctx.readTreeAsValue(rootNode, ToolCallMessage.class);
            }

            // 处理普通消息
            else {
                return deserializeMessage(ctx, role, rootNode);
            }

        }

        private Message deserializeMessage(DeserializationContext ctx, Message.Role role, JsonNode rootNode) throws IOException {

            // -- 读取相关JSON节点
            final JsonNode contentNode = rootNode.required("content");
            final JsonNode reasoningContentNode = rootNode.get("reasoning_content");

            // -- 解析相关节点为对应类型对象
            final String reasoningContent = Optional.ofNullable(reasoningContentNode)
                    .map(JsonNode::asText)
                    .orElse("");

            final List<Content<?>> contents = new ArrayList<>();

            /*
             * 处理多模态消息
             * [
             *     {"text": "图片中一共多少个男孩?"}
             *     {“image":"http://example.com/image.png"}
             *     {"video":"http://example.com/video.mp4"}
             * ]
             */
            if (contentNode.isArray()) {
                for (final JsonNode itemNode : contentNode) {
                    final Content<?> content = ctx.readTreeAsValue(itemNode, Content.class);
                    contents.add(content);
                }
            }

            /*
             * 处理文本模态消息
             * {
             *     ...
             *     content: "图片中一共多少个男孩?",
             *     ...
             * }
             */
            else {
                final Content<?> content = Content.ofText(contentNode.asText());
                contents.add(content);
            }

            // 构建并返回消息
            return new Message(
                    role,
                    contents,
                    reasoningContent
            );
        }

    }

}
