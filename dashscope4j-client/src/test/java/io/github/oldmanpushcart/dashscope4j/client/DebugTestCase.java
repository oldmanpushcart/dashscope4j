package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeSessionUpdateClientEvent;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug4() {

        final var parameters = new Parameters()
                .append(OmniRealtimeParameterKeys.VOICE, "OMPC");
        final var event = new OmniRealtimeSessionUpdateClientEvent("1", new OmniRealtimeSession(parameters));
        final var json = JacksonJsonUtils.toJson(event);
        System.out.println(json);

    }

    private void reconnect(OmniRealtimeOp omniRealtimeOp) {

    }

    @Test
    public void debug5() throws InterruptedException {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_OMNI_FLASH)
                .addMessage(Message.user(List.of(
                        Content.text("请用中文描述图片"),
                        Content.image(new File("./test-data/image/red-cup.jpeg").toURI())
                )))
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                .build();

        final var chatOp = client.chat();

        final var publisher = chatOp.flow(request);

        final var latch = new CountDownLatch(1);
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ChatResponse item) {
                if (!item.output().choices().isEmpty()) {
                    System.out.println("===" + item.output().best().message().text());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("complete");
                latch.countDown();
            }
        });

        latch.await();

    }

    private record Person(

            @JsonProperty(required = true)
            String name,

            @JsonProperty
            int age,

            @JsonProperty
            Gender gender,

            @JsonProperty(required = true)
            List<Address> addresses,

            @JsonProperty
            String email,

            @JsonProperty
            Instant birthday
    ) {

    }

    private record Address(

            @JsonProperty
            String province,

            @JsonProperty
            String city,

            @JsonProperty
            String street,

            @JsonProperty
            String zipCode
    ) {

    }

    private enum Gender {
        MALE,
        FEMALE
    }

    @Test
    public void debug6() {

        for (int i = 0; i < 5; i++) {
            client.base().files().create(new File("./test-data/image/red-cup.jpeg"), Purpose.FILE_EXTRACT)
                    .toCompletableFuture()
                    .join();
        }

        FlowX.fromPublisher(client.base().files().flow(1))
                .blockingCollect(Collectors.toList())
                .forEach(meta -> {
                    client.base().files().detail(meta.identity())
                            .toCompletableFuture()
                            .join();
                });


    }

}
