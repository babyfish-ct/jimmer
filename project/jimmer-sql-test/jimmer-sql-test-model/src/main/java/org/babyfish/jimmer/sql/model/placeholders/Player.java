package org.babyfish.jimmer.sql.model.placeholders;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "${tables.player}", schema = "${schema}")
public interface Player {
    @Id
    long id();

    @Column(name = "${columns.player.name}")
    String name();

    @ManyToOne
    @JoinColumn(name = "${columns.player.teamId}", referencedColumnName = "ID")
    Team team();
}
