package com.github.jojotech.spring.cloud.webflux.config;

import com.github.jojotech.spring.cloud.webflux.webclient.WebClientNamedContextFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebClientConfigurationProperties.class)
public class WebClientConfiguration {
    @Bean
    public WebClientNamedContextFactory getWebClientNamedContextFactory() {
        return new WebClientNamedContextFactory();
    }
}
