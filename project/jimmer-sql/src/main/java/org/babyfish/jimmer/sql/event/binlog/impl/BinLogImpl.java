package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.babyfish.jimmer.jackson.codec.Node;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.meta.JoinTableFilterInfo;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BinLogImpl implements BinLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinLog.class);

    private static final Set<String> EXCLUDED_TABLE_NAMES =
            standardTableNames(
                    TransactionCacheOperator.TABLE_NAME
            );

    private final EntityManager entityManager;

    private final String microServiceName;

    private final MetadataStrategy strategy;

    private final BinLogParser parser;

    private final Triggers triggers;

    public BinLogImpl(
            EntityManager entityManager,
            String microServiceName,
            MetadataStrategy strategy,
            BinLogParser parser,
            Triggers triggers
    ) {
        this.entityManager = entityManager;
        this.microServiceName = microServiceName;
        this.strategy = strategy;
        this.parser = parser;
        this.triggers = triggers;
    }

    @Override
    public void accept(String tableName, Node oldData, Node newData) {
        accept(tableName, oldData, newData, null);
    }

    @Override
    public void accept(String tableName, Node oldData, Node newData, String reason) {
        boolean isOldNull = oldData == null || oldData.isNull();
        boolean isNewNull = newData == null || newData.isNull();
        if (isOldNull && isNewNull) {
            return;
        }
        Map<List<Object>, ImmutableType> typeMap = entityManager.getTypeMapByServiceAndTable(microServiceName, tableName, strategy);
        if (typeMap.isEmpty()) {
            if (!EXCLUDED_TABLE_NAMES.contains(DatabaseIdentifiers.comparableIdentifier(tableName))) {
                LOGGER.warn(
                        "Illegal table name \"{}\" of micro service \"{}\", it is not managed by current entity manager",
                        tableName,
                        microServiceName
                );
            }
            return;
        }
        for (ImmutableType type : typeMap.values()) {
            if (type instanceof AssociationType) {
                AssociationType associationType = (AssociationType) type;
                JoinTableFilterInfo filterInfo = associationType.getJoinTableFilterInfo();
                MiddleRow<?, ?> oldRow = isOldNull ?
                        null :
                        parser.parseMiddleRow(associationType.getBaseProp(), oldData);
                MiddleRow<?, ?> newRow = isNewNull ?
                        null :
                        MiddleRow.merge(oldRow, parser.parseMiddleRow(associationType.getBaseProp(), newData));
                if (oldRow != null && !Boolean.TRUE.equals(oldRow.deleted) &&
                        (filterInfo == null ||
                                oldRow.filteredValue == null ||
                                filterInfo.getValues().contains(oldRow.filteredValue))
                ) {
                    triggers.fireMiddleTableDelete(
                            associationType.getBaseProp(),
                            oldRow.sourceId,
                            oldRow.targetId,
                            null,
                            reason
                    );
                }
                if (newRow != null && !Boolean.TRUE.equals(newRow.deleted) &&
                        (filterInfo == null ||
                                newRow.filteredValue == null ||
                                filterInfo.getValues().contains(newRow.filteredValue))
                ) {
                    triggers.fireMiddleTableInsert(
                            associationType.getBaseProp(),
                            newRow.sourceId,
                            newRow.targetId,
                            null,
                            reason
                    );
                }
            } else {
                triggers.fireEntityTableChange(
                        parser.parseEntity(type, oldData),
                        parser.parseEntity(type, newData),
                        null,
                        reason
                );
            }
        }
    }

    public BinLogParser parser() {
        return parser;
    }

    private static Set<String> standardTableNames(String... tableNames) {
        Set<String> set = new HashSet<>();
        for (String tableName : tableNames) {
            set.add(DatabaseIdentifiers.comparableIdentifier(tableName));
        }
        return set;
    }

    private static JsonNode nodeOf(JsonNode jsonNode, String columnName) {
        JsonNode childNode = jsonNode.get(columnName);
        if (childNode != null) {
            return childNode;
        }
        String comparableIdentifier = DatabaseIdentifiers.comparableIdentifier(columnName);

        // Low version jackson does not support `properties`
        Iterator<Map.Entry<String, JsonNode>> fieldEntryItr = jsonNode.fields();

        while (fieldEntryItr.hasNext()) {
            Map.Entry<String, JsonNode> e = fieldEntryItr.next();
            if (DatabaseIdentifiers.comparableIdentifier(e.getKey()).equals(comparableIdentifier)) {
                return e.getValue();
            }
        }
        return null;
    }
}
