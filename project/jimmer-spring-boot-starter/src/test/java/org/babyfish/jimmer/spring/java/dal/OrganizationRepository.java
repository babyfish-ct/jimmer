package org.babyfish.jimmer.spring.java.dal;

import org.babyfish.jimmer.spring.java.model.issue1162.Organization;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JRepository<Organization, UUID> {

    Optional<Organization> findByIdmOrgId(
            UUID idmOrgId,
            Fetcher<Organization> fetcher
    );
}
