package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "AUTHOR_COUNTRY")
public interface Country {

    @Id
    String code();

    String name();

    @OneToMany(mappedBy = "country")
    List<Author> authors();
}
