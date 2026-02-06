package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.timespan;

public record Range(int begin, int end) {

    public boolean hasEnd() {
        return end > 0;
    }

    public static Range of(int begin, Integer end) {
        return new Range(
                begin,
                null == end ? -1 : end
        );
    }

}
