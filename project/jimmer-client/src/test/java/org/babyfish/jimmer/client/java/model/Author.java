package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToMany;

import java.util.List;

@Entity
public interface Author {

    @Id
    @JsonConverter(LongToStringConverter.class)
    long id();

    FullName fullName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
