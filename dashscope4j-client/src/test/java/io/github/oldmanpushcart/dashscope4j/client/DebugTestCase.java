package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug() {

        final var model = GeneralAigcModel.newBuilder()
                .name("wanx-sketch-to-image-lite")
                .path("/api/v1/services/aigc/image2image/image-synthesis")
                .inlineEnabled(true)
                .build();

        final var request = AigcRequest.newBuilder(model)
                .input(Map.of(
                        "sketch_image_url", new File("./test-data/image/sketch-tree.jpg"),
                        "prompt", "画一颗参天大树"
                ))
                .addParameter("n",1)
                .build();

        final var response = client.task(request)
                .thenCompose(task-> task.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        System.out.println(response.output());

    }

    @Test
    public void debug2() {

        final var model = GeneralAigcModel.newBuilder()
                .name("qwen3-vl-embedding")
                .path("/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding")
                .inlineEnabled(true)
                .build();

        final var request = AigcRequest.newBuilder(model)
                .input(Map.of(
                        "contents", List.of(
                                Map.of(
                                        "text", "我是一个杯子",
                                        "image", new File("./test-data/image/red-cup.jpeg")
                                )
                        )
                ))
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output());

    }

}
