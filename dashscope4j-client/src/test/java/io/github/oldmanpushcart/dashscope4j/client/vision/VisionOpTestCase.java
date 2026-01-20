package io.github.oldmanpushcart.dashscope4j.client.vision;

import io.github.oldmanpushcart.dashscope4j.client.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageModel;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoModel;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.time.Duration;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;

public class VisionOpTestCase implements LoadingEnv {

    private static Stream<Text2ImageModel> provideModelsForText2Image() {
        return Stream.of(
                Text2ImageModel.WAN_T2I,
                Text2ImageModel.QWEN_IMAGE,
                Text2ImageModel.QWEN_IMAGE_PLUS
        );
    }

    @ParameterizedTest
    @MethodSource("provideModelsForText2Image")
    public void test$vision$text2image(Text2ImageModel model) {

        final var request = Text2ImageRequest.newBuilder()
                .model(model)
                .prompt("画一只紫色的狗")
                .build();

        final var response = client.vision().t2i().task(request)
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

        final var request = Text2VideoRequest.newBuilder()
                .model(Text2VideoModel.WAN_T2V)
                .prompt("杯子在海滩上跳舞")
                .build();

        final var response = client.vision().t2v().task(request)
                .thenCompose(half -> half.waitingFor(always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        final var videoURI = response.output().video();
        DashscopeAssertions.dashscopeAssertVideo(
                client,
                videoURI,
                "这是一个杯子，在海滩上跳舞"
        );

    }

}
