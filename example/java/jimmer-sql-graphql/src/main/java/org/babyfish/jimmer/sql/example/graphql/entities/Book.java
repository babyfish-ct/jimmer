package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.entities.common.TenantAware;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Entity
@StaticType(
        alias = "default",
        topLevelName = "BookInput",
        autoScalarStrategy = AutoScalarStrategy.DECLARED
)
public interface Book extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @NotBlank
    String name();

    @Key
    @Positive
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
    @Static(name = "authorIds", idOnly = true)
    List<Author> authors();

    /*
     * Here, `chapters` is mapped to static type, the `targetAlias` in `forBookInput`.
     *
     * The target type `Chapter` is decorated by the annotation @StaticType, whose `alias`
     * is `forBookInput` and `autoScalarStrategy` is `AutoScalarStrategy.DECLARED`. That means
     * only the properties declared in `Chapter` should be mapped automatically, not include
     * the properties inherited from `BaseEntity`.
     *
     * There is another solution: using `@Static(enabled = false)` to decorate properties
     * of `BaseEntity`, let those properties cannot be mapped into static types. That means
     * it is unnecessary to specify the static type of `Chapter` with `AutoScalarStrategy.DECLARED`,
     * at this time, the `targetAlias` of the current property can be unspecified.
     */
    @OneToMany(mappedBy = "book", orderedProps = @OrderedProp("index"))
    @Static(targetAlias = "forBookInput")
    List<Chapter> chapters();
}

