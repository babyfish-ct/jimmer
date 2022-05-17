package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;

public interface ListLoader extends Loader {

    @OldChain
    @Override
    ListLoader batch(int size);

    @OldChain
    ListLoader limit(int limit);
}
