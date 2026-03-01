package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.util.concurrent.CompletionStage;

public interface QwenTtsRealtimeEmitter extends Realtime.Emitter<ClientEvent> {

    QwenTtsRealtimeSession session();

    interface ServerVad extends QwenTtsRealtimeEmitter {

        void text(String text);
    }

    interface ManualVad extends QwenTtsRealtimeEmitter {

        InputOp newInput();

        interface InputOp {

            InputOp text(String text);

            CompletionStage<InputOp> clear();

            CompletionStage<ManualVad> commit();

        }

    }

}
