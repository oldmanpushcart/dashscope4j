package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.internal.util.JacksonUtils;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.util.List;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.agent.internal.util.ResourceUtils.resourceToString;

public class BaseRewriteUserMessagePromptTemplate extends PromptTemplate {

    private static final String PROMPT_RES_NAME = "dashscope4j/agent/prompt/base-rewrite-user-message-prompt.md";
    public static final String NAME_ATTACHMENTS = "attachments";
    public static final String NAME_QUESTION = "question";

    public BaseRewriteUserMessagePromptTemplate() {
        super(resourceToString(PROMPT_RES_NAME));
    }

    public BaseRewriteUserMessagePromptTemplate message(Message message) {

        final String question = message.text();
        parameter(NAME_QUESTION, question);

        final List<Content<?>> nonTextContents = message.contents()
                .stream()
                .filter(v -> v.type() != Content.Type.TEXT)
                .collect(Collectors.toList());
        parameter(NAME_ATTACHMENTS, JacksonUtils.toJson(nonTextContents));

        return this;
    }

}
