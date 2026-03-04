open module dashscope4j.client {

    requires transitive dashscope4j.common;

    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.jackson.core;
    requires com.github.victools.jsonschema.generator;
    requires com.github.victools.jsonschema.module.jackson;
    requires com.github.victools.jsonschema.module.jakarta.validation;
    requires okhttp3;
    requires org.reactivestreams;
    requires org.jspecify;
    requires reactor.core;
    requires okhttp3.sse;
    requires io.opentelemetry.api;
    requires io.opentelemetry.exporter.logging;
    requires io.opentelemetry.sdk.trace;
    requires io.opentelemetry.sdk;
    requires io.opentelemetry.instrumentation.reactor_3_1;

}