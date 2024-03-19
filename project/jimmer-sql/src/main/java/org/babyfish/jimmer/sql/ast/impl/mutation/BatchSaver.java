package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.runtime.SavePath;

import java.sql.Connection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BatchSaver {

    private static final String GENERAL_OPTIMISTIC_DISABLED_JOIN_REASON = "Joining is disabled in general optimistic lock";

    private final ShapedEntityMap<ImmutableSpi> entityMap = new ShapedEntityMap<>();

    private final IdentityHashMap<Object, Object> newEntityMap = new IdentityHashMap<>();

    private final AbstractEntitySaveCommandImpl.Data data;

    private final Connection con;

    private final SaverCache cache;

    private final MutationTrigger trigger;

    private final boolean triggerSubmitImmediately;

    private final Map<AffectedTable, Integer> affectedRowCountMap;

    private final SavePath path;

    private boolean triggerSubmitted;

    BatchSaver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableType type
    ) {
        this(data, con, type, new SaverCache(data), true, new LinkedHashMap<>());
    }

    BatchSaver(
            AbstractEntitySaveCommandImpl.Data data,
            Connection con,
            ImmutableType type,
            SaverCache cache,
            boolean triggerSubmitImmediately,
            Map<AffectedTable, Integer> affectedRowCountMap
    ) {
        this.data = data;
        this.con = con;
        this.cache = cache;
        this.trigger = data.getTriggers() != null ? new MutationTrigger() : null;
        this.triggerSubmitImmediately = triggerSubmitImmediately && this.trigger != null;
        this.affectedRowCountMap = affectedRowCountMap;
        this.path = SavePath.root(type);
    }

    @SuppressWarnings("unchecked")
    public <E> void add(E entity) {
        if (!(entity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("The argument \"entity\" must be immutable object");
        }
        if (entity instanceof DraftSpi) {
            throw new IllegalArgumentException("The argument \"entity\" cannot be draft obect");
        }
        entityMap.add((ImmutableSpi) entity);
    }

    @SuppressWarnings("unchecked")
    public boolean execute() {
        List<ImmutableSpi> entities = entityMap.remove();
        if (entities.isEmpty()) {
            return false;
        }
        List<Object> newEntities = Internal.produceList(
                entities.get(0).__type(),
                entities,
                draft -> {
                    execute((List<DraftSpi>) draft);
                },
                trigger == null ? null : trigger::prepareSubmit
        );
        IdentityHashMap<Object, Object> newEntityMap = new IdentityHashMap<>();
        int size = entities.size();
        for (int i = 0; i < size; i++) {
            newEntityMap.put(entities.get(i), newEntities.get(i));
        }
        return true;
    }

    private void execute(List<DraftSpi> drafts) {
        
    }
}
