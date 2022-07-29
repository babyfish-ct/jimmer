
[![logo](logo.png)](https://babyfish-ct.github.io/jimmer-doc/)

A powerful java framework for Java and Kotlin.
- Immutable data model
- ORM based on immutable data model

## 1. Bechmark

- The abscissa represents the count of data objects queried from the database.
- The ordinate represents the operation count per second.

<kbd>
   <a href="https://babyfish-ct.github.io/jimmer-doc/docs/benchmark">
       <img src="benchmark.png"/>
   </a>
</kbd>


> 
> - If you want to view full benchmark report, click [here](https://babyfish-ct.github.io/jimmer-doc/docs/benchmark) please.
> 
> - If you want to run the benchmark, run the project under the sub directory [benchmark](./benchmark) please.

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
- Revolutionary ORM based on the immutable data model

### Part I: Powerful immutable data model

We port the popular JavaScript project [immer](https://github.com/immerjs/immer) to Java/Kotlin. You can manipulate immutable objects naturally and intuitively the same way you manipulate mutable objects, you can have all the well-known advantages of immutable objects without any notorious overhead. This is the most powerful solution for immutable objects.

Jimmer can be used to replace java records(or kotlin data classes) in any context where immutable data structures are preferred. We use very effective mechanisms to detect changes and eliminate unnecessary replication overhead. In general, any change of an object would create a new object reference, that is, the object is immutable in the sense of any specific reference. The unchanged parts would be shared among all versions of the data in memory to avoid naive copying and achieve the best performance.

Jimmer could help you:

1. Detect unexpected mutation and throw appropriate errors;
2. Throw away the tedious boilerplate code for manipulating the deep structure of immutable objects, avoid manual replication and save the overhead of redundant copy construction;
3. Make changes to draft objects that record and trace the modifications, and create any necessary copies automatically with the original intact.

With Jimmer, you don't need to learn specialized APIs or data structures to benefit from immutability.

To support ORM, Jimmer improves the dynamic features of objects. Any property of an object is allowed to be missing.
- Missing properties cause exceptions when accessed directly with code;
- Missing properties are automatically ignored during Jackson serialization and would not cause exceptions.

### Part II: Revolutionary ORM Based on the Immutable Data Model

1. Check for errors at compile time rather than runtime whenever possible with strong typed SQL DSL like JPA Criteria, QueryDSL, or JOOQ;

   > Kotlin nullsafety would be introduced to SQL for kotlin API.

2. Strongly typed SQL DSL and Native SQL can be mixed without extra restrictions, Using database-specific features is very easy;

3. Always use much more efficient [ResultSet.getObject(int)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(int)) instead of  [ResultSet.getObject(String)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(java.lang.String))

4. Implicit dynamic table joins: automatically merge conflicting table joins in different code logic branches for complex dynamic queries *(Even with the highly controllable myBatis, it is difficult to implement this feature)*;

5. In the paging scenario, developers only need to focus on querying the actual data, and the query on the number of rows is automatically generated and optimized by the framework.

6. **Object Fetcher**: extend the ability of SQL. If a column in the query is an object type, it can be specified as the query format of the object. This format accepts any association depth and breadth and even allows recursively query self-association attributes. It can be considered that SQL has been extended to a capability comparable to GraphQL.

7. **Save Command**: the data to be saved is no longer a simple object, but an arbitrarily complex object tree. No matter how complex the tree is, the framework takes care of all the internal details and the developers can handle the whole operation with a single statement. This feature is the inverse of the object fetcher.   

8. Work with any external cache. By default, the framework is just a very lightweight and powerful SQL generator without caching. Still, users can attach any external cache, **and keep it consistent with the business system's own cache technology**. Unlike other ORMs, it supports both object cache and also associative cache, and works with object fetchers, especially when recursively querying self-associative properties.

   > This is an important and brand new feature, we will add the corresponding demo and documentation as soon as possible.
   

9. No reflection, no dynamic bytecode generation, the internal implementation is much more lightweight than myBatis and guaranteed to be Graal friendly.

### Other highlights

1. Provide rapid development support for Spring GraphQL introduced in spring boot 2.7;

2. The dynamic capabilities of immutable objects introduce beneficial side effects and a new design philosophy.

   In the process of information management software development, HTTP APIs often interact with object trees that only contain one-way associations, people will not use two-way associations even though the JSON serialization technology used has some corresponding ability.

   Therefore, DTOs are important when designing module API, because the DTOs required by each business clearly define the aggregate root. DTO design becomes a prerequisite for business design and development.

   Immutable objects in Jimmer are dynamic. Although two-way associations can be defined in the design of ORM entity types, Jimmer ensures that there are only one-way associations between object instances when creating objects for specific business scenarios, and any attempt to establish a two-way association will result in exceptions.

   That is, the design of the aggregate root is deferred from the design of the system API until the object is created in a specific business scenario. It is no longer necessary to design and develop based on DTO, and it is possible to completely use entity objects as the basis for development, which makes the development process natural.

   Of course, such a design concept does not deny the value of DTO. If you think it is safer and clearer to design external APIs with static DTOs than with dynamic entities, you can still use DTOs to encapsulate a layer. This is a useful but not required option.
