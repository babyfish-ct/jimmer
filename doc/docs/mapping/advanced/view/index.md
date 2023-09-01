---
sidebar_position: 4
title: View Properites
---

View attributes do not maintain their own data, they are just another representation of another attribute *(called original attribute)* of their owning entity type.

View attributes and original attributes share the same underlying data, the only difference is that the returned data format is different from the original attribute.

In this article, you will learn about these annotations:

-   org.babyfish.jimmer.sql.IdView
-   org.babyfish.jimmer.sql.ManyToView