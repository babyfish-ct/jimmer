package org.babyfish.jimmer.benchmark.jpa;

import org.babyfish.jimmer.benchmark.BenchmarkExecutor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

@Component
public class JpaExecutor extends BenchmarkExecutor {

    private final EntityManagerFactory emf;

    public JpaExecutor(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public String name() {
        return "JPA";
    }

    @Override
    protected List<?> query() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("from JpaData").getResultList();
        } finally {
            em.close();
        }
    }
}
