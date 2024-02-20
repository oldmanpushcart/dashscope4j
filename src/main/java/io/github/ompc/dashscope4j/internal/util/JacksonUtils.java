package io.github.ompc.dashscope4j.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class JacksonUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 获取一个新的mapper
     *
     * @return mapper
     */
    public static ObjectMapper mapper() {
        return mapper.copy();
    }

    /**
     * {@code json -> T}
     *
     * @param mapper mapper
     * @param json   json
     * @param type   对象类型
     * @param <T>    对象类型
     * @return 目标对象
     */
    public static <T> T toObject(ObjectMapper mapper, String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    /**
     * {@code object -> json}
     *
     * @param mapper mapper
     * @param object 目标对象
     * @return json
     */
    public static String toJson(ObjectMapper mapper, Object object) {
        try {
            return mapper.writer().writeValueAsString(object);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse object to json failed!", cause);
        }
    }

}
