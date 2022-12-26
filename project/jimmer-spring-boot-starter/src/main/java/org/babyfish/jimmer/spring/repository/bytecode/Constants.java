package org.babyfish.jimmer.spring.repository.bytecode;

import org.babyfish.jimmer.impl.asm.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.spring.repository.parser.Context;
import org.babyfish.jimmer.spring.repository.parser.QueryMethod;
import org.babyfish.jimmer.spring.repository.support.KRepositoryImpl;
import org.babyfish.jimmer.spring.repository.support.QueryExecutors;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.kt.KSqlClient;
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

    String K_SQL_CLIENT_INTERNAL_NAME = Type.getInternalName(KSqlClient.class);

    String K_SQL_CLIENT_DESCRIPTOR = Type.getDescriptor(KSqlClient.class);

    String J_SQL_CLIENT_DESCRIPTOR = Type.getDescriptor(JSqlClient.class);

    String K_REPOSITORY_IMPL = Type.getInternalName(KRepositoryImpl.class);

    String QUERY_EXECUTORS_INTERNAL_NAME = Type.getInternalName(
            QueryExecutors.class);

    String QUERY_EXECUTORS_METHOD_DESCRIPTOR = '(' +
            J_SQL_CLIENT_DESCRIPTOR +
            IMMUTABLE_TYPE_DESCRIPTOR +
            QUERY_METHOD_DESCRIPTOR +
            Type.getDescriptor(Pageable.class) +
            Type.getDescriptor(Sort.class) +
            Type.getDescriptor(Fetcher.class) +
            "[Ljava/lang/Object;)Ljava/lang/Object;";
}
