package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class PropConfigBuilder<T extends BaseType, P extends BaseProp> {

    private final CompilerContext<T, P> ctx;

    private final T baseType;

    private PropConfig.Predicate predicate;

    private List<PropConfig.OrderItem<P>> orderItems = Collections.emptyList();

    private String filterClassName;

    private String recursionClassName;

    private String fetchType;

    private int limit = Integer.MAX_VALUE;

    private int offset;

    private int batch;

    private int depth = Integer.MAX_VALUE;

    private boolean modified;

    PropConfigBuilder(CompilerContext<T, P> ctx, T baseType) {
        this.ctx = ctx;
        this.baseType = baseType;
    }

    public void setPredicate(DtoParser.WhereContext where) {
        if (filterClassName != null) {
            throw ctx.exception(
                    where.start.getLine(),
                    where.start.getCharPositionInLine(),
                    "Cannot specify `#where` when `#filter` exists"
            );
        }
        this.predicate = createPredicate(where.predicate());
        this.modified = true;
    }

    void setOrderItems(DtoParser.OrderByContext orderBy) {
        if (filterClassName != null) {
            throw ctx.exception(
                    orderBy.start.getLine(),
                    orderBy.start.getCharPositionInLine(),
                    "Cannot specify `#orderBy` when `#filter` exists"
            );
        }
        List<DtoParser.OrderByItemContext> orderItems = orderBy.items;
        List<PropConfig.OrderItem<P>> items = new ArrayList<>(orderItems.size());
        for (DtoParser.OrderByItemContext item : orderItems) {
            items.add(
                    new OrderItemImpl<>(
                            createPropPath(item.propPath()),
                            item.desc != null
                    )
            );
        }
        this.orderItems = Collections.unmodifiableList(items);
        this.modified = true;
    }

    void setFilterClassName(DtoParser.FilterContext filter) {
        if (predicate != null) {
            throw ctx.exception(
                    filter.start.getLine(),
                    filter.start.getCharPositionInLine(),
                    "Cannot specify `#filter` when `#where` exists"
            );
        }
        if (!orderItems.isEmpty()) {
            throw ctx.exception(
                    filter.start.getLine(),
                    filter.start.getCharPositionInLine(),
                    "Cannot specify `#filter` when `#orderBy` exists"
            );
        }
        this.filterClassName = filter
                .qualifiedName()
                .parts
                .stream()
                .map(Token::getText)
                .collect(Collectors.joining("."));
        this.modified = true;
    }

    void setRecursionClassName(DtoParser.RecursionContext recursion) {
        if (depth != Integer.MAX_VALUE) {
            throw ctx.exception(
                    recursion.start.getLine(),
                    recursion.start.getCharPositionInLine(),
                    "Cannot specify `#recursion` when `#depth` exists"
            );
        }
        this.recursionClassName = recursion
                .qualifiedName()
                .parts
                .stream()
                .map(Token::getText)
                .collect(Collectors.joining("."));
        this.modified = true;
    }

    void setFetchType(DtoParser.FetchTypeContext fetchType) {
        this.fetchType = fetchType.fetchMode().getText();
        this.modified = true;
    }

    void setLimit(DtoParser.LimitContext limit) {
        int value = Integer.parseInt(limit.IntegerLiteral().getText());
        if (value < 1) {
            throw ctx.exception(
                    limit.start.getLine(),
                    limit.start.getCharPositionInLine(),
                    "The limit cannot be less than 1"
            );
        }
        this.limit = value;
        this.modified = true;
    }

    void setOffset(DtoParser.OffsetContext offset) {
        int value = Integer.parseInt(offset.IntegerLiteral().getText());
        if (value < 0) {
            throw ctx.exception(
                    offset.start.getLine(),
                    offset.start.getCharPositionInLine(),
                    "The offset cannot be less than 0"
            );
        }
        this.offset = value;
        this.modified = true;
    }

    void setBatch(DtoParser.BatchContext batch) {
        int value = Integer.parseInt(batch.IntegerLiteral().getText());
        if (value < 1) {
            throw ctx.exception(
                    batch.start.getLine(),
                    batch.start.getCharPositionInLine(),
                    "The batch cannot be less than 1"
            );
        }
        this.batch = value;
        this.modified = true;
    }

    void setDepth(DtoParser.RecursionDepthContext depth) {
        if (recursionClassName != null) {
            throw ctx.exception(
                    depth.start.getLine(),
                    depth.start.getCharPositionInLine(),
                    "Cannot specify `#depth` when `#recusion` exists"
            );
        }
        int value = Integer.parseInt(depth.IntegerLiteral().getText());
        if (value < 0) {
            throw ctx.exception(
                    depth.start.getLine(),
                    depth.start.getCharPositionInLine(),
                    "The offset cannot be less than 0"
            );
        }
        this.depth = value;
        this.modified = true;
    }

    PropConfig<P> build() {
        if (!modified) {
            return null;
        }
        return new PropConfigImpl<>(
                predicate,
                orderItems,
                filterClassName,
                recursionClassName,
                fetchType,
                limit,
                offset,
                batch,
                depth
        );
    }

    private PropConfig.Predicate createPredicate(DtoParser.PredicateContext predicate) {
        List<PropConfig.Predicate> predicates =
                new ArrayList<>(predicate.subPredicates.size());
        for (DtoParser.AndPredicateContext p : predicate.subPredicates) {
            predicates.add(createPredicate(p));
        }
        return OrPredicateImpl.of(predicates);
    }

    private PropConfig.Predicate createPredicate(DtoParser.AndPredicateContext predicate) {
        List<PropConfig.Predicate> predicates =
                new ArrayList<>(predicate.subPredicates.size());
        for (DtoParser.AtomPredicateContext p : predicate.subPredicates) {
            predicates.add(createPredicate(p));
        }
        return AndPredicateImpl.of(predicates);
    }

    private PropConfig.Predicate createPredicate(DtoParser.AtomPredicateContext predicate) {
        if (predicate.cmpPredicate() != null) {
            return createPredicate(predicate.cmpPredicate());
        }
        if (predicate.nullityPredicate() != null) {
            return createPredicate(predicate.nullityPredicate());
        }
        return createPredicate(predicate.predicate());
    }

    private PropConfig.Predicate createPredicate(DtoParser.NullityPredicateContext predicate) {
        return new NullityPredicate<>(createPropPath(predicate.propPath()), predicate.not != null);
    }

    private PropConfig.Predicate createPredicate(DtoParser.CmpPredicateContext predicate) {
        if (predicate.op.getType() == DtoLexer.Identifier) {
            String opText = predicate.op.getText();
            switch (opText) {
                case "like":
                case "ilike":
                    break;
                default:
                    throw ctx.exception(
                            predicate.op.getLine(),
                            predicate.op.getCharPositionInLine(),
                            "The infix operator must `like` or `ilike`"
                    );
            }
        }
        List<P> path = createPropPath(predicate.propPath());
        P lastProp = path.get(path.size() - 1);
        SimplePropType simplePropType = ctx.getSimplePropType(lastProp);
        if (simplePropType == SimplePropType.NONE) {
            List<Token> parts = predicate.propPath().parts;
            Token lastPart = parts.get(parts.size() - 1);
            throw ctx.exception(
                    lastPart.getLine(),
                    lastPart.getCharPositionInLine(),
                    "The last property \"" +
                            lastProp +
                            "\" must be boolean, number, string or date"
            );
        }
        return new CmpPredicate<>(
                path,
                predicate.op.getText(),
                createPropValue(predicate.right, simplePropType)
        );
    }

    private List<P> createPropPath(DtoParser.PropPathContext propPath) {
        T baseType = this.baseType;
        int size = propPath.parts.size();
        List<P> props = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Token part = propPath.parts.get(i);
            P prop = ctx.getProps(baseType).get(part.getText());
            if (prop == null) {
                throw ctx.exception(
                        part.getLine(),
                        part.getCharPositionInLine(),
                        "There is no property \"" +
                                part.getText() +
                                "\" in type \"" +
                                baseType.getQualifiedName() +
                                "\""
                );
            }
            if (i + 1< size) {
                if (!prop.isEmbedded()) {
                    throw ctx.exception(
                            part.getLine(),
                            part.getCharPositionInLine(),
                            "There property \"" +
                                    prop +
                                    "\" is not last property but it does not return embeddable type"
                    );
                }
                baseType = ctx.getTargetType(prop);
            }
            props.add(prop);
        }
        return props;
    }

    private Object createPropValue(DtoParser.PropValueContext value, SimplePropType simplePropType) {
        if (!value.stringTokens.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Token token : value.stringTokens) {
                String text = token.getText();
                builder.append(text, 1, text.length() - 1);
            }
            return builder.toString();
        }
        if (value.booleanToken != null) {
            return "true".equals(value.booleanToken.getText());
        }
        if (value.characterToken != null) {
            String text = value.characterToken.getText();
            return text.substring(1, text.length() - 1);
        }
        if (value.integerToken != null) {
            return Integer.parseInt(value.integerToken.getText());
        }
        return new BigDecimal(value.floatingPointToken.getText());
    }

    private abstract static class CompositePredicate implements PropConfig.Predicate.Or {

        private final List<PropConfig.Predicate> predicates;

        CompositePredicate(List<PropConfig.Predicate> predicates) {
            this.predicates = Collections.unmodifiableList(predicates);
        }

        @Override
        public List<PropConfig.Predicate> getPredicates() {
            return predicates;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            boolean addSeparator = false;
            String separator = separator();
            builder.append('(');
            for (PropConfig.Predicate predicate : predicates) {
                if (addSeparator) {
                    builder.append(separator);
                } else {
                    addSeparator = true;
                }
                builder.append(predicate);
            }
            builder.append(')');
            return builder.toString();
        }

        abstract String separator();
    }

    private static class AndPredicateImpl extends CompositePredicate implements PropConfig.Predicate.And {

        private AndPredicateImpl(List<PropConfig.Predicate> predicates) {
            super(predicates);
        }

        static PropConfig.Predicate of(List<PropConfig.Predicate> predicates) {
            if (predicates.size() == 1) {
                return predicates.get(0);
            }
            return new AndPredicateImpl(predicates);
        }

        @Override
        String separator() {
            return " and ";
        }
    }

    private static class OrPredicateImpl extends CompositePredicate implements PropConfig.Predicate.Or {

        private OrPredicateImpl(List<PropConfig.Predicate> predicates) {
            super(predicates);
        }

        static PropConfig.Predicate of(List<PropConfig.Predicate> predicates) {
            if (predicates.size() == 1) {
                return predicates.get(0);
            }
            return new OrPredicateImpl(predicates);
        }

        @Override
        String separator() {
            return " or ";
        }
    }

    private static abstract class PathHolder<P extends BaseProp> {

        final List<P> path;

        PathHolder(List<P> path) {
            this.path = path.size() == 1 ?
                    Collections.singletonList(path.get(0)) :
                    Collections.unmodifiableList(path);
        }

        public List<P> getPath() {
            return path;
        }

        String path() {
            StringBuilder builder = new StringBuilder();
            boolean addComma = false;
            for (P prop : path) {
                if (addComma) {
                    builder.append('.');
                } else {
                    addComma = true;
                }
                builder.append(prop.getName());
            }
            return builder.toString();
        }
    }

    private static class OrderItemImpl<P extends BaseProp> extends PathHolder<P> implements PropConfig.OrderItem<P> {

        private final boolean desc;

        private OrderItemImpl(List<P> path, boolean desc) {
            super(path);
            this.desc = desc;
        }

        @Override
        public boolean isDesc() {
            return desc;
        }

        @Override
        public String toString() {
            return path() + (desc ? " desc" : " asc");
        }
    }

    private static class NullityPredicate<P extends BaseProp>
            extends PathHolder<P>
            implements PropConfig.Predicate.Nullity<P> {

        private final boolean negative;

        NullityPredicate(List<P> path, boolean negative) {
            super(path);
            this.negative = negative;
        }

        @Override
        public boolean isNegative() {
            return negative;
        }

        @Override
        public String toString() {
            return path() + (negative ? " is not null" : " is null");
        }
    }

    private static class CmpPredicate<P extends BaseProp> extends PathHolder<P> implements PropConfig.Predicate.Cmp<P> {

        private final String operator;

        private final Object value;

        CmpPredicate(List<P> path, String operator, Object value) {
            super(path);
            this.operator = operator;
            this.value = value;
        }

        @Override
        public String getOperator() {
            return operator;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return path() + " " +operator + " " +
                    (value instanceof String ? "\"" + value + "\"" : value);
        }
    }

    private static class PropConfigImpl<P extends BaseProp> implements PropConfig<P> {

        private final PropConfig.Predicate predicate;

        private final List<PropConfig.OrderItem<P>> orderItems;

        private final String filterClassName;

        private final String recursionClassName;

        private final String fetchType;

        private final int limit;

        private final int offset;

        private final int batch;

        private final int depth;

        private PropConfigImpl(
                Predicate predicate,
                List<OrderItem<P>> orderItems,
                String filterClassName,
                String recursionClassName,
                String fetchType,
                int limit,
                int offset,
                int batch,
                int depth
        ) {
            this.predicate = predicate;
            this.orderItems = orderItems;
            this.filterClassName = filterClassName;
            this.recursionClassName = recursionClassName;
            this.fetchType = fetchType;
            this.limit = limit;
            this.offset = offset;
            this.batch = batch;
            this.depth = depth;
        }

        @Nullable
        @Override
        public PropConfig.Predicate getPredicate() {
            return predicate;
        }

        @Override
        public List<OrderItem<P>> getOrderItems() {
            return orderItems;
        }

        @Nullable
        @Override
        public String getFilterClassName() {
            return filterClassName;
        }

        @Nullable
        @Override
        public String getRecursionClassName() {
            return recursionClassName;
        }

        @Nullable
        @Override
        public String getFetchType() {
            return fetchType;
        }

        @Override
        public int getLimit() {
            return limit;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public int getBatch() {
            return batch;
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            CommaAdder commaAdder = new CommaAdder(builder);
            if (predicate != null) {
                commaAdder.add();
                builder.append("#where(").append(predicate).append(')');
            }
            if (!orderItems.isEmpty()) {
                commaAdder.add();
                builder.append("#orderBy(");
                boolean addComma = false;
                for (OrderItem<P> item : orderItems) {
                    if (addComma) {
                        builder.append(", ");
                    } else {
                        addComma = true;
                    }
                    builder.append(item);
                }
                builder.append(')');
            }
            if (filterClassName != null) {
                commaAdder.add();
                builder.append("#filter(").append(filterClassName).append(')');
            }
            if (recursionClassName != null) {
                commaAdder.add();
                builder.append("#recursion(").append(recursionClassName).append(')');
            }
            if (fetchType != null) {
                commaAdder.add();
                builder.append("#fetchType(").append(fetchType).append(')');
            }
            if (limit != Integer.MAX_VALUE) {
                commaAdder.add();
                builder.append("#limit(").append(limit).append(')');
            }
            if (offset != 0) {
                commaAdder.add();
                builder.append("#offset(").append(offset).append(')');
            }
            if (batch != 0) {
                commaAdder.add();
                builder.append("#batch(").append(batch).append(')');
            }
            if (depth != Integer.MAX_VALUE) {
                commaAdder.add();
                builder.append("#depth(").append(depth).append(')');
            }
            return builder.toString();
        }

        private static class CommaAdder {

            private final StringBuilder builder;

            private boolean add;

            CommaAdder(StringBuilder builder) {
                this.builder = builder;
            }

            void add() {
                if (add) {
                    builder.append(", ");
                } else {
                    add = true;
                }
            }
        }
    }
}
