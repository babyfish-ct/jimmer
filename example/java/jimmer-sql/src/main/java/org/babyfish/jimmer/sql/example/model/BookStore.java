package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
public interface BookStore extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Null
    String website();

    @Transient(BookStoreAvgPriceResolver.class)
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
