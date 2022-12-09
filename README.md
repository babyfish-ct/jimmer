
[![logo](logo.png)](https://babyfish-ct.github.io/jimmer/)

A revolutionary ORM framework for both java and kotlin.


## 1. Documentation

The project provides complete documentation.

Please view [**documentation**](https://babyfish-ct.github.io/jimmer/)*(Both english & chinese)* to know everything.

## 2. Video links

<table>
   <tr>
      <td>English</td>
      <td>https://www.youtube.com/watch?v=Rt5zNv0YR2E</td>
   </tr>
   <tr>
      <td rowspan="3">Chinese</td>
      <td>Global introduction: https://www.bilibili.com/video/BV1kd4y1A7K3</td>
   </tr>
   <tr>
      <td>Table Join Topic: https://www.bilibili.com/video/BV19t4y177PX</td>
   </tr>
   <tr>
      <td>Global filters and parameterized cache: https://www.bilibili.com/video/BV1Mt4y1u7fz/</td>
   </tr>
<table>

## 3. Bechmark

- The abscissa represents the count of data objects queried from the database.
- The ordinate represents the operation count per second.

<kbd>
   <a href="https://babyfish-ct.github.io/jimmer/docs/benchmark/">
       <img src="https://raw.githubusercontent.com/babyfish-ct/jimmer/orphan/doc/static/img/benchmark-snapshot.jpg"/>
   </a>
</kbd>

> 
> - If you want to view full benchmark report, click [here](https://babyfish-ct.github.io/jimmer-doc/docs/benchmark) please.
> 
> - If you want to run the benchmark, run the project under the sub directory [benchmark](./benchmark) please.

## 4. Examples:

This framework provides three examples

1. Java Examples
   - [example/java/jimmer-core](example/java/jimmer-core): How to use immutable objects
   - [example/java/jimmer-sql](example/java/jimmer-sql): How to use ORM framework
   - [example/java/jimmer-sql-graphql](example/java/jimmer-sql-graphql): How to quickly develop [Spring GraphQL](https://spring.io/projects/spring-graphql) services based on jimmer.


2. Kotlin Examples
   - [example/kotlin/jimmer-core-kt](example/kotlin/jimmer-core-kt): How to use immutable objects
   - [example/kotlin/jimmer-sql-kt](example/kotlin/jimmer-sql-kt): How to use ORM framework
   - [example/kotlin/jimmer-sql-graphql-kt](example/kotlin/jimmer-sql-graphql-kt): How to quickly develop [Spring GraphQL](https://spring.io/projects/spring-graphql) services based on jimmer.

## 5. Discuss

|Language|Entry point|
|---|---|
|English|https://discord.com/channels/1016206034827743283/|
|Chinese|<img src="https://raw.githubusercontent.com/babyfish-ct/jimmer/orphan/doc/static/img/public-wechat.jpg" height=300/>|

## 6. Ecosystem

Jimmer only focuses on the ORM itself, and does not provide auxiliary tools, such as producing entity interface definitions based on database structures.

Enthusiastic people have contributed related tools. So far, the projects included are:

|Project type|Supported languages|Project link|
|---|---|---|
|Intellij plugin|Java&Kotlin|https://github.com/ClearPlume/jimmer-generator|
|Maven plugin|Java|https://github.com/TokgoRonin/code-generator-jimmer|
|Intellij plugin|Java&Kotlin|https://github.com/huyaro/CodeGenX|

Everyone is welcome to actively contribute related tools, and submit PR for me to include them in the list
