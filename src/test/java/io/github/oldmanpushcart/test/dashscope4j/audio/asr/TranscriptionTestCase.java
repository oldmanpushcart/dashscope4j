package io.github.oldmanpushcart.test.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionOptions;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public class TranscriptionTestCase implements LoadingEnv {

    @Test
    public void test$asr$transcription() {

        final var request = TranscriptionRequest.newBuilder()
                .model(TranscriptionModel.PARAFORMER_V2)
                .resources(List.of(URI.create("https://ompc-storage.oss-cn-hangzhou.aliyuncs.com/dashscope4j/video/%5Bktxp%5D%5BFullmetal%20Alchemist%5D%5Bjap_chn%5D01.rmvb")))
                .option(TranscriptionOptions.ENABLE_DISFLUENCY_REMOVAL, true)
                .option(TranscriptionOptions.LANGUAGE_HINTS, new TranscriptionRequest.LanguageHint[]{TranscriptionRequest.LanguageHint.JA})
                .build();

        final var response = client.audio().transcription(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L * 30)))
                .toCompletableFuture()
                .join();

        Assertions.assertTrue(response.ret().isSuccess());
        Assertions.assertEquals(1, response.output().results().size());

        // assert items
        response.output().results().forEach(item -> {
            Assertions.assertTrue(item.ret().isSuccess());
            final var transcription = item.lazyFetchTranscription().toCompletableFuture().join();
            Assertions.assertNotNull(transcription.meta());
            Assertions.assertNotNull(transcription.originURI());
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

        });

    }

}