package com.github.jojotech.spring.cloud.webmvc.feign;

import io.github.resilience4j.feign.FeignDecorators;

/**
 * 用于包装FeignDecoratorBuilder
 * 可以用于实现 fallback
 */
@FunctionalInterface
public interface FeignDecoratorBuilderInterceptor {
    void intercept(FeignDecorators.Builder builder);
}
