package org.babyfish.jimmer.client.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToMany;

import java.util.List;

@Entity
public interface Author {

    @Id
    long id();

    String firstName();

    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
