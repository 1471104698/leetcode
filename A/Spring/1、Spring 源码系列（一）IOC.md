# Spring IOC



## 1、IOC 的作用

> #### IOC 的作用一：无需硬编码

IOC 的好处就是，当存在 10 个类内部引用了 UserDao 这个类的时候，我们不需要硬编码去指定引用的是哪个对象

只需要使用一个注解 @Autowire，表示注入对应的实现对象

```java
class A{
    @Autowire
	UserDao userdao;
}
class B{
    @Autowire
	UserDao userdao;
}
class C{
    @Autowire
	UserDao userdao;
}
//剩下 7 个类
//....xxx
```



而对应的实现类对象，我们只需要在类上添加 @Component 让 Spring 通过 IOC 创建存储到 IOC 容器中，然后后续自动注入

```java
interface UserDao{
 	//xxx   
}
@Component
class  FuxkUserDao implements UserDao{
    //xxx
}
```

对于上面这个例子，我们注入的就是 FuxkUserDao 对象

但是，假如我们后续不想用 FuxkUserDao 作为实现类了，而是想要一个注入 ShitUserDao 作为实现类

那么我们只需要在 ShitUserDao 类上添加 @Component，在 A、B、C 类中的对象创建代码全部无需改动

```java
interface UserDao{
 	//xxx   
}
class  FuxkUserDao implements UserDao{
    //xxx
}
@Component
class  ShitUserDao  implements UserDao{
    //xxx
}
```



> #### IOC 的作用二：自动解决多层依赖

这个其实也算是解决硬编码，不过跟上面的硬编码方面属于不同方面的，因此单独拿出来讲

假如： A 依赖 B， B 依赖 C，C 依赖 D，这样的话，当我们需要一个 A 的时候，一般情况下我们需要这么做：

```java
class Main{
	A a = new A(new B(new C(new D())));
}
```

而一旦 D 中需要再引用一个 E，或者 不再需要 D，这样就需要去改动代码了：

```java
//D 引用一个 E
class Main{
	A a = new A(new B(new C(new D(new E()))));
}
//不需要 D
class Main{
    A a = new A(new B(new C()));
}
```

而可能这个 A 类在很多地方都需要用到，这样就需要多处改动代码。

当然，这样的话，我们可能就会想到使用 **工厂模式**，将创建 A 的逻辑给封装起来，这样的话，所有地方调用都只需要调用这个方法获取一个 A 对象，而我们修改也只需要修改这个方法即可

```java
class AFactory{
	public static A createA(){
        return new A(new B(new C(new D())));
    }
}
```



同理，**IOC 内部创建 bean 也是使用了 工厂模式 中的简单工厂模式**，只对外提供一个 getBean()，bean 的创建在 getBean() 中进行封装，通过方法入参来标识需要创建的对象

大致逻辑如下：

（上面的是构造传参，这里就不说构造传参了，说变量注入，原理都是通过反射）

在生成 bean 的时候会获取所有的成员变量，判断是否需要注入，如果需要，那么就创建注入对象进行注入

```java
class BeanFactory{
    public<T> T	createBean(String className){
        try{
            Class<?> clazz = Class.forName(className);
            Object bean = clazz.newInstance();
            //处理属性注入
            processBean(clazz, bean);
            return (T)bean;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    private void processBean(Class<?> clazz, Object o) throws IllegalAccessException {
        //获取成员变量
        Field[] fs = clazz.getDeclaredFields();
        for(Field field : fs){
            //获取所有的注解
            Annotation[] annotations = field.getAnnotations();
            //判断是否存在 Autowire
            for(Annotation annotation : annotations){
                //如果存在，那么初始化
                if("Autowire".equals(annotation)){
                    Object bean = createBean(field.getClass().getName());
                    //将 bean 注入进去
                    field.set(o, bean);
                }
            }
        }
    }
}
```

IOC 会自动在 C 中注入 D，在 B 中注入 C，在 A 中注入 B，然后将 A 返回，无需我们自己手动添加这层关系

 



## 2、IOC 和 DI 的关系

IOC 即 控制反转，创建对象的操作不再需要程序员来完成，这是一种设计思想，就类似 JVM 的方法区，它需要有一个具体的实现

DI 即  **Dependency Injection** ，依赖注入，通过 DI 的方式来实现 IOC。

在 Bean 创建过程中，判断该 Bean 依赖其他哪些 Bean，那么去创建依赖的 Bean，然后通过 各种注入方法 设置到 该 Bean 对应的 field 中。这里就是 IOC 所谓的 由 Spring 来控制对象的创建的设计思想的体现，而实现它的是这个自动查找依赖的对象，并且根据不同的方式设置到创建对象的对应 field 中的 DI



DI 三种方式：

- 构造注入（调用构造器时注入）
- setter 注入（反射调用 setter 方法注入）
- @Autowire的 field 注入（反射获取 field，然后填充对象）





## 3、BeanFactory 和 ApplicationContext

[从源码看待 BeanFactory 和 ApplicationContext](https://cloud.tencent.com/developer/article/1574870)



Spring 中定义了 BeanFactory 和 ApplicationContext 两种接口，好像之前是把这两种当作两种不同的 IOC 容器的



ApplicationContext 继承了 BeanFactory 接口，即 **ApplicationContext 本身是对 BeanFactory 的扩展**

ApplicationContext 和 BeanFactory 的继承关系如下：

 ![img](https://pic4.zhimg.com/80/v2-1006341abadfd3466b5b4587f349ab27_720w.jpg?source=1940ef5c) 

BeanFactory 作为一个 Bean 工厂，只具备创建 Bean 的功能，不支持 AOP、事件发布（监听器）等功能

而我们常用的 ApplicationContext ： AnnotationConfigApplicationContext，它具有 BeanFactory 的创建 bean 的功能，并且还添加了 AOP 和 事件发布 功能，同时它内部还维护了一个真正的 BeanFactory 



AnnotationConfigApplicationContext 继承了一个 GenericApplicationContext 类

```java
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {
```

在 GenericApplicationContext 类中维护了一个 DefaultListableBeanFactory ，它是一个真正的 beanFactroy，是 IOC 的核心，ApplicationContext 的 getBean() 逻辑就是获取这个 BeanFactory 并且调用它的 getBean() 来创建 Bean 的

DefaultListableBeanFactory 中维护了所有 Bean 的 BeanDefinition，后续调用 getBean() 时就是使用 BeanDefinition 中的信息来创建 Bean

```java
public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {
	//beanFactory
	private final DefaultListableBeanFactory beanFactory;
}
```



同时 GenericApplicationContext 的父类 AbstractApplicationContext 中维护了所有的后置处理器 以及 事件广播器 和 所有注册的监听器 Listener

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
    implements ConfigurableApplicationContext {

    //存储所有的后置处理器
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

    //监听广播器，当有消息的时候，会找到所有的监听该事件的监听器 Listener，然后回调它们
    private ApplicationEventMulticaster applicationEventMulticaster;

    //存储所有的监听器 Listener
    private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
```



> #### IOC 创建 Bean 和 存储 Bean 的类

在 Spring 中，创建 Bean 和 存储创建好的 Bean 使用的是不同的类

**创建 Bean 使用的是 BeanFactory，即 GenericApplicationContext 中维护的 DefaultListableBeanFactory** ，它负责创建 Bean 的逻辑

**而创建好的 Bean 不会存储在 DefaultListableBeanFactory 中，而是存储到 IOC 三级缓存中的 第一级缓存 SingletonObjects 中**

**我们称它为单例池**

当 Spring 启动完成，所有的 Bean 都创建好后，程序员调用 getBean() 获取 Bean 实际上是在 SingletonObjects 中获取已经创建好的 Bean





## 4、bean 的生命周期（重点）



我这里只会讲个大概的思路，不会细讲每个细节（因为我也不可能知道每个细节），不过这个思路足够理解整个 IOC 了

整个流程用文字描述就是：

- spring 启动过程中，扫描类，调用 getBean() 创建 A bean

- 进入到 doGetBean()，再调用 getSingleton()，这时候三级缓存肯定是没有对象的，因此继续往下走

- 先处理 @DependOn 

- 再调用 createBean()，这里会第一次调用后置处理器，目前没去理解干嘛的，不管了

- 然后后面调用 doCreateBean()，第二次调用后置处理器选定好构造方法，通过反射创建 bean

- 第三次调用后置处理器,处理 @Autowire 注解的方法和字段，封装成一个个 InjectedElement，形成一个列表，在五、六次后置处理器可以直接获取这个列表进行处理

- 判断是否支持循环依赖 boolean earlySingletonExposure，如果支持的话，那么将提前暴露早期对象，将 半成品 bean 放入封装到一个 单例工厂 ObjectFactory---singletonFactory 中，然后存放到第三级缓存中，在工厂中会调用 getEarlyBeanReference()，然后在这个方法中第四次调用后置处理器，如果需要的话，就生成代理对象

- 调用 populateBean() 进行属性赋值，主要是依赖注入，这里会调用第五、六次后置处理器完成依赖注入

  - ```
    发生循环依赖
    A bean 依赖 B bean， B bean 依赖 A bean，并且 A bean 需要进行 AOP 代理
    已经将 A bean 作为早期对象放入到第三级缓存当中了
    在创建 B bean 的时候调用 getSingleton() 获取第三级缓存中的 A bean 单例工厂，在 单例工厂中会对 A bean 判断是否需要进行加工 -- AOP
    由于 A bean 需要进行 AOP，因此这里会调用第四次后置处理器，产生 A bean 的 一个 AOP 对象，然后返回，将 AOP 对象存储到二级缓存中，然后返回给 B bean，然后 B bean 完成 A bean 的属性注入，然后将这个 B bean 返回给 A bean
    A bean 拿到 B bean 后完成属性注入
    populateBean() 方法结束
    ```

- 调用 initializeBean() 进行 A bean 的初始化，这里会调用第七、八次后置处理器，完成 @PostConstruct 方法调用，以及对 A bean 的 AOP 代理，但是这里跟 第三级缓存 是二选一的，如果已经调用了 getEarlyBeanReference() 生成代理对象，那么在 initializeBean() 是不会再次创建的，在 initializeBean() 直接返回原始 bean

- 最后在一个 if() 条件体里判断返回哪个 bean，这个 if() 条件体很重要，在下面单独拿出来讲



在 doCreateBean() 方法完成了 bean 实例化、属性注入、初始化 后，会接着一个 if() 判断，这个判断里会判断返回哪个 bean，这里也就是解决 循环依赖情况下 AOP 对象的返回

以下是大量精简的 doCreateBean() 方法：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {

    //实例化 bean
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);

    final Object bean = instanceWrapper.getWrappedInstance();

    //判断是否支持循环依赖，这个参数很重要
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                                      isSingletonCurrentlyInCreation(beanName));
    //将 bean 封装到 单例工厂中，再添加到 第三级缓存
    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

    Object exposedObject = bean;
    //属性注入
    populateBean(beanName, mbd, instanceWrapper);
	//初始化，调用 @PostConstruct 和 创建代理对象
    exposedObject = initializeBean(beanName, exposedObject, mbd);
	/*
        1、bean：原始 bean
        2、exposedObject：第八次后置处理器创建的 AOP 对象，如果不需要代理，那么返回 bean
        3、earlySingletonReference：从缓存中获取的 bean 对象
        */
    if (earlySingletonExposure) {
        /*
        调用 getSingleton()，由于这里肯定不存在于一级缓存中，因此只能 二级/三级缓存中获取 bean
        但实际上只能从 二级缓存中获取，因为这里后面传了 false,意味着不从 三级缓存中获取
        这表示如果没有循环依赖，那么这里得到的 bean 为空，因为它还呆在三级缓存中
        */
        Object earlySingletonReference = getSingleton(beanName, false);
        //缓存内对象不为空
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                //xxx
            }
        }
    }
    return exposedObject;
}
```



我们可以发现，if() 判断的条件是 if (earlySingletonExposure)，即是否支持循环依赖，因此它是跟循环依赖相关的，它是用来解决循环依赖情况下返回 AOP 对象的问题



假设 A bean 不支持循环依赖 + AOP 对象，那么 bean 的创建描述如下：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
	//.....
    
    Object exposedObject = bean;
    //A bean 执行属性注入，创建 B bean 过程中注入 A bean
    populateBean(beanName, mbd, instanceWrapper);
    /*
    	A bean 不支持循环依赖，因此不会将 A bean 存储到三级缓存中，同样的也不会在前面创建 AOP 对象
    	这样的话就是在 initializeBean() 里创建了
    	因此 exposedObject 为 AOP 代理对象
    */
    exposedObject = initializeBean(beanName, exposedObject, mbd);
    
    //不支持循环依赖，跳过这个 if() 判断
    if (earlySingletonExposure) {
        //xxxx
    }
    //返回 exposedObject AOP 代理对象
    return exposedObject;
}
```



假设 A bean 已经被循环依赖了 + 需要 AOP，那么 bean 的创建描述如下：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
	//.....
    
    Object exposedObject = bean;
    //A bean 执行属性注入，创建 B bean 过程中注入 A bean
    populateBean(beanName, mbd, instanceWrapper);
    /*
    	A bean 已经被循环依赖的 bean 注入了，这样的话表示 A bean 已经通过 三级缓存的单例工厂创建出 AOP 对象了
    	并且存储在 二级缓存中，因此在 initializeBean() 不会再重复创建，这里返回的是原始 bean
    	即 exposedObject == bean
    */
    exposedObject = initializeBean(beanName, exposedObject, mbd);
    
    /*
    判断支持循环依赖
    因为只有支持了循环依赖的 bean 才有可能被调用第三级缓存的单例工厂创建 AOP 对象，因此就需要将已经创建的 AOP 对象返回
    不支持循环依赖在上面不会添加进三级缓存中
    */
    if (earlySingletonExposure) {
        /*
        调用 getSingleton()，由于这里肯定不存在于一级缓存中，因此只能 二级/三级缓存中获取 bean
        但实际上只能从 二级缓存中获取，因为这里后面传了 false,意味着不从 三级缓存中获取
        这表示如果没有循环依赖，那么这里得到的 bean 为空，因为它还呆在三级缓存中
        */
        Object earlySingletonReference = getSingleton(beanName, false);
        /*
        缓存内对象不为空，表示已经创建了 AOP 对象了，这意味着 exposedObject == bean 等式成立，
        那么将已经创建的 AOP 对象 earlySingletonReference 赋值给 exposedObject，然后返回
        至此结束 A bean 的创建
        */
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                //xxx
            }
        }
    }
    //返回 exposedObject
    return exposedObject;
}
```



假设 A bean 支持循环依赖，但是没循环依赖的对象 + 需要 AOP，那么 bean 的描述如下：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
	//.....
    
    Object exposedObject = bean;
    //A bean 执行属性注入，创建 B bean 过程中注入 A bean
    populateBean(beanName, mbd, instanceWrapper);
    /*
    	A bean 支持循环依赖，但是不存在循环依赖的对象，因此在前面不会 第四次调用后置处理器创建 AOP 对象
    	因此 A bean 的早期对象还在第三级缓存中，换句话说，就是 AOP 对象还没有创建
    	因此会在 initializeBean() 方法里创建 AOP 对象
    	即 exposedObject 是 AOP 对象
    */
    exposedObject = initializeBean(beanName, exposedObject, mbd);
    
    /*
    判断支持循环依赖
    因为只有支持了循环依赖的 bean 才有可能被调用第三级缓存的单例工厂创建 AOP 对象，因此就需要将已经创建的 AOP 对象返回
    不支持循环依赖在上面不会添加进三级缓存中
    */
    if (earlySingletonExposure) {
        /*
        调用 getSingleton()，由于这里肯定不存在于一级缓存中，因此只能 二级/三级缓存中获取 bean
        但实际上只能从 二级缓存中获取，因为这里后面传了 false,意味着不从 三级缓存中获取
        这表示如果没有循环依赖，那么这里得到的 bean 为空，因为它还呆在三级缓存中
        */
        Object earlySingletonReference = getSingleton(beanName, false);
        /*
        由于没被循环依赖调用，所以 earlySingletonReference 为空，因此这里直接相当于退出了，
        将 exposedObject 代理对象返回了
        */
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                //xxx
            }
        }
    }
    //返回 exposedObject
    return exposedObject;
}
```
