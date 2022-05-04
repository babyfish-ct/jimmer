package org.babyfish.jimmer.sql.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.List;
import java.util.UUID;

@Entity
public interface Author {

    @Id
    UUID id();

    String firstName();

    String lastName();

    @ManyToMany(mappedBy = "author")
    List<Book> books();
}
