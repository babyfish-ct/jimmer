package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;

public interface Loader {

    @OldChain
    Loader batch(int size);

    @OldChain
    Loader limit(int limit);
}
