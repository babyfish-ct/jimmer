package org.babyfish.jimmer.sql.ddl;

import org.apache.commons.lang3.StringUtils;
import org.babyfish.jimmer.sql.ddl.annotations.OnDeleteAction;
import org.babyfish.jimmer.sql.ddl.dialect.DDLDialect;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.List;

/**
 * @author honhimW
 */

public class StandardForeignKeyExporter implements Exporter<ForeignKey> {

    protected final JSqlClientImplementor client;

    protected final DDLDialect dialect;

    public StandardForeignKeyExporter(JSqlClientImplementor client) {
        this.client = client;
        DatabaseVersion databaseVersion = client.getConnectionManager().execute(connection -> {
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                int databaseMajorVersion = metaData.getDatabaseMajorVersion();
                int databaseMinorVersion = metaData.getDatabaseMinorVersion();
                String databaseProductVersion = metaData.getDatabaseProductVersion();
                return new DatabaseVersion(databaseMajorVersion, databaseMinorVersion, databaseProductVersion);
            } catch (Exception e) {
                // cannot get database version, using latest as default
                return DatabaseVersion.LATEST;
            }
        });
        this.dialect = DDLDialect.of(client.getDialect(), databaseVersion);
    }

    public StandardForeignKeyExporter(JSqlClientImplementor client, DatabaseVersion version) {
        this.client = client;
        this.dialect = DDLDialect.of(client.getDialect(), version);
    }

    @Override
    public List<String> getSqlCreateStrings(ForeignKey exportable) {
        if (!dialect.hasAlterTable()) {
            return Collections.emptyList();
        }
        String sourceTableName = exportable.table.getTableName(client.getMetadataStrategy());
        String targetTableName = exportable.referencedTable.getTableName(client.getMetadataStrategy());

        StringBuilder buf = new StringBuilder();
        buf.append("alter table ");
        if (dialect.supportsIfExistsAfterAlterTable()) {
            buf.append("if exists ");
        }
        buf.append(sourceTableName);

        String joinColumnName = DDLUtils.getName(exportable.joinColumn, client.getMetadataStrategy());
        String foreignKeyName = getForeignKeyName(exportable);
        if (StringUtils.isNotBlank(exportable.foreignKey.definition())) {
            buf.append(" add constraint ")
                .append(dialect.quote(foreignKeyName))
                .append(' ')
                .append(exportable.foreignKey.definition());
        } else {
            buf.append(" add constraint ")
                .append(dialect.quote(foreignKeyName))
                .append(" foreign key (")
                .append(joinColumnName)
                .append(')')
                .append(" references ")
                .append(targetTableName)
                .append(" (")
                .append(DDLUtils.getName(exportable.referencedTable.getIdProp(), client.getMetadataStrategy()))
                .append(')');
        }
        OnDeleteAction action = exportable.foreignKey.action();
        if (action != OnDeleteAction.NONE) {
            buf.append(" on delete ").append(action.sql);
        }
        return Collections.singletonList(buf.toString());
    }

    @Override
    public List<String> getSqlDropStrings(ForeignKey exportable) {
        if (!dialect.hasAlterTable()) {
            return Collections.emptyList();
        }
        StringBuilder buf = new StringBuilder();
        buf.append("alter table ");
        if (dialect.supportsIfExistsAfterAlterTable()) {
            buf.append("if exists ");
        }
        buf
            .append(exportable.table.getTableName(client.getMetadataStrategy()))
            .append(' ')
            .append(dialect.getDropForeignKeyString())
            .append(' ');
        if (dialect.supportsIfExistsBeforeConstraintName()) {
            buf.append("if exists ");
        }
        buf.append(dialect.quote(getForeignKeyName(exportable)));
        return Collections.singletonList(buf.toString());
    }

    protected String getForeignKeyName(ForeignKey exportable) {
        String sourceTableName = exportable.table.getTableName(client.getMetadataStrategy());
        String foreignKeyName = exportable.foreignKey.name();
        String joinColumnName = DDLUtils.getName(exportable.joinColumn, client.getMetadataStrategy());
        if (StringUtils.isBlank(foreignKeyName)) {
            try {
                ConstraintNamingStrategy ns = exportable.foreignKey.naming().getConstructor().newInstance();
                foreignKeyName = ns.determineForeignKeyName(sourceTableName, new String[]{joinColumnName});
            } catch (Exception e) {
                throw new IllegalArgumentException("NamingStrategy doesn't have a no-arg constructor");
            }
        }
        return foreignKeyName;
    }

}
