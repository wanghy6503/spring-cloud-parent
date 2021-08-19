package com.github.jojotech.spring.cloud.webflux.auto;

import com.github.jojotech.spring.cloud.webflux.config.WebClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(WebClientConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class WebClientAutoConfiguration {
}
