package io.github.ompc.dashscope4j.test.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

public class ChatResponseTestCase {

    @Test
    public void test$response$support_text() throws IOException {
        final var json = """
                {
                    "output":{
                        "text":"TEST-TEXT",
                        "finish_reason":"stop"
                    },
                    "usage":{
                        "output_tokens":380,
                        "input_tokens":633
                    },
                    "request_id":"d89c06fb-46a1-47b6-acb9-bfb17f814969"
                }
                """;
        final var response = new ObjectMapper().reader().readValue(json, ChatResponse.class);
        Assertions.assertEquals("d89c06fb-46a1-47b6-acb9-bfb17f814969", response.uuid());
        Assertions.assertEquals(1, response.data().choices().size());
        Assertions.assertEquals(1, response.data().choices().get(0).message().contents().size());
        Assertions.assertEquals("TEST-TEXT", response.data().choices().get(0).message().contents().get(0).data());
        Assertions.assertEquals(ChatResponse.Finish.NORMAL, response.data().choices().get(0).finish());
        Assertions.assertEquals(380, response.usage().total(item -> item.name().equals("output_tokens")));
        Assertions.assertEquals(633, response.usage().total(item -> item.name().equals("input_tokens")));
    }

    @Test
    public void test$response$support_text_messages() throws IOException {
        final var json = """
                {
                    "output":{
                        "choices": [
                        	{
                            	"finish_reason":"stop",
                            	"message": {
                              		"role": "system",
                                	"content": "TEST-TEXT"
                            	}
                            }
                        ]
                    },
                    "usage":{
                        "output_tokens":380,
                        "input_tokens":633
                    },
                    "request_id":"d89c06fb-46a1-47b6-acb9-bfb17f814969"
                }
                """;
        final var response = new ObjectMapper().reader().readValue(json, ChatResponse.class);
        Assertions.assertEquals("d89c06fb-46a1-47b6-acb9-bfb17f814969", response.uuid());
        Assertions.assertEquals(1, response.data().choices().size());
        Assertions.assertEquals(1, response.data().choices().get(0).message().contents().size());
        Assertions.assertEquals("TEST-TEXT", response.data().choices().get(0).message().contents().get(0).data());
        Assertions.assertEquals(ChatResponse.Finish.NORMAL, response.data().choices().get(0).finish());
        Assertions.assertEquals(380, response.usage().total(item -> item.name().equals("output_tokens")));
        Assertions.assertEquals(633, response.usage().total(item -> item.name().equals("input_tokens")));
    }

    @Test
    public void test$response$support_multi_message() throws IOException {
        final var json = """
                {
                    "output": {
                        "choices": [
                            {
                                "finish_reason": "stop",
                                "message": {
                                    "role": "assistant",
                                    "content": [
                                        {
                                            "text": "TEST-TEXT"
                                        },
                                        {
                                            "image": "https://example.com/example.png"
                                        }
                                    ]
                                }
                            }
                        ]
                    },
                    "usage": {
                        "output_tokens": 380,
                        "input_tokens": 480,
                        "image_tokens": 680
                    },
                    "request_id": "b042e72d-7994-97dd-b3d2-7ee7e0140525"
                }
                """;
        final var response = new ObjectMapper().reader().readValue(json, ChatResponse.class);
        Assertions.assertEquals("b042e72d-7994-97dd-b3d2-7ee7e0140525", response.uuid());
        Assertions.assertEquals(1, response.data().choices().size());
        Assertions.assertEquals(2, response.data().choices().get(0).message().contents().size());
        Assertions.assertEquals("TEST-TEXT", response.data().choices().get(0).message().contents().get(0).data());
        Assertions.assertEquals(URI.create("https://example.com/example.png"), response.data().choices().get(0).message().contents().get(1).data());
        Assertions.assertEquals(ChatResponse.Finish.NORMAL, response.data().choices().get(0).finish());
        Assertions.assertEquals(380, response.usage().total(item -> item.name().equals("output_tokens")));
        Assertions.assertEquals(480, response.usage().total(item -> item.name().equals("input_tokens")));
        Assertions.assertEquals(680, response.usage().total(item -> item.name().equals("image_tokens")));
    }

    @Test
    public void test$response$error() throws IOException {
        final var json = """
                {
                    "code":"InvalidApiKey",
                    "message":"Invalid API-key provided.",
                    "request_id":"fb53c4ec-1c12-4fc4-a580-cdb7c3261fc1"
                }
                """;
        final var response = new ObjectMapper().reader().readValue(json, ChatResponse.class);
        Assertions.assertEquals("fb53c4ec-1c12-4fc4-a580-cdb7c3261fc1", response.uuid());
        Assertions.assertEquals("InvalidApiKey", response.ret().code());
        Assertions.assertEquals("Invalid API-key provided.", response.ret().message());
    }

}
