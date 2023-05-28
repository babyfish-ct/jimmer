---
sidebar_position: 6
title: 计算属性
---

在这里，你会了解到Jimmer的计算属性，以及注解：

-   org.babyfish.jimmer.sql.Formula

    简单计算属性，可以是Java/Kotlin表达式，也可以是SQL表达式

-   org.babyfish.jimmer.sql.Transient

    复杂计算属性，具备如下特征

    -   不仅可以是计算简单值，还可以是关联计算值。即，计算属性可以是关联属性

    -   可以使用任意复杂的计算规则，例如，利用和当前关系型数据库无关的OLAP系统进行计算

:::caution
只有当一个计算指标和隶属于某个实体，才定义计算属性。

如果计算指标不隶属于任何实体，则应该设计称全局的Service API。
:::