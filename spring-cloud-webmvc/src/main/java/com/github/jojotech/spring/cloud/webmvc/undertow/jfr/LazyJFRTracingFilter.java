package com.github.jojotech.spring.cloud.webmvc.undertow.jfr;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.Tracer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class LazyJFRTracingFilter implements Filter {
    private final BeanFactory beanFactory;
    private JFRTracingFilter jfrTracingFilter;

    public LazyJFRTracingFilter(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        jfrTracingFilter().doFilter(servletRequest, servletResponse, filterChain);
    }

    private Filter jfrTracingFilter() {
        if (this.jfrTracingFilter == null) {
            this.jfrTracingFilter = new JFRTracingFilter(this.beanFactory.getBean(Tracer.class));
        }
        return this.jfrTracingFilter;
    }
}