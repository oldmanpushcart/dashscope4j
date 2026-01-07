package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.openai;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

public interface OpenAiChatParameterKeys {

    Parameters.SimpleParameterKey<Boolean> ENABLE_STREAM = new Parameters.SimpleParameterKey<>("stream", Boolean.class);

}
