open module dashscope4j.agent {

    requires transitive dashscope4j.client;
    requires static lombok;

    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires io.modelcontextprotocol.sdk.mcp;
    requires reactor.core;
    requires java.net.http;

    exports io.github.oldmanpushcart.dashscope4j.agent;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.react;
    exports io.github.oldmanpushcart.dashscope4j.agent.prompt;
    exports io.github.oldmanpushcart.dashscope4j.agent.function;
    exports io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;
    exports io.github.oldmanpushcart.dashscope4j.agent.component;
    exports io.github.oldmanpushcart.dashscope4j.agent.component.memory;

}