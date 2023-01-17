package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.TenantAware;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;

@Entity
@StaticType(
        alias = "default",
        topLevelName = "BookInput",
        autoScalarStrategy = AutoScalarStrategy.DECLARED
)
@StaticType(
        alias = "composite",
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

    @Null // Null property, Java API requires this annotation, but kotlin API does not
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

    /*
     * Here, `chapters` is mapped to static type, the `targetAlias` in `forCompositeBookInput`.
     *
     * The target type `Chapter` is decorated by the annotation @StaticType, whose `alias`
     * is `forCompositeBookInput` and `autoScalarStrategy` is `AutoScalarStrategy.DECLARED`. That means
     * only the properties declared in `Chapter` should be mapped automatically, not include
     * the properties inherited from `BaseEntity`.
     *
     * There is another solution: using `@Static(enabled = false)` to decorate properties
     * of `BaseEntity`, let those properties cannot be mapped into static types. That means
     * it is unnecessary to specify the static type of `Chapter` with `AutoScalarStrategy.DECLARED`,
     * at this time, the `targetAlias` of the current property can be unspecified.
     */
    @OneToMany(mappedBy = "book", orderedProps = @OrderedProp("index"))
    @Static(alias = "composite", targetAlias = "forCompositeBookInput")
    List<Chapter> chapters();
}
