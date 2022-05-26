package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.Key;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Entity
public interface Book {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequence:BOOK_ID_SEQ"
    )
    long id();

    @Key
    @NotBlank
    String name();

    @Key
    @Positive
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

