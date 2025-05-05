package io.github.oldmanpushcart.dashscope4j.client.internal.base;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.util.Objects;

public abstract class OpenAiResponse<D> extends ApiResponse<D> {

    protected OpenAiResponse(OpenAiRequest<?> request, String uuid, OpenAiError error) {
        super(
                request,
                uuid,
                parseCode(error),
                parseMessage(error)
        );
    }

    private static String parseCode(OpenAiError error) {
        if (Objects.isNull(error)) {
            return CODE_SUCCESS;
        }
        if (Objects.nonNull(error.code())) {
            return error.code();
        }
        if (Objects.nonNull(error.type())) {
            return error.type();
        }
        return CODE_FAILURE;
    }

    private static String parseMessage(OpenAiError error) {
        if (Objects.isNull(error)) {
            return "SUCCESS";
        }
        return error.message();
    }

}
