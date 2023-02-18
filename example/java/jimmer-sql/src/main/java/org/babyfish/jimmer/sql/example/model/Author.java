package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.model.common.BaseEntity;

import java.util.List;

@Entity
public interface Author extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String firstName();

    @Key
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors", orderedProps = {
            @OrderedProp("name"),
            @OrderedProp(value = "edition", desc = true)
    })
    List<Book> books();

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The simple calculated properties are shown here. As for the
    // complex calculated properties, you can view `BookStore.avgPrice` and
    // `BookStore.newestBooks`
    // -----------------------------

    @Formula(dependencies = {"firstName", "lastName"})
    default String fullName() {
        return firstName() + ' ' + lastName();
    }

    // The simple property above is simple calculation based on JAVA expression,
    // you can also define simple calculations given SQL expressions like this
    //
    // @Formula(sql = "concat(%alias.FIRST_NAME, ' ', %alias.LAST_NAME)")
    // String fullName();
}
