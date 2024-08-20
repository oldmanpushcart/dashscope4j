package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Ret;

import java.io.IOException;

record OutFrame(

        @JsonProperty("header")
        Header header,

        @JsonDeserialize(using = PayloadAsStringDeserializer.class)
        @JsonProperty("payload")
        String payload

) {

    public static class PayloadAsStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parser.getCodec().<JsonNode>readTree(parser).toString();
        }
    }

    public record Header(

            @JsonProperty("task_id")
            String uuid,

            @JsonProperty("event")
            Type type,

            @JsonProperty("error_code")
            String code,

            @JsonProperty("error_message")
            String message

    ) {

        public Ret ret() {
            return Ret.of(code, message);
        }

    }

    public enum Type {

        @JsonProperty("task-started")
        STARTED,

        @JsonProperty("result-generated")
        GENERATED,

        @JsonProperty("task-failed")
        FAILED,

        @JsonProperty("task-finished")
        FINISHED

    }

}
