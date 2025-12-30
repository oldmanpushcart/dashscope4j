package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.openai.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenAiContent<T>() {


    public enum Type {

        @JsonProperty("text")
        TEXT,

        /**
         * 图像
         */
        @JsonProperty("image_url")
        IMAGE,

        /**
         * 音频
         */
        @JsonProperty("input_audio")
        AUDIO,

        /**
         * 视频
         */
        @JsonProperty("video_url")
        VIDEO,

        /**
         * 文件
         */
        @JsonProperty("file")
        FILE

    }

}
