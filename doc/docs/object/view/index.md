---
sidebar_position: 5
title: DTO conversion 
---

Here is the English translation:

When necessary, we have to convert between Jimmer dynamic objects and static DTO objects. Jimmer provides two methods for this:

-   Use the DTO language that comes with Jimmer. This is the preferred solution. Based on the entity type, quickly define the shapes of several data structures. After compilation by Jimmer, for each shape definitation, it automatically generates:
  
    -   Java/Kotlin definition of DTO type
  
    -   Mutual conversion logic between DTO object and entity object
  
    -   [Object fetcher](../../query/object-fetcher) matching the shape
  
-   For DTO types already defined in legacy projects, use [mapstruct](https://mapstruct.org/) for mutual conversion.

:::tip 
Whether it is a dynamic entity object that represents any data structure shape, or a static DTO object corresponding to a specific data structure shape, they are equivalent in Jimmer's view, and can be queried in one line of code or saved in one line of code as a whole.

Therefore, developers only need to be responsible for the mutual conversion between dynamic entity objects and static DTO objects. From the perspective of database and cache operations, there is no additional cost.
:::