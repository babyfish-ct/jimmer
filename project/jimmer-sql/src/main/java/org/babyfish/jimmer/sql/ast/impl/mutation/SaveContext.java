package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class SaveContext extends MutationContext {

    final SaveOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final ImmutableProp backReferenceProp;

    final boolean backReferenceFrozen;

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type
    ) {
        this(
                options,
                con,
                type,
                options.getTriggers() != null ? new MutationTrigger() : null,
                new LinkedHashMap<>()
        );
    }

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type,
            MutationTrigger trigger,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        super(MutationPath.root(type));
        this.options = options;
        this.con = con;
        this.trigger = trigger;
        this.backReferenceProp = null;
        this.backReferenceFrozen = false;
        this.affectedRowCountMap = affectedRowCountMap;
    }

    private SaveContext(SaveContext parent, ImmutableProp prop, ImmutableProp backProp) {
        super(prop != null ? parent.path.to(prop) : parent.path.backFrom(backProp));
        if (prop == null) {
            prop = backProp.getOpposite();
        } else {
            backProp = prop.getOpposite();
        }
        SaveMode saveMode = SaveMode.UPSERT;
        if (prop != null) {
            switch (parent.options.getAssociatedMode(prop)) {
                case APPEND:
                    saveMode = SaveMode.INSERT_ONLY;
                    break;
                case APPEND_IF_ABSENT:
                    saveMode = SaveMode.INSERT_IF_ABSENT;
                    break;
                case UPDATE:
                    saveMode = SaveMode.UPDATE_ONLY;
                    break;
                case VIOLENTLY_REPLACE:
                    if (prop.isColumnDefinition()) {
                        saveMode = SaveMode.NON_IDEMPOTENT_UPSERT;
                    } else {
                        saveMode = SaveMode.INSERT_ONLY;
                    }
                    break;
            }
        }
        this.options = parent.options.withMode(saveMode);
        this.con = parent.con;
        this.trigger = parent.trigger;
        if (prop != null) {
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null && mappedBy.isReference(TargetLevel.ENTITY)) {
                this.backReferenceProp = mappedBy;
                this.backReferenceFrozen =
                        prop.getAssociationAnnotation().annotationType() == OneToMany.class &&
                                !parent.options.isTargetTransferable(prop);
            } else {
                this.backReferenceProp = null;
                this.backReferenceFrozen = false;
            }
        } else {
            this.backReferenceProp = backProp;
            this.backReferenceFrozen = false;
        }
        this.affectedRowCountMap = parent.affectedRowCountMap;
    }

    private SaveContext(SaveContext base, JSqlClientImplementor sqlClient) {
        super(base.path);
        this.options = base.options.withSqlClient(sqlClient);
        this.con = base.con;
        this.trigger = base.trigger;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.backReferenceProp = base.backReferenceProp;
        this.backReferenceFrozen = base.backReferenceFrozen;
    }

    public Object allocateId() {
        IdGenerator idGenerator = options.getSqlClient().getIdGenerator(path.getType().getJavaClass());
        if (idGenerator == null) {
            throw new SaveException.NoIdGenerator(
                    path,
                    "Cannot save \"" +
                            path.getType() + "\" " +
                            "without id because id generator is not specified"
            );
        }
        JSqlClientImplementor sqlClient = options.getSqlClient();
        if (idGenerator instanceof SequenceIdGenerator) {
            String sql = sqlClient.getDialect().getSelectIdFromSequenceSql(
                    ((SequenceIdGenerator)idGenerator).getSequenceName()
            );
            return sqlClient.getExecutor().execute(
                    new Executor.Args<>(
                            sqlClient,
                            con,
                            sql,
                            Collections.emptyList(),
                            sqlClient.getSqlFormatter().isPretty() ? Collections.emptyList() : null,
                            ExecutionPurpose.MUTATE,
                            options.getExceptionTranslator(),
                            null,
                            (stmt, args) -> {
                                try (ResultSet rs = stmt.executeQuery()) {
                                    rs.next();
                                    return rs.getObject(1);
                                }
                            }
                    )
            );
        }
        if (idGenerator instanceof UserIdGenerator<?>) {
            return ((UserIdGenerator<?>)idGenerator).generate(path.getType().getJavaClass());
        }
        if (idGenerator instanceof IdentityIdGenerator) {
            return null;
        }
        throw new SaveException.IllegalIdGenerator(
                path,
                "Illegal id generator type: \"" +
                        idGenerator.getClass().getName() +
                        "\", id generator must be sub type of \"" +
                        SequenceIdGenerator.class.getName() +
                        "\", \"" +
                        IdentityIdGenerator.class.getName() +
                        "\" or \"" +
                        UserIdGenerator.class.getName() +
                        "\""
        );
    }

    public SaveContext prop(ImmutableProp prop) {
        return new SaveContext(this, prop, null);
    }

    public SaveContext backProp(ImmutableProp backProp) {
        return new SaveContext(this, null, backProp);
    }

    public SaveContext investigator(JSqlClientImplementor sqlClient) {
        return new SaveContext(
                this,
                Investigators.toInvestigatorSqlClient(sqlClient, null)
        );
    }

    public SaveContext investigator(Executor.BatchContext ctx) {
        return new SaveContext(
                this,
                Investigators.toInvestigatorSqlClient(ctx.sqlClient(), ctx)
        );
    }
}
