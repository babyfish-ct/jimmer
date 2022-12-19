package org.babyfish.jimmer.spring.java.model;

import org.babyfish.jimmer.client.Doc;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToMany;

import java.util.List;
import java.util.UUID;

@Entity
public interface Author {

    @Id
    UUID id();

    String firstName();

    String lastName();

    Gender gender();

    @Doc("All the books i have written")
    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
