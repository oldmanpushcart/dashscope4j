package io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class ByteBufferBase64JsonDeserializer extends JsonDeserializer<ByteBuffer> {

    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    @Override
    public ByteBuffer deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        final var token = parser.currentToken();

        if (token == JsonToken.VALUE_NULL) {
            return EMPTY;
        }

        if (token != JsonToken.VALUE_STRING) {
            context.reportWrongTokenException(
                    ByteBuffer.class,
                    JsonToken.VALUE_STRING,
                    "Expected a Base64-encoded string for ByteBuffer"
            );
        }

        final var string = parser.getValueAsString();
        if (string.isBlank()) {
            return EMPTY;
        }

        final var decoded = Base64.getDecoder().decode(string);
        return ByteBuffer.wrap(decoded).asReadOnlyBuffer();
    }

}
