package io.github.oldmanpushcart.dashscope4j.client.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;

/**
 * Json 工具类
 */
public class JsonUtils {

    /**
     * {@code object -> json}
     *
     * @param object 目标对象
     * @return json
     */
    public static String toJson(Object object) {
        return JacksonJsonUtils.toJson(object);
    }

    /**
     * {@code object -> node}
     *
     * @param object object
     * @return node
     */
    public static JsonNode toNode(Object object) {
        return JacksonJsonUtils.toNode(object);
    }

    /**
     * {@code json -> T}
     *
     * @param json json
     * @param type 对象类型
     * @param <T>  对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String json, Class<T> type) {
        return JacksonJsonUtils.toObject(json, type);
    }

    /**
     * {@code json -> TypeRef}
     *
     * @param json    json
     * @param typeRef 对象类型
     * @param <T>     对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String json, TypeReference<T> typeRef) {
        return JacksonJsonUtils.toObject(json, typeRef);
    }

    /**
     * 压缩Json字符串
     *
     * @param json json
     * @return json
     */
    public static String compact(String json) {
        return JacksonJsonUtils.compact(json);
    }

}
