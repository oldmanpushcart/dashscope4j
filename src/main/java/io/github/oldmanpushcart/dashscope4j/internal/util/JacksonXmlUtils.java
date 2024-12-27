package io.github.oldmanpushcart.dashscope4j.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

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

}
