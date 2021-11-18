package com.github.jojotech.spring.cloud.webflux.webclient.resilience4j;

import com.github.jojotech.spring.cloud.commons.metric.ServiceInstanceMetrics;
import com.github.jojotech.spring.cloud.webflux.config.WebClientConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.ClientResponse;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;

public class ClientResponseMonoCircuitBreaker extends MonoOperator<ClientResponse, ClientResponse> {
    private final CircuitBreaker circuitBreaker;
    private final ServiceInstance serviceInstance;
    private final ServiceInstanceMetrics serviceInstanceMetrics;
    private final WebClientConfigurationProperties.WebClientProperties webClientProperties;

    ClientResponseMonoCircuitBreaker(Mono<? extends ClientResponse> source, CircuitBreaker circuitBreaker, ServiceInstance serviceInstance, ServiceInstanceMetrics serviceInstanceMetrics, WebClientConfigurationProperties.WebClientProperties webClientProperties) {
        super(source);
        this.circuitBreaker = circuitBreaker;
        this.serviceInstance = serviceInstance;
        this.serviceInstanceMetrics = serviceInstanceMetrics;
        this.webClientProperties = webClientProperties;
    }

    @Override
    public void subscribe(CoreSubscriber<? super ClientResponse> actual) {
        if (circuitBreaker.tryAcquirePermission()) {
            source.subscribe(new ClientResponseCircuitBreakerSubscriber(circuitBreaker, actual, serviceInstance, serviceInstanceMetrics, true, webClientProperties));
        } else {
            Operators.error(actual, createCallNotPermittedException(circuitBreaker));
        }
    }
}
