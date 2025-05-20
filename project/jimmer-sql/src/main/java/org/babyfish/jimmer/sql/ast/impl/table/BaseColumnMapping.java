package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.Expression;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface BaseColumnMapping {

    String map(String selectedName);

    String map(Expression<?> expression);

    static BaseColumnMapping empty() {
        return EmptyBaseColumnMapping.INSTANCE;
    }

    static BaseColumnMapping of(String alias, Map<Object, Integer> map) {
        return new DefaultBaseColumnMapping(alias, map);
    }
}

class EmptyBaseColumnMapping implements BaseColumnMapping {

    static final BaseColumnMapping INSTANCE = new EmptyBaseColumnMapping();

    @Override
    public String map(String selectedName) {
        return selectedName;
    }

    @Override
    public String map(Expression<?> expression) {
        throw new UnsupportedOperationException();
    }
}

class DefaultBaseColumnMapping implements BaseColumnMapping {

    private final String outerAlias;

    private final Map<Object, Integer> map;

    DefaultBaseColumnMapping(String outerAlias, Map<Object, Integer> map) {
        this.outerAlias = outerAlias;
        this.map = map;
    }

    @Override
    public String map(String selectedName) {
        Integer index = map.computeIfAbsent(selectedName, k -> map.size() + 1);
        return outerAlias + ".c" + index;
    }

    @Nullable
    @Override
    public String map(Expression<?> expression) {
        Integer index = map.computeIfAbsent(expression, k -> map.size() + 1);
        return outerAlias + ".c" + index;
    }
}
