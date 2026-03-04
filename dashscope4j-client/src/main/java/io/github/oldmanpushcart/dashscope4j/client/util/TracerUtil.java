package io.github.oldmanpushcart.dashscope4j.client.util;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.instrumentation.reactor.v3_1.ReactorAsyncOperationEndStrategy;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class TracerUtil {

    private static final Tracer tracer;

    static {
        final var exporter = new LoggingSpanExporter();
        final var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();

        final var openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        tracer = openTelemetry.getTracer("dashscope4j-client");

        ReactorAsyncOperationEndStrategy.builder()
                .build();

    }


}
