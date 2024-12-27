package io.github.oldmanpushcart.dashscope4j.api.image.generation;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.assertApiResponseSuccessful;
import static io.github.oldmanpushcart.dashscope4j.util.CompletableFutureUtils.unwrapEx;

public class GenImageTestCase extends ClientSupport {

    @Test
    @Timeout(60)
    public void test$image$gen() {

        final GenImageRequest request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .prompt("一只五彩斑斓的美女")
                .negative("非亚裔")
                .option(GenImageOptions.NUMBER, 2)
                .option(GenImageOptions.SIZE, GenImageOptions.Size.S_1024_1024)
                .option(GenImageOptions.STYLE, GenImageOptions.Style.CARTOON_3D)
                .build();

        final GenImageResponse response = client.image().generation().task(request)
                .thenCompose(half -> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        assertApiResponseSuccessful(response);
        Assertions.assertEquals(2, response.output().results().size());
        Assertions.assertEquals(2, response.output().results().stream().filter(Ret::isSuccess).count());
        response.output().results().forEach(result -> {
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.isSuccess());
            Assertions.assertNotNull(result.image());
        });

    }

    @Test
    @Timeout(60)
    public void test$image$gen$cancelled_by_timeout() {
        final GenImageRequest request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .prompt("一只五彩斑斓的美女")
                .build();
        Assertions.assertThrows(CancellationException.class, () -> {
            try {
                client.image().generation().task(request)
                        .thenCompose(half -> half.waitingFor(Task.WaitStrategies.until(Duration.ofSeconds(1), Duration.ofSeconds(5))))
                        .toCompletableFuture()
                        .get();
            } catch (ExecutionException ex) {
                throw unwrapEx(ex);
            }

        });

    }

}
