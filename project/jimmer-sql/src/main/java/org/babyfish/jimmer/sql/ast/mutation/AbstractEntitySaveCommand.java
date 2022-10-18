package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public interface AbstractEntitySaveCommand {

    @NewChain
    AbstractEntitySaveCommand configure(Consumer<Cfg> block);

    interface Cfg {

        @OldChain
        Cfg setMode(SaveMode mode);

        @OldChain
        Cfg setKeyProps(ImmutableProp ... props);

        @OldChain
        default Cfg setKeyProps(TypedProp<?, ?> ... props) {
            return setKeyProps(
                    Arrays
                            .stream(props)
                            .map(TypedProp::unwrap)
                            .toArray(ImmutableProp[]::new)
            );
        }

        @OldChain
        Cfg setAutoAttachingAll();

        @OldChain
        default Cfg setAutoAttaching(TypedProp.Association<?, ?> prop) {
            return setAutoAttaching(prop.unwrap());
        }

        @OldChain
        Cfg setAutoAttaching(ImmutableProp prop);

        @OldChain
        default Cfg setDissociateAction(
                TypedProp.Reference<?, ?> prop,
                DissociateAction dissociateAction
        ) {
            return setDissociateAction(prop.unwrap(), dissociateAction);
        }

        @OldChain
        Cfg setDissociateAction(
                ImmutableProp prop,
                DissociateAction dissociateAction
        );
    }

    interface KeyPropCfg<T> {

        @OldChain
        KeyPropCfg<T> add(ImmutableProp prop);

        @OldChain
        KeyPropCfg<T> add(TypedProp<?, ?> prop);
    }
}
