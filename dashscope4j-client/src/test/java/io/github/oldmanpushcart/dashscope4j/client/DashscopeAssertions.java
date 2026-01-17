package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.Content;

import java.net.URI;
import java.util.List;

public class DashscopeAssertions {

    /**
     * 断言文本是否符合预期描述
     *
     * @param dashscope client
     * @param text      文本
     * @param expect    预期描述
     */
    public static void dashscopeAssertText(DashscopeClient dashscope, String text, String expect) {
        final String prompt = """
                请你判断文本内容意思符合预期。如果意思符合，请只输出 TRUE；如果不满足，请只输出 FALSE。不要添加任何解释或额外信息。
                
                内容
                ----------
                %s
                ----------
                
                预期
                ----------
                %s
                ----------
                """.formatted(text, expect);
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.user(prompt))
                .build();
        final ChatResponse response = dashscope.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("""
                    预期描述与文本内容不符
                    
                    预期:
                    %s
                    
                    内容:
                    %s
                    """.formatted(expect, text)
            );
        }
    }


    /**
     * 断言图片是否符合预期描述
     *
     * @param client   client
     * @param imageURI 图片
     * @param expect   预期描述
     */
    public static void dashscopeAssertImage(DashscopeClient client, URI imageURI, String expect) {
        final String prompt = """
                请你判断图片内容符合预期。如果符合，请只输出 TRUE；如果不满足，请只输出 FALSE。不要添加任何解释或额外信息。
                
                预期
                ----------
                %s
                ----------
                """.formatted(expect);
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.user(List.of(
                        Content.text(prompt),
                        Content.image(imageURI)
                )))
                .build();
        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("""
                    预期与实际图片不符
                    
                    预期：
                    %s
                    """.formatted(expect)
            );
        }
    }

}
