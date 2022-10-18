package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.TenantAware;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;

@Entity
public interface BookStore extends TenantAware {

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
