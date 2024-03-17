package io.github.oldmanpushcart.internal.dashscope4j.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.TimeZone;

public class JacksonUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy())
            .setTimeZone(TimeZone.getTimeZone("GMT+8"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * {@code json -> T}
     *
     * @param json   json
     * @param type   对象类型
     * @param <T>    对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    /**
     * {@code object -> json}
     * @param object 目标对象
     * @return json
     */
    public static String toJson(Object object) {
        try {
            return mapper.writer().writeValueAsString(object);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse object to json failed!", cause);
        }
    }

}
