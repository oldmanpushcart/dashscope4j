package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UsageTestCase {

    @Test
    public void test$usage$success() throws JsonProcessingException {

        final var json = """
                {
                   "input_tokens_details": {
                     "image_tokens": 1249,
                     "text_tokens": 16
                   },
                   "prompt_tokens_details": {
                     "cached_tokens": 0
                   },
                   "total_tokens": 1316,
                   "output_tokens": 51,
                   "input_tokens": 1265,
                   "output_tokens_details": {
                     "text_tokens": 51
                   },
                   "image_tokens": 1249
                 }
                """;

        final var mapper = new ObjectMapper();
        final var usage = mapper.readValue(json, Usage.class);

        Assertions.assertEquals(1249, usage.total(item -> item.name().equals("image_tokens")));
        Assertions.assertEquals(1265, usage.total(item -> item.name().equals("input_tokens")));
        Assertions.assertEquals(51, usage.total(item -> item.name().equals("output_tokens")));
        Assertions.assertEquals(1316, usage.total(item-> item.name().equals("total_tokens")));
        Assertions.assertEquals(4, usage.items().size());
        Assertions.assertEquals(3, usage.children().size());

        Assertions.assertEquals(1249, usage.children().get("input_tokens_details").total(item -> item.name().equals("image_tokens")));
        Assertions.assertEquals(16, usage.children().get("input_tokens_details").total(item -> item.name().equals("text_tokens")));
        Assertions.assertEquals(0, usage.children().get("prompt_tokens_details").total(item -> item.name().equals("cached_tokens")));
        Assertions.assertEquals(51, usage.children().get("output_tokens_details").total(item -> item.name().equals("text_tokens")));

        Assertions.assertEquals(0, usage.children().get("prompt_tokens_details").total(item -> item.name().equals("cached_tokens")));


    }

}
