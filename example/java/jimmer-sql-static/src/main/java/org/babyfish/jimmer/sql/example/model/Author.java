package org.babyfish.jimmer.sql.example.model;

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
}
