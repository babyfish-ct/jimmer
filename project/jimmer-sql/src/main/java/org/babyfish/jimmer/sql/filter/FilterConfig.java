package org.babyfish.jimmer.sql.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class FilterConfig {

    private FilterManager filterManager;

    public FilterConfig(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    public FilterConfig enable(Filter<?> filter) {
        this.filterManager = filterManager.enable(Collections.singletonList(filter));
        return this;
    }

    public FilterConfig enable(Filter<?>... filters) {
        this.filterManager = filterManager.enable(Arrays.asList(filters));
        return this;
    }

    public FilterConfig enable(Collection<Filter<?>> filters) {
        this.filterManager = filterManager.enable(filters);
        return this;
    }

    public FilterConfig disabled(Filter<?> filter) {
        this.filterManager = filterManager.disable(Collections.singletonList(filter));
        return this;
    }

    public FilterConfig disable(Filter<?>... filters) {
        this.filterManager = filterManager.disable(Arrays.asList(filters));
        return this;
    }

    public FilterConfig disable(Collection<Filter<?>> filters) {
        this.filterManager = filterManager.disable(filters);
        return this;
    }

    public FilterConfig enableByType(Class<?> filterType) {
        this.filterManager = filterManager.enableByTypes(Collections.singletonList(filterType));
        return this;
    }

    public FilterConfig enableByType(Class<?>... filterTypes) {
        this.filterManager = filterManager.enableByTypes(Arrays.asList(filterTypes));
        return this;
    }

    public FilterConfig enableByType(Collection<Class<?>> filterTypes) {
        this.filterManager = filterManager.enableByTypes(filterTypes);
        return this;
    }

    public FilterConfig disableByType(Class<?> filterType) {
        this.filterManager = filterManager.disableByTypes(Collections.singletonList(filterType));
        return this;
    }

    public FilterConfig disableByType(Class<?>... filterTypes) {
        this.filterManager = filterManager.disableByTypes(Arrays.asList(filterTypes));
        return this;
    }

    public FilterConfig disableByType(Collection<Class<?>> filterTypes) {
        this.filterManager = filterManager.disableByTypes(filterTypes);
        return this;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }
}
