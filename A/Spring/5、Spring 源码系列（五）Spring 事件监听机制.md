# Spring 事件监听机制



## 1、Spring 监听事件架构

[Spring 事件监听器分析 -- CSDN](https://blog.csdn.net/shenchaohao12321/article/details/85303453)



### 1.1、事件监听的三个角色（事件、监听者、广播器）

**Spring 的事件监听机制本质上也是一个观察者模式**

存在三个重要的角色：

- ApplicationEvent：所有监听事件的父类，是
- ApplicationLisenter ：所有监听者需要实现的接口，通过接口泛型指定监听的事件
- ApplicationEventMulticaster ：事件广播器，在 ApplicationContext 中维护该对象，当某个 ApplicationContext 发生了某个事件，那么调用该事件广播器 来找出监听该事件的监听者，然后回调监听者的回调函数 onApplicationEvent()

ApplicationContext （Spring 默认是 AnnotationConfigApplicationContext）维护了一个事件广播器 applicationEventMulticaster，当触发事件时，由该事件广播器进行处理



Spring 定义了 4 种继承了 ApplicationEvent 的监听事件类型：

- `ContextRefreshedEvent`：容器的 refresh() 被调用时触发。
- `ContextStartedEvent`： 容器的 start() 被调用时触发
- `ContextStoppedEvent`：容器的 stop() 被调用时触发。
- `ContextClosedEvent`：容器的 close() 被调用时触发。**一个已关闭的上下文到达生命周期末端；它不能被刷新或重启**



ApplicationContext 在刷新容器完成后，会在最后发布 refresh 事件。

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        //xxxxx

        //初始化事件广播器
        initApplicationEventMulticaster();

        //注册监听器，这是 spring 事件监听器的一部分
        registerListeners();

        //实例化 bean，利用 beanFactory 来进行实例化
        finishBeanFactoryInitialization(beanFactory);

        //完成 refresh()，进行最后的收尾工作
        finishRefresh();
    }
}

protected void finishRefresh() {
    clearResourceCaches();

    initLifecycleProcessor();

    getLifecycleProcessor().onRefresh();
	
    //发布 refresh() 完成事件，事件源为当前 ApplicationContext
    publishEvent(new ContextRefreshedEvent(this));

    LiveBeansView.registerApplicationContext(this);
}
```



### 1.2 ApplicationEvent 事件 和 事件源

ApplicationEvent 类继承了 JDK java.utl 包下的 EventObject，没有空的构造方法，只有一个有参构造方法，这意味着发布一个事件就必须添加一个事件源

```java
public abstract class ApplicationEvent extends EventObject {

    private static final long serialVersionUID = 7099057708183571937L;

    public ApplicationEvent(Object source) {
        //调用 父类 EventObject 的构造方法将事件源传入
        super(source);
        this.timestamp = System.currentTimeMillis();
    }
}
```



EventObject 类：

```java
public class EventObject implements java.io.Serializable {

    private static final long serialVersionUID = 5516075349620653480L;

    //事件源
    protected transient Object  source;

    public EventObject(Object source) {
        //如果事件源为 null，那么抛出异常
        if (source == null)
            throw new IllegalArgumentException("null source");

        this.source = source;
    }

    //获取事件源
    public Object getSource() {
        return source;
    }
}

```



> ####  为什么事件需要事件源？

因为一个事件发生可能存在多个来源，比如吃东西，人能吃，其他动物也能吃，有些监听器它只想监听 “人吃东西” 的事件，如果当发布事件的时候需要指定事件源，然后从监听器列表中找到支持该事件 和 该事件的事件源 的监听器

当 ApplicationContext 刷新容器完成后，发布 refresh() 事件时，事件源就是它本身，即它本身启动的，那么这时候就会找到监听 ApplicationContext refresh() 的监听器





### 1.3、事件发布接口 ApplicationEventPublisher

在该接口中定义了 两个重载的方法 publishEvent()，用于发布事件

```java
@FunctionalInterface
public interface ApplicationEventPublisher {
    //参数为 ApplicationEvent
	default void publishEvent(ApplicationEvent event) {
		publishEvent((Object) event);
	}
    //参数为 Object
	void publishEvent(Object event);
}
```

而 ApplicationContext 接口继承了该接口，因此所有的 ApplicationContext 实例对象都具有发布事件的能力

比如 AonntationConfigApplicationContext 的父类 AbstractApplicationContext 就是实现了这个方法

因此在我们使用 AonntationConfigApplicationContext  的时候，可以调用它的 publishEvent() 来发布事件，内部会调用事件广播器进行广播

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
    implements ConfigurableApplicationContext {
    
    @Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}
    
    @Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

    protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
        //类型转换,将 Object 转换为 ApplicationEvent
        ApplicationEvent applicationEvent (ApplicationEvent) event;

        /*
        getApplicationEventMulticaster() 获取事件广播器
        调用事件广播器的 multicastEvent() 进行广播
        获取广播器内部的监听器列表，从中获取所有支持该事件以及该事件内部事件源的监听者，
        然后回调它们的 onAppicationEvent() ，完成广播
    */
        getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
    }
}
```



### 1.4、事件广播器接口 ApplicationEventMulticaster

该接口内部定义了多个方法来 添加 / / 移除 监听器列表

```java
public interface ApplicationEventMulticaster {
	//添加监听器
	void addApplicationListener(ApplicationListener<?> listener);
	
	void addApplicationListenerBean(String listenerBeanName);
	//移除监听器
	void removeApplicationListener(ApplicationListener<?> listener);

	void removeApplicationListenerBean(String listenerBeanName);
	//移除所有的监听器
	void removeAllListeners();
	//广播事件
	void multicastEvent(ApplicationEvent event);
	//广播事件 + 对应的事件源
	void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}
```



在 AbstractApplicationContext 中会维护一个事件广播器，对象类型为 SimpleApplicationEventMulticaster，该广播器实现了 ApplicationEventMulticaster 接口。

```java
private ApplicationEventMulticaster applicationEventMulticaster =  new SimpleApplicationEventMulticaster();
```

一旦 ApplicationContext 调用 publishEvent() 发布事件，那么在该方法内部会调用事件广播器的 multicastEvent() 进行广播



SimpleApplicationEventMulticaster 内部维护了 监听器 Set 集合列表 、异步线程池 taskExecutor、异常处理器 errorHandler

```java
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

    //事件监听器列表（父类维护的，这里是直接拉到这里来显示），并且 addApplicationEvent() 等方法也是在父类中实现
    public final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
    
	@Nullable
	private Executor taskExecutor;

	@Nullable
	private ErrorHandler errorHandler;
}
```



广播函数 multicastEvent() ：

1）从广播器维护的监听器列表中获取所有支持该事件 event 和 该事件的事件源 source 的监听器

2）判断是否维护了一个用来异步回调监听器的线程池 taskExecutor ，如果维护了，那么将回调执行的任务提交给线程池，如果没有维护，那么直接使用当前线程执行回调函数

```java
@Override
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
    //事件类型
    ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
    //getApplicationListeners(event, type) 获取广播器维护的监听器列表中支持 该事件 event 和 该事件的事件源 source 的监听器
    for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
        //获取线程池
        Executor executor = getTaskExecutor();
        //如果线程池不为空，那么异步执行回调函数
        if (executor != null) {
            executor.execute(() -> invokeListener(listener, event));
        }
        else {
            //线程池为空，同步执行回调函数
            invokeListener(listener, event);
        }
    }
}
```



执行回调函数 invokeListener()：

1）判断是否设置了错误处理器 errorHandler

2）如果设置了，那么调用 doInvokeListener() 在内部调用监听器的 回调函数 onApplicationEvent()，如果执行过程中出现异常，那么在外层 catch 然后使用 errorHandler 进行处理

3）如果没有设置，那么直接调用 doInvokeListener()，发生异常直接往上抛

```java
protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
    ErrorHandler errorHandler = getErrorHandler();
    if (errorHandler != null) {
        try {
            doInvokeListener(listener, event);
        }
        catch (Throwable err) {
            errorHandler.handleError(err);
        }
    }
    else {
        doInvokeListener(listener, event);
    }
}

private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
    try {
        //调用监听器的回调函数 onApplicationEvent()
        listener.onApplicationEvent(event);
    }
    catch (ClassCastException ex) {
        //以下都是日志处理，不用在意
        String msg = ex.getMessage();
        if (msg == null || matchesClassCastMessage(msg, event.getClass())) {
            Log logger = LogFactory.getLog(getClass());
            if (logger.isDebugEnabled()) {
                logger.debug("Non-matching event type for listener: " + listener, ex);
            }
        }
        else {
            throw ex;
        }
    }
}
```





## 2、SpringBoot 启动流程 + 借助事件监听器实现的自动装配原理