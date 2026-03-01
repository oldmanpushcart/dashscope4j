package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.REALTIME_PATH;

/**
 * QWEN-ASR 实时语音识别模型
 *
 * @param name 名称
 * @param path 路径
 */
public record QwenAsrRealtimeModel(String name, String path) implements Model<ClientEvent, ServerEvent> {

    /**
     * QWEN3_ASR_FLASH_REALTIME
     */
    public static final QwenAsrRealtimeModel QWEN3_ASR_FLASH_REALTIME = new QwenAsrRealtimeModel("qwen3-asr-flash-realtime");

    public QwenAsrRealtimeModel(String name) {
        this(name, String.format("%s?model=%s".formatted(REALTIME_PATH, name)));
    }

}
