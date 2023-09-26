---
sidebar_position: 2
title: Object Fetcher
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

import { ObjectFetcherPanel } from '@site/src/components/HomepageFeatures/ObjectFetcher';

## Concept

The object fetcher is an important feature of Jimmer that automatically fetches specified fields and assembles them into objects, saving a lot of time writing manual conversion logic.

The object fetcher is similar to the following technologies, but more powerful:

-   [JPA's EntityGraph](https://www.baeldung.com/jpa-entity-graph)

-   [ADO.NET EntityFramework's include](https://docs.microsoft.com/en-us/dotnet/api/system.data.objects.objectquery-1.include?view=netframework-4.8) 

-   [ActiveRecord's include](https://guides.rubyonrails.org/active_record_querying.html#includes)

Although the code to return entire objects in queries is simple, the default object format often does not meet development needs very well. It is easy to encounter two problems:

-   Over fetching problem

    Object properties that we don't need are queried, causing waste, especially when there are many object fields.

    Take JPA as an example. The returned object by default is a complete object that contains non-associative properties that are not needed. This is a big problem for traditional ORMs *(JPA's `@Basic(fetch = FetchType.LAZY)` is desgined for LOB column, it cannot be used by any column)*.

-   Under fetching problem

    The object properties we need have not been fetched and are in an unavailable unloaded state, causing the program to fail to run correctly.

The object fetcher solves this problem very well by allowing users to specify properties to fetch, and then utilizing the characteristics of dynamic objects to make the returned objects from the query neither over fetched nor under fetched.

By using the object fetcher, it is easy to specify the return format of queries, because dynamic objects can describe data structures of any shape. You can decide whether a certain business perspective needs to query some entities, associations, or even every properties.

## Examples 

<ObjectFetcherPanel/>