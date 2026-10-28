package com.contoh.test.repository;

import com.contoh.test.dto.BatchJobsSearchCriteria;
import com.contoh.test.model.Batchjob;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Repository
public class BatchJobSearchRepository {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "jobName", "status", "retryCount", "createdAt");

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Batchjob> search(BatchJobsSearchCriteria c) {
        if (!SORTABLE_FIELDS.contains(c.sortBy())) {
            throw new IllegalArgumentException("sort tidak valid, pilihan: " + SORTABLE_FIELDS);
        }
        boolean asc = "asc".equalsIgnoreCase(c.sortDirection());
        if (!asc && !"desc".equalsIgnoreCase(c.sortDirection())) {
            throw new IllegalArgumentException("sort tidak valid, pilihan: " + SORTABLE_FIELDS);
        }

        // Query data
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Batchjob> dataQuery = cb.createQuery(Batchjob.class);
        Root<Batchjob> root = dataQuery.from(Batchjob.class);
        Order order = asc ? cb.asc(root.get(c.sortBy())) : cb.desc(root.get(c.sortBy()));
        dataQuery.select(root).where(buildPredicate(cb, root, c)).orderBy(order);

        List<Batchjob> content = entityManager.createQuery(dataQuery)
                .setFirstResult(c.page() * c.size())
                .setMaxResults(c.size())
                .getResultList();

        // Query total pagination
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Batchjob> countRoot = countQuery.from(Batchjob.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicate(cb, countRoot, c));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        Sort sort = Sort.by(asc ? Sort.Direction.ASC : Sort.Direction.DESC, c.sortBy());
        return new PageImpl<>(content, PageRequest.of(c.page(), c.size()), total);
    }

    private Predicate[] buildPredicate(CriteriaBuilder cb, Root<Batchjob> root, BatchJobsSearchCriteria c) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(c.jobName())) {
            predicates.add(cb.like(cb.lower(root.<String>get("jobName")), "%" + c.jobName().trim().toLowerCase() + "%"));
        }

        if (c.status() != null) {
            predicates.add(cb.equal(root.get("status"), c.status()));
        }

        if (StringUtils.hasText(c.jobType())) {
            predicates.add(cb.equal(cb.lower(root.<String>get("jobType")), c.jobType().trim().toLowerCase()));
        }

        if (c.startDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), c.startDate().atStartOfDay()));
        }

        if (c.endDate() != null) {
            predicates.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), c.endDate().plusDays(1).atStartOfDay()));
        }

        return predicates.toArray(new Predicate[0]);
    }
}
