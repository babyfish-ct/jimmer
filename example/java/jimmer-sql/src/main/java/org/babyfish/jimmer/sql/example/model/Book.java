package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

@Entity
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
    long id();

    @Key
    String name();

    @Key
    int edition();

    BigDecimal price();

    @Nullable // Null property, Java API requires this annotation, but kotlin API does not
    @ManyToOne
    @Static(name = "storeId", idOnly = true)
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
    @Static(name="authorIds", idOnly = true)
    List<Author> authors();

    @OneToMany(mappedBy = "book", orderedProps = @OrderedProp("index"))
    @Static(alias = "compositeInput")
    List<Chapter> chapters();
}
