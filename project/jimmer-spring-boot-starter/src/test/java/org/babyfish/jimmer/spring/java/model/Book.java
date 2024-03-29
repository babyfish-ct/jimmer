package org.babyfish.jimmer.spring.java.model;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface Book {

    @Id
    UUID id();

    String name();

    int edition();

    BigDecimal price();

    /**
     * The bookstore to which the current book belongs, null is allowed
     */
    @ManyToOne
    @Nullable
    BookStore store();

    /**
     * All authors involved in writing the work
     */
    @ManyToMany
    @JoinTable(deletedWhenEndpointIsLogicallyDeleted = true)
    List<Author> authors();
}
