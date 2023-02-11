package org.babyfish.jimmer.sql.example.dal

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.model.BookStore

interface BookStoreRepository : KRepository<BookStore, Long>