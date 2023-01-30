package org.babyfish.jimmer.sql2.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql2.example.model.BookStore;

public interface BookStoreRepository extends JRepository<BookStore, Long> {
}
