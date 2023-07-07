package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.event.binlog.BinLog;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

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

    public void accept(String tableName, JsonNode oldData, JsonNode newData) {
        accept(tableName, oldData, newData, null);
    }

    public void accept(String tableName, JsonNode oldData, JsonNode newData, String reason) {
        boolean isOldNull = oldData == null || oldData.isNull();
        boolean isNewNull = newData == null || newData.isNull();
        if (isOldNull && isNewNull) {
            return;
        }
        ImmutableType type = entityManager.getTypeByServiceAndTable(microServiceName, tableName, strategy);
        if (type == null) {
            if (!EXCLUDED_TABLE_NAMES.contains(DatabaseIdentifiers.comparableIdentifier(tableName))) {
                LOGGER.warn(
                        "Illegal table name \"{}\" of micro service \"{}\", it is not managed by current entity manager",
                        tableName,
                        microServiceName
                );
            }
            return;
        }
        if (type instanceof AssociationType) {
            if (isOldNull) {
                AssociationType associationType = (AssociationType) type;
                Tuple2<?, ?> idPair = parser.parseIdPair(associationType, newData);
                triggers.fireMiddleTableInsert(
                        associationType.getBaseProp(),
                        idPair.get_1(),
                        idPair.get_2(),
                        null,
                        reason
                );
            } else {
                AssociationType associationType = (AssociationType) type;
                Tuple2<?, ?> idPair = parser.parseIdPair(associationType, oldData);
                triggers.fireMiddleTableDelete(
                        associationType.getBaseProp(),
                        idPair.get_1(),
                        idPair.get_2(),
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

    public BinLogParser parser() {
        return parser;
    }

    private static Set<String> standardTableNames(String ... tableNames) {
        Set<String> set = new HashSet<>();
        for (String tableName: tableNames) {
            set.add(DatabaseIdentifiers.comparableIdentifier(tableName));
        }
        return set;
    }
}
