package org.babyfish.jimmer.sql.example.kt.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.kt.model.BookStore

interface BookStoreRepository : KRepository<BookStore, Long> // ❶

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept
---------------------------------------------------*/
