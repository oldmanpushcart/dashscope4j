package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

import java.io.IOException;

@JsonDeserialize(using = OmniRealtimeSession.SessionJsonDeserializer.class)
public record OmniRealtimeSession(
        String id,
        String object,
        String model,
        Parameters parameters
) {

    public OmniRealtimeSession() {
        this(null, null, null, new Parameters());
    }

    public OmniRealtimeSession(Parameters parameters) {
        this(null, null, null, parameters);
    }

    static class SessionJsonDeserializer extends JsonDeserializer<OmniRealtimeSession> {

        @Override
        public OmniRealtimeSession deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
            final var parameters = new Parameters();
            final var mapper = parser.getCodec();
            final var rootNode = mapper.<JsonNode>readTree(parser);

            if (null == rootNode || !rootNode.isObject()) {
                return null;
            }

            String id = null;
            String model = null;
            String object = null;

            final var entryIt = rootNode.fields();
            while (entryIt.hasNext()) {

                final var entry = entryIt.next();
                final var name = entry.getKey();
                final var valueNode = entry.getValue();

                if ("id".equals(name)) {
                    id = valueNode.asText();
                    continue;
                }

                if ("object".equals(name)) {
                    object = valueNode.asText();
                    continue;
                }

                if ("model".equals(name)) {
                    model = valueNode.asText();
                    continue;
                }

                Parameters.StdParameterKey<?, ?> key = null;
                for (var registeredKey : OmniRealtimeParameterKeys.REGISTRIES) {
                    if (registeredKey.name().equals(name)) {
                        key = registeredKey;
                        break;
                    }
                }

                if (null == key) {
                    continue;
                }

                final var value = mapper.treeToValue(valueNode, key.type());
                parameters.append(name, value);

            }

            return new OmniRealtimeSession(
                    id,
                    object,
                    model,
                    parameters
            );
        }

    }

}
