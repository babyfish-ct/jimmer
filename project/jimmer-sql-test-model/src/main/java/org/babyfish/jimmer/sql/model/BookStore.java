package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.model.calc.BookStoreAvgPriceResolver;
import org.babyfish.jimmer.sql.model.calc.BookStoreNameWithVersionResolver;
import org.babyfish.jimmer.sql.model.calc.BookStoreNewestBooksResolver;
import org.jspecify.annotations.Nullable;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The BookStore entity $:)$
 */
@Entity
@KeyUniqueConstraint
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

    @Transient(BookStoreNameWithVersionResolver.class)
    String nameWithVersion();

    @Nullable
    @Transient(ref = "bookStoreMostPopularAuthorResolver")
    Author mostPopularAuthor();

    @Transient(BookStoreNewestBooksResolver.class)
    List<Book> newestBooks();

    // For issue714
    @Transient(BookStoreNewestBooksResolver.class)
    List<UUID> newestBookIds();

    @Formula(dependencies = "books.price")
    default BigDecimal maxPrice() {
        BigDecimal maxPrice = BigDecimal.ZERO;
        for (Book book : books()) {
            BigDecimal price = book.price();
            if (maxPrice.compareTo(price) < 0) {
                maxPrice = price;
            }
        }
        return maxPrice;
    }
}
