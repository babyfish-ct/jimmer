package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class PolymorphicEntityReadPlan {

    private static final String ROOT_ALIAS = "tb_1_";

    private final JSqlClientImplementor sqlClient;

    private final ImmutableType rootType;

    private final InheritanceInfo inheritanceInfo;

    private final ImmutableProp idProp;

    private final ImmutableProp discriminatorProp;

    private final Slot idSlot;

    private final List<Slot> rootSlots;

    private final List<Segment> segments;

    private final Map<Object, ImmutableType> discriminatorTypeMap;

    private final ValueGetter discriminatorValueGetter;

    private PolymorphicEntityReadPlan(JSqlClientImplementor sqlClient, ImmutableType rootType) {
        InheritanceInfo inheritanceInfo = rootType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getRootType() != rootType) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            rootType +
                            "\" is not an inheritance root type"
            );
        }
        this.sqlClient = sqlClient;
        this.rootType = rootType;
        this.inheritanceInfo = inheritanceInfo;
        this.idProp = rootType.getIdProp();
        this.discriminatorProp = inheritanceInfo.getDiscriminatorProp();
        this.discriminatorTypeMap = inheritanceInfo.getDiscriminatorTypeMap();
        this.discriminatorValueGetter = ValueGetter.valueGetters(sqlClient, discriminatorProp).get(0);
        this.idSlot = new Slot(sqlClient, idProp, ROOT_ALIAS);
        this.rootSlots = rootSlots();
        this.segments = segments();
    }

    public static List<ImmutableSpi> readByIds(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableType rootType,
            QueryReason reason,
            Collection<?> ids,
            ExceptionTranslator<Exception> exceptionTranslator
    ) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return new PolymorphicEntityReadPlan(sqlClient, rootType).readByIds(
                con,
                reason,
                ids,
                exceptionTranslator
        );
    }

    private List<ImmutableSpi> readByIds(
            Connection con,
            QueryReason reason,
            Collection<?> ids,
            ExceptionTranslator<Exception> exceptionTranslator
    ) {
        Tuple3<String, List<Object>, List<Integer>> sql = buildSql(ids);
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        con,
                        sql.get_1(),
                        sql.get_2(),
                        sql.get_3(),
                        ExecutionPurpose.command(reason),
                        exceptionTranslator,
                        null,
                        (stmt, args) -> {
                            List<ImmutableSpi> rows = new ArrayList<>();
                            Reader.Context readerContext = new Reader.Context(null, sqlClient);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    readerContext.resetCol();
                                    ImmutableSpi row = read(rs, readerContext);
                                    if (row != null) {
                                        rows.add(row);
                                    }
                                }
                            }
                            return rows;
                        }
                )
        );
    }

    private Tuple3<String, List<Object>, List<Integer>> buildSql(Collection<?> ids) {
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.sql("select ");
        builder.enter(AbstractSqlBuilder.ScopeType.COMMA);
        renderSlot(builder, idSlot);
        for (Slot slot : rootSlots) {
            builder.separator();
            renderSlot(builder, slot);
        }
        for (Segment segment : segments) {
            for (Slot slot : segment.slots) {
                builder.separator();
                renderSlot(builder, slot);
            }
        }
        builder.leave();
        builder
                .sql(" from ")
                .sql(rootType.getTableName(strategy))
                .sql(" ")
                .sql(ROOT_ALIAS);
        if (inheritanceInfo.getStrategy() == InheritanceType.JOINED) {
            for (Segment segment : segments) {
                renderJoinedTable(builder, segment);
            }
        }
        builder.sql(" where ");
        renderIdIn(builder, ids);
        builder.sql(" order by ");
        builder.definition(ROOT_ALIAS, idProp.getStorage(strategy));
        return builder.build();
    }

    private void renderSlot(SqlBuilder builder, Slot slot) {
        SqlTemplate template = slot.prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            builder.sql(((FormulaTemplate) template).toSql(slot.alias));
        } else {
            builder.definition(slot.alias, slot.prop.getStorage(sqlClient.getMetadataStrategy()));
        }
    }

    private void renderJoinedTable(SqlBuilder builder, Segment segment) {
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        builder
                .sql(" left join ")
                .sql(segment.tableType.getTableName(strategy))
                .sql(" ")
                .sql(segment.alias)
                .sql(" on ");
        renderIdEquality(builder, segment);
        builder.sql(" and ");
        renderDiscriminatorCondition(builder, segment);
    }

    private void renderIdEquality(SqlBuilder builder, Segment segment) {
        ColumnDefinition rootIdDefinition = idProp.getStorage(sqlClient.getMetadataStrategy());
        ColumnDefinition childIdDefinition = segment.tableType.getIdProp().getStorage(sqlClient.getMetadataStrategy());
        builder.enter(AbstractSqlBuilder.ScopeType.AND);
        int size = rootIdDefinition.size();
        for (int i = 0; i < size; i++) {
            builder.separator();
            builder
                    .sql(segment.alias)
                    .sql(".")
                    .sql(childIdDefinition.name(i))
                    .sql(" = ")
                    .sql(ROOT_ALIAS)
                    .sql(".")
                    .sql(rootIdDefinition.name(i));
        }
        builder.leave();
    }

    private void renderDiscriminatorCondition(SqlBuilder builder, Segment segment) {
        ColumnDefinition definition = discriminatorProp.getStorage(sqlClient.getMetadataStrategy());
        builder.definition(ROOT_ALIAS, definition);
        if (segment.discriminatorValues.size() == 1) {
            builder.sql(" = ");
            builder.variable(segment.discriminatorValues.get(0));
        } else {
            builder.sql(" in ");
            builder.enter(AbstractSqlBuilder.ScopeType.LIST);
            for (Object value : segment.discriminatorValues) {
                builder.separator();
                builder.variable(value);
            }
            builder.leave();
        }
    }

    private void renderIdIn(SqlBuilder builder, Collection<?> ids) {
        List<ValueGetter> idGetters = ValueGetter.valueGetters(sqlClient, idProp);
        if (idGetters.size() == 1) {
            renderAliasedGetter(builder, idGetters.get(0));
            if (ids.size() == 1) {
                builder.sql(" = ");
                builder.variable(idGetters.get(0).get(ids.iterator().next()));
            } else {
                builder.sql(" in ");
                builder.enter(AbstractSqlBuilder.ScopeType.LIST);
                for (Object id : ids) {
                    builder.separator();
                    builder.variable(idGetters.get(0).get(id));
                }
                builder.leave();
            }
            return;
        }
        if (sqlClient.getDialect().isTupleSupported()) {
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : idGetters) {
                builder.separator();
                renderAliasedGetter(builder, getter);
            }
            builder.leave();
            builder.sql(" in ");
            builder.enter(AbstractSqlBuilder.ScopeType.LIST);
            for (Object id : ids) {
                builder.separator();
                builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
                for (ValueGetter getter : idGetters) {
                    builder.separator();
                    builder.variable(getter.get(id));
                }
                builder.leave();
            }
            builder.leave();
            return;
        }
        builder.enter(AbstractSqlBuilder.ScopeType.OR);
        for (Object id : ids) {
            builder.separator();
            builder.enter(AbstractSqlBuilder.ScopeType.AND);
            for (ValueGetter getter : idGetters) {
                builder.separator();
                renderAliasedGetter(builder, getter);
                builder.sql(" = ");
                builder.variable(getter.get(id));
            }
            builder.leave();
        }
        builder.leave();
    }

    private void renderAliasedGetter(SqlBuilder builder, ValueGetter getter) {
        builder
                .sql(ROOT_ALIAS)
                .sql(".")
                .sql(getter.metadata().getColumnName());
    }

    private ImmutableSpi read(ResultSet rs, Reader.Context ctx) throws SQLException {
        Object id = idSlot.reader.read(rs, ctx);
        if (id == null) {
            skip(ctx);
            return null;
        }
        Map<ImmutableProp, Object> values = new LinkedHashMap<>();
        Object discriminator = null;
        for (Slot slot : rootSlots) {
            Object value = slot.reader.read(rs, ctx);
            values.put(slot.prop, value);
            if (slot.prop.toOriginal() == discriminatorProp.toOriginal()) {
                discriminator = value;
            }
        }
        ImmutableType actualType = discriminatorTypeMap.get(discriminator);
        if (actualType == null) {
            throw new ExecutionException(
                    "Cannot resolve the concrete type of \"" +
                            rootType +
                            "\" because there is no type mapped by discriminator value \"" +
                            discriminator +
                            "\""
            );
        }
        for (Segment segment : segments) {
            boolean used = segment.tableType.isAssignableFrom(actualType);
            for (Slot slot : segment.slots) {
                Object value = slot.reader.read(rs, ctx);
                if (used) {
                    values.put(slot.prop, value);
                }
            }
        }
        DraftSpi spi = (DraftSpi) actualType.getDraftFactory().apply(ctx.draftContext(), null);
        spi.__set(actualType.getIdProp().getId(), id);
        try {
            for (Map.Entry<ImmutableProp, Object> e : values.entrySet()) {
                spi.__set(e.getKey().getId(), e.getValue());
            }
        } catch (Throwable ex) {
            throw DraftConsumerUncheckedException.rethrow(ex);
        }
        return (ImmutableSpi) ctx.resolve(spi);
    }

    private void skip(Reader.Context ctx) {
        for (Slot slot : rootSlots) {
            slot.reader.skip(ctx);
        }
        for (Segment segment : segments) {
            for (Slot slot : segment.slots) {
                slot.reader.skip(ctx);
            }
        }
    }

    private List<Slot> rootSlots() {
        Map<ImmutableProp, Slot> slotMap = new LinkedHashMap<>();
        boolean hasDiscriminator = false;
        for (ImmutableProp prop : rootType.getSelectableProps().values()) {
            if (prop.isId()) {
                continue;
            }
            ImmutableProp readableProp = readableProp(prop);
            Slot slot = slot(readableProp, ROOT_ALIAS);
            if (slot != null) {
                slotMap.put(readableProp, slot);
                if (readableProp.toOriginal() == discriminatorProp.toOriginal()) {
                    hasDiscriminator = true;
                }
            }
        }
        if (!hasDiscriminator) {
            Slot slot = slot(discriminatorProp, ROOT_ALIAS);
            if (slot != null) {
                slotMap.put(discriminatorProp, slot);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(slotMap.values()));
    }

    private List<Segment> segments() {
        List<ImmutableType> tableTypes = nonRootTableTypes();
        if (tableTypes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Segment> segments = new ArrayList<>(tableTypes.size());
        int index = 1;
        for (ImmutableType tableType : tableTypes) {
            ImmutableType parentType = parentTableType(tableType);
            String alias = inheritanceInfo.getStrategy() == InheritanceType.SINGLE_TABLE ?
                    ROOT_ALIAS :
                    "tb_1__sub_" + index + "_";
            List<Slot> slots = slots(tableType, parentType, alias);
            if (!slots.isEmpty()) {
                segments.add(
                        new Segment(
                                tableType,
                                alias,
                                slots,
                                discriminatorValues(tableType)
                        )
                );
                if (inheritanceInfo.getStrategy() == InheritanceType.JOINED) {
                    index++;
                }
            }
        }
        return Collections.unmodifiableList(segments);
    }

    private List<ImmutableType> nonRootTableTypes() {
        Set<ImmutableType> tableTypes = new LinkedHashSet<>();
        for (ImmutableType concreteType : concreteTypes()) {
            for (ImmutableType type : pathFromRoot(concreteType)) {
                if (type != rootType) {
                    tableTypes.add(type);
                }
            }
        }
        List<ImmutableType> list = new ArrayList<>(tableTypes);
        list.sort(Comparator
                .comparingInt((ImmutableType it) -> pathFromRoot(it).size())
                .thenComparing(it -> it.getJavaClass().getName()));
        return list;
    }

    private List<Slot> slots(ImmutableType tableType, ImmutableType parentType, String alias) {
        Map<ImmutableProp, Slot> slotMap = new LinkedHashMap<>();
        for (ImmutableProp prop : tableType.getSelectableProps().values()) {
            if (prop.isId()) {
                continue;
            }
            ImmutableProp readableProp = readableProp(prop);
            ImmutableType declaringType = readableProp.toOriginal().getDeclaringType();
            if (declaringType.isAssignableFrom(tableType) &&
                    !declaringType.isAssignableFrom(parentType)) {
                Slot slot = slot(readableProp, alias);
                if (slot != null) {
                    slotMap.put(readableProp, slot);
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(slotMap.values()));
    }

    private static ImmutableProp readableProp(ImmutableProp prop) {
        ImmutableProp idViewProp = prop.getIdViewProp();
        return idViewProp != null ? idViewProp : prop;
    }

    private Slot slot(ImmutableProp prop, String alias) {
        Reader<?> reader = sqlClient.getReader(prop);
        if (reader == null) {
            return null;
        }
        if (!prop.isColumnDefinition() && !(prop.getSqlTemplate() instanceof FormulaTemplate)) {
            return null;
        }
        return new Slot(sqlClient, prop, alias);
    }

    private ImmutableType parentTableType(ImmutableType tableType) {
        ImmutableType parent = tableType.getPrimarySuperType();
        while (parent != null && parent.isMappedSuperclass()) {
            parent = parent.getPrimarySuperType();
        }
        return parent != null ? parent : rootType;
    }

    private List<ImmutableType> concreteTypes() {
        List<ImmutableType> types = new ArrayList<>(discriminatorTypeMap.values());
        types.sort(Comparator.comparing(it -> it.getJavaClass().getName()));
        return types;
    }

    private List<ImmutableType> pathFromRoot(ImmutableType type) {
        List<ImmutableType> path = new ArrayList<>();
        for (ImmutableType t = type; t != null && t != rootType; t = t.getPrimarySuperType()) {
            if (t.isEntity()) {
                path.add(t);
            }
        }
        Collections.reverse(path);
        return path;
    }

    private List<Object> discriminatorValues(ImmutableType tableType) {
        List<Object> values = new ArrayList<>();
        for (ImmutableType concreteType : concreteTypes()) {
            if (!tableType.isAssignableFrom(concreteType)) {
                continue;
            }
            String discriminatorValue = concreteType.getDiscriminatorValue();
            if (discriminatorValue != null) {
                values.add(discriminatorValueGetter.get(inheritanceInfo.discriminatorValue(discriminatorValue)));
            }
        }
        return Collections.unmodifiableList(values);
    }

    private static class Slot {

        final ImmutableProp prop;

        final String alias;

        final Reader<?> reader;

        Slot(JSqlClientImplementor sqlClient, ImmutableProp prop, String alias) {
            this.prop = prop;
            this.alias = alias;
            this.reader = sqlClient.getReader(prop);
        }
    }

    private static class Segment {

        final ImmutableType tableType;

        final String alias;

        final List<Slot> slots;

        final List<Object> discriminatorValues;

        Segment(
                ImmutableType tableType,
                String alias,
                List<Slot> slots,
                List<Object> discriminatorValues
        ) {
            this.tableType = tableType;
            this.alias = alias;
            this.slots = slots;
            this.discriminatorValues = discriminatorValues;
        }
    }
}
