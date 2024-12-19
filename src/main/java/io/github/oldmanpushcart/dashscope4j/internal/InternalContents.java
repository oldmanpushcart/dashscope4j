package io.github.oldmanpushcart.dashscope4j.internal;

import okhttp3.MediaType;

/**
 * 内部常量
 */
public interface InternalContents {

    int WEBSOCKET_CLOSE_REASON_MAX_LENGTH = 123;

    MediaType MT_APPLICATION_JSON = MediaType.get("application/json");
}
