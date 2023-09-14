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

    // It is inappropriate to use `firstName` and `lastName`
    // as keys in actual project, but this is just a small demo.

    @Key // ❶
    String firstName();

    @Key // ❷
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors", orderedProps = { // ❸
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

    @Formula(dependencies = {"firstName", "lastName"}) // ❹
    default String fullName() {
        return firstName() + ' ' + lastName();
    }

    // The simple property above is simple calculation based on JAVA expression,
    // you can also define simple calculations given SQL expressions like this
    //
    // @Formula(sql = "concat(%alias.FIRST_NAME, ' ', %alias.LAST_NAME)")
    // String fullName();
}

/*----------------Documentation Links----------------
❶ ❷ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❸ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-many
❹ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/calculated/formula
---------------------------------------------------*/
