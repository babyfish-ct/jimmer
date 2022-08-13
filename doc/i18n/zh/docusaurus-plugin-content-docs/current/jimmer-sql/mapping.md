---
sidebar_position: 2
title: 映射
---

## 注解

jimmer-sql用JPA的注解的部分子集进行实体和数据库之间的映射，到目前为止。支持的注解有

- javax.persistance.Entity
- javax.persistance.Table
- javax.persistance.SequenceGenerator
- javax.persistance.Transient
- javax.persistance.Id
- javax.persistance.GeneratedValue
- javax.persistance.Version
- javax.persistance.Column
- javax.persistance.JoinColumn
- javax.persistance.JoinColumns
- javax.persistance.JoinTable
- javax.persistance.OneToOne
- javax.persistance.ManyToOne
- javax.persistance.OneToMany
- javax.persistance.ManyToMany

:::info
注意

1. 虽然jimmer-sql使用了一些JPA注解，但是jimmer-sql不是JPA实现，它有自己的API。jimmer-sql和JPA没有任何关系。

2. 这些注解的使用方式和JPA有细微差异，本文将详细讨论这些差异。

3. 对于@OneToOne、@ManyToOne、@OneToMany以及@ManyToMany这四个关联性注解，**无需配置targetEntity属性**。
   事实上，jimmer-sql也不关心此属性被如何配置，因为jimmer-sql要求严格地使用范型。

4. 对于@OneToOne、@ManyToOne、@OneToMany以及@ManyToMany这四个关联性注解，**无需配置fetch属性**。
   事实上，jimmer-sql也不关心此属性被如何配置，因为jimmer-sql在这个方面有更便捷的解决方案。

   请查看[对象抓取器](./query/fetcher)以了解更多。

5. 对于@OneToOne、@ManyToOne、@OneToMany以及@ManyToMany这四个关联性注解，**无需配置cascade属性**。
   事实上，jimmer-sql也不关心此属性被如何配置，因为jimmer-sql在这个方面有更便捷的解决方案。

   请查看[Save指令](./mutation/save-command)以了解更多。
:::

jimmer-sql本身提供的注解如下

- org.babyfish.jimmer.sql.Key
- org.babyfish.jimmer.sql.OnDelete

## javax.persistance.Entity

`Entity`注解用于修饰不可变接口，表示一个ORM实体。

:::note
1. 一旦使用了`@Entity`, 就隐含了该接口是不可变接口，无需再使用`@org.babyfish.jimmer.Immutable`了。

2. 然而，`@Immutable`可以指定各属性可空性的默认值，这是一个很重要的功能，所以，`@Entity`和`@Immutable`可以混合使用。
:::

## javax.persistence.Table

默认情况下，可以不使用`@Table`注解，由接口名推导表名。

默认表名全部使用大写字母，原名称每处从小写字母切换为大写字母的地方，都会自动添加下划线。比如，接口名`BookStore`会自动推导出表名`BOOK_STORE`。

如果默认表名无满足你的要求，请使用`@Tabale`指定你的表名。

```java title="BookStore.java"
@Entity
@Table("MY_BOOK_STORE")
public interface BookStore {
    ...
}
```

:::note
截止目前为止，仅解析`@Table`注解的`name`属性。
:::

## javax.persistance.SequenceGenerator

该注解在jimmer-sql中很少使用，即便使用了，也需要注意一下事项。

:::caution
1. jimmer-sql仅解析其`name`和`sequenceName`属性，而不会解析其它属性，尤其是`initialValue`和`allocationSize`。

2. 如果想采用分库技术，并保证不同数据库中同一张表的id唯一性，更推荐采用雪花id。
:::

## javax.persistance.Transient

表示某个属性和持久化无关，和JPA原本的用法无异。

## javax.persistance.Id

声明某个属性是id属性，如下

```java title="Book.java"
@Entity
public interface Book {

    // highlight-next-line
    @Id
    long id();
}
```

:::note
1. 和JPA中鼓励用户把id声明为装箱类型(如`java.lang.Long`)完全相反；jimmer-sql中基于8种基本类型的id必须被声明为原生类型（如long）。原因在于jimmer不可变对象具备动态性，不需要以id是否为null作为判断依据在insert和update操作之间抉择。

2. 目前，jimmer-sql暂不支持Composite id或多个id属性。
:::

默认情况下，id列的列名就是由Java属性名推导。默认列名全部使用大写字母，原名称每处从小写字母切换为大写字母的地方，都会自动添加下划线。这里对应的数据库列名就是ID。

如果默认的列名不是你的期望，可以配合@Column注解
```java title="Book.java(Error version)"
public interface Book {

    @Id
    @Colmun(name = "BOOK_ID")
    long id();
}
```

:::danger
这段代码会导致annotation processor报错。因为`@Id`表示属性非null，而`@Column`的`nullable`属性默认为true，二者彼此矛盾。Annotation processor会对此作非常严格的检查，报错如下

Illegal property "org.babyfish.jimmer.sql.example.model.Book.id", its nullity is conflict because it is marked by both @javax.persistence.Id and @javax.persistence.Column(nullable=true)
:::

为了修复问题，请修改代码，如下
```java title="Book.java"
public interface Book {

    @Id
    @Colmun(name = "BOOK_ID", nullable = false)
    long id();
}
```

## javax.persistance.GeneratedValue

JPA规范中，`@GeneratedValue`的`strategy`属性有四种策略，它们在jimmer-sql中被如此解析

### 1. AUTO *(或默认)*

此时，注解的`generator`属性必须被指定一个类名，该类必须实现`org.babyfish.jimmer.sql.meta.IdGenerator`接口。

`IdGenerator`告诉jimmer-sql如何对没有id属性的对象进行insert，其定义如下

```java title="IdGenerator"
package org.babyfish.jimmer.sql.meta;
public interface IdGenerator {}
```

`IdGenerator`接口有一个典型的实现，UserIdGenerator。即，由用户编写代码决定id如何自动生成。
```java title="UserIdGenerator.java"
package org.babyfish.jimmer.sql.meta;

public interface UserIdGenerator extends IdGenerator {

    Object generate(Class<?> entityType);
}
```

特别地，当id是UUID类型，jimmer-sql提供了一个叫做`org.babyfish.jimmer.sql.meta.UUIDIdGenerator`的类，用于随机生成UUID

使用如下
```java
@Entity
public interface Book {

    @Id
    @GeneratedValue(generate = "org.babyfish.jimmer.sql.meta.UUIDIdGenerator")
    UUID id();
}
```

如果要使用雪花id，需要用户自己实现`IdGenerator`接口，使用如下

```java
public SnowflakeIdGenerator implements UserIdGenerator {
    @Override
    public Long generator() {
        请调用某些第三方库生成64位的雪花id
    }
}

@Entity
public interface Book {

    @Id
    @GeneratedValue(generate = "yourpackage.SnowflakeIdGenerator")
    UUID id();
}
```

### 2. IDENTITY

使用数据库的自动编号

```java
@Entity
public interface Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();
}
```

### 3. SEQUENCE

使用数据库的序列。

第一种配置方法如下。这种写法保留了JPA的使用习惯，但相对繁琐。

```java
@Entity
@SequenceGenerator(
    name = "bookIdSequence" 
    sequenceName = "BOOK_ID_SEQ"
)
public interface Book {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "bookIdSequence"
    )
    long id();
}
```

jimmer-sql提供一种简化配置策略，只要`@GeneratedValue`注解的`generator`以`sequence:`开头，就可以直接指定数据库中的sequence名称。

```java
@Entity
public interface Book {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        // highlight-next-line
        generator = "sequence:BOOK_ID_SEQ"
    )
    long id();
}
```

当然，也可以不指定`generator`，此时序列名称为`表名 + "_ID_SEQ"`

```java
@Entity
public interface Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    long id();
}
```

### 4. TABLE

:::danger
jimmier-sql不支持这种策略
:::

## javax.persistance.Version

指定实体的乐观锁版本属性

```java
@Entity
public interface Book {

    @Version
    int version();

    ...
}
```

:::note
注意：

1. version字段必须为int类型，`java.lang.Integer`也不行

2. 如果配套使用@Column注解，必须指定其nullable为false
:::

## javax.persistance.ManyToOne

有两种方法可以实现多对一关联，基于外键和基于中间表。

### 1. 基于外键
    
```java
@Entity
public interface Book {

    @ManyToOne
    BookStore store();

    ...
}
```

这里并没有配合使用`@JoinColumn`明确指定外键列名，就采用默认列名。

默认列名全部采用大写字母，Java属性名每处从小写字母切换为大写字母的地方，都会自动添加下划线，最后再追加后缀"_ID"。

此处，多对一属性`store`，会自动推导出外键列名STORE_ID。

也可以配合`@JoinColum`使用

```java
@Entity
public interface Book {

    @ManyToOne
    @JoinColumn(name = "BOOK_STORE_ID")
    BookStore store();

    ...
}
```

这个例子中
- `@ManyToOne`的`optional`属性默认为true
- `@JoinColumn`的`nullable`属性也默认为true

二者一致，并不矛盾。所以，`store`属性可空。

要定义非空的store属性，可这样做
```java
@Entity
public interface Book {

    @ManyToOne(optional = false)
    @JoinColumn(name = "BOOK_STORE_ID", nullable = false)
    BookStore store();

    ...
}
```

这个例子中
- `@ManyToOne`的`optional`属性被指定为false
- `@JoinColumn`的`nullable`属性也指定为false

二者一致，并不矛盾。所以，`store`属性非空。

但是如果二者配置不一致，比如
```java
@Entity
public interface Book {

    @ManyToOne
    @JoinColumn(name = "BOOK_STORE_ID", nullable = false)
    BookStore store();

    ...
}
```

:::caution
这种不一致的配置将会导致Annotation Processor报错

Illegal property "org.babyfish.jimmer.sql.model.Book.store", its nullity is conflict because it is marked by both @javax.persistence.JoinColumn(nullable=false) and @javax.persistence.ManyToOne(optional=true)
:::

:::note
到目前为止，jimmer-sql暂不支持一个属性使用多个`@JoinColumn`注解。
:::

### 2. 基于中间表

```java
@Entity
public interface Book {

    @ManyToOne
    @JoinTable
    BookStore store();

    ...
}
```

这里，并没有为`@JoinTable`指定任何属性，默认属性如下
- `name`: 己方表名 + "_" + 对方表名 + "_" + "_MAPPING"。
- `joinColumns`: 一个`@JoinColumn`对象，其列名为己方实体id的列名。
- `inverseJoinColumns`: 一个`@JoinColumn`对象，其列名为有属性名自动推断，全部采用大写字母，原名称每处从小写字母切换为大写字母的地方，都会自动添加下划线，最后再追加后缀"_ID"。

假设`Book.id`的列名为`BOOK_ID`, 那么上面的配置就等价于
```java
@Entity
public interface Book {

    @ManyToOne
    @JoinTable(
        name = "BOOK_STORE_MAPPING",
        joinColumns = @JoinColumn(name = "BOOK_ID")
        inverseJoinColumns = @JoinColumn(name = "STORE_ID")
    )
    BookStore store();

    ...
}
```

:::caution
基于中间表的多对一关联必须是可空的。如果@ManyToOne注解的optional为false，
```java
@Entity
public interface Book {

    @ManyToOne(optional = false)
    @JoinTable
    BookStore store();

    ...
}
```
会导致Annotation processor异常

Illegal property "org.babyfish.jimmer.sql.model.Book.store", it is marked by @javax.persistence.ManyToOne(optional=false), but it is considered as nullable because it's a many-to-one association base on middle table
:::

:::note
到目前为止，jimmer-sql暂不支持
1. 为`@JoinTable`的`joinColumns`或`inverseJoinColumns`属性指定多个`@JoinColumn`注解。
2. 为`@JoinColumn`指定`referencedColumnName`，因为它总是引用目标表的id。
:::

## javax.persistance.OneToOne

一对一关联只能作为多对一关联的镜像。也就是说，一对一关联必然意味着双向关联。

```java title="Address.java"
@Entity
public interface Adress {

    @ManyToOne
    Customer customer();
    ...
}
```

```java title="Customer.java"
@Entity
public interface Customer {

    // highlight-next-line
    @OneToOne(mappedBy = "customer")
    Address address();
    ...
}
```

:::caution
被`@OneToOne`修饰的属性总是可null的，将`@OneToOne`的`optional`属性指定为false，或使用其它任何非null验证注解，都会导致Annotation processor报错。
:::

## javax.persistance.OneToMany

和JPA不同，一对多关联只能作为多对一关联镜像。也就是说，一对多关联必然意味着双向关联。

```java title="Book.java"
@Entity
public interface Book {

    @ManyToOne
    BookStore store();
    ...
}
```

```java title="Customer.java"
@Entity
public interface Customer {

    // highlight-next-line
    @OneToMany(mappedBy = "store")
    List<Boo> books();
    ...
}
```

:::caution
集合属性总被视为非null，当然包括被`@OneToMany`修饰的属性在内，使用任何nullable的验证注解都会导致Annotation processor报错。

## javax.persistance.ManyToMany

:::caution
集合属性总被视为非null，当然包括被`@ManyToMany`修饰的属性在内，使用任何nullable的验证注解都会导致Annotation processor报错。
:::

多对多关联既然可以作为主动方，也可以为作为从动方。

### 1. 作为主动方
```java
@Entity
public interface Book {

    @ManyToMany
    @JoinTable
    List<Author> autors();

    ...
}
```

这里，并没有为`@JoinTable`指定任何属性，默认属性如下
- name: 己方表名 + "_" + 对方表名 + "_" + "_MAPPING"。
- joinColumns: 一个`@JoinColumn`对象，其列名为，己方实体id的列名。
- inverseJoinColumns: 一个`@JoinColumn`对象，其列名基于属性名自动推导。全部采用大写字母，原名称每处从小写字母切换为大写字母的地方，都会自动添加下划线，最后再追加后缀"_ID"。

假设`Book.id`的列名为`BOOK_ID`, 那么上面的配置就等价于
```java
@Entity
public interface Book {

    @ManyToMany
    @JoinTable(
        name = "BOOK_AUTHOR_MAPPING",
        joinColumns = @JoinColumn(name = "BOOK_ID")
        inverseColumns = @JoinColumn(name = "AUTHOR_ID")
    )
    List<Author> autors();

    ...
}
```
:::note
到目前为止，jimmer-sql暂不支持
1. 为`@JoinTable`的`joinColumns`或`inverseJoinColumns`属性指定多个`@JoinColumn`注解。
2. 为`@JoinColumn`指定`referencedColumnName`，因为它总是引用目标表的id。
:::

### 2. 作为从动方

```java
@Entity
public interface Author {

    @ManyToMany(mappedBy = "authors")
    List<Book> books();

    ...
}
```

## org.babyfish.jimmer.sql.Key

`@org.babyfish.jimmer.sql.Key`和`@javax.persistance.Id`类似，却又不同。

- @Id用于指定表的技术性主键。
- @Key用于指定表的业务性主键。

比如:

```java title="Book.java"
@Entity
public interface Book {

    @Id
    UUID id();

    @Key
    String name();

    @Key
    int edition();

    ...
}
```

- 从技术层面讲，Book具备一个主键，叫做id。

    技术性主键，往往存储一些和无业务意义的唯一性数据，比如自动编号，序列值，UUID，雪花id。但它足够简单，可以最大程度低简化表之间的连接，优化连接性能。

- 从业务层面讲，`name`和`edition`联合起来，唯一确定一本书籍。

    业务性主键：存储和和无业务意义的唯一性数据，而且往往多列联合联用。但它相对复杂，并不直接参与表之间的连接操作。

:::note

1. 在讲解使用[Sava指令](./mutation/save-command)保存数据的文档中，我们可以看到业务性主键发挥了非常重要的作用。

2. 除了像这个例子一样静态地使用`@Key`注解来指定业务主键外，也可以在代码中动态指定业务主键，请查看[Sava指令](./mutation/save-command)以了解更多。
:::

## org.babyfish.jimmer.sql.OnDelete

只能用在基于外键映射的多对一关系上，比如

```java
@Entity
public interface Book {

    @ManyToOne
    @OnDissociate(DissociateAction.SET_NULL)
    BookStore store();
    ...
}
```

DissociateAction具有3个选项，对应数据库的外键行为：

- NONE: 当前外键不支持删除动作，阻止其父对象被删除。
- SET_NULL: 当父对象被删除时，此外键自动清空。对应于SQL的`on delete set null`语句，只能用于可空外键。
- DELETE: 当父对象被删除时，当前对象也被自动删除。对应于SQL的`on delete cascade `语句。