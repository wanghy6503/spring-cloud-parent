package com.github.jojotech.spring.cloud.commons.auto;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class SchedulingAutoConfiguration {
}
