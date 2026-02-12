package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.List;
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

    public CompletionStage<Void> text(List<String> texts) {
        return CompletableFutureUtils
                .sequentialMap(texts, this::text)
                .thenAccept(unused -> {

                });
    }

}
