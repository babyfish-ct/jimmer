package org.babyfish.jimmer.client.java.model;

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

    @ManyToOne
    @Nullable
    BookStore store();

    @ManyToMany
    List<Author> authors();

    @JsonConverter(LongConverter.class)
    @IdView
    Long storeId();

    @JsonConverter(LongListConverter.class)
    @IdView("authors")
    List<Long> authorIds();
}
