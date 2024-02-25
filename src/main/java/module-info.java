module dashscope4j {

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.slf4j;


    opens io.github.ompc.internal.dashscope4j.task to com.fasterxml.jackson.databind;
    opens io.github.ompc.internal.dashscope4j.algo to com.fasterxml.jackson.databind;
    opens io.github.ompc.internal.dashscope4j.api to com.fasterxml.jackson.databind;
    opens io.github.ompc.internal.dashscope4j.chat to com.fasterxml.jackson.databind;
    opens io.github.ompc.internal.dashscope4j.image.generation to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.task to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.chat.message to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.image.generation to com.fasterxml.jackson.databind;

    exports io.github.ompc.dashscope4j;
    exports io.github.ompc.dashscope4j.algo;
    exports io.github.ompc.dashscope4j.api;
    exports io.github.ompc.dashscope4j.task;
    exports io.github.ompc.dashscope4j.chat;
    exports io.github.ompc.dashscope4j.chat.message;
    exports io.github.ompc.dashscope4j.image.generation;
    exports io.github.ompc.dashscope4j.util;


}