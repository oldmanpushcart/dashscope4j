package io.github.oldmanpushcart.dashscope4j.client.base.api;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.ImageContent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;

public class ApiOpTestCase implements LoadingEnv {

    @Test
    public void test$api$task() {

        final var model = new ChatModel("wan2.6-image", "/api/v1/services/aigc/image-generation/generation");

        final var imageURI = client.base().store().upload(new File("./test-data/image/red-cup.jpeg").toURI(), model)
                .toCompletableFuture()
                .join();

        final var request = AigcRequest.newBuilder(model)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user(List.of(
                                Content.text("帮我弄成紫色，上边还要画只史努比。"),
                                Content.image(imageURI)
                        )))
                        .build())
                .build();

        final var response = client.base().api().task(request)
                .thenCompose(task -> task.waitingFor(always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        Assertions.assertFalse(response.output().best().message().contents().isEmpty());
        response.output().best().message().contents()
                .stream()
                .filter(ImageContent.class::isInstance)
                .map(ImageContent.class::cast)
                .forEach(content ->
                        DashscopeAssertions.dashscopeAssertImage(
                                client,
                                content.image(),
                                "一只杯子，还是紫色的。杯子上有只史努比的图案"
                        ));


    }

}
