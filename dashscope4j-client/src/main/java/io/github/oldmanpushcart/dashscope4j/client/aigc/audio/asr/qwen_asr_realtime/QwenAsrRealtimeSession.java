package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler.ManualVadHandler;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler.ServerVadHandler;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler.SessionHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.HandlerChain;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.DurationMsJsonDeserializer;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.DurationMsJsonSerializer;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.time.Duration;
import java.util.function.Function;

/**
 * QWEN-ASR 实时语音识别会话
 * <p>
 * 配置参考：<a href="https://bailian.console.aliyun.com/cn-beijing/?tab=api#/api/?type=model&url=2987033">实时语音识别（Qwen-ASR）客户端事件</a>
 * </p>
 */
public record QwenAsrRealtimeSession(

        @JsonProperty("id")
        String id,

        QwenAsrRealtimeModel model,

        @JsonProperty("sample_rate")
        Integer sampleRate,

        @JsonProperty("audio_format")
        InputAudioFormat inputAudioFormat,

        @JsonProperty("input_audio_transcription")
        InputAudioTranscription inputAudioTranscription,

        @JsonProperty("turn_detection")
        TurnDetection turnDetection

) implements Realtime.Session<ClientEvent, ServerEvent> {

    private QwenAsrRealtimeSession(Builder builder) {
        this(
                null,
                builder.model,
                builder.sampleRate,
                builder.inputAudioFormat,
                builder.inputAudioTranscription,
                builder.turnDetection
        );
    }

    @Override
    public QwenAsrRealtimeModel model() {
        return model;
    }

    @Override
    public Function<Realtime.Handler<ClientEvent, ServerEvent>, Realtime.Handler<String, String>> provider() {
        return ioHandler -> HandlerChain
                .<String, String>identity()
                .<ClientEvent, ServerEvent>map(JacksonJsonUtils::toJson, payload -> JacksonJsonUtils.toObject(payload, ServerEvent.class))
                .<ClientEvent, ServerEvent>then(h -> new SessionHandshakeHandler(this, newHandler(h)))
                .build(ioHandler);
    }

    private Realtime.Handler<ClientEvent, ServerEvent> newHandler(Realtime.Handler<ClientEvent, ServerEvent> handler) {
        if (turnDetection == null || turnDetection.type() == TurnDetection.Type.SERVER_VAD) {
            return new ServerVadHandler(handler);
        } else {
            return new ManualVadHandler(handler);
        }
    }

    /**
     * 输入音频格式
     */
    public enum InputAudioFormat {

        @JsonProperty("pcm")
        PCM,

        @JsonProperty("opus")
        OPUS

    }

    /**
     * 输入音频转录配置
     *
     * @param language 输入语言
     */
    public record InputAudioTranscription(

            @JsonProperty("language")
            String language

    ) {

    }

    /**
     * 会话轮检测配置
     *
     * @param type      检测方式
     * @param threshold 检测阈值
     * @param silence   断句检测阈值（ms）
     */
    public record TurnDetection(

            @JsonProperty("type")
            Type type,

            @JsonProperty("threshold")
            Float threshold,

            @JsonProperty("silence_duration_ms")
            @JsonSerialize(using = DurationMsJsonSerializer.class)
            @JsonDeserialize(using = DurationMsJsonDeserializer.class)
            Duration silence

    ) {

        /**
         * 服务端自动检测
         */
        public static final TurnDetection SERVER_VAD = new TurnDetection(
                Type.SERVER_VAD,
                null,
                null
        );

        /**
         * 手动提交
         */
        public static final TurnDetection MANUAL_VAD = new TurnDetection(
                Type.MANUAL_VAD,
                null,
                null
        );

        /**
         * 检测方式
         */
        public enum Type {

            @JsonProperty("server_vad")
            SERVER_VAD,

            @JsonProperty("manual_vad")
            MANUAL_VAD

        }

    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 创建构建器
     *
     * @param session 会话
     * @return 构建器
     */
    public static Builder newBuilder(QwenAsrRealtimeSession session) {
        return new Builder(session);
    }

    /**
     * 构建器
     */
    public static class Builder implements Buildable<QwenAsrRealtimeSession, Builder> {

        private QwenAsrRealtimeModel model;
        private Integer sampleRate;
        private InputAudioFormat inputAudioFormat;
        private InputAudioTranscription inputAudioTranscription;
        private TurnDetection turnDetection;

        public Builder() {

        }

        public Builder(QwenAsrRealtimeSession session) {
            this.model = session.model;
            this.sampleRate = session.sampleRate;
            this.inputAudioFormat = session.inputAudioFormat;
            this.inputAudioTranscription = session.inputAudioTranscription;
            this.turnDetection = session.turnDetection;
        }

        /**
         * 设置模型
         *
         * @param model 模型
         * @return this
         */
        public Builder model(QwenAsrRealtimeModel model) {
            this.model = model;
            return this;
        }

        /**
         * 设置采样率
         *
         * @param sampleRate 采样率
         * @return this
         */
        public Builder sampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        /**
         * 设置输入音频格式
         *
         * @param inputAudioFormat 输入音频格式
         * @return this
         */
        public Builder inputAudioFormat(InputAudioFormat inputAudioFormat) {
            this.inputAudioFormat = inputAudioFormat;
            return this;
        }

        /**
         * 设置输入音频转录配置
         *
         * @param inputAudioTranscription 输入音频转录配置
         * @return this
         */
        public Builder inputAudioTranscription(InputAudioTranscription inputAudioTranscription) {
            this.inputAudioTranscription = inputAudioTranscription;
            return this;
        }

        /**
         * 设置会话轮检测配置
         *
         * @param turnDetection 会话轮检测配置
         * @return this
         */
        public Builder turnDetection(TurnDetection turnDetection) {
            this.turnDetection = turnDetection;
            return this;
        }

        @Override
        public QwenAsrRealtimeSession build() {
            return new QwenAsrRealtimeSession(this);
        }

    }

}
