package org.babyfish.jimmer.sql.model.wild;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity
public interface Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Column(name = "NAME")
    String taskName();

    @Nullable
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Worker owner();
}
