package org.babyfish.jimmer.spring.graphql;

import graphql.language.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import org.babyfish.jimmer.meta.ImmutableProp;
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
        Context ctx = new Context(env, rootType);
        ctx.add(env.getMergedField().getSingleField().getSelectionSet());
        return (Fetcher<T>) ctx.fetcher;
    }

    private static class Context {

        private final DataFetchingEnvironment env;

        private final Class<?> type;

        private FetcherImplementor<?> fetcher;

        Context(DataFetchingEnvironment env, Class<?> type) {
            this.env = env;
            this.type = type;
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
            if (fetcher == null) {
                fetcher = new FetcherImpl<>(type);
            }
            FetcherImplementor<?> childFetcher = null;
            if (field.getSelectionSet() != null) {
                ImmutableProp prop = fetcher.getImmutableType().getProps().get(field.getName());
                if (prop != null) {
                    Context subContext = new Context(env, prop.getTargetType().getJavaClass());
                    subContext.add(field.getSelectionSet());
                    childFetcher = subContext.fetcher;
                }
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
            return isValidSimpleName(((GraphQLObjectType) graphQLType).getName(), type);
        }

        private static boolean isValidSimpleName(String simpleName, Class<?> t) {
            if (t.getSimpleName().equals(simpleName)) {
                return true;
            }
            for (Class<?> itf : t.getInterfaces()) {
                if (isValidSimpleName(simpleName, itf)) {
                    return true;
                }
            }
            return false;
        }
    }
}

