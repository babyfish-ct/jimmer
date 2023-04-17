package org.babyfish.jimmer.sql.meta;

import java.util.Collections;
import java.util.List;

public class FormulaTemplate extends SqlTemplate {

    private static final Placeholder PLACEHOLDER = new Placeholder("alias");

    private FormulaTemplate(List<Object> parts) {
        super(parts);
    }

    public static FormulaTemplate of(String sql) {
        return create(sql, Collections.singletonList(PLACEHOLDER), FormulaTemplate::new);
    }

    public String toSql(String alias) {
        return toSql(Collections.singletonMap(PLACEHOLDER, alias));
    }
}
