---
sidebar_position: 9
title: 对Spring GraphQL的支持
---

Spring Boot 2.7.0带来了Spring GraphQL，jimmer-sql为了提供了专门的API，加快Spring GraphQL的开发。

## 查询 

对于Query类型的查询，即根查询，GraphQL实现和REST实现没有区别，无需特殊之处。

关键点在于对象之间的关联查询。本身是个简单的概念，但是为了性能，实际项目通常会采用`DataLoader`，`DataLoader`对开发体验产生了较大破坏。

:::note
对象之间的关联查询和`DataLoader`增加了GraphQL服务的开发难度，但也正因为如此，在客户端看来，GraphQL服务很强大。
:::

为了缓解`DataLoader`对开发体验的破坏，Spring GraphQL引入了新注解[@BatchMapping](https://docs.spring.io/spring-graphql/docs/current/reference/html/#controllers-batch-mapping)。

jimmer-sql对此提供了特别支持，提供专用的API，让开发人员可以一句话实现Spring GraphQL的[@BatchMapping](https://docs.spring.io/spring-graphql/docs/current/reference/html/#controllers-batch-mapping)方法。

与此相关的API

- SqlClient.getReferenceLoader
- SqlClient.getListLoader

### SqlClient.getReferenceLoader

此API用于快速实现一对一或多对一关联

```java title="BookController.java"
@Controller
public class BookController {

    private final SqlClient sqlClient;

    public BookController(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    // Many-to-one associaton: Book.store
    // highlight-next-line
    @BatchMapping
    public Map<Book, BookStore> store(
        Collection<Book> books
    ) {
        return sqlClient
            // highlight-next-line
            .getReferenceLoader(
                    BookTable.class,
                    BookTable::store
            )
            .batchLoad(books);
    }
}
```

### SqlClient.getListLoader

此API用于快速实现一对多或多对多关联

```java title="BookStoreController.java"
@Controller
public class BookStoreController {

    private final SqlClient sqlClient;

    public BookStoreController(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    // One-to-many associaton: BookStore.books
    // highlight-next-line
    @BatchMapping
    public Map<BookStore, List<Book>> books(
            List<BookStore> bookStores
    ) {
        return sqlClient
            // highlight-next-line
            .getListLoader(
                BookStoreTableEx.class,
                BookStoreTableEx::books
            )
            .batchLoad(bookStores);
    }
}
```

```java title="BookController.java"
@Controller
public class BookController {

    private final SqlClient sqlClient;

    public BookController(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    // Many-to-many associaton: Book.authors
    // highlight-next-line
    @BatchMapping
    public Map<Book, List<Author>> authors(List<Book> books) {
        return sqlClient
            // highlight-next-line
            .getListLoader(
                BookTableEx.class,
                BookTableEx::authors
            )
            .batchLoad(books);
    }
}
```

```java title="AuthorController.java"
@Controller
public class AuthorController {

    private final SqlClient sqlClient;

    public AuthorController(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    // Many-to-many associaton: Author.books
    // highlight-next-line
    @BatchMapping
    public Map<Author, List<Book>> books(
            List<Author> authors
    ) {
        return sqlClient
                // highlight-next-line
                .getListLoader(
                        AuthorTableEx.class,
                        AuthorTableEx::books
                )
                .batchLoad(authors);
    }
}
```

## 变更

### GraphQL Input类型的存在价值

在介绍变更功能之前，需要讨论GraphQL协议为什么要引入Input类型。

从输出的角度来看，GraphQL字段返回`Object`类型。 但是，从输入的角度来看，GraphQL字段的参数不接受`Object`类型，只能接受标量类型、`Input`类型以及它们的集合类型。

二者之间存在如下差异：

- `Object`类型是<b>动态</b>的，客户端可以随意定义对象的形状。

    `Object`类型的动态性实现了GraphQL的核心价值。因为这种动态性，客户端可以指定哪些字段需要，哪些不需要，从而灵活地控制要查询的对象树格式。

- `Input`类型是<b>静态</b>的，客户端必须提供严格满足服务器要求的参数。

    和查询不同，变更业务往往对输入数据格式有严格的限制，如果客户端随意传递不符合服务端期望的数据格式，很可能导致变更业务异常。

    因此，GraphQL协议引入`Input`类型，该类型是静态的，客户端必须传递严格符合`Input`类型定义的数据格式，才可以调用变更业务。

这个动态静态的差异，就是`Input`类型存在的根本原因。

### 定义Input类型

首先，我们需要在Spring GraphQL约定的文件`src/main/resources/graphql/schema.graphqls`中定义input类型

```graphql
input BookInput {
    id: Long
    name: String!
    edition: Int
    price: BigDecimal!
    storeId: Long
    authorIds: [Long!]!
}
```

然后，在Java文件中，定义对应的BookInput类型

```java title="BookInput.java"
public class BookInput {

    @Nullable
    private final Long id;

    private final String name;

    private final int edition;

    private final BigDecimal price;

    @Nullable
    private final Long storeId;

    private final List<Long> authorIds;

    public BookInput(
        @Nullable Long id,
        String name,
        int edition,
        BigDecimal price,
        @Nullable Long storeId,
        List<Long> authorIds
    ) {
        this.id = id;
        this.name = name;
        this.edition = edition;
        this.price = price;
        this.storeId = storeId;
        this.authorIds = authorIds;
    }

    // Convert static input object
    // to dynamic entity object
    // highlight-next-line
    public Book toBook() {
        return BookDraft.$.produce(book -> {
            if (id != null) {
                book.setId(id);
            }
            if (storeId != null) {
                book.setStore(
                    store -> store.setId(storeId)
                );
            }
            book
                .setName(name)
                .setEdition(edition)
                .setPrice(price);
            for (Long authorId : authorIds) {
                book.addIntoAuthors(
                    author -> author.setId(authorId)
                );
            }
        });
    }
}
```

:::info
1. jimmer-sql的[Save指令](./mutation/save-command)提供把任意复杂的对象树保存到数据库的功能，因此，jimmer-sql关注的是实体对象树，而非input对象。所以，我们需要提供`toBook`方法，把静态的`BookInput`对象转换为动态的`Book`对象。

2. `Book`对象是jimmer-core不可变对象，本身具备动态性，即，`Book`的数据格式千变万化，包罗万象。所以，无论如何定义`BookInput`类型，以及`BookInput`是否存在较深数据镶套，都可以将之转换为`Book`类型。根本不存在`BookInput`和`Book`因格式不兼容而无法转换的问题。

3. `BookInput`类型存在的价值是为了符合GraphQL协议，对客户端传递的输入数据进行格式约束。但对jimmer-sql而言，`BookInput`类型的唯一价值就是创建`Book`对象。所以，除了`toBook`方法外，`BookInput`类没有定义任何其它方法，甚至连getter方法都没有，因为不需要（当然，如果想配合调试器的展示功能，你可以为其定义一个`toString`方法）。
:::

### 实现变更业务

现在我们知道

1. jimmer-sql的[Save指令](./mutation/save-command)，允许开发使人使用一句话，把任意复杂的实体对象树保存到数据库。

2. 上面定义的`BookInput`类型，可以通过其`toBook`方法，转换为`Book`类型的实体对象树。

那么，数据变更业务的实现就很简单了。

```java
@MutationMapping
@Transactional
public Book saveBook(@Argument BookInput input) {
    return sqlClient
        .getEntities()
        .save(
            // highlight-next-line
            input.toBook()
        )
        .getModifiedEntity();
}
```