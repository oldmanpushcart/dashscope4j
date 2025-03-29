package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addMessage(Message.ofUser(
                        "用户输入：\n" +
                            "英伟达公司最新股价" +
                            "\n\n"+
                            "参考资料：\n" +
                            "[]"
                ))
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption()
                        .enableSource(true)
                        .enableCitation(true)
                        .forcedSearch(true))
                .build();

        final Flowable<ChatResponse> responseFlow = client.chat().directFlow(request);
        responseFlow
                .blockingSubscribe(
                        System.out::println,
                        Throwable::printStackTrace
                );

    }

}
