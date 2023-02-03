package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.dal.calc.BookStoreAvgPriceResolver;
import org.babyfish.jimmer.sql.example.model.common.BaseEntity;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;

@Entity
public interface BookStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Null
    String website();

    @Transient(BookStoreAvgPriceResolver.class)
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store", orderedProps = {
            @OrderedProp("name"),
            @OrderedProp(value = "edition", desc = true)
    })
    List<Book> books();
}
