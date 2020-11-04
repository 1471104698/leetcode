# Spring 各个注解的作用



## 1、@Configuration

我们都知道 @Configuration 是将某个类声明为配置类

以下是 @Configuration 的源码

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	@AliasFor(annotation = Component.class)
	String value() default "";
    
	boolean proxyBeanMethods() default true;
}
```



我们可以看出 @Configuration 也使用了 @Component 注解，表示它也是一个 @Component 

但是它跟 @Component 注解的类有什么区别呢？

一个很大的区别在于：@Configuration 注解的类会自动生成代理对象，而 @Compoment 注解的类则不会，除非对它使用 AOP

在处理 @Configuration 时，会根据 @Configuration 注解的一个属性 proxyBeanMethods 来进行处理

如果 proxyBeanMethods = true，那么 @Configuration 注解的类就是一个 Full 类，会创建 代理对象

如果 proxyBeanMethods = false，那么 @Configuration 注解的类就是一个 Lite 类，不会创建 代理对象

当然默认情况下为 true



那么给 @Configuration 注解的类 生成代理对象的作用是什么呢？

**为了 @Bean 方法产生单例对象**



proxyBeanMethods = true 的情况下：

```java
@Configuration
public class UserConfiguration {

    @Bean
    public Dog getDog(){
        Cat cat = getCat();
        Cat cat1 = getCat();
        System.err.println(cat == cat1);
        System.err.println(this.getClass());
        return new Dog();
    }

    @Bean
    public Cat getCat(){
        return new Cat();
    }

    class Dog{
    }
    class Cat{
        public Cat(){
            System.err.println("初始化");
        }
    }
}
```

输出：

```java
getCat() 调用
初始化
true
class cn.oy.cache.config.UserConfiguration$$EnhancerBySpringCGLIB$$622e87bc
```



proxyBeanMethods = false 的情况下：

```java
@Configuration(proxyBeanMethods = false)
public class UserConfiguration {

    @Bean
    public Dog getDog(){
        Cat cat = getCat();
        Cat cat1 = getCat();
        System.err.println(cat == cat1);
        System.err.println(this.getClass());
        return new Dog();
    }

    @Bean
    public Cat getCat(){
        return new Cat();
    }

    class Dog{
    }
    class Cat{
        public Cat(){
            System.err.println("初始化");
        }
    }
}
```

输出：

```java
初始化
初始化
false
class cn.oy.cache.config.UserConfiguration
初始化
getCat() 调用
getCat() 调用
getCat() 调用
```



可以看出在 proxyBeanMethods = true 的情况下，@Configuration 注解的类 生成的是一个代理对象，同时，在 @Bean 注解的 getDog() 方法中多次调用了 @Bean 注解的 getCat()，那么它实际上并不会真正的多次去调用 getCat()，而是只会调用一次，包括 getCat() 自身创建 bean 时的那一次调用，即意味着 getCat() 只会执行一次，这也就保证了 @Bean 产生的 bean 只有一个，保证了单例，那么多次调用 getCat() 返回的 Cat 对象是如何获得的呢？

这就涉及到 @Configuration 的代理对象，在每次调用 getCat() 的时候，拦截器拦截该方法，之后都会先到 BeanFactory 中去尝试获取，如果有就返回，没有再调用 getCat() 创建 bean，在第一次调用的时候没有，所以会创建一个 Cat 对象，然后存储到 BeanFactory 中，之后调用都是从 BeanFactory 中获取

而如果 proxyBeanMethods = false，那么不会涉及到 BeanFactory，每次调用获取的都是一个新的 Cat 对象



## 2、@Bean

也许有个疑问，为什么有了 @Compoment 了还要出现 @Bean ？

因为 @Compoment 注解的类都是我们自定义的类，对于第三方的类，是不能添加 @Compoment 注解的，因为添加不了

所以只能通过 @Bean 将它注入进来，同时还可以在 @Bean 方法中对该 bean 做一些处理



## 3、@Import

这个类跟 @Bean 差不多，都是用来注入 bean 的，但是 @Import 能够同时注入多个 bean，并且 @Import 更加的便捷，能够直接作为注解添加到类上

最主要的一个区别是，**只有 @Import 注入的 ImportSelector 类 才会去执行 selectImport() 方法**，这类似于一种规范

ImportSelector 的 selectImport() 方法返回一个字符串数组，里面每个字符串都是需要注入的 类的名字



```java
public class UserImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{UserService.class.getName()};
    }
}
```

这个 ImportSelector 看起来好像也没什么用处，就是简单的表明需要注入的 bean

但实际上，这个类在 SpringBoot 中用处很大，**跟 自动配置 的实现原理相关**

在 SpringBoot 中，自动配置的类都是写明在 spring.factories 文件中的，那么就需要去这个文件中读取所有需要配置的类，然后注入到 IOC 容器中，执行这个读取的就是 @EnableAutoConfigration 注解 @Import 进去的 AutoConfigurationImportSelector.class

它实现了 ImportSelector.接口，在扫描的时候会调用它的 selectImprts() 方法，该方法内部会读取 spring.factories 文件，将文件内的类整合到一个字符串数组中返回，这样在后续它们都能够被注入到 IOC 文件中，完成自动配置