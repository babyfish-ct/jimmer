package org.babyfish.jimmer.sql.example.model;

import javax.persistence.*;
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

    @ManyToOne(optional = true)
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumns = @JoinColumn(name = "BOOK_ID"),
            inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID")
    )
    List<Author> authors();
}
