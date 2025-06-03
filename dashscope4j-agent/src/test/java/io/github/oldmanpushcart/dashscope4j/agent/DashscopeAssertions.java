package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.ConfigContext;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.net.URI;
import java.util.Arrays;

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
                请你判断文本内容符合预期描述。如果符合，请只输出 TRUE；如果不满足，请只输出 FALSE。不要添加任何解释或额外信息。
                
                文本内容
                ----------
                %s
                ----------
                
                预期描述
                ----------
                %s
                ----------
                """.formatted(text, expect);
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser(prompt))
                .build();
        final ChatResponse response = dashscope.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("""
                    预期描述与文本内容不符
                    
                    预期描述
                    ----------
                    %s
                    ----------
                    
                    文本内容
                    ----------
                    %s
                    ----------
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
                请你判断图片内容符合预期描述。如果符合，请只输出 TRUE；如果不满足，请只输出 FALSE。不要添加任何解释或额外信息。
                
                预期描述
                ----------
                %s
                ----------
                """.formatted(expect);
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .context(ConfigContext.class, new ConfigContext().autoUpload(true))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText(prompt),
                        Content.ofImage(imageURI)
                )))
                .build();
        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("""
                    预期描述与实际图片不符
                    
                    预期描述：
                    %s
                    """.formatted(expect)
            );
        }
    }

}
