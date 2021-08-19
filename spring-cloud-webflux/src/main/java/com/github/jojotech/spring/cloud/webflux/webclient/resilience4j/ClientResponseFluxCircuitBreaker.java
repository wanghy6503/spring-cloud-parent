package com.github.jojotech.spring.cloud.webflux.webclient.resilience4j;

import com.github.jojotech.spring.cloud.webflux.config.WebClientConfigurationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.Operators;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;

public class ClientResponseFluxCircuitBreaker extends FluxOperator<ClientResponse, ClientResponse> {
    private final CircuitBreaker circuitBreaker;
    private final WebClientConfigurationProperties.WebClientProperties webClientProperties;

    ClientResponseFluxCircuitBreaker(Flux<? extends ClientResponse> source, CircuitBreaker circuitBreaker, WebClientConfigurationProperties.WebClientProperties webClientProperties) {
        super(source);
        this.circuitBreaker = circuitBreaker;
        this.webClientProperties = webClientProperties;
    }

    @Override
    public void subscribe(CoreSubscriber<? super ClientResponse> actual) {
        if (circuitBreaker.tryAcquirePermission()) {
            source.subscribe(new ClientResponseCircuitBreakerSubscriber(circuitBreaker, actual, false, webClientProperties));
        } else {
            Operators.error(actual, createCallNotPermittedException(circuitBreaker));
        }
    }
}
