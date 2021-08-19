![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列为之前系列的整理重启版，随着项目的发展以及项目中的使用，之前系列里面很多东西发生了变化，并且还有一些东西之前系列并没有提到，所以重启这个系列重新整理下，欢迎各位留言交流，谢谢！~

我们实现的 Spring Cloud 微服务框架，里面运用了许多 Spring Cloud 组件，并且对于某些组件进行了个性化改造。那么对于某个 Spring Cloud 组件，我们一般是如何入手理解其中的原理呢？以及如何知道其中的扩展点呢？一般从下面两个方面入手：

1. 通过 spring-boot SPI 机制查看模块的扩展点
2. 查看该模块实现的 NamedContextFactory

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/9-01.%20spring.factories%20SPI%20%E6%9C%BA%E5%88%B6.jpg)

`spring-core` 项目中提供了 Spring 框架多种 SPI 机制，其中一种非常常用并灵活运用在了 Spring-boot 的机制就是基于 `spring.factories` 的 SPI 机制。

那么什么是 SPI（Service Provider）呢？ 在系统设计中，为了模块间的协作，往往会设计统一的接口供模块之间的调用。面向的对象的设计里，我们一般推荐模块之间基于接口编程，**模块之间不对实现类进行硬编码**，而是将指定哪个实现置于程序之外指定。Java 中默认的 SPI 机制就是通过 `ServiceLoader` 来实现，简单来说就是通过在`META-INF/services`目录下新建一个名称为接口全限定名的文件，内容为接口实现类的全限定名，之后程序通过代码:
```
//指定加载的接口类，以及用来加载类的类加载器，如果类加载器为 null 则用根类加载器加载
ServiceLoader<SpiService> serviceLoader = ServiceLoader.load(SpiService.class, someClassLoader);
Iterator<SpiService> iterator = serviceLoader.iterator();
while (iterator.hasNext()){
    SpiService spiService = iterator.next();
}
```
获取指定的实现类。

在 Spring 框架中，这个类是`SpringFactoriesLoader`，需要在`META-INF/spring.factories`文件中指定接口以及对应的实现类，例如 Spring Cloud Commons 中的：
```
# Environment Post Processors
org.springframework.boot.env.EnvironmentPostProcessor=\
org.springframework.cloud.client.HostInfoEnvironmentPostProcessor
```
其中指定了`EnvironmentPostProcessor`的实现`HostInfoEnvironmentPostProcessor`。

同时，Spring Boot 中会通过`SpringFactoriesLoader.loadXXX`类似的方法读取所有的`EnvironmentPostProcessor`的实现类并生成 Bean 到 ApplicationContext 中：

[`EnvironmentPostProcessorApplicationListener`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/env/EnvironmentPostProcessorApplicationListener.java)
```
//这个类也是通过spring.factories中指定ApplicationListener的实现而实现加载的，这里省略
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {
    //创建这个Bean的时候，会调用
    public EnvironmentPostProcessorApplicationListener() {
		this(EnvironmentPostProcessorsFactory
				.fromSpringFactories(EnvironmentPostProcessorApplicationListener.class.getClassLoader()));
	}
}
```
[`EnvironmentPostProcessorsFactory`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/env/EnvironmentPostProcessorsFactory.java)
```
static EnvironmentPostProcessorsFactory fromSpringFactories(ClassLoader classLoader) {
	return new ReflectionEnvironmentPostProcessorsFactory(
	        //通过 SpringFactoriesLoader.loadFactoryNames 获取文件中指定的实现类并初始化
			SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class, classLoader));
}
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/9-02.%20spring.factories%20%E7%9A%84%E7%89%B9%E6%AE%8A%E4%BD%BF%E7%94%A8%20-%20EnableAutoConfiguration.jpg)

`META-INF/spring.factories` 文件中不一定指定的是接口以及对应的实现类，例如：

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration,\
org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration,\
```
其中`EnableAutoConfiguration`是一个注解，`LoadBalancerAutoConfiguration`与`BlockingLoadBalancerClientAutoConfiguration`都是配置类并不是`EnableAutoConfiguration`的实现。那么这个是什么意思呢？`EnableAutoConfiguration`是一个注解，`LoadBalancerAutoConfiguration`与`BlockingLoadBalancerClientAutoConfiguration`都是配置类。`spring.factories`这里是另一种特殊使用，记录要载入的 Bean 类。`EnableAutoConfiguration`在注解被使用的时候，这些 Bean 会被加载。这就是`spring.factories`的另外一种用法。

`EnableAutoConfiguration`是 Spring-boot 自动装载的核心注解。有了这个注解，Spring-boot 就可以自动加载各种`@Configuration`注解的类。那么这个机制是如何实现的呢？

来看下`EnableAutoConfiguration`的源码
[`EnableAutoConfiguration`](https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/EnableAutoConfiguration.java)
```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
	String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";
	//排除的类
	Class<?>[] exclude() default {};
	//排除的Bean名称
	String[] excludeName() default {};
}
```
我们看到了有 `@Import` 这个注解。这个注解是 Spring 框架的一个很常用的注解，是 Spring 基于 Java 注解配置的主要组成部分。

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/9-03.%20%E9%80%9A%E8%BF%87%E6%A8%A1%E5%9D%97%E7%9A%84%20spring.factories%20%E6%9F%A5%E7%9C%8B%E6%A8%A1%E5%9D%97%E7%9A%84%E8%87%AA%E5%8A%A8%E9%85%8D%E7%BD%AE.jpg)

1. 查看 jar 包的 META-INF/spring.factories
3. 查看里面的内容，尤其关注 org.springframework.boot.autoconfigure.EnableAutoConfiguration= 自动加载的配置类
4. 查看自动加载的配置类，关注哪些 Bean 可以扩展（例如，包含@ConditionalOnMissingBean 注解的 Bean）

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/9-04.%20%E6%AF%8F%E4%B8%AA%E5%BE%AE%E6%9C%8D%E5%8A%A1%E7%8B%AC%E7%AB%8B%E7%9A%84%20NamedContextFactory.jpg)

我们一般想个性化定制都是针对调用不同微服务不同的 Bean 配置，所以其实要重点关注的就是这个模块扩展的 NamedContextFactory：

1. 寻找这个组件扩展 NamedContextFactory 的类
2. 查看类的源代码，查看默认配置是什么类，以及 Specification 是什么类，以及如何获取当前微服务的名称。
3. 根据默认配置类，查看里面的 Bean 有哪些，并且哪些可以被替换（例如，包含@ConditionalOnMissingBean 注解的 Bean）
4. 根据 Specification 查看扩展配置的方式

我们这里拿 spring-cloud-loadbalancer 举一个简单例子，即：

spring-cloud-loadbalancer 中扩展 NamedContextFactory 的类是 LoadBalancerClientFactory，查看 LoadBalancerClientFactory 的代码可以知道：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/9-05.%20Spring%20Cloud%20Loadbaclancer.png)

1. 可以通过 `loadbalancer.client.name` 这个属性获取当前要创建的 Bean 是哪个微服务的
2. 可以知道默认配置是 `LoadBalancerClientConfiguration`，再查看它里面的源代码我们可以知道主要初始化两个 Bean：
   1. ReactorLoadBalancer，负载均衡器，因为有 `@ConditionalOnMissingBean` 所以可以被替换，这就是我们的扩展点
   2. ServiceInstanceSupplier，提供实例信息的 Supplier，因为有 `@ConditionalOnMissingBean` 所以可以被替换，这就是我们的扩展点
3. Specification 为 LoadBalancerSpecification，再分析其调用可以知道，可以通过 `@LoadBalancerClient` 和 `@LoadBalancerClients` 在 `LoadBalancerClientConfiguration` 的基础上额外指定配置。


![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


我们这一节详细分析了如何使用以及分析改造一个 Spring Cloud 组件。下一节我们将开始具体分析我们实现的微服务框架的每一块功能。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)