package org.babyfish.jimmer.sql.example.graphql.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.springframework.lang.Nullable;

import java.util.List;

public interface BookStoreRepository extends JRepository<BookStore, Long> {

    List<BookStore> findByNameLikeOrderByName(@Nullable String name);
}
