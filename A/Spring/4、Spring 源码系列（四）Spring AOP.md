# Spring AOP



## 1、AOP 组件

AOP 代理三大组件：

- @Aspect 定义的切面
- @Pointcut 定义的切点
- @Before、@After、@AfterReturning、@Around、@AfterThrowing 定义的通知类型

@AspectJ 是 JDK 实现 AOP 用的，Spring 没有自己定义关于 AOP 相关的注解，全部是使用的 @AspectJ 的注解，但是并没有使用 @AspectJ 的技术，这点需要注意，即 Spring 的 AOP 只有注解跟 @AspectJ 扯上关系，其他的创建解析方法都跟它没有关系了



@Aspect 注解的 DaoAspect 类表示这是一个切面

@Pointcut 注解的方法表示切入的点，即表示对什么进行代理，可以精确到类 或者 方法

@Before、@After 表示什么时候进行切入，指定切入的对象，可以使用 @Pointcut  或者 自己定义 execution()，

一般是使用 @Pointcut ，@Pointcut 就跟方法封装一样，别的地方可以直接调用，无需自己再去写逻辑

```java
@Component
@Aspect
public class DaoAspect {

	@Pointcut("execution(* com.luban.spring.dao.*.*(..))")
	public void pointCut1() {
		System.out.println("切面 1");
	}

	@Pointcut("execution(* com.luban.spring.dao.IndexDao.*(..))")
	public void pointCut2() {
		System.out.println("切面 2");
	}

	@Before("pointCut1()")
	public void beforeAAA() {
		System.out.println("before");
	}

	@After("pointCut2()")
	public void afterAAA() {
		System.out.println("after");
	}

	@AfterThrowing("execution(* com.luban.spring.dao.IndexDao1.*(..))")
	public void afterThrowingAAA() {
		System.out.println("after");
	}
}
```





## 2、开启 AOP 代理 - @EnableAspectJAutoProxy

添加 `@EnableAspectJAutoProxy`，它会注入 一个 后置处理器，用来处理 AOP 对象

```java
@Configuration
@ComponentScan({"com.luban"})
//proxyTargetClass = true，表示强制使用 CGLIB 代理，默认为 false
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ImportResource("classpath:spring.xml")
public class Appconfig {
	//do something
}
```



EnableAspectJAutoProxy 注解内部：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)//导入一个类，这个类非常重要，它内部会注册一个后置处理器，用来处理 AOP 对象
public @interface EnableAspectJAutoProxy {
  
   //决定使用 JDK 动态代理 还是 CGLIB 代理，默认为 false，即使用 JDK 动态代理
   boolean proxyTargetClass() default false;

   boolean exposeProxy() default false;

}
```



EnableAspectJAutoProxy 导入的 AspectJAutoProxyRegistrar 类内部：

它会注册一个 后置处理器 `AnnotationAwareAspectJAutoProxyCreator`，用来第四次后置处理器调用 和 第八次后置处理器调用生成 AOP 对象的

```java
/**
 * Registers an {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator
 * AnnotationAwareAspectJAutoProxyCreator} against the current {@link BeanDefinitionRegistry}
 * as appropriate based on a given @{@link EnableAspectJAutoProxy} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see EnableAspectJAutoProxy
 */
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

   @Override
   public void registerBeanDefinitions(
         AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

      /*
      注册一个基于注解的 AOP 对象后置处理器 AnnotationAwareAspectJAutoProxyCreator，
      该后置处理器用来创建 AOP 对象的
      */
      AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

      AnnotationAttributes enableAspectJAutoProxy =
            AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
      if (enableAspectJAutoProxy != null) {
         //proxyTargetClass == true，表示指定使用 CGLIB 代理
         if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
            AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
         }
         if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
            AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
         }
      }
   }
}
```





## 3、Spring AOP 的大致执行流程

> ### 文字说明

我这里先用文字说明一下大致的流程，后面再用代码演示：

- 定义好切面，给 A bean 执行 切入 逻辑
- 第一次后置处理器会判断该 bean 是否需要增强，不需要增强会假如到一个 map 集合 advisedBeans 中过滤掉，当然，这个 A bean 是需要增强的，所以不会添加进去
- 创建 A bean
  - 如果出现循环依赖，那么调用第四次后置处理器，提前创建 AOP 对象，这里需要使用了一个 集合 earlyProxyReference 来记录在这里创建的 AOP 对象，这样就可以避免在 第八次后置处理器重复创建 AOP 对象 
  - 如果没有循环依赖，那么前面进行属性注入完后，进行初始化，然后调用第八次后置处理器，它会先判断在 earlyProxyReference  中是否已经存在了这个类，如果存在了那么直接返回 原始 bean
- 在第四、八次后置处理器中，都会调用一个 wrapIfNecessary()，该方法是 AOP 的入口
- 第八次后置处理器在调用 wrapIfNecessary() 前，会判断早期对象集合 earlyProxyReference 中是否存在该 bean，如果存在，那么意味着在 第四次后置处理器中已经产生 AOP 对象了，那么这里就不再重复产生，直接返回。
- wrapIfNecessary() 会先判断 advisedBeans 是否存在这个 bean，如果在第一次后置处理器中过滤掉了，那么会直接返回，不会再执行 AOP 逻辑
- 然后获取一个 Advisor 数组，即 @Before、@After的切入点，每个切入点都是一个 Advisor 
- 调用 createProxy()，会先创建一个 ProxyFactpry 对象，它是 ProxyCreatorSupport 的一个子类
- 然后将 Advisor 数组 设置到这个 ProxyFactpry 中，调用 ProxyFactpry 的 getProxy()，实际上是调用的父类 ProxyCreatorSupport  的 getProxy()
- 该方法会先执行一个 createAopProxy()，获取一个对象工厂，然后使用这个对象工厂选定一个用来实现 AOP 的代理实现类：JDK 动态代理 或者 CGLIB 动态代理
- 然后使用返回的 JDK 动态代理 或者 CGLIB 动态代理，调用它们的 getProxy() 真正的创建 AOP 对象，它们各自实现了 getProxy() 逻辑

简单讲逻辑就是找到切面上所有的 通知，然后获取工厂对象选定实现代理的 方式类：JDK 或者 CGLIB，再调用它们的 getProxy() 创建 AOP 对象



> ###  代码演示

首先我们定义切面，指定对 dao 包下的类进行 AOP

```java
@Component
@Aspect
public class DaoAspect {

	@Pointcut("execution(* com.luban.spring.dao.*.*(..))")
	public void pointCut() {
		System.out.println("切面 1");
	}

	@Before("pointCut()")
	public void beforeAAA() {
		System.out.println("before");
	}
}

```



然后 getBean() 根据 BeanDefinition 创建 bean

第一次后置处理器将不需要增强的类记录起来

```java
@Override
public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
    Object cacheKey = getCacheKey(beanClass, beanName);

    if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
        if (this.advisedBeans.containsKey(cacheKey)) {
            return null;
        }
        /*
			Spring 判断该 bean 100% 不需要增强，那么会存储到 advisedBeans 这个 map 中,
			在后续进行增强的时候可以直接跳过
			调用 isInfrastructureClass() 进行判断
			 */
        if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return null;
        }
    }

    //以下满足一个的话，那么该 bean 就不需要增强
    TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
    if (targetSource != null) {
        if (StringUtils.hasLength(beanName)) {
            this.targetSourcedBeans.add(beanName);
        }
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
        Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    }

    return null;
}
```



如果发生循环依赖，那么在第四次后置处理器处调用 `AnnotationAwareAspectJAutoProxyCreator`后置处理器进行 AOP 代理

如果没有循环依赖，那么在第八次后置处理器处调用 `AnnotationAwareAspectJAutoProxyCreator`后置处理器进行 AOP 代理

如果是第四次后置处理器处调用，那么如下：

```java
@Override
public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
    Object cacheKey = getCacheKey(bean.getClass(), beanName);
    /*
		earlyProxyReferences（早期代理引用） 存储在第四次后置处理器（即当前）中创建了 AOP 对象的 bean，
		这样在第八次后置处理器就不会重复创建 AOP 对象
	*/
    if (!this.earlyProxyReferences.contains(cacheKey)) {
        this.earlyProxyReferences.add(cacheKey);
    }
    //调用 wrapIfNecessary()，它是 AOP 的执行入口，第四次和第八次后置处理器的 AOP 执行都是从这里进入的
    return wrapIfNecessary(bean, beanName, cacheKey);
}
```

如果是第八次后置处理器，那么如下：

```java
@Override
public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) throws BeansException {
    if (bean != null) {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        /*
              在调用 wrapIfNecessary() 之前先判断 earlyProxyReferences 中是否存在存储了该 bea
              即判断第四次后置处理器是否已经创建了 AOP 对象，如果创建了，那么直接返回
              contains() 返回 false，那么调用 wrapIfNecessary() 执行 AOP 逻辑
			 */
        if (!this.earlyProxyReferences.contains(cacheKey)) {
            return wrapIfNecessary(bean, beanName, cacheKey);
        }
    }
    return bean;
}
```



wrapIfNecessary() 方法内部：

```java
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
        return bean;
    }
    /*
        advisedBeans 是一个 map，记录的是 bean 是否需要增强，或者 是否已经增强
        如果对应的 bean 为 false,表示不需要增强 或者 已经增强过了
        如果在第一次后置处理器中判断 当前 bean %100 不需要增强，那么标记为 false, 这里会直接返回
    */
    if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
        return bean;
    }
    if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    // Create proxy if we have advice.
    /*
    	获取当前 bean 需要的所有的 @Pointcut、@Before 切入点（通知）
    	封装成一个 Object[] 数组
    */
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
    //当切入点数组 != null，表示需要增强
    if (specificInterceptors != DO_NOT_PROXY) {
        //标记为 true
        this.advisedBeans.put(cacheKey, Boolean.TRUE);
        //调用 createProxy()
        Object proxy = createProxy(
            bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    }
	//添加进 advisedBeans，表示已经增强过了，不需要重复增强
    this.advisedBeans.put(cacheKey, Boolean.FALSE);
    return bean;
}
```



createProxy() 方法如下：

```java
protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
                             @Nullable Object[] specificInterceptors, TargetSource targetSource) {

    if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
        AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
    }
    //创建 ProxyFactory 对象，它是 ProxyCreatorSupport 的一个子类
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);
	
    /*
    	将 specificInterceptors 这个 Object[] 数组 转换为 Advisor[] 数组，
    */
    Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
    //将切入点集合添加到 proxyFactory 中
    proxyFactory.addAdvisors(advisors);
    proxyFactory.setTargetSource(targetSource);
    //空方法
    customizeProxyFactory(proxyFactory);

    proxyFactory.setFrozen(this.freezeProxy);
    if (advisorsPreFiltered()) {
        proxyFactory.setPreFiltered(true);
    }
    //调用 proxyFactory 的 getProxy()，真正创建 AOP 对象的逻辑
    return proxyFactory.getProxy(getProxyClassLoader());
}
```



proxyFactory 的 getProxy() 内部分为了两部分 createAopProxy() 和 getProxy()

其中是先执行 createAopProxy()，再获取该方法的返回值调用 getProxy()

```java
public Object getProxy(@Nullable ClassLoader classLoader) {
    /*
		createAopProxy() 创建 JDK 代理 或者 CGLIB 代理对象，内部有一个工厂对象来选定其中之一
		然后调用 返回的 JDK 代理 或者 CGLIB 代理 对象的 getProxy() 创建代理类
	*/
    return createAopProxy().getProxy(classLoader);
}
```



createAopProxy() 方法如下：

它主要是获取 ProxyCreatorSupport 中维护的工厂对象 DefaultAopProxyFactory

然后调用这个工厂对象的 createAopProxy()

```java
// createAopProxy() 在 ProxyCreatorSupport 类内部，之前说了 proxyFactory 是 ProxyCreatorSupport 的一个子类
public class ProxyCreatorSupport extends AdvisedSupport {

    private AopProxyFactory aopProxyFactory;

    /**
	 * Create a new ProxyCreatorSupport instance.
	 */
    public ProxyCreatorSupport() {
        //DefaultAopProxyFactory 是生成 JDK 代理 和 CGLIB 代理 对象 的工厂对象，
        // 内部的 createAopProxy() 就是决定使用 JDK 代理 还是 CGLIB 代理的
        this.aopProxyFactory = new DefaultAopProxyFactory();
    }
    
    protected final synchronized AopProxy createAopProxy() {
        /*
            调用 aopProxyFactory 这个工厂对象的 createAopProxy() 方法选定 JDK 代理 和 CGLIB 代理
            然后将选定的 JDK 代理 或者 CGLIB 代理返回
		 */
        return this.aopProxyFactory.createAopProxy(this);
    }
}
```



工厂对象 DefaultAopProxyFactory 类内部如下：

可以发现它内部是在选定使用的是 JDK 动态代理还是 CGLIB 动态代理

因此 DefaultAopProxyFactory 的作用就是根据 bean 的情况选择 AOP 代理对象实现是 JDK 还是 CGLIB，然后将选定的 AOP 对象实现者 返回

```java
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	//创建动态代理对象 -- JDK、CGLIB
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		/*
		isProxyTargetClass() 即为我们之前强制设置的 proxyTargetClass = true，默认为 false
		几种情况：
		如果 proxyTargetClass 没有设置，默认为 false，
			如果 目标对象没有实现接口，那么使用 CGLIB
			如果 目标对象实现了接口，那么使用 JDK
		如果 proxyTargetClass 设置为 ture
			如果目标对象是一个接口，那么使用 JDK（少见）
			其余情况使用 CGLIB
		这里其实说明了如果设置 proxyTargetClass = true，那么就是强制使用了 CGLIB 代理
		 */
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			//如果 目标对象 是接口，那么使用 JDK 动态代理....明显是不可能的，怎么可能会是一个接口
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			//使用 CGLIB 代理
			return new ObjenesisCglibAopProxy(config);
		}
		else {
            //proxyTargetClass == false 并且 实现了接口，那么选择 JDK 动态代理
			return new JdkDynamicAopProxy(config);
		}
	}

	//判断目标对象是否 没有 实现接口，如果没有实现接口返回 true
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
```



综上，最开始的 createAopProxy() 方法的目的就是为了获取一个工厂对象，然后让这个工厂对象根据 bean 的情况选定一个用来实现 AOP 对象的方式：JDK 动态代理 或者 CGLIB 代理，将这个实现封装的 JDK 类 和 CGLIB 类返回

然后调用这个类的 **getProxy() 方法，真正的创建出 bean 的代理对象**，getProxy() 方法是 JDK 和 CGLIB 各自的实现逻辑了



上面需要注意的是，创建 JDK 和 CGLIB 的时候，会传入一个 config，这个 config 是 AdvisedSupport 类型的

它内部维护了 bean 的所有接口 和 所有 AOP 通知，因此后续 AOP 对象创建根据这个 config 来执行代理逻辑即可

```java
public class AdvisedSupport extends ProxyConfig implements Advised {
	
    // bean 的接口
    private List<Class<?>> interfaces = new ArrayList<>();
	// bean 的所有 AOP 通知
    private List<Advisor> advisors = new ArrayList<>();
    
    //xxxx
}
```





## 4、AOP 中的 Advisor

Advisor 这个类，它是用来封装 @Before、@After 等注解的切入点的

比如下面这个就有两个切入点，分别是 @Before 注解的 beforeAAA() 和 @After 注解的 afterAAA()

```java
@Component
@Aspect
public class DaoAspect {

	@Pointcut("execution(* com.luban.spring.dao.*.*(..))")
	public void pointCut() {
		System.out.println("切面 1");
	}


	@Before("pointCut()")
	public void beforeAAA() {
		System.out.println("before");
	}

	@After("pointCut()")
	public void afterAAA() {
		System.out.println("after");
	}
}
```



Advisor 是一个接口，AOP 中它的实现类是 `InstantiationModelAwarePointcutAdvisorImpl`，内部数据结构如下：

```java
class InstantiationModelAwarePointcutAdvisorImpl
		implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation, Serializable {

	private static final Advice EMPTY_ADVICE = new Advice() {};
	
    //当前切入点指向指定的切入面，@Pointcut
	private final AspectJExpressionPointcut declaredPointcut;

    //属于该切入点的切面的 切面名，这里是 daoAspect
	private final String aspectName;
    
    //当前切入点切入的方法名，这里是 beforeAAA 或者 afterAAA
	private final String methodName;

    //在这个字段中记录了 当前切入点的类型：Before、After 等
	private transient Method aspectJAdviceMethod;

	
    //do something
}
```

一个 Advisor 封装了一整个切入点，比如 通知类型、切入的方法、切入面（切入哪个类的哪个方法）等



在 wrapIfNecessary() 中会调用 getAdvicesAndAdvisorsForBean() 获取 bean 的 Advisor 数组

获取到后将 Advisor 数组赋值给 proxyFactory，这样在后面根据这个 advisor 数组进行 AOP 代理

```java
@Override
@Nullable
protected Object[] getAdvicesAndAdvisorsForBean(
    Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
    //找到 bean 所有的 AOP 切入点 advisor
    List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
    if (advisors.isEmpty()) {
        return DO_NOT_PROXY;
    }
    return advisors.toArray();
}

//在上面的 findEligibleAdvisors() 方法中，最终经过一层层封装会来到这个方法
public List<Advisor> buildAspectJAdvisors() {
    //aspectBeanNames 存储了 bean 所有需要切入的 切面（@Aspect，这里目前只有 DaoAspect）
    List<String> aspectNames = this.aspectBeanNames;
	
    if (aspectNames.isEmpty()) {
        return Collections.emptyList();
    }
    //advisors 用来存储所有的 advisor（@Before 等的切入点）
    List<Advisor> advisors = new ArrayList<>();
    //遍历所有的切面
    for (String aspectName : aspectNames) {
        //获取这个切面对应的所有 advisor
        List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
        if (cachedAdvisors != null) {
            //添加到 advisors 中
            advisors.addAll(cachedAdvisors);
        }
        else {
            MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
            advisors.addAll(this.advisorFactory.getAdvisors(factory));
        }
    }
    //返回 advisor 集合
    return advisors;
}
```

