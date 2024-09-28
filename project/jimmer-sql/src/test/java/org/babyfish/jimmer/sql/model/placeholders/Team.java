package org.babyfish.jimmer.sql.model.placeholders;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "TEAMS")
public interface Team {
    @Id
    long id();

    @Column(name = "TEAM_NAME")
    String name();

    @OneToMany(mappedBy = "team")
    List<Player> players();
}
