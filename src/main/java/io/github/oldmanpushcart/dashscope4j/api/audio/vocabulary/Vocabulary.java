package io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * 热词表
 *
 * @since 3.1.0
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class Vocabulary {

    String identity;
    String target;
    Instant createdAt;
    Instant updatedAt;
    List<Item> items;

    /**
     * 热词
     */
    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    public static class Item {

        /**
         * 文本
         */
        @JsonProperty("text")
        String text;

        /**
         * 语言代码
         */
        @JsonProperty("lang")
        String lang;

        /**
         * 权重
         */
        @JsonProperty("weight")
        int weight;

        public static Item of(String text) {
            return builder()
                    .text(text)
                    .lang("zh")
                    .weight(0)
                    .build();
        }

        public static Item of(String text, String lang) {
            return builder()
                    .text(text)
                    .lang(lang)
                    .weight(1)
                    .build();
        }

        public static Item of(String text, String lang, int weight) {
            return builder()
                    .text(text)
                    .lang(lang)
                    .weight(weight)
                    .build();
        }

    }

}
