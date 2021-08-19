![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列为之前系列的整理重启版，随着项目的发展以及项目中的使用，之前系列里面很多东西发生了变化，并且还有一些东西之前系列并没有提到，所以重启这个系列重新整理下，欢迎各位留言交流，谢谢！~

在理解 Spring Cloud 之前，我们先了解下 Spring 框架、Spring Boot、Spring Cloud 这三者的关系，从一个简单的 Bean，是如何发展出一个具有微服务特性的 Spring Cloud 的呢？

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/7-01.%20%E4%BB%80%E4%B9%88%E6%98%AF%20Spring%20bean.jpg)

Spring bean 是 Spring 框架在运行时管理的对象。Spring bean 是任何Spring应用程序的基本构建块。你编写的大多数应用程序逻辑代码都将放在Spring bean 中。之后**我们就用 Bean 来简称 Spring bean**。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/7-02.%20%E4%BB%80%E4%B9%88%E6%98%AFBeanFactory.jpg)

BeanFactory 是 Spring 容器的核心，是一个管理着所有 Bean 的容器。通常情况下，BeanFactory 的实现是使用懒加载的方式，这意味着 **Bean 只有在我们通过 `getBean()` 方法直接调用获取它们时才进行实例化**。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/7-03.%20%E4%BB%80%E4%B9%88%E6%98%AFApplicationContext.jpg)

ApplicationContext 在 BeanFactory 的基础上，增加了：
 - **资源定位与加载**，基于 `ResourcePatternResolver`（其实就是带通配符的 `ResourceLoader`），用来定位并加载各种文件或者网络资源
 - **所处环境**，基于 `EnvironmentCapable`。每个  `ApplicationContext` 都是有 `Environment` 的，这个 `Environment`，包括 Profile 定义还有 Properties。Profile 配置是一个被命名的、bean 定义的逻辑组，这些 bean 只有在给定的 profile 配置激活时才会注册到容器。Properties 是 Spring 的属性组，这些属性可能来源于 properties 文件、JVM properties、system环境变量、JNDI、servlet context parameters 上下文参数、专门的 properties 对象，Maps 等等。
 - **Bean 的初始化**
 - **更加完整的 Bean 生命周期**，包括 `BeanPostProcessor` 以及 `BeanFactoryPostProcessor` 的各种处理
 - **国际化**，核心类`MessageSource`
 - **事件发布**，基于 `ApplicationEventPublisher`：将 `ApplicationEvent` 或者其他类型的事件，发给所有的 Listener

可以理解为 ApplicationContext 内部包含了一个 BeanFactory，并增加了其他的组件，实现了更丰富的功能，并且，与 BeanFactory 懒加载的方式不同，它是预加载，所以，每一个 Bean 都在 ApplicationContext 启动之后实例化。


![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/7-04.%20%E4%BB%80%E4%B9%88%E6%98%AF%20Spring%20Boot.jpg)

在 ApplicationContext 的基础上，Spring 框架引入了很多特性。其中最常见的就是 Spring Web 程序。在过去，Spring Web 应用程序被嵌入到 servlet 容器中运行，大多数的企业应用都是在 servlet 容器上配置并部署运行的。这对于开发人员来说，又增加了关于对应 servlet 容器的学习曲线，这包括：

 - web.xml 和其他面向 servlet 的配置概念
 - .war 文件目录结构
 - 不同容器的特定配置（例如暴露端口配置，线程配置等等）
 - 复杂的类加载层次
 - 在应用程序之外配置的监控管理相关设施
 - 日志相关
 - 应用程序上下文配置等等

以上配置不同容器并不统一，开发者需要在知道 spring 相关配置的基础上，还要了解容器这些配置特性。这些复杂的配置特性导致学习门槛变高，并且随着技术发展掌握 Servlet 原理的开发者越来越少了。在企业应用开发的时候，应用程序框架越简单，开发人员就越有可能采用该框架。于是，Mike Youngstrom 提出 [Improved support for 'containerless' web application architectures](https://github.com/spring-projects/spring-framework/issues/14521)，意图通过内置 Servlet 容器以及预设加载某些类组成特定的 ApplicationContext，来简化 Spring 应用开发的配置。

Spring Boot，在 ApplicationContext 的基础上，实现了 Spring Boot 特有的 ApplicationContext，并通过添加不同 ApplicationEvent 的 Listener 实现了**特有的生命周期**和**配置**与 **SPI 加载机制**(`spring.factories` 和 `application.properties`)，在此基础上进而实现了如下功能：

1. 内置 servlet 容器，提供了容器的统一抽象，即 `WebServer`。目前包括：Tomcat(`TomcatWebServer`)，Jetty(`JettyWebServer`)，Undertow(`UndertowWebServer`)，Netty(`NettyWebServer`)
2. 不同 servlet 容器的配置都可以用相同的 key 在 application.yml 中配置。例如暴露端口不用再在不同的 servlet 容器中配置，而是直接在 application.yml 中配置 `server.port` 即可。
3. 不再需要构造 war 包部署到 servlet 容器中，而是直接打包成一个 jar 包直接运行。
3. 用户不用关心 ApplicationContext 的创建与管理，而是可以直接使用。
4. 只存在一个 ClassLoader，而不是像 servlet 容器那样有独立的 ClassLoader

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/7-05.%20%E4%BB%80%E4%B9%88%E6%98%AF%20Spring%20Cloud.jpg)

Spring Cloud 在 Spring Boot 的基础上，增加微服务相关组件的接口与实现，不同的 Spring Cloud 体系组件接口与实现不同。但是公共的组件接口在 spring-cloud-commons 这个项目中，其中关于微服务组件的接口包括：
 - 服务注册接口
 - 服务发现接口
 - 负载均衡接口
 - 断路器接口

实现这些接口的组件，会基于 Spring Cloud 的 `NamedContextFactory`，对于不同微服务的调用或者控制，以微服务名称区分，产生不同的子 ApplicationContext。对于这个 `NamedContextFactory`，我们这个系列会专门有一节进行分析。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


我们这一节梳理清楚了从 Bean 到 BeanFactory，在 BeanFactory 基础上封装的 ApplicationContext，以及主要基于注解的 ApplicationContext 以及 Spring factory SPI 的 Spring Boot，以及在 Spring Boot 基础上增加微服务抽象的 Spring Cloud 的这一系列关系。接下来我们会详细分析下 Spring Cloud 中很重要的抽象 - NamedContextFactory

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)