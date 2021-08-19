![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/14-01.accesslog%20%E7%9B%B8%E5%85%B3%E9%85%8D%E7%BD%AE.jpg)

```
server:
  undertow:
    # access log相关配置
    accesslog:
      # 存放目录，默认为 logs
      dir: ./log
      # 是否开启
      enabled: true
      # 格式，各种占位符后面会详细说明
      pattern: '{
                  "transportProtocol":"%{TRANSPORT_PROTOCOL}",
                  "scheme":"%{SCHEME}",
                  "protocol":"%{PROTOCOL}",
                  "method":"%{METHOD}",
                  "reqHeaderUserAgent":"%{i,User-Agent}",
                  "cookieUserId": "%{c,userId}",
                  "queryTest": "%{q,test}",
                  "queryString": "%q",
                  "relativePath": "%R, %{REQUEST_PATH}, %{RESOLVED_PATH}",
                  "requestLine": "%r",
                  "uri": "%U",
                  "thread": "%I",
                  "hostPort": "%{HOST_AND_PORT}",
                  "localIp": "%A",
                  "localPort": "%p",
                  "localServerName": "%v",
                  "remoteIp": "%a",
                  "remoteHost": "%h",
                  "bytesSent": "%b",
                  "time":"%{time,yyyy-MM-dd HH:mm:ss.S}",
                  "status":"%s",
                  "reason":"%{RESPONSE_REASON_PHRASE}",
                  "respHeaderUserSession":"%{o,userSession}",
                  "respCookieUserId":"%{resp-cookie,userId}",
                  "timeUsed":"%Dms, %Ts, %{RESPONSE_TIME}ms, %{RESPONSE_TIME_MICROS} us, %{RESPONSE_TIME_NANOS} ns",
                }'
      # 文件前缀，默认为 access_log
      prefix: access.
      # 文件后缀，默认为 log
      suffix: log
      # 是否另起日志文件写 access log，默认为 true
      # 目前只能按照日期进行 rotate，一天一个日志文件
      rotate: true
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/14-02.%E6%97%A5%E5%BF%97%E6%96%87%E4%BB%B6%20rotate%20%E7%9B%AE%E5%89%8D%E5%8F%AA%E8%83%BD%E6%8C%89%E7%85%A7%E6%97%A5%E6%9C%9F.jpg)

Undertow 的 accesslog 处理核心类抽象是 `io.undertow.server.handlers.accesslog.AccesslogReceiver`。由于目前 Undertow 的 `AccesslogReceiver` 只有一种实现在使用，也就是 `io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver`。

查看 `DefaultAccessLogReceiver` 的 rotate 时机：

[`DefaultAccessLogReceiver`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/server/handlers/accesslog/DefaultAccessLogReceiver.java)
```
/**
 * 计算 rotate 时间点
 */
private void calculateChangeOverPoint() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    //当前时间日期 + 1，即下一天
    calendar.add(Calendar.DATE, 1);
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    currentDateString = df.format(new Date());
    // if there is an existing default log file, use the date last modified instead of the current date
    if (Files.exists(defaultLogFile)) {
        try {
            currentDateString = df.format(new Date(Files.getLastModifiedTime(defaultLogFile).toMillis()));
        } catch(IOException e){
            // ignore. use the current date if exception happens.
        }
    }
    //rotate 时机是下一天的 0 点
    changeOverPoint = calendar.getTimeInMillis();
}
```

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/14-03.accesslog%20%E5%8D%A0%E4%BD%8D%E7%AC%A6.jpg)

其实 Undertow 中的 accesslog 占位符，就是之前我们提到的 Undertow Listener 解析请求后抽象的 HTTP server exchange 的属性。

[官网文档](https://undertow.io/undertow-docs/undertow-docs-2.1.0/#exchange-attributes)的表格并不是最全的，并且注意点并没有说明，例如某些占位符必须打开某些 Undertow 特性才能使用等等。这里我们列出下。

首先先提出一个注意点，参数占位符，例如 `%{i,你要看的header值}` 查看 header 的某个 key 的值。**逗号后面注意不要有空格，因为这个空格会算入 key 里面导致拿不到你想要的 key**。

## 请求相关属性
| 描述 | 缩写占位符 | 全名占位符 | 参数占位符 | 源码 |
|------|-------|------|------|------|
| 请求传输协议，等价于**请求协议** | 无 | `%{TRANSPORT_PROTOCOL}` | 无 | [`TransportProtocolAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/TransportProtocolAttribute.java) |
| 请求模式，例如 http、https 等 | | `%{SCHEME}` | 无 | [`RequestSchemeAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestSchemeAttribute.java) |
| 请求协议，例如 `HTTP/1.1` 等 | `%H` | `%{PROTOCOL}` | 无 | [`RequestProtocolAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestProtocolAttribute.java) |
| 请求方法，例如 GET、POST 等 | `%m` | `%{METHOD}` | 无 | [`RequestMethodAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestMethodAttribute.java) |
| 请求 Header 的某一个值 | 无 | 无 | `%{i,你要看的header值}` | [`RequestHeaderAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestHeaderAttribute.java)|
| Cookie 的某一个值| 无 | 无 | `%{c,你要看的cookie值}` 或者 `%{req-cookie,你要看的cookie值}` | 分别对应 [`CookieAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/CookieAttribute.java) 和 [`RequestCookieAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestCookieAttribute.java) |
| 路径参数 PathVariable 由于并没有被 Undertow 的 Listener 或者 Handler 解析处理，所以拦截不到，无法确认是否是一个 PathVariable 还是就是 url 路径。所以，**PathVariable 的占位符是不会起作用的**。 | 无 | 无 | `%{p, 你想查看的路径参数 key }` | [`PathParameterAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/PathParameterAttribute.java) |
| 请求参数，即 url 的 ? 之后键值对，这里可以选择查看某个 key 的值。| 无 | 无 | `%{q, 你想查看的请求参数 key}` | [`QueryParameterAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/QueryParameterAttribute.java) |
| 请求参数字符串，即 url 的 ? 之后的所有字符} |`%q`(不包含 ?)|`%{QUERY_STRING}`(不包含 ?);`%{BARE_QUERY_STRING}`(包含 ?)| 无 | [`QueryStringAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/QueryStringAttribute.java) |
|请求相对路径（在 Spring Boot 环境下，大多数情况 RequestPath 和 RelativePath 还有 ResolvedPath 是等价的），即除去 host，port，请求参数字符串的路径 | `%R` | `%{RELATIVE_PATH}` 或者 `%{REQUEST_PATH}` 或者 `%{RESOLVED_PATH}` | 无 | 分别对应 [`RelativePathAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RelativePathAttribute.java) 和 [`RequestPathAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestPathAttribute.java) 和 [`ResolvedPathAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ResolvedPathAttribute.java)|
| 请求整体字符串，包括请求方法，请求相对路径，请求参数字符串，请求协议，例如 `Get /test?a=b HTTP/1.1` | `%r` | `%{REQUEST_LINE}` | 无 | [`RequestLineAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestLineAttribute.java) |
| 请求 URI，包括请求相对路径，请求参数字符串 | `%U` | `%{REQUEST_URL}` | 无 | [`RequestURLAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RequestURLAttribute.java) |
| 处理请求的线程 | `%I` | `%{THREAD_NAME}` | 无 | [`ThreadNameAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ThreadNameAttribute.java) |

注意：

1. 路径参数 PathVariable 由于并没有被 Undertow 的 Listener 或者 Handler 解析处理，所以拦截不到，无法确认是否是一个 PathVariable 还是就是 url 路径。所以，**PathVariable 的占位符是不会起作用的**。

## 请求地址相关

| 描述 | 缩写占位符 | 全名占位符 | 参数占位符 | 源码 |
|------|-------|------|------|------|
|host 和 port，一般就是 HTTP 请求 Header 中的 Host 值，如果 Host 为空则获取本地地址和端口，如果没获取到端口则根据协议用默认端口（http:80,，https:443）| 无 | `%{HOST_AND_PORT}` | 无 | [`HostAndPortAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/HostAndPortAttribute.java) |
|请求本地地址 IP| `%A` | `%{LOCAL_IP}` | 无 | [`LocalIPAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/LocalIPAttribute.java)|
|请求本地端口 Port| `%p` | `%{LOCAL_PORT}` | 无 | [`LocalPortAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/LocalPortAttribute.java)|
|请求本地主机名，一般就是 HTTP 请求 Header 中的 Host 值，如果 Host 为空则获取本地地址 | `%v` | `%{LOCAL_SERVER_NAME}` | 无 | [`LocalServerNameAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/LocalServerNameAttribute.java)|
|请求远程主机名，通过连接获取远端的主机地址 | `%h` | `%{REMOTE_HOST}` | 无 | [`RemoteHostAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RemoteHostAttribute.java)|
|请求远程 IP，通过连接获取远端的 IP | `%a` | `%{REMOTE_IP}` | 无 | [`RemoteIPAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/RemoteIPAttribute.java)|

注意：

1. 请求的远程地址我们一般不从请求连接获取，而是通过 Http Header 里面的 `X-forwarded-for` 或者 `X-real-ip` 等获取，因为现在请求都是通过各种 VPN，负载均衡器发上来的。

## 响应相关属性

| 描述 | 缩写占位符 | 全名占位符 | 参数占位符 | 源码 |
|------|-------|------|------|------|
| 发送的字节数大小，除了 Http Header 以外 | `%b` (如果为空就是 -) 或者 `%B` (如果为空就是 0) | `%{BYTES_SENT}` (如果为空就是 0) | 无 | [`BytesSentAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/BytesSentAttribute.java)|
| accesslog 时间，这个不是收到请求的时间，而是响应的时间 | `%t` | `%{DATE_TIME}` | `%{time, 你自定义的 java 中 SimpleDateFormat 的格式}` | [`DateTimeAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/DateTimeAttribute.java)|
| HTTP 响应状态码 | `%s` | `%{RESPONSE_CODE}` | 无 | [`ResponseCodeAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ResponseCodeAttribute.java)|
| HTTP 响应原因 | 无 | `%{RESPONSE_REASON_PHRASE}` | 无 | [`ResponseReasonPhraseAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ResponseReasonPhraseAttribute.java)|
| 响应 Header 的某一个值 | 无 | 无 | `%{o,你要看的header值}` | [`ResponseHeaderAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ResponseHeaderAttribute.java)|
| 响应 Cookie 的某一个值 | 无 | 无 | `%{resp-cookie,你要看的cookie值}` | [`ResponseCookieAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ResponseCookieAttribute.java)|
| 响应时间，**默认 undertow 没有开启请求时间内统计，需要打开才能统计响应时间** | `%D`(毫秒，例如 56 代表 56ms) `%T`(秒，例如 5.067 代表 5.067 秒) | `%{RESPONSE_TIME}`(等价于 `%D`) `%{RESPONSE_TIME_MICROS}` （微秒） `%{RESPONSE_TIME_NANOS}`（纳秒） | 无 | [`ResponseTimeAttribute`](https://github.com/undertow-io/undertow/blob/2.2.7.Final/core/src/main/java/io/undertow/attribute/ResponseTimeAttribute.java)|

注意：**默认 undertow 没有开启请求时间内统计，需要打开才能统计响应时间**，如何开启呢？通过注册一个 `WebServerFactoryCustomizer` 到 Spring ApplicationContext 中即可。请看下面的代码（项目地址：[https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/](https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/)）：

[`spring.factories`（省略无关代码）](https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-service-common/src/main/resources/META-INF/spring.factories)

```
# AutoConfiguration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
    com.github.hashjang.spring.cloud.iiford.service.common.auto.UndertowAutoConfiguration
```

[`UndertowAutoConfiguration`](https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-service-common/src/main/java/com/github/hashjang/spring/cloud/iiford/service/common/auto/UndertowAutoConfiguration.java)
```
//设置proxyBeanMethods=false，因为没有 @Bean 的方法互相调用需要每次返回同一个 Bean，没必要代理，关闭增加启动速度
@Configuration(proxyBeanMethods = false)
@Import(WebServerConfiguration.class)
public class UndertowAutoConfiguration {
}
```

[`WebServerConfiguration`](https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-service-common/src/main/java/com/github/hashjang/spring/cloud/iiford/service/common/undertow/WebServerConfiguration.java)

```
//设置proxyBeanMethods=false，因为没有 @Bean 的方法互相调用需要每次返回同一个 Bean，没必要代理，关闭增加启动速度
@Configuration(proxyBeanMethods = false)
public class WebServerConfiguration {
    @Bean
    public WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory> undertowWebServerAccessLogTimingEnabler(ServerProperties serverProperties) {
        return new DefaultWebServerFactoryCustomizer(serverProperties);
    }
}
```

[`DefaultWebServerFactoryCustomizer`](https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-service-common/src/main/java/com/github/hashjang/spring/cloud/iiford/service/common/undertow/DefaultWebServerFactoryCustomizer.java)
```
public class DefaultWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory> {

    private final ServerProperties serverProperties;

    public DefaultWebServerFactoryCustomizer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void customize(ConfigurableUndertowWebServerFactory factory) {
        String pattern = serverProperties.getUndertow().getAccesslog().getPattern();
        // 如果 accesslog 配置中打印了响应时间，则打开记录请求开始时间配置
        if (logRequestProcessingTiming(pattern)) {
            factory.addBuilderCustomizers(builder -> builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true));
        }
    }

    private boolean logRequestProcessingTiming(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return false;
        }
        //判断 accesslog 是否配置了查看响应时间
        return pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_MICROS)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_MILLIS)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_NANOS)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_MILLIS_SHORT)
                || pattern.contains(ResponseTimeAttribute.RESPONSE_TIME_SECONDS_SHORT);
    }
}
```

## 其他

还有安全相关的属性（SSL 相关，登录认证 Authentication 相关），微服务内部调用一般用不到，我们这里就不赘述了。
其它内置的属性，在 Spring Boot 环境下一般用不到，我们这里就不讨论了。

## 举例

我们最开始配置的 accesslog 的例子请求返回如下（ JSON 格式化之后的结果）：

```
{
	"transportProtocol": "http/1.1",
	"scheme": "http",
	"protocol": "HTTP/1.1",
	"method": "GET",
	"reqHeaderUserAgent": "PostmanRuntime/7.26.10",
	"cookieUserId": "testRequestCookieUserId",
	"queryTest": "1",
	"queryString": "?test=1&query=2",
	"relativePath": "/test, /test, -",
	"requestLine": "GET /test?test=1&query=2 HTTP/1.1",
	"uri": "/test",
	"thread": "XNIO-2 task-1",
	"hostPort": "127.0.0.1:8102",
	"localIp": "127.0.0.1",
	"localPort": "8102",
	"localServerName": "127.0.0.1",
	"remoteIp": "127.0.0.1",
	"remoteHost": "127.0.0.1",
	"bytesSent": "26",
	"time": "2021-04-08 00:07:50.410",
	"status": "200",
	"reason": "OK",
	"respHeaderUserSession": "testResponseHeaderUserSession",
	"respCookieUserId": "testResponseCookieUserId",
	"timeUsed": "3683ms, 3.683s, 3683ms, 3683149 us, 3683149200 ns",
}
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


我们这一节详细介绍了如何配置 Undertow 的 accesslog，将 accesslog 各种占位符都罗列了出来，用户可以根据这些信息配置出自己想要的 accesslog 信息以及格式。下一节，我们将详细介绍我们框架中针对 Undertow 的定制代码


> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)