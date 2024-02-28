module dashscope4j {

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.slf4j;

    opens io.github.oldmanpushcart.internal.dashscope4j.base.task to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.internal.dashscope4j.base.algo to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.internal.dashscope4j.base.api to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.internal.dashscope4j.chat to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.internal.dashscope4j.chat.message to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.internal.dashscope4j.image.generation to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.internal.dashscope4j.embedding to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.dashscope4j to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.dashscope4j.base.task to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.dashscope4j.chat.message to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.dashscope4j.image.generation to com.fasterxml.jackson.databind;
    opens io.github.oldmanpushcart.dashscope4j.embedding to com.fasterxml.jackson.databind;

    exports io.github.oldmanpushcart.dashscope4j;
    exports io.github.oldmanpushcart.dashscope4j.base.algo;
    exports io.github.oldmanpushcart.dashscope4j.base.api;
    exports io.github.oldmanpushcart.dashscope4j.base.task;
    exports io.github.oldmanpushcart.dashscope4j.chat;
    exports io.github.oldmanpushcart.dashscope4j.chat.message;
    exports io.github.oldmanpushcart.dashscope4j.image.generation;
    exports io.github.oldmanpushcart.dashscope4j.embedding;
    exports io.github.oldmanpushcart.dashscope4j.util;


}