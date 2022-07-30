---
sidebar_position: 4
title: jimmer-sql subproject
---

jimmer-sql is an ORM framework based on jimmer-core immutable objects.

For a long time, due to the limitation of the expressive ability of data entities, it is difficult for many ORM frameworks to make major breakthroughs. 

The data entities provided by jimmer-core have enough expressive power, so jimmer-sql can provide more powerful capabilities.

jimmer-sql provides powerful features that technologies such as JPA, myBatis, JOOQ, exposed and ktorm do not provide.

Main features of jimmer-sql

1. Like JPA Criteria, QueryDSL and JOOQ, strongly typed SQL SDL strives to find SQL errors at compile time, not runtime, debugging, and testing.

2. Strongly typed SQL DSL and Native SQL can be freely mixed, encouraging the use of database-specific functions.

   Please click [here](./expression#native-sql-expression)

3. Always use high performance [ResultSet.getObject(int)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(int)); instead of Relatively inefficient [ResultSet.getObject(String)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(java.lang.String))

4. Implicit dynamic table joins, in the implementation of complex dynamic queries, automatically merge conflicting table joins in different code logic branches * (Even with the highly controllable myBatis, it is difficult to achieve this function)*

   Please click [here](./table-join#dynamic-join)

5. A general paging query requires two SQLs, one for data and one for row count. Developers are responsible for the former, and the latter are automatically generated and optimized by the framework.

   Please click [here](./query/pagination)

6. **Object fetcher**: Extend the ability of SQL, if a column in the query is an object type, it can be specified as the query format of the object, accept any association depth and breadth, and even recursively query self-association attributes. It can be considered that SQL has been extended to a sufficiently powerful form, with capabilities comparable to GraphQL.

   Please click [here](./query/fetcher)

7. **Sava command**: The data to be saved (inserted or modified) is no longer a simple object, but an arbitrarily complex object tree. No matter how complex the tree is, the framework takes care of all the internal details, and the developer can complete the whole operation in just one sentence. This function is the inverse of the object fetcher.

   Please click [here](./mutation/save-command)

8. Any external cache system can be connected. By default, there is no cache, it is just a very lightweight and powerful SQL generator; but users can attach any cache, ** and consistent with the business system's own cache technology**. Unlike other ORMs, it supports not only object caching, but also associative caching, and is effective for object grabbers, especially when recursively querying self-associative properties. 

   > This is a newly added function, there is currently no more detailed documentation, it will be added as soon as possible.

9. Very lightweight, the internal implementation is lighter than myBatis, without any reflection behavior, without any dynamic bytecode generation behavior, ensuring graal friendliness.

10. Spring Boot 2.7 introduces Spring GraphQL, jimmer provides rapid development support for it.

    Please click [here](./spring-graphql.md)
