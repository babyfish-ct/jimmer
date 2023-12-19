package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface BookStore {

    @Id
    @JsonConverter(LongToStringConverter.class)
    long id();

    String name();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
