package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.AbstractBaseTable;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimpleBaseTables {

    private SimpleBaseTables() {}

    public static BaseTableImplementor of(
            TypedBaseQueryImplementor<?> query,
            List<Selection<?>> selections
    ) {
        switch (selections.size()) {
            case 1:
                return new Table1<>(query, selections);
            case 2:
                return new Table2<>(query, selections);
            case 3:
                return new Table3<>(query, selections);
            default:
                throw new IllegalArgumentException("Illegal selection count: " + selections.size());
        }
    }

    private static class Table1<S1 extends Selection<?>>
            extends AbstractBaseTable
            implements BaseTable1<S1>, BaseTableImplementor {

        protected Table1(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
            super(query, selections);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S1 get_1() {
            return (S1) selections.get(0);
        }

        @Override
        public String toString() {
            return "BaseTable1{" +
                    "_1=" + selections.get(0) +
                    '}';
        }
    }

    private static class Table2<S1 extends Selection<?>, S2 extends Selection<?>>
            extends AbstractBaseTable
            implements BaseTable2<S1, S2>, BaseTableImplementor {

        protected Table2(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
            super(query, selections);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S1 get_1() {
            return (S1) selections.get(0);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S2 get_2() {
            return (S2) selections.get(1);
        }

        @Override
        public String toString() {
            return "BaseTable2{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    '}';
        }
    }

    private static class Table3<S1 extends Selection<?>, S2 extends Selection<?>, S3 extends Selection<?>>
            extends AbstractBaseTable
            implements BaseTable3<S1, S2, S3>, BaseTableImplementor {

        protected Table3(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
            super(query, selections);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S1 get_1() {
            return (S1) selections.get(0);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S2 get_2() {
            return (S2) selections.get(1);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S3 get_3() {
            return (S3) selections.get(2);
        }

        @Override
        public String toString() {
            return "BaseTable3{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    '}';
        }
    }
}
