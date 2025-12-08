package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;

import java.io.IOException;

class SessionJsonDeserializer extends JsonDeserializer<Parameters> {

    @Override
    public Parameters deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        final var session = new Parameters();
        final var mapper = parser.getCodec();
        final var rootNode = mapper.<JsonNode>readTree(parser);

        if (null == rootNode || !rootNode.isObject()) {
            return null;
        }

        final var entryIt = rootNode.fields();
        while (entryIt.hasNext()) {

            final var entry = entryIt.next();
            final var name = entry.getKey();
            final var valueNode = entry.getValue();

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
            session.append(name, value);

        }

        return session;
    }

}
