package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class InheritanceMutationUtils {

    private static final String ROOT_GUARD_ALIAS = "tb_root_";

    private InheritanceMutationUtils() {}

    static Collection<ImmutableType> deletedTypes(
            @Nullable InheritanceInfo inheritanceInfo,
            ImmutableType type,
            TypeMatchMode typeMatchMode
    ) {
        if (inheritanceInfo == null) {
            return Collections.singleton(type);
        }
        TypeMatchMode resolvedMode = TypeMatchModes.resolve(type, typeMatchMode);
        if (resolvedMode == TypeMatchMode.EXACT) {
            if (!type.isInstantiable()) {
                throw new ExecutionException(
                        "Cannot delete inheritance entity type \"" +
                                type +
                                "\" exactly because it is abstract. Delete an instantiable type or use " +
                                TypeMatchMode.POLYMORPHIC +
                                " type match mode."
                );
            }
            return Collections.singleton(type);
        }
        Collection<ImmutableType> types = inheritanceInfo.getConcreteTypes(type);
        if (types.isEmpty()) {
            throw new ExecutionException(
                    "Cannot delete inheritance entity type \"" +
                            type +
                            "\" polymorphically because it has no instantiable type"
            );
        }
        return types;
    }

    static List<ImmutableType> joinedTableTypes(ImmutableType rootType, ImmutableType type) {
        List<ImmutableType> tableTypes = new ArrayList<>();
        for (ImmutableType t = type; t != rootType; t = t.getPrimarySuperType()) {
            if (t.isEntity()) {
                tableTypes.add(t);
            }
        }
        return tableTypes;
    }

    static Comparator<ImmutableType> joinedCleanupTableTypeComparator(MetadataStrategy strategy) {
        return (a, b) -> {
            int cmp = Integer.compare(b.getAllTypes().size(), a.getAllTypes().size());
            if (cmp != 0) {
                return cmp;
            }
            cmp = a.getTableName(strategy).compareTo(b.getTableName(strategy));
            if (cmp != 0) {
                return cmp;
            }
            return a.getJavaClass().getName().compareTo(b.getJavaClass().getName());
        };
    }

    static List<Object> discriminatorValues(
            InheritanceInfo inheritanceInfo,
            Collection<ImmutableType> types
    ) {
        List<Object> values = new ArrayList<>();
        for (ImmutableType type : types) {
            String value = type.getDiscriminatorValue();
            if (value != null) {
                values.add(inheritanceInfo.discriminatorValue(value));
            }
        }
        return values;
    }

    static void renderDiscriminatorPredicate(
            SqlBuilder builder,
            ImmutableType tableType,
            Collection<ImmutableType> deletedTypes
    ) {
        InheritanceInfo inheritanceInfo = tableType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getRootType() != tableType) {
            return;
        }
        List<Object> values = discriminatorValues(inheritanceInfo, deletedTypes);
        if (values.isEmpty()) {
            return;
        }
        ImmutableProp discriminatorProp = inheritanceInfo.getDiscriminatorProp();
        builder.sql(" and ")
                .sql(discriminatorProp.<SingleColumn>getStorage(builder.getAstContext().getSqlClient().getMetadataStrategy()).getName());
        if (values.size() == 1) {
            builder.sql(" = ")
                    .variable(Variables.process(values.get(0), discriminatorProp, builder.getAstContext().getSqlClient()));
        } else {
            builder.sql(" in ");
            builder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : values) {
                builder.separator()
                        .variable(Variables.process(value, discriminatorProp, builder.getAstContext().getSqlClient()));
            }
            builder.leave();
        }
    }

    static void renderJoinedChildRootGuard(
            SqlBuilder builder,
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            String sourcePrefix,
            PropertyGetter idGetter,
            PropertyGetter discriminatorGetter
    ) {
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
        renderJoinedChildRootGuardPrefix(builder, rootType, strategy);
        builder
                .sql(sourcePrefix)
                .sql(idGetter)
                .sql(" and ")
                .sql(ROOT_GUARD_ALIAS)
                .sql(".")
                .sql(discriminatorColumnName(inheritanceInfo, strategy))
                .sql(" = ")
                .sql(sourcePrefix)
                .sql(discriminatorGetter)
                .sql(")");
    }

    static void renderJoinedChildRootGuard(
            BatchSqlBuilder builder,
            JSqlClientImplementor sqlClient,
            ImmutableType rootType,
            InheritanceInfo inheritanceInfo,
            List<PropertyGetter> idGetters,
            PropertyGetter discriminatorGetter
    ) {
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        renderJoinedChildRootGuardPrefix(builder, rootType, strategy);
        for (PropertyGetter idGetter : idGetters) {
            builder.variable(idGetter);
        }
        builder
                .sql(" and ")
                .sql(ROOT_GUARD_ALIAS)
                .sql(".")
                .sql(discriminatorColumnName(inheritanceInfo, strategy))
                .sql(" = ")
                .variable(discriminatorGetter)
                .sql(")");
    }

    private static void renderJoinedChildRootGuardPrefix(
            SqlBuilder builder,
            ImmutableType rootType,
            MetadataStrategy strategy
    ) {
        builder
                .sql("exists(select 1 from ")
                .sql(rootType.getTableName(strategy))
                .sql(" ")
                .sql(ROOT_GUARD_ALIAS)
                .sql(" where ")
                .sql(ROOT_GUARD_ALIAS)
                .sql(".")
                .sql(rootIdColumnName(rootType, strategy))
                .sql(" = ");
    }

    private static void renderJoinedChildRootGuardPrefix(
            BatchSqlBuilder builder,
            ImmutableType rootType,
            MetadataStrategy strategy
    ) {
        builder
                .sql("exists(select 1 from ")
                .sql(rootType.getTableName(strategy))
                .sql(" ")
                .sql(ROOT_GUARD_ALIAS)
                .sql(" where ")
                .sql(ROOT_GUARD_ALIAS)
                .sql(".")
                .sql(rootIdColumnName(rootType, strategy))
                .sql(" = ");
    }

    private static String rootIdColumnName(ImmutableType rootType, MetadataStrategy strategy) {
        return rootType.getIdProp().<SingleColumn>getStorage(strategy).getName();
    }

    private static String discriminatorColumnName(InheritanceInfo inheritanceInfo, MetadataStrategy strategy) {
        return inheritanceInfo
                .getDiscriminatorProp()
                .<SingleColumn>getStorage(strategy)
                .getName();
    }
}
