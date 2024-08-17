package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class SaveContext extends MutationContext {

    final SaveOptions options;

    final Connection con;

    final MutationTrigger2 trigger;

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
                options.getTriggers() != null ? new MutationTrigger2() : null,
                new LinkedHashMap<>()
        );
    }

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type,
            MutationTrigger2 trigger,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        super(MutationPath.root(type));
        this.options = options;
        this.con = con;
        this.trigger = trigger;
        this.affectedRowCountMap = affectedRowCountMap;
        this.backReferenceProp = null;
        this.backReferenceFrozen = false;
    }

    private SaveContext(SaveContext parent, ImmutableProp prop, ImmutableProp backProp) {
        super(prop != null ? parent.path.to(prop) : parent.path.backFrom(backProp));
        if (prop == null) {
            prop = backProp.getOpposite();
        } else {
            backProp = prop.getOpposite();
        }
        this.options = parent.options.toMode(
                prop != null ?
                        parent.options.getAssociatedMode(prop) == AssociatedSaveMode.APPEND ?
                                SaveMode.INSERT_ONLY :
                                SaveMode.UPSERT :
                        SaveMode.UPSERT
        );
        this.con = parent.con;
        this.trigger = parent.trigger;
        this.affectedRowCountMap = parent.affectedRowCountMap;
        if (prop != null && prop.getAssociationAnnotation().annotationType() == OneToMany.class) {
            this.backReferenceProp = prop.getMappedBy();
            this.backReferenceFrozen = !parent.options.isTargetTransferable(prop);
        } else {
            this.backReferenceProp = backProp;
            this.backReferenceFrozen = false;
        }
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
                            null,
                            stmt -> {
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
}
