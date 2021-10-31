package com.github.jojotech.spring.cloud.commons.loadbalancer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import brave.Span;
import brave.Tracer;
import com.codahale.metrics.MetricRegistry;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.jojotech.spring.cloud.commons.metric.ServiceInstanceMetrics;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

//一定必须是实现ReactorServiceInstanceLoadBalancer
//而不是ReactorLoadBalancer<ServiceInstance>
//因为注册的时候是ReactorServiceInstanceLoadBalancer
@Log4j2
public class RoundRobinWithRequestSeparatedPositionLoadBalancer implements ReactorServiceInstanceLoadBalancer {
	private final ServiceInstanceListSupplier serviceInstanceListSupplier;
	//每次请求算上重试不会超过3分钟
	//对于超过3分钟的，这种请求肯定比较重，不应该重试
	private final LoadingCache<Long, AtomicInteger> positionCache = Caffeine.newBuilder()
			.expireAfterWrite(3, TimeUnit.MINUTES)
			//随机初始值，防止每次都是从第一个开始调用
			.build(k -> new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 1000)));
	private final LoadingCache<Long, Set<String>> calledIpPrefixes = Caffeine.newBuilder()
			.expireAfterAccess(3, TimeUnit.MINUTES)
			.build(k -> Sets.newConcurrentHashSet());
	private final String serviceId;
	private final Tracer tracer;
	private final ServiceInstanceMetrics serviceInstanceMetrics;

	@VisibleForTesting
	public LoadingCache<Long, AtomicInteger> getPositionCache() {
		return positionCache;
	}

	@VisibleForTesting
	LoadingCache<Long, Set<String>> getCalledIpPrefixes() {
		return calledIpPrefixes;
	}

	public RoundRobinWithRequestSeparatedPositionLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplier, String serviceId, Tracer tracer, ServiceInstanceMetrics serviceInstanceMetrics) {
		this.serviceInstanceListSupplier = serviceInstanceListSupplier;
		this.serviceId = serviceId;
		this.tracer = tracer;
		this.serviceInstanceMetrics = serviceInstanceMetrics;
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		Span span = tracer.currentSpan();
		return serviceInstanceListSupplier.get().next()
				.map(serviceInstances -> {
					//保持 span 和调用 choose 的 span 一样
					try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
						return getInstanceResponse(serviceInstances);
					}
				});
	}

	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> serviceInstances) {
		if (serviceInstances.isEmpty()) {
			log.warn("No servers available for service: " + this.serviceId);
			return new EmptyResponse();
		}
		Span currentSpan = tracer.currentSpan();
		if (currentSpan == null) {
			currentSpan = tracer.newTrace();
		}
		long l = currentSpan.context().traceId();
		return getInstanceResponseByRoundRobin(l, serviceInstances);
	}

	@VisibleForTesting
	public Response<ServiceInstance> getInstanceResponseByRoundRobin(long traceId, List<ServiceInstance> serviceInstances) {
		Collections.shuffle(serviceInstances);
		//需要先将所有参数缓存起来，否则 comparator 会调用多次，并且可能在排序过程中参数发生改变
		Map<ServiceInstance, Integer> used = Maps.newHashMap();
		Map<ServiceInstance, Long> callings = Maps.newHashMap();
		Map<ServiceInstance, Double> failedInRecentOneMin = Maps.newHashMap();
		serviceInstances = serviceInstances.stream().sorted(
				Comparator
						//之前已经调用过的网段，这里排后面
						.<ServiceInstance>comparingInt(serviceInstance -> {
							return used.computeIfAbsent(serviceInstance, k -> {
								return calledIpPrefixes.get(traceId).stream().anyMatch(prefix -> {
									return serviceInstance.getHost().contains(prefix);
								}) ? 1 : 0;
							});
						})
						//当前错误率最少的
						.thenComparingDouble(serviceInstance -> {
							return failedInRecentOneMin.computeIfAbsent(serviceInstance, k -> {
								double value = serviceInstanceMetrics.getFailedInRecentOneMin(serviceInstance);
								//由于使用的是移动平均值（EMA），需要忽略过小的差异（保留两位小数，不是四舍五入，而是直接舍弃）
								return ((int) (value * 100)) / 100.0;
							});
						})
						//当前负载请求最少的
						.thenComparingLong(serviceInstance -> {
							return callings.computeIfAbsent(serviceInstance, k ->
									serviceInstanceMetrics.getCalling(serviceInstance)
							);
						})
		).collect(Collectors.toList());
		if (serviceInstances.isEmpty()) {
			log.warn("No servers available for service: " + this.serviceId);
			return new EmptyResponse();
		}
		ServiceInstance serviceInstance = serviceInstances.get(0);
		//记录本次返回的网段
		calledIpPrefixes.get(traceId).add(serviceInstance.getHost().substring(0, serviceInstance.getHost().lastIndexOf(".")));
		//目前记录这个只为了兼容之前的单元测试（调用次数测试）
		positionCache.get(traceId).getAndIncrement();
		return new DefaultResponse(serviceInstance);
	}
}
