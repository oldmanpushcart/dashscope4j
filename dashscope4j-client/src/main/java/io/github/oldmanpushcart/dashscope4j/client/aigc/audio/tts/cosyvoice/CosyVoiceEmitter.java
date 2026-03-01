package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.util.List;

public class CosyVoiceEmitter
        extends Realtime.DelegateEmitter<CosyVoiceModel.In>
        implements Realtime.Emitter<CosyVoiceModel.In> {

    CosyVoiceEmitter(Realtime.Emitter<CosyVoiceModel.In> delegate) {
        super(delegate);
    }

    public CosyVoiceEmitter text(String text) {
        data(CosyVoiceModel.In.of(text));
        return this;
    }

    public CosyVoiceEmitter text(List<String> texts) {
        texts.forEach(this::text);
        return this;
    }

}
