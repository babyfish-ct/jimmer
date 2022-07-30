---
sidebar_position: 3
title: jimmer-core subproject
---

jimmer-core is committed to creating powerful immutable object model to solve the unsolved problems of java record (or kotlin data class). Immutable objects are the underlying foundation of all functions of the jimmer framework.

jimme-core ports a well-known project [immer](https://github.com/immerjs/immer) for Java, modifying immutable objects in the way of mutable draft objects.

Jimmer can be used in any context where immutable data structures are required to replace java records. Immutable data structures allow for (effective) change detection: if the reference to the object hasn't changed, then neither has the object itself. Also, it makes cloning relatively cheap: unchanged parts of the data tree do not need to be copied and are shared in memory with older versions of the same state.

In general, these benefits are achieved by ensuring that you never change any properties of an object or list, but always create a changed copy. In practice, this can lead to very cumbersome code to write, and it is easy to accidentally violate these constraints. Jimmer will help you follow the immutable data paradigm by addressing the following pain points:

1. Jimmer will detect an unexpected mutation and throw an error.
2. Jimmer will eliminate the need to create the typical boilerplate code required when doing deep updates to immutable objects: without Jimmer, you would need to manually make copies of objects at each level. Usually by using a lot of copy construction.
3. When using JImmer, changes are made to the draft object, which records the changes and takes care of creating the necessary copies without affecting the original.

When using Jimmer, you don't need to learn specialized APIs or data structures to benefit from paradigms.

In addition, to support ORM, Jimmer adds object dynamics to immer. Any property of an object is allowed to be missing.
- Missing properties cause exceptions when accessed directly by code
- Missing properties are automatically ignored during Jackson serialization and will not cause an exception
