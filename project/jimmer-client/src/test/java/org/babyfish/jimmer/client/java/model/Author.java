package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.client.Doc;
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

    @Doc("All the books i have written")
    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
