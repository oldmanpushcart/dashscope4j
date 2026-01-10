open module dashscope4j.client {

    requires transitive dashscope4j.common;

    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires java.desktop;
    requires com.github.victools.jsonschema.generator;
    requires com.github.victools.jsonschema.module.jackson;
    requires com.github.victools.jsonschema.module.jakarta.validation;

}