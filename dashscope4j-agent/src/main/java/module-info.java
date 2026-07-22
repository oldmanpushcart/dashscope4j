open module dashscope4j.agent {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires dashscope4j.client;
    requires okhttp3;
    requires okio;
    requires org.reactivestreams;
    requires org.slf4j;
    requires reactor.core;
    requires mcp;
}