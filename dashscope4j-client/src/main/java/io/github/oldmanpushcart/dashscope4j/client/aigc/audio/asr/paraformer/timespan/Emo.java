package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.timespan;

public record Emo(String tag, Float confidence) {

    public static Emo of(String tag, Float confidence) {
        return tag == null && confidence == null
                ? null
                : new Emo(tag, confidence);
    }

}
