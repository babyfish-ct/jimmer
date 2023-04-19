package org.babyfish.jimmer.example.cloud.model;

import org.babyfish.jimmer.example.cloud.model.common.BaseEntity;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity(microServiceName = "book-service")
public interface Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Key
    int edition();

    BigDecimal price();

    @Nullable
    @ManyToOne
    BookStore store();

    @ManyToMany(orderedProps = {
            @OrderedProp("firstName"),
            @OrderedProp("lastName")
    })
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumnName = "BOOK_ID",
            inverseJoinColumnName = "AUTHOR_ID"
    )
    List<Author> authors();
}
