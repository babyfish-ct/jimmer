---
sidebar_position: 8
title: Mutation
---

jimmer-sql provides two ways to modify the database

1. Mutation statement

    The mutation statement corresponds to the update and delete statements of SQL, and is suitable for situations where the logic is simple but batch operations are required.

2. Mutation command

    The mutation command is suitable for occasions with complex logic.
    
    The save command is very powerful. If GraphQL is a powerful dynamic tree output solution, then the save command is a dynamic tree input solution.

## Table of contents

- [Update statement](./update-statement)
- [Delete statement](./delete-statement)
- [&#128161; Save command](./save-command)
- [Delete command](./delete-command)
- [Mutate middle table](./association)
- [Draft interceptor](./interceptor)
- [Trigger](./trigger)