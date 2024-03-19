package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.util.Map;
import java.util.Objects;

class FetcherMergeContext {

    private final String path;

    FetcherMergeContext() {
        this("");
    }

    private FetcherMergeContext(String path) {
        this.path = path;
    }

    @SuppressWarnings("unchecked")
    public Fetcher<?> merge(Fetcher<?> fetcher1, Fetcher<?> fetcher2, boolean implicit) throws ConflictException {
        if (fetcher1 == null) {
            return fetcher2;
        }
        if (fetcher2 == null) {
            return fetcher1;
        }
        Map<String, Field> fieldMap2 = fetcher2.getFieldMap();
        for (Field field1 : fetcher1.getFieldMap().values()) {
            if (field1.getProp().isId()) {
                continue;
            }
            Field field2 = fieldMap2.get(field1.getProp().getName());
            if (field2 != null) {
                String conflictCfgName = null;
                if (field1.getBatchSize() != field2.getBatchSize()) {
                    conflictCfgName = "batchSize";
                } else if (field1.getLimit() != field2.getLimit()) {
                    conflictCfgName = "limit";
                } else if (field1.getOffset() != field2.getOffset()) {
                    conflictCfgName = "offset";
                } else if (!Objects.equals(field1.getFilter(),field1.getFilter())) {
                    conflictCfgName = "batchSize";
                } else if (!Objects.equals(field1.getRecursionStrategy(),field1.getRecursionStrategy())) {
                    conflictCfgName = "batchSize";
                }
                if (conflictCfgName != null) {
                    throw new ConflictException(path, conflictCfgName);
                }
                if (field1.getRecursionStrategy() != null || field2.getRecursionStrategy() != null) {
                    throw new IllegalArgumentException(
                            "Both \"" +
                                    field1.getProp() +
                                    "\" and \"" +
                                    field2.getProp() +
                                    "\" are fetched, so the recursion strategy cannot be specified"
                    );
                }
            }
            fetcher2 = new FetcherImpl<>(
                    (FetcherImpl<Object>) fetcher2,
                    field1.getProp(),
                    (FetcherImpl<?>) subContext(field1.getProp().getName()).merge(
                            field2 != null ? field2.getChildFetcher() : null,
                            field1.getChildFetcher(),
                            implicit
                    ),
                    implicit && (field2 == null || field2.isImplicit())
            );
        }
        return fetcher2;
    }

    private FetcherMergeContext subContext(String prop) {
        return new FetcherMergeContext('.' + prop);
    }

    public static class ConflictException extends Exception {

        final String path;

        final String cfgName;

        public ConflictException(String path, String cfgName) {
            this.path = path;
            this.cfgName = cfgName;
        }
    }
}
