package org.babyfish.jimmer.sql.model.inheritance.singletable;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "SINGLE_CLIENT_PROJECT")
public interface ClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @Nullable
    @ManyToOne
    Client client();

    @ManyToMany
    @JoinTable(
            name = "SINGLE_CLIENT_PROJECT_PARTICIPANT_MAPPING",
            joinColumnName = "PROJECT_ID",
            inverseJoinColumnName = "CLIENT_ID",
            readonly = true,
            cascadeDeletedByTarget = true
    )
    List<Client> participants();
}
