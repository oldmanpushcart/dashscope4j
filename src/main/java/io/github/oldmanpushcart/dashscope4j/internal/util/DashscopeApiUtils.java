package io.github.oldmanpushcart.dashscope4j.internal.util;

import io.github.oldmanpushcart.dashscope4j.api.ApiException;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils.unwrapEx;

public class DashscopeApiUtils {

    public static boolean isCauseByResourceNotExisted(Throwable ex) {
        final Throwable cause = unwrapEx(ex);
        if (cause instanceof ApiException) {
            final ApiException apiEx = (ApiException) cause;
            return apiEx.status() == 400
                   && "BadRequest.ResourceNotExist".equals(apiEx.code());
        }
        return false;
    }

}
