package io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class JacksonJsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy())
            .setTimeZone(TimeZone.getTimeZone("GMT+8"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ObjectMapper newMapper() {
        return mapper.copy();
    }

    public static String toJson(Class<?> view, Object object) {
        try {
            return mapper.writerWithView(view).writeValueAsString(object);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse object to json failed!", cause);
        }
    }

    /**
     * {@code object -> json}
     *
     * @param object 目标对象
     * @return json
     */
    public static String toJson(Object object) {
        return toJson(mapper, object);
    }

    public static String toJson(ObjectMapper mapper, Object object) {
        try {
            return mapper.writer().writeValueAsString(object);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse object to json failed!", cause);
        }
    }

    /**
     * {@code json -> node}
     *
     * @param json json
     * @return node
     */
    public static JsonNode toNode(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException cause) {
            throw new RuntimeException("parse json to node failed!", cause);
        }
    }

    /**
     * {@code json -> T}
     *
     * @param json json
     * @param type 对象类型
     * @param <T>  对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String json, Type type) {
        try {
            final JavaType jType = mapper.constructType(type);
            return mapper.readValue(json, jType);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    public static <T> T toObject(String json, Class<T> type) {
        return toObject(mapper, json, type);
    }

    public static <T> T toObject(ObjectMapper mapper, String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    public static <T extends ApiResponse> T toApiResponse(String json, Class<T> type, ApiRequest<?> request, HttpResponse<?> httpResponse) {
        final Map<String, Object> variableMap = new HashMap<>();
        httpResponse.headers().map()
                .forEach((name, values) -> variableMap.put("http/header/%s".formatted(name), String.join("", values)));
        variableMap.put("dashscope/request", request);
        try {
            return mapper.reader(new NullableInjectableValues(variableMap)).forType(type).readValue(json);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }
}
