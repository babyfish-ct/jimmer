---
sidebar_position: 11  
title: Global Filters
---

Global filters are used to automatically add filtering conditions for an entity type. Most queries for the entity type will automatically have related `where` conditions added.

This feature has the following application scenarios:

-   Multi-perspective data. Querying data requires query conditions related to user identity, even if all query parameters except user identity are the same, different users will see different data.

    Multi-perspective data has a typical case: multi-tenancy. This document uses multi-tenancy as an example to explain custom global filters defined by users.

-   Logical deletion

    Logical deletion is a built-in global filter in Jimmer and does not require user definition.

:::note
For users with Hibernate background knowledge, global filters are similar to [org.hibernate.annotations.Where](https://javadoc.io/static/org.hibernate/hibernate-core/5.4.13.Final/org/hibernate/annotations/Where.html), [org.hibernate.annotations.Filter](https://javadoc.io/static/org.hibernate/hibernate-core/5.4.13.Final/org/hibernate/annotations/Filter.html) and [org.hibernate.annotations.FilterDef](https://javadoc.io/static/org.hibernate/hibernate-core/5.4.13.Final/org/hibernate/annotations/FilterDef.html). 

The difference is that Jimmer's global filters have flexible registration methods and are very easy to be managed by IOC frameworks (such as Spring).

Thus, global filters can directly obtain parameters from the IOC framework, such as user identity information, so developers do not need to pass parameters to filters before queries.
:::

:::caution
1.  After repeated consideration, currently global filters only affect the main table of the top-level query, and have no effect on tables obtained from join operations and tables in subqueries.

    This has no impact on [object fetchers](../object-fetcher) because [object fetchers](../object-fetcher) are not table Join operations.

2.  Jimmer provides some simple APIs to query entities (or collections of entities) by id (or collection of ids). These APIs are exceptional and are not affected by global filters.
:::
