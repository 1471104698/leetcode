# IOC 九次后置处理器调用



## 1、第一次调用后置处理器

createBean() 前面会先 第一次调用后置处理器，这里的后置处理器有两个作用：

- 可以按照自己的逻辑创建 指定 beanName 应的 bean，这样在这个 bean 创建的时候就会在这里创建了，而不会再去调用 doCreateBean() 去调用其他的后置处理器了，直接返回这个 bean
  - （但目前这里没有任何作用，所有的后置处理器都默认返回 null，除非程序员自己定义逻辑）
- 选出 %100 不需要被增强的类，比如 @Aspect 注解的切面类，然后将这个 beanName 存储到一个 map 中，value = false，这样在创建 AOP 对象的过程中会判断通过这个 map 直接跳过

```java
//第一次调用后置处理器 InstantiationAwareBeanPostProcessor
// 在 bean 初始化前应用后置处理，如果后置处理返回的 bean 不为空，则直接返回
// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
//当第一次调用后置处理器，返回了一个 bean，那么这里直接返回了，不会再去创建 bean 了，即不会再调用实例化 bean 和 后面的所有的后置处理器了
if (bean != null) {
    return bean;
}
```



第一次后置处理器调用逻辑如下：

```java
@Nullable
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        // Make sure bean class is actually resolved at this point.
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = determineTargetType(beanName, mbd);
            if (targetType != null) {
                //出现的第一个后置处理器 -- InstantiationAwareBeanPostProcessor
                bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
            }
        }
        mbd.beforeInstantiationResolved = (bean != null);
    }
    return bean;
}

@Nullable
protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
        //出现的第一个后置处理器 -- InstantiationAwareBeanPostProcessor
        if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
            //这里主要是调用 AbstractProxyCreater 的方法
            Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
            if (result != null) {
                return result;
            }
        }
    }
    return null;
}

//以下是 AbstractProxyCreater 中的方法
@Override
public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
    Object cacheKey = getCacheKey(beanClass, beanName);

    if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
        if (this.advisedBeans.containsKey(cacheKey)) {
            return null;
        }
        /*
			Spring 判断该 bean 100% 不需要增强，
			那么会存储到 advisedBeans 这个 map 中，并且 value = true，在后续进行增强的时候可以直接跳过
			像这种 100% 确定的 bean 有 @Aspect 注解的切面等
			 */
        if (isInfrastructureClass(beanClass)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return null;
        }
    }
    return null;
}
//下面只要有一个满足，那么就不需要增强
protected boolean isInfrastructureClass(Class<?> beanClass) {
    boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
        Pointcut.class.isAssignableFrom(beanClass) ||
            Advisor.class.isAssignableFrom(beanClass) ||
                AopInfrastructureBean.class.isAssignableFrom(beanClass);
    return retVal;
}
```



## 2、第二次调用后置处理器

调用 第二次后置处理器，推断创建 bean 的构造器，然后通过选定的 构造器 创建一个 原始 bean

```java
/**
			 * 创建 bean 实例，并将实例包裹在 BeanWrapper 实现类对象中返回。
			 * createBeanInstance中包含三种创建 bean 实例的方式：
			 *   1. 通过工厂方法创建 bean 实例
			 *   2. 通过构造方法自动注入（autowire by constructor）的方式创建 bean 实例
			 *   3. 通过无参构造方法方法创建 bean 实例
			 *
			 * 若 bean 的配置信息中配置了 lookup-method 和 replace-method，则会使用 CGLIB
			 * 增强 bean 实例。关于lookup-method和replace-method后面再说。
			 */
			//调用 构造方法创建 实例
			//内部 第二次调用后置处理器  SmartInstantiationAwareBeanPostProcessor 推断构造方法，用来决定哪个使用哪个构造方法进行 new
			instanceWrapper = createBeanInstance(beanName, mbd, args);
```



## 3、第三次调用后置处理器

在调用第二次后置处理器实例化 原始 bean 后，会调用第三次后置处理器，里面主要的两个后置处理器为

- CommonAnnotationBeanPostProcessor ：找到 bean 所有的 @Resource、@PostConstruct、@PreDestroy 切入点
- AutowiredAnnotationBeanPostProcessor ：找到 bean 所有的 @Autowire 切入点

对于 @Resource 和 @Autowire 的切入点，它们都是封装成一个个的 injectedElement，存储到 `Collection<InjectedElement> injectedElements` ，然后将这个 injectedElements 封装到 InjectionMetadata 中，

**用于第六次后置处理器进行属性注入**

```java
public class InjectionMetadata {

	private final Class<?> targetClass;
	//bean 的切入点集合
	private final Collection<InjectedElement> injectedElements;
}
```

它们内部都维护了一个 map，存储每个 bean 与之对应的 InjectionMetadata 

```java
public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor
		implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {
    //存储每个 bean @Resource 注解注入点列表
    private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
}
```

```java
public class AutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {
    //存储每个 bean @Autowire注解注入点列表
    private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
}
```



对于 @PostConstruct、@PreDestroy 切入点，他们封装成一个个 LifecycleElement，存储到 `Set<LifecycleElement> initMethods` 和 `Set<LifecycleElement> destroyMethods;`，然后将这两个集合封装到 LifecycleMetadata 中，同样是使用 map 存储，一个 bean 对应一个 LifecycleMetadata 

**@PostConstruct 在 第七次后置处理器前调，@PreDestroy 在第九次后置处理器调用** 

```java
private class LifecycleMetadata {

    private final Class<?> targetClass;
    //存储 @PostConstruct
    private final Collection<LifecycleElement> initMethods;
    //存储 @PreDestroy
    private final Collection<LifecycleElement> destroyMethods;

}

//该类是 CommonAnnotationBeanPostProcessor 的是父类
public class InitDestroyAnnotationBeanPostProcessor
    implements DestructionAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor{

    //存储某个 bean 所有的 @PostConstruct 和 @PreDestroy 切入点
    private Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);
}
```





需要注意的是，@Autowire 和 @Resource 注入的时候会跳过 static 字段和方法，比如静态字段，在类加载时期就已经准备好了

```java
	private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
		/*
		elements 用来存储当前 bean 的 @Autowire 所有注入点
		我们 @Autowire 注解的字段和方法 能够注入 就是因为在这里提前找到了所有的注入点，
		将 @Autowire 注解的方法和字段封装成了一个个的 InjectedElement，然后整合到结构体 InjectionMetadata 中
		*/
		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;

		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();
		//处理所有被 @Autowire 标注的字段
			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				//找到当前字段的 @Autowire 注解
				AnnotationAttributes ann = findAutowiredAnnotation(field);
				if (ann != null) {
					//跳过静态字段
					if (Modifier.isStatic(field.getModifiers())) {
						if (logger.isWarnEnabled()) {
							//打印出静态字段不支持注解，因为静态字段是在类加载阶段赋值的
							logger.warn("Autowired annotation is not supported on static fields: " + field);
						}
						return;
					}
					boolean required = determineRequiredStatus(ann);
					currElements.add(new AutowiredFieldElement(field, required));
				}
			});
		//处理所有被 @Autowire 标注的方法
			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				AnnotationAttributes ann = findAutowiredAnnotation(bridgedMethod);
				if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					//跳过静态方法
					if (Modifier.isStatic(method.getModifiers())) {
                        //打印 @Autowire 不支持静态方法
						if (logger.isWarnEnabled()) {
							logger.warn("Autowired annotation is not supported on static methods: " + method);
						}
						return;
					}
					boolean required = determineRequiredStatus(ann);
					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
					currElements.add(new AutowiredMethodElement(method, required, pd));
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new InjectionMetadata(clazz, elements);
	}
```



## 4、第四次后置处理器调用

如果 bean 支持循环依赖，那么将 bean 封装到一个 ObjectFactory 接口的匿名实现类中，我们称作 单例工厂 singletonFactory 中，然后将 单例工厂 存储进 第三级缓存中，后续在循环依赖的直接从三级缓存中获取 单例工厂，调用 getObject() 获取 bean

这时候在 getObject() 会调用 getEarlyBeanReference() ，如果 bean 是一个普通的 bean，那么直接返回

如果 bean 需要 AOP 代理，那么会调用 第四次后置处理器，产生一个 AOP 对象，注入到循环依赖的 bean 中

```java
//判断是否支持循环依赖，关键是 this.allowCircularReferences（这里用到，在 第一个 getSingleton() 里也用到，那里传的这个参数是写死 true）
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			/*
			暴露早期对象，用来解决循环依赖
			这里的 () -> 是创建一个 ObjectFactory 接口的匿名实现类，我们称作单例工厂 singletonFactory，
			ObjectFactory 接口如下：
			@FunctionalInterface
			public interface ObjectFactory<T> {
				T getObject() throws BeansException;
			}
			因此这里是将 getEarlyBeanReference() 作为 getObject() 的实现，即
			public T getObject(){
				return getEarlyBeanReference();
			}
			这个工厂对象在获取 bean 的时候会调用 getObject()，然后内部还会调用 getEarlyBeanReference()
			这时会对 bean 进行加工处理

			addSingletonFactory() 是将 singletonFactory 添加到三级缓存中
        */
			//第四次调用后置处理器 -- SmartInstantiationAwareBeanPostProcessor（跟前面第二次调用的后置处理器一致，调用的方法不同）
			//如果支持循环依赖，则将该 未完成的 bean 存放进 三级缓存（工厂）
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}
```



第四次后置处理器调用逻辑如下：

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    //判断是否需要增强
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            /*
				这里主要是调用后置处理器 AnnotatonAwareAspectAutoProxyCreater
				在内部会创建一个 AOP 对象返回
				并将 beanName 存储进 earlyProxyReferences 中，表示
				*/
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                //将生成的代理对象赋值给 exposedObject 返回
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
}

//AnnotatonAwareAspectAutoProxyCreater 类的抽象父类 AbstractAutoProxyCreator
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        /*
        判断不在 earlyProxyReferences 中，那么将当前 beanName 添加进 earlyProxyReferences
        这样在第八次调用后置处理器创建 AOP 对象的时候可以根据这个 earlyProxyReferences 来跳过，无需重复创建 AOP 对象
        */
        if (!this.earlyProxyReferences.contains(cacheKey)) {
            this.earlyProxyReferences.add(cacheKey);
        }
        //调用 wrapIfNecessary() 创建 AOP 对象
        return wrapIfNecessary(bean, beanName, cacheKey);
    }
}
```



## 5、第五、六次后置处理器调用

在 populateBean() 方法中进行属性注入，这时候会调用第五、六次后置处理器

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {

    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.
    /*
        第五次调用后置处理器 -- InstantiationAwareBeanPostProcessor
        上面的英文翻译过来就是，给 InstantiationAwareBeanPostProcessor 类型的后置处理器一个机会
        在属性注入前修改 bean，即程序员可以自己实现一个 InstantiationAwareBeanPostProcessor
        然后在内部自己对某个 bean 进行一些修改，还可以返回 false
        如果返回了 false，那么 continueWithPropertyPopulation 就变成了 false，那么就会直接返回，
        不会调用第六次后置处理器进行属性注入
        */
    boolean continueWithPropertyPopulation = true;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        //后置处理器遍历
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }
    }
    // continueWithPropertyPopulation == false，直接返回
    if (!continueWithPropertyPopulation) {
        return;
    }

    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

    if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
        // Add property values based on autowire by name if applicable.
        if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }
        // Add property values based on autowire by type if applicable.
        if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }
        pvs = newPvs;
    }

    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

    if (hasInstAwareBpps || needsDepCheck) {
        if (pvs == null) {
            pvs = mbd.getPropertyValues();
        }
        /*
            第六次调用后置处理器，对 @Autowrire 和 @Resource 注解的方法和字段 进行注入
            CommonAnnotationBeanPostProcessor 后置处理器处理 @Resource
            AutowiredAnnotationBeanPostProcessor 后置处理器处理 @Autowire
            对于 @Autowire，第三次后置处理器将 @Autowire 注解的每个方法和字段封装成了一个个 injectedElements
            然后存储进 InjectionMetadata 类，变成 Collection<InjectedElement> injectedElements;
            这样 AutowiredAnnotationBeanPostProcessor 就是直接获取 InjectionMetadata 然后进行处理
            */
        PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
        if (hasInstAwareBpps) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                    if (pvs == null) {
                        return;
                    }
                }
            }
        }
    }
    if (pvs != null) {
        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}
```



## 6、第七、八次调用后置处理器

在属性注入之后，会调用 initializeBean() 进行 bean 的初始化

第七次后置处理器会回调 @PostConstruct 方法

第八次后置处理器会产生 AOP 对象，但如果第四次后置处理器中产生了 AOP 对象，那么这里第八次后置处理器就不会产生 AOP 对象，即只能二者选一

```java
protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
    
    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        //第七次执行后置处理器
        //主要是调用 CommonAnnotationBeanPostProcessor 后置处理器 回调 bean 的 @PostConstruct 切入点
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        //执行bean的声明周期回调中的 init 方法
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
    }
    if (mbd == null || !mbd.isSynthetic()) {
        //第八次执行后置处理器
        //执行后置处理器的 after()	-- 创建 aop 代理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```



第八次后置处理器调用的方法如下：可以发现

- 如果第四次后置处理器创建了 AOP 对象，那么会直接返回，不会再次创建
- 如果第一次后置处理器判定 这个 bean %100 不需要增强，那么也会直接返回

```java
@Override
public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) throws BeansException {
    if (bean != null) {
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        /*
        这里 earlyProxyReferences 存储的是 第四次后置处理器创建的 AOP 对象，
        这里判断是否已经在第四次后置处理器创建了 AOP 对象，如果创建了那么就不再次创建了
        */
        if (!this.earlyProxyReferences.contains(cacheKey)) {
            //调用 wrapIfNecessary() 创建 AOP 对象
            return wrapIfNecessary(bean, beanName, cacheKey);
        }
    }
    return bean;
}

protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
        return bean;
    }
    //这里判断如果在 第一次后置处理器 中已经标记为 %100 不会被增强，那么直接返回
    if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
        return bean;
    }
    if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    // Create proxy if we have advice.
    //创建代理
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
    if (specificInterceptors != DO_NOT_PROXY) {
        this.advisedBeans.put(cacheKey, Boolean.TRUE);
        Object proxy = createProxy(
            bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    }

    this.advisedBeans.put(cacheKey, Boolean.FALSE);
    return bean;
}
```



## 7、第九次后置处理器

在销毁 bean 比如调用 ac.close()，会调用 InitDestroyAnnotationBeanPostProcessor 后置处理器去回调 bean 的 @PreDestroy 注解的方法

调用流程如下：

```java
@Override
public void close() {
    synchronized (this.startupShutdownMonitor) {
        doClose();
    }
}
protected void doClose() {
    //do something 

    destroyBeans();

    //do something
}

protected void destroyBeans() {
    getBeanFactory().destroySingletons();
}

public void destroySingletons() {

    String[] disposableBeanNames;
    synchronized (this.disposableBeans) {
        disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
    }
    //获取所有存在 @PreDestroy 注解的 bean，然后调用 destroy() 去执行这些方法
    for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
        destroy(disposableBeanNames[i]);
    }
}

public void destroy() {
    if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
        /*
        后置处理器 InitDestroyAnnotationBeanPostProcessor 调用 postProcessBeforeDestruction()
        在方法内执行 bean 的 @PreDestroy 方法
        */
        for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
            processor.postProcessBeforeDestruction(this.bean, this.beanName);
        }
    }
    //do something
}

@Override
public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
    try {
        //调用 invokeDestroyMethods() 内部真正执行 @PreDestroy 方法
        metadata.invokeDestroyMethods(bean, beanName);
    }
    catch (InvocationTargetException ex) {
        //do something
    }

}

public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
    Collection<LifecycleElement> checkedDestroyMethods = this.checkedDestroyMethods;
    Collection<LifecycleElement> destroyMethodsToUse =
        (checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
    if (!destroyMethodsToUse.isEmpty()) {
        //遍历该 bean 所有的 @PreDestroy 方法，然后调用它们
        for (LifecycleElement element : destroyMethodsToUse) {
            element.invoke(target);
        }
    }
}
```

