package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.entities.common.TenantAware;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Entity
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

    @Null // // Null property, Java API requires this annotation, but kotlin API does not
    @ManyToOne
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
    List<Author> authors();
}

