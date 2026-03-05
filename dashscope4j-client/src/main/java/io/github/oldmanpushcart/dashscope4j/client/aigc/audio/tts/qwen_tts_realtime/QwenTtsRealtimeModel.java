package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import static io.github.oldmanpushcart.dashscope4j.client.Constants.REALTIME_PATH;

public record QwenTtsRealtimeModel(
        String name,
        String path
) implements Model<ClientEvent, ServerEvent> {

    public static final QwenTtsRealtimeModel QWEN3_TTS_FLASH_REALTIME = new QwenTtsRealtimeModel("qwen3-tts-flash-realtime");
    public static final QwenTtsRealtimeModel QWEN3_TTS_REALTIME = new  QwenTtsRealtimeModel("qwen3-tts-realtime");

    public QwenTtsRealtimeModel(String name) {
        this(name, String.format("%s?model=%s".formatted(REALTIME_PATH, name)));
    }

}
