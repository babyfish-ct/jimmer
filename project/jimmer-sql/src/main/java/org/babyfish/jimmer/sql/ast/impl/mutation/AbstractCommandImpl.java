package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

abstract class AbstractCommandImpl {

    final Cfg cfg;

    private Object options;

    AbstractCommandImpl(Cfg cfg) {
        this.cfg = cfg;
    }

    @SuppressWarnings("unchecked")
    final <T> T options() {
        Object options = this.options;
        if (options == null) {
            this.options = options = createOptions();
        }
        return (T) options;
    }

    Object createOptions() {
        return new AbstractEntitySaveCommandImpl.OptionsImpl(cfg);
    }

    static class ListNode<E> {

        private final ListNode<E> prev;

        private final E element;

        private final int size;

        public ListNode(ListNode<E> prev, E element) {
            this.prev = prev;
            this.element = element;
            this.size = (prev != null ? prev.size : 0) + 1;
        }

        static <T, E> List<E> toList(T source, Function<T, ListNode<E>> block) {
            if (source == null) {
                return Collections.emptyList();
            }
            ListNode<E> listNode = block.apply(source);
            if (listNode == null) {
                return Collections.emptyList();
            }
            if (listNode.prev == null) {
                return Collections.singletonList(listNode.element);
            }
            List<E> list = new ArrayList<>(listNode.size);
            listNode.toList(list);
            return list;
        }

        private void toList(List<E> outputList) {
            if (prev != null) {
                prev.toList(outputList);
            }
            outputList.add(element);
        }
    }

    static class MapNode<K, V> {

        private final MapNode<K, V> prev;

        private final K key;

        private final V value;

        private final int size;

        public MapNode(MapNode<K, V> prev, K key, V value) {
            this.prev = prev;
            this.key = key;
            this.value = value;
            this.size = (prev != null ? prev.size : 0) + 1;
        }

        static <T, K, V> Map<K, V> toMap(T source, Function<T, MapNode<K, V>> block) {
            if (source == null) {
                return Collections.emptyMap();
            }
            MapNode<K, V> mapNode = block.apply(source);
            if (mapNode == null) {
                return Collections.emptyMap();
            }
            if (mapNode.prev == null) {
                return Collections.singletonMap(mapNode.key, mapNode.value);
            }
            Map<K, V> map = new LinkedHashMap<>((mapNode.size * 4 + 2) / 3);
            mapNode.toMap(map);
            return map;
        }

        private void toMap(Map<K, V> outputMap) {
            if (prev != null) {
                prev.toMap(outputMap);
            }
            outputMap.put(key, value);
        }
    }

    static abstract class Cfg {

        final Cfg prev;

        Cfg(Cfg prev) {
            this.prev = prev;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        final <T extends Cfg> T as(Class<T> type) {
            if (this.getClass() == type) {
                return (T) this;
            }
            if (prev == null) {
                return null;
            }
            return prev.as(type);
        }

        static <K, V> Map<K, V> mergedMap(Map<K, V> oldMap, K key, V value) {
            if (oldMap == null || oldMap.isEmpty()) {
                return Collections.singletonMap(key, value);
            }
            Map<K, V> mergedMap = new LinkedHashMap<>((oldMap.size() * 4 + 2) / 3);
            mergedMap.putAll(oldMap);
            mergedMap.put(key, value);
            return mergedMap;
        }
    }

    static class RootCfg extends Cfg {

        final JSqlClientImplementor sqlClient;

        final Object argument;

        public RootCfg(JSqlClientImplementor sqlClient, Object argument) {
            super(null);
            this.sqlClient = sqlClient;
            this.argument = argument;
        }
    }

    static class ConnectionCfg extends Cfg {

        final Connection con;

        public ConnectionCfg(Cfg prev, Connection con) {
            super(prev);
            this.con = con;
        }
    }

    static class DeleteModeCfg extends Cfg {

        final DeleteMode mode;

        public DeleteModeCfg(Cfg prev, DeleteMode mode) {
            super(prev);
            this.mode = mode != null ? mode : DeleteMode.PHYSICAL;
        }
    }

    static class MaxCommandJoinCountCfg extends Cfg {

        final int maxCommandJoinCount;

        MaxCommandJoinCountCfg(Cfg prev, int maxCommandJoinCount) {
            super(prev);
            if (maxCommandJoinCount < 0 || maxCommandJoinCount > 8) {
                throw new IllegalArgumentException("maxCommandJoinCount must between 0 and 8");
            }
            this.maxCommandJoinCount = maxCommandJoinCount;
        }
    }

    static class DumbBatchAcceptableCfg extends Cfg {

        final boolean acceptable;

        DumbBatchAcceptableCfg(Cfg prev, boolean acceptable) {
            super(prev);
            this.acceptable = acceptable;
        }
    }

    static class ConstraintViolationTranslatableCfg extends Cfg {

        final boolean translatable;

        ConstraintViolationTranslatableCfg(Cfg prev, boolean translatable) {
            super(prev);
            this.translatable = translatable;
        }
    }

    static class TransactionRequiredCfg extends Cfg {

        final boolean required;

        TransactionRequiredCfg(Cfg prev, boolean required) {
            super(prev);
            this.required = required;
        }
    }

    static class DissociationActionCfg extends Cfg {

        final MapNode<ImmutableProp, DissociateAction> mapNode;

        public DissociationActionCfg(Cfg prev, ImmutableProp prop, DissociateAction action) {
            super(prev);
            if (!prop.isReference(TargetLevel.PERSISTENT) || !prop.isColumnDefinition()) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" is not reference(one-to-one/many-to-one) association property " +
                                "based on foreign key directly"
                );
            }
            if (action == DissociateAction.SET_NULL && !prop.isNullable()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is not nullable so that it does not support 'on delete set null'"
                );
            }
            if (action == DissociateAction.SET_NULL && prop.isInputNotNull()) {
                throw new IllegalArgumentException(
                        "'" + prop + "' is `inputNotNull` so that it does not support 'on delete set null'"
                );
            }
            DissociationActionCfg p = prev.as(DissociationActionCfg.class);
            this.mapNode = new MapNode<>(p != null ? p.mapNode : null, prop, action);
        }
    }

    static class FetcherCfg extends Cfg {

        final Fetcher<?> fetcher;

        FetcherCfg(Cfg prev, Fetcher<?> fetcher) {
            super(prev);
            this.fetcher = fetcher;
        }
    }
}
