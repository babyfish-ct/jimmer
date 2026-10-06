package org.babyfish.jimmer.sql.model.steam;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "STEAM_BUNDLE")
@KeyUniqueConstraint
public interface Bundle {

    @Id
    String id();

    @Key
    int bundleId();

    @ManyToMany
    @JoinTable(name = "STEAM_BUNDLE_GAME", joinColumnName = "BUNDLE_ID", inverseJoinColumnName = "GAME_ID")
    List<Game> games();
}
