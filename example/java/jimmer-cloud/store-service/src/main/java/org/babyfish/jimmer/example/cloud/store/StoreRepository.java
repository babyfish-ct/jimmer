package org.babyfish.jimmer.example.cloud.store;

import org.babyfish.jimmer.example.cloud.model.BookStore;
import org.babyfish.jimmer.spring.repository.JRepository;

public interface StoreRepository extends JRepository<BookStore, Long> {
}
