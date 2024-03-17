open module dashscope4j {

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.slf4j;

    requires transitive com.fasterxml.jackson.module.jsonSchema;

    exports io.github.oldmanpushcart.dashscope4j;
    exports io.github.oldmanpushcart.dashscope4j.base.algo;
    exports io.github.oldmanpushcart.dashscope4j.base.api;
    exports io.github.oldmanpushcart.dashscope4j.base.task;
    exports io.github.oldmanpushcart.dashscope4j.chat;
    exports io.github.oldmanpushcart.dashscope4j.chat.message;
    exports io.github.oldmanpushcart.dashscope4j.chat.function;
    exports io.github.oldmanpushcart.dashscope4j.image.generation;
    exports io.github.oldmanpushcart.dashscope4j.embedding;
    exports io.github.oldmanpushcart.dashscope4j.util;


}