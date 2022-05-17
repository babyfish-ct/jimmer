package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;

public interface RecursiveLoader extends Loader {

    @OldChain
    RecursiveLoader depth(int depth);

    @OldChain
    RecursiveLoader recursive();

    @OldChain
    @Override
    RecursiveLoader batch(int size);

    @OldChain
    @Override
    RecursiveLoader limit(int limit);
}
