package org.babyfish.jimmer.sql.model.enumeration;

import org.babyfish.jimmer.sql.*;

@DatabaseValidationIgnore
@Entity
public interface Article {

    @Id
    @GeneratedValue
    long id();

    String name();

    @ManyToOne
    Writer writer();
}
