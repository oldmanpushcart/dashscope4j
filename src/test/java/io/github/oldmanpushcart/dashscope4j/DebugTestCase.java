package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final GenImageRequest request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .prompt("一只五彩斑斓的美女")
                .negative("非亚裔")
                .reference(URI.create("https://p3-pc-sign.douyinpic.com/tos-cn-p-0015c000-ce/o4ut2eYWDQNyor82ArFB8e7BHCfnP0JQEWgbBa~tplv-dy-360p.jpeg?biz_tag=pcweb_cover&card_type=153&column_n=0&from=327834062&lk3s=138a59ce&s=PackSourceEnum_SEARCH&sc=origin_cover&se=false&x-expires=1745730000&x-signature=9dMLVWv3ev81pu2O%2BYAcYOh%2B6a4%3D"))
                .option(GenImageOptions.NUMBER, 2)
                .option(GenImageOptions.SIZE, GenImageOptions.Size.S_1024_1024)
                .option(GenImageOptions.STYLE, GenImageOptions.Style.CARTOON_3D)
                .build();

        final GenImageResponse response = client.image().generation().task(request)
                .thenCompose(half -> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))))
                .toCompletableFuture()
                .join();

    }

}
