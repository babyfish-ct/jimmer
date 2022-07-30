---
sidebar_position: 1
title: First experience
---

jimmer-sql has relatively more content, and subsequent articles will explain it step by step. This article briefly lists some of its functions to give readers a perceptual understanding.

## Add dependencies

```groovy title="build.gradle"
depdencies {
    
    implementation 'org.babyfish.jimmer:jimmer-sql:0.0.35'
    annotationProcessor 'org.babyfish.jimmer:jimmer-apt:0.0.35'

    runtimeOnly 'com.h2database:h2:2.1.212'
}
```

## Define entity interfaces

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
    UUID id();

    @Key
    String name();

    @Null
    String website();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
```

:::info
Notice:

1. Although jimmer-sql uses some JPA annotations, jimmer-sql is not a JPA implementation, it has its own API. jimmer-sql and JPA are two completely different things.

2. The annotation `@Key` in the code is `org.babyfish.jimmer.sql.Key`, which is not a JPA annotation, but a jimmer-sql annotation. This annotation will be explained in subsequent articles. Here, the reader is asked to ignore it.
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
    UUID id();

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
    UUID id();

    @Key
    String firstName();

    @Key
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
```

A new type `Gender` is used in the interface `Author`, `Gender` is an enum type defined as follows

```java title="Gender.java"
package org.babyfish.jimmer.sql.example.model;

public enum Gender {
    MALE,
    FEMALE
}
```

## Create SqlClient

SqlClient is the entry point of all APIs of jimmer-sql, therefore, SqlClient needs to be created first.

```java
package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.example.model.Gender;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import org.babyfish.jimmer.sql.example.model.Gender;
import javax.sql.DataSource;

DataSource dataSource = ...;

SqlClient sqlClient = SqlClient
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
This example only let jimmer-sql can automatically create connections based on the connection pool.

For the topic of cooperation with Spring transaction mechanism, please refer to [SqlClient](./sql-client).
:::

## Query

### Basic query

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

The final generated SQL is as follows

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

### Use table join

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

The final generated SQL is as follows

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

### Sub query

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

The final generated SQL is as follows

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

### Group query
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

The final generated SQL is as follows

```sql
select 
    tb_1_.STORE_ID, 
    /* highlight-next-line */
    avg(tb_1_.PRICE) 
from BOOK as tb_1_ 
/* highlight-next-line */
group by tb_1_.STORE_ID
```

### Pagination query
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

Two SQL statements are genearted

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

### Hybrid Native SQL
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

The final generated SQL is as follows

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

### Use object fetcher
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

Three SQL statements are genearted

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

The first SQL statement is used to query the `Book` entity itself; the other two SQL statements are responsible for querying the associated objects, which are executed after the main query.

## Mutation

jimmer-sql provides two ways to modify the database, statement and command

- Statement: You can execute update and delete, which is suitable for occasions where the logic is simple but batch operations are required.
- command: can execute insert, update and delete, which is suitable for situations with complex logic.

### Use statement

#### update statement

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

The final generated SQL is as follows

```sql
update BOOK tb_1_ 
set PRICE = tb_1_.PRICE + ? 
where tb_1_.NAME like ?
```

#### delete statement

```java
int affectedRowCount = sqlClient
    .createDelete(BookTable.class, (d, book) -> {
        d.where(book.name().like("Java"));
    })
    .execute();
System.out.println("Affected row count: " + affectedRowCount);
```

The final generated SQL is as follows

```sql
delete from BOOK as tb_1_ where tb_1_.NAME like ?
```

### Use command

#### Save command

Due to the jimmer object is dynamic, entity objects can represent a variety of information, such as:

- Partial object
- Complete object
- Shallower object tree
- Deeper object tree

In any case, you can use the save command to insert, update, or save it with a single line of code (the so-called save, that is insert or update).

In order to give a perceptual cognition and preliminary understanding, this article demonstrates two examples, saving a single object and saving a object tree.

##### 1. Save single object

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

The actual execution logic of this command depends on the existing data in the database. Assuming that there is no book whose `name` is "BookName" and `edition` is 1, the following SQL statements are generated.

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

##### 2. Save object tree
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

The actual execution logic of this command depends on the existing data in the database. Assuming that all objects in the object tree do not exist in the database, the following SQL statements are generated.

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

#### Delete command

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

The final generated SQL is as follows

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

The print result is as follows

```
Affected row count: 9
Affected row count of table 'BOOK': 4
Affected row count of middle table 'BOOK_AUTHOR_MAPPING': 5
```
