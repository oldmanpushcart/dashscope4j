package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * 重写用户输入的插件
 */
class BaseRewriteUserMessagePlugin implements Plugin {

    @Override
    public CompletionStage<ChatResponse> onAsync(Processor<ChatResponse> processor) {
        final ChatRequest newRequest = newChatRequest(processor.request());
        return processor.process(newRequest);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> onFlow(Processor<Flowable<ChatResponse>> processor) {
        final ChatRequest newRequest = newChatRequest(processor.request());
        return processor.process(newRequest);
    }

    /**
     * 创建新的对话请求
     *
     * @param request 原始对话请求
     * @return 新的对话请求
     */
    private ChatRequest newChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .building(builder -> buildingForRewriteUserMessage(builder, request))
                .build();
    }

    // 重写用户输入
    private void buildingForRewriteUserMessage(ChatRequest.Builder builder, ChatRequest request) {

        /*
         * 将消息重写为用户的输入
         *
         * 这里之所以需要这样做，主要是消息的多媒体部分是藏在 Message#contents() 中的，
         * 这种情况下并不利于基于文本构建的智能体进行处理，比如ReAct。
         *
         * 所以这里得想办法将消息格式转变为文本的信息，以便于智能体后续的处理
         */
        final Message message = request.requireLastMessageFromUser();
        final String prompt = PromptTemplate.newBuilder()
                .template("### INPUT\n" +
                          "${input}\n" +
                          "\n" +
                          "### PARTS\n" +
                          "${parts}")
                .variable("input", message::text)
                .variable("parts", message.mediaContents()
                        .stream()
                        .map(content -> String.format("- **%s**: %s", content.type(), content.data()))
                        .collect(Collectors.joining("\n")))
                .build()
                .render();

        /*
         * 重组对话请求消息
         * 将重写的消息替换最后一个用户消息
         */
        builder.self()
                .messages(emptyList())
                .addMessages(request.historyMessages())
                .addMessage(Message.ofUser(prompt));
    }

}
