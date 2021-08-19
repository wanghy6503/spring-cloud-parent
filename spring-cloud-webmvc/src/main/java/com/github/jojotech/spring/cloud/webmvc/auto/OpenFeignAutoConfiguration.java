package com.github.jojotech.spring.cloud.webmvc.auto;

import com.github.jojotech.spring.cloud.webmvc.config.CommonOpenFeignConfiguration;
import com.github.jojotech.spring.cloud.webmvc.config.DefaultOpenFeignConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(CommonOpenFeignConfiguration.class)
@EnableFeignClients(value = "com.github.jojotech", defaultConfiguration = DefaultOpenFeignConfiguration.class)
public class OpenFeignAutoConfiguration {
}
