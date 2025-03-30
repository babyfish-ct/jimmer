package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.Input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface SaveCommandCreator {

    <E> SimpleEntitySaveCommand<E> saveCommand(E entity);

    default <E> SimpleEntitySaveCommand<E> saveCommand(Input<E> input) {
        return saveCommand(input.toEntity());
    }

    <E> BatchEntitySaveCommand<E> saveEntitiesCommand(Iterable<E> entities);

    default <E> BatchEntitySaveCommand<E> saveInputsCommand(Iterable<? extends Input<E>> inputs) {
        List<E> entities = inputs instanceof Collection<?> ?
                new ArrayList<>(((Collection<?>)inputs).size()) :
                new ArrayList<>();
        for (Input<E> input : inputs) {
            entities.add(input.toEntity());
        }
        return saveEntitiesCommand(entities);
    }
}
