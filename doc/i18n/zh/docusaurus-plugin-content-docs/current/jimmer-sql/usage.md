---
sidebar_position: 1
title: jimmer-sql初体验
---

jimmer-sql内容相对较多，后续文章会逐步讲解。本文对其部分功能进行一个简单的罗列，让读者有一个感性的认知。

## 导入依赖

```groovy title="build.gradle"
depdencies {
    
    implementation 'org.babyfish.jimmer:jimmer-sql:0.1.25'
    annotationProcessor 'org.babyfish.jimmer:jimmer-apt:0.1.25'

    runtimeOnly 'com.h2database:h2:2.1.212'
}
```

## 定义实体
```java title="BookStore.java"
package org.babyfish.jimmer.sql.example.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.UUID;

import org.babyfish.jimmer.sql.Key;

@Entity
public interface BookStore {

    @Id
    long id();

    @Key
    String name();

    @Null
    String website();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
```

:::info
注意：

1. 虽然jimmer-sql使用了一些JPA注解，但是jimmer-sql不是JPA实现，它有自己的API。jimmer-sql和JPA没有任何关系。

2. 代码中的注解@Key是org.babyfish.jimmer.sql.Key，它并非JPA注解，而是jimmer-sql的注解。此注解在后续文章会讲解。这里，请读者先忽略之。
:::

```java title="Book.java"
package org.babyfish.jimmer.sql.example.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.babyfish.jimmer.sql.Key;

@Entity
public interface Book {

    @Id
    long id();

    @Key
    String name();

    @Key
    int edition();

    BigDecimal price();

    @ManyToOne(optional = true)
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumns = @JoinColumn(name = "BOOK_ID"),
            inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID")
    )
    List<Author> authors();
}
```

```java title="Author.java"
package org.babyfish.jimmer.sql.example.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.List;
import java.util.UUID;

@Entity
public interface Author {

    @Id
    long id();

    @Key
    String firstName();

    @Key
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
```

Author类中使用了一个Gender类型，这是一个枚举类型，其定义如下

```java title="Gender.java"
package org.babyfish.jimmer.sql.example.model;

public enum Gender {
    MALE,
    FEMALE
}
```

## 创建SqlClient

SqlClient是jimmer-sql所有Api的入口，因此，需要先创建SqlClient。

```java
package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.example.model.Gender;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import org.babyfish.jimmer.sql.example.model.Gender;
import javax.sql.DataSource;

DataSource dataSource = ...;

JSqlClient sqlClient = JSqlClient
    .newBuilder()
    .setConnectionManager(
        ConnectionManager.simpleConnectionManager(dataSource)
    )
    .addScalarProvider(
            ScalarProvider.enumProviderByString(Gender.class, it -> {
                it.map(Gender.MALE, "M");
                it.map(Gender.FEMALE, "F");
            })
    )
    .build();
```

:::note
这个例子，仅仅让jimmer-sql能根据连接池自动创建连接。

关于和Spring事务机制配合的话题，其参见[JSqlClient](./sql-client)
:::

## 查询

### 简单查询

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(book.name().like("Java"))
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
where tb_1_.NAME like ?
```

### 使用表连接

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                book
                // highlight-next-line
                .store()
                .name()
                .eq("MANNING")
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
/* highlight-next-line */
inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
where tb_2_.NAME = ?
```

### 使用子查询

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                book.id().in(
                    //highlight-next-line
                    q.createSubQuery(
                        AuthorTableEx.class, (sq, author) -> {
                            return sq
                                .where(author.firstName().like("Alex"))
                                .select(author.books().id());
                        }
                    )
                )
            )
            .select(book);
    }).execute();
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
where tb_1_.ID in (
    /* highlight-next-line */
    select tb_3_.BOOK_ID 
    from AUTHOR as tb_2_ 
    inner join BOOK_AUTHOR_MAPPING as tb_3_ 
    on tb_2_.ID = tb_3_.AUTHOR_ID 
    where tb_2_.FIRST_NAME like ?
)
```

### 分组查询
```java
// Column1: BookStore id
// Column2: Average book price of each BookStore
List<Tuple2<UUID, BigDecimal>> tuples = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            // highlight-next-line
            .groupBy(book.store().id())
            .select(
                book.store().id(),
                // highlight-next-line
                book.price().avg()
            );
    }).execute();
tuples.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    tb_1_.STORE_ID, 
    /* highlight-next-line */
    avg(tb_1_.PRICE) 
from BOOK as tb_1_ 
/* highlight-next-line */
group by tb_1_.STORE_ID
```

### 分页查询
```java
ConfigurableTypedRootQuery<BookTable, Book> bookQuery =
    sqlClient.createQuery(BookTable.class, (q, book) -> {
        return q
            .where(book.store().name().eq("O'REILLY"))
            .orderBy(book.name())
            .select(book);
    });

TypedRootQuery<Long> countQuery = bookQuery
    // highlight-next-line
    .reselect((oldQuery, book) ->
        oldQuery.select(book.count())
    )
    .withoutSortingAndPaging();

int rowCount = countQuery.execute().get(0).intValue();
System.out.println("Total row count: " + rowCount);

List<Book> top2Rows = bookQuery
    // highlight-next-line
    .limit(2, 0)
    .execute();
System.out.println("Top 2 rows: " + top2Rows);
```

最终生成会生成两条SQL

1.
    ```sql
    /* highlight-next-line */
    select count(tb_1_.ID) 
    from BOOK as tb_1_ 
    inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
    where tb_2_.NAME = ?
    ```

2.
    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.EDITION, 
        tb_1_.PRICE, 
        tb_1_.STORE_ID 
    from BOOK as tb_1_ 
    inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
    where tb_2_.NAME = ? 
    order by tb_1_.NAME asc 
    /* highlight-next-line */
    limit ?
    ```

### 混合Native SQL
```java
// Column 1: Book object
// Column 2: Global price rank
// Column 3: Local price rank in its BookStore
List<Tuple3<Book, Integer, Integer>> tuples = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q.select(
            book,
            // highlight-next-line
            Expression.numeric().sql(
                Integer.class,
                "rank() over(order by %e desc)",
                it -> it.expression(book.price())
            ),
            // highlight-next-line
            Expression.numeric().sql(
                Integer.class,
                "rank() over(partition by %e order by %e desc)",
                it -> it
                        .expression(book.store().id())
                        .expression(book.price())
            )
        );
    }).execute();
tuples.forEach(System.out::println);
```

最终生成的SQL如下

```sql
select 
    
    tb_1_.ID, 
    tb_1_.NAME, 
    tb_1_.EDITION, 
    tb_1_.PRICE, 
    tb_1_.STORE_ID, 
    
    /* highlight-next-line */
    rank() over(order by tb_1_.PRICE desc), 

    /* highlight-next-line */
    rank() over(partition by tb_1_.STORE_ID order by tb_1_.PRICE desc) 
from BOOK as tb_1_
```

### 使用对象抓取器
```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .orderBy(book.name())
            .select(
                // highlight-next-line
                book.fetch(
                    BookFetcher.$
                        .allScalarFields()
                        // highlight-next-line
                        .store(
                            BookStoreFetcher.$
                                .allScalarFields()
                        )
                        // highlight-next-line
                        .authors(
                            AuthorFetcher.$
                                .allScalarFields()
                        )
                )
            );
    })
    .execute();
books.forEach(System.out::println);
```

最终生成的三条SQL

1.
    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.EDITION, 
        tb_1_.PRICE, 
        tb_1_.STORE_ID 
    from BOOK as tb_1_ 
    order by tb_1_.NAME asc
    ```

2.
    ```sql
    select 
        tb_1_.ID, 
        tb_1_.NAME, 
        tb_1_.WEBSITE 
    from BOOK_STORE as tb_1_ 
    where tb_1_.ID in (?, ?)
    ```
3.    
    ```sql
    select 
        tb_1_.BOOK_ID, 
        tb_1_.AUTHOR_ID, 
        tb_3_.FIRST_NAME, 
        tb_3_.LAST_NAME, 
        tb_3_.GENDER 
    from BOOK_AUTHOR_MAPPING as tb_1_ 
    inner join AUTHOR as tb_3_ on tb_1_.AUTHOR_ID = tb_3_.ID 
    where tb_1_.BOOK_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ```

这里，第一条SQL用于查询Book实体本身；后续两条额外的SQL负责查询`Book`的关联对象，它们在主查询之后执行。

## 修改

jimmer-sql提供两种方式修改数据库，语句和指令

- 语句：可以执行update和delete，适用于逻辑简单但需要批量操作的场合。
- 指令：可以执行insert、update和delete，适用于逻辑复杂的场合。

### 使用修改语句

#### update语句

```java
int affectedRowCount = sqlClient
    .createUpdate(BookTable.class, (u, book) -> {
        u.set(
            book.price(), 
            book.price().plus(new BigDecimal(10))
        );
        u.where(book.name().like("Java"));
    })
    .execute();
System.out.println("Affected row count: " + affectedRowCount);
```

最终生成如下SQL

```sql
update BOOK tb_1_ 
set PRICE = tb_1_.PRICE + ? 
where tb_1_.NAME like ?
```

#### delete语句

```java
int affectedRowCount = sqlClient
    .createDelete(BookTable.class, (d, book) -> {
        d.where(book.name().like("Java"));
    })
    .execute();
System.out.println("Affected row count: " + affectedRowCount);
```

最终生成如下SQL

```sql
delete from BOOK as tb_1_ where tb_1_.NAME like ?
```

### 使用修改指令

#### Save指令

由于jimmer-core提供的不可变实体对象具备动态性，所以实体对象可以表述各种各样的信息，比如：

- 残缺的对象
- 完整的对象
- 较浅的对象树
- 较深的对象树

无论是何种情况，都可以使用Save指令，靠一句代码对其进行插入、更新、或保存（所谓保存，即插入或更新）。

为了给予一个感性认知和初步了解，本文示范两个例子，保存单个对象，保存对象树。

##### 1. 保存单个对象

```java
SimpleSaveResult<Book> result = sqlClient
    .getEntities()
    .save(
        BookDraft.$.produce(book ->
            book.setName("BookName")
                .setEdition(1)
                .setPrice(new BigDecimal(50))
        )
    );
System.out.println(
    "Affected row count: " + 
    result.getTotalAffectedRowCount()
);
```

这条指令的实际执行逻辑依赖于数据库中现有数据，假设数据中不存在`name`为"BookName"且`edition`为1的书籍，则生成如下这些SQL

1.
    ```sql
    select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
    from BOOK as tb_1_ 
    where tb_1_.NAME = ? and tb_1_.EDITION = ?
    ```

2.
    ```sql
    insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)
    ```

##### 2. 保存对象树
```java
SimpleSaveResult<Book> result = sqlClient
    .getEntities()
    .saveCommand(
        BookDraft.$.produce(book -> {
            book.setName("BookName")
                .setEdition(1)
                .setPrice(new BigDecimal(50))
                .setStore(store -> 
                    store.setName("StoreName")
                )
                .addIntoAuthors(author -> {
                    author
                            .setFirstName("AuthorFirstName-1")
                            .setLastName("AuthorLastName-1")
                            .setGender(Gender.MALE);
                })
                .addIntoAuthors(author -> {
                    author
                            .setFirstName("AuthorFirstName-2")
                            .setLastName("AuthorLastName-2")
                            .setGender(Gender.FEMALE);
                });
        })
    )
    .configure(
            AbstractSaveCommand.Cfg::setAutoAttachingAll
    )
    .execute();

System.out.println(

    "Affected row count: " +
    result.getTotalAffectedRowCount() +

    "\nAffected row count of table 'BOOK': " +
    result.getAffectedRowCount(AffectedTable.of(Book.class)) +

    "\nAffected row count of table 'BOOK_STORE': " +
    result.getAffectedRowCount(AffectedTable.of(BookStore.class)) +

    "\nAffected row count of table 'AUTHOR': " +
    result.getAffectedRowCount(AffectedTable.of(Author.class)) +

    "\nAffected row count of middle table 'BOOK_AUTHOR_MAPPING': " +
    result.getAffectedRowCount(
        AffectedTable.of(BookTableEx.class, BookTableEx::authors)
    )
);
```

这条指令的实际执行逻辑依赖于数据库中现有数据，假设对象树中所有对象在数据库中都不存在，则生成如下这些SQL

1. 
    ```sql
    select tb_1_.ID, tb_1_.NAME 
    from BOOK_STORE as tb_1_ 
    where tb_1_.NAME = ?
    ```
2.
    ```sql
    insert into BOOK_STORE(ID, NAME) values(?, ?)
    ```

3.
    ```sql
    select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION 
    from BOOK as tb_1_ 
    where tb_1_.NAME = ? and tb_1_.EDITION = ?
    ```
4.
    ```sql
    insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) 
    values(?, ?, ?, ?, ?)
    ```

5.

    ```sql
    select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
    from AUTHOR as tb_1_ 
    where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?
    ```
6.

    ```sql
    insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) 
    values(?, ?, ?, ?)
    ```

7.

    ```sql
    select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME 
    from AUTHOR as tb_1_ 
    where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?
    ```
8.

    ```sql
    insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) 
    values(?, ?, ?, ?)
    ```

9.
    ```sql
    insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) 
    values 
        (?, ?), 
        (?, ?)
    ```

#### Delete指令

```java
Collection<UUID> ids = ...

DeleteResult result = sqlClient
    .getEntities()
    .batchDelete(Book.class, ids);

System.out.println(
    
    "Affected row count: " + 
    result.getTotalAffectedRowCount() +

    "\nAffected row count of table 'BOOK': " +
    result.getAffectedRowCount(AffectedTable.of(Book.class)) +
    
    "\nAffected row count of middle table 'BOOK_AUTHOR_MAPPING': " +
    result.getAffectedRowCount(
        AffectedTable.of(BookTableEx.class, BookTableEx::authors)
    )
);
```

最终生成的SQL如下

1. 
    ```sql
    delete from BOOK_AUTHOR_MAPPING 
    where BOOK_ID in(?, ?, ?, ?)
    ```

2. 
    ```sql
    delete from BOOK 
    where ID in(?, ?, ?, ?)
    ```

打印结果如下

```
Affected row count: 9
Affected row count of table 'BOOK': 4
Affected row count of middle table 'BOOK_AUTHOR_MAPPING': 5
```
