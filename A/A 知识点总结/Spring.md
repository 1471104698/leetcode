# 简单总结五



## 1、IOC 是什么？作用？

```java
1、如何理解 IOC ？ IOC 解决的是什么问题？
IOC 译为控制反转，它是一种设计思想，不是单单在 Spring 中才会使用，不过在 Spring 中表示将对象的创建交由 Spring 容器处理，而不需要开发人员自己去创建。
实现 IOC 的就是 DI，译为依赖注入，
DI 的实现有以下几种方式：
    1）构造注入：调用构造方法注入
    2）setter 注入：调用 set() 注入
    3）field 注入：使用 Filed 对象注入

其中构造注入是无法解决循环依赖的，比如
    class A{
        B b;
        @Autowire
        public A(B b){
            this.b = b;
        }
    }

    class B{
        A a;

        @Autowire
        public B(A a){
            this.a = a;
        }
    }
调用 getBean() 创建 A bean，在第二次后置处理器调用中，获取 A 的构造方法创建 bean 对象
由于 A 对象中依赖 B 对象，因此会调用 getBean() 创建 B bean，而在创建 B 同样是在第二次后置处理器中，同样依赖 A 对象，那么这时候又会去调用 getBean() 创建 A，这个过程如果没有进行处理，那么将是一个死循环，所以在这个过程中发现 A 正在创建，那么就会抛出异常。
因此构造注入无法解决循环依赖。

此外，原型对象也是无法解决循环依赖的，因为它没有暴露早期对象。


setter 注入 和 field 注入 如何解决循环依赖的后面再讲！


使用 IOC 的好处是什么？
比如我们创建一个 A 对象，它的创建逻辑很复杂，比如 new A(new B(new C(new D(new E()))))，当然我们不一定需要构造传参，这里只是为了体现创建 A 对象很复杂，假设有很多个地方都需要实例化一个 A 对象，我们不可能在每个地方都写一遍实例化代码，那么这时候我们会创建一个方法 createA()，将创建 A 对象的逻辑给封装起来（类似工厂模式），那么在需要 A 对象的地方就调用该方法即可。
	public A createA(){
    	return new A(new B(new C(new D(new E()))));
	}
不过这么做对 A 的创建逻辑也是写死的，在后续需要修改 A 或者其他类的注入对象时，我们仍然需要去修改这个代码。
因此，我们可以怎么做？我们在需要注入的对象上添加一个自定义注解，然后在创建 A 对象的时候，通过反射获取 A 对象的 Field 数组，然后判断 Field 是否存在我们自定义的这个注解，如果存在，表示需要注入，我们只需要通过递归调用创建对象的方法逻辑去创建这个需要注入的对象，然后返回即可。
    这样的话就变成了 反射 + 递归 来实现。
IOC 的做法就是如此，它使用的就是简单工厂模式，通过维护一个 Bean 工厂（Bean Factory），将所有 bean 的创建逻辑在这个 Bean 工厂中实现，而在这个 getBean() 中能够创建出各种不同类型的对象，主要是归功于 后置处理器（Spring 这个后置处理器的设计非常的强），对应不同的 bean 使用不同的后置处理器来实现不同的创建逻辑，可以说 IOC 使用的是 加强版的简单工厂模式。
在这个创建逻辑中，通过反射获取 bean 所需要的注入的字段，比如 A 需要注入 B，那么就会去创建 B，在创建 B 的过程中发现需要注入 C ，那么就去创建 C，无论需要注入什么对象，都通过 递归 + 反射 来实现，不需要开发者去维护这个过程。
同时，AOP 可以说是借助 IOC 实现的，在创建 bean 的过程中，如果发现 A 需要注入的是一个 B 的代理对象，那么就会通过 AOP 创建 B 的代理对象，然后将 B 的代理对象注入到 A 中。
```



## 2、IOC 解决循环依赖

```java
IOC 如何解决循环依赖？
IOC 使用三级缓存来解决循环依赖，三级缓存全部都是 Map，不过存储的数据不同

//第一级缓存， 存储 createBean() 创建完成的 bean，该缓存中的 bean 可以直接使用
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
//第二级缓存，用来解决循环依赖，存储从第三级缓存中移除的 成品 bean(原始 bean 或者 AOP bean)
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
//第三级缓存，用来解决循环依赖，存放 singletonFactory，它是一个工厂，用来给早期对象生成 AOP 代理对象
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

循环依赖解决过程：
1）调用 getBean() 创建 A bean，在第三次后置处理器调用后（该 bean 只是半成品 bean，称作早期对象），判断 IOC 是否支持循环依赖，如果支持，那么将 A 的早期对象 bean 封装到一个工厂对象中，然后存储到第三级缓存中，以 beanName 作为 key，工厂对象作为 value，该步骤称作暴露早期对象，它在第四次后置处理器会调用
2）当存储到第三级缓存完成后，继续执行，在第六次后置处理器中进行属性注入，这时候发现 A 需要注入 B 对象，那么调用 getBean() 创建 B bean
3）创建 B bean 是同样的过程：暴露早期对象、属性注入，在属性注入的时候，发现 B 需要注入 A 对象，那么调用 getBean() 创建 A bean
4）在 getBean() 中最开始会调用 getSingleton()，该方法会先从一级缓存中尝试获取，此时获取失败，从二级缓存中获取，此时获取失败，然后从三级缓存中获取，由于 A 暴露了早期对象，所以此时的三级缓存中是存在 A 的，不过得到的是一个维护了 A bean 的工厂对象，调用工厂对象的 getObject() 方法，得到 A bean，然后将 A bean 从三级缓存中移除，存储到二级缓存中。
5）在 getObject() 中会调用 getEarlyBeanReference()，该方法中会调用第四次后置处理器，如果 A bean 需要进行 AOP 代理的话，那么在这里会调用处理 AOP 的后置处理器提前生成代理对象返回，如果不需要的话，那么就直接返回原始的 A bean
6）B 获取到 A 的早期对象，完成属性注入，然后在第八次后置处理器中，判断是否需要进行 AOP 代理，如果需要，那么调用处理 AOP 的后置处理器生成代理对象，然后将 B 的代理对象返回。
7）A 获取到 B 的代理对象，完成属性注入，然后在第八次后置处理器中判断是否需要生成代理对象，由于在第四次后置处理器中已经生成了，所以不需要再次生成，后续直接获取二级缓存中的代理对象，然后返回即可
至此，解决了循环依赖
 
    
    
 在调用了第八次后置处理器后，会再进行最后的判断：返回哪个对象？
 	（这里前面会存在两个对象：原始 bean 对象 o1 和 第八次后置处理器返回的对象 o2）
 如果不支持循环依赖，那么表示该 bean 不可能提前创建 AOP 对象，那么可以直接返回 o2
 如果支持循环依赖，那么判断二级缓存中是否存在 beanName 对应的 bean 对象：
 	如果存在，表示已经调用过第四次后置处理器，二级缓存中可能是提前创建的 AOP 对象 o3，那么 o2 就会是原始对象，那么返回 o3；
 	如果不存在，表示该 bean 没有被循环依赖或者循环依赖的最先创建不是它，那么 o2 可能就是 AOP 对象，那么返回 o2
		（什么叫做最先调用的不是它？比如 A 和 B 循环依赖，如果最先创建的是 A 的话，那么 A 是会被 B 提前获取早期对象的，那么就需要调用第四次后置处理，从三级缓存中移到二级缓存中）

    
    
    
这里需要讲一下的是，为什么第四次后置处理器创建 A 的 AOP 对象注入到 B 中能够起作用？
A 的原始 bean，这里 叫做 a，第四次后置处理器创建的 AOP 对象，这里叫做 a-aop，在 a-aop 中是会维护 a 对象，，注入到 B 的是 a-aop，此时 a-aop 中的 a 对象是一个半成品，但是需要注意的是，在 a-aop 中存储的是 a 的地址，它是可以感知到 a 对象的变化的，所以 a 对象在后面完成属性注入，a-aop 最终引用的也会是成品的 a。而在最后我们只需要将 a-aop 返回即可，因为 a-aop 作为 AOP 对象实际上是对 a 对象的封装，我们属性注入之类的，实际上就是完成了对 a 对象的实例化，剩下的只需要将外层对象 a-aop 返回出去即可（这种设计思想是真的强）
```





## 3、IOC 的九次后置处理器调用

```java
1、第一次后置处理器
这里调用的后置处理器的方法有两个作用：
1）如果有后置处理器在该方法中能够提前创建对象，那么不会继续执行后面的创建 bean 的逻辑，不过默认都是为 null，这是一个扩展的点
2）筛选出必定不需要增强的类，比如 @Aspect 注解的类，将它存储到一个集合中，在 AOP 创建判断时就直接返回

2、第二次后置处理器
该方法用来推断构造方法，选择最合适的构造方法来创建 bean 原始对象，如果有构造方法使用 @Autowrie 注解，那么首选使用该方法，可以在这里实现构造注入

3、第三次后置处理器
这里会找出 bean 中的 @Autowire 和 @Resource 注解的字段，将它们封装起来，方便在第六次后置处理器中进行属性注入，以及 @PostConstruct 注解的方法，方便在第七次后置处理器中进行调用

4、第四次后置处理器
这里是由三级缓存中的工厂对象调用的，主要用于完成对早期对象的 AOP 创建，帮助循环依赖的实现，在这里创建了 AOP 对象的会存储到一个集合中，表示已经创建过 AOP 对象了，在第八次后置处理器就不会重复创建

5、第五次后置处理器
判断是否需要进行属性注入，同时可以获取 bean 然后对 bean 进行一些修改，这里如果不需要属性注入，那么不会调用第六次后置处理器

6、第六次后置处理器
对第三次后置处理器找到的 @Autowire 和 @Resource 字段进行属性注入，这里也是循环依赖的开始

7、第七次后置处理器
调用第三次后置处理器找到的 @PostConstruct 方法
在第七次后置处理器调用完成后，如果 bean 实现了 InitializingBean 接口，那么会调用 afterPropertiesSet() 方法（这里也是 Spring MVC 将 Controller 中的 @RequestMapping 和 Method 进行映射处理的地方）

8、第八次后置处理器
这里 Spring 在我们没有进行扩展的情况下会干两件事：
1）如果添加了 @EnableAspectJAutoProxy 注解，那么会 Import 一个处理 AOP 的后置处理器 AbstractAutoProxyCreator，在这里会生成 AOP 对象，它会判断在第一次后置处理器中该 bean 存在于 必定不需要代理的类的集合中，以及是否存在于第四次后置处理器已经处理的早期对象集合中，如果都没有，那么进行 AOP 代理
	动态代理类型的选择
	①、如果在 @EnableAspectJAutoProxy 中指定了 proxyTargetClass = true，那么使用 CGLIB 代理
	②、如果类没有实现任何接口，那么使用 CGLIB 代理
	③、如果 proxyTargetClass = false 并且 类实现了接口，那么使用 JDK 动态代理
2）ApplicationListenerDetector 后置处理器会判断该 bean 是否是监听器，如果是的话那么调用 ApplicationContext 的 addApplicationListener() 添加到监听器集合中，同时在该方法内部还会调用内部维护的广播器的 addApplicationListener()，即在 ApplicationContext 和 ApplicationContext 内部的广播器都添加该监听器。在广播的时候广播器使用的是它维护的监听器列表

9、第九次后置处理器
bean 销毁时调用 @PreDestory 注解的方法
```





## 4、JDK 动态代理 和 CGLIB 动态代理

```java
1、JDK 动态代理
JDK 动态代理 API：
	Object proxy = 
		Proxy.newProxyInstance(ClassLoader classLoader, Interface[] interfaces, InvocationHandler h);

该 API 会返回一个代理对象 proxy。
代理对象 proxy、被代理对象 a、方法拦截器 h 之间的关系为：
1）proxy 内部会存在一个字段维护 方法拦截器 h 
2）方法拦截器 h 内部会存在一个字段维护被代理对象 a

JDK 动态代理要求被代理类实现接口，然后代理类 proxy 也会实现这些接口，代理类通过重写这些方法，在这些方法内部统一调用一调用方法拦截器 h。当外部调用了代理对象的某个方法，内部会调用方法拦截器的 invoke()，然后执行代理逻辑，然后再调用 method.invoke(a) 来执行被代理对象的方法逻辑，从而实现 代理逻辑 和 被代理对象 的糅合。

简单点的代码演示如下：
interface B{
	public void say();
}
class A implements B{
	@Override
	public void say(){
		System.out.println("吃饭");
	}
}
class H implements InvocationHandler{
	private Object target;
	//通过构造方法也好，set 方法也好，反正就是需要将被代理对象传入到方法拦截器中
	public H(Object target){
		this.target = target;
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
		//切入逻辑
		System.out.println("这里是在 method 方法执行 前 切入逻辑");
		//通过反射，执行被代理对象的 method 方法，
		//注意，这里不能执行 proxy 的代理方法，否则在 proxy 的 method 又会调用方法拦截器 h 的 invoke()，陷入死循环
		Object res = method.invoke(target);
		System.out.println("这里是在 method 方法执行 后 切入逻辑");
		//方法返回结果
		return res;
	}
}
class AProxy extends Proxy implements B{
	private static Method m0; //hashCode()
	private static Method m1; //equals()
	private static Method m2; //toString()
	private static Method m3; //say()
	
	private static InvocationHandler h;
	
	public AProxy(InvocationHandler h){
		this.h = h;
	}
	
	public final void say(){
		/*
			this：当前代理对象
			m3：代理对象的方法
		*/
		h.invoke(this, m3, null);	
	}
	
	static {
        try {
	        m0 = Class.forName("java.lang.Object").getMethod("hashCode");
            m1 = Class.forName("java.lang.Object").getMethod("equals", Class.forName("java.lang.Object"));
            m2 = Class.forName("java.lang.Object").getMethod("toString");
            //这里是直接通过反射获取 A 对象的 say，而不是代理对象的 say()
            m3 = Class.forName("cn.oy.A").getMethod("say");
        } catch (NoSuchMethodException var2) {
            throw new NoSuchMethodError(var2.getMessage());
        } catch (ClassNotFoundException var3) {
            throw new NoClassDefFoundError(var3.getMessage());
        }
    }
}

创建代理对象的 demo 代码：
	Object proxy = Proxy.newProxyInstance(
                                        A.class.getClassLoader(), //类加载器
                                        A.class.getInterfaces(),  //被代理类接口
                                        new H(new A()));		  //方法拦截器，同时传入被代理类实例对象
	
可以很容易看出，对外部返回的是这个 AProxy 代理对象，它实现了 B 接口，因为在外部可以直接强转为 B 接口，这里是借助多态进行调用 say()，此时进入到 AProxy 的 say()，在内部调用方法拦截器 h 的 invoke()，同时将 当前代理对象 和 原始对象 a 的 say() 对应的 Method 对象传入，在 h 中执行完代理逻辑再执行 a 的原始方法，完成代理逻辑的切入。



2、CGLIB 动态代理
CGLIB 动态代理通过继承被代理类来实现，因此不需要要求被代理类实现接口。
CGLIB 是通过 FastClass 机制来实现方法调用的，而不是利用反射，所以效率要比 JDK 动态代理高。

class A{
	public void say(){
		System.out.println("吃饭");
	}
}
class M implements MethodInterceptor{
	/*
		proxy：代理对象
		method：被代理对象的原始方法对象
		methodProxy：代理对象的重写方法对象
	*/
	@Override
	public Object intercept(Object proxy, Method method, Object[] args， MethodProxy methodProxy) throws Throwable{
		//切入逻辑
		System.out.println("这里是在 method 方法执行 前 切入逻辑");
		/*
		调用 methodProxy 的 invokeSuper()，底层是通过 FastClass 机制调用的，而不是通过反射
		注意：这里不能调用 methodProxy.invoke()，否则会重新进入 proxy 的代理方法，陷入死循环
		*/
		Object res = methodProxy.invokeSuper(target, args);
		System.out.println("这里是在 method 方法执行 后 切入逻辑");
		//方法返回结果
		return res;
	}
}
class AProxy extends A{
	//say() 对应的方法对象
	private Method method;
	//say() 对应的代理对象（内部使用了 FastClass 机制），method 和 methodProxy 是一一对应的
	private MethodProxy methodProxy;
	
	private MethodInterceptor h;
	static{
		//反射获取原始对象的方法对象
		method = ReflectUtils.findMethods(new String[]{"h2", "()V"}, (var1 = Class.forName("com.luban.spring.test.D")).getDeclaredMethods())[0];
		//调用 MethodProxy 类的静态方法 -- create() 创建一个 MethodProxy 对象
		methodProxy = MethodProxy.create(var1, var0, "()V", "h2", "CGLIB$say");
	}
	
	//这里是别名方法，因为原来重写的方法用来进行代理了。这里通过 super 来调用原始方法 say()，这也算是继承的一个特性
	public void CGLIB$say(){
		super.say();
	}
	@Override
	public void say(){
		if(h == null){
			//在 Enhancer 对象通过 setCallBack() 设置了一个方法拦截器，这里调用获取
			h = getCallBack();
		}
		if(h != null){
			h.intercept(this, method, null, methodProxy);
		}else{
			super.h();
		}
	}
}

class MethodProxy{
	
	private volatile MethodProxy.FastClassInfo fastClassInfo;
	
	//FastClassInfo 内部类，是 FastClass 机制的主要实现
	//内部维护了 被代理对象 和 代理对象的 FastClass 对象以及当前 MethodProxy 对应的方法在 FastClass 中对应的索引位置
	private static class FastClassInfo {
        FastClass f1;
        FastClass f2;
        int i1;
        int i2;
        private FastClassInfo() {
        }
    }
    
    //方法拦截器调用该方法
    public Object invokeSuper(Object obj, Object[] args) throws Throwable {
        try {
            //初始化 fastClassInfo
            this.init();
            //获取 fastClassInfo fc
            MethodProxy.FastClassInfo fc = this.fastClassInfo;
            //调用 fc 中的 FastClass 对象 f2 的 invoke()
            return fc.f2.invoke(fc.i2, obj, args);
        } catch (InvocationTargetException var4) {
            throw var4.getTargetException();
        }
    }
    
    private void init() {
    	//初始化 FastClassInfo 对象
        if (this.fastClassInfo == null) {
            synchronized(this.initLock) {
                if (this.fastClassInfo == null) {
                    MethodProxy.CreateInfo ci = this.createInfo;
                    MethodProxy.FastClassInfo fci = new MethodProxy.FastClassInfo();
                    //设置 FastClass 对象
                    fci.f1 = helper(ci, ci.c1);
                    fci.f2 = helper(ci, ci.c2);
                    //获取方法在两个 FastClass 对象中的索引
                    fci.i1 = fci.f1.getIndex(this.sig1);
                    fci.i2 = fci.f2.getIndex(this.sig2);
                    this.fastClassInfo = fci;
                    this.createInfo = null;
                }
            }
        }
    }
}
public Object invoke(int var1, Object var2, Object[] var3) throws InvocationTargetException {
    56a1dd2f var10000 = (56a1dd2f)var2;
    int var10001 = var1;

    try {
        switch(var10001) {
            case 0:
                return new Boolean(var10000.equals(var3[0]));
                //省略 1 - 16
            case 17:
                /*
                    	这里的 var1000 是 代理对象
                        这里根据索引位置定位到代理对象的 CGLIB$say()，在内部来调用目标对象的 say() 方法
                        final void CGLIB$say() {
                            super.h2();
                        }
                    */
                var10000.CGLIB$say();	
                return null;
            case 18:
                return new Integer(var10000.CGLIB$hashCode$3());
            case 19:
                return new Boolean(var10000.CGLIB$equals$1(var3[0]));
            case 20:
                return var10000.CGLIB$toString$2();
        }
    } catch (Throwable var4) {
        throw new InvocationTargetException(var4);
    }
}


CGLIB 的实现机制算是比较繁杂的了，并且其中有很多定义的对象实际上都没有使用到，比如 method、f1、i1，这种在这里没有实际作用。
真正使用到的对象为 methodProxy、f2、i2
这里我总结下它们之间的关系：
1）代理对象 proxy 内部会为每个方法创建一个 MethodProxy（会通过反射获取 Method 对象），Method 和 MethodProxy 是一一对应的，即有多少个重写的方法就有多少个 MethodProxy 对象。
2）proxy 继承被代理类，重写它们的方法，对于每个父类的方法，还会通过起别名的方式定义另外的方法，比如 CGLIB$say()（可以理解为桥接方法），在别名方法内部调用父类的方法 super.say()，即存在多少个重写的方法，那么就存在多少个别名方法，同时在别名方法中调用原始方法。
3）当外部调用了 say()，那么进入的是 proxy 的拦截方法 say()，在内部会调用方法拦截器的 intercept()，在 intercept() 内部执行切入逻辑，同时调用 MethodProxy 对象的 invokeSuper() 来执行原始方法（注意不是 invoke()），该方法内部是通过 FastClass 调用方法，而不是反射
4）MethodProxy 内部维护了一个 FastClassInfo 对象，该对象中存在两个 FastClass f1,f2 和 两个标号 i1,i2。在 MethodProxy 中的 invokeSuper() 就是调用 FastClassInfo 中的 f2 的 invoke()，同时将 代理对象 proxy 和 标号 i2 传入
5）FastClass f2 的 invoke()，是一个 switch 多分支判断，根据 i2 来进行判断的，而在这个 switch 中，i2 分支是调用 proxy 的 CGLIB$say()，而在该方法中，是调用 super.say()，因此完成了原始方法的调用。
6）当原始调用完成后，继续返回到 方法拦截器的 intercept()，继续完成切入逻辑。

可以看出，CGLIB 的方法调用没有涉及到反射，而是通过生成一个 FastClass 对象，在内部通过 switch 扩充出不同的分支，按照不同的序号来直接写死调用哪些方法，而每个 MethodProxy 都会分配到一个标号 i2，通过这个标号 i2 可以直接在 FastClass 位置中定位到要调用的别名方法，从而成功调用到原始方法。
FastClass 机制比 JDK 动态代理的反射机制效率要高得多。


    
3、CGLIB 动态代理 跟 JDK 动态代理的不同点：
    1）JDK 动态代理要求被代理类实现接口，然后代理类通过实现接口的方式实现多态；CGLIB 不要求被代理类实现接口，自己通过继承的方式实现多态。
        ①、JDK 动态代理只能局限于被代理类实现的接口方法，无法代理被代理类自身或者父类非接口的方法（因为它是接口来实现的）,而接口的定义的方法都是能够被重写的，不可能存在 private、final、static 这种无法重写的方法
        （当然 JDK 8 的时候出现了 static、default，而 static 无法被代理，因为无法被继承/重写，无法被实例对象调用，只能通过类名调用，而 default 是能够被继承/重写的，所以可以进行代理）
        ②、CGLIB 动态代理对于 private、final、static 这些无法重写的方法无法进行代理
    2）JDK 动态代理的方法拦截器是 InvocationHandler，需要程序员在定义这个方法拦截器时将被代理类的实例化对象传入维护，因此需要程序员创建好被代理对象；CGLIB 使用的方法拦截器是 MethodIntercept，不需要维护被代理对象，同时也只需要设置好被代理类的 Class 对象，不需要创建对象（因为不需要，代理对象可以直接通过 super 调用原始方法）。
    3）JDK 动态代理的方法拦截器调用原始方法是通过 method.invoke() 反射调用的；CGLIB 方法拦截器调用原始方法是利用 FastClass 机制来避免反射调用，通过 MethodProxy 内部维护的 FastClass 对象来进行直接调用的。
    4）JDK 动态代理 和 CGLIB 内部都存在缓存来存储已经代理过的类，从而来避免重复生成代理类导致方法区 OOM，而 CGLIB 对外提供了一个方法 setUseCache(boolean useCache)，可以设置是否使用缓存，默认是使用的，如果不使用的话，当创建了大量的代理类时会导致方法区 OOM

```





## 5、AOP 是什么？解决了什么问题？

[深入理解Spring AOP的动态代理](https://juejin.cn/post/6844903533456982030)

[面试被问了几百遍的 IoC 和 AOP ，还在傻傻搞不清楚？](https://cloud.tencent.com/developer/article/1628865)

```java
1、什么是 AOP？
AOP 译为：面向切面编程。
在 AOP 下我们只需要关注切面、切点
我们可以把切面当作一个模块，将业务逻辑当作一个模块，可以随意进行组合，如果需要使用的话就切入，不需要使用的话就直接移除，没有任何的耦合，不会侵入到业务逻辑代码中。

2、AOP 解决了什么问题？
在这之前，我们需要知道，使用 静态代理 和 使用单纯的动态代理存在什么问题？
1）静态代理：
	耦合度很高，并且一些切入逻辑会重复，多个类要实现相同的切入逻辑需要定义多个重复的类，扩展和修改不易。
2）动态代理：
个人认为单纯的动态代理，有以下两个缺点：（感觉很多文章都没有理解到这个层面）
	①、需要我们自己去定义方法拦截器，在内部写明切入逻辑，以及需要我们手动去执行创建代理对象的代码，并且方法拦截器的逻辑是写死的，一般情况下我们写的一个方法拦截器只能执行一种切入逻辑，如果存在多个切入逻辑，那么就需要定义多个方法拦截器类
	②、单纯的动态代理不能精确到某个方法，它会代码整个类的方法，不能根据每个类的方法定义不同的切入逻辑，局限性很大
    	（虽然也不是必定不能精确到某个方法，可以通过自定义一个注解，然后在需要代理的方式上添加这个注解，然后在 invoke() 中判断 method 是否存在这个注解即可）

AOP 是基于动态代理来实现的，它将方法拦截器的切入逻辑给抽象成一个切面类，我们修改切入逻辑只需要修改切面类即可，同时，它可以利用切点具体定位到某个需要切入的方法，根据不同的方法可以直接定义不同的切入逻辑，并且增加了通知类型（Before、After、AfterReturning、AfterThrowing、Around）用来定义切入逻辑的切入时机
	注：Around 环绕通知很强大，一般不怎么建议使用。
在 Spring 中，AOP 是借助 IOC 来实现的。


日志切入 和 声明式事务（@Transactional）就是 AOP，它们都是利用动态代理的方法拦截来实现。
声明式事务的方法拦截器可以说就是属于 BeforeAdviseInterceptor，它是在方法调用前进行切入的
```

<img src="https://pic.leetcode-cn.com/1607615833-shGpWd-image.png" style="zoom:70%;" />



## 6、AOP 的方法拦截器链的获取和执行

```java
1、
Spring AOP 借用了 AspectJ 技术的注解，但是没有使用到它的技术
@Aspect 注解的类是一个切面，定义了以下几种通知类型：
1）Before：前置通知，在原始方法执行前执行
2）After：后置通知，在原始方法执行后执行，发生异常无论是否 catch 都会执行
3）AfterReturning：后置返回通知，在原始方法返回后执行，如果发生异常抛出到方法拦截器处被捕获则不会执行（任何方法都有 return 指令，即使是 void 也一样）
4）AfterThrowing：后置异常通知，原始方法发生异常，抛出到方法拦截器处被捕获时执行，和 AfterReturning 是互斥的
5）Arount：环绕通知，很吊，这里不讲了



2、方法拦截器链 chain 的获取
在第八次后置处理器中，创建 AOP 对象前，会提起处理原始 bean 对象每个 method 对应的通知对象 advisor（它是一个封装对象，内部维护了 advice）

实际上可以看出，AOP 对切面的处理跟 Spring MVC 对 Controller 的处理是一样的，在 Spring MVC 中，是获取所有的 Controller，然后将它们的 @RequestMapping 和 对应的方法进行映射，淡化了 Controller 层，在跟用户 request 进行匹配的时候只有 @RequestMapping，而不会先去找是哪个 Controller，即我们定义的多个 Controller 类都是为了方便我们定位和开发而已。
而 Spring AOP 也是如此，它将所有切面统一进行处理，然后将所有的 advisor 都存储到一个集合中，然后在 第八次后置处理器中根据 method 找到需要切入到该 method 的 advisor 组成一个集合，存储到 AOP 对象中。
    可以认为 Spring MVC 是在 Spring AOP 的设计上进行修改的

获取方法拦截器链的过程：（代码自己查看 JDKDynamicAopProxy 的 invoke()）
1）获取当前 AOP 对象维护的 advisor 对象集合，然后进行遍历
2）Spring 内部会维护一个 advice 适配器集合，扫描这些集合，判断哪个适配器适配 该 advice，如果适配，那么将该 advice 封装到对应通知类型的方法拦截器中，以下是前置通知适配器的源代码：
	class MethodBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

        @Override
        public boolean supportsAdvice(Advice advice) {
            //判断该 Advice 适配器是否适配当前通知类型 Advice
            return (advice instanceof MethodBeforeAdvice);
        }

        @Override
        public MethodInterceptor getInterceptor(Advisor advisor) {
            //如果上面适配了，那么在这里将该 通知类型 advice（内部含有注解的方法）封装到 前置通知方法拦截器对象中
            MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
            return new MethodBeforeAdviceInterceptor(advice);
        }

    }
3）@After 和 @AfterThrowing 对应的 Advice 对象本身就是一个方法拦截器，所以不在上面的适配器集合中，直接类型转换为方法拦截器方法即可

可以看出，上面实际上使用了适配器模式，由于存在多种通知类型，每种通知类型的处理方法不一样，为每个通知类型定义一个适配器，每个适配器都实现 AdvisorAdapter 接口，然后存储到适配器集合中，我们对于每一种 Advice 都只遍历这个适配器集合，如果适配就进行对应的处理。这样的话，当出现新的通知类型或者减少新的通知类型，都只需要定义一个新的适配器，而不需要修改源代码，便于扩展。



3、方法拦截器链 chain 的执行
在上面返回方法拦截器链后，会将原始方法 method 和 方法拦截器链 chain 封装到一个 ReflectiveMethodInvocation 对象中
然后调用它的 proceed() 开始方法拦截器链的执行
①、ReflectiveMethodInvocation 对象的 proceed()：
    
    @Override
    public Object proceed() throws Throwable {
    
        //当 currentInterceptorIndex 为拦截器链的最后一个拦截器，那么执行原始方法
        //	We start with an index of -1 and increment early.
        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            return invokeJoinpoint();
        }
        /*
        	根据全局变量 currentInterceptorIndex 来判断当前应该执行哪个拦截器 h，
        	每次获取一个，那么索引值 + 1，表示下次递归调用获取的是下一个拦截器
        */
        Object h = this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
		//调用方法拦截器的 invoke()，同时将当前 ReflectiveMethodInvocation 对象传入该方法中
        return ((MethodInterceptor) h).invoke(this);
    }

②、前置通知的方法拦截器 MethodBeforeAdviceInterceptor invoke()：
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
    	//调用 before()，执行 @Before 注解的方法
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
    	//递归调用 ReflectiveMethodInvocation 的 proceed()
		return mi.proceed();
	}

③、后置通知的方法拦截器 AspectJAfterAdvice 的 invoke()：
    @Override
	public Object invoke(MethodInvocation mi) throws Throwable {	//异常抛出
		try {
            //递归调用 ReflectiveMethodInvocation 的 proceed()
			return mi.proceed();
		}
		finally {
            //在 finally 中执行 @After 注解的方法
			invokeAdviceMethod(getJoinPointMatch(), null, null);
		}
	}

④、后置异常通知的方法拦截器 AspectJAfterThrowingAdvice 的 invoke()：
    @Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
            //递归调用 ReflectiveMethodInvocation 的 proceed()
			return mi.proceed();
		}
		catch (Throwable ex) {
            //捕获异常，执行 @AfterThrowing 注解的方法
			if (shouldInvokeOnThrowing(ex)) {
				invokeAdviceMethod(getJoinPointMatch(), null, ex);
			}
            //再抛出异常
			throw ex;
		}
	}

⑤、后置返回通知的方法拦截器  AfterReturningAdviceInterceptor 的 invoke()：
    @Override
	public Object invoke(MethodInvocation mi) throws Throwable {
    	//递归调用 ReflectiveMethodInvocation 的 proceed()
		Object retVal = mi.proceed();
    	//执行 @AfterReturning 注解的方法
		this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
		return retVal;
	}

1）在第八次后置处理器中，会处理好 bean 每个 method 对象的所有切点（通知类型 + 注解的方法对象），存储到一个 map 中，key 为 method，value 为该 method 的切点集合，是一个 List<Object>
2）在调用代理对象的方法后，方法拦截器的 invoke() 会获取原始方法 method 的切点集合，然后根据通知类型封装为一个个方法拦截器，形成一个方法拦截器链 chain（List<Object>），内部已经根据通知类型排好顺序了，根据通知类型倒序排序的。
    0
    1 AspectJAfterThrowingAdvice
    2 AfterReturningAdviceInterceptor
    3 AspectJAfterAdvice
    4 MethodBeforeAdviceInterceptor
3）然后再将 method 和 chain 封装成一个 ReflectiveMethodInvocation（以下简称 ref），调用它的 proceed() 
4）在 ReflectiveMethodInvocation 中维护了一个全局索引 currentInterceptorIndex（以下简称 idx），用来表示当前需要执行chain 中的哪个方法拦截器，初始值为 -1，它是一个递归调用的过程：
    ①、按照顺序获取，那么会先获取到 AspectJAfterThrowingAdvice，调用它的 invoke()，同时再将当前 ref 对象 this 传入，在该方法内部会先递归调用 ref 的 proceed()，那么就是再次按照顺序获取 AfterReturningAdviceInterceptor
    ②、调用 AfterReturningAdviceInterceptor 的 invoke()，同时将 ref 对象传入，方法内部会调用 ref 的 proceed()，那么按照顺序就是获取 AspectJAfterAdvice
    ③、调用 AspectJAfterAdvice 的 invoke()，内部会先调用 ref 的 proceed(),按照顺序获取 MethodBeforeAdviceInterceptor
    ④、调用 MethodBeforeAdviceInterceptor 的 invoke()，该方法内部会先执行 @Before 注解的方法，即前置通知逻辑，然后再调用 proceed()
    ⑤、最后这个 proceed()，由于 idx 是最后一个方法拦截器的位置，表示所有方法拦截器已经调用完成了，那么执行原始方法，然后直接 return
    ⑥、return 后到达 MethodBeforeAdviceInterceptor，再 return；然后返回 AspectJAfterAdvice，执行  @After 注解的方法，即后置通知逻辑，然后 return；
    ⑦、从 @After 方法拦截器返回后，有两种情况，可以看出方法拦截器都在上面 throws 异常，如果前面都发生了异常，那么 @AfterReturning 注解的方法不会执行，因为它没有捕获异常，不会执行下面的代码；而 @AfterThrowing 注解的方法会执行，因为它会 catch 异常，然后执行注解的方法，然后再将异常抛出。
 	小细节：@After 无论如何都会执行的，因为在 @After 中虽然没有 catch，但是在 finally 中来执行注解的方法，所以不会受到异常的影响
```





## 7、Spring 事务

```java
1、事务的实现

首先 Spring 事务是依靠 AOP 来实现的，默认是使用 CGLIB

标注了 @Service 注解的会被做成一个 AOP 代理对象，对注解了 @Transactional 的方法进行拦截，在方法拦截器中，会调用事务管理器创建事务

事务创建过程包含以下几个关键 api：
1）getTransaction()
2）handleExistingTransaction()
3）doBegin()

这里需要注意一下，所谓的一个事务，实际上就是对应一个数据库连接 Connection 

在 getTransaction() 中获取当前线程的事务状态
1）如果已经存在事务了，那么调用 handleExistingTransaction() 进行处理，比如 A 调用了 B，在 A 中已经创建了事务，那么在调用 B 的时候会发现当前线程已经存在事务， 那么根据 B 的事务传播行为进行处理
2）如果不存在事务，那么再根据调用方法的事务传播行为进行处理，如果需要创建一个新的事务,那么调用 doBegin() 创建一个新的事务

doBegin() 流程：
1）创建一个数据库连接 Connection
2）如果是自动提交那么设置为 false
3）将 Connection 和 当前线程进行绑定，事务管理器内部维护了一个 ThreadLocal，元素是 map，key 是数据源的信息对象（比如当前连接了多少个 connection），value 是对应的 connection 连接，这样一个线程就可以同时处理多个数据源多个连接而不会造成混乱
	因为 A 对应一个 connection，那么它对应的数据源信息对象中 连接数为 1
	A 调用了 B，那么如果 B 创建一个新的 connection，那么它对应的数据源信息对象中连接数为 2，跟 A 的数据源对象是不同的对象，根据这个对象可以区分它们的 connection
	
	
	
2、事务的异常和回滚
当创建完 connection，绑定完线程后，开始执行事务，这时候就涉及到异常和回滚的问题：
我们上面说了，所谓的事务实际上就是对应一个 connection
A 调用了 B，而如果 B 发生了异常，由于使用的是方法拦截，如果 B 没有捕获异常，那么从 B 往外抛的时候会在方法拦截器处被捕获，而不会直接抛到 A，一旦方法拦截器捕获了异常，那么就会执行 rollback() 回滚当前 connection 的事务，但它不会处理异常，它会在 catch 中继续将异常往上抛，这里就会抛到 A 处。
根据不同的事务传播行为有不同的回滚情况：
1）如果 A 和 B 共用一个连接，B 发生异常没有捕获，那么 A 和 B 都会回滚
2）如果 A 和 B 各自一个连接，B 发生异常没有捕获，那么 B 回滚，如果 A 没有捕获，那么 A 也回滚，如果 A 捕获了，那么 A 不会回滚


3、事务 AOP 自调用：
Service 对象是一个 AOP 对象，如果 A 和 B 都在同一个类中，那么 A 直接调用 B 是不会触发 B 的事务传播行为的，因为它是 this.B()，不会走代理方法

解决：
1）注入 ApplicationContext 对象，然后通过 getBean() 获取代理对象再进行调用
2）将 A 和 B 放到两个不同的 Service 类中，然后在 A 类中注入 B 类，这样获取的就是 B 的 AOP 代理对象



4、事务传播行为：
Spring 有 7 种事务传播机制：
1）REQUIRED：如果 A 存在事务，那么 B 也使用该事务，如果 A 没有事务，那么 B 创建一个事务（连接）
2）REQUIRED_NEW：不论 A 是否存在事务，B 都创建一个新的事务
3）SUPPORTS：B 可以有事务，也可以没有事务，看 A 的情况，如果 A 存在事务，那么 B 也使用该事务，如果 A 没有事务，那么 B 也以非事务方式执行
4）NOT_SUPPORTS：B 本身不支持事务，无论 A 是否存在事务，B 都以非事务方式执行
5）MANDATORY（mandatory 强制的）：强制要求 A 要有事务，如果 A 没有事务，那么 B 抛出异常，如果 A 有事务，那么 A 和 B 使用同一个事务
6）NEVER：强制要求 A 没有事务，如果 A 有事务，那么 B 抛出异常，如果 A 没有事务，那么 B 以非事务方式执行
7）NESTED：嵌套事务，如果 A 有事务，那么 B 创建 A 事务的一个子事务，如果 A 没有事务，那么 B 创建一个新的事务。
			如果是子事务，那么 A 发生异常， A 和 B 都回滚，如果 B 发生异常，如果 A 捕获了异常，那么只有 B 回滚
```





## 8、Spring MVC

```java
在 MVC 中最重要的是 DispatcherServlet ，它相当于指挥人员，用来给其他各个组件分发任务的。

在用户发起请求时，会进入到 service() 中，然后在该方法最终会调用到 DispatcherServlet 的 doDispatch()

doDispatch() 的运行流程：
1）找到能够处理该请求的 HanlderMapping，这里现在是 RequestMappingHandlerMapping 这个类来处理，它内部存储了所有 Controller 中 @RequestMapping 和 方法对象的映射，这个类实现了 InxxBean 接口的 afterPropertiesSet() 方法，在第七次后置处理器调用后会调用该方法，该方法内部会获取所有的 Controller，然后再将 @RequestMapping 注解信息转换为 RequestMappingInfo 对象，比如我们 @RequestMapping 注解了匹配哪个 url，匹配哪个 HTTP 请求方法，它转换为 RequestMappingInfo 对象进行存储，然后获取注解方法的 Method，使用 map 进行映射。在 RequestMappingHandlerMapping 中，它会根据用户请求 request 中的 url 和 HTTP 请求方法，扫描所有的 RequestMappingInfo，找到能够匹配的一个，如果存在多个匹配，那么进行排序，如果最终排序完还是找不到最优的，那么抛异常，否则就算是找到了调用的 Method 对象，这个对象实际上是 HandlerMethod，里面封装了 Method 和其他的信息，将这个 HandlerMethod 对象返回给 DispatcherServlet

2）DispatcherServlet 会根据 HandlerMethod 对象找到能够处理该对象的适配器 HandlerAdapter，这里算是使用了适配器模型，因为 HandlerMethod 对象有很多的类型，不同类型的 HandlerMethod 类型的处理逻辑都不一样，如果使用 if-else 来进行分支处理，那么显然是不利于扩展的，所以它为每个 HandlerMethod 都实现了一个适配器，通过遍历所有的适配器，调用它们的适配方法，如果适配的话，那么就使用该适配器进行处理，这样就算出现了新的 HandlerMethod 类型，也只需要创建一个新的 HandlerAdapter 即可，不需要修改原来的代码

3）适配器在处理的时候，如果是返回 JSON 类型，那么对应的适配器会直接将 JSON 数据写到页面上，不会生成 View 视图，如果是其他的类型，那么需要生成视图。
```





## 9、mybatis

```java
1、sql 注入
如果是单纯的 sql 拼接，那么假设 sql 语句为
	select a, b from test where id = '?'
假设恶意用户传输恶意参数 1' order by 1 #
这样的话，sql 语句就变成了 select a, b from test where id = '1' order by 1 #'
# 在 sql 中是用来注释的，最后面的 ' 会被 # 注释掉，导致恶意参数自带 ' 跟前面的 ' 进行匹配，改成了查找 id = 1 并且按照第一列进行排序
	问题在于，当 order by 1 成功后，表示 select 存在一列，那么下面使用同样的方法 order by 2，按照第二列排序，如果成功表示 select 存在两列，继续 order by 3，失败了，那么表示 select 最终查询出来的是两列数据，接下来它可以使用 union select 合并子查询来获取一些敏感的数据：表名、数据库名 之类的。
	
    
    
2、sql 预编译 PreparedStatement + 占位符
sql 语句在执行前，需要先经过 sql 解析（语法解析和词法解析、预处理），再让查询优化器生成对应的执行计划，但实际上执行的 sql 很大程度上只有参数会发生改变，而其余的部分是没有变化的，比如：
	select a, b from test where id = '?'
    只有 id 参数部分会发生变化，其他的部分都是不变的，如果每次都进行 sql 解析，显然是没必要的，因此出现了预编译。
 sql 预编译表示先将无参的 sql 语句进行解析，然后做成一个模板，提前生成语法树 和 执行计划，然后缓存起来，后续只需要将参数传进来，将参数填充到对应的数据结构中，然后就可以直接按照执行计划执行，而不需要再进行 sql 解析。
 	相当于是做成一个函数调用，对外开放接口进行传参的感觉。
 	
sql 预编译的优点：
    1）能够加快 sql 执行速度（一次编译，多次运行）
    2）够解决 sql 注入，因为用户的恶意参数会直接被当作参数填充到语法树的参数数据结构中，而不会再去将参数放到 sql 语句中重新开始解析。
 
    
    
 3、mybatis 解决 sql 注入
 mybatis 就是通过这种预编译的方式来解决 sql 注入，它会将所有的 sql 语句都交给 mysql 服务器进行 sql 解析，做成一个模板，后续直接将用户的参数传给 mysql 服务器进行调用即可。
 
 #{} 会进行预编译，而 ${} 不会进行预编译，所以 #{} 能够解决 sql 注入。
 
 
    
    
 4、mybatis 一级缓存和二级缓存
1）一级缓存：每个 sqlSession（connection 连接） 都有对应的一级缓存，底层是 Map，查询的时候会将得到的数据存储在一级缓存中，下次使用该 sqlSession 查询的时候直接从一级缓存中获取，如果数据发生增删改，那么缓存无效（insert、delete、update 任何数据都会导致缓存失效）
	一级缓存是默认开启的
2）二级缓存：每个 mapper 的 namespace 都有对应的二级缓存，所有属于这个 namespace 的 sqlSession 都中创建出来的 sqlSession 都会共用这个缓存
	二级缓存是默认关闭的
查询出来的数据都是默认存储在一级缓存中的，sqlSession 关闭了，才会将该 sqlSession 一级缓存的数据存储到二级缓存中
    
  
5、为什么我们在 mapper 类中方法参数需要使用 @Param 来指定参数名？
    比如我们 mapper 文件中存在方法：
    　　@Select("select * from student where  s_name= #{aaaa} and class_id = #{bbbb}") 
    	public Student select(@Param("aaaa") String name,@Param("bbbb")int class_id);  

因为 Java 在编译后方法参数名称是全部丢失的，所以反射获取一个方法再来获取它的参数得到的会是 var0、var1
所以这里我们需要使用 @Param 来跟参数进行绑定，告知这个参数的参数名是什么。
	（虽然 Java 8 增加了 Parameter 类，增加了虚拟机参数 -parampters 用来在编译时期保存方法参数名，但是 mybatis 为了兼容 Java 8 以下的版本，所以只能使用这种方式来保存参数名）
```

