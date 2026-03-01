package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * QWEN-ASR 实时语音识别数据发送器
 */
public interface QwenAsrRealtimeEmitter extends Realtime.Emitter<ClientEvent> {

    /**
     * @return 会话
     */
    QwenAsrRealtimeSession session();

    /**
     * 手动 vad
     */
    interface ManualVad extends QwenAsrRealtimeEmitter {

        /**
         * 创建一个输入操作
         * <p>同一时刻，一个连接只能有一个输入操作，直到输入操作完成。</p>
         *
         * @return 输入操作
         */
        InputOp newInput();

        /**
         * 输入操作
         */
        interface InputOp {

            /**
             * 添加音频数据
             *
             * @param buffer 音频数据
             * @return this
             */
            InputOp audio(ByteBuffer buffer);

            /**
             * 批量添加音频数据
             *
             * @param buffers 音频数据
             * @return this
             */
            default InputOp audio(List<ByteBuffer> buffers) {
                buffers.forEach(this::audio);
                return this;
            }

            /**
             * 提交音频数据
             *
             * @return 完成提交
             */
            CompletionStage<ManualVad> commit();

        }

    }

    /**
     * 服务端 vad
     */
    interface ServerVad extends QwenAsrRealtimeEmitter {

        /**
         * 添加音频数据
         *
         * @param buffer 音频数据
         * @return this
         */
        ServerVad audio(ByteBuffer buffer);

        /**
         * 批量添加音频数据
         *
         * @param buffers 音频数据
         * @return this
         */
        default ServerVad audio(List<ByteBuffer> buffers) {
            buffers.forEach(this::audio);
            return this;
        }

    }

}
