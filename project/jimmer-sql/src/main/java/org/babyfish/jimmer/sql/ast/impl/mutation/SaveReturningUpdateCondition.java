package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

interface SaveReturningUpdateCondition {

    void addSourceValues(List<SaveReturningColumnValue> sourceValues);

    default boolean isSourceValueReady(List<SaveReturningColumnValue> sourceValues) {
        return true;
    }

    void append(
            SqlBuilder builder,
            String targetPrefix,
            String targetSuffix,
            String sourcePrefix,
            String sourceSuffix
    );

    static SaveReturningUpdateCondition and(
            @Nullable SaveReturningUpdateCondition left,
            SaveReturningUpdateCondition right
    ) {
        if (left == null) {
            return right;
        }
        return new And(left, right);
    }

    static SaveReturningUpdateCondition discriminatorGuard(
            JSqlClientImplementor sqlClient,
            ImmutableType type,
            @Nullable Object value
    ) {
        ImmutableProp prop = type.getInheritanceInfo().getDiscriminatorProp(type);
        return new DiscriminatorGuard(
                PropertyGetter.propertyGetters(sqlClient, prop).get(0),
                value
        );
    }

    static SaveReturningUpdateCondition joinedChildGuard(
            JSqlClientImplementor sqlClient,
            ImmutableType rootType,
            ImmutableType childType,
            InheritanceInfo inheritanceInfo
    ) {
        return new JoinedChildGuard(
                rootType,
                Shape.fullOf(sqlClient, childType.getJavaClass()).getIdGetters().get(0),
                PropertyGetter
                        .propertyGetters(sqlClient, inheritanceInfo.getDiscriminatorProp(childType))
                        .get(0),
                inheritanceInfo
        );
    }

    static SaveReturningUpdateCondition logicalDeletedGuard(LogicalDeletedInfo logicalDeletedInfo) {
        return new LogicalDeletedGuard(logicalDeletedInfo);
    }

    static @Nullable SaveReturningUpdateCondition userOptimisticLock(
            JSqlClientImplementor sqlClient,
            Predicate predicate
    ) {
        List<PropertyGetter> newValueGetters = new ArrayList<>();
        boolean[] valid = {true};
        ((Ast) predicate).accept(new AstVisitor(new AstContext(sqlClient)) {

            @Override
            public void visitTableReference(
                    org.babyfish.jimmer.sql.ast.impl.table.RealTable table,
                    @Nullable ImmutableProp prop,
                    boolean rawId
            ) {
                if (prop == null || singleColumnGetter(sqlClient, prop) == null) {
                    valid[0] = false;
                }
            }

            @Override
            public void visitOptimisticLockNewValue(ImmutableProp prop) {
                PropertyGetter getter = singleColumnGetter(sqlClient, prop);
                if (getter == null) {
                    valid[0] = false;
                } else if (!containsProp(newValueGetters, getter.prop())) {
                    newValueGetters.add(getter);
                }
            }
        });
        if (!valid[0]) {
            return null;
        }
        return new UserOptimisticLock(predicate, newValueGetters);
    }

    static @Nullable PropertyGetter singleColumnGetter(
            JSqlClientImplementor sqlClient,
            ImmutableProp prop
    ) {
        List<PropertyGetter> getters = PropertyGetter.propertyGetters(sqlClient, prop);
        if (getters.size() != 1 || getters.get(0).metadata().getColumnName() == null) {
            return null;
        }
        return getters.get(0);
    }

    static boolean containsProp(List<PropertyGetter> getters, ImmutableProp prop) {
        ImmutableProp originalProp = prop.toOriginal();
        for (PropertyGetter getter : getters) {
            if (getter.prop().toOriginal() == originalProp) {
                return true;
            }
        }
        return false;
    }

    class And implements SaveReturningUpdateCondition {

        private final SaveReturningUpdateCondition left;

        private final SaveReturningUpdateCondition right;

        private And(SaveReturningUpdateCondition left, SaveReturningUpdateCondition right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public void addSourceValues(List<SaveReturningColumnValue> sourceValues) {
            left.addSourceValues(sourceValues);
            right.addSourceValues(sourceValues);
        }

        @Override
        public boolean isSourceValueReady(List<SaveReturningColumnValue> sourceValues) {
            return left.isSourceValueReady(sourceValues) &&
                    right.isSourceValueReady(sourceValues);
        }

        @Override
        public void append(
                SqlBuilder builder,
                String targetPrefix,
                String targetSuffix,
                String sourcePrefix,
                String sourceSuffix
        ) {
            left.append(builder, targetPrefix, targetSuffix, sourcePrefix, sourceSuffix);
            builder.sql(" and ");
            right.append(builder, targetPrefix, targetSuffix, sourcePrefix, sourceSuffix);
        }
    }

    class DiscriminatorGuard implements SaveReturningUpdateCondition {

        private final PropertyGetter getter;

        @Nullable
        private final Object value;

        private DiscriminatorGuard(PropertyGetter getter, @Nullable Object value) {
            this.getter = getter;
            this.value = value;
        }

        @Override
        public void addSourceValues(List<SaveReturningColumnValue> sourceValues) {
            if (value == null) {
                SaveReturningColumnValue.addIfAbsent(sourceValues, getter, SaveReturningValueMode.VALUE);
            }
        }

        @Override
        public void append(
                SqlBuilder builder,
                String targetPrefix,
                String targetSuffix,
                String sourcePrefix,
                String sourceSuffix
        ) {
            builder
                    .sql(targetPrefix)
                    .sql(getter)
                    .sql(targetSuffix)
                    .sql(" = ");
            if (value != null) {
                builder.variable(value);
            } else {
                builder.sql(sourcePrefix).sql(getter).sql(sourceSuffix);
            }
        }
    }

    class JoinedChildGuard implements SaveReturningUpdateCondition {

        private final ImmutableType rootType;

        private final PropertyGetter idGetter;

        private final PropertyGetter discriminatorGetter;

        private final InheritanceInfo inheritanceInfo;

        private JoinedChildGuard(
                ImmutableType rootType,
                PropertyGetter idGetter,
                PropertyGetter discriminatorGetter,
                InheritanceInfo inheritanceInfo
        ) {
            this.rootType = rootType;
            this.idGetter = idGetter;
            this.discriminatorGetter = discriminatorGetter;
            this.inheritanceInfo = inheritanceInfo;
        }

        @Override
        public void addSourceValues(List<SaveReturningColumnValue> sourceValues) {
            SaveReturningColumnValue.addIfAbsent(sourceValues, discriminatorGetter, SaveReturningValueMode.VALUE);
        }

        @Override
        public void append(
                SqlBuilder builder,
                String targetPrefix,
                String targetSuffix,
                String sourcePrefix,
                String sourceSuffix
        ) {
            InheritanceMutationUtils.renderJoinedChildRootGuard(
                    builder,
                    rootType,
                    inheritanceInfo,
                    sourcePrefix,
                    idGetter,
                    discriminatorGetter
            );
        }
    }

    class LogicalDeletedGuard implements SaveReturningUpdateCondition {

        private final LogicalDeletedInfo logicalDeletedInfo;

        private LogicalDeletedGuard(LogicalDeletedInfo logicalDeletedInfo) {
            this.logicalDeletedInfo = logicalDeletedInfo;
        }

        @Override
        public void addSourceValues(List<SaveReturningColumnValue> sourceValues) {
        }

        @Override
        public void append(
                SqlBuilder builder,
                String targetPrefix,
                String targetSuffix,
                String sourcePrefix,
                String sourceSuffix
        ) {
            builder.logicalDeleteFilter(logicalDeletedInfo, alias(targetPrefix));
        }

        private static @Nullable String alias(String prefix) {
            if (prefix.isEmpty()) {
                return null;
            }
            return prefix.charAt(prefix.length() - 1) == '.' ?
                    prefix.substring(0, prefix.length() - 1) :
                    prefix;
        }
    }

    class UserOptimisticLock implements SaveReturningUpdateCondition {

        private final Predicate predicate;

        private final List<PropertyGetter> newValueGetters;

        private UserOptimisticLock(Predicate predicate, List<PropertyGetter> newValueGetters) {
            this.predicate = predicate;
            this.newValueGetters = newValueGetters;
        }

        @Override
        public void addSourceValues(List<SaveReturningColumnValue> sourceValues) {
            for (PropertyGetter getter : newValueGetters) {
                SaveReturningColumnValue.addIfAbsent(sourceValues, getter, SaveReturningValueMode.VALUE);
            }
        }

        @Override
        public boolean isSourceValueReady(List<SaveReturningColumnValue> sourceValues) {
            for (PropertyGetter getter : newValueGetters) {
                if (!SaveReturningColumnValue.contains(sourceValues, getter)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void append(
                SqlBuilder builder,
                String targetPrefix,
                String targetSuffix,
                String sourcePrefix,
                String sourceSuffix
        ) {
            SqlBuilder tempBuilder = builder.createTempBuilder();
            tempBuilder.pushValueGetterRender(targetPrefix, targetSuffix);
            tempBuilder.pushOptimisticLockNewValueRender(sourcePrefix, sourceSuffix);
            try {
                AbstractExpression.renderChild(
                        (Ast) predicate,
                        ExpressionPrecedences.AND,
                        tempBuilder
                );
            } finally {
                tempBuilder.popOptimisticLockNewValueRender();
                tempBuilder.popValueGetterRender();
            }
            builder.appendTempBuilder(tempBuilder);
        }
    }
}
