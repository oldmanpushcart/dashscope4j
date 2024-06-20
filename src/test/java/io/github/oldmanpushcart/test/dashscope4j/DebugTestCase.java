package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.util.ConsumeFlowSubscriber;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

@Disabled
public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug$text() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .user(
                        Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                        Content.ofText("图片中一共多少辆自行车?")
                )
//                .user(
//                        Content.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav")),
//                        Content.ofText("请告诉我说话的人的性别。请不要告诉我其他无关的信息")
//                )
                .build();

//        {
//            final var response = client.chat(request).async()
//                    .join();
//            System.out.println(response.output().best().message().text());
//        }

        {
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        System.out.println(r.output().best().message().text());
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();
        }


    }

    @Test
    public void test$debug$image$gen() {
        final var request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .option(GenImageOptions.NUMBER, 1)
                .prompt("画一只猫")
                .build();
        final var response = client.genImage(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L)))
                .join();
        System.out.println(response);
    }

    @Test
    public void test$debug$embedding$request() {

        final var request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents("我爱北京天安门", "天安门上太阳升")
                .build();

        final var response = client.embedding(request)
                .async()
                .join();

        System.out.println(response);

    }

    @Disabled
    @Test
    public void test$debug() throws Exception {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .system("""
                        Task Description:
                        你的任务是对输入的搜索查询进行改写，以提高其清晰度和检索效果。改写后的查询应该更加具体、完整，并且能更好地表达原查询的意图。你可以通过添加同义词、相关细节或澄清上下文来实现这一点。
                        
                        Instructions:
                        保持查询的中心意图不变。
                        尽量使用自然语言，避免过于生硬或技术性的表达。
                        如果原查询存在语法错误或拼写错误，请予以纠正。
                        考虑到查询可能的上下文，尝试添加相关细节或背景信息。
                        避免添加与查询无关的信息。
                        输出内容控制在2048个字符以内
                        
                        Examples:
                        Input Query: "北京天气"
                        Output Rewritten Query: "北京市当前的天气状况"
                        Input Query: "电影推荐"
                        Output Rewritten Query: "近期值得观看的热门电影推荐"
                        Input Query: "怎么做披萨"
                        Output Rewritten Query: "制作传统意大利披萨的步骤和所需材料"
                        """
                )
                .user("""
                        what is the windows xp end of life?
                        """)
                .build();

        final var response = client.chat(request).async().join();
        final var text = response.output().best().message().text();
        System.out.println(text.replaceAll("^[^\"]*\"", "").replaceAll("\"$", ""));

    }

}
