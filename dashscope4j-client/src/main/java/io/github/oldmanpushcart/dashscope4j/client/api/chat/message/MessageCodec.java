package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;

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
        if (messageNode instanceof ObjectNode node) {
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
        return JacksonJsonUtils.toObject(messageNode, Message.class);
    }

    /**
     * 解码消息
     *
     * @param messageJson 消息JSON
     * @return 消息
     */
    public static Message decode(String messageJson) {
        return JacksonJsonUtils.toObject(messageJson, Message.class);
    }

}
