package io.github.oldmanpushcart.dashscope4j.internal;

import okhttp3.MediaType;

/**
 * 内部常量
 */
public interface InternalContents {

    String CACHE_NAMESPACE_STORE = "store";
    String CACHE_NAMESPACE_FILES = "files";

    String ENABLE = "enable";

    int WEBSOCKET_CLOSE_REASON_MAX_LENGTH = 123;

    MediaType MT_APPLICATION_JSON = MediaType.get("application/json");

    MediaType MT_APPLICATION_OCTET_STREAM = MediaType.get("application/octet-stream");

    String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    String HTTP_HEADER_AUTHORIZATION = "Authorization";

    String HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE = "X-DashScope-OssResourceResolve";

    String HTTP_HEADER_X_DASHSCOPE_PLUGIN = "X-DashScope-Plugin";

    String HTTP_HEADER_X_DASHSCOPE_CLIENT = "X-DashScope-Client";

    String HTTP_HEADER_X_DASHSCOPE_SSE = "X-DashScope-SSE";

    String HTTP_HEADER_X_DASHSCOPE_ASYNC = "X-DashScope-Async";
}
