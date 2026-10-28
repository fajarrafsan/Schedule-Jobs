package com.contoh.test.repository;

import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchJobsRepository extends JpaRepository<Batchjob, Long> {
    List<Batchjob> findByStatus(Status status);
}
