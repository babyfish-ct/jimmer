---
sidebar_position: 3  
title: Save Command
---

Save commands are a very powerful capability of Jimmer that can greatly simplify the development difficulty of persisting complex data structures.

If [object fetchers](../../query/object-fetcher) enable output data to be in any shape, then save commands allow input data to also be in any shape.

For readers familiar with web frontend technologies, this can be analogized to `Virtual DOM diff` in [React](https://react.dev/) or [Vue](https://vuejs.org/).

Calling a save command only takes one line of code, but hides countless details internally that documentation cannot exhaustively enumerate. Therefore, save commands have a dedicated sample project:

-   Java: [example/java/save-command](https://github.com/babyfish-ct/jimmer/tree/main/example/java/save-command) 

-   Kotlin: [example/kotlin/save-command-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/save-command-kt)

Simply open either one in an IDE, and run the unit tests.
