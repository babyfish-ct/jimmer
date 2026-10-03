package org.babyfish.jimmer.sql.model.ld;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface LifecycleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String code();

    String name();

    @Default("NEW")
    @LogicalDeleted("DELETED")
    Status status();

    enum Status {
        NEW,
        RUNNING,
        DELETED
    }
}
