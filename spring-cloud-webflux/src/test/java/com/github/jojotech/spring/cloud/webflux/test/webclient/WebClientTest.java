package com.github.jojotech.spring.cloud.webflux.test.webclient;

import brave.Span;
import brave.Tracer;
import com.github.jojotech.spring.cloud.commons.loadbalancer.RoundRobinWithRequestSeparatedPositionLoadBalancer;
import com.github.jojotech.spring.cloud.webflux.webclient.WebClientNamedContextFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.netflix.eureka.loadbalancer.LoadBalancerEurekaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;

//SpringExtension也包含了MockitoJUnitRunner，所以 @Mock 等注解也生效了
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "webclient.configs." + WebClientTest.SERVICE_WITH_CANNOT_CONNECT + ".baseUrl=http://" + WebClientTest.SERVICE_WITH_CANNOT_CONNECT,
        "webclient.configs." + WebClientTest.SERVICE_WITH_CANNOT_CONNECT + ".serviceName=" + WebClientTest.SERVICE_WITH_CANNOT_CONNECT,
        "webclient.configs." + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE + ".baseUrl=http://" + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE,
        "webclient.configs." + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE + ".serviceName=" + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE,
        "webclient.configs." + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE + ".responseTimeout=2s",
        "webclient.configs." + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE + ".retryablePaths[0]=/delay/3",
        "webclient.configs." + WebClientTest.SERVICE_WITH_ONLY_ONE_NODE + ".retryablePaths[1]=/status/4*",
        LoadBalancerEurekaAutoConfiguration.LOADBALANCER_ZONE + "=zone1",
        "resilience4j.retry.configs.default.maxAttempts=" + WebClientTest.DEFAULT_RETRY_ATTEMPT,
        "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50",
        "resilience4j.circuitbreaker.configs.default.slidingWindowType=TIME_BASED",
        "resilience4j.circuitbreaker.configs.default.slidingWindowSize=5",
        "resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.configs.default.recordExceptions=java.lang.Exception",
})
public class WebClientTest {
    public static final String SERVICE_WITH_CANNOT_CONNECT = "testServiceWithCannotConnect";
    public static final String SERVICE_WITH_ONLY_ONE_NODE = "testService";
    public static final int DEFAULT_RETRY_ATTEMPT = 3;

    @EnableAutoConfiguration
    @Configuration
    public static class App {
        @Bean
        public ReactiveDiscoveryClient myDiscoveryClient() {
            ServiceInstance zone1Instance1 = Mockito.spy(ServiceInstance.class);
            ServiceInstance zone1Instance2 = Mockito.spy(ServiceInstance.class);
            Map<String, String> zone1 = Map.ofEntries(
                    Map.entry("zone", "zone1")
            );
            when(zone1Instance1.getMetadata()).thenReturn(zone1);
            when(zone1Instance1.getInstanceId()).thenReturn("instance1");
            when(zone1Instance1.getHost()).thenReturn("www.httpbin.org");
            when(zone1Instance1.getPort()).thenReturn(80);
            when(zone1Instance2.getMetadata()).thenReturn(zone1);
            when(zone1Instance2.getInstanceId()).thenReturn("instance2");
            when(zone1Instance2.getHost()).thenReturn("www.httpbin.org");
            //这个端口无法访问，主要验证重试
            when(zone1Instance2.getPort()).thenReturn(8081);
            ReactiveDiscoveryClient spy = Mockito.spy(ReactiveDiscoveryClient.class);
            Mockito.when(spy.getInstances(SERVICE_WITH_CANNOT_CONNECT))
                    .thenReturn(Flux.fromIterable(List.of(zone1Instance1, zone1Instance2)));
            Mockito.when(spy.getInstances(SERVICE_WITH_ONLY_ONE_NODE))
                    .thenReturn(Flux.fromIterable(List.of(zone1Instance1)));
            return spy;
        }
    }

    @SpyBean
    private WebClientNamedContextFactory webClientNamedContextFactory;
    @SpyBean
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @SpyBean
    private LoadBalancerClientFactory loadBalancerClientFactory;
    @SpyBean
    private Tracer tracer;

    @Test
    public void testRetryOnConnectTimeout() {
        //由于我们针对 testService 返回了两个实例，一个可以正常连接，一个不可以，但是我们配置了重试 3 次，所以每次请求应该都能成功，并且随着程序运行，后面的调用不可用的实例还会被断路
        //这里主要测试针对 connect time out 还有 断路器打开的情况都会重试，并且无论是 GET 方法还是其他的
        Span span = tracer.nextSpan();
        circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
        for (int i = 0; i < 10; i++) {
            try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_CANNOT_CONNECT)
                        .get().uri("/anything").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
                stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_CANNOT_CONNECT)
                        .post().uri("/anything").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            }
        }
    }

    @Test
    public void testRetryOnReadTimeout() throws Exception {
        //测试只对于 GET 以及允许重试的请求进行重试，重试次数通过负载均衡器的 seed 增长
        Span span = tracer.nextSpan();
        try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
            boolean hasException = false;
            //清除断路器影响
            circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
            long l = span.context().traceId();
            RoundRobinWithRequestSeparatedPositionLoadBalancer loadBalancerClientFactoryInstance
                    = (RoundRobinWithRequestSeparatedPositionLoadBalancer) loadBalancerClientFactory.getInstance(SERVICE_WITH_ONLY_ONE_NODE);
            AtomicInteger atomicInteger = loadBalancerClientFactoryInstance.getPositionCache().get(l);
            int start = atomicInteger.get();
            try {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_ONLY_ONE_NODE)
                        .get().uri("/delay/3").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            } catch (Exception e) {
                hasException = true;
            }
            //验证请求了 3 次，通过负载均衡器被调用了几次得知请求了几次
            Assertions.assertEquals(DEFAULT_RETRY_ATTEMPT, atomicInteger.get() - start);
            Assertions.assertTrue(hasException);

            //清除断路器影响
            circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
            hasException = false;
            start = atomicInteger.get();
            try {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_ONLY_ONE_NODE)
                        .post().uri("/delay/4").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            } catch (Exception e) {
                hasException = true;
            }
            //由于 post 请求默认不重试，并且请求路径也不在重试路径中，所以只会请求一次
            //验证请求了 1 次，通过负载均衡器被调用了几次得知请求了几次
            Assertions.assertEquals(1, atomicInteger.get() - start);
            Assertions.assertTrue(hasException);

            //清除断路器影响
            circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
            hasException = false;
            start = atomicInteger.get();
            try {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_ONLY_ONE_NODE)
                        .post().uri("/delay/3").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            } catch (Exception e) {
                hasException = true;
            }
            //由于请求路径在重试路径中，所以会正常重试
            //验证请求了 3 次，通过负载均衡器被调用了几次得知请求了几次
            Assertions.assertEquals(DEFAULT_RETRY_ATTEMPT, atomicInteger.get() - start);
            Assertions.assertTrue(hasException);
        }
    }

    @Test
    public void testStatusNot2xx() {
        Span span = tracer.nextSpan();
        try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
            boolean hasException = false;
            //清除断路器影响
            circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
            long l = span.context().traceId();
            RoundRobinWithRequestSeparatedPositionLoadBalancer loadBalancerClientFactoryInstance
                    = (RoundRobinWithRequestSeparatedPositionLoadBalancer) loadBalancerClientFactory.getInstance(SERVICE_WITH_ONLY_ONE_NODE);
            AtomicInteger atomicInteger = loadBalancerClientFactoryInstance.getPositionCache().get(l);
            int start = atomicInteger.get();
            try {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_ONLY_ONE_NODE)
                        .get().uri("/status/500").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            } catch (Exception e) {
                hasException = true;
            }
            //验证请求了 3 次，通过负载均衡器被调用了几次得知请求了几次
            Assertions.assertEquals(DEFAULT_RETRY_ATTEMPT, atomicInteger.get() - start);
            Assertions.assertTrue(hasException);

            //清除断路器影响
            circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
            hasException = false;
            start = atomicInteger.get();
            try {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_ONLY_ONE_NODE)
                        .post().uri("/status/500").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            } catch (Exception e) {
                hasException = true;
            }
            //由于 post 请求默认不重试，并且请求路径也不在重试路径中，所以只会请求一次
            //验证请求了 1 次，通过负载均衡器被调用了几次得知请求了几次
            Assertions.assertEquals(1, atomicInteger.get() - start);
            Assertions.assertTrue(hasException);

            //清除断路器影响
            circuitBreakerRegistry.getAllCircuitBreakers().asJava().forEach(CircuitBreaker::reset);
            hasException = false;
            start = atomicInteger.get();
            try {
                Mono<String> stringMono = webClientNamedContextFactory.getWebClient(SERVICE_WITH_ONLY_ONE_NODE)
                        .post().uri("/status/400").retrieve()
                        .bodyToMono(String.class);
                System.out.println(stringMono.block());
            } catch (Exception e) {
                hasException = true;
            }
            //由于请求路径在重试路径中，所以会正常重试
            //验证请求了 3 次，通过负载均衡器被调用了几次得知请求了几次
            Assertions.assertEquals(DEFAULT_RETRY_ATTEMPT, atomicInteger.get() - start);
            Assertions.assertTrue(hasException);
        }
    }
}
