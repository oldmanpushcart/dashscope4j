package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.assertApiResponseSuccessful;

public class ChatAudioTestCase extends ClientSupport {

    @Test
    public void test$chat$audio$local() {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN2_AUDIO_INSTRUCT)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("说话的人是男还是女?"),
                        Content.ofAudio(new File("./test-data/audio-001-16K.wav").toURI())
                )))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        assertApiResponseSuccessful(response);
        Assertions.assertTrue(response.output().best().message().text().contains("男"));

    }

}
