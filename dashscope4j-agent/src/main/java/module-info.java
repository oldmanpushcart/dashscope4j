open module dashscope4j.agent {

    requires transitive dashscope4j.client;

    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires io.modelcontextprotocol.sdk.mcp;
    requires java.net.http;

}