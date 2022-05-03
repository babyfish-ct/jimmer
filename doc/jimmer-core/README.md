# Jimer-core

jimmer-core ported https://github.com/immerjs/immer to Java.

## 1. Unresolved problems of java record.

Java supported record type since 14, which guarantees the immutability of data and automatically generates all behavioral solidification code.

The immutability of objects is very important, no doubt about it!

However, although it is suitable for building tuples with few fields in the project, it will face the following problems when used to replace the heavy entity objects in the project.

1. Entity object usually has a certain complexity and have many fields (such as 50 fields), Use record to implement it will make a constructor with long parameter list. Unlike kotlin and C#, Java supports neither default parameters nor named parameters, when a method has too many parameters, all parameters still need be given in strict order can lead to heavy task and hard to read.

2. In practical work, developers do not always build a brand new immutable object from scratch. Many times we need to create a new mutable object based on an existing immutable object. The values of most of the fields of the new object are the same as those of the old object, and only a few of them have changed.
  
  ```java
  var oldData = ...

  var newData = MyData(

      "NewValueForMethod1",

      oldData.method2(),
      oldData.method3(),
      ... ...
      oldData.methodN()
  );
  ```

  From method2 to methodN, they are values that I don't care about, but I still have to write code to copy them one by one.

  > In the kotlin language, the data class supports a [copy function](https://kotlinlang.org/docs/data-classes.html#copying) to solve this problem. However, kotlin's scheme cannot be used in java language because it supports neither default parameters nor named parameters.


3. It is not easy to create new immutable objects based on existing immutable objects (even kotlin data class that support [copy function](https://kotlinlang.org/docs/data-classes.html#copying) also face this problem)

  Look at this example

  a. Declare tree node

    ```java
    data class Node(
        val name: String,
        val childNodes: List<Node>
    )

    val node = ...blabla...
    ```


4. Entity objects need to be dynamic, not all properties of the object need to be initialized, it allows some properties to be be missing. Note: The missing discussed here is not null value, but unknown.

Taking an ORM as an example, one entity type can navigate to other entity types through an association property (whether one-to-one, many-to-one, one-to-many or many-to-many). If all fields of an object must be specified, then querying an entity object will result in all associated objects being queried recursively and unconditioanlly, which is not acceptable.

If you've ever worked with JPA/Hibernate, you've heard about the concept of lazy loading. Objects allow some properties not to be initialized (not null, but unknown). When these unknown properties are accessed for the first time, if the object still maintains a database connection (common in monolithic applications), the data will be loaded from the database; otherwise (common in distributed applications), an exception will be thrown (such as classic exception of Hibernate: org.hibernate.LazyInitializationException).

Of course, there are many technical solutions in the field of data access, not limited to JPA/Hibernate, so not all readers have used JPA/Hibernate. However, the people who have used JPA/Hibernate in the past should be the most, so I still use this example to illustrate the point: entity objects need to be dynamic, not all properties need to be assigned. Unassigned fields cause exceptions when accessed directly, and are automatically ignored in JSON serialization.
