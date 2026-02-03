package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.util.concurrent.CompletionStage;

public interface QwenTtsRealtimeEmitter extends Realtime.Emitter<QwenTtsRealtimeClientEvent> {

    QwenTtsRealtimeSession session();

    interface ServerVad extends QwenTtsRealtimeEmitter {

        CompletionStage<Void> text(String text);
    }

    interface ManualVad extends QwenTtsRealtimeEmitter {

        CompletionStage<InputOp> newInput();

        interface InputOp {

            CompletionStage<InputOp> text(String text);

            CompletionStage<InputOp> clear();

            CompletionStage<ManualVad> commit();

        }

    }

}
