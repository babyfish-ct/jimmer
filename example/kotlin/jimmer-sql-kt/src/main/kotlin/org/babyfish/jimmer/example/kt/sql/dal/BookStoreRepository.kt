package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.BookStore
import org.babyfish.jimmer.spring.repository.KRepository

@JvmDefaultWithCompatibility
interface BookStoreRepository : KRepository<BookStore, Long>