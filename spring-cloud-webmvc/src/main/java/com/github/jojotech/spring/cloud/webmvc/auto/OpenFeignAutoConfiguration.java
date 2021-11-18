package com.github.jojotech.spring.cloud.webmvc.auto;

import com.github.jojotech.spring.cloud.webmvc.config.CommonOpenFeignConfiguration;
import com.github.jojotech.spring.cloud.webmvc.config.DefaultOpenFeignConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

//设置 `@Configuration(proxyBeanMethods=false)`，因为没有 @Bean 的方法互相调用需要每次返回同一个 Bean，没必要代理，关闭增加启动速度
@Configuration(proxyBeanMethods = false)
//加载配置，CommonOpenFeignConfiguration
@Import(CommonOpenFeignConfiguration.class)
//启用 OpenFeign 注解扫描和配置，默认配置为 DefaultOpenFeignConfiguration，其实就是 Feign 的 NamedContextFactory（即 FeignContext）的默认配置类是 DefaultOpenFeignConfiguration
@EnableFeignClients(value = "com.github.jojotech", defaultConfiguration = DefaultOpenFeignConfiguration.class)
public class OpenFeignAutoConfiguration {
}
