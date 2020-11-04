# Spring 事件监听机制



## 1、Spring 监听事件架构

**Spring 的事件监听机制本质上也是一个观察者模式**

存在三个重要的角色：

- ApplicationEvent 表示对应的事件
- ApplicationLisenter 表示监听者
- ApplicationEventMulticaster 表示监听广播者，用来回调监听某个事件的监听者的回调函数

ApplicationContext 维护了 所有的监听事件 和 一个 监听广播器

```java
public abstract class AbstractApplicationContext implements ApplicationContext{
    //存储所有的后置处理器
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    
	//监听广播器
	private ApplicationEventMulticaster applicationEventMulticaster;

	//存储所有的监听器 Lisenter，用于回调 onApplicationEvent()
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
}
```

它跟我们之前讲的观察者模式最大的不同点在于 观察者列表是存储在一个广播器中的，而被观察者 维护了这么一个广播器，是组合的方式，

而我们之前讲的观察者模式是 被观察者继承 广播器，耦合度增加，同时不能够去继承别的类

显然是 spring 的 组合 方式情况更好，因为这不是必须使用继承的情况



首先是 ApplicationEvent

Spring 默认的监听事件有

- `ContextRefreshedEvent`：容器初始化完成刷新时触发。此时所有的Bean已经初始化完成、后置处理器等都已经完成
- `ContextStartedEvent`： 需要手动调用 ac.start()，个人觉得没啥卵用
- `ContextStoppedEvent`：容器的stop方法被手动调用时。  也没啥卵用
- `ContextClosedEvent`：close() 关闭容器时候发布。**一个已关闭的上下文到达生命周期末端；它不能被刷新或重启**



如果我们要自定义监听事件，那么需要继承 ApplicationEvent，不需要注入到 IOC 中

```java
public class SpringEmailEvent extends ApplicationEvent {
	//这里的 source 就是 ac，即 ApplicationContext ，实现类是 AnnotationConfigApplicationContext
	public SpringEmailEvent(Object source) {
		super(source);
	}
}
```



然后是 ApplicationLisenter，它是用来监听某个事件的发生，然后做出对应的操作（知道观察者模式应该就清楚是怎么回事）

自定义监听者，那么需要实现 ApplicationLisenter，它有一个回调函数，即当事件发生时调用的函数 onApplicationLisenter()

```java
@Component									//这里泛型指定监听的事件
public class SpringEmailListener implements ApplicationListener<SpringEmailEvent> {	

	@Override
	public void onApplicationEvent(SpringEmailEvent event) {
		//do something
        sendMsg();
	}
    
    public void sendMsg(){
        System.out.println("触发信息发送事件，发送信息");
    }
}
```



ApplicationEvent 内部维护了一个 source，它是意味着在表明当前这个 Event 事件对象是属于谁的

一般情况下这个 source 是 AnnotationConfigApplicationContext，它是 ApplicationContext 的子类

可能存在多个 ApplicationContext ，而每个 ApplicationContext 注册了不同的 监听者 Lisenter，这样的话，我们在发布某个事件的时候，需要指定是哪个 ApplicationContext ，这样才能够获取对应 ApplicationContext 中的监听者，然后通知对应的监听者

类似我们观察者模式的 Subject，每个 Subject 都有一个对应的观察者列表，
People 会继承这个 Subject，即每个 People 拥有监听它的观察者列表
而 People 可以有很多的操作，比如攻击，比如防御，这些观察者可以指定对应观察的 操作，等到存在对应的操作时，那么去通知这些观察者做出对应的逻辑

这里的 People 就类比 ApplicationContext 

```java
class A{
    public static void main(){
        //不同的人物维护各自一个监听者列表， 监听不同的事件，当某个人物的某个事件发生时，指定人物对应的监听者列表进行操作
        People p1 = new People();
        //陷阱
        Trap trap = new Trap(new MoveEvent());
        p1.addLisenter(trap);
        
        People p2 = new People();
        //血包
        Blood blood = new Blood(new MoveEvent());
        p2.addLisenter(blood);
        
        p1.move();
        //通知 p1 监听者列表中监听 MoveEevent 事件的监听者，调用它们的回调函数 onApplicationEvent()
        publishEvent(new MoveEevent(p1));
    }
}
```



## 2、Spring 监听事件大致执行流程

当我们执行某个事件，然后将事件发布出去，通知监听者

```java
@Component
public class MailBean {

	@Autowired
	ApplicationContext ac;

	public void sendMail() {
        //调用 MQ 发送短信
        mq.sendMsg();
        //触发短信事件，将事务发布出去
		ac.publishEvent(new SpringEmailEvent(ac));
	}
}

class A{
    @Autowire
    MailBean mb;
    
    public static void main(String[] args){
        //do something
    	mb.sendMail();    
    }
}
```

我们在 MailBean 中发送完短信后，将短信事件发布出去，然后让 ac 获取内部自己维护的监听者列表，获取监听这个事件的监听者，调用它们的回调函数

由于哪个方法发布哪个 Event 是我们决定的，因此 publishEvent() 需要手动调用



publishEvent(Object event) 指定通知监听某个事件的监听者 

```java
protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
  
   ApplicationEvent applicationEvent;
    //判断 事件 是否是 ApplicationEvent，如果是则进行类型转换
   if (event instanceof ApplicationEvent) {
      applicationEvent = (ApplicationEvent) event;
   }
   else {
      applicationEvent = new PayloadApplicationEvent<>(this, event);
      if (eventType == null) {
         eventType = ((PayloadApplicationEvent) applicationEvent).getResolvableType();
      }
   }

    /*
        getApplicationEventMulticaster() 获取监听广博器
        调用 监听广博器的 multicastEvent()，内部获取 event 中 source 所有的监听者，
        然后筛选出监听这个事件的坚挺着，然后回调函数
    */
    getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);

}
```



Spring 默认的监听广播器 SimpleApplicationEventMulticaster 内部的 multicastEvent()

```java
@Override
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
    //获取事件类型，比如 SpringEmailEvent
   ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
    //getApplicationListeners(event, type) 获取 event 的 source 中所有监听 type 事件的监听器 
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



获取监听器：getApplicationListeners()

```java
	protected Collection<ApplicationListener<?>> getApplicationListeners(
			ApplicationEvent event, ResolvableType eventType) {
		//获取事件的 source，这里是 ApplicationContext
		Object source = event.getSource();
		//source 类型，即 ApplicationContext
		Class<?> sourceType = (source != null ? source.getClass() : null);
        //创建 事件类型和 source 类型的映射关系作为 key，用来存储到 retrieverCache 中下次直接获取
		ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);
        //使用 cacheKey 作为 key，判断是否之前已经缓存过了，如果是则直接返回
		ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
		if (retriever != null) {
			return retriever.getApplicationListeners();
		}

		if (this.beanClassLoader == null ||
				(ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
						(sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
			// Fully synchronized building and caching of a ListenerRetriever
			synchronized (this.retrievalMutex) {
				retriever = this.retrieverCache.get(cacheKey);
				if (retriever != null) {
					return retriever.getApplicationListeners();
				}
				retriever = new ListenerRetriever(true);
				//调用 retrieveApplicationListeners() 获取 source 中监听此事件的监听器
				Collection<ApplicationListener<?>> listeners =
						retrieveApplicationListeners(eventType, sourceType, retriever);
				//将监听器缓存起来，下次直接获取
				this.retrieverCache.put(cacheKey, retriever);
				return listeners;
			}
		}
		else {
			// No ListenerRetriever caching -> no synchronization necessary
			return retrieveApplicationListeners(eventType, sourceType, null);
		}
	}
```



## 3、监听器 Lisenter 的添加时机

在 preInstantiateSingletons() 中 getBean() 执行完后，将 bean 添加进对应 ac 的监听器列表中

```java
@Override
public void preInstantiateSingletons() throws BeansException {
    //所有bean的名字
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // Trigger initialization of all non-lazy singleton beans...
    // 触发所有非延迟加载单例 bean 的初始化，主要步骤为调用 getBean()
    for (String beanName : beanNames) {
        //合并父 BeanDefinition
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            if (isFactoryBean(beanName)) {
                // do something
            }
            else {
                //创建 bean
                getBean(beanName);
            }
        }
    }
    /*
		 事件监听者 lisenter 就是在这里进行添加的
		 */
    // Trigger post-initialization callback for all applicable beans...
    for (String beanName : beanNames) {
        Object singletonInstance = getSingleton(beanName);
        if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;

            else {
                //调用 EventListenerMethodProcessor 添加事件监听者
                //这里的 smartSingleton 就是 EventListenerMethodProcessor
                smartSingleton.afterSingletonsInstantiated();
            }
        }
    }
}
```



在 EventListenerMethodProcessor 类中调用 afterSingletonsInstantiated() 添加监听器

```java
public class EventListenerMethodProcessor implements SmartInitializingSingleton, ApplicationContextAware {	
    @Override
    public void afterSingletonsInstantiated() {
        List<EventListenerFactory> factories = getEventListenerFactories();
        ConfigurableApplicationContext context = getApplicationContext();
        String[] beanNames = context.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            if (!ScopedProxyUtils.isScopedTarget(beanName)) {
                Class<?> type = null;
                type = AutoProxyUtils.determineTargetClass(context.getBeanFactory(), beanName);
                try {
                    //真正的添加事件监听者
                    processBean(factories, beanName, type);
                }
                catch (Throwable ex) {
                    throw new BeanInitializationException("Failed to process @EventListener " +
                                                          "annotation on bean with name '" + beanName + "'", ex);
                }
            }
        }
    }

    protected void processBean(
        final List<EventListenerFactory> factories, final String beanName, final Class<?> targetType) {

        if (!this.nonAnnotatedClasses.contains(targetType)) {
            //do something
        }
        else {
            //获取应用上下文 ac，将监听器添加到这个 ac 的监听器列表中
            ConfigurableApplicationContext context = getApplicationContext();
            for (Method method : annotatedMethods.keySet()) {
                for (EventListenerFactory factory : factories) {
                    if (factory.supportsMethod(method)) {
                        Method methodToUse = AopUtils.selectInvocableMethod(method, context.getType(beanName));
                        ApplicationListener<?> applicationListener =
                            factory.createApplicationListener(beanName, targetType, methodToUse);
                        if (applicationListener instanceof ApplicationListenerMethodAdapter) {
                            ((ApplicationListenerMethodAdapter) applicationListener).init(context, this.evaluator);
                        }
                        //添加事件监听者到 ApplicationContext 中
                        context.addApplicationListener(applicationListener);
                        break;
                    }
                }
            }
        }
    }
}

```

