package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.fetcher.impl.JoinFetchFieldVisitor;

import java.util.*;

class Readers {

    private Readers() {}

    public static Reader<?> createReader(JSqlClientImplementor sqlClient, List<Selection<?>> selections) {
        switch (selections.size()) {
            case 1:
                return createSingleReader(sqlClient, selections.get(0));
            case 2:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1))
                );
            case 3:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2))
                );
            case 4:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3))
                );
            case 5:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4))
                );
            case 6:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5))
                );
            case 7:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5)),
                        createSingleReader(sqlClient, selections.get(6))
                );
            case 8:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5)),
                        createSingleReader(sqlClient, selections.get(6)),
                        createSingleReader(sqlClient, selections.get(7))
                );
            case 9:
                return Reader.tuple(
                        createSingleReader(sqlClient, selections.get(0)),
                        createSingleReader(sqlClient, selections.get(1)),
                        createSingleReader(sqlClient, selections.get(2)),
                        createSingleReader(sqlClient, selections.get(3)),
                        createSingleReader(sqlClient, selections.get(4)),
                        createSingleReader(sqlClient, selections.get(5)),
                        createSingleReader(sqlClient, selections.get(6)),
                        createSingleReader(sqlClient, selections.get(7)),
                        createSingleReader(sqlClient, selections.get(8))
                );
            default:
                throw new IllegalArgumentException("The selection count must between 1 and 9");
        }
    }

    private static Reader<?> createSingleReader(JSqlClientImplementor sqlClient, Selection<?> selection) {
        if (selection instanceof TableSelection) {
            ImmutableType immutableType =
                    ((TableSelection)selection).getImmutableType();
            return sqlClient.getReader(immutableType);
        }
        if (selection instanceof Table<?>) {
            ImmutableType immutableType =
                    ((Table<?>)selection).getImmutableType();
            return sqlClient.getReader(immutableType);
        }
        if (selection instanceof FetcherSelection<?>) {
            Fetcher<?> fetcher = ((FetcherSelection<?>) selection).getFetcher();
            ImmutableType type = fetcher.getImmutableType();
            if (type.isEmbeddable()) {
                return createDynamicEmbeddableReader(sqlClient, type, fetcher);
            }
            DynamicEntityReaderCreator creator = new DynamicEntityReaderCreator(sqlClient, type);
            creator.visit(fetcher);
            return creator.create();
        }
        ExpressionImplementor<?> unwrapped = AbstractTypedEmbeddedPropExpression.<ExpressionImplementor<?>>unwrap(selection);
        if (unwrapped instanceof PropExpression<?>) {
            ImmutableProp prop = ((PropExpressionImplementor<?>) unwrapped).getProp();
            if (prop.isScalar(TargetLevel.ENTITY) && !prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                return sqlClient.getReader(prop);
            }
        }
        return sqlClient.getReader(unwrapped.getType());
    }

    private static Reader<?> createDynamicEmbeddableReader(JSqlClientImplementor sqlClient, ImmutableType type, Fetcher<?> fetcher) {
        List<ImmutableProp> props = new ArrayList<>(type.getProps().size());
        List<Reader<?>> readers = new ArrayList<>(type.getProps().size());
        List<PropId> shownPropIds = null;
        List<PropId> hiddenPropIds = null;
        if (fetcher == null) {
            for (ImmutableProp prop : type.getProps().values()) {
                Reader<?> reader;
                if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    reader = createDynamicEmbeddableReader(sqlClient, prop.getTargetType(), null);
                } else if (!prop.isFormula()) {
                    assert prop.getSqlTemplate() == null; // SQL formula is not supported by embeddable
                    reader = sqlClient.getReader(prop);
                } else {
                    reader = null;
                }
                if (reader != null) {
                    props.add(prop);
                    readers.add(reader);
                }
            }
        } else {
            for (Field field : fetcher.getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                Reader<?> reader;
                if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                    reader = createDynamicEmbeddableReader(sqlClient, prop.getTargetType(), field.getChildFetcher());
                } else if (!prop.isFormula()) {
                    assert prop.getSqlTemplate() == null; // SQL formula is not supported by embeddable
                    reader = sqlClient.getReader(prop);
                } else {
                    reader = null;
                }
                if (reader != null) {
                    props.add(prop);
                    readers.add(reader);
                }
                if (!prop.getDependencies().isEmpty()) {
                    if (shownPropIds == null) {
                        shownPropIds = new ArrayList<>();
                    }
                    shownPropIds.add(prop.getId());
                }
                if (field.isImplicit()) {
                    if (hiddenPropIds == null) {
                        hiddenPropIds = new ArrayList<>();
                    }
                    hiddenPropIds.add(prop.getId());
                }
            }
        }
        return new DynamicEmbeddedReader(type, props, readers, shownPropIds, hiddenPropIds);
    }

    private static class DynamicEntityReaderCreator extends JoinFetchFieldVisitor {

        private final JSqlClientImplementor sqlClient;

        private Args args;

        DynamicEntityReaderCreator(JSqlClientImplementor sqlClient, ImmutableType type) {
            super(sqlClient);
            this.sqlClient = sqlClient;
            this.args = new Args(type);
        }

        @Override
        protected Object enter(Field field) {
            Args parentArgs = this.args;
            this.args = new Args(field.getProp().getTargetType());
            return parentArgs;
        }

        @Override
        protected void leave(Field field, Object enterValue) {
            Args parentArgs = (Args) enterValue;
            Reader<?> subReader = args.create(sqlClient);
            parentArgs.set(field.getProp(), subReader);
            this.args = parentArgs;
        }

        @Override
        protected void visit(Field field, int depth) {
            ImmutableProp prop = field.getProp();
            if (!prop.isId() && (prop.hasStorage() || prop.getSqlTemplate() != null)) {
                Reader<?> subReader =
                        prop.isEmbedded(EmbeddedLevel.SCALAR) ?
                                createDynamicEmbeddableReader(sqlClient, prop.getTargetType(), field.getChildFetcher()) :
                                sqlClient.getReader(prop);
                if (subReader != null) {
                    args.set(prop, subReader);
                }
            }
            if (!prop.getDependencies().isEmpty()) {
                args.show(prop);
            }
            if (field.isImplicit()) {
                args.hide(prop);
            }
        }

        Reader<?> create() {
            return args.create(sqlClient);
        }

        private static class Args {

            private final ImmutableType type;

            private Map<ImmutableProp, Reader<?>> nonIdReaderMap = Collections.emptyMap();

            private List<PropId> shownPropIds;

            private List<PropId> hiddenPropIds;

            private Args(ImmutableType type) {
                this.type = type;
            }

            void set(ImmutableProp prop, Reader<?> reader) {
                Map<ImmutableProp, Reader<?>> map = nonIdReaderMap;
                if (map.isEmpty()) {
                    nonIdReaderMap = map = new LinkedHashMap<>();
                }
                map.put(prop, reader);
            }

            void show(ImmutableProp prop) {
                List<PropId> list = shownPropIds;
                if (list == null) {
                    this.shownPropIds = list = new ArrayList<>();
                }
                list.add(prop.getId());
            }

            void hide(ImmutableProp prop) {
                List<PropId> list = hiddenPropIds;
                if (list == null) {
                    this.hiddenPropIds = list = new ArrayList<>();
                }
                list.add(prop.getId());
            }

            ObjectReader create(JSqlClientImplementor sqlClient) {
                return new ObjectReader(
                        type,
                        sqlClient.getReader(type.getIdProp()),
                        nonIdReaderMap,
                        shownPropIds,
                        hiddenPropIds
                );
            }
        }
    }
}
