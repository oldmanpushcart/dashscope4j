package io.github.oldmanpushcart.dashscope4j.client.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 从可读字符串反序列化为 Instant
 */
public class InstantAsStringDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }
        // 优先使用 ObjectMapper 配置的时区，否则使用系统默认时区
        ZoneId zoneId = ctxt.getTimeZone() != null 
                ? ctxt.getTimeZone().toZoneId() 
                : ZoneId.systemDefault();
        return Instant.from(FORMATTER.withZone(zoneId).parse(text));
    }
}
