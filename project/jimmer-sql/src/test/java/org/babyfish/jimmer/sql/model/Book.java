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
@StaticType(topLevelName = "BookInput")
@StaticType(alias = "simple", topLevelName = "SimpleBookInput")
public interface Book {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    @Static(optional = true)
    @Static(ownerAlias = "simple")
    UUID id();

    @Key
    String name();

    @Key
    int edition();

    @Positive
    @Static(enabled = false, ownerAlias = "simple")
    BigDecimal price();

    @Null
    @ManyToOne
    @Static(asTargetId = true)
    BookStore store();

    @ManyToMany
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumnName = "BOOK_ID",
            inverseJoinColumnName = "AUTHOR_ID"
    )
    @Static(asTargetId = true)
    List<Author> authors();
}
