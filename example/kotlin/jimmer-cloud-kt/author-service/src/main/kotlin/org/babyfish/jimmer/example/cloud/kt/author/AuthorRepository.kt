package org.babyfish.jimmer.example.cloud.kt.author

import org.babyfish.jimmer.example.cloud.kt.model.Author
import org.babyfish.jimmer.spring.repository.KRepository

interface AuthorRepository : KRepository<Author, Long>