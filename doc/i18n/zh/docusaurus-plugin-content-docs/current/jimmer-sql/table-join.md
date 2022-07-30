---
sidebar_position: 5
title: 表连接
---

本文将系统性介绍表之间的join操作。包含四个部分

1. 动态连接
2. 幻连接
3. 半连接
4. 逆连接

## 动态连接

:::tip
动态连接是jimmer-sql极具特色的一个功能，是jimmer-sql明显区别于其它ORM的特征之一。

动态连接很实用，但是靠经典手段实现起来并不方便，即便使用注重可控性的myBatis来实现这个功能仍然如此。
:::

### 示例

让我们先来看一个动态连接的示范。

```java
public class BookRepository {

    private final SqlClient sqlClient;

    public BookRepository(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<Book> findBooks(
        @Nullable String name, 
        @Nullable String storeName,
        @Nullable String storeWebsite
    ) {
        return sqlClient
            .createQuery(
                BookTable.class, (q, book) -> {
                    if (name != null) {
                        q.where(book.name().eq(name));
                    }
                    if (storeName != null) {
                        q.where(
                            book
                                // highlight-next-line
                                .store() // α
                                .name()
                                .eq(storeName)
                        );
                    }
                    if (storeWebsite != null) {
                        q.where(
                            book
                                // highlight-next-line
                                .store() // β
                                .website()
                                .eq(storeWebsite)
                        );
                    }
                    return q.select(book);
                }
            )
            .execute();
    }
}
```

这是一个典型的动态查询，三个查询参数都允许为null。

- 指定`name`，但`storeName`和`storeWebsite`仍然为null。

    这时，`α`和`β`两处的代码都不会执行，最终生成的SQL不会包含任何join。

    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.EDITION, 
        tb_1_.PRICE, 
        tb_1_.STORE_ID 
    from BOOK as tb_1_ 
        where tb_1_.NAME = ?
    ```
- 指定`name`和`storeName`, 但`storeWebsite`仍然为null。

    这时，`α`处的连接生效但`β`处的代码不会被执行，最终生成的SQL如下。

    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.EDITION, 
        tb_1_.PRICE, 
        tb_1_.STORE_ID 
    from BOOK as tb_1_ 
    /* highlight-start */
    inner join BOOK_STORE as tb_2_ 
        on tb_1_.STORE_ID = tb_2_.ID
    /* highlight-end */ 
    where 
        tb_1_.NAME = ? 
    and 
        tb_2_.NAME = ?
    ```
- 指定`name`和`storeWebsite`, 但`storeName`仍然为null。

    这时，`β`处的连接生效但`α`处的代码不会被执行，最终生成的SQL如下。

    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.EDITION, 
        tb_1_.PRICE, 
        tb_1_.STORE_ID 
    from BOOK as tb_1_ 
    /* highlight-start */
    inner join BOOK_STORE as tb_2_ 
        on tb_1_.STORE_ID = tb_2_.ID 
    /* highlight-end */
    where 
        tb_1_.NAME = ? 
    and 
        tb_2_.WEBSITE = ?
    ```

- 指定所有参数，`name`, `storeName`和`storeWebsite`都非null。
    
    这时，`α`、`β`两处的连接都生效，这种情况叫连接冲突。
    
    这种冲突并不会导致任何问题，因为在最终SQL中，**冲突的table join会被合并成了一个，而非join多次**。

    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.EDITION, 
        tb_1_.PRICE, 
        tb_1_.STORE_ID 
    from BOOK as tb_1_ 
        /* highlight-start */
        inner join BOOK_STORE as tb_2_ on 
            tb_1_.STORE_ID = tb_2_.ID 
        /* highlight-end */
    where 
        tb_1_.NAME = ? 
    and 
        tb_2_.NAME = ? 
    and 
        tb_2_.WEBSITE = ?
    ```

:::info
小结

1. 无需像其它ORM一样使用局部变量来记住join对象，可以在SQL的任何位置创建并使用临时join对象。

2. 更重要的是，不必考虑这些join对象之间是否存在冲突，冲突的join对象会被自动合并。

这样特性使得jimmer-sql极其适合实现复杂的动态查询。也是jimmer-sql项目被创建的动机之一。
:::

另外，如果创建了表连接但并不使用，该表连接将会被忽略，不会在最终SQL中被生成。

### 冲突合并规则

上面的例子很简单，Java代码中的连接只有一层。事实上，可以创建较深的join对象

```java
q.where(
    store // 假设store是TableEx，允许使用集合关联
        // highlight-next-line
        .books().authors() // 多层join
        .firstName()
        .eq("X")
);
```

或

```java
q.where(
    author // 假设author是TableEx，允许使用集合关联
        // highlight-next-line
        .books().store() // 多层join
        .name()
        .eq("X")
);
```

由此可见，Java代码创建的join对象，其实是一种长度任意的路径，叫做join路径。join路径包含1到无穷个join节点。

为了让描述更具普适性，让我们来看三个比较长的路径 *（实际项目中不可能有如此长的表连接路径，仅借此做阐述而已）*。

1. a -> b -> c -> d -> e -> f -> g
2. a -> b -> c -> h -> i -> j
3. a -> x -> y -> z -> a-> b -> c -> d

为了消除冲突，jimmer-sql会把这些路径中的节点合并成一棵树
```
-+-a
 |
 +----+-b
 |    |
 |    \----+-c 
 |         |
 |         +----+-d
 |         |    |
 |         |    \----+-e
 |         |         |
 |         |         \----+-f
 |         |              |
 |         |              \------g
 |         |
 |         \----+-h
 |              |
 |              \----+-i
 |                   |
 |                   \------j
 |
 \----+-x
      |
      \----+-y
           |
           \----+-z
                |
                \----+-a
                     |
                     \----+-b
                          |
                          \----+-c
                               |
                               \------d
```

jimmer-sql会根据这棵树来生成最终SQL中的join子句。

另外一个需要说明的规则，就是连接方式。创建join对象的方法具备参数，以指定连接方式，比如，左连接：

```java
book.store(JoinType.LEFT);
```
> 如果不指定该参数，默认内连接。

连接方式合并规则如下：

- 如果发生冲突的join节点的连接方式全部一样，合并后连接方式不变。
- 否则，合并后一定是内连接。

## 幻连接

幻连接是一个很简单的优化概念，和普通的连接对比一下就明白了。

我们先来看一个普通表连接的例子。

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(book.store().name().eq("MANNING"))
            .select(book);
    })
    .execute();
books.stream().forEach(System.out::println);
```

生成的SQL如下：

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
    /* highlight-start */
    inner join BOOK_STORE as tb_2_ 
        on tb_1_.STORE_ID = tb_2_.ID
    /* highlight-end */     
where 
    tb_2_.NAME = ?
```

现在，再来看一个幻连接的例子

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                book
                    .store()
                    // highlight-next-line
                    .id() // 只访问id
                    .eq(
                        UUID.fromString(
                            "2fa3955e-3e83-49b9-902e-0465c109c779"
                        )
                    )
            )
            .select(book);
    })
    .execute();
books.stream().forEach(System.out::println);
```

这次，生成的SQL如下：

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
    where tb_1_.STORE_ID = ?
```

我们没有在SQL中看到任何表连接，我们只看到条件一个基于外键的判断条件`tb_1_.STORE_ID = ?`。

原因：对于基于外键映射的多对一关联而言，父表的id其实就是子表自己的外键。

:::info
1. 对于基于外键映射的多对一关联，在Java查询代码中通过连接操作获取了关联对象，如果此关联对象除了id字段以外没有任何的其它字段被访问，那么该连接将被视为幻连接。

2. 幻连接将会被忽略，不会在最终SQL中生成相关的JOIN语句。
:::

## 半连接

半连接是一个和幻象连接类似的概念，但用于基于中间表的关联。

我们先来看一个基于中间表的普通连接。

```java
List<UUID> bookIds = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                ((BookTableEx)book)
                    .authors()
                    .firstName()
                    .eq("Alex")
            )
            .select(book.id());
    })
    .distinct()
    .execute();
bookIds.forEach(System.out::println);
```

生成的SQL如下：
```sql
select 
    distinct tb_1_.ID 
from BOOK as tb_1_ 
/* highlight-start */
inner join BOOK_AUTHOR_MAPPING as tb_2_ 
    on tb_1_.ID = tb_2_.BOOK_ID 
inner join AUTHOR as tb_3_ on 
    tb_2_.AUTHOR_ID = tb_3_.ID
/* highlight-end */ 
where tb_3_.FIRST_NAME = ?
```

我们看到基于中间表的连接会产生两个SQL JOIN子句

- 第一步：连接到中间表
    `inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID `

- 第二步：连接到目标表
    `inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID`

接下来，让我们看看半连接的例子

```java
List<UUID> bookIds = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                ((BookTableEx)book)
                    .authors()
                    // highlight-next-line
                    .id() // 只访问id
                    .eq(
                        UUID.fromString(
                            "1e93da94-af84-44f4-82d1-d8a9fd52ea94"
                        )
                    )
            )
            .select(book.id());
    })
    .distinct()
    .execute();
bookIds.forEach(System.out::println);
```

这次，生成的SQL如下：
```sql
select 
    distinct tb_1_.ID 
from BOOK as tb_1_ 
/* highlight-start */
inner join BOOK_AUTHOR_MAPPING as tb_2_ 
    on tb_1_.ID = tb_2_.BOOK_ID 
/* highlight-end */
where tb_2_.AUTHOR_ID = ?
```

这一次，我们只看到一个SQL JOIN子句，而不是两个。

原因：目标表的主键，其实就是中间表到目标表的外键。

:::info
1. 对于基于中间表映射的关联，在Java查询代码中通过连接操作获取了关联对象，如果此关联对象除了id字段以外没有任何的其它字段被访问，那么该连接将被视为半连接。

2. 在最终生成的SQL中，半连接仅使用一条JOIN语句连接到中间表，不会进一步使用第二条JOIN语句连接到目标表。
:::

## 逆连接

到目前为止，我们讨论过的所有表连接仅适用于在实体接口中定义了java属性的情况。

诚然，如果开发人员定义了实体接口之间的双向关联

`A <--> B`

我们可以从任何一端连接到另外一端，无论是从`A`到`B`还是从`B`到`A`。

但是，有时开发者在实体接口中只定义单向关联

`A --> B`
现在，我们只能从`A`连接到B，无法从`B`连接到`A`。

诚然，子查询可以解决所有问题。然而，jimmer-sql仍然可以让您通过表连接来解决这个问题，这被称为逆连接。

为了更好地阐述逆连接，我们先来看看普通的连接。

```java
q.where(
    book
        // 这是一个普通的正向连接
        // highlight-next-line
        .authors()
        .firstName()
        .eq("Alex")
);
```

与之完全等价的逆连接，有两种写法

1. 弱类型写法
    ```java
    q.where(
        book
            // Auhtor.books反过来就是Book.authors
            // highlight-next-line
            .inverseJoin(Author.class, "books")
            .firstName()
            .eq("Alex")
    );
    ```

2. 强类型写法
    ```java
    q.where(
        book
            // Auhtor.books反过来就是Book.authors
            // highlight-start
            .inverseJoin(
                AuthorTableEx.class, 
                AuthorTableEx::books
            )
            // highlight-end
            .firstName()
            .eq("Alex")
    );
    ```

:::info

注意

虽然逆连接不难理解，但代码阅读读起相对晦涩。正因如此，不希望它被滥用。

它只应该在某些特殊情况下被使用，例如

1. 实体接口的定义属于第三方，不是自己可以控制的代码，而第三方实体只定义了单向关联，而没有定义双向关联。

2. 开发某些通用性的框架时，无法假设用户一定会定义双向关联。

然而，日常业务系统开发中，应考虑在实体接口中定义双向关联，而不是使用逆连接。
:::