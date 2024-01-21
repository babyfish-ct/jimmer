package io.quarkiverse.jimmer.it.config;

import org.babyfish.jimmer.sql.meta.UserIdGenerator;

import java.util.UUID;


public class StringIdGenerator implements UserIdGenerator<String> {

    @Override
    public String generate(Class<?> entityType) {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
