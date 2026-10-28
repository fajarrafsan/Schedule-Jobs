package com.contoh.test.repository;

import com.contoh.test.dto.BatchJobsSearchCriteria;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(BatchJobSearchRepository.class)
public class BatchJobRepositoryTest {
    @Autowired
    private BatchJobSearchRepository searchRepository;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() {
        // createdAt diisi manual supaya filter tanggal bisa dites
        simpan("Cleanup Log", "CLEANUP", Status.INACTIVE, LocalDateTime.of(2026, 9, 30, 23, 0));
        simpan("Sync Data Nasabah", "SYNC", Status.ACTIVE, LocalDateTime.of(2026, 10, 1, 8, 0));
        simpan("Sync Data Transaksi", "SYNC", Status.FAILED, LocalDateTime.of(2026, 10, 15, 23, 59));
        simpan("Report Harian", "REPORT", Status.ACTIVE, LocalDateTime.of(2026, 10, 20, 12, 0));
        em.flush();
        em.clear();
    }

    private void simpan(String jobName, String jobType, Status status, LocalDateTime createdAt) {
        Batchjob job = new Batchjob();
        job.setJobName(jobName);
        job.setJobType(jobType);
        job.setStatus(status);
        job.setRetryCount(0);
        job.setCreatedAt(createdAt);
        em.persist(job);
    }

    private BatchJobsSearchCriteria filter(String jobName, Status status, String jobType,
                                           LocalDate startDate, LocalDate endDate) {
        return new BatchJobsSearchCriteria(jobName, status, jobType, startDate, endDate,
                0, 10, "createdAt", "asc");
    }

    private BatchJobsSearchCriteria pageAndSort(int page, int size, String sortBy, String sortDirection) {
        return new BatchJobsSearchCriteria(null, null, null, null, null, page, size, sortBy, sortDirection);
    }

    @Test
    @DisplayName("tanpa filter -> semua data tampil")
    void tanpaFilter() {
        Page<Batchjob> result = searchRepository.search(filter(null, null, null, null, null));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Cleanup Log", "Sync Data Nasabah", "Sync Data Transaksi", "Report Harian");
    }

    @Test
    @DisplayName("jobName -> partial match, tidak peka huruf besar/kecil, spasi di-trim")
    void filterJobName_partialMatch() {
        Page<Batchjob> result = searchRepository.search(filter("  DATA ", null, null, null, null));

        assertThat(result.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Sync Data Nasabah", "Sync Data Transaksi");
    }

    @Test
    @DisplayName("jobName isinya cuma spasi -> filter diabaikan")
    void filterJobName_kosong() {
        Page<Batchjob> result = searchRepository.search(filter("   ", null, null, null, null));

        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("status -> harus sama persis")
    void filterStatus() {
        Page<Batchjob> result = searchRepository.search(filter(null, Status.ACTIVE, null, null, null));

        assertThat(result.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Sync Data Nasabah", "Report Harian");
    }

    @Test
    @DisplayName("jobType -> sama persis (bukan partial), tidak peka huruf besar/kecil")
    void filterJobType() {
        Page<Batchjob> cocok = searchRepository.search(filter(null, null, "report", null, null));
        Page<Batchjob> sebagian = searchRepository.search(filter(null, null, "SYN", null, null));

        assertThat(cocok.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Report Harian");
        assertThat(sebagian.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("startDate & endDate -> endDate ikut sampai jam 23:59")
    void filterRentangTanggal() {
        Page<Batchjob> result = searchRepository.search(
                filter(null, null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));

        // Cleanup Log (30 Sep) dan Report Harian (20 Okt) di luar rentang
        assertThat(result.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Sync Data Nasabah", "Sync Data Transaksi");
    }

    @Test
    @DisplayName("hanya startDate -> semua data mulai dari tanggal tersebut")
    void filterStartDateSaja() {
        Page<Batchjob> result = searchRepository.search(filter(null, null, null, LocalDate.of(2026, 10, 15), null));

        assertThat(result.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Sync Data Transaksi", "Report Harian");
    }

    @Test
    @DisplayName("beberapa filter sekaligus -> digabung dengan AND")
    void kombinasiFilter() {
        Page<Batchjob> result = searchRepository.search(filter("sync", Status.ACTIVE, null, null, null));

        assertThat(result.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Sync Data Nasabah");
    }

    @Test
    @DisplayName("tidak ada yang cocok -> content kosong, total 0")
    void tidakAdaYangCocok() {
        Page<Batchjob> result = searchRepository.search(filter("tidak-ada", null, null, null, null));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("pagination -> page 0 dan page 1 dengan size 2")
    void pagination() {
        Page<Batchjob> page0 = searchRepository.search(pageAndSort(0, 2, "createdAt", "asc"));
        Page<Batchjob> page1 = searchRepository.search(pageAndSort(1, 2, "createdAt", "asc"));

        assertThat(page0.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Cleanup Log", "Sync Data Nasabah");
        assertThat(page0.getTotalElements()).isEqualTo(4);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.isLast()).isFalse();

        assertThat(page1.getContent()).extracting(Batchjob::getJobName)
                .containsExactly("Sync Data Transaksi", "Report Harian");
        assertThat(page1.isLast()).isTrue();
    }

    @Test
    @DisplayName("pagination + filter -> total di hitung dari data yang tersaring saja")
    void paginationDenganFilter() {
        Page<Batchjob> result = searchRepository.search(new BatchJobsSearchCriteria("sync", null, null, null, null, 0, 1, "createdAt", "asc"));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

}
