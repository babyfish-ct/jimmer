package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.client.Doc;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity
public interface Book {

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

    @IdView
    Long storeId();

    @IdView("authors")
    List<Long> authorIds();
}
