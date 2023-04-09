package org.babyfish.jimmer.example.cloud.kt.store

import org.babyfish.jimmer.example.cloud.kt.model.BookStore
import org.babyfish.jimmer.spring.repository.KRepository

interface StoreRepository : KRepository<BookStore, Long>