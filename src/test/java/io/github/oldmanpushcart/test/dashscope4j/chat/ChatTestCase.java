package io.github.oldmanpushcart.test.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.message.PluginCallMessage;
import io.github.oldmanpushcart.dashscope4j.chat.message.PluginMessage;
import io.github.oldmanpushcart.dashscope4j.util.ConsumeFlowSubscriber;
import io.github.oldmanpushcart.test.dashscope4j.CommonAssertions;
import io.github.oldmanpushcart.test.dashscope4j.DashScopeAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import io.github.oldmanpushcart.test.dashscope4j.chat.function.ComputeAvgScoreFunction;
import io.github.oldmanpushcart.test.dashscope4j.chat.function.EchoFunction;
import io.github.oldmanpushcart.test.dashscope4j.chat.function.QueryScoreFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ChatTestCase implements LoadingEnv {

    private static final Set<ChatModel> models = Set.of(

            // TEXT
            ChatModel.QWEN_PLUS,
            ChatModel.QWEN_TURBO,
            ChatModel.QWEN_MAX,
            ChatModel.QWEN_MAX_LONGCONTEXT,

            // VL
            ChatModel.QWEN_VL_MAX,
            ChatModel.QWEN_VL_PLUS,

            // AUDIO
            ChatModel.QWEN_AUDIO_CHAT,
            ChatModel.QWEN_AUDIO_TURBO
    );

    private static ChatModel getModel(String name) {
        return models.stream()
                .filter(m -> m.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max",
            //"qwen-max-longcontext", //-- 限流太多，暂时从测试用例中去除
            "qwen-vl-max",
            "qwen-vl-plus"
    })
    public void test$chat$text(String name) {

        final var request = ChatRequest.newBuilder()
                .model(getModel(name))
                .messages(
                        Message.ofUser("小红有10块钱"), Message.ofAi("收到"),
                        Message.ofUser("小明比小红多3块"), Message.ofAi("收到"),
                        Message.ofUser("他两一共有多少钱?")
                )
                .build();

        DashScopeAssertions.assertChatRequest(request);

        // ASYNC
        {
            final var response = client.chat(request).async().join();
            Assertions.assertTrue(response.output().best().message().text().contains("23"));
            DashScopeAssertions.assertChatResponse(response);
        }

        // FLOW
        {
            final var stringRef = new AtomicReference<String>();
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        stringRef.set(r.output().best().message().text());
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();

            Assertions.assertTrue(stringRef.get().contains("23"));
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-vl-max",
            "qwen-vl-plus"
    })
    public void test$chat$image(String name) {

        final var request = ChatRequest.newBuilder()
                .model(getModel(name))
                .messages(
                        Message.ofUser(
                                Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                                Content.ofText("请告诉我照片里一共有多少辆自行车?")
                        )
                )
                .build();

        DashScopeAssertions.assertChatRequest(request);

        // ASYNC
        {
            final var response = client.chat(request).async().join();
            final var text = response.output().best().message().text();
            Assertions.assertTrue(text.contains("2") || text.contains("两辆"));
            DashScopeAssertions.assertChatResponse(response);
        }

        // FLOW
        {
            final var stringRef = new AtomicReference<String>();

            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        stringRef.set(r.output().best().message().text());
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();

            final var text = stringRef.get();
            Assertions.assertTrue(text.contains("2") || text.contains("两辆"));
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-audio-turbo",
            "qwen-audio-chat"
    })
    public void test$chat$audio(String name) {

        final var request = ChatRequest.newBuilder()
                .model(getModel(name))
                .messages(
                        Message.ofUser(
                                Content.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav")),
                                Content.ofText("说话的人是男还是女?")
                        )
                )
                .build();

        DashScopeAssertions.assertChatRequest(request);

        // ASYNC
        {
            final var response = client.chat(request).async().join();
            DashScopeAssertions.assertChatResponse(response);
            Assertions.assertTrue(response.output().best().message().text().contains("男"));
        }

        // FLOW
        {
            final var stringRef = new AtomicReference<String>();

            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        stringRef.set(r.output().best().message().text());
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();

            final var text = stringRef.get();
            Assertions.assertTrue(text.contains("男"));
        }

    }

    @Test
    public void test$chat$not_exists_module() {
        final var not_exists_model = ChatModel.ofText("not-exists-module");
        final var request = ChatRequest.newBuilder()
                .model(not_exists_model)
                .user("hello!")
                .build();
        DashScopeAssertions.assertChatRequest(request);
        CommonAssertions.assertRootThrows(ApiException.class, () -> client.chat(request).async().join(), ex -> {
            Assertions.assertTrue(200 != ex.status());
            Assertions.assertFalse(ex.ret().isSuccess());
            Assertions.assertFalse(ex.ret().code().isBlank());
            Assertions.assertFalse(ex.ret().message().isBlank());
        });
    }

    private static final ChatPlugin[] PLUGINS = new ChatPlugin[]{
            ChatPlugin.CALCULATOR,
            ChatPlugin.PDF_EXTRACTER,
    };

    @Test
    public void test$chat$plugin$calculator() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .plugins(PLUGINS)
                .user("1+2*3-4/5=?")
                .build();
        DashScopeAssertions.assertChatRequest(request);
        final var response = client.chat(request)
                .async()
                .join();
        DashScopeAssertions.assertChatResponse(response);
        Assertions.assertTrue(response.output().best().message().text().contains("6.2"));
        Assertions.assertEquals(3, response.output().best().history().size());
        Assertions.assertInstanceOf(PluginCallMessage.class, response.output().best().history().get(0));
        Assertions.assertInstanceOf(PluginMessage.class, response.output().best().history().get(1));
    }

    @Test
    public void test$chat$plugin$pdf_extracter() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .plugins(PLUGINS)
                .user(
                        Content.ofText("请总结这篇文档"),
                        Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"))
                )
                .build();
        final var response = client.chat(request)
                .async()
                .join();
        final var text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("五年规划"));
    }

    @Test
    public void test$chat$function$echo() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .functions(new EchoFunction())
                .user("echo: HELLO!")
                .build();

        // FLOW
        {
            final var stringRef = new AtomicReference<String>();
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        stringRef.set(r.output().best().message().text());
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();
            Assertions.assertEquals("HELLO!", stringRef.get());
        }

        // ASYNC
        {
            final var response = client.chat(request)
                    .async()
                    .join();
            Assertions.assertEquals("HELLO!", response.output().best().message().text());
        }

    }

    @Test
    public void test$chat$function$single_function() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .functions(new QueryScoreFunction())
                .user("查询张三的数学成绩")
                .build();
        final var response = client.chat(request)
                .async()
                .join();
        Assertions.assertTrue(response.output().best().message().text().contains("80"));
    }

    @Test
    public void test$chat$function$multi_function() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .functions(new QueryScoreFunction(), new ComputeAvgScoreFunction())
                .user("张三的所有成绩，并计算平均分")
                .build();
        final var response = client.chat(request)
                .async()
                .join();
        Assertions.assertTrue(response.output().best().message().text().contains("80"));
    }

}
