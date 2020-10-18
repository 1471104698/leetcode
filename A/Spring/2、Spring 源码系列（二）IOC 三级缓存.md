# IOC 三级缓存



## 1、三级缓存的作用

```java
//存储 成品 bean，从该缓存中取出的 bean可以直接使用
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

//存放实例化但未初始化，即还没有属性注入的 bean 早期对象 用于解决循环依赖
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

//存放 singletonFactory 对象解决循环依赖
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);


```

| 名称                  | 作用                                                         |
| --------------------- | ------------------------------------------------------------ |
| singletonObjects      | 存放初始化后的单例对象，也就是完成的bean对象                 |
| earlySingletonObjects | 存放实例化，未完成初始化的单例对象（未完成属性注入的对象），也是用来解决性能问题 |
| singletonFactories    | 存放ObjectFactory对象，存放的是工厂对象，也是用来解决aop的问题 |

三级缓存都是 map 对象

第一级缓存 存储 最终 bean 创建完成后存放的 map，某种意义上是真正的 IOC 容器

第二级缓存 和 第三级缓存 是用来解决循环依赖的，在其他情况下目前没有什么作用



## 2、三级缓存解决循环依赖

这里先说下什么是循环依赖，循环依赖就是 A 需要注入 B，B 需要注入 A，这样的话在创建 A 的 bean 的时候，就需要去生成 B 的 bean 来注入，而生成 B 的 bean 的过程中又会发生需要生成 A 的 bean 来注入，这样粗略看来好像会陷入死循环，那么 spring 如何解决的？

以下是主要的代码：

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd){
    
    //...
    
    /*
    内部 第二次调用后置处理器  推断构造方法， 用来决定哪个使用哪个构造方法，然后通过反射实例化对象
    */
    instanceWrapper = createBeanInstance(beanName, mbd, args);
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
    //第四次调用后置处理器 （跟前面第二次调用的后置处理器一致，调用的方法不同）
    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    
    //属性注入，同时也是循环依赖的发生时机
    populateBean(beanName, mbd, instanceWrapper);
}

protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    synchronized (this.singletonObjects) {
        if (!this.singletonObjects.containsKey(beanName)) {
            //将 bean 所在的 singletonFactory 放入到第三级缓存中
            this.singletonFactories.put(beanName, singletonFactory);
            //如果二级缓存存在这个 bean，那么移除，因此现在不需要在二级缓存中存在
            this.earlySingletonObjects.remove(beanName);
        }
    }
}

@Nullable
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    //从map中获取bean如果不为空直接返回，不再进行初始化工作
    //讲道理一个程序员提供的对象这里一般都是为空的
    Object singletonObject = this.singletonObjects.get(beanName);
    //一级缓存为空
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            //二级缓存为空
            if (singletonObject == null && allowEarlyReference) {
                //从三级缓存中获取 bean 对应的 singletonFactory
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    //从 singletonFactory 中获取 bean，放入到二级缓存中
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    //将 bean 对应的单例工厂 singletonFactory 从三级缓存中删除
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

流程解析：

- 调用 doGreatBean() 的时候，会使用 后置处理器 实例化 bean（还没有初始化）

- 在后面再调用 addSingletonFactory() 将 bean 封装为一个 单例工厂 singletonFactory 存储进第三级缓存中，相当于是暴露早期对象

- 在后面再调用 populateBean() 进行属性注入，这时候会发生循环依赖

  ```java
  假设 A 需要注入 B，B 需要注入 A
  当前正在创建 A bean，属性注入的时候发现需要 B bean，因此调用 getBean()，再次走一遍流程，
  而在 B 进行属性注入的时候，发现需要 A bean，因此调用 getBean()，而此时在 doGetBean() 最前面会调用 getSingleton()
  
  getSingleton() 执行流程如下：
  从一级缓存获取，发现为 null
  从二级缓存获取，发现为 null
  从三级缓存获取，不为 null，因为 A 在前面就已经添加进三级缓存，暴露早期对象了，就是为了这时候 B 来获取 A
  然后从三级缓存中获取到 A bean 的 singletonFactory，通过这个单例工厂获取到 A bean
  将 A bean 存储到二级缓存中，再将 A bean 的 singletonFactory 从三级缓存中移除
      然后将 A bean 返回，注入到 B 中
  完美解决了循环依赖
  ```

这里需要说一点，B 中存储的是 A 的半成品，那么不是有问题吗？

其实不会，这涉及到 **引用传递 **了，B 中的 A a 指针指向的是 A bean 的内存地址，而后续对 A bean 初始化也是对这个地址上的 对象进行初始化，因此当 A bean 初始化完毕后，对应 B 中的 a 也是初始化完毕了的

```java
//注：java 只有值传递，所谓的引用传递实际上是拷贝了对象的内存地址来达到引用传递的效果
```



## 3、第三级缓存的作用

可以很容易看出，第三级缓存 存储的是 singletonFactory 的，而这个 singletonFactory 内部存储了 半成品 bean，后续循环依赖的时候就调用 getSingleton() 获取将 bean 返回，那么这个 singletonFactory 有什么意义？暴露早期对象的时候直接存储进 第二级缓存 不就 好了吗？

其实不然，如果 B 需要注入的 A bean 是一个普通的 bean 的话，这个第三级缓存确实是一个鸡肋

但是，如果这个 A 是一个需要 AOP 的对象呢？

在第二次后置处理器实例化出来的 bean 是一个原始对象，不是一个 AOP 对象，因此如果 A 是一个 AOP 对象，那么如果直接放入二级缓存中，那么 B 获取到的 A bean 就是一个原始对象，而不是 A 的一个 AOP 对象，这显然是不可行的

因此，spring 设计出了三级缓存，三级缓存不是直接存储 bean，而是存储一个 工厂，它用来对 bean 进行加工，在内部会调用 

getEarlyBeanReference()，而正是这个方法，在内部会调用 后置处理器 生成 A 的一个 AOP 对象

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            //第四次调用后置处理器 SmartInstantiationAwareBeanPostProcessor -- getEarlyBeanReference()，
            // 解决循环依赖，得到一个提前暴露的对象 -- 不是bean
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                //将生成的代理对象赋值给 exposedObject 返回
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    //在上面如果不需要代理，那么这里的 exposedObject 就是原始对象，如果需要代理，那么这里的就是 AOP 对象
    return exposedObject;
}
```

它不会立即生成 AOP 对象，而是在循环依赖的时候调用了对应的 bean 它才会使用这个工厂去生成 AOP 对象，然后放入二级缓存中





## 4、无法解决的循环依赖

之前说了 DI 的注入类型有两种：

- 构造注入
- setter 注入（原理是反射注入，通过反射调用 setter()，如果没有 setter()  就反射调用 field 的 set() -- @Autowire 注入方式）

而实际上 setter 注入 能够解决循环依赖，构造注入的循环依赖是无法解决的



```java
@Component
class A  {
	B b;
	public A(B b){ }
}
@Component
class B{
	A a;
	public B(A a){ }
}
```

输出结果：

```java
//Error creating bean with name 'a': Requested bean is currently in creation: Is there an unresolvable circular reference?
```

以上是构造注入，由于 A 添加了 @Component 注解，因此会在 spring 启动时创建 bean，调用构造方法的时候发现只有一个有参的构造方法，因此会创建 B bean，请注意，A 这时候还没有创建出 A bean，因为它还没有调用构造方法成功，因此在创建 B bean 的时候，同样的去获取 A bean，而 A bean 又没有创建出来，因此这个循环依赖是解决不了的



同时，如果注入的是一个 原型 bean 的话，也是无法注入成功的

```java
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class A  {
	@Autowired
	B b;
}
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class B{
	@Autowired
	A a;
}
```

这样普通的启动过程不会报错，但是，如果我们开始使用 A bean 或 B bean 的话，就会报错

```java
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class A  {
    @Autowired
    B b;
}
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class B{
    @Autowired
    A a;
}
public static void main(String[] args) {
    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
    ac.register(Appconfig.class);
    ac.refresh();
    //获取 A bean
    A a = (A)ac.getBean("a");
    System.out.println(a.b);
}

```

输出结果：

```java
//Error creating bean with name 'a': Requested bean is currently in creation: Is there an unresolvable circular reference?
```

这是因为 spring 启动的时候只会创建单例 bean，对于原型的 bean 只有在使用的时候才会去创建，由于原型 bean 是能够创建多个的，因此 spring 不会去存储这些 bean，因此在 A bean 发现需要注入 B 的时候，会去创建 B bean，而 B bean 也是原型 bean，因此 spring 没有管理，所以需要创建，后面 B bean 发现需要注入 A，因此会去创建 A bean，而这时就出现问题了，因为 spring 没有存储 A bean 的半成品，所以会导致再次创建 A bean，所以会导致死循环，因此循环依赖解决不了



原型 bean 的前提是两个循环依赖的 bean 都是原型，如果一个不是原型的话那么就没有问题

比如 A bean 是原型，B bean 是单例，这样的话在使用 A bean 前 B bean 就已经创建好了

在创建 B bean 的时候由于 A bean 是原型，但是也是可以直接创建的，只是不需要存储起来而已

因此没有任何问题



**为什么不使用提前暴露 A bean 早期对象的方法来解决 原型 的循环依赖问题？**

因为 A bean 是原型的，它不是在 spring 启动的时候进行创建的，而是在代码运行的时候进行创建的，这样的话就会存在多线程，如果使用提前暴露早期 bean 的方法，那么线程 1 放入的 A bean 半成品，不是也可能会被 线程 2 拿到吗？这样的话 A bean 就不是原型的了，所谓的原型就是 每个地方、每个线程 的使用都需要是不同的对象