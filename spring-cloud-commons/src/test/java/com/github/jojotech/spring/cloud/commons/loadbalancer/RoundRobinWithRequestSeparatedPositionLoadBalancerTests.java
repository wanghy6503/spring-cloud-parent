package com.github.jojotech.spring.cloud.commons.loadbalancer;

import java.util.ArrayList;

import com.github.jojotech.spring.cloud.commons.metric.ServiceInstanceMetrics;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import static org.mockito.Mockito.when;

class RoundRobinWithRequestSeparatedPositionLoadBalancerTests {
	@Test
	public void getInstanceResponseByRoundRobin() {
		DefaultServiceInstance service1Instance1 = new DefaultServiceInstance();
		service1Instance1.setHost("10.238.1.1");
		service1Instance1.setPort(1);
		DefaultServiceInstance service1Instance2 = new DefaultServiceInstance();
		service1Instance2.setHost("10.238.2.2");
		service1Instance2.setPort(2);
		DefaultServiceInstance service1Instance3 = new DefaultServiceInstance();
		service1Instance3.setHost("10.238.3.3");
		service1Instance3.setPort(3);
		DefaultServiceInstance service1Instance4 = new DefaultServiceInstance();
		service1Instance4.setHost("10.238.4.4");
		service1Instance4.setPort(4);
		ServiceInstanceListSupplier serviceInstanceListSupplier = Mockito.mock(ServiceInstanceListSupplier.class);
		String serviceId = "test";
		ServiceInstanceMetrics serviceInstanceMetrics = Mockito.mock(ServiceInstanceMetrics.class);
		RoundRobinWithRequestSeparatedPositionLoadBalancer roundRobinWithRequestSeparatedPositionLoadBalancer
				= new RoundRobinWithRequestSeparatedPositionLoadBalancer(serviceInstanceListSupplier, serviceId, null, serviceInstanceMetrics);
		ArrayList<ServiceInstance> serviceInstances = Lists
				.newArrayList(service1Instance1, service1Instance2, service1Instance3, service1Instance4);
		long traceId = 1234;
		when(serviceInstanceMetrics.getCalling(service1Instance1)).thenReturn(1L);
		when(serviceInstanceMetrics.getCalling(service1Instance2)).thenReturn(2L);
		when(serviceInstanceMetrics.getCalling(service1Instance3)).thenReturn(1L);
		when(serviceInstanceMetrics.getCalling(service1Instance4)).thenReturn(1L);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance1)).thenReturn(0.1);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance2)).thenReturn(0.1);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance3)).thenReturn(0.2);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance4)).thenReturn(0.3);
		Response<ServiceInstance> instanceResponseByRoundRobin = roundRobinWithRequestSeparatedPositionLoadBalancer
				.getInstanceResponseByRoundRobin(traceId, serviceInstances);
		//第一次调用，实例 1 和实例 2 错误率 最少，同时 实例 1 的调用小于 实例 2，所以返回 实例 1
		Assertions.assertEquals(instanceResponseByRoundRobin.getServer(), service1Instance1);
		instanceResponseByRoundRobin = roundRobinWithRequestSeparatedPositionLoadBalancer
				.getInstanceResponseByRoundRobin(traceId, serviceInstances);
		//由于实例 1 已经调用过，这次调用的是实例 2
		Assertions.assertEquals(instanceResponseByRoundRobin.getServer(), service1Instance2);
		instanceResponseByRoundRobin = roundRobinWithRequestSeparatedPositionLoadBalancer
				.getInstanceResponseByRoundRobin(traceId, serviceInstances);
		//这时候还没调用过实例 3，所以返回实例 3
		Assertions.assertEquals(instanceResponseByRoundRobin.getServer(), service1Instance3);
		instanceResponseByRoundRobin = roundRobinWithRequestSeparatedPositionLoadBalancer
				.getInstanceResponseByRoundRobin(traceId, serviceInstances);
		//这时候还没调用过实例 4，所以返回实例 4
		Assertions.assertEquals(instanceResponseByRoundRobin.getServer(), service1Instance4);
		when(serviceInstanceMetrics.getCalling(service1Instance1)).thenReturn(2L);
		when(serviceInstanceMetrics.getCalling(service1Instance2)).thenReturn(1L);
		when(serviceInstanceMetrics.getCalling(service1Instance3)).thenReturn(2L);
		when(serviceInstanceMetrics.getCalling(service1Instance4)).thenReturn(2L);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance1)).thenReturn(1.0);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance2)).thenReturn(1.0);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance3)).thenReturn(1.0);
		when(serviceInstanceMetrics.getFailedInRecentOneMin(service1Instance4)).thenReturn(1.0);
		instanceResponseByRoundRobin = roundRobinWithRequestSeparatedPositionLoadBalancer
				.getInstanceResponseByRoundRobin(traceId, serviceInstances);
		//所有实例都调用过，错误率一样，调用量实例 2 最少，所以返回实例 2
		Assertions.assertEquals(instanceResponseByRoundRobin.getServer(), service1Instance2);
	}
}