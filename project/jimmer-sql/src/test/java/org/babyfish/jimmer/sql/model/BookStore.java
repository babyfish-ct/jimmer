package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.meta.sql.UUIDIdGenerator;

import javax.persistence.*;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    @GeneratedValue(generator = UUIDIdGenerator.FULL_NAME)
    UUID id();

    String name();

    @Null
    String website();

    @Version
    int version();

    @ManyToMany(mappedBy = "store")
    List<Book> books();
}
