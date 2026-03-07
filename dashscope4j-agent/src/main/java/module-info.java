open module dashscope4j.agent {
    requires dashscope4j.client;
    requires org.reactivestreams;
    requires org.slf4j;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires reactor.core;
    requires okhttp3;
    requires java.net.http;
}