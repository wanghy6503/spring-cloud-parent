package com.github.jojotech.spring.cloud.commons.auto;

import com.github.jojotech.spring.cloud.commons.config.Log4j2Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({Log4j2Configuration.class})
@AutoConfigureAfter(PrometheusMetricsExportAutoConfiguration.class)
public class Log4j2AutoConfiguration {
}
