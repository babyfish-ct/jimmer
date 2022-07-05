package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
public interface Author {

    @Id
    @GeneratedValue(generator = UUIDIdGenerator.FULL_NAME)
    UUID id();

    @Key
    String firstName();

    @Key
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();

    @ManyToOne
    @JoinTable(
            name = "AUTHOR_COUNTRY_MAPPING",
            joinColumns = @JoinColumn(name = "AUTHOR_ID"),
            inverseJoinColumns = @JoinColumn(name = "COUNTRY_CODE")
    )
    Country country();
}
