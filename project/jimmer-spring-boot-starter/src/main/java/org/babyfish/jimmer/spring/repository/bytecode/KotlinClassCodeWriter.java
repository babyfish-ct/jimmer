package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.spring.repository.support.KRepositoryImpl;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.springframework.data.repository.core.RepositoryInformation;

public class KotlinClassCodeWriter extends ClassCodeWriter {

    public KotlinClassCodeWriter(RepositoryInformation metadata) {
        super(metadata, KSqlClient.class, KRepositoryImpl.class);
    }
}
