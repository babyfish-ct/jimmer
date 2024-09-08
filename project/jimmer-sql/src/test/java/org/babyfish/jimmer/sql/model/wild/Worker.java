package org.babyfish.jimmer.sql.model.wild;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
public interface Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @OneToMany(mappedBy = "owner")
    List<Task> tasks();
}
