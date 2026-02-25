open module dashscope4j.agent {

    requires transitive dashscope4j.client;
    requires org.slf4j;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

}