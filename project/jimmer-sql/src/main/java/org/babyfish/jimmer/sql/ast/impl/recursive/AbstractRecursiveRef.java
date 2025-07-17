package org.babyfish.jimmer.sql.ast.impl.recursive;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.*;
import org.babyfish.jimmer.sql.ast.table.base.recursive.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.temporal.Temporal;
import java.util.Date;

public class AbstractRecursiveRef<B extends BaseTable> implements RecursiveRef<B> {

    private final Class<?>[] selectionTypes;

    private final B proxyBaseTable;

    private B realBaseTable;

    @SuppressWarnings("unchecked")
    AbstractRecursiveRef(AbstractRecursiveRef<?> prev, Class<?> selectionType) {
        int oldSize = prev != null ? prev.selectionTypes.length : 0;
        Class<?>[] arr = new Class[oldSize + 1];
        if (prev != null) {
            System.arraycopy(prev.selectionTypes, 0, arr, 0, oldSize);
        }
        arr[oldSize] = selectionType;
        selectionTypes = arr;
        proxyBaseTable = (B) Proxy.newProxyInstance(
                RecursiveRef.class.getClassLoader(),
                new Class[] {BaseTableSymbol.class, baseTableType(arr.length) },
                this::invoke
        );
    }

    private static Class<?> baseTableType(int span) {
        switch (span) {
            case 1:
                return BaseTable1.class;
            case 2:
                return BaseTable2.class;
            case 3:
                return BaseTable3.class;
            case 4:
                return BaseTable4.class;
            case 5:
                return BaseTable5.class;
            case 6:
                return BaseTable6.class;
            case 7:
                return BaseTable7.class;
            case 8:
                return BaseTable8.class;
            case 9:
                return BaseTable9.class;
            default:
                throw new IllegalArgumentException("Illegal selection count " + span + " for RecursiveRef");
        }
    }

    private Object invoke(Object proxy, Method method, Object[] args) {
        return null;
    }

    public void setRealBaseTable(B realBaseTable) {
        this.realBaseTable = realBaseTable;
    }

    public B toBaseTable() {
        return proxyBaseTable;
    }

    public static class Ref1<S1 extends Selection<?>> extends AbstractRecursiveRef<BaseTable1<S1>> implements RecursiveRef1<S1> {

        public Ref1(Class<?> selectionType) {
            super(null, selectionType);
        }

        @Override
        public <T extends Table<?>> RecursiveRef2<S1, T> table(Class<T> tableType) {
            return new Ref2<>(this, tableType);
        }

        @Override
        public RecursiveRef2<S1, StringExpression> string() {
            return new Ref2<>(this, StringExpression.class);
        }

        @Override
        public <N extends Number & Comparable<N>> RecursiveRef2<S1, NumericExpression<N>> numeric(Class<N> numberType) {
            return new Ref2<>(this, NumericExpression.class);
        }

        @Override
        public <T extends Date> RecursiveRef2<S1, DateExpression<T>> date(Class<T> dateType) {
            return new Ref2<>(this, DateExpression.class);
        }

        @Override
        public <T extends Temporal & Comparable<T>> RecursiveRef2<S1, TemporalExpression<T>> temporal(Class<T> temporalType) {
            return new Ref2<>(this, TemporalExpression.class);
        }

        @Override
        public <T extends Comparable<?>> RecursiveRef2<S1, ComparableExpression<T>> comparable(Class<T> comparableType) {
            return new Ref2<>(this, Comparable.class);
        }

        @Override
        public <T> RecursiveRef2<S1, Expression<T>> value(Class<?> valueType) {
            return new Ref2<>(this, Expression.class);
        }
    }

    private static class Ref2<
            S1 extends Selection<?>,
            S2 extends Selection<?>
    > extends AbstractRecursiveRef<BaseTable2<S1, S2>> implements RecursiveRef2<S1, S2> {

        Ref2(AbstractRecursiveRef<?> prev, Class<?> selectionType) {
            super(prev, selectionType);
        }

        @Override
        public <T extends Table<?>> RecursiveRef3<S1, S2, T> table(Class<T> tableType) {
            return null;
        }

        @Override
        public RecursiveRef3<S1, S2, StringExpression> string() {
            return null;
        }

        @Override
        public <N extends Number & Comparable<N>> RecursiveRef3<S1, S2, NumericExpression<N>> numeric(Class<N> numberType) {
            return null;
        }

        @Override
        public <T extends Date> RecursiveRef3<S1, S2, DateExpression<T>> date(Class<T> dateType) {
            return null;
        }

        @Override
        public <T extends Temporal & Comparable<T>> RecursiveRef3<S1, S2, TemporalExpression<T>> temporal(Class<T> temporalType) {
            return null;
        }

        @Override
        public <T extends Comparable<?>> RecursiveRef3<S1, S2, ComparableExpression<T>> comparable(Class<T> comparableType) {
            return null;
        }

        @Override
        public <T> RecursiveRef3<S1, S2, Expression<T>> value(Class<?> valueType) {
            return null;
        }
    }

    private static class Ref8<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>
    > extends AbstractRecursiveRef<BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8>>
    implements RecursiveRef8<S1, S2, S3, S4, S5, S6, S7, S8> {

        Ref8(AbstractRecursiveRef<?> prev, Class<?> selectionType) {
            super(prev, selectionType);
        }

        @Override
        public <T extends Table<?>> RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, T> table(Class<T> tableType) {
            return new Ref9<>(this, tableType);
        }

        @Override
        public RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, StringExpression> string() {
            return null;
        }

        @Override
        public <N extends Number & Comparable<N>> RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, NumericExpression<N>> numeric(Class<N> numberType) {
            return null;
        }

        @Override
        public <T extends Date> RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, DateExpression<T>> date(Class<T> dateType) {
            return null;
        }

        @Override
        public <T extends Temporal & Comparable<T>> RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, TemporalExpression<T>> temporal(Class<T> temporalType) {
            return null;
        }

        @Override
        public <T extends Comparable<?>> RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, ComparableExpression<T>> comparable(Class<T> comparableType) {
            return null;
        }

        @Override
        public <T> RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, Expression<T>> value(Class<?> valueType) {
            return null;
        }
    }

    private static class Ref9<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>,
            S9 extends Selection<?>
    > extends AbstractRecursiveRef<BaseTable9<S1, S2, S3, S4, S5, S6, S7, S8, S9>>
    implements RecursiveRef9<S1, S2, S3, S4, S5, S6, S7, S8, S9> {

        Ref9(AbstractRecursiveRef<?> prev, Class<?> selectionType) {
            super(prev, selectionType);
        }
    }
}
