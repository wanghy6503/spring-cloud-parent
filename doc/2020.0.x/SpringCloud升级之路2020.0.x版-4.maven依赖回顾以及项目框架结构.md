![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/4-01.%20%20maven%20%E4%BE%9D%E8%B5%96%E7%9A%84%E6%9C%80%E7%9F%AD%E8%B7%AF%E5%BE%84%E5%8E%9F%E5%88%99%E5%9B%9E%E9%A1%BE.jpg)

我们先来回顾下 maven 依赖中一个重要原则：最短路径原则。这在之后我们的使用中会经常用到。

举一个例子，假设我们以 `spring-boot-parent` 作为 parent：
```
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.0.9</version>
</parent>
```
我们想用想用 `elasticsearch` 作为搜索引擎，在项目中添加了依赖

```
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>7.10.2</version>
</dependency>
```

写好代码，一跑，报类不存在异常：
```
 java.lang.NoClassDefFoundError: org/elasticsearch/common/xcontent/DeprecationHandler
    at com.lv.springboot.datasource.ClientUTis.main(ClientUTis.java:13)
Caused by: java.lang.ClassNotFoundException: org.elasticsearch.common.xcontent.DeprecationHandler
    at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
    at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:331)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
    ... 1 more
```
看下依赖`mvn dependency:tree`，发现依赖的`elasticsearch`版本是：
```
org.elasticsearch.client:elasticsearch-rest-high-level-client:7.0.1
|--org.elasticsearch:elasticsearch:5.6.16
|--org.elasticsearch.client:elasticsearch-rest-client:7.0.1
|--org.elasticsearch.plugin:parent-join-client:7.0.1
|--org.elasticsearch.plugin:aggs-matrix-stats-client:7.0.1
|--org.elasticsearch.plugin:rank-eval-client:7.0.1
|--org.elasticsearch.plugin:lang-mustache-client:7.0.1
```
可能读者会感觉很奇怪，明明指定了`elasticsearch`的依赖了啊，而且是项目的根 pom，依赖不是最短路径原则么？不应该以这个依赖为准么？

仔细分析，原来 SpringBoot的DependencyManagement 中，`org.elasticsearch:elasticsearch`已经被包含了(以下为节选)：

```
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-dependencies</artifactId>
<version>2.0.9.RELEASE</version>

<properties>
<elasticsearch.version>5.6.16</elasticsearch.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.elasticsearch</groupId>
            <artifactId>elasticsearch</artifactId>
            <version>${elasticsearch.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

```

spring-boot 其实已经考虑到用户可能要换版本了，所以将版本放入了 `<properties/>`，properties 也具有最短路径原则，所以可以通过在你的项目根 pom 中的 properties 增加相同 key 修改版本：
```
<properties>
    <elasticsearch.version>7.10.2</elasticsearch.version>
</properties>
```
所有可以这么替换的属性， spring-boot 官方文档已经列出了，参考[官方文档附录：Version Properties](https://docs.spring.io/spring-boot/docs/2.4.2/reference/htmlsingle/#10.F.2.%20Version%20Properties)

也可以通过 dependencyManagement 的最短路径原则，通过在你的项目根 pom 中的增加想修改依赖的 dependencyManagement 即可：
```
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.elasticsearch</groupId>
            <artifactId>elasticsearch</artifactId>
            <version>7.10.2</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

最后，可以记住下面的原则，就知道项目的依赖到底是哪个版本啦：

**Maven依赖可以分为如下几部分：**
1. 直接依赖，就是本项目 dependencies 部分的依赖
2. 间接依赖，就是本项目 dependencies 部分的依赖所包含的依赖
3. 依赖管理，就是本项目 dependency management 里面的依赖
4. parent 的直接依赖
5. parent 的间接依赖
6. parent 的依赖管理
7. bom 的直接依赖（一般没有）
8. bom 的间接依赖（一般没有）
9. bom 的依赖管理

**可以这么理解依赖：**
1. 首先，将 parent 的直接依赖，间接依赖，还有依赖管理，插入本项目，放入本项目的直接依赖，间接依赖还有依赖管理之前
2. 对于直接依赖，如果有 version，那么就依次放入 DependencyMap 中。如果没有 version，则从依赖管理中查出来 version，之后放入 DependencyMap 中。key 为依赖的 groupId + artifactId，value为version，**后放入的会把之前放入的相同 key 的 value 替换**
3. 对于每个依赖，各自按照 1，2 加载自己的 pom 文件，**但是如果第一步中的本项目 dependency management 中有依赖的版本，使用本项目 dependency management的依赖版本**，生成 TransitiveDependencyMap，这里面就包含了所有的间接依赖。
4. 所有间接依赖的 TransitiveDependencyMap， **对于项目的 DependencyMap 里面没有的 key，依次放入项目的 DependencyMap**
5. 如果 TransitiveDependencyMap 里面还有间接依赖，那么递归执行3， 4。

由于是先放入本项目的 DependencyMap，再去递归 TransitiveDependencyMap，这就解释了 maven 依赖的最短路径原则。

Bom 的效果基本和 Parent 一样，只是一般限制中，Bom 只有 dependencyManagement 没有 dependencies

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/4-02.%20%E5%9F%BA%E7%A1%80%E6%A1%86%E6%9E%B6%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84.jpg)

如下图所示，我们会抽象出如下几个依赖：

![image](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/4-01.%20Dependency.png)

1. **所有项目的 parent**：以某版本 Spring Boot 作为 parent，管理 Spring Cloud 依赖，并且包括一些公共依赖，还有单元测试依赖。如果以后我们想修改 Spring Boot 或者 Spring Cloud 版本，就在这里修改。并且，指定了所有项目编译配置。
2. **Spring Framework Common**：所有使用了 Spring 或者 Spring Boot 的公共依赖，一般我们编写 starter，或者编写一些工具包，不需要 Spring Cloud 的特性，就会添加这个依赖。
3. **Spring Cloud Common**：添加了 Spring Framework Common 的依赖。我们的微服务分为主要基于 spring-webmvc 的同步微服务项目以及主要基于 spring-webflux 的异步微服务项目，其中有一些公共的依赖和代码，就放在了这个项目中。
4. **Spring Cloud WebMVC**：添加了 Spring Cloud Common 的依赖。基于 spring-webmvc 的同步微服务项目需要添加的核心依赖。
5. **Spring Cloud WebFlux**：添加了 Spring Cloud Common 的依赖。基于 spring-webflux 的异步微服务项目需要添加的核心依赖。


我们在微服务项目中主要使用的依赖为：
1. 对于纯工具包，只使用了 Spring 与 Spring Boot 的特性的，添加 Spring Framework Common 的依赖。
2. 对于基于 spring-webmvc 的同步微服务项目，添加 Spring Cloud WebMVC 的依赖。
3. 对于基于 spring-webflux 的异步微服务项目，添加 Spring Cloud WebFlux 的依赖。

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


本小节我们回顾了并深入理解了 maven 依赖最短路径原则，然后给出了我们项目框架的结构，主要对外提供了三种依赖：只使用了 Spring 与 Spring Boot 的特性的依赖，对于基于 spring-webmvc 的同步微服务项目的依赖以及对于基于 spring-webflux 的异步微服务项目的依赖。下一节我们将对这些项目模块的 pom 文件进行详细分析。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)