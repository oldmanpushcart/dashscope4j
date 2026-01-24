package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2i.TextToImageModel;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.time.Duration;

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
                .thenCompose(half -> half.waitingFor(always(Duration.ofSeconds(1))))
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

        final AigcModel<?, ?> model = TextToImageModel.QWEN_IMAGE;
        final Type type = JacksonJsonUtils.newMapper()
                .getTypeFactory()
                .constructParametricType(AigcResponse.class, model.outputType());

        final var response = JacksonJsonUtils.toApiResponse(json, type, null, null);
        System.out.println(response);

    }


}
