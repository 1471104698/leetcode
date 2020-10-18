# Oop-Klass模型



**我们需要先知道，Java 有两种对象：实例对象 和 Class 对象**

**Class 对象是加载的最终产物**（注意不是类加载）



在普通 Java 程序员认知中，Java 的所有对象的起源就是 Object 类

但这是是在 Java 语言层面的，在 JVM 层面显然不是 Object 类表示，甚至 Object 类还需要被表示

在 JVM 层面使用 Oop-Klass模型 来表示 类和对象

![img](https://img-blog.csdn.net/20170615230126453?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvbGlueGRjbg==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)





## 1、OOP 体系

具体看 [Java的对象模型——Oop-Klass模型（一）](https://www.yrunz.com/archives/Java%E7%9A%84%E5%AF%B9%E8%B1%A1%E6%A8%A1%E5%9E%8B%E2%80%94%E2%80%94Oop-Klass%E6%A8%A1%E5%9E%8B%EF%BC%88%E4%B8%80%EF%BC%89)

[OOP 对象如何获取对象--源码分析](https://www.it610.com/article/1306048742218043392.htm) 





### 1、什么是 OOP

这里的 OOP 并不是 Object-oriented programming（面向对象编程），而是 **Ordinary object pointer（普通对象指针）**

**在堆中 每个 Java 对象 就是一个 OOP 对象，当我们 new 的时候，在 JVM 层面创建出来的就是一个 OOP 对象**



HotSpot 使用 C++ 写了 OOP 的一个类体系 用来表示 Java 的实例对象

```C++
// hotspot/src/share/vm/oops/oopsHierarchy.hpp
...
// Oop的继承体系
typedef class oopDesc*                            oop;
typedef class   instanceOopDesc*            instanceOop;
typedef class   arrayOopDesc*                    arrayOop;
typedef class     objArrayOopDesc*            objArrayOop;
typedef class     typeArrayOopDesc*            typeArrayOop;
...
```

![Oop继承体系](https://tva1.sinaimg.cn/large/006tNbRwgy1gb0siqioykj317s0po7wi.jpg)

从上面可以看出来，OOP 体系中最高的类就是 oopDesc，它是所有 OOP 体系中其他类的父类（类似 Java 中的 Object）

它的子类有 instanceOopDesc 和 arrayOopDesc

- instanceOopDesc 用来表示普通的 Java 对象类型

- arrayOopDesc 用来表示数组类型，它还有两个子类：objArrayOopDesc 和 typeArrayOopDesc
  - objArrayOopDesc 用来表示普通的对象的数组类型
  - typeArrayOopDesc 用来表示基本数据的数组类型

OOP 主要由两部分组成：对象头 和 对象体

 ![img](https://upload-images.jianshu.io/upload_images/9300974-57e8d1d5b73d75e8.png?imageMogr2/auto-orient/strip|imageView2/2/w/429/format/webp)





### 2、OOP 对象头

对象头主要有两个属性：_mark 和 _metadata

- _mark 记录的是对象的 hashCode、GC 年龄、拥有者线程 ID、锁状态（sync 锁利用的就是这里面的属性）
- _metadata 指向方向区中 Klass 的元数据，因此 也叫做元数据指针

```C++
// hotspot/src/share/vm/oops/oop.hpp
class oopDesc {
    
 private:
  // markOop 对象，用于存储对象的运行时记录信息，如哈希值、GC分代年龄、锁状态等
  volatile markOop  _mark;
    
  // Klass指针的联合体，指向当前对象所属的Klass对象
  union _metadata {
    // 未采用指针压缩技术时使用
    Klass*      _klass;
    // 采用指针压缩技术时使用
    narrowKlass _compressed_klass;
  } _metadata;
    
 //...
}
```



在对象头的 markOop 中，维护了一个 ObjectMonitor，就是用来记录 sync 重量级锁时的状态

```C++
class BasicLock;
class ObjectMonitor;	//
class JavaThread;

class markOopDesc: public oopDesc {
    //xxx
}
```

ObjectMonitor 结构如下：

```C++
ObjectMonitor() {
    _header       = NULL;//markOop对象头
    _count        = 0;
    _waiters      = 0,//等待线程数
    _recursions   = 0;//重入次数
    _object       = NULL;//监视器锁寄生的对象。锁不是平白出现的，而是寄托存储于对象中。
    _owner        = NULL;//占有锁的线程
    _WaitSet      = NULL;//处于wait状态的线程，会被加入到waitSet，比如调用 wait()；
    _WaitSetLock  = 0;
    _Responsible  = NULL;
    _succ         = NULL;
    _cxq          = NULL;
    FreeNext      = NULL;
    _EntryList    = NULL;//处于阻塞block 状态的线程，会被加入到entryList，比如在 sync 锁处竞争锁而阻塞；
    _SpinFreq     = 0;
    _SpinClock    = 0;
    OwnerIsThread = 0;
    _previous_owner_tid = 0;//监视器前一个拥有者线程的ID
}
```

monitor 对象维护了两个队列：WaitSet 和 EntryList

当在 sync 处竞争锁时，没有获取锁的会进入 EntryList，即进入阻塞状态

当获取到锁后，会将 monitor 中的 owner 设置为当前线程，然后将 count = 1，表示重入度为 1，

如果调用 wait()，那么就会将锁释放，即将 当前线程放入 WaitSet 中，然后将 owner 设置为 null，count 设置为 1，但是进入到 WaitSet 封装的节点会记录当前线程的重入度，等到获取线程的时候会重新将 count 复原



### 3、OOP 对象体

OOP 对象体中存储的是所有变量的数据，但不包括变量名、变量类型、变量访问修饰符等

而在 OOP 中提供了一些访问变量数据的方法，从 Klass 中接收变量在 OOP 对象中的 offset 或者 内存地址，然后获取对应的数据，方法如下：

```C++
// hotspot/src/share/vm/oops/oop.hpp
class oopDesc {
 ... 
  /*
  	返回成员属性的地址
  */
  void*     field_base(int offset)        const;
  // 如果成员是基础类型，则用特有的方法
  jbyte*    byte_field_addr(int offset)   const;
  jchar*    char_field_addr(int offset)   const;
  jboolean* bool_field_addr(int offset)   const;
  jint*     int_field_addr(int offset)    const;
  jshort*   short_field_addr(int offset)  const;
  jlong*    long_field_addr(int offset)   const;
  jfloat*   float_field_addr(int offset)  const;
  jdouble*  double_field_addr(int offset) const;
  Metadata** metadata_field_addr(int offset) const;
  // 同样是成员的地址获取方法，在GC时使用
  template <class T> T* obj_field_addr(int offset) const;   
 ...
  /*
  	instanceOop 获取和设置其成员属性的方法
  */
  oop obj_field(int offset) const;
  volatile oop obj_field_volatile(int offset) const;
  void obj_field_put(int offset, oop value);
  void obj_field_put_raw(int offset, oop value);
  void obj_field_put_volatile(int offset, oop value);
  // 如果成员时基础类型，则使用其特有的方法，这里只列出针对byte类型的方法
  jbyte byte_field(int offset) const;
  void byte_field_put(int offset, jbyte contents);
 ...
}
```



OOP 对象体中存储的是 非静态 基本类型的数据 和 引用的其他的 OOP 对象的内存地址，它不会存储 变量名、变量类型等

它用一段连续的内存空间来存储这些所有的数据，但是它自己不会去标记哪段内存地址是属于哪个变量的

因此，它提供了方法：使用 offset 来定位数据，而这个 offset 是由 Klass 提供的，Klass 存储了除变量数据之外的所有变量信息，包括在 OOP 对象体中的偏移量 offset





### 4、总结

我们知道了 JVM 底层表示 Java 对象跟 我们平常看到的 Java 层面表示的 Java 对象不同

在堆内存中的实际上是 OOP 对象，而 OOP 对象由 对象头 和 对象体组成



OOP 对象是一段连续的内存空间，对象头的尾地址紧靠着对象体的起始地址

发生 GC 就是因为找不到连续的内存空间来存储这个 OOP 对象



关于变量数据获取，上面也说了，OOP 对象体也是一片连续的内存空间，它存储的是基本数据类型的数值 和 引用的 OOP 对象的内存地址，而不会存储变量名、变量类型之类的，因此它通过别人指给它需要获取的变量数据的 offset

这个存储了 方法信息、类信息、变量信息 同时又给 OOP 对象指明变量位置的就是 Klass



## 2、Klass

具体看 [Java的对象模型——Oop-Klass模型（二）](https://www.yrunz.com/archives/Java%E7%9A%84%E5%AF%B9%E8%B1%A1%E6%A8%A1%E5%9E%8B%E2%80%94%E2%80%94Oop-Klass%E6%A8%A1%E5%9E%8B%EF%BC%88%E4%BA%8C%EF%BC%89)



### 1、什么是 Klass

Klass 的作用如下：

- 存储 Java 类的元信息（方法、类名、访问权限等）
- 利用虚方法表 实现 JVM 多态机制



在 C++ 中，每个对象都存在一个虚方法表，用来实现方法的动态绑定（比如实现父类引用方法指针指向子类重写的方法）

但是为了 HotSpot 又不想每个 Java 对象都创建一张 虚方法表，并且由于每个类的实例对象类的元数据信息都是相同的，因此设计出了 Klass 



跟 OOP 一样，Klass 也有自己的体系：

```c++
// hotspot/src/share/vm/oops/oopsHierarchy.hpp
...
class Klass;  // Klass继承体系的最高父类
class   InstanceKlass;  // 表示一个Java普通类，包含了一个类运行时的所有信息
class     InstanceMirrorKlass;  // 表示java.lang.Class
class     InstanceClassLoaderKlass; // 主要用于遍历ClassLoader继承体系
class     InstanceRefKlass;  // 表示java.lang.ref.Reference及其子类
class   ArrayKlass;  // 表示一个Java数组类
class     ObjArrayKlass;  // 普通对象的数组类
class     TypeArrayKlass;  // 基础类型的数组类
...
```

![Klass继承体系](https://tva1.sinaimg.cn/large/006tNbRwgy1gbhp5vm4rtj31gy0pinpe.jpg)

Klass 类跟 OOP 中的 oopDesc 一样，是最高的父类，类似 Object

Klass 有两个子类 InstanceKlass 和 ArrayKlass，用来区分了 普通的对象类型 和 数组类型

它们的子类就不说了，看上面的注释即可



### 2、类的元数据

Klass 主要作用之一就是保存 Java 类的元数据，作为 field 保存的，以下是 Klass 中定义的一些比较重要的 field：

```C++
// hotspot/src/share/vm/oops/klass.hpp
class Klass : public Metadata {
...
  // 类名，其中普通类名和数组类名略有不同
  // 普通类名如：java/lang/String，数组类名如：[Ljava/lang/String;
  Symbol*     _name;
  // 最后一个secondary supertype
  Klass*      _secondary_super_cache;
  // 保存所有secondary supertypes
  Array<Klass*>* _secondary_supers;
  // 保存所有primary supertypes的有序列表
  Klass*      _primary_supers[_primary_super_limit];
  // 当前类所属的java/lang/Class对象对应的oop，即堆中的 Class 对象的 OOP 对象
  oop       _java_mirror;
  // 当前类的直接父类
  Klass*      _super;
  // 第一个子类 (NULL if none); _subklass->next_sibling() 为下一个
  Klass*      _subklass;
  // 串联起当前类所有的子类
  Klass*      _next_sibling;
  // 串联起被同一个ClassLoader加载的所有类（包括当前类）
  Klass*      _next_link;
  // 对应用于加载当前类的java.lang.ClassLoader对象
  ClassLoaderData* _class_loader_data;
  // 提供访问当前类的限定符途径, 主要用于Class.getModifiers()方法.
  jint        _modifier_flags;
  // 访问限定符
  AccessFlags _access_flags;    
...
}
```

Klass 类定义的是一些基础字段，比如继承的父类、当前的子类、以及还有一个指向 _java_mirror 的指针，即堆中的 Class 对象



方法、变量之类的存储在 Klass 的子类中实现

InstanceKlass 类 用来表示 普通对象，它是 Klass 的一个子类，以下  InstanceKlass 所包含的部分重要 field：

```C++
// hotspot/src/share/vm/oops/instanceKlass.hpp
class InstanceKlass: public Klass {
    ...
        // 当前类的状态
        enum ClassState {
        allocated,  // 已分配
        loaded,  // 已加载，并添加到类的继承体系中
        linked,  // 链接/验证完成
        being_initialized,  // 正在初始化
        fully_initialized,  // 初始化完成
        initialization_error  // 初始化失败
    };
    //普通字段的内存大小
    int               _nonstatic_field_size;
    //静态字段的内存大小
    int               _static_field_size; 
    // 当前类的注解
    Annotations*    _annotations;
    // 当前类数组中持有的类型
    Klass*          _array_klasses;
    // 当前类的常量池
    ConstantPool* _constants;
    // 当前类的内部类信息
    Array<jushort>* _inner_classes;
    // 保存当前类的所有方法列表属性
    Array<Method*>* _methods;
    // 如果当前类实现了接口，则保存该接口的 default 方法（接口中方法有方法体那么都是 static 和 default 方法）
    Array<Method*>* _default_methods;
    // 保存当前类所有方法的位置信息
    Array<int>*     _method_ordering;
    // 保存当前类所有default方法在虚函数表中的位置信息
    Array<int>*     _default_vtable_indices;
    /*
  _fields 保存 当前类的 field 信息 （包括static field），数组结构为：
      f1: [access, name index, sig index, initial value index, low_offset, high_offset]
      f2: [access, name index, sig index, initial value index, low_offset, high_offset]
            ...
      fn: [access, name index, sig index, initial value index, low_offset, high_offset]
      [generic signature index]	泛型签名信息
      [generic signature index]
  		...
  */
    Array<u2>*      _fields;
    ...
}
```

 _methods 存储了所有的方法属性，它将每个方法实例化为一个 Method 对象进行存储

- 方法名
- 方法访问修饰符
- 方法参数
- **在虚方法表中的索引**
- 等等



_fields 存储了所有的变量属性，它将每个变量实例化为一个 Field 对象进行存储

- 变量名
- 变量类型
- 变量访问修饰符
- 当前变量 在 OOP 对象中的 起始偏移量 和 末尾偏移量 offset
- 等等



### 3、方法的 code 属性

方法的 Code 属性存储在 Method 中的 Attribute 属性中

 ![image](https://oscimg.oschina.net/oscnet/cddfec32980004cf5f411e6f7b2b2b5e1d6.jpg)

 

Code 属性存储的是方法的代码的执行逻辑

```C++
code_attribute_structure{
    attribute_name_index;//指向常量池中“code”的索引值 2个字节 u2
    attribute_length;//code属性的长度 nu4
    max_stack;//操作数栈的最大深度，用于分配栈帧的操作数栈深度的参考值 u2
    max_locals;//局部变量所需的存储空间 u2
    code_length;//机器指令的长度，其值是多少，就向后面数多少个字节表示机器指令 u4
    code;//跟在code_length后面code_length个字节的机器指令的具体的值（JVM最底层的机器指令）u1
    exception_talbe_length;//显示异常长度 u2
    exception_table;显示异常表  exception_info类型(长度为exception_table_length)
    attribute_count;//属性计数器，code属性中还包含有其他子属性的数目 u2
    attribute_info;//属性code的子属性，主要有lindeNumberTable,LocalVariableTable
}
```

转变为字节码指令看如下：

```java
    Code:
      stack=4, locals=2, args_size=1
         0: new           #2                  // class java/lang/StringBuilder
         3: dup
         4: invokespecial #3                  // Method java/lang/StringBuilder."<init>":()V
         7: new           #4                  // class java/lang/String
        10: dup
        11: ldc           #5                  // String he
        13: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        16: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        19: new           #4                  // class java/lang/String
        22: dup
        23: ldc           #8                  // String llo
        25: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        28: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        31: invokevirtual #9                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        34: astore_1
        35: getstatic     #10                 // Field java/lang/System.out:Ljava/io/PrintStream;
        38: aload_1
        39: invokevirtual #11                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        42: return

```





### 4、虚方法表

虚方法表是方法重写机制的实现，它只存储虚方法（非 private、static、final 方法）

在 C++ 中，虚方法表指向的方法的入口地址

而在 JVM 中，它指向的不是方法的入口地址，而是 Klass 中的 Method*，而 Method 里面有一个字段才是真正的方法的调用入口，即上面的 code

因此 JVM 比 C++ 多了一层，需要先定位到 Method*,再找到方法调用入口



vtable 实际上是一个 由多个 vtableEntry 组成的数组，每个 vtableEntry 存储的是对应方法的字节码地址

```C++
class vtable{
	vtableEntry[] vtableEntrys;
}
class vtableEntry{
    //方法字节码地址
	int* addr;
	//xxxx
}
```

 ![vtalbe结构](https://tva1.sinaimg.cn/large/006tNbRwgy1gbhwk4e0zbj30xm0k0b29.jpg)

  





### 5、总结



Klass 存储了一个类的所有信息，但是不会存储具体的数据，只是相当于把这个类的信息整合成一个模板

它里面 统计了各种变量的个数，以及需要的内存大小，规定了某个方法在虚方法表的某个位置，规定了某个变量的数据在 OOP对象中的内存偏移量 、 父类、子类信息 以及 堆中的用于反射的 Class 对象

由于代码是写死的，所以一个类的模板就是这个固定的 Klass，它是不会改变的

当 new 创建 OOP 对象的时候，就是参照这个 Klass 对象来进行内存分配的，因为 Klass 已经将类的信息全部统计了，那么需要的连续内存大小也都统计了

而由于每个 OOP 对象存储的内存地址每次都是不同的，因此 Klass 存储的是变量数据在 OOP 对象中的偏移量，只要保证创建出来的 OOP 对象是按照 Klass 模板来的，那么变量的相对位置也都是固定了的，因此可以直接通过偏移量获取



## 3、Class 对象

在 Klass 中有一个 _java_mirror 字段，这个字段指向的就是堆中 Class 对象的 OOP 对象

Klass 对象 和 Class 对象实际上是双向指向的，因此 Class 对象也可以访问 Klass 对象

这是一个关键点，反射就是通过获取 Class 对象来访问 Klass 对象获取类的元数据的

所以才说 Class 对象是反射的入口，即 Class 对象本身并没有存储什么类的信息

因为反射它需要知道 Klass 对象的地址，而 Class 对象正是用来 存储这个地址的一个载体而已





## 4、JVM 计算变量偏移量 的源码分析

具体看 [类变量加载源码分析]( https://blog.csdn.net/li1376417539/category_9390865.html ) 

 [JVM-如何保存-Java-对象](http://blog.zhangjikai.com/2019/09/08/%E3%80%90Java%E3%80%91-JVM-%E5%A6%82%E4%BD%95%E4%BF%9D%E5%AD%98-Java-%E5%AF%B9%E8%B1%A1/) 



JVM 类加载是将 class 文件的各部分结构转换为 JVM 内存数据结构，这里是将变量转换为 Field，计算好每个变量的偏移量

调用 parse_fields() 进行计算，方法如下

```C++
typeArrayHandle ClassFileParser::parse_fields(Symbol* class_name,
                                              constantPoolHandle cp, bool is_interface,
                                              FieldAllocationCount *fac,
                                              objArrayHandle* fields_annotations,
                                              u2* java_fields_count_ptr, TRAPS) {
    //二进制数流
    ClassFileStream* cfs = stream();
    typeArrayHandle nullHandle;
    // length，获取Java类域变量的数量
    cfs->guarantee_more(2, CHECK_(nullHandle));  
    u2 length = cfs->get_u2_fast();
    *java_fields_count_ptr = length;

    int num_injected = 0;
    /*
    这里使用 for 循环遍历所有的变量
    */
    for (int n = 0; n < length; n++) {
        cfs->guarantee_more(8, CHECK_(nullHandle));  
        
        //读取变量访问表示，如private|public等
        AccessFlags access_flags;
        
        jint flags = cfs->get_u2_fast() & JVM_RECOGNIZED_FIELD_MODIFIERS;
        verify_legal_field_modifiers(flags, is_interface, CHECK_(nullHandle));
        access_flags.set_flags(flags);
        
        //读取变量名称索引
        u2 name_index = cfs->get_u2_fast();
        
        int cp_size = cp->length();
        check_property(
            valid_cp_range(name_index, cp_size) && cp->tag_at(name_index).is_utf8(),
            "Invalid constant pool index %u for field name in class file %s",
            name_index, CHECK_(nullHandle));
        Symbol*  name = cp->symbol_at(name_index);
        verify_legal_field_name(name, CHECK_(nullHandle));
        
        //读取类型索引
        u2 signature_index = cfs->get_u2_fast();
        
        check_property(
            valid_cp_range(signature_index, cp_size) &&
            cp->tag_at(signature_index).is_utf8(),
            "Invalid constant pool index %u for field signature in class file %s",
            signature_index, CHECK_(nullHandle));

        u2 constantvalue_index = 0;
        bool is_synthetic = false;
        u2 generic_signature_index = 0;
        bool is_static = access_flags.is_static();
        
        //读取变量属性
        u2 attributes_count = cfs->get_u2_fast();
        if (attributes_count > 0) {
            parse_field_attributes(cp, attributes_count, is_static, signature_index,
                                   &constantvalue_index, &is_synthetic,
                                   &generic_signature_index, &field_annotations,
                                   CHECK_(nullHandle));
            if (field_annotations.not_null()) {
                if (fields_annotations->is_null()) {
                    objArrayOop md = oopFactory::new_system_objArray(length, CHECK_(nullHandle));
                    *fields_annotations = objArrayHandle(THREAD, md);
                }
                (*fields_annotations)->obj_at_put(n, field_annotations());
            }
        }
        //判断变量属性
        BasicType type = cp->basic_type_for_signature_at(signature_index);

        //xxxxxxx 其他代码
    }


    return fields;
}
```

从源码可以看出 JVM 解析变量的逻辑：

- 获取当前类中所有 成员变量的个数
- 获取变量的访问修饰符
- 获取变量名
- 获取变量类型
- 统计各个类型的变量的个数（静态变量、非静态变量）



后面就是**计算偏移量**

计算 静态变量（基本数据类型和 OOP对象）和 非静态类型（基本数据类型和 OOP对象） 的总偏移量

```C++
instanceKlassHandle ClassFileParser::parseClassFile(Symbol* name,
                                                    Handle class_loader,
                                                    Handle protection_domain,
                                                    KlassHandle host_klass,
                                                    GrowableArray* cp_patches,
                                                    TempNewSymbol& parsed_name,
                                                    bool verify,
                                                    TRAPS) {
    //省略部分代码
 // Field size and offset computation
    int nonstatic_field_size = super_klass() == NULL ? 0 : super_klass->nonstatic_field_size();
#ifndef PRODUCT
    int orig_nonstatic_field_size = 0;
#endif
    int static_field_size = 0;
    //静态 OOP 对象的偏移量
    int next_static_oop_offset;
    //静态基本数据类型的偏移量
    int next_static_double_offset;
    int next_static_word_offset;
    int next_static_short_offset;
    int next_static_byte_offset;
    int next_static_type_offset;
    //非静态 OOP 对象的偏移量
    int next_nonstatic_oop_offset;
    //非静态基本数据类型的偏移量
    int next_nonstatic_double_offset;
    int next_nonstatic_word_offset;
    int next_nonstatic_short_offset;
    int next_nonstatic_byte_offset;
    int next_nonstatic_type_offset;
    //第一个非静态 OOP 对象的偏移量
    int first_nonstatic_oop_offset;
    //第一个非静态基本数据类型的偏移量
    int first_nonstatic_field_offset;
    int next_nonstatic_field_offset;

    // Calculate the starting byte offsets
    next_static_oop_offset      = instanceMirrorKlass::offset_of_static_fields();
    next_static_double_offset   = next_static_oop_offset +
                                  (fac.count[STATIC_OOP] * heapOopSize);
    if ( fac.count[STATIC_DOUBLE] &&
         (Universe::field_type_should_be_aligned(T_DOUBLE) ||
          Universe::field_type_should_be_aligned(T_LONG)) ) {
      next_static_double_offset = align_size_up(next_static_double_offset, BytesPerLong);
    }

    next_static_word_offset     = next_static_double_offset +
                                  (fac.count[STATIC_DOUBLE] * BytesPerLong);
    next_static_short_offset    = next_static_word_offset +
                                  (fac.count[STATIC_WORD] * BytesPerInt);
    next_static_byte_offset     = next_static_short_offset +
                                  (fac.count[STATIC_SHORT] * BytesPerShort);
    next_static_type_offset     = align_size_up((next_static_byte_offset +
                                  fac.count[STATIC_BYTE] ), wordSize );
    static_field_size           = (next_static_type_offset -
                                  next_static_oop_offset) / wordSize;

    first_nonstatic_field_offset = instanceOopDesc::base_offset_in_bytes() +
                                   nonstatic_field_size * heapOopSize;
    next_nonstatic_field_offset = first_nonstatic_field_offset;
    
    //省略部分代码 
```

当总偏移量计算完后，它会分别计算每一个变量的偏移量 offset



当计算好每个字段后，在 JDK 1.7 中，静态变量是存储在 Class 对象中的，因此 程序员访问静态变量的步骤为：

- 通过 OOP 对象头的 元数据指针找到 方法区中的 Klass
- 通过 Klass 中的 _java_mirror  指针 和 对应 field 的偏移量 找到堆中的 Class 对象并计算偏移量获取静态变量数据



由于静态变量不能继承，所以不会去计算父类的静态变量

而非静态变量是可以继承的，无论子类是否重写了父类的字段，在子类 OOP 对象头后面紧跟着的都是父类的非静态字段

当子类重写了父类的字段，那么会先拷贝父类的所有非静态字段，然后再在后面放重写的字段

 ![img](http://blog.zhangjikai.com/images/jvm-java/instance.png) 



> ### 类的 private 字段会被子类继承吗？

答案是会的，子类的 OOP 对象头后面会存储父类的所有非静态字段，无论是 private 还是public 还是 final（不是 static final），只是对于 private 字段 子类没有访问权限而已，即存在是存在，但是没有权限访问，相当于是占了内存但是不干事那种