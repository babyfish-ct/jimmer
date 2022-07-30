---
sidebar_position: 5
title: Mutable delete table
---

First, the middle table is hidden by the object model and has no direct corresponding java entity type. This issue has been discussed in the article [Query middle Table](../query/association) and will not be repeated here.

Also, in the [Save command](./save-command) chapter, we introduced the save command. It can compare the existing data in the database with the new data to be saved by the user. If an associated properties based on the middle table changes, the data of middle table will be modified.

It is true that the save command has many functions, including the modifying middle table. However, sometimes, we just need to simply insert or delete the data of middle table, we do not need the powerful functions of the save command, but we want the modification behavior of the middle table to be simple and efficient enough.

jimmer-sql allows developers to directly insert and delete data for middle tables in the simplest and most efficient way.

## Insert assciations

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
    .save(bookId, authorId);
```

The generated SQL is as follows

```sql
insert into 
    BOOK_AUTHOR_MAPPING(
        /* highlight-next-line */
        BOOK_ID, AUTHOR_ID
    ) 
    values (?, ?)
```

This example demonstrates how to use the association property `Book.authors` to manipulate the middle table.

In the same way, we can also achieve the same purpose through the association property `Authors.books`:

```java
UUID bookId = UUID.fromString(
    "780bdf07-05af-48bf-9be9-f8c65236fecc"
);
UUID authorId = UUID.fromString(
    "1e93da94-af84-44f4-82d1-d8a9fd52ea94"
);
sqlClient
    .getAssociations(
        AuthorTableEx.class,
        AuthorTableEx::books
    )
    // highlight-next-line
    .save(authorId, bookId);
```

Note that the parameter order of the `save` method is different compared to the previous example because the direction of the association is different.

The generated SQL is as follows

```sql
insert into 
    BOOK_AUTHOR_MAPPING(
        /* highlight-next-line */
        AUTHOR_ID, BOOK_ID
    )
    values (?, ?)
```

## Inverse transform

For bidirectional associations, you can use `reverse` to switch between positive and negative associations

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

In this code:
- `bookToAuthor` and `bookToAuthor2` are exactly equivalent
- `authorToBook` and `authorToBook2` are exactly equivalent

## Batch insert

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

UUID authorId1 = UUID.fromString(
    "1e93da94-af84-44f4-82d1-d8a9fd52ea94"
);
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

There are 3 books, 2 authors, and 6 combinations in total. The `batchSave` method inserts all these 6 combinations into the middle table, and the generated SQL is as follows

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

The code above is very cumbersome, `3 * 2 = 6` is acceptable, but what about `7 * 9 = 63`? Is it acceptable to build 63 tuples?

jimmer-sql provides a shortcut

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

The `batchSave` method above takes two collection parameters and inserts the Cartesian product of the two collections into the middle table table, so the function is the same as the previous example.

## Check for existence

If you insert an existing id tuple into the middle table, the database will report an error because the uniqueness constraint is violated.

To solve this problem, an existence check can be performed.

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

Here `checkExistence()` means to first check which data to be inserted already exists, and then only insert data that does not exist.

The generated SQL statement is as follows

1. Check which data to be inserted already exists
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

2. Using the query results of the previous step, the data that really needs to be inserted can be calculated. If there is no data to be inserted, skip this step; otherwise, execute this step.

    Here, assuming that there are still two rows that really need to be inserted after the judgment, the generated SQL is:

    ```sql
    insert into BOOK_AUTHOR_MAPPING(
        BOOK_ID, AUTHOR_ID
    ) values 
        (?, ?), 
        (?, ?)
    ```

:::tip
There is an unimplemented optimization due to timing.

In the future, this feature will be improved. Optimized for some specific databases, such as Postgres, MySql.

Taking postgres as an example, you can directly execute
```sql
insert into BOOK_AUTHOR_MAPPING(
    BOOK_ID, AUTHOR_ID
) values (?, ?), (?, ?), (?, ?)
on conflict(BOOK_ID, AUTHOR_ID)
    do nothing;
```
without executing an extra select statement.
:::

## Delete associations

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

The generated SQL is as follows

```sql
delete from BOOK_AUTHOR_MAPPING 
where 
    (BOOK_ID, AUTHOR_ID) in (
        (?, ?)
    )
```

## Batch delete

Similar to batch insert, there are two ways to implement batch delete

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
Or
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

The generated SQL is as follows

```sql
delete from BOOK_AUTHOR_MAPPING 
where 
    (BOOK_ID, AUTHOR_ID) in (
        (?, ?), 
        (?, ?)
    )
```