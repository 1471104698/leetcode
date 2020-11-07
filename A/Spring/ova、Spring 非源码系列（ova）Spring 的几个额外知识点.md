# Spring 的几个额外知识点



## 1、Bean 的作用域

- singleton：整个 IOC 容器只有一个对象
- prototype：原型，每次注入创建一个新的对象
- request：每次请求都创建一次新的对象，同一个 HTTP 请求处理过程中使用的是同一个对象，不同的 HTTP 请求使用的是不同的对象
- session：同一个 session 中使用的是同一个对象，不同的 session 使用的是不同的对象
- global session ：不知道什么玩意，不太重要



## 2、Spring 事务的 5 种隔离级别

Spring 事务的隔离级别 跟 数据库事务 的隔离级别基本一致，不过多了一种 隔离级别：

-  ISOLATION_DEFAULT   ：使用后台数据库的隔离级别
-  ISOLATION_READ_UNCOMMITTED   ：读未提交
-  ISOLATION_READ_COMMITTED   ：读已提交
-  ISOLATION_REPEATABLE_READ   ：可重复读
-  ISOLATION_SERIALIZABLE   ：串行化



## 3、Spring 的 7 种事务传播机制



### 3.1、什么是事务的传播？

事务的传播指多个方法间嵌套调用，事务如何在这些方法中进行传播

这是概念性的讲解，以下举例子来通俗讲解：

```
方法 A 调用了 方法 B，即 方法 A 嵌套调用了 方法 B，那么方法 B 有无事务 和 方法 B 对事务的不同要求都会直接影响到 方法 A
比如方法 A 需要事务，方法 B 也需要事务，如果 方法 A 和 方法 B 是处在同一个事务中的，那么当 方法 B 发生异常了，那么 方法 A 和 方法 B 都需要回滚
比如方法 A 需要事务，方法 B 需要事务，不过 方法 A 和 方法 B 不在同一个事务中，那么当 方法 B 发生异常了，那么 方法 A 正常执行，方法 B 需要回滚
```

通过以上例子我们可以很显然的得知，嵌套调用的方法之间的事务影响着彼此

方法之间是否存在影响，以及这种影响的程度 就是 由 指定的事务传播机制来决定的 



### 3.2、7 种 事务传播机制



[Spring 的事务传播机制详解（一）](https://blog.51cto.com/jaeger/1761660 )

[Spring 的事务传播机制详解（二）](https://blog.51cto.com/jaeger/1761851 )

[Spring 的事务传播机制详解（三）](https://blog.51cto.com/jaeger/1762039 )



以 A 的事务类型为 REQUIRED，探讨 B 各种事务类型下的产生的不同情况：

- REQUIRED：A 有事务，那么 A 和 B 共用一个事务；A 没有事务，那么 B 自己创建一个事务（Spring 默认）
- REQUIRED_NEW：无论 A 是否有事务，B 都自己创建一个事务
- SUPPORTS：A 有事务，那么 A 和 B 共用一个事务；A 没有事务，那么 B 也以非事务形式执行
- NOT_SUPPORTS：B 不支持事务，如果 A 有事务，那么 A 事务挂起，执行完 B 后再继续 A 的事务，但如果 B 抛异常，那么 A 会回滚
- MANDATORY：B 和 A 共用一个事务，如果 A 没有事务，那么 B 抛出异常，但是 A 不会回滚，因为 A 没有事务
- NEVER：A 和 B 都不能有事务，如果 A 有事务，那么 B 抛异常
- NESTED：如果 A 有事务，那么 B 创建一个嵌套子事务，如果 A 发生异常，那么 A 和 B 都回滚，如果 B 异常，但是在 A 中捕获了异常，那么 A 正常提交， B 回滚；如果 A 没有事务，那么 B 跟 REQUIRED 一样创建一个事务



在详细举例子讲解这些事务传播机制之前，需要先知道：

- 当某个事务在某个方法中发生异常，而该方法没有捕捉，即使在上层方法捕捉了，那么也意味着该事务发生了异常，那么事务会回滚
- 当某个事物在某个方法中发生异常，只要该方法进行了捕捉，那么事务不会回滚
- 两个不同的事务不会影响彼此，即使一个事务发生异常，也不会影响到其他的事务

因此我们分析 事务传播机制 的时候，判断事务是否回滚就按照上面的来



#### 3.2.1、REQUIRED

以下 A 和 B 都是 REQUIRED，表示 A 和 B 共用一个事务

B 抛出异常，无论 A 是否捕获异常，A 和 B 都会进行回滚，因为它们在同一个事务中，B 存在异常，表示当前事务存在异常，需要将整个事务进行回滚，所以 A 的也会被回滚

```java
/*
A
*/
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        try {
            bService.B();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(1);
    }
}
/*
B
*/
@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，B 为 REQUIRED，那么 B 单独创建一个事务

B 抛出异常，由于 B 有事务，所以 B 会回滚，A 没有事务，所以提交成功

```java
/*
A
*/
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        try {
            bService.B();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(1);
    }
}
/*
B
*/
@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



#### 3.2.2、REQUIRED_NEW

A 为 REQUIRED， B 为 REQUIRES_NEW，那么 A 和 B 为不同的事务

B 抛异常，如果 A 没有捕捉，那么表示 A 和 B 两个事务全部发生异常，两个都需要回滚，如果 A 捕捉了，那么表示在 事务 A 中没有出现异常，那么 A 正常提交， B 回滚（同样的，如果 B 捕捉了异常，那么表示 事务 B 没有发生异常，那么 A 和 B 都会提交）

```java
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        // bService.B();
        try {
            bService.B();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(1);
    }
}

@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，B 为 REQUIRES_NEW 的情况也没啥好说的，B 无论是否存在异常都不会影响到 A 



#### 3.2.3、SUPPORTS

A 为 REQUIRED，B 为 SUPPORTS，那么 A 和 B 共用一个事务

B 抛异常，表示 事务发生了异常，无论 A 是否捕捉，事务都会回滚，所以 A 和 B 都会回滚

```java
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        bService.B();
        System.out.println(1);
    }
}

@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.SUPPORTS)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，B 为 SUPPORTS，那么 A 和 B 都以非事务形式执行，这个没什么好讲的



#### 3.2.4、NOT_SUPPORTS

A 为 REQUIRED，B 为 NOT_SUPPORTS，表示 A 无论是否有事务，B 都没有事务

B 抛异常，由于 B 没有事务，所以正常提交，由于 A 有事务，如果 A 捕捉了异常，那么 A 正常提交，如果没有捕捉，那么 A 会回滚

```java
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        bService.B();
        System.out.println(1);
    }
}

@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.NOT_SUPPORTS)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，那么 B 也没有事务，那么没什么好讲的



#### 3.2.5、MANDATORY

A 为 REQUIRED，B 为 MANDATORY，A 和 B 共用一个事务

B 抛异常，无论 A 是否捕捉，A 和 B 都会回滚

```java
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        bService.B();
        System.out.println(1);
    }
}

@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，B 为 MANDATORY，那么 B 会抛异常，A 会正常提交



#### 3.2.6、NEVER

A 为 REQUIRED，B 为 NEVER，A 和 B 都不能有事务

由于 A 有事务，所以 B 会抛异常，如果 A 捕捉了异常，那么 A 正常提交，如果 A 没有捕捉异常，那么 A 回滚

```java
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        bService.B();
        System.out.println(1);
    }
}

@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，B 为 NEVER，表示 A 和 B 都没有事务，那没什么好讲的



#### 3.2.7、NESTED

A 为 REQUIRED，B 为 NESTED，由于 A 有事务，所以 B 会创建一个嵌套子事务，但实际上它们还是在一个事务中

当 B 抛出异常，如果 A 没有捕捉，那么 A 回滚，如果 A 捕捉，那么 A 正常提交

当 A 抛出异常，那么 A 和 B 都会回滚，即 父事务 A 能够影响到 子事务 B，意味着 A 和 B 在同一个事务中



NESTED 和 REQUIRED 虽然 A 和 B 都是在一个事务中，但是区别在于：在 REQUIRED 状态下，B 发生异常时无论 A 是否捕捉，A  和 B 都会回滚，因为它们真真切切的在一个事务中；在 NESTED 状态下，A 捕捉了异常就不会回滚，**内部使用了 savepoint 机制**

```java
@Service
public class AService {
    @Resource
    UserMapper userMapper;
    @Autowired
    BService bService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void A() {
        Te te = new Te(6, "1", "1");
        userMapper.insertTe(te);
        bService.B();
        System.out.println(1);
    }
}

@Service
class BService {
    @Resource
    UserMapper userMapper;

    @Transactional(propagation = Propagation.NESTED)
    public void B() {
        Te te = new Te(7, "2", "2");
        userMapper.insertTe(te);
        int i = 1 / 0;
        System.out.println(2);
    }
}
```



A 没有事务，B 有事务，那么 B 就跟 REQUIRED 一样单独创建一个事务，没什么好讲的