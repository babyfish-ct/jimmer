package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.persistence.*;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface Book {

    @Id
    @GeneratedValue(generator = UUIDIdGenerator.FULL_NAME)
    UUID id();

    @Key
    String name();

    @Key
    int edition();

    @Positive
    BigDecimal price();

    @ManyToOne
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumns = @JoinColumn(name = "BOOK_ID"),
            inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID")
    )
    List<Author> authors();
}
