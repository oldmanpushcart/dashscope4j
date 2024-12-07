package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.util.JacksonUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Value
@Accessors(fluent = true)
public class ChatFunctionTool implements Tool {

    @JsonProperty("function")
    Meta meta;

    ChatFunction<?, ?> function;

    @JsonProperty("type")
    @Override
    public Classify classify() {
        return Classify.FUNCTION;
    }

    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class Call implements Tool.Call {

        @JsonProperty
        String name;

        @JsonProperty
        String arguments;

        @JsonProperty("type")
        @Override
        public Classify classify() {
            return Classify.FUNCTION;
        }

        @JsonProperty("function")
        Map<?, ?> extract() {
            return new HashMap<Object, Object>() {{
                put("name", name);
                put("arguments", arguments);
            }};
        }

    }

    @Value
    @Accessors(fluent = true)
    public static class Meta implements Tool.Meta {

        @JsonProperty
        String name;

        @JsonProperty
        String description;

        @JsonProperty
        TypeSchema parameterTs;

        @Accessors(fluent = true)
        public static class TypeSchema {

            @Getter
            private final Type type;

            @Getter
            private final String schema;

            @JsonValue
            private final JsonNode node;

            public TypeSchema(Type type) {
                final JsonNode node = JacksonUtils.schema(type);
                final String schema = JacksonUtils.toJson(node);
                this.type = type;
                this.node = node;
                this.schema = schema;
            }

            public TypeSchema(Type type, String schema) {
                final JsonNode node = JacksonUtils.toNode(schema);
                this.type = type;
                this.node = node;
                this.schema = schema;
            }

        }

    }


}
