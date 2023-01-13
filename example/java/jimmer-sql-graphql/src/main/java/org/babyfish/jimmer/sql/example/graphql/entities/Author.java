package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.example.graphql.entities.common.BaseEntity;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Entity
@StaticType(
        alias = "default",
        topLevelName = "AuthorInput",
        autoScalarStrategy = AutoScalarStrategy.DECLARED
)
public interface Author extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @NotBlank
    String firstName();

    @Key
    @NotBlank
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors", orderedProps = @OrderedProp("name"))
    List<Book> books();
}