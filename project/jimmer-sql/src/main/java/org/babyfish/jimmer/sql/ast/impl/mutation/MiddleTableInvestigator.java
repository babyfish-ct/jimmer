package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.MutationPath;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.util.*;

class MiddleTableInvestigator {

    private final BatchUpdateException ex;

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final MutationPath path;

    private final ImmutableType targetType;

    private final Collection<Tuple2<Object, Object>> idTuples;

    private final Fetcher<ImmutableSpi> targetIdFetcher;

    @SuppressWarnings("unchecked")
    MiddleTableInvestigator(
            BatchUpdateException ex,
            JSqlClientImplementor sqlClient,
            Connection con,
            MutationPath path,
            Collection<Tuple2<Object, Object>> idTuples) {
        this.ex = ex;
        this.sqlClient = sqlClient;
        this.con = con;
        this.path = path;
        this.targetType = path.getProp().getTargetType();
        this.idTuples = idTuples;
        this.targetIdFetcher = new FetcherImpl<>((Class<ImmutableSpi>)targetType.getJavaClass());
    }

    public Exception investigate() {
        if (sqlClient.getDialect().isBatchUpdateExceptionUnreliable()) {
            Exception translated = translateAll();
            if (translated != null) {
                return translated;
            }
        } else {
            int[] rowCounts = ex.getUpdateCounts();
            int index = 0;
            for (Tuple2<Object, Object> idTuple : idTuples) {
                if (rowCounts[index++] < 0) {
                    Exception translated = translateOne(idTuple);
                    if (translated != null) {
                        return translated;
                    }
                }
            }
        }
        return ex;
    }

    private Exception translateOne(Tuple2<Object, Object> idTuple) {
        List<ImmutableSpi> targets = Rows.findRows(
                sqlClient,
                con,
                targetType,
                QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                targetIdFetcher,
                (q, t) -> {
                    q.where(t.getId().eq(idTuple.get_2()));
                }
        );
        if (targets.isEmpty()) {
            return MutationContext.createIllegalTargetId(path, Collections.singleton(idTuple.get_2()));
        }
        return null;
    }

    private Exception translateAll() {
        Collection<Object> targetIds = Tuple2.projection2(idTuples);
        List<ImmutableSpi> existingTargets = Rows.findRows(
                sqlClient,
                con,
                targetType,
                QueryReason.INVESTIGATE_CONSTRAINT_VIOLATION_ERROR,
                targetIdFetcher,
                (q, t) -> {
                    q.where(t.getId().in(targetIds));
                }
        );
        Set<Object> existingTargetIds = new LinkedHashSet<>(
                (existingTargets.size() * 4 + 2) / 3
        );
        PropId targetIdPropId = targetType.getIdProp().getId();
        for (ImmutableSpi target : existingTargets) {
            existingTargetIds.add(target.__get(targetIdPropId));
        }
        for (Object targetId : targetIds) {
            if (!existingTargetIds.contains(targetId)) {
                return MutationContext.createIllegalTargetId(path, Collections.singleton(targetId));
            }
        }
        return null;
    }
}
