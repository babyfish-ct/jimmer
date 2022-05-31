package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore {

    @Id
    @GeneratedValue(generator = UUIDIdGenerator.FULL_NAME)
    UUID id();

    @Key
    String name();

    @Null
    String website();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
