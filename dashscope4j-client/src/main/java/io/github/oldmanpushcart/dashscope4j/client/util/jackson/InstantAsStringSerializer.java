package io.github.oldmanpushcart.dashscope4j.client.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Instant 序列化为可读字符串
 */
public class InstantAsStringSerializer extends JsonSerializer<Instant> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            // 优先使用 ObjectMapper 配置的时区，否则使用系统默认时区
            ZoneId zoneId = provider.getTimeZone() != null 
                    ? provider.getTimeZone().toZoneId() 
                    : ZoneId.systemDefault();
            String formatted = FORMATTER.withZone(zoneId).format(value);
            gen.writeString(formatted);
        }
    }
}
