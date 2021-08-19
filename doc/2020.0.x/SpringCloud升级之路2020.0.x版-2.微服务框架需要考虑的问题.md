![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列为之前系列的整理重启版，随着项目的发展以及项目中的使用，之前系列里面很多东西发生了变化，并且还有一些东西之前系列并没有提到，所以重启这个系列重新整理下，欢迎各位留言交流，谢谢！~

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-01.%20%E4%B8%80%E4%B8%AA%E7%AE%80%E5%8D%95%E7%9A%84%E5%BE%AE%E6%9C%8D%E5%8A%A1%E6%9E%B6%E6%9E%84.jpg)

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-01.%20Simple%20MicroService%20Structure.png)

上图中演示了一个非常简单的微服务架构：
 - 微服务会向注册中心进行注册。
 - 微服务从注册中心读取服务实例列表。
 - 基于读取到的服务实例列表，微服务之间互相调用。
 - 外部访问通过统一的 API 网关。
 - API 网关从注册中心读取服务实例列表，根据访问路径调用相应的微服务进行访问。

在这个微服务架构中的每个进程需要实现的功能都在下图中：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-02.%20MicroService%20Component.png)

接下来我们逐个分析这个架构中的每个角色涉及的功能、要考虑的问题以及我们这个系列使用的库。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-02.%20%E6%AF%8F%E4%B8%AA%E5%BE%AE%E6%9C%8D%E5%8A%A1%E7%9A%84%E5%85%AC%E5%85%B1%E7%BB%84%E4%BB%B6.jpg)

每个微服务的基础功能包括：
 - **输出日志**，并且在**日志中输出链路追踪信息**。并且，随着业务压力越来越大，每个进程输出的日志可能越来越多，输出日志可能会成为性能瓶颈，我们这里使用了 **log4j2 异步日志**，并且使用了 **spring-cloud-sleuth 作为链路追踪**的核心依赖。
 - **Http 容器**：提供 Http 接口的容器，分为针对同步的 spring-mvc 以及针对异步的 spring-webflux 的：
   - 对于 spring-mvc，默认的 Http 容器为 Tomcat。在高并发环境下，请求会有很多。我们考虑通过使用直接内存处理请求来减少应用 GC 来优化性能，所以没有使用默认的 Tomcat，而是**使用 Undertow**。
   - 对于 spring-webflux，我们直接使用 **webflux 本身**作为 Http 容器，其实底层就是 reactor-http，再底层其实就是基于 Http 协议的 netty 服务器。本身就是异步响应式的，并且请求内存基本使用了直接内存。
 - **微服务发现与注册**：我们使用了 **Eureka 作为注册中心**。我们的集群平常有很多发布，需要**快速感知实例的上下线**。同时我们有很多套集群，每个集群服务实例节点数量是 100 个左右，如果**每个集群使用一个 Eureka 集群感觉有些浪费**，并且我们希望能有**一个直接管理所有集群节点的管理平台**。所以我们**所有集群使用同一套 Eureka**，但是通过框架配置保证**只有同一集群内的实例互相发现并调用**。
 - **健康检查**：由于 K8s 需要进程提供健康检查接口，我们使用 Spring Boot 的 **actuator** 功能，来作为健康检查接口。同时，我们也通过 Http 暴露了其他 actuator 相关接口，例如动态修改日志级别，热重启等等。
 - **指标采集**：我们通过 **prometheus** 实现进程内部指标采集，并且暴露了 actuator 接口供 grafana 以及 K8s 调用采集。
 - **Http 客户端**：内部微服务调用都是 Http 调用。每个微服务都需要 Http 客户端。在我们这里 Http 客户端有：
   - 对于同步的 spring-mvc，我们一般使用 **Open-feign**，并且每个微服务自己维护自己微服务提供的 Open-feign 客户端。我们一般不使用 `@LoadBalanced` 注解的 `RestTemplate`
   - 对于同步的 spring-flux，一般使用 **`WebClient` 进行调用**。
 - **负载均衡**：很明显，Spring Cloud 中的负载均衡大多是**客户端负载均衡**，我们使用 **spring-cloud-loadbalancer** 作为我们的负载均衡器。
 - **优雅关闭**：我们希望微服务进程在收到关闭信号后，在注册中心标记自己为下线；同时收到的请求全部不处理，返回类似于 503 的状态码；并且在所有线程处理完手头的活之后，再退出，这就是优雅关闭。在 Spring Boot 2.3.x 之后，引入了这个功能，在我们这个系列中也会用到。

另外还会有**重试机制，限流机制以及断路机制**，这里我们先来关心最核心的针对调用其他微服务的 Http 客户端中的这些机制以及需要考虑的问题。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-03.%20Http%20%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%B8%AD%E7%9A%84%E9%87%8D%E8%AF%95%E6%9C%BA%E5%88%B6.jpg)

来看几个场景：

1.在线发布服务的时候，或者某个服务出现问题下线的时候，旧服务实例已经在注册中心下线并且实例已经关闭，但是其他微服务本地有服务实例缓存或者正在使用这个服务实例进行调用，这时候一般会因为无法建立 TCP 连接而抛出一个 `java.io.IOException`，不同框架使用的是这个异常的不同子异常，但是提示信息一般有 `connect time out` 或者 `no route to host`。这时候如果重试，并且重试的实例不是这个实例而是正常的实例，就能调用成功。如下图所示：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-03.%20Connect%20Timeout.png)

**2.当调用一个微服务返回了非 2XX 的响应码**：

**a) 4XX**：在发布接口更新的时候，可能**调用方和被调用方都需要发布**。假设新的接口参数发生变化，没有兼容老的调用的时候，就会有异常，**一般是参数错误，即返回 4XX 的响应码**。例如新的调用方调用老的被调用方。针对这种情况，重试可以解决。但是**为了保险，我们对于这种请求已经发出的，只重试 GET 方法（即查询方法，或者明确标注可以重试的非 GET 方法），对于非 GET 请求我们不重试**。如下图所示：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-04.%204XX.png)

**b) 5XX**：当某个实例发生异常的时候，例如连不上数据库，JVM Stop-the-world 等等，就会有 5XX 的异常。针对这种情况，重试也可以解决。同样为了保险，我们对于**这种请求已经发出的，只重试 GET 方法（即查询方法，或者明确标注可以重试的非 GET 方法），对于非 GET 请求我们不重**试。如下图所示：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-05.%205XX.png)


**3.断路器打开的异常**：后面我们会知道，我们的断路器是针对微服务某个实例某个方法级别的，如果抛出了断路器打开的异常，请求其实并没有发出去，我们可以直接重试。

这些场景在线上在线发布更新的时候，以及流量突然到来导致某些实例出现问题的时候，还是很常见的。如果没有重试，用户会经常看到异常页面，影响用户体验。所以这些场景下的重试还是很必要的。对于重试，我们**使用 resilience4j 作为我们整个框架实现重试机制的核心**。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-04.%20Http%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%B8%AD%E7%9A%84%E9%99%90%E6%B5%81%E4%B8%8E%E9%9A%94%E7%A6%BB%E6%9C%BA%E5%88%B6.jpg)

再看下面一个场景：

微服务 A 通过同一个线程池调用微服务 B 的所有实例。如果有一个实例有问题，阻塞了请求，或者是响应非常慢。那么久而久之，这个线程池会被发送到这个异常实例的请求而占满，但是实际上微服务 B 是有正常工作的实例的。

为了防止这种情况，也为了限制调用每个微服务实例的并发（也就是限流），我们**使用不同线程池调用不同的微服务的不同实例**。这个也是**通过 resilience4j 实现**的。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/2-05.%20Http%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%B8%AD%E7%9A%84%E6%96%AD%E8%B7%AF%E6%9C%BA%E5%88%B6.jpg)

如果一个实例在一段时间内压力过大导致请求慢，或者实例正在关闭，以及实例有问题导致请求响应大多是 500，那么即使我们有重试机制，如果很多请求都是按照请求到有问题的实例 -> 失败 -> 重试其他实例，这样效率也是很低的。这就需要使用**断路器**。

在实际应用中我们发现，大部分异常情况下，是某个微服务的某些实例的某些接口有异常，而这些问题实例上的其他接口往往是可用的。所以我们的断路器**不能直接将这个实例整个断路，更不能将整个微服务断路**。所以，我们使用 **resilience4j** 实现的是**微服务实例方法级别**的断路器（即不同微服务，不同实例的不同方法是不同的断路器）。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


本小节我们提出了一个简单的微服务架构，并仔细分析了其微服务实例的涉及的公共组件使用的库以及需要考虑的问题，并且针对微服务调用的核心 Http 客户端的重试机制，线程隔离机制和断路器机制需要考虑的问题以及如何设计做了较为详细的说明。接下来我们继续分析关于 Eureka 注册中心以及 API 网关设计需要考虑的机制。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)