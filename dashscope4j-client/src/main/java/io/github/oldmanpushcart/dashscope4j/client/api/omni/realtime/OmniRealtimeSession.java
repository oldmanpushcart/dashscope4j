package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

import java.io.IOException;

@JsonDeserialize(using = OmniRealtimeSession.SessionJsonDeserializer.class)
@JsonSerialize(using = OmniRealtimeSession.SessionJsonSerializer.class)
public record OmniRealtimeSession(
        String id,
        String object,
        String model,
        Parameters parameters
) {

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_MODEL = "model";

    public OmniRealtimeSession(Parameters parameters) {
        this(null, null, null, parameters);
    }

    static class SessionJsonDeserializer extends JsonDeserializer<OmniRealtimeSession> {

        @Override
        public OmniRealtimeSession deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {

            final var mapper = (ObjectMapper) parser.getCodec();
            final var node = mapper.<JsonNode>readTree(parser);

            final var id = node.has(FIELD_ID)
                    ? node.get(FIELD_ID).asText()
                    : null;
            final var object = node.has(FIELD_OBJECT)
                    ? node.get(FIELD_OBJECT).asText()
                    : null;
            final var model = node.has(FIELD_MODEL)
                    ? node.get(FIELD_MODEL).asText()
                    : null;

            final var parameters = new Parameters();
            final var fields = node.fields();
            while (fields.hasNext()) {
                final var entry = fields.next();
                final var key = entry.getKey();
                if (FIELD_ID.equals(key) || FIELD_OBJECT.equals(key) || FIELD_MODEL.equals(key)) {
                    continue;
                }
                parameters.append(key, mapper.treeToValue(entry.getValue(), Object.class));
            }

            return new OmniRealtimeSession(id, object, model, parameters);
        }

    }

    static class SessionJsonSerializer extends JsonSerializer<OmniRealtimeSession> {

        @Override
        public void serialize(OmniRealtimeSession session, JsonGenerator generator, SerializerProvider provider) throws IOException {

            generator.writeStartObject();

            // 写入固定字段
            if (session.id() != null) {
                generator.writeStringField(FIELD_ID, session.id());
            }
            if (session.object() != null) {
                generator.writeStringField(FIELD_OBJECT, session.object());
            }
            if (session.model() != null) {
                generator.writeStringField(FIELD_MODEL, session.model());
            }

            // 写入 parameters 中的所有字段
            if (session.parameters() != null && !session.parameters().isEmpty()) {
                for (final var entry : session.parameters().dump().entrySet()) {
                    generator.writeObjectField(entry.getKey(), entry.getValue());
                }
            }

            generator.writeEndObject();

        }

    }

}
