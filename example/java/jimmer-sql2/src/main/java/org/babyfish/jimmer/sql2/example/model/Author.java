package org.babyfish.jimmer.sql2.example.model;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql2.example.model.common.BaseEntity;

import java.util.List;

@Entity
@StaticType(
        alias = "simple",
        topLevelName = "SimpleAuthorDto",
        autoScalarStrategy = AutoScalarStrategy.NONE
)
@StaticType(alias = "default", topLevelName = "AuthorDto")
@StaticType(alias = "complex", topLevelName = "ComplexAuthorDto")
@StaticType(alias = "input", topLevelName = "AuthorInput")
public interface Author extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Static(alias = "simple")
    long id();

    @Key
    @Static(alias = "simple")
    String firstName();

    @Key
    @Static(alias = "simple")
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors", orderedProps = {
            @OrderedProp("name"),
            @OrderedProp(value = "edition", desc = true)
    })
    @Static(alias = "complex", targetAlias = "forComplexAuthorDto")
    List<Book> books();
}
