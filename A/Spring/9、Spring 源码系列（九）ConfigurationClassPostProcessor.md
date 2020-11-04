# ConfigurationClassPostProcessor



ConfigurationClassPostProcessor 是 IOC 后置处理器的核心，就是它完成注解扫描，将它们转化成 BeanDefinition 存储到 BeanFactory 中的

那么具体过程是如何的呢？下面进行详细的解说



定义好一个配置类 AppConfig

```java
@Configuration
@ComponentScan({"com.luban"})
@EnableAspectJAutoProxy(proxyTargetClass = true)//proxyTargetClass = true，表示强制使用 CGLIB 代理，默认为 false
@ImportResource("classpath:spring.xml")
@Import(Giao.class)
public class AppConfig {

	@Bean("index")
	public IndexDao indexDao(){
		indexDao1();
		return new IndexDao();
	}

	@Bean("dao1")
	public IndexDao1 indexDao1(){

		return new IndexDao1();
	}
}
```

首先我们会先调用 register() 将我们写好的配置类 AppConfig 注册进去

然后调用 rehash() 刷新容器，实际上就是启动 Spring 服务

```java
public class AppTest {
	public static void main(String[] args) throws NoSuchFieldException {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		ac.register(Appconfig.class);
		ac.refresh();
	}
}
```



以下是 rehash() 的整个过程：

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        prepareRefresh();

        //获取 beanFacotry ---- DefaultListableBeanFactory
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        //准备工厂,（添加后置处理器）
        prepareBeanFactory(beanFactory);

        //这个方法在当前版本的spring是没用任何代码的
        //可能spring期待在后面的版本中去扩展吧
        postProcessBeanFactory(beanFactory);

        //在spring的环境中去执行已经被注册的 factory processors
        //重点：在这个方法中执行 ConfigurationClassPostProcessor，进行类的扫描，将类转换为 BD 注册进行 Bean 工厂
        invokeBeanFactoryPostProcessors(beanFactory);

        registerBeanPostProcessors(beanFactory);
        initMessageSource();
        //初始化应用事件广播器
        initApplicationEventMulticaster();
        onRefresh();
        //注册监听器，这是 spring 事件监听器的一部分
        registerListeners();
        //实例化 bean，利用 beanFactory 来进行实例化
        finishBeanFactoryInitialization(beanFactory);
        finishRefresh();
    }
}
```



在 invokeBeanFactoryPostProcessors() 中最终会进入到 ConfigurationClassPostProcessor 中的 processConfigBeanDefinitions()

在该方法中进行类的扫描以及转换 BD 以及注册

```java
public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {

    //BeanDefinitionHolder 是一个封装，内部存储了 BeanDefinition 和 beanName
    List<BeanDefinitionHolder> configCandidates = new ArrayList<>();

    //所有的 beanName 进行解析，这里只存在我们 手动 register() 进行来的 AppConfig.class，以及 Spring 自动注入的几个 后置处理器
    String[] candidateNames = registry.getBeanDefinitionNames();

    /*
    所有真正进行解析的只有 AppConfig 类，它会解析该类上的 @Configuration 、@ComponentScan、@Import 之类的
    
    1、解析 @ComponentScan，会获取对应扫描的包，进而获取扫描的类，然后将这些类生成 BeanDefinition 注册进 Bean 工厂
    
    2、解析 @Import，会获取 导入的类，如果导入的类是一个普通类，那么会先存放进一个 Set 集合中，
     	如果导入的类是一个 ImportSelector，那么会调用 selectImports() 方法获取字符串数组，然后递归进行判断，如果是普通类，那么存储进上面的 Set 集合中，如果是 ImportSelector，同样方法进行
     	
    3、解析 @Configuration，获取内部的 @Bean 方法，存储进上面的 Set 集合中
    
    最终在 parse() 后下面的 loadBeanDefinitions() 方法中处理这个 Set 集合，将它们转换为 bean，注册进 bean 工厂
    即在 parse() 中，只有 @ComponentScan 扫描出来的 bean 以及我们手动注册进去的 AppConfig 才会注册进 Bean 工厂
    其他的 @Import、@Bean 都是在 parse() 后统一处理的
    */
    for (String beanName : candidateNames) {
        //从 bean 工厂中获取 bd
        BeanDefinition beanDef = registry.getBeanDefinition(beanName);
        //在 checkConfigurationClassCandidate() 中会判断是否是 @Configuration，如果是则根据 proxyBeanMethod 的值标注为 Full 类 或者 Lite 类
        if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
            configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
        }
    }

    //ConfigurationClassParser：真正用来解析各个配置类
    ConfigurationClassParser parser = new ConfigurationClassParser(
        this.metadataReaderFactory, this.problemReporter, this.environment,
        this.resourceLoader, this.componentScanBeanNameGenerator, registry);

    //candidates 需要解析的类，这里不包括几个后置处理器，只存储了 AppConfig 配置类
    Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);

    //parse() 对类进行解析，candidates 集合内只有我们手动注册进去的 AppConfig
    parser.parse(candidates);

    Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
   
    //将 @Import、@Bean 的类组成的集合在这里统一处理，注册到 Bean 工厂，并且注册 ImportBeandDefinitionRegistrar
    this.reader.loadBeanDefinitions(configClasses);
}
```

在上面方法中有两个主要方法：parse() 和 loadBeanDefinitions()

parse() 用来解析我们手动注册的 AppConfig，它会扫描它所有的注解，比如 @Configuration、@ComponentScan、@Import 之类的

然后通过处理这些注解，进而获取到需要注入的 @Bean 注入类、@Import 注入类、@ComponentScan 扫描范围下的类，对于普通的类，会将它们转换为 BeanDefinition，然后注册进 BeanFactroy 中，对于 @Bean 和 @Import 的类，会将它们存储到一个 Set 集合中，然后在  loadBeanDefinitions() 方法中再进行处理，当然，关于 @Import 还有几种情况，后面再讲



parse() 方法逻辑如下：

```java
protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
    doProcessConfigurationClass(new ConfigurationClass(metadata, beanName));
}


protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
    throws IOException {

    /*
			1、处理 @ComponentScan		
		*/
    Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
        sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
    if (!componentScans.isEmpty() &&
        !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
        //①、for 循环
        for (AnnotationAttributes componentScan : componentScans) {
            /*
				②、根据扫描路径获取所有的 BeanDefinition
				这里在 parse() 中完成某个路径 componentScan 下的扫描并且将对应路径下的类转换为 BD 注册进 Bean 工厂
				后返回，因此 bean 的注册在 parse() 方法中
				*/
            Set<BeanDefinitionHolder> scannedBeanDefinitions =
                this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());


            //检查扫描出来的类当中是否还有configuration
            for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
                BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
                if (bdCand == null) {
                    bdCand = holder.getBeanDefinition();
                }
                //检查  todo
                if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                    parse(bdCand.getBeanClassName(), holder.getBeanName());
                }
            }
        }
    }

    /*

			2、处理 @Import

		*/
    processImports(configClass, sourceClass, getImports(sourceClass), true);

    return null;
}
```

在 doProcessConfigurationClass() 方法中，有两个重要方法：parse() 和 processImports()

parse() 是完成简单 @ComponentScan 路径下的扫描，将它们注册成 bean 的，并且返回后如果它们存在 @Import、@Confuguration 之类的，还会进行处理



processImports() 则是处理 @Import 导入的类



我们看看 parse()，它内部最终会调用 doScan() 扫描包路径下的类，然后进行注册

```java
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    //basePackages 是 @ComponentScan 指定的所有扫描路径，这里遍历所有路径，一个个进行处理
    for (String basePackage : basePackages) {
        //扫描 basePackage 路径下的类
        //符合条件的并把它转成 ScannedGenericBeanDefinition 类型 -- sgbd
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);

        for (BeanDefinition candidate : candidates) {
            //解析scope属性
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            //给 scope 赋值
            candidate.setScope(scopeMetadata.getScopeName());

            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
		
            BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
            beanDefinitions.add(definitionHolder);
            //加入到 BeanFactory 中，这个 this.registry 就是 DefaultListableBeanFactory，即所谓的 Bean 工厂
            registerBeanDefinition(definitionHolder, this.registry);
        }
    }
    return beanDefinitions;
}
```



再看看 processImports() 方法：

```java
private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
                            Collection<SourceClass> importCandidates, boolean checkForCircularImports) {

    for (SourceClass candidate : importCandidates) {
        //1、处理 ImportSelector
        if (candidate.isAssignable(ImportSelector.class)) {
            Class<?> candidateClass = candidate.loadClass();
            //反射创建 实现了 ImportSelector 接口的对象
            ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
            //调用 selectImports() 得到需要注入的类的字符串数组
            String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
            //将 类名字符串数组 转换成一个 存储 SourceClass 元素的集合
            Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
            //递归调用 processImports()，解析 这个数组转换成的集合
            processImports(configClass, currentSourceClass, importSourceClasses, false);
        }
        //2、处理 ImportBeanDefinitionRegistrar，只是扫描该类转换成 bd 注册进 bdMap，尚未调用方法
        else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
            Class<?> candidateClass = candidate.loadClass();
            ImportBeanDefinitionRegistrar registrar =
                BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
            ParserStrategyUtils.invokeAwareMethods(
                registrar, this.environment, this.resourceLoader, this.registry);
            configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
        }
        //3、处理 @Import 注入 或者 ImportSelector 返回的普通类，那么添加到 Set 集合中，在后续注入
        else {
            processConfigurationClass(candidate.asConfigClass(configClass));
        }
    }
}
```



至此，在 ConfigurationClassPostProcessor  中调用的 parse() 方法解析全部完成，回到 parse() 调用的地方：

```java
Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);

parser.parse(candidates);

//将 尚未注册的 bd 注册到 bdMap，并且注册 ImportBeandDefinitionRegistrar
this.reader.loadBeanDefinitions(configClasses);
```

parse() 调用后，它会再调用一个 loadBeanDefinitions()，它就是用来处理 @Import、@Bean 注入的普通类，将它们经过特殊处理转换为 BD 然后注册进 Bean 工厂中



最终，完成类的扫描和注册