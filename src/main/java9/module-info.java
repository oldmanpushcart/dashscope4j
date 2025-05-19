module dashscope4j {

    requires static lombok;
    requires static jakarta.validation;

    requires org.slf4j;
    requires kotlin.stdlib;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.module.jsonSchema.jakarta;
    requires com.fasterxml.jackson.dataformat.xml;
    requires transitive io.reactivex.rxjava3;
    requires okhttp3;
    requires okhttp3.sse;
    requires okio;

    // exports
    exports io.github.oldmanpushcart.dashscope4j;
    exports io.github.oldmanpushcart.dashscope4j.util;

    // exports agent
    exports io.github.oldmanpushcart.dashscope4j.agent;
    exports io.github.oldmanpushcart.dashscope4j.agent.function;
    exports io.github.oldmanpushcart.dashscope4j.agent.component;
    exports io.github.oldmanpushcart.dashscope4j.agent.component.memory;
    exports io.github.oldmanpushcart.dashscope4j.agent.prompt;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.react;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

    // exports client
    exports io.github.oldmanpushcart.dashscope4j.client;
    exports io.github.oldmanpushcart.dashscope4j.client.api;
    exports io.github.oldmanpushcart.dashscope4j.client.api.chat;
    exports io.github.oldmanpushcart.dashscope4j.client.api.chat.message;
    exports io.github.oldmanpushcart.dashscope4j.client.api.chat.plugin;
    exports io.github.oldmanpushcart.dashscope4j.client.api.chat.tool;
    exports io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio.asr;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio.tts;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio.tts.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio.vocabulary;
    exports io.github.oldmanpushcart.dashscope4j.client.api.audio.voice;
    exports io.github.oldmanpushcart.dashscope4j.client.api.embedding;
    exports io.github.oldmanpushcart.dashscope4j.client.api.embedding.text;
    exports io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm;
    exports io.github.oldmanpushcart.dashscope4j.client.api.image;
    exports io.github.oldmanpushcart.dashscope4j.client.api.image.generation;
    exports io.github.oldmanpushcart.dashscope4j.client.api.video;
    exports io.github.oldmanpushcart.dashscope4j.client.api.video.generation;
    exports io.github.oldmanpushcart.dashscope4j.client.base;
    exports io.github.oldmanpushcart.dashscope4j.client.base.tokenizer;
    exports io.github.oldmanpushcart.dashscope4j.client.base.store;
    exports io.github.oldmanpushcart.dashscope4j.client.base.files;
    exports io.github.oldmanpushcart.dashscope4j.client.task;

}