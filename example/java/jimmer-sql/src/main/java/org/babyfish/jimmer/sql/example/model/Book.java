package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.TenantAware;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.List;

@Entity
@StaticType(alias = "default", topLevelName = "BookInput")
@StaticType(alias = "composite", topLevelName = "CompositeBookInput")
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
    @Static(alias = "default", enabled = false, name="authorIds", idOnly = true)
    @Static(alias = "composite")
    List<Author> authors();
}
