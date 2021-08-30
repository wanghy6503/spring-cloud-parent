package com.github.jojotech.spring.cloud.webflux.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "webclient")
public class WebClientConfigurationProperties {
    private Map<String, WebClientProperties> configs;
    @Data
    @NoArgsConstructor
    public static class WebClientProperties {
        private static AntPathMatcher antPathMatcher = new AntPathMatcher();
        private Cache<String, Boolean> retryablePathsMatchResult = Caffeine.newBuilder().build();
        /**
         * 服务地址，不填写则为 http://serviceName
         */
        private String baseUrl;
        /**
         * 微服务名称，不填写就是 configs 这个 map 的 key
         */
        private String serviceName;
        /**
         * 可以重试的路径，默认只对 GET 方法重试，通过这个配置增加针对某些非 GET 方法的路径的重试
         */
        private List<String> retryablePaths;
        /**
         * 连接超时
         */
        private Duration connectTimeout = Duration.ofMillis(500);
        /**
         * 响应超时
         */
        private Duration responseTimeout = Duration.ofSeconds(8);

        /**
         * 是否匹配
         * @param path
         * @return
         */
        public boolean retryablePathsMatch(String path) {
            if (CollectionUtils.isEmpty(retryablePaths)) {
                return false;
            }
            return retryablePathsMatchResult.get(path, k -> {
                return retryablePaths.stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
            });
        }
    }
}
