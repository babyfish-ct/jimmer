[![logo](logo.png)](https://babyfish-ct.github.io/jimmer/)

# A revolutionary ORM framework for both java and kotlin, and a complete integrated solution

## Switch Langue 

English | [中文](./README_zh_CN.md)

## 1. Key features

Based on the aforementioned core concept, Jimmer provides the following features:

-   Convenient query API, and elegant Kotlin DSL
-   DTO language and corresponding code generator
-   Powerful dynamic querying, query conditions can apply to multiple tables
-   Query arbitrary graph structures
    -   No N+1 problem
    -   Objects at any level can be parial object
    -   Recursive querying of self-associated properties
-   Save arbitrary graph structures
    -   Merge data utilizing the database's own upsert capability
    -   Batch SQL used at all levels
    -   Automatic translation of constraint violation exceptions
    -   Saved objects at any level can be parial object
-   Advanced SQL optimization capabilities
    -   Automatic removal of unnecessary table joins
    -   Automatic merging of logically equivalent table joins and subqueries
    -   In paginated queries, automatically generate and optimize the count query
-   Powerful caching
    -   Multi-level caching
    -   Not just object caching *(associations, colculated values, multi-view)*
    -   Automatic maintenance of cache consistency
-   Rapid support for GraphQL
-   Client contracts based on documentation comments *(OpenAPI, TypeScript)*

## 2. Core Concept

The core concept of Jimmer is to read and write data structures of arbitrary shapes as a whole, rather than simply processing entity objects.

-   Jimmer entity objects **are not POJOs**, and can easily **express data structures of arbitrary shapes**.

-   Data structures of any shape can be processed as a whole for:

    -   Reading: Jimmer creates this infinitely flexible data structure and passes it to you
    -   Writing: You create this infinitely flexible data structure and pass it to Jimmer

Since Jimmer's design philosophy is to read and write data structures of arbitrary shapes rather than processing simple objects, how does it differ from technologies with similar capabilities?

<table>
<thead>
<tr>
<th>Comparison</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td rowspan="2">GraphQL</td>
<td>GraphQL only focuses on querying data structures of arbitrary shapes; Jimmer not only does this but also focuses on how to write data structures of arbitrary shapes</td>
</tr>
<tr>
<td>GraphQL does not support recursive queries based on self-referencing properties, Jimmer does</td>
</tr>
<tr>
<td rowspan="5">JPA</td>
<td>In JPA, to control the shape of the data structure being saved, properties must be configured with insertable, updatable, or cascade <i>(for associated properties)</i>.
Regardless of the configuration, the saved data structure is fixed; Jimmer entities are not POJOs, their data structure shapes are ever-changing,
no prior planning and design is needed, any business scenario can construct the data structure it needs and save it directly</td>
</tr>
<tr>
<td>

For queries, JPA's `EntityGraphQL` is very complex; Jimmer provides two methods to achieve similar functionality:
controlling the format of returned entity objects, or generating DTOs through a cost-effective way and querying directly. Either way is much simpler than `EntityGraph`

</td>
</tr>
<tr>
<td>

In JPA, if you need to use a DTO object to query only part of the properties, the DTO must be a simple object without any associations. That is, it loses the most valuable capability of ORM, degrading from `ORM` to `OM`;
Jimmer's automatically generated DTOs support arbitrarily complex hierarchical relationships, **Jimmer is currently the only ORM that supports nested projections based on DTO**

</td>
</tr>
<tr>
<td>

In JPA, updating an object results in all updatable columns being modified. For simplicity, developers rarely use `update`, instead choosing to first query the complete object, modify some properties, and finally save the entire object; Jimmer can construct and directly save incomplete objects

</td>
</tr>
<tr>
<td>

JPA's `EntityGraphQL` does not support recursive queries based on self-referencing properties, Jimmer does

</td>
</tr>
<tr>
<td>MongoDB</td>
<td>
In MongoDB, each document structure is a data island. Although MongoDB's data structure is weakly typed, from a business perspective, which data islands exist and the internal hierarchical structure of each data island need to be designed and agreed upon in advance.
Once the design and agreement are completed, the format of the entire data view is fixed and must be processed from a fixed perspective;
In Jimmer, the shape of the data structure does not need to be designed in advance, any business scenario can freely plan a data structure format, and read and write the corresponding data structure as a whole.
</td>
</tr>
</tbody>
</table>

**Based on this core concept, Jimmer will bring you convenience that was previously unattainable in any technology stack, 
freeing you from dealing with tedious details and allowing you to focus on quickly implementing complex business logic.**

## 3. Comprehensive capabilities
![feature](./feature.svg)

## 4. Ultimate performance
![performance](./performance.jpg)

## 5. Links

-   Examples: https://github.com/babyfish-ct/jimmer-examples
-   Dcoumentation: https://babyfish-ct.github.io/jimmer-doc/
-   Discoard: https://discord.gg/PmgR5mpY3E