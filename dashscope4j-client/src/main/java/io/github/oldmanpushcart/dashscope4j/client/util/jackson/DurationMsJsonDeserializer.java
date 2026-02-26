package io.github.oldmanpushcart.dashscope4j.client.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Duration;

public class DurationMsJsonDeserializer extends JsonDeserializer<Duration> {

    @Override
    public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return Duration.ofMillis(parser.getValueAsLong());
    }

}
