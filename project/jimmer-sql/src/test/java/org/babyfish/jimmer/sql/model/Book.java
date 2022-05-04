package org.babyfish.jimmer.sql.model;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface Book {

    UUID id();

    String name();

    BigDecimal price();

    @ManyToOne
    BookStore store();

    @ManyToOne
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumns = @JoinColumn(name = "BOOK_ID"),
            inverseJoinColumns = @JoinColumn(name = "AUTHOR_ID")
    )
    List<Author> authors();
}
