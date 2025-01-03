package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.assertApiResponseSuccessful;

public class ChatVisionTestCase extends ClientSupport {

    @Test
    public void test$chat$vision$local$image() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofImage(new File("./test-data/IMG_0942.JPG").toURI()),
                        Content.ofText("图片中一共多少个男孩?")
                )))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        assertApiResponseSuccessful(response);
        final String text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("5") || text.contains("五"));

    }

    @Test
    public void test$chat$vision$local$images() {

        final List<URI> images = Stream.of(Objects.requireNonNull(new File("./test-data/video-001-images").listFiles()))
                .filter(File::isFile)
                .map(File::toURI)
                .limit(20)
                .collect(Collectors.toList());

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(List.of(
                        Content.ofVideo(images),
                        Content.ofText("请根据视频内容来判断我的描述是否正确，如果正确请返回TRUE，否则返回FALSE，不需要说多余的话。\n" +
                                       "我的描述是：\n" +
                                       "1. 三个人坐在一张桌子旁。\n" +
                                       "2. 左边是一位穿着白色上衣和米色外套的女士，中间是一位穿着蓝色衬衫和领带的男士，右边是一位穿着黑色西装和领带的男士。\n" +
                                       "3. 桌子上放着两瓶啤酒、几碗食物和一些餐具。\n" +
                                       "4. 他们看起来在聊天和享受美食。\n" +
                                       "5. 背景是一个家庭厨房，可以看到冰箱和其他厨房用具。"
                        )
                )))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        assertApiResponseSuccessful(response);
        Assertions.assertTrue("TRUE".equalsIgnoreCase(response.output().best().message().text()));

    }

}
