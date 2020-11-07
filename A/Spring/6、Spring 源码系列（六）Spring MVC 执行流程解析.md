# Spring MVC



## 1、Spring MVC 的执行流程图

![img](https://img-blog.csdn.net/20181024222954778?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2d1ZHVkZWRhYmFp/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

![img](https://img-blog.csdnimg.cn/20190228202741179.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NDA3ODE5Ng==,size_16,color_FFFFFF,t_70)





## 2、Spring MVC 各个组件的关系和作用

1. 前端控制器  DispatcherServlet  
2. 处理映射器  HandlerMapping
3. 处理适配器 HandlerAdapter
4. 处理器 Handler
5. 视图解析器 View Resolver
6. 视图 View



各个组件大体功能如下：

**DispatcherServlet：**调度器，控制整个流程，对其他各个组件进行分工，拦截器拦截的请求会 调用 DispatcherServlet 的 service()，交给 DispatcherServlet 去处理

**HandlerMapping ：**管理所有的 Handler，它会根据 request 的 HTTP 类型 和 url 解析出应该调用哪个 Handler，即内部存储的是映射，通过映射关系可以获取调用的 Handler

**HandlerAdapter：**为了消除不同类型的 Handler 的方法调用差异，需要转换为适配器

**Handler：**实际上是 HandMethod 类型，内部存储了某个 Controller 的 name 和 Class 对象，以及 该 Controller 中某个方法的 Method 对象，后续 HandMethod 会被封装为 **HandlerExecutionChain**

View Resolver：对 ModelAndView 对象进行解析，根据内部的 View 信息解析成真正的视图 View（如通过一个JSP路径返回一个真正的JSP页面）

View：其本身是一个接口，实现类支持不同的View类型（JSP、FreeMarker、Excel 等）。



以上 6 个组件中，需要程序员编写的是 Handler (Handler 是由 Controller 分解产生，所以程序员编写的应该是 Controller） 和 View（html 页面）





## 3、DispatcherServlet 处理流程

从最上面的流程图可以看出，用户的请求最先进入到 DispatcherServlet 类，这个类也是Spring MVC 最为核心的类

```java
public class DispatcherServlet extends FrameworkServlet {
}

public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {
}

public abstract class HttpServletBean extends HttpServlet implements EnvironmentAware {
}

public abstract class HttpServlet extends GenericServlet {
}

public abstract class GenericServlet implements Servlet, java.io.Serializable{
}
```

可以看出 DispatcherServlet 实际上是 HttpServlet 的一个子类，而 HttpServlet 又是 Servlet 的一个子类

即 涉及到的 类都是 Servlet 类，而在 Servlet 接口中，存在一个最为重要的方法：service()

Servlet 拦截用户的请求后就是进入到 service() 中进行处理，所以 DispatcherServlet 处理用户请求的入口逻辑就是这个 service()



DispatcherServlet 类内部没有 service() 方法，该方法继承了 父类 FrameworkServlet 的逻辑

FrameworkServlet 的 service() 的 逻辑如下：

```java
@Override
protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
    if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
        //调用 doService()
        doService(request, response);
    }
    else {
        super.service(request, response);
    }
}
//FrameworkServlet 的 doService()，一个抽象方法
protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
			throws Exception;
```

在 service() 中调用了 doService()，但是在 FrameworkServlet 中，该方法为一个抽象方法，需要由子类实现，所以该方法的具体实现是在 DispatcherServlet 中

DispatcherServlet 中 doService() 主要逻辑如下：

```java
@Override
protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Map<String, Object> attributesSnapshot = null;
    if (WebUtils.isIncludeRequest(request)) {
        attributesSnapshot = new HashMap<>();
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                attributesSnapshot.put(attrName, request.getAttribute(attrName));
            }
        }
    }

    // Make framework objects available to handlers and view objects.
    request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
    request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
    request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
    request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

    try {
        //调用 doDispatch()
        doDispatch(request, response);
    }
    finally {

    }
}
```

在 doService() 中在给 request 设置了一些参数后，调用了 doDispatch()，该方法是 Spring MVC 的核心处理逻辑，进入该方法才是真正进入了 Spring MVC 

DispatcherServlet 中 doDispatch() 主要逻辑如下：

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;
    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

    try {
        try {
            //创建一个视图对象 ModelAndView
            ModelAndView mv = null;
            Object dispatchException = null;
            try {
                processedRequest = this.checkMultipart(request);
                multipartRequestParsed = processedRequest != request;
                //获取处理请求的 视图对象 handler，里面封装了我们的 Controller 对象
                mappedHandler = this.getHandler(processedRequest);
                if (mappedHandler == null) {
                    this.noHandlerFound(processedRequest, response);
                    return;
                }
                //获取 handler 对应的适配器
                HandlerAdapter ha = this.getHandlerAdapter(mappedHandler.getHandler());
                
                //适配器 ha 执行 handler，并返回一个 视图 ModelAndView（如果是返回的 JSON 数据，那么这里不会返回 ModelAndView 对象，直接在 返回值处理器中将数据写回页面了）
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }
                this.applyDefaultViewName(processedRequest, mv);
                mappedHandler.applyPostHandle(processedRequest, response, mv);
            }
			//对 ModelAndView 对象解析，获取 View
            this.processDispatchResult(processedRequest, response, mappedHandler, mv, (Exception)dispatchException);
        } 
    } 
}
```



## 4、HandlerMapping 组件

当 DispatcherServlet 获取到用户请求后，会委托 HandlerMapping 解析 request 需要调用的 handler



映射器接口 HandlerMapping 实现链：

*![image.png](https://pic.leetcode-cn.com/1604305019-EHATUb-image.png)*



我们现在使用的是注解开发，因此使用 RequestMappingHandlerMapping 来处理注解

```java
@RestController
@RequestMapping("/user")
public class UserController {

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public User getUser(HttpServletRequest request, @PathVariable Integer id) {
		return new User();
	}
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public User getUserString(HttpServletRequest request, @PathVariable String id) {
		return new User();
	}
    @RequestMapping(value = "/condition", method = RequestMethod.GET)
	public User getByNameOrAge(@RequestParam String name, @RequestParam Integer age) {
		return new User();
	}
}
```



RequestMappingHandlerMapping 的父类是 AbstractHandlerMethodMapping

AbstractHandlerMethodMapping 源码如下：

```java
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
	
    //维护了一个 MappingRegistry 对象
    private final MappingRegistry mappingRegistry = new MappingRegistry();

    
    class MappingRegistry {
		
        private final Map<T, MappingRegistration<T>> registry = new HashMap<>();
		/*
		这里的 T 一般是 RequestMappingInfo
		因此这里的 map 维护了 RequestMappingInfo 和 HandlerMethod 的映射关系
		*/
        private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<>();
		
        private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<>();
        private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

        private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    }
}
```



我们需要知道，RequestMappingInfo 中的参数来源是我们指定的，在某个方法上使用了 @RequestMapping

@RequestMapping 注解会转换为 RequestMappingInfo 对象，而 RequestMappingInfo 会跟 getUser() 方法绑定在一起，用于后续的映射查找

```java
@RequestMapping(value = "/{id}", method = RequestMethod.GET, params = "[]")
public User getUser(HttpServletRequest request, @PathVariable Integer id) {
    return new User();
}
```

RequestMappingInfo 源码如下：

```java
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	@Nullable
	private final String name;
	//能够匹配的 url 格式 [/user/{id}]，对应 @RequestMapping 的 value
	private final PatternsRequestCondition patternsCondition;
	//能够匹配的 HTTP 类型 比如 [GET]，对应 @RequestMapping 注解的 method
	private final RequestMethodsRequestCondition methodsCondition;
    //能够匹配的参数类型，对应 @RequestMapping 注解的 params
    private final ParamsRequestCondition paramsCondition;
	//能够匹配的请求头类型，对应 @RequestMapping 的 headers 
	private final HeadersRequestCondition headersCondition; 
	//能够匹配的 post 内容的类型，比如 application/json, text/html; 对应 @RequestMapping 的 consumes
	private final ConsumesRequestCondition consumesCondition;
	//返回的内容类型，比如 application/json，对应 @RequestMapping 的 produces
	private final ProducesRequestCondition producesCondition;
	//自定义条件
	private final RequestConditionHolder customConditionHolder;
	
    //重写 equals()，用于 map 查找
    @Override
	public boolean equals(Object other) {
		RequestMappingInfo otherInfo = (RequestMappingInfo) other;
		return (this.patternsCondition.equals(otherInfo.patternsCondition) &&
				this.methodsCondition.equals(otherInfo.methodsCondition) &&
				this.paramsCondition.equals(otherInfo.paramsCondition) &&
				this.headersCondition.equals(otherInfo.headersCondition) &&
				this.consumesCondition.equals(otherInfo.consumesCondition) &&
				this.producesCondition.equals(otherInfo.producesCondition) &&
				this.customConditionHolder.equals(otherInfo.customConditionHolder));
	}
    
    /**
    	getMatchingCondition() 判断 用户 request 和 当前 RequestMappingInfo 是否匹配
    */
    @Override
	@Nullable
	public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
        /*
        当前 RequestMappingInfo 匹配的 HTTP 类型为 [GET],匹配的 url 格式为 [/user/{id}]
        request 请求中 HTTP 类型为 get，url 为 [user/1]
        判断 request 的 HTTP 类型 和 url 是否能够跟 当前 RequestMappingInfo 对象匹配
        */
        //匹配 方法的 HTTP 类型
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(request);
		if (methods == null) {
			return null;
		}
        //匹配 url 格式，比如用户的是 [user/1]，而当前的 patternsCondition 是 [/user/{id}]，类似模式匹配
		PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(request);
		if (patterns == null) {
			return null;
		}

		return new RequestMappingInfo(this.name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition());
	}
```



HandlerMethod 源码如下：

```java
public class HandlerMethod {

	//这里一般是 String 类型，存储 bean 的名字，比如 userController
    private final Object bean;
    
	//上面的 bean 的 Class 对象 cn.oy.cache.UserController
    private final Class<?> beanType 
        
	//对应 bean 内 反射的方法对象 Method，比如 getUser 方法的 Method
    private final Method method;
}
```





综上，RequestMappingInfo 存储了匹配的 HTTP 类型 和 匹配 url 格式、匹配的请求参数等 ，HandlerMethod 记录了 某个Controller 以及 该 Controller 对应的某个 方法对象 Method

在 RequestMappingHandlerMapping 的父类 AbstractHandlerMethodMapping 中维护了一个 MappingRegistry 对象，该对象内部又维护了多个 map，其中一个 map 对象 mappingLookup 存储了 RequestMappingInfo 和 HandlerMethod 的映射关系

比如

```java
@RestController
@RequestMapping("/user")
public class UserController {

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public User getUser(HttpServletRequest request, @PathVariable Integer id) {
		return new User();
	}
}
```

**RequestMappingInfo 和  HandlerMethod  的映射关系就是 [GET] + [/user/{id} ➡ ➡ userController + getUser**

当用户发起请求时，通过 mappingLookup  的 entrySet() 获取所有的 RequestMappingInfo ，然后将 每个 RequestMappingInfo 和 用户 request 进行匹配，一旦匹配成功，那么就调用 `mappingLookup.get()` ，获取 RequestMappingInfo 对应的 HandlerMethod ，即可得知到用户 request 对应请求的是 哪个 Controller 的 哪个方法



Spring MVC 使用这种 RequestMappingInfo 和 HandlerMethod  映射的方式，实际上是分解了所有的 Controller ，即在查找的时候，不会去查找请求的是哪个 Controller，因为它将所有的 Controller 的方法都作为一个 HandlerMethod  组件，存储在一个 map 集合中

在 Spring MVC 眼里，对于用户的请求，只有 RequestMappingInfo 这一种匹配方式，只要用户请求的 request 匹配某个 RequestMappingInfo ，那么就直接获取映射的 HandlerMethod ，选定的这个 HandlerMethod 就是用来处理用户请求的



> RequestMappingHandlerMapping 什么时候处理好 HandlerMethod 的？

`start`

RequestMappingHandlerMapping 的父类 AbstractHandlerMapping 实现了 InitializingBean 接口，它有一个 afterPropertiesSet()

学过 Spring 应该知道，该方法在 第七次后置处理器 调用完后会调用 bean 的该方法

在 AbstractHandlerMapping bean 创建过程中，在第七次后置处理器调用完成后，会调用该方法，然后遍历所有的 bean，从中找到 Controller，获取 Controller 中存在 @RequestMapping 注解的方法，将 它 和 它注解的 @RequestMapping 得到的 RequestMappingInfo 对象 形成映射关系，注册进 mappingRegistry 对象中



```java
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
    
    private final MappingRegistry mappingRegistry = new MappingRegistry();
    
    @Override
    public void afterPropertiesSet() {
        initHandlerMethods();
    }
    
    protected void initHandlerMethods() {
        /*
        遍历所有的 beanName，只处理 Controller 之类的 bean
        在 AbstractHandlerMethodMapping 第七次后置处理器调用完成后，
        调用 afterPropertiesSet() 后来这里处理 Controller
        */
		for (String beanName : getCandidateBeanNames()) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				processCandidateBean(beanName);
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
	}
    
    protected void processCandidateBean(String beanName) {
		Class<?> beanType = obtainApplicationContext().getType(beanName);
		if (beanType != null && isHandler(beanType)) {
            //主要处理逻辑
			detectHandlerMethods(beanName);
		}
	}
    
    protected void detectHandlerMethods(Object handler) {
		Class<?> handlerType = (handler instanceof String ?
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		if (handlerType != null) {
            //这里的 userType 是 Controller 的类型
			Class<?> userType = ClassUtils.getUserClass(handlerType);
            //获取 Controller 存在 @RequestMapping 注解的方法
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> {
						try {
                            //这里是返回一个 RequestMappingInfo
							return getMappingForMethod(method, userType);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Invalid mapping on handler class [" +
									userType.getName() + "]: " + method, ex);
						}
					});
            /*
            遍历 methods，
            调用 registerHandlerMethod() 将每个 method 注册到 mappingRegistry 中
            
            method：Controller 的方法对象
            mapping：方法对象对应的 RequestMappingInfo，在 return 时获取的
            */
			methods.forEach((method, mapping) -> {
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
                //注意：这里的 handler 是 beanName，而不是 HandleMethod，需要在该方法内部封装成 HandleMethod
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
	}
}
```

`end`

## 5、HandlerAdapter 组件

当 HandlerMapping 解析出 Handler 后，DispatcherServlet 会委托 HandlerAdapter 使用 Handler 处理 用户请求，返回视图



适配器接口 HandlerAdapter 实现链：

*![image.png](https://pic.leetcode-cn.com/1604304874-rIdyVy-image.png)*

我们现在使用的是 注解开发，因此使用的是 **RequestMappingHandlerAdapter**，其他的几个是在 XML 配置 Controller/Servlet 之类的 才使用的



> 为什么要使用适配器？

`start`

HandlerMapping 存在多个实现类，并且每个实现类返回的 handler 类型各不相同，它们对参数的获取 之类的逻辑各不相同

如果要 DispatcherServlet 来处理这些 handler，那么 DispatcherServlet 就需要开不同的分支，对不同类型的 handler 定义一个不同的方法，以此来处理不同的逻辑，同时，如果需要添加一种新的 handler 类型，那么 DispatcherServlet 还需要修改代码

这么做显然是不合理的

因此，出现了适配器，给每个不同类型的 handler 定义一个适配器类，让这些适配器类去处理各自的 handler 类型

这样的话，只要适配器实现相同的适配器接口，DispatcherServlet 只需要维护一个 适配器列表，当需要处理一个 handler 的时候，DispatcherServlet 只需要遍历 适配器列表，找到一个能够处理该 handler 的适配器即可

这样跟 DispatcherServlet 交互的只有一个 适配器接口，后续添加i新的 handler 和 适配器 也不需要改动 DispatcherServlet 代码



简单来讲，所谓的适配器就是将不同的 handler 的处理逻辑抽象成一个个的类，并且这些类实现同一个接口，方便统一管理

适配器模式就是为了让存在差异的多个类适配同一套方案

对于 FutureTask 来说，就是为了让 Runnable 适配 Callable 的方案

对于 HandlerAdapter 来说，就是为了解耦，使得 DispatcherServlet 对于 handler 的扩展不需要轻易修改代码，让所有的 handler 类型都适配 DispatcherServlet 定义的这一套处理流程

`end`



RequestMappingHandlerAdapter 的父类是 AbstractHandlerMethodAdapter

适配器接口存在三个方法，其中重要的有两个方法：support() 和 handle()

support()：判断当前适配器是否适配 传入的 handler 对象

handle() ：如果当前适配器适配 指定的 handler 对象，那么调用 handler() 方法处理该 handler 对象

这两个方法的实现都是在 AbstractHandlerMethodAdapter 类中

```java
public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {

    //RequestMappingHandlerAdapter 用来处理 HandlerMethod 类型的 handler 对象
    @Override
    public final boolean supports(Object handler) {
        //判断 handler 对象是否是 HandlerMethod 类型
        return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
    }

    @Override
    @Nullable
    public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
		//调用 handleInternal()
        return handleInternal(request, response, (HandlerMethod) handler);
    }
}
```

在 handler() 中调用的是 handleInternal()，该方法在子类 RequestMappingHandlerAdapter 中实现

handleInternal() 逻辑如下：

```java
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
		implements BeanFactoryAware, InitializingBean {	
    @Override
    protected ModelAndView handleInternal(HttpServletRequest request,
            HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

        ModelAndView mav;
        checkRequest(request);

        // 判断是否需要同步 session，即同一个 session 的请求线性执行，默认为 false
        if (this.synchronizeOnSession) {
            // 获取当前请求的 session 对象
            HttpSession session = request.getSession(false);
            if (session != null) {
                // 为当前 session 生成一个唯一的可以用于锁定的 key
                Object mutex = WebUtils.getSessionMutex(session);
                synchronized (mutex) {
                	/*
                	invokeHandlerMethod（）是处理用户请求的核心逻辑方法，这里就不进去看了
                	*/
                    // 对 HandlerMethod 进行参数等的适配处理，并调用目标 handler
                    mav = invokeHandlerMethod(request, response, handlerMethod);
                }
            } else {
                // 如果当前不存在 session，则直接对 HandlerMethod 进行适配
                mav = invokeHandlerMethod(request, response, handlerMethod);
            }
        } else {
            // 如果当前不需要对 session 进行同步处理，则直接对 HandlerMethod 进行适配
            mav = invokeHandlerMethod(request, response, handlerMethod);
        }

        // 判断当前请求头中是否包含 Cache-Control 请求头，如果不包含，则对当前 response 进行处理，
        // 为其设置过期时间
        if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
            // 如果当前 SessionAttribute 中存在配置的 attributes，则为其设置过期时间。
            // 这里 SessionAttribut e主要是通过 @SessionAttribute 注解生成的
            if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
                applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
            } else {
                // 如果当前不存在 SessionAttributes，则判断当前是否存在 Cache-Control 设置，
                // 如果存在，则按照该设置进行 response 处理，如果不存在，则设置 response 中的
                // Cache 的过期时间为 -1，即立即失效
                prepareResponse(response);
            }
        }
        return mav;
    }
}
```





## 6、获取 ModelAndView

通过调用 HandlerAdapter（RequestMappingHandlerAdapter） 的 handle() 对 HandlerMapping 返回的 handler 进行处理，获取 ModelAndView 对象

handle() 有两种处理情况：

1. 在方法上添加 @ResponseBody 或者 类上添加 @RestController，这种是属于直接返回 JSON，那么它就不会生成 ModelAndView，而是直接将数据写回到页面上
2. 返回一个 html、jsp 页面 或者 Excel 表格，这种就需要生成 ModelAndView，然后解析成 View 再返回



> ### 1、处理 JSON

假设我们请求 user/1，最终返回的应该是 `不得了`JSON 字符串

```java
@Controller
@RequestMapping("/user")
public class UserController {
    @ResponseBody
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String getUser(HttpServletRequest request, @PathVariable Integer id) {
        return "不得了";
    }
}
```



首先，经过 DispatcherServlet  doDispatch() 在 RequestMappingHandlerMapping 中获取 handler，然后遍历所有的适配器，选定合适的适配器 RequestMappingHandlerAdapter ，再调用 RequestMappingHandlerAdapter 的 handle() 处理 handler 对象（HandlerMethod 类型）

```java
mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
```



经过中间方法调用，进入到 RequestMappingHandlerAdapter 的 invokeHandlerMethod()

```java
@Nullable
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
                                           HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

    try {
		/*
		这里将 handler 封装为 ServletInvocableHandlerMethod，将 handler 作为它内部的一个变量进行维护
		ServletInvocableHandlerMethod 也是一个 HandlerMethod 对象
		*/
        ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
      	
        //ModelAndView 容器
        ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
        modelFactory.initModel(webRequest, mavContainer, invocableMethod);
        mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);
        
        //调用 ServletInvocableHandlerMethod 的 invokeAndHandle()
        invocableMethod.invokeAndHandle(webRequest, mavContainer);
        return getModelAndView(mavContainer, modelFactory, webRequest);
    }
    finally {
        webRequest.requestCompleted();
    }
}
```



ServletInvocableHandlerMethod 的 invokeAndHandle() 逻辑如下：

```java
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
                            Object... providedArgs) throws Exception {
	
    //这里执行 handler 对象的方法，获取到该方法的返回值 returnValue
    Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
    setResponseStatus(webRequest);

    mavContainer.setRequestHandled(false);
    try {
        /*
        	getReturnValueType(returnValue) 是将返回值封装成一个 MethodParameter 对象，同时该对象内部还含有 handler对象 的注解信息，比如 @RequestMapping、@ResponseBody
        	调用 handleReturnValue() 处理返回值
        */
        this.returnValueHandlers.handleReturnValue(
            returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
    }
    catch (Exception ex) {
        throw ex;
    }
}
```



返回值处理方法：handleReturnValue() 方法逻辑如下：

```java
@Override
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                              ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
	//调用 selectHandler() 根据 returnType 选择一个返回值处理器
    HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
    if (handler == null) {
        throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
    }
    //使用上面选定的返回值处理器对返回值进行处理
    handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
}
```



返回值处理器选择方法：selectHandler() 方法逻辑如下：

```java
@Nullable
private HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, MethodParameter returnType) {
    boolean isAsyncValue = isAsyncReturnValue(value, returnType);
    for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
        //遍历所有的返回值处理器，调用 supportsReturnType() 根据 returnType 判断是否支持处理该返回值
        if (handler.supportsReturnType(returnType)) {
            return handler;
        }
    }
    return null;
}
```



最终是一个叫 RequestResponseBodyMethodProcessor 的返回值处理器来处理 JSON 类型的返回值

可以看到它的 supportsReturnType() 就是判断该 handler 的 Controller 类上是否有 @ResponseBody 或者 该 handler 上是否存在 @ResponseBody 

```java
@Override
public boolean supportsReturnType(MethodParameter returnType) {
    //判断该 handler 的 Controller 类上是否有 @ResponseBody 或者 该 handler 上是否存在 @ResponseBody 
    return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||
            returnType.hasMethodAnnotation(ResponseBody.class));
}
```



当选定后，就是调用 返回值处理器 的 handleReturnValue() 对返回值进行处理，该处理器的方法逻辑 是**直接将 JSON 数据写回到页面上**，后续就生成的 ModelAndView 对象就为 null，不会再进行额外处理



> ### 2、HTML 页面

假设我们请求 user/1，最终返回的应该是 `不得了.html`页面

```java
@Controller
@RequestMapping("/user")
public class UserController {
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String getUser(HttpServletRequest request, @PathVariable Integer id) {
        return "不得了";
    }
}
```



前面的跟 JSON 的流程一样， 区别在 选择 返回值处理器，由于最终需要返回的是 HTML 页面，所以不会在 Controller 上添加 @ResponseBody 或者 在方法上添加 @ResponseBody，那么就不会选择 RequestResponseBodyMethodProcessor 作为返回值处理器，这里选定的是 ViewNameMethodReturnValueHandler 作为返回值处理器

以下是 ViewNameMethodReturnValueHandler 的 supportsReturnType()：

```java
@Override
public boolean supportsReturnType(MethodParameter returnType) {
    //获取返回值的类型
    Class<?> paramType = returnType.getParameterType();
    /*
    判断返回值是否为空，或者是否是 String 类型（String 实现了 CharSequence 接口），
    因为如果要解析为 html 页面的话，那么返回的就应该是 字符串，这样才能够跟 suffix 进行拼接查找请求的 html 页面
    */
    return (void.class == paramType || CharSequence.class.isAssignableFrom(paramType));
}
```



选定好返回值处理器后，调用它的 handleReturnValue() 处理器返回值：

```java
@Override
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
                              ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
	
    /*
    判断 返回值 是否是 String 类型
    如果不是 String 类型，但又不为空，那么当前 返回值处理器无法处理，抛出异常
    如果是的话，那么将 返回值 "不得了" 设置到 ModelAndViewContainer 视图容器 中
    
    注意：这里还会调用一个 isRedirectViewName()，该方法是判断返回值是否及逆行重定向
    */
    if (returnValue instanceof CharSequence) {
        String viewName = returnValue.toString();
        mavContainer.setViewName(viewName);
        //如果需要重定向
        if (isRedirectViewName(viewName)) {
            //将 ModelAndViewContainer 中重定向标识设置为 true
            mavContainer.setRedirectModelScenario(true);
        }
    }
    else if (returnValue != null) {
        // should not happen
        throw new UnsupportedOperationException("Unexpected return type: " +
                                                returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
    }
}
```



isRedirectViewName() 方法逻辑如下：

```java
protected boolean isRedirectViewName(String viewName) {
    /*
    正则匹配如果以 指定的 redirectPatterns 字符串开头，
    或者以 "redirect:" 字符串开头，那么表示需要进行重定向
    
    redirectPatterns 字符串默认为 空
    */
    return simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:");
}
```



当 返回值处理器的 handleReturnValue() 处理完成后，将属性都封装在了 ModelAndViewContainer 中，后面调用 getModelAndView() ，从 ModelAndViewContainer 中抽取出信息，封装成一个 ModelAndView 对象，然后一路**从 适配器 处 返回**给 doDispatch() 处的调用

后面使用 ModelAndView 对象中的 返回值 "不得了" 拼接 suffix 后缀形成 `不得了.html`，然后到静态页面目录找到对应的 html 文件返回即可

需要注意的是：在拼接前，会判断是否存在 "forward:" 或者 "redirect:" 前缀，如果有，那么特殊处理

最终返回的 View 的类型为 InternalResourceView

*![image.png](https://pic.leetcode-cn.com/1604556940-kzgisp-image.png)*