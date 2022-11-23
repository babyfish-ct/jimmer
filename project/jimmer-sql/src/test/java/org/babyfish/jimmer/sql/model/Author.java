package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import org.babyfish.jimmer.sql.*;
import java.util.List;
import java.util.UUID;

@Entity
public interface Author {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @Key
    String firstName();

    @Key
    String lastName();

    default String fullName() {
        return firstName() + lastName();
    }

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();

    @ManyToOne
    @JoinTable(
            name = "AUTHOR_COUNTRY_MAPPING",
            joinColumnName = "AUTHOR_ID",
            inverseJoinColumnName = "COUNTRY_CODE"
    )
    Country country();

    @Transient
    Organization organization();
}
