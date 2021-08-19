![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

# 集群结构

我们的业务集群结构是这样的：
 - 不同 Region，使用不同的 Eureka 集群管理，不同 Region 之间不互相访问。
 - 同一 Region 内，可能有不同的业务集群，不同业务集群之间也不互相访问，共用同一套业务集群。
 - 同一业务集群内可以随意访问，同时同一业务集群会做跨可用区的容灾。
 - 在我们这里的抽象中，**zone 代表不同集群，而不是实际的不同可用区**。



在这里，我们提供一个 Eureka Server 的集群模板，供大家参考。

# 启动 Eureka Server




> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)