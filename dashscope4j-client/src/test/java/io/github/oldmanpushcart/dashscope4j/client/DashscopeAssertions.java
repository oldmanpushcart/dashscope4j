package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;

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
        final var request = AigcRequest.newBuilder(ChatModel.QWEN_FLASH)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user(prompt))
                        .build())
                .build();
        final var response = dashscope.async(request)
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
        final var request = AigcRequest.newBuilder(ChatModel.QWEN_VL_MAX)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user(List.of(
                                Content.text(prompt),
                                Content.image(imageURI)
                        )))
                        .build())
                .build();
        final var response = client.async(request)
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

    public static void dashscopeAssertVideo(DashscopeClient client, URI videoURI, String expect) {
        final String prompt = """
                请你判断视频内容符合预期。如果符合，请只输出 TRUE；如果不满足，请只输出 FALSE。不要添加任何解释或额外信息。
                
                预期
                ----------
                %s
                ----------
                """.formatted(expect);
        final var request = AigcRequest.newBuilder(ChatModel.QWEN_VL_MAX)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.user(List.of(
                                Content.text(prompt),
                                Content.video(videoURI)
                        )))
                        .build())
                .build();
        final var response = client.async(request)
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
