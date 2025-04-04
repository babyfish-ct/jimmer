package org.babyfish.jimmer.sql.placeholders;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.placeholders.PlayerFetcher;
import org.babyfish.jimmer.sql.model.placeholders.TeamFetcher;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PlaceholdersTest extends AbstractQueryTest {

    @Test
    public void testFetchWithPlaceholders() {
        TeamFetcher teamFetcher = TeamFetcher.$
                .allScalarFields()
                .players(PlayerFetcher.$.allScalarFields());

        connectAndExpect(con ->
                getSqlClient()
                        .getEntities()
                        .forConnection(con)
                        .findById(teamFetcher, 1L), it -> {
            it.sql(
                    "select tb_1_.ID, tb_1_.TEAM_NAME " +
                    "from TEAMS tb_1_ " +
                    "where tb_1_.ID = ?"
            ).variables(1L);

            it.statement(1).sql(
                    "select tb_1_.ID, tb_1_.PLAYER_NAME " +
                    "from public.PLAYERS tb_1_ " +
                    "where tb_1_.TEAM_ID = ?"
            ).variables(1L);
        });
    }

    @Override
    protected JSqlClient getSqlClient(Consumer<JSqlClient.Builder> block) {
        Map<String, String> props = new HashMap<>();
        props.put("${schema}", "public");
        props.put("${tables.player}", "PLAYERS");
        props.put("${columns.player.name}", "PLAYER_NAME");
        props.put("${columns.player.teamId}", "TEAM_ID");

        return super.getSqlClient(builder -> {
            block.accept(builder);
            builder.setMetaStringResolver(value -> props.getOrDefault(value, value));
        });
    }
}