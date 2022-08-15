---
sidebar_position: 5
title: 修改中间表
---

首先，中间表被对象模型隐藏，没有与之直接对应的实体类型。此问题已在[查询中间表](../query/association)一文中被阐述，此处不作重复。

另外，在[Save指令](./save-command)章节中，我们介绍了save指令。它能对比数据库现有数据和用户要保存的新数据，如果某个基于中间表的关联属性发生了变化，中间表就会被修改。

诚然，Save指令功能很多，其中也包含了中间表的修改功能。然而，有时候，我们只是需要简单地对中间表进行插入或删除，我们不需要Save指令那么强大的功能，但我们希望中间表的修改行为足够简单、足够高效。

jimmer-sql允许开发人员通过最简单也最高效的方式，直接对中间表的数据进行插入和删除。

## 插入关联

```java
UUID bookId = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID authorId = 2L;
sqlClient
    .getAssociations(
        BookTableEx.class, 
        BookTableEx::authors
    )
    // highlight-next-line
    .save(bookId, authorId);
```

生成的SQL如下

```sql
insert into 
    BOOK_AUTHOR_MAPPING(
        /* highlight-next-line */
        BOOK_ID, AUTHOR_ID
    ) 
    values (?, ?)
```

这个例子演示如何使用关联属性`Book.authors`操作中间表。

同理我们也可以通过关联属性`Authors.books`达到相同目的：

```java
UUID bookId = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID authorId = UUID.fromString(2L);
sqlClient
    .getAssociations(
        AuthorTableEx.class,
        AuthorTableEx::books
    )
    // highlight-next-line
    .save(authorId, bookId);
```

注意，和上一个例子相比，因为关联的方向不同，所以`save`方法的参数顺序不同。

生成的SQL如下

```sql
insert into 
    BOOK_AUTHOR_MAPPING(
        /* highlight-next-line */
        AUTHOR_ID, BOOK_ID
    )
    values (?, ?)
```

不难发现，insert语句列的顺序也不同。

## 逆变换

对于双向关联，可以使用`reverse`在正反关联之间切换

```java
Associations bookToAuthor = sqlClient
    .getAssociations(
        BookTableEx.class,
        BookTableEx::authors
    );
Associations authorToBook = sqlClient
    .getAssociations(
        AuthorTableEx.class,
        AuthorTableEx::books
    );
Associations authorToBook2 = 
    // highlight-next-line
    bookToAuthor.reverse();
Associations bookToAuthor2 =
    // highlight-next-line
    authorToBook.reverse();
```

这段代码中：
- `bookToAuthor`和`bookToAuthor2`完全等价
- `authorToBook`和`authorToBook2`完全等价

## 批量插入

```java
UUID bookId1 = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID bookId2 = UUID.fromString(
    "914c8595-35cb-4f67-bbc7-8029e9e6245a"
);
UUID bookId3 = UUID.fromString(
    "058ecfd0-047b-4979-a7dc-46ee24d08f08"
);

UUID authorId1 = UUID.fromString(2L);
UUID authorId2 = UUID.fromString(
    "c14665c8-c689-4ac7-b8cc-6f065b8d835d"
);

sqlClient
    .getAssociations(
        BookTableEx.class,
        BookTableEx::authors
    )
    // highlight-next-line
    .batchSave(
        Arrays.asList(
            new Tuple2<>(bookId1, authorId1),
            new Tuple2<>(bookId1, authorId2),
            new Tuple2<>(bookId2, authorId1),
            new Tuple2<>(bookId2, authorId2),
            new Tuple2<>(bookId3, authorId1),
            new Tuple2<>(bookId3, authorId2)
        )
    );
```

这里有3本书，两个作者，共6种组合方式。`batchSave`方法把这6种组合全部插入到中间表中，生成的SQL如下

```sql
insert into BOOK_AUTHOR_MAPPING(
    BOOK_ID, AUTHOR_ID
) values 
    /* highlight-start */
    (?, ?), 
    (?, ?), 
    (?, ?), 
    (?, ?), 
    (?, ?), 
    (?, ?)
    /* highlight-end */
```

上面这种写法很繁琐，`3 * 2 = 6`还可以接受，但`7 * 9 = 63`呢？难道构建63个元组吗？

jimmer-sql提供一种快捷写法

```java
UUID bookId1 = ...;
UUID bookId2 = ...;
UUID bookId3 = ...;

UUID authorId1 = ...;
UUID authorId2 = ...;

sqlClient
    .getAssociations(
        BookTableEx.class,
        BookTableEx::authors
    )
    // Batch save `Cartesian product` 
    // of two id collections
    // highlight-next-line
    .batchSave(
        Arrays.asList(
            bookId1, bookId2, bookId3
        ),
        Arrays.asList(
            authorId1, authorId2
        )
    );
```

上面的`batchSave`方法接受两个集合参数，把这两个集合形成的笛卡尔乘积插入中间表，所以，功能和上一个例子的相同。

## 检查存在性

如果向中间表插入已经存在的id元组，数据库会被报错，因为违背了唯一性约束。

为了解决这个问题，可以进行存在性检查。

```java
UUID bookId1 = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID bookId2 = UUID.fromString(
    "914c8595-35cb-4f67-bbc7-8029e9e6245a"
);
UUID bookId3 = UUID.fromString(
    "058ecfd0-047b-4979-a7dc-46ee24d08f08"
);

UUID authorId = UUID.fromString(
    "1e93da94-af84-44f4-82d1-d8a9fd52ea94"
);

sqlClient
    .getAssociations(
        BookTableEx.class,
        BookTableEx::authors
    )
    .batchSaveCommand(
        Arrays.asList(bookId1, bookId2, bookId3),
        Collections.singletonList(authorId)
    )
    // highlight-next-line
    .checkExistence()
    .execute();
```

这里`checkExistence()`表示先检查要插入的数据有哪些已经存在，然后只插入不存在的数据。

生成的SQL语句如下

1. 检查哪些待插入数据已经存在数据
    ```sql
    select 
        BOOK_ID, AUTHOR_ID 
    from BOOK_AUTHOR_MAPPING 
    where 
        (BOOK_ID, AUTHOR_ID) in(
            (?, ?), 
            (?, ?),
            (?, ?)
        )
    ```

2. 利用上一步的查询结果，可以计算得到真正需要插入的数据。如果没有需要插入的数据，跳过本步骤；否则，执行本步骤。

    这里，假设判断后真正需要插入的数据还剩两条，生成的SQL为:

    ```sql
    insert into BOOK_AUTHOR_MAPPING(
        BOOK_ID, AUTHOR_ID
    ) values 
        (?, ?), 
        (?, ?)
    ```

:::tip
由于时间关系，有一个未实现的优化。

未来，此功能会改进。针对某些特定的数据库进行优化，如Postgres, MySql。

以Postgres为例，可以直接执行
```sql
insert into BOOK_AUTHOR_MAPPING(
    BOOK_ID, AUTHOR_ID
) values (?, ?), (?, ?), (?, ?)
on conflict(BOOK_ID, AUTHOR_ID)
    do nothing;
```
而无需执行一条额外的select语句。
:::

## 删除关联

```java
UUID bookId = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID authorId = UUID.fromString(
    "1e93da94-af84-44f4-82d1-d8a9fd52ea94"
);
sqlClient
    .getAssociations(
            BookTableEx.class,
            BookTableEx::authors
    )
    // highlight-next-line
    .delete(bookId, authorId);
```

生成的SQL如下

```sql
delete from BOOK_AUTHOR_MAPPING 
where 
    (BOOK_ID, AUTHOR_ID) in (
        (?, ?)
    )
```

## 批量删除

和批量插入类似，有两种批量删除的写法

```java
UUID bookId1 = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID bookId2 = UUID.fromString(
    "914c8595-35cb-4f67-bbc7-8029e9e6245a"
);
UUID authorId = UUID.fromString(
    "1e93da94-af84-44f4-82d1-d8a9fd52ea94"
);
sqlClient
    .getAssociations(
        BookTableEx.class,
        BookTableEx::authors
    )
    // highlight-next-line
    .batchDelete(
        Arrays.asList(
            new Tuple2<>(bookId1, authorId),
            new Tuple2<>(bookId2, authorId)
        )
    );
```
或
```java
UUID bookId1 = ...;
UUID bookId2 = ...;
UUID authorId = ...;
sqlClient
    .getAssociations(
        BookTableEx.class,
        BookTableEx::authors
    )
    // highlight-next-line
    .batchDelete(
        Arrays.asList(book1, book2),
        Collections.singletonList(authorId)
    );
```

生成的SQL如下

```sql
delete from BOOK_AUTHOR_MAPPING 
where 
    (BOOK_ID, AUTHOR_ID) in (
        (?, ?), 
        (?, ?)
    )
```