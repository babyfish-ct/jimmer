package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;
import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class PropConfigBuilder<T extends BaseType, P extends BaseProp> {

    private final CompilerContext<T, P> ctx;

    private final P baseProp;

    private final String funcName;

    private final boolean recursive;

    private PropConfig.Predicate predicate;

    private List<PropConfig.OrderItem<P>> orderItems = Collections.emptyList();

    private String filterClassName;

    private String recursionClassName;

    private String fetchType = "AUTO";

    private int limit = Integer.MAX_VALUE;

    private int offset;

    private int batch;

    private int depth = Integer.MAX_VALUE;

    private boolean modified;

    PropConfigBuilder(
            CompilerContext<T, P> ctx,
            P baseProp,
            String funcName,
            boolean recursive
    ) {
        this.ctx = ctx;
        this.baseProp = baseProp;
        this.funcName = funcName;
        this.recursive = recursive;
    }

    public void setPredicate(DtoParser.WhereContext where) {
        if (filterClassName != null) {
            throw ctx.exception(
                    where.start.getLine(),
                    where.start.getCharPositionInLine(),
                    "Cannot specify \"!where\" when \"!filter\" exists"
            );
        }
        if (!baseProp.isAssociation(true)) {
            throw ctx.exception(
                    where.start.getLine(),
                    where.start.getCharPositionInLine(),
                    "Cannot be specify \"!where\" when the property is not association"
            );
        }
        if (baseProp.isReference() && !baseProp.isNullable()) {
            throw ctx.exception(
                    where.start.getLine(),
                    where.start.getCharPositionInLine(),
                    "Cannot be specify \"!where\" when the property is non-null reference"
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
                    "Cannot specify \"!orderBy\" when \"!filter\" exists"
            );
        }
        if (!baseProp.isAssociation(true) || !baseProp.isList()) {
            throw ctx.exception(
                    orderBy.start.getLine(),
                    orderBy.start.getCharPositionInLine(),
                    "Cannot be specify \"!orderBy\" when the property is not associated list"
            );
        }
        List<DtoParser.OrderByItemContext> orderItems = orderBy.items;
        List<PropConfig.OrderItem<P>> items = new ArrayList<>(orderItems.size());
        for (DtoParser.OrderByItemContext item : orderItems) {
            Token modeToken = item.orderMode;
            String mode = modeToken != null ? modeToken.getText() : null;
            if (mode != null && !"asc".equals(mode) && !"desc".equals(mode)) {
                throw ctx.exception(
                        modeToken.getLine(),
                        modeToken.getCharPositionInLine(),
                        "The order mode is neither \"asc\" nor \"desc\""
                );
            }
            items.add(
                    new OrderItemImpl<>(
                            createPropPath(item.propPath()),
                            "desc".equals(mode)
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
                    "Cannot specify \"!filter\" when \"!where\" exists"
            );
        }
        if (!orderItems.isEmpty()) {
            throw ctx.exception(
                    filter.start.getLine(),
                    filter.start.getCharPositionInLine(),
                    "Cannot specify \"!filter\" when \"!orderBy\" exists"
            );
        }
        if (!baseProp.isAssociation(true) || !baseProp.isList()) {
            throw ctx.exception(
                    filter.start.getLine(),
                    filter.start.getCharPositionInLine(),
                    "Cannot be specify \"!filter\" when the property is not associated list"
            );
        }
        String qualifiedName = filter
                .qualifiedName()
                .parts
                .stream()
                .map(Token::getText)
                .collect(Collectors.joining("."));
        this.filterClassName = ctx.resolve(
                qualifiedName,
                filter.qualifiedName().start.getLine(),
                filter.qualifiedName().start.getCharPositionInLine()
        );
        this.modified = true;
    }

    void setRecursionClassName(DtoParser.RecursionContext recursion) {
        if (depth != Integer.MAX_VALUE) {
            throw ctx.exception(
                    recursion.start.getLine(),
                    recursion.start.getCharPositionInLine(),
                    "Cannot specify \"!recursion\" when \"!depth\" exists"
            );
        }
        if (!recursive) {
            throw ctx.exception(
                    recursion.start.getLine(),
                    recursion.start.getCharPositionInLine(),
                    "\"!recursion\" can only be applied for recursive property"
            );
        }
        String qualifiedName = recursion
                .qualifiedName()
                .parts
                .stream()
                .map(Token::getText)
                .collect(Collectors.joining("."));
        this.recursionClassName = ctx.resolve(
                qualifiedName,
                recursion.qualifiedName().start.getLine(),
                recursion.qualifiedName().start.getCharPositionInLine()
        );
        this.modified = true;
    }

    void setFetchType(DtoParser.FetchTypeContext fetchType) {
        if (!baseProp.isAssociation(true) || baseProp.isList()) {
            throw ctx.exception(
                    fetchType.start.getLine(),
                    fetchType.start.getCharPositionInLine(),
                    "Cannot be specify \"!fetchType\" when the property is not associated reference"
            );
        }
        this.fetchType = fetchType.fetchMode.getText();
        switch (this.fetchType) {
            case "SELECT":
            case "JOIN_IF_NO_CACHE":
            case "JOIN_ALWAYS":
                break;
            default:
                throw ctx.exception(
                        fetchType.fetchMode.getLine(),
                        fetchType.fetchMode.getCharPositionInLine(),
                        "The fetch mode can only be \"SELECT\", \"JOIN_IF_NO_CACHE\" or \"JOIN_ALWAYS\""
                );
        }
        this.modified = true;
    }

    void setLimit(DtoParser.LimitContext limit) {
        if (!baseProp.isAssociation(true) || !baseProp.isList()) {
            throw ctx.exception(
                    limit.start.getLine(),
                    limit.start.getCharPositionInLine(),
                    "Cannot be specify \"!limit\" when the property is not associated list"
            );
        }
        int limitValue = Integer.parseInt(limit.limitArg.getText());
        if (limitValue < 1) {
            throw ctx.exception(
                    limit.limitArg.getLine(),
                    limit.limitArg.getCharPositionInLine(),
                    "The limit cannot be less than 1"
            );
        }
        int offsetValue = 0;
        if (limit.offsetArg != null) {
            offsetValue = Integer.parseInt(limit.offsetArg.getText());
            if (offsetValue < 0) {
                throw ctx.exception(
                        limit.offsetArg.getLine(),
                        limit.offsetArg.getCharPositionInLine(),
                        "The offset cannot be less than 0"
                );
            }
        }

        this.limit = limitValue;
        this.offset = offsetValue;
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
                    "Cannot specify \"!depth\" when \"!recursion\" exists"
            );
        }
        if (!recursive) {
            throw ctx.exception(
                    depth.start.getLine(),
                    depth.start.getCharPositionInLine(),
                    "\"!depth\" can only be applied for recursive property"
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
                            "The infix operator must \"like\" or \"ilike\""
                    );
            }
        }
        List<PropConfig.PathNode<P>> path = createPropPath(predicate.propPath());
        PropConfig.PathNode<P> lastProp = path.get(path.size() - 1);
        SimplePropType simplePropType = ctx.getSimpleType(lastProp);
        if (simplePropType == SimplePropType.NONE) {
            List<Token> parts = predicate.propPath().parts;
            Token lastPart = parts.get(parts.size() - 1);
            throw ctx.exception(
                    lastPart.getLine(),
                    lastPart.getCharPositionInLine(),
                    "The \"!where\" in DTO must be simple predicate " +
                            "so that the last property \"" +
                            lastProp +
                            "\" must be boolean, number, string"
            );
        }
        String op = predicate.op.getText();
        if (simplePropType != SimplePropType.STRING && (op.equals("like") || op.equals("ilike"))) {
            throw ctx.exception(
                    predicate.op.getLine(),
                    predicate.op.getCharPositionInLine(),
                    "The operator \"" +
                            op +
                            "\" is not allowed here because the left operand is not string"
            );
        }
        Object value = createPropValue(predicate.right, simplePropType);
        return new CmpPredicate<>(path, predicate.op.getText(), value);
    }

    @SuppressWarnings("unchecked")
    private List<PropConfig.PathNode<P>> createPropPath(DtoParser.PropPathContext propPath) {
        T baseType = ctx.getTargetType(this.baseProp);
        int size = propPath.parts.size();
        List<PropConfig.PathNode<P>> pathNodes = new ArrayList<>(size + 1);
        for (int i = 0; i < size; i++) {
            Token part = propPath.parts.get(i);
            P prop = ctx.getProps(baseType).get(part.getText());
            if (prop == null) {
                if (part.getText().endsWith("Id")) {
                    String referenceName = part.getText().substring(0, part.getText().length() - 2);
                    P referenceProp = ctx.getProps(baseType).get(referenceName);
                    if (referenceProp != null && referenceProp.isReference() && referenceProp.isAssociation(true)) {
                        pathNodes.add(
                                new AssociatedIdPathNodeImpl<>(
                                        referenceProp,
                                        ctx.getIdProp(ctx.getTargetType(referenceProp)).getName()
                                )
                        );
                        T targetType = ctx.getTargetType(referenceProp);
                        prop = ctx.getIdProp(targetType);
                        baseType = ctx.getTargetType(prop);
                        continue;
                    }
                }
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
            if (prop.getIdViewBaseProp() != null) {
                P referenceProp = (P) prop.getIdViewBaseProp();
                if (!referenceProp.isList()) {
                    pathNodes.add(
                            new AssociatedIdPathNodeImpl<>(
                                    referenceProp,
                                    ctx.getIdProp(ctx.getTargetType(referenceProp)).getName()
                            )
                    );
                    T targetType = ctx.getTargetType(referenceProp);
                    P idProp = ctx.getIdProp(targetType);
                    baseType = ctx.getTargetType(idProp);
                    continue;
                }
            }
            if (prop.isAssociation(true)) {
                if (prop.isReference()) {
                    if (i + 1 < size) {
                        String idPropName = ctx.getIdProp(ctx.getTargetType(prop)).getName();
                        if (propPath.parts.get(i + 1).getText().equals(idPropName)) {
                            throw ctx.exception(
                                    part.getLine(),
                                    part.getCharPositionInLine(),
                                    "Please replace \"" +
                                            prop.getName() +
                                            "." +
                                            idPropName +
                                            "\" to \"" +
                                            prop.getName() +
                                            "Id\""
                            );
                        }
                    } else {
                        throw ctx.exception(
                                part.getLine(),
                                part.getCharPositionInLine(),
                                "Please replace \"" +
                                        prop.getName() +
                                        "\" to \"" +
                                        prop.getName() +
                                        "Id\""
                        );
                    }
                } else {
                    throw ctx.exception(
                            part.getLine(),
                            part.getCharPositionInLine(),
                            "There property \"" +
                                    prop +
                                    "\" cannot be supported because join is forbidden by fetcher field predicate"
                    );
                }
            } else if (i + 1 < size && !prop.isEmbedded()) {
                throw ctx.exception(
                        part.getLine(),
                        part.getCharPositionInLine(),
                        "There property \"" +
                                prop +
                                "\" is not last property but it is not embedded object"
                );
            }
            pathNodes.add(new SimplePathNodeImpl<>(prop));
            baseType = ctx.getTargetType(prop);
        }
        return pathNodes;
    }

    private Object createPropValue(DtoParser.PropValueContext value, SimplePropType simplePropType) {
        if (value.stringToken != null) {
            if (simplePropType != SimplePropType.STRING) {
                throw ctx.exception(
                        value.start.getLine(),
                        value.start.getCharPositionInLine(),
                        "Illegal string literal, the left operand is not string"
                );
            }
            String text = value.stringToken.getText();
            return text.substring(1, text.length() - 1);
        }
        if (value.booleanToken != null) {
            if (simplePropType != SimplePropType.BOOLEAN) {
                throw ctx.exception(
                        value.start.getLine(),
                        value.start.getCharPositionInLine(),
                        "Illegal string literal, the left operand is not boolean"
                );
            }
            return "true".equals(value.booleanToken.getText());
        }
        if (value.characterToken != null) {
            if (simplePropType != SimplePropType.STRING) {
                throw ctx.exception(
                        value.start.getLine(),
                        value.start.getCharPositionInLine(),
                        "Illegal char literal, the left operand is not string"
                );
            }
            String text = value.characterToken.getText();
            return text.substring(1, text.length() - 1);
        }
        if (value.integerToken != null) {
            switch (simplePropType) {
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                    long l = Long.parseLong(value.integerToken.getText());
                    if (value.negative != null) {
                        l = -l;
                    }
                    return l;
                case BIG_INTEGER:
                    BigInteger bi = new BigInteger(value.integerToken.getText());
                    if (value.negative != null) {
                        bi = bi.negate();
                    }
                    return bi;
                default:
                    if (simplePropType != SimplePropType.STRING) {
                        throw ctx.exception(
                                value.start.getLine(),
                                value.start.getCharPositionInLine(),
                                "Illegal integer literal, the left operand is not integer"
                        );
                    }
            }
        }
        switch (simplePropType) {
            case FLOAT:
            case DOUBLE:
            case BIG_DECIMAL:
                BigDecimal bc =  new BigDecimal(value.floatingPointToken.getText());
                if (value.negative != null) {
                    bc = bc.negate();
                }
                return bc;
            default:
                throw ctx.exception(
                        value.start.getLine(),
                        value.start.getCharPositionInLine(),
                        "Illegal float/decimal literal, the left operand is neither float nor decimal"
                );
        }
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

        final List<PropConfig.PathNode<P>> path;

        PathHolder(List<PropConfig.PathNode<P>> path) {
            this.path = path.size() == 1 ?
                    Collections.singletonList(path.get(0)) :
                    Collections.unmodifiableList(path);
        }

        public List<PropConfig.PathNode<P>> getPath() {
            return path;
        }

        String path() {
            StringBuilder builder = new StringBuilder();
            boolean addComma = false;
            for (PropConfig.PathNode<P> pathNode : path) {
                if (addComma) {
                    builder.append('.');
                } else {
                    addComma = true;
                }
                builder.append(pathNode.getProp().getName());
                if (pathNode.isAssociatedId()) {
                    builder.append("Id");
                }
            }
            return builder.toString();
        }
    }

    private static class OrderItemImpl<P extends BaseProp> extends PathHolder<P> implements PropConfig.OrderItem<P> {

        private final boolean desc;

        private OrderItemImpl(List<PropConfig.PathNode<P>> path, boolean desc) {
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

        NullityPredicate(List<PropConfig.PathNode<P>> path, boolean negative) {
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

        CmpPredicate(List<PropConfig.PathNode<P>> path, String operator, Object value) {
            super(path);
            this.operator = "!=".equals(operator) ? "<>" : operator;
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
            if (predicate != null) {
                builder.append("!where(").append(predicate).append(") ");
            }
            if (!orderItems.isEmpty()) {
                builder.append("!orderBy(");
                boolean addComma = false;
                for (OrderItem<P> item : orderItems) {
                    if (addComma) {
                        builder.append(", ");
                    } else {
                        addComma = true;
                    }
                    builder.append(item);
                }
                builder.append(") ");
            }
            if (filterClassName != null) {
                builder.append("!filter(").append(filterClassName).append(") ");
            }
            if (recursionClassName != null) {
                builder.append("!recursion(").append(recursionClassName).append(") ");
            }
            if (!"AUTO".equals(fetchType)) {
                builder.append("!fetchType(").append(fetchType).append(") ");
            }
            if (limit != Integer.MAX_VALUE) {
                if (offset != 0) {
                    builder.append("!limit(").append(limit).append(", ").append(offset).append(") ");
                } else {
                    builder.append("!limit(").append(limit).append(") ");
                }
            }
            if (batch != 0) {
                builder.append("!batch(").append(batch).append(") ");
            }
            if (depth != Integer.MAX_VALUE) {
                builder.append("!depth(").append(depth).append(") ");
            }
            return builder.toString();
        }
    }

    private static class SimplePathNodeImpl<P extends BaseProp> implements PropConfig.PathNode<P> {

        private final P prop;

        SimplePathNodeImpl(P prop) {
            this.prop = prop;
        }

        @Override
        public P getProp() {
            return prop;
        }

        @Override
        public boolean isAssociatedId() {
            return false;
        }

        @Override
        public String toString() {
            return prop.toString();
        }
    }

    private static class AssociatedIdPathNodeImpl<P extends BaseProp> implements PropConfig.PathNode<P> {

        private final P prop;

        private final String idPropName;

        AssociatedIdPathNodeImpl(P prop, String idPropName) {
            this.prop = prop;
            this.idPropName = idPropName;
        }

        @Override
        public P getProp() {
            return prop;
        }

        @Override
        public boolean isAssociatedId() {
            return true;
        }

        @Override
        public String toString() {
            return prop + "." + idPropName;
        }
    }
}
