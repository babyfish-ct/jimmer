package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.model.calc.BookStoreAvgPriceResolver;
import org.babyfish.jimmer.sql.model.calc.BookStoreMostPopularAuthorResolver;
import org.babyfish.jimmer.sql.model.calc.BookStoreNewestBooksResolver;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @Key
    String name();

    @Null
    String website();

    @Version
    int version();

    @OneToMany(mappedBy = "store")
    List<Book> books();

    @Transient(BookStoreAvgPriceResolver.class)
    BigDecimal avgPrice();

    @Nullable
    @Transient(BookStoreMostPopularAuthorResolver.class)
    Author mostPopularAuthor();

    @Transient(BookStoreNewestBooksResolver.class)
    List<Book> newestBooks();
}
