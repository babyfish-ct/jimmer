---
sidebar_position: 5
title: 查询中间表
---

:::tip
查询中间表是jimmer-sql极具特色的一个小功能。
:::

## 被对象模型隐藏的中间表

让我们回顾一下这段实体接口定义代码

```java
@Entity
public interface Book {

    @ManyToMany
    @JoinTable(
        // highlight-next-line
        name = "BOOK_AUTHOR_MAPPING",
        joinColumns = 
            @JoinColumn(name = "BOOK_ID"),
        inverseJoinColumns = 
            @JoinColumn(name = "AUTHOR_ID")
    )
    List<Author> authors();

    ...其它代码略...
}
```

上述代码中，`BOOK_AUTHOR_MAPPING`表作为中间表被使用。

- 数据库的BOOK表，Java代码有与之对应的实体接口Book。
- 数据库的AUTHOR表，Java代码有与之对应的实体接口Author。
- 但是，数据库中的BOOK_AUTHOR_MAPPING表，在Java代码中没有对应的实体接口。

即，中间表被对象模型隐藏了。

## 直接查询中间表

jimmer-sql提供了一个有趣的功能，即便中间表被隐藏没有对应实体，也可以对其直接查询。

```java
List<Association<Book, Author>> associations =
    sqlClient
        // highlight-next-line
        .createAssociationQuery(
            BookTableEx.class,
            BookTableEx::authors,
            (q, association) -> {
                q.where(
                    association.source().id().eq(
                        UUID.fromString(
                            "64873631-5d82-4bae-8eb8-72dd955bfc56"
                        )
                    )
                );
                return q.select(association);
            }
        ).execute();
associations.forEach(System.out::println);
```

这里，`createAssociation`表示基于中间表创建查询，而非基于实体表。

前两个参数，`BookTableEx.class`和`BookTableEx::authors`表示，查询关联`Book.authors`所对应的中间表`BOOK_AUTHOR_MAPPING`。

生成的SQL如下
```sql
select 
    tb_1_.BOOK_ID, 
    tb_1_.AUTHOR_ID 
/* hight-next-line */
from BOOK_AUTHOR_MAPPING as tb_1_
where tb_1_.BOOK_ID = ?
```

果然，这是一个基于中间表的查询。

最终打印结果如下（原输出是紧凑的，为了方便阅读，这里进行了格式化）:

```
Association{
    source={
        "id":"64873631-5d82-4bae-8eb8-72dd955bfc56"
    }, target={
        "id":"1e93da94-af84-44f4-82d1-d8a9fd52ea94"
    }
}
Association{
    source={
        "id":"64873631-5d82-4bae-8eb8-72dd955bfc56"
    }, 
    target={
        "id":"fd6bb6cf-336d-416c-8005-1ae11a6694b5"
    }
}
```

返回数据是一系列Association对象

```java
package org.babyfish.jimmer.sql.association;

public class Association<S, T> {

    public Association(S source, T target) {
        this.source = source;
        this.target = target;
    }

    public S source() {
        return source;
    }

    public T target() {
        return target;
    }
}
```

`Association<S, T>`表示从`S`类型指向`T`类型关联的中间表实体。中间表实体是伪实体，没有id。它只有两个属性:

- `source`: 中间表指向己方的外键所对应的对象(在这个例子中，就是`Book`对象)。
- `target`: 中间表指向对方的外键所对应的对象(在这个例子中，就是`Author`对象)。

:::note

1. 在这个例子中，并未使用对象抓取器定义Association的对象格式（事实上Association也不支持对象抓取器），因此对象的`source`和`targate`关联属性仅包含对象id。

2. `Author`也有一个从动的多对多关联`Author.books`, 它是`Book.authors`的镜像。
    ```
    @Entity
    public interface Author {

        // highlight-next-line
        @ManyToMany(mappedBy = "authors")
        List<Book> books();

        ...
    }
    ```
    基于`Author.books`也可以创建中间表查询，但是`source`代表Author，而`target`代表Book。和当前例子相反。
:::

这个例子中，我们只查询了中间表本身。所以，`source`和`target`对象中只有id。

要获获取完整的`source`和`target`对象，可以表连接，然后利用元组进行返回。

代码如下

```java
List<Tuple2<Book, Author>> tuples =
    sqlClient
        .createAssociationQuery(
            BookTableEx.class,
            BookTableEx::authors,
            (q, association) -> {
                q.where(
                    association.source().id().eq(
                        UUID.fromString(
                            "64873631-5d82-4bae-8eb8-72dd955bfc56"
                        )
                    )
                );
                // highlight-next-line
                return q.select(
                    association.source(),
                    association.target()
                );
            }
        ).execute();
tuples.forEach(System.out::println);
```

生成的SQL如下:
```sql
select 

    /* source() */
    tb_1_.BOOK_ID, 
    tb_2_.NAME, 
    tb_2_.EDITION, 
    tb_2_.PRICE, 
    tb_2_.STORE_ID, 

    /* target() */
    tb_1_.AUTHOR_ID, 
    tb_3_.FIRST_NAME, 
    tb_3_.LAST_NAME, 
    tb_3_.GENDER

from BOOK_AUTHOR_MAPPING as tb_1_ 
inner join BOOK as tb_2_ 
    on tb_1_.BOOK_ID = tb_2_.ID 
inner join AUTHOR as tb_3_ 
    on tb_1_.AUTHOR_ID = tb_3_.ID 
where tb_1_.BOOK_ID = ?
```

最终打印结果如下（原输出是紧凑的，为了方便阅读，这里进行了格式化）:
```
Tuple2{
    _1={
        "id":"64873631-5d82-4bae-8eb8-72dd955bfc56",
        "name":"Learning GraphQL",
        "edition":3,
        "price":51.00,
        "store":{
            "id":"d38c10da-6be8-4924-b9b9-5e81899612a0"
        }
    }, 
    _2={
        "id":"1e93da94-af84-44f4-82d1-d8a9fd52ea94",
        "firstName":"Alex",
        "lastName":"Banks",
        "gender":"MALE"
    }
}
Tuple2{
    _1={
        "id":"64873631-5d82-4bae-8eb8-72dd955bfc56",
        "name":"Learning GraphQL",
        "edition":3,
        "price":51.00,
        "store":{"id":"d38c10da-6be8-4924-b9b9-5e81899612a0"}
    }, 
    _2={
        "id":"fd6bb6cf-336d-416c-8005-1ae11a6694b5",
        "firstName":"Eve",
        "lastName":"Procello",
        "gender":"MALE"
    }
}
```

:::note
关联对象`Association<S, T>`很简单也很特殊，不支持也不需要[对象抓取器](./fetcher)。

注意，这里仅指`Association<S, T>`对象<b>本身</b>不支持，其关联属性`source`和`target`仍然支持[对象抓取器](./fetcher)，如：
```java
return q.select(
    association
        .source()
        // highlight-next-line
        .fetch(
            BookFetcher.$.store(
                BookStoreFetcher.$.allScalarFields()
            )
        ),
    association.target()
);
```
:::

## 和非中间表查询的对比

读者可能会认为，基于中间表查询的查询存在的价值，是为了让开发人员写出性能更高的查询。

但其实不是这样的。由于[幻连接](../table-join#幻连接)和[半连接](../table-join#半连接)这两个优化手段的存在，无论是否使用基于中间表的查询，都能达到很好的性能。是否选择使用基于中间表的查询，完全看用户自己喜好。

### 1. 基于中间表子查询实现一个功能

之前的代码，我们演示基于中间表的顶级查询；而这个例子演示基于中间表的子查询。

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        q.where(
            book.id().in(
                // highlight-next-line
                q.createAssociationSubQuery(
                    BookTableEx.class,
                    BookTableEx::authors,
                    (sq, association) -> {
                        sq.where(
                            association.target() // α
                                .firstName().eq("Alex")
                        );
                        return sq.select(
                            association.source() // β
                                .id()
                        );
                    }
                )
            )
        );
        return q.select(book);
    }).execute();
books.forEach(System.out::println);
```

其中
- `createAssociationSubQuery`用于创建一个基于中间表的子查询。该查询用户寻找所有包含`firstName`为"Alex"的作者的书籍。
- `α`处`association.target()`是真正的表连接，会生成SQL JOIN连接`AUTHOR`表进行条件判断。
- `β`处`association.source()`是由于[幻连接](../table-join#幻连接)，并不会生成SQL join。

最终生成的SQL如下: 

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where 
    tb_1_.ID in (
        /* highlight-next-line */
        select 
            tb_2_.BOOK_ID 
        from BOOK_AUTHOR_MAPPING as tb_2_ 
        inner join AUTHOR as tb_3_ 
            on tb_2_.AUTHOR_ID = tb_3_.ID 
        where tb_3_.FIRST_NAME = ?
    )
```

### 2. 不基于中间表子查询实现同样的功能

```java
List<Book> books = sqlClient
        .createQuery(BookTable.class, (q, book) -> {
            q.where(
                book.id().in(
                    // highlight-next-line
                    q.createSubQuery(
                        AuthorTableEx.class,
                        (sq, author) -> {
                            sq.where(
                                author.firstName()
                                    .eq("Alex")
                            );
                            return sq.select(
                                author.books() // α
                                    .id()
                            );
                        }
                    )
                )
            );
            return q.select(book);
        }).execute();
books.forEach(System.out::println);
```

- `createSubQuery`用于创建一个普通的子查询，不使用中间表。实现完全相同的功能。
- `α`处`author.books()`是[半连接](../table-join#半连接)，所以仅仅生成从表`AUTHOR`到中间表`BOOK_AUTHOR_MAPPING`的SQL JOIN，而不会进一步SQL JOIN到`BOOK表`。

最终生成的SQL如下: 

```sql
select 

    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 

from BOOK as tb_1_ 
where 
    tb_1_.ID in (
        /* highlight-next-line */
        select 
            tb_3_.BOOK_ID 
        from AUTHOR as tb_2_ 
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.AUTHOR_ID 
        where tb_2_.FIRST_NAME = ?
    )
```

对比这两个SQL，不难发现，它们功能一样，性能一样。

所以，是否使用基于中间表的查询，对性能没有影响。随意选择自己喜欢的风格即可。