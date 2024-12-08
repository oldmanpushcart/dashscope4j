package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.EchoFunction;
import org.junit.jupiter.api.Test;

public class DebugTestCase {

    @Test
    public void test$debug() throws JsonProcessingException {

        final ObjectMapper mapper = new ObjectMapper();
        final JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        final JsonSchema schema = schemaGen.generateSchema(EchoFunction.Echo.class);
        final String schemaJson = mapper.writer().writeValueAsString(schema);
        final JsonNode schemaNode = mapper.reader().readTree(schemaJson);
        System.out.println(schemaJson);

    }

}
