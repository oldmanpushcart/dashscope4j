package io.github.oldmanpushcart.dashscope4j.client.internal;


public interface InternalContents {

    /**
     * 启用
     */
    String ENABLE = "enable";

    /**
     * 禁用
     */
    String DISABLE = "disable";

    int WEBSOCKET_CLOSE_REASON_MAX_LENGTH = 123;

    String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    String HTTP_HEADER_AUTHORIZATION = "Authorization";

    String HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE = "X-DashScope-OssResourceResolve";

    String HTTP_HEADER_X_DASHSCOPE_PLUGIN = "X-DashScope-Plugin";

    String HTTP_HEADER_X_DASHSCOPE_CLIENT = "X-DashScope-Client";

    String HTTP_HEADER_X_DASHSCOPE_SSE = "X-DashScope-SSE";

    String HTTP_HEADER_X_DASHSCOPE_ASYNC = "X-DashScope-Async";

}
