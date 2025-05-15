open module dashscope4j.agent {

    requires static lombok;

    requires dashscope4j.client;

    exports io.github.oldmanpushcart.dashscope4j.agent;
    exports io.github.oldmanpushcart.dashscope4j.agent.function;
    exports io.github.oldmanpushcart.dashscope4j.agent.plugin;
    exports io.github.oldmanpushcart.dashscope4j.agent.plugin.memory;
    exports io.github.oldmanpushcart.dashscope4j.agent.prompt;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.react;
    exports io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

}