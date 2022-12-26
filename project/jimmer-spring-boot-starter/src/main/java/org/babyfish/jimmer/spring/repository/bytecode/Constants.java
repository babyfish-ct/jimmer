package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.QueryMethod;
import org.babyfish.jimmer.spring.repository.support.JavaExecutors;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;

interface Constants {

    String CONTEXT_INTERNAL_NAME = Type.getInternalName(Context.class);

    String CONTEXT_DESCRIPTOR = Type.getDescriptor(Context.class);

    String QUERY_METHOD_INTERNAL_NAME = Type.getInternalName(QueryMethod.class);

    String QUERY_METHOD_DESCRIPTOR = Type.getDescriptor(QueryMethod.class);

    String IMMUTABLE_TYPE_INTERNAL_NAME = Type.getInternalName(ImmutableType.class);

    String IMMUTABLE_TYPE_DESCRIPTOR = Type.getDescriptor(ImmutableType.class);

    String METHOD_DESCRIPTOR = Type.getDescriptor(Method.class);

    String JAVA_EXECUTORS_INTERNAL_NAME = Type.getInternalName(JavaExecutors.class);

    String JAVA_EXECUTORS_EXECUTE_DESCRIPTOR = '(' +
            Type.getDescriptor(JSqlClient.class) +
            IMMUTABLE_TYPE_DESCRIPTOR +
            QUERY_METHOD_DESCRIPTOR +
            Type.getDescriptor(Pageable.class) +
            Type.getDescriptor(Sort.class) +
            Type.getDescriptor(Fetcher.class) +
            "[Ljava/lang/Object;)Ljava/lang/Object;";
}
