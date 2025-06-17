package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.base.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class BaseTableSymbols {

    private BaseTableSymbols() {}

    public static boolean contains(TableLike<?> table1, BaseTableSymbol table2) {
        BaseTableSymbol parentTable2 = table2.getParent();
        if (parentTable2 != null) {
            return contains(table1, parentTable2);
        }
        return table1 == table2;
    }

    public static BaseTableSymbol of(
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
            case 4:
                return new Table4<>(query, selections);
            case 5:
                return new Table5<>(query, selections);
            case 6:
                return new Table6<>(query, selections);
            case 7:
                return new Table7<>(query, selections);
            case 8:
                return new Table8<>(query, selections);
            case 9:
                return new Table9<>(query, selections);
            default:
                throw new IllegalArgumentException("Illegal selection count: " + selections.size());
        }
    }

    public static BaseTableSymbol of(
            BaseTableSymbol base,
            BaseTableSymbol parent,
            WeakJoinHandle handle,
            JoinType joinType
    ) {
        switch (base.getSelections().size()) {
            case 1:
                return new Table1<>(base, (AbstractBaseTableSymbol) parent, handle, joinType);
            default:
                throw new IllegalArgumentException("Illegal selection count: " + base.getSelections().size());
        }
    }

    private static class Table1<S1 extends Selection<?>>
            extends AbstractBaseTableSymbol
            implements BaseTable1<S1> {

        Table1(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
            super(query, selections);
        }

        Table1(BaseTableSymbol base, AbstractBaseTableSymbol parent, WeakJoinHandle handle, JoinType joinType) {
            super(base, parent, handle, joinType);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S1 get_1() {
            return (S1) selections.get(0);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable1<S1>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType);
        }

        @Override
        public String toString() {
            return "BaseTable1{" +
                    "_1=" + selections.get(0) +
                    (parent != null ? ",parent=" + parent : "") +
                    '}';
        }
    }

    private static class Table2<S1 extends Selection<?>, S2 extends Selection<?>>
            extends AbstractBaseTableSymbol
            implements BaseTable2<S1, S2> {

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
            extends AbstractBaseTableSymbol
            implements BaseTable3<S1, S2, S3> {

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

    private static class Table4<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>
    > extends AbstractBaseTableSymbol
            implements BaseTable4<S1, S2, S3, S4> {

        protected Table4(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S4 get_4() {
            return (S4) selections.get(3);
        }

        @Override
        public String toString() {
            return "BaseTable4{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    ",_4=" + selections.get(3) +
                    '}';
        }
    }

    private static class Table5<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>
    > extends AbstractBaseTableSymbol
            implements BaseTable5<S1, S2, S3, S4, S5> {

        protected Table5(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S4 get_4() {
            return (S4) selections.get(3);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S5 get_5() {
            return (S5) selections.get(4);
        }

        @Override
        public String toString() {
            return "BaseTable4{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    ",_4=" + selections.get(3) +
                    ",_5=" + selections.get(4) +
                    '}';
        }
    }

    private static class Table6<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>
    > extends AbstractBaseTableSymbol
            implements BaseTable6<S1, S2, S3, S4, S5, S6> {

        protected Table6(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S4 get_4() {
            return (S4) selections.get(3);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S5 get_5() {
            return (S5) selections.get(4);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S6 get_6() {
            return (S6) selections.get(5);
        }

        @Override
        public String toString() {
            return "BaseTable4{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    ",_4=" + selections.get(3) +
                    ",_5=" + selections.get(4) +
                    ",_6=" + selections.get(5) +
                    '}';
        }
    }

    private static class Table7<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>
    > extends AbstractBaseTableSymbol
            implements BaseTable7<S1, S2, S3, S4, S5, S6, S7> {

        protected Table7(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S4 get_4() {
            return (S4) selections.get(3);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S5 get_5() {
            return (S5) selections.get(4);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S6 get_6() {
            return (S6) selections.get(5);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S7 get_7() {
            return (S7) selections.get(6);
        }

        @Override
        public String toString() {
            return "BaseTable4{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    ",_4=" + selections.get(3) +
                    ",_5=" + selections.get(4) +
                    ",_6=" + selections.get(5) +
                    ",_7=" + selections.get(6) +
                    '}';
        }
    }

    private static class Table8<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>
    > extends AbstractBaseTableSymbol
            implements BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8> {

        protected Table8(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S4 get_4() {
            return (S4) selections.get(3);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S5 get_5() {
            return (S5) selections.get(4);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S6 get_6() {
            return (S6) selections.get(5);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S7 get_7() {
            return (S7) selections.get(6);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S8 get_8() {
            return (S8) selections.get(7);
        }

        @Override
        public String toString() {
            return "BaseTable4{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    ",_4=" + selections.get(3) +
                    ",_5=" + selections.get(4) +
                    ",_6=" + selections.get(5) +
                    ",_7=" + selections.get(6) +
                    ",_8=" + selections.get(7) +
                    '}';
        }
    }

    private static class Table9<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>,
            S9 extends Selection<?>
    > extends AbstractBaseTableSymbol
            implements BaseTable9<S1, S2, S3, S4, S5, S6, S7, S8, S9> {

        protected Table9(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections) {
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

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S4 get_4() {
            return (S4) selections.get(3);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S5 get_5() {
            return (S5) selections.get(4);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S6 get_6() {
            return (S6) selections.get(5);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S7 get_7() {
            return (S7) selections.get(6);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S8 get_8() {
            return (S8) selections.get(7);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull S9 get_9() {
            return (S9) selections.get(8);
        }

        @Override
        public String toString() {
            return "BaseTable4{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    ",_3=" + selections.get(2) +
                    ",_4=" + selections.get(3) +
                    ",_5=" + selections.get(4) +
                    ",_6=" + selections.get(5) +
                    ",_7=" + selections.get(6) +
                    ",_8=" + selections.get(7) +
                    ",_9=" + selections.get(8) +
                    '}';
        }
    }
}
