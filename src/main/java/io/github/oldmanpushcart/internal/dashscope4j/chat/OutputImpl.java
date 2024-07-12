package io.github.oldmanpushcart.internal.dashscope4j.chat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;

import java.util.List;
import java.util.Optional;

@JsonDeserialize(using = OutputImplJsonDeserializer.class)
record OutputImpl(List<ChatResponse.Choice> choices) implements ChatResponse.Output {

    public OutputImpl(ChatResponse.Choice choice) {
        this(List.of(choice));
    }

    @Override
    public ChatResponse.Choice best() {
        return Optional.ofNullable(choices)
                .flatMap(choices -> choices.stream().sorted().findFirst())
                .orElse(null);
    }

}
