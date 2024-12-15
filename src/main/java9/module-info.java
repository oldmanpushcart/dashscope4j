open module dashscope4j {
    requires static lombok;
    requires static annotations;

    requires org.slf4j;
    requires kotlin.stdlib;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.annotation;
    requires transitive com.fasterxml.jackson.module.jsonSchema;
    requires transitive io.reactivex.rxjava3;
    requires okhttp3;
    requires okhttp3.sse;
    requires okio;

    exports io.github.oldmanpushcart.dashscope4j;
    exports io.github.oldmanpushcart.dashscope4j.util;
    exports io.github.oldmanpushcart.dashscope4j.api;
    exports io.github.oldmanpushcart.dashscope4j.api.chat;
    exports io.github.oldmanpushcart.dashscope4j.api.chat.message;
    exports io.github.oldmanpushcart.dashscope4j.api.chat.plugin;
    exports io.github.oldmanpushcart.dashscope4j.api.chat.tool;
    exports io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;
    exports io.github.oldmanpushcart.dashscope4j.api.audio;
    exports io.github.oldmanpushcart.dashscope4j.api.audio.asr;
    exports io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan;
    exports io.github.oldmanpushcart.dashscope4j.api.audio.tts;
    exports io.github.oldmanpushcart.dashscope4j.api.audio.tts.timespan;
    exports io.github.oldmanpushcart.dashscope4j.base;
    exports io.github.oldmanpushcart.dashscope4j.base.tokenizer;

}