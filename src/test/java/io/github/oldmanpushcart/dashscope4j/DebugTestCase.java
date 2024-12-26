package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionModel;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {
        final FileMeta meta = client.base().files().create(new File("./test-data/P020210313315693279320.pdf"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        System.out.println(meta);
    }

    @Test
    public void test$debug1() throws IOException {
        final TranscriptionRequest request = TranscriptionRequest.newBuilder()
                .model(TranscriptionModel.PARAFORMER_V2)
                .addResource(new File("./test-data/poetry-DengHuangHeLou.wav").toURI())
                .build();
        final TranscriptionResponse response = client.audio().transcription().task(request)
                .thenCompose(half-> {
                    return half.waitingFor(new Task.WaitStrategy() {
                        @Override
                        public CompletionStage<?> performWait(Task task) {
                            return CompletableFuture.completedFuture(null);
                        }
                    });
                })
                .toCompletableFuture()
                .join();
        System.out.println(response);
        response.output().results().forEach(item-> {
            item.fetchTranscription()
                    .thenAccept(System.out::println)
                    .toCompletableFuture()
                    .join();
        });
    }

}
