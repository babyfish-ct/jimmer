package org.babyfish.jimmer.spring.repository;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.Input;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JRepository<E, ID> extends Repository<E, ID> {

    JSqlClient sql();

    Page<E> page(Pageable pageable, ConfigurableRootQuery<?, E> query);

    E findById(ID id);

    E findById(ID id, Fetcher<E> fetcher);

    Optional<E> findOptionalById(ID id);

    Optional<E> findOptionalById(ID id, Fetcher<E> fetcher);

    List<E> findByIds(Collection<ID> ids);

    List<E> findByIds(Collection<ID> ids, Fetcher<E> fetcher);

    Map<ID, E> findMapByIds(Collection<ID> ids);

    Map<ID, E> findMapByIds(Collection<ID> ids, Fetcher<E> fetcher);

    List<E> findAll();

    List<E> findAll(Fetcher<E> fetcher);

    List<E> findAll(TypedProp.Scalar<E, ?> prop);

    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<E, ?> prop);

    List<E> findAll(TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2);

    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2);

    List<E> findAll(TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2, TypedProp.Scalar<E, ?> prop3);

    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2, TypedProp.Scalar<E, ?> prop3);

    List<E> findAll(TypedProp.Scalar<?, ?> ... props);

    List<E> findAll(Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... props);

    Page<E> findPage(int pageIndex, int pageSize);

    Page<E> findPage(int pageIndex, int pageSize, Fetcher<E> fetcher);

    Page<E> findPage(int pageIndex, int pageSize, TypedProp.Scalar<E, ?> prop);

    Page<E> findPage(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<E, ?> prop);

    Page<E> findPage(int pageIndex, int pageSize, TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2);

    Page<E> findPage(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2);

    Page<E> findPage(int pageIndex, int pageSize, TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2, TypedProp.Scalar<E, ?> prop3);

    Page<E> findPage(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<E, ?> prop1, TypedProp.Scalar<E, ?> prop2, TypedProp.Scalar<E, ?> prop3);

    Page<E> findPage(int pageIndex, int pageSize, TypedProp.Scalar<?, ?> ... props);

    Page<E> findPage(int pageIndex, int pageSize, Fetcher<E> fetcher, TypedProp.Scalar<?, ?> ... props);

    E save(E entity);

    E save(Input<E> input);

    int delete(E entity);

    int deleteById(ID id);

    int deleteByIds(Collection<ID> ids);
}
