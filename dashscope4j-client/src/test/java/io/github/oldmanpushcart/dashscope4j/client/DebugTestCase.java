package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.Model;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i.TextToImageModel;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeSessionUpdateClientEvent;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug() {

        final var request = AigcRequest.newBuilder(TextToImageModel.QWEN_IMAGE)
                .input(TextToImageModel.Input.newBuilder()
                        .prompt("Red Cup")
                        .build())
                .build();

        final var response = client.base().api().task(request)
                .thenCompose(half-> half.waitingFor(always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        System.out.println(response);

    }


    @Test
    public void test$debug2() {

        final var json = """
                {
                  "request_id": "18eb9866-a9d2-4d40-a02f-544e0fa3b6e5",
                  "output": {
                    "task_id": "6e55ec38-c54d-4437-a76e-f0a452eee269",
                    "task_status": "SUCCEEDED",
                    "submit_time": "2026-01-20 14:21:28.194",
                    "scheduled_time": "2026-01-20 14:21:28.222",
                    "end_time": "2026-01-20 14:21:34.843",
                    "results": [
                      {
                        "orig_prompt": "red cup",
                        "actual_prompt": "A sleek, modern red ceramic coffee cup centered in the frame, standing upright on a minimalist white marble surface. The cup features a smooth, glossy finish with subtle matte undertones, reflecting soft ambient light. A thin golden rim accents the top edge, enhancing its premium aesthetic. In the bottom-right corner, the text \\"Sip & Savor\\" is rendered in clean, sans-serif font with a brushed gold effect, slightly raised for depth. The background is softly blurred, featuring a faint gradient of warm beige and pale gray, evoking a calm, contemporary interior setting. Shot in natural daylight with shallow depth of field, capturing fine texture details—tiny micro-cracks in the glaze, slight variations in red hue. Style: hyper-realistic product photography, 8K resolution, Fujifilm GFX100S sensor simulation, studio lighting with soft shadows, cinematic clarity, emphasizing materiality and elegance.",
                        "url": "https://dashscope-result-wlcb-acdr-1.oss-cn-wulanchabu-acdr-1.aliyuncs.com/7d/b2/20260120/cfc32567/6e55ec38-c54d-4437-a76e-f0a452eee2693421461243.png?Expires=1769495894&OSSAccessKeyId=LTAI5tKPD3TMqf2Lna1fASuh&Signature=esPzuW0r46PHtf%2BE%2B0eq%2BdX4fMI%3D"
                      }
                    ]
                  },
                  "usage": {
                    "image_count": 1
                  }
                }
                """;

        final Model<?,?> model = TextToImageModel.QWEN_IMAGE;
        final Type type = JacksonJsonUtils.newMapper()
                .getTypeFactory()
                .constructParametricType(AigcResponse.class, model.outputType());

        final var response = JacksonJsonUtils.toApiResponse(json, type, null, null);
        System.out.println(response);

    }

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
