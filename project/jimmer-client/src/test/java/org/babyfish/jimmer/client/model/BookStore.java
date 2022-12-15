package org.babyfish.jimmer.client.model;

import org.babyfish.jimmer.client.Doc;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface BookStore {

    @Id
    long id();

    String name();

    @Doc("All books available in this bookstore")
    @OneToMany(mappedBy = "store")
    List<Book> books();
}
