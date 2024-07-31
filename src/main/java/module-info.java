open module dashscope4j {

    requires java.net.http;
    requires org.slf4j;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.module.jsonSchema;

    exports io.github.oldmanpushcart.dashscope4j;
    exports io.github.oldmanpushcart.dashscope4j.base;
    exports io.github.oldmanpushcart.dashscope4j.base.algo;
    exports io.github.oldmanpushcart.dashscope4j.base.api;
    exports io.github.oldmanpushcart.dashscope4j.base.task;
    exports io.github.oldmanpushcart.dashscope4j.base.files;
    exports io.github.oldmanpushcart.dashscope4j.base.store;
    exports io.github.oldmanpushcart.dashscope4j.base.cache;
    exports io.github.oldmanpushcart.dashscope4j.base.interceptor;
    exports io.github.oldmanpushcart.dashscope4j.base.tokenizer;
    exports io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit;
    exports io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process;
    exports io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.retry;

    exports io.github.oldmanpushcart.dashscope4j.chat;
    exports io.github.oldmanpushcart.dashscope4j.chat.message;
    exports io.github.oldmanpushcart.dashscope4j.chat.tool;
    exports io.github.oldmanpushcart.dashscope4j.chat.tool.function;
    exports io.github.oldmanpushcart.dashscope4j.chat.plugin;

    exports io.github.oldmanpushcart.dashscope4j.image;
    exports io.github.oldmanpushcart.dashscope4j.image.generation;

    exports io.github.oldmanpushcart.dashscope4j.embedding;
    exports io.github.oldmanpushcart.dashscope4j.embedding.text;
    exports io.github.oldmanpushcart.dashscope4j.embedding.mm;

    exports io.github.oldmanpushcart.dashscope4j.util;

}