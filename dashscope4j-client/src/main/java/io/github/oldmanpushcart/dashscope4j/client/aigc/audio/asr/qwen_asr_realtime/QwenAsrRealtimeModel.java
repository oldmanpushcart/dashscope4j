package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

public record QwenAsrRealtimeModel(String name, String path) implements Model<ClientEvent, ServerEvent> {



}
