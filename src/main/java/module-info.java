module dashscope4j {

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.slf4j;

    opens io.github.ompc.dashscope4j.internal.task to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.internal.algo to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.internal.api to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.internal.chat to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.internal.image.generation to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.chat to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.chat.message to com.fasterxml.jackson.databind;
    opens io.github.ompc.dashscope4j.image.generation to com.fasterxml.jackson.databind;


    exports io.github.ompc.dashscope4j;
    exports io.github.ompc.dashscope4j.util;
    exports io.github.ompc.dashscope4j.chat;
    exports io.github.ompc.dashscope4j.chat.message;
    exports io.github.ompc.dashscope4j.image.generation;



}