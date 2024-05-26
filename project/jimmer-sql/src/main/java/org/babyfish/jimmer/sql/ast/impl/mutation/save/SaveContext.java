package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
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

class SaveContext {

    final SaveOptions options;

    final Connection con;

    final MutationTrigger trigger;

    final boolean triggerSubmitImmediately;

    final Map<AffectedTable, Integer> affectedRowCountMap;

    final SavePath path;

    final ImmutableProp backReferenceProp;

    final boolean backReferenceFrozen;

    private boolean triggerSubmitted;

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type
    ) {
        this(options, con, type, true, new LinkedHashMap<>());
    }

    SaveContext(
            SaveOptions options,
            Connection con,
            ImmutableType type,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.options = options;
        this.con = con;
        this.trigger = options.getTriggers() != null ? new MutationTrigger() : null;
        this.triggerSubmitImmediately = triggerSubmitImmediately && this.trigger != null;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = SavePath.root(type);
        this.backReferenceProp = null;
        this.backReferenceFrozen = false;
    }

    SaveContext(SaveContext base, SaveOptions options, ImmutableProp prop) {
        this.options = options;
        this.con = base.con;
        this.trigger = base.trigger;
        this.triggerSubmitImmediately = this.trigger != null;
        this.affectedRowCountMap = base.affectedRowCountMap;
        this.path = base.path.to(prop);
        if (prop.getAssociationAnnotation().annotationType() == OneToMany.class) {
            this.backReferenceProp = prop.getMappedBy();
            this.backReferenceFrozen = !((OneToMany)prop.getAssociationAnnotation()).isTargetTransferable();
        } else {
            this.backReferenceProp = null;
            this.backReferenceFrozen = false;
        }
    }

    public Object allocateId() {
        IdGenerator idGenerator = path.getType().getIdGenerator(options.getSqlClient());
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
}
