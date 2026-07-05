package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

interface SaveReturningUpdateCondition {

    void addSourceValues(List<SaveReturningColumnValue> sourceValues);

    void append(SqlBuilder builder, String targetPrefix, String sourcePrefix);

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
                sqlClient,
                rootType,
                Shape.fullOf(sqlClient, childType.getJavaClass()).getIdGetters().get(0),
                PropertyGetter
                        .propertyGetters(sqlClient, inheritanceInfo.getDiscriminatorProp(childType))
                        .get(0),
                inheritanceInfo
        );
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
        public void append(SqlBuilder builder, String targetPrefix, String sourcePrefix) {
            builder
                    .sql(targetPrefix)
                    .sql(getter)
                    .sql(" = ");
            if (value != null) {
                builder.variable(value);
            } else {
                builder.sql(sourcePrefix).sql(getter);
            }
        }
    }

    class JoinedChildGuard implements SaveReturningUpdateCondition {

        private static final String ROOT_ALIAS = "tb_root_";

        private final JSqlClientImplementor sqlClient;

        private final ImmutableType rootType;

        private final PropertyGetter idGetter;

        private final PropertyGetter discriminatorGetter;

        private final InheritanceInfo inheritanceInfo;

        private JoinedChildGuard(
                JSqlClientImplementor sqlClient,
                ImmutableType rootType,
                PropertyGetter idGetter,
                PropertyGetter discriminatorGetter,
                InheritanceInfo inheritanceInfo
        ) {
            this.sqlClient = sqlClient;
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
        public void append(SqlBuilder builder, String targetPrefix, String sourcePrefix) {
            MetadataStrategy strategy = sqlClient.getMetadataStrategy();
            String rootTableName = rootType.getTableName(strategy);
            String rootIdColumnName = rootType.getIdProp().<SingleColumn>getStorage(strategy).getName();
            String discriminatorColumnName = inheritanceInfo
                    .getDiscriminatorProp()
                    .<SingleColumn>getStorage(strategy)
                    .getName();
            builder
                    .sql("exists(select 1 from ")
                    .sql(rootTableName)
                    .sql(" ")
                    .sql(ROOT_ALIAS)
                    .sql(" where ")
                    .sql(ROOT_ALIAS)
                    .sql(".")
                    .sql(rootIdColumnName)
                    .sql(" = ")
                    .sql(sourcePrefix)
                    .sql(idGetter)
                    .sql(" and ")
                    .sql(ROOT_ALIAS)
                    .sql(".")
                    .sql(discriminatorColumnName)
                    .sql(" = ")
                    .sql(sourcePrefix)
                    .sql(discriminatorGetter)
                    .sql(")");
        }
    }
}
