package com.github.jojotech.spring.cloud.webflux.webclient.resilience4j;

import java.util.function.UnaryOperator;

import com.github.jojotech.spring.cloud.commons.metric.ServiceInstanceMetrics;
import com.github.jojotech.spring.cloud.webflux.config.WebClientConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.IllegalPublisherException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * 基于官方的 CircuitBreakerOperator 针对 ClientResponse 改造，基于 ClientResponse 的 http status code
 * @see io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
 */
public class ClientResponseCircuitBreakerOperator implements UnaryOperator<Publisher<ClientResponse>> {
    private final CircuitBreaker circuitBreaker;
    private final ServiceInstance serviceInstance;
    private final WebClientConfigurationProperties.WebClientProperties webClientProperties;
    private final ServiceInstanceMetrics serviceInstanceMetrics;

    private ClientResponseCircuitBreakerOperator(CircuitBreaker circuitBreaker, ServiceInstance serviceInstance, ServiceInstanceMetrics serviceInstanceMetrics, WebClientConfigurationProperties.WebClientProperties webClientProperties) {
        this.circuitBreaker = circuitBreaker;
        this.serviceInstance = serviceInstance;
        this.serviceInstanceMetrics = serviceInstanceMetrics;
        this.webClientProperties = webClientProperties;
    }

    public static ClientResponseCircuitBreakerOperator of(CircuitBreaker circuitBreaker, ServiceInstance serviceInstance, ServiceInstanceMetrics serviceInstanceMetrics, WebClientConfigurationProperties.WebClientProperties webClientProperties) {
        return new ClientResponseCircuitBreakerOperator(circuitBreaker, serviceInstance, serviceInstanceMetrics, webClientProperties);
    }

    @Override
    public Publisher<ClientResponse> apply(Publisher<ClientResponse> clientResponsePublisher) {
        if (clientResponsePublisher instanceof Mono) {
            return new ClientResponseMonoCircuitBreaker((Mono<? extends ClientResponse>) clientResponsePublisher, circuitBreaker, serviceInstance, serviceInstanceMetrics, webClientProperties);
        } else if (clientResponsePublisher instanceof Flux) {
            return new ClientResponseFluxCircuitBreaker((Flux<? extends ClientResponse>) clientResponsePublisher, circuitBreaker, serviceInstance, serviceInstanceMetrics, webClientProperties);
        } else {
            throw new IllegalPublisherException(clientResponsePublisher);
        }
    }
}
