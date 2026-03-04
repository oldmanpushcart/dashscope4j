package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 可追踪的 Dashscope 客户端装饰器
 * <p>
 * 基于 OpenTelemetry 实现埋点功能，仅依赖最轻量的 opentelemetry-api。
 * </p>
 * <p>
 * 设计模式：装饰器模式（Decorator Pattern）
 * - 委托给实际的 DashscopeClient 执行真实操作
 * - 在执行前后添加追踪逻辑
 * </p>
 *
 * @author dashscope4j
 */
public class TraceableDashscopeClient implements DashscopeClient {

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("dashscope4j");

    /**
     * 被装饰的实际客户端
     */
    private final DashscopeClient delegate;

    /**
     * 构造可追踪客户端
     *
     * @param delegate 实际客户端实例
     */
    public TraceableDashscopeClient(DashscopeClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<Interceptor> interceptors) {
        return traceAsync("async", (v) -> delegate.async(request, interceptors));
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> flow(T request, List<Interceptor> interceptors) {
        return traceFlow("flow", (v) -> delegate.flow(request, interceptors));
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<Interceptor> interceptors) {
        return traceTask("task", (v) -> delegate.task(request, interceptors));
    }

    @Override
    public <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler) {
        final var span = tracer.spanBuilder("realtime")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        
        try (Scope ignored = span.makeCurrent()) {
            return delegate.realtime(session, wrapHandler(handler, span));
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, "Failed to execute realtime");
            span.recordException(e);
            span.end();
            throw new RuntimeException(e);
        }
    }

    /**
     * 包装 Handler，添加埋点逻辑
     * <p>
     * 在 Handler 的 onClosed() 回调中结束 Span，这是最可靠的方式：
     * - onClosed() 是 WebSocket 连接关闭的标准回调
     * - 无论是正常关闭、异常关闭还是主动关闭，都会触发此回调
     * - 不需要额外的监听机制
     * </p>
     *
     * @param original 原始 Handler
     * @param span     追踪 Span
     * @return 包装后的 Handler
     */
    private <I, O> Realtime.Handler<I, O> wrapHandler(Realtime.Handler<I, O> original, Span span) {
        return new Realtime.Handler<I, O>() {
            @Override
            public void onOpen(Realtime.Emitter<I> emitter) {
                original.onOpen(emitter);
            }

            @Override
            public void onData(O output) {
                original.onData(output);
            }

            @Override
            public void onBinary(java.nio.ByteBuffer buffer) {
                original.onBinary(buffer);
            }

            @Override
            public void onClosed(Throwable ex) {
                original.onClosed(ex);
                // 在连接关闭时结束 Span（标准且唯一的方式）
                if (ex != null) {
                    span.setStatus(StatusCode.ERROR, "Connection closed due to error");
                    span.recordException(ex);
                    span.setAttribute("connection.close_reason", "error");
                } else {
                    span.setStatus(StatusCode.OK);
                    span.setAttribute("connection.close_reason", "normal");
                }
                span.end();
            }
        };
    }

    @Override
    public BaseOp base() {
        return delegate.base();
    }

    /**
     * 为 CompletionStage 操作添加 OpenTelemetry 埋点
     *
     * @param operationName 操作名称
     * @param supplier      操作提供者
     * @return 带追踪的 CompletionStage
     */
    private <R> CompletionStage<R> traceAsync(String operationName, Function<Void, CompletionStage<R>> supplier) {
        final var span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            return supplier.apply(null)
                    .whenComplete((response, throwable) -> completeSpan(span, throwable));
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, "Failed to execute " + operationName);
            span.recordException(e);
            span.end();
            throw new RuntimeException(e);
        }
    }

    /**
     * 为 Publisher 操作添加 OpenTelemetry 埋点
     *
     * @param operationName 操作名称
     * @param supplier      操作提供者
     * @return 带追踪的 Publisher
     */
    private <R> Publisher<R> traceFlow(String operationName, Function<Void, Publisher<R>> supplier) {
        final var span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            return Flux.from(supplier.apply(null))
                    .doOnComplete(() -> {
                        span.setStatus(StatusCode.OK);
                        span.end();
                    })
                    .doOnError(throwable -> {
                        span.setStatus(StatusCode.ERROR, "Error during flow execution");
                        span.recordException(throwable);
                        span.end();
                    });
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, "Failed to execute " + operationName);
            span.recordException(e);
            span.end();
            throw new RuntimeException(e);
        }
    }

    /**
     * 为 Task.Half（两阶段 CompletionStage）操作添加 OpenTelemetry 埋点
     *
     * @param operationName 操作名称
     * @param supplier      操作提供者
     * @return 带追踪的 CompletionStage
     */
    private <R> CompletionStage<? extends Task.Half<R>> traceTask(String operationName, Function<Void, CompletionStage<? extends Task.Half<R>>> supplier) {
        final var span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            return supplier.apply(null)
                    .thenApply(half -> {
                        // 包装 Half 对象，在 waitingFor 阶段也进行埋点
                        return new Task.Half<R>() {
                            @Override
                            public CompletionStage<R> waitingFor(Task.WaitStrategy strategy) {
                                return traceAsync("task.waitingFor", v -> half.waitingFor(strategy));
                            }
                        };
                    })
                    .whenComplete((half, throwable) -> completeSpan(span, throwable));
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, "Failed to execute " + operationName);
            span.recordException(e);
            span.end();
            throw new RuntimeException(e);
        }
    }

    /**
     * 完成 Span，设置状态并记录异常
     *
     * @param span      Span
     * @param throwable 异常（可选）
     */
    private void completeSpan(Span span, Throwable throwable) {
        if (throwable != null) {
            span.setStatus(StatusCode.ERROR, "Operation failed: " + throwable.getMessage());
            span.recordException(throwable);
        } else {
            span.setStatus(StatusCode.OK);
        }
        span.end();
    }

}
