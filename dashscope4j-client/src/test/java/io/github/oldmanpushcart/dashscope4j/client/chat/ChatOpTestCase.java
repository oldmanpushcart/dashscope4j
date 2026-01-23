package io.github.oldmanpushcart.dashscope4j.client.chat;

import io.github.oldmanpushcart.dashscope4j.client.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

public class ChatOpTestCase implements LoadingEnv {

    private static final Set<Function<AigcRequest<ChatModel.Input, ChatModel.Output>, CompletionStage<AigcResponse<ChatModel.Output>>>> ops = Set.of(
            new Function<>() {
                @Override
                public CompletionStage<AigcResponse<ChatModel.Output>> apply(AigcRequest<ChatModel.Input, ChatModel.Output> request) {
                    return client.aigc().async(request)
                            .thenApply(r -> {
                                ApiAssertions.assertApiResponseSuccessful(r);
                                return r;
                            });
                }

                @Override
                public String toString() {
                    return "asyncOp";
                }

            },
            new Function<>() {
                @Override
                public CompletionStage<AigcResponse<ChatModel.Output>> apply(AigcRequest<ChatModel.Input, ChatModel.Output> request) {
                    final var newRequest = AigcRequest.newBuilder(request)
                            .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, true)
                            .build();
                    return FlowX.fromPublisher(client.aigc().flow(newRequest))
                            .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                            .reduce(AigcResponse::accumulate);
                }

                @Override
                public String toString() {
                    return "flowOp";
                }

            }
    );

    /**
     * 测试参数
     *
     * @param model 模型
     * @param op    操作
     */
    public record Data(ChatModel model,
                       Function<AigcRequest<ChatModel.Input, ChatModel.Output>, CompletionStage<AigcResponse<ChatModel.Output>>> op) {

        @Override
        public String toString() {
            return "%s , %s".formatted(model.name(), op);
        }

    }

    private static Stream<Data> provideDataFromModels(ChatModel... models) {
        return Arrays.stream(models)
                .flatMap(m -> ops.stream().map(op -> new Data(m, op)));
    }

    static Stream<Data> provideDataForText() {
        return provideDataFromModels(
                ChatModel.QWEN_FLASH,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX,
                ChatModel.QWEN_LONG,
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWQ_PLUS,
                ChatModel.QVQ_MAX,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForText")
    public void test$chat$text(Data data) {
        final var request = AigcRequest.newBuilder(data.model)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user("(1+2+3+4)/5=?"))
                        .build())
                .build();
        final var response = data.op().apply(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client, response.output().best().message().text(), "答案是2");
    }


    static Stream<Data> provideDataForFunction() {
        return provideDataFromModels(
                ChatModel.QWEN_FLASH,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX,
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWQ_PLUS,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForFunction")
    public void test$chat$text$function(Data data) {
        final var called = new AtomicBoolean(false);
        final var request = AigcRequest.newBuilder(data.model)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user("请问英语和物理分别是多少分?"))
                        .build())
                .addParameter(ChatParameterKeys.TOOLS, new Tool[]{
                        new QueryScoreFunction() {

                            @Override
                            public Result query(Query query) {
                                called.set(true);
                                return super.query(query);
                            }

                        }.toTool()
                })
                .build();
        final var response = data.op().apply(request)
                .toCompletableFuture()
                .join();
        final var message = response.output().best().message();
        Assertions.assertTrue(called.get(), "Expected function to be called, but it was not.");
        DashscopeAssertions.dashscopeAssertText(client, message.text(), "英语成绩是50分，物理成绩是85分");
    }

    static Stream<Data> provideDataForImage() {
        return provideDataFromModels(
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWEN3_OMNI_FLASH,
                ChatModel.QVQ_MAX
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForImage")
    public void test$chat$image(Data data) {
        final var request = AigcRequest.newBuilder(data.model())
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user(List.of(
                                Content.text("请描述图片的内容"),
                                Content.image(new File("./test-data/image/red-cup.jpeg").toURI())
                        )))
                        .uploadEnabled(true)
                        .build())
                .build();
        final var response = data.op().apply(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client,
                response.output().best().message().text(),
                "这是一个红色的马克杯"
        );
    }


    static Stream<Data> provideDataForAudio() {
        return provideDataFromModels(
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForAudio")
    public void test$chat$audio(Data data) {
        final var request = AigcRequest.newBuilder(data.model())
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user(List.of(
                                Content.text("这个音频文件提到了什么?"),
                                Content.audio(new File("./test-data/audio/beach-woman-dog.wav").toURI())
                        )))
                        .inlineEnabled(true)
                        .build())
                .build();
        final var response = data.op().apply(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client,
                response.output().best().message().text(),
                "提到了：海滩、女人和狗"
        );
    }

    static Stream<Data> provideDataForVideo() {
        return provideDataFromModels(
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForVideo")
    public void test$chat$video$images(Data data) {
        final var imageURIs = Stream.of(Objects.requireNonNull(new File("./test-data/image/video-001-images").listFiles()))
                .filter(File::isFile)
                .map(File::toURI)
                .limit(20)
                .toList();

        final var request = AigcRequest.newBuilder(data.model())
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.system("请用中文回答"))
                        .addMessage(Message.user(List.of(
                                Content.text("请告诉我视频中的性别和对应数量"),
                                Content.video(imageURIs)
                        )))
                        .inlineEnabled(true)
                        .build())
                .build();

        final var response = data.op().apply(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client,
                response.output().best().message().text(),
                "视频中有2男1女"
        );
    }


    static Stream<Data> provideDataForLong() {
        return provideDataFromModels(
                ChatModel.QWEN_LONG
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForLong")
    public void test$chat$long(Data data) {
        final var request = AigcRequest.newBuilder(data.model())
                .input(ChatModel.Input.newBuilder()
                        // .addMessage(Message.system("you are a helpful assistant"))
                        .addMessage(Message.system("fileid://file-fe-58febfa682b34d898b1693a6"))
                        .addMessage(Message.user("请帮我分析这个文件，并给出一个总结"))
                        .build())
                .build();

        final var response = data.op().apply(request)
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(
                client,
                response.output().best().message().text(),
                "这篇文章说的是中国第十四个五年规划"
        );
    }

    static Stream<Data> provideDataForText2Image() {
        return provideDataFromModels(
                ChatModel.QWEN_IMAGE_MAX,
                ChatModel.WAN_T2I
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForText2Image")
    public void test$chat$text2image(Data data) {
        final var request = AigcRequest.newBuilder(data.model())
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user("帮我画一朵紫色的向日葵"))
                        .build())
                .build();

        final var imageURIs = data.op().apply(request)
                .thenApply(response -> {
                    final var message = response.output().best().message();
                    return message.contents().stream()
                            .filter(ImageContent.class::isInstance)
                            .map(ImageContent.class::cast)
                            .map(ImageContent::image)
                            .toList();
                })
                .toCompletableFuture()
                .join();

        Assertions.assertFalse(imageURIs.isEmpty());
        for (final var imageURI : imageURIs) {
            DashscopeAssertions.dashscopeAssertImage(client, imageURI, "这是一朵紫色的向日葵");
        }
    }

}
