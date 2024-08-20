package io.github.oldmanpushcart.test.dashscope4j.image.generation;

import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.test.dashscope4j.CommonAssertions;
import io.github.oldmanpushcart.test.dashscope4j.DashScopeAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CancellationException;

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

        DashScopeAssertions.assertGenImageRequest(request);

        final var response = client.image().generation(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofSeconds(1)))
                .toCompletableFuture()
                .join();

        DashScopeAssertions.assertGenImageResponse(response);
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
        CommonAssertions.assertRootThrows(CancellationException.class, () ->
                client.image().generation(request)
                        .task(Task.WaitStrategies.timeout(Duration.ofSeconds(1), Duration.ofSeconds(5)))
                        .toCompletableFuture()
                        .join());
    }

}
