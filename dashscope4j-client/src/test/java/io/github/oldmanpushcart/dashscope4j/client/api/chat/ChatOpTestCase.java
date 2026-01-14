package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOpHelper;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ChatOpTestCase implements LoadingEnv {

    static Stream<ChatModel> provideModelsForText() {
        return Stream.of(
                ChatModel.QWEN_TURBO,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX,
                ChatModel.QWEN_LONG,
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWQ_PLUS,
                ChatModel.QWQ_PLUS_LATEST,
                ChatModel.QVQ_MAX,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    static Stream<ChatModel> provideModelsForFunction() {
        return Stream.of(
                ChatModel.QWEN_TURBO,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX,
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWQ_PLUS,
                ChatModel.QWQ_PLUS_LATEST,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    static Stream<ChatModel> provideModelsForImage() {
        return Stream.of(
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWEN3_OMNI_FLASH,
                ChatModel.QVQ_MAX
        );
    }

    static Stream<ChatModel> provideModelsForAudio() {
        return Stream.of(
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    static Stream<ChatModel> provideModelsForVideo() {
        return Stream.of(
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }


    @ParameterizedTest
    @MethodSource("provideModelsForText")
    public void test$chat$text$async(ChatModel model) {
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user("(1+2+3+4)/5=?"))
                .build();
        final var response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client, response.output().best().message().text(), "答案是2");
    }

    @ParameterizedTest
    @MethodSource("provideModelsForText")
    public void test$chat$text$flow(ChatModel model) {
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user("(1+2+3+4)/5=?"))
                .build();
        final var response = FlowX.fromPublisher(client.chat().flow(request))
                .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                .reduce(ChatResponse::accumulate)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client, response.output().best().message().text(), "答案是2");
    }

    @ParameterizedTest
    @MethodSource("provideModelsForFunction")
    public void test$chat$text$function$async(ChatModel model) {
        final var called = new AtomicBoolean(false);
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user("请问英语和物理分别是多少分?"))
                .addTool(new QueryScoreFunction() {

                    @Override
                    public Result query(Query query) {
                        called.set(true);
                        return super.query(query);
                    }

                }.toTool())
                .build();
        final var response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        final var message = response.output().best().message();
        Assertions.assertTrue(called.get(), "Expected function to be called, but it was not.");
        DashscopeAssertions.dashscopeAssertText(client, message.text(), "英语成绩是50分，物理成绩是85分");
    }

    @ParameterizedTest
    @MethodSource("provideModelsForFunction")
    public void test$chat$text$function$flow(ChatModel model) {

        final var called = new AtomicBoolean(false);
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user("请问英语和物理分别是多少分?"))
                .addTool(new QueryScoreFunction() {

                    @Override
                    public Result query(Query query) {
                        called.set(true);
                        return super.query(query);
                    }

                }.toTool())
                .build();

        final var response = FlowX.fromPublisher(client.chat().flow(request))
                .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                .reduce(ChatResponse::accumulate)
                .toCompletableFuture()
                .join();
        final var message = response.output().best().message();
        Assertions.assertTrue(called.get(), "Expected function to be called, but it was not.");
        DashscopeAssertions.dashscopeAssertText(client, message.text(), "英语成绩是50分，物理成绩是85分");
    }

    @ParameterizedTest
    @MethodSource("provideModelsForImage")
    public void test$chat$image(ChatModel model) {
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user(List.of(
                        Content.text("请描述图片的内容"),
                        Content.image(new File("./test-data/image/red-cup.jpeg").toURI())
                )))
                .build();
        final var response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client,
                response.output().best().message().text(),
                "这是一个红色的马克杯"
        );
    }

    @ParameterizedTest
    @MethodSource("provideModelsForAudio")
    public void test$chat$audio(ChatModel model) {
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user(List.of(
                        Content.text("这个音频文件提到了什么?"),
                        Content.audio(new File("./test-data/audio/beach-woman-dog.wav").toURI())
                )))
                .build();
        final var response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client,
                response.output().best().message().text(),
                "提到了：海滩、女人和狗"
        );
    }

    @ParameterizedTest
    @MethodSource("provideModelsForVideo")
    public void test$chat$video$images(ChatModel model) {

        final var imageURIs = Stream.of(Objects.requireNonNull(new File("./test-data/image/video-001-images").listFiles()))
                .filter(File::isFile)
                .map(File::toURI)
                .limit(20)
                .toList();

        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user(List.of(
                        Content.text("请告诉我视频中的性别和对应数量"),
                        Content.video(imageURIs)
                )))
                .build();

        final var response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client,
                response.output().best().message().text(),
                "视频中有2男1女"
        );

    }


    @Test
    public void test$chat$long() {

        final var file = new File("./test-data/document/P020210313315693279320.pdf");
        final var fileURI = file.toURI();
        final var name = FilesOpHelper.encodeFilename(file.getName());
        final var meta = client.base().files().create(fileURI, name, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .addMessage(Message.system(meta.toURI().toString()))
                .addMessage(Message.user("请帮我分析这个文件，并给出一个总结"))
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        final var response = FlowX.fromPublisher(client.chat().flow(request))
                .reduce(ChatResponse::accumulate)
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(
                client,
                response.output().best().message().text(),
                "这篇文章说的是中国第十四个五年规划"
        );

    }

//    @Test
//    public void test$chat$genImage() {
//
//        final var request = ChatRequest.newBuilder()
//                .model(ChatModel.QWEN_IMAGE)
//                .addMessage(Message.user("帮我画一朵紫色的向日葵"))
//                .build();
//
//        final var response = client.chat().async(request)
//                .toCompletableFuture()
//                .join();
//
//        System.out.println(response.output().best().message());
//
//    }

}
