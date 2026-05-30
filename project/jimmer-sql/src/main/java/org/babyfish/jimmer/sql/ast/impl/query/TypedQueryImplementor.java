package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.TupleCreator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface TypedQueryImplementor extends Ast {

    List<Selection<?>> getSelections();

    default TupleCreator<?> getTupleCreator() {
        return null;
    }

    JSqlClientImplementor getSqlClient();

    default void collectSelectionJoinRequirements(SelectionJoinRequirementCollector collector) {}

    default void renderAsMergedOperand(@NotNull AbstractSqlBuilder<?> builder, boolean leading) {
        builder.sql("(");
        renderTo(builder);
        builder.sql(")");
    }

    interface SelectionJoinRequirementCollector {

        QueryAnalysisContext analysisContext();

        void require(Table<?> table);
    }
}
