package io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Base64;

public class ByteArrayBase64JsonDeserializer extends JsonDeserializer<byte[]> {

    private static final byte[] EMPTY = new byte[0];

    @Override
    public byte[] deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        final var token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return EMPTY;
        }

        // 必须是字符串
        if (token != JsonToken.VALUE_STRING) {
            context.reportWrongTokenException(
                    byte[].class,
                    JsonToken.VALUE_STRING,
                    "Expected as Base64-encoded string!"
            );
        }

        final var string = parser.getValueAsString();
        if (string.isBlank()) {
            return EMPTY;
        }

        return Base64.getDecoder().decode(string);

    }

}
