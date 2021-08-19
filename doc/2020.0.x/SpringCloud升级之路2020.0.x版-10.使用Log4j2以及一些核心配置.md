![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/10-01.%20%E4%BD%BF%E7%94%A8%E5%BC%82%E6%AD%A5%E6%97%A5%E5%BF%97.jpg)

我们使用 Log4j2 异步日志配置，防止日志过多的时候，成为性能瓶颈。这里简单说一下 Log4j2 异步日志的原理：Log4j2 异步日志基于高性能数据结构 Disruptor，Disruptor 是一个环形 buffer，做了很多性能优化（具体原理可以参考我的另一系列：[高并发数据结构（disruptor）](https://blog.csdn.net/zhxdick/category_6121943.html)），Log4j2 对此的应用如下所示：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E9%97%AE%E9%A2%98%E5%AE%9A%E4%BD%8D/%E4%B8%80%E6%AC%A1%E9%9E%AD%E8%BE%9F%E5%85%A5%E9%87%8C%E7%9A%84%20Log4j2%20%E6%97%A5%E5%BF%97%E8%BE%93%E5%87%BA%E9%98%BB%E5%A1%9E%E9%97%AE%E9%A2%98%E7%9A%84%E5%AE%9A%E4%BD%8D/log4j2-disruptor.png)

简单来说，多线程通过 log4j2 的门面类 `org.apache.logging.log4j.Logger` 进行日志输出，被封装成为一个 `org.apache.logging.log4j.core.LogEvent`，放入到 Disruptor 的环形 buffer 中。在消费端有一个单线程消费这些 LogEvent 写入对应的 Appender.

这里我们给出一个我们日志配置的模板，供大家参考：

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <Properties>
        <Property name="springAppName">app名称</Property>
        <Property name="LOG_ROOT">log</Property>
        <Property name="LOG_DATEFORMAT_PATTERN">yyyy-MM-dd HH:mm:ss.SSS</Property>
        <Property name="LOG_EXCEPTION_CONVERSION_WORD">%xwEx</Property>
        <!--对于日志级别，为了日志能对齐好看，我们占 5 个字符-->
        <Property name="LOG_LEVEL_PATTERN">%5p</Property>
        <Property name="logFormat">
            %d{${LOG_DATEFORMAT_PATTERN}} ${LOG_LEVEL_PATTERN} [${springAppName},%X{traceId},%X{spanId}] [${sys:PID}] [%t][%C:%L]: %m%n${sys:LOG_EXCEPTION_CONVERSION_WORD}
        </Property>
    </Properties>
    <appenders>
        <RollingFile name="file" append="true"
                     filePattern="${LOG_ROOT}/app.log-%d{yyyy.MM.dd.HH}"
                     immediateFlush="false">
            <PatternLayout pattern="${logFormat}"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
            </Policies>
            <DirectWriteRolloverStrategy maxFiles="72"/>
        </RollingFile>
    </appenders>
    <loggers>
        <!--default logger -->
        <Asyncroot level="info" includeLocation="true">
            <appender-ref ref="file" />
        </Asyncroot>
        <AsyncLogger name="org.mybatis" level="off" additivity="false" includeLocation="false">
            <appender-ref ref="file"/>
        </AsyncLogger>
    </loggers>
</configuration>

```
对于其中一些重要的配置，我们这里单独拿出来分析下。

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/10-02.%20%E5%9C%A8%E6%97%A5%E5%BF%97%E4%B8%AD%E5%A2%9E%E5%8A%A0%E9%93%BE%E8%B7%AF%E8%BF%BD%E8%B8%AA%E4%BF%A1%E6%81%AF.jpg)

我们项目的依赖中包含了 spring-cloud-sleuth 这个链路追踪相关的依赖，其核心基于 Opentracing 标准实现。日志中可以通过打印 Span 的 SpanContext 中的 traceId 以及 spanId，就能通过这些信息，确定日志中的一条完整链路。spring-cloud-sleuth 是如何将这些信息放入日志中的呢？ Log4j2 中有这样一个抽象，即 `org.apache.logging.log4j.ThreadContext`，这个其实就是 Java 日志中 MDC（Mapped Diagnostic Context）的实现，可以理解成是一个线程本地的 Map，每个线程可以将日志需要的元素放入这个 ThreadContext 中，这样这个线程在打印日志的时候，就可以从这个 ThreadContext 中取出放入日志内容。日志需要有对应的占位符，例如下面这个就是将 ThreadContext 中 key 为 traceId 以及 spanId 的值取出输出：
```
%X{traceId},%X{spanId}
```
**Spring Cloud 2020.0.x 之后**，也就是 spring-cloud-sleuth 3.0.0 之后，放入 `ThreadContext` 的 key 发生了变化，原来的 traceId 与 spanId 分别是 `X-B3-traceId` 与 `X-B3-spanId`，现在改成了更为通用的 `traceId` 和 `spanId`。

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/10-03.%20%E5%A6%82%E4%BD%95%E8%A7%A3%E5%86%B3%E6%97%A5%E5%BF%97%E4%B8%AD%E6%B2%A1%E6%9C%89%E9%93%BE%E8%B7%AF%E8%BF%BD%E8%B8%AA%E4%BF%A1%E6%81%AF%E7%9A%84%E9%97%AE%E9%A2%98.jpg)

这个主要因为你打日志的地方不在 spring-cloud-sleuth 管理的范围内，或者是 Span 提前结束了。这种时候，你可以在确定有 Span 的地方将 Span 缓存起来，之后再没有链路追踪信息的地方使用这个 Span，例如：

```
import brave.Tracer;

@Autowire
private Tracer tracer;

//在确定有 span 的地方获取当前 span 将 span 缓存起来
Span span = tracer.currentSpan();

//之后在没有链路追踪信息的地方，使用 span 包裹起来
try (Tracer.SpanInScope cleared = tracer.withSpanInScope(span)) {
    //你的业务代码
}
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/10-04.%20%E4%B8%BA%E4%BD%95%E5%85%B3%E9%97%AD%20includeLocation.jpg)

设置 `includeLocation=false`，这样在日志中就无法看到日志属于的代码以及行数了。获取这个代码行数，其实是通过获取当前调用堆栈实现的。Java 9 之前是通过 new 一个 Exception 获取堆栈，Java 9 之后是通过 StackWalker。**两者其实都有性能问题，在高并发的情况下，会吃掉很多 CPU，得不偿失**。所以我推荐，**在日志内容中直接体现所在代码行数**，就不通过这个 includeLocation 获取当前堆栈从而获取代码行数了。

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/10-05.%20%E4%B8%BA%E4%BD%95%E5%85%B3%E9%97%AD%20immediateFlush.jpg)

关闭 immediateFlush，可以减少硬盘 IO，会先写入内存 Buffer（默认是 8 KB），之后**在 RingBuffer 目前消费完或者 Buffer 写满的时候才会刷盘**。这个 Buffer 可以通过系统变量 `log4j.encoder.byteBufferSize` 改变。

这里的原理对应源码：

[`AbstractOutputStreamAppender.java`](https://github.com/apache/logging-log4j2/blob/master/log4j-core/src/main/java/org/apache/logging/log4j/core/appender/AbstractOutputStreamAppender.java)
```
protected void directEncodeEvent(final LogEvent event) {
    getLayout().encode(event, manager);
    //如果配置了 immdiateFlush （默认为 true）或者当前事件是 EndOfBatch
    if (this.immediateFlush || event.isEndOfBatch()) {
        manager.flush();
    }
}
```
那么对于 Log4j2 Disruptor 异步日志来说，什么时候 `LogEvent` 是 `EndOfBatch` 呢？是在消费到的 index 等于生产发布到的最大 index 的时候，这也是比较符合性能设计考虑，即在没有消费完的时候，尽可能地不 flush，消费完当前所有的时候再去 flush：

[`BatchEventProcessor.java`](https://github.com/LMAX-Exchange/disruptor/blob/master/src/main/java/com/lmax/disruptor/BatchEventProcessor.java)
```
private void processEvents()
{
    T event = null;
    long nextSequence = sequence.get() + 1L;

    while (true)
    {
        try
        {
            final long availableSequence = sequenceBarrier.waitFor(nextSequence);
            if (batchStartAware != null)
            {
                batchStartAware.onBatchStart(availableSequence - nextSequence + 1);
            }

            while (nextSequence <= availableSequence)
            {
                event = dataProvider.get(nextSequence);
                //这里 nextSequence == availableSequence 就是 EndOfBatch
                eventHandler.onEvent(event, nextSequence, nextSequence == availableSequence);
                nextSequence++;
            }

            sequence.set(availableSequence);
        }
        catch (final TimeoutException e)
        {
            notifyTimeout(sequence.get());
        }
        catch (final AlertException ex)
        {
            if (running.get() != RUNNING)
            {
                break;
            }
        }
        catch (final Throwable ex)
        {
            exceptionHandler.handleEventException(ex, nextSequence, event);
            sequence.set(nextSequence);
            nextSequence++;
        }
    }
}

```


![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


我们这一节详细分析了我们微服务框架中日志相关的各种配置，包括基础配置，链路追踪实现与配置以及如果没有链路追踪信息时候的解决办法，并且针对一些影响性能的核心配置做了详细说明。下一节我们将会开始分析针对日志的 RingBuffer 进行的监控。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)