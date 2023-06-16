package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.example.model.*
import org.babyfish.jimmer.sql.kt.ast.expression.*
import java.math.BigDecimal

interface BookStoreRepository : KRepository<BookStore, Long>