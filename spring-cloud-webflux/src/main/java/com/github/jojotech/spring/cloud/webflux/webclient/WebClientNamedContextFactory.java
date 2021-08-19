package com.github.jojotech.spring.cloud.webflux.webclient;

import com.github.jojotech.spring.cloud.webflux.config.WebClientDefaultConfiguration;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class WebClientNamedContextFactory extends NamedContextFactory<WebClientSpecification> {
    public static final String NAMESPACE = "webclient";
    public static final String PROPERTY_NAME = NAMESPACE + ".name";

    public WebClientNamedContextFactory() {
        super(WebClientDefaultConfiguration.class, NAMESPACE, PROPERTY_NAME);
    }

    /**
     * 获取 WebClient
     * @param name
     * @return
     */
    public WebClient getWebClient(String name) {
        return getInstance(name, WebClient.class);
    }
}
