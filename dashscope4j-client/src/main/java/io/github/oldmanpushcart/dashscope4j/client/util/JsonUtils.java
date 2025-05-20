package io.github.oldmanpushcart.dashscope4j.client.util;

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

}
