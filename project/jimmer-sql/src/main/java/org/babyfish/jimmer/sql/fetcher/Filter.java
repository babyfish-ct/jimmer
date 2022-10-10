package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.query.NullOrderMode;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.cache.CacheFilter;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Filter<T extends Table<?>> extends CacheFilter {

    void apply(FilterArgs<T> args);

    static <T extends Table<?>> Filter<T> parameterized(
            Map<String, Object> args,
            Consumer<FilterArgs<T>> block
    ) {
        NavigableMap<String, Object> sortedArgs;
        if (args == null || args.isEmpty()) {
            sortedArgs = Collections.emptyNavigableMap();
        } else {
            sortedArgs = new TreeMap<>(args);
        }
        return new Filter<T>() {
            @Override
            public void apply(FilterArgs<T> args) {
                block.accept(args);
            }
            @Override
            public NavigableMap<String, Object> getArgs() {
                return sortedArgs;
            }
        };
    }

    static <E, T extends Table<E>> Filter<T> sortingFilter(
            Class<T> tableType,
            Function<T, PropExpression<?>> block
    ) {
        return sortingFilter(tableType, block, OrderMode.ASC, NullOrderMode.UNSPECIFIED);
    }

    static <E, T extends Table<E>> Filter<T> sortingFilter(
            Class<T> tableType,
            Function<T, PropExpression<?>> block,
            OrderMode orderMode
    ) {
        return sortingFilter(tableType, block, orderMode, NullOrderMode.UNSPECIFIED);
    }

    static <T extends Table<?>> Filter<T> sortingFilter(
            Class<T> tableType,
            Function<T, PropExpression<?>> block,
            OrderMode orderMode,
            NullOrderMode nullOrderMode
    ) {
        return sortingFilterBuilder(tableType)
                .add(block, orderMode, nullOrderMode)
                .build();
    }

    static <T extends Table<?>> SortingFilterBuilder<T> sortingFilterBuilder(
            Class<T> tableType
    ) {
        return new SortingFilterBuilder<>(tableType);
    }

    class SortingFilterBuilder<T extends Table<?>> {

        private Class<T> tableType;

        private List<SortedField> sortedFields = new ArrayList<>();

        public SortingFilterBuilder(Class<T> tableType) {
            this.tableType = tableType;
        }

        @OldChain
        public SortingFilterBuilder<T> add(
                Function<T, PropExpression<?>> block
        ) {
            return add(block, OrderMode.ASC, NullOrderMode.UNSPECIFIED);
        }

        @OldChain
        public SortingFilterBuilder<T> add(
                Function<T, PropExpression<?>> block,
                OrderMode orderMode
        ) {
            return add(block, orderMode, NullOrderMode.UNSPECIFIED);
        }

        @OldChain
        public SortingFilterBuilder<T> add(
                Function<T, PropExpression<?>> block,
                OrderMode orderMode,
                NullOrderMode nullOrderMode
        ) {
            ImmutableProp prop = ImmutableProps.get(tableType, block);
            SortedField sortedField = new SortedField(
                    prop.getName(),
                    Objects.requireNonNull(orderMode, "orderMode cannot be null"),
                    Objects.requireNonNull(nullOrderMode, "nullOrderMode cannot be null")
            );
            sortedFields.add(sortedField);
            return this;
        }

        public Filter<T> build() {
            return new FilterImpl<>(sortedFields);
        }

        private static class SortedField {

            final String prop;
            final OrderMode orderMode;
            final NullOrderMode nullOrderMode;

            public SortedField(String prop, OrderMode orderMode, NullOrderMode nullOrderMode) {
                this.prop = prop;
                this.orderMode = orderMode;
                this.nullOrderMode = nullOrderMode;
            }

            @Override
            public int hashCode() {
                return Objects.hash(prop, orderMode, nullOrderMode);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                SortedField that = (SortedField) o;
                return prop.equals(that.prop) &&
                        orderMode == that.orderMode &&
                        nullOrderMode == that.nullOrderMode;
            }

            @Override
            public String toString() {
                return "SortedField{" +
                        "prop=" + prop +
                        ", orderMode=" + orderMode +
                        ", nullOrderMode=" + nullOrderMode +
                        '}';
            }
        }

        private static class FilterImpl<T extends Table<?>> implements Filter<T> {

            private List<SortedField> sortedFields;

            public FilterImpl(List<SortedField> sortedFields) {
                this.sortedFields = sortedFields;
            }

            @Override
            public void apply(FilterArgs<T> args) {
                for (SortedField sortedField : sortedFields) {
                    args.orderBy(
                            new Order(
                                args.getTable().get(sortedField.prop),
                                sortedField.orderMode,
                                sortedField.nullOrderMode
                            )
                    );
                }
            }

            @Override
            public int hashCode() {
                return Objects.hash(sortedFields);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                FilterImpl<?> filter = (FilterImpl<?>) o;
                return sortedFields.equals(filter.sortedFields);
            }

            @Override
            public String toString() {
                return "FilterImpl{" +
                        "sortedFields=" + sortedFields +
                        '}';
            }
        }
    }
}
