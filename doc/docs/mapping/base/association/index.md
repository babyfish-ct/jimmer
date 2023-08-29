---
sidebar_position: 3
title: Association Mapping
---

Here, you will learn about the most important capability of ORM: entity relationship mapping. You will learn about these annotations:

- org.babyfish.jimmer.sql.OneToOne
- org.babyfish.jimmer.sql.ManyToOne
- org.babyfish.jimmer.sql.OneToMany
- org.babyfish.jimmer.sql.ManyToMany
- org.babyfish.jimmer.sql.JoinColumn
- org.babyfish.jimmer.sql.JoinTable

:::caution
For associations, the type of the basic properties in the entity type should be the associated object, not the associated Id. 

If you want to define associated Id properties, please

- First complete the association mapping according to this chapter

- Then add associated Id properties according to [IdView](../../advanced/view/id-view)
:::
