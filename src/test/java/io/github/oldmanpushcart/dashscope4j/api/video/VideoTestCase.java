package io.github.oldmanpushcart.dashscope4j.api.video;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.*;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.time.Duration;

public class VideoTestCase extends ClientSupport {

    @Test
    public void test$video$genByText() {

        final TextGenVideoRequest request = TextGenVideoRequest.newBuilder()
                .model(TextGenVideoModel.WANX_V2_1_T2V_TURBO)
                .prompt("中式美女小乔出嫁，坐在梳妆台前，楚楚动人的在装扮着自己。")
                .option(TextGenVideoOptions.ENABLE_PROMPT_EXTEND, true)
                .build();

        final URI videoURI = client.video().genByText().task(request)
                .thenCompose(half-> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(30))))
                .thenApply(response-> response.output().video())
                .toCompletableFuture()
                .join();

        Assertions.assertNotNull(videoURI);

    }

    @Test
    public void test$video$genByImage() {

        final ImageGenVideoRequest request = ImageGenVideoRequest.newBuilder()
                .model(ImageGenVideoModel.WANX_V2_1_I2V_TURBO)
                .prompt("中式美女小乔出嫁，坐在梳妆台前，楚楚动人的在装扮着自己。")
                .image(new File("./test-data/lingzhiling.jpg").toURI())
                .option(TextGenVideoOptions.ENABLE_PROMPT_EXTEND, true)
                .build();

        final URI videoURI = client.video().genByImage().task(request)
                .thenCompose(half-> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(30))))
                .thenApply(response-> response.output().video())
                .toCompletableFuture()
                .join();

        Assertions.assertNotNull(videoURI);

    }

}
