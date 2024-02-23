package org.babyfish.jimmer.spring.graphql;

import graphql.language.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;

public class DataFetchingEnvironments {

    private DataFetchingEnvironments() {}

    @SuppressWarnings("unchecked")
    public static <T> Fetcher<T> createFetcher(
            Class<T> rootType,
            DataFetchingEnvironment env
    ) {
        ImmutableType type = ImmutableType.tryGet(rootType);
        if (type == null || !type.isEntity()) {
            throw new IllegalArgumentException(
                    "The root type \"" +
                            rootType +
                            "\" is not entity type so that it cannot be used to " +
                            "create fetcher from DataFetchingEnvironment"
            );
        }
        Context ctx = new Context(env, type);
        ctx.add(env.getMergedField().getSingleField().getSelectionSet());
        return (Fetcher<T>) ctx.fetcher;
    }

    private static class Context {

        private final DataFetchingEnvironment env;

        private final ImmutableType immutableType;

        private FetcherImplementor<?> fetcher;

        Context(DataFetchingEnvironment env, ImmutableType immutableType) {
            this.env = env;
            this.immutableType = immutableType;
        }

        @SuppressWarnings("rawtypes")
        public void add(SelectionSet selectionSet) {
            if (selectionSet == null) {
                return;
            }
            for (Selection<?> selection : selectionSet.getSelections()) {
                if (selection instanceof Field) {
                    add((Field) selection);
                } else if (selection instanceof FragmentSpread) {
                    add((FragmentSpread) selection);
                } else if (selection instanceof InlineFragment) {
                    add((InlineFragment) selection);
                }
            }
        }

        private void add(Field field) {
            if (!field.getArguments().isEmpty()) {
                return;
            }
            ImmutableProp prop = immutableType.getProps().get(field.getName());
            if (prop == null) {
                return;
            }
            if (fetcher == null) {
                fetcher = new FetcherImpl<>(immutableType.getJavaClass());
            }

            FetcherImplementor<?> childFetcher = null;
            if (field.getSelectionSet() != null && prop.isAssociation(TargetLevel.ENTITY)) {
                Context subContext = new Context(env, prop.getTargetType());
                subContext.add(field.getSelectionSet());
                childFetcher = subContext.fetcher;
            }
            fetcher = fetcher.add(field.getName(), childFetcher);
        }

        private void add(FragmentSpread fragmentSpread) {
            FragmentDefinition definition = env.getFragmentsByName().get(fragmentSpread.getName());
            if (definition == null) {
                return;
            }
            if (isValidFragmentOwner(definition.getTypeCondition())) {
                add(definition.getSelectionSet());
            }
        }

        private void add(InlineFragment inlineFragment) {
            if (isValidFragmentOwner(inlineFragment.getTypeCondition())) {
                add(inlineFragment.getSelectionSet());
            }
        }

        private boolean isValidFragmentOwner(TypeName fragmentOwner) {
            GraphQLType graphQLType = env.getGraphQLSchema().getType(fragmentOwner.getName());
            if (!(graphQLType instanceof GraphQLObjectType)) {
                return false;
            }
            return isValidSimpleName(((GraphQLObjectType) graphQLType).getName(), immutableType.getJavaClass());
        }

        private static boolean isValidSimpleName(String simpleName, Class<?> type) {
            if (type.getSimpleName().equals(simpleName)) {
                return true;
            }
            for (Class<?> itf : type.getInterfaces()) {
                if (isValidSimpleName(simpleName, itf)) {
                    return true;
                }
            }
            return false;
        }
    }
}

