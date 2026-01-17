package io.github.oldmanpushcart.dashscope4j.client.api.image;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageModel;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.task.Task.WaitStrategies.always;

public class ImageOpTestCase implements LoadingEnv {

    private static Stream<Text2ImageModel> provideModelsForText2Image() {
        return Stream.of(
                Text2ImageModel.WANX,
                Text2ImageModel.QWEN_IMAGE,
                Text2ImageModel.QWEN_IMAGE_PLUS
        );
    }

    @ParameterizedTest
    @MethodSource("provideModelsForText2Image")
    public void test$text2image(Text2ImageModel model) {

        final var request = Text2ImageRequest.newBuilder()
                .model(model)
                .prompt("画一只紫色的狗")
                .build();

        final var response = client.image().text2image(request)
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

}
