package org.babyfish.jimmer.sql.example.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    UUID id();

    String name();

    @Null
    String website();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
