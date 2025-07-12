package org.babyfish.jimmer.spring.repository.parser;

import kotlin.reflect.KClass;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

class QueryMethodParser {

    private final Context ctx;

    private final ImmutableType type;

    private final Method method;

    private final Class<?>[] parameterTypes;

    private final Type[] genericParameterTypes;

    private final int pageableParamIndex;

    private final int sortParamIndex;

    private final int specificationIndex;

    private final int fetcherParamIndex;

    private Class<?> viewType;

    private final int viewTypeParamIndex;

    private int paramIndex = -1;

    private int logicParamIndex = -1;

    private QueryMethodParser(Context ctx, ImmutableType type, Method method) {
        this.ctx = ctx;
        this.type = type;
        this.method = method;
        this.parameterTypes = method.getParameterTypes();
        this.genericParameterTypes = method.getGenericParameterTypes();
        this.pageableParamIndex = implicitParameterIndex(Pageable.class);
        this.sortParamIndex = implicitParameterIndex(Sort.class);
        this.specificationIndex = implicitParameterIndex(Specification.class);
        this.fetcherParamIndex = implicitParameterIndex(Fetcher.class);
        int vtpIndex = implicitParameterIndex(Class.class);
        if (vtpIndex == -1) {
            vtpIndex = implicitParameterIndex(KClass.class);
        }
        this.viewTypeParamIndex = vtpIndex;
        if (pageableParamIndex != -1 && sortParamIndex != -1) {
            throw new IllegalArgumentException(
                    "Cannot have parameters of type \"" +
                            Pageable.class.getName() +
                            "\" and \"" +
                            Sort.class.getName() +
                            "\" at the same time"
            );
        }
    }

    static QueryMethod parse(
            Context ctx,
            ImmutableType type,
            Method method
    ) {
        return new QueryMethodParser(ctx, type, method).parse();
    }

    private QueryMethod parse() {
        try {
            return parse0();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Illegal abstract spring-data method \"" + method + "\": " + ex.getMessage(),
                    ex
            );
        }
    }

    private QueryMethod parse0() {
        Type actualElementType = method.getGenericReturnType();
        if (actualElementType instanceof ParameterizedType) {
            actualElementType = ((ParameterizedType) actualElementType).getActualTypeArguments()[0];
        }
        boolean isObjectQuery = actualElementType == type.getJavaClass();
        Query query = Query.of(ctx, new Source(method.getName()), type, !isObjectQuery);
        if (query.getPredicate() != null) {
            query = new Query(query, resolve(query.getPredicate()));
        }
        while (++paramIndex < parameterTypes.length) {
            if (!isImplicitParameterType(parameterTypes[paramIndex])) {
                throw new IllegalArgumentException("Too many parameters");
            }
        }
        if (isPage(method.getReturnType()) && pageableParamIndex == -1) {
            throw new IllegalArgumentException(
                    "Return type \"" +
                            method.getReturnType() +
                            "\" requires parameter whose type is \"" +
                            Pageable.class +
                            "\""
            );
        }
        if (!isPage(method.getReturnType()) && pageableParamIndex != -1) {
            throw new IllegalArgumentException(
                    "The parameter whose type is \"" +
                            Pageable.class +
                            "\" requires the return type \"" +
                            Page.class.getName() +
                            "\" or \"" +
                            org.babyfish.jimmer.Page.class.getName() +
                            "\""
            );
        }
        if (query.getAction() == Query.Action.FIND) {
            Class<?> entityType = type.getJavaClass();
            Class<?> returnType = method.getReturnType();
            if (returnType == List.class ||
                    returnType == Collection.class ||
                    returnType == Iterable.class ||
                    isPage(method.getReturnType()) ||
                    returnType == Optional.class
            ) {
                Type genericReturnType = method.getGenericReturnType();
                if (!(genericReturnType instanceof ParameterizedType)) {
                    throw new IllegalArgumentException(
                            "Return type must be parameterized type when raw return type is " +
                                    "\"" +
                                    List.class.getName() +
                                    "\", \"" +
                                    Collection.class.getName() +
                                    "\", \"" +
                                    Iterator.class.getName() +
                                    "\", \"" +
                                    Page.class.getName() +
                                    "\", \"" +
                                    org.babyfish.jimmer.Page.class.getName() +
                                    "\" or \"" +
                                    Optional.class +
                                    "\""
                    );
                }
            }
            if (query.getSelectedPath() != null) {
                if (actualElementType != query.getSelectedPath().getType()) {
                    throw new IllegalArgumentException(
                            "The returned element type must be \"" +
                                    query.getSelectedPath().getType() +
                                    "\""
                    );
                }
            } else {
                ReturnedElementType returnedElementType = returnedElementType(actualElementType, entityType);
                if (returnedElementType == null) {
                    throw new IllegalArgumentException(
                            "The returned element type must be \"" +
                                    entityType.getName() +
                                    "\", a class implements \"" +
                                    View.class.getName() +
                                    "<" +
                                    entityType.getName() +
                                    ">\" or a method level type variable extends \"" +
                                    View.class.getName() +
                                    "<" +
                                    entityType.getName() +
                                    ">\""
                    );
                }
                this.viewType = returnedElementType.viewType;
            }
            if (query.getSelectedPath() == null && actualElementType instanceof TypeVariable<?>) {
                if (viewTypeParamIndex == -1) {
                    throw new IllegalArgumentException(
                            "A parameter whose type is \"Class<" +
                                    ((TypeVariable<?>)actualElementType).getName() +
                                    ">\" or \"KClass<" +
                                    ((TypeVariable<?>)actualElementType).getName() +
                                    ">\" is required"
                    );
                }
                Type type = method.getGenericParameterTypes()[viewTypeParamIndex];
                boolean valid = false;
                if (type instanceof ParameterizedType) {
                    Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
                    if (typeArgument == actualElementType) {
                        valid = true;
                    }
                }
                if (!valid) {
                    throw new IllegalArgumentException(
                            "The type argument of parameters[" +
                                    viewTypeParamIndex +
                                    "] must be the type variable \"" +
                                    ((TypeVariable<?>)actualElementType).getName() +
                                    "\""
                    );
                }
            } else if (viewTypeParamIndex != -1) {
                throw new IllegalArgumentException(
                        "The parameters[" + viewTypeParamIndex + "] is illegal"
                );
            }
        } else if (query.getAction() == Query.Action.EXISTS) {
            if (method.getReturnType() != boolean.class) {
                throw new IllegalArgumentException("The return type must be boolean");
            }
        } else if (query.getAction() == Query.Action.COUNT) {
            if (method.getReturnType() != int.class && method.getReturnType() != long.class) {
                throw new IllegalArgumentException("The return type must be int or long");
            }
        } else if (query.getAction() == Query.Action.DELETE) {
            if (method.getReturnType() != int.class && method.getReturnType() != void.class) {
                throw new IllegalArgumentException("The return type must be int or void");
            }
        }
        if (specificationIndex != -1) {
            if (query.getAction() != Query.Action.FIND && query.getAction() != Query.Action.COUNT && query.getAction() != Query.Action.EXISTS) {
                throw new IllegalArgumentException("The method must be query method when there is a specification parameter");
            }
            Type specificationType = genericParameterTypes[specificationIndex];
            if (!(specificationType instanceof ParameterizedType)) {
                if (specificationType != Specification.class) {
                    throw new IllegalArgumentException(
                            "The specification parameter must be \"" +
                                    Specification.class.getName() +
                                    "<" +
                                    type.getJavaClass().getName() +
                                    ">\""
                    );
                }
                throw new IllegalArgumentException("The specification parameter must be parameterized type");
            }
            ParameterizedType parameterizedType = (ParameterizedType) specificationType;
            if (parameterizedType.getRawType() != Specification.class) {
                throw new IllegalArgumentException(
                        "The raw type of specification parameter must be \"" +
                                Specification.class.getName() +
                                "\""
                );
            }
            if (parameterizedType.getActualTypeArguments()[0] != type.getJavaClass()) {
                throw new IllegalArgumentException(
                        "The type argument of specification parameter must be \"" +
                                type.getJavaClass().getName() +
                                "\""
                );
            }
        }
        if (fetcherParamIndex != -1) {
            if (query.getAction() != Query.Action.FIND) {
                throw new IllegalArgumentException("The method must be object finding method when there is a fetcher parameter");
            }
            if (query.getSelectedPath() != null) {
                throw new IllegalArgumentException("Cannot explicitly select columns when there is a fetcher parameter");
            }
            Type fetcherType = genericParameterTypes[fetcherParamIndex];
            if (!(fetcherType instanceof ParameterizedType)) {
                throw new IllegalArgumentException("The fetcher parameter must be parameterized type");
            }
            ParameterizedType parameterizedType = (ParameterizedType) fetcherType;
            if (parameterizedType.getActualTypeArguments()[0] != type.getJavaClass()) {
                throw new IllegalArgumentException(
                        "The type argument of fetch parameter must be \"" +
                                type.getJavaClass() +
                                "\""
                );
            }
        }
        if (viewTypeParamIndex != -1 && query.getAction() != Query.Action.FIND) {
            throw new IllegalArgumentException("The method must be query method when there is a view type parameter");
        }
        if (pageableParamIndex != -1 && query.getAction() != Query.Action.FIND) {
            throw new IllegalArgumentException("The method must be query method when there is a pageable parameter");
        }
        if (sortParamIndex != -1 && query.getAction() != Query.Action.FIND) {
            throw new IllegalArgumentException("The method must be query method when there is a sort parameter");
        }

        return new QueryMethod(
                method,
                query,
                viewType,
                pageableParamIndex,
                sortParamIndex,
                specificationIndex,
                fetcherParamIndex,
                viewTypeParamIndex
        );
    }

    private ReturnedElementType returnedElementType(Type type, Class<?> entityType) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz == entityType) {
                return new ReturnedElementType(null);
            } else if (View.class.isAssignableFrom(clazz)) {
                Type[] typeArguments = GenericTypeResolver.resolveTypeArguments(clazz, View.class);
                if (typeArguments != null && typeArguments[0] == entityType) {
                    return new ReturnedElementType(clazz);
                }
            }
        } else if (type instanceof TypeVariable<?>) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            if (method.equals(typeVariable.getGenericDeclaration())) {
                Type boundType = typeVariable.getBounds()[0];
                if (boundType instanceof ParameterizedType) {
                    ParameterizedType parameterizedBoundType = (ParameterizedType) boundType;
                    if (parameterizedBoundType.getRawType() == View.class) {
                        if (parameterizedBoundType.getActualTypeArguments()[0] == entityType) {
                            return new ReturnedElementType(null);
                        }
                    }
                }
            }
        }
        return null;
    }

    private Predicate resolve(Predicate predicate) {
        if (predicate instanceof AndPredicate) {
            return AndPredicate.of(
                    ((AndPredicate)predicate)
                            .getPredicates()
                            .stream()
                            .map(this::resolve)
                            .collect(Collectors.toList())
            );
        } else if (predicate instanceof OrPredicate) {
            return OrPredicate.of(
                    ((OrPredicate)predicate)
                            .getPredicates()
                            .stream()
                            .map(this::resolve)
                            .collect(Collectors.toList())
            );
        } else if (predicate instanceof PropPredicate) {
            return resolve((PropPredicate) predicate);
        } else {
            throw new AssertionError("Internal bug, unexpected prop predicate: " + predicate);
        }
    }

    private Predicate resolve(PropPredicate propPredicate) {
        if (!propPredicate.getPath().isScalar() &&
                propPredicate.getOp() != PropPredicate.Op.NULL &&
                propPredicate.getOp() != PropPredicate.Op.NOT_NULL) {
            throw new IllegalArgumentException(
                    "Illegal property \"" +
                            propPredicate.getPath() +
                            "\" of \"" +
                            propPredicate.getPath().getSource() +
                            "\", it cannot be reference property when the predicate is nether `IsNull` nor `IsNotNull`"
            );
        }
        switch (propPredicate.getOp()) {
            case TRUE:
            case FALSE:
                if (!Classes.matches(propPredicate.getPath().getType(), boolean.class)) {
                    throw new IllegalArgumentException(
                            "Illegal property \"" +
                                    propPredicate.getPath() +
                                    "\", its type must be boolean when the predicate is `IsTrue` or `IsFalse`"
                    );
                }
                return ((PropPredicate.Unresolved)propPredicate).resolve();
            case NULL:
            case NOT_NULL:
                return ((PropPredicate.Unresolved)propPredicate).resolve();
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
            case LIKE:
            case NOT_LIKE:
            case IN:
            case NOT_IN:
                return ((PropPredicate.Unresolved)propPredicate).resolve(nextParam(propPredicate));
            case BETWEEN:
            case NOT_BETWEEN:
                return ((PropPredicate.Unresolved)propPredicate).resolve(
                        nextParam(propPredicate),
                        nextParam(propPredicate)
                );
            default:
                throw new AssertionError(
                        "Internal bug: unexpected predicate op: " +
                                propPredicate.getOp()
                );
        }
    }

    private Param nextParam(PropPredicate predicate) {
        paramIndex++;
        while (paramIndex < parameterTypes.length && isImplicitParameterType(parameterTypes[paramIndex])) {
            paramIndex++;
        }
        if (paramIndex >= parameterTypes.length) {
            throw new IllegalArgumentException(
                    "No enough parameters for the property \"" +
                            predicate.getPath() +
                            "\" of \"" +
                            predicate.getPath().getSource() +
                            "\""
            );
        }
        String expectedTypeName = null;
        String actualTypeName = null;
        boolean isCollection = false;
        if (predicate.getOp() == PropPredicate.Op.IN || predicate.getOp() == PropPredicate.Op.NOT_IN) {
            boolean valid = false;
            ParameterizedType parameterizedType =
                    genericParameterTypes[paramIndex] instanceof ParameterizedType ?
                            (ParameterizedType) genericParameterTypes[paramIndex] :
                            null;
            if (parameterizedType != null) {
                if (parameterizedType.getRawType() == Collection.class || parameterizedType.getRawType() == List.class) {
                    valid = Classes.boxTypeOf(predicate.getPath().getType()) == parameterizedType.getActualTypeArguments()[0];
                }
            }
            if (!valid) {
                expectedTypeName = "Collection<" + Classes.boxTypeOf(predicate.getPath().getType()).getName() + '>';
                actualTypeName = parameterizedType == null ?
                        parameterTypes[paramIndex].getName() :
                        parameterizedType.getRawType().getTypeName() +
                                '<' +
                                Arrays.stream(parameterizedType.getActualTypeArguments())
                                        .map(Type::getTypeName)
                                        .collect(Collectors.joining(", "))+
                                '>';
                isCollection = true;
            }
        } else {
            if (!Classes.matches(parameterTypes[paramIndex], predicate.getPath().getType())) {
                expectedTypeName = predicate.getPath().getType().getName();
                actualTypeName = parameterTypes[paramIndex].getName();
            }
        }
        if (expectedTypeName != null) {
            throw new IllegalArgumentException(
                    "This type of " +
                            (isCollection ? "the collection whose element is the " : "") +
                            "property \"" +
                            predicate.getPath() +
                            "\" is \"" +
                            expectedTypeName +
                            "\", but the type of parameters[" +
                            paramIndex +
                            "] of java method is \"" +
                            actualTypeName +
                            "\""
            );
        }
        return new Param(paramIndex, ++logicParamIndex);
    }

    private static boolean isImplicitParameterType(Class<?> type) {
        return Pageable.class.isAssignableFrom(type) ||
                Sort.class.isAssignableFrom(type) ||
                Specification.class.isAssignableFrom(type) ||
                Fetcher.class.isAssignableFrom(type) ||
                Class.class.isAssignableFrom(type) ||
                KClass.class.isAssignableFrom(type);
    }

    private int implicitParameterIndex(Class<?> type) {
        int index = -1;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (type.isAssignableFrom(parameterTypes[i])) {
                if (index != -1) {
                    throw new IllegalArgumentException(
                            "Both parameters[" +
                                    index +
                                    "] and parameters[" +
                                    i +
                                    "] are of type \"" +
                                    type.getName() +
                                    "\""
                    );
                }
                index = i;
            }
        }
        return index;
    }

    private static boolean isPage(Class<?> returnType) {
        return returnType == Page.class || returnType == org.babyfish.jimmer.Page.class;
    }

    static class Param {

        private final int index;

        private final int logicIndex;

        public Param(int index, int logicIndex) {
            this.index = index;
            this.logicIndex = logicIndex;
        }

        public int getIndex() {
            return index;
        }

        public int getLogicIndex() {
            return logicIndex;
        }

        @Override
        public String toString() {
            return "Param{" +
                    "index=" + index +
                    ", logicIndex=" + logicIndex +
                    '}';
        }
    }

    static class ReturnedElementType {

        final Class<?> viewType;

        ReturnedElementType(Class<?> viewType) {
            this.viewType = viewType;
        }
    }
}
