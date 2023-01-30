package org.babyfish.jimmer.sql2.example.model;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql2.example.dal.calc.BookStoreAvgPriceResolver;
import org.babyfish.jimmer.sql2.example.model.common.BaseEntity;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;

@Entity
@StaticType(
        alias = "simple",
        topLevelName = "SimpleBookStoreDto",
        autoScalarStrategy = AutoScalarStrategy.NONE
)
@StaticType(alias = "default", topLevelName = "BookStoreDto")
@StaticType(alias = "complex", topLevelName = "ComplexBookStoreDto")
@StaticType(alias = "forComplexBookDto")
@StaticType(alias = "forComplexAuthorDto")
@StaticType(alias = "input", topLevelName = "BookStoreInput")
public interface BookStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Static(alias = "simple")
    long id();

    @Key
    @Static(alias = "simple")
    String name();

    @Null
    String website();

    @Transient(BookStoreAvgPriceResolver.class)
    @Static(alias = "forComplexBookDto")
    @Static(alias = "forComplexAuthorDto")
    BigDecimal avgPrice();

    @OneToMany(mappedBy = "store", orderedProps = {
            @OrderedProp("name"),
            @OrderedProp(value = "edition", desc = true)
    })
    @Static(alias = "complex")
    List<Book> books();
}
