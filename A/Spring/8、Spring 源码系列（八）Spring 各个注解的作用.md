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





## 4、@Autowire 和 @Resource 的区别

@Autowire 源码如下：

```java
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	boolean required() default true;
}
```

它只有一个 required 字段，用来标识注解的变量是否必须注入，默认为 true，如果注解的字段的 bean 为 Null，那么抛出异常



@Resource 源码如下：

```java
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
public @interface Resource {

    String name() default "";

    Class<?> type() default java.lang.Object.class;
}
```

@Resource 中可以指定注入的 bean 的 name 和 type



@Autowire 和 @Resource 的区别：

- @Autowire 默认按照 type 注入，如果存在多个相同 type 的 bean，那么按照 字段名 进行 name 注入，即 @Autowire 存在 name 注入，但是不能自己指定 name，而是 Spring 默认**按照字段名注入**

- @Resource 可以通过 @Resource(name, type) 字段来执行 type 注入 或者 name 注入；同时，当只使用 type 注入时，跟 @Autowire 一样，如果存在多个 bean，那么默认**按照 字段名 注入**，如果同时使用了 type 和 name，那么先找 type，再按照 name 注入，即 @Resource 可以自己指定注入的 name，而不需要强制要求使用 变量名
- @Autowire(require = ?) 可以设置该字段对应的 bean 为空时是否抛出异常，如果 require = false，表示允许字段注入的 bean 为空，而不会抛异常；@Resource 则必须强制注入 bean 存在，如果找不到对应的 bean，那么就抛出异常
- @Autowire 也可以跟 @Qualifier 一起使用，组合形成 @Resource，同时进行 type 注入 和 name 注入，同时还保留了 @Autowire 的指定 require 的特点

```java
@Component
class Q  {
    //注入成功，存在多个 R bean，根据字段名 rr2 进行 name 注入，存在 beanName 为 rr2 的 bean，成功注入
    @Autowired
    R ar1;	
    //注入失败，找到 3 个 类型为 R 的 bean，同时没有任何一个 beanName 为 r，无法注入
    @Resource(type = R.class)	
    R r;	
    //注入成功，找到 3 个 类型为 R 的 bean，但是存在一个 beanName 为 rr1 的 R1 
    @Resource(type = R.class)	
    R rr1;	
    //注入成功，存在一个 beanName 为 rr3 的 R3
    @Resource(name = “rr3")		
    R rr2;	
    //注入失败，不存在 R1 bean 的 beanName 为 rr3 的
    @Resource(type = R1.class, name = “rr3")		
    R r1;	
    //注入成功，存在 R bean 的 beanName 为 rr3 的，注入
    @Resource(type = R.class, name = “rr3")		
    R r3;	
}

class R{
}
@Component("rr1")
class R1 extends R{
}
@Component("rr2")
class R2 extends R{
}
@Component("rr3")
class R3 extends R{
}
```

