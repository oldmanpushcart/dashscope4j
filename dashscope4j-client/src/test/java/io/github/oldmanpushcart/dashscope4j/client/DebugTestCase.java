package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug() {

        final var model = GeneralAigcModel.newBuilder()
                .name("qwen-image-plus")
                .path("/api/v1/services/aigc/text2image/image-synthesis")
                .build();

        final var request = AigcRequest.newBuilder(model)
                .input(Map.of(
                        "prompt", "一副典雅庄重的对联悬挂于厅堂之中，房间是个安静古典的中式布置，桌子上放着一些青花瓷，对联上左书“义本生知人机同道善思新”，右书“通云赋智乾坤启数高志远”， 横批“智启千问”，字体飘逸，在中间挂着一幅中国风的画作，内容是岳阳楼。"
                ))
                .addParameter("n",1)
                .addParameter("prompt_extend", true)
                .build();

        final var response = client.task(request)
                .thenCompose(task-> task.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

        System.out.println(response.output());

    }

}
