open module dashscope4j.client {

    requires transitive dashscope4j.common;

    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires com.github.victools.jsonschema.generator;
    requires com.github.victools.jsonschema.module.jackson;
    requires com.github.victools.jsonschema.module.jakarta.validation;

    // Export main API packages
    exports io.github.oldmanpushcart.dashscope4j.client;

    exports io.github.oldmanpushcart.dashscope4j.client.api;
    exports io.github.oldmanpushcart.dashscope4j.client.api.interceptor;
    exports io.github.oldmanpushcart.dashscope4j.client.api.realtime;
    exports io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;
    exports io.github.oldmanpushcart.dashscope4j.client.api.task;

    exports io.github.oldmanpushcart.dashscope4j.client.base;
    exports io.github.oldmanpushcart.dashscope4j.client.base.files;
    exports io.github.oldmanpushcart.dashscope4j.client.base.store;
    exports io.github.oldmanpushcart.dashscope4j.client.base.tokenizer;

    exports io.github.oldmanpushcart.dashscope4j.client.util;
    exports io.github.oldmanpushcart.dashscope4j.client.util.tracer;

    exports io.github.oldmanpushcart.dashscope4j.client.aigc.chat;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.handler;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan;
    exports io.github.oldmanpushcart.dashscope4j.client.aigc.embedding;


}