package __PACKAGE_NAME__.observability;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.reactor.autoconfigure.ReactorAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class TracingConfigurationTest {

    @BeforeEach
    @AfterEach
    void resetAutomaticContextPropagation() {
        Hooks.disableAutomaticContextPropagation();
    }

    @Test
    void shouldPreserveLogCorrelationAcrossReactiveThreads() {
        try (var context = application().run()) {
            var tracer = context.getBean(Tracer.class);
            var observation = Observation.start("starter.reactive.test", context.getBean(ObservationRegistry.class));
            try (var scope = observation.openScope()) {
                TraceContext trace = tracer.currentSpan().context();
                Thread caller = Thread.currentThread();
                Mono.delay(Duration.ofMillis(1))
                    .doOnNext(ignored -> {
                        assertThat(Thread.currentThread()).isNotSameAs(caller);
                        assertThat(tracer.currentSpan()).isNotNull();
                        assertThat(tracer.currentSpan().context().traceId()).isEqualTo(trace.traceId());
                        assertThat(MDC.get("traceId")).isEqualTo(trace.traceId());
                        assertThat(MDC.get("spanId")).isEqualTo(trace.spanId());
                    })
                    .contextCapture()
                    .block(Duration.ofSeconds(5));
            } finally {
                observation.stop();
            }
        }
    }

    @Test
    void shouldTraceLocallyWithoutACollectorByDefault() {
        try (var context = application().run()) {
            assertThat(context.getBeansOfType(SpanExporter.class)).isEmpty();
            assertThat(context.getBeansOfType(MeterRegistry.class).values())
                .extracting(registry -> registry.getClass().getName())
                .doesNotContain("io.micrometer.registry.otlp.OtlpMeterRegistry");
            assertThat(context.getEnvironment().getProperty("management.tracing.sampling.probability"))
                .isEqualTo("0.1");
            assertCorrelatedObservation(context);
        }
    }

    @Test
    void shouldExportCorrelatedSpansWhenAnEndpointIsConfigured() throws Exception {
        BlockingQueue<ExportRequest> requests = new LinkedBlockingQueue<>();
        HttpServer collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        collector.createContext("/v1/traces", exchange -> {
            try (exchange) {
                requests.add(new ExportRequest(exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestBody().readAllBytes()));
                exchange.getResponseHeaders().set("Content-Type", "application/x-protobuf");
                exchange.sendResponseHeaders(200, -1);
            }
        });
        collector.start();
        try (var context = application().run(
                "--management.opentelemetry.tracing.export.otlp.endpoint=http://127.0.0.1:"
                    + collector.getAddress().getPort() + "/v1/traces",
                "--management.tracing.sampling.probability=1.0")) {
            assertThat(context.getBeansOfType(SpanExporter.class)).hasSize(1);
            TraceContext trace = assertCorrelatedObservation(context);
            var flush = context.getBean(SdkTracerProvider.class).forceFlush();
            flush.join(10, TimeUnit.SECONDS);
            assertThat(flush.isSuccess()).isTrue();

            ExportRequest request = requests.poll(10, TimeUnit.SECONDS);
            assertThat(request).as("OTLP request received by the collector").isNotNull();
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.contentType()).isEqualTo("application/x-protobuf");
            // OTLP encodes names as UTF-8 and trace/span IDs as raw bytes.
            assertThat(request.body()).containsSequence("starter.tracing.test".getBytes(StandardCharsets.UTF_8));
            assertThat(request.body()).containsSequence(HexFormat.of().parseHex(trace.traceId()));
            assertThat(request.body()).containsSequence(HexFormat.of().parseHex(trace.spanId()));
        } finally {
            collector.stop(0);
        }
    }

    @Test
    void shouldAllowExportToBeDisabledWhileRetainingLocalTracing() {
        try (var context = application().run(
                "--management.opentelemetry.tracing.export.otlp.endpoint=http://127.0.0.1:4318/v1/traces",
                "--management.tracing.export.otlp.enabled=false")) {
            assertThat(context.getBeansOfType(SpanExporter.class)).isEmpty();
            assertCorrelatedObservation(context);
        }
    }

    private SpringApplicationBuilder application() {
        // Use the real Boot lifecycle and application.yaml, without starting web/database infrastructure.
        return new SpringApplicationBuilder(ObservabilityConfiguration.class)
            .web(WebApplicationType.NONE)
            .registerShutdownHook(false)
            .properties("spring.main.banner-mode=off", "spring.docker.compose.enabled=false");
    }

    private TraceContext assertCorrelatedObservation(ConfigurableApplicationContext context) {
        var observation = Observation.start("starter.tracing.test", context.getBean(ObservationRegistry.class));
        TraceContext trace;
        try (var scope = observation.openScope()) {
            var span = context.getBean(Tracer.class).currentSpan();
            assertThat(span).isNotNull();
            trace = span.context();
            assertThat(trace.traceId()).matches("[0-9a-f]{32}").isNotEqualTo("0".repeat(32));
            assertThat(trace.spanId()).matches("[0-9a-f]{16}").isNotEqualTo("0".repeat(16));
            assertThat(MDC.get("traceId")).isEqualTo(trace.traceId());
            assertThat(MDC.get("spanId")).isEqualTo(trace.spanId());
        } finally {
            observation.stop();
        }
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
        return trace;
    }

    private record ExportRequest(String method, String contentType, byte[] body) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
        MetricsAutoConfiguration.class,
        OtlpMetricsExportAutoConfiguration.class,
        ObservationAutoConfiguration.class,
        MicrometerTracingAutoConfiguration.class,
        OpenTelemetryTracingAutoConfiguration.class,
        OpenTelemetrySdkAutoConfiguration.class,
        OtlpTracingAutoConfiguration.class,
        ReactorAutoConfiguration.class
    })
    static class ObservabilityConfiguration {
    }
}
