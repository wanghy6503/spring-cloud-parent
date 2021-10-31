package com.github.jojotech.spring.cloud.commons.metric;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.log4j.Log4j2;

import org.springframework.cloud.client.ServiceInstance;

@Log4j2
public class ServiceInstanceMetrics {
	private static final String CALLING = "-Calling";
	private static final String FAILED = "-Failed";

	private MetricRegistry metricRegistry;

	ServiceInstanceMetrics() {
	}

	public ServiceInstanceMetrics(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	/**
	 * 记录调用实例
	 * @param serviceInstance
	 */
	public void recordServiceInstanceCall(ServiceInstance serviceInstance) {
		String key = serviceInstance.getHost() + ":" + serviceInstance.getPort();
		metricRegistry.counter(key + CALLING).inc();
	}
	/**
	 * 记录调用实例结束
	 * @param serviceInstance
	 * @param isSuccess 是否成功
	 */
	public void recordServiceInstanceCalled(ServiceInstance serviceInstance, boolean isSuccess) {
		String key = serviceInstance.getHost() + ":" + serviceInstance.getPort();
		metricRegistry.counter(key + CALLING).dec();
		if (!isSuccess) {
			//不成功则记录失败
			metricRegistry.meter(key + FAILED).mark();
		}
	}

	/**
	 * 获取正在运行的调用次数
	 * @param serviceInstance
	 * @return
	 */
	public long getCalling(ServiceInstance serviceInstance) {
		String key = serviceInstance.getHost() + ":" + serviceInstance.getPort();
		long count = metricRegistry.counter(key + CALLING).getCount();
		log.info("ServiceInstanceMetrics-getCalling: {} -> {}", key, count);
		return count;
	}

	/**
	 * 获取最近一分钟调用失败次数分钟速率，其实是滑动平均数
	 * @param serviceInstance
	 * @return
	 */
	public double getFailedInRecentOneMin(ServiceInstance serviceInstance) {
		String key = serviceInstance.getHost() + ":" + serviceInstance.getPort();
		double rate = metricRegistry.meter(key + FAILED).getOneMinuteRate();
		log.info("ServiceInstanceMetrics-getFailedInRecentOneMin: {} -> {}", key, rate);
		return rate;
	}
}
