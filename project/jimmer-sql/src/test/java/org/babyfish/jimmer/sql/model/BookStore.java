package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.persistence.*;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
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

    @Version
    int version();

    @Transient
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
