# 数据库连接池



## 1、Connection 和 SqlSession 的关系

在 JDBC 中，数据库的每一个连接就是一个 Connection，Connection 封装了 数据库隔离级别、事务是否自动提交 等信息，Connection 就类似 TCP 长连接，一个 TCP 长连接可以传输 多个请求响应，同样的一个 Connection 上可以执行多条 sql 语句

在 Mybatis 中，数据库的每一个连接被封装成了 SqlSession，即使用这个 SqlSession 来代替 JDBC 的 Connection



我们需要知道，我们执行 sql 语句是需要先跟 数据库 建立连接的，**使用的是 socket 通信，默认是 TCP 连接**，sql 语句的执行是借助 Connection 和 SqlSession 实现的

因此，高并发情况下我们需要 避免 频繁的 连接和断开 TCP 连接，因为 三次握手 和 四次握手 和 TIME_WAIT 的效率太低

因此，我们需要跟 线程池 一样，使用一个 数据库连接池 来管理 数据库连接，让建立好的数据库连接可以进行复用



## 2、数据库连接池原理

我们这里讲的是 JDBC 的连接池，实际上 Mybatis 的也是类似的原理



数据库连接池已经存储了多条连接

![img](https://pic3.zhimg.com/80/v2-1bf937561557abec02568f6ab5741eb6_720w.jpg)

需要执行 sql 的时候，获取其中一条连接

![img](https://pic4.zhimg.com/80/v2-e0e4e2929aa128b58bc77670ace95fd7_720w.jpg)

当 sql 执行完毕后，不会直接断开连接，连接会回到数据库连接池中复用



> ### 以下是代码 demo

数据库连接池类

```java
//数据库连接池
public class MyDataSource implements DataSource {
    //链表 --- 实现栈结构
    private LinkedList<Connection> pool = new LinkedList<Connection>();

    //初始化连接数量
    public MyDataSource() {
        init();
    }
    
    prvate void init(){
        //一次性建立 10 条 Connection 放入到链表队列中
        for(int i = 0; i < 10; i++) {
            try {
                //1、装载sqlserver驱动对象
                DriverManager.registerDriver(new SQLServerDriver());
                //2、通过 JDBC 建立数据库连接，
                Connection con = DriverManager.getConnection(
                    "jdbc:sqlserver://192.168.2.6:1433;DatabaseName=customer", "sa", "123");
                //3、将连接加入连接池中
                pool.add(con);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        //取出连接池中一个连接
        final Connection conn = pool.removeFirst(); // 删除第一个连接返回
        return conn;
    }

    //将连接放回连接池
    public void releaseConnection(Connection conn) {
        pool.add(conn);
    }
}
```



当需要执行 sql 语句的时候，从 数据库连接池中获取连接 connection，当执行完毕完将 connection 放回连接池中

```java
MyDataSource pool = new MyDataSource();

//查询所有用户
public void queryUsers(){
    //1、使用连接池建立数据库连接
    Connection conn = pool.getConnection();        
    //2、创建状态
    Statement state = con.createStatement();           
    //3、查询数据库并返回结果
    ResultSet result = state.executeQuery("select * from user");           
    //4、输出查询结果
    while(result.next()){
        System.out.println(result.getString("email"));
    }            
    //5、断开数据库连接
    result.close();
    state.close();
    //6、归还数据库连接给连接池
    pool.releaseConnection(conn);
}
```



## 3、数据库连接池的 Connection 问题

[Connection 问题](<https://blog.csdn.net/xwq911/article/details/49150043>)

[ThreadLocal 解决事务问题](<https://blog.csdn.net/java_raylu/article/details/73729162>)



> ### 1、Connection 是线程安全的吗？

显然不是，如果线程 A 开启了事务，多条 sql 只执行了一部分，但是线程 B 获取了这个 Connection， 而且还执行了 commit()，这就有问题了，因为一个 Connection 控制着 事务信息 和 sql 数据信息

因此解决方法有二：

- 使用 sync 锁将 获取的 Connection 锁住
- 每次获取这个 Connection 时，将它从 数据库连接池中弹出，等到用完后，将它返回数据库连接池中

现在我们使用的数据库连接池默认就是这种操作



> ### 2、如何保证线程开启的事务的多条 sql 是同个 Connection 执行？



sql 的执行分为两种情况：非事务 和 事务

如果是非事务，那么 sql 正常执行即可，不要求上下两条 sql 语句的执行获取的是同一个 Connection，因为这不会出现什么问题

如果是事务，一个事务中可能存在多条 sql 语句的执行，比如我们调用的方法中 需要调用两个不同的方法进行两次 select，如果在这两个方法中使用的是不同的 Connection，那么 回滚操作就有点麻烦了，如何保证在这两个方法中使用的是同一个 Connection？**通过 ThreadLocal 进行事务绑定**

```java
package com.shop.Utils;
 
import java.sql.Connection;
import java.sql.SQLException;
 
import javax.sql.DataSource;
 
import com.mchange.v2.c3p0.ComboPooledDataSource;
 
public class DataSourceUtils {
    //数据库连接池
	private static DataSource dataSource = new ComboPooledDataSource();
    //将当前线程 和 Connection 进行绑定，通过 ThreadLocal 可以获取当前线程的 Connection
	private static ThreadLocal<Connection> tl = new ThreadLocal<>();
	
	//将Connection绑定到当前线程
	public static Connection getConnection() throws SQLException{
        //获取绑定的连接
		Connection conn = tl.get();
        //如果之前没有绑定，那么获取一个连接，然后进行绑定
		if(conn==null){
				conn = dataSource.getConnection();
			tl.set(conn);
		}
		return conn;
	}
	
	public static DataSource getDataSource(){
		return dataSource;
	}
	//开启事务
	public static void startTransaction() throws SQLException{
        /*
        调用 getConnection() 获取连接，在该方法内部会将 当前线程 和 连接 进行绑定
        */
		Connection conn = getConnection();
		if(conn!=null)
			conn.setAutoCommit(false); //开启事务
	}
	//回滚事务
	public static void rollback() throws SQLException{
		Connection conn = getConnection();
		if(conn!=null)
			conn.rollback();
	}
	//提交事务并释放资源
	public static void commitAndRelease() throws SQLException{
		Connection conn = getConnection();
		if(conn!=null){
            //提交事务
			conn.commit();
            //将连接放回数据库连接池
            dataSource.add(conn);
            //移除绑定关系
			tl.remove();
		}
	}
	//关闭资源方法：conn, rs, stat
}
```

