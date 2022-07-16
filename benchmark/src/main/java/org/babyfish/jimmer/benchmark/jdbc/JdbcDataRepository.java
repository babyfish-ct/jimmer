package org.babyfish.jimmer.benchmark.jdbc;

import org.springframework.data.repository.CrudRepository;

public interface JdbcDataRepository extends CrudRepository<JdbcData, Long> {}
