package io.github.oldmanpushcart.test.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class QwenVlChatTestCase implements LoadingEnv {

    @Test
    public void test$chat$vl$local_file_image() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofImage(new File("./document/test-resources/image/IMG_0942.JPG").toURI()),
                                Content.ofText("图片中一共多少个男孩?")
                        ))
                ))
                .build();

        final var response = client.chat(request).async().toCompletableFuture().join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("5") || text.contains("五"));

    }

    @Test
    public void test$chat$vl$remote_file_image() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                                Content.ofText("图片中一共多少辆自行车?")
                        ))
                ))
                .build();

        final var response = client.chat(request).async().toCompletableFuture().join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("2") || text.contains("两"));

    }

    @Test
    public void test$chat$vl$local_video_images() {

        final var images = Stream.of(Objects.requireNonNull(new File("./document/test-resources/video/video-001-images").listFiles()))
                .filter(File::isFile)
                .map(File::toURI)
                .limit(20)
                .toList();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofVideo(images),
                                Content.ofText("""
                                        请根据视频内容来判断我的描述是否正确，如果正确请返回TRUE，否则返回FALSE，不需要说多余的话。
                                        我的描述是：
                                        1. 三个人坐在一张桌子旁。
                                        2. 左边是一位穿着白色上衣和米色外套的女士，中间是一位穿着蓝色衬衫和领带的男士，右边是一位穿着黑色西装和领带的男士。
                                        3. 桌子上放着两瓶啤酒、几碗食物和一些餐具。
                                        4. 他们看起来在聊天和享受美食。
                                        5. 背景是一个家庭厨房，可以看到冰箱和其他厨房用具。
                                        """
                                )
                        ))
                ))
                .build();

        final var response = client.chat(request).async()
                .toCompletableFuture()
                .join();

        Assertions.assertTrue("TRUE".equalsIgnoreCase(response.output().best().message().text()));

    }

}
