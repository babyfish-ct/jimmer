package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.table.Props;

import java.util.Map;
import java.util.SortedMap;

public interface Filters {

    default Filter<Props> getFilter(Class<?> type) {
        return getFilter(type, false);
    }

    default Filter<Props> getFilter(ImmutableType type) {
        return getFilter(type, false);
    }

    default Filter<Props> getTargetFilter(ImmutableProp prop) {
        return getTargetFilter(prop, false);
    }

    default Filter<Props> getTargetFilter(TypedProp.Association<?, ?> prop) {
        return getTargetFilter(prop, false);
    }

    Filter<Props> getFilter(Class<?> type, boolean coerciveOnly);

    Filter<Props> getFilter(ImmutableType type, boolean coerciveOnly);

    Filter<Props> getTargetFilter(ImmutableProp prop, boolean coerciveOnly);

    Filter<Props> getTargetFilter(TypedProp.Association<?, ?> prop, boolean coerciveOnly);

    /**
     * Returns the reference wrapper of parameterMap
     * @param type The filtered type
     * @return
     * <ul>
     *     <li>If the `Ref` itself is null, means there is a filter but not cacheable filter</li>
     *     <li>If the `Ref` itself is not null, there is no filter or there is a cacheable filter</li>
     * </ul>
     */
    Ref<SortedMap<String, Object>> getParameterMapRef(Class<?> type);

    /**
     * Returns the reference wrapper of parameterMap
     * @param type The filtered type
     * @return
     * <ul>
     *     <li>If the `Ref` itself is null, means there is a filter but not cacheable filter</li>
     *     <li>If the `Ref` itself is not null, there is no filter or there is a cacheable filter</li>
     * </ul>
     */
    Ref<SortedMap<String, Object>> getParameterMapRef(ImmutableType type);

    /**
     * Returns the reference wrapper of parameterMap
     * @param prop The property associates the filtered type
     * @return
     * <ul>
     *     <li>If the `Ref` itself is null, means there is a filter but not cacheable filter</li>
     *     <li>If the `Ref` itself is not null, there is no filter or there is a cacheable filter</li>
     * </ul>
     */
    Ref<SortedMap<String, Object>> getTargetParameterMapRef(ImmutableProp prop);

    /**
     * Returns the reference wrapper of parameterMap
     * @param prop The property associates the filtered type
     * @return
     * <ul>
     *     <li>If the `Ref` itself is null, means there is a filter but not cacheable filter</li>
     *     <li>If the `Ref` itself is not null, there is no filter or there is a cacheable filter</li>
     * </ul>
     */
    Ref<SortedMap<String, Object>> getTargetParameterMapRef(TypedProp.Association<?, ?> prop);
}
