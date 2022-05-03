# Jimer-core

jimmer-core ported https://github.com/immerjs/immer to Java.

## 1. Unresolved problems of java record.

Java supported record type since 14, which guarantees the immutability of data and automatically generates all behavioral solidification code.

The immutability of objects is very important, no doubt about it!

However, although it is suitable for building tuples with few fields in the project, it will face the following problems when used to replace the heavy entity objects in the project.

1.  Entity object usually has a certain complexity and have many fields (such as 50 fields), Use record to implement it will make a constructor with long parameter list. Unlike kotlin and C#, Java supports neither default parameters nor named parameters, when a method has too many parameters, all parameters still need be given in strict order can lead to heavy task and hard to read.

2.  In practical work, developers do not always build a brand new immutable object from scratch. Many times we need to create a new mutable object based on an existing immutable object. The values of most of the fields of the new object are the same as those of the old object, and only a few of them have changed.

    ```java
    var oldData = ...

    var newData = new MyData(

        "NewValueForMethod1",

        oldData.method2(), 
        oldData.method3(),
        ... ...
        oldData.methodN()
    );
    ```  
    From method2 to methodN, they are values that I don't care about, but I still have to write code to copy them one by one.

    > In the kotlin language, the data class supports a [copy function](https://kotlinlang.org/docs/data-classes.html#copying) to solve this problem. However, kotlin's scheme cannot be used in java language because it supports neither default parameters nor named parameters.


3.  It is not easy to create new immutable objects based on existing immutable objects (even kotlin data class that support [copy function](https://kotlinlang.org/docs/data-classes.html#copying) also face this problem)

    Declare tree node type

    ```java
    record Node(String name, List<Node> childNodes) {}
    ```

    Prepare a node variable
    ```java
    var oldNode = ...blabla...
    ```

    1.  Change the name of the root object to "Hello"
        ```java
        var newNode = new TreeNode(
            "Hello", // Set name of root node
            oldNode.childNodes()
        );
        ```

    2.  Change the name of a first-level object to "Hello"
   
        Breadcrumbs condition as follows
        - first-level object position: pos1
   
        ```java
        var newNode = new TreeNode(
            oldNode.name(),
            IntStream
                .range(0, oldNode.childNodes().size())
                .mapToObj(index1 -> {
                    if (index1 != pos1) {
                        return oldNode.childNodes().get(index1);
                    }    
                    return new TreeNode(
                            "Hello", // Set name of level-1 node
                            oldNode.childNodes().get(index1).childNodes()
                    );
                })
                .toList()        
        );
        ```

    3.  Change the name of a second-level object to "Hello"

        Breadcrumbs condition as follows
        - first-level object position: pos1
        - second-level object position: pos2

        ```java
        var newNode = new TreeNode(
                oldNode.name(),
                IntStream
                    .range(0, oldNode.childNodes().size())
                    .mapToObj(index1 -> {
                        if (index1 != pos1) {
                            return oldNode.childNodes().get(index1);
                        }
                        return new TreeNode(
                            oldNode.name(),
                            IntStream
                                    .range(0, oldNode.childNodes().get(index1).childNodes().size())
                                    .mapToObj(index2 -> {
                                        if (index2 != pos2) {
                                            return oldNode.childNodes().get(index1).childNodes().get(index2);
                                        } else {
                                            return new TreeNode(
                                                    "Hello", // Set name of level-2 node
                                                    oldNode
                                                        .childNodes().get(index1)
                                                        .childNodes().get(index2)
                                                        .childNodes()
                                            );
                                        }
                                    })
                                    .toList()
                        );
                    })
                    .toList()
        );
        ```
    Thus, as long as the object tree has a little depth, creating new immutable object based on another immutable object will be a nightmare task.

4.  Entity objects need to be dynamic, not all properties of the object need to be initialized, it allows some properties to be be missing. Note: The missing discussed here is not null value, but unknown.

    Taking an ORM as an example, one entity type can navigate to other entity types through an association property (whether one-to-one, many-to-one, one-to-many or many-to-many). If all fields of an object must be specified, then querying an entity object will result in all associated objects being queried recursively and unconditioanlly, which is not acceptable.

    If you've ever worked with JPA/Hibernate, you've heard about the concept of lazy loading. Objects allow some properties not to be initialized (not null, but unknown). When these unknown properties are accessed for the first time, if the object still maintains a database connection (common in monolithic applications), the data will be loaded from the database; otherwise (common in distributed applications), an exception will be thrown (such as classic exception of Hibernate: org.hibernate.LazyInitializationException).

    Of course, there are many technical solutions in the field of data access, not limited to JPA/Hibernate, so not all readers have used JPA/Hibernate. However, the people who have used JPA/Hibernate in the past should be the most, so I still use this example to illustrate the point: entity objects need to be dynamic, not all properties need to be assigned. Unassigned fields cause exceptions when accessed directly, and are automatically ignored in JSON serialization.
      
## 2. Is it possible to make immutable objects powerful enough to solve all of the above problems?

Sure!

In the field of JavaScript/TypeScript, there is a well-known open source project [immer](https://github.com/immerjs/immer), which can solve the first three points of the above problems.

> It's winner of the "Breakthrough of the year" React open source award and "Most impactful contribution" JavaScript open source award in 2019

Jimmer-core ported it to Java, let's start

1.  Create project and add gradle dependencies
    ```grovvy
    dependencies {
      implementation 'org.babyfish.jimmer:jimmer-core:0.0.2'
      annotationProcessor 'org.babyfish.jimmer:jimmer-apt:0.0.2'
    }
    ```
    
2.  Define your immutable interfaces
    - BookStore.java
      ```java
      package yourpackage;
      
      import org.babyfish.jimmer.Immutable;
      import java.util.List;
      
      @Immutable
      public interface BookStore {
          String name();
          List<Book> books(); // one-to-many
      }
      ```
    - Book.java
      ```java
      package yourpackage;
      
      import org.babyfish.jimmer.Immutable;
      import java.util.List;
      
      @Immutable
      public interface Book {
          String name();
          BookStore store(); // many-to-one
          List<Author> authors(); // many-to-many
      }
      ```
    - Author.java
      ```java
      package yourpackage;
      
      import org.babyfish.jimmer.Immutable;
      import java.util.List;
      
      @Immutable
      public interface Author {
          String name();
          List<Book> books(); // many-to-many
      }
      ```
      
    > When defining these interfaces, you can either follow the naming convention of java record, with properties not starting with "get" (as in the example here), or follow the naming convention of traditional Java Beans, with properties starting with "get".
    > 
    > Regardless of the choice, everyone on the team should be on the same page.
    
    After the Annotation processor completes, 3 new interfaces are automatically generated: **BookStoreDraft**, **BookDraft** and **AuthorDraft**. All the magic is in these three auto-generated interfaces.
     
3.  Create a completely new object from scratch

    ```java
    Book book = BookDraft.$.produce(b -> {
        b.setName("book");
        b.setStore(s -> {
            s.setName("parent");
        });
        b.addIntoAuthors(a -> {
            a.setName("child-1");
        });
        b.addIntoAuthors(a -> {
            a.setName("child-2");
        });
    });
    System.out.println(book);
    ```
    
    The result is
    ```
    {"name":"book","store":{"name":"parent"},"authors":[{"name":"child-1"},{"name":"child-2"}]}
    ```
    > Properties not found in the output (eg: *BookStore.books*, *Author.books*) are unloaded properties, they are automatically ignored in JSON.

4. Create new object base on old object(Core value of immer[https://github.com/immerjs/immer] and jimmer-core)

    ```java
    Book book2 = BookDraft.$.produce(book, b -> { // "book" is the old object
        b.setName(b.name() + "!");
        b.store().setName(b.store().name() + "!");
        for (AuthorDraft author : b.authors(true)) {
            author.setName(author.name() + "!");
        }
    });
    ```
    
     The result is
    ```
    {"name":"book!","store":{"name":"parent!"},"authors":[{"name":"child-1!"},{"name":"child-2!"}]}
    ```
