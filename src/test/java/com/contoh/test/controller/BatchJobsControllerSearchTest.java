package com.contoh.test.controller;

import com.contoh.test.controllers.BatchJobController;
import com.contoh.test.dto.BatchJobsSearchCriteria;
import com.contoh.test.model.Status;
import com.contoh.test.service.BatchJobsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;

@WebMvcTest(BatchJobController.class)
public class BatchJobsControllerSearchTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchJobsService service;

    @Test
    @DisplayName("tanpa parameter -> pakai default: page 0, size 10, createdAt, asc")
    void search_default() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(service).search(new BatchJobsSearchCriteria(null, null, null, null, null, 0, 10, "createdAt", "desc"));
    }

    @Test
    @DisplayName("semua parameter -> terbaca benar (enum status & tanggal format ISO)")
    void search_semuaParameter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs")
                        .param("jobName", "sync")
                        .param("status", "ACTIVE")
                        .param("jobType", "SYNC")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "jobName")
                        .param("sortDirection", "asc"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(service).search(new BatchJobsSearchCriteria(
                "sync", Status.ACTIVE, "SYNC",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                1, 5, "jobName", "asc"));
    }
}
