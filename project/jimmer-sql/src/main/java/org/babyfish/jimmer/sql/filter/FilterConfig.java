package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class FilterConfig {

    private FilterManager filterManager;

    public FilterConfig(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    @OldChain
    public FilterConfig enable(Filter<?>... filters) {
        this.filterManager = filterManager.enable(Arrays.asList(filters));
        return this;
    }

    @OldChain
    public FilterConfig enable(Collection<Filter<?>> filters) {
        this.filterManager = filterManager.enable(filters);
        return this;
    }

    @OldChain
    public FilterConfig disable(Filter<?>... filters) {
        this.filterManager = filterManager.disable(Arrays.asList(filters));
        return this;
    }

    @OldChain
    public FilterConfig disable(Collection<Filter<?>> filters) {
        this.filterManager = filterManager.disable(filters);
        return this;
    }

    @OldChain
    public FilterConfig disableByTypes(Class<?>... filterTypes) {
        this.filterManager = filterManager.disableByTypes(Arrays.asList(filterTypes));
        return this;
    }

    @OldChain
    public FilterConfig disableByTypes(Collection<Class<?>> filterTypes) {
        this.filterManager = filterManager.disableByTypes(filterTypes);
        return this;
    }

    @OldChain
    public FilterConfig addFilterableReferenceProps(ImmutableProp ... props) {
        this.filterManager = filterManager.addFilterableReferenceProps(Arrays.asList(props));
        return this;
    }

    @OldChain
    public FilterConfig addFilterableReferenceProps(TypedProp.Reference<?, ?> ... props) {
        this.filterManager = filterManager.addFilterableReferenceProps(
                Arrays.stream(props).map(TypedProp::unwrap).collect(Collectors.toList())
        );
        return this;
    }

    @OldChain
    public FilterConfig addFilterableReferenceProps(Collection<ImmutableProp> props) {
        this.filterManager = filterManager.addFilterableReferenceProps(props);
        return this;
    }

    @OldChain
    public FilterConfig removeFilterableReferenceProps(ImmutableProp ... props) {
        this.filterManager = filterManager.removeFilterableReferenceProps(Arrays.asList(props));
        return this;
    }

    @OldChain
    public FilterConfig removeFilterableReferenceProps(TypedProp.Reference<?, ?> ... props) {
        this.filterManager = filterManager.removeFilterableReferenceProps(
                Arrays.stream(props).map(TypedProp::unwrap).collect(Collectors.toList())
        );
        return this;
    }

    @OldChain
    public FilterConfig removeFilterableReferenceProps(Collection<ImmutableProp> props) {
        this.filterManager = filterManager.removeFilterableReferenceProps(props);
        return this;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }
}
