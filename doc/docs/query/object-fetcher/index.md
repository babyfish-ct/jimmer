---
sidebar_position: 2
title: Object fetcher
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import { ObjectFetcherPanel } from '@site/src/components/HomepageFeatures/ObjectFetcher';

## 概念
对象抓取器，是Jimmer的一个重要功能，抓取指定的字段，然后组装成对象，这一切是自动完成的，节省了很多手写转换逻辑的时间。

对象抓取器和以下这类技术类似，但更加强大

- [JPA的EntityGraph](https://www.baeldung.com/jpa-entity-graph)

- [ADO.NET EntityFramework的Include](https://docs.microsoft.com/en-us/dotnet/api/system.data.objects.objectquery-1.include?view=netframework-4.8)

- [ActiveRecord的include](https://guides.rubyonrails.org/active_record_querying.html#includes)

虽然在查询中返回整个对象的代码很简单，但是默认对象格式往往不能很好地符合开发需求。很容易遇到两个问题

-   over fetch问题

    我们不需要的对象属性查询了，形成了浪费，尤其是对象字段很多的时。

    以JPA为例，其返回对象默认是一个完整对象，不需要的非关联属性也会包含在内。这是传统ORM一个很大的问题。

-   under fetch问题

    我们需要的对象属性被并未被获取，处于不可用unloaded状态，程序无法正确运行。

对象抓取器很好地解决这个问题，通过让用户指定要抓取的属性，然后利用动态对象的特性，让查询返回的对象既不over fetch也不under fetch。

利用对象抓取器的可以轻松指定查询的返回格式，因为动态对象可以描述任意形状的数据结构，你可以决定某个业务视角是否需要查询某些实体、关系、甚至每一个属性。

## 例子

<ObjectFetcherPanel/>
