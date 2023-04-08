package org.babyfish.jimmer.example.cloud.book;

import org.babyfish.jimmer.example.cloud.model.Book;
import org.babyfish.jimmer.spring.repository.JRepository;

public interface BookRepository extends JRepository<Book, Long> {}
