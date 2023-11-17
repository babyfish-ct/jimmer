package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.client.Doc;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongConverter;
import org.babyfish.jimmer.jackson.LongListConverter;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity
public interface Book {

    @JsonConverter(LongConverter.class)
    @Id
    long id();

    String name();

    int edition();

    BigDecimal price();

    @Doc("The bookstore to which the current book belongs, null is allowd")
    @ManyToOne
    @Nullable
    BookStore store();

    @Doc("All authors involved in writing the work")
    @ManyToMany
    List<Author> authors();

    @JsonConverter(LongConverter.class)
    @IdView
    Long storeId();

    @JsonConverter(LongListConverter.class)
    @IdView("authors")
    List<Long> authorIds();
}
