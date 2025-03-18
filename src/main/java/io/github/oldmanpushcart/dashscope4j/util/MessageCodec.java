package io.github.oldmanpushcart.dashscope4j.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.*;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;

import java.util.Arrays;
import java.util.function.Function;

/**
 * 消息编解码器
 *
 * @since 3.1.0
 */
public class MessageCodec {

    /**
     * 编码消息
     *
     * @param mode    对话模式
     * @param message 消息
     * @param mapper  编码映射器
     * @param <T>     映射结果类型
     * @return 映射结果
     */
    public static <T> T encode(ChatModel.Mode mode, Message message, Function<JsonNode, T> mapper) {
        final JsonNode messageNode = JacksonJsonUtils.toNode(message);
        if (messageNode instanceof ObjectNode) {
            final ObjectNode node = (ObjectNode) messageNode;
            switch (mode) {
                case TEXT:
                    node.put("content", message.text());
                    break;
                case MULTIMODAL:
                    node.putPOJO("content", message.contents());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported mode: " + mode);
            }
        }
        return mapper.apply(messageNode);
    }

    /**
     * 编码消息
     *
     * @param mode    对话模式
     * @param message 消息
     * @return JSON-NODE
     */
    public static JsonNode encodeToJsonNode(ChatModel.Mode mode, Message message) {
        return encode(mode, message, Function.identity());
    }

    /**
     * 编码消息
     *
     * @param mode    对话模式
     * @param message 消息
     * @return 消息JSON
     */
    public static String encodeToJson(ChatModel.Mode mode, Message message) {
        return encode(mode, message, JacksonJsonUtils::toJson);
    }

    /**
     * 解码消息
     *
     * @param messageNode 消息节点
     * @return 消息
     */
    public static Message decode(JsonNode messageNode) {

        final JsonNode roleNode = messageNode.required("role");
        final JsonNode contentNode = messageNode.required("content");
        final Message.Role role = JacksonJsonUtils.toObject(roleNode, Message.Role.class);

        // 处理多模态内容
        if (contentNode.isArray()) {
            final Content<?>[] contents = JacksonJsonUtils.toObject(contentNode, Content[].class);
            return new Message(role, Arrays.asList(contents));
        }

        // 处理文本内容
        else {

            // 处理插件应答消息
            if (role == Message.Role.PLUGIN) {
                return JacksonJsonUtils.toObject(messageNode, PluginMessage.class);
            }

            // 处理插件请求消息
            else if (role == Message.Role.AI && messageNode.hasNonNull("plugin_call")) {
                return JacksonJsonUtils.toObject(messageNode, PluginCallMessage.class);
            }

            // 处理工具应答消息
            else if (role == Message.Role.TOOL) {
                return JacksonJsonUtils.toObject(messageNode, ToolMessage.class);
            }

            // 处理工具请求消息
            else if (role == Message.Role.AI && messageNode.hasNonNull("tool_calls")) {
                return JacksonJsonUtils.toObject(messageNode, ToolCallMessage.class);
            }

            // 处理普通消息
            else {
                return JacksonJsonUtils.toObject(messageNode, Message.class);
            }

        }

    }

    /**
     * 解码消息
     *
     * @param messageJson 消息JSON
     * @return 消息
     */
    public static Message decode(String messageJson) {
        return decode(JacksonJsonUtils.toNode(messageJson));
    }

}
