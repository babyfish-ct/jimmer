package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongConverter;
import org.babyfish.jimmer.jackson.LongListConverter;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * The book object
 */
@Entity
public interface Book {

    /**
     * The id is long, but the client type is string
     * because JS cannot retain large long values
     */
    @JsonConverter(LongConverter.class)
    @Id
    long id();

    /**
     * The name of this book,
     * <p>Together with `edition`, this property forms the key of the book</p>
     */
    String name();

    /**
     * The edition of this book,
     * <p>Together with `name`, this property forms the key of the book</p>
     */
    int edition();

    /**
     * The price of this book
     */
    BigDecimal price();

    /**
     * The many-to-one association from `Book` to `BookStore`
     */
    @ManyToOne
    @Nullable
    BookStore store();

    /**
     * The many-to-many association from `Book` to `Author`
     */
    @ManyToMany
    List<Author> authors();

    /**
     * The id view of `Book.store`
     */
    @JsonConverter(LongConverter.class)
    @IdView
    Long storeId();

    /**
     * The id view of `Book.authors`
     */
    @JsonConverter(LongListConverter.class)
    @IdView("authors")
    List<Long> authorIds();
}
