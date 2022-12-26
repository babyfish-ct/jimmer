package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.LikeMode;

class PropPredicateParser {

    private final Context ctx;

    private final boolean allowCollection;

    private final boolean allIgnoreCase;

    private Path path;

    private PropPredicate.Op op = PropPredicate.Op.EQ;

    private boolean ignoreCase;

    private LikeMode likeMode = LikeMode.EXACT;

    PropPredicateParser(Context ctx, boolean allowCollection, boolean allIgnoreCase) {
        this.ctx = ctx;
        this.allowCollection = allowCollection;
        this.allIgnoreCase = allIgnoreCase;
    }

    PropPredicate parse(Source source, ImmutableType type) {
        source = parseIgnoreCase(source);
        source = parseOp(source);
        Path path = new PathParser(ctx, allowCollection).parse(source, type);
        return new PropPredicate.Unresolved(
                path,
                op,
                ignoreCase,
                likeMode
        );
    }

    private Source parseIgnoreCase(Source source) {
        Source restSource = source.trimEnd("IgnoringCase", "IgnoreCase");
        if (restSource != null) {
            if (allIgnoreCase) {
                throw new IllegalArgumentException(
                        "The predicate \"" +
                                source +
                                "\" cannot be ignore case when \"AllIgnoreCase\" is already set"
                );
            }
        }
        ignoreCase = allIgnoreCase || restSource != null;
        return restSource != null ? restSource : source;
    }

    private Source parseOp(Source source) {
        
        Source restSource;
        
        restSource= source.trimEnd("IsTrue", "True");
        if (restSource != null){
            op = PropPredicate.Op.TRUE;
            return restSource;
        }
        
        restSource = source.trimEnd("IsFalse", "False");
        if (restSource != null) {
            op = PropPredicate.Op.FALSE;
            return restSource;
        }

        restSource = source.trimEnd("Is", "Equals");
        if (restSource != null){
            op = PropPredicate.Op.EQ;
            return restSource;
        }
        
        restSource = source.trimEnd("IsNot", "Not");
        if (restSource != null) {
            op = PropPredicate.Op.NE;
            return restSource;
        }
        
        restSource = source.trimEnd("IsLessThan", "LessThan");
        if (restSource != null) {
            op = PropPredicate.Op.LT;
            return restSource;
        }

        restSource = source.trimEnd("IsLessThanEqual", "LessThanEqual");
        if (restSource != null) {
            op = PropPredicate.Op.LE;
            return restSource;
        }

        restSource = source.trimEnd("IsGreaterThan", "GreaterThan");
        if (restSource != null) {
            op = PropPredicate.Op.GT;
            return restSource;
        }

        restSource = source.trimEnd("IsGreaterThanEqual", "GreaterThanEqual");
        if (restSource != null) {
            op = PropPredicate.Op.GE;
            return restSource;
        }

        restSource = source.trimEnd("IsBefore", "Before");
        if (restSource != null) {
            op = PropPredicate.Op.LT;
            return restSource;
        }

        restSource = source.trimEnd("IsAfter", "After");
        if (restSource != null) {
            op = PropPredicate.Op.GT;
            return restSource;
        }

        restSource = source.trimEnd("IsNotBetween", "NotBetween");
        if (restSource != null) {
            op = PropPredicate.Op.NOT_BETWEEN;
            return restSource;
        }

        restSource = source.trimEnd("IsBetween", "Between");
        if (restSource != null) {
            op = PropPredicate.Op.BETWEEN;
            return restSource;
        }

        restSource = source.trimEnd("IsNotLike", "NotLike");
        if (restSource != null) {
            op = PropPredicate.Op.NOT_LIKE;
            likeMode = LikeMode.ANYWHERE;
            return restSource;
        }

        restSource = source.trimEnd("IsLike", "Like");
        if (restSource != null) {
            op = PropPredicate.Op.LIKE;
            likeMode = LikeMode.ANYWHERE;
            return restSource;
        }

        restSource = source.trimEnd("IsContaining", "Containing", "Contains");
        if (restSource != null) {
            op = PropPredicate.Op.LIKE;
            likeMode = LikeMode.ANYWHERE;
            return restSource;
        }

        restSource = source.trimEnd("IsStartingWith", "StartingWith", "StartsWith");
        if (restSource != null) {
            op = PropPredicate.Op.LIKE;
            likeMode = LikeMode.START;
            return restSource;
        }

        restSource = source.trimEnd("IsEndingWith", "EndingWith", "EndsWith");
        if (restSource != null) {
            op = PropPredicate.Op.LIKE;
            likeMode = LikeMode.END;
            return restSource;
        }

        restSource = source.trimEnd("IsNotIn", "NotIn");
        if (restSource != null) {
            op = PropPredicate.Op.NOT_IN;
            return restSource;
        }

        restSource = source.trimEnd("IsIn", "In");
        if (restSource != null) {
            op = PropPredicate.Op.IN;
            return restSource;
        }

        restSource = source.trimEnd("IsNotNull", "NotNull");
        if (restSource != null) {
            op = PropPredicate.Op.NOT_NULL;
            return restSource;
        }

        restSource = source.trimEnd("IsNull", "Null");
        if (restSource != null) {
            op = PropPredicate.Op.NULL;
            return restSource;
        }

        op = PropPredicate.Op.EQ;
        return source;
    }
}
