package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

public class BaseQueryScope {

    private final BaseTableImplementor table;

    private final Map<BaseTable, Map<Integer, BaseSelectionMapper>> mapperMap =
            new LinkedHashMap<>();

    private int colNoSequence;

    public BaseQueryScope(BaseTableImplementor table) {
        this.table = table;
    }

    public BaseTableImplementor table() {
        return table;
    }

    public BaseSelectionMapper mapper(BaseTableOwner baseTableOwner, BaseTable baseTable) {
        return mapperMap
                .computeIfAbsent(baseTable, it -> new LinkedHashMap<>())
                .computeIfAbsent(baseTableOwner.index, it -> new BaseSelectionMapper(this));
    }

    int colNo() {
        return ++colNoSequence;
    }

    public BaseSelectionRender toBaseSelectionRender(BaseTable baseTable) {
        Map<Integer, BaseSelectionMapper> map = mapperMap.get(baseTable);
        if (map == null) {
            return null;
        }
        return new BaseSelectionRenderImpl(map);
    }

    private static class BaseSelectionRenderImpl implements BaseSelectionRender {

        private final Map<Integer, BaseSelectionMapper> map;

        private BaseSelectionRenderImpl(Map<Integer, BaseSelectionMapper> map) {
            this.map = map;
        }


        @Override
        public void render(int index, Selection<?> selection, SqlBuilder builder) {
            BaseSelectionMapper mapper = map.get(index);
            if (mapper == null) {
                return;
            }
            if (selection instanceof Expression<?>) {
                builder.separator();
                ((Ast) selection).renderTo(builder);
                builder.sql(" c").sql(Integer.toString(mapper.expressionIndex));
                return;
            }
            for (Map.Entry<String, Integer> e : mapper.indexMap.entrySet()) {
                builder.separator()
                        .sql(e.getKey())
                        .sql(" c").sql(Integer.toString(e.getValue()));
            }
        }
    }
}
