package io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class JacksonXmlUtils {

    private static final ObjectMapper mapper = new XmlMapper()
            .setTimeZone(TimeZone.getTimeZone("GMT+8"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * {@code xml -> T}
     *
     * @param xml  xml
     * @param type 对象类型
     * @param <T>  对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String xml, Class<T> type) {
        try {
            return mapper.readValue(xml, type);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse xml to object failed!", cause);
        }
    }

    /**
     * {@code xml -> T}
     *
     * @param xml          xml
     * @param type         对象类型
     * @param request      请求
     * @param httpResponse HTTP响应
     * @param <T>          对象类型
     * @return 目标对象
     */
    public static <T> T toApiResponse(String xml, Class<T> type, ApiRequest<?> request, HttpResponse<?> httpResponse) {
        final Map<String, Object> variableMap = new HashMap<>();
        httpResponse.headers().map()
                .forEach((name, values) -> variableMap.put("http/header/%s".formatted(name), values));
        variableMap.put("dashscope/request", request);
        try {
            return mapper.reader(new NullableInjectableValues(variableMap)).forType(type).readValue(xml);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse xml to object failed!", cause);
        }
    }

}
