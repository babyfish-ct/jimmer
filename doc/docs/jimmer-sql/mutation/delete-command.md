---
sidebar_position: 4
title: Delete Command
---

## Basic usage

The delete command is a relatively simple concept.

```java
Collection<UUID> ids = Arrays.asList(
    UUID.fromString(
        "e110c564-23cc-4811-9e81-d587a13db634"
    ),
    UUID.fromString(
        "8f30bc8a-49f9-481d-beca-5fe2d147c831"
    ),
    UUID.fromString(
        "914c8595-35cb-4f67-bbc7-8029e9e6245a"
    ),
    UUID.fromString(
        "a62f7aa3-9490-4612-98b5-98aae0e77120"
    )
);

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

## Delete parent object of many-to-one association

As can be seen from the above discussion, the delete command may cause the deletion of the data in the many-to-many association based on middle table, which is a relatively simple case.

For one-to-one or one-to-many associations based directly on foreign keys, the situation is a bit more complicated.

```java
UUID storeId = UUID.fromString(
    "d38c10da-6be8-4924-b9b9-5e81899612a0"
);

DeleteResult result = sqlClient
    .getEntities()
    .delete(BookStore.class, storeId);

System.out.println(

    "Affected row count: " +
            result.getTotalAffectedRowCount() +

            "\nAffected row count of table 'BOOK_STORE': " +
            result.getAffectedRowCount(AffectedTable.of(BookStore.class)) +

            "\nAffected row count of table 'BOOK': " +
            result.getAffectedRowCount(AffectedTable.of(Book.class)) +

            "\nAffected row count of middle table 'BOOK_AUTHOR_MAPPING': " +
            result.getAffectedRowCount(
                    AffectedTable.of(BookTableEx.class, BookTableEx::authors)
            )
);
```

This code deletes a `BookStore` object.

Since the `BookStore` object has a one-to-many association `BookStore.books`, if the deleted object already has some associated objects in the database, jimmer-sql will discard these objects.

The one-to-many association `BookStore.books` is not based on the middle table, but based on the foreign key. How will jimmer-sql discard these `Book` objects?

Unlike JPA, jimmer-sql does not allow direct use of `@OneToOne` and `@OneToMany` for association mapping, `@OneToOne` and `@OneToMany` must use the `mappedBy` attribute. See [@OneToMany](../mapping#javaxpersistanceonetomany) for more information.
Unlike JPA, jimmer-sql does not allow direct use of `@OneToOne` and `@OneToMany` for association mapping, `@OneToOne` and `@OneToMany` must use the `mappedBy` attribute. See [@OneToMany](../mapping#javaxpersistanceonetomany) for more information.

This means, through the one-to-many association `BookStore.books`, jimmer-sql can definitely find the corresponding many-to-one association `Book.store`.

Next, jimmer-sql will look at the annotation [@OnDelete](../mapping#orgbabyfishjimmersqlondelete) on the many-to-one association property `Book.store`.

1. If the foreign key of `Book.store` is configured as `SET_NULL` by the annotated [@OnDelete](../mapping#orgbabyfishjimmersqlondelete), then execute the following SQL

    ```sql
    update BOOK set STORE_ID = null where STORE_ID in(?)
    ```
    The parameter is the id of the deleted object. Thus, the foreign keys of these discarded objects are set to null.

2. Otherwise, first, execute

    ```sql
    select ID from BOOK where STORE_ID in(?)
    ```
    The parameter is the id of the deleted object. In this way, the ids of these discarded objects are obtained.

    > If the query returns no data, ignore the next steps.

    - If the foreign key of `Book.store` is configured as `CASCADE` by the annotated [@OnDelete](../mapping#orgbabyfishjimmersqlondelete), Use the new delete command to delete these discarded objects, which is actually the automatic recursive execution capability of the delete command.

    - Otherwise, throw an exception.

All of the situations discussed above require the developer to use the annotation [@OnDelete](../mapping#orgbabyfishjimmersqlondelete) on the `Book.store` property.

However, you can also dynamically specify the `deleteAction` configuration for the delete command insteading of using the [@OnDelete](../mapping#orgbabyfishjimmersqlondelete) annotation.

```java
UUID storeId = ...;
DeleteResult result = sqlClient
    .getEntities()
    .deleteCommand(BookStore.class, storeId)
    .configure(it ->
            it
                // highlight-next-line
                .setDeleteAction(
                    BookTable.class,
                    BookTable::store,
                    DeleteAction.SET_NULL
                )
    )
    .execute();
```

Here, calling `setDeleteAction` of delete command has the same effect as statically using the annotation [@OnDelete](../mapping#orgbabyfishjimmersqlondelete).

:::info
1. If the last parameter of the `setDeleteAction` method is `DeleteAction.SET_NULL`, the association property must be nullable, otherwise an exception will be thrown.

2. If the delete rule is not only dynamically configured for the save command, but also statically configured in the entity interface through the annotation [@OnDelete](../mapping#orgbabyfishjimmersqlondelete), the dynamic configuration takes precedence.
:::





