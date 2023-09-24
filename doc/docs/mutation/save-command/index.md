---
sidebar_position: 3  
title: Save Command
---

:::tip
One statement to save complex data of arbitrary shape, find DIFF to change database, like React/Vue
:::

Save commands are a very powerful capability of Jimmer that can greatly simplify the development difficulty of persisting complex data structures.

If [object fetchers](../../query/object-fetcher) enable output data to be in any shape, then save commands allow input data to also be in any shape.

For readers familiar with web frontend technologies, this can be analogized to `Virtual DOM diff` in [React](https://react.dev/) or [Vue](https://vuejs.org/).

:::tip 
Save command require developers to **completely change their thinking pattern**

-   Fundamental difference in thinking

    -   The traditional thinking pattern:

        Manually compare the data structure to be saved with existing data in the database, and execute `INSERT`, `UPDATE` or `DELETE` on the changed parts

    -   The thinking pattern for save command:

        Accept the data structure passed from client as a whole and just save it. Jimmer will handle everything *(automatically compare the data structure to be saved with existing data in the database, and execute `INSERT`, `UPDATE` or `DELETE` on the changed parts)*

-   Old habits should be replaced by better methods

    In the traditional development mode, developers like to do one thing: query an object first, then modify some of its properties, and finally save the modified object.

    Although Jimmer also allows developers to do this, it is recommended to use a more performant approach, please refer to [Incomplete Object](./incomplete).
:::

Calling a save command only takes one line of code, but hides countless details internally that documentation cannot exhaustively enumerate. Therefore, save commands have a dedicated sample project:

-   Java: [example/java/save-command](https://github.com/babyfish-ct/jimmer/tree/main/example/java/save-command) 

-   Kotlin: [example/kotlin/save-command-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/save-command-kt)

Simply open either one in an IDE, and run the unit tests.
