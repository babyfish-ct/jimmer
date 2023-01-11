package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.validation.constraints.Null;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@StaticType(alias = "default", topLevelName = "BookInput")
@StaticType(alias = "complex", topLevelName = "CompositeBookInput")
public interface Book {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    @Static(optional = true)
    UUID id();

    @Key
    String name();

    @Key
    int edition();

    @Positive
    BigDecimal price();

    @Null
    @ManyToOne
    @Static(alias = "default", name = "storeId", idOnly = true)
    @Static(alias = "complex")
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumnName = "BOOK_ID",
            inverseJoinColumnName = "AUTHOR_ID"
    )
    @Static(alias = "default", name = "authorIds", idOnly = true)
    @Static(alias = "complex")
    List<Author> authors();
}
