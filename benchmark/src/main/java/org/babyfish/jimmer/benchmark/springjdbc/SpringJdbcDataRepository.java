package org.babyfish.jimmer.benchmark.springjdbc;

import org.springframework.data.repository.CrudRepository;

public interface SpringJdbcDataRepository extends CrudRepository<SpringJdbcData, Long> {}
