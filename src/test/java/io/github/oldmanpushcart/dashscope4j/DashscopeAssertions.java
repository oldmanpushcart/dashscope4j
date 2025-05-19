package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.net.URI;
import java.util.Arrays;

public class DashscopeAssertions {

    public static void dashscopeAssertText(DashscopeClient dashscope, String condition, String target) {
        final String text = "请严格比较以下内容\n"
                            + "### 实际结果\n"
                            + target
                            + "\n\n"
                            + "### 预期条件\n"
                            + condition
                            + "\n\n"
                            + "请你仔细判断实际结果是否完全满足预期条件。如果满足，请只输出 TRUE；如果不满足，请只输出 FALSE。不要添加任何解释或额外信息。";
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser(text))
                .build();
        final ChatResponse response = dashscope.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("预期与实际不符\n"
                                     + "预期条件：\n"
                                     + condition
                                     + "\n\n"
                                     + "实际结果：\n"
                                     + target
            );
        }
    }

    public static void dashscopeAssertImage(DashscopeClient client, String condition, URI imageURI) {
        final String text = "请根据提供的图片内容和以下判断条件进行严格比对\n"
                            + "### 判断条件\n"
                            + condition
                            + "\n\n请仔细检查图片中的信息是否完全符合上述条件。\n"
                            + "如果完全符合，请仅输出 TRUE；否则，请仅输出 FALSE。\n"
                            + "不要添加任何解释或其他多余内容。";
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .context(AutoUploadContext.class, new AutoUploadContext().autoUpload(true))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText(text),
                        Content.ofImage(imageURI)
                )))
                .build();
        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        if (!response.output().best().message().text().contains("TRUE")) {
            throw new AssertionError("期待情况与实际不符\n"
                                     + "期待情况：\n"
                                     + condition
            );
        }
    }

}
