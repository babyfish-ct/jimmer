---
sidebar_position: 11
title: 全局过滤器
---

全局过滤器用于为自动某些实体添加过滤条件，绝大部分针对这些实体类型的查询都会被自动添加相关`where`条件。

该功能有如下应用场景

-   逻辑删除

    逻辑删除是Jimmer内置的全局过滤器，无需用户定义。

-   多视角数据，数据查询需要携带用户身份相关的查询条件，即使除用户身份以外的所有查询参数都相同，不同的用户也会看到不同的数据。

    多视角数据有一个典型的案例：多租户。本文档用多租户作为例子，讲解用户自定义全局过滤器。

:::note
对于有Hibernate背景知识的用户而言，全局过滤器和[org.hibernate.annotations.Where](https://javadoc.io/static/org.hibernate/hibernate-core/5.4.13.Final/org/hibernate/annotations/Where.html)、[org.hibernate.annotations.Filter](https://javadoc.io/static/org.hibernate/hibernate-core/5.4.13.Final/org/hibernate/annotations/Filter.html)以及[org.hibernate.annotations.FilterDef](https://javadoc.io/static/org.hibernate/hibernate-core/5.4.13.Final/org/hibernate/annotations/FilterDef.html)类似。

不同点在于，Jimmer的全局过滤器具有灵活的注册方式，很容易被IOC框架 *(比如Spring)* 管理。

这样，全局过滤器可以直接从IOC框架中获取参数，比如用户身份信息，所以，开发人员无需在查询前为过滤器传参。
:::

:::caution
Jimmer提供了一些按照id *(或id集合)* 查询实体 *(或实体集合)*的简单API，这类API例外，不受全局过滤器的影响。
:::