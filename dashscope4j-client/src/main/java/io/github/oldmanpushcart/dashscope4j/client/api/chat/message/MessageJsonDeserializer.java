package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 消息JSON解码器
 * <p>
 * 消息格式1：
 * {@code
 * {
 * "role": "user",
 * "reasoning_content": "",
 * "content": "遵化未来5天天气情况?"
 * }
 * }
 * </p>
 * <p>
 * 消息格式2：
 * {@code
 * {
 * "role": "user",
 * "reasoning_content": "",
 * "content": [
 * {
 * "image": "oss://dashscope-instant/97d58d93fb32669908a4e42d8a2d8112/2025-05-06/3f988e77-fad3-9080-bb5a-dbd13401b729/a2973dd7-3009-4b9f-96ab-3af09eae2655.JPG"
 * },
 * {
 * "text": "图片中一共多少个男孩?"
 * }
 * ]
 * }
 * }
 * </p>
 */
class MessageJsonDeserializer extends JsonDeserializer<Message> {

    @Override
    public Message deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {

        // -- 读取相关JSON节点
        final JsonNode rootNode = context.readTree(parser);
        final JsonNode roleNode = rootNode.required("role");

        // -- 解析相关节点为对应类型对象
        final Message.Role role = context.readTreeAsValue(roleNode, Message.Role.class);



        // 处理插件应答消息
        if (role == Message.Role.PLUGIN) {
            return context.readTreeAsValue(rootNode, PluginMessage.class);
        }

        // 处理插件请求消息
        else if (role == Message.Role.AI && rootNode.hasNonNull("plugin_call")) {
            return context.readTreeAsValue(rootNode, PluginCallMessage.class);
        }

        // 处理工具应答消息
        else if (role == Message.Role.TOOL) {
            return context.readTreeAsValue(rootNode, ToolMessage.class);
        }

        // 处理工具请求消息
        else if (role == Message.Role.AI && rootNode.hasNonNull("tool_calls")) {
            return context.readTreeAsValue(rootNode, ToolCallMessage.class);
        }

        // 处理普通消息
        else {
            return deserializeMessage(context, role, rootNode);
        }

    }


    private Message deserializeMessage(DeserializationContext context, Message.Role role, JsonNode rootNode) throws IOException {

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
                final Content<?> content = context.readTreeAsValue(itemNode, Content.class);
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
