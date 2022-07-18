
[![logo](logo.png)](https://babyfish-ct.github.io/jimmer-doc/)

A powerful java framework for 
- Immutable data model
- ORM based on immutable data model

## 1. Bechmark

- The abscissa represents the count of data objects queried from the database.
- The ordinate represents the operation count per second.

<kbd>
   <img src="benchmark.png"/>
</kbd>


> 
> - Click [here](https://babyfish-ct.github.io/jimmer-doc/docs/benchmark) to view full benchmark report.
> 
> - If you want to run the benchmark, run the project under the sub directory [benchmark](./benchmark)

## 2. Documentation

The project provides complete documentation.

Please view [**jimmer documentation**](https://babyfish-ct.github.io/jimmer-doc/) to know everything.

## 3. Examples:

This framework provides three examples

- [example/jimmer-core](example/jimmer-core): How to use immutable objects
- [example/jimmer-sql](example/jimmer-sql): How to use ORM framework
- [example/jimmer-sql-graphql](example/jimmer-sql-graphql): How to quickly develop [Spring GraphQL](https://spring.io/projects/spring-graphql) services based on jimmer.

## 4. Introduce

### Purpose
- Powerful immutable data model
- Revolutionary ORM based on immutable data model

### Part 1: Powerful immutable data model

Porting a well-known project [immer](https://github.com/immerjs/immer) for Java, modifying immutable objects in the way of mutable objects.

Jimmer can be used in any context where immutable data structures are required to replace java records. Immutable data structures allow for (effective) change detection: if the reference to the object hasn't changed, then neither has the object itself. Also, it makes cloning relatively cheap: unchanged parts of the data tree do not need to be copied and are shared in memory with older versions of the same state.

In general, these benefits are achieved by ensuring that you never change any properties of an object or list, but always create a changed copy. In practice, this can lead to very cumbersome code to write, and it is easy to accidentally violate these constraints. Jimmer will help you follow the immutable data paradigm by addressing the following pain points:

1. Jimmer will detect an unexpected mutation and throw an error.
2. Jimmer will eliminate the need to create the typical boilerplate code required when doing deep updates to immutable objects: without Jimmer, you would need to manually make copies of objects at each level. Usually by using a lot of copy construction.
3. When using JImmer, changes are made to the draft object, which records the changes and takes care of creating the necessary copies without affecting the original.

When using Jimmer, you don't need to learn specialized APIs or data structures to benefit from paradigms.

In addition, to support ORM, Jimmer adds object dynamics to immer. Any property of an object is allowed to be missing.
- Missing properties cause exceptions when accessed directly by code
- Missing properties are automatically ignored during Jackson serialization and will not cause an exception

### Part 2: A revolutionary ORM based on immutable data model

1. Like JPA Criteria, QueryDSL and JOOQ, strongly typed SQL SDL strives to find SQL errors at compile time, not runtime, debugging, and testing.

2. Strongly typed SQL DSL and Native SQL can be freely mixed, encouraging the use of database-specific functions.

3. Always use high performance [ResultSet.getObject(int)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(int)); instead of Relatively inefficient [ResultSet.getObject(String)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(java.lang.String))

4. Implicit dynamic table joins, in the implementation of complex dynamic queries, automatically merge conflicting table joins in different code logic branches * (Even with the highly controllable myBatis, it is difficult to achieve this function)*

5. A general paging query requires two SQLs, one for data and one for row count. Developers are responsible for the former, and the latter are automatically generated and optimized by the framework.

6. **Object fetcher**: Extend the ability of SQL, if a column in the query is an object type, it can be specified as the query format of the object, accept any association depth and breadth, and even recursively query self-association attributes. It can be considered that SQL has been extended to a sufficiently powerful form, with capabilities comparable to GraphQL.

7. **Sava instruction**: The data to be saved (inserted or modified) is no longer a simple object, but an arbitrarily complex object tree. No matter how complex the tree is, the framework takes care of all the internal details, and the developer can complete the whole operation in just one sentence. This function is the inverse of the object fetcher.

8. Any external cache system can be connected. By default, there is no cache, it is just a very lightweight and powerful SQL generator; but users can attach any cache, ** and consistent with the business system's own cache technology**. Unlike other ORMs, it supports not only object caching, but also associative caching, and is effective for object grabbers, especially when recursively querying self-associative properties. 

> New feature, detail documentation is not ready temporarily.

9. Very lightweight, the internal implementation is lighter than myBatis, without any reflection behavior, without any dynamic bytecode generation behavior, ensuring graal friendliness

### Other Features

1. Spring Boot 2.7 introduces Spring GraphQL to provide rapid development support for it.

2. The dynamism of immutable objects brings beneficial side effects and a new design philosophy to users

   In the development process of information management software, HTTP APIs often interact with object trees that only contain one-way associations. Even if the dependent JSON serialization technology has a certain ability to handle bidirectional associations, people will not use it.

   Therefore, DTOs are important when designing module APIs, because the DTOs required by each business clearly define the aggregate root. DTO design becomes a prerequisite for business design and development.

   Jimmer's immutable objects are dynamic. Although bidirectional associations can be defined in the design of ORM entity types; when creating objects for specific business scenarios, Jimmer ensures that there is only a unidirectional association between object instances, and any attempt to establish a bidirectional association between object instances will result in an exception.

   That is, the design of the aggregate root, from the time of system API design, is deferred to when the object is created for a specific business scenario. Naturally, it is no longer necessary to design and develop based on DTO, and it is possible to completely use entity objects as the basis for development, and the development process is very natural.

   Of course, this design concept is not to completely deny the value of DTOs. If you think that it is safer and clearer to design external APIs with static DTOs than with dynamic entities, you can still choose to use DTOs as a final encapsulation. However, it's a icing on the cake, rather than a must-have option.
