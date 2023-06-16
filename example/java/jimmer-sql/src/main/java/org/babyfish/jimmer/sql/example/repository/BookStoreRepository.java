package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.model.BookStore;
import org.babyfish.jimmer.sql.example.model.BookStoreTable;
import org.babyfish.jimmer.sql.example.model.BookTableEx;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface BookStoreRepository extends JRepository<BookStore, Long> {
}
