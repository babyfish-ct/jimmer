---
sidebar_position: 4
title: Dynamic Object
---

## Jimmer and Jackson

jimmer immutable objects are dynamic, not all properties of the object need to be initialized, it allows some properties to be missing.

- Unspecified properties cause exceptions when accessed directly by code
- Unspecified properties are automatically ignored in JSON serialization without exception.

JSON serialization mentioned here is [jackson](https://github.com/FasterXML/jackson).

jimmer-core defines a jackson module: `org.babyfish.jimmer.jackson.ImmutableModule`, which can be used to add the ability to serialize/deserialize jimmer immutable objects for jackson.

```java
ObjectMapper mapper = new ObjectMapper()
    // highlight-next-line
    .registerModule(new ImmutableModule());

TreeNode treeNode = TreeNodeDraft.$.produce(
    draft -> draft.setName("Root Node")
);

// Serialization
String json = mapper.writeValueAsString(treeNode);

// Deserialization
TreeNode deserializedTreeNode = 
    mapper.readValue(json, TreeNode);
```

For serialization operations, there is a convenienter way, which is the `toString` method of the jimmer object.

```java
TreeNode treeNode = TreeNodeDraft.$.produce(
    draft -> draft.setName("Root Node")
);
String json = 
    // Shortcut for JSON serialization
    // highlight-next-line
    treeNode.toString()
System.out.println(json);
```

:::info
Since the code to call the `toString` method is simple, the rest of the examples in this article use it instead of calling jackson's API.
:::

## Demonstration

Unspecified properties are ignored in JSON serialization, however, accessing directly from code will throw an exception: `org.babyfish.jimmer.UnloadedException`.

:::note
This behavior of accessing unspecified properties throws exception, similar to ORM frameworks, for example: `org.hibernate.LazyInitializationException`.
:::

### Demonstration 1: Single Object

```java
TreeNode treeNode = TreeNodeDraft.$
    .produce(current -> 
        current.setName("Current Node")
    );

System.out.println(
    "JSON serialization: " + treeNode
);

System.out.println(
    "this.name(): " + treeNode.name()
);

try {
    System.out.println(
        "this.parent(): " + treeNode.parent()
    );
} catch (UnloadedException ex) {
    System.out.println(
        "UnloadedException message: " +
            ex.getMessage()
    );
}

try {
    System.out.println(
        "this.childNodes(): " + treeNode.childNodes()
    );
} catch (UnloadedException ex) {
    System.out.println(
        "UnloadedException message: " +
            ex.getMessage()
    );
}
```

:::note
The print result is as follows

---

JSON serialization: {"name":"Current Node"}

this.name(): Current Node

UnloadedException message: The property "yourpackage.TreeNode.parent" is unloaded

UnloadedException message: The property "yourpackage.TreeNode.childNodes" is unloaded
:::

### Demonstration 2: Multiple Objects

```java
TreeNode treeNode = TreeNodeDraft.$
    .produce(current ->
        current
            .setName("Current Node")
            .setParent(parent ->
                    parent.setName("Parent Node")
            )
            .addIntoChildNodes(child ->
                    child.setName("Child Node")
            )
    );


System.out.println(
    "JSON serialization: " + treeNode
);

System.out.println(
    "this.name(): " + treeNode.name()
);

System.out.println(
    "this.parent(): " + treeNode.parent()
);

System.out.println(
    "this.childNodes(): " + treeNode.childNodes()
);


System.out.println(
    "this.parent().name(): " +
    treeNode.parent().name()
);

try {
    System.out.println(
        "this.parent().parent(): " +
            treeNode.parent().parent()
    );
} catch (UnloadedException ex) {
    System.out.println(
        "The message of UnloadedException of this.parent().parent(): " +
            ex.getMessage()
    );
}

try {
    System.out.println(
        "this.parent().childNodes(): " +
            treeNode.parent().childNodes()
    );
} catch (UnloadedException ex) {
    System.out.println(
        "The message of UnloadedException of this.parent().childNodes(): " +
            ex.getMessage()
    );
}


System.out.println(
        "this.childNodes().get(0).name(): " +
                treeNode.childNodes().get(0).name()
);

try {
    System.out.println(
        "this.childNodes().get(0).parent(): " +
            treeNode.childNodes().get(0).parent()
    );
} catch (UnloadedException ex) {
    System.out.println(
        "The message of UnloadedException of this.childNodes.get(0).parent(): " +
            ex.getMessage()
    );
}

try {
    System.out.println(
        "this.childNodes().get(0).childNodes(): " +
            treeNode.childNodes().get(0).childNodes()
    );
} catch (UnloadedException ex) {
    System.out.println(
        "The message of UnloadedException of this.childNodes.get(0).childNodes(): " +
            ex.getMessage()
    );
}
```

:::note
The print result is as follows

---

JSON serialization: {"name":"Current Node","parent":{"name":"Parent Node"},"childNodes":[{"name":"Child Node"}]}

this.name(): Current Node

this.parent(): {"name":"Parent Node"}

this.childNodes(): [{"name":"Child Node"}]

this.parent().name(): Parent Node

The message of UnloadedException of this.parent().parent(): The property "yourpackage.TreeNode.parent" is unloaded

The message of UnloadedException of this.parent().childNodes(): The property "yourpackage.TreeNode.childNodes" is unloaded

this.childNodes().get(0).name(): Child Node

The message of UnloadedException of this.childNodes.get(0).parent(): The property "yourpackage.TreeNode.parent" is unloaded

The message of UnloadedException of this.childNodes.get(0).childNodes(): The property "yourpackage.TreeNode.childNodes" is unloaded
:::

## CircularReferenceException

### Concept

In technical fields such as GUIs, game engines, and physics engines that visually simulate the real world, bidirectional associations between data objects are very important, and bidirectional associations are the basic assumptions of many algorithms.

However, the field of information management is different from that field. Engineers in the field of information management prefer to have only one-way associations between object, they care more about the simplicity of the object format. The existence of bidirectional associations complicates the serialization. Using only one-way associations can simplify the serialization mechanism, simplify the interaction between different microservices, and simplify the interaction with the front-end UI.

:::note
In fact, solving bidirectional associations in serialization is not complicated, as jackson supports [@JsonBackReference](https://fasterxml.github.io/jackson-annotations/javadoc/2.5/com/fasterxml/jackson/annotation/JsonBackReference.html), you can read [this article](https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion).

Although the serialization frameworks add the ability to handle bidirectional associations in order to be as general as possible, in actual projects it is believed that this does more harm than good. Therefore it is rarely used.
:::

jimmer prevents developers from building bidirectional associations between entity objects.

Readers may be very confused after reading this. In the previous example, the `TreeNode` type has both the parent node property `parent` and the child nodes collection property `childNodes`, which is clearly a bidirectional association.

This is actually two different point of view:

1. The point of view of entity type definition

2. The point of view of entity object instantiation

:::tip
- From the point of view of entity interface type definition, jimmer allows and encourages the definition of bidirectional associations.

- From the point of view of entity object instantiation, jimmer prohibits bidirectional associations between different objects, any code that attempts to do so will result in an exception: `org.babyfish.jimmer.CircularReferenceException`.

jimmer established such a rule for developers: **aggregate root** design lag.

1. When defining an entity type, don't think about the **aggregate root** of the one-way association object tree, just declare the entity type according to the original storage of database, allowing and encouraging the declaration of bidirectional associations.

2. In order to realize a specific business, when you need to create an instance of the object tree, then decide what the **aggregation root** is. jimmer guarantees that the object tree instance referenced by the aggregate root never contains a bidirectional association.
:::

### Example

```java
TreeNode aggregateRoot = TreeNodeDraft.$
    .produce(aggregateRootDraft ->
        aggregateRootDraft
            .setName("Aggregate root")
            .addIntoChildNodes(childDraft ->
                childDraft
                    .setName("Child")
                    // CircularReferenceException will be thrown
                    // highlight-next-line
                    .setParent(aggregateRootDraft)
            )
    );
```

:::caution
This code causes exception: org.babyfish.jimmer.CircularReferenceException.

While developers can define bidirectional associations between entity types, they cannot create bidirectional associations between entity objects. This behavior is expressly prohibited.
:::