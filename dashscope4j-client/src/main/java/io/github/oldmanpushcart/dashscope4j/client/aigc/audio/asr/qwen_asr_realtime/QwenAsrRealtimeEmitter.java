package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public interface QwenAsrRealtimeEmitter extends Realtime.Emitter<ClientEvent> {

    QwenAsrRealtimeModel model();

    interface ManualVad extends QwenAsrRealtimeEmitter {

        CompletionStage<InputOp> newInput();

        interface InputOp {

            CompletionStage<Void> audio(ByteBuffer buffer);

            default CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
                return audio(ByteBuffer.wrap(bytes, offset, length));
            }

            default CompletionStage<Void> audio(byte[] bytes) {
                return audio(ByteBuffer.wrap(bytes));
            }

            CompletionStage<Void> commit();

        }

    }

    interface ServerVad extends QwenAsrRealtimeEmitter {

        CompletionStage<Void> audio(ByteBuffer buffer);

        default CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
            return audio(ByteBuffer.wrap(bytes, offset, length));
        }

        default CompletionStage<Void> audio(byte[] bytes) {
            return audio(ByteBuffer.wrap(bytes));
        }

    }

}
