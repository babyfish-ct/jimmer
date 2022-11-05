package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntity;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

@Entity
public interface BookStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @NotBlank
    String name();

    @Nullable
    @NotBlank
    String website();

    @Transient(BookStoreAvgPriceResolver.class)
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store", orderedProps = @OrderedProp("name"))
    List<Book> books();
}