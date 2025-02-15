package org.babyfish.jimmer.sql.model.issue918;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "ISSUE918_MODEL")
@KeyUniqueConstraint
public interface Issue918Model {

    /**
     * Id $:)$
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * Name $:)$
     */
    @Key
    String name();
}
