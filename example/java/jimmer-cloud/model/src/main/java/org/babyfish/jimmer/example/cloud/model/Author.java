package org.babyfish.jimmer.example.cloud.model;

import org.babyfish.jimmer.example.cloud.model.common.BaseEntity;
import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity(microServiceName = "author-service")
public interface Author extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String firstName();

    @Key
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
