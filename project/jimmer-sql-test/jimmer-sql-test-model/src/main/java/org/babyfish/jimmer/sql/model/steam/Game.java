package org.babyfish.jimmer.sql.model.steam;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "STEAM_GAME")
@KeyUniqueConstraint
public interface Game {

    @Id
    String id();

    @Key
    int appId();
}
