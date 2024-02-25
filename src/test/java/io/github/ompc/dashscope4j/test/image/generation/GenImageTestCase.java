package io.github.ompc.dashscope4j.test.image.generation;

import io.github.ompc.dashscope4j.task.Task;
import io.github.ompc.dashscope4j.image.generation.GenImageModel;
import io.github.ompc.dashscope4j.image.generation.GenImageOptions;
import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.dashscope4j.test.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

public class GenImageTestCase implements LoadingEnv {

    @Test
    @Timeout(60)
    public void test$image$gen() {

        final var request = GenImageRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .model(GenImageModel.WANX_V1)
                .prompt("一只五彩斑斓的美女")
                .negative("非亚裔")
                .option(GenImageOptions.NUMBER, 4)
                .option(GenImageOptions.SIZE, GenImageRequest.Size.S_1024_1024)
                .option(GenImageOptions.STYLE, GenImageRequest.Style.CARTOON_3D)
                .build();

        GenImageAssertions.assertGenImageRequest(request);

        final var response = client.genImage(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofSeconds(1)))
                .join();

        GenImageAssertions.assertGenImageResponse(response);
        Assertions.assertEquals(4, response.output().results().size());
        Assertions.assertEquals(4, response.output().results().stream().filter(result -> result.ret().isSuccess()).count());

    }

    @Test
    @Timeout(60)
    public void test$image$gen$cancelled_by_timeout() {
        final var request = GenImageRequest.newBuilder()
                .timeout(Duration.ofSeconds(5))
                .model(GenImageModel.WANX_V1)
                .prompt("一只五彩斑斓的美女")
                .build();
        Assertions.assertThrows(CancellationException.class, () -> {
            try {
                client.genImage(request)
                        .task(Task.WaitStrategies.timeout(Duration.ofSeconds(1), Duration.ofSeconds(5)))
                        .join();
            } catch (CompletionException ce) {
                throw ce.getCause();
            }
        });
    }

}
