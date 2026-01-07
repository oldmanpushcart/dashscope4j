package io.github.oldmanpushcart.dashscope4j.client.internal.api;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;

public class OpenAiResponse extends ApiResponse {

    protected OpenAiResponse(OpenAiRequest<?> request, String uuid, OpenAiError error) {
        super(request, uuid, parseErrorCode(error), parseErrorDesc(error));
    }

    private static String parseErrorCode(OpenAiError error) {
        if (null == error) {
            return Ret.CODE_SUCCESS;
        }
        if (null == error.code()) {
            return Ret.CODE_FAILURE;
        }
        return error.code();
    }

    private static String parseErrorDesc(OpenAiError error) {
        if (null == error) {
            return Ret.CODE_SUCCESS;
        }
        if (null == error.message()) {
            return Ret.CODE_FAILURE;
        }
        return error.message();
    }

}
