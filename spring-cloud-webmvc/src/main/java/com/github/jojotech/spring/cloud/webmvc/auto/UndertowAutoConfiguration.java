package com.github.jojotech.spring.cloud.webmvc.auto;

import com.github.jojotech.spring.cloud.webmvc.config.WebServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(WebServerConfiguration.class)
public class UndertowAutoConfiguration {
}
