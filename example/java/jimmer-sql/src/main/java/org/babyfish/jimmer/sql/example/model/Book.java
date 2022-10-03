package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface Book extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Key
    int edition();

    BigDecimal price();

    @Null // Null property, Java API requires this annotation, but kotlin API does not
    @ManyToOne
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumnName = "BOOK_ID",
            inverseJoinColumnName = "AUTHOR_ID"
    )
    List<Author> authors();
}
