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
    @Static(alias = "default", name="authorIds", idOnly = true)
    @Static(alias = "composite", targetAlias = "declaredOnly")
    List<Author> authors();

    @OneToMany(mappedBy = "book", orderedProps = @OrderedProp("index"))
    List<Chapter> chapters();
}
