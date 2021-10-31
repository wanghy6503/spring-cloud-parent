package com.github.jojotech.spring.cloud.commons.config;

import com.codahale.metrics.MetricRegistry;
import com.github.jojotech.spring.cloud.commons.metric.ServiceInstanceMetrics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LoadBalancerConfiguration {
	@Bean
	public ServiceInstanceMetrics getLoadBalancerMetricRegistry() {
		return new ServiceInstanceMetrics(new MetricRegistry());
	}
}
