![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/19-01.Eureka%20Server%20%E9%85%8D%E7%BD%AE.jpg)

Eureka Server 配置是 Eureka Server 需要的一些配置，包括之前多次提到的定时检查实例过期的配置，自我保护相关的配置，同一 zone 内集群相关的配置和跨 zone 相关的配置。在 Spring Cloud 中，Eureka 客户端配置以 `eureka.server` 开头，对应配置类为 [`EurekaServerConfigBean`](https://github.com/spring-cloud/spring-cloud-netflix/blob/main/spring-cloud-netflix-eureka-server/src/main/java/org/springframework/cloud/netflix/eureka/server/EurekaServerConfigBean.java)

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/19-02.Eureka%20Server%20%E9%85%8D%E7%BD%AE%20-%20%E5%AE%9A%E6%97%B6%E6%A3%80%E6%9F%A5%E5%AE%9E%E4%BE%8B%E8%BF%87%E6%9C%9F%E7%9B%B8%E5%85%B3%E9%85%8D%E7%BD%AE.jpg)

实例注册后需要发送心跳证明这个实例是活着的，Eureka Server 中也有定时任务检查实例是否已经过期。

```
eureka:
    server:
      #主动检查服务实例是否失效的任务执行间隔，默认是 60s
      eviction-interval-timer-in-ms: 3000
      #这个配置在两个地方被使用：
      #如果启用用了自我保护，则会 renewal-threshold-update-interval-ms 指定的时间内，收到的心跳请求个数是否小于实例个数乘以这个 renewal-percent-threshold
      #定时任务检查过期实例，每次最多过期 1 - renewal-percent-threshold 这么多比例的实例
      renewal-percent-threshold: 0.85
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/19-03.Eureka%20Server%20%E9%85%8D%E7%BD%AE%20-%20%E8%87%AA%E6%88%91%E4%BF%9D%E6%8A%A4%E7%9B%B8%E5%85%B3%E9%85%8D%E7%BD%AE.jpg)

服务器中有定时过期的任务，检查迟迟没有心跳的实例，并注销他们。自我保护主要针对集群中**网络出现问题**，导致有**很多实例无法发送心跳**导致很多实例状态异常，但是**实际实例还在正常工作**的情况，不要让这些实例不参与负载均衡。

```
eureka:
    server: 
	  #注意，最好所有的客户端实例配置的心跳时间相关的配置，是相同的。这样使用自我保护的特性最准确。
      #关闭自我保护
      #我们这里不使用自我保护，因为：
      #自我保护主要针对集群中网络出现问题，导致有很多实例无法发送心跳导致很多实例状态异常，但是实际实例还在正常工作的情况，不要让这些实例不参与负载均衡
      #启用自我保护的情况下，就会停止对于实例的过期
      #但是，如果出现这种情况，其实也代表很多实例无法读取注册中心了。
      #并且还有一种情况就是，Eureka 重启。虽然不常见，但是对于镜像中其他的组件更新我们还是很频繁的
      #我倾向于从客户端对于实例缓存机制来解决这个问题，如果返回实例列表为空，则使用上次的实例列表进行负载均衡，这样既能解决 Eureka 重启的情况，又能处理一些 Eureka 网络隔离的情况
	  #自我保护模式基于每分钟需要收到 renew （实例心跳）请求个数，如果启用了自我保护模式，只有上一分钟接收到的 renew 个数，大于这个值，实例过期才会被注销
      enable-self-preservation: false
	  # 每分钟需要收到 renew （实例心跳）请求个数是需要动态刷新的，这个刷新间隔就是 renewal-threshold-update-interval-ms
	  #更新流程大概是：计算当前一共有多少实例，如果大于之前期望的实例量 * renewal-percent-threshold（或者没开启自我保护模式）,则更新期望的实例数量为当前一共有多少实例
      #之后根据期望的实例数量，计算期望需要收到的实例心跳请求个数 = 期望的实例数量 * （60 / expected-client-renewal-interval-seconds） * renewal-percent-threshold
      #公式中 60 代表一分钟，因为公式用到了 expected-client-renewal-interval-seconds，也就是实例平均心跳间隔，为了使这个公式准确，最好每个实例配置一样的心跳时间
      #默认 900000ms = 900s = 15min
	  renewal-threshold-update-interval-ms: 900000
	  #上面提到的实例平均心跳间隔，或者说是期望的心跳间隔，为了使这个公式准确，最好每个实例配置一样的心跳时间
      #默认 30s
      expected-client-renewal-interval-seconds: 30
	  #这个配置在两个地方被使用：
      #如果启用用了自我保护，则会 renewal-threshold-update-interval-ms 指定的时间内，收到的心跳请求个数是否小于实例个数乘以这个 renewal-percent-threshold
      #定时任务检查过期实例，每次最多过期 1 - renewal-percent-threshold 这么多比例的实例
      renewal-percent-threshold: 0.85
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/19-04.Eureka%20Server%20%E9%85%8D%E7%BD%AE%20-%20%E5%90%8C%E4%B8%80%E5%8C%BA%E5%9F%9F%E5%86%85%E9%9B%86%E7%BE%A4%E9%85%8D%E7%BD%AE%E7%9B%B8%E5%85%B3.jpg)

上面我们提到了，同一区域内的 Eureka 服务器实例，收到的客户端请求，会转发到同一区域内的的其他 Eureka 服务器实例。同时，在某一 Eureka 服务器实例启动的时候，会从同一区域内其他 Eureka 服务器同步实例列表。并且，转发到其他 Eureka 服务器实例是异步转发的，这就有专门的线程池进行转发。同时，转发的也是 HTTP 请求，这就需要 HTTP 连接池：

```
eureka:
    server: 
	  #Eureka Server 从配置中更新同一区域内的其他 Eureka Server 实例列表间隔，默认10分钟
      peer-eureka-nodes-update-interval-ms: 600000
	  #启动时从其他 Eureka Server 同步服务实例信息的最大重试次数，直到实例个数不为 0，默认为 0，这样其实就是不同步
      registry-sync-retries: 0
      #启动时从其他 Eureka Server 同步服务实例信息重试间隔
      registry-sync-retry-wait-ms: 30000
	  #集群内至少有多少个 UP 的 Eureka Server 实例数量，当前 Eureka Server 状态为 UP。默认 -1，也就是 Eureka Server 状态不考虑 UP 的集群内其他 Eureka Server 数量。
      min-available-instances-for-peer-replication: -1
	  #请求其他实例任务的最大超时时间，默认 30 秒
      max-time-for-replication: 30000
	  #用来处理同步任务的线程数量，有两个线程池，一个处理批量同步任务，默认大小为20
      max-threads-for-peer-replication: 20
      #另一个处理非批量任务（如果没用 AWS Autoscaling 对接相关特性则没有啥用），默认大小为20
      max-threads-for-status-replication: 20
      #处理批量任务的线程池队列长度，默认为 10000
      max-elements-in-peer-replication-pool: 10000
      #处理非批量任务的线程池队列长度，默认为 10000
      max-elements-in-status-replication-pool: 10000
	  #Eureka Server 通过 httpclient 访问其他 Eureka Server 同步实例，httpclient 的连接超时，默认 200ms
      peer-node-connect-timeout-ms: 200
      #httpclient 的读取超时，默认 200ms，一般不用太长
      peer-node-read-timeout-ms: 200
      #httpclient 的最大总连接数量，默认 1000
      peer-node-total-connections: 1000
      #httpclient 的对于某一 host 最大总连接数量，默认 500
      peer-node-total-connections-per-host: 500
      #httpclient 的连接空闲保持时间，默认 30s
      peer-node-connection-idle-timeout-seconds: 30
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/19-05.Eureka%20Server%20%E9%85%8D%E7%BD%AE%20-%20%E8%B7%A8%E5%8C%BA%E5%9F%9F%E7%9B%B8%E5%85%B3%E9%85%8D%E7%BD%AE.jpg)

Eureka 服务器会定时拉取其他区域的服务实例列表缓存在本地。在查询本地查询不到某个微服务的时候，就会查询这个远程区域服务实例的缓存。相关配置如下：
```
eureka:
    server: 
      #请求其他 Region 的 httpclient 的连接超时，默认 1000ms
      remote-region-connect-timeout-ms: 1000
      #请求其他 Region 的 httpclient 的读取超时，默认 1000ms
      remote-region-read-timeout-ms: 1000
      #请求其他 Region 的 httpclient 的最大总连接数量，默认 1000
      remote-region-total-connections: 1000
      #请求其他 Region 的 httpclient 的对于某一 host 最大总连接数量，默认 500
      remote-region-total-connections-per-host: 500
      #请求其他 Region 的 httpclient 的连接空闲保持时间，默认 30s
      remote-region-connection-idle-timeout-seconds: 30
      #请求其他 Region 的 http 请求是否开启 gzip，对于其他 Region 我们认为网络连接是比较慢的，所以默认开启压缩
      g-zip-content-from-remote-region: true
      #    remote-region-urls-with-name:
      #      region2eureka1: http://127:0:0:1:8212/eureka/
      #      region2eureka2: http://127:0:0:1:8213/eureka/
      #    remote-region-app-whitelist:
      #如果需要从其他 Region 获取实例信息，这个获取间隔，默认为 30s
      remote-region-registry-fetch-interval: 30
      #如果需要从其他 Region 获取实例信息，这个任务的线程池，默认为 20个
      remote-region-fetch-thread-pool-size: 20
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)

我们这一节详细分析了 Eureka Server 相关的配置。下一节，我们将给大家提供一个配置模板，启动一个 Eureka Server 集群。


> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)