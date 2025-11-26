open module dashscope4j.client {

    requires static jakarta.validation;

    requires transitive dashscope4j.common;

    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.module.jsonSchema.jakarta;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires java.desktop;

}