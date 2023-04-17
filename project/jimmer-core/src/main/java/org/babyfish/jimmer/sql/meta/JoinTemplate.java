package org.babyfish.jimmer.sql.meta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinTemplate extends SqlTemplate {

    private static final Placeholder PLACEHOLDER = new Placeholder("alias");

    private static final Placeholder TARGET_PLACEHOLDER = new Placeholder("target_alias");

    private JoinTemplate(List<Object> parts) {
        super(parts);
    }

    public static JoinTemplate of(String sql) {
        return create(sql, Arrays.asList(PLACEHOLDER, TARGET_PLACEHOLDER), JoinTemplate::new);
    }

    public String toSql(String alias, String targetAlias) {
        Map<Placeholder, String> valueMap = new HashMap<>();
        valueMap.put(PLACEHOLDER, alias);
        valueMap.put(TARGET_PLACEHOLDER, targetAlias);
        return toSql(valueMap);
    }
}
