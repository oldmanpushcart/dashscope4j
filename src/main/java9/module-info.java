open module dashscope4j {
    requires static lombok;
    requires static annotations;

    requires org.slf4j;
    requires kotlin.stdlib;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.module.jsonSchema;
    requires com.fasterxml.jackson.dataformat.xml;
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
    exports io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary;
    exports io.github.oldmanpushcart.dashscope4j.api.audio.voice;
    exports io.github.oldmanpushcart.dashscope4j.api.embedding;
    exports io.github.oldmanpushcart.dashscope4j.api.embedding.text;
    exports io.github.oldmanpushcart.dashscope4j.api.embedding.mm;
    exports io.github.oldmanpushcart.dashscope4j.api.image;
    exports io.github.oldmanpushcart.dashscope4j.api.image.generation;
    exports io.github.oldmanpushcart.dashscope4j.base;
    exports io.github.oldmanpushcart.dashscope4j.base.tokenizer;
    exports io.github.oldmanpushcart.dashscope4j.base.store;
    exports io.github.oldmanpushcart.dashscope4j.base.files;
    exports io.github.oldmanpushcart.dashscope4j.task;


}