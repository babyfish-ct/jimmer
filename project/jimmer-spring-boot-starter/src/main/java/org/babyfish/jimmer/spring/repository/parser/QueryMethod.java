package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Method;

public class QueryMethod {

    private final Method javaMethod;

    private final Query query;

    private final int pageableParamIndex;

    private final int sortParamIndex;

    private final int fetcherIndex;

    public QueryMethod(
            Method javaMethod,
            Query query,
            int pageableParamIndex,
            int sortParamIndex,
            int fetcherIndex
    ) {
        this.javaMethod = javaMethod;
        this.query = query;
        this.pageableParamIndex = pageableParamIndex;
        this.sortParamIndex = sortParamIndex;
        this.fetcherIndex = fetcherIndex;
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

    public int getPageableParamIndex() {
        return pageableParamIndex;
    }

    public int getSortParamIndex() {
        return sortParamIndex;
    }

    public int getFetcherIndex() {
        return fetcherIndex;
    }

    @Override
    public String toString() {
        return "QueryMethod{" +
                "javaMethod=" + javaMethod +
                ", query=" + query +
                ", pageableParamIndex=" + pageableParamIndex +
                ", sortParamIndex=" + sortParamIndex +
                ", fetcherIndex=" + fetcherIndex +
                '}';
    }
}
