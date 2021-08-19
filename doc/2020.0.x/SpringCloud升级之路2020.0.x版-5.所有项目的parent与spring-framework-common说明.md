![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF%20Logo.jpg)

> 本系列代码地址：https://github.com/HashZhang/spring-cloud-scaffold/tree/master/spring-cloud-iiford

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/5-01.%20%E6%89%80%E6%9C%89%E9%A1%B9%E7%9B%AE%E7%9A%84%20parent.jpg)

> 源代码文件：https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/pom.xml

**1. 使用 log4j2 异步日志所需要的依赖**：需要排除默认的日志实现 logback，增加 log4j2 的依赖，并且添加 log4j2 异步日志需要的 disruptor 依赖。

```
<!--日志需要用log4j2-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
<!--log4j2异步日志需要的依赖，所有项目都必须用log4j2和异步日志配置-->
<dependency>
    <groupId>com.lmax</groupId>
    <artifactId>disruptor</artifactId>
    <version>${disruptor.version}</version>
</dependency>
```

**2. javax.xml 的相关依赖**。我们的项目使用 JDK 11。JDK 9 之后的模块化特性导致 javax.xml 不自动加载，所以需要如下模块：
```
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>${jaxb.version}</version>
</dependency>
<dependency>
    <groupId>com.sun.xml.bind</groupId>
    <artifactId>jaxb-impl</artifactId>
    <version>${jaxb.version}</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>${jaxb.version}</version>
</dependency>
<dependency>
    <groupId>com.sun.xml.bind</groupId>
    <artifactId>jaxb-xjc</artifactId>
    <version>${jaxb.version}</version>
</dependency>
<dependency>
    <groupId>javax.activation</groupId>
    <artifactId>activation</artifactId>
    <version>${activation.version}</version>
</dependency>
```

**3. 使用 Junit 5 进行单元测试**，Junit 5 使用可以参考：[Junit5 user guide](https://junit.org/junit5/docs/current/user-guide/)
```
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
```

**4. 使用 Spring Boot 单元测试**，可以参考：[features.testing](https://docs.spring.io/spring-boot/docs/2.5.3/reference/htmlsingle/#features.testing)
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**5. mockito扩展，主要是需要mock final类**：Spring Boot 单元测试已经包含了 mockito 依赖了，但是我们还需要 Mock final 类，所以添加以下依赖：
```
<!--mockito扩展，主要是需要mock final类-->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <version>${mokito.version}</version>
    <scope>test</scope>
</dependency>
```

**6. embedded-redis**：使用 embedded-redis 用于涉及 Redis 的单元测试：如果你的单元测试需要访问 redis，则需要在测试前初始化一个 redis，并在测试后关闭。使用 embedded-redis 就可以。我们在 spring-cloud-parent 中已经添加了这个依赖，所以可以直接使用。参考：[embedded-redis](https://github.com/kstyrc/embedded-redis)
```
<dependency>
    <groupId>com.github.kstyrc</groupId>
    <artifactId>embedded-redis</artifactId>
    <version>${embedded-redis.version}</version>
    <scope>test</scope>
</dependency>
```

**7. sqlite 单元测试依赖**：对于数据库的单元测试，我们可以使用 SQLite。参考：[sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)

```
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>${sqlite-jdbc.version}</version>
    <scope>test</scope>
</dependency>
```

**8. 指定编译级别为 Java 11**
```
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>11</source>
                <!--ingore javac compiler assert error-->
                <forceJavacCompilerUse>true</forceJavacCompilerUse>
                <target>11</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/5-02.%20spring-framework-common%20%E9%A1%B9%E7%9B%AE%E4%BE%9D%E8%B5%96.jpg)

> 源代码文件：https://github.com/HashZhang/spring-cloud-scaffold/blob/master/spring-cloud-iiford/spring-cloud-iiford-spring-framework-common/pom.xml

作为使用 spring 与 spring boot 框架的公共依赖 spring-framework-common 项目是一个纯依赖的项目。

**1. 内部缓存框架统一采用caffeine**：这是一个很高效的本地缓存框架，接口设计与 Guava-Cache 完全一致，可以很容易地升级。性能上，caffeine 源码里面就有和 Guava-Cache， ConcurrentHashMap，ElasticSearchMap，Collision 和 Ehcache 等等实现的对比测试，并且测试给予了 yahoo 测试库，模拟了近似于真实用户场景，并且，caffeine 参考了很多论文实现不同场景适用的缓存，例如：

1. Adaptive Replacement Cache：[http://www.cs.cmu.edu/~15-440/READINGS/megiddo-computer2004.pdf]()
2.Quadruply-segmented LRU：http://www.cs.cornell.edu/~qhuang/papers/sosp_fbanalysis.pdf
3. 2 Queue：http://www.tedunangst.com/flak/post/2Q-buffer-cache-algorithm
4. Segmented LRU：http://www.is.kyusan-u.ac.jp/~chengk/pub/papers/compsac00_A07-07.pdf
5. Filtering-based Buffer Cache：http://storageconference.us/2017/Papers/FilteringBasedBufferCacheAlgorithm.pdf

所以，我们选择 caffeine 作为我们的本地缓存框架，参考：[caffeine](https://github.com/ben-manes/caffeine)

```
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```


**2. 使用 google 的 java 开发库 guava**：guava 是 google 的 Java 库，虽然本地缓存我们不使用 guava，但是 guava 还有很多其他的元素我们经常用到。参考：[guava docs](https://guava.dev/releases/snapshot-jre/api/docs/)
```
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>${guava.version}</version>
</dependency>
```

**3. 内部序列化统一采用fastjson**：注意 json 库一般都需要预热一下，后面会提到怎么做。参考：[fastjson](https://github.com/alibaba/fastjson)
```
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>${fastjson.version}</version>
</dependency>
```

**4. 使用 lombok 简化代码**，参考：[projectlombok](https://projectlombok.org/)
```
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

**5. 调用路径记录 - sleuth**。参考：[spring-cloud-sleuth](https://spring.io/projects/spring-cloud-sleuth)
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

**6. 跨线程 ThreadLocal**。参考：[transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local)
```
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>transmittable-thread-local</artifactId>
    <version>${transmittable-thread-local.version}</version>
</dependency>
```

**7. Swagger 相关**。参考：[swagger](https://swagger.io/)

```
<!--Swagger-->
<!-- swagger java元数据集成 -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>${swagger.version}</version>
</dependency>
<!-- swagger 前端页面 -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>${swagger.version}</version>
</dependency>
```

**8. Apache Commons 相关工具包**。我们会使用一些 Commons 工具包，来简化代码：

 - [commons-lang](https://commons.apache.org/proper/commons-lang/)
 - [commons-collections](https://commons.apache.org/proper/commons-collections/)
 - [commons-text](https://commons.apache.org/proper/commons-text/)
 
```
<!-- https://mvnrepository.com/artifact/org.apache.commons/commons-lang3 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
<!-- https://mvnrepository.com/artifact/org.apache.commons/commons-collections4 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-collections4</artifactId>
    <version>${commons-collections4.version}</version>
</dependency>
<!-- https://mvnrepository.com/artifact/org.apache.commons/commons-text -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>${commons-text.version}</version>
</dependency>
```


![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/Spring%20Cloud%20%E5%8D%87%E7%BA%A7%E4%B9%8B%E8%B7%AF/2020.x/%E6%80%BB%E7%BB%93%E4%B8%8E%E5%90%8E%E7%BB%AD.png)


本小节我们详细说明了我们所有项目的 parent，以及 使用了 Spring 与 Spring Boot 特性的工具包依赖 spring-framework-common 的设计。下一节我们将详细分析提供微服务特性的依赖。

> **微信搜索“我的编程喵”关注公众号，每日一刷，轻松提升技术，斩获各种offer**：

![](https://zhxhash-blog.oss-cn-beijing.aliyuncs.com/%E5%85%AC%E4%BC%97%E5%8F%B7QR.gif)