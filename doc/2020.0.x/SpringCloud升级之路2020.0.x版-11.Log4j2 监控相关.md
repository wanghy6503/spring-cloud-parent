![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/11-01.%20%E5%A2%9E%E5%8A%A0%E5%BC%82%E6%AD%A5%E6%97%A5%E5%BF%97%20RingBuffer%20%E7%9B%91%E6%8E%A7.jpg)

Log4j2 异步日志核心通过 RingBuffer 实现，如果某一时刻产生大量日志并且写的速度不及时导致 RingBuffer 满了，业务代码中调用日志记录的地方就会阻塞。所以我们需要对 RingBuffer 进行监控。Log4j2 对于每一个 AsyncLogger 配置，都会创建一个独立的 RingBuffer，例如下面的 Log4j2 配置：
```
<!--省略了除了 loggers 以外的其他配置-->
 <loggers>
    <!--default logger -->
    <Asyncroot level="info" includeLocation="true">
        <appender-ref ref="console"/>
    </Asyncroot>
    <AsyncLogger name="RocketmqClient" level="error" additivity="false" includeLocation="true">
        <appender-ref ref="console"/>
    </AsyncLogger>
    <AsyncLogger name="com.alibaba.druid.pool.DruidDataSourceStatLoggerImpl" level="error" additivity="false" includeLocation="true">
        <appender-ref ref="console"/>
    </AsyncLogger>
    <AsyncLogger name="org.mybatis" level="error" additivity="false" includeLocation="true">
        <appender-ref ref="console"/>
    </AsyncLogger>
</loggers>
```
这个配置包含 4 个 AsyncLogger，对于每个 AsyncLogger 都会创建一个 RingBuffer。Log4j2 也考虑到了监控 AsyncLogger 这种情况，所以将 AsyncLogger 的监控暴露成为一个 MBean（JMX Managed Bean）。

相关源码如下：

[`Server.java`](https://github.com/apache/logging-log4j2/blob/master/log4j-core/src/main/java/org/apache/logging/log4j/core/jmx/Server.java)
```
private static void registerLoggerConfigs(final LoggerContext ctx, final MBeanServer mbs, final Executor executor)
        throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

    //获取 log4j2.xml 配置中的 loggers 标签下的所有配置值
    final Map<String, LoggerConfig> map = ctx.getConfiguration().getLoggers();
    //遍历每个 key，其实就是 logger 的 name
    for (final String name : map.keySet()) {
        final LoggerConfig cfg = map.get(name);
        final LoggerConfigAdmin mbean = new LoggerConfigAdmin(ctx, cfg);
        //对于每个 logger 注册一个 LoggerConfigAdmin
        register(mbs, mbean, mbean.getObjectName());
        //如果是异步日志配置，则注册一个 RingBufferAdmin
        if (cfg instanceof AsyncLoggerConfig) {
            final AsyncLoggerConfig async = (AsyncLoggerConfig) cfg;
            final RingBufferAdmin rbmbean = async.createRingBufferAdmin(ctx.getName());
            register(mbs, rbmbean, rbmbean.getObjectName());
        }
    }
}
```

创建的 MBean 的类源码：[`RingBufferAdmin.java`](https://github.com/apache/logging-log4j2/blob/master/log4j-core/src/main/java/org/apache/logging/log4j/core/jmx/RingBufferAdmin.java)
```
public class RingBufferAdmin implements RingBufferAdminMBean {
    private final RingBuffer<?> ringBuffer;
    private final ObjectName objectName;
    //... 省略其他我们不关心的代码
    
    public static final String DOMAIN = "org.apache.logging.log4j2";
    String PATTERN_ASYNC_LOGGER_CONFIG = DOMAIN + ":type=%s,component=Loggers,name=%s,subtype=RingBuffer";
    
    //创建 RingBufferAdmin，名称格式符合 Mbean 的名称格式
    public static RingBufferAdmin forAsyncLoggerConfig(final RingBuffer<?> ringBuffer, 
            final String contextName, final String configName) {
        final String ctxName = Server.escape(contextName);
        //对于 RootLogger，这里 cfgName 为空字符串
        final String cfgName = Server.escape(configName);
        final String name = String.format(PATTERN_ASYNC_LOGGER_CONFIG, ctxName, cfgName);
        return new RingBufferAdmin(ringBuffer, name);
    }
    
    //获取 RingBuffer 的大小
    @Override
    public long getBufferSize() {
        return ringBuffer == null ? 0 : ringBuffer.getBufferSize();
    }
    //获取 RingBuffer 剩余的大小
    @Override
    public long getRemainingCapacity() {
        return ringBuffer == null ? 0 : ringBuffer.remainingCapacity();
    }
    public ObjectName getObjectName() {
        return objectName;
    }
}
```


我们的微服务项目中使用了 spring boot，并且集成了 prometheus。我们可以通过将 Log4j2 RingBuffer 大小作为指标暴露到 prometheus 中，通过如下代码：

对应源码：[`Log4j2Configuration.java`](https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-spring-cloud-common/src/main/java/com/github/hashjang/spring/cloud/iiford/spring/cloud/common/config/Log4j2Configuration.java)

```
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.jmx.RingBufferAdminMBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

@Log4j2
@Configuration(proxyBeanMethods = false)
//需要在引入了 prometheus 并且 actuator 暴露了 prometheus 端口的情况下才加载
@ConditionalOnEnabledMetricsExport("prometheus")
public class Log4j2Configuration {
    @Autowired
    private ObjectProvider<PrometheusMeterRegistry> meterRegistry;
    //只初始化一次
    private volatile boolean isInitialized = false;

    //需要在 ApplicationContext 刷新之后进行注册
    //在加载 ApplicationContext 之前，日志配置就已经初始化好了
    //但是 prometheus 的相关 Bean 加载比较复杂，并且随着版本更迭改动比较多，所以就直接偷懒，在整个 ApplicationContext 刷新之后再注册
    // ApplicationContext 可能 refresh 多次，例如调用 /actuator/refresh，还有就是多 ApplicationContext 的场景
    // 这里为了简单，通过一个简单的 isInitialized 判断是否是第一次初始化，保证只初始化一次
    @EventListener(ContextRefreshedEvent.class)
    public synchronized void init() {
        if (!isInitialized) {
            //通过 LogManager 获取 LoggerContext，从而获取配置
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            org.apache.logging.log4j.core.config.Configuration configuration = loggerContext.getConfiguration();
            //获取 LoggerContext 的名称，因为 Mbean 的名称包含这个
            String ctxName = loggerContext.getName();
            configuration.getLoggers().keySet().forEach(k -> {
                try {
                    //针对 RootLogger，它的 cfgName 是空字符串，为了显示好看，我们在 prometheus 中将它命名为 root
                    String cfgName = StringUtils.isBlank(k) ? "" : k;
                    String gaugeName = StringUtils.isBlank(k) ? "root" : k;
                    Gauge.builder(gaugeName + "_logger_ring_buffer_remaining_capacity", () ->
                    {
                        try {
                            return (Number) ManagementFactory.getPlatformMBeanServer()
                                    .getAttribute(new ObjectName(
                                            //按照 Log4j2 源码中的命名方式组装名称
                                            String.format(RingBufferAdminMBean.PATTERN_ASYNC_LOGGER_CONFIG, ctxName, cfgName)
                                            //获取剩余大小，注意这个是严格区分大小写的
                                    ), "RemainingCapacity");
                        } catch (Exception e) {
                            log.error("get {} ring buffer remaining size error", k, e);
                        }
                        return -1;
                    }).register(meterRegistry.getIfAvailable());
                } catch (Exception e) {
                    log.error("Log4j2Configuration-init error: {}", e.getMessage(), e);
                }
            });
            isInitialized = true;
        }
    }
}
```

增加这个代码之后，请求 `/actuator/prometheus` 之后，可以看到对应的返回：
```
//省略其他的
# HELP root_logger_ring_buffer_remaining_capacity  
# TYPE root_logger_ring_buffer_remaining_capacity gauge
root_logger_ring_buffer_remaining_capacity 262144.0
# HELP org_mybatis_logger_ring_buffer_remaining_capacity  
# TYPE org_mybatis_logger_ring_buffer_remaining_capacity gauge
org_mybatis_logger_ring_buffer_remaining_capacity 262144.0
# HELP com_alibaba_druid_pool_DruidDataSourceStatLoggerImpl_logger_ring_buffer_remaining_capacity  
# TYPE com_alibaba_druid_pool_DruidDataSourceStatLoggerImpl_logger_ring_buffer_remaining_capacity gauge
com_alibaba_druid_pool_DruidDataSourceStatLoggerImpl_logger_ring_buffer_remaining_capacity 262144.0
# HELP RocketmqClient_logger_ring_buffer_remaining_capacity  
# TYPE RocketmqClient_logger_ring_buffer_remaining_capacity gauge
RocketmqClient_logger_ring_buffer_remaining_capacity 262144.0
```
这样，当这个值为 0 持续一段时间后（就代表 RingBuffer 满了，日志生成速度已经远大于消费写入 Appender 的速度了），我们就认为这个应用日志负载过高了。

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/11-02.%20%E9%80%9A%E8%BF%87%20actuator%20%E6%9F%A5%E7%9C%8B%E5%B9%B6%E5%8A%A8%E6%80%81%E4%BF%AE%E6%94%B9%E6%97%A5%E5%BF%97%E9%85%8D%E7%BD%AE.jpg)

其实可以通过 JMX 直接查看动态修改 Log4j2 的各种配置，Log4j2 中暴露了很多 JMX Bean，例如通过 JConsole 可以查看并修改：
![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E9%97%AE%E9%A2%98%E5%AE%9A%E4%BD%8D/%E4%B8%80%E6%AC%A1%E9%9E%AD%E8%BE%9F%E5%85%A5%E9%87%8C%E7%9A%84%20Log4j2%20%E6%97%A5%E5%BF%97%E8%BE%93%E5%87%BA%E9%98%BB%E5%A1%9E%E9%97%AE%E9%A2%98%E7%9A%84%E5%AE%9A%E4%BD%8D/ring_buffer_admin.png)

但是，JMX 里面包含的信息太多，并且我们的服务器在世界各地，远程 JMX 很不稳定，所以我们还是通过 actuator 暴露 http 接口进行操作。

首先，要先配置 actuator 要通过 HTTP 暴露出日志 API，我们这里的配置是：
```
management:
  endpoints:
    # 不通过 JMX 暴露任何 actuator 接口
    jmx:
      exposure:
        exclude: '*'
    # 通过 JMX 暴露所有 actuator 接口
    web:
      exposure:
        include: '*'
```

请求接口 `GET /actuator/loggers`，可以看到如下的返回，可以知道当前日志框架支持哪些级别的日志配置，以及每个 Logger 的级别配置。

```
{
	"levels": [
		"OFF",
		"FATAL",
		"ERROR",
		"WARN",
		"INFO",
		"DEBUG",
		"TRACE"
	],
	"loggers": {
		"ROOT": {
			"configuredLevel": "WARN",
			"effectiveLevel": "WARN"
		},
		"org.mybatis": {
			"configuredLevel": "ERROR",
			"effectiveLevel": "ERROR"
		}
	},
	"groups": {
	}
}
```

如果我们想增加或者修改某一 Logger 的配置，可以通过 `POST /actuator/loggers/自定义logger名称`，Body 为：
```
{
	"configuredLevel": "WARN"
}
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


我们这一节详细分析了我们微服务框架中日志相关的各种配置，包括基础配置，链路追踪实现与配置以及如果没有链路追踪信息时候的解决办法，并且针对一些影响性能的核心配置做了详细说明。然后针对日志的 RingBuffer 监控做了个性化定制，并且说明了通过 actuator 查看并动态修改日志配置。下一节我们将会开始分析基于 spring-mvc 同步微服务使用的 web 容器 - Undertow。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)