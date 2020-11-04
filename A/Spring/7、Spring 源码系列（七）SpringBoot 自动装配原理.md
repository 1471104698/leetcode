# SpringBoot 自动装配原理



## 1、SpringBoot 相比 Spring 的优点

SpringBoot 相比 Spring ，强在哪里？

我们都知道 SpringBoot 简化了开发，因为它有一个 `自动装配` 的功能，而不再依赖于 XML 配置

不过说是这么说，我们要想知道 SpringBoot 比 Spring 真正强在哪里，那么就需要对两者进行对比



在 Spring 使用 Spring MVC 的时候，需要配置 dispatcherServlet、视图解析器、Servlet、log4j、Listen 等等

当我们使用数据库的时候，需要配置数据源

当我们使用事务的时候，需要配置事务管理器

这些配置都需要在 XML 文件中手动配置，使得 XML 看起来繁琐，不容易维护，并且信息过多压根记不住，在新开一个项目的时候，需要从别的地方复制一份配置信息。。。

```XML
<!-- 视图解析器 和 DispatcherServlet-->
<bean
      class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="prefix">
        <value>/WEB-INF/views/</value>
    </property>
    <property name="suffix">
        <value>.jsp</value>
    </property>
</bean>
<mvc:resources mapping="/webjars/**" location="/webjars/"/>
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>
        org.springframework.web.servlet.DispatcherServlet
    </servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/todo-servlet.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
<!-- 数据库 数据源 -->
<bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource"
      destroy-method="close">
    <property name="driverClass" value="${db.driver}" />
    <property name="jdbcUrl" value="${db.url}" />
    <property name="user" value="${db.username}" />
    <property name="password" value="${db.password}" />
</bean>
<jdbc:initialize-database data-source="dataSource">
    <jdbc:script location="classpath:config/schema.sql" />
    <jdbc:script location="classpath:config/data.sql" />
</jdbc:initialize-database>
<bean
      class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean"
      id="entityManagerFactory">
    <property name="persistenceUnitName" value="hsql_pu" />
    <property name="dataSource" ref="dataSource" />
</bean>
<!-- 事务管理器 -->
<bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager">
    <property name="entityManagerFactory" ref="entityManagerFactory" />
    <property name="dataSource" ref="dataSource" />
</bean>
<tx:annotation-driven transaction-manager="transactionManager"/>
```



SpringBoot 实际上是 Spring + Spring MVC 的扩展

SpringBoot 解决上 Spring 的这些问题，SpringBoot 实现了 `自动装配` 功能，我们只需要在 配置文件上写好参数配置，SpringBoot 在启动的时候会去扫描这些参数，一旦扫描到对应类的参数，那么就会自动生成这些 bean，并且将参数填充进去，无需用户去创建 bean，极大的简化了开发



## 2、SpringBoot 自动配置原理

[SpringBoot 自动配置原理](<https://blog.csdn.net/u014745069/article/details/83820511>)



首先我们大家都知道的 @SpringBootApplication

```java
@SpringBootApplication
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class);
	}
}
```

它是一个复合注解，聚合了多个注解

```java
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration	//开启自动配置，一旦删除这个注解，自动配置就失效了
//扫描包
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),	
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
}
```

其中最为重要的是 @EnableAutoConfiguration 注解，它是开启自动配置的核心注解

```java
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)	//引入自动配置 核心处理类
public @interface EnableAutoConfiguration {
}
```

@EnableAutoConfiguration 注解 内部 @Import 了一个 AutoConfigurationImportSelector 类，它是 完成自动配置的核心类

它实现了 ImportSelector 接口，在类扫描 处理 @Import 注解时，发现导入的该类是一个 ImportSelector 类，那么就会调用它的 selectImports() 方法，在该类方法中会去读取 spring.factories 文件，在这个文件中，定义了一大堆的类名，selectImports() 获取这些类名，封装成一个数组返回，这样就能够让 Spring 注册成 Bean，从而实现自动配置



我们需要知道，SpringBoot 相比 Spring 来说，是如何做到只需要在 配置文件上配置几个参数，就能够使用 redis、数据库、MVC 功能的，这也跟 spring.factories 文件上的类相关



## 3、spring.factories 文件



从 SpringBoot 的源码看来，spring.factories 文件 位于 spring-boot-autoconfigure 模块下

*![image.png](https://pic.leetcode-cn.com/1604323563-HdTggg-image.png)*



以下是 spring.factories 文件的内容：

```properties
# Initializers
org.springframework.context.ApplicationContextInitializer=\
org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer,\
org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener

# Application Listeners
org.springframework.context.ApplicationListener=\
org.springframework.boot.autoconfigure.BackgroundPreinitializer

# Auto Configuration Import Listeners
org.springframework.boot.autoconfigure.AutoConfigurationImportListener=\
org.springframework.boot.autoconfigure.condition.ConditionEvaluationReportAutoConfigurationImportListener

# Auto Configuration Import Filters
org.springframework.boot.autoconfigure.AutoConfigurationImportFilter=\
org.springframework.boot.autoconfigure.condition.OnBeanCondition,\
org.springframework.boot.autoconfigure.condition.OnClassCondition,\
org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition

# Auto Configure	自动配置类
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\
org.springframework.boot.autoconfigure.aop.AopAutoConfiguration,\
org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration,\
org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration,\	
								#以下定义了 redis 相关的用于自动配置类
org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,\
org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,\
org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,\
org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration,\

#未完
```



我们进入到 RedisAutoConfiguration 类中

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisOperations.class)
/*
@EnableConfigurationProperties 和 @ConfigurationProperties 配合使用，主要用于读取配置文件的属性
这里表示使 RedisProperties.class 类生效
*/
@EnableConfigurationProperties(RedisProperties.class)
@Import({ LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class })
public class RedisAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "redisTemplate")
	public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory)
			throws UnknownHostException {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	@Bean
	@ConditionalOnMissingBean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory)
			throws UnknownHostException {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}
}
```

RedisAutoConfiguration @Import 的 JedisConnectionConfiguration 类会自动帮我们将我们配置的 redis 属性进行填充到 redis 连接中

在内部定义了我们使用的 bean，比如 RedisTemplate、StringRedisTemplate，它自动帮我们生成了

那么我们在配置文件中配置的 redis 参数是如何被读取的呢？

RedisAutoConfiguration 还存在一个 注解 @EnableConfigurationProperties(RedisProperties.class)，它表示使 RedisProperties.class 这个类生效



进入 RedisProperties 类中

```java
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
	private int database = 0;
	private String url;
	private String host = "localhost";
	private String password;
	private int port = 6379;
	private boolean ssl;
	private Duration timeout;
    
    //xxxx
    
    public static class Pool {
		private int maxIdle = 8;
		private int minIdle = 0;
		private int maxActive = 8;
        
        //xxxxx
    }
    
    public static class Cluster {
		private List<String> nodes;
		private Integer maxRedirects;
		//xxxx
	}
    public static class Sentinel {
		private String master;
		private List<String> nodes;
        
		//xxxx
	}

	public static class Jedis {
		private Pool pool;
        
        //xxx
	}
}
```

RedisProperties 类中定义了 redis 是所有属性字段，并且加上了 @ConfigurationProperties 注解，该注解用于读取配置文件对应的属性值，这里也说明了我们写在配置文件中的属性值是在这里被读取并使用的，SpringBoot 将它封装成了一个 RedisProperties  对象



简单讲，SpringBoot 就是将生成 bean 的逻辑给我们写好了，我们只需要在配置文件中进行参数配置，然后让 SpringBoot 读取，然后填充到 bean 中，再存储到 IOC 容器中，我们只需要注入即可使用

这些生成 bean 的逻辑大部分都是固定不会改变的，真正会改变的只是参数而已，SpringBoot只是将它们写好作为一个模板，通过读取参数的方式开箱即用

