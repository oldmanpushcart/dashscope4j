package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface QwenAsrRealtimeEmitter extends Realtime.Emitter<ClientEvent> {

    QwenAsrRealtimeSession session();

    interface ManualVad extends QwenAsrRealtimeEmitter {

        CompletionStage<InputOp> newInput();

        interface InputOp {

            CompletionStage<InputOp> audio(ByteBuffer buffer);

            default CompletionStage<InputOp> audio(List<ByteBuffer> buffers) {
                return CompletableFutureUtils
                        .sequentialMap(buffers, this::audio)
                        .thenApply(unused -> this);
            }

            CompletionStage<ManualVad> commit();

        }

    }

    interface ServerVad extends QwenAsrRealtimeEmitter {

        CompletionStage<Void> audio(ByteBuffer buffer);

        default CompletionStage<Void> audio(List<ByteBuffer> buffers) {
            return CompletableFutureUtils
                    .sequentialMap(buffers, this::audio)
                    .thenAccept(unused -> {
                    });
        }

    }

}
