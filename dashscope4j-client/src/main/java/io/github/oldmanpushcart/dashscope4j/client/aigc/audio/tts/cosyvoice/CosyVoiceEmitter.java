package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.util.concurrent.CompletionStage;

public class CosyVoiceEmitter
        extends Realtime.DelegateEmitter<CosyVoiceModel.In>
        implements Realtime.Emitter<CosyVoiceModel.In> {

    CosyVoiceEmitter(Realtime.Emitter<CosyVoiceModel.In> delegate) {
        super(delegate);
    }

    public CompletionStage<Void> text(String text) {
        return data(CosyVoiceModel.In.of(text));
    }

}
