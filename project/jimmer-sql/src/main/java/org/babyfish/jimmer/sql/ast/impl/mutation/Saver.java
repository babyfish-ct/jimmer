package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Saver {

    private AbstractSaveCommandImpl.Data data;

    private Connection con;

    private ImmutableType immutableType;

    private Map<String, Integer> affectedRowCount;

    public Saver(AbstractSaveCommandImpl.Data data, Connection con) {
        this.data = data;
        this.con = con;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        E newEntity = (E)Internal.produce(immutableType, entity, draft -> {
            saveImpl((DraftSpi) draft);
        });
        return null;
    }

    private void saveImpl(DraftSpi draftSpi) {

    }
}
