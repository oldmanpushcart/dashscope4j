package io.github.ompc.dashscope4j.test.chat;

import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.message.Content;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.dashscope4j.test.LoadingEnv;
import io.github.ompc.dashscope4j.util.ConsumeFlowSubscriber;
import org.junit.jupiter.api.Assertions;
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
            ChatModel.QWEN_VL_PLUS
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
            // "qwen-max-longcontext", -- 限流太多，暂时从测试用例中去除
            "qwen-vl-max",
            "qwen-vl-plus"
    })
    public void test$chat$text(String name) {

        final var request = ChatRequest.newBuilder()
                .model(getModel(name))
                .messages(
                        Message.ofUser("小红有10块钱"), Message.ofAi("收到"),
                        Message.ofUser("小明比小红多3块"), Message.ofAi("收到"),
                        Message.ofUser("他两一共有多少钱?"
                        )
                )
                .build();


        // ASYNC
        {
            final var response = client.chat(request).async().join();
            Assertions.assertTrue(response.best().message().text().contains("23"));
            ChatAssertions.assertChatResponse(response);
        }

        // FLOW
        {
            final var stringRef = new AtomicReference<String>();
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        stringRef.set(r.best().message().text());
                        ChatAssertions.assertChatResponse(r);
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

        // ASYNC
        {
            final var response = client.chat(request).async().join();
            final var text = response.best().message().text();
            Assertions.assertTrue(text.contains("2") || text.contains("两辆"));
            ChatAssertions.assertChatResponse(response);
        }

        // FLOW
        {
            final var stringRef = new AtomicReference<String>();

            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        stringRef.set(r.best().message().text());
                        ChatAssertions.assertChatResponse(r);
                    }))
                    .join();

            final var text = stringRef.get();
            Assertions.assertTrue(text.contains("2") || text.contains("两辆"));
        }

    }

}
