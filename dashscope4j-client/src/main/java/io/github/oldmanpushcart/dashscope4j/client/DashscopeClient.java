package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;

public interface DashscopeClient {

    ChatOp chat();

    OmniOp omni();

}
