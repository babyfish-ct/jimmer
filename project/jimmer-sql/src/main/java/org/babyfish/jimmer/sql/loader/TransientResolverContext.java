package org.babyfish.jimmer.sql.loader;

import org.babyfish.jimmer.sql.TransientResolver;

import java.sql.Connection;
import java.util.*;

public class TransientResolverContext {

    private static final ThreadLocal<TransientResolverContext> CONTEXT_LOCAL = new ThreadLocal<>();

    private final TransientResolverContext parent;

    private final Connection con;

    private final TransientResolver<?, ?> resolver;

    private final Set<Object> sourceIds;

    private TransientResolverContext(
            TransientResolverContext parent,
            Connection con,
            TransientResolver<?, ?> resolver,
            Set<Object> sourceIds
    ) {
        this.parent = parent;
        this.con = con;
        this.resolver = resolver;
        this.sourceIds = sourceIds;
    }

    public Connection getConnection() {
        return con;
    }

    public static TransientResolverContext push(
            Connection con,
            TransientResolver<?, ?> resolver,
            Collection<Object> sourceIds
    ) {
        Set<Object> sourceIdSet;
        if (sourceIds instanceof Set<?>) {
            sourceIdSet = (Set<Object>) sourceIds;
        } else {
            sourceIdSet = new LinkedHashSet<>(sourceIds);
        }
        TransientResolverContext parent = CONTEXT_LOCAL.get();
        for (TransientResolverContext ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor.resolver.equals(resolver)) {
                List<Object> intersect = intersect(sourceIdSet, ancestor.sourceIds);
                if (intersect != null) {
                    throw new IllegalStateException(
                            "Discover the dead recursion of transient resolver invocations, " +
                                    "transient resolver: " +
                                    ancestor.resolver +
                                    "sourceIds: " + intersect
                    );
                }
            }
        }
        TransientResolverContext ctx = new TransientResolverContext(
                parent,
                con,
                resolver,
                sourceIdSet
        );
        CONTEXT_LOCAL.set(ctx);
        return ctx;
    }

    public static void pop(TransientResolverContext ctx) {
        CONTEXT_LOCAL.set(ctx.parent);
    }

    public static TransientResolverContext peek() {
        return CONTEXT_LOCAL.get();
    }

    private static List<Object> intersect(Set<Object> a, Set<Object> b) {
        if (a.size() > b.size()) {
            return intersect(b, a);
        }
        List<Object> intersect = null;
        for (Object e : a) {
            if (b.contains(e)) {
                if (intersect == null) {
                    intersect = new ArrayList<>();
                }
                intersect.add(e);
            }
        }
        return intersect;
    }
}
