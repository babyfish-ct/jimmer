---
sidebar_position: 4
title: 动态性
---

## Jimmer和JACKSON

jimmer不可变对象是动态的，并不是对象的所有属性都需要初始化，它允许缺少一些属性。

- 未指定的属性在直接被代码访问时会导致异常
- 未指定的属性在JSON序列化中自动忽略，不会异常。

这里提到了JSON序列化，指[jackson](https://github.com/FasterXML/jackson)。

jimmer-core定了一个jackson模块：`org.babyfish.jimmer.jackson.ImmutableModule`，利用该模块可以为jackson增加序列化/反序列化jimmer不可变对象的能力。

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

对于序列化操作而言，有一个便捷方式，就是jimmer对象的`toString`方法。

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
由于调用`toString`方法的代码很简单，本文后续例子都使用它，而非调用jackson的API。
:::

## 动态性展示

未被指定的属性在JSON序列化中会被忽略，然而，直接用代码访问将会异常：`org.babyfish.jimmer.UnloadedException`。

:::note
这种访问未设置属性抛出异常的行为，和ORM框架类似，例如：`org.hibernate.LazyInitializationException`。
:::

### 示范一：单个对象

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
打印结果如下

JSON serialization: {"name":"Current Node"}

this.name(): Current Node

UnloadedException message: The property "yourpackage.TreeNode.parent" is unloaded

UnloadedException message: The property "yourpackage.TreeNode.childNodes" is unloaded
:::

### 示范二：多个对象

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
打印结果如下

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

### 概念

在GUI、游戏引擎以及物理引擎这类可视化地模拟现实世界技术领域中，数据对象之间的双向关联非常重要，双向关联很多算法的基本假设。

然而，信息化管理领领域和这类领域不同，这个领域的工程师更青睐让对象之间仅具备单向关联，更在乎对象格式的简单性。双向关联的存在会让序列化问题复杂化，仅使用单向关联可以使得序列化机制最简化，简化在不同的微服务之间交互，也简化和前端UI的交互。

:::note
事实上，在序列化中解决双向关联并不复杂，比如jackson支持[@JsonBackReference](https://fasterxml.github.io/jackson-annotations/javadoc/2.5/com/fasterxml/jackson/annotation/JsonBackReference.html)，你可以阅读[这篇文章](https://www.baeldung.com/jackson-bidirectional-relationships-and-infinite-recursion)。

虽然序列化框架为了尽可能通用会加入处理双向关联的能力，但是实际项目中人们认为这样做弊大于利。故很少使用。
:::

jimmer防止开发人员之间在实体对象之间构建双向关联。

读者读到这里可能很疑惑，前面的例子中`TreeNode`类型既具备父节点属性`parent`，又具备子节点集合属性`childNodes`，这分明是双向关联呀。

这其实是两个不同的思考角度：

1. 实体类型定义的角度

2. 实体对象实例化的角度

:::tip
- 从实体接口类型定义的角度讲，jimmer允许也鼓励定义双向关联。

- 从实体对象实例化的角度讲，jimmer禁止不同对象之间存在双向关联，任何有这种企图的代码都会导致异常：org.babyfish.jimmer.CircularReferenceException。

jimmer为开发人员确立了这样一个法则：**聚合根**设计滞后化。

1. 定义实体类型时，不要去考虑单向关联对象树的**聚合根**，只需按照数据的原始存储方式声明实体类型，允许并鼓励声明双向关联。

2. 为实现某个特定业务，需要创建对象树实例时，再决定**聚合根**是什么。jimmer保证**聚合根**所引用的对象树实例绝不包含双向关联。
:::

### 演示

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
这段代码会导致异常：org.babyfish.jimmer.CircularReferenceException。

虽然开发人员可以在实体类型之间定义双向关联，但无法在实体对象之间创建双向关联。此行为被明确禁止。
:::