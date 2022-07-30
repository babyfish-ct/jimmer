---
sidebar_position: 6
title: 表达式
---

## 字面量表达式

先来看个例子 *（这个查询没有实际业务意义，仅为演示）*
```java
List<
    Tuple5<
        String,
        Long,
        OffsetDateTime,
        String,
        Boolean
    >
> tuples = sqlClient
    .createQuery(BookTable.class, (q, store) -> {
        return q.select(
            Expression.string().value("String"),
            Expression.numeric().value(3L),
            Expression.comparable().value(OffsetDateTime.now()),
            Expression.any().value("String"),
            Expression.nullValue(Boolean.class)
        );
    }).execute();
tuples.forEach(System.out::println);
```

生成的SQL如下
```sql
select ?, ?, ?, ?, null from BOOK as tb_1_
```

除了null以外，其余各种类型的字面量都变成了JDBC参数。

:::caution
这段示范中，可以看出，`value()`方法接受了很多种类型的参数。

需要注意的是，无论`value()`方法参数类型是什么，都不能为null，否则将导致异常。

要为null创建字面量表达式，必须使用`nullValue()`方法，该方法需要指定表达式类型。
:::

:::note
这个例子中使用了类型引导方法：
1. Expression.string()，针对字符串类型。
2. Expression.numeric()，针对数字类型。
3. Expression.comparable()，针对可比较类型，即继承自java.lang.Comparable的类型。
4. Expression.any()，其它类型。

类型引导方法是针对java语言的不足而设计，并非当前我们的关注点，请查看本文末尾的[类型引导](#类型引导)了解更多。
:::

大部分情况下，开发人员无需手动创建字面量表达式。

以下文即将讨论的相等判断为例，下面这种相对繁琐的写法
```java
q.where(
    book.name().eq(
        Expression.string().value("Java in Action")
    )
);
```
可以用更便捷的写法代替
```java
q.where(book.name().eq("Java in Action"));
```

不难发现，其它API会提供重载版本，以避免让开发人员亲自构建字面量表达式。

但是，这无法100%做到。极少数情况下，仍然需要开发人员亲自构建字面量表达式。

## 常量表达式

常量表达式和字面量表达式高度类似，先看例子 *（这个查询没有实际业务意义，仅为演示）*
```java
List<Integer> constants = sqlClient
    .createQuery(BookTable.class, (q, store) -> {
        return q.select(
            // highlight-next-line
            Expression.constant(1)
        );
    }).execute();
constants.forEach(System.out::println);
```

生成的SQL如下

```sql
select 
    /* highlight-next-line */
    1 
from BOOK as tb_1_
```

不难看出，和字面量表达式总是使用JDBC参数不同，常量表达式直接把值硬编码进SQL语句。

为了杜绝注入方式攻击的问题，常量表达式只支持数字类型，这是一个硬性限制。

:::info
虽然有常量表达式只支持数字类型这个硬性限制，不用担心注入方式攻击问题，但实际项目仍需要严格限制其使用。

常量表达式之所以存在的唯一理由：某些数据库支持函数索引，如果定义函数索引的SQL表达式内部存在数字常量，这时，为了匹配这样的函数索引，常量表达式会非常有用。

如果你的项目没有这种场景，请永远不要使用常量表达式，而应该全部使用字面量表达式。

错误地使用常量表达式会带来严重的后果。错误地把善变的数字类型参数作为常量表达式植入SQL，将会破坏SQL字符串的稳定性，最终导致数据库内部执行计划缓存命中率极低，影响性能。
:::

## 比较

1. 等于
```java
q.where(book.name().eq("SQL in Action"));
```

2. 不等
```java
q.where(book.name().ne("SQL in Action"));
```

3. 大于
```java
q.where(book.price().gt(new BigDecimal(50)));
```

4. 大于或等于
```java
q.where(book.price().ge(new BigDecimal(50)));
```

5. 小于
```java
q.where(book.price().lt(new BigDecimal(50)));
```

6. 小于或等于
```java
q.where(book.price().le(new BigDecimal(50)));
```

7. between
```java
q.where(
    book.price().between(
        new BigDecimal(40), 
        new BigDecimal(40)
    )
);
```

## 模糊匹配

### 大小写

1. like: 大小写敏感
    ```java
    q.where(book.name().like("Ab"));
    ```
    最终生成的SQL条件
    ```sql
    where tb_1_.NAME like ?
    ```
    相应的JDBC参数: `%Ab%`

2. ilike: 大小写不敏感
    ```java
    q.where(book.name().ilike("Ab"));
    ```
    最终生成的SQL条件
    ```sql
    where lower(tb_1_.NAME) like ?
    ```
    相应的JDBC参数: `%ab%`

### 匹配模式

1. `LikeMode.ANYWHERE`（不指定时的默认行为）：出现在任何位置
    ```java
    q.where(book.name().like("Ab", LikeMode.ANYWHERE));
    ```
    或
    ```java
    q.where(book.name().like("Ab"));
    ```
    相应的JDBC参数: `'%Ab%'`

2. `LikeMode.START`：作为开头
    ```java
    q.where(book.name().like("Ab", LikeMode.START));
    ```
    相应的JDBC参数: `'Ab%'`

3. `LikeMode.END`：作为结尾
    ```java
    q.where(book.name().like("Ab", LikeMode.END));
    ```
    相应的JDBC参数: `'%Ab'`

4. `LikeMode.EXACT`：精确匹配
    ```java
    q.where(book.name().like("Ab", LikeMode.EXACT));
    ```
    相应的JDBC参数: `'Ab'`

## 空判断

```java
q.where(book.store().isNull());
```

```java
q.where(book.store().isNotNull());
```

## IN LIST

### 使用单值

```java
q.where(
    book.name().in(
        "SQL in Action",
        "Java in Action"
    )
);
```
生成的SQL条件
```sql
where tb_1_.NAME in (?, ?)
```

### 使用元组

使用元祖需要借助静态方法`org.babyfish.jimmer.sql.ast.Expression.tuple`。

```java
q.where(
    Expression.tuple(
        book.name(),
        book.edition()
    ).in(
        Arrays.asList(
            new Tuple2<>("SQL in Action", 1),
            new Tuple2<>("SQL in Action", 2),
            new Tuple2<>("Java in Action", 1),
            new Tuple2<>("Java in Action", 2)
        )
    )
);
```

生成的SQL条件
```sql
where (tb_1_.NAME, tb_1_.EDITION) in (
    (?, ?), (?, ?), (?, ?), (?, ?)
)
```

:::note
除了和数据集合配合使用外，in还可以可以子查询配合使用。

这部分内容会在[子查询](./query/sub-query)相关文档中详细介绍，本文不做重复介绍。
:::

## 与、或、非

### 与
```java
q.where(
    Predicate.and(
        book.name().like("Ab"),
        book.price().ge(new BigDecimal(40)),
        book.price().le(new BigDecimal(60))
    )
);
```
:::note
注意，如果逻辑与表达式直接作为where方法的参数，以下两种等价写法更值得推荐.
:::

1. 使用where方法的变参版本
    ```java
    q.where(
        book.name().like("Ab"),
        book.price().ge(new BigDecimal(40)),
        book.price().le(new BigDecimal(60))
    );
    ```

1. 多次调用where方法
    ```java
    q
        .where(book.name().like("Ab"))
        .where(book.price().ge(new BigDecimal(40)))
        .where(book.price().le(new BigDecimal(60)));
    ```

所以，直接使用`and`的写法在实际项目中应该不常见。

### 或

```java
q.where(
    Predicate.or(
        book.name().like("Ab"),
        book.price().ge(new BigDecimal(40)),
        book.price().le(new BigDecimal(60))
    );
);
```

### 非

```java
q.where(
    book.name().like("Ab").not()
);
```

并不总是需要调用`not()`函数。很多时候有快捷方式可用，比如
1. `.eq(value).not()`可以简写成`.ne(value)`
2. `.isNull().not()`可以简写成`.isNotNull(value)`
3. `.exists().not()`可以简写成`.notExists()` *(exists会在[子查询](./query/sub-query)中介绍，本文不会介绍)*

甚至，即便开发人员明确使用了not()，最终SQL也不一定会出现not，比如
```java
q.where(
    book.price().ge(new BigDecimal(40))
        .not()
);
```
实际生成的SQL却是
```sql
where tb_1_1.PRICE < ?
```
jimmer-sql尽量避免在SQL中直接使用not，但无论如何，最终SQL逻辑和你想要的逻辑等价。

## 数学运算

1. +
```java
q.select(book.price.plus(BigDecimal.TWO));
```
2. -
```java
q.select(book.price.minus(BigDecimal.TWO));
```
3. *
```java
q.select(book.price.times(BigDecimal.TWO));
```

4. /
```java
q.select(book.price.div(BigDecimal.TWO));
```

5. %
```java
q.select(book.price.rem(BigDecimal.TWO));
```

## 聚合函数

```java
List<
    Tuple6<
        Long, 
        Long, 
        BigDecimal, 
        BigDecimal, 
        BigDecimal, 
        BigDecimal
    >
> tuples = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q.select(
                book.count(),
                book.id().count(/* disintct */ true),
                book.price().sum(),
                book.price().min(),
                book.price().max(),
                book.price().avg()
        );
    }).execute();
tuples.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    count(tb_1_.ID), 
    count(distinct tb_1_.ID), 
    sum(tb_1_.PRICE), 
    min(tb_1_.PRICE), 
    max(tb_1_.PRICE), 
    avg(tb_1_.PRICE) 
from BOOK as tb_1_
```

## Coalesce

Coalesce语句指定一系列表达式，返回第一个非null值。

```java
List<String> results = sqlClient
    .createQuery(BookStoreTable.class, (q, store) -> {
        return q.select(
            store.website() // 1
                // highlight-next-line
                .coalesceBuilder()
                .or(store.name()) // 2
                .or("Default Value") // 3
                .build()
        );
    }).execute();
results.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    /* highlight-next-line */
    coalesce(
        tb_1_.WEBSITE, 
        tb_1_.NAME, 
        ?
    ) 
from BOOK_STORE as tb_1_
```

特别地，如果SQL的coalesce函数只有两个参数，即上述Java代码中`or()`方法只会被调用一次时，有一个快捷写法：

```java
List<String> results = sqlClient
    .createQuery(BookStoreTable.class, (q, store) -> {
        return q.select(
            store.website().coalesce(store.name())
        );
    }).execute();
results.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    /* highlight-next-line */
    coalesce(tb_1_.WEBSITE, tb_1_.NAME) 
from BOOK_STORE as tb_1_
```

其实，这种写法才是最常见的。

## Concat

Contact表达式用于字符串拼接。

这个例子以空格作为连接符，把作者的firstName和lastName拼起来

```java
List<String> fullNames = sqlClient
    .createQuery(AuthorTable.class, (q, author) -> {
        return q.select(
            author.firstName()
                .concat(
                        Expression.string().value(" "),
                        author.lastName()
                )
        );
    }).execute();
fullNames.forEach(System.out::println);
```

最终生成的SQL

```sql
select 
    concat(
        tb_1_.FIRST_NAME, 
        ?, 
        tb_1_.LAST_NAME
    ) 
from AUTHOR as tb_1_
```

## Case

case表达式分为两种，简单的case和普通的case

### 简单的case

简单的case语句，需要在语句开头处指定一个表达式，后续每一个判断分支指定一个期望值，检查指定的表达式是否和某个期望值匹配。

```java
List<String> results = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q.select(
            Expression.string()
                // highlight-next-line
                .caseBuilder(book.name())
                .when("Java in Action", "matched")
                .when("SQL in Action", "matched")
                .otherwise("not matched")
        );
    }).execute();
results.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    /* highlight-next-line */
    case tb_1_.NAME 
        when ? then ? 
        when ? then ? 
        else ? 
    end 
from BOOK as tb_1_
```

### 普通的case

普通的case语句，语句开头处不需要指定任何参数，但后续每一个判断分支都可以指定一个任意复杂度的逻辑判断表达式，检查是否有分支的逻辑判断成立。

```java
List<String> results = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q.select(
            Expression.string()
                // highlight-next-line
                .caseBuilder()
                .when(
                    book.price().lt(new BigDecimal(30)),
                    "Cheap"
                )
                .when(
                    book.price().gt(new BigDecimal(70)),
                    "Expensive"
                )
                .otherwise("Appropriate")
        );
    }).execute();
results.forEach(System.out::println);
```

最终生成的SQL如下
```sql
select 
    /* highlight-next-line */
    case 
        when tb_1_.PRICE < ? then ? 
        when tb_1_.PRICE > ? then ? 
        else ? 
    end 
from BOOK as tb_1_
```

## Native SQL表达式

NativeSQL表达式是一个重要的功能，数据库产品总会有特有的功能，需要吧数据产品特有的能力发挥出来。

这个例子，演示如何使用Oracle和HSQLDB的正则表达式匹配。

```java
List<Author> authors = sqlClient.
    .createQuery(AuthorTable.class, (q, author) -> {
        q.where(
            // highlight-next-line
            Predicate.sql(
                "regexp_like(%e, %v)",
                it -> it
                        .expression(author.firstName())
                        .value("^Ste(v|ph)en$")
            )
        );
    })
    .execute();
authors.forEach(System.out::println);
```

在这里，我们调用了`Predicate.sql`创建基于本地SQL的查询条件。事实上，其它表达式类型也支持NativeSQL表达式，`sql`函数共有5个

1. Predicate.sql(...)
2. Expression.string().sql(...)
3. Expression.numeric().sql(...)
4. Expression.comparable().sql(...)
5. Expression.any().sql(...)

`sql(...)`方法的第一个参数是SQL字符串模板，可以包含特殊符号"%e"和"%v"。

- %e: 即Expression，植入一个表达式
- %v: 即Value，植入一个值

`sql(...)`方法的第二个参数是一个可选的，是一个lambda表达式，lambda表达式的参数是一个对象，该对象支持两个方法：
- `expression(Expresion<?>)`: 植入一个表达式，和SQL模板中的"%e"匹配。
- `value(Object)`: 植入一个值，和SQL模板中的"%v"匹配。

最终生成的SQL是
```sql
select 
    tb_1_.ID, 
    tb_1_.FIRST_NAME, 
    tb_1_.LAST_NAME, 
    tb_1_.GENDER 
from AUTHOR as tb_1_ 
where
    /* highlight-next-line */
    regexp_like(tb_1_.FIRST_NAME, ?)
```

## 类型引导

上面的例子多次出现这些方法调用

1. Expression.string()，针对字符串类型。
2. Expression.numeric()，针对数字类型。
3. Expression.string()，针对可比较类型，即继承自java.lang.Comparable的类型。
4. Expression.any()，其它类型。

这些方法叫做类型引导方法。由于Java本身的缺陷，需要以轻微影响开发体验为代价，帮助DSL更好地进行类型推断。

:::info
本节讲解SQL DSL设计遇到的困难，以及这些类型引导方法出现的原因，和jimmer-sql要暴露的功能无关。对此不感兴趣的读者可以选择忽略。
:::

强类型SQL DSL需要定义一个重要的基础类型Expression，以表示任意类型的SQL表达式

```java
public interface Expression<T> {
    ...任何类型表达式都具备的公共行为，略...
}
```
很明显
- 对`Expression<String>`而言，我们期望支持like、ilike等操作
- 对`Expresion<? extends Number>`而言，我们期望支持plus, minus等操作

即，DSL期望`Expression<T>`类型暴露的行为能因范型参数的不同而不同。

这是一个非常经典的问题，很多语言都支持，大体上讲，有两种流派

1. 以C++为代表的流派，破坏类型的统一性

    ```cpp
    template <typename T> class Expression {
    public:
        ...Common behaviors...
    };

    template <> class Epxression<std::string> {
    public:
        ...Common behaviors...
        Predicate like(const char* pattern) const;
        Predicate ilike(const char* pattern) const;
    }

    template <> class Epxression<int> {
    public:
        ...Common behaviors...
        Expression<int> operator +(const Expression<int> &right) const;
        Expression<int> operator -(const Expression<int> &right) const;
    }
    ```

    C++这个特性叫模板特化。虽然它达到了设计目标，但是不同范型参数会导致类型定义不同，破坏了类型系统的统一性，所以对JVM这种靠统一类型系统支持反射的平台是不可接受的。我以C++举例只是想表明这其实是一个古老的需求。

2. 以kotlin为代表的流派，保持类型的统一性

    为了不破坏已经定义好的`Expression<T>`类型，kotlin可以使用扩展函数来实现同样的目的。

    ```kotlin
    infix fun Expression<String>.like(String pattern): Predicate { ... }
    infix fun Expression<String>.ilike(String pattern): Predicate { ... }

    operator fun <N: Number> Expression<N>.plus(
        Expression<N> right
    ): Expression<N> { ... }
    operator fun <N: Number> Expression<N>.minus(
        Expression<N> right
    ): Expression<N> { ... }
    ```

很显然，这样的能力是目前的java所不具备的。所以，退而求其次，结合继承使用

```java
public interface Expression<T> {
    ...任何类型表达式都具备的公共行为，略...
}

public interface NumericExpression<
    N extends Number
> extends Expression<N> {

    NumericExpression<N> plus(
        Expression<N> right
    );
    NumericExpression<N> minus(
        Expression<N> right
    );

    ...
}

public interface ComparableExpression<
    T extends Compariable<T>
> extends Expression<N> {

    ...
}

public interface StringExpression 
extends ComparableExpression<String> {

    Predicate like(String pattern);
    Predicate ilike(String pattern);

    ...
}
```

因为表达式类型从一个`Expression<T>`分裂成了`Expression<T>`, `StringExpression`, `NumericExpression<N>`和`Comparable<T>`这4个类型，所以那4个类型引导辅助方法就出现了。

```java
interface Expression {

    static StringFactory string() {...}

    static NumericFactory numeric() {...}

    static ComparableFactory comparable() {...}

    static AnyFactory any() {...}

    interface StringFactory {

        StringExpression value(String value);

        ...
    }

    interface NumericFactory {

        <N extends Number> NumericExpression<N> value(N value);

        ...
    }

    interface ComparableFactory {

        <T extends Comparable<T>> ComparableExpression<T> value(T value);

        ...
    }

    interface AnyFactory {

        <T> Expression<T> value(T value);

        ...
    }
}
```

这里列举了字面量表达式的构建API，用户进行类型引导，可以让不同类型的字面量表达式可以进行不同后续操作。

其实，在设计Java DSL时，为了解决这个问题，还有另外一个方法：不把`like`, `+`, `-`这些运算操作设计成Expession&lt;T&gt;的成员方法，而是设计成一个全局对象的方法，甚至是一些静态方法。

JPA Criteria就是设计了这样的一个全局对象，代码片段如下:

```java
package javax.persistence.criteria;

public interface CriteriaBuilder {

    // For string expressions...
    Predicate like(Expression<String> x, String pattern);

    // For numeric expressions...
    <N extends Number> Expression<N> sum(
        Expression<? extends N> x, 
        Expression<? extends N> y
    );

    ....
}
```

这样的设计绕开了Java语言的这个缺陷，但是用户在书写任何表达式时，都要获取到这个全局对象（如果是静态方法的设计，就需要静态导入）。然后利用这个全局对象的方法创建所有表达式对象。比如: 

`cb.like(book.get(Book_.name), "a")`，其中`cb`就是那个全局对象。

显然，对象自身的方法更自然，也更便捷: 

`book.name().like("a")`

这种更自然更便捷的写法是在绝大部分情况都会使用的，而类型引导在开发中使用频率并不高。所以，为了在Java有限的表达能力下尽可能提供方便的设计，经过了慎重的考虑和取舍，jimmer-sql的表达式系统设计成了大家现在看到的这样。