---
sidebar_position: 4
title: Table and TableEx
---

## Problem of collection association

Let's look at this entity definition

```java title="Book.java"

package org.babyfish.jimmer.sql.example.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface Book {

    @Id
    UUID id();

    String name();

    int edition();

    BigDecimal price();

    @ManyToOne
    // highlight-next-line
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumns = @JoinColumn(name = "BOOK_ID"),
            inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID")
    )
    // highlight-next-line
    List<Author> authors();
}
```

in the code above
- `store` is a many-to-one association
- `authors` is a many-to-many association

One-to-one associations and many-to-one associations are collectively referred to as reference associations, one-to-many associations and many-to-many associations are collectively referred to as collection associations.

The differences between reference associations and collection associations are as follows

- Join query base on reference associations dose not cause duplicate results.
- Join query base on collection associations causes duplicate results.

Table joins based on collection associations can cause duplicate results, which in turn can cause the following problems:

1. If developers forget to deduplicate, bugs will be introduced.

2. Even if the developer does not forget to deduplicate, it is not a good choice. Developers usually choose to use `java.util.LinkedHashSet` for deduplication on the client side. This practice consumes hardware resources to process duplicate data and puts unnecessary pressure on both network and JVM memory.

3. **The most important point, it is not friendly to pagination queries**.

    Pagination query of join results at the SQL level is often not what people need; in more scenarios, people want pagination operations to be applied to the main object *(The aggregate root) *.

    Take Hibernate as an example. In this case, Hibernate has to give up the paging strategy at the SQL level and adopt the paging strategy at the memory level. This is very low performance, in order to attract the attention of developers, hibernate will warn in the log. If you has experience using Hibernate, the following log will give you a headache.

    [firstResult/maxResults specified with collection fetch; applying in memory](https://tech.asimio.net/2021/05/19/Fixing-Hibernate-HHH000104-firstResult-maxResults-warning-using-Spring-Data-JPA.html)

    In fact, jimmer-sql provides a very powerful associated object fetch function, [Object Fetcher](./query/fetcher). And it is for this reason that Object Fetcher does not use table joins.

To sum up, the disadvantages of using collection joins in top-level queries are very obvious, but there is no denying that there is still objective value in using collection joins in subqueries.

So, jimmer-sql has an important design goal

:::info
1. Table joins based on collection associations need to be prohibited in **top-level** queries.
2. Table joins based on collection associations are still available in subqueries, update statements, and delete statements.
:::

## Table and TableEx

To support a strongly typed DSL, the annotation processor generates some source code based on user-defined entity interfaces.

Taking the Book entity interface at the beginning of the article as an example, the following two types will be automatically generated

```java title="BookTable.java(Auto-generated)"
package org.babyfish.jimmer.sql.example.model;

import java.lang.Integer;
import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.criteria.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;

public class BookTable extends AbstractTableWrapper<Book> {
    public BookTable(Table<Book> table) {
        super(table);
    }

    public Expression<UUID> id() {
        return get("id");
    }

    public PropExpression.Str name() {
        return get("name");
    }

    public PropExpression.Num<Integer> edition() {
        return get("edition");
    }

    public PropExpression.Num<BigDecimal> price() {
        return get("price");
    }

    public BookStoreTable store() {
        return join("store");
    }

    public BookStoreTable store(JoinType joinType) {
        return join("store", joinType);
    }
}
```

```java title="BookTableEx.java(Auto-generated)"
package org.babyfish.jimmer.sql.example.model;

import javax.persistence.criteria.JoinType;
import org.babyfish.jimmer.sql.ast.table.TableEx;

public class BookTableEx extends BookTable implements TableEx<Book> {
    public BookTableEx(TableEx<Book> table) {
        super(table);
    }

    public AuthorTableEx authors() {
        return join("authors");
    }

    public AuthorTableEx authors(JoinType joinType) {
        return join("authors", joinType);
    }
}
```

Looking at these two auto-generated types, it can be seen that

- `BookTableEx` extends `BookTable`.
- `BookTable` does not support collection associations, but supports scalar property and reference associations (`store` in this case).
- `BookTableEx` adds support for collection associations (`authors` in this case).

Therefore, the API of jimmer-sql follows the following pattern
- Top-level queries can only be created based on `Table`.
- Subqueries, update statements and delete statements can be created based on either `Table` or `TableEx`.

Next, take the top-level query and sub-query as example for a comparative demonstration.

### Top-level queries that can only be created based on Table

Top-level queries that can only be created based on Table
```java
sqlClient.createQuery(BookTable.class, (q, book) -> ...);
```

If it takes TableEx as its parameter,
```java
sqlClient.createQuery(BookTableEx.class, (q, book) -> ...);
```

the exception will be thrown
:::caution
Top-level query does not support TableEx
:::

```java
List<Book> books = sqlClient
    .createQuery(BookTable.class, (q, book) -> {
        return q
            .where(
                book
                    // highlight-next-line
                    .name() // scalar field "name"
                    .eq("Book Name")
            )
            .where(
                book
                    // highlight-next-line
                    .store() // reference association "store"
                    .name()
                    .eq("Store Name")
            )
            /*
              * However, "book.authors()" cannot be used 
              * because the method "authors()" is defined 
              * in BookTableEx, not BookTable.
              *
              * That is, compile-time prohibits users 
              * from joining collection associations 
              * in top-level queries
              */
            .select(book);
    })
    .execute();
books.stream().forEach(System.out::println);
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
inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
where 
    tb_1_.NAME = ? 
and 
    tb_2_.NAME = ?
```
### Allow subqueries created based on TableEx

Unlike top-level queries, TableEx can be used to create subqueries, update statements, and delete statements.

```java
List<Author> authors = sqlClient
    .createQuery(AuthorTable.class, (q, author) -> {
        return q
                .where(author.firstName().eq("Author Name"))
                .where(
                    q.createWildSubQuery(
                        // highlight-next-line
                        BookTableEx.class, // TableEx is allowed
                        (sq, book) -> {
                            sq.where(
                                book.name().eq("Book name"),
                                book
                                    // highlight-next-line
                                    .authors() // collection association
                                    .eq(author)
                            );
                        }
                    ).exists()
                )
                .select(author);
    })
    .execute();
authors.stream().forEach(System.out::println);
```

The final generated SQL is as follows

```sql
select 
    tb_1_.ID, 
    tb_1_.FIRST_NAME, 
    tb_1_.LAST_NAME, 
    tb_1_.GENDER 
from AUTHOR as tb_1_ 
where 
    tb_1_.FIRST_NAME = ? 
    and exists (
        select 1 
        from BOOK as tb_2_ 
        /* highlight-start */
        inner join BOOK_AUTHOR_MAPPING as tb_3_ 
            on tb_2_.ID = tb_3_.BOOK_ID 
        /* highlight-end */
        where 
            tb_2_.NAME = ? 
        and 
            tb_3_.AUTHOR_ID = tb_1_.ID
    )
```

## Break through soft limits

Disallowing collection associations in top-level queries is reasonable in most, but not all cases.

For example, users do not query the entire object, but query individual fields, and use SQL keyword `distinct` to counteract the side effects of table joins base on collection associations. This scenario is completely reasonable.

So, prohibiting the use of collection associations in top-level queries is a soft restriction, not a rigid one. Can easily break through.

There are two ways to break through

1. Use weakly typed API *(not recommended)*

    ```java
    List<UUID> bookIds = sqlClient
        .createQuery(BookTable.class, (q, book) -> {
            return q
                .where(
                    book
                        // highlight-next-line
                        .<AuthorTable>join("authors")
                        .firstName()
                        .ilike("%")
                )
                .select(book.id());
        })
        .distinct()
        .execute();
    bookIds.stream().forEach(System.out::println);
    ```

2. Cast Table to TableEx *(recommended)*

    ```java
    List<UUID> bookIds = sqlClient
        .createQuery(BookTable.class, (q, book) -> {
            return q
                .where(
                    // highlight-next-line
                    ((BookTableEx)book)
                        // highlight-next-line
                        .authors()
                        .firstName()
                        .ilike("%")
                )
                .select(book.id());
        })
        .distinct()
        .execute();
    bookIds.stream().forEach(System.out::println);
    ```

Either way, you can break through this soft limitation and use collection associations in top-level queries.

The final generated SQL is as follows

```sql
select 
    distinct tb_1_.ID 
from BOOK as tb_1_ 
/* highlight-start */
inner join BOOK_AUTHOR_MAPPING as tb_2_ 
    on tb_1_.ID = tb_2_.BOOK_ID 
inner join AUTHOR as tb_3_ 
    on tb_2_.AUTHOR_ID = tb_3_.ID 
/* highlight-end */
where lower(tb_3_.FIRST_NAME) like ?
```

:::info
This limit is added, but it's easy to be broken through. What is the purpose of such a seemingly contradictory approach?

When a developer really wants to do it, make sure he understands what he's doing.
:::