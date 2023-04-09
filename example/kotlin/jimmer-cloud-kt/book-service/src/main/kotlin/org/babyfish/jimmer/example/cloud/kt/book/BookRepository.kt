package org.babyfish.jimmer.example.cloud.kt.book

import org.babyfish.jimmer.example.cloud.kt.model.Book
import org.babyfish.jimmer.spring.repository.KRepository

interface BookRepository : KRepository<Book, Long>