package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.base.*;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BaseTableSymbols {

    private BaseTableSymbols() {}

    public static boolean contains(TableLike<?> table1, BaseTableSymbol table2) {
        return contains0(table1, table2);
    }

    private static boolean contains0(TableLike<?> table1, TableLike<?> table2) {
        if (table1 == table2) {
            return true;
        }
        if (table1 instanceof Table<?> && table2 instanceof Table<?>) {
            return AbstractTypedTable.__refEquals(table1, table2);
        }
        TableLike<?> parentTable2 = TableUtils.parent(table2);
        if (parentTable2 != null) {
            return contains0(table1, parentTable2);
        }
        return false;
    }

    public static BaseTableSymbol of(
            TypedBaseQueryImplementor<?> query,
            List<Selection<?>> selections,
            byte[] kotlinSelectionTypes,
            boolean cte
    ) {
        switch (selections.size()) {
            case 1:
                return new Table1<>(query, selections, kotlinSelectionTypes, cte);
            case 2:
                return new Table2<>(query, selections, kotlinSelectionTypes, cte);
            case 3:
                return new Table3<>(query, selections, kotlinSelectionTypes, cte);
            case 4:
                return new Table4<>(query, selections, kotlinSelectionTypes, cte);
            case 5:
                return new Table5<>(query, selections, kotlinSelectionTypes, cte);
            case 6:
                return new Table6<>(query, selections, kotlinSelectionTypes, cte);
            case 7:
                return new Table7<>(query, selections, kotlinSelectionTypes, cte);
            case 8:
                return new Table8<>(query, selections, kotlinSelectionTypes, cte);
            case 9:
                return new Table9<>(query, selections, kotlinSelectionTypes, cte);
            default:
                throw new IllegalArgumentException("Illegal selection count: " + selections.size());
        }
    }

    public static BaseTableSymbol of(
            BaseTableSymbol base,
            TableLike<?> parent,
            WeakJoinHandle handle,
            JoinType joinType
    ) {
        return of(base, parent, handle, joinType, null);
    }

    public static BaseTableSymbol of(
            RecursiveRef<?> recursiveRef,
            TableLike<?> parent,
            WeakJoinHandle handle,
            JoinType joinType
    ) {
        BaseTableSymbol recursive = (BaseTableSymbol) baseTableOf(recursiveRef);
        return of(recursive, parent, handle, joinType, recursive);
    }

    public static BaseTableSymbol of(
            BaseTableSymbol base,
            TableLike<?> parent,
            WeakJoinHandle handle,
            JoinType joinType,
            BaseTableSymbol recursive
    ) {
        switch (base.getSelections().size()) {
            case 1:
                return new Table1<>(base, parent, handle, joinType, recursive);
            case 2:
                return new Table2<>(base, parent, handle, joinType, recursive);
            case 3:
                return new Table3<>(base, parent, handle, joinType, recursive);
            case 4:
                return new Table4<>(base, parent, handle, joinType, recursive);
            case 5:
                return new Table5<>(base, parent, handle, joinType, recursive);
            case 6:
                return new Table6<>(base, parent, handle, joinType, recursive);
            case 7:
                return new Table7<>(base, parent, handle, joinType, recursive);
            case 8:
                return new Table8<>(base, parent, handle, joinType, recursive);
            case 9:
                return new Table9<>(base, parent, handle, joinType, recursive);
            default:
                throw new IllegalArgumentException("Illegal selection count: " + base.getSelections().size());
        }
    }

    public static <B extends BaseTable> RecursiveRef<B> recursive(B baseTable) {
        return new RecursiveRefImpl<>(baseTable);
    }

    public static <B extends BaseTable> B baseTableOf(RecursiveRef<B> recursiveRef) {
        if (!(recursiveRef instanceof RecursiveRefImpl<?>)) {
            throw new IllegalArgumentException("Unexpected " + RecursiveRef.class.getName());
        }
        return ((RecursiveRefImpl<B>)recursiveRef).baseTable;
    }

    private static class RecursiveRefImpl<B extends BaseTable> implements RecursiveRef<B> {

        private final B baseTable;

        RecursiveRefImpl(B baseTable) {
            this.baseTable = baseTable;
        }
    }

    private static class Table1<S1 extends Selection<?>>
            extends AbstractBaseTableSymbol
            implements BaseTable1<S1> {

        Table1(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table1(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public S1 get_1() {
            return (S1) selections.get(0);
        }

        @Override
        public Table1<S1> query(TypedBaseQueryImplementor<?> query) {
            return new Table1<>(query, wrapSelections(selections, query.asBaseTable()), kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable1<S1>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable1<S1>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable1" + suffix() + "{" +
                    "_1=" + selections.get(0) +
                    (parent != null ? ",parent=" + parent : "") +
                    '}';
        }
    }

    private static class Table2<S1 extends Selection<?>, S2 extends Selection<?>>
            extends AbstractBaseTableSymbol
            implements BaseTable2<S1, S2> {

        Table2(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table2(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table2<S1, S2> query(TypedBaseQueryImplementor<?> query) {
            return new Table2<>(query, wrapSelections(selections, query.asBaseTable()), kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable2<S1, S2>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable2<S1, S2>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable2" + suffix() + "{" +
                    "_1=" + selections.get(0) +
                    ",_2=" + selections.get(1) +
                    '}';
        }
    }

    private static class Table3<S1 extends Selection<?>, S2 extends Selection<?>, S3 extends Selection<?>>
            extends AbstractBaseTableSymbol
            implements BaseTable3<S1, S2, S3> {

        Table3(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table3(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table3<S1, S2, S3> query(TypedBaseQueryImplementor<?> query) {
            return new Table3<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable3<S1, S2, S3>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable3<S1, S2, S3>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable3" + suffix() + "{" +
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

        Table4(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table4(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table4<S1, S2, S3, S4> query(TypedBaseQueryImplementor<?> query) {
            return new Table4<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable4<S1, S2, S3, S4>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable4<S1, S2, S3, S4>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable4" + suffix() + "{" +
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

        Table5(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table5(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table5<S1, S2, S3, S4, S5> query(TypedBaseQueryImplementor<?> query) {
            return new Table5<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable5<S1, S2, S3, S4, S5>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable5<S1, S2, S3, S4, S5>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable5" + suffix() + "{" +
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

        Table6(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table6(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table6<S1, S2, S3, S4, S5, S6> query(TypedBaseQueryImplementor<?> query) {
            return new Table6<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable6<S1, S2, S3, S4, S5, S6>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable6<S1, S2, S3, S4, S5, S6>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable6" + suffix() + "{" +
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

        Table7(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table7(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table7<S1, S2, S3, S4, S5, S6, S7> query(TypedBaseQueryImplementor<?> query) {
            return new Table7<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable7<S1, S2, S3, S4, S5, S6, S7>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable7<S1, S2, S3, S4, S5, S6, S7>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable7" + suffix() + "{" +
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

        Table8(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table8(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table8<S1, S2, S3, S4, S5, S6, S7, S8> query(TypedBaseQueryImplementor<?> query) {
            return new Table8<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable8" + suffix() + "{" +
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

        Table9(TypedBaseQueryImplementor<?> query, List<Selection<?>> selections, byte[] kotlinSelectionTypes, boolean cte) {
            super(query, selections, kotlinSelectionTypes, cte);
        }

        Table9(BaseTableSymbol base, TableLike<?> parent, WeakJoinHandle handle, JoinType joinType, BaseTableSymbol recursive) {
            super(base, parent, handle, joinType, recursive);
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
        public Table9<S1, S2, S3, S4, S5, S6, S7, S8, S9> query(TypedBaseQueryImplementor<?> query) {
            return new Table9<>(query, selections, kotlinSelectionTypes, cte);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable> TT weakJoin(
                TT targetBaseTable,
                JoinType joinType,
                WeakJoin<BaseTable9<S1, S2, S3, S4, S5, S6, S7, S8, S9>, TT> weakJoinLambda
        ) {
            WeakJoinLambda lambda = JWeakJoinLambdaFactory.get(weakJoinLambda);
            WeakJoinHandle handle = WeakJoinHandle.of(
                    lambda,
                    true,
                    true,
                    (WeakJoin<TableLike<?>, TableLike<?>>) (WeakJoin<?, ?>) weakJoinLambda
            );
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TT extends BaseTable, WJ extends WeakJoin<BaseTable9<S1, S2, S3, S4, S5, S6, S7, S8, S9>, TT>> TT weakJoin(
                TT targetBaseTable,
                Class<WJ> weakJoinType,
                JoinType joinType
        ) {
            WeakJoinHandle handle = WeakJoinHandle.of(weakJoinType);
            return (TT) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
        }

        @Override
        public String toString() {
            return "BaseTable9" + suffix() + "{" +
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
