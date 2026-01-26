package io.github.oldmanpushcart.dashscope4j.client.vision;

import io.github.oldmanpushcart.dashscope4j.client.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i.TextToImageModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v.TextToVideoModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;

public class VisionOpTestCase implements LoadingEnv {

    private static Stream<TextToImageModel> provideModelsForText2Image() {
        return Stream.of(
                TextToImageModel.QWEN_IMAGE
        );
    }

    @ParameterizedTest
    @MethodSource("provideModelsForText2Image")
    public void test$vision$text2image(TextToImageModel model) {

        final var request = AigcRequest.newBuilder(model)
                .input(TextToImageModel.Input.newBuilder()
                        .prompt("画一只紫色的狗")
                        .build())
                .build();

        final var response = client.task(request)
                .thenCompose(half -> half.waitingFor(always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        ApiAssertions.assertApiResponseSuccessful(response);
        Assertions.assertFalse(response.output().items().isEmpty());
        for (final var item : response.output().items()) {
            DashscopeAssertions.dashscopeAssertImage(
                    client,
                    item.image(),
                    "这是一只狗，还是紫色的"
            );
        }

    }

    @Test
    public void test$vision$text2video() {

        final var request = AigcRequest.newBuilder(TextToVideoModel.WAN_T2V)
                .input(TextToVideoModel.Input.newBuilder()
                        .prompt("杯子在海滩上跳舞")
                        .build())
                .build();

        final var response = client.task(request)
                .thenCompose(half -> half.waitingFor(always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        final var videoURI = response.output().video();
        DashscopeAssertions.dashscopeAssertVideo(
                client,
                videoURI,
                "这是一个杯子，在海滩上"
        );

    }

}
