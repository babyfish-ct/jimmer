package org.babyfish.jimmer.sql2.example.model;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql2.example.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity
@StaticType(
        alias = "simple",
        topLevelName = "SimpleBookDto",
        autoScalarStrategy = AutoScalarStrategy.NONE
)
@StaticType(alias = "default", topLevelName = "BookDto")
@StaticType(alias = "complex", topLevelName = "ComplexBookDto")
@StaticType(alias = "forComplexAuthorDto")
@StaticType(
        alias = "input",
        topLevelName = "BookInput",
        autoScalarStrategy = AutoScalarStrategy.DECLARED
)
@StaticType(
        alias = "compositeInput",
        topLevelName = "CompositeBookInput",
        autoScalarStrategy = AutoScalarStrategy.DECLARED
)
public interface Book extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Static(alias = "simple")
    long id();

    @Key
    @Static(alias = "simple")
    String name();

    @Key
    @Static(alias = "simple")
    int edition();

    BigDecimal price();

    @Nullable // Null property, Java API requires this annotation, but kotlin API does not
    @ManyToOne
    @Static(name = "storeId", idOnly = true)
    @Static(alias = "complex", targetAlias = "forComplexBookDto")
    @Static(alias = "forComplexAuthorDto", targetAlias = "forComplexAuthorDto")
    BookStore store();

    @ManyToMany(orderedProps = {
            @OrderedProp("firstName"),
            @OrderedProp("lastName")
    })
    @JoinTable(
            name = "BOOK_AUTHOR_MAPPING",
            joinColumnName = "BOOK_ID",
            inverseJoinColumnName = "AUTHOR_ID"
    )
    @Static(name = "authorIds", idOnly = true)
    @Static(alias = "complex")
    List<Author> authors();

    @OneToMany(mappedBy = "book", orderedProps = @OrderedProp("index"))
    @Static(alias = "compositeInput")
    List<Chapter> chapters();
}
