package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.Key;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Entity
public interface Author {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequence:AUTHOR_ID_SEQ"
    )
    long id();

    @Key
    @NotBlank
    String firstName();

    @Key
    @NotBlank
    String lastName();

    Gender gender();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}