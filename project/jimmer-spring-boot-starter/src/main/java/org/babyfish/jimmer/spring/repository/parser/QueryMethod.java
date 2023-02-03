package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Method;

public class QueryMethod {

    private final Method javaMethod;

    private final Query query;

    private final Class<?> staticType;

    private final int pageableParamIndex;

    private final int sortParamIndex;

    private final int fetcherParamIndex;

    private final int staticTypeParamIndex;

    public QueryMethod(
            Method javaMethod,
            Query query,
            Class<?> staticType,
            int pageableParamIndex,
            int sortParamIndex,
            int fetcherParamIndex,
            int staticTypeParamIndex
    ) {
        this.javaMethod = javaMethod;
        this.query = query;
        this.staticType = staticType;
        this.pageableParamIndex = pageableParamIndex;
        this.sortParamIndex = sortParamIndex;
        this.fetcherParamIndex = fetcherParamIndex;
        this.staticTypeParamIndex = staticTypeParamIndex;
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

    public Class<?> getStaticType() {
        return staticType;
    }

    public int getPageableParamIndex() {
        return pageableParamIndex;
    }

    public int getSortParamIndex() {
        return sortParamIndex;
    }

    public int getFetcherParamIndex() {
        return fetcherParamIndex;
    }

    public int getStaticTypeParamIndex() {
        return staticTypeParamIndex;
    }

    @Override
    public String toString() {
        return "QueryMethod{" +
                "javaMethod=" + javaMethod +
                ", query=" + query +
                ", staticType=" + staticType +
                ", pageableParamIndex=" + pageableParamIndex +
                ", sortParamIndex=" + sortParamIndex +
                ", fetcherParamIndex=" + fetcherParamIndex +
                ", staticTypeParamIndex=" + staticTypeParamIndex +
                '}';
    }
}
