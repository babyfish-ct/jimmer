package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface BookStore {

    @Id
    long id();

    String name();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
