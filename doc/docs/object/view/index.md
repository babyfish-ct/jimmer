---
sidebar_position: 5
title: DTO conversion 
---

Even if entity objects support dynamism to express arbitrary data structures, conversion between entities and DTOs is still unavoidable.

Jimmer provides dynamic entities that can solve a large part of the DTO explosion problem very well. Therefore, generally speaking, it is not necessary to define output DTO types to express query results.

However, not all DTO types can be eliminated. Input DTO objects are hard to remove.

> For example, in GraphQL, although dynamic `GraphQLObject` data is returned for the client from the output perspective, static `GraphQLInput` data submitted by the client is accepted from the input perspective.
>
> Why does the GraphQL protocol define `GraphQLInput` as a static type? Because API explicitness and system security are very important requirements, please refer to [Problems with dynamic objects as input parameters](../../mutation/save-command/input-dto/problem).
>
> The problems faced by the GraphQL protocol are also faced by Jimmer, which must provide a complete solution.

As a comprehensive solution, Jimmer is not limited to ORM itself, but considers the whole project. To solve this problem, it provides two ways:

-   DTO Language

    A solution tailored for Jimmer with extremely high development efficiency.

    [DTO Language](./dto-language.mdx) is designed for that part of the DTO types that cannot be eliminated, with the goal of making them extremely cheap.

-   MapStruct

    A solution that combines the [MapStruct](https://mapstruct.org/) framework and can implement arbitrarily complex conversion logic.