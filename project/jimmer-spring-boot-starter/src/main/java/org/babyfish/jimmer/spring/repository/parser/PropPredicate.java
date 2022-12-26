package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.LikeMode;

public abstract class PropPredicate implements Predicate {

    protected final Path path;

    protected final Op op;

    protected final boolean insensitive;

    protected final LikeMode likeMode;

    public static PropPredicate of(Context ctx, boolean allowCollection, boolean allIgnoreCase, Source source, ImmutableType type) {
        return new PropPredicateParser(ctx, allowCollection, allIgnoreCase).parse(source, type);
    }

    public PropPredicate(
            Path path,
            Op op,
            boolean insensitive,
            LikeMode likeMode
    ) {
        this.path = path;
        this.op = op;
        this.insensitive = insensitive;
        this.likeMode = likeMode;
    }

    public Path getPath() {
        return path;
    }

    public Op getOp() {
        return op;
    }

    public boolean isInsensitive() {
        return insensitive;
    }

    public LikeMode getLikeMode() {
        return likeMode;
    }

    public abstract int getParamIndex();

    public abstract int getLogicParamIndex();

    public abstract int getParamIndex2();

    public abstract int getLogicParamIndex2();

    public enum Op {
        TRUE("IsTrue"),
        FALSE("IsFalse"),
        EQ("Equals"),
        NE("NotEqual"),
        LT("LessThan"),
        LE("LessThanEqual"),
        GT("GreaterThan"),
        GE("GreaterThanEqual"),
        NULL("IsNull"),
        NOT_NULL("IsNot"),
        IN("In"),
        NOT_IN("NotIn"),
        BETWEEN("Between"),
        NOT_BETWEEN("NotBetween"),
        LIKE("Like"),
        NOT_LIKE("NotLike");

        private final String text;

        Op(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    static class Unresolved extends PropPredicate {

        public Unresolved(Path path, Op op, boolean insensitive, LikeMode likeMode) {
            super(path, op, insensitive, likeMode);
        }

        @Override
        public int getParamIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLogicParamIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getParamIndex2() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLogicParamIndex2() {
            throw new UnsupportedOperationException();
        }

        Resolved resolve() {
            return new Resolved(
                    path,
                    op,
                    insensitive,
                    likeMode,
                    -1,
                    -1,
                    -1,
                    -1
            );
        }

        Resolved resolve(QueryMethodParser.Param param) {
            return new Resolved(
                    path,
                    op,
                    insensitive,
                    likeMode,
                    param.getIndex(),
                    param.getLogicIndex(),
                    -1,
                    -1
            );
        }

        Resolved resolve(QueryMethodParser.Param param, QueryMethodParser.Param param2) {
            return new Resolved(
                    path,
                    op,
                    insensitive,
                    likeMode,
                    param.getIndex(),
                    param.getLogicIndex(),
                    param2.getIndex(),
                    param2.getLogicIndex()
            );
        }

        @Override
        public String toString() {
            return "UnresolvedPredicate{" +
                    "path=" + path +
                    ", op=" + op +
                    ", insensitive=" + insensitive +
                    ", likeMode=" + likeMode +
                    '}';
        }
    }

    static class Resolved extends PropPredicate {

        private final int paramIndex;

        private final int logicParamIndex;

        private final int paramIndex2;

        private final int logicParamIndex2;

        public Resolved(
                Path path,
                Op op,
                boolean insensitive,
                LikeMode likeMode,
                int paramIndex,
                int logicParamIndex,
                int paramIndex2,
                int logicParamIndex2
        ) {
            super(path, op, insensitive, likeMode);
            this.paramIndex = paramIndex;
            this.logicParamIndex = logicParamIndex;
            this.paramIndex2 = paramIndex2;
            this.logicParamIndex2 = logicParamIndex2;
        }

        @Override
        public int getParamIndex() {
            return paramIndex;
        }

        @Override
        public int getLogicParamIndex() {
            return logicParamIndex;
        }

        @Override
        public int getParamIndex2() {
            return paramIndex2;
        }

        @Override
        public int getLogicParamIndex2() {
            return logicParamIndex2;
        }

        @Override
        public String toString() {
            return "ResolvedPredicate{" +
                    "path=" + path +
                    ", op=" + op +
                    ", insensitive=" + insensitive +
                    ", likeMode=" + likeMode +
                    ", paramIndex=" + paramIndex +
                    ", logicParamIndex=" + logicParamIndex +
                    ", paramIndex2=" + paramIndex2 +
                    ", logicParamIndex2=" + logicParamIndex2 +
                    '}';
        }
    }
}
