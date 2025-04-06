package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.github.oldmanpushcart.dashscope4j.util.HttpUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;

public class TranscriptionTestCase extends ClientSupport {

    private static void assertTranscriptionResponse(TranscriptionResponse response) {
        Assertions.assertFalse(response.output().results().isEmpty());
        response.output().results().forEach(item -> {

            Assertions.assertNotNull(item.transcriptionURI());
            Assertions.assertNotNull(item.originURI());
            Assertions.assertTrue(item.isSuccess());
            item.fetchTranscription(uri -> HttpUtils.fetchAsString(client.base().http(), uri))
                    .thenAccept(transcription -> {
                        Assertions.assertFalse(transcription.transcripts().isEmpty());

                        // assert transcripts
                        transcription.transcripts().forEach(transcript -> {
                            Assertions.assertNotNull(transcript.text());
                            Assertions.assertTrue(transcript.duration().toMillis() > 0);
                            Assertions.assertFalse(transcript.sentences().isEmpty());

                            // assert sentences
                            transcript.sentences().forEach(sentence -> {
                                Assertions.assertNotNull(sentence.text());
                                Assertions.assertTrue(sentence.end() > sentence.begin());
                                Assertions.assertTrue(sentence.isEnd());

                                // assert words
                                sentence.words().forEach(word -> {
                                    Assertions.assertNotNull(word.text());
                                    Assertions.assertTrue(word.end() > word.begin());
                                });

                            });

                        });

                    })
                    .toCompletableFuture()
                    .join();

        });
    }

    @Test
    public void test$transcription() {

        final TranscriptionRequest request = TranscriptionRequest.newBuilder()
                .model(TranscriptionModel.PARAFORMER_V2)
                .addResource(new File("./test-data/poetry-DengHuangHeLou.wav").toURI())
                .build();

        final TranscriptionResponse response = client.audio().transcription().task(request)
                .thenCompose(half -> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        ApiAssertions.assertApiResponseSuccessful(response);
        assertTranscriptionResponse(response);
        response.output().results().forEach(item ->
                item.fetchTranscription(uri -> HttpUtils.fetchAsString(client.base().http(), uri))
                        .thenAccept(transcription -> {
                            final boolean matched = transcription.transcripts().stream()
                                    .anyMatch(transcript -> transcript.text().equals("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。"));
                            Assertions.assertTrue(matched);
                        })
                        .toCompletableFuture()
                        .join());

    }

}
