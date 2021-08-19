package com.github.jojotech.spring.cloud.webflux.webclient.resilience4j;

import com.github.jojotech.spring.cloud.webflux.config.WebClientConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.IllegalPublisherException;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.UnaryOperator;

/**
 * 基于官方的 CircuitBreakerOperator 针对 ClientResponse 改造，基于 ClientResponse 的 http status code
 * @see io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
 */
public class ClientResponseCircuitBreakerOperator implements UnaryOperator<Publisher<ClientResponse>> {
    private final CircuitBreaker circuitBreaker;
    private final WebClientConfigurationProperties.WebClientProperties webClientProperties;

    private ClientResponseCircuitBreakerOperator(CircuitBreaker circuitBreaker, WebClientConfigurationProperties.WebClientProperties webClientProperties) {
        this.circuitBreaker = circuitBreaker;
        this.webClientProperties = webClientProperties;
    }

    public static ClientResponseCircuitBreakerOperator of(CircuitBreaker circuitBreaker, WebClientConfigurationProperties.WebClientProperties webClientProperties) {
        return new ClientResponseCircuitBreakerOperator(circuitBreaker, webClientProperties);
    }

    @Override
    public Publisher<ClientResponse> apply(Publisher<ClientResponse> clientResponsePublisher) {
        if (clientResponsePublisher instanceof Mono) {
            return new ClientResponseMonoCircuitBreaker((Mono<? extends ClientResponse>) clientResponsePublisher, circuitBreaker, webClientProperties);
        } else if (clientResponsePublisher instanceof Flux) {
            return new ClientResponseFluxCircuitBreaker((Flux<? extends ClientResponse>) clientResponsePublisher, circuitBreaker, webClientProperties);
        } else {
            throw new IllegalPublisherException(clientResponsePublisher);
        }
    }
}
