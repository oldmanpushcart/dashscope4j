package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.util.concurrent.CompletionStage;

/**
 * OpenTelemetry 上下文信息拦截器
 * <p>
 * 在当前 Span 上补充请求信息，便于问题排查和分析
 * </p>
 */
public class OpenTelemetryContextInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 跳过非 AigcRequest 的请求
        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)) {
            return chain.proceed();
        }

        // 从当前 Context 获取 Span 并添加属性
        final var span = Span.fromContext(Context.current());
        if (span.getSpanContext().isValid()) {
            span.setAttribute("aigc.model.name", aigcRequest.model().name())
                    .setAttribute("aigc.request.type", chain.type().name());
        }

        // 继续执行请求
        return chain.proceed();
    }

}