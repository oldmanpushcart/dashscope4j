package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class ChatResponseJsonDeserializer extends JsonDeserializer<ChatResponse> {

    @Override
    public ChatResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        return null;
    }

}
