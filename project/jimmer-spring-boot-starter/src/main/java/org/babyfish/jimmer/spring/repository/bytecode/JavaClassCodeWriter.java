package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.spring.repository.support.JRepositoryImpl;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.data.repository.core.RepositoryInformation;

import java.lang.reflect.Method;

public class JavaClassCodeWriter extends ClassCodeWriter {

    public JavaClassCodeWriter(RepositoryInformation metadata) {
        super(metadata, JSqlClient.class, JRepositoryImpl.class);
    }

    @Override
    protected MethodCodeWriter createMethodCodeWriter(Method method, String id) {
        return new JavaMethodCodeWriter(this, method, id);
    }
}
