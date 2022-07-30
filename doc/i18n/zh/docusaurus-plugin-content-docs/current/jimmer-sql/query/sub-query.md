---
sidebar_position: 1
title: 子查询
---

## 有类型子查询

### 基于单列的IN表达式
```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                // highlight-next-line
                book.id().in(
                    q.createSubQuery(AuthorTableEx.class, (sq, author) -> {
                        return sq
                            .where(
                                    author.firstName().eq("Alex")
                            )
                            .select(author.books().id());
                    })
                )
            )
            .select(book);
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where 
    /* highlight-next-line */
    tb_1_.ID in (
        select 
            tb_3_.BOOK_ID 
        from AUTHOR as tb_2_ 
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.AUTHOR_ID 
        where 
            tb_2_.FIRST_NAME = ?
    )

```

### 基于多列的IN表达式

```java
List<Book> newestBooks = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                // highlight-next-line
                Expression.tuple(
                    book.name(),
                    book.edition()
                ).in(
                    q.createSubQuery(BookTable.class, (sq, book2) -> {
                        return sq
                            .groupBy(book2.name())
                            .select(
                                    book2.name(),
                                    book2.edition().max()
                            );
                    })
                )
            )
            .select(book);
    })
    .execute();
newestBooks.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where 
    /* highlight-next-line */
    (tb_1_.NAME, tb_1_.EDITION) in (
        select 
            tb_2_.NAME, 
            max(tb_2_.EDITION) 
            from BOOK as tb_2_ 
            group by tb_2_.NAME
    )
```

### 将子查询视为简单值

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                // highlight-next-line
                book.price().gt(
                    q.createSubQuery(BookTable.class, (sq, book2) -> {
                        return sq.select(
                            book2
                                .price()
                                .avg()
                                .coalesce(BigDecimal.ZERO)
                        );
                    })
                )
            )
            .select(book);
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where 
    /* highlight-next-line */
    tb_1_.PRICE > (
        select 
            coalesce(avg(tb_2_.PRICE), ?) 
        from BOOK as tb_2_
    )
```

### 在select和orderBy子句中使用子查询

```java
List<Tuple2<BookStore, BigDecimal>> storeAvgPriceTuples = sqlClient
    .createQuery(BookStoreTable.class, (q, store) -> {
        TypedSubQuery<BigDecimal> avgPriceSubQuery =
            q.createSubQuery(BookTable.class, (sq, book) -> {
                return sq.select(
                    book
                        .price()
                        .avg()
                        .coalesce(BigDecimal.ZERO)
                );
            });
        return q
                .orderBy(
                    // highlight-next-line
                    avgPriceSubQuery,
                    OrderMode.DESC
                )
                .select(
                    store,
                    // highlight-next-line
                    avgPriceSubQuery
                );
    })
    .execute();
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.WEBSITE, 
    (
        /* highlight-next-line */
        select coalesce(avg(tb_2_.PRICE), ?) 
        from BOOK as tb_2_
    ) 
from BOOK_STORE as tb_1_ 
order by (
    /* highlight-next-line */
    select coalesce(avg(tb_2_.PRICE), ?) 
    from BOOK as tb_2_
) desc
```

### 使用any运算符

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                book.id().eq(
                    q
                        .createSubQuery(
                            AuthorTableEx.class, 
                            (sq, author) -> {
                                return sq
                                    .where(
                                        author.firstName().in(
                                            "Alex", 
                                            "Bill"
                                        )
                                    )
                                    .select(author.books().id());
                                }
                        )
                        // highlight-next-line
                        .any()
                )
            )
            .select(book);
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where tb_1_.ID = 
    /* highlight-next-line */
    any(
        select 
            tb_3_.BOOK_ID 
        from AUTHOR as tb_2_ 
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.AUTHOR_ID 
        where 
            tb_2_.FIRST_NAME in (?, ?)
    )
```

### 使用all运算符

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                book.id().eq(
                    q
                        .createSubQuery(
                            AuthorTableEx.class, 
                            (sq, author) -> {
                                return sq
                                    .where(
                                        author.firstName().in(
                                            "Alex", 
                                            "Bill"
                                        )
                                    )
                                    .select(author.books().id());
                                }
                        )
                        // highlight-next-line
                        .all()
                )
            )
            .select(book);
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where tb_1_.ID = 
    /* highlight-next-line */
    all(
        select 
            tb_3_.BOOK_ID 
        from AUTHOR as tb_2_ 
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.AUTHOR_ID 
        where 
            tb_2_.FIRST_NAME in (?, ?)
    )
```

### 使用exists运算符

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                q.createSubQuery(
                    AuthorTableEx.class,
                    (sq, author) -> {
                        return sq
                            .where(
                                    book.eq(author.books()),
                                    author.firstName().eq("Alex")
                            )
                            .select(author);
                    }
                )
                // highlight-next-line
                .exists()
            )
            .select(book);
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where 
    /* highlight-next-line */
    exists (
        select 
            1 
        from AUTHOR as tb_2_ 
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.AUTHOR_ID 
        where 
            tb_1_.ID = tb_3_.BOOK_ID 
        and 
            tb_2_.FIRST_NAME = ?
    )
```

:::note
注意，在最终生成的SQL中，子查询选取的列是常量`1`，并非Java代码的设置。

这是因为`exists`运算符只在乎子查询是否能匹配到数据，并不在乎子查询选取了那些列。无论你在Java代码中让子查询选取什么，都会被无视。
:::

## 无类型子查询

上一节最后一个例子是`exists`子查询，无论你在Java代码中让子查询选取什么都会被无视。

既然如此，为什么要为`exists`子查询指定返回格式呢？

因此，jimmer-sql支持无类型子查询(Wild sub query)，和普通子查询不同，无类型子查询实现中，不再需要最后那一句select方法调用，即，不需要返回类型。

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                // highlight-next-line
                q.createWildSubQuery(
                    AuthorTableEx.class,
                    (sq, author) -> {
                        sq.where(
                                book.eq(author.books()),
                                author.firstName().eq("Alex")
                        );
                        // 结尾处不需要调用select
                    }
                )
                .exists()
            )
            .select(book);
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的SQL不变，仍然是

```sql
select 
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID 
from BOOK as tb_1_ 
where 
    /* highlight-next-line */
    exists (
        select 
            1 
        from AUTHOR as tb_2_ 
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.AUTHOR_ID 
        where 
            tb_1_.ID = tb_3_.BOOK_ID 
        and 
            tb_2_.FIRST_NAME = ?
    )
```

:::note
无类型子查询唯一的价值，就是和`exists`运算符配合。
:::