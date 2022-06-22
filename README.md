
[![logo](logo.png)](https://babyfish-ct.github.io/jimmer-doc/)

# A powerful java framework for immutable data and ORM based on immutable data

## 1. Documentation

The project provides complete documentation.

Please view [**jimmer documentation**](https://babyfish-ct.github.io/jimmer-doc/) to know everything.

## 2. Examples:

This framework provides three examples

- [example/jimmer-core](example/jimmer-core): How to use immutable objects
- [example/jimmer-sql](example/jimmer-sql): How to use ORM framework
- [example/jimmer-sql-graphql](example/jimmer-sql-graphql): How to quickly develop [Spring GraphQL](https://spring.io/projects/spring-graphql) services based on jimmer.

## Introduce

Introduction

This introduction is very simple and introduces the project features with minimal space. For complete features, please refer to the [documentation](https://babyfish-ct.github.io/jimmer-doc/).

Now, there are many ORM frameworks to choose, such as: JPA, myBatis, JOOQ, Exposed, KtOrm. Why develop a whole new ORM?

The answer is to introduce huge improvement, provide some powerful functionalities that cannot be imaged by traditional ORMs.

The traditional ORM uses a simple User Bean as an entity object, and the function of the entity object is very limited.

Taking Hibernate, a well-known implementation of JPA, as an example, in order to support lazy loading for many-to-one associations, a dynamic proxy that inherits from User Bean is created to express the parent object with only the id attribute. Hibernate will expand User Bean because the function of User Bean is not powerful enough.

As this idea continues, as long as we find a way to make User Bean powerful enough, we can develop a powerful ORM based on it.

### 1. Make User Bean powerful enough

#### 1.1  Use immutable data, but support temporary mutable proxies.

```java
@Immutable
public interface TreeNode {
    String name();
    TreeNode parent();
    List<TreeNode> childNodes();
}
```
The annotation processor supporting the framework will generate a mutable derived interface for the user: `TreeNodeDraft`. User can use it like this

```java
// Step1: Create object from scratch
TreeNode oldTreeNode = TreeNodeDraft.$.produce(root ->  
    root
        .setName("Root")
        .addIntoChildNodes(child ->
            child.setName("Drinks")        
        )
        .addIntoChildNodes(child ->
            child.setName("Breads")        
        )
);

// Step2: Create object based on existing object
TreeNode newTreeNode = TreeNodeDraft.$.produce(
    oldTreeNode, // existing object
    root ->
      root.childNodes(false).get(0) // Get child proxy
          .setName("Dranks+"); // Change child proxy
);

System.out.println("Old tree node: ");
System.out.println(oldTreeNode);

System.out.println("New tree node: ");
System.out.println(newTreeNode);
```

The final print result is as follows

```
Old tree node: 
{"name": "Root", childNodes: [{"name": "Drinks"}, {"name": "Breads"}]}
New tree node: 
{"name": "Root", childNodes: [{"name": "`Drinks+`"}, {"name": "Breads"}]}
```
#### 1.2 Dynamic object.

Any property of the data object can be unspecified.

1. Direct access to unspecified properties causes an exception.
2. Using Jackson serialization, Unspecified properties will be ignored without exception.

```java
TreeNode current = TreeNodeDraft.$.produce(current ->
    node
        .setName("Current")
        .setParent(parent -> parent.setName("Father"))
        .addIntoChildNodes(child -> child.setName("Son"))
);

// You can access specified properties
System.out.println(current.name());
System.out.println(current.parent());
System.out.println(current.childNodes());

/*
 * But you cannot access unspecified fields, like this
 *
 * System.out.println(current.parent().parent());
 * System.out.println(
 *     current.childNodes().get(0).childNodes()
 * );
 *
 * , because direct access to unspecified 
 * properties causes an exception.
 */

/*
 * Finally You will get JSON string like this
 * 
 * {
 *     "name": "Current", 
 *     parent: {"name": "Father"},
 *     childNodes:[
 *         {"name": "Son"}
 *     ]
 * }
 *
 * , because unspecified will be ignored by 
 * jackson serialization without exception.
 */
String json = new ObjectMapper()
    .registerModule(new ImmutableModule())
    .writeValueAsString(current);

System.out.println(json);
```

Because entity objects are dynamic, users can build arbitrarily complex data structures. There are countless possibilities, such as

1.  Lonely object, for example

    ```java
    TreeNode lonelyObject = TreeNodeDraft.$.produce(draft ->
        draft.setName("Lonely object")
    );
    ```

2.  Shallow object tree, for example
    ```java
    TreeNode shallowTree = TreeNodeDraft.$.produce(draft ->
        draft
            .setName("Shallow Tree")
            .setParent(parent -> parent.setName("Father"))
            .addIntoChildNodes(child -> parent.setName("Son"))
    );
    ```

3. Deep object tree, for example
    ```java
    TreeNode deepTree = TreeNodeDraft.$.produce(draft ->
        draft
            .setName("Deep Tree")
            .setParent(parent -> 
                 parent
                     .setName("Father")
                     .setParent(deeperParent ->
                         deeperParent.setName("Grandfather")
                     )
            )
            .addIntoChildNodes(child -> 
                child
                    .setName("Son")
                    .addIntoChildNodes(deeperChild -> 
                        deeperChild.setName("Grandson");
                    )
            )
    );
    ```

> **This object dynamism, which includes countless possibilities, is the fundamental reason why jimmer's ORM can provide more powerful features.**

### 2. ORM base on immutable object.

In jimmer's ORM, entities are also immutable interfaces

```java
@Entity
public interface TreeNode {
    
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "sequence:TREE_NODE_ID_SEQ"
    )
    long id();
    
    @Key // jimmer annotation, `name()` is business key,
    // business key will be used when `id` property is not specified
    String name();

    @ManyToOne
    @OnDelete(DeleteAction.DELETE)
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
```

> Note!
> 
> Although jimmer uses some JPA annotations to complete the mapping between entities and tables, jimmer is not JPA.

#### 2.1 Save arbitrarily complex object tree into database

1.  Save lonely entity
    ```java
    sqlClient.getEntities().save(
        TreeNode lonelyObject = TreeNodeDraft.$.produce(draft ->
            draft
                .setName("RootNode")
                .setParent((TreeNode)null)
        )
    );
    ```

2. Save shallow entity tree

    ```java
    sqlClient.getEntities().save(
        TreeNode lonelyObject = TreeNodeDraft.$.produce(draft ->
            draft
                .setName("RootNode")
                .setParent(parent ->
                    parent.setId(100L)
                )
                .addIntoChildNodes(child ->
                    child.setId(101L)
                )
                .addIntoChildNodes(child ->
                    child.setId(102L)
                )
        )
    );
    ```

3. Save deep entity tree

    ```java
    sqlClient.getEntities().saveCommand(
        TreeNode lonelyObject = TreeNodeDraft.$.produce(draft ->
            draft
                .setName("RootNode")
                .setParent(parent ->
                    parent
                        .setName("Parent")
                        .setParent(grandParent ->
                            grandParent.setName("Grand parent")
                        )
                )
                .addIntoChildNodes(child ->
                    child
                        .setName("Child-1")
                        .addIntoChildNodes(grandChild ->
                            grandChild.setName("Child-1-1")
                        )
                       .addIntoChildNodes(grandChild ->
                            grandChild.setName("Child-1-2")
                        )
                )
                .addIntoChildNodes(child ->
                    child
                        .setName("Child-2")
                        .addIntoChildNodes(grandChild ->
                            grandChild.setName("Child-2-1")
                        )
                        .addIntoChildNodes(grandChild ->
                            grandChild.setName("Child-2-2")
                        )
                )
        )
    ).configure(it ->
        // Auto insert associated objects 
        // if they do not exists in database
        it.setAutoAttachingAll()
    ).execute();
    ```

#### 2.2 Query arbitrarily complex object trees from a database

1.  Select root nodes from database *(`TreeNodeTable` is a java class generated by annotation processor)*

    ```java
    List<TreeNode> rootNodes = sqlClient
        .createQuery(TreeNodeTable.class, (q, treeNode) -> {
            q.where(treeNode.parent().isNull()) // filter roots
            return q.select(treeNode);
        })
        .execute();
    ```

2. Select root nodes and their child nodes from database *(`TreeNodeFetcher` is a java class generated by annotation processor)*

    ```java
    List<TreeNode> rootNodes = sqlClient
        .createQuery(TreeNodeTable.class, (q, treeNode) -> {
            q.where(treeNode.parent().isNull()) // filter roots
            return q.select(
                treeNode.fetch(
                    TreeNodeFetcher.$
                        .allScalarFields()
                        .childNodes(
                            TreeNodeFetcher.$
                                .allScalarFields()
                        )
                )
            );
        })
        .execute();
    ```

3.  Query the root nodes, with two levels of child nodes

    You have two ways to complete a function

    -   Specify a deeper tree format

        ```java
        List<TreeNode> rootNodes = sqlClient
        .createQuery(TreeNodeTable.class, (q, treeNode) -> {
            q.where(treeNode.parent().isNull()) // filter roots
            return q.select(
                treeNode.fetch(
                    TreeNodeFetcher.$
                        .allScalarFields()
                        .childNodes( // level-1 child nodes
                            TreeNodeFetcher.$
                                .allScalarFields()
                                .childNodes( // level-2 child nodes
                                    TreeNodeFetcher.$
                                        .allScalarFields()
                                )
                        )
                )
            );
        })
        .execute();
        ```

    -   You can also specify depth for self-associative property, this is better way

        ```java
        List<TreeNode> rootNodes = sqlClient
        .createQuery(TreeNodeTable.class, (q, treeNode) -> {
            q.where(treeNode.parent().isNull()) // filter roots
            return q.select(
                treeNode.fetch(
                    TreeNodeFetcher.$
                        .allScalarFields()
                        .childNodes(
                            TreeNodeFetcher.$
                                .allScalarFields(),
                            it -> it.depth(2) // Fetch 2 levels
                        )
                )
            );
        })
        .execute();
        ```
4. Query all root nodes, recursively get all child nodes, no matter how deep

    ```java
    List<TreeNode> rootNodes = sqlClient
    .createQuery(TreeNodeTable.class, (q, treeNode) -> {
        q.where(treeNode.parent().isNull()) // filter roots
        return q.select(
            treeNode.fetch(
                TreeNodeFetcher.$
                    .allScalarFields()
                    .childNodes(
                        TreeNodeFetcher.$
                            .allScalarFields(),

                        // Recursively fetch all, 
                        // no matter how deep
                        it -> it.recursive() 
                    )
            )
        );
    })
    .execute();
    ```

5. Query all root nodes, it is up to the developer to control whether each node needs to recursively query child nodes

    ```java
    List<TreeNode> rootNodes = sqlClient
    .createQuery(TreeNodeTable.class, (q, treeNode) -> {
        q.where(treeNode.parent().isNull()) // filter roots
        return q.select(
            treeNode.fetch(
                TreeNodeFetcher.$
                    .allScalarFields()
                    .childNodes(
                        TreeNodeFetcher.$
                            .allScalarFields(),
                        it -> it.recursive(args ->
                            // - If the node name starts with `Tmp_`, 
                            // do not recursively query child nodes.
                            //
                            // - Otherwise, 
                            // recursively query child nodes.
                            !args.getEntity().name().startsWith("Tmp_")
                        )
                    )
            )
        );
    })
    .execute();
    ```

#### 2.3 Dynamic table joins.

In order to develop powerful dynamic queries, it is not enough to support dynamic where predicates, but dynamic table joins are required.

```java
@Repository
public class TreeNodeRepository {
    
    private final SqlClient sqlClient;

    public TreeNodeRepository(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<TreeNode> findTreeNodes(
        @Nullable String name,
        @Nullable String parentName,
        @Nullable String grandParentName
    ) {
        return sqlClient
            .createQuery(TreeNodeTable.class, (q, treeNode) -> {
               if (name != null && !name.isEmpty()) {
                   q.where(treeNode.name().eq(name));
               }
               if (parentName != null && !parentName.isEmpty()) {
                   q.where(
                       treeNode
                       .parent() // Join: current -> parent
                       .name()
                       .eq(parentName)
                   );
               }
               if (grandParentName != null && !grandParentName.isEmpty()) {
                   q.where(
                       treeNode
                           .parent() // Join: current -> parent
                           .parent() // Join: parent -> grand parent
                           .name()
                           .eq(grandParentName)
                   );
               }
               return q.select(treeNode);
            })
            .execute();
    }
}
```

This dynamic query supports three nullable parameters.

1. When the parameter `parentName` is not null, the table join `current -> parent` is required
2. When the parameter `grandParentName` is not null, you need to join `current -> parent -> grandParent`

When the parameters `parentName` and `grandParent` are both specified, the table join paths `current -> parent` and `current -> parent -> grandParent` are both added to the query conditions. Among them, `current->parent` appears twice, jimmer will automatically merge the duplicate table joins. 

This means

```
`current -> parent` 
+ 
`current -> parent -> grandParent` 
= 
--+-current
  |
  \--+-parent
     |
     \----grandParent
```
In the process of merging different table join paths into a join tree, duplicate table joins are removed.

The final SQL is

```sql
select 
    tb_1_.ID, tb_1_.NAME, tb_1_.PARENT_ID
from TREE_NODE as tb_1_

/* Two java joins are merged to one sql join*/
inner join TREE_NODE as tb_2_ 
    on tb_1_.PARENT_ID = tb_2_.ID

inner join TREE_NODE as tb_3_ 
    on tb_2_.PARENT_ID = tb_3_.ID
where
    tb_2_.NAME = ? /* parentName */
and
    tb_3_.NAME = ? /* grandParentName */
```

#### 2.4 Automatically generate count-query by data-query.

Pagination query requires two SQL statements, one for querying the total row count of data, and the other one for querying data in one page, let's call them count-query and data-query. 

Developers only need to focus on data-count, and count-query can be generated automatically.

```java

// Developer create data-query
ConfigurableTypedRootQuery<TreeNodeTable, TreeNode> dataQuery = 
    sqlClient
        .createQuery(TreeNodeTable.class, (q, treeNode) -> {
            q
                .where(treeNode.parent().isNull())
                .orderBy(treeNode.name());
            return q.select(book);
        });

// Framework generates count-query
TypedRootQuery<Long> countQuery = dataQuery
    .reselect((oldQuery, book) ->
        oldQuery.select(book.count())
    )
    .withoutSortingAndPaging();

// Execute count-query
int rowCount = countQuery.execute().get(0).intValue();

// Execute data-query
List<TreeNode> someRootNodes = 
    dataQuery
        // limit(limit, offset), from 1/3 to 2/3
        .limit(rowCount / 3, rowCount / 3)
        .execute();
``` 
