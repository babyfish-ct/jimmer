---
sidebar_position: 2
title: Delete statement
---

## Basic usage

Delete statement usage is as follows

```java
int affectedRowCount = sqlClient
    .createDelete(BookTable.class, (d, book) -> {
        d.where(book.name().eq("Learning GraphQL"));
    })
    .execute();
System.out.println("Affected row count: " + affectedRowCount);
```

The generated SQL is as follows:
```sql
delete 
from BOOK as tb_1_ 
where tb_1_.NAME = ?
```

## Use table joins

The Delete statement supports table joins, like this

```java
int affectedRowCount = sqlClient
    .createDelete(BookTable.class, (d, book) -> {
        d.where(book.store().name().eq("MANNING"));
    })
    .execute();
System.out.println("Affected row count: " + affectedRowCount);
```

Finally, three SQL statements are generated:

1. 
    ```sql
    select 
        distinct tb_1_.ID 
    from BOOK as tb_1_ 
    inner join BOOK_STORE as tb_2_ 
        on tb_1_.STORE_ID = tb_2_.ID 
    where 
        tb_2_.NAME = ?
    ```

2. 
    ```sql
    delete from BOOK_AUTHOR_MAPPING 
    where BOOK_ID in(?, ?, ?)
    ```
3. 
    ```sql
    delete from BOOK 
    where ID in(?, ?, ?)
    ```

:::note
If you use table join in a delete statement, jimmer-sql will translate it to `select` + `delete`. First use the `select` statement with the table joins to query the id of the data to be deleted, and then use the [Delete command](./delete-command) to delete the data.

This scheme is valid for any database.
:::