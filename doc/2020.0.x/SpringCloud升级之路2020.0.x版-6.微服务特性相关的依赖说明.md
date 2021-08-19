![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/6-01.%20spring-cloud-common%20%E4%BE%9D%E8%B5%96%E4%B8%8E%E5%8A%9F%E8%83%BD%E8%AF%B4%E6%98%8E.jpg)

spring-cloud-common 不再是一个纯依赖的项目，这个模块包括：

1. spring-framework-common 的依赖
2. 同步与异步微服务公共的依赖
3. **同步与异步微服务公共的框架代码改造，这个我们后面分析框架以及我们设计的修改的时候，会详细分析，这里先跳过**

**同步与异步微服务公共的依赖包括**：

> 代码请参考：https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-spring-cloud-common/pom.xml

**1.启用 Spring Cloud 的 Bootstrap Context**：在 Spring Cloud 2020.0.x 版本开始，**Bootstrap Context 默认不再启用**。我们的项目，某些模块使用了 spring-cloud-config，这个是需要启用 Bootstrap Context 的。同时，我们的配置，还通过 `bootstrap.yml` 与 `application.yml` 区分了不同配置，如果多环境中配置是一样并且基本不会动态更改的则放入 `bootstrap.yml`，不同环境不同或者可能动态修改则放入 `application.yml`。所以通过加入如下依赖来启用：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```
这个底层实现非常简单，是否启用 Bootstrap Context 是通过检查这个依赖中的 Marker 类是否存在而决定的。参考代码：

[`PropertyUtils.java`](https://github.com/spring-cloud/spring-cloud-commons/blob/v3.0.3/spring-cloud-context/src/main/java/org/springframework/cloud/util/PropertyUtils.java)：

```
/**
 * Property name for bootstrap marker class name.
 */
public static final String MARKER_CLASS = "org.springframework.cloud.bootstrap.marker.Marker";

/**
 * Boolean if bootstrap marker class exists.
 */
public static final boolean MARKER_CLASS_EXISTS = ClassUtils.isPresent(MARKER_CLASS, null);
```

**2.使用 Eureka 作为注册中心**，我们需要添加 Eureka 的客户端依赖：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**3.不使用 Ribbon，使用 Spring Cloud LoadBalancer 作为我们的负载均衡器**：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-loadbalancer</artifactId>
</dependency>
```

**4.使用 resilience4j 作为重试、断路、限并发、限流的组件基础**：
```
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-cloud2</artifactId>
</dependency>
```

**5.暴露 actuator 相关端口**
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**6.使用 prometheus 进行指标监控采集**
```
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/6-02.%20spring-cloud-webmvc%20%E4%BE%9D%E8%B5%96%E4%B8%8E%E5%8A%9F%E8%83%BD%E8%AF%B4%E6%98%8E.jpg)

spring-cloud-webmvc 是针对基于同步 spring-mvc 的微服务的依赖，同样的，spring-cloud-webmvc 也包含**同步微服务公共的框架代码改造，这个我们后面分析框架以及我们设计的修改的时候，会详细分析，这里先跳过**。我们这里先只说明依赖：

> 代码请参考：https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-spring-cloud-webmvc/pom.xml

**1.spring-cloud-common 的依赖**：之前提到过 spring-cloud-common 是 spring-cloud-webmvc 与 spring-cloud-webflux 的公共依赖。

**2.使用 undertow 作为我们的 web 容器**：web-mvc 默认的容器是 tomcat，需要排除这个依赖，并添加 undertow 相关依赖。

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

**3.使用 webflux 相关异步接口**，某些微服务主要基于同步接口，但有一些特殊的接口使用的异步响应式实现，这个并不会发生冲突，所以在这里我们也添加了 web-flux 依赖。
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**4.使用 OpenFeign 作为同步微服务调用客户端**，OpenFeign 目前主要还是作为同步客户端使用，虽然目前也有异步实现，但是功能与粘合代码还不完整，异步的我们还是会使用 WebClient。
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**5.粘合 OpenFeign 与 resilience4j 的代码依赖**，官方提供了 OpenFeign 与 resilience4j 粘合代码，请参考：[resilience4j-feign](https://resilience4j.readme.io/docs/feign)。我们会在此基础上做一些个性化改造，后面我们会详细分析。
```
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-feign</artifactId>
</dependency>
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/6-03.%20spring-cloud-webflux%20%E4%BE%9D%E8%B5%96%E4%B8%8E%E5%8A%9F%E8%83%BD%E8%AF%B4%E6%98%8E.jpg)

spring-cloud-webmvc 是针对基于异步响应式 spring-webflux 的微服务的依赖，同样的，spring-cloud-webflux 也包含**异步微服务公共的框架代码改造，这个我们后面分析框架以及我们设计的修改的时候，会详细分析，这里先跳过**。我们这里先只说明依赖：

> 代码请参考：https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-spring-cloud-webflux/pom.xml

**1.spring-cloud-common 的依赖**：之前提到过 spring-cloud-common 是 spring-cloud-webmvc 与 spring-cloud-webflux 的公共依赖。

**2.使用 webflux 作为我们的 web 容器**，这里我们不需要额外 web 容器。
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```
**3.粘合 project-reactor 与 resilience4j**，这个在异步场景经常会用到，请参考：[resilience4j-reactor](https://resilience4j.readme.io/docs/examples-1)
```
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
</dependency>
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)

本小节我们分析了我们项目中的微服务公共依赖以及基于 web-mvc 同步的微服务依赖和基于 web-flux 异步的微服务依赖。下一节我们将从一些 Spring 基础开始，逐步深入分析我们的 Spring Cloud 框架。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)