package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;

import java.util.StringJoiner;

class FetcherWriter {

    private final StringBuilder builder = new StringBuilder();

    private final String indent;

    private int depth;

    public FetcherWriter() {
        indent = "";
    }

    public FetcherWriter(int indent) {
        if (indent > 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = indent; i > 0; --i) {
                builder.append(' ');
            }
            this.indent = builder.toString();
        } else {
            this.indent = "";
        }
    }

    public void write(Fetcher<?> fetcher) {
        builder.append(' ');
        if (indent.isEmpty()) {
            builder.append("{ ");
            boolean first = true;
            for (Field field : fetcher.getFieldMap().values()) {
                if (!field.isImplicit()) {
                    if (!first) {
                        builder.append(", ");
                    } else {
                        first = false;
                    }
                    write(field);
                }
            }
            builder.append(" }");
        } else {
            builder.append('{');
            depth++;
            for (Field field : fetcher.getFieldMap().values()) {
                if (!field.isImplicit()) {
                    newLine();
                    write(field);
                }
            }
            depth--;
            newLine();
            builder.append('}');
        }
    }

    public void writeRoot(Fetcher<?> fetcher) {
        builder.append(fetcher.getImmutableType());
        write(fetcher);
    }

    private void newLine() {
        builder.append('\n');
        if (depth != 0 && !indent.isEmpty()) {
            for (int i = depth; i > 0; --i) {
                builder.append(indent);
            }
        }
    }

    public void write(Field field) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")").setEmptyValue("");
        int batchSize = field.getBatchSize();
        int limit = field.getLimit();
        int offset = field.getOffset();
        RecursionStrategy<?> recursionStrategy = field.getRecursionStrategy();
        FieldFilter<?> filter = field.getFilter();
        Fetcher<?> childFetcher = field.getChildFetcher();
        if (batchSize != 0) {
            joiner.add("batchSize: " + batchSize);
        }
        if (limit != Integer.MAX_VALUE) {
            joiner.add("limit: " + limit);
        }
        if (offset != 0) {
            joiner.add("offset: " + offset);
        }
        if (recursionStrategy instanceof DefaultRecursionStrategy<?>) {
            int depth = ((DefaultRecursionStrategy<?>) recursionStrategy).getDepth();
            if (depth == Integer.MAX_VALUE) {
                joiner.add("recursive: true");
            } else if (depth > 1) {
                joiner.add("depth: " + depth);
            }
        } else if (recursionStrategy != null) {
            joiner.add("recursive: <java-code>");
        }
        if (filter != null) {
            joiner.add("filter: <java-code>");
        }
        builder.append(field.getProp().getName()).append(joiner.toString());
        if (childFetcher != null) {
            write(childFetcher);
        }
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
