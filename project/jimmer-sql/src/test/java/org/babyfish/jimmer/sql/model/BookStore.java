package org.babyfish.jimmer.sql.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    UUID id();

    String name();

    @ManyToMany(mappedBy = "store")
    List<Book> books();
}
