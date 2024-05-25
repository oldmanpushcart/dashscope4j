package io.github.oldmanpushcart.internal.dashscope4j.base.openai;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.util.Optional;

public interface OpenAiResponse<D extends OpenAiResponse.Output> extends ApiResponse<D> {

    @Override
    default String uuid() {
        return EMPTY_UUID;
    }

    @Override
    default Ret ret() {
        final var error = error();
        if (null == error) {
            return Ret.ofSuccess("success");
        }
        return new Ret(
                Optional.ofNullable(error.code())
                        .filter(CommonUtils::isNotBlankString)
                        .orElse(Ret.EMPTY_CODE),
                error.message()
        );
    }

    @Override
    default Usage usage() {
        return Usage.empty();
    }

    Error error();

    interface Output extends ApiResponse.Output {

    }

}
