package io.github.oldmanpushcart.internal.dashscope4j.base.openai;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.internal.dashscope4j.util.StringUtils;

import java.util.Optional;

/**
 * OpenAi 格式的响应
 *
 * @param <D> 输出
 */
public interface OpenAiResponse<D> extends HttpApiResponse<D> {

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
                        .filter(StringUtils::isNotBlank)
                        .orElse(Ret.EMPTY_CODE),
                error.message()
        );
    }

    @Override
    default Usage usage() {
        return Usage.empty();
    }

    Error error();

}
