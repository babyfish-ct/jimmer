---
sidebar_position: 5  
title: Calculated Properties
---

In this article, you will learn about Jimmer's calculated properties and annotations:

- org.babyfish.jimmer.sql.Formula

    Simple calculated properties, can be Java/Kotlin expressions or SQL expressions

- org.babyfish.jimmer.sql.Transient

    Complex calculated properties, with the following features:

    - Not only can simple values be calculated, but associated calculated values can also be calculated. That is, calculated properties can be associated properties

    - Any complex calculation rules can be used, for example, using OLAP systems unrelated to the current relational database for calculation

:::caution
Calculated properties should only be defined when a calculated metric belongs to an entity. 

If a calculated metric does not belong to any entity, it should be designed as a global Service API.
:::