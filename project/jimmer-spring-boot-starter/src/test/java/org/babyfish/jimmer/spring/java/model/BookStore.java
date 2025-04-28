package org.babyfish.jimmer.spring.java.model;

import org.babyfish.jimmer.spring.java.bll.resolver.BookStoreNewestBooksResolver;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.Transient;

import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    UUID id();

    String name();

    /**
     * All books available in this bookstore
     */
    @OneToMany(mappedBy = "store")
    List<Book> books();

    @Transient(ref = "bookStoreNewestBooksResolver")
    List<Book> newestBooks();
}
