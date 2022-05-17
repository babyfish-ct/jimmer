package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;

public interface RecursiveListLoader extends RecursiveLoader, ListLoader {

    @OldChain
    RecursiveListLoader depth(int depth);

    @OldChain
    RecursiveListLoader recursive();

    @OldChain
    @Override
    RecursiveListLoader batch(int size);

    @OldChain
    @Override
    RecursiveListLoader limit(int limit);
}
