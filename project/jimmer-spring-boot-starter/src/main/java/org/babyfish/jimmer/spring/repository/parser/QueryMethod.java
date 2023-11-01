package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Method;

public class QueryMethod {

    private final Method javaMethod;

    private final Query query;

    private final Class<?> viewType;

    private final int pageableParamIndex;

    private final int sortParamIndex;

    private final int specificationParamIndex;

    private final int fetcherParamIndex;

    private final int viewTypeParamIndex;

    public QueryMethod(
            Method javaMethod,
            Query query,
            Class<?> viewType,
            int pageableParamIndex,
            int sortParamIndex,
            int specificationParamIndex,
            int fetcherParamIndex,
            int viewTypeParamIndex
    ) {
        this.javaMethod = javaMethod;
        this.query = query;
        this.viewType = viewType;
        this.pageableParamIndex = pageableParamIndex;
        this.sortParamIndex = sortParamIndex;
        this.specificationParamIndex = specificationParamIndex;
        this.fetcherParamIndex = fetcherParamIndex;
        this.viewTypeParamIndex = viewTypeParamIndex;
    }

    public static QueryMethod of(Context ctx, ImmutableType type, Method method) {
        return QueryMethodParser.parse(ctx, type, method);
    }

    public Method getJavaMethod() {
        return javaMethod;
    }

    public Query getQuery() {
        return query;
    }

    public Class<?> getViewType() {
        return viewType;
    }

    public int getPageableParamIndex() {
        return pageableParamIndex;
    }

    public int getSortParamIndex() {
        return sortParamIndex;
    }

    public int getSpecificationParamIndex() {
        return specificationParamIndex;
    }

    public int getFetcherParamIndex() {
        return fetcherParamIndex;
    }

    public int getViewTypeParamIndex() {
        return viewTypeParamIndex;
    }

    @Override
    public String toString() {
        return "QueryMethod{" +
                "javaMethod=" + javaMethod +
                ", query=" + query +
                ", viewType=" + viewType +
                ", pageableParamIndex=" + pageableParamIndex +
                ", sortParamIndex=" + sortParamIndex +
                ", fetcherParamIndex=" + fetcherParamIndex +
                ", viewTypeParamIndex=" + viewTypeParamIndex +
                '}';
    }
}
